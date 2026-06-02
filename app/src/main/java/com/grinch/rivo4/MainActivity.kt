package com.grinch.rivo4

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.isAlreadyDefaultDialer
import com.grinch.rivo4.view.screen.transitions.AppTransitions
import com.grinch.rivo4.view.screen.transitions.getAppTransition
import com.grinch.rivo4.view.theme.Rivo4Theme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DialPadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactEditScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DefaultDialerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import org.koin.android.ext.koin.androidContext
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {
    private val requestRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(appModule)
            }
        }

        setContent {
            Rivo4Theme {
                val navController = rememberNavController()

                val prefs = koinInject<PreferenceManager>()
                val defBar = prefs.getInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, 0)
                val transitionStyle = prefs.getInt(PreferenceManager.KEY_TRANSITION_STYLE, 0)

                // Needed for app transitions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        navController = navController,
                        defaultTransitions = getAppTransition(transitionStyle)
                    )
                }

                LaunchedEffect(Unit) {
                    if (!isAlreadyDefaultDialer(this@MainActivity)) {
                        navController.navigate(DefaultDialerScreenDestination.route) {
                            popUpTo(ContactScreenDestination.route) {
                                inclusive = true
                            }
                        }
                    } else if (defBar == 0) {
                        navController.navigate(RecentScreenDestination.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }

                LaunchedEffect(intent) {
                    handleIntent(intent, navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
    }

    private fun handleIntent(intent: Intent?, navController: androidx.navigation.NavController) {
        intent ?: return
        val data = intent.data
        val action = intent.action

        when (action) {
            Intent.ACTION_DIAL, Intent.ACTION_VIEW -> {
                if (data?.scheme == "tel") {
                    val number = data.schemeSpecificPart
                    navController.navigate(DialPadScreenDestination(initialNumber = number).route)
                } else if (data?.toString()?.contains("contacts") == true || data?.toString()?.contains("com.android.contacts") == true || intent.hasExtra("contact_id")) {
                    val id = data?.lastPathSegment ?: intent.getStringExtra("contact_id")
                    if (id != null) {
                        navController.navigate(ContactDetailsScreenDestination(contactId = id).route)
                    }
                }
            }
            Intent.ACTION_INSERT -> {
                val name = intent.getStringExtra(ContactsContract.Intents.Insert.NAME)
                val phone = intent.getStringExtra(ContactsContract.Intents.Insert.PHONE)
                navController.navigate(ContactEditScreenDestination(initialName = name, initialPhone = phone).route)
            }
            Intent.ACTION_EDIT -> {
                val id = data?.lastPathSegment
                if (id != null) {
                    navController.navigate(ContactEditScreenDestination(contactId = id).route)
                }
            }
        }
    }
}