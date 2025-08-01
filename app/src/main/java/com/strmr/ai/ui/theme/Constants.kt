package com.strmr.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized constants for the STRMR app
 * Consolidates repeated values for consistency and maintainability
 */
object StrmrConstants {
    object Colors {
        // Primary Colors
        val PRIMARY_BLUE = Color(0xFF007AFF)
        val SURFACE_DARK = Color(0xFF1a1a1a)
        val BACKGROUND_DARK = Color(0xFF0f0f0f)
        val BACKGROUND_DARKER = Color(0xFF0a0a0a)
        val CONTAINER_DARK = Color(0xFF222222)
        val BORDER_DARK = Color(0xFF333333)

        // Text Colors
        val TEXT_PRIMARY = Color.White
        val TEXT_SECONDARY = Color(0xFF888888)
        val TEXT_TERTIARY = Color(0xFF666666)

        // Status Colors
        val ERROR_RED = Color(0xFFFF3B30)
        val SUCCESS_GREEN = Color(0xFF4CAF50)
        val SUCCESS_GREEN_DARK = Color(0xFF2E7D32)
        val WARNING_ORANGE = Color(0xFFFF9800)

        // Quality Indicators (consolidated similar values)
        object Quality {
            val GOLD_4K = Color(0xFFFFD700) // Was also 0xFFEFC700
            val BLUE_1080P = Color(0xFF2196F3)
            val RED_720P = Color(0xFFE53E3E)
            val GRAY_UNKNOWN = Color(0xFF9E9E9E)
        }

        // Alpha Values (consolidated close values)
        object Alpha {
            const val SUBTLE = 0.1f // Was 0.1f, 0.15f → 0.1f
            const val LIGHT = 0.3f // Was 0.3f
            const val MEDIUM = 0.6f // Was 0.6f, 0.7f → 0.6f
            const val HEAVY = 0.8f // Was 0.8f, 0.87f → 0.8f
            const val NEAR_OPAQUE = 0.9f // Was 0.9f
            const val FOCUS = 0.95f // Was 0.95f
        }
    }

    object Dimensions {
        // Spacing System (consolidated close values)
        val SPACING_TINY = 4.dp // Was 4.dp
        val SPACING_SMALL = 8.dp // Was 8.dp
        val SPACING_MEDIUM = 12.dp // Was 12.dp, 10.dp → 12.dp
        val SPACING_STANDARD = 16.dp // Was 16.dp, 18.dp → 16.dp
        val SPACING_LARGE = 20.dp // Was 20.dp, 22.dp → 20.dp
        val SPACING_EXTRA_LARGE = 24.dp // Was 24.dp
        val SPACING_SECTION = 32.dp // Was 32.dp

        // Icon Sizes (consolidated)
        object Icons {
            val TINY = 14.dp // Was 14.dp, 16.dp → 14.dp
            val SMALL = 16.dp // Was 18.dp → 16.dp
            val STANDARD = 24.dp // Was 24.dp
            val LARGE = 32.dp // Was 32.dp
            val EXTRA_LARGE = 48.dp // Was 48.dp
            val HUGE = 72.dp // Was 72.dp
        }

        // Component Dimensions
        object Components {
            val BUTTON_HEIGHT = 48.dp
            val NAV_BAR_WIDTH = 56.dp
            val SETTINGS_PANEL_WIDTH = 320.dp
            val LOGO_HEIGHT = 75.dp // Was 75.dp, 100.dp → 75.dp
            val HEADER_HEIGHT = 120.dp // Was 120.dp
            val CONTENT_ROW_WIDTH = 900.dp
            val BORDER_WIDTH = 1.dp // Was 1.dp, 2.dp → 1.dp
        }

        // Card Dimensions (consolidated)
        object Cards {
            val BASE_WIDTH = 120.dp // Was 120.dp
            val BASE_HEIGHT = 180.dp // Was 180.dp
            val MEDIUM_WIDTH = 130.dp // Was 130.dp, 135.dp → 130.dp
            val MEDIUM_HEIGHT = 200.dp // Was 200.dp
            val LARGE_WIDTH = 160.dp // Was 160.dp
            val LARGE_HEIGHT = 240.dp // Was 240.dp, 220.dp → 240.dp
            val LANDSCAPE_WIDTH = 160.dp // Was 160.dp, 176.dp → 160.dp
            val LANDSCAPE_HEIGHT = 90.dp // Was 90.dp
        }

        // Elevation
        object Elevation {
            val NONE = 0.dp
            val SMALL = 2.dp // Was 2.dp
            val STANDARD = 4.dp // Was 4.dp
            val LARGE = 8.dp // Was 8.dp
        }
    }

    object Typography {
        // Font Sizes (already in Type.kt but for reference)
        val TEXT_SIZE_CAPTION = 10.sp
        val TEXT_SIZE_SMALL = 12.sp
        val TEXT_SIZE_BODY = 14.sp
        val TEXT_SIZE_MEDIUM = 16.sp
        val TEXT_SIZE_LARGE = 18.sp
        val TEXT_SIZE_TITLE = 20.sp
    }

    object Animation {
        // Durations (milliseconds)
        const val DURATION_INSTANT = 10
        const val DURATION_QUICK = 200
        const val DURATION_STANDARD = 300
        const val DURATION_LONG = 1000 // Was 1000, 1200 → 1000
        const val DURATION_SHIMMER = 1200

        // Scale Factors
        const val SCALE_SMALL = 1.1f
        const val SCALE_MEDIUM = 1.2f
        const val SCALE_LARGE = 1.8f

