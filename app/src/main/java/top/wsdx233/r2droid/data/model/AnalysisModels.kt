package top.wsdx233.r2droid.data.model

import org.json.JSONObject

data class BinInfo(
    val arch: String,
    val bits: Int,
    val os: String,
    val type: String,
    val compiled: String,
    val language: String,
    val machine: String,
    val subSystem: String
) {
    companion object {
        fun fromJson(json: JSONObject): BinInfo {
            return BinInfo(
                arch = json.optString("arch", "Unknown"),
                bits = json.optInt("bits", 0),
                os = json.optString("os", "Unknown"),
                type = json.optString("class", "Unknown"), // "class": "PE32"
                compiled = json.optString("compiled", ""),
                language = json.optString("lang", "Unknown"),
                machine = json.optString("machine", "Unknown"),
                subSystem = json.optString("subsys", "Unknown")
            )
        }
    }
}

data class Section(
    val name: String,
    val size: Long,
    val vSize: Long,
    val perm: String,
    val vAddr: Long,
    val pAddr: Long
) {
    companion object {
        fun fromJson(json: JSONObject): Section {
            return Section(
                name = json.optString("name", ""),
                size = json.optLong("size", 0),
                vSize = json.optLong("vsize", 0),
                perm = json.optString("perm", ""),
                vAddr = json.optLong("vaddr", 0),
                pAddr = json.optLong("paddr", 0)
            )
        }
    }
}

data class Symbol(
    val name: String,
    val type: String,
    val vAddr: Long,
    val pAddr: Long,
    val isImported: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): Symbol {
            return Symbol(
                name = json.optString("name", ""),
                type = json.optString("type", ""),
                vAddr = json.optLong("vaddr", 0),
                pAddr = json.optLong("paddr", 0),
                isImported = json.optBoolean("is_imported", false)
            )
        }
    }
}

data class ImportInfo(
    val name: String,
    val ordinal: Int,
    val type: String,
    val plt: Long
) {
    companion object {
        fun fromJson(json: JSONObject): ImportInfo {
            // Adjust based on standard r2 iij output
            return ImportInfo(
                name = json.optString("name", ""),
                ordinal = json.optInt("ordinal", 0),
                type = json.optString("type", ""),
                plt = json.optLong("plt", 0)
            )
        }
    }
}

data class Relocation(
    val name: String,
    val type: String,
    val vAddr: Long,
    val pAddr: Long
) {
    companion object {
        fun fromJson(json: JSONObject): Relocation {
            return Relocation(
                name = json.optString("name", ""),
                type = json.optString("type", ""),
                vAddr = json.optLong("vaddr", 0),
                pAddr = json.optLong("paddr", 0)
            )
        }
    }
}

data class StringInfo(
    val string: String,
    val vAddr: Long,
    val section: String,
    val type: String
) {
    companion object {
        fun fromJson(json: JSONObject): StringInfo {
            return StringInfo(
                string = json.optString("string", ""),
                vAddr = json.optLong("vaddr", 0),
                section = json.optString("section", ""),
                type = json.optString("type", "")
            )
        }
    }
}

data class FunctionInfo(
    val name: String,
    val addr: Long,
    val size: Long,
    val nbbs: Int, // Number of basic blocks
    val signature: String
) {
    companion object {
        fun fromJson(json: JSONObject): FunctionInfo {
            return FunctionInfo(
                name = json.optString("name", ""),
                addr = json.optLong("addr", 0),
                size = json.optLong("size", 0),
                nbbs = json.optInt("nbbs", 0),
                signature = json.optString("signature", "")
            )
        }
    }
}
