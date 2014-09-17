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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;


/**
 * Layout that adjusts to maintain a specific aspect ratio.
 */
public class AspectFrameLayout extends FrameLayout {
    private static final String TAG = "TAG_GRAPHICA_AFL";

    private double mTargetAspect = -1.0;        // initially use default window size
    private double mOriginalAspectRatio = -1.0;
    private int cropBarsSize = 0;
    private View topBlackBar;
    private View bottomBlackBar;

    public AspectFrameLayout(Context context) {
        super(context);
    }

    public AspectFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCroppingBarsViews(View topBlackBar, View bottomBlackBar) {
        this.topBlackBar = topBlackBar;
        this.bottomBlackBar = bottomBlackBar;
    }

    /**
     * Sets the desired aspect ratio.  The value is <code>width / height</code>.
     */
    public void setOriginalAspectRatio(double originalAspectRatio) {
        if (originalAspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        mOriginalAspectRatio = originalAspectRatio;
    }
    public void setAspectRatio(double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        Log.d(TAG, "Setting aspect ratio to " + aspectRatio + " (was " + mTargetAspect + ")");
        if (mTargetAspect != aspectRatio) {
            mTargetAspect = aspectRatio;
            requestLayout();
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure target=" + mTargetAspect +
                " width=[" + MeasureSpec.toString(widthMeasureSpec) +
                "] height=[" + View.MeasureSpec.toString(heightMeasureSpec) + "]");

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // Target aspect ratio will be < 0 if it hasn't been set yet.  In that case,
        // we just use whatever we've been handed.
        if (mTargetAspect > 0) {
            // factor the padding out
            int horizPadding = getPaddingLeft() + getPaddingRight();
            int vertPadding = getPaddingTop() + getPaddingBottom();
            width -= horizPadding;
            height -= vertPadding;

            double viewAspectRatio = (double) width / height;
            double aspectDiff = mTargetAspect / viewAspectRatio - 1;

            cropBarsSize = 0;
            if (aspectDiff > 0) {
                // limited by narrow width; restrict height
                height = (int) (width / mTargetAspect);
            } else {
                // limited by short height; restrict width
                width = (int) (height * mTargetAspect);
            }
            Log.d(TAG, "new size=" + width + "x" + height + " + padding " + horizPadding + "x" + vertPadding);

            width += horizPadding;
            height += vertPadding;

            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        // Stretch video vertically and crop it in case in need to maintain aspect ratio of the original video
        Log.d(TAG, String.format("Original video aspect ratio:%f    requested aspect ration:%f", mOriginalAspectRatio, mTargetAspect));
        if (!Double.isNaN(mOriginalAspectRatio) && mOriginalAspectRatio < mTargetAspect) {
            Log.d(TAG, "Need to stretch video vertically to maintain aspect ratio of the original video");
            int stretchedHeight = (int)((double)width/mOriginalAspectRatio);
            Log.v(TAG, String.format("Cropping is required. Stretch to height:%d and cropped.", stretchedHeight));
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(stretchedHeight, MeasureSpec.EXACTLY);

            // Crop if needed
            if (topBlackBar != null && bottomBlackBar != null) {
                View parent = (View)getParent();
                int psize = parent.getMeasuredHeight();
                int barsSize = (psize - height)/2;
                Log.d(TAG, String.format("cropping bars size:%d", barsSize));
                if (barsSize>10 && barsSize < 200) {
                    updateCroppingBlackBars(barsSize);
                    topBlackBar.setVisibility(View.VISIBLE);
                    bottomBlackBar.setVisibility(View.VISIBLE);
                } else {
                    topBlackBar.setVisibility(View.GONE);
                    bottomBlackBar.setVisibility(View.GONE);
                }
            }
        }

        Log.d(TAG, "set width=[" + MeasureSpec.toString(widthMeasureSpec) + "] height=[" + View.MeasureSpec.toString(heightMeasureSpec) + "]");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void updateCroppingBlackBars(int h) {
        android.widget.RelativeLayout.LayoutParams params;
        params = (android.widget.RelativeLayout.LayoutParams) topBlackBar.getLayoutParams();
        params.height = h;
        topBlackBar.setLayoutParams(params);

        params = (android.widget.RelativeLayout.LayoutParams) bottomBlackBar.getLayoutParams();
        params.height = h;
        bottomBlackBar.setLayoutParams(params);
    }


}
