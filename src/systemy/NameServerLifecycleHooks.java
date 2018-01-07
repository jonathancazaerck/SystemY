package systemy;

public interface NameServerLifecycleHooks extends LifecycleHooks {
    void onReady(Runnable runnable);
}
