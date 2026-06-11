# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**

-keep public class paspo.id.ssoprovider.shared.models.** { *; }
-keepclassmembers class paspo.id.ssoprovider.shared.models.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class paspo.id.ssoprovider.shared.models.**$$serializer { *; }


-keep public class paspo.id.ssoprovider.shared.models.internal.** { *; }

# Constants
-keep public class paspo.id.ssoprovider.shared.models.PaspoSsoConstants {
    public static final paspo.id.ssoprovider.shared.models.PaspoSsoConstants INSTANCE;
    public static final <fields>;
}

-keep public class paspo.id.ssoprovider.shared.models.PaspoSsoProviderException { *; }
-keep public class paspo.id.ssoprovider.shared.models.SsoCancelledException { *; }
-keep public class paspo.id.ssoprovider.shared.models.PaspoNotInstalledException { *; }
