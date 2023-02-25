package jp.syoboi.android.app.tiny;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class MinimumActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static class LifecycleOwner implements jp.syoboi.android.app.tiny.LifecycleOwner {

    }
}
