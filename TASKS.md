# TASKS.md

## Android TV App: Netflix-Style UI with Trakt & TMDB

### 1. Project Setup
- [x] Create a new Android TV project in Android Studio
- [x] Set up necessary dependencies (Leanback, Retrofit, Glide/Picasso, etc.)
- [x] Add Trakt and TMDB API keys to project securely
- [x] Create Retrofit service and data models for Trakt trending movies
- [x] Create ViewModel to fetch and store trending movies
- [x] Fetch TMDB poster/backdrop images using TMDB API and movie TMDB IDs

### 2. UI Design
- [x] Design main screen layout:
    - [x] Thin, sleek left navigation bar (search, movies, TV icons)
    - [x] Main content area with a single row: "Trending Movies"
    - [x] Poster carousel (horizontal scrolling)
    - [x] Top-left area for selected movie details (title, description, rating)
    - [x] Backdrop image for selected movie
- [x] Create drawable directory and add vector assets/icons for navigation bar (search, film reel, TV)
- [x] Simplify navigation system using Navigation Component (replaced complex manual focus management)

### 3. API Integration
- [x] Integrate Trakt API to fetch trending movies
- [x] Integrate TMDB API to fetch poster and backdrop images
- [x] Map Trakt movie data to TMDB images

### 4. Main Screen Implementation
- [x] Implement left navigation bar UI and logic
- [x] Implement horizontal row for "Trending Movies"
- [x] Display movie posters in row (with focus/selection effect)
- [x] On selection, show movie details and backdrop in top-left

### 5. Polish & UX
- [x] Add smooth focus animations and transitions
- [x] Schedule and cache into db lists (ex Trending) updates, use previous list order in db for load speed
- [x] Ensure TV remote navigation works seamlessly
- [x] Backdrop image should go to top and right of screen (remove padding)
- [ ] Handle loading and error states
- [ ] Responsive layout for different TV sizes

### 6. Testing
- [x] Test on Android TV emulator
- [x] Test on real Android TV device

### 7. Additional Features
- [x] Add more rows (e.g., Popular, Top Rated)
- [x] Implement search functionality
- [x] Add TV shows section
- [x] User authentication with Trakt
- [x] Display ratings from IMDb, Rotten Tomatoes, and Trakt in the details area (alongside or instead of current ratings)
- [x] Display genre info in the details area

### 8. MediaDetails Page Implementation
- [x] Create dedicated details page for movies and TV shows (shown when poster is clicked)
- [x] Large backdrop image with title, logo, year, runtime, genres, rating, summary/overview
- [x] Trakt actions: add/remove from watchlist, add/remove from collections (show correct state if already added)
- [x] Actor list (cast)
- [x] Similar movies/TV shows (horizontal row, context-aware)
- [x] TV show specific features:
    - [x] Season selector (dropdown or horizontal row)
    - [x] Scrollable list of episode images for the selected season
    - [x] Episode details (title, air date, overview, etc.)
- [x] Ratings integration:
    - [x] Integrate OMDb API to fetch IMDb, Rotten Tomatoes, and Metacritic ratings
    - [x] Create Retrofit interface for OMDb (apikey=a8787305, for local/dev use only, do not commit)
    - [x] Add data models for OMDb responses (ratings, votes, etc.)
    - [x] Fetch ratings by IMDb ID (or title/year fallback)
    - [x] Display ratings row with IMDb, Trakt, Rotten Tomatoes, Metacritic (show/hide as available)
    - [x] Cache/store ratings to avoid excessive API calls
- [x] Integrate with navigation so clicking a poster opens this page
- [x] Ensure smooth focus/TV navigation and visual polish

### 9. Row Configuration Refactor
- [x] Move all row configuration (order, type, data source) out of HomePage.kt and into JSON files
- [x] Create MOVIES.json and TV.json for movies and TV-specific row configs
- [x] Ensure all rows (Continue Watching, Networks, Collections, etc.) are defined in JSON, not hardcoded
- [x] Include all hardcoded elements like HomeMediaItem.Collection into JSON
- [x] Support nested fields (like Networks - a row of items that each has its own logo and Trakt data source)
- [x] Refactor code to dynamically build rows from JSON at compile time

