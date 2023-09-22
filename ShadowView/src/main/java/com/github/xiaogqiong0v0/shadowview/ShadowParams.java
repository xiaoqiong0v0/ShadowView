package com.github.xiaogqiong0v0.shadowview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;

/**
 * 包名：com.github.xiaogqiong0v0.shadowview
 * 创建者：xiaoqiong0v0
 * 邮箱：king-afu@hotmail.com
 * 时间：2022/3/23 - 17:25
 * 说明：阴影区域 等于 rect - spread(张度) - blur(模糊半径) + shadowDx + shadowDy
 * 边框直接绘制在显示区域 不算在padding里面
 * 普通阴影 涉及样式改变 可直接 invalidate 否则需要手动调用 refreshParams
 * ╭─────────────────────────────────────────────────────────────────────────────────────╮
 * │                          shadowBlur                                                 │
 * │     ╭──────────────────────────────────────────────────────────────────────────╮    │
 * │     │                   shadow spread                                          │    │
 * │     │     ╭──────────────────────────────────────────────────────────────╮     │    │
 * │     │     │                                                              │     │    │
 * │     │     │              display area                                    │     │    │
 * │     │     │                                                              │     │    │
 * │     │     ╰──────────────────────────────────────────────────────────────╯     │    │
 * │     │                                                                          │    │
 * │     ╰──────────────────────────────────────────────────────────────────────────╯    │
 * │                                                                                     │
 * ╰─────────────────────────────────────────────────────────────────────────────────────╯
 * inset阴影
 * ╭─────────────────────────────────────────────────────────────────────────────────────╮
 * │                          shadow spread               d                              │
 * │     ╭────────────────────────────────────────────────s─────────────────────────╮    │
 * │     │                   shadow blur                  p                         │    │
 * │     │     ╭──────────────────────────────────────────l───────────────────╮     │    │
 * │     │     │                                          y                   │     │    │
 * │     │     │                                                              │     │    │
 * │     │     │                                          a                   │     │    │
 * │     │     ╰──────────────────────────────────────────r───────────────────╯     │    │
 * │     │                                                e                         │    │
 * │     ╰────────────────────────────────────────────────a─────────────────────────╯    │
 * │                                                                                     │
 * ╰─────────────────────────────────────────────────────────────────────────────────────╯
 */
public class ShadowParams {
    @IntDef({BORDER_TYPE_SOLID, BORDER_TYPE_DASHED})
    public @interface BorderType {
    }

    public static final int BORDER_TYPE_SOLID = 0;
    public static final int BORDER_TYPE_DASHED = 1;

    @IntDef({SHADOW_TYPE_SOFT, SHADOW_TYPE_HARD})
    public @interface ShadowType {
    }

    public static final int SHADOW_TYPE_SOFT = 0;
    public static final int SHADOW_TYPE_HARD = 1;

    @IntDef({SHADOW_CLIP_NONE, SHADOW_CLIP_LEFT, SHADOW_CLIP_TOP, SHADOW_CLIP_RIGHT, SHADOW_CLIP_BOTTOM})
    public @interface ShadowClip {
    }

    public static final int SHADOW_CLIP_NONE = 0;
    public static final int SHADOW_CLIP_LEFT = 1;
    public static final int SHADOW_CLIP_TOP = 2;
    public static final int SHADOW_CLIP_RIGHT = 4;
    public static final int SHADOW_CLIP_BOTTOM = 8;

    @IntDef({OUT_CLEAR_MODE_CLIP, OUT_CLEAR_MODE_CLEAR})
    public @interface OutClearMode {
    }

    public static final int OUT_CLEAR_MODE_CLIP = 0;
    public static final int OUT_CLEAR_MODE_CLEAR = 1;
    //
    public int shadowColor;
    public int shadowDx;
    public int shadowDy;
    // 虚化大小
    public int shadowBlur;
    // 阴影张度
    public int shadowSpread;
    public boolean shadowInset;
    @ShadowClip
    public int shadowClip;
    @ShadowType
    public int shadowType;
    public PwPhValue boxRadiusLeftTop;
    public PwPhValue boxRadiusRightTop;
    public PwPhValue boxRadiusRightBottom;
    public PwPhValue boxRadiusLeftBottom;
    public int boxBorderThickness;
    public int boxBorderColor;
    @BorderType
    public int boxBorderType;
    /**
     * 阴影下层颜色 设置非透明可以提升性能
     */
    public int underColor;
    /**
     * 外部清除模式 仅外阴影 默认 clip
     */
    @OutClearMode
    public int outClearMode;
    /**
     * 自动添加padding 仅外阴影  默认 true
     * 否则布局参数不变 阴影绘制在内部
     */
    public boolean autoAddPadding;
    /**
     * 阴影自动转换宽高  仅外阴影 如果设置自动 margin 先使用margin 方便布局
     * 意在尽量保持设置的宽高为可见宽高
     */
    public boolean autoAddWidthHeight;
    /**
     * 阴影自动占用  仅外阴影 margin 优先自动调整宽高 方便布局
     * 意在尽量保持设置的宽高为可见宽高
     */
    public boolean autoDelMargin;

    /////////////////////////////////////////////
    private final View view;
    private final PorterDuffXfermode porterDuffClearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private final PorterDuffXfermode porterDuffDstOutMode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private Paint paint;
    @Nullable
    private BlurMaskFilter blurMaskFilter;
    @Nullable
    private Shader leftTopShader;
    @Nullable
    private Shader rightTopShader;
    @Nullable
    private Shader rightBottomShader;
    @Nullable
    private Shader leftBottomShader;
    @Nullable
    private Shader toLeftShader;
    @Nullable
    private Shader toTopShader;
    @Nullable
    private Shader toRightShader;
    @Nullable
    private Shader toBottomShader;
    @Nullable
    private Path clipPath;
    @Nullable
    private Path outerPath;
    @Nullable
    private Path borderPath;
    @Nullable
    private Path innerPath;
    @Nullable
    private Path shadowInnerPath;
    @Nullable
    private Path shadowLeftTopPath;
    @Nullable
    private Path shadowRightTopPath;
    @Nullable
    private Path shadowRightBottomPath;
    @Nullable
    private Path shadowLeftBottomPath;
    @Nullable
    private Path shadowToLeftPath;
    @Nullable
    private Path shadowToTopPath;
    @Nullable
    private Path shadowToRightPath;
    @Nullable
    private Path shadowToBottomPath;

    /////////// status
    private boolean drawAble = false;
    private boolean allRadiusZero = false;
    private boolean shadowBlurZero = false;
    private boolean borderThicknessZero = false;
    private boolean shadowThicknessZero = false;
    private boolean layoutChanged = false;
    ///////////
    private int savedLayerType;
    private Rect savedMargins;
    private Rect savedPaddings;
    private Size savedWidthHeight;
    // 阴影边距
    private Rect currentPaddings;
    private int currentW;
    private int currentH;
    private int validW;
    private int validH;
    @Nullable
    private Drawable backgroundDrawable;

    public ShadowParams(View view, @Nullable AttributeSet attrs, int defStyleAttr) {
        this.view = view;
        view.setWillNotDraw(false);
        Context context = view.getContext();
        final TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.ShadowView, defStyleAttr, 0);
        shadowColor = attr.getColor(R.styleable.ShadowView_shadow_color, Color.BLACK);
        shadowDx = attr.getDimensionPixelSize(R.styleable.ShadowView_shadow_dx, 0);
        shadowDy = attr.getDimensionPixelSize(R.styleable.ShadowView_shadow_dy, 0);
        shadowBlur = attr.getDimensionPixelSize(R.styleable.ShadowView_shadow_blur, 0);
        shadowSpread = attr.getDimensionPixelSize(R.styleable.ShadowView_shadow_spread, 0);
        shadowInset = attr.getBoolean(R.styleable.ShadowView_shadow_inset, false);
        shadowType = attr.getInt(R.styleable.ShadowView_shadow_type, SHADOW_TYPE_SOFT);
        shadowClip = attr.getInt(R.styleable.ShadowView_shadow_clip, SHADOW_CLIP_NONE);
        PwPhValue radiusValue = getPhPwValue(attr, R.styleable.ShadowView_box_radius);
        boxRadiusLeftTop = getPhPwValue(attr, R.styleable.ShadowView_box_radius_left_top, radiusValue);
        boxRadiusRightTop = getPhPwValue(attr, R.styleable.ShadowView_box_radius_right_top, radiusValue);
        boxRadiusRightBottom = getPhPwValue(attr, R.styleable.ShadowView_box_radius_right_bottom, radiusValue);
        boxRadiusLeftBottom = getPhPwValue(attr, R.styleable.ShadowView_box_radius_left_bottom, radiusValue);
        boxBorderThickness = attr.getDimensionPixelSize(R.styleable.ShadowView_box_border_thickness, 0);
        boxBorderColor = attr.getColor(R.styleable.ShadowView_box_border_color, Color.BLACK);
        boxBorderType = attr.getInt(R.styleable.ShadowView_box_border_type, BORDER_TYPE_SOLID);
        underColor = attr.getColor(R.styleable.ShadowView_under_color, Color.TRANSPARENT);
        outClearMode = attr.getInt(R.styleable.ShadowView_out_clear_mode, OUT_CLEAR_MODE_CLIP);
        autoAddPadding = attr.getBoolean(R.styleable.ShadowView_auto_add_padding, true);
        autoAddWidthHeight = attr.getBoolean(R.styleable.ShadowView_auto_add_width_height, false);
        autoDelMargin = attr.getBoolean(R.styleable.ShadowView_auto_del_margin, false);
        attr.recycle();
        //
        savedLayerType = view.getLayerType();
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(view.getContext(), attrs);
        savedPaddings = new Rect(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
        savedMargins = new Rect(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin);
        savedWidthHeight = new Size(params.width, params.height);
        init();
    }

