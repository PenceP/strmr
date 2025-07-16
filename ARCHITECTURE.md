# Strmr Architecture

## Overview

This document describes the refactored architecture of the Strmr Android TV app, which follows modern Android development best practices and clean architecture principles.

## Project Structure

```
app/src/main/java/com/strmr/ai/
├── MainActivity.kt                    # Main entry point (simplified)
├── data/                             # Data layer
│   ├── database/                     # Room database entities and DAOs
│   ├── MovieRepository.kt           # Movie data operations
│   ├── TvShowRepository.kt          # TV show data operations
│   └── RetrofitInstance.kt          # API client setup
├── viewmodel/                        # ViewModel layer
│   ├── BaseMediaViewModel.kt        # Base class for media ViewModels
│   ├── MoviesViewModel.kt           # Movie-specific ViewModel
│   └── TvShowsViewModel.kt          # TV show-specific ViewModel
├── ui/                              # UI layer
│   ├── components/                  # Reusable UI components
│   │   ├── MediaCard.kt            # Individual media item card
│   │   ├── MediaDetails.kt         # Media details panel
│   │   ├── MediaHero.kt            # Hero section with backdrop
│   │   └── MediaRow.kt             # Horizontal scrolling media row
│   ├── navigation/                  # Navigation components
│   │   └── NavigationBar.kt        # Side navigation bar
│   ├── screens/                     # Screen components
│   │   ├── MediaPage.kt            # Generic media page
│   │   ├── MoviesPage.kt           # Movie-specific page
│   │   ├── TvShowsPage.kt          # TV show-specific page
│   │   └── PlaceholderPage.kt      # Placeholder for unimplemented pages
│   └── theme/                       # App theming
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## Architecture Principles

### 1. Separation of Concerns
- **UI Layer**: Handles only UI logic and user interactions
- **ViewModel Layer**: Manages UI state and business logic
- **Data Layer**: Handles data operations and API calls

### 2. Component Reusability
- Generic `MediaPage` component used by both Movies and TV Shows
- Reusable UI components (`MediaCard`, `MediaDetails`, `MediaHero`, `MediaRow`)
- Base ViewModel class to reduce code duplication

### 3. Single Responsibility
- Each component has a single, well-defined purpose
- ViewModels handle specific media types
- UI components are focused and composable

## Key Components

### BaseMediaViewModel
Abstract base class that provides common functionality for media ViewModels:
- Error handling
- Logo fetching
- State management
- Common logging

### MediaPage
Generic page component that can display any type of media:
- Handles loading, error, and success states
- Manages selection state
- Provides consistent UI across different media types

### UI Components
- **MediaCard**: Individual media item with selection animation
- **MediaDetails**: Displays media information (title, rating, overview, etc.)
- **MediaHero**: Hero section with backdrop image and gradients
- **MediaRow**: Horizontal scrolling list with keyboard navigation

## Benefits of Refactor

### Before Refactor
- MainActivity: 911 lines
- Code duplication between Movies and TV Shows
- Mixed concerns in single file
- Hard to maintain and extend

### After Refactor
- MainActivity: ~80 lines
- Reusable components
- Clear separation of concerns
- Easy to maintain and extend
- Consistent UI patterns

## Adding New Features

### Adding a New Media Type
1. Create a new ViewModel extending `BaseMediaViewModel`
2. Create a new page component using `MediaPage`
3. Add navigation route in `MainActivity`

### Adding New UI Components
1. Create component in `ui/components/`
2. Make it generic and reusable
3. Use composition over inheritance

### Adding New Screens
1. Create screen component in `ui/screens/`
2. Add navigation route
3. Update navigation bar if needed

## Best Practices Followed

1. **Composition over Inheritance**: Using composition for UI components
2. **Single Responsibility**: Each class has one reason to change
3. **DRY (Don't Repeat Yourself)**: Eliminated code duplication
4. **SOLID Principles**: Applied throughout the architecture
5. **Modern Android Development**: Using Jetpack Compose, ViewModels, and Coroutines
6. **Clean Architecture**: Clear separation between layers

## Future Improvements

1. **Dependency Injection**: Implement Hilt for better dependency management
2. **Testing**: Add unit tests for ViewModels and UI tests for components
3. **State Management**: Consider using more advanced state management patterns
4. **Error Handling**: Implement more sophisticated error handling and retry mechanisms
5. **Accessibility**: Improve accessibility features for TV navigation 