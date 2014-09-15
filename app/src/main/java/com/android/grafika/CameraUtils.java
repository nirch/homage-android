/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.grafika;

import android.hardware.Camera;
import android.util.Log;

import java.util.List;

/**
 * Camera-related utility functions.
 */
public class CameraUtils {
    private static final String TAG = "TAG_CameraUtils";

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     * <p>
     * TODO: should do a best-fit match, e.g.
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preview size for video chosen " + width + "x" + height);
        }

        //for (Camera.Size size : parms.getSupportedPreviewSizes()) {
        //    Log.d(TAG, "supported: " + size.width + "x" + size.height);
        //}

        List <Camera.Size> sizesList = parms.getSupportedPreviewSizes();
        for (Camera.Size size : sizesList) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(size.width, size.height);
                return;
            }
        }

        // Unable to choose the preffered size.
        // Choose the camera's default size instead.
        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        Log.d(TAG, "Set to default size of " + ppsfv.width + "x" + ppsfv.height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
    }

    /**
     * Chooses the fps closest to the desired settings.
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static void chooseBestFPSRange(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();
        long minDistance = Long.MAX_VALUE;
        int[] chosenFPSRange = supported.get(0);
        for (int[] r : supported) {
            long distance = Math.abs(r[0]-desiredThousandFps) + Math.abs(r[1]-desiredThousandFps);
            if (distance < minDistance) {
                chosenFPSRange = r;
                minDistance = distance;
            }
        }
        parms.setPreviewFpsRange(chosenFPSRange[0], chosenFPSRange[1]);
    }
}
