package com.homage.FileHandler;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.homage.app.Download.DownloadForAsyncTask;
import com.homage.app.R;
import com.homage.app.Utils.constants;
import com.homage.networking.server.HomageServer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by dangalg on 1/25/2015.
 */
public class VideoHandler {

    private static int tempFileNumber = 0;

    public void CreateCachedVideo(Context context, HashMap<String, String> info) {
        new DownloadVideoAsync(context, info).execute();
    }

    public class DownloadVideoAsync extends AsyncTask<Void, Integer, String> {

        String videoURL;
        String localFileUrlName;
        Context context;
        boolean shareVideo;
        HashMap<String,String> videoinfo;
        File outFile;
        private Progress progress;
        URL url;
        ProgressDialog pd;
        boolean inbackground;
        String downloadStatus;


        public DownloadVideoAsync(Context pcontext, HashMap<String,String> info) {
            super();
            // do stuff
            context = pcontext;

            videoinfo = info;
            shareVideo = Boolean.valueOf(videoinfo.get(constants.SHARE_VIDEO));
            videoURL = videoinfo.get(constants.VIDEO_URL);
            localFileUrlName = videoinfo.get(constants.LOCAL_FILE_NAME);
            inbackground = Boolean.valueOf(videoinfo.get(constants.DOWNLOAD_IN_BACKGROUND));

            this.progress = new Progress(this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(!inbackground) {
                pd = new ProgressDialog(context);
                Resources res = context.getResources();
                pd.setTitle(res.getString(R.string.pd_title_please_wait));
                pd.setMessage(res.getString(R.string.pd_msg_sharing_movie));
                pd.setCancelable(true);
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setMax(100);
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        // Do something...
                        DownloadVideoAsync.this.cancel(true);
                    }
                });
                pd.show();
            }
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                //Get contour url from scene and download contour


                if (videoURL != null) {


                        try {
                            url = new URL(videoURL);
                            File cacheDir = context.getCacheDir();
                            outFile = new File(cacheDir, localFileUrlName);
                            File tempFile = new File(cacheDir, "tempVideo" + ++tempFileNumber);


                            if(!inbackground) {
                                downloadStatus = DownloadForAsyncTask.WriteFileToStorage(progress, outFile, tempFile, url);
                            }
                            else{
                                downloadStatus = DownloadForAsyncTask.WriteFileToStorage(null, outFile, tempFile, url);
                            }
                            if(downloadStatus == "Error"){
                                pd.setMessage("There was an error with your download, please try again later");
                            }
                        } catch (MalformedURLException e) {
                            Log.d("MalformedURLException: ", e.toString());
                            return null;
                        }

                    return outFile.getAbsolutePath();
                } else {
                    return null;
                }
            }
            catch(Exception ex){
                Log.d("Contour Handler exception: ",  ex.getMessage().toString());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if(pd != null) {
                pd.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(String outfileAbsolutePath) {
            super.onPostExecute(outfileAbsolutePath);

            if(shareVideo){
                Intent intent = new Intent(HomageServer.INTENT_USER_SHARED_VIDEO);
                intent.putExtra(constants.VIDEO_INFO, videoinfo);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
            if(pd != null) {
                pd.dismiss();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if(pd != null) {
                pd.dismiss();
            }
        }

        public class Progress {
            private DownloadVideoAsync task;
            public boolean canceled = false;

            public Progress(DownloadVideoAsync task) {
                this.task = task;
            }

            public void publish(int val) {
                task.publishProgress(val);
            }
        }
    }
}
