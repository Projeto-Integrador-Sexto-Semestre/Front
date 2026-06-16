package com.example.smarthome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

actual interface PlatformNavKey

@Composable
actual fun <T : PlatformNavKey> rememberPlatformNavStack(initialKey: T): MutableList<T> {
    return remember { mutableStateListOf(initialKey) }
}