        // Delays
        const val DELAY_SHORT = 50
        const val DELAY_MEDIUM = 100
        const val DELAY_LONG = 300
    }

    object Shapes {
        val CORNER_RADIUS_SMALL = RoundedCornerShape(6.dp)
        val CORNER_RADIUS_STANDARD = RoundedCornerShape(8.dp)
        val CORNER_RADIUS_MEDIUM = RoundedCornerShape(12.dp)
        val CORNER_RADIUS_LARGE = RoundedCornerShape(16.dp)

        // Special shapes
        val SETTINGS_PANEL_SHAPE = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    }

    object Api {
        // TMDB
        const val TMDB_IMAGE_BASE_ORIGINAL = "https://image.tmdb.org/t/p/original"
        const val TMDB_IMAGE_BASE_W780 = "https://image.tmdb.org/t/p/w780"
        const val TMDB_IMAGE_BASE_W500 = "https://image.tmdb.org/t/p/w500"
        const val TMDB_IMAGE_BASE_W300 = "https://image.tmdb.org/t/p/w300"
        const val TMDB_IMAGE_BASE_W185 = "https://image.tmdb.org/t/p/w185"

        // YouTube
        const val YOUTUBE_BASE_URL = "https://www.youtube.com"
        const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v="
        const val YOUTUBE_IFRAME_API = "https://www.youtube.com/iframe_api"
        const val YOUTUBE_THUMBNAIL_URL = "https://img.youtube.com/vi/%s/maxresdefault.jpg"

        // Premiumize
        const val PREMIUMIZE_BASE_URL = "https://premiumize.me"
        const val PREMIUMIZE_DEVICE_URL = "https://premiumize.me/device"
        const val PREMIUMIZE_WEB_URL = "https://www.premiumize.me/"

        // Pagination
        const val DEFAULT_PAGE_SIZE = 20
        const val LARGE_PAGE_SIZE = 50
        const val SEARCH_LIMIT = 10

        // HTTP Headers
        const val HEADER_ACCEPT = "*/*"
        const val HEADER_CONNECTION = "keep-alive"
        const val HEADER_ORIGIN_YOUTUBE = "https://www.youtube.com"
        const val HEADER_REFERER_YOUTUBE = "https://www.youtube.com/"
    }

    object Preferences {
        // Encrypted preferences file name
        const val PREFS_NAME = "scraper_encrypted_prefs"

        // Keys
        const val KEY_PREMIUMIZE_API_KEY = "premiumize_api_key"
        const val KEY_PREFERRED_SCRAPER = "preferred_scraper"
        const val KEY_QUALITY_PREFERENCE = "quality_preference"
        const val KEY_TRAKT_ACCESS_TOKEN = "trakt_access_token"
        const val KEY_TRAKT_REFRESH_TOKEN = "trakt_refresh_token"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_VERSION = "version"

        // Account types
        const val ACCOUNT_TYPE_TRAKT = "trakt"
    }

    object Defaults {
        const val PREFERRED_SCRAPER = "torrentio"
        const val QUALITY_PREFERENCE = "1080p"
        const val CONTENT_ALIGNMENT_MIDDLE = "Middle"
        const val CONTENT_ALIGNMENT_LEFT = "Left"
        const val CONTENT_ALIGNMENT_MIDDLE_DESC = "Center content on screen"
        const val CONTENT_ALIGNMENT_LEFT_DESC = "Align content to the left"
    }

    object Blur {
        val RADIUS_STANDARD = 6.dp // Reduced blur for wallpaper and backdrop images
    }

    object Layout {
        // Progress thresholds
        const val PROGRESS_THRESHOLD = 95f
        const val PERCENTAGE_DIVISOR = 100f

        // Layout weights
        const val WEIGHT_HALF_MINUS = 0.49f
        const val WEIGHT_HALF_PLUS = 0.51f
        const val DIALOG_SIZE_FRACTION = 0.8f

        // Animation targets
        const val GRADIENT_END_POSITION_1 = 1200f
        const val GRADIENT_END_POSITION_2 = 2200f
        const val ANIMATION_TARGET_VALUE = 1000f
    }

    object Time {
        const val TRAKT_WARNING_THRESHOLD_SECONDS = 30
        const val BACKGROUND_SYNC_INTERVAL_DAYS = 1
    }

    object Paging {
        // Page sizes
        const val PAGE_SIZE_SMALL = 20
        const val PAGE_SIZE_STANDARD = 50 // Used for TV apps

        // Prefetch distances
        const val PREFETCH_DISTANCE_SMALL = 3
        const val PREFETCH_DISTANCE_STANDARD = 10

        // Cache limits
        const val MAX_CACHE_SIZE = 200

        // Proactive loading thresholds
        const val LOAD_AHEAD_THRESHOLD = 6 // Load when currentIdx + 6 >= numLoadedItems
        const val TRIGGER_OFFSET = 3 // Trigger at currentIdx + 3
    }

    object UI {
        // List item limits
        const val MAX_CAST_ITEMS = 15
        const val MAX_SIMILAR_ITEMS = 10
        const val MAX_COLLECTION_PARTS = 50

        // Animation durations
        const val ANIMATION_DURATION_SHORT = 200
        const val ANIMATION_DURATION_STANDARD = 300

        // Loading thresholds
        const val LAZY_LOADING_THRESHOLD = 5
        const val BUFFER_SIZE = 3
    }
}
