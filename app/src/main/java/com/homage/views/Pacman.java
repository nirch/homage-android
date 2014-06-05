package com.homage.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import com.homage.app.R;

public class Pacman extends View {
    private String TAG = "TAG_"+getClass().getName();

    public float mCurrAngle = 0;
    float mSweepAngle = 0;

    public Pacman(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    public Pacman(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public Pacman(Context context)
    {
        super(context);
        init();
    }

    private void init() {
        mCurrAngle = 0;
        mSweepAngle = 360;
        if (isInEditMode()) {
            setVisibility(VISIBLE); // Visible by default in edit mode
        } else {
            setVisibility(INVISIBLE); // Invisible by default
        }

    }

    /* Here we override onDraw */
    @Override
    protected void onDraw(final Canvas canvas) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF oval = new RectF(canvas.getClipBounds());
        RectF oval2 = new RectF(canvas.getClipBounds());

        oval.inset(5.0f,5.0f);
        oval2.inset(7.0f,7.0f);

        p.setColor(Color.GRAY);
        p.setStrokeWidth(10.0f);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawOval(oval2, p);

        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.FILL);
        canvas.drawOval(oval, p);

        p.setColor(Color.GRAY);
        p.setStrokeWidth(6.0f);
        if (isInEditMode()) {
            canvas.drawArc(oval, -90, 80, true, p);
        } else {
            canvas.drawArc(oval, -90, mCurrAngle, true, p);
        }
    }

    public Animation startOneSecondAnimation() {
        Animation anim = new OpenPacman(0 , 360, 1000);
        this.startAnimation(anim);
        return anim;
    }

    /* Here we define our nested custom animation */
    public class OpenPacman extends Animation {
        float mStartAngle;
        float mSweepAngle;

        public OpenPacman (int startAngle, int sweepAngle, long duration) {
            mStartAngle = startAngle;
            mSweepAngle = sweepAngle;
            setDuration(duration);
            setInterpolator(new LinearInterpolator());
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float currAngle = mStartAngle + ((mSweepAngle - mStartAngle) * interpolatedTime);
            Pacman.this.mCurrAngle = currAngle; //negative for counterclockwise animation.
            Pacman.this.invalidate();
        }
    }
}