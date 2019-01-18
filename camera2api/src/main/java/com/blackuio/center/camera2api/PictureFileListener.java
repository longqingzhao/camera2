package com.blackuio.center.camera2api;

import java.io.File;

public interface PictureFileListener {

    void error();
    void success(File file);

}
