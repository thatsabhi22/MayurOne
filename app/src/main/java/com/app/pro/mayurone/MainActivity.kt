package com.app.pro.mayurone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.app.pro.mayurone.ui.MayurOneApp
import com.app.pro.mayurone.ui.theme.MayurOneTheme
import com.app.pro.mayurone.ui.viewmodel.MayurOneViewModel
import com.app.pro.mayurone.ui.viewmodel.MayurOneViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MayurOneViewModel by viewModels {
        MayurOneViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MayurOneTheme {
                MayurOneApp(viewModel = viewModel)
            }
        }
    }
}
