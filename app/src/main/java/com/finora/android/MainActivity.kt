package com.finora.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.finora.android.core.security.SecurityManager
import com.finora.android.ui.navigation.FinoraNavHost
import com.finora.android.ui.screens.security.AppLockScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import com.finora.android.core.di.DatabaseModule
import com.finora.android.ui.theme.FinoraTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val securityManager = SecurityManager.getInstance(this)
        val profileFlow = DatabaseModule.profileRepository.getActiveProfileFlow()

        setContent {
            val activeProfile by profileFlow.collectAsState(initial = null)
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (activeProfile?.themeMode?.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }

            FinoraTheme(darkTheme = isDarkTheme) {
                val lifecycleOwner = LocalLifecycleOwner.current
                var isAppLocked by remember {
                    mutableStateOf(securityManager.shouldRequireUnlock())
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                securityManager.onAppBackgrounded()
                            }
                            Lifecycle.Event.ON_START -> {
                                if (securityManager.shouldRequireUnlock()) {
                                    isAppLocked = true
                                }
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (isAppLocked) {
                    AppLockScreen(
                        securityManager = securityManager,
                        onUnlocked = { isAppLocked = false }
                    )
                } else {
                    FinoraNavHost()
                }
            }
        }
    }
}
