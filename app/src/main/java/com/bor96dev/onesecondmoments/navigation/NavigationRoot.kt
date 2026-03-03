package com.bor96dev.onesecondmoments.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.bor96dev.calendar.presentation.CalendarScreen
import com.bor96dev.edit.presentation.EditScreenRoute
import com.bor96dev.glue.presentation.GlueScreenRoute
import com.bor96dev.montage.MontageScreen
import com.bor96dev.record.presentation.RecordScreen
import com.bor96dev.ui.R
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Route.Record::class, Route.Record.serializer())
                    subclass(Route.Montage::class, Route.Montage.serializer())
                    subclass(Route.Calendar::class, Route.Calendar.serializer())
                    subclass(Route.Edit::class, Route.Edit.serializer())
                    subclass(Route.Glue::class, Route.Glue.serializer())
                }
            }
        },
        Route.Record
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                val currentRoute = backStack.last()
                NavigationBarItem(
                    selected = currentRoute is Route.Montage,
                    onClick = {
                        if (currentRoute !is Route.Montage) {
                            backStack.clear()
                            backStack.add(Route.Montage)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.montage),
                            contentDescription = null
                        )
                    },
                    label = { Text("Montage") }
                )

                NavigationBarItem(
                    selected = currentRoute is Route.Record,
                    onClick = {
                        if (currentRoute !is Route.Record) {
                            backStack.clear()
                            backStack.add(Route.Record)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.record),
                            contentDescription = null
                        )
                    },
                    label = { Text("Record") }
                )

                NavigationBarItem(
                    selected = currentRoute is Route.Calendar,
                    onClick = {
                        if (currentRoute !is Route.Calendar) {
                            backStack.clear()
                            backStack.add(Route.Calendar)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.calendar),
                            contentDescription = null
                        )
                    },
                    label = { Text("Calendar") }
                )
            }
        }
    ) { padding ->
        val currentRoute = backStack.last()

        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                val tabOrder = listOf(
                    Route.Montage::class,
                    Route.Record::class,
                    Route.Calendar::class
                )
                val initialIndex = tabOrder.indexOf(initialState::class)
                val targetIndex = tabOrder.indexOf(targetState::class)
                if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
                    val forward = targetIndex > initialIndex
                    val enter =
                        slideInHorizontally { offset -> if (forward) offset else -offset } + fadeIn()
                    val exit =
                        slideOutHorizontally { offset -> if (forward) -offset else offset } + fadeOut()
                    enter togetherWith exit
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) { route ->
            when (route) {
                is Route.Record -> {
                    RecordScreen(
                        onVideoRecorded = { uri, date ->
                            backStack.add(Route.Edit(uri.toString(), date))
                        }
                    )
                }

                is Route.Montage -> {
                    MontageScreen(
                        onNavigateToGlue = { month, year ->
                            backStack.add(Route.Glue(monthQuery = month, year = year))
                        }
                    )
                }

                is Route.Calendar -> {
                    CalendarScreen(
                        onNavigateToRecord = {
                            backStack.add(Route.Record)
                        },
                        onNavigateToEdit = { uri, date ->
                            backStack.add(Route.Edit(uri.toString(), date))
                        }
                    )
                }

                is Route.Edit -> {
                    EditScreenRoute(
                        videoUri = route.videoUri,
                        date = route.date,
                        navId = route.id,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                is Route.Glue -> {
                    GlueScreenRoute(
                        monthQuery = route.monthQuery,
                        year = route.year,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                else -> error("Unknown NavKey: $route")
            }
        }
    }
}
