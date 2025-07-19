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
- [ ] Investigate and fix broken paging system
- [ ] Ensure movies and TV shows load beyond first page from API
- [ ] Implement proper infinite scrolling for all content rows
- [ ] Add loading indicators for pagination

### 15. Auto-Update System
- [ ] Implement app versioning system
- [ ] Create server endpoint to check for updates
- [ ] Add update checking on app startup
- [ ] Implement automatic download and installation of updates
- [ ] Add user notification system for available updates
- [ ] **Note: Should be implemented after CI/CD system (Task 16)**

### 16. CI/CD Pipeline with GitHub Actions
- [ ] Set up build stage with proper Android SDK configuration
- [ ] Implement automated testing stage
- [ ] Configure APK compilation with version number in filename
- [ ] Add artifacts upload for release builds
- [ ] **Optional:** Set up emulator testing with basic smoke tests
- [ ] Configure release deployment workflow
- [ ] **Must be completed before auto-update functionality**

---

**Next Priority: Complete Task 11 (Video Player Integration) - Add ExoPlayer and trailer functionality to MediaDetails page**