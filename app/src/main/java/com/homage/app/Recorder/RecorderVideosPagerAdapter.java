package com.homage.app.recorder;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.player.VideoPlayerActivity;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;

public class RecorderVideosPagerAdapter
        extends PagerAdapter
        implements
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private String TAG = "TAG_"+getClass().getName();

    private Context context;
    private Remake remake;
    private Scene scene;
    private Story story;
    public int sceneID;
    private boolean startedPlaying;

    private View videoSceneContainer;
    private View videoStoryContainer;

    public RecorderVideosPagerAdapter(Context context, Remake remake)
    {
        super();
        this.startedPlaying = false;
        this.context = context;
        this.remake = remake;
        this.sceneID = 1;
        this.story = remake.getStory();
        updateScene();
    }

    private void updateScene() {
        this.scene = story.findScene(sceneID);
        if (videoSceneContainer != null) {
            AQuery aq = new AQuery(videoSceneContainer);
            //VideoView videoView = (VideoView)aq.id(R.id.videoView).getView();
            //videoView.setVideoURI(Uri.parse(scene.videoURL));
            aq.id(R.id.videoThumbnailImage).image(scene.thumbnailURL, true, true, 200, R.drawable.glass_dark);
        }
    }

    public void hideSurfaces() {
        AQuery aq;
        //VideoView videoView;

        aq = new AQuery(this.videoSceneContainer);
        aq.visibility(View.GONE);
        //videoView = (VideoView)aq.id(R.id.videoView).getView();
        //videoView.suspend();

        aq = new AQuery(this.videoStoryContainer);
        aq.visibility(View.GONE);
        //videoView = (VideoView)aq.id(R.id.videoView).getView();
        //videoView.suspend();
    }

    public void showSurfaces () {
        AQuery aq;
        //VideoView videoView;

        aq = new AQuery(this.videoSceneContainer);
        aq.visibility(View.VISIBLE);
        //videoView = (VideoView)aq.id(R.id.videoView).getView();
        //videoView.resume();

        aq = new AQuery(this.videoStoryContainer);
        aq.visibility(View.VISIBLE);
        //videoView = (VideoView)aq.id(R.id.videoView).getView();
        //videoView.resume();
    }

    @Override
    public void notifyDataSetChanged() {
        updateScene();
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == (View)object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Integer pos = new Integer(position);
        View videoContainer = inflater.inflate(R.layout.video_container_view, null);
        container.addView(videoContainer);
        AQuery aq = new AQuery(videoContainer);

        // Set the video container UI
        aq.id(R.id.videoPlayButton).tag(pos);
        aq.id(R.id.videoPlayButton).clicked(onPressedBigPlayButton);
        //VideoView videoView = (VideoView)aq.id(R.id.videoView).getView();
        //videoView.setOnPreparedListener(this);
        //videoView.setOnErrorListener(this);
        //videoView.setOnCompletionListener(this);


        if (position == 0) {
            aq.id(R.id.videoTitle).text(R.string.video_title_scene);
            //videoView.setVideoURI(Uri.parse(scene.videoURL));
            aq.id(R.id.videoThumbnailImage).image(scene.thumbnailURL, true, true, 200, R.drawable.glass_dark);
            videoSceneContainer = videoContainer;
        } else {
            aq.id(R.id.videoTitle).text(R.string.video_title_story);
            //videoView.setVideoURI(Uri.parse(story.video));
            aq.id(R.id.videoThumbnailImage).image(story.thumbnail, true, true, 200, R.drawable.glass_dark);
            videoStoryContainer = videoContainer;
        }

        return videoContainer;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof View) {
            container.removeView((View)object);
        }
    }

    private void startPlayingVideoInVideoContainer(View videoContainer) {
        /*
        AQuery aq = new AQuery(videoContainer);
        aq.id(R.id.videoPlayButton).visibility(View.GONE);
        aq.id(R.id.videoThumbnailImage).visibility(View.GONE);
        aq.id(R.id.videoTitle).visibility(View.GONE);
        final VideoView videoView = (VideoView)aq.id(R.id.videoView).getView();
        videoView.setZOrderOnTop(true);
        this.startedPlaying = true;
        videoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                videoView.setAlpha(1);
                videoView.setHovered(true);
                videoView.requestFocus();
                videoView.start();

            }
        }, 200);
        */
    }

    private void doneWithPlayingVideoInVideoContainer(View videoContainer) {
        /*
        AQuery aq = new AQuery(videoContainer);
        aq.id(R.id.videoPlayButton).visibility(View.VISIBLE);
        aq.id(R.id.videoThumbnailImage).visibility(View.VISIBLE);
        aq.id(R.id.videoTitle).visibility(View.VISIBLE);
        VideoView videoView = (VideoView)aq.id(R.id.videoView).getView();
        videoView.setAlpha(0);
        videoView.setZOrderOnTop(false);
        videoView.seekTo(0);
        videoView.pause();
        */
    }

    public void done() {
        /*
        doneWithPlayingVideoInVideoContainer(videoSceneContainer);
        doneWithPlayingVideoInVideoContainer(videoStoryContainer);
        this.startedPlaying = false;
        */
    }

    public void doneIfPlaying() {
        if (startedPlaying) done();
    }

    //region *** Video events handlers ***
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        done();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        Toast.makeText(context, String.format("Error with video %d %d", i, i2), Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Toast.makeText(context, String.format("Video prepared %s", mediaPlayer.getTrackInfo().toString()), Toast.LENGTH_SHORT).show();
        Log.d(TAG, String.format("Video is prepared for playing: %s", mediaPlayer.getTrackInfo().toString()));
    }
    //endregion

    //region *** UI event handlers ***
    /**
     *  ==========================
     *      UI event handlers.
     *  ==========================
     */
    View.OnClickListener onPressedBigPlayButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AQuery aq = new AQuery(view);

            // Open video player.
            Intent myIntent = new Intent(context, VideoPlayerActivity.class);
            Bundle b = new Bundle();

            Integer pos = (Integer)aq.getTag();
            if (pos == 0) {
                Log.d(TAG, "Clicked our scene video button");
                b.putString("videoFileURL", scene.videoURL);
            } else {
                Log.d(TAG, "Clicked our story video button");
                b.putString("videoFileURL", story.video);
            }
            myIntent.putExtras(b);
            context.startActivity(myIntent);





            // Deprecated. Will be implemented with a single video player instead of embedded ones.
            /*
            // TODO: Allow only to play prepared videos
            AQuery aq = new AQuery(view);
            Integer pos = (Integer)aq.getTag();
            if (pos == 0) {
                Log.d(TAG, "Clicked our scene video button");
            } else {
                Log.d(TAG, "Clicked our story video button");
            }
            View videoContainer = aq.parent(R.id.videoContainer).getView();
            startPlayingVideoInVideoContainer(videoContainer);
            */



        }
    };
    //endregion
}
