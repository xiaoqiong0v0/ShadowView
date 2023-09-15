package com.github.xiaogqiong0v0.shadowview;


import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class ShadowImageView extends AppCompatImageView {
    private ShadowParams shadowParams;

    public ShadowImageView(@NonNull Context context) {
        this(context, null);
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        shadowParams.draw(canvas);
    }
}
