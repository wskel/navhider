package dev.wskel.navhider

import android.content.ContentResolver
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import dev.wskel.navhider.ui.theme.NavHiderTheme
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

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val initialGestureHintState = context.contentResolver.isGestureHintEnabled()
    val isGestureHintEnabled = remember { mutableStateOf(initialGestureHintState) }

    val swipeRefreshState = remember { SwipeRefreshState(isRefreshing = false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                scope.launch {
                    swipeRefreshState.isRefreshing = true
                    delay(2000)
                    swipeRefreshState.isRefreshing = false

                    val message = if (Settings.System.canWrite(context)) {
                        isGestureHintEnabled.value = context.contentResolver.isGestureHintEnabled()
                        context.getString(R.string.permission_granted)
                    } else {
                        context.getString(R.string.permission_not_granted)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Switch(
                    checked = isGestureHintEnabled.value,
                    onCheckedChange = { enabled ->
                        isGestureHintEnabled.value = enabled
                        context.contentResolver.setNavigationBarGestureHint(enabled)
                    },
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = if (isGestureHintEnabled.value)
                        stringResource(R.string.gesture_hint_enabled)
                    else stringResource(R.string.gesture_hint_disabled),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

private fun ContentResolver.isGestureHintEnabled() =
    TRUE == Settings.Global.getInt(this, NAV_BAR_GESTURE_HINT, TRUE)

private fun ContentResolver.setNavigationBarGestureHint(enabled: Boolean) {
    try {
        val value = if (enabled) TRUE else FALSE
        Settings.Global.putInt(this, NAV_BAR_GESTURE_HINT, value)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}