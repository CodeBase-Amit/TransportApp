package com.example.transportapp.doc.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The §9.14 pipeline invariants as tests: determinism, escaping, the goods-grid row count,
 * recomputed totals from the snapshot's own values, section visibility, the copy-sheets
 * shape, the void watermark — and the source-level guard that this renderer contains no
 * business field name at all.
 */
class HtmlRendererTest {

    @Test
    fun `rendering the same snapshot twice is byte-identical`() {
        val template = BiltyFixture.template()
        val first = HtmlRenderer.render(template, BiltyFixture.SNAPSHOT_04188)
        val second = HtmlRenderer.render(template, BiltyFixture.SNAPSHOT_04188)
        assertEquals(first, second)
        assertTrue(first.contains("IND/2627/04188"))
        assertTrue(first.contains("3,944.00"))
        assertTrue(first.contains("Rupees three thousand nine hundred forty four only"))
    }

    @Test
    fun `every metacharacter in a value appears escaped in the output`() {
        val template = BiltyFixture.template()
        val hostile = BiltyFixture.SNAPSHOT_04188 + mapOf(
            "consignorName" to "Sharma & Sons <Traders> \"Ltd\" 'Pune'",
            "goodsDescription" to "<script>alert(1)</script>",
        )
        val html = HtmlRenderer.render(template, hostile)
        assertFalse(html.contains("<script>alert(1)</script>"))
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
        assertTrue(html.contains("Sharma &amp; Sons &lt;Traders&gt; &quot;Ltd&quot; &#39;Pune&#39;"))
    }

    @Test
    fun `the goods grid prints exactly one header plus max of rows and the template minimum`() {
        val template = BiltyFixture.template()
        val html = HtmlRenderer.render(template, BiltyFixture.SNAPSHOT_04188)
        val tbody = html.substringAfter("<tbody>").substringBefore("</tbody>")
        assertEquals(6, tbody.split("<tr>").size - 1) // minRows 6 with a single real row

        val many = BiltyFixture.SNAPSHOT_04188 + mapOf(
            "items" to """[{"goodsDescription":"A","freight":"1.00"},{"goodsDescription":"B","freight":"2.00"},{"goodsDescription":"C","freight":"3.00"}]""",
        )
        val htmlMany = HtmlRenderer.render(template, many)
        val tbodyMany = htmlMany.substringAfter("<tbody>").substringBefore("</tbody>")
        assertEquals(6, tbodyMany.split("<tr>").size - 1) // 3 real rows padded to 6
        assertTrue(htmlMany.contains(">A<") && htmlMany.contains(">B<") && htmlMany.contains(">C<"))
    }

    @Test
    fun `totals carrying a whitelisted expression are recomputed from the snapshot's own values`() {
        val template = BiltyFixture.template()
        // The 04188 map stores freight = 3,510.00; an expression on that figure must
        // recompute to the same rupee amount from the goods row — never trust the stored total.
        val recomputeTemplate = template.copy(
            sections = template.sections.map { s ->
                if (s.type == "totals") s.copy(fields = s.fields.map { f ->
                    if (f.key == "grandTotal") f.copy(expression = "sum(items.freight)") else f
                }) else s
            },
        )
        val html = HtmlRenderer.render(recomputeTemplate, BiltyFixture.SNAPSHOT_04188)
        assertTrue(html.contains("3,510.00"))
        assertEquals(1, Expressions.evaluate("count(items)", listOf(mapOf("a" to "1")))!!.toInt())
    }

    @Test
    fun `the sum expression adds grouped money exactly in paise`() {
        val rows = listOf(
            mapOf("amount" to "1,234.56"),
            mapOf("amount" to "2,710.44"),
        )
        assertEquals("3,945.00", Expressions.evaluate("sum(items.amount)", rows))
        assertEquals("0.20", Expressions.evaluate("sum(items.rounding)", listOf(mapOf("rounding" to "0.20"))))
        assertEquals("2", Expressions.evaluate("count(items)", rows))
        assertEquals("0.00", Expressions.evaluate("sum(items.amount)", emptyList()))
    }

    @Test
    fun `visibleWhen hides a section whose guard does not match`() {
        val template = BiltyFixture.template().let { t ->
            t.copy(sections = t.sections.map { s ->
                if (s.type == "footer") s.copy(visibleWhen = VisibleWhen(field = "stamp", equals = "PAID")) else s
            })
        }
        val hidden = HtmlRenderer.render(template, BiltyFixture.SNAPSHOT_04188) // stamp is TO PAY
        assertFalse(hidden.contains("Amount in words"))

        val shown = HtmlRenderer.render(template, BiltyFixture.SNAPSHOT_04188 + mapOf("stamp" to "PAID"))
        assertTrue(shown.contains("Amount in words"))
    }

    @Test
    fun `four copies produce one document with four sheets and escaped labels`() {
        val html = HtmlRenderer.renderCopies(
            BiltyFixture.template(),
            BiltyFixture.SNAPSHOT_04188,
            listOf("Copy 1 · Office", "Copy 2 · Driver <b>", "Copy 3 · Consignee", "Copy 4 · Book"),
        )
        assertEquals(4, Regex("<div class=\"sheet\">").findAll(html).count())
        val style = html.substringAfter("<style>").substringBefore("</style>")
        assertTrue("every sheet breaks after, except the last", style.contains(".sheet { width:") && style.contains("page-break-after: always") && style.contains(".sheet:last-child { page-break-after: auto;"))
        assertTrue(html.contains("Copy 1 · Office"))
        assertTrue(html.contains("Copy 2 · Driver &lt;b&gt;"))
        assertTrue(html.contains("Copy 4 · Book"))
    }

    @Test
    fun `a voided snapshot carries the watermark`() {
        val html = HtmlRenderer.render(BiltyFixture.template(), BiltyFixture.SNAPSHOT_04188 + mapOf("voided" to "true"))
        assertTrue(html.contains("watermark"))
        assertTrue(html.contains("VOID"))
    }

    @Test
    fun `the golden file matches — run with -Dgolden-dot-update equals true to regenerate`() {
        val html = HtmlRenderer.render(BiltyFixture.template(), BiltyFixture.SNAPSHOT_04188)
        val golden = File("src/test/resources/golden/bilty-04188.html")
        if (System.getProperty("golden.update") == "true") {
            golden.parentFile.mkdirs()
            golden.writeText(html, Charsets.UTF_8)
        }
        assertTrue("golden file missing — run :doc-engine:test with -Dgolden.update=true", golden.exists())
        assertEquals(golden.readText(Charsets.UTF_8), html)
    }

    @Test
    fun `the renderer source contains no business field name — the generalisation guard`() {
        val forbidden = listOf(
            "bilty", "consignor", "consignee", "freight", "hamali", "gst", "challan",
            "lorry", "consignment", "docket", "indore", "nashik", "shivshakti", "rupee",
        )
        val sources = listOf("HtmlRenderer.kt", "Expressions.kt").map {
            File("src/main/java/com/example/transportapp/doc/engine/$it").readText(Charsets.UTF_8)
        }
        sources.forEachIndexed { index, source ->
            forbidden.forEach { term ->
                assertFalse(
                    "renderer source ${listOf("HtmlRenderer.kt", "Expressions.kt")[index]} mentions '$term'",
                    source.lowercase().contains(term),
                )
            }
        }
    }
}
