package com.dean.convenient.ui.view.navigation.model;

import android.content.Intent;

/**
 * Created by dean on 2017/4/11.
 */
public class NavigationItemModel {

    private String name;
    private int resId;
    private Intent intent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }
}
