package nikeno.Tenki.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;

import nikeno.Tenki.Prefs;
import nikeno.Tenki.R;
import nikeno.Tenki.TenkiApp;

public class DisplaySettingsDialog extends AlertDialog {

    Prefs mPrefs;

    public DisplaySettingsDialog(@NonNull Activity activity) {
        super(activity);
        View v = getLayoutInflater().inflate(R.layout.display_settings, null);
        setView(v);
        setOwnerActivity(activity);
        setTitle(R.string.display_settings);
        setButton(Dialog.BUTTON_POSITIVE, getContext().getString(R.string.close), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        int[] ids = {R.id.show_weather_icon, R.id.show_weather_icon_label,
                R.id.show_temperature, R.id.show_humidity,
                R.id.show_precipitation, R.id.show_wind
        };
        Prefs.BoolValue[] keys = {
                Prefs.SHOW_WEATHER_ICON, Prefs.SHOW_WEATHER_ICON_LABEL,
                Prefs.SHOW_TEMPERATURE, Prefs.SHOW_HUMIDITY,
                Prefs.SHOW_PRECIPITATION, Prefs.SHOW_WIND
        };

        mPrefs = TenkiApp.from(getContext()).getPrefs();

        for (int j = 0; j < ids.length; j++) {
            CheckBox cb = v.findViewById(ids[j]);
            cb.setChecked(mPrefs.get(keys[j]));
            cb.setTag(keys[j]);
            cb.setOnCheckedChangeListener(onCheckedChangeListener);
        }
    }

    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            Prefs.BoolValue key = (Prefs.BoolValue) compoundButton.getTag();
            mPrefs.set(key, b);
            if (getOwnerActivity() instanceof Listener) {
                ((Listener) getOwnerActivity()).onDisplaySettingChanged();
            }
        }
    };

    public interface Listener {
        void onDisplaySettingChanged();
    }

}
