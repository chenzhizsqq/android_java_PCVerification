package com.dean.convenient.ui.view.loading.circle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dean.convenient.ui.R;

/**
 * Created by Dean on 16/8/29.
 */
public class ConvenientRotationLoadingView extends FrameLayout {

    private Context context;

    private ConvenientRotationProgress convenientRotationProgress;
    private TextView textView;

    public ConvenientRotationLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        initView();
    }

    private void initView() {
        @SuppressLint("InflateParams")
        View layout = LayoutInflater.from(context).inflate(R.layout.view_circle_loading, null);
        convenientRotationProgress = layout.findViewById(R.id.rotationProgress);
        textView = layout.findViewById(R.id.textView);
        addView(layout);
    }

    public void start(View view) {
        if (view != null)
            view.setVisibility(View.GONE);

        if (convenientRotationProgress != null) {
            convenientRotationProgress.start();
            this.setVisibility(View.VISIBLE);
        }
    }

    public void stop(View view) {
        if (convenientRotationProgress != null) {
            this.setVisibility(View.GONE);
            convenientRotationProgress.stop();
        }

        if (view != null)
            view.setVisibility(View.VISIBLE);
    }

}
