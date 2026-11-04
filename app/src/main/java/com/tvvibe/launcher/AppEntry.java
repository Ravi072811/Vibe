package com.tvvibe.launcher;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

/** One installed, launchable app. */
final class AppEntry {
    final String pkg;
    final ComponentName component;
    final String label;
    final Drawable banner; // 16:9 TV banner, may be null
    final Drawable icon;

    AppEntry(String pkg, ComponentName component, String label, Drawable banner, Drawable icon) {
        this.pkg = pkg;
        this.component = component;
        this.label = label;
        this.banner = banner;
        this.icon = icon;
    }
}
