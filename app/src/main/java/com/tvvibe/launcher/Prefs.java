package com.tvvibe.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Remembers the Top Row order and which apps are hidden. */
final class Prefs {
    private static final String KEY_FAV = "favorites";
    private static final String KEY_HIDDEN = "hidden";
    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getSharedPreferences("vibe", Context.MODE_PRIVATE);
    }

    boolean hasFavorites() {
        return sp.contains(KEY_FAV);
    }

    List<String> favorites() {
        String s = sp.getString(KEY_FAV, "");
        if (s == null || s.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(s.split(",")));
    }

    void setFavorites(List<String> pkgs) {
        sp.edit().putString(KEY_FAV, TextUtils.join(",", pkgs)).apply();
    }

    Set<String> hidden() {
        return new HashSet<>(sp.getStringSet(KEY_HIDDEN, Collections.<String>emptySet()));
    }

    void setHidden(Set<String> pkgs) {
        sp.edit().putStringSet(KEY_HIDDEN, new HashSet<>(pkgs)).apply();
    }
}
