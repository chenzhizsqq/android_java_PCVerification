package com.dean.convenient.ui.view.navigation.popupwindow;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.dean.convenient.ui.R;
import com.dean.convenient.ui.view.navigation.NavigationListAdapter;
import com.dean.convenient.ui.view.navigation.listener.OnNavigationPopupWindowListener;
import com.dean.convenient.ui.view.navigation.model.NavigationItemModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 导航弹出窗
 * <p>
 * Created by dean on 2017/4/11.
 */
public class NavigationPopupWindow extends PopupWindow {

    private Context context;

    private ListView navigationListView;
    private NavigationListAdapter navigationListAdapter;
    private View bottomView;

    private View switchView;

    private OnNavigationPopupWindowListener onNavigationPopupWindowListener;

    public NavigationPopupWindow(Context context, View rootView) {
        super(rootView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);

        this.context = context;
        navigationListView = (ListView) rootView.findViewById(R.id.navigationListView);
        bottomView = rootView.findViewById(R.id.bottomView);

        List<NavigationItemModel> navigationItemModels = new ArrayList<>();
        NavigationItemModel navigationItemModel = new NavigationItemModel();
        navigationItemModel.setResId(R.drawable.icon_elasticity_loading_view_circle);
        navigationItemModel.setName("测试一");
        navigationItemModels.add(navigationItemModel);
        NavigationItemModel navigationItemModel1 = new NavigationItemModel();
        navigationItemModel1.setResId(R.drawable.icon_elasticity_loading_view_square);
        navigationItemModel1.setName("测试二");
        navigationItemModels.add(navigationItemModel1);

        navigationListAdapter = new NavigationListAdapter(context, navigationItemModels);
        navigationListView.setAdapter(navigationListAdapter);

        setAnimationStyle(R.style.NavigationAnim);
        setBackgroundDrawable(new BitmapDrawable());
        setFocusable(true);
        setOutsideTouchable(false);

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (event.getX() > switchView.getX() && event.getX() < switchView.getX() + switchView.getWidth() && event.getY() > switchView.getY()
                                && event.getY() < switchView.getY() + switchView.getHeight()) {
                            NavigationPopupWindow.this.dismiss();

                            if (onNavigationPopupWindowListener != null)
                                onNavigationPopupWindowListener.onClosed();

                            return true;
                        }

                        break;
                }

                return false;
            }
        });
    }

    public void show(View view) {
        switchView = view;

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bottomView.getLayoutParams();
        layoutParams.height = switchView.getHeight() + 110;

        bottomView.setLayoutParams(layoutParams);
        super.showAtLocation(view, Gravity.BOTTOM, 0, 0);
    }

    public void close() {
        super.dismiss();
    }

    public void setOnNavigationPopupWindowListener(OnNavigationPopupWindowListener onNavigationPopupWindowListener) {
        this.onNavigationPopupWindowListener = onNavigationPopupWindowListener;
    }
}
