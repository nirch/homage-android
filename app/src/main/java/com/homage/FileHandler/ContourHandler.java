package com.homage.FileHandler;

import android.os.AsyncTask;
import android.util.Log;

import com.homage.app.Download.DownloadForAsyncTask;
import com.homage.app.recorder.RecorderActivity;
import com.homage.model.Remake;
import com.homage.model.Scene;
import com.homage.model.Story;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by dangalg on 11/27/2014.
 */
public class ContourHandler {
    private static int tempFileNumber = 0;

    private String contourLocalUrl;

    public void DownloadContour(RecorderActivity recorderActivity, Remake remake, String folderName) {
        Story story = remake.getStory();
        Scene scene = story.findScene(remake.lastSceneID());
        new DownloadContourAsync(recorderActivity, scene.contourURL, folderName).execute();
    }

    private class DownloadContourAsync extends AsyncTask<Void, Void, String> {

        String contourURL;
        String folderName;
        RecorderActivity recorderActivity;


        public DownloadContourAsync(RecorderActivity precorderActivity, String contourUrl, String pfolderName) {
            super();
            // do stuff
            contourURL = contourUrl;
            folderName = pfolderName;
            recorderActivity = precorderActivity;
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                //Get contour url from scene and download contour


                if (contourURL != null) {
//                    String[] splitURL = contourURL.split("/");
//                    String endOfContourURL = splitURL[splitURL.length - 1];
//                    File storagePath = new File(recorderActivity.getFilesDir(), endOfContourURL);
//                    String contourLocalUrl = endOfContourURL;
//                    File contourFile = new File(contourLocalUrl);
                    String contourLocalUrl = getLocalVideoFile(contourURL);
                    File cacheDir = recorderActivity.getCacheDir();
                    File outFile = new File(cacheDir, contourLocalUrl);
                    File tempFile = new File(cacheDir, "tempVideo" + ++tempFileNumber);
                    if (!outFile.exists()) {

                        URL url = null;

                        try {
                            url = new URL(contourURL);
//                            Download.CreateFolderInLocalStorage(storagePath+folderName);



                            DownloadForAsyncTask.WriteFileToStorage(null, outFile, tempFile, url);

                        } catch (MalformedURLException e) {
                            Log.d("MalformedURLException: ", e.toString());
                            return null;
                        }

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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            recorderActivity.contourLocalUrl = s;
        }
    }

    private String getLocalVideoFile(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
        fileName = fileName.replace("%20", " ");
        String fileLocalUrl = fileName;
        return fileLocalUrl;
    }
}
