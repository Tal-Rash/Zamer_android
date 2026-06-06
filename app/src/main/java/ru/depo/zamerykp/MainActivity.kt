package ru.depo.zamerykp

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape
import ru.depo.zamerykp.ui.AppRoot
import ru.depo.zamerykp.ui.AppViewModel
import ru.depo.zamerykp.ui.AppViewModelFactory

class MainActivity : ComponentActivity() {
    private var audioManager: AudioManager? = null
    private var previousMusicVolume: Int? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val container = (application as ZameryKpApp).container
        requestRuntimePermissions()
        setContent {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<AppViewModel>(
                factory = AppViewModelFactory(container)
            )
            val settings by viewModel.settings.collectAsState()
            SideEffect {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            MaterialTheme(
                shapes = Shapes(
                    extraSmall = RoundedCornerShape(8.dp),
                    small = RoundedCornerShape(8.dp),
                    medium = RoundedCornerShape(8.dp),
                    large = RoundedCornerShape(8.dp),
                    extraLarge = RoundedCornerShape(8.dp)
                )
            ) {
                Surface {
                    AppRoot(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        maximizeMusicVolume()
    }

    override fun onStop() {
        restoreMusicVolume()
        super.onStop()
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        permissionLauncher.launch(permissions)
    }

    private fun maximizeMusicVolume() {
        val manager = audioManager ?: return
        if (previousMusicVolume == null) {
            previousMusicVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        manager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
    }

    private fun restoreMusicVolume() {
        val manager = audioManager ?: return
        val previous = previousMusicVolume ?: return
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, previous, 0)
        previousMusicVolume = null
    }
}
