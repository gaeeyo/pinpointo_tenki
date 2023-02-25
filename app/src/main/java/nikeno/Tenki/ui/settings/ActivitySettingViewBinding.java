package nikeno.Tenki.ui.settings;

import android.app.Activity;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import nikeno.Tenki.R;

class ActivitySettingViewBinding {
    public final SeekBar    backgroundTransparency;
    public final RadioGroup  theme;
    public final FrameLayout widgetPreview;

    public static ActivitySettingViewBinding setContentView(@NonNull Activity activity) {
        return new ActivitySettingViewBinding(activity);
    }

    public ActivitySettingViewBinding(Activity activity) {
        activity.setContentView(R.layout.activity_settings);
        backgroundTransparency = activity.findViewById(R.id.backgroundTransparency);
        theme = activity.findViewById(R.id.theme);
        widgetPreview = activity.findViewById(R.id.widgetPreview);
    }
}
