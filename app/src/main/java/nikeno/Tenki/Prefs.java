package nikeno.Tenki;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class Prefs {
    static final String RECENT_PREFIX = "Recent";
    static final String URL           = "url";
    static final String THEME    = "theme";
    private static final int RECENT_MAX = 5;
    final SharedPreferences mPrefs;

    public enum ThemeNames {
        DEFAULT("default"), DARK("dark");

        String value;
        ThemeNames(String value) {
            this.value = value;
        }
    }

    public Prefs(SharedPreferences prefs) {
        mPrefs = prefs;
    }

    public ThemeNames getTheme() {
        String value = mPrefs.getString(THEME, ThemeNames.DEFAULT.value);
        if (ThemeNames.DARK.value.equals(value)) {
            return ThemeNames.DARK;
        } else {
            return ThemeNames.DEFAULT;
        }
    }

    public void setTheme(ThemeNames theme) {
        mPrefs.edit().putString(THEME, theme.value).apply();
    }

    public List<Area> getRecentAreaList() {
        List<Area> list = new ArrayList<>();

        String data;
        for (int j = 0; j < RECENT_MAX; j++) {
            data = mPrefs.getString(RECENT_PREFIX + j, null);
            if (data != null) {
                Area areaData = Area.deserialize(data);
                if (areaData != null) {
                    list.add(areaData);
                }
            }
        }
        return list;
    }

    public void putRecentAreaList(List<Area> list) {
        SharedPreferences.Editor editor = mPrefs.edit();

        for (int j = 0; j < RECENT_MAX; j++) {
            if (j < list.size()) {
                editor.putString(RECENT_PREFIX + j, list.get(j).serialize());
            } else {
                editor.remove(RECENT_PREFIX + j);
            }
        }
        editor.commit();
    }

    public List<Area> addRecentArea(Area area) {
        List<Area> list = getRecentAreaList();
        list.remove(area);
        list.add(0, area);
        putRecentAreaList(list);
        return list;
    }

    public List<Area> removeRecentArea(Area area) {
        List<Area> list = getRecentAreaList();
        list.remove(area);
        putRecentAreaList(list);
        return list;
    }


    public String getCurrentAreaUrl() {
        return mPrefs.getString(URL, "http://weather.yahoo.co.jp/weather/jp/13/4410/13101.html");
    }

    public void setCurrentAreaUrl(String url) {
        mPrefs.edit().putString(URL, url).commit();
    }
}
