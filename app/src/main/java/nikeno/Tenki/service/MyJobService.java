package nikeno.Tenki.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MyJobService extends JobService {

    static final String TAG = "MyJobService";

    WidgetUpdateService.UpdateTask mTask;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.d(TAG, "onStartJob");
        mTask = new WidgetUpdateService.UpdateTask() {
            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                if (mTask == this) {
                    jobFinished(jobParameters, false);
                    mTask = null;
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                if (mTask == this) {
                    mTask = null;
                }
            }
        };
        mTask.execute(this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mTask != null) {
            mTask.cancel(true);
        }
        return true;
    }
}
