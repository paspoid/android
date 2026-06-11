-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Public API
-keep public class paspo.id.ssoprovider.client.PaspoID {
    public <init>(...);
    public *** authenticate(...);
    public *** checkInstallation(...);
    public void cleanup();
}
