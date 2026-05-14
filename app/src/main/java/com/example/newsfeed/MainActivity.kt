package com.example.newsfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.newsfeed.ui.RtsNewsApp
import com.example.newsfeed.ui.theme.NewsfeedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsfeedTheme {
                RtsNewsApp()
            }
        }
    }
}
