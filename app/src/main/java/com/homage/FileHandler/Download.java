package com.homage.FileHandler;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by dangalg on 11/27/2014.
 */
public class Download {
    public static void WriteBitmapToDisk(Bitmap bmp, String folder_path, String file_name)
    {
        File dir = new File(Environment.getExternalStorageDirectory() + folder_path);
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(dir, file_name);
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);

            bmp.compress(Bitmap.CompressFormat.PNG, 85, fOut);

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.d("IOException: ", e.toString());
        }
    }

    public static void WriteFileToStorage(String filepath, URL url) {
        OutputStream output = null;
        InputStream input = null;
        try {
            output = new FileOutputStream(filepath);
            input = url.openStream();

            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (MalformedURLException e) {
            Log.d("MalformedURLException: ", e.toString());
        } catch (IOException e1) {
            Log.d("IOException: ", e1.toString());
        }catch (Exception e){
            Log.d("Exception: ", e.toString());
        } finally {
            try {
                if(input != null)
                    input.close();
                if(output != null)
                    output.close();
            } catch (IOException e1) {
                Log.d("IOException: ", e1.toString());
            }
        }
    }

    public static void CreateFolderInLocalStorage(String foldername){
        File folder = new File(Environment.getExternalStorageDirectory() + foldername);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            // Do something on success
        } else {
            // Do something else on failure
        }
    }
}
