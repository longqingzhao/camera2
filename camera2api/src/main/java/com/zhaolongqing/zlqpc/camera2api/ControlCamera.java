package com.zhaolongqing.zlqpc.camera2api;

import android.app.Activity;

import java.io.File;

public interface ControlCamera {
    void initCamera(Activity activity, AutoFitTextureView autoFitTextureView);

    void openCamera();

    void controlCamera(boolean isRecord);

    File takePicture();

    File stopRecord();

    void closeCamera();

}
