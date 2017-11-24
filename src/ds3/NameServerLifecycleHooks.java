package ds3;

public interface NameServerLifecycleHooks extends LifecycleHooks {
    void onReady(Runnable runnable);
}
