package com.example.angelo.nfcreader

import android.nfc.NdefMessage
import android.nfc.NdefRecord



class  NdefMessageParser {

    fun parse(message: NdefMessage): List<ParsedNdefRecord> {
        return getRecords(message.records)
    }

    fun getRecords(records: Array<NdefRecord>): List<ParsedNdefRecord> {
        val elements = ArrayList<ParsedNdefRecord>()

        for (record in records) {
            val uriRecord = UriRecord(null)
            val textRecord = TextRecord(null,null)
            val smartPoster = SmartPoster(null,null,null,null)
            if (uriRecord.isUri(record)) {
                elements.add(uriRecord.parse(record))
            } else if (textRecord.isText(record)) {
                elements.add(textRecord.parse(record))
            } else if (smartPoster.isPoster(record)) {
                elements.add(smartPoster.parse(record))
            } else {
                elements.add(object : ParsedNdefRecord {
                    override fun str(): String? {
                        return String(record.payload)
                    }
                })
            }
        }
        return elements
    }

}