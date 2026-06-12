-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'b'

-keep public class paspo.id.ssoprovider.ui.PaspoSignInButton {
    public <init>(...);
    public *** setAuthHandler(...);
    public *** get*();
    public *** set*(...);
}

-keep public enum paspo.id.ssoprovider.ui.PaspoButtonTheme { *; }
