package com.tvvibe.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finds every app the user can open: TV apps first, then sideloaded phone apps. */
final class AppRepository {

    private AppRepository() {}

    static List<AppEntry> load(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Map<String, AppEntry> byPkg = new LinkedHashMap<>();
        collect(pm, byPkg, Intent.CATEGORY_LEANBACK_LAUNCHER, ctx.getPackageName());
        collect(pm, byPkg, Intent.CATEGORY_LAUNCHER, ctx.getPackageName());

        List<AppEntry> list = new ArrayList<>(byPkg.values());
        Collections.sort(list, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return list;
    }

    private static void collect(PackageManager pm, Map<String, AppEntry> out, String category, String self) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ResolveInfo> found;
        try {
            found = pm.queryIntentActivities(query, 0);
        } catch (RuntimeException e) {
            return;
        }
        for (ResolveInfo ri : found) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null) continue;
            String pkg = ai.packageName;
            if (pkg.equals(self) || out.containsKey(pkg)) continue;

            Drawable banner = null;
            try {
                banner = ai.loadBanner(pm);
                if (banner == null && ai.applicationInfo != null) {
                    banner = ai.applicationInfo.loadBanner(pm);
                }
            } catch (RuntimeException ignored) {
            }
            Drawable icon = ri.loadIcon(pm);
            CharSequence label = ri.loadLabel(pm);
            out.put(pkg, new AppEntry(pkg, new ComponentName(pkg, ai.name),
                    label == null ? pkg : label.toString(), banner, icon));
        }
    }
}