    public ShadowParams(View view) {
        this.view = view;
        view.setWillNotDraw(false);
        shadowColor = Color.BLACK;
        shadowDx = 0;
        shadowDy = 0;
        shadowBlur = 0;
        shadowSpread = 0;
        shadowInset = false;
        shadowType = SHADOW_TYPE_SOFT;
        underColor = Color.TRANSPARENT;
        outClearMode = OUT_CLEAR_MODE_CLIP;
        shadowClip = SHADOW_CLIP_NONE;
        boxRadiusLeftTop = new PwPhValue();
        boxRadiusRightTop = new PwPhValue();
        boxRadiusRightBottom = new PwPhValue();
        boxRadiusLeftBottom = new PwPhValue();
        boxBorderThickness = 0;
        boxBorderColor = Color.BLACK;
        boxBorderType = BORDER_TYPE_SOLID;
        autoAddPadding = true;
        autoAddWidthHeight = false;
        autoDelMargin = false;
        //
        savedLayerType = view.getLayerType();
        savedMargins = new Rect();
        savedPaddings = new Rect();
        savedWidthHeight = new Size(0, 0);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeWidth(0);
    }

    public final Rect getPaddingRect() {
        int dis = shadowSpread + shadowBlur;
        Rect rect = new Rect(dis, dis, dis, dis);
        // 外阴影 dx 增加 整体左移 来绘制右侧阴影
        // dy 增加 整体上移 来绘制下侧阴影 出现负值需要偏移到可见区域
        rect.left -= shadowDx;
        rect.top -= shadowDy;
        rect.right += shadowDx;
        rect.bottom += shadowDy;
        // 如果有小于0的值,再次偏移
        if (rect.right < 0) {
            rect.left += rect.right;
            rect.right = 0;
        } else if (rect.left < 0) {
            rect.right += rect.left;
            rect.left = 0;
        }
        if (rect.bottom < 0) {
            rect.top += rect.bottom;
            rect.bottom = 0;
        } else if (rect.top < 0) {
            rect.bottom += rect.top;
            rect.top = 0;
        }
        if ((shadowClip & SHADOW_CLIP_LEFT) == SHADOW_CLIP_LEFT) {
            rect.left = 0;
        }
        if ((shadowClip & SHADOW_CLIP_TOP) == SHADOW_CLIP_TOP) {
            rect.top = 0;
        }
        if ((shadowClip & SHADOW_CLIP_RIGHT) == SHADOW_CLIP_RIGHT) {
            rect.right = 0;
        }
        if ((shadowClip & SHADOW_CLIP_BOTTOM) == SHADOW_CLIP_BOTTOM) {
            rect.bottom = 0;
        }
        return rect;
    }

    /**
     * 动态调用需手动更新
     */
    public final void loadLayerTypeChanged() {
        savedLayerType = view.getLayerType();
    }

    /**
     * 动态调用需手动更新
     */
    public final void loadPaddingChanged() {
        savedPaddings = new Rect(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
    }

    /**
     * 动态调用需手动更新
     */
    public final void loadMarginChanged() {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
            savedMargins = new Rect(marginLayoutParams.leftMargin, marginLayoutParams.topMargin,
                    marginLayoutParams.rightMargin, marginLayoutParams.bottomMargin);
        } else {
            savedMargins = new Rect();
        }
    }

    /**
     * 动态调用需手动更新
     */
    public final void loadWidthHeightChanged() {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        savedWidthHeight = new Size(params.width, params.height);
    }

    /**
     * 动态调用需手动更新
     */
    public final void setBackgroundDrawable(@Nullable Drawable drawable) {
        backgroundDrawable = drawable;
        if (drawable != null && currentW > 0 && currentH > 0) {
            drawable.setBounds(0, 0, currentW, currentH);
        }
    }

    public final void measure(int widthMeasureSpec, int heightMeasureSpec, OnMeasureListener onMeasureSuperListener) {
        if (shadowInset) {
            restoreLayout();
        } else {
            // 先改变layout参数 改变与宽高无关
            changeLayout();
        }
        // 多次测量
        onMeasureSuperListener.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = view.getMeasuredWidth();
        int h = view.getMeasuredHeight();
        if (w <= 0 || h <= 0) {
            drawAble = false;
            return;
        }
        currentW = w;
        currentH = h;
        if (currentPaddings != null) {
            validW = w - currentPaddings.left - currentPaddings.right;
            validH = h - currentPaddings.top - currentPaddings.bottom;
        } else {
            validW = w;
            validH = h;
        }
        refreshParams();
        drawAble = true;
    }

    public final void draw(Canvas canvas, OnDrawSuperListener onDrawSuperListener) {
        if (!drawAble) {
            return;
        }
        if (outClearMode == OUT_CLEAR_MODE_CLEAR) {
            drawSuper(canvas, onDrawSuperListener);
            if (!shadowThicknessZero) {
                // 绘制阴影
                if (shadowInset) {
                    drawInsetShadow(canvas);
                    // 把多余的清除
                    drawOuterClear(canvas);
                } else {
                    // 把多余的清除
                    drawOuterClear(canvas);
                    drawShadow(canvas);
                }
            } else {
                drawOuterClear(canvas);
            }
        } else {
            if (!shadowThicknessZero) {
                // 绘制阴影
                if (shadowInset) {
                    drawClipSuper(canvas, onDrawSuperListener);
                    drawInsetShadow(canvas);
                } else {
                    drawShadow(canvas);
                    canvas.saveLayer(0, 0, currentW, currentH, null, Canvas.ALL_SAVE_FLAG);
                    drawClipSuper(canvas, onDrawSuperListener);
                    canvas.restore();
                }
            } else {
                drawClipSuper(canvas, onDrawSuperListener);
            }
        }
        drawBorder(canvas);
    }

    public final void refreshParams() {
        if (currentW == 0 || currentH == 0) {
            return;
        }
        if (backgroundDrawable != null) {
            backgroundDrawable.setBounds(0, 0, currentW, currentH);
        }
        allRadiusZero = allRadiusZero();
        shadowBlurZero = shadowBlurZero();
        borderThicknessZero = borderThicknessZero();
        shadowThicknessZero = shadowThicknessZero();
        if (outClearMode == OUT_CLEAR_MODE_CLEAR) {
            outerPath = getOuterPath(currentW, currentH);
            if (outerPath != null) {
                if (underColor == Color.TRANSPARENT) {
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                } else {
                    view.setLayerType(savedLayerType, null);
                }
            }
        } else {
            clipPath = getClipPath(currentW, currentH);
        }
        borderPath = getBorderPath(currentW, currentH);
        if (shadowThicknessZero) return;
        if (shadowInset) {
            if (shadowType == SHADOW_TYPE_SOFT) {
                shadowInnerPath = getInsetShadowPath(currentW, currentH);
                if (!shadowBlurZero) {
                    blurMaskFilter = new BlurMaskFilter(shadowBlur, BlurMaskFilter.Blur.NORMAL);
                } else {
                    blurMaskFilter = null;
                }
            } else {
                obtainInsetShadowSplitPathAndShader();
            }
        } else {
            innerPath = getInnerPath(currentW, currentH);
            if (shadowType == SHADOW_TYPE_SOFT) {
                // 软阴影 使用 blurMaskFilter
                // 获取 BlurMaskFilter
                shadowInnerPath = getShadowInnerPath(currentW, currentH);
                int min = dpi2px(1.5f);
                if (shadowBlur > min) {
                    blurMaskFilter = new BlurMaskFilter(shadowBlur - min, BlurMaskFilter.Blur.NORMAL);
                } else {
                    blurMaskFilter = null;
                }
            } else {
                // 硬阴影 使用 渐变 shader
                obtainOuterShadowSplitPathAndShader();
            }
        }
    }

    private void drawSuper(Canvas canvas, OnDrawSuperListener onDrawSuperListener) {
        if (backgroundDrawable != null) {
            backgroundDrawable.draw(canvas);
        }
        onDrawSuperListener.onDrawSuper(canvas);
    }

    private void drawClipSuper(Canvas canvas, OnDrawSuperListener onDrawSuperListener) {
        if (clipPath != null) {
            canvas.clipPath(clipPath);
        }
        if (backgroundDrawable != null) {
            backgroundDrawable.draw(canvas);
        }
        onDrawSuperListener.onDrawSuper(canvas);
    }

