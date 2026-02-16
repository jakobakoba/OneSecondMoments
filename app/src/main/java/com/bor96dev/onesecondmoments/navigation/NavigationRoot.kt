package com.bor96dev.onesecondmoments.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.bor96dev.calendar.CalendarScreen
import com.bor96dev.edit.presentation.EditScreenRoute
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
        NavDisplay(
            modifier = Modifier.padding(padding),
            backStack = backStack,
            entryProvider = { key ->
                when (key) {
                    is Route.Record -> {
                        NavEntry(key) {
                            RecordScreen(
                                onVideoRecorded = { uri, date ->
                                    backStack.add(Route.Edit(uri.toString(), date))
                                }
                            )
                        }
                    }

                    is Route.Montage -> {
                        NavEntry(key) {
                            MontageScreen()
                        }
                    }

                    is Route.Calendar -> {
                        NavEntry(key) {
                            CalendarScreen()
                        }
                    }

                    is Route.Edit -> {
                        NavEntry(key) {
                            EditScreenRoute(
                                videoUri = key.videoUri,
                                date = key.date,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }

                    else -> error("Unknown NavKey: $key")
                }
            },
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None }
        )
    }
}