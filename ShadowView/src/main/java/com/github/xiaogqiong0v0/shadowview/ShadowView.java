package com.github.xiaogqiong0v0.shadowview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

/**
 * 包名：com.github.xiaogqiong0v0.shadowview
 * 创建者：xiaoqiong0v0
 * 邮箱：king-afu@hotmail.com
 * 时间：2022/3/25 - 16:16
 * 说明：
 */
public class ShadowView extends View {
    private ShadowParams shadowParams;
    private Drawable background;

    public ShadowView(Context context) {
        super(context);
        shadowParams = new ShadowParams(this);
    }

    public ShadowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr) {
        shadowParams = new ShadowParams(this, attrs, defStyleAttr);
        if (background != null) {
            shadowParams.setBackgroundDrawable(background);
        }
    }

    public ShadowParams getShadowParams() {
        return shadowParams;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        shadowParams.measure(widthMeasureSpec, heightMeasureSpec, super::onMeasure);
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        if (shadowParams == null) {
            this.background = background;
            return;
        }
        shadowParams.setBackgroundDrawable(background);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        shadowParams.draw(canvas, super::onDraw);
    }
}
