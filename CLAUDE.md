# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Strmr** is an Android TV streaming application built with modern Android technologies:
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose for Android TV
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Database**: Room with Paging 3
- **Networking**: Retrofit + OkHttp
- **Video Playback**: ExoPlayer (Media3) and LibVLC
- **Async**: Coroutines + Flow
- **Build System**: Gradle with Kotlin DSL

## Essential Commands

### Building and Running
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Install on connected device
./gradlew installDebug
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.strmr.ai.domain.usecase.FetchLogoUseCaseTest"

# Run all checks (includes lint)
./gradlew check
```

### Required Setup
Before building, create a `secrets.properties` file in the project root with API keys:
```
TRAKT_API_KEY=your_trakt_key_here
TMDB_READ_KEY=your_tmdb_key_here
OMDB_API_KEY=your_omdb_key_here
```

## Architecture Overview

### Project Structure
```
app/src/main/java/com/strmr/ai/
├── data/               # Data layer - API services, repositories, models
│   ├── api/           # Retrofit services (Trakt, TMDB, OMDb)
│   ├── database/      # Room entities, DAOs, converters
│   ├── models/        # API response models
│   └── paging/        # Paging 3 sources
├── di/                # Hilt dependency injection modules
├── domain/            # Business logic layer
│   └── usecase/       # Use cases for business operations
├── ui/                # Presentation layer
│   ├── components/    # Reusable Compose components
│   ├── navigation/    # Navigation components
│   ├── screens/       # Screen composables
│   └── theme/         # Material theme configuration
├── utils/             # Utility classes and extensions
├── viewmodel/         # ViewModels for UI state management
└── MainActivity.kt    # Main activity with navigation setup
```

### Key Architectural Patterns

1. **MVVM with Repository Pattern**
   - ViewModels manage UI state and business logic
   - Repositories abstract data sources (API, database)
   - Use cases encapsulate single business operations

2. **Dependency Injection with Hilt**
   - All major components use constructor injection
   - Modules provide implementations in `di/` directory
   - ViewModels use `@HiltViewModel` annotation

3. **Reactive Data Flow**
   - Kotlin Flow for data streams
   - StateFlow in ViewModels for UI state
   - Coroutines for async operations

4. **Paging 3 Integration**
   - Used for infinite scrolling in content lists
   - Integrated with Room for caching
   - Custom PagingSource implementations

5. **Navigation**
   - Jetpack Compose Navigation
   - Focus management for TV navigation
   - Custom FocusMemoryManager for focus restoration

## Important Implementation Details

### API Integration
- **Trakt API**: User authentication, watchlists, trending content
- **TMDB API**: Movie/TV metadata, images, trailers
- **OMDb API**: Ratings from IMDb, Rotten Tomatoes, Metacritic
- **Scrapers**: Torrentio integration for stream sources

### Database Schema
- Room database with entities for movies, TV shows, episodes
- Caching strategy for API responses
- Paging 3 RemoteMediator for cache + network

### TV-Specific Considerations
- Focus handling with custom `FocusMemoryManager`
- D-pad navigation support
- Large screen layouts optimized for 10-foot UI
- Hardware remote key handling (including backspace as back)

### Video Playback
- ExoPlayer (Media3) as primary player
- LibVLC as fallback for better codec support
- YouTube video extraction for trailers
- Stream selection from multiple sources

## Development Workflow

1. **Feature Development**
   - Create feature branch from `main`
   - Follow existing patterns in similar components
   - Add appropriate error handling and loading states
   - Test on Android TV emulator (API 30+)

2. **Code Style**
   - Follow Kotlin coding conventions
   - Use Compose preview annotations for UI components
   - Maintain consistent naming patterns
   - Keep composables small and focused

3. **Testing Approach**
   - Unit tests for ViewModels and use cases
   - Mock repositories for testing
   - Use `kotlinx-coroutines-test` for async testing

4. **Performance Considerations**
   - Minimize recompositions in Compose
   - Use proper keys in lazy lists
   - Cache network responses in Room
   - Optimize image loading with Coil

## Current Development Focus

The project uses a detailed task tracking system in `TASKS.md`. Key priorities:

1. **Code Quality & Architecture** - Refactoring to improve maintainability
2. **Performance Optimization** - Improving app speed and responsiveness
3. **Torrent Scraper Integration** - Adding stream sources via debrid services
4. **Trakt Scrobbling** - Automatic watch progress tracking

## Android TV Specific Guidelines

- Always test with D-pad navigation
- Ensure focus indicators are clearly visible
- Handle back button properly for navigation
- Support voice search where applicable
- Optimize for landscape orientation only
- Test on actual Android TV hardware when possible