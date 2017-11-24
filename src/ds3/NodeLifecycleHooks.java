package ds3;

public interface NodeLifecycleHooks extends LifecycleHooks {
    void onReady(Runnable runnable);
    void onFilesReplicated(Runnable runnable);
    void onNeighbourChanged(Runnable runnable);
}
