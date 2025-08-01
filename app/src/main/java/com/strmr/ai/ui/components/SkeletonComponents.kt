package com.strmr.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Skeleton component matching MediaCard dimensions and animations
 */
@Composable
fun MediaCardSkeleton(
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val baseWidth = 120.dp
    val baseHeight = 180.dp
    val targetWidth = if (isSelected) baseWidth * 1.2f else baseWidth
    val targetHeight = if (isSelected) baseHeight * 1.1f else baseHeight

    Box(
        modifier =
            modifier
                .width(targetWidth)
                .height(targetHeight),
    ) {
        ShimmerBox(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 8,
        )
    }
}

/**
 * Skeleton component matching LandscapeMediaCard dimensions
 */
@Composable
fun LandscapeMediaCardSkeleton(
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val cardWidth = if (isSelected) 240.dp else 220.dp
    val cardHeight = if (isSelected) 155.dp else 135.dp

    Column(
        modifier =
            modifier
                .width(cardWidth)
                .height(cardHeight),
    ) {
        // Main image area
        ShimmerBox(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            cornerRadius = 8,
        )

        // Progress bar area (optional)
        Spacer(modifier = Modifier.height(2.dp))
        ShimmerBox(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            cornerRadius = 0,
        )
    }
}

/**
 * Skeleton component matching SquareMediaCard (if used)
 */
@Composable
fun SquareMediaCardSkeleton(
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val size = if (isSelected) 132.dp else 120.dp

    Box(
        modifier =
            modifier
                .size(size),
    ) {
        ShimmerBox(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 8,
        )
    }
}

/**
 * Skeleton component for MediaRow with title and cards
 */
@Composable
fun MediaRowSkeleton(
    title: String = "Loading...",
    cardCount: Int = 8,
    itemWidth: Dp = 120.dp,
    itemSpacing: Dp = 18.dp,
    modifier: Modifier = Modifier,
    cardType: SkeletonCardType = SkeletonCardType.PORTRAIT,
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Row title skeleton
        ShimmerText(
            modifier =
                Modifier
                    .padding(start = 8.dp, bottom = 4.dp)
                    .width(150.dp),
            height = 16,
        )

        // Cards row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        when (cardType) {
                            SkeletonCardType.PORTRAIT -> 210.dp
                            SkeletonCardType.LANDSCAPE -> 165.dp
                            SkeletonCardType.SQUARE -> 150.dp
                        },
                    ),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            items(cardCount) { index ->
                when (cardType) {
                    SkeletonCardType.PORTRAIT -> {
                        MediaCardSkeleton(
                            isSelected = index == 0, // First item appears selected
                            modifier = Modifier.width(itemWidth),
                        )
                    }
                    SkeletonCardType.LANDSCAPE -> {
                        LandscapeMediaCardSkeleton(
                            isSelected = index == 0,
                            modifier = Modifier.width(itemWidth * 1.8f),
                        )
                    }
                    SkeletonCardType.SQUARE -> {
                        SquareMediaCardSkeleton(
                            isSelected = index == 0,
                            modifier = Modifier.width(itemWidth),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Skeleton for multiple media rows (full page loading)
 */
@Composable
fun MediaPageSkeleton(
    rowCount: Int = 4,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        repeat(rowCount) { index ->
            MediaRowSkeleton(
                cardType =
                    when (index) {
                        0 -> SkeletonCardType.LANDSCAPE // First row is usually "Continue Watching"
                        else -> SkeletonCardType.PORTRAIT
                    },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Skeleton for hero section (large featured content)
 */
@Composable
fun MediaHeroSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Color.Black.copy(alpha = 0.1f)),
    ) {
        // Background image skeleton
        ShimmerBox(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 0,
        )

        // Content overlay skeleton
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(32.dp),
        ) {
            // Title skeleton
            ShimmerText(
                modifier = Modifier.width(300.dp),
                height = 32,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description skeleton
            ShimmerText(
                modifier = Modifier.width(400.dp),
                height = 16,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ShimmerText(
                modifier = Modifier.width(350.dp),
                height = 16,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Button skeleton
            ShimmerBox(
                modifier =
                    Modifier
                        .width(120.dp)
                        .height(40.dp),
                cornerRadius = 20,
            )
        }
    }
}

enum class SkeletonCardType {
    PORTRAIT,
    LANDSCAPE,
    SQUARE,
}
