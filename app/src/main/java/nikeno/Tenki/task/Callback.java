package nikeno.Tenki.task;

public abstract class Callback<T> {
    public abstract void onSuccess(T result);

    public void onError(Throwable error) {
    }

    public void onFinish() {
    }
}
