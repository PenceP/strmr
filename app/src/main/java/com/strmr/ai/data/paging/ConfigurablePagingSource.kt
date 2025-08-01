package com.strmr.ai.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.sqlite.db.SimpleSQLiteQuery
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.database.DataSourceQueryBuilder
import com.strmr.ai.data.database.MovieDao
import com.strmr.ai.data.database.TvShowDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A PagingSource that properly implements pagination for both Movies and TV Shows.
 * This replaces the broken MediaPagingSource that was loading all data at once.
 *
 * Key features:
 * - Loads data from database in pages (not all at once)
 * - Supports configurable page size
 * - Works with dynamic data sources via DataSourceConfig
 */
class ConfigurablePagingSource<T : Any>(
    private val config: DataSourceConfig,
    private val movieDao: MovieDao? = null,
    private val tvShowDao: TvShowDao? = null,
    private val pageSize: Int = 20,
    private val invalidationTracker: Int = 0, // Force recreation when data changes
) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return withContext(Dispatchers.IO) {
            try {
                val page = params.key ?: 1
                val offset = (page - 1) * pageSize

                Log.d("ConfigurablePagingSource", "📄 Loading page $page for ${config.title} (offset: $offset, pageSize: $pageSize)")

                // Build the query with LIMIT and OFFSET for proper pagination
                val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
                val baseQuery =
                    when {
                        movieDao != null -> "SELECT * FROM movies WHERE $fieldName IS NOT NULL ORDER BY $fieldName ASC"
                        tvShowDao != null -> "SELECT * FROM tv_shows WHERE $fieldName IS NOT NULL ORDER BY $fieldName ASC"
                        else -> throw IllegalArgumentException("Either movieDao or tvShowDao must be provided")
                    }

                val paginatedQuery = "$baseQuery LIMIT $pageSize OFFSET $offset"
                val sqlQuery = SimpleSQLiteQuery(paginatedQuery)

                val items: List<T> =
                    when {
                        movieDao != null -> {
                            val movies = movieDao.getMoviesFromDataSourcePaged(sqlQuery)
                            movies as List<T>
                        }
                        tvShowDao != null -> {
                            val shows = tvShowDao.getTvShowsFromDataSourcePaged(sqlQuery)
                            shows as List<T>
                        }
                        else -> emptyList()
                    }

                Log.d("ConfigurablePagingSource", "✅ Loaded ${items.size} items for page $page of ${config.title}")

                // Determine if there are more pages
                val nextKey = if (items.size < pageSize) null else page + 1
                val prevKey = if (page == 1) null else page - 1

                LoadResult.Page(
                    data = items,
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
            } catch (e: Exception) {
                Log.e("ConfigurablePagingSource", "❌ Error loading page ${params.key} for ${config.title}", e)
                LoadResult.Error(e)
            }
        }
    }
}
