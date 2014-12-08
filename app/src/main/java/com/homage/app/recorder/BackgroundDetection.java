package com.homage.app.recorder;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;

import com.homage.FileHandler.Download;
import com.homage.app.R;
import com.homage.app.main.HomageApplication;
import com.homage.matting.Matting;
import com.homage.networking.analytics.HMixPanel;
import com.vim.vimapi.vTool;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

/**
 * Created by dangalg on 11/24/2014.
 */
public class BackgroundDetection {
    Context context;
    RecorderActivity recorderActivity;
    Resources res;
    Matting mat;
    String TAG = "TAG_BackgroundDetection";
    boolean matInitialized = false;

    String folderPath;
    String contourLocalUrl;
    int HardCodedWidth = 640;
    int HardCodedHeight = 360;
    int imageCounter = 0;
    boolean isProductionServer;

    public BackgroundDetection(Context pcontext, Camera pcamera) {
        res = HomageApplication.getContext().getResources();
        context = pcontext;
        recorderActivity = ((RecorderActivity)this.context);
        new StartMatting(pcamera).execute();
        isProductionServer = res.getBoolean(R.bool.is_production_server);
    }

    public void RunTestOnFrame(byte[] pdata, Camera pcamera){
        new TestFrameWithMatting(pdata, pcamera).execute("");
    }

    private class StartMatting extends AsyncTask<Void, Void, Void> {

//        Camera camera;
        int width, height;

        public StartMatting(Camera pcamera) {
            super();
            // do stuff
//            camera = pcamera;

            Camera.Parameters parameters = pcamera.getParameters();
            width = parameters.getPreviewSize().width;
            height = parameters.getPreviewSize().height;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                mat = new Matting();
                mat.logopen(vTool.getExtrnalPath("Notes", "mapping.log"));

            }catch(Exception e){
                Log.d("Matting: ",e.toString());
            }
            return null;
        }
    }

    private class TestFrameWithMatting extends AsyncTask<String, Void, String> {

        byte[] data;
//        Camera camera;
        int width, height;

        public TestFrameWithMatting(byte[] pdata, Camera pcamera) {
            super();
            // do stuff
            data = pdata;
//            camera = pcamera;

            Camera.Parameters parameters = pcamera.getParameters();
            width = parameters.getPreviewSize().width;
            height = parameters.getPreviewSize().height;
        }

        @Override
        protected String doInBackground(String... params) {
            int cc = 1;
            contourLocalUrl = recorderActivity.contourLocalUrl;

            if(contourLocalUrl != null) {
                if(!matInitialized) {
                    int aa = mat.init(null, contourLocalUrl, HardCodedWidth, HardCodedHeight);
                    matInitialized = true;
                }

                try {
                    // Convert to JPG
//                        int x = camera.getParameters().getPreviewFormat();
//                    Camera.Size previewSize = camera.getParameters().getPreviewSize();

                    byte[] croppedData = new byte[(int) ((width * height * 3) / 2)];
//                      DEBUG Save image to phone
//                    imageCounter++;
//                    byte[] rgbaData = new byte[width * height * 4];
//                    Matting.imageNV21toRGB4(width, height, data, rgbaData);
//                    String filepath = vTool.getExtrnalPath("/homage/images/", Integer.toString(imageCounter) + "_before_crop_.jpg");
//                    vTool.writeJpeg(rgbaData, width, height, filepath);
//                    vTool.write(data, "/homage/images/", Integer.toString(imageCounter) + "_data.txt");
//                      ------------------------------------------
                    Matting.imageNV21to640X360(width, height, data, croppedData);
                    cc = mat.processBackground(HardCodedWidth, HardCodedHeight, croppedData);

//                      DEBUG Save image to phone
//                    byte[] rgbaData2 = new byte[HardCodedWidth * HardCodedHeight * 4];
//                    Matting.imageNV21toRGB4(HardCodedWidth, HardCodedHeight, croppedData, rgbaData2);
//                    String filepath2 = vTool.getExtrnalPath("/homage/images/", Integer.toString(imageCounter) + "_cc_" + Integer.toString(cc) + ".jpg");
//                    vTool.writeJpeg(rgbaData2, HardCodedWidth, HardCodedHeight, filepath2);
//                      ------------------------------------------------
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }

            String ccs = Integer.toString(cc);
            Log.d("cc",ccs);
            return ccs;
        }



        @Override
        protected void onPostExecute(String result) {
            //TODO update UI elements using res
            int intresult = Integer.parseInt(result);
            recorderActivity.lastcc = intresult;
            if(intresult >= 0){
                //Success to UI using res Recorder activity has boolean of let user take movie
                recorderActivity.HideWarningButton();
            }
            else if(intresult == -10)
            {
                //Too Dark
                recorderActivity.ShowWarningButton();
            }
            else if(intresult == -11)
            {
                //Unacceptable
                recorderActivity.ShowWarningButton();
            }
            else if(intresult < 0 && intresult > -10){
                //user is risking a bad movie
                recorderActivity.ShowWarningButton();
            }
        }


    }
}
