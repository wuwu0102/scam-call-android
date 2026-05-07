package com.alertanumero.mx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alertanumero.mx.ui.MainScreen
import com.alertanumero.mx.ui.MainViewModel
import com.alertanumero.mx.ui.theme.AlertaNumeroMXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlertaNumeroMXApp()
        }
    }
}

@Composable
private fun AlertaNumeroMXApp(viewModel: MainViewModel = viewModel()) {
    AlertaNumeroMXTheme {
        Surface(modifier = Modifier) {
            MainScreen(viewModel = viewModel)
        }
    }
}
