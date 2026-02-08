package com.bor96dev.onesecondmoments

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bor96dev.onesecondmoments.navigation.NavigationRoot
import com.bor96dev.onesecondmoments.ui.theme.OneSecondMomentsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneSecondMomentsTheme {
                NavigationRoot()
            }
        }
    }
}

