package com.strmr.ai.data.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube signature decryptor based on SmartTube's approach
 * Handles encrypted signature deciphering for YouTube videos
 */
@Singleton
class YouTubeSignatureDecryptor @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    
    // Cache for decryption functions to avoid repeated downloads
    private val decryptionFunctionCache = mutableMapOf<String, String>()
    
    companion object {
        // Regex patterns for extracting decryption functions from YouTube player JS
        private val DECRYPTION_SIGNATURE_FUNCTION_REGEX = listOf(
            "([\\w$]+)\\s*=\\s*function\\(\\s*[a-zA-Z_$][\\w$]*\\s*\\)\\s*\\{[^\\}]*?\\breturn\\s+[a-zA-Z_$][\\w$]*\\.reverse\\(\\s*\\)".toRegex(),
            "([\\w$]+)\\s*=\\s*function\\(\\s*[a-zA-Z_$][\\w$]*\\s*\\)\\s*\\{[^\\}]*?\\breturn\\s+[a-zA-Z_$][\\w$]*\\.slice\\(\\s*\\d+\\s*\\)".toRegex(),
            "([\\w$]+)\\s*=\\s*function\\(\\s*[a-zA-Z_$][\\w$]*\\s*\\)\\s*\\{[^\\}]*?var\\s+[a-zA-Z_$][\\w$]*\\s*=\\s*[a-zA-Z_$][\\w$]*\\[0\\]".toRegex()
        )
        
        private val HELPER_OBJECT_REGEX = "var\\s+([a-zA-Z_$][\\w$]*)\\s*=\\s*\\{[^\\}]*?\\}\\s*;".toRegex()
        private val FUNCTION_CALL_REGEX = "([a-zA-Z_$][\\w$]*)\\.([a-zA-Z_$][\\w$]*)\\(([^\\)]*)\\)".toRegex()
    }
    
    /**
     * Decrypt a signature cipher to get the actual video URL
     */
    suspend fun decryptSignatureCipher(signatureCipher: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("SignatureDecryptor", "🔐 Decrypting signature cipher")
            
            // Parse the signature cipher
            val cipherParams = parseSignatureCipher(signatureCipher)
            val encryptedSignature = cipherParams["s"]
            val baseUrl = cipherParams["url"]
            val signatureParam = cipherParams["sp"] ?: "signature"
            
            if (encryptedSignature == null || baseUrl == null) {
                Log.w("SignatureDecryptor", "❌ Missing required cipher parameters")
                return@withContext null
            }
            
            // Get the decryption function
            val decryptionFunction = getDecryptionFunction() ?: return@withContext null
            
            // Decrypt the signature
            val decryptedSignature = executeDecryptionFunction(encryptedSignature, decryptionFunction)
            if (decryptedSignature == null) {
                Log.w("SignatureDecryptor", "❌ Failed to decrypt signature")
                return@withContext null
            }
            
            // Construct the final URL
            val decodedUrl = URLDecoder.decode(baseUrl, "UTF-8")
            val finalUrl = "$decodedUrl&$signatureParam=$decryptedSignature"
            
            Log.d("SignatureDecryptor", "✅ Successfully decrypted signature")
            finalUrl
        } catch (e: Exception) {
            Log.e("SignatureDecryptor", "❌ Error decrypting signature cipher", e)
            null
        }
    }
    
    private fun parseSignatureCipher(signatureCipher: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        try {
            val decodedCipher = URLDecoder.decode(signatureCipher, "UTF-8")
            val pairs = decodedCipher.split("&")
            
            for (pair in pairs) {
                val keyValue = pair.split("=", limit = 2)
                if (keyValue.size == 2) {
                    params[keyValue[0]] = keyValue[1]
                }
            }
        } catch (e: Exception) {
            Log.e("SignatureDecryptor", "❌ Error parsing signature cipher", e)
        }
        
        return params
    }
    
    private suspend fun getDecryptionFunction(): String? {
        return try {
            // First, get the player URL
            val playerUrl = getPlayerUrl() ?: return null
            
            // Check cache first
            decryptionFunctionCache[playerUrl]?.let { return it }
            
            // Download and parse the player JS
            val playerJs = downloadPlayerJs(playerUrl) ?: return null
            val decryptionFunction = extractDecryptionFunction(playerJs)
            
            // Cache the result
            if (decryptionFunction != null) {
                decryptionFunctionCache[playerUrl] = decryptionFunction
            }
            
            decryptionFunction
        } catch (e: Exception) {
            Log.e("SignatureDecryptor", "❌ Error getting decryption function", e)
            null
        }
    }
    
    private suspend fun getPlayerUrl(): String? {
        return try {
            // Get the main YouTube page to extract player URL
            val request = Request.Builder()
                .url("https://www.youtube.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            
            val html = response.body?.string() ?: return null
            
            // Extract player URL from the HTML
            val playerUrlRegex = "\"/s/player/([a-zA-Z0-9_-]+)/player_ias\\.vflset/[a-zA-Z0-9_-]+/base\\.js\"".toRegex()
            val match = playerUrlRegex.find(html)
            
            if (match != null) {
                val playerPath = match.value.replace("\"", "")
                "https://www.youtube.com$playerPath"
            } else {
                Log.w("SignatureDecryptor", "❌ Could not extract player URL")
                null
            }
        } catch (e: Exception) {
            Log.e("SignatureDecryptor", "❌ Error getting player URL", e)
            null
        }
    }
    
    private suspend fun downloadPlayerJs(playerUrl: String): String? {
        return try {
            val request = Request.Builder()
                .url(playerUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            
            response.body?.string()
        } catch (e: Exception) {
            Log.e("SignatureDecryptor", "❌ Error downloading player JS", e)
            null
        }
    }
    
    private fun extractDecryptionFunction(playerJs: String): String? {
        return try {
            // Find the main decryption function
            for (regex in DECRYPTION_SIGNATURE_FUNCTION_REGEX) {
                val match = regex.find(playerJs)
                if (match != null) {
                    val functionName = match.groupValues[1]
                    
                    // Extract the full function definition
                    val functionRegex = "($functionName=function\\([^)]*\\)\\{[^}]*\\})".toRegex()
                    val functionMatch = functionRegex.find(playerJs)
                    
                    if (functionMatch != null) {
                        val functionCode = functionMatch.groupValues[1]
                        
                        // Also extract helper object if referenced
                        val helperObjectName = extractHelperObjectName(functionCode)
                        val helperObject = if (helperObjectName != null) {
                            extractHelperObject(playerJs, helperObjectName)
                        } else null
                        
                        return if (helperObject != null) {
                            "$helperObject\n$functionCode"
                        } else {
                            functionCode
                        }
                    }
                }
            }
            
            Log.w("SignatureDecryptor", "❌ Could not extract decryption function")
            null
        } catch (e: Exception) {
            Log.e("SignatureDecryptor", "❌ Error extracting decryption function", e)
            null
        }
    }
    
    private fun extractHelperObjectName(functionCode: String): String? {
        val match = FUNCTION_CALL_REGEX.find(functionCode)
        return match?.groupValues?.get(1)
    }
    
    private fun extractHelperObject(playerJs: String, objectName: String): String? {
        val regex = "var\\s+$objectName\\s*=\\s*\\{[^\\}]*\\}\\s*;".toRegex()
        val match = regex.find(playerJs)
        return match?.value
    }
    
    /**
     * Execute the decryption function on the encrypted signature using JavaScript engine
     */
    private fun executeDecryptionFunction(encryptedSignature: String, decryptionFunction: String): String? {
        var context: Context? = null
        
        return try {
            Log.d("SignatureDecryptor", "🔄 Executing decryption function with JS engine")
            
            // Create Rhino context
            context = Context.enter()
            context.optimizationLevel = -1 // Disable optimization for Android
            
            // Create scope
            val scope = context.initStandardObjects()
            
            // Evaluate the decryption function and helper code
            context.evaluateString(scope, decryptionFunction, "decrypt.js", 1, null)
            
            // Find the main decryption function
            // Try different function name patterns
            val functionNames = listOf(
                decryptionFunction.substringAfter("var ").substringBefore("="),
                decryptionFunction.substringAfter("function ").substringBefore("("),
                "decrypt"
            ).filter { it.isNotBlank() }
            
            var decryptFunc: Function? = null
            var funcName: String? = null
            
            for (name in functionNames) {
                val obj = scope.get(name, scope)
                if (obj is Function) {
                    decryptFunc = obj
                    funcName = name
                    break
                }
            }
            
            if (decryptFunc == null) {
                // Try to find any function in the scope
                val ids = scope.ids
                for (id in ids) {
                    if (id is String) {
                        val obj = scope.get(id, scope)
                        if (obj is Function && id != "constructor") {
                            decryptFunc = obj
                            funcName = id
                            Log.d("SignatureDecryptor", "Found function: $id")
                            break
                        }
                    }
                }
            }
            
            if (decryptFunc == null) {
                Log.e("SignatureDecryptor", "❌ No decryption function found in scope")
                
                // Fallback to manual transformations
                return executeManualDecryption(encryptedSignature, decryptionFunction)
            }
            
            Log.d("SignatureDecryptor", "🎯 Using function: $funcName")
            
            // Call the function with encrypted signature
            val args = arrayOf(encryptedSignature)
            val result = decryptFunc.call(context, scope, scope, args)
            
            // Convert result to string
            val decrypted = Context.toString(result)
            
            Log.d("SignatureDecryptor", "✅ Decryption completed: ${encryptedSignature.take(10)}... -> ${decrypted.take(10)}...")
            
            decrypted
        } catch (e: Exception) {
            Log.e("SignatureDecryptor", "❌ Error executing decryption with JS engine", e)
            
            // Fallback to manual transformations
            executeManualDecryption(encryptedSignature, decryptionFunction)
        } finally {
            Context.exit()
        }
    }
    
    /**
     * Manual decryption fallback for when JS engine fails
     */
    private fun executeManualDecryption(encryptedSignature: String, decryptionFunction: String): String {
        Log.d("SignatureDecryptor", "📝 Attempting manual decryption")
        
        var signature = encryptedSignature
        
        // Common transformations found in YouTube decryption functions:
        
        // Pattern 1: Reverse the string
        if (decryptionFunction.contains("reverse()")) {
            signature = signature.reversed()
            Log.d("SignatureDecryptor", "Applied reverse transformation")
        }
        
        // Pattern 2: Slice from a position
        val sliceRegex = "\\.slice\\((\\d+)\\)".toRegex()
        val sliceMatch = sliceRegex.find(decryptionFunction)
        if (sliceMatch != null) {
            val slicePos = sliceMatch.groupValues[1].toIntOrNull() ?: 0
            if (slicePos < signature.length) {
                signature = signature.substring(slicePos)
                Log.d("SignatureDecryptor", "Applied slice transformation from position $slicePos")
            }
        }
        
        // Pattern 3: Swap characters
        val swapRegex = "\\[(\\d+)\\]\\s*=\\s*\\w+\\[(\\d+)\\]".toRegex()
        val swapMatch = swapRegex.find(decryptionFunction)
        if (swapMatch != null) {
            val pos1 = swapMatch.groupValues[1].toIntOrNull() ?: 0
            val pos2 = swapMatch.groupValues[2].toIntOrNull() ?: 0
            if (pos1 < signature.length && pos2 < signature.length) {
                val chars = signature.toCharArray()
                val temp = chars[pos1]
                chars[pos1] = chars[pos2]
                chars[pos2] = temp
                signature = String(chars)
                Log.d("SignatureDecryptor", "Applied swap transformation: positions $pos1 <-> $pos2")
            }
        }
        
        return signature
    }
}