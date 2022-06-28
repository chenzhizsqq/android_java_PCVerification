package com.dean.convenient.ui.view.loading.elasticity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.dean.convenient.ui.R;

/**
 * “弹性”加载View
 * <p/>
 * Created by Dean on 16/8/31.
 */
public class ConvenientElasticityLoadingView extends FrameLayout {

    public static int ANIMATION_DURATION = 500;

    private Context context;

    private ImageView shadowImageView, elasticityView;

    private ScaleAnimation contractAnimation;
    private ScaleAnimation spreadAnimation;
    private ObjectAnimator upwardLeftRotationAnimation;
    private ObjectAnimator upwardRightRotationAnimation;
    private ObjectAnimator downwardObjectAnimation;

    private float imageViewY;
    private int graphIndex = -1;
    private boolean running = true;

    private Animator.AnimatorListener upwardAnimationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            shadowImageView.startAnimation(contractAnimation);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }

            downwardObjectAnimation.start();
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }
    };

    public ConvenientElasticityLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        initView();
        setGraph();
    }

    private void initView() {
        View view = LayoutInflater.from(context).inflate(R.layout.view_elasticity_loading, null);
        addView(view);

        elasticityView = (ImageView) view.findViewById(R.id.elasticityView);
        shadowImageView = (ImageView) view.findViewById(R.id.shadowImageView);

        imageViewY = 160;
        initAnimation();
    }

    /**
     * 初始化动画
     */
    private void initAnimation() {
        // 阴影缩放动画
        contractAnimation = new ScaleAnimation(1, 0.2f, 1, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        contractAnimation.setInterpolator(new DecelerateInterpolator());
        contractAnimation.setDuration(ANIMATION_DURATION);
        spreadAnimation = new ScaleAnimation(0.2f, 1, 1, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        spreadAnimation.setDuration(ANIMATION_DURATION);

        // 位移动画
        PropertyValuesHolder translateHolder = PropertyValuesHolder.ofFloat("y", 0f);
        // 向左旋转动画
        PropertyValuesHolder leftRotateHolder = PropertyValuesHolder.ofFloat("rotation", 0, 180f);
        // 向右旋转动画
        PropertyValuesHolder rightRotateHolder = PropertyValuesHolder.ofFloat("rotation", 0, -120f);

        upwardLeftRotationAnimation = ObjectAnimator.ofPropertyValuesHolder(elasticityView, translateHolder, leftRotateHolder);
        upwardLeftRotationAnimation.setDuration(ANIMATION_DURATION);
        upwardLeftRotationAnimation.setInterpolator(new DecelerateInterpolator());
        upwardLeftRotationAnimation.addListener(upwardAnimationListener);

        upwardRightRotationAnimation = ObjectAnimator.ofPropertyValuesHolder(elasticityView, translateHolder, rightRotateHolder);
        upwardRightRotationAnimation.setDuration(ANIMATION_DURATION);
        upwardRightRotationAnimation.setInterpolator(new DecelerateInterpolator());
        upwardRightRotationAnimation.addListener(upwardAnimationListener);

        downwardObjectAnimation = ObjectAnimator.ofFloat(elasticityView, "y", imageViewY);
        downwardObjectAnimation.setDuration(ANIMATION_DURATION);
        downwardObjectAnimation.setInterpolator(new AccelerateInterpolator());
        downwardObjectAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                shadowImageView.startAnimation(spreadAnimation);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (!running)
                    return;

                setGraph();

                switch (graphIndex) {
                    case 0:
                    case 1:
                        upwardLeftRotationAnimation.start();
                        break;
                    case 2:
                        upwardRightRotationAnimation.start();
                        break;
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
    }

    private void setGraph() {
        graphIndex += 1;
        if (graphIndex == 3)
            graphIndex = 0;

        switch (graphIndex) {
            case 0:
                elasticityView.setImageResource(R.drawable.icon_elasticity_loading_view_circle);
                break;
            case 1:
                elasticityView.setImageResource(R.drawable.icon_elasticity_loading_view_square);
                break;
            case 2:
                elasticityView.setImageResource(R.drawable.icon_elasticity_loading_view_triangle);
                break;
        }
    }

    /**
     * 开始执行动画
     */
    public void start() {
        running = true;

        upwardLeftRotationAnimation.start();
    }

    /**
     * 停止执行动画
     */
    public void stop() {
        running = false;
    }

    /**
     * 开始执行动画 并 隐藏指定View
     *
     * @param view
     */
    public void startAndHideView(View view) {
        if (view != null)
            view.setVisibility(View.GONE);

        this.setVisibility(View.VISIBLE);
        this.start();
    }

    /**
     * 停止执行动画 并 显示指定View
     *
     * @param view
     */
    public void stopAndShowView(View view) {
        this.setVisibility(View.GONE);

        if (view != null)
            view.setVisibility(View.VISIBLE);

        this.stop();
    }

}
