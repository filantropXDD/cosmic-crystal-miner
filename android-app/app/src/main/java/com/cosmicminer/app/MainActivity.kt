package com.cosmicminer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.cosmicminer.app.ui.CosmicMinerScreen
import com.cosmicminer.app.ui.GameViewModel
import com.cosmicminer.app.ui.theme.CosmicMinerTheme

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CosmicMinerTheme {
                CosmicMinerScreen(viewModel = gameViewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        gameViewModel.saveOnBackground()
    }
}
