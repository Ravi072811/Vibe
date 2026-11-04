package com.tvvibe.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS doesn't allow choosing another home app. This service notices when the
 * Amazon home screen comes to the front (for example after pressing Home) and
 * brings Vibe forward instead. It only looks at which app is showing.
 */
public class HomeWatcherService extends AccessibilityService {

    static final String AMAZON_HOME = "com.amazon.tv.launcher";

    /** Set when the user deliberately opens the Amazon home from Vibe. */
    static volatile boolean bypass = false;

    private long lastLaunch = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;

        if (!AMAZON_HOME.contentEquals(pkg)) {
            // User moved into some other app: the next Home press should return to Vibe.
            if (!getPackageName().contentEquals(pkg) && !"com.android.systemui".contentEquals(pkg)) {
                bypass = false;
            }
            return;
        }
        if (bypass) return;

        long now = SystemClock.uptimeMillis();
        if (now - lastLaunch < 800) return;
        lastLaunch = now;

        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(i);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onInterrupt() {
    }
}
