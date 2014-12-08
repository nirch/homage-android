package com.homage.FileHandler;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.homage.app.recorder.RecorderActivity;
import com.homage.matting.Matting;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;
import com.vim.vimapi.vTool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by dangalg on 11/27/2014.
 */
public class ContourHandler {

    private String contourLocalUrl;

    public void DownloadContour(RecorderActivity recorderActivity, Remake remake, String folderName) {
        new DownloadContourAsync(recorderActivity, remake, folderName).execute();
    }

    private class DownloadContourAsync extends AsyncTask<Void, Void, String> {

        Remake remake;
        String folderName;
        RecorderActivity recorderActivity;


        public DownloadContourAsync(RecorderActivity precorderActivity, Remake premake, String pfolderName) {
            super();
            // do stuff
            remake = premake;
            folderName = pfolderName;
            recorderActivity = precorderActivity;
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                //Get contour url from scene and download contour
                Story story = remake.getStory();
                Scene scene = story.findScene(remake.lastSceneID());

                if (scene.contourURL != null) {
                    String[] splitURL = scene.contourURL.split("/");
                    String endOfContourURL = splitURL[splitURL.length - 1];
                    File storagePath = new File(recorderActivity.getFilesDir(), endOfContourURL);
                    String contourLocalUrl = storagePath + folderName + endOfContourURL;
                    File contourFile = new File(contourLocalUrl);
                    if (!contourFile.exists()) {

                        URL url = null;

                        try {
                            url = new URL(scene.contourURL);
                            Download.CreateFolderInLocalStorage(storagePath+folderName);
                            Download.WriteFileToStorage(contourLocalUrl, url);
                        } catch (MalformedURLException e) {
                            Log.d("MalformedURLException: ", e.toString());
                            return null;
                        }

                    }
                    return contourLocalUrl;
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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            recorderActivity.contourLocalUrl = s;
        }
    }
}
