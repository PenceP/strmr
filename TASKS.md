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
- [ ] Handle loading and error states
- [x] Schedule and cache into db lists (ex Trending) updates, use previous list order in db for load speed
- [x] Ensure TV remote navigation works seamlessly
- [] Responsive layout for different TV sizes
- [x] Backdrop image should go to top and right of screen  (remove padding)

### 6. Testing
- [x] Test on Android TV emulator
- [ ] Test on real Android TV device

### 7. (Optional/Future)
- [ ] Add more rows (e.g., Popular, Top Rated)
- [ ] Implement search functionality
- [x] Add TV shows section
- [] User authentication with Trakt
- [ ] Display ratings from IMDb, Rotten Tomatoes, and Trakt in the details area (alongside or instead of current ratings)
- [x] Display genre info in the details area 

## Upcoming Major Tasks

### 1. Implement MediaDetails Page (NEXT)
- Create a dedicated details page for movies and TV shows, shown when a poster is clicked.
- Features:
  - Large backdrop image
  - Title, logo, year, runtime, genres, rating, summary/overview
  - Trakt actions: add/remove from watchlist, add/remove from collections (show correct state if already added)
  - Actor list (cast)
  - Similar movies/TV shows (horizontal row, context-aware)
  - **For TV shows:**
    - Season selector (dropdown or horizontal row)
    - Scrollable list of episode images for the selected season
    - Episode details (title, air date, overview, etc.)
  - **Ratings integration:**
    - Integrate OMDb API to fetch IMDb, Rotten Tomatoes, and Metacritic ratings.
    - Create Retrofit interface for OMDb (apikey=a8787305, for local/dev use only, do not commit).
    - Add data models for OMDb responses (ratings, votes, etc.).
    - Fetch ratings by IMDb ID (or title/year fallback).
    - Display ratings row with IMDb, Trakt, Rotten Tomatoes, Metacritic (show/hide as available).
    - Cache/store ratings to avoid excessive API calls.
- Integrate with navigation so clicking a poster opens this page.
- Ensure smooth focus/TV navigation and visual polish.

### 2. Intermediate View/Page for Networks & Collections
- When a user clicks a network or collection, show an intermediate page with:
  - A hero/details section at the top (like MediaPage)
  - A single long row of posters for all movies/shows in that network/collection
- Reuse MediaPage/MediaHero logic where possible.
- Ensure navigation back to previous page and onward to MediaDetails.

### 3. Refactor HOME.json and Add MOVIES.json/TV.json for Row Config
- Move all row configuration (order, type, data source) out of HomePage.kt and into JSON files.
- Create MOVIES.json and TV.json for movies and TV-specific row configs.
- Ensure all rows (Continue Watching, Networks, Collections, etc.) are defined in JSON, not hardcoded.
- Think through all info needed in the JSON: row type, title, data source, display options, etc.
- Refactor code to dynamically build rows from JSON at compile time.

---

**Next up: Start on Task 1 (MediaDetails Page).** 