package org.ethereum.lists.tokens

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess
import org.kethereum.model.ChainId


val networkMapping = mapOf("etc" to 61, "eth" to 1, "kov" to 42, "rin" to 4, "rop" to 3, "rsk" to 30, "ella" to 64, "esn" to 2, "gor" to 5, "avax" to 43114)

suspend fun main() {
    checkForTokenDefinitionsInWrongPath()

    allNetworksTokenDir.listFiles()?.forEach { singleNetworkTokenDirectory ->
        val jsonArray = JsonArray<JsonObject>()
        singleNetworkTokenDirectory.listFiles()?.forEach {
            try {
                it.reader().use { reader ->
                    jsonArray.add(Klaxon().parseJsonObject(reader))
                }

                checkTokenFile(it, true, getChainId(networkMapping, singleNetworkTokenDirectory.name))
            } catch (e: Exception) {
                println("Problem with $it: $e")

                exitProcess(1)
            }
        }

        jsonArray.writeJSON("full", singleNetworkTokenDirectory.name)
        val minified = jsonArray.copyFields(mandatoryFields)
        minified.writeJSON("minified", singleNetworkTokenDirectory.name)
        networkMapping[singleNetworkTokenDirectory.name]?.let {
            minified.writeJSON("minifiedByNetworkId", it.toString())
        }
    }
}

private fun getChainId(networkMapping: Map<String, Int>, networkName: String) = networkMapping[networkName]?.let {
    ChainId(it.toBigInteger())
}

private fun checkForTokenDefinitionsInWrongPath() {
    File(".").walk().forEach { path ->
        if (path.isDirectory
                && !Files.isSameFile((path.parentFile ?: path).toPath(), allNetworksTokenDir.toPath())
                && !path.absolutePath.contains("/test_tokens/")) {
            path.list()?.firstOrNull { it.startsWith("0x") }?.let {
                throw IllegalArgumentException("There is a token definition file ($it) placed in a directory where it does not belong (${path.absolutePath})")
            }
        }
    }
}

fun JsonArray<*>.writeJSON(pathName: String, filename: String) {
    val fullOutDir = File(outDir, pathName)
    fullOutDir.mkdirs()
    val fullOutFile = File(fullOutDir, "$filename.json")

    fullOutFile.writeText(toJsonString(false))
}

fun List<JsonObject>.copyFields(fields: List<String>): JsonArray<JsonObject> {
    val minimalJSONArray = JsonArray<JsonObject>()
    forEach { jsonObject ->
        val minimalJsonObject = JsonObject()
        fields.forEach {
            minimalJsonObject[it] = jsonObject[it]
        }
        minimalJSONArray.add(minimalJsonObject)
    }
    return minimalJSONArray
}