package com.dean.convenient.ui.view.actionbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dean.convenient.ui.R;

/**
 * 工具栏
 * <p/>
 * Created by Dean on 16/3/4.
 */
public class MaterialToolbar extends FrameLayout {

    public static final int MENU = 0;
    public static final int BACK = 1;
    public static final int TITLE = 2;
    public static final int OTHER = 3;

    private Toolbar toolbar;
    private TextView title;

    public MaterialToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(context).inflate(R.layout.view_material_toolbar, null);
        toolbar = view.findViewById(R.id.toolbar);
        title = view.findViewById(R.id.title);
        this.addView(view);
    }

    /**
     * 初始化（直接设置标题）
     */
    public void init(final AppCompatActivity activity, int type, String title) {
        this.init(activity);
        setNavigationIcon(type);
        this.setTitle(title);

        if (type == BACK)
            setNavigationOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.finish();
                }
            });
    }

    /**
     * 初始化（直接设置标题）
     */
    public void init(AppCompatActivity activity, int type, String title, int resId) {
        this.init(activity);
        setNavigationIcon(type, resId);
        this.setTitle(title);
    }

    public void setNavigationIcon(int type) {
        switch (type) {
            case MENU:
                break;
            case BACK:
                this.setBackIcon(R.drawable.ic_action_back);
                break;
            case TITLE:
                // 没有任何菜单图标
                toolbar.setNavigationIcon(null);
                break;
            case OTHER:
                break;
        }
    }

    public void setNavigationIcon(int type, int resId) {
        switch (type) {
            case MENU:
                this.setBackIcon(resId);
                break;
            case BACK:
                this.setBackIcon(R.drawable.ic_action_back);
                break;
            case TITLE:
                // 没有任何菜单图标
                toolbar.setNavigationIcon(null);
                break;
            case OTHER:
                this.setBackIcon(resId);
                break;
        }
    }

    /**
     * 初始化
     */
    public void init(AppCompatActivity activity) {
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setHomeButtonEnabled(true);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    /**
     * 获取Toolbar实例
     *
     * @return
     */
    public Toolbar getInstance() {
        return this.toolbar;
    }

    /**
     * 设置返回键图标
     *
     * @param id
     */
    public void setBackIcon(int id) {
        this.toolbar.setNavigationIcon(id);
    }

    /**
     * 设置标题
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title.setText(title);
    }

    /**
     * 设置左侧功能键的点击事件
     *
     * @param onClickListener
     */
    public void setNavigationOnClickListener(OnClickListener onClickListener) {
        if (onClickListener != null)
            this.toolbar.setNavigationOnClickListener(onClickListener);
    }

    /**
     * 设置事件监听
     *
     * @param onMenuItemClickListener
     */
    public void setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener onMenuItemClickListener) {
        this.toolbar.setOnMenuItemClickListener(onMenuItemClickListener);
    }

}
