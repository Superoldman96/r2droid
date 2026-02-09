package top.wsdx233.r2droid.feature.hex.data

import org.json.JSONArray
import top.wsdx233.r2droid.core.data.source.R2DataSource

import javax.inject.Inject
class HexRepository @Inject constructor(private val r2DataSource: R2DataSource) {

    suspend fun getHexDump(offset: Long, length: Int): Result<ByteArray> {
        // pxj: Hex Dump
        val cmd = "pxj $length @ $offset"
        return r2DataSource.executeJson(cmd).mapCatching { output ->
             if (output.isBlank()) return@mapCatching ByteArray(0)
             val jsonArray = JSONArray(output)
             val bytes = ByteArray(jsonArray.length())
             for (i in 0 until jsonArray.length()) {
                 bytes[i] = jsonArray.getInt(i).toByte()
             }
             bytes
        }
    }

    suspend fun writeHex(addr: Long, hex: String): Result<String> {
        // wx [hex] @ [addr]
        return r2DataSource.execute("wx $hex @ $addr")
    }

    suspend fun writeString(addr: Long, text: String): Result<String> {
        // w [text] @ [addr]
        // Escape quotes
        val escaped = text.replace("\"", "\\\"")
        return r2DataSource.execute("w \"$escaped\" @ $addr")
    }
}
