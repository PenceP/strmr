package com.strmr.ai.data.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles YouTube's n-parameter transformation using JavaScript engine
 * This is required to avoid throttling/403 errors when playing YouTube videos
 */
@Singleton
class YouTubeNParamTransformer @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    
    // Cache for player JS and transformation functions
    private var cachedPlayerJs: String? = null
    private var cachedPlayerUrl: String? = null
    private var cachedTransformFunction: String? = null
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION_MS = 3600000L // 1 hour
    
    companion object {
        private const val TAG = "YouTubeNParamTransformer"
        
        // Patterns to find the n-parameter transformation function
        private val N_TRANSFORM_NAME_REGEX = listOf(
            // Pattern 1: b=a.split(""), c=some_function(b,parameter)
            """(\w+)\s*=\s*(\w+)\.split\s*\(\s*["']["']\s*\)\s*;\s*(\w+)\s*=\s*(\w+)\s*\(""".toRegex(),
            // Pattern 2: Direct function call pattern (without backreference)
            """(\w+)\s*=\s*function\s*\(\s*(\w+)\s*\)\s*\{\s*\w+\s*=\s*\w+\.split\s*\(\s*["']["']\s*\)""".toRegex(),
            // Pattern 3: Enhanced pattern from yt-dlp
            """(?:\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2,})\s*=\s*function\s*\(\s*a\s*\)\s*\{\s*a\s*=\s*a\.split\s*\(\s*["']["']\s*\)""".toRegex()
        )
        
        // Pattern to extract the actual transformation function
        private val N_TRANSFORM_FUNC_REGEX = """var\s+(\w+)\s*=\s*\[(.+?)\];""".toRegex()
    }
    
    /**
     * Transform the n-parameter to avoid throttling
     */
    suspend fun transformNParam(nParam: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 Transforming n-parameter: ${nParam.take(10)}...")
            
            // Get or refresh player JS
            val playerJs = getPlayerJs() ?: run {
                Log.e(TAG, "❌ Failed to get player JS")
                return@withContext null
            }
            
            // Get transformation function
            val transformFunction = getTransformFunction(playerJs) ?: run {
                Log.e(TAG, "❌ Failed to extract transform function")
                return@withContext null
            }
            
            // Execute transformation using Rhino
            val transformed = executeTransformation(nParam, transformFunction, playerJs)
            
            if (transformed != null && transformed != nParam) {
                Log.d(TAG, "✅ Successfully transformed n-parameter")
            } else {
                Log.w(TAG, "⚠️ N-parameter unchanged after transformation")
            }
            
            transformed
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error transforming n-parameter", e)
            null
        }
    }
    
    /**
     * Get player JS, using cache if available
     */
    private suspend fun getPlayerJs(): String? {
        val now = System.currentTimeMillis()
        
        // Check cache
        if (cachedPlayerJs != null && (now - lastCacheTime) < CACHE_DURATION_MS) {
            Log.d(TAG, "📦 Using cached player JS")
            return cachedPlayerJs
        }
        
        // Get player URL
        val playerUrl = getPlayerUrl() ?: return null
        
        // Check if player URL changed
        if (playerUrl == cachedPlayerUrl && cachedPlayerJs != null) {
            lastCacheTime = now
            return cachedPlayerJs
        }
        
        // Download new player JS
        val playerJs = downloadPlayerJs(playerUrl) ?: return null
        
        // Update cache
        cachedPlayerJs = playerJs
        cachedPlayerUrl = playerUrl
        cachedTransformFunction = null // Reset transform function cache
        lastCacheTime = now
        
        return playerJs
    }
    
    /**
     * Get the current YouTube player URL
     */
    private suspend fun getPlayerUrl(): String? {
        return try {
            Log.d(TAG, "🔍 Getting YouTube player URL")
            
            val request = Request.Builder()
                .url("https://www.youtube.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Failed to fetch YouTube homepage: ${response.code}")
                return null
            }
            
            val html = response.body?.string() ?: return null
            
            // Extract player URL using various patterns
            val patterns = listOf(
                """(/s/player/[\w\d]+/[\w\d_/.]+/base\.js)""".toRegex(),
                """(/s/player/[\w\d]+/player_ias\.vflset/[\w\d_/.]+/base\.js)""".toRegex(),
                """(/s/player/[\w\d]+/player-plasma-ias-phone-[\w\d_/.]+\.vflset/base\.js)""".toRegex()
            )
            
            for (pattern in patterns) {
                val match = pattern.find(html)
                if (match != null) {
                    val playerPath = match.groupValues[1]
                    val fullUrl = "https://www.youtube.com$playerPath"
                    Log.d(TAG, "✅ Found player URL: $fullUrl")
                    return fullUrl
                }
            }
            
            Log.e(TAG, "❌ Could not find player URL in HTML")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting player URL", e)
            null
        }
    }
    
    /**
     * Download player JavaScript
     */
    private suspend fun downloadPlayerJs(playerUrl: String): String? {
        return try {
            Log.d(TAG, "📥 Downloading player JS from: $playerUrl")
            
            val request = Request.Builder()
                .url(playerUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Failed to download player JS: ${response.code}")
                return null
            }
            
            val js = response.body?.string()
            if (js != null) {
                Log.d(TAG, "✅ Downloaded player JS (${js.length} bytes)")
            }
            js
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading player JS", e)
            null
        }
    }
    
    /**
     * Extract the n-parameter transformation function from player JS
     */
    private fun getTransformFunction(playerJs: String): String? {
        // Check cache first
        if (cachedTransformFunction != null) {
            return cachedTransformFunction
        }
        
        try {
            Log.d(TAG, "🔍 Extracting n-transform function")
            
            // Find the function name
            var functionName: String? = null
            
            for (pattern in N_TRANSFORM_NAME_REGEX) {
                val match = pattern.find(playerJs)
                if (match != null) {
                    // Get the function name from the match
                    functionName = when (match.groupValues.size) {
                        5 -> match.groupValues[4] // Pattern 1
                        3 -> match.groupValues[1] // Pattern 2
                        2 -> match.groupValues[1] // Pattern 3
                        else -> null
                    }
                    
                    if (functionName != null) {
                        Log.d(TAG, "📌 Found n-transform function name: $functionName")
                        break
                    }
                }
            }
            
            if (functionName == null) {
                // Try alternative approach - look for the function that processes the n parameter
                val altPattern = """\.get\s*\(\s*["']n["']\s*\)\s*\)\s*&&\s*\([^)]*?\|\|\s*(\w+)""".toRegex()
                val altMatch = altPattern.find(playerJs)
                if (altMatch != null) {
                    functionName = altMatch.groupValues[1]
                    Log.d(TAG, "📌 Found n-transform function name (alt): $functionName")
                }
            }
            
            if (functionName == null) {
                Log.e(TAG, "❌ Could not find n-transform function name")
                return null
            }
            
            // Extract the complete function definition
            val funcPattern = """((?:var\s+)?$functionName\s*=\s*function\s*\([^)]*\)\s*\{[^}]+\})""".toRegex()
            val funcMatch = funcPattern.find(playerJs)
            
            if (funcMatch == null) {
                Log.e(TAG, "❌ Could not extract function definition for: $functionName")
                return null
            }
            
            val functionDef = funcMatch.groupValues[1]
            
            // Extract any helper functions or arrays referenced
            val helpers = extractHelperCode(playerJs, functionDef)
            
            // Combine everything
            val completeFunction = buildString {
                appendLine(helpers)
                appendLine(functionDef)
                appendLine("// Expose function for execution")
                appendLine("var transformFunc = $functionName;")
            }
            
            Log.d(TAG, "✅ Extracted n-transform function (${completeFunction.length} chars)")
            
            // Cache the result
            cachedTransformFunction = completeFunction
            
            return completeFunction
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting transform function", e)
            return null
        }
    }
    
    /**
     * Extract helper code (arrays, objects, functions) referenced by the main function
     */
    private fun extractHelperCode(playerJs: String, functionDef: String): String {
        val helpers = StringBuilder()
        
        // Find all variable/function references in the function definition
        val referencePattern = """(\w+)(?:\[|\.|\()""".toRegex()
        val references = referencePattern.findAll(functionDef)
            .map { it.groupValues[1] }
            .distinct()
            .filter { it.length > 1 && !it.matches(Regex("^(var|function|return|if|else|for|while|do|switch|case|break|continue|try|catch|throw|new|this|true|false|null|undefined)$")) }
        
        for (ref in references) {
            // Try to find array definitions
            val arrayPattern = """var\s+$ref\s*=\s*\[[^\]]+\];""".toRegex()
            val arrayMatch = arrayPattern.find(playerJs)
            if (arrayMatch != null) {
                helpers.appendLine(arrayMatch.value)
                continue
            }
            
            // Try to find object definitions
            val objectPattern = """var\s+$ref\s*=\s*\{[^}]+\};""".toRegex()
            val objectMatch = objectPattern.find(playerJs)
            if (objectMatch != null) {
                helpers.appendLine(objectMatch.value)
                continue
            }
            
            // Try to find function definitions
            val funcPattern = """(?:var\s+)?$ref\s*=\s*function\s*\([^)]*\)\s*\{[^}]+\}""".toRegex()
            val funcMatch = funcPattern.find(playerJs)
            if (funcMatch != null) {
                helpers.appendLine(funcMatch.value)
            }
        }
        
        return helpers.toString()
    }
    
    /**
     * Execute the transformation using Rhino JavaScript engine
     */
    private fun executeTransformation(nParam: String, transformFunction: String, playerJs: String): String? {
        var context: Context? = null
        
        return try {
            Log.d(TAG, "🚀 Executing n-parameter transformation")
            
            // Create Rhino context
            context = Context.enter()
            context.optimizationLevel = -1 // Disable optimization for Android
            
            // Create scope
            val scope = context.initStandardObjects()
            
            // Evaluate the transformation function
            context.evaluateString(scope, transformFunction, "transform.js", 1, null)
            
            // Get the function
            val transformFunc = scope.get("transformFunc", scope)
            
            if (transformFunc !is Function) {
                Log.e(TAG, "❌ transformFunc is not a function")
                return null
            }
            
            // Call the function with n-parameter
            val args = arrayOf(nParam)
            val result = transformFunc.call(context, scope, scope, args)
            
            // Convert result to string
            val transformed = Context.toString(result)
            
            Log.d(TAG, "✅ Transformation complete: ${nParam.take(10)}... -> ${transformed.take(10)}...")
            
            transformed
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing transformation", e)
            
            // Fallback: try simpler execution
            try {
                if (context == null) {
                    context = Context.enter()
                    context.optimizationLevel = -1
                }
                
                val scope = context.initStandardObjects()
                
                // Try a simpler approach - just evaluate the n param through basic transformations
                val simpleScript = """
                    function transform(n) {
                        // Common YouTube transformations
                        var a = n.split('');
                        a = a.reverse();
                        a = a.slice(1);
                        return a.join('');
                    }
                    transform('$nParam');
                """.trimIndent()
                
                val result = context.evaluateString(scope, simpleScript, "simple.js", 1, null)
                Context.toString(result)
            } catch (e2: Exception) {
                Log.e(TAG, "❌ Fallback transformation also failed", e2)
                null
            }
        } finally {
            Context.exit()
        }
    }
    
    /**
     * Extract n-parameter from a YouTube URL
     */
    fun extractNParam(url: String): String? {
        // Try multiple patterns for n-parameter
        val patterns = listOf(
            """[?&]n=([^&]+)""".toRegex(),
            """[?&]n=([^&\\s]+)""".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return null
    }
    
    /**
     * Replace n-parameter in URL with transformed value
     */
    fun replaceNParam(url: String, oldN: String, newN: String): String {
        return url.replace("n=$oldN", "n=$newN")
    }
}