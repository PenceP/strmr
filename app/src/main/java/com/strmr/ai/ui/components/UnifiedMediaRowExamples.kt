// package com.strmr.ai.ui.components
//
// import androidx.compose.foundation.layout.PaddingValues
// import androidx.compose.runtime.Composable
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.focus.FocusRequester
// import androidx.compose.ui.unit.dp
// import androidx.paging.compose.LazyPagingItems
//
// /**
// * Usage examples for UnifiedMediaRow showing common patterns
// */
// object UnifiedMediaRowExamples {
//
//    /**
//     * Basic movie/TV show row with poster cards
//     */
//    @Composable
//    fun <T : Any> StandardMediaRow(
//        title: String,
//        items: List<T>,
//        selectedIndex: Int,
//        isRowSelected: Boolean,
//        onSelectionChanged: (Int) -> Unit,
//        onItemClick: (T) -> Unit,
//        modifier: Modifier = Modifier
//    ) {
//        UnifiedMediaRow(
//            config = MediaRowConfig(
//                title = title,
//                dataSource = DataSource.RegularList(items),
//                selectedIndex = selectedIndex,
//                isRowSelected = isRowSelected,
//                onSelectionChanged = onSelectionChanged,
//                onItemClick = onItemClick,
//                cardType = CardType.PORTRAIT,
//                itemContent = { item, isSelected ->
//                    MediaCard(
//                        title = item.getTitle(),
//                        posterUrl = item.getPosterUrl(),
//                        isSelected = isSelected,
//                        onClick = { onItemClick(item) }
//                    )
//                }
//            ),
//            modifier = modifier
//        )
//    }
//
//    /**
//     * Episode-style row with landscape cards (like Continue Watching)
//     */
//    @Composable
//    fun <T : Any> EpisodeRow(
//        title: String,
//        items: List<T>,
//        selectedIndex: Int,
//        isRowSelected: Boolean,
//        onSelectionChanged: (Int) -> Unit,
//        onItemClick: (T) -> Unit,
//        focusRequester: FocusRequester? = null,
//        modifier: Modifier = Modifier
//    ) {
//        UnifiedMediaRow(
//            config = MediaRowConfig(
//                title = title,
//                dataSource = DataSource.RegularList(items),
//                selectedIndex = selectedIndex,
//                isRowSelected = isRowSelected,
//                onSelectionChanged = onSelectionChanged,
//                onItemClick = onItemClick,
//                focusRequester = focusRequester,
//                cardType = CardType.LANDSCAPE,
//                itemWidth = 200.dp,
//                itemSpacing = 12.dp,
//                contentPadding = PaddingValues(horizontal = 48.dp),
//                itemContent = { item, isSelected ->
//                    // Use your existing landscape card component or MediaCard
//                    MediaCard(
//                        title = item.getTitle(),
//                        posterUrl = item.getPosterUrl(),
//                        isSelected = isSelected,
//                        onClick = { onItemClick(item) }
//                    )
//                }
//            ),
//            modifier = modifier
//        )
//    }
//
//    /**
//     * Infinite scrolling row with paging
//     */
//    @Composable
//    fun <T : Any> PagingRow(
//        title: String,
//        pagingItems: LazyPagingItems<T>,
//        selectedIndex: Int,
//        isRowSelected: Boolean,
//        onSelectionChanged: (Int) -> Unit,
//        onItemClick: (T) -> Unit,
//        keyExtractor: (T) -> Any,
//        modifier: Modifier = Modifier
//    ) {
//        UnifiedMediaRow(
//            config = MediaRowConfig(
//                title = title,
//                dataSource = DataSource.PagingList(pagingItems),
//                selectedIndex = selectedIndex,
//                isRowSelected = isRowSelected,
//                onSelectionChanged = onSelectionChanged,
//                onItemClick = onItemClick,
//                keyExtractor = keyExtractor,
//                cardType = CardType.PORTRAIT,
//                itemContent = { item, isSelected ->
//                    MediaCard(
//                        title = item.getTitle(),
//                        posterUrl = item.getPosterUrl(),
//                        isSelected = isSelected,
//                        onClick = { onItemClick(item) }
//                    )
//                }
//            ),
//            modifier = modifier
//        )
//    }
//
//    /**
//     * Loading state row with skeletons
//     */
//    @Composable
//    fun <T : Any> LoadingRow(
//        title: String,
//        items: List<T>,
//        selectedIndex: Int,
//        isRowSelected: Boolean,
//        onSelectionChanged: (Int) -> Unit,
//        onItemClick: (T) -> Unit,
//        isLoading: Boolean,
//        modifier: Modifier = Modifier
//    ) {
//        UnifiedMediaRow(
//            config = MediaRowConfig(
//                title = title,
//                dataSource = DataSource.RegularList(items),
//                selectedIndex = selectedIndex,
//                isRowSelected = isRowSelected,
//                onSelectionChanged = onSelectionChanged,
//                onItemClick = onItemClick,
//                isLoading = isLoading,
//                skeletonCount = 8,
//                cardType = CardType.PORTRAIT,
//                itemContent = { item, isSelected ->
//                    MediaCard(
//                        title = item.getTitle(),
//                        posterUrl = item.getPosterUrl(),
//                        isSelected = isSelected,
//                        onClick = { onItemClick(item) }
//                    )
//                }
//            ),
//            modifier = modifier
//        )
//    }
//
//    /**
//     * Builder pattern for easy configuration
//     */
//    @Composable
//    fun <T : Any> BuilderExample(
//        title: String,
//        items: List<T>,
//        selectedIndex: Int,
//        isRowSelected: Boolean,
//        onSelectionChanged: (Int) -> Unit,
//        onItemClick: (T) -> Unit,
//        modifier: Modifier = Modifier
//    ) {
//        UnifiedMediaRow(
//            config = MediaRowBuilder.regular(
//                title = title,
//                items = items,
//                selectedIndex = selectedIndex,
//                isRowSelected = isRowSelected,
//                onSelectionChanged = onSelectionChanged,
//                itemContent = { item, isSelected ->
//                    MediaCard(
//                        title = item.getTitle(),
//                        posterUrl = item.getPosterUrl(),
//                        isSelected = isSelected,
//                        onClick = { onItemClick(item) }
//                    )
//                }
//            ).copy(
//                cardType = CardType.PORTRAIT,
//                onItemClick = onItemClick,
//                itemWidth = 120.dp,
//                itemSpacing = 16.dp
//            ),
//            modifier = modifier
//        )
//    }
// }
//
// /**
// * Migration guide showing how to replace old components
// */
// object MigrationGuide {
//    /*
//    // OLD: MediaRow
//    MediaRow(
//        title = "Popular Movies",
//        mediaItems = movies,
//        selectedIndex = selectedIndex,
//        isRowSelected = isRowSelected,
//        onSelectionChanged = onSelectionChanged,
//        onItemClick = onItemClick,
//        itemContent = { movie, isSelected ->
//            MovieCard(movie, isSelected, onItemClick)
//        }
//    )
//
//    // NEW: UnifiedMediaRow
//    UnifiedMediaRow(
//        config = MediaRowConfig(
//            title = "Popular Movies",
//            dataSource = DataSource.RegularList(movies),
//            selectedIndex = selectedIndex,
//            isRowSelected = isRowSelected,
//            onSelectionChanged = onSelectionChanged,
//            onItemClick = onItemClick,
//            cardType = CardType.PORTRAIT,
//            itemContent = { movie, isSelected ->
//                MovieCard(movie, isSelected, onItemClick)
//            }
//        )
//    )
//
//    // OR use builder:
//    UnifiedMediaRow(
//        config = MediaRowBuilder.regular(
//            title = "Popular Movies",
//            items = movies,
//            selectedIndex = selectedIndex,
//            isRowSelected = isRowSelected,
//            onSelectionChanged = onSelectionChanged,
//            itemContent = { movie, isSelected ->
//                MovieCard(movie, isSelected, onItemClick)
//            }
//        ).copy(onItemClick = onItemClick)
//    )
//
//    // OLD: CenteredMediaRow (ELIMINATE - use left-aligned instead)
//    CenteredMediaRow(...)
//
//    // NEW: UnifiedMediaRow (left-aligned by default)
//    UnifiedMediaRow(config = MediaRowConfig(...))
//
//    // OLD: PagingMediaRow
//    PagingMediaRow(
//        title = "Trending",
//        pagingFlow = pagingFlow,
//        selectedIndex = selectedIndex,
//        isRowSelected = isRowSelected,
//        onSelectionChanged = onSelectionChanged
//    )
//
//    // NEW: UnifiedMediaRow with paging
//    val pagingItems = pagingFlow.collectAsLazyPagingItems()
//    UnifiedMediaRow(
//        config = MediaRowConfig(
//            title = "Trending",
//            dataSource = DataSource.PagingList(pagingItems),
//            selectedIndex = selectedIndex,
//            isRowSelected = isRowSelected,
//            onSelectionChanged = onSelectionChanged,
//            itemContent = { item, isSelected ->
//                MediaCard(item.getTitle(), item.getPosterUrl(), isSelected, {})
//            }
//        )
//    )
//    */
// }
