package com.example.smarthome

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

expect interface PlatformNavKey

@Serializable
data class CrudNavKey(val kindId: String) : PlatformNavKey

@Serializable
data class ScreenNavKey(val screen: String) : PlatformNavKey

@Composable
expect fun <T : PlatformNavKey> rememberPlatformNavStack(initialKey: T): MutableList<T>
