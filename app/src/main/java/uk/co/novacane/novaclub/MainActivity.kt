package uk.co.novacane.novaclub

import android.os.Bundle
// Use our Nova theme (NovaTheme.kt), not Android Studio's purple starter theme
import com.novacane.novaclub.NovaClubTheme
// Makes the notification channels when the app opens (NovaNotifications.kt)
import com.novacane.novaclub.createNotificationChannels

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.novacane.novaclub.NovaClubApp

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}


@Composable
fun AssistChipExample() {
    AssistChip(
        onClick = { Log.d("Assist chip", "hello world") },
        label = { Text("HELLO THERE") },
        leadingIcon = {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Localized description",
                Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NovaClubTheme {
        Greeting("Android")
    }

}

// The first thing Android opens when the app starts
class MainActivity : ComponentActivity() {
    // Runs once when the app opens
    override fun onCreate(savedInstanceState: Bundle?) {
        // Do Android's normal start-up work first
        super.onCreate(savedInstanceState)
        // Use the full screen, edge to edge, with light status-bar icons even when the phone is in light mode (the app is always space-dark)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        // Set up the notification channels (flash sales), so notifications have somewhere to go
        createNotificationChannels(this)
        // Put our UI on screen
        setContent {
            // Wrap everything in the Nova colours
            NovaClubTheme {
                // Draw the app
                NovaClubApp()
            }
        }
    }
}