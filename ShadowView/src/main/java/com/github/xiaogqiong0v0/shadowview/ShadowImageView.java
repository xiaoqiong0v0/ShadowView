package com.github.xiaogqiong0v0.shadowview;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class ShadowImageView extends AppCompatImageView {
    private ShadowParams shadowParams;
    private Drawable background;

    public ShadowImageView(@NonNull Context context) {
        super(context);
        shadowParams = new ShadowParams(this);
    }

    public ShadowImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
