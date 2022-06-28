package com.dean.convenient.ui.view.navigation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dean.convenient.ui.R;
import com.dean.convenient.ui.view.navigation.listener.OnNavigationPopupWindowListener;
import com.dean.convenient.ui.view.navigation.popupwindow.NavigationPopupWindow;

/**
 * 导航按钮
 * <p>
 * Created by dean on 2017/4/11.
 */
public class NavigationButton extends FrameLayout {

    private Context context;

    private View rootView;
    private CheckBox mainCheckBox;

    private NavigationPopupWindow navigationPopupWindow;

    private OnNavigationPopupWindowListener onNavigationPopupWindowListener = new OnNavigationPopupWindowListener() {
        @Override
        public void onClosed() {
            mainCheckBox.setChecked(false);
        }
    };

    public NavigationButton(@NonNull final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        rootView = LayoutInflater.from(this.context).inflate(R.layout.view_button_navigation, null);
        this.addView(rootView);

        mainCheckBox = (CheckBox) rootView.findViewById(R.id.mainCheckBox);
        mainCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    showNavigation();
                else
                    closeNavigation();
            }
        });
    }

    private void showNavigation() {
        if (navigationPopupWindow == null) {
            navigationPopupWindow = new NavigationPopupWindow(context, LayoutInflater.from(context).inflate(R.layout.popupwindow_navigation, null));
            navigationPopupWindow.setOnNavigationPopupWindowListener(onNavigationPopupWindowListener);
        }

        if (!navigationPopupWindow.isShowing())
            navigationPopupWindow.show(mainCheckBox);
    }

    private void closeNavigation() {
        if (navigationPopupWindow != null && navigationPopupWindow.isShowing())
            navigationPopupWindow.close();
    }

}
