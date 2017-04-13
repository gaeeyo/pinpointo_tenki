package nikeno.Tenki.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MyJobService extends JobService {

    static final String TAG = "MyJobService";

    Thread mThread;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.d(TAG, "onStartJob");
        mThread = new Thread() {
            @Override
            public void run() {
                super.run();
                new WidgetUpdateService.UpdateTask().doInBackground(MyJobService.this);
                jobFinished(jobParameters, false);
                synchronized (this) {
                    mThread = null;
                }
            }
        };
        mThread.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        synchronized (this) {
            if (mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
        }
        return true;
    }
}
