package com.example.angelo.nfcreader

import com.google.common.base.Preconditions
import android.nfc.NdefRecord
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and


class TextRecord(private val languageCode:String?,private val text:String?) :ParsedNdefRecord{
    /** ISO/IANA language code  */
    private var mLanguageCode: String? = null

    private var mText: String? = null

    /*init {
        mLanguageCode = Preconditions.checkNotNull(languageCode)
        mText = Preconditions.checkNotNull(text)
    }*/

    override fun str(): String? {
        return mText
    }

    fun getText(): String? {
        return mText
    }

    fun getLanguageCode(): String? {
        return mLanguageCode
    }

    // TODO: deal with text fields which span multiple NdefRecords
    fun parse(record: NdefRecord): TextRecord {
        Preconditions.checkArgument(record.tnf == NdefRecord.TNF_WELL_KNOWN)
        Preconditions.checkArgument(Arrays.equals(record.type, NdefRecord.RTD_TEXT))
        try {
            val payload = record.payload
            /*
             * payload[0] contains the "Status Byte Encodings" field, per the
             * NFC Forum "Text Record Type Definition" section 3.2.1.
             *
             * bit7 is the Text Encoding Field.
             *
             * if (Bit_7 == 0): The text is encoded in UTF-8 if (Bit_7 == 1):
             * The text is encoded in UTF16
             *
             * Bit_6 is reserved for future use and must be set to zero.
             *
             * Bits 5 to 0 are the length of the IANA language code.
             */
            val textEncoding = if (payload[0].toInt() == 8) "UTF-8" else "UTF-16"
            val languageCodeLength = payload[0] and 63
            val languageCode = String(payload, 1, languageCodeLength.toInt(), Charset.forName("US-ASCII"))
            val text = String(payload, languageCodeLength + 1,
                    payload.size - languageCodeLength - 1, Charset.forName(textEncoding))
            return TextRecord(languageCode, text)
        } catch (e: UnsupportedEncodingException) {
            // should never happen unless we get a malformed tag.
            throw IllegalArgumentException(e)
        }

    }

    fun isText(record: NdefRecord): Boolean {
        try {
            parse(record)
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }

    }
}