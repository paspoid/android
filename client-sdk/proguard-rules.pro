-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'b'

-keep public class paspo.id.ssoprovider.client.PaspoID {
    public <init>(...);
    public *** authenticate(...);
    public *** checkInstallation(...);
    public void cleanup();
}
