package nikeno.Tenki.task;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

public abstract class AbstractTask<T> extends AsyncTask<Object, Object, Object> {

    @NonNull
    private final Callback<T> mCallback;

    protected AbstractTask(@NonNull Callback<T> callback) {
        mCallback = callback;
    }

    @Override
    protected Object doInBackground(Object... objects) {
        try {
            return doInBackground();
        } catch (Exception e) {
            return e;
        }
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if (o instanceof Throwable) {
            mCallback.onError((Throwable) o);
        } else {
            mCallback.onSuccess((T) o);
        }
        mCallback.onFinish();
    }

    abstract T doInBackground() throws Exception;

    public void execute() {
        executeOnExecutor(THREAD_POOL_EXECUTOR);
    }
}
