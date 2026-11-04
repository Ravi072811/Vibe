package com.tvvibe.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The home screen: a big "top shelf", a Top Row of favourites, and every app below. */
public class MainActivity extends Activity {

    private static final int COLUMNS = 5;
    private static final String KEY_SETTINGS = "special:settings";
    private static final String KEY_HIDDEN = "special:hidden";
    private static final String KEY_AMAZON = "special:amazon";

    /** Apps placed in the Top Row on first launch, if installed. */
    private static final String[] DEFAULT_FAVORITES = {
            "com.netflix.ninja",
            "com.amazon.firetv.youtube",
            "com.google.android.youtube.tv",
            "com.amazon.amazonvideo.livingroom",
            "in.startv.hotstar",
            "com.spotify.tv.android",
            "com.plexapp.android",
            "org.xbmc.kodi",
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;

    private ImageView backdrop;
    private Drawable currentBackdrop;
    private TextView clock, date, heroTitle, heroHint, notice, appsHeader;
    private LinearLayout topRow, grid;
    private ScrollView scroller;

    private int tileW, tileH, gap, pad;
    private float density;

    private boolean dirty = true;
    private boolean resumed = false;
    private String lastFocusKey;

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 15_000);
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dirty = true;
            if (resumed) rebuild();
        }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs = new Prefs(this);
        measure();
        setContentView(buildLayout());

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        registerReceiver(packageReceiver, f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (dirty) rebuild();
        updateNotice();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        handler.removeCallbacks(clockTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(packageReceiver);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home: return to the start of the Top Row.
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && topRow.getChildCount() > 0) {
            scroller.smoothScrollTo(0, 0);
            topRow.getChildAt(0).requestFocus();
        }
    }

    @Override
    public void onBackPressed() {
        // This is the home screen: Back has nowhere to go. Jump to the first tile instead.
        if (topRow.getChildCount() > 0) {
            scroller.smoothScrollTo(0, 0);
            topRow.getChildAt(0).requestFocus();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            View v = getCurrentFocus();
            if (v != null && v.getTag() instanceof AppEntry) {
                showAppMenu((AppEntry) v.getTag());
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---------------------------------------------------------------- layout

    private void measure() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        density = dm.density;
        int width = Math.max(dm.widthPixels, dm.heightPixels);
        pad = dp(56);
        gap = dp(26);
        tileW = (width - 2 * pad - (COLUMNS - 1) * gap) / COLUMNS;
        tileH = tileW * 9 / 16;
    }

    private View buildLayout() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int height = Math.min(dm.widthPixels, dm.heightPixels);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        backdrop = new ImageView(this);
        backdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        backdrop.setAlpha(0.85f);
        root.addView(backdrop, match());

        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x55000000, 0xB0000000, 0xF0000000}));
        root.addView(scrim, match());

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        root.addView(column, match());

        // Top shelf: large title of whatever is focused.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.BOTTOM);
        hero.setPadding(pad, 0, pad, dp(8));
        heroTitle = text(46, Color.WHITE, "sans-serif-medium");
        heroTitle.setSingleLine(true);
        heroHint = text(15, 0xB3FFFFFF, "sans-serif");
        heroHint.setText("Press  ☰  on the remote for options");
        notice = text(14, 0xFFFFD27A, "sans-serif");
        notice.setVisibility(View.GONE);
        hero.addView(heroTitle);
        hero.addView(heroHint);
        hero.addView(notice);
        column.addView(hero, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (height * 0.36f)));

        scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setClipChildren(false);
        scroller.setClipToPadding(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setSmoothScrollingEnabled(true);
        column.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setClipChildren(false);
        body.setClipToPadding(false);
        body.setPadding(0, 0, 0, dp(64));
        scroller.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        HorizontalScrollView rowScroll = new HorizontalScrollView(this);
        rowScroll.setHorizontalScrollBarEnabled(false);
        rowScroll.setClipChildren(false);
        rowScroll.setClipToPadding(false);
        rowScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rowScroll.setPadding(pad, dp(18), pad, 0);
        topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setClipChildren(false);
        rowScroll.addView(topRow);
        body.addView(rowScroll);

        appsHeader = text(17, 0x99FFFFFF, "sans-serif-medium");
        appsHeader.setText("Apps");
        appsHeader.setPadding(pad, dp(10), pad, dp(18));
        body.addView(appsHeader);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        body.addView(grid);

        // Clock, top right.
        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.END);
        clock = text(30, Color.WHITE, "sans-serif-light");
        date = text(14, 0xB3FFFFFF, "sans-serif");
        clockBox.addView(clock);
        clockBox.addView(date);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        cl.topMargin = dp(32);
        cl.rightMargin = pad;
        root.addView(clockBox, cl);

        return root;
    }

    // ---------------------------------------------------------------- content

    private void rebuild() {
        dirty = false;
        List<AppEntry> apps = AppRepository.load(this);
        Map<String, AppEntry> byPkg = new HashMap<>();
        for (AppEntry e : apps) byPkg.put(e.pkg, e);

        Set<String> hidden = prefs.hidden();
        List<String> favs = prefs.favorites();
        if (!prefs.hasFavorites()) {
            favs = defaultFavorites(apps, byPkg);
            prefs.setFavorites(favs);
        }

        topRow.removeAllViews();
        for (String pkg : favs) {
            AppEntry e = byPkg.get(pkg);
            if (e == null || hidden.contains(pkg)) continue;
            addToRow(topRow, appTile(e));
        }
        TileView settings = specialTile("Settings", getDrawable(android.R.drawable.ic_menu_preferences),
                Color.rgb(90, 96, 110), KEY_SETTINGS);
        addToRow(topRow, settings);

        List<View> tiles = new ArrayList<>();
        for (AppEntry e : apps) {
            if (hidden.contains(e.pkg) || favs.contains(e.pkg)) continue;
            tiles.add(appTile(e));
        }
        if (!hidden.isEmpty()) {
            tiles.add(specialTile("Hidden Apps (" + hidden.size() + ")",
                    getDrawable(android.R.drawable.ic_menu_view), Color.rgb(70, 70, 80), KEY_HIDDEN));
        }
        tiles.add(specialTile("Amazon Home", getDrawable(android.R.drawable.ic_menu_revert),
                Color.rgb(40, 110, 140), KEY_AMAZON));

        grid.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                row.setPadding(pad, 0, pad, dp(8));
                grid.addView(row);
            }
            addToRow(row, tiles.get(i));
        }

        restoreFocus();
    }

    private List<String> defaultFavorites(List<AppEntry> apps, Map<String, AppEntry> byPkg) {
        List<String> out = new ArrayList<>();
        for (String p : DEFAULT_FAVORITES) {
            if (byPkg.containsKey(p) && out.size() < 6) out.add(p);
        }
        for (AppEntry e : apps) {
            if (out.size() >= 4) break;
            if (!out.contains(e.pkg) && !e.pkg.startsWith("com.amazon.")) out.add(e.pkg);
        }
        return out;
    }

    private void addToRow(LinearLayout row, View tile) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (row.getChildCount() > 0) lp.leftMargin = gap;
        row.addView(tile, lp);
    }

    private TileView appTile(final AppEntry e) {
        final TileView t = new TileView(this, tileW, tileH);
        t.bind(e);
        t.setTag(e);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch(e);
            }
        });
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showAppMenu(e);
                return true;
            }
        });
        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) onTileFocused(t, e.pkg, e.label, e.banner, e.icon);
            }
        });
        return t;
    }

    private TileView specialTile(final String title, final Drawable icon, int color, final String key) {
        final TileView t = new TileView(this, tileW, tileH);
        t.bindSpecial(title, icon, color);
        t.setTag(key);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSpecialClicked(key);
            }
        });
        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) onTileFocused(t, key, title, null, icon);
            }
        });
        return t;
    }

    private void restoreFocus() {
        View target = null;
        if (lastFocusKey != null) target = findTile(lastFocusKey);
        if (target == null && topRow.getChildCount() > 0) target = topRow.getChildAt(0);
        if (target != null) {
            final View t = target;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    t.requestFocus();
                }
            });
        }
    }

    private View findTile(String key) {
        View v = findIn(topRow, key);
        if (v != null) return v;
        for (int i = 0; i < grid.getChildCount(); i++) {
            v = findIn((ViewGroup) grid.getChildAt(i), key);
            if (v != null) return v;
        }
        return null;
    }

    private static View findIn(ViewGroup row, String key) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View c = row.getChildAt(i);
            Object tag = c.getTag();
            String k = tag instanceof AppEntry ? ((AppEntry) tag).pkg : String.valueOf(tag);
            if (key.equals(k)) return c;
        }
        return null;
    }

    // ---------------------------------------------------------------- focus & backdrop

    private Runnable pendingBackdrop;

    private void onTileFocused(final TileView tile, String key, String title, final Drawable banner,
                               final Drawable icon) {
        lastFocusKey = key;
        if (!title.contentEquals(heroTitle.getText())) {
            heroTitle.setText(title);
            heroTitle.setAlpha(0f);
            heroTitle.animate().alpha(1f).setDuration(220).start();
        }
        if (pendingBackdrop != null) handler.removeCallbacks(pendingBackdrop);
        pendingBackdrop = new Runnable() {
            @Override
            public void run() {
                setBackdrop(banner, icon, tile.accentColor);
            }
        };
        handler.postDelayed(pendingBackdrop, 120);
    }

    private void setBackdrop(Drawable banner, Drawable icon, int accent) {
        Bitmap src;
        try {
            src = banner != null
                    ? Graphics.render(banner, 160, 90, Color.BLACK)
                    : Graphics.renderIconOnColor(icon, 160, 90, Graphics.tone(accent, 0.4f, 0.45f));
        } catch (RuntimeException e) {
            return;
        }
        Bitmap soft = Graphics.blur(src);
        src.recycle();
        BitmapDrawable next = new BitmapDrawable(getResources(), soft);
        next.setFilterBitmap(true);
        Drawable prev = currentBackdrop != null ? currentBackdrop : new ColorDrawable(Color.BLACK);
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{prev, next});
        td.setCrossFadeEnabled(true);
        backdrop.setImageDrawable(td);
        td.startTransition(450);
        currentBackdrop = next;
    }

    // ---------------------------------------------------------------- actions

    private void launch(AppEntry e) {
        Intent i = null;
        try {
            i = getPackageManager().getLeanbackLaunchIntentForPackage(e.pkg);
            if (i == null) i = getPackageManager().getLaunchIntentForPackage(e.pkg);
        } catch (RuntimeException ignored) {
        }
        if (i == null) {
            i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(e.component);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
        } catch (RuntimeException ex) {
            Toast.makeText(this, "Couldn't open " + e.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void onSpecialClicked(String key) {
        if (KEY_SETTINGS.equals(key)) {
            if (!tryStart(new Intent(Settings.ACTION_SETTINGS))) {
                tryStart(getPackageManager().getLaunchIntentForPackage("com.amazon.tv.settings.v2"));
            }
        } else if (KEY_HIDDEN.equals(key)) {
            showHiddenApps();
        } else if (KEY_AMAZON.equals(key)) {
            HomeWatcherService.bypass = true;
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.setPackage(HomeWatcherService.AMAZON_HOME);
            if (!tryStart(i)) {
                HomeWatcherService.bypass = false;
                Toast.makeText(this, "Amazon home isn't available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean tryStart(Intent i) {
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void showAppMenu(final AppEntry e) {
        final List<String> favs = prefs.favorites();
        final boolean isFav = favs.contains(e.pkg);
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        if (isFav) {
            labels.add("Remove from Top Row");
            actions.add(new Runnable() { public void run() {
                favs.remove(e.pkg); prefs.setFavorites(favs); rebuild(); } });
            labels.add("Move Left");
            actions.add(new Runnable() { public void run() { move(favs, e.pkg, -1); } });
            labels.add("Move Right");
            actions.add(new Runnable() { public void run() { move(favs, e.pkg, 1); } });
        } else {
            labels.add("Add to Top Row");
            actions.add(new Runnable() { public void run() {
                favs.add(e.pkg); prefs.setFavorites(favs); rebuild(); } });
        }
        labels.add("Hide");
        actions.add(new Runnable() { public void run() {
            Set<String> h = prefs.hidden(); h.add(e.pkg); prefs.setHidden(h);
            lastFocusKey = null; rebuild(); } });
        labels.add("App Info");
        actions.add(new Runnable() { public void run() {
            tryStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", e.pkg, null))); } });
        labels.add("Uninstall");
        actions.add(new Runnable() { public void run() {
            tryStart(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + e.pkg))); } });

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(e.label)
                .setItems(labels.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        actions.get(which).run();
                    }
                })
                .show();
    }

    private void move(List<String> favs, String pkg, int delta) {
        int i = favs.indexOf(pkg);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= favs.size()) return;
        favs.set(i, favs.get(j));
        favs.set(j, pkg);
        prefs.setFavorites(favs);
        lastFocusKey = pkg;
        rebuild();
    }

    private void showHiddenApps() {
        final List<String> pkgs = new ArrayList<>(prefs.hidden());
        final List<String> names = new ArrayList<>();
        for (String p : pkgs) {
            String name = p;
            try {
                name = getPackageManager().getApplicationLabel(
                        getPackageManager().getApplicationInfo(p, 0)).toString();
            } catch (Exception ignored) {
            }
            names.add(name);
        }
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Select an app to show again")
                .setItems(names.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        Set<String> h = prefs.hidden();
                        h.remove(pkgs.get(which));
                        prefs.setHidden(h);
                        lastFocusKey = pkgs.get(which);
                        rebuild();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // ---------------------------------------------------------------- small helpers

    private void updateClock() {
        Date now = new Date();
        clock.setText(android.text.format.DateFormat.getTimeFormat(this).format(now));
        date.setText(android.text.format.DateFormat.format("EEEE, d MMMM", now));
    }

    private void updateNotice() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String me = new ComponentName(this, HomeWatcherService.class).flattenToString();
        boolean on = enabled != null && enabled.contains(me);
        notice.setVisibility(on ? View.GONE : View.VISIBLE);
        notice.setText("Home button still opens Amazon's screen — run the setup script to fix this.");
    }

    private TextView text(float sp, int color, String family) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(family, Typeface.NORMAL));
        t.setShadowLayer(dp(6), 0, dp(1), 0x66000000);
        return t;
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }
}
