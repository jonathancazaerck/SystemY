package ds3;

public interface NodeLifecycleHooks extends LifecycleHooks {
    void onReady(Runnable runnable);
    void onBound(Runnable runnable);
    void onFilesReplicated(Runnable runnable);
    void onNeighboursChanged(Runnable runnable);
    void onFileListChanged(Runnable runnable);
}