    private void drawOuterClear(Canvas canvas) {
        if (outerPath == null) {
            return;
        }
        // 设置画笔
        paint.setColor(underColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setXfermode(underColor != 0 ? null : porterDuffClearMode);
        canvas.drawPath(outerPath, paint);
        // 重置画笔
        paint.setXfermode(null);
    }

    private void drawShadow(Canvas canvas) {
        if (innerPath == null || shadowInnerPath == null) {
            return;
        }
        canvas.saveLayer(0, 0, currentW, currentH, null, Canvas.ALL_SAVE_FLAG);
        paint.setColor(shadowColor);
        paint.setStyle(Paint.Style.FILL);
        if (shadowType == SHADOW_TYPE_SOFT) {
            if (blurMaskFilter != null) {
                paint.setMaskFilter(blurMaskFilter);
            }
        } else {
            if (shadowLeftTopPath != null) {
                paint.setShader(leftTopShader);
                canvas.drawPath(shadowLeftTopPath, paint);
            }
            if (shadowRightTopPath != null) {
                paint.setShader(rightTopShader);
                canvas.drawPath(shadowRightTopPath, paint);
            }
            if (shadowRightBottomPath != null) {
                paint.setShader(rightBottomShader);
                canvas.drawPath(shadowRightBottomPath, paint);
            }
            if (shadowLeftBottomPath != null) {
                paint.setShader(leftBottomShader);
                canvas.drawPath(shadowLeftBottomPath, paint);
            }
            if (shadowToLeftPath != null) {
                paint.setShader(toLeftShader);
                canvas.drawPath(shadowToLeftPath, paint);
            }
            if (shadowToTopPath != null) {
                paint.setShader(toTopShader);
                canvas.drawPath(shadowToTopPath, paint);
            }
            if (shadowToRightPath != null) {
                paint.setShader(toRightShader);
                canvas.drawPath(shadowToRightPath, paint);
            }
            if (shadowToBottomPath != null) {
                paint.setShader(toBottomShader);
                canvas.drawPath(shadowToBottomPath, paint);
            }
            paint.setMaskFilter(null);
            paint.setShader(null);
        }
        canvas.drawPath(shadowInnerPath, paint);
        paint.setMaskFilter(null);
        paint.setXfermode(porterDuffDstOutMode);
        canvas.drawPath(innerPath, paint);
        paint.setXfermode(null);
        canvas.restore();
    }

    private void drawInsetShadow(Canvas canvas) {
        paint.setColor(shadowColor);
        paint.setStyle(Paint.Style.FILL);
        if (shadowType == SHADOW_TYPE_SOFT) {
            if (blurMaskFilter != null) {
                paint.setMaskFilter(blurMaskFilter);
            }
        } else {
            if (shadowLeftTopPath != null) {
                paint.setShader(leftTopShader);
                canvas.drawPath(shadowLeftTopPath, paint);
            }
            if (shadowRightTopPath != null) {
                paint.setShader(rightTopShader);
                canvas.drawPath(shadowRightTopPath, paint);
            }
            if (shadowRightBottomPath != null) {
                paint.setShader(rightBottomShader);
                canvas.drawPath(shadowRightBottomPath, paint);
            }
            if (shadowLeftBottomPath != null) {
                paint.setShader(leftBottomShader);
                canvas.drawPath(shadowLeftBottomPath, paint);
            }
            if (shadowToLeftPath != null) {
                paint.setShader(toLeftShader);
                canvas.drawPath(shadowToLeftPath, paint);
            }
            if (shadowToTopPath != null) {
                paint.setShader(toTopShader);
                canvas.drawPath(shadowToTopPath, paint);
            }
            if (shadowToRightPath != null) {
                paint.setShader(toRightShader);
                canvas.drawPath(shadowToRightPath, paint);
            }
            if (shadowToBottomPath != null) {
                paint.setShader(toBottomShader);
                canvas.drawPath(shadowToBottomPath, paint);
            }
        }
        if (shadowInnerPath == null) {
            paint.setShader(null);
            paint.setMaskFilter(null);
            return;
        }
        paint.setShader(null);
        canvas.drawPath(shadowInnerPath, paint);
        paint.setMaskFilter(null);
    }


    private void drawBorder(Canvas canvas) {
        if (borderPath == null) {
            return;
        }
        paint.setColor(boxBorderColor);
        paint.setStrokeWidth(boxBorderThickness);
        paint.setStyle(Paint.Style.STROKE);
        if (boxBorderType == BORDER_TYPE_DASHED) {
            paint.setPathEffect(new DashPathEffect(new float[]{boxBorderThickness * 2, boxBorderThickness}, 0));
        }
        canvas.drawPath(borderPath, paint);
        paint.setStrokeWidth(0);
        paint.setPathEffect(null);
    }

    private void restoreLayout() {
        if (!layoutChanged) {
            return;
        }
        layoutChanged = false;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
            marginLayoutParams.leftMargin = savedMargins.left;
            marginLayoutParams.topMargin = savedMargins.top;
            marginLayoutParams.rightMargin = savedMargins.right;
            marginLayoutParams.bottomMargin = savedMargins.bottom;
        }
        params.width = savedWidthHeight.getWidth();
        params.height = savedWidthHeight.getHeight();
        view.setLayoutParams(params);
        if (autoAddPadding) {
            view.setPadding(savedPaddings.left, savedPaddings.top, savedPaddings.right, savedPaddings.bottom);
        }
    }

    private void changeLayout() {
        layoutChanged = true;
        currentPaddings = getPaddingRect();
        Rect paddings = new Rect(currentPaddings.left, currentPaddings.top, currentPaddings.right, currentPaddings.bottom);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = savedWidthHeight.getWidth();
        params.height = savedWidthHeight.getHeight();
        if (autoDelMargin && params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
            marginLayoutParams.leftMargin = savedMargins.left;
            marginLayoutParams.topMargin = savedMargins.top;
            marginLayoutParams.rightMargin = savedMargins.right;
            marginLayoutParams.bottomMargin = savedMargins.bottom;
            int tmp;
            if (savedMargins.left > 0) {
                tmp = Math.min(savedMargins.left, paddings.left);
                marginLayoutParams.leftMargin -= tmp;
                paddings.left -= tmp;
                params.width += tmp;
            }
            if (savedMargins.top > 0) {
                tmp = Math.min(savedMargins.top, paddings.top);
                marginLayoutParams.topMargin -= tmp;
                paddings.top -= tmp;
                params.height += tmp;
            }
            if (savedMargins.right > 0) {
                tmp = Math.min(savedMargins.right, paddings.right);
                marginLayoutParams.rightMargin -= tmp;
                paddings.right -= tmp;
                params.width += tmp;
            }
            if (savedMargins.bottom > 0) {
                tmp = Math.min(savedMargins.bottom, paddings.bottom);
                marginLayoutParams.bottomMargin -= tmp;
                paddings.bottom -= tmp;
                params.height += tmp;
            }
        }
        if (autoAddWidthHeight) {
            params.width += paddings.left + paddings.right;
            params.height += paddings.top + paddings.bottom;
        }
        view.setLayoutParams(params);
        if (autoAddPadding) {
            view.setPadding(savedPaddings.left + currentPaddings.left, savedPaddings.top + currentPaddings.top,
                    savedPaddings.right + currentPaddings.right, savedPaddings.bottom + currentPaddings.bottom);
        }
    }

    private void rectPath(Path path, RectF rectF, boolean clockwise) {
        rectPath(path, rectF.left, rectF.top, rectF.right, rectF.bottom, clockwise, false);
    }

    private void rectPath(Path path, float left, float top, float right, float bottom, boolean clockwise) {
        rectPath(path, left, top, right, bottom, clockwise, false);
    }

    private void rectPath(Path path, float left, float top, float right, float bottom, boolean clockwise, boolean lineToStart) {
        if (clockwise) {
            // 顺时针
            if (lineToStart) {
                path.lineTo(left, top);
            } else {
                path.moveTo(left, top);
            }
            path.lineTo(right, top);
            path.lineTo(right, bottom);
            path.lineTo(left, bottom);
            path.lineTo(left, top);
        } else {
            // 逆时针
            if (lineToStart) {
                path.lineTo(left, top);
            } else {
                path.moveTo(left, top);
            }
            path.lineTo(left, bottom);
            path.lineTo(right, bottom);
            path.lineTo(right, top);
            path.lineTo(left, top);
        }
    }

    private void rectRadiusPath(Path path, RectF rectF) {
        rectRadiusPath(path, rectF.left, rectF.top, rectF.right, rectF.bottom, true, false, 0);
    }

    private void rectRadiusPath(Path path, RectF rectF, boolean clockwise, float thickness) {
        rectRadiusPath(path, rectF.left, rectF.top, rectF.right, rectF.bottom, clockwise, false, thickness);
    }

    /**
     * 根据当前 圆角半径绘制路径  未关闭
     */
    private void rectRadiusPath(
            Path path, float left, float top, float right, float bottom,
            boolean clockwise, boolean lineToStart, float thickness
    ) {
        float radiusLeftTop = Math.max(getRadiusLeftTop() + thickness, 0);
        float radiusRightTop = Math.max(getRadiusRightTop() + thickness, 0);
        float radiusRightBottom = Math.max(getRadiusRightBottom() + thickness, 0);
        float radiusLeftBottom = Math.max(getRadiusLeftBottom() + thickness, 0);
        IntegerBox angleTopLeft = new IntegerBox(),
                angleTopRight = new IntegerBox(),
                angleRightTop = new IntegerBox(),
                angleRightBottom = new IntegerBox(),
                angleBottomRight = new IntegerBox(),
                angleBottomLeft = new IntegerBox(),
                angleLeftBottom = new IntegerBox(),
                angleLeftTop = new IntegerBox();
        getAngles(angleTopLeft, angleTopRight, angleRightTop, angleRightBottom,
                angleBottomRight, angleBottomLeft, angleLeftBottom, angleLeftTop,
                left, top, right, bottom,
                radiusLeftTop, radiusRightTop, radiusRightBottom, radiusLeftBottom);
        float doubleRadius;
        if (clockwise) {
            // 顺时针
            if (radiusLeftTop == 0) {
                if (lineToStart) {
                    path.lineTo(left, top);
                } else {
                    path.moveTo(left, top);
                }
            } else {
                if (lineToStart) {
                    path.lineTo(left + radiusLeftTop, top);
                } else {
                    path.moveTo(left + radiusLeftTop, top);
                }
            }
            if (radiusRightTop == 0) {
                path.lineTo(right, top);
            } else {
                doubleRadius = radiusRightTop * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    path.arcTo(right - doubleRadius, top, right, top + doubleRadius,
                            angleTopRight.value, angleRightTop.value - angleTopRight.value, false);
                } else {
                    path.arcTo(new RectF(right - doubleRadius, top, right, top + doubleRadius),
                            angleTopRight.value, angleRightTop.value - angleTopRight.value, false);
                }
            }
            if (radiusRightBottom == 0) {
                path.lineTo(right, bottom);
            } else {
                doubleRadius = radiusRightBottom * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    path.arcTo(right - doubleRadius, bottom - doubleRadius, right, bottom,
                            angleRightBottom.value, angleBottomRight.value - angleRightBottom.value, false);
                } else {
                    path.arcTo(new RectF(right - doubleRadius, bottom - doubleRadius, right, bottom),
                            angleRightBottom.value, angleBottomRight.value - angleRightBottom.value, false);
                }
            }
            if (radiusLeftBottom == 0) {
                path.lineTo(left, bottom);
            } else {
                doubleRadius = radiusLeftBottom * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    path.arcTo(left, bottom - doubleRadius, left + doubleRadius, bottom,
                            angleBottomLeft.value, angleLeftBottom.value - angleBottomLeft.value, false);
                } else {
                    path.arcTo(new RectF(left, bottom - doubleRadius, left + doubleRadius, bottom),
                            angleBottomLeft.value, angleLeftBottom.value - angleBottomLeft.value, false);
                }
            }
            if (radiusLeftTop == 0) {
                path.lineTo(left, top);
            } else {
                doubleRadius = radiusLeftTop * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    path.arcTo(left, top, left + doubleRadius, top + doubleRadius,
                            angleLeftTop.value, angleTopLeft.value - angleLeftTop.value, false);
                } else {
                    path.arcTo(new RectF(left, top, left + doubleRadius, top + doubleRadius),
                            angleLeftTop.value, angleTopLeft.value - angleLeftTop.value, false);
                }
            }
        } else {
            // 逆时针
            if (radiusLeftTop == 0) {
                if (lineToStart) {
                    path.lineTo(left, top);
                } else {
                    path.moveTo(left, top);
                }
            } else {
                doubleRadius = radiusLeftTop * 2f;
                if (lineToStart) {
                    path.lineTo(left + radiusLeftTop, top);
                } else {
                    path.moveTo(left + radiusLeftTop, top);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    path.arcTo(left, top, left + doubleRadius, top + doubleRadius,
                            angleTopLeft.value, angleLeftTop.value - angleTopLeft.value, false);
                } else {
                    path.arcTo(new RectF(left, top, left + doubleRadius, top + doubleRadius),
                            angleTopLeft.value, angleLeftTop.value - angleTopLeft.value, false);
                }
            }
            if (radiusLeftBottom == 0) {
                path.lineTo(left, bottom);
            } else {
                doubleRadius = radiusLeftBottom * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    path.arcTo(left, bottom - doubleRadius, left + doubleRadius, bottom,
                            angleLeftBottom.value, angleBottomLeft.value - angleLeftBottom.value, false);
                } else {
                    path.arcTo(new RectF(left, bottom - doubleRadius, left + doubleRadius, bottom),
                            angleLeftBottom.value, angleBottomLeft.value - angleLeftBottom.value, false);
                }
            }
            if (radiusRightBottom == 0) {
                path.lineTo(right, bottom);
            } else {
                doubleRadius = radiusRightBottom * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    path.arcTo(right - doubleRadius, bottom - doubleRadius, right, bottom,
                            angleBottomRight.value, angleRightBottom.value - angleBottomRight.value, false);
                } else {
                    path.arcTo(new RectF(right - doubleRadius, bottom - doubleRadius, right, bottom),
                            angleBottomRight.value, angleRightBottom.value - angleBottomRight.value, false);
                }
            }
            if (radiusRightTop == 0) {
                path.lineTo(right, top);
            } else {
                doubleRadius = radiusRightTop * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    path.arcTo(right - doubleRadius, top, right, top + doubleRadius,
                            angleRightTop.value, angleTopRight.value - angleRightTop.value, false);
                } else {
                    path.arcTo(new RectF(right - doubleRadius, top, right, top + doubleRadius),
                            angleRightTop.value, angleTopRight.value - angleRightTop.value, false);
                }
            }
            if (radiusLeftTop == 0) {
                path.lineTo(left, top);
            } else {
                path.lineTo(left + radiusLeftTop, top);
            }
        }
    }

    private void obtainRadiusSplitPathAndShader(
            PathBox leftTopPath, PathBox rightTopPath, PathBox rightBottomPath, PathBox leftBottomPath,
            PathBox leftPath, PathBox topPath, PathBox rightPath, PathBox bottomPath,
            ShaderBox leftTopShader, ShaderBox rightTopShader, ShaderBox rightBottomShader, ShaderBox leftBottomShader,
            ShaderBox leftShader, ShaderBox topShader, ShaderBox rightShader, ShaderBox bottomShader,
            float rectLeft, float rectTop, float rectRight, float rectBottom, float thickness,
            @ColorInt int innerColor, @ColorInt int outerColor) {
        obtainRadiusSplitPathAndShader(leftTopPath, rightTopPath, rightBottomPath, leftBottomPath,
                leftPath, topPath, rightPath, bottomPath,
                leftTopShader, rightTopShader, rightBottomShader, leftBottomShader,
                leftShader, topShader, rightShader, bottomShader,
                rectLeft, rectTop, rectRight, rectBottom, thickness,
                innerColor, outerColor, true);
    }

    private void obtainRadiusSplitPathAndShader(
            PathBox leftTopPath, PathBox rightTopPath, PathBox rightBottomPath, PathBox leftBottomPath,
            PathBox leftPath, PathBox topPath, PathBox rightPath, PathBox bottomPath,
            ShaderBox leftTopShader, ShaderBox rightTopShader, ShaderBox rightBottomShader, ShaderBox leftBottomShader,
            ShaderBox leftShader, ShaderBox topShader, ShaderBox rightShader, ShaderBox bottomShader,
            float rectLeft, float rectTop, float rectRight, float rectBottom, float thickness,
            @ColorInt int innerColor, @ColorInt int outerColor, boolean colorToOuter) {
        // thickness 正外 负内
        float radiusLeftTop = getRadiusLeftTop();
        float radiusRightTop = getRadiusRightTop();
        float radiusRightBottom = getRadiusRightBottom();
        float radiusLeftBottom = getRadiusLeftBottom();
        if (thickness > 0) {
            obtainRadiusSplitPathAndShader(leftTopPath, rightTopPath, rightBottomPath, leftBottomPath,
                    leftPath, topPath, rightPath, bottomPath,
                    leftTopShader, rightTopShader, rightBottomShader, leftBottomShader,
                    leftShader, topShader, rightShader, bottomShader,
                    rectLeft - thickness, rectTop - thickness,
                    rectRight + thickness, rectBottom + thickness,
                    rectLeft, rectTop, rectRight, rectBottom,
                    radiusLeftTop + thickness, radiusRightTop + thickness,
                    radiusRightBottom + thickness, radiusLeftBottom + thickness,
                    radiusLeftTop, radiusRightTop, radiusRightBottom, radiusLeftBottom,
                    innerColor, outerColor, colorToOuter);
        } else {
            thickness = -thickness;
            obtainRadiusSplitPathAndShader(leftTopPath, rightTopPath, rightBottomPath, leftBottomPath,
                    leftPath, topPath, rightPath, bottomPath,
                    leftTopShader, rightTopShader, rightBottomShader, leftBottomShader,
                    leftShader, topShader, rightShader, bottomShader,
                    rectLeft, rectTop, rectRight, rectBottom,
                    rectLeft + thickness, rectTop + thickness,
                    rectRight - thickness, rectBottom - thickness,
                    radiusLeftTop, radiusRightTop, radiusRightBottom, radiusLeftBottom,
                    radiusLeftTop - thickness, radiusRightTop - thickness,
                    radiusRightBottom - thickness, radiusLeftBottom - thickness,
                    innerColor, outerColor, colorToOuter);
        }
    }

    private boolean shadowThicknessZero() {
        return shadowSpread + shadowBlur < 0.5f;
    }

    private boolean shadowBlurZero() {
        return shadowBlur < 0.5f;
    }

    private boolean borderThicknessZero() {
        return boxBorderThickness < 0.5f;
    }

    private boolean allRadiusZero() {
        return getRadiusLeftTop() == 0 && getRadiusRightTop() == 0 && getRadiusRightBottom() == 0 && getRadiusLeftBottom() == 0;
    }

    private PwPhValue getPhPwValue(TypedArray arr, @StyleableRes int resId) {
        PwPhValue value = new PwPhValue();
        int type;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            type = arr.getType(resId);
        } else {
            type = arr.peekValue(resId).type;
        }
        if (type == TypedValue.TYPE_STRING) {
            value.percent = true;
            String tmp = arr.getString(resId);
            if (tmp == null || tmp.length() == 0) {
                value.value = 0;
            } else {
                // 从字符串里取出数字
                if (tmp.endsWith("pw")) {
                    //noinspection DuplicateExpressions
                    value.value = Integer.parseInt(tmp.substring(0, tmp.length() - 2));
                    value.isWidth = true;
                } else if (tmp.endsWith("ph")) {
                    //noinspection DuplicateExpressions
                    value.value = Integer.parseInt(tmp.substring(0, tmp.length() - 2));
                } else {
                    value.value = Integer.parseInt(tmp);
                }
            }
        } else {
            value.value = arr.getDimensionPixelSize(resId, 0);
        }
        return value;
    }

    private PwPhValue getPhPwValue(TypedArray arr, @StyleableRes int resId, PwPhValue defaultValue) {
        PwPhValue value = new PwPhValue();
        int type;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            type = arr.getType(resId);
        } else {
            type = arr.peekValue(resId).type;
        }
        if (type == TypedValue.TYPE_NULL) {
            value.value = defaultValue.value;
            value.percent = defaultValue.percent;
            value.isWidth = defaultValue.isWidth;
        } else if (type == TypedValue.TYPE_STRING) {
            value.percent = true;
            String tmp = arr.getString(resId);
            if (tmp == null || tmp.length() == 0) {
                value.value = 0;
            } else {
                // 从字符串里取出数字
                if (tmp.endsWith("pw")) {
                    //noinspection DuplicateExpressions
                    value.value = Integer.parseInt(tmp.substring(0, tmp.length() - 2));
                    value.isWidth = true;
                } else if (tmp.endsWith("ph")) {
                    //noinspection DuplicateExpressions
                    value.value = Integer.parseInt(tmp.substring(0, tmp.length() - 2));
                } else {
                    value.value = Integer.parseInt(tmp);
                }
            }
        } else {
            value.value = arr.getDimensionPixelSize(resId, 0);
        }
        return value;
    }

    private float getRadiusLeftTop() {
        if (!boxRadiusLeftTop.percent) {
            return boxRadiusLeftTop.value;
        }
        if (boxRadiusLeftTop.value == 0) {
            return 0;
        }
        float percent = Math.min(100f, boxRadiusLeftTop.value) / 100f;
        if (boxRadiusLeftTop.isWidth) {
            return validW * percent;
        }
        return validH * percent;
    }

    private float getRadiusRightTop() {
        if (!boxRadiusRightTop.percent) {
            return boxRadiusRightTop.value;
        }
        if (boxRadiusRightTop.value == 0) {
            return 0;
        }
        float percent = Math.min(100f, boxRadiusRightTop.value) / 100f;
        if (boxRadiusRightTop.isWidth) {
            return validW * percent;
        }
        return validH * percent;
    }

    private float getRadiusRightBottom() {
        if (!boxRadiusRightBottom.percent) {
            return boxRadiusRightBottom.value;
        }
        if (boxRadiusRightBottom.value == 0) {
            return 0;
        }
        float percent = Math.min(100f, boxRadiusRightBottom.value) / 100f;
        if (boxRadiusRightBottom.isWidth) {
            return validW * percent;
        }
        return validH * percent;
    }

    private float getRadiusLeftBottom() {
        if (!boxRadiusLeftBottom.percent) {
            return boxRadiusLeftBottom.value;
        }
        if (boxRadiusLeftBottom.value == 0) {
            return 0;
        }
        float percent = Math.min(100f, boxRadiusLeftBottom.value) / 100f;
        if (boxRadiusLeftBottom.isWidth) {
            return validW * percent;
        }
        return validH * percent;
    }

    /**
     * 获取内部矩形区域
     */
    @Nullable
    private RectF getInnerArea(int w, int h) {
        float dis = shadowSpread + shadowBlur;
        RectF rectF = new RectF(dis, dis, dis, dis);
        // 外阴影 dx 增加 整体左移 来绘制右侧阴影
        // dy 增加 整体上移 来绘制下侧阴影 出现负值需要偏移到可见区域
        rectF.left -= shadowDx;
        rectF.top -= shadowDy;
        rectF.right += shadowDx;
        rectF.bottom += shadowDy;
        // 如果有小于0的值,再次偏移
        if (rectF.right < 0) {
            rectF.left -= rectF.right;
            rectF.right = 0;
        } else if (rectF.left < 0) {
            rectF.right -= rectF.left;
            rectF.left = 0;
        }
        if (rectF.bottom < 0) {
            rectF.top -= rectF.bottom;
            rectF.bottom = 0;
        } else if (rectF.top < 0) {
            rectF.bottom -= rectF.top;
            rectF.top = 0;
        }
        if ((shadowClip & SHADOW_CLIP_LEFT) == SHADOW_CLIP_LEFT) {
            rectF.left = 0;
        }
        if ((shadowClip & SHADOW_CLIP_TOP) == SHADOW_CLIP_TOP) {
            rectF.top = 0;
        }
        if ((shadowClip & SHADOW_CLIP_RIGHT) == SHADOW_CLIP_RIGHT) {
            rectF.right = 0;
        }
        if ((shadowClip & SHADOW_CLIP_BOTTOM) == SHADOW_CLIP_BOTTOM) {
            rectF.bottom = 0;
        }
        rectF.right = w - rectF.right;
        rectF.bottom = h - rectF.bottom;
        if (rectNoSize(rectF)) {
            return null;
        }
        return rectF;
    }

    /**
     * 获取外部区域 圆角外或者阴影区域
     */
    @Nullable
    private Path getOuterPath(int w, int h) {
        if (shadowInset) {
            if (allRadiusZero) {
                return null;
            } else {
                Path path = new Path();
                RectF rectF = new RectF(0, 0, w, h);
                rectRadiusPath(path, rectF);
                rectPath(path, rectF, false);
                return path;
            }
        }
        if (shadowThicknessZero && allRadiusZero) {
            return null;
        }
        Path path = new Path();
        RectF innerArea = getInnerArea(w, h);
        if (innerArea == null) {
            return null;
        } else {
            rectRadiusPath(path, innerArea);
            rectPath(path, 0, 0, w, h, false, false);
        }
        return path;
    }

    @Nullable
    private Path getClipPath(int w, int h) {
        if (shadowInset) {
            if (allRadiusZero) {
                return null;
            }
            Path path = new Path();
            rectRadiusPath(path, 0, 0, w, h, true, false, 0);
            return path;
        }
        if (shadowThicknessZero && allRadiusZero) {
            return null;
        }
        Path path = new Path();
        RectF innerArea = getInnerArea(w, h);
        if (innerArea == null) {
            return null;
        } else {
            rectRadiusPath(path, innerArea);
        }
        return path;
    }

    /**
     * 获取内部矩形区域路径
     */
    @Nullable
    private Path getInnerPath(int w, int h) {
        RectF innerArea = getInnerArea(w, h);
        if (innerArea == null) {
            return null;
        }
        Path path = new Path();
        rectRadiusPath(path, innerArea);
        return path;
    }

    /**
     * 获取阴影内部路径 阴影内部区域 减去绘制内部区域
     */
    @Nullable
    private Path getShadowInnerPath(int w, int h) {
        RectF rectF = getInnerArea(w, h);
        if (rectF == null) {
            return null;
        }
        rectF.left -= shadowSpread;
        rectF.top -= shadowSpread;
        rectF.right += shadowSpread;
        rectF.bottom += shadowSpread;
        rectF.left += shadowDx;
        rectF.top += shadowDy;
        rectF.right += shadowDx;
        rectF.bottom += shadowDy;
        if (rectNoSize(rectF)) {
            return null;
        }
        Path path = new Path();
        rectRadiusPath(path, rectF);
        return path;
    }

    @Nullable
    private Path getInsetShadowPath(int w, int h) {
        Path path = new Path();
        float dis = shadowSpread + boxBorderThickness;
        RectF rectF = new RectF(dis, dis, w - dis, h - dis);
        if (rectNoSize(rectF)) {
            return null;
        }
        rectF.left += shadowDx;
        rectF.top += shadowDy;
        rectF.right += shadowDx;
        rectF.bottom += shadowDy;
        rectRadiusPath(path, rectF, true, -boxBorderThickness);
        rectPath(path, -shadowBlur, -shadowBlur, w + shadowBlur, h + shadowBlur, false);
        return path;
    }

    /**
     * 获取边框路径
     */
    @Nullable
    private Path getBorderPath(int w, int h) {
        if (borderThicknessZero) {
            return null;
        }
        RectF rectF;
        if (shadowInset) {
            rectF = new RectF(0, 0, w, h);
        } else {
            rectF = getInnerArea(w, h);
        }
        if (rectF == null) {
            return null;
        }
        float halfBorderThickness = boxBorderThickness / 2f - 0.5f;
        rectF.left += halfBorderThickness;
        rectF.top += halfBorderThickness;
        rectF.right -= halfBorderThickness;
        rectF.bottom -= halfBorderThickness;
        if (rectNoSize(rectF)) {
            return null;
        }
        Path path = new Path();
        rectRadiusPath(path, rectF, true, -halfBorderThickness);
        return path;
    }

    private void obtainOuterShadowSplitPathAndShader() {
        leftTopShader = null;
        rightTopShader = null;
        rightBottomShader = null;
        leftBottomShader = null;
        toLeftShader = null;
        toTopShader = null;
        toRightShader = null;
        toBottomShader = null;
        if (shadowBlurZero) {
            return;
        }
        // shadowInnerPath 不为空才调用 所有这里不可能为空
        RectF shadowInnerArea = getInnerArea(currentW, currentH);
        //noinspection ConstantConditions
        shadowInnerArea.left -= shadowSpread;
        shadowInnerArea.top -= shadowSpread;
        shadowInnerArea.right += shadowSpread;
        shadowInnerArea.bottom += shadowSpread;
        shadowInnerArea.left += shadowDx;
        shadowInnerArea.top += shadowDy;
        shadowInnerArea.right += shadowDx;
        shadowInnerArea.bottom += shadowDy;
        if (rectNoSize(shadowInnerArea)) {
            shadowInnerPath = null;
            return;
        }
        shadowInnerPath = new Path();
        rectRadiusPath(shadowInnerPath, shadowInnerArea);
        obtainShadowSplitPathAndShader(shadowInnerArea, shadowBlur, shadowColor, Color.TRANSPARENT);
    }


    private void obtainInsetShadowSplitPathAndShader() {
        leftTopShader = null;
        rightTopShader = null;
        rightBottomShader = null;
        leftBottomShader = null;
        toLeftShader = null;
        toTopShader = null;
        toRightShader = null;
        toBottomShader = null;
        shadowInnerPath = null;
        if (shadowBlurZero) {
            return;
        }
        float dis = shadowSpread + boxBorderThickness;
        final RectF shadowOuterArea = new RectF(dis, dis, currentW - dis, currentH - dis);
        shadowOuterArea.left += shadowDx;
        shadowOuterArea.top += shadowDy;
        shadowOuterArea.right += shadowDx;
        shadowOuterArea.bottom += shadowDy;
        if (shadowOuterArea.left != 0 || shadowOuterArea.top != 0 ||
                shadowOuterArea.right != currentW || shadowOuterArea.bottom != currentH) {
            shadowInnerPath = new Path();
            rectRadiusPath(shadowInnerPath, shadowOuterArea);
            rectRadiusPath(shadowInnerPath, 0, 0, currentW, currentH, false, true, 0);
        } // else 没厚度
        obtainShadowSplitPathAndShader(shadowOuterArea, -shadowBlur, Color.TRANSPARENT, shadowColor);
    }

    private void obtainShadowSplitPathAndShader(RectF area, float thickness, @ColorInt int innerColor, @ColorInt int outerColor) {
        PathBox leftTopPathBox = new PathBox(),
                rightTopPathBox = new PathBox(),
                rightBottomPathBox = new PathBox(),
                leftBottomPathBox = new PathBox(),
                leftPathBox = new PathBox(),
                topPathBox = new PathBox(),
                rightPathBox = new PathBox(),
                bottomPathBox = new PathBox();
        ShaderBox leftTopShaderBox = new ShaderBox(),
                rightTopShaderBox = new ShaderBox(),
                rightBottomShaderBox = new ShaderBox(),
                leftBottomShaderBox = new ShaderBox(),
                leftShaderBox = new ShaderBox(),
                topShaderBox = new ShaderBox(),
                rightShaderBox = new ShaderBox(),
                bottomShaderBox = new ShaderBox();
        obtainRadiusSplitPathAndShader(leftTopPathBox, rightTopPathBox, rightBottomPathBox, leftBottomPathBox,
                leftPathBox, topPathBox, rightPathBox, bottomPathBox,
                leftTopShaderBox, rightTopShaderBox, rightBottomShaderBox, leftBottomShaderBox,
                leftShaderBox, topShaderBox, rightShaderBox, bottomShaderBox,
                area.left, area.top, area.right, area.bottom, thickness,
                innerColor, outerColor);
        shadowLeftTopPath = leftTopPathBox.value;
        shadowRightTopPath = rightTopPathBox.value;
        shadowRightBottomPath = rightBottomPathBox.value;
        shadowLeftBottomPath = leftBottomPathBox.value;
        shadowToLeftPath = leftPathBox.value;
        shadowToTopPath = topPathBox.value;
        shadowToRightPath = rightPathBox.value;
        shadowToBottomPath = bottomPathBox.value;
        leftTopShader = leftTopShaderBox.value;
        rightTopShader = rightTopShaderBox.value;
        rightBottomShader = rightBottomShaderBox.value;
        leftBottomShader = leftBottomShaderBox.value;
        toLeftShader = leftShaderBox.value;
        toTopShader = topShaderBox.value;
        toRightShader = rightShaderBox.value;
        toBottomShader = bottomShaderBox.value;
    }

    @SuppressWarnings("SameParameterValue")
    private int dpi2px(float dpi) {
        Context context = view.getContext();
        return (int) (context.getResources().getDisplayMetrics().density * dpi + 0.5f);
    }

    private static boolean rectNoSize(RectF rectF) {
        return rectF.right <= rectF.left || rectF.bottom <= rectF.top;
    }

    private static int getAngle(float y, float x) {
        return (int) Math.toDegrees(Math.atan2(y, x));
    }

    private static void getAngles(
            IntegerBox angleTopLeft, IntegerBox angleTopRight, IntegerBox angleRightTop, IntegerBox angleRightBottom,
            IntegerBox angleBottomRight, IntegerBox angleBottomLeft, IntegerBox angleLeftBottom, IntegerBox angleLeftTop,
            float left, float top, float right, float bottom,
            float radiusLeftTop, float radiusRightTop, float radiusRightBottom, float radiusLeftBottom
    ) {
        FloatBox sizeTopLeft = new FloatBox();
        FloatBox sizeTopRight = new FloatBox();
        FloatBox sizeRightTop = new FloatBox();
        FloatBox sizeRightBottom = new FloatBox();
        FloatBox sizeBottomRight = new FloatBox();
        FloatBox sizeBottomLeft = new FloatBox();
        FloatBox sizeLeftBottom = new FloatBox();
        FloatBox sizeLeftTop = new FloatBox();
        getAngles(angleTopLeft, angleTopRight, angleRightTop, angleRightBottom,
                angleBottomRight, angleBottomLeft, angleLeftBottom, angleLeftTop,
                sizeTopLeft, sizeTopRight, sizeRightTop, sizeRightBottom,
                sizeBottomRight, sizeBottomLeft, sizeLeftBottom, sizeLeftTop,
                left, top, right, bottom,
                radiusLeftTop, radiusRightTop, radiusRightBottom, radiusLeftBottom);
    }

    private static void getAngles(
            IntegerBox angleTopLeft, IntegerBox angleTopRight, IntegerBox angleRightTop, IntegerBox angleRightBottom,
            IntegerBox angleBottomRight, IntegerBox angleBottomLeft, IntegerBox angleLeftBottom, IntegerBox angleLeftTop,
            FloatBox sizeTopLeft, FloatBox sizeTopRight, FloatBox sizeRightTop, FloatBox sizeRightBottom,
            FloatBox sizeBottomRight, FloatBox sizeBottomLeft, FloatBox sizeLeftBottom, FloatBox sizeLeftTop,
            float left, float top, float right, float bottom,
            float radiusLeftTop, float radiusRightTop, float radiusRightBottom, float radiusLeftBottom
    ) {
        sizeTopLeft.value = radiusLeftTop;
        sizeTopRight.value = radiusRightTop;
        sizeRightTop.value = radiusRightTop;
        sizeRightBottom.value = radiusRightBottom;
        sizeBottomRight.value = radiusRightBottom;
        sizeBottomLeft.value = radiusLeftBottom;
        sizeLeftBottom.value = radiusLeftBottom;
        sizeLeftTop.value = radiusLeftTop;
        float width = right - left;
        float height = bottom - top;
        float radiusSubLeft = radiusLeftTop + radiusLeftBottom;
        float radiusSubRight = radiusRightTop + radiusRightBottom;
        float radiusSubTop = radiusLeftTop + radiusRightTop;
        float radiusSubBottom = radiusLeftBottom + radiusRightBottom;
        float scaleLeft, scaleRight, scaleTop, scaleBottom;
        if (radiusSubLeft < height) {
            scaleLeft = 1f;
        } else {
            scaleLeft = height / radiusSubLeft;
        }
        if (radiusSubRight < height) {
            scaleRight = 1f;
        } else {
            scaleRight = height / radiusSubRight;
        }
        if (radiusSubTop < width) {
            scaleTop = 1f;
        } else {
            scaleTop = width / radiusSubTop;
        }
        if (radiusSubBottom < width) {
            scaleBottom = 1f;
        } else {
            scaleBottom = width / radiusSubBottom;
        }
        sizeTopLeft.value *= scaleTop;
        sizeTopRight.value *= scaleTop;
        sizeRightTop.value *= scaleRight;
        sizeRightBottom.value *= scaleRight;
        sizeBottomRight.value *= scaleBottom;
        sizeBottomLeft.value *= scaleBottom;
        sizeLeftBottom.value *= scaleLeft;
        sizeLeftTop.value *= scaleLeft;
        angleTopLeft.value = 270 - getAngle(radiusLeftTop - sizeTopLeft.value, radiusLeftTop);
        angleTopRight.value = 270 + getAngle(radiusRightTop - sizeTopRight.value, radiusRightTop);
        angleRightTop.value = 360 - getAngle(radiusRightTop - sizeRightTop.value, radiusRightTop);
        angleRightBottom.value = getAngle(radiusRightBottom - sizeRightBottom.value, radiusRightBottom);
        angleBottomRight.value = 90 - getAngle(radiusRightBottom - sizeBottomRight.value, radiusRightBottom);
        angleBottomLeft.value = 90 + getAngle(radiusLeftBottom - sizeBottomLeft.value, radiusLeftBottom);
        angleLeftBottom.value = 180 - getAngle(radiusLeftBottom - sizeLeftBottom.value, radiusLeftBottom);
        angleLeftTop.value = 180 + getAngle(radiusLeftTop - sizeLeftTop.value, radiusLeftTop);
    }

    private static void obtainRadiusSplitPathAndShader(
            PathBox leftTopPath, PathBox rightTopPath, PathBox rightBottomPath, PathBox leftBottomPath,
            PathBox leftPath, PathBox topPath, PathBox rightPath, PathBox bottomPath,
            ShaderBox leftTopShader, ShaderBox rightTopShader, ShaderBox rightBottomShader, ShaderBox leftBottomShader,
            ShaderBox leftShader, ShaderBox topShader, ShaderBox rightShader, ShaderBox bottomShader,
            float outerLeft, float outerTop, float outerRight, float outerBottom,
            float innerLeft, float innerTop, float innerRight, float innerBottom,
            float outerRadiusLeftTop, float outerRadiusRightTop, float outerRadiusRightBottom, float outerRadiusLeftBottom,
            float innerRadiusLeftTop, float innerRadiusRightTop, float innerRadiusRightBottom, float innerRadiusLeftBottom,
            @ColorInt int innerColor, @ColorInt int outerColor, boolean colorToOuter
    ) {
        if (outerRight <= outerLeft || outerBottom <= outerTop) {
            return;
        }
        int[] color3 = new int[]{innerColor, colorToOuter ? innerColor : outerColor, outerColor};
        int[] color2 = new int[]{innerColor, outerColor};
        if (innerRight < innerLeft) {
            innerLeft = innerRight = (innerLeft + innerRight) / 2f;
            // 没有尺寸
            innerRadiusLeftTop = innerRadiusRightTop = innerRadiusRightBottom = innerRadiusLeftBottom = 0;
        }
        if (innerBottom < innerTop) {
            innerTop = innerBottom = (innerTop + innerBottom) / 2f;
            // 没有尺寸
            innerRadiusLeftTop = innerRadiusRightTop = innerRadiusRightBottom = innerRadiusLeftBottom = 0;
        }
        float disLeft = innerLeft - outerLeft;
        float disTop = innerTop - outerTop;
        float disRight = outerRight - innerRight;
        float disBottom = outerBottom - innerBottom;
        innerRadiusLeftTop = Math.max(innerRadiusLeftTop, 0);
        innerRadiusRightTop = Math.max(innerRadiusRightTop, 0);
        innerRadiusRightBottom = Math.max(innerRadiusRightBottom, 0);
        innerRadiusLeftBottom = Math.max(innerRadiusLeftBottom, 0);
        IntegerBox outerAngleTopLeft = new IntegerBox(),
                outerAngleTopRight = new IntegerBox(),
                outerAngleRightTop = new IntegerBox(),
                outerAngleRightBottom = new IntegerBox(),
                outerAngleBottomRight = new IntegerBox(),
                outerAngleBottomLeft = new IntegerBox(),
                outerAngleLeftBottom = new IntegerBox(),
                outerAngleLeftTop = new IntegerBox();
        FloatBox outerSizeTopLeft = new FloatBox();
        FloatBox outerSizeTopRight = new FloatBox();
        FloatBox outerSizeRightTop = new FloatBox();
        FloatBox outerSizeRightBottom = new FloatBox();
        FloatBox outerSizeBottomRight = new FloatBox();
        FloatBox outerSizeBottomLeft = new FloatBox();
        FloatBox outerSizeLeftBottom = new FloatBox();
        FloatBox outerSizeLeftTop = new FloatBox();
        getAngles(outerAngleTopLeft, outerAngleTopRight, outerAngleRightTop, outerAngleRightBottom,
                outerAngleBottomRight, outerAngleBottomLeft, outerAngleLeftBottom, outerAngleLeftTop,
                outerSizeTopLeft, outerSizeTopRight, outerSizeRightTop, outerSizeRightBottom,
                outerSizeBottomRight, outerSizeBottomLeft, outerSizeLeftBottom, outerSizeLeftTop,
                outerLeft, outerTop, outerRight, outerBottom,
                outerRadiusLeftTop, outerRadiusRightTop, outerRadiusRightBottom, outerRadiusLeftBottom);
        IntegerBox innerAngleTopLeft = new IntegerBox(),
                innerAngleTopRight = new IntegerBox(),
                innerAngleRightTop = new IntegerBox(),
                innerAngleRightBottom = new IntegerBox(),
                innerAngleBottomRight = new IntegerBox(),
                innerAngleBottomLeft = new IntegerBox(),
                innerAngleLeftBottom = new IntegerBox(),
                innerAngleLeftTop = new IntegerBox();
        FloatBox innerSizeTopLeft = new FloatBox();
        FloatBox innerSizeTopRight = new FloatBox();
        FloatBox innerSizeRightTop = new FloatBox();
        FloatBox innerSizeRightBottom = new FloatBox();
        FloatBox innerSizeBottomRight = new FloatBox();
        FloatBox innerSizeBottomLeft = new FloatBox();
        FloatBox innerSizeLeftBottom = new FloatBox();
        FloatBox innerSizeLeftTop = new FloatBox();
        getAngles(innerAngleTopLeft, innerAngleTopRight, innerAngleRightTop, innerAngleRightBottom,
                innerAngleBottomRight, innerAngleBottomLeft, innerAngleLeftBottom, innerAngleLeftTop,
                innerSizeTopLeft, innerSizeTopRight, innerSizeRightTop, innerSizeRightBottom,
                innerSizeBottomRight, innerSizeBottomLeft, innerSizeLeftBottom, innerSizeLeftTop,
                innerLeft, innerTop, innerRight, innerBottom,
                innerRadiusLeftTop, innerRadiusRightTop, innerRadiusRightBottom, innerRadiusLeftBottom);
        float doubleRadius;
        if (outerRadiusLeftTop != 0) {
            // 画外圆角
            leftTopPath.value = new Path();
            doubleRadius = outerRadiusLeftTop * 2f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                leftTopPath.value.arcTo(outerLeft, outerTop, outerLeft + doubleRadius, outerTop + doubleRadius,
                        outerAngleLeftTop.value, outerAngleTopLeft.value - outerAngleLeftTop.value, true);
            } else {
                leftTopPath.value.arcTo(new RectF(outerLeft, outerTop, outerLeft + doubleRadius, outerTop + doubleRadius),
                        outerAngleLeftTop.value, outerAngleTopLeft.value - outerAngleLeftTop.value, true);
            }
            if (innerRadiusLeftTop == 0) {
                leftTopPath.value.lineTo(innerLeft, innerTop);
                // 设置左上角阴影
                float shaderRadiusLeftTop;
                float shaderLeftTopOffsetX = 0, shaderLeftTopOffsetY = 0;
                if (disTop > disLeft) {
                    shaderRadiusLeftTop = disTop;
                    shaderLeftTopOffsetX = disLeft - disTop;
                } else {
                    shaderRadiusLeftTop = disLeft;
                    shaderLeftTopOffsetY = disTop - disLeft;
                }
                leftTopShader.value = new RadialGradient(innerLeft + shaderLeftTopOffsetX, innerTop + shaderLeftTopOffsetY,
                        shaderRadiusLeftTop, color2, null, Shader.TileMode.CLAMP);
            } else {
                // 画内圆角
                doubleRadius = innerRadiusLeftTop * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    leftTopPath.value.arcTo(innerLeft, innerTop, innerLeft + doubleRadius, innerTop + doubleRadius,
                            innerAngleTopLeft.value, innerAngleLeftTop.value - innerAngleTopLeft.value, false);
                } else {
                    leftTopPath.value.arcTo(new RectF(innerLeft, innerTop, innerLeft + doubleRadius, innerTop + doubleRadius),
                            innerAngleTopLeft.value, innerAngleLeftTop.value - innerAngleTopLeft.value, false);
                }
                // 设置左上角阴影
                leftTopShader.value = new RadialGradient(outerLeft + outerRadiusLeftTop,
                        outerTop + outerRadiusLeftTop, outerRadiusLeftTop, color3,
                        new float[]{0, innerRadiusLeftTop / outerRadiusLeftTop, 1}, Shader.TileMode.CLAMP);
            }
            leftTopPath.value.close();
        } // else // 没有外圆角 内圆角也将没有
        if (outerRadiusRightTop != 0) {
            // 画外圆角
            rightTopPath.value = new Path();
            doubleRadius = outerRadiusRightTop * 2f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                rightTopPath.value.arcTo(outerRight - doubleRadius, outerTop, outerRight, outerTop + doubleRadius,
                        outerAngleTopRight.value, outerAngleRightTop.value - outerAngleTopRight.value, false);
            } else {
                rightTopPath.value.arcTo(new RectF(outerRight - doubleRadius, outerTop, outerRight, outerTop + doubleRadius),
                        outerAngleTopRight.value, outerAngleRightTop.value - outerAngleTopRight.value, false);
            }
            if (innerRadiusRightTop == 0) {
                rightTopPath.value.lineTo(innerRight, innerTop);
                // 设置右上角阴影
                float shaderRadiusRightTop;
                float shaderRightTopOffsetX = 0, shaderRightTopOffsetY = 0;
                if (disTop > disRight) {
                    shaderRadiusRightTop = disTop;
                    shaderRightTopOffsetX = disRight - disTop;
                } else {
                    shaderRadiusRightTop = disRight;
                    shaderRightTopOffsetY = disTop - disRight;
                }
                rightTopShader.value = new RadialGradient(innerRight + shaderRightTopOffsetX, innerTop + shaderRightTopOffsetY,
                        shaderRadiusRightTop, color2, null, Shader.TileMode.CLAMP);
            } else {
                // 画内圆角
                doubleRadius = innerRadiusRightTop * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rightTopPath.value.arcTo(innerRight - doubleRadius, innerTop, innerRight, innerTop + doubleRadius,
                            innerAngleRightTop.value, innerAngleTopRight.value - innerAngleRightTop.value, false);
                } else {
                    rightTopPath.value.arcTo(new RectF(innerRight - doubleRadius, innerTop, innerRight, innerTop + doubleRadius),
                            innerAngleRightTop.value, innerAngleTopRight.value - innerAngleRightTop.value, false);
                }
                // 设置右上角阴影
                rightTopShader.value = new RadialGradient(outerRight - outerRadiusRightTop,
                        outerTop + outerRadiusRightTop, outerRadiusRightTop, color3,
                        new float[]{0, innerRadiusRightTop / outerRadiusRightTop, 1}, Shader.TileMode.CLAMP);
            }
            rightTopPath.value.close();
        }// else // 没有外圆角 内圆角也将没有
        if (outerRadiusRightBottom != 0) {
            // 画外圆角
            rightBottomPath.value = new Path();
            doubleRadius = outerRadiusRightBottom * 2f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                rightBottomPath.value.arcTo(outerRight - doubleRadius, outerBottom - doubleRadius, outerRight, outerBottom,
                        outerAngleRightBottom.value, outerAngleBottomRight.value - outerAngleRightBottom.value, false);
            } else {
                rightBottomPath.value.arcTo(new RectF(outerRight - doubleRadius, outerBottom - doubleRadius, outerRight, outerBottom),
                        outerAngleRightBottom.value, outerAngleBottomRight.value - outerAngleRightBottom.value, false);
            }
            if (innerRadiusRightBottom == 0) {
                rightBottomPath.value.lineTo(innerRight, innerBottom);
                // 设置右下角阴影
                float shaderRadiusRightBottom;
                float shaderRightBottomOffsetX = 0, shaderRightBottomOffsetY = 0;
                if (disBottom > disRight) {
                    shaderRadiusRightBottom = disBottom;
                    shaderRightBottomOffsetX = disRight - disBottom;
                } else {
                    shaderRadiusRightBottom = disRight;
                    shaderRightBottomOffsetY = disBottom - disRight;
                }
                rightBottomShader.value = new RadialGradient(innerRight + shaderRightBottomOffsetX, innerBottom + shaderRightBottomOffsetY,
                        shaderRadiusRightBottom, color2, null, Shader.TileMode.CLAMP);
            } else {
                // 画内圆角
                doubleRadius = innerRadiusRightBottom * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rightBottomPath.value.arcTo(innerRight - doubleRadius, innerBottom - doubleRadius, innerRight, innerBottom,
                            innerAngleBottomRight.value, innerAngleRightBottom.value - innerAngleBottomRight.value, false);
                } else {
                    rightBottomPath.value.arcTo(new RectF(innerRight - doubleRadius, innerBottom - doubleRadius, innerRight, innerBottom),
                            innerAngleBottomRight.value, innerAngleRightBottom.value - innerAngleBottomRight.value, false);
                }
                // 设置右下角阴影
                rightBottomShader.value = new RadialGradient(outerRight - outerRadiusRightBottom,
                        outerBottom - outerRadiusRightBottom, outerRadiusRightBottom, color3,
                        new float[]{0, innerRadiusRightBottom / outerRadiusRightBottom, 1}, Shader.TileMode.CLAMP);
            }
            rightBottomPath.value.close();
        }// else // 没有外圆角 内圆角也将没有
        if (outerRadiusLeftBottom != 0) {
            // 画外圆角
            leftBottomPath.value = new Path();
            doubleRadius = outerRadiusLeftBottom * 2f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                leftBottomPath.value.arcTo(outerLeft, outerBottom - doubleRadius, outerLeft + doubleRadius, outerBottom,
                        outerAngleBottomLeft.value, outerAngleLeftBottom.value - outerAngleBottomLeft.value, false);
            } else {
                leftBottomPath.value.arcTo(new RectF(outerLeft, outerBottom - doubleRadius, outerLeft + doubleRadius, outerBottom),
                        outerAngleBottomLeft.value, outerAngleLeftBottom.value - outerAngleBottomLeft.value, false);
            }
            if (innerRadiusLeftBottom == 0) {
                leftBottomPath.value.lineTo(innerLeft, innerBottom);
                // 设置左下角阴影
                float shaderRadiusLeftBottom;
                float shaderLeftBottomOffsetX = 0, shaderLeftBottomOffsetY = 0;
                if (disBottom > disLeft) {
                    shaderRadiusLeftBottom = disBottom;
                    shaderLeftBottomOffsetX = disLeft - disBottom;
                } else {
                    shaderRadiusLeftBottom = disLeft;
                    shaderLeftBottomOffsetY = disBottom - disLeft;
                }
                leftBottomShader.value = new RadialGradient(innerLeft + shaderLeftBottomOffsetX, innerBottom + shaderLeftBottomOffsetY,
                        shaderRadiusLeftBottom, color2, null, Shader.TileMode.CLAMP);
            } else {
                // 画内圆角
                doubleRadius = innerRadiusLeftBottom * 2f;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    leftBottomPath.value.arcTo(innerLeft, innerBottom - doubleRadius, innerLeft + doubleRadius, innerBottom,
                            innerAngleLeftBottom.value, innerAngleBottomLeft.value - innerAngleLeftBottom.value, false);
                } else {
                    leftBottomPath.value.arcTo(new RectF(innerLeft, innerBottom - doubleRadius, innerLeft + doubleRadius, innerBottom),
                            innerAngleLeftBottom.value, innerAngleBottomLeft.value - innerAngleLeftBottom.value, false);
                }
                // 设置左下角阴影
                leftBottomShader.value = new RadialGradient(outerLeft + outerRadiusLeftBottom,
                        outerBottom - outerRadiusLeftBottom, outerRadiusLeftBottom, color3,
                        new float[]{0, innerRadiusLeftBottom / outerRadiusLeftBottom, 1}, Shader.TileMode.CLAMP);
            }
            leftBottomPath.value.close();
        }// else // 没有外圆角 内圆角也将没有
        float outerWidth = outerRight - outerLeft;
        float outerHeight = outerBottom - outerTop;
        if (outerHeight - outerSizeLeftTop.value - outerSizeLeftBottom.value > 0) {
            // 画左侧梯形
            leftPath.value = new Path();
            leftPath.value.moveTo(outerLeft, outerTop + outerSizeLeftTop.value);
            leftPath.value.lineTo(innerLeft, innerTop + innerSizeLeftTop.value);
            leftPath.value.lineTo(innerLeft, innerBottom - innerSizeLeftBottom.value);
            leftPath.value.lineTo(outerLeft, outerBottom - outerSizeLeftBottom.value);
            leftPath.value.close();
            // 设置左侧阴影
            leftShader.value = new LinearGradient(innerLeft, 0, outerLeft, 0, color2, null, Shader.TileMode.CLAMP);
        } // else // 左侧没有尺寸
        if (outerWidth - outerSizeTopLeft.value - outerSizeTopRight.value > 0) {
            // 画上侧梯形
            topPath.value = new Path();
            topPath.value.moveTo(outerLeft + outerSizeTopLeft.value, outerTop);
            topPath.value.lineTo(innerLeft + innerSizeTopLeft.value, innerTop);
            topPath.value.lineTo(innerRight - innerSizeTopRight.value, innerTop);
            topPath.value.lineTo(outerRight - outerSizeTopRight.value, outerTop);
            topPath.value.close();
            // 设置上侧阴影
            topShader.value = new LinearGradient(0, innerTop, 0, outerTop, color2, null, Shader.TileMode.CLAMP);
        } // else // 上侧没有尺寸
        if (outerHeight - outerSizeRightTop.value - outerSizeRightBottom.value > 0) {
            // 画右侧梯形
            rightPath.value = new Path();
            rightPath.value.moveTo(outerRight, outerTop + outerSizeRightTop.value);
            rightPath.value.lineTo(innerRight, innerTop + innerSizeRightTop.value);
            rightPath.value.lineTo(innerRight, innerBottom - innerSizeRightBottom.value);
            rightPath.value.lineTo(outerRight, outerBottom - outerSizeRightBottom.value);
            rightPath.value.close();
            // 设置右侧阴影
            rightShader.value = new LinearGradient(innerRight, 0, outerRight, 0, color2, null, Shader.TileMode.CLAMP);
        } // else // 右侧没有尺寸
        if (outerWidth - outerSizeBottomLeft.value - outerSizeBottomRight.value > 0) {
            // 画下侧梯形
            bottomPath.value = new Path();
            bottomPath.value.moveTo(outerLeft + outerSizeBottomLeft.value, outerBottom);
            bottomPath.value.lineTo(innerLeft + innerSizeBottomLeft.value, innerBottom);
            bottomPath.value.lineTo(innerRight - innerSizeBottomRight.value, innerBottom);
            bottomPath.value.lineTo(outerRight - outerSizeBottomRight.value, outerBottom);
            bottomPath.value.close();
            // 设置下侧阴影
            bottomShader.value = new LinearGradient(0, innerBottom, 0, outerBottom, color2, null, Shader.TileMode.CLAMP);
        } // else // 下侧没有尺寸
    }

    public interface OnMeasureListener {
        void onMeasure(int widthMeasureSpec, int heightMeasureSpec);
    }

    public interface OnDrawSuperListener {
        void onDrawSuper(Canvas canvas);
    }

    public static class PwPhValue {
        private int value;
        private boolean percent;
        private boolean isWidth;

        public PwPhValue() {

        }

        public void setValue(int value) {
            this.value = value;
        }

        public void setPercent(boolean percent) {
            this.percent = percent;
        }

        public int getValue() {
            return value;
        }

        public boolean isPercent() {
            return percent;
        }

        public boolean isWidth() {
            return isWidth;
        }

        public void setWidth(boolean width) {
            isWidth = width;
        }
    }

    private static class Size {
        private final int width;
        private final int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    private static class IntegerBox {
        public int value;

        public IntegerBox() {
            value = 0;
        }

        public IntegerBox(int value) {
            this.value = value;
        }
    }

    private static class FloatBox {
        public float value;

        public FloatBox() {
            value = 0;
        }

        public FloatBox(float value) {
            this.value = value;
        }
    }

    private static class ShaderBox {
        public Shader value;

        public ShaderBox() {
            value = null;
        }

        public ShaderBox(Shader value) {
            this.value = value;
        }
    }

    private static class PathBox {
        public Path value;

        public PathBox() {
            value = null;
        }

        public PathBox(Path value) {
            this.value = value;
        }
    }
}
