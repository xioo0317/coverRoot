package com.coverRoot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack

class Navigator(
    val backStack: NavBackStack,
) {
    fun push(key: NavKey) {
        if (key !in backStack) backStack.add(key)
    }

    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) backStack[backStack.lastIndex] = key
        else backStack.add(key)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun current(): NavKey? = backStack.lastOrNull()

    fun backStackSize(): Int = backStack.size
}

@Composable
fun rememberNavigator(startRoute: Route): Navigator {
    val backStack = rememberNavBackStack<Route>(startRoute)
    return androidx.compose.runtime.remember(backStack) { Navigator(backStack) }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("LocalNavigator not provided")
}
