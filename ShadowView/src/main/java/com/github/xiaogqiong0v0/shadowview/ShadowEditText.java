package com.github.xiaogqiong0v0.shadowview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * 包名：com.github.xiaogqiong0v0.shadowview
 * 创建者：xiaoqiong0v0
 * 邮箱：king-afu@hotmail.com
 * 时间：2023/2/27 - 9:40
 * 说明：
 */
public class ShadowEditText extends AppCompatEditText {
    private ShadowParams shadowParams;

    public ShadowEditText(@NonNull Context context) {
        this(context, null);
    }

    public ShadowEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public ShadowEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
