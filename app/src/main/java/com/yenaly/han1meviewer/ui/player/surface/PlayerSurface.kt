package com.yenaly.han1meviewer.ui.player.surface

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.ui.player.HanimePlayerController

@Composable
fun rememberMovablePlayerSurface(
    controller: HanimePlayerController,
): @Composable (Modifier) -> Unit {
    return remember(controller) {
        var savedSurfaceTexture: SurfaceTexture? = null
        movableContentOf { modifier: Modifier ->
            AndroidView(
                factory = { context ->
                    TextureView(context).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {

                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                val saved = savedSurfaceTexture
                                if (saved == null) {
                                    savedSurfaceTexture = surfaceTexture
                                    controller.attachSurfaceTexture(surfaceTexture)
                                } else if (saved !== surfaceTexture) {

                                    setSurfaceTexture(saved)
                                }
                                controller.onSurfaceSizeChanged(width, height)
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                controller.onSurfaceSizeChanged(width, height)
                            }

                            override fun onSurfaceTextureDestroyed(
                                surfaceTexture: SurfaceTexture,
                            ): Boolean {

                                return false
                            }

                            override fun onSurfaceTextureUpdated(
                                surfaceTexture: SurfaceTexture,
                            ) = Unit
                        }
                    }
                },
                modifier = modifier,
            )
        }
    }
}
