package com.ispgr5.locationsimulator.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ispgr5.locationsimulator.presentation.delay.DelayScreen
import com.ispgr5.locationsimulator.presentation.edit.EditScreen
import com.ispgr5.locationsimulator.presentation.run.InfinityService
import com.ispgr5.locationsimulator.presentation.run.RunScreen
import com.ispgr5.locationsimulator.presentation.select.SelectScreen
import com.ispgr5.locationsimulator.ui.theme.LocationSimulatorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationSimulatorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "selectScreen") {
                        composable(route = "selectScreen") {
                            SelectScreen(navController = navController)
                        }
                        composable("editScreen?configurationId={configurationId}",
                            arguments = listOf(navArgument(
                                name = "configurationId"
                            ) {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                            )
                        ) {
                            EditScreen(navController = navController)
                        }
                        composable(route = "delayScreen?configurationId={configurationId}",
                            arguments = listOf(navArgument(
                                name = "configurationId"
                            ) {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                            )
                        ) {
                            DelayScreen(navController = navController, startServiceFunction = { startService() })
                        }
                        composable("runScreen"){
                            RunScreen(navController, stopServiceFunction = {stopService()})
                        }
                        composable("stopService"){
                            navController.navigateUp()
                        }
                    }
                }
            }
        }
    }

    private fun startService(){
        Intent(this, InfinityService::class.java).also {
            Log.d("debug","itAction: ${it.action}")
            it.action = "START"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
            }else{
                startService(it)
            }
        }
    }

    private fun stopService(){
        Intent(this, InfinityService::class.java).also {
            Log.d("debug","itAction: ${it.action}")
            it.action = "STOP"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
            }else{
                startService(it)
            }
        }
    }
}