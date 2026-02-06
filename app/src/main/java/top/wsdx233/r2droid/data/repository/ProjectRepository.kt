package top.wsdx233.r2droid.data.repository

import org.json.JSONArray
import org.json.JSONObject
import top.wsdx233.r2droid.data.model.*
import top.wsdx233.r2droid.util.R2PipeManager

/**
 * Repository to fetch analysis data from R2Pipe.
 */
class ProjectRepository {

    suspend fun getOverview(): Result<BinInfo> {
        // iIj: Binary Info
        return R2PipeManager.executeJson("iIj").mapCatching { output ->
            if (output.isBlank()) throw RuntimeException("Empty response from r2")
            val json = JSONObject(output)
            BinInfo.fromJson(json)
        }
    }

    suspend fun getSections(): Result<List<Section>> {
        // iSj: Sections
        return R2PipeManager.executeJson("iSj").mapCatching { output ->
            if (output.isBlank()) return@mapCatching emptyList()
            val jsonArray = JSONArray(output)
            val list = mutableListOf<Section>()
            for (i in 0 until jsonArray.length()) {
                list.add(Section.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun getSymbols(): Result<List<Symbol>> {
        // isj: Symbols
        return R2PipeManager.executeJson("isj").mapCatching { output ->
             if (output.isBlank()) return@mapCatching emptyList()
            val jsonArray = JSONArray(output)
            val list = mutableListOf<Symbol>()
            for (i in 0 until jsonArray.length()) {
                list.add(Symbol.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun getImports(): Result<List<ImportInfo>> {
        // iij: Imports (Standard r2 command)
        return R2PipeManager.executeJson("iij").mapCatching { output ->
             if (output.isBlank()) return@mapCatching emptyList()
            val jsonArray = JSONArray(output)
            val list = mutableListOf<ImportInfo>()
            for (i in 0 until jsonArray.length()) {
                list.add(ImportInfo.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun getRelocations(): Result<List<Relocation>> {
        // irj: Relocations
        return R2PipeManager.executeJson("irj").mapCatching { output ->
             if (output.isBlank()) return@mapCatching emptyList()
            val jsonArray = JSONArray(output)
            val list = mutableListOf<Relocation>()
            for (i in 0 until jsonArray.length()) {
                list.add(Relocation.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun getStrings(): Result<List<StringInfo>> {
        // izj: Strings
        return R2PipeManager.executeJson("izj").mapCatching { output ->
            if (output.isBlank()) return@mapCatching emptyList()
            val jsonArray = JSONArray(output)
            val list = mutableListOf<StringInfo>()
            for (i in 0 until jsonArray.length()) {
                list.add(StringInfo.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun getFunctions(): Result<List<FunctionInfo>> {
        // aflj: Function List
        return R2PipeManager.executeJson("aflj").mapCatching { output ->
            if (output.isBlank()) return@mapCatching emptyList()
            val jsonArray = JSONArray(output)
            val list = mutableListOf<FunctionInfo>()
            for (i in 0 until jsonArray.length()) {
                list.add(FunctionInfo.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        }
    }
}
