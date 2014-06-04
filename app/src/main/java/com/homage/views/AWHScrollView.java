package com.homage.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.HorizontalScrollView;

public class AWHScrollView extends HorizontalScrollView {
    String TAG = "TAG_" + getClass().getName();

    OnBottomReachedListener onBottomReachedListener;

    private int scrollState;
    private int lastDelta;

    public AWHScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initOnTouchListener();
    }

    public AWHScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initOnTouchListener();
    }

    public AWHScrollView(Context context) {
        super(context);
        initOnTouchListener();
    }

    private void initOnTouchListener() {
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_SCROLL:
                    case MotionEvent.ACTION_MOVE:
                        setScrollState(AbsListView.OnScrollListener.SCROLL_STATE_FLING);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        setScrollState(AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        setScrollState(AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
                        fixPaging();
                        break;
                }
                return false;
            }
        });
    }

    private void setScrollState(int scrollState) {
        this.scrollState = scrollState;
    }

    private void fixPaging() {
        int newX;
        if (lastDelta>10)
            newX = getWidth();
        else if (lastDelta<-10)
            newX = 0;
        else if (getScaleX() < getWidth()/2)
            newX = 0;
        else
            newX = getWidth();

        final ObjectAnimator animScroll = ObjectAnimator.ofInt(this, "scrollX", newX);
        animScroll.setDuration(500);
        animScroll.start();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        lastDelta = l-oldl;
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public OnBottomReachedListener getOnBottomReachedListener() {
        return onBottomReachedListener;
    }

    public void setOnBottomReachedListener(
            OnBottomReachedListener onBottomReachedListener) {
        this.onBottomReachedListener = onBottomReachedListener;
    }

    public interface OnBottomReachedListener{
        public void onBottomReached();
    }

}
