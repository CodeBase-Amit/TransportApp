package com.example.transportapp.domain.transport.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** §3 rate resolution: the five-step order is the product promise's engine. */
class RateResolverTest {

    private fun candidate(
        localId: String,
        partyId: String? = null,
        routeId: String? = null,
        goodsId: String? = null,
        ratePaise: Long = 450,
        sortOrder: Int = 0,
    ) = RateCandidate(localId, partyId, routeId, goodsId, RateBasis.PER_KG, ratePaise, sortOrder = sortOrder)

    private val party = "party-1"
    private val route = "route-1"
    private val goods = "goods-1"

    @Test
    fun `steps win in the fixed order`() {
        val candidates = listOf(
            candidate("company-default"),
            candidate("route-row", routeId = route),
            candidate("route-goods-row", routeId = route, goodsId = goods),
            candidate("party-route-row", partyId = party, routeId = route),
            candidate("party-route-goods-row", partyId = party, routeId = route, goodsId = goods),
        )
        assertEquals(1, RateResolver.resolve(candidates, party, route, goods)!!.step)
        assertEquals("party-route-goods-row", RateResolver.resolve(candidates, party, route, goods)!!.candidate.localId)
    }

    @Test
    fun `each fallback step is reached in turn`() {
        val candidates = listOf(
            candidate("company-default"),
            candidate("route-row", routeId = route),
            candidate("route-goods-row", routeId = route, goodsId = goods),
        )
        assertEquals("step 3: route+goods", "route-goods-row", RateResolver.resolve(candidates, party, route, goods)!!.candidate.localId)
        assertEquals(3, RateResolver.resolve(candidates, party, route, goods)!!.step)

        val withoutGoodsRow = candidates.filter { it.localId != "route-goods-row" }
        assertEquals("step 4: route", "route-row", RateResolver.resolve(withoutGoodsRow, party, route, goods)!!.candidate.localId)
        assertEquals(4, RateResolver.resolve(withoutGoodsRow, party, route, goods)!!.step)

        val routeOnly = withoutGoodsRow.filter { it.localId != "route-row" }
        assertEquals("step 5: company default", "company-default", RateResolver.resolve(routeOnly, party, route, goods)!!.candidate.localId)
        assertEquals(5, RateResolver.resolve(routeOnly, party, route, goods)!!.step)
    }

    @Test
    fun `lowest sort order wins within a step`() {
        val candidates = listOf(
            candidate("b", partyId = party, routeId = route, goodsId = goods, ratePaise = 500, sortOrder = 2),
            candidate("a", partyId = party, routeId = route, goodsId = goods, ratePaise = 400, sortOrder = 1),
        )
        assertEquals("a", RateResolver.resolve(candidates, party, route, goods)!!.candidate.localId)
    }

    @Test
    fun `booking without a route falls straight to the company default`() {
        val candidates = listOf(
            candidate("party-route-row", partyId = party, routeId = route),
            candidate("company-default"),
        )
        val resolved = RateResolver.resolve(candidates, party, routeId = null, goodsId = null)!!
        assertEquals(5, resolved.step)
        assertEquals("company-default", resolved.candidate.localId)
    }

    @Test
    fun `out-of-scope rows never win`() {
        val candidates = listOf(
            candidate("other-party", partyId = "party-2", routeId = route, goodsId = goods),
            candidate("other-route", partyId = party, routeId = "route-2", goodsId = goods),
            candidate("other-goods", partyId = party, routeId = route, goodsId = "goods-2"),
        )
        assertNull("no matching row and no company default", RateResolver.resolve(candidates, party, route, goods))
    }

    @Test
    fun `no candidates at all resolves to null`() {
        assertNull(RateResolver.resolve(emptyList(), party, route, goods))
    }
}
