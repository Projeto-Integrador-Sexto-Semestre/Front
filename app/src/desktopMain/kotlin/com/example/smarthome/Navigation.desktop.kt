package com.example.smarthome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

actual interface PlatformNavKey : NavKey

@Suppress("UNCHECKED_CAST")
@Composable
actual fun <T : PlatformNavKey> rememberPlatformNavStack(initialKey: T): MutableList<T> {
    val navConfig = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(CrudNavKey::class)
                    subclass(ScreenNavKey::class)
                }
            }
        }
    }
    return rememberNavBackStack(navConfig, initialKey) as MutableList<T>
}
