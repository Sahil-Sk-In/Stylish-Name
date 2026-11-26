package com.sahil.namemaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sahil.namemaker.data.repository.AppRepository
import com.sahil.namemaker.ui.screens.MainScreen
import com.sahil.namemaker.ui.theme.DarkBg
import com.sahil.namemaker.ui.theme.NameMakerTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AppRepository(applicationContext)

        setContent {
            NameMakerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    MainScreen(repository = repository)
                }
            }
        }
    }
}
