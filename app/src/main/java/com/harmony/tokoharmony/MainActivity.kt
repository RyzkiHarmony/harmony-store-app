package com.harmony.tokoharmony

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.harmony.tokoharmony.navigation.AppNavigation
import com.harmony.tokoharmony.ui.theme.TokoHarmonyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TokoHarmonyTheme {
                AppNavigation()
            }
        }
    }
}
