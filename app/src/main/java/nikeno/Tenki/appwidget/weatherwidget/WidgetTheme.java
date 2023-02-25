package nikeno.Tenki.appwidget.weatherwidget;

public class WidgetTheme {

    public static final int THEME_DARK  = 0;
    public static final int THEME_LIGHT = 1;

    public final int theme;
//    public final int backgroundColor;
    public final int timeColor;
    public final int tempColor;
    public final int rainColor;
    public final int humidColor;
    public final int textColor;

//    public WidgetTheme(@NonNull Context context) {
//        Prefs prefs        = TenkiApp.from(context).getPrefs();
//        int   transparency = prefs.get(Prefs.WW_BG_TRANSPARENCY);
//        int theme        = prefs.get(Prefs.WW_THEME);
//        int   alpha        = ((100 - transparency) * 255) / 100;
//
//        alpha = 255;
//        int   color;
//          this(theme);
////        backgroundColor = alpha << 24 | (color & 0xffffff);
//    }

    public WidgetTheme(int theme) {
        this.theme = theme;
        switch (theme) {
            case THEME_LIGHT:
//                color = 0xffffff;
                timeColor = 0xff000000;
                tempColor = 0xffFFAC59;
                rainColor = 0xff8EC7FF;
                humidColor = 0xff8EFF8E;
                textColor = 0xffe0e0e0;
                break;
            case THEME_DARK:
            default:
//                color = 0;
                timeColor = 0xffd0d0d0;
                tempColor = 0xffFFAC59;
                rainColor = 0xff8EC7FF;
                humidColor = 0xff8EFF8E;
                textColor = 0xffe0e0e0;
                break;
        }
    }
}
