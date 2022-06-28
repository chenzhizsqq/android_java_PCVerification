package com.dean.convenient.ui.view.loading.progress;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by dean on 2017/5/19.
 */
public class ConvenientProgressDialog {

    private static ProgressDialog instance;

    public static ProgressDialog getInstance(Context context, String message, boolean isCancelable) {
        instance = new ProgressDialog(context);
        instance.setMessage(message);
        instance.setCancelable(isCancelable);

        return instance;
    }
}
