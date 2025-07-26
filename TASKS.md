# TASKS.md

## Android TV App: Netflix-Style UI with Trakt & TMDB

### 🎯 Core Principles
**Enforce through code review, tooling, and architectural decisions:**

- **SOLID Principles** (Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion)
- **DRY** - Don't Repeat Yourself (identify and eliminate duplication)
- **KISS** - Keep It Simple, Stupid (prefer simple solutions)
- **YAGNI** - You Aren't Gonna Need It (avoid over-engineering)
- **Android Architecture Guidelines** (follow Google's recommended patterns)
- **Consistent Error Handling** (standardize across all components)
- **Testable Code** (design for unit testing from the start)

---

## ✅ Completed Features

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

### 11. Video Player Integration
- [x] Add ExoPlayer [https://github.com/androidx/media]
- [x] Integrate TMDB API for trailers [api.themoviedb.org/3/movie/120/videos?language=en-US]
- [x] Implement trailers button functionality from MediaDetails page
- [x] Use videos marked as "official" and "Trailer" type

### 12. Intermediate View for Networks & Collections & Directors
- [x] Create intermediate page for when user clicks a network or collection
- [x] Hero/details section at the top (similar to MediaPage layout)
- [x] Single long row of posters for all movies/shows in that network/collection
- [x] Reuse MediaPage/MediaHero logic where possible
- [x] Ensure proper navigation back to previous page (HOME in this case) and forward to MediaDetails
- [x] The data source is in the HOME.json, Netflix has an example trakt list, i will fill the rest in later
- [x] Ensure "Trakt Lists" row sub-items go to their respective pages (Movie collection, movie watchlist, tv collection, tv watchlist), pulling from Trakt api

### 14. Pagination Fix
- [x] Investigate and fix broken paging system
- [x] Ensure movies and TV shows load beyond first page from API
- [x] Implement proper infinite scrolling for all content rows
- [x] Add loading indicators for pagination

### 15. Auto-Update System
- [x] Implement app versioning system
- [x] Create server endpoint to check for updates
- [x] Add update checking on app startup
- [x] Implement automatic download and installation of updates
- [x] Add user notification system for available updates

### 16. CI/CD Pipeline with GitHub Actions
- [x] Set up build stage with proper Android SDK configuration
- [x] Implement automated testing stage
- [x] Configure APK compilation with version number in filename
- [x] Add artifacts upload for release builds
- [x] **Optional:** Set up emulator testing with basic smoke tests

### 19. Performance Optimization Round - Phase 1 Complete
- [x] **Profiling Setup**
    - [x] Set up Android Studio CPU Profiler for performance monitoring
    - [x] Configure method tracing for critical user flows (app launch, page navigation, poster loading)
    - [x] Establish baseline performance metrics (startup time, frame drops, memory usage)

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
    
    - [x] **Network Layer Performance**
        - [x] Review Retrofit service implementations for synchronous calls
        - [x] Audit API response handling for unnecessary object creation
        - [x] Check network request batching opportunities (multiple poster requests)
        - [x] Identify redundant API calls across screens

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

### 20. Torrent Scraper Integration - Research Complete
- [x] **Primary Scraper Integration (Torrentio vs Comet Decision)**
    - [x] Implement Torrentio scraper's API client using Retrofit
    - [x] Create data models for scraper responses (stream links, quality, size, etc.)
    - [x] Map IMDb IDs from TMDB/Trakt data to scraper requests

### 22. Branding
- [x] Splash Screen display with animation while background loading completes (rows load/init db/etc)

---

## 🚧 In Progress

### 11. Video Player Integration (Continued)
- [ ] Add ffmpeg support, that's what handles various encode types
- [ ] Create video player overlay with proper TV navigation controls (check https://github.com/Stremio/)

### 12. Intermediate View for Networks & Collections & Directors (Continued)
- [ ] Add support for 2 dataUrl in json (first is movies, second tv shows. make 2 rows in the intermediate view if detected)
- [ ] Reuse paging and cache lists, images, ratings,details, etc to database 
- [ ] use omdb/logos as well. for all intents and purposes make this look like (front end and back) trending movies (or movies page with only 1 row and no nav bar i guess.)

---

## 📋 Upcoming Tasks

### **PRIORITY 1: Code Quality & Architecture** 🔥

### 25. Code Architecture & Standards Refactor
**Priority:** Critical - Technical debt is impacting development velocity and maintainability

#### Phase 1: AI-Assisted Code Quality Analysis
- [ ] **Comprehensive Codebase Review**
    - [ ] Use AI to analyze entire repository for architectural anti-patterns
    - [ ] Identify violations of SOLID principles mentioned in project principles
    - [ ] Generate report on code duplication and DRY principle violations
    - [ ] Analyze adherence to Android/Kotlin best practices and conventions
    - [ ] Assess standard repository/directory structure compliance
    - [ ] Document unused files, functions, and dead code
    - [ ] Document "spaghetti code" areas that need immediate refactoring
    - [ ] **Row Implementation Analysis**
        - [ ] Document code duplication in row implementations
        - [ ] Identify similar row types that could share implementation
        - [ ] Analyze current row architecture (should only need 2 types: poster and landscape)
        - [ ] Verify all rows support: caching/database storage, pagination (when multiple pages exist)
        - [ ] Recommend consolidation to single row component with 2 MediaCard variants
    - [ ] Document ANY OTHER code duplication

- [ ] **Architecture Pattern Analysis**
    - [ ] Review current MVP/MVVM implementation consistency
    - [ ] Identify areas where dependency injection could improve testability
    - [ ] Analyze navigation patterns for consistency and best practices
    - [ ] Review data flow patterns and suggest improvements
    - [ ] Assess separation of concerns across layers

#### Phase 2: Incremental Refactoring Plan
- [ ] **High-Impact Refactoring Areas** (based on AI analysis)
    - [ ] Refactor identified god classes/methods
    - [ ] Extract common functionality into reusable components
    - [ ] Implement proper error handling patterns consistently
    - [ ] Standardize naming conventions across codebase
    - [ ] Remove dead code and unused dependencies

- [ ] **Architectural Improvements**
    - [ ] Implement consistent dependency injection pattern
    - [ ] Standardize data layer patterns (Repository pattern)
    - [ ] Create proper abstraction layers for external APIs
    - [ ] Implement consistent state management approach
    - [ ] Add proper logging and debugging infrastructure

#### Phase 3: Testing & Documentation
- [ ] **Code Quality Metrics**
    - [ ] Set up code quality tools (detekt, ktlint, SonarQube)
    - [ ] Establish code coverage targets and measurement
    - [ ] Create automated code quality checks in CI/CD
    - [ ] Document coding standards and contribution guidelines
    - [ ] Implement pre-commit hooks for code quality

- [ ] **Refactoring Validation**
    - [ ] Ensure all refactored code maintains existing functionality
    - [ ] Add unit tests for newly extracted components
    - [ ] Update documentation to reflect architectural changes
    - [ ] Performance test refactored components
    - [ ] Create migration guide for future development

### **PRIORITY 2: Performance Optimization** ⚡

### 19. Performance Optimization Round (Continued)
**Focus on speed improvements identified through AI analysis**

#### Phase 1: AI-Enhanced Analysis
- [ ] **AI-Assisted Codebase Analysis**
    - [ ] Use AI tools to analyze entire repository for performance bottlenecks
    - [ ] Generate automated performance improvement suggestions across all files
    - [ ] Identify patterns of inefficient code that may not be obvious in manual review
    - [ ] Cross-reference AI suggestions with profiling data for validation
    - [ ] Document AI-identified improvement opportunities by priority/impact

#### Phase 2: Targeted Speed Improvements
- [ ] **Database Layer Optimization**
    - [ ] Profile Room database queries using Database Inspector
    - [ ] Identify missing indices on frequently queried columns (movie IDs, timestamps)
    - [ ] Review DAO methods for N+1 query problems
    - [ ] Analyze cache invalidation strategies and optimization opportunities

- [ ] **Image Loading Optimization**
    - [ ] Profile Glide/Picasso usage patterns and memory consumption
    - [ ] Review image caching strategies and cache hit rates
    - [ ] Identify oversized image downloads (posters, backdrops)
    - [ ] Check for memory leaks in image loading callbacks

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

---

### **PRIORITY 3: Core Features** 🎬

### 8. MediaDetails Page Implementation (Continued)
- [ ] **Trakt Actions Enhancement**
    - [ ] Replace current Trakt actions with single "My Lists" button (Trakt logo on left)
    - [ ] Create popup with checkboxes for collection and watchlists 
    - [ ] Show correct state if already added (checked boxes)
    - [ ] Add "OK" and "Cancel" buttons, send updates to Trakt on "OK" press
- [ ] **Content Enhancement**
    - [ ] Add "Viewers also liked" row from TMDB API (under similar row)
    - [ ] Replace TMDB similar row with Trakt API → https://private-anon-45113404ac-trakt.apiary-mock.com/shows/game-of-thrones/related

### 5. Polish & UX (Continued)
- [ ] Handle loading and error states
- [ ] Responsive layout for different TV sizes

### 13. Season/Episode View Enhancement
- [ ] **Modern Episode Interface**
    - [ ] Air date display
    - [ ] Episode summary/overview
    - [ ] Episode screenshot as landscape image
    - [ ] Episode runtime and rating info
    - [ ] Watch status integration with Trakt
    - [ ] Proper TV navigation between episodes

### 16. CI/CD Pipeline (Continued)
- [ ] Configure release deployment workflow
- [ ] Separate "BETA" channel for development testing (enable in settings)
- [ ] **Must be completed before auto-update functionality**

### 15. Auto-Update System (Continued)
- [ ] **Note: Should be implemented after CI/CD system (Task 16)**

### 17. Watched Indicators
- [ ] **Visual Progress Tracking**
    - [ ] Watched flag on poster if media is fully watched
    - [ ] Watched flag with % watched if partial
    - [ ] For TV: Show number of episodes remaining unwatched

### 18. Long-Press Functionality
- [ ] **Enhanced Poster Interactions**
    - [ ] Add to/remove from Watchlist/Collection (depending on current status)
    - [ ] Mark Watched/unwatched (works on posters, season buttons, episodes)
    - [ ] Additional context menu options as needed

### 21. Continue Watching Logic Update
- [ ] **Smart Episode Display**
    - [ ] Do not show episode in row if episode is not yet released

### 22. Branding (Continued)
- [ ] **Complete Logo Suite**
    - [ ] All logos (different sizes for all Android TVs: circle, square, landscape, etc.)

### 23. Links Select Page Enhancement
- [ ] **UI/UX Improvements**
    - [ ] Proper styling implementation
    - [ ] Remove play button icon, move file size to that location
    - [ ] Loading screen enhancement: Replace "loading trailer" text with:
        - [ ] Same blurred backdrop
        - [ ] Large, centered media logo
        - [ ] Fading in/out animation until stream loads

---

### **PRIORITY 4: Advanced Features** 🚀

### 20. Torrent Scraper Integration (Continued)
**Reference Links:**
- Torrentio Scraper: https://github.com/TheBeastLT/torrentio-scraper
- Torrentio Configuration: https://torrentio.strem.fun/
- Comet Scraper: https://github.com/g0ldyy/comet  
- Comet Hosted Instance: comet.elfhosted.com

#### Phase 1: Research & Architecture Planning ✅ (Completed)

#### Phase 2: Debrid Service Integration
- [ ] **Premiumize Integration**
    - [ ] Implement OAuth flow for Premiumize authentication
    - [ ] Create Retrofit service for Premiumize API endpoints
    - [ ] Add secure storage for Premiumize API keys using EncryptedSharedPreferences
    - [ ] Implement API key validation and refresh mechanisms
    - [x] Add user settings screen for debrid service configuration

- [ ] **Alternative Debrid Services Support**
    - [ ] Research and document Real-Debrid API integration
    - [ ] Plan AllDebrid, DebridLink, TorBox integration for future expansion
    - [ ] Create abstracted debrid service interface for multiple providers
    - [ ] Implement debrid service selection in user settings

#### Phase 3: Scraper Service Implementation
- [ ] **Enhanced Scraper Integration**
    - [ ] Handle scraper rate limiting and error responses gracefully
    - [ ] Implement caching strategy for scraper results to avoid redundant requests
    - [ ] Implement Comet scraper as backup (only if Torrentio pulls <12 links)
    - [ ] Cache streams in database for instant display in future

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
    - [ ] Implement quality preference sliders and bitrate settings
    - [ ] Filter streams by type (Cam, Dolby Vision, others)
    - [ ] Buffer size selector (small, medium, large)
    - [ ] Add language and subtitle preference configuration
    - [ ] Create debrid service management interface

- [ ] **Autoplay Settings** 
    - [ ] Enable autoplay next episode (slider 1-75 seconds before episode end)
    - [ ] If enabled, prefetch next episode links from scraper

- [ ] **Stream History & Analytics**
    - [ ] Implement watch history with stream source tracking
    - [ ] Add stream quality analytics and user preference learning
    - [ ] Create "Recently Watched" integration with stream continuity
    - [ ] Implement stream source reliability scoring based on success rates
    - [ ] Add usage analytics for scraper performance optimization

### 24. Trakt Scrobbling Integration
**Priority:** High - Core feature for tracking watch progress and syncing with Trakt ecosystem

#### Phase 1: Core Scrobbling Infrastructure
- [ ] **Scrobble API Implementation**
    - [ ] Create Retrofit service for Trakt scrobbling endpoints (/scrobble/start, /scrobble/pause, /scrobble/stop)
    - [ ] Implement data models for scrobble requests and responses
    - [ ] Add proper OAuth token handling for authenticated scrobble requests
    - [ ] Create ScrobbleManager singleton to handle all scrobbling operations
    - [ ] Implement secure storage for scrobble session data and sync state

- [ ] **Check-in System**
    - [ ] Implement automatic check-in when video playback starts
    - [ ] Send initial scrobble with media ID, progress (0%), and timestamp
    - [ ] Handle check-in conflicts (user watching on multiple devices)
    - [ ] Add manual check-in option for users who want to mark "currently watching"
    - [ ] Implement check-in cancellation when user stops before completion

#### Phase 2: Progress Tracking & Synchronization
- [ ] **Real-time Progress Monitoring**
    - [ ] Integrate progress tracking with ExoPlayer playback position
    - [ ] Send progress updates to Trakt every 5 minutes during active playback
    - [ ] Handle playback interruptions (pause, seek, app backgrounding)
    - [ ] Implement progress validation to prevent invalid timestamps
    - [ ] Add network failure handling with offline progress caching

- [ ] **Session Management**
    - [ ] Track viewing sessions with start/end timestamps
    - [ ] Handle session resumption when app is reopened during playback
    - [ ] Manage multiple concurrent sessions (different episodes/movies)
    - [ ] Implement session timeout handling for abandoned playback
    - [ ] Add session conflict resolution for multi-device usage

#### Phase 3: Completion & Watch Status
- [ ] **Automatic Completion Detection**
    - [ ] Mark content as "watched" when progress reaches 90% completion
    - [ ] Send final scrobble/stop request with completion percentage
    - [ ] Handle early termination (user stops before 90% - still send progress)
    - [ ] Implement different completion thresholds for movies vs TV episodes
    - [ ] Add user preference for custom completion percentage threshold

- [ ] **Watch Status Synchronization**
    - [ ] Update local database with watched status after successful scrobble
    - [ ] Sync watch status across all app screens (Continue Watching, etc.)
    - [ ] Handle watched status conflicts between local and Trakt data
    - [ ] Implement batch sync for multiple completed items
    - [ ] Add manual "Mark as Watched/Unwatched" with immediate scrobble

#### Phase 4: Advanced Scrobbling Features
- [ ] **Smart Scrobbling Logic**
    - [ ] Implement intelligent episode progression for TV series
    - [ ] Auto-advance to next episode with seamless scrobbling transition
    - [ ] Handle season finale completion and series completion tracking
    - [ ] Add binge-watching detection with optimized scrobbling frequency
    - [ ] Implement rewatch detection and proper scrobbling for rewatches

- [ ] **Offline Scrobbling Support**
    - [ ] Cache scrobble data when network is unavailable
    - [ ] Implement background sync when connectivity is restored
    - [ ] Handle timestamp conflicts for delayed scrobbles
    - [ ] Add queue management for pending scrobble requests
    - [ ] Provide user feedback for successful/failed offline sync

#### Phase 5: Error Handling & User Experience
- [ ] **Robust Error Management**
    - [ ] Handle Trakt API rate limiting with exponential backoff
    - [ ] Implement retry logic for failed scrobble requests
    - [ ] Add graceful degradation when Trakt is unavailable
    - [ ] Handle authentication expiration during active sessions
    - [ ] Provide clear error messages for scrobbling failures

- [ ] **User Interface Integration**
    - [ ] Add scrobbling status indicators in video player overlay
    - [ ] Show "Currently Watching" status in Continue Watching row
    - [ ] Display sync status in user profile/settings area
    - [ ] Add scrobbling history view for debugging and user awareness
    - [ ] Implement manual scrobble correction tools for edge cases

#### Phase 6: Settings & Customization
- [ ] **Scrobbling Preferences**
    - [ ] Add toggle to enable/disable automatic scrobbling
    - [ ] Create completion percentage threshold setting (default 90%)
    - [ ] Implement progress update frequency setting (default 5 minutes)
    - [ ] Add privacy settings for public vs private scrobbling
    - [ ] Create manual vs automatic scrobbling mode selection

- [ ] **Advanced Configuration**
    - [ ] Add scrobbling exclusions for specific content types
    - [ ] Implement device-specific scrobbling settings
    - [ ] Create scrobbling analytics and statistics view
    - [ ] Add bulk scrobbling tools for importing watch history
    - [ ] Implement scrobbling backup and restore functionality

#### Phase 7: Integration with Existing Features
- [ ] **Continue Watching Enhancement**
    - [ ] Update Continue Watching logic to use scrobble progress data
    - [ ] Remove completed items from Continue Watching automatically 
    - [ ] Show accurate progress percentages from scrobble data
    - [ ] Implement cross-device progress synchronization
    - [ ] Add "Resume from last position" using scrobble timestamps

- [ ] **Collection & Watchlist Integration**
    - [ ] Auto-remove completed movies from Watchlist (user preference)
    - [ ] Update Collection completion statistics based on scrobbles
    - [ ] Sync scrobbling data with Trakt Collection/Watchlist changes
    - [ ] Add completion badges to poster overlays based on scrobble data
    - [ ] Implement "Watched" filtering for all content rows

---

## 🎯 Implementation Strategy

### **Phase 1: Foundation (Current Priority)**
1. **Task 25** - Code Architecture & Standards Refactor
2. **Task 19** - Performance Optimization (AI-Enhanced)
3. Complete remaining MediaDetails enhancements

### **Phase 2: Core Functionality**
1. **Task 24** - Trakt Scrobbling Integration
2. **Task 20** - Torrent Scraper Integration (Phases 2-4)
3. Polish & UX improvements

### **Phase 3: Advanced Features**
1. **Task 20** - Advanced Torrent Features (Phases 5-7)
2. Enhanced user interactions (long-press, watched indicators)
3. Final polish and testing

---

## 📝 Technical Notes

### **Performance Focus Areas:**
- Startup time optimization
- Navigation smoothness
- Memory management
- Database query optimization
- Image loading efficiency

### **Architecture Priorities:**
- SOLID principles enforcement
- Dependency injection consistency
- Error handling standardization
- Testing infrastructure
- Code quality metrics

### **Integration Considerations:**
- Secure credential storage
- Offline functionality
- Cross-device synchronization
- API rate limiting
- Error recovery mechanisms

---

*Last Updated: 7/26/25*
*Next Review: After Task 25 Phase 1 completion*