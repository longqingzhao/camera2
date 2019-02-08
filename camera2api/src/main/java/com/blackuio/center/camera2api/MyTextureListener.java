package com.blackuio.center.camera2api;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

public class MyTextureListener implements TextureView.SurfaceTextureListener {

    private static final String TAG = "MyTextureListener";
    private ControlCamera controlCamera;

    MyTextureListener(ControlCamera controlCamera) {
        this.controlCamera = controlCamera;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.g(TAG, "onSurfaceTextureAvailable");
        controlCamera.openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.g(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.g(TAG, "onSurfaceTextureDestroyed");//
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        Log.g(TAG, "onSurfaceTextureUpdated");
    }
}
