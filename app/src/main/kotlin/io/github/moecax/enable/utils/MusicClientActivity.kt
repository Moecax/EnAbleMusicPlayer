package io.github.moecax.enable.utils

import androidx.appcompat.app.AppCompatActivity
import io.github.moecax.enable.services.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

abstract class MusicClientActivity: AppCompatActivity(), CoroutineScope, MusicService.MusicClient {

    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    override fun onResume() {
        super.onResume()
        launch(Dispatchers.Default) {
            MusicService.registerClient(this@MusicClientActivity)
        }
    }

    override fun onPause() {
        super.onPause()
        launch(Dispatchers.Default) {
            MusicService.unregisterClient(this@MusicClientActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
        MusicService.unregisterClient(this@MusicClientActivity)
    }
}