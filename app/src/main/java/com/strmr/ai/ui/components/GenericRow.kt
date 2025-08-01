package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strmr.ai.config.GenericRowConfiguration
import com.strmr.ai.viewmodel.GenericRowViewModel
import com.strmr.ai.viewmodel.rememberGenericRowViewModel

/**
 * Generic row component that works with any row configuration
 * Replaces the need for individual row implementations
 */
@Composable
fun GenericRow(
    configuration: GenericRowConfiguration,
    onItemClick: ((Any) -> Unit)? = null,
    onItemLongPress: ((Any) -> Unit)? = null,
    onSelectionChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel = rememberGenericRowViewModel(configuration)
    val items by viewModel.items.collectAsStateWithLifecycle()
    val paginationState by viewModel.paginationState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        // Row title - always show for now (could be made configurable)
        Text(
            text = configuration.title,
            modifier = Modifier.padding(start = 56.dp, bottom = 4.dp)
        )

        // Use existing UnifiedMediaRow with our generic ViewModel data
        UnifiedMediaRow(
            config = MediaRowConfig(
                title = configuration.title,
                dataSource = DataSource.RegularList(
                    items = items,
                    paginationState = paginationState
                ),
                onItemClick = onItemClick,
                onPaginate = { page ->
                    if (configuration.isPaginated) {
                        viewModel.loadPage(page)
                    } else {
                        viewModel.loadStaticData()
                    }
                },
                cardType = when (configuration.cardType) {
                    com.strmr.ai.config.CardType.PORTRAIT -> CardType.PORTRAIT
                    com.strmr.ai.config.CardType.LANDSCAPE -> CardType.LANDSCAPE
                },
                itemWidth = if (configuration.cardType == com.strmr.ai.config.CardType.PORTRAIT) 120.dp else 200.dp,
                itemSpacing = 12.dp,
                itemContent = { item, isFocused ->
                    MediaCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        isSelected = isFocused,
                        onClick = {
                            onItemClick?.invoke(item)
                        }
                    )
                },
                onSelectionChanged = onSelectionChanged,
                onItemLongPress = onItemLongPress
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Simplified usage for common cases
 */
@Composable
fun GenericRow(
    configuration: GenericRowConfiguration,
    onNavigateToDetails: ((Int) -> Unit)? = null,
    onSelectionChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    GenericRow(
        configuration = configuration,
        onItemClick = { item ->
            if (item is com.strmr.ai.data.database.MovieEntity) {
                onNavigateToDetails?.invoke(item.tmdbId)
            }
        },
        onSelectionChanged = onSelectionChanged,
        modifier = modifier
    )
}

/**
 * Example usage demonstrating how easy it is to create rows:
 * 
 * ```kotlin
 * @Composable
 * fun ExamplePage() {
 *     val rowConfigs = loadRowConfigurations("MOVIES")
 *     
 *     LazyColumn {
 *         items(rowConfigs) { config ->
 *             GenericRow(
 *                 configuration = config.toGenericRowConfiguration(),
 *                 onNavigateToDetails = { tmdbId -> 
 *                     // Navigate to details
 *                 }
 *             )
 *         }
 *     }
 * }
 * ```
 */