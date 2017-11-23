package ds3;

public interface LifecycleHooks {
    void onReady(Runnable runnable);
    void onShutdown(Runnable runnable);
}
