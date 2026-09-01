package com.example.transportapp.doc.engine

/**
 * The S12 fixture: the default bilty template (identical JSON to the seeder's row) plus the
 * 04188 snapshot's payload as the values map. The golden test pins the rendered HTML; the
 * Phase 3 S13 repository test re-uses the same golden file with rows read straight from the
 * database, so any drift between engine, seeder and fixture fails loudly.
 */
object BiltyFixture {

    val TEMPLATE_JSON = """
        {
          "schemaVersion": 1,
          "id": "tpl-bilty-default",
          "name": "Default Bilty",
          "version": 1,
          "paper": { "size": "A4", "marginMm": 10, "orientation": "portrait" },
          "theme": { "primaryColor": "#0E4D38", "textOnPrimary": "#FFFFFF", "fontFamily": "sans" },
          "business": {
            "shopName": "SHIVSHAKTI ROADLINES",
            "address": "Plot 14, Transport Nagar, Indore 452003",
            "mobile": "94250 61183",
            "taxId": "23AABCS4521M1Z9"
          },
          "sections": [
            { "type": "header" },
            { "type": "title", "title": "CONSIGNMENT NOTE" },
            { "type": "meta", "fields": [
              { "key": "docNo", "label": "GR No", "required": true },
              { "key": "date", "label": "Date" },
              { "key": "fromStation", "label": "From" },
              { "key": "toStation", "label": "To" },
              { "key": "stamp", "label": "Payment" }
            ] },
            { "type": "customer", "fields": [
              { "key": "consignorName", "label": "Consignor", "required": true },
              { "key": "consignorAddress", "label": "Address" },
              { "key": "consigneeName", "label": "Consignee", "required": true },
              { "key": "consigneeAddress", "label": "Address" }
            ] },
            { "type": "items", "minRows": 6, "columns": [
              { "key": "goodsDescription", "label": "Goods", "widthMm": 40 },
              { "key": "packages", "label": "Pkgs", "widthMm": 14 },
              { "key": "actualWeight", "label": "Weight", "widthMm": 20 },
              { "key": "rate", "label": "Rate", "widthMm": 18 },
              { "key": "freight", "label": "Freight", "widthMm": 24 }
            ] },
            { "type": "totals", "fields": [
              { "key": "hamali", "label": "Hamali" },
              { "key": "doorDelivery", "label": "Door delivery" },
              { "key": "taxable", "label": "Taxable" },
              { "key": "gst", "label": "GST 5%" },
              { "key": "rounding", "label": "Rounding" },
              { "key": "grandTotal", "label": "Grand Total" }
            ] },
            { "type": "footer", "fields": [
              { "key": "amountInWords", "label": "Amount in words" },
              { "key": "footer", "label": "Terms" }
            ] }
          ]
        }
    """.trimIndent()

    /** The 04188 snapshot payload as a flat value map (org.json decodes this on the device). */
    val SNAPSHOT_04188: Map<String, String> = mapOf(
        "companyName" to "SHIVSHAKTI ROADLINES",
        "addressLine" to "Plot 14, Transport Nagar, Indore 452003",
        "contactLine" to "Ph 94250 61183 · GSTIN 23AABCS4521M1Z9",
        "consignorName" to "Deepak Steel Traders",
        "consignorContact" to "Indore · +91 94250 61183",
        "consignorGstin" to "GSTIN 23AACDS8812K1Z4",
        "consignorAddress" to "Plot 14, Transport Nagar, Indore, 452003",
        "consigneeName" to "Nashik Hardware Mart",
        "consigneeContact" to "Nashik · +91 98600 27419",
        "consigneeGstin" to "GSTIN 27AAFCN3390L1Z8",
        "consigneeAddress" to "MIDC Ambad, Nashik",
        "docNo" to "IND/2627/04188",
        "date" to "25.08.2026",
        "fromStation" to "INDORE",
        "toStation" to "NASHIK",
        "packages" to "12",
        "goodsDescription" to "MS PIPES",
        "actualWeight" to "780 kg",
        "chargeableWeight" to "780 kg",
        "rate" to "4.50",
        "freight" to "3,510.00",
        "hamali" to "96.00",
        "doorDelivery" to "150.00",
        "taxable" to "3,756.00",
        "gst" to "187.80",
        "rounding" to "0.20",
        "gstLabel" to "GST 5% — we pay, forward charge",
        "totalLabel" to "Grand Total",
        "grandTotal" to "3,944.00",
        "amountInWords" to "Rupees three thousand nine hundred forty four only",
        "stamp" to "TO PAY",
        "footer" to "At owner's risk · Door delivery · Private mark DST-114 · E-way bill 281047556392",
    )

    fun template(): TemplateModel = (TemplateParser.parse(TEMPLATE_JSON) as TemplateParser.ParseResult.Ok).template
}
