package com.homage.app.Utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

/**
 * Created by dangalg on 1/26/2015.
 */
public class cacheUtil {

    public static Intent getSendVideoIntent(Context context, String email,
                                            String subject, String body, String fileName, String type, String spackage) {

        final Intent emailIntent = new Intent(
                android.content.Intent.ACTION_SEND);


        emailIntent.setPackage(spackage);

        emailIntent.setType(type);

        //Add the recipients
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[] { email });

        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);

        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);

        Uri uri = null;
        if(spackage.equals("com.google.android.youtube")) {
            ContentValues content = new ContentValues(4);
            content.put(MediaStore.Video.VideoColumns.DATE_ADDED,
                    System.currentTimeMillis() / 1000);
            content.put(MediaStore.Video.Media.MIME_TYPE, type);
            content.put(MediaStore.Video.Media.DATA, context.getCacheDir() + File.separator + fileName);
            ContentResolver resolver = context.getContentResolver();
            uri = resolver.insert(MediaStore.Video.Media.INTERNAL_CONTENT_URI, content);
        }
        else{
            // Add the attachment by specifying a reference to our custom ContentProvider
            // and the specific file of interest
            File shareFile = new File(context.getCacheDir() + File.separator + fileName);
            // This line makes the magic!
            shareFile.setReadable(true, false);
            uri = Uri.fromFile(shareFile);
        }

        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);


        return emailIntent;
    }
}
