package com.homage.CustomAdapters;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ScrollView;

/**
 * Created by dangalg on 12/2/2014.
 */
public class ObservableScrollView extends ScrollView {
    private OnOverScrolledListener scrollViewListener = null;
    private static final int MAX_Y_OVERSCROLL_DISTANCE = 100;
    private int mMaxYOverscrollDistance;
    private Context pcontext;

    public ObservableScrollView(Context context) {
        super(context);
        pcontext = context;
    }

    public ObservableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        pcontext = context;
    }

    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnOverScrolledListener(Context context, OnOverScrolledListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
//        initBouncyScrollView(context);
        pcontext = context;
    }

//    Make scroll view overscroll bouncy
    public void initBouncyScrollView(Context context)
    {
        //get the density of the screen and do some maths with it on the max overscroll distance
        //variable so that you get similar behaviors no matter what the screen size
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final float density = metrics.density;
        mMaxYOverscrollDistance = (int) (density * MAX_Y_OVERSCROLL_DISTANCE);
    }

//    When over scrolled fire this!
    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if (scrollViewListener != null) {
            scrollViewListener.onOverScrolled(this, scrollX, scrollY, clampedX, clampedY);
        }
    }

//    How much to over scroll by to create bouncy overscroll effect!
    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent)
    {
        if(scrollY <= 0)
        {
            mMaxYOverscrollDistance = 0;
        }
        else {
            initBouncyScrollView(pcontext);
        }
        //This is where the magic happens, we have replaced the incoming maxOverScrollY with our own custom variable mMaxYOverscrollDistance;
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, mMaxYOverscrollDistance, isTouchEvent);
    }
}
