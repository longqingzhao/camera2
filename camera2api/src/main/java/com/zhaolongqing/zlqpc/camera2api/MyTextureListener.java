package com.zhaolongqing.zlqpc.camera2api;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

public class MyTextureListener implements TextureView.SurfaceTextureListener {

    private ControlCamera controlCamera;

    MyTextureListener(ControlCamera controlCamera) {
        this.controlCamera = controlCamera;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        controlCamera.openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
