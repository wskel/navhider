package dev.wskel.navhider

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.wskel.navhider.ui.theme.NavHiderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val NAV_BAR_GESTURE_HINT = "navigation_bar_gesture_hint"
private const val TRUE = 1
private const val FALSE = 0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NavHiderTheme {
                MainScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NavHiderTheme {
        MainScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val initialGestureHintState = context.contentResolver.isGestureHintEnabled()

    val isGestureHintEnabled = remember { mutableStateOf(initialGestureHintState) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isRefreshing = remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    val onRefreshLocal = onRefresh(
        context,
        isGestureHintEnabled,
        scope,
        snackbarHostState,
        pullRefreshState
    )

    Scaffold(
        snackbarHost = { MessageSnackBarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing.value,
            onRefresh = onRefreshLocal,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center
            ) {
                item {
                    GestureHintControl(context, isGestureHintEnabled, scope, snackbarHostState)
                }
            }

        }
    }
}

@Composable
private fun MessageSnackBarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(snackbarHostState) { data ->
        Snackbar(
            dismissAction = {
                TextButton(onClick = { data.dismiss() }) {
                    Text(stringResource(R.string.button_dismiss))
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(data.visuals.message)
        }
    }
}

@Composable
private fun GestureHintControl(
    context: Context,
    isGestureHintEnabled: MutableState<Boolean>,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Switch(
            checked = isGestureHintEnabled.value,
            onCheckedChange = { enabled ->
                isGestureHintEnabled.value = enabled
                context.contentResolver.setNavigationBarGestureHint(
                    context,
                    enabled,
                    scope,
                    snackbarHostState
                )
            },
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = if (isGestureHintEnabled.value)
                stringResource(R.string.gesture_hint_enabled)
            else stringResource(R.string.gesture_hint_disabled),
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun onRefresh(
    context: Context,
    isGestureHintEnabled: MutableState<Boolean>,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    pullRefreshState: PullToRefreshState
): () -> Unit = {
    isGestureHintEnabled.value = context.contentResolver.isGestureHintEnabled()

    val message = if (context.hasWriteSecureSettingsPermission()) {
        context.getString(R.string.permission_granted)
    } else {
        context.getString(R.string.permission_not_granted)
    }

    scope.run {
        launch { snackbarHostState.showSnackbarWithDismissal(message) }
        launch {
            delay(100)
            pullRefreshState.animateToHidden()
        }
    }
}

suspend fun SnackbarHostState.showSnackbarWithDismissal(
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    currentSnackbarData?.dismiss()
    showSnackbar(message, duration = duration)
}

private fun ContentResolver.isGestureHintEnabled() =
    TRUE == Settings.Global.getInt(this, NAV_BAR_GESTURE_HINT, TRUE)

private fun Context.hasWriteSecureSettingsPermission(): Boolean {
    val permissionCheckResult =
        checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
    return permissionCheckResult == PackageManager.PERMISSION_GRANTED
}

private fun ContentResolver.setNavigationBarGestureHint(
    context: Context,
    enabled: Boolean,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    fun showSnackbarError(message: String) = scope.launch {
        snackbarHostState.showSnackbarWithDismissal(message, SnackbarDuration.Indefinite)
    }

    try {
        val value = if (enabled) TRUE else FALSE
        Settings.Global.putInt(this, NAV_BAR_GESTURE_HINT, value)
    } catch (ex: Exception) {
        Log.e("NavigationBarGesture", "Error setting navigation bar gesture hint", ex)
        when (ex) {
            is SecurityException -> showSnackbarError(
                context.getString(R.string.error_hide_gesture_hint, context.packageName)
            )

            is RemoteException -> showSnackbarError(
                context.getString(R.string.error_something_went_wrong)
            )

            else -> throw ex
        }
    }
}