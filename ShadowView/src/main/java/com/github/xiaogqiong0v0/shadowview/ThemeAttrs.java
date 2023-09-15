package com.github.xiaogqiong0v0.shadowview;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

/**
 * 包名：com.github.xiaogqiong0v0.shadowview
 * 创建者：xiaoqiong0v0
 * 邮箱：king-afu@hotmail.com
 * 时间：2023/9/14 - 10:06
 * 说明：
 */
public class ThemeAttrs {
    private final Resources.Theme theme;
    private final DisplayMetrics metrics;

    public ThemeAttrs(Resources.Theme theme) {
        this.theme = theme;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            metrics = theme.getResources().getDisplayMetrics();
        } else {
            metrics = Resources.getSystem().getDisplayMetrics();
        }
    }

    public int getDimensionPixelSize(@AttrRes int attr) {
        return getDimensionPixelSize(attr, 0);
    }

    public int getDimensionPixelSize(@AttrRes int attr, int defValue) {
        final TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        if (typedValue.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.complexToDimensionPixelSize(typedValue.data, metrics);
        }
        return defValue;
    }

    public int getColor(@AttrRes int attr) {
        return getColor(attr, 0);
    }

    public int getColor(@AttrRes int attr, @ColorInt int defValue) {
        final TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        final int type = typedValue.type;
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) {
            return typedValue.data;
        } else if (type == TypedValue.TYPE_STRING) {
            return ColorStateList.valueOf(typedValue.data).getDefaultColor();
        }
        return defValue;
    }

    public boolean getBoolean(@AttrRes int attr) {
        return getBoolean(attr, false);
    }

    public boolean getBoolean(@AttrRes int attr, boolean defValue) {
        final TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        final int type = typedValue.type;
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) {
            return typedValue.data != 0;
        } else if (type == TypedValue.TYPE_STRING) {
            return Boolean.parseBoolean(typedValue.string.toString());
        }
        return defValue;
    }

    public int getInt(@AttrRes int attr) {
        return getInt(attr, 0);
    }

    public int getInt(@AttrRes int attr, int defValue) {
        final TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        final int type = typedValue.type;
        if (type == TypedValue.TYPE_NULL) {
            return defValue;
        } else if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) {
            return typedValue.data;
        } else if (type == TypedValue.TYPE_STRING) {
            return Integer.parseInt(typedValue.string.toString());
        }
        return defValue;
    }
}
