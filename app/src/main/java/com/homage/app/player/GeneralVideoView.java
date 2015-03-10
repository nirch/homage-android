package com.homage.app.player;


import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by dangal on 3/10/15.
 */
public class GeneralVideoView extends VideoView implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnInfoListener {

    long lastPausedTime  = 0; // The time of the last pause (milliseconds)
    long totalPausedTime = 0; // The total time paused (milliseconds)

    public GeneralVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
    }

    @Override
    public void pause() {
        lastPausedTime = System.currentTimeMillis();

        super.pause();
    }

    @Override
    public void start() {
        if (lastPausedTime != 0) {
            totalPausedTime += System.currentTimeMillis() - lastPausedTime;
        }
        super.start();
    }

    public long getTotalTimeMillis() {
        return totalPausedTime;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

    }
}
