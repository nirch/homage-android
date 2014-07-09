package com.homage.app.recorder;

import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

class OnDraggingControlsDrawerListener implements View.OnTouchListener
{
    private String TAG = "TAG_OnDraggingControlsDrawerListener";

    float startPosTouch, deltaTouch, startPosView, newPosView;
    float heightForClosing;
    RecorderActivity recorderActivity;

    public OnDraggingControlsDrawerListener(RecorderActivity recorderActivity) {
        super();
        this.recorderActivity = recorderActivity;
    }

    @Override
    public boolean onTouch(final View v,final MotionEvent event)
    {
         switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                heightForClosing = recorderActivity.getHeightForClosingDrawer();
                startPosTouch = event.getRawY();
                startPosView = recorderActivity.getControlsDrawerPosition();
            }
            case MotionEvent.ACTION_MOVE:
            {
                deltaTouch = startPosTouch - event.getRawY();
                newPosView = startPosView - deltaTouch;
                if (newPosView < 0) newPosView = 0;
                if (newPosView > recorderActivity.viewHeightForClosingControlsDrawer)
                    newPosView = recorderActivity.viewHeightForClosingControlsDrawer;
                recorderActivity.setControlsDrawerPosition(newPosView);
            }
            case MotionEvent.ACTION_UP:
            {
                deltaTouch = startPosTouch - event.getRawY();
                newPosView = startPosView - deltaTouch;
                if (newPosView < heightForClosing / 2) {
                    recorderActivity.openControlsDrawer(true);
                } else {
                    recorderActivity.closeControlsDrawer(true);
                }
            }
        }
        return true;
    }
}