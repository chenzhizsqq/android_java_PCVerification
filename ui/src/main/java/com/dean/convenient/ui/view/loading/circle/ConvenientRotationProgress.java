package com.dean.convenient.ui.view.loading.circle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.dean.convenient.ui.R;

/**
 * 自定义旋转的进度条
 *
 * @author Dean
 */
public class ConvenientRotationProgress extends View {

    private int widthSize;
    private int heightSize;
    private Paint paint;
    private Bitmap bitmapLoading;
    private int bitmapWidth;
    private int bitmapHeight;
    private int rotation = 10;
    private int rotate = 0;
    private boolean isRun = false;

    public ConvenientRotationProgress(Context context) {
        super(context);
        init();
    }

    public ConvenientRotationProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        bitmapLoading = BitmapFactory.decodeResource(getResources(), R.drawable.icon_rotation_loading_circle);
        bitmapWidth = bitmapLoading.getWidth();
        bitmapHeight = bitmapLoading.getHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        uploadWait(canvas);
    }

    private void uploadWait(Canvas canvas) {
        if (isRun)
            return;

        canvas.save();
        canvas.rotate(rotate += rotation, (int) (widthSize / 2.0), (int) (heightSize / 2.0));
        canvas.drawBitmap(bitmapLoading, (int) ((widthSize - bitmapWidth) / 2.0), (int) ((heightSize - bitmapHeight) / 2.0), paint);
        canvas.restore();
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        widthSize = MeasureSpec.getSize(widthMeasureSpec);
        heightSize = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void start() {
        isRun = false;
        postInvalidate();
        setVisibility(View.VISIBLE);
    }

    public void stop() {
        setVisibility(View.GONE);
        isRun = true;
    }

}
