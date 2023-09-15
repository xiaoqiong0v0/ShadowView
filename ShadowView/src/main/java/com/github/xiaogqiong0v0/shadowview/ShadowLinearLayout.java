package com.github.xiaogqiong0v0.shadowview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

/**
 * 包名：com.github.xiaogqiong0v0.shadowview
 * 创建者：xiaoqiong0v0
 * 邮箱：king-afu@hotmail.com
 * 时间：2022/3/25 - 13:53
 * 说明：
 */
public class ShadowLinearLayout extends LinearLayout {
    private ShadowParams shadowParams;

    public ShadowLinearLayout(Context context) {
        this(context, null);
    }

    public ShadowLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr) {
        shadowParams = new ShadowParams(this, attrs, defStyleAttr);
    }

    public ShadowParams getShadowParams() {
        return shadowParams;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        shadowParams.initDraw(w, h);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        shadowParams.draw(canvas);
    }
}
