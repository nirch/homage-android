package com.homage.app.Utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

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

        //Add the attachment by specifying a reference to our custom ContentProvider
        //and the specific file of interest
        File shareFile = new File(context.getCacheDir() + File.separator + fileName);
//        This line makes the magic!
        shareFile.setReadable(true, false);

        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(shareFile));

        return emailIntent;
    }
}
