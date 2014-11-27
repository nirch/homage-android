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
import com.homage.app.main.HomageApplication;
import com.homage.matting.Matting;
import com.vim.vimapi.vTool;

import java.io.ByteArrayOutputStream;

/**
 * Created by dangalg on 11/24/2014.
 */
public class BackgroundDetection {
    Context context;
    RecorderActivity recorderActivity;
    Resources res;
    Matting mat;
    String TAG = "TAG_BackgroundDetection";

    String folderPath;
    String contourLocalUrl;
    int HardCodedWidth = 640;
    int HardCodedHeight = 360;
    int imageCounter = 0;

    public BackgroundDetection(Context pcontext, Camera pcamera) {
        res = HomageApplication.getContext().getResources();
        context = pcontext;
        recorderActivity = ((RecorderActivity)this.context);
        new StartMatting(pcamera).execute();
    }

    public void RunTestOnFrame(byte[] pdata, Camera pcamera){
        new TestFrameWithMatting(pdata, pcamera).execute("");
    }

    private class StartMatting extends AsyncTask<Void, Void, Void> {

        Camera camera;
        int width, height;

        public StartMatting(Camera pcamera) {
            super();
            // do stuff
            camera = pcamera;

            Camera.Parameters parameters = camera.getParameters();
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
        Camera camera;
        int width, height;

        public TestFrameWithMatting(byte[] pdata, Camera pcamera) {
            super();
            // do stuff
            data = pdata;
            camera = pcamera;

            Camera.Parameters parameters = camera.getParameters();
            width = parameters.getPreviewSize().width;
            height = parameters.getPreviewSize().height;
        }

        @Override
        protected String doInBackground(String... params) {
            int cc = 1;

                //String ctrFile = vTool.getExtrnalPath("Notes", contourLocalUrl);
                contourLocalUrl = recorderActivity.contourLocalUrl;
                if (!contourLocalUrl.isEmpty()) {
                    try{
                        // Convert to JPG
//                        int x = camera.getParameters().getPreviewFormat();
                        Camera.Size previewSize = camera.getParameters().getPreviewSize();

                        byte[] croppedData = new byte[data.length];
                        Matting.imageNV21to640X360(width, height, data, croppedData);

                        int aa = mat.init(null, contourLocalUrl, HardCodedWidth, HardCodedHeight);
                        cc = mat.processBackground(HardCodedWidth, HardCodedHeight, croppedData);

//                      DEBUG--------------------------------------------------------------------------
                        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 80, baos);
                        byte[] jdata = baos.toByteArray();


                        // DEBUG Convert to Bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
                        Bitmap cropped = Bitmap.createBitmap(bitmap, 0,60,HardCodedWidth, HardCodedHeight);
//                      For testing if bitmap is good


//                      DEBUG  save pictures to disk to see the grading
                        imageCounter++;
//                        vTool.writeJpeg(jdata,HardCodedWidth,HardCodedHeight,folderPath+Integer.toString(imageCounter) + "_cc_" + Integer.toString(cc) + ".jpg");
//                        DEBUG download jpegs so we can see output
                        String folderPath = contourLocalUrl.split("\\.")[0].split("storage/sdcard0")[1];
                        Download.WriteBitmapToDisk(cropped, folderPath, Integer.toString(imageCounter) + "_cc_" + Integer.toString(cc) + ".jpg");
//                    DEBUG------------------------------------------------------------------------------------
                    }
                    catch(Exception e){
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
