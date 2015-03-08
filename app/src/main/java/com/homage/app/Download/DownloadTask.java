package com.homage.app.Download;

/**
 * Created by dangal on 2/18/15.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

import android.content.Context;
import android.util.Log;

import com.homage.app.main.HomageApplication;
import com.homage.app.main.MainActivity;


public class DownloadTask implements Runnable {

    private static final String TAG = DownloadTask.class.getSimpleName();

    private File mOutFile;
    private File mTempFile;
    private URL mUrl;
    private boolean mOverwrite;


    public DownloadTask(File outFile, URL url, boolean overWrite) {

        mOutFile = outFile;
        mTempFile = new File(outFile.getPath() + "_tmp");
        mUrl = url;
        mOverwrite = overWrite;
    }

    @Override
    public void run() {
        if (!HomageApplication.getInstance().downloadPaused && (mOverwrite || !mOutFile.exists())) {
            try {

                Log.d(TAG, "Downloading: " + mOutFile.getName());

                OutputStream output = null;
                InputStream input = null;
                String downloadStatus = "OK";

                output = new FileOutputStream(mTempFile.getAbsolutePath());
                URLConnection connection = mUrl.openConnection();
                connection.connect();
                input = mUrl.openStream();

                byte[] buffer = new byte[1024];
                int bytesRead = 0;
                while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                    output.write(buffer, 0, bytesRead);
                }
                CopyFile(mTempFile, mOutFile);

                input.close();

                output.close();
                Log.d(TAG, "Downloaded: " + mOutFile.getName());

            } catch (Throwable t) {
                Log.e(TAG, "Error in DownloadTask", t);
            }
        }
    }

    public static void CopyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        boolean deleted = src.delete();

        in.close();
        out.close();
    }
}