### 10. Bug Fixes (Post-JSON Refactor)
- [x] Fix up/down arrows not showing on movies page
- [x] Fix media logos not working intermittently

## Current Tasks

### 11. Video Player Integration
- [ ] Add ExoPlayer [https://github.com/androidx/media]
- [ ] Add ffmpeg support, that's what handles various encode types
- [ ] Integrate TMDB API for trailers [api.themoviedb.org/3/movie/120/videos?language=en-US]
- [ ] Implement trailers button functionality from MediaDetails page
- [ ] Use videos marked as "official" and "Trailer" type
- [ ] Create video player overlay with proper TV navigation controls

### 12. Intermediate View for Networks & Collections
- [ ] Create intermediate page for when user clicks a network or collection
- [ ] Hero/details section at the top (similar to MediaPage layout)
- [ ] Single long row of posters for all movies/shows in that network/collection
- [ ] Reuse MediaPage/MediaHero logic where possible
- [ ] Ensure proper navigation back to previous page and forward to MediaDetails

### 13. Season/Episode View Enhancement
- [ ] Create modern episode view with:
    - [ ] Air date display
    - [ ] Episode summary/overview
    - [ ] Episode screenshot as landscape image
    - [ ] Episode runtime and rating info
    - [ ] Watch status integration with Trakt
    - [ ] Proper TV navigation between episodes

### 14. Pagination Fix
- [x] Investigate and fix broken paging system
- [x] Ensure movies and TV shows load beyond first page from API
- [x] Implement proper infinite scrolling for all content rows
- [x] Add loading indicators for pagination

### 15. Auto-Update System
- [ ] Implement app versioning system
- [ ] Create server endpoint to check for updates
- [ ] Add update checking on app startup
- [ ] Implement automatic download and installation of updates
- [ ] Add user notification system for available updates
- [ ] **Note: Should be implemented after CI/CD system (Task 16)**

### 16. CI/CD Pipeline with GitHub Actions
- [x] Set up build stage with proper Android SDK configuration
- [x] Implement automated testing stage
- [x] Configure APK compilation with version number in filename
- [x] Add artifacts upload for release builds
- [ ] **Optional:** Set up emulator testing with basic smoke tests
- [ ] Configure release deployment workflow
- [ ] **Must be completed before auto-update functionality**

### 17. Watched Indicators
- [ ] Watched Flag on poster if media is fully watched
- [ ] Watched Flag with % watched if partial
- [ ] For TV: If tv has partial watch, show the number of episodes that remain unwatched

### 18. Add long-press functionality to posters/media items
- [ ] Add to/remove from Watchlist/Collection (depending on current status)
- [ ] Mark Watched/unwatched (this should work on posters, season buttons, episodes)
- [ ] Possibly any more I missed


### 19. Performance Optimization Round
#### Phase 1: Systematic Performance Analysis
- [x] **Profiling Setup**
    - [x] Set up Android Studio CPU Profiler for performance monitoring
    - [ ] Install Systrace/Perfetto for system-level analysis
    - [ ] Configure method tracing for critical user flows (app launch, page navigation, poster loading)
    - [ ] Establish baseline performance metrics (startup time, frame drops, memory usage)

- [x] **File-by-File Performance Audit**
    - [x] **Activity/Fragment Analysis**
        - [x] Review `MainActivity.kt` for heavyweight operations on main thread
        - [x] Analyze `HomePage.kt` for inefficient RecyclerView operations and excessive API calls
        - [x] Examine `MediaDetailsActivity.kt` for blocking image loads and redundant data fetching
        - [x] Check navigation fragments for memory leaks and retained instances
    
    - [x] **ViewModel Performance Review**
        - [x] Audit all ViewModels for synchronous database operations
        - [x] Identify ViewModels performing heavy computations on main thread
        - [x] Review LiveData/StateFlow usage for unnecessary emissions
        - [x] Check for improper coroutine usage causing thread blocking
    
    - [ ] **Database Layer Optimization**
        - [ ] Profile Room database queries using Database Inspector
        - [ ] Identify missing indices on frequently queried columns (movie IDs, timestamps)
        - [ ] Review DAO methods for N+1 query problems
        - [ ] Analyze cache invalidation strategies and optimization opportunities
    
    - [x] **Network Layer Performance**
        - [x] Review Retrofit service implementations for synchronous calls
        - [x] Audit API response handling for unnecessary object creation
        - [x] Check network request batching opportunities (multiple poster requests)
        - [x] Identify redundant API calls across screens
    
    - [ ] **Image Loading Optimization**
        - [ ] Profile Glide/Picasso usage patterns and memory consumption
        - [ ] Review image caching strategies and cache hit rates
        - [ ] Identify oversized image downloads (posters, backdrops)
        - [ ] Check for memory leaks in image loading callbacks

#### Phase 2: Targeted Speed Improvements
- [x] **Main Thread Optimization**
    - [x] Move all JSON parsing operations to background threads
    - [x] Offload database operations from main thread using coroutines
    - [x] Implement async image processing for poster transformations
    - [x] Remove any blocking file I/O operations from UI thread
    
- [x] **RecyclerView Performance Tuning**
    - [x] Implement ViewHolder recycling optimizations
    - [x] Add item prefetching for smooth scrolling (`setItemPrefetchEnabled(true)`)
    - [x] Optimize adapter diffing using DiffUtil for large datasets
    - [x] Implement view binding caching to reduce findViewById calls
    
- [ ] **Memory Management**
    - [ ] Implement object pooling for frequently created objects (movie items, view holders)
    - [ ] Add proper cleanup in Fragment/Activity onDestroy methods
    - [ ] Review bitmap management and implement proper recycling
    - [ ] Optimize string concatenation using StringBuilder for loops
    
- [ ] **Startup Time Optimization**
    - [ ] Implement lazy initialization for non-critical components
    - [ ] Move heavy SDK initialization to background threads
    - [ ] Add content providers for faster initial data loading
    - [ ] Optimize Application class onCreate method

#### Phase 3: Advanced Optimizations
- [ ] **Code-Level Micro-optimizations**
    - [ ] Replace Collections.forEach with enhanced for-loops in hot paths
    - [ ] Use SparseArray instead of HashMap for integer keys
    - [ ] Implement object reuse patterns for frequently allocated objects
    - [ ] Add @JvmStatic annotations to frequently called Kotlin functions
    
- [ ] **Rendering Performance**
    - [ ] Profile overdraw using GPU rendering tools
    - [ ] Optimize layout hierarchies to reduce view depth
    - [ ] Implement view flattening where possible
    - [ ] Add hardware acceleration flags for custom views
    
- [ ] **Background Processing Optimization**
    - [ ] Implement WorkManager for non-time-critical background tasks
    - [ ] Add intelligent prefetching based on user navigation patterns
    - [ ] Optimize coroutine dispatchers for different operation types
    - [ ] Implement request deduplication for identical API calls

#### Phase 4: Performance Testing & Validation
- [ ] **Performance Metrics Collection**
    - [ ] Measure and document improvement in app startup time
    - [ ] Track frame rate improvements during scrolling and navigation
    - [ ] Monitor memory usage reduction across all major screens
    - [ ] Benchmark API response handling speed improvements
    
- [ ] **Regression Testing**
    - [ ] Create automated performance test suite
    - [ ] Set up continuous performance monitoring
    - [ ] Establish performance budgets for critical user journeys
    - [ ] Document performance optimization guidelines for future development

**Priority:** High - Performance directly impacts user experience on Android TV devices with limited resources. Focus on startup time and navigation smoothness first, then drilling down into specific bottlenecks identified through profiling.

### 20. Torrent Scraper Integration
**Reference Links:**
- Torrentio Scraper: https://github.com/TheBeastLT/torrentio-scraper
- Torrentio Configuration: https://torrentio.strem.fun/
- Comet Scraper: https://github.com/g0ldyy/comet  
- Comet Hosted Instance: comet.elfhosted.com

**Sample Configuration JSONs:**
```json
// Torrentio Configuration
{
  "id": "com.stremio.torrentio.addon",
  "version": "0.0.15",
  "name": "Torrentio PM",
  "description": "Provides torrent streams from scraped torrent providers. Currently supports YTS(+), EZTV(+), RARBG(+), 1337x(+), ThePirateBay(+), KickassTorrents(+), TorrentGalaxy(+), MagnetDL(+), HorribleSubs(+), NyaaSi(+), TokyoTosho(+), AniDex(+), Rutor(+), Rutracker(+), Comando(+), BluDV(+), Torrent9(+), ilCorSaRoNeRo(+), MejorTorrent(+), Wolfmax4k(+), Cinecalidad(+), BestTorrents(+) and Premiumize enabled. To configure providers, RealDebrid/Premiumize/AllDebrid/DebridLink/EasyDebrid/Offcloud/TorBox/Put.io support and other settings visit https://torrentio.strem.fun",
  "catalogs": [
    {
      "id": "torrentio-premiumize",
      "name": "Premiumize",
      "type": "other",
      "extra": [{"name": "skip"}]
    }
  ],
  "resources": [
    {
      "name": "stream",
      "types": ["movie", "series", "anime"],
      "idPrefixes": ["tt", "kitsu"]
    },
    {
      "name": "meta",
      "types": ["other"],
      "idPrefixes": ["premiumize"]
    }
  ],
  "types": ["movie", "series", "anime", "other"],
  "background": "https://torrentio.strem.fun/images/background_v1.jpg",
  "logo": "https://torrentio.strem.fun/images/logo_v1.png",
  "behaviorHints": {
    "configurable": true,
    "configurationRequired": false
  }
}

// Comet Configuration  
{
  "id": "comet.elfhosted.com.VZwT",
  "description": "Stremio's fastest torrent/debrid search add-on.",
  "version": "2.0.0",
  "catalogs": [],
  "resources": [
    {
      "name": "stream",
      "types": ["movie", "series"],
      "idPrefixes": ["tt", "kitsu"]
    }
  ],
  "types": ["movie", "series", "anime", "other"],
  "logo": "https://i.imgur.com/jmVoVMu.jpeg",
  "background": "https://i.imgur.com/WwnXB3k.jpeg",
  "behaviorHints": {
    "configurable": true,
    "configurationRequired": false
  },
  "name": "Comet | ElfHosted | PM"
}
```

#### Phase 1: Research & Architecture Planning
- [ ] **Scraper Service Investigation**
    - [ ] Research Torrentio scraper API endpoints and request/response format
    - [ ] Investigate Comet scraper API at comet.elfhosted.com for comparison
    - [ ] Document API differences, reliability, and performance characteristics
    - [ ] Test both scrapers manually with sample IMDb IDs to understand data structure
    - [ ] Analyze which scraper provides better quality/speed for our use case

- [ ] **Authentication & Configuration Analysis**
    - [ ] Reverse engineer Torrentio configuration URL structure at https://torrentio.strem.fun/
    - [ ] Understand how user configurations (providers, debrid services) are encoded
    - [ ] Map Premiumize API key integration into configuration payload
    - [ ] Research Comet configuration requirements and API key handling
    - [ ] Plan secure storage of user debrid service credentials

#### Phase 2: Debrid Service Integration
- [ ] **Premiumize Integration**
    - [ ] Implement OAuth flow for Premiumize authentication
    - [ ] Create Retrofit service for Premiumize API endpoints
    - [ ] Add secure storage for Premiumize API keys using EncryptedSharedPreferences (or in accounts db with trakt api)
    - [ ] Implement API key validation and refresh mechanisms
    - [x] Add user settings screen for debrid service configuration

- [ ] **Alternative Debrid Services Support**
    - [ ] Research and document Real-Debrid API integration
    - [ ] Plan AllDebrid, DebridLink, TorBox integration for future expansion
    - [ ] Create abstracted debrid service interface for multiple providers
    - [ ] Implement debrid service selection in user settings

#### Phase 3: Scraper Service Implementation
- [ ] **Primary Scraper Integration (Torrentio vs Comet Decision)**
    - [ ] Implement chosen scraper's API client using Retrofit
    - [ ] Create data models for scraper responses (stream links, quality, size, etc.)
    - [ ] Map IMDb IDs from TMDB/Trakt data to scraper requests
    - [ ] Handle scraper rate limiting and error responses gracefully
    - [ ] Implement caching strategy for scraper results to avoid redundant requests

- [ ] **Stream Resolution & Quality Management**
    - [ ] Parse scraper responses for available stream qualities (4K, 1080p, 720p, etc.)
    - [ ] Implement quality filtering based on user preferences
    - [ ] Sort streams by quality, file size, and seeders/peers count
    - [ ] Add user preference system for preferred quality and file size limits
    - [ ] Handle multi-part files and season packs for TV shows

#### Phase 4: UI Integration for Stream Selection
- [ ] **MediaDetails Page Stream Integration**
    - [ ] Link "Play" button in MediaDetails page
    - [ ] Create stream selection page showing available options
    - [ ] Display stream information: quality, file size, provider, seeds/peers
    - [ ] Implement stream quality badges and provider icons
    - [ ] Add fallback messaging when no streams are available

- [ ] **Episode-Level Stream Handling**
    - [ ] Integrate scraper calls for individual TV episodes
    - [ ] Handle season pack detection and episode extraction
    - [ ] Map episode numbers to correct files in multi-episode torrents
    - [ ] Add "Watch Episode" functionality to episode list items
    - [ ] Implement next episode auto-progression with stream continuity

#### Phase 5: Advanced Stream Features
- [ ] **Stream Processing & Preparation**
    - [ ] Implement magnet link handling and torrent file processing
    - [ ] Add support for direct debrid service streaming URLs
    - [ ] Create stream health checking (verify links before playback)
    - [ ] Implement subtitle detection and integration from stream metadata
    - [ ] Add torrent progress monitoring for real-time downloads

- [ ] **Smart Stream Selection**
    - [ ] Implement automatic quality selection based on device capabilities
    - [ ] Add bandwidth-aware stream selection for slower connections
    - [ ] Create user viewing history to improve automatic selections
    - [ ] Implement fallback stream selection when primary choice fails
    - [ ] Add manual stream switching during playback

#### Phase 6: Performance & Reliability
- [ ] **Caching & Optimization**
    - [ ] Implement intelligent stream result caching with TTL
    - [ ] Add background prefetching for likely-to-be-watched content
    - [ ] Create stream availability checking without full scraper calls
    - [ ] Optimize scraper request batching for multi-episode content
    - [ ] Add offline capability for previously cached stream data

- [ ] **Error Handling & Fallbacks**
    - [ ] Implement graceful degradation when scrapers are unavailable
    - [ ] Add automatic failover between Torrentio and Comet
    - [ ] Create comprehensive error messaging for failed stream attempts
    - [ ] Implement retry logic with exponential backoff for failed requests
    - [ ] Add network status monitoring and offline mode handling

#### Phase 7: User Experience & Settings
- [ ] **Stream Preferences Configuration**
    - [ ] Create comprehensive settings page for stream preferences
    - [ ] Add provider priority configuration (YTS, EZTV, 1337x, etc.)
    - [ ] Implement quality preference sliders and bandwidth settings
    - [ ] Filter stream by type (Cam, Dolby Vision, others)
    - [ ] Add language and subtitle preference configuration
    - [ ] Create debrid service management interface

- [ ] **Stream History & Analytics**
    - [ ] Implement watch history with stream source tracking
    - [ ] Add stream quality analytics and user preference learning
    - [ ] Create "Recently Watched" integration with stream continuity
    - [ ] Implement stream source reliability scoring based on success rates
    - [ ] Add usage analytics for scraper performance optimization

**Implementation Priority:** High - This is a core feature that differentiates the app from standard streaming services. Start with Phase 1 research, then implement Premiumize integration before choosing primary scraper service.

**Technical Notes:**
- Consider implementing both Torrentio and Comet with automatic failover for maximum reliability
- Ensure all debrid service API keys are stored securely and never logged
- Plan for legal compliance and appropriate content warnings
- Design with scalability in mind for adding more scraper services in the future


### 21. Update Continue Watching Logic
- [ ] Do not show episode in row if episode is not yet released 

### 22. Branding
- [ ] All logos (different size for all Android TVs, circle, square, landscape, etc)
- [ ] Splash Screen display with animation while background loading completes (rows load/init db/etc)

### 23. Links Select Page
- [ ] Proper Style
- [ ] Get rid of play button icon, move Filesze to that location

