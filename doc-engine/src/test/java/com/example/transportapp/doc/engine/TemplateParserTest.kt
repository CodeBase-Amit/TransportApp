package com.example.transportapp.doc.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S11: the §9.2 validation matrix — each reject reason fires, a valid template parses, and
 * the schemaVersion gate refuses the future. The default bilty template (the seeder's
 * fixture) must parse clean.
 */
class TemplateParserTest {

    private val biltyTemplate = """
        {
          "schemaVersion": 1,
          "id": "tpl-bilty-default",
          "name": "Default Bilty",
          "version": 1,
          "paper": { "size": "A4", "marginMm": 10 },
          "business": { "shopName": "SHIVSHAKTI ROADLINES", "taxId": "23AABCS4521M1Z9" },
          "sections": [
            { "type": "header" },
            { "type": "title", "title": "CONSIGNMENT NOTE" },
            { "type": "meta", "fields": [ { "key": "docNo", "label": "GR No" }, { "key": "date", "label": "Date" } ] },
            { "type": "customer", "fields": [ { "key": "consignorName", "label": "Consignor" }, { "key": "consigneeName", "label": "Consignee" } ] },
            { "type": "items", "minRows": 6, "columns": [ { "key": "goodsDescription", "label": "Goods" }, { "key": "freight", "label": "Freight" } ] },
            { "type": "totals", "fields": [ { "key": "grandTotal", "label": "Grand Total" } ] },
            { "type": "footer", "fields": [ { "key": "footer", "label": "Terms" } ] }
          ]
        }
    """.trimIndent()

    @Test
    fun `the default bilty template parses clean`() {
        val result = TemplateParser.parse(biltyTemplate)
        val template = (result as TemplateParser.ParseResult.Ok).template
        assertEquals("tpl-bilty-default", template.id)
        assertEquals("SHIVSHAKTI ROADLINES", template.business.shopName)
        assertEquals(7, template.sections.size)
    }

    @Test
    fun `a schema version from the future is refused with an update-the-app message`() {
        val future = biltyTemplate.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
        val result = TemplateParser.parse(future)
        val reason = (result as TemplateParser.ParseResult.Refused).reason
        assertTrue(reason.contains("schemaVersion"))
        assertTrue(reason.contains("update the app"))
    }

    @Test
    fun `an unknown section type is refused`() {
        val bad = biltyTemplate.replace("\"type\": \"notes\"", "\"type\": \"sparkline\"")
            .replace("\"type\": \"footer\"", "\"type\": \"sparkline\"")
        val result = TemplateParser.parse(bad)
        assertTrue((result as TemplateParser.ParseResult.Refused).reason.contains("unknown section type 'sparkline'"))
    }

    @Test
    fun `a non-whitelisted expression is refused`() {
        val bad = biltyTemplate.replace(
            "{ \"key\": \"grandTotal\", \"label\": \"Grand Total\" }",
            "{ \"key\": \"grandTotal\", \"label\": \"Grand Total\", \"expression\": \"sum(customer.phone)\" }",
        )
        val result = TemplateParser.parse(bad)
        assertTrue((result as TemplateParser.ParseResult.Refused).reason.contains("non-whitelisted expression"))
    }

    @Test
    fun `a missing business shop name is refused`() {
        val bad = biltyTemplate.replace("\"shopName\": \"SHIVSHAKTI ROADLINES\"", "\"shopName\": \"\"")
        val result = TemplateParser.parse(bad)
        assertTrue((result as TemplateParser.ParseResult.Refused).reason.contains("shop name"))
    }

    @Test
    fun `a duplicate field key is refused`() {
        val bad = biltyTemplate.replace(
            "{ \"key\": \"date\", \"label\": \"Date\" }",
            "{ \"key\": \"date\", \"label\": \"Date\" }, { \"key\": \"date\", \"label\": \"Date again\" }",
        )
        val result = TemplateParser.parse(bad)
        assertTrue((result as TemplateParser.ParseResult.Refused).reason.contains("more than once"))
    }

    @Test
    fun `malformed json is refused, not thrown`() {
        val result = TemplateParser.parse("{ not json ")
        assertTrue(result is TemplateParser.ParseResult.Refused)
    }

    @Test
    fun `unknown keys are tolerated so small additions stay forward-compatible`() {
        val extended = biltyTemplate.replace(
            "\"business\": { \"shopName\": \"SHIVSHAKTI ROADLINES\", \"taxId\": \"23AABCS4521M1Z9\" }",
            "\"business\": { \"shopName\": \"SHIVSHAKTI ROADLINES\", \"taxId\": \"23AABCS4521M1Z9\", \"logoBadge\": \"new-in-v2\" }",
        )
        assertTrue(TemplateParser.parse(extended) is TemplateParser.ParseResult.Ok)
    }

    @Test
    fun `whitelisted expressions are accepted`() {
        val ok = biltyTemplate.replace(
            "{ \"key\": \"grandTotal\", \"label\": \"Grand Total\" }",
            "{ \"key\": \"grandTotal\", \"label\": \"Grand Total\", \"expression\": \"sum(items.freight)\" }",
        )
        assertEquals(1, TemplateParser.parse(ok).let { it as TemplateParser.ParseResult.Ok }.template.sections
            .first { it.type == "totals" }.fields.count { it.expression == "sum(items.freight)" })
    }
}
