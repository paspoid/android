-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'b'

-keep public class paspo.id.ssoprovider.ui.compose.PaspoSignInButtonKt {
    public *** PaspoSignInButton(...);
    public *** PaspoSignInButtonContent(...);
}

-keep public class paspo.id.ssoprovider.ui.compose.PaspoButtonColors { *; }
-keep public class paspo.id.ssoprovider.ui.compose.PaspoSignInButtonDefaults { *; }
