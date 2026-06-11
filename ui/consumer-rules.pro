# Public API of the View UI module
-keep public class paspo.id.ssoprovider.ui.PaspoSignInButton {
    public <init>(...);
    public *** setAuthHandler(...);
    public *** getButton*();
    public *** setButton*(...);
    public *** getIconOnly();
    public *** setIconOnly(...);
}

-keep public enum paspo.id.ssoprovider.ui.PaspoButtonTheme { *; }
-keep public enum paspo.id.ssoprovider.ui.PaspoButtonShape { *; }
