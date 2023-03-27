package com.ispgr5.locationsimulator.presentation.delay

import Timer
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ispgr5.locationsimulator.R
import com.ispgr5.locationsimulator.domain.model.ConfigComponent
import com.ispgr5.locationsimulator.presentation.editTimeline.components.Timeline
import com.ispgr5.locationsimulator.presentation.universalComponents.TopBar

/**
 * The Delay Screen.
 * Here you can check you have Select the right Configuration
 * and set a timer
 */
@ExperimentalAnimationApi
@Composable
fun DelayScreen(
	navController: NavController,
	viewModel: DelayViewModel = hiltViewModel(),
	startServiceFunction: (List<ConfigComponent>, Boolean) -> Unit,
) {
	//The state from viewmodel
	val state = viewModel.state.value

	Scaffold(
		topBar = { TopBar(navController, stringResource(id = R.string.ScreenDelay)) },
		content = {
			Spacer(modifier = Modifier.height(it.calculateTopPadding()))
			Column(
				Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Spacer(modifier = Modifier.size(8.dp))

				if (state.configuration == null) {
					Text(text = "Configuration is null")
				} else {
					Text(
						text = state.configuration.name,
						style = TextStyle(fontSize = 24.sp)
					)

					Spacer(modifier = Modifier.size(8.dp))
					Divider(color = MaterialTheme.colors.primary, thickness = 1.dp)
					Spacer(modifier = Modifier.size(8.dp))

					Text(text = state.configuration.description)
				}

				/**
				 * The Timeline
				 */
				Spacer(modifier = Modifier.size(8.dp))
				Divider(color = MaterialTheme.colors.primary, thickness = 1.dp)
				Spacer(modifier = Modifier.size(8.dp))

				LazyColumn {
					items(1) {
						state.configuration?.components?.let {
							Timeline(
								components = it,
								selectedComponent = null,
								onSelectAComponent = fun(_: ConfigComponent) {},
								onAddClicked = fun() {},
								showAddButton = false
							)
						}
					}
				}

				Spacer(modifier = Modifier.size(8.dp))
				Divider(color = MaterialTheme.colors.primary, thickness = 1.dp)
				Spacer(modifier = Modifier.size(8.dp))

				Timer(viewModel, startServiceFunction, navController)
			}
		})
}