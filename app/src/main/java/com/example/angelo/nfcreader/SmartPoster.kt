package com.example.angelo.nfcreader

import com.google.common.base.Preconditions
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterables
import java.util.*






class SmartPoster(private  val uri:UriRecord?, private  val title:TextRecord?, private  val action:RecommendedAction?, private  val type:String?):ParsedNdefRecord {

    /**
     * NFC Forum Smart Poster Record Type Definition section 3.2.1.
     *
     * "The Title record for the service (there can be many of these in
     * different languages, but a language MUST NOT be repeated). This record is
     * optional."
     */
    private var mTitleRecord: TextRecord? = null

    /**
     * NFC Forum Smart Poster Record Type Definition section 3.2.1.
     *
     * "The URI record. This is the core of the Smart Poster, and all other
     * records are just metadata about this record. There MUST be one URI record
     * and there MUST NOT be more than one."
     */
    private var mUriRecord: UriRecord? = null

    /**
     * NFC Forum Smart Poster Record Type Definition section 3.2.1.
     *
     * "The Action record. This record describes how the service should be
     * treated. For example, the action may indicate that the device should save
     * the URI as a bookmark or open a browser. The Action record is optional.
     * If it does not exist, the device may decide what to do with the service.
     * If the action record exists, it should be treated as a strong suggestion;
     * the UI designer may ignore it, but doing so will induce a different user
     * experience from device to device."
     */
    private var mAction: RecommendedAction? = null

    /**
     * NFC Forum Smart Poster Record Type Definition section 3.2.1.
     *
     * "The Type record. If the URI references an external entity (e.g., via a
     * URL), the Type record may be used to declare the MIME type of the entity.
     * This can be used to tell the mobile device what kind of an object it can
     * expect before it opens the connection. The Type record is optional."
     */
    private var mType: String? = null

    /*init {
        mUriRecord = Preconditions.checkNotNull(uri);
        mTitleRecord = title;
        mAction = Preconditions.checkNotNull(action);
        mType = type;
    }*/

    fun getUriRecord(): UriRecord? {
        return mUriRecord
    }

    /**
     * Returns the title of the smart poster. This may be `null`.
     */
    fun getTitle(): TextRecord? {
        return mTitleRecord
    }

    fun parse(record: NdefRecord): SmartPoster {
        Preconditions.checkArgument(record.tnf == NdefRecord.TNF_WELL_KNOWN)
        Preconditions.checkArgument(Arrays.equals(record.type, NdefRecord.RTD_SMART_POSTER))
        try {
            val subRecords = NdefMessage(record.payload)
            return parse(subRecords.records as NdefRecord)
        } catch (e: FormatException) {
            throw IllegalArgumentException(e)
        }

    }

    fun parse(recordsRaw: Array<NdefRecord>): SmartPoster {
        try {
            val ndef = NdefMessageParser()
            val records = ndef.getRecords(recordsRaw)
            val uri = Iterables.getOnlyElement(Iterables.filter(records, UriRecord::class.java))
            val title = getFirstIfExists(records, TextRecord::class.java)
            val action = parseRecommendedAction(recordsRaw)
            val type = parseType(recordsRaw)
            return SmartPoster(uri, title!!, action!!, type!!)
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException(e)
        }

    }

    fun isPoster(record: NdefRecord): Boolean {
        try {
            parse(record)
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }

    }

    override fun str(): String? {
        return if (mTitleRecord != null) {
            mTitleRecord!!.str() + "\n" + mUriRecord!!.str()
        } else {
            mUriRecord!!.str()
        }
    }

    /**
     * Returns the first element of `elements` which is an instance of
     * `type`, or `null` if no such element exists.
     */
    private fun <T> getFirstIfExists(elements: Iterable<*>, type: Class<T>): T? {
        val filtered = Iterables.filter(elements, type)
        var instance: T? = null
        if (!Iterables.isEmpty(filtered)) {
            instance = Iterables.get(filtered, 0)
        }
        return instance
    }


    enum class RecommendedAction private constructor(private val byte: Byte) {
        UNKNOWN((-1).toByte()), DO_ACTION(0.toByte()), SAVE_FOR_LATER(1.toByte()), OPEN_FOR_EDITING(
                2.toByte());


        companion object {

            val LOOKUP: ImmutableMap<Byte, RecommendedAction>

            init {
                val builder = ImmutableMap.builder<Byte, SmartPoster.RecommendedAction>()
                for (action in RecommendedAction.values()) {
                    builder.put(action.byte, action)
                }
                LOOKUP = builder.build()
            }
        }
    }

    private fun getByType(type: ByteArray, records: Array<NdefRecord>): NdefRecord? {
        for (record in records) {
            if (Arrays.equals(type, record.type)) {
                return record
            }
        }
        return null
    }

    private val ACTION_RECORD_TYPE = byteArrayOf('a'.toByte(), 'c'.toByte(), 't'.toByte())

    private fun parseRecommendedAction(records: Array<NdefRecord>): RecommendedAction? {
        val record = getByType(ACTION_RECORD_TYPE, records) ?: return RecommendedAction.UNKNOWN
        val action = record.payload[0]
        return if (RecommendedAction.LOOKUP.containsKey(action)) {
            RecommendedAction.LOOKUP!![action]
        } else RecommendedAction.UNKNOWN
    }

    private val TYPE_TYPE = byteArrayOf('t'.toByte())

    private fun parseType(records: Array<NdefRecord>): String? {
        val type = getByType(TYPE_TYPE, records) ?: return null
        return String(type.payload, Charsets.UTF_8)
    }
}