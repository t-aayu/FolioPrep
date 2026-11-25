package com.example.domain

data class PageSlice<T>(
    val items: List<T>,
    val pageIndex: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val startItemNumber: Int,
    val endItemNumber: Int
) {
    val hasPrevious: Boolean get() = pageIndex > 0
    val hasNext: Boolean get() = pageIndex < totalPages - 1
    val previousCount: Int get() = if (hasPrevious) pageSize else 0
    val nextCount: Int get() = if (hasNext) minOf(pageSize, totalItems - endItemNumber) else 0
    val isPaginated: Boolean get() = totalItems > pageSize
}

object LibraryPagination {
    const val DEFAULT_PAGE_SIZE = 5
    const val SEARCH_PAGE_SIZE = 10

    fun pageSizeFor(searchQuery: String): Int =
        if (searchQuery.isNotBlank()) SEARCH_PAGE_SIZE else DEFAULT_PAGE_SIZE

    fun <T> slice(
        allItems: List<T>,
        requestedPage: Int,
        pageSize: Int
    ): PageSlice<T> {
        val safePageSize = pageSize.coerceAtLeast(1)
        if (allItems.isEmpty()) {
            return PageSlice(
                items = emptyList(),
                pageIndex = 0,
                pageSize = safePageSize,
                totalItems = 0,
                totalPages = 1,
                startItemNumber = 0,
                endItemNumber = 0
            )
        }
        val totalPages = (allItems.size + safePageSize - 1) / safePageSize
        val safePage = requestedPage.coerceIn(0, totalPages - 1)
        val fromIndex = safePage * safePageSize
        val toIndex = minOf(fromIndex + safePageSize, allItems.size)
        return PageSlice(
            items = allItems.subList(fromIndex, toIndex),
            pageIndex = safePage,
            pageSize = safePageSize,
            totalItems = allItems.size,
            totalPages = totalPages,
            startItemNumber = fromIndex + 1,
            endItemNumber = toIndex
        )
    }
}
