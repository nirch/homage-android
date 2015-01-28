package com.homage.app.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.homage.FileHandler.VideoHandler.DownloadVideoAsync.Progress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by dangalg on 11/27/2014.
 */
public class DownloadUtil {
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

    public static String WriteFileToStorage(Progress progress, File outFile, File tempFile, URL url) {
        OutputStream output = null;
        InputStream input = null;
        String downloadStatus = "OK";
        try {
            output = new FileOutputStream(tempFile.getAbsolutePath());
            URLConnection connection = url.openConnection();
            connection.connect();
            int filesize = connection.getContentLength();
            input = url.openStream();

            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            long downloadTotal = 0;
            while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                output.write(buffer, 0, bytesRead);
                downloadTotal += bytesRead;
                if(progress != null) {
                    progress.publish(((int) (downloadTotal * 100 / filesize)));
                }

            }
            copy(tempFile, outFile);

        } catch (MalformedURLException e) {
            Log.d("MalformedURLException: ", e.toString());
            downloadStatus = "Error";
        } catch (IOException e1) {
            Log.d("IOException: ", e1.toString());
            downloadStatus = "Error";
        }catch (Exception e){
            Log.d("Exception: ", e.toString());
            downloadStatus = "Error";
        } finally {
            try {
                if(input != null)
                    input.close();
                if(output != null)
                    output.close();
            } catch (IOException e1) {
                Log.d("IOException: ", e1.toString());
                downloadStatus = "Error";
            }
        }
        return downloadStatus;
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void CreateFolderInLocalStorage(String foldername){
        File folder = new File(foldername);
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
