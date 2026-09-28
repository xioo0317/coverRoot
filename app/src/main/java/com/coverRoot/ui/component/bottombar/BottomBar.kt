package com.coverRoot.ui.component.bottombar

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.utils.springAnimateToPage

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
    private val animatePageChanges: Boolean,
    initialPage: Int = pagerState.currentPage,
) {
    var selectedPage by mutableIntStateOf(initialPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    var usePager by mutableStateOf(true)

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return
        navJob?.cancel()
        selectedPage = targetIndex
        if (!usePager) return
        isNavigating = true
        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                if (animatePageChanges) {
                    pagerState.springAnimateToPage(targetIndex)
                } else {
                    pagerState.scrollToPage(targetIndex)
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!usePager) return
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    animatePageChanges: Boolean = true,
    initialPage: Int = pagerState.currentPage,
): MainPagerState {
    val coroutineScope = rememberCoroutineScope()
    return remember(pagerState, animatePageChanges) {
        MainPagerState(
            pagerState = pagerState,
            coroutineScope = coroutineScope,
            animatePageChanges = animatePageChanges,
            initialPage = initialPage,
        )
    }
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }
