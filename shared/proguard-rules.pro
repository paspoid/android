-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'a'

-keep public class paspo.id.ssoprovider.shared.crypto.ECDHKeyManager {
    public static final paspo.id.ssoprovider.shared.crypto.ECDHKeyManager INSTANCE;
    public *** generateEphemeralKeyPair(...);
    public *** computeSharedSecret(...);
    public *** deleteKey(...);
    public *** clearStore();
}

-keep public class paspo.id.ssoprovider.shared.crypto.AesGcmCipher {
   public static final paspo.id.ssoprovider.shared.crypto.AesGcmCipher INSTANCE;
    public *** encrypt(...);
    public *** decrypt(...);
}

-keep public class paspo.id.ssoprovider.shared.crypto.KeyDerivation {
    public static final paspo.id.ssoprovider.shared.crypto.KeyDerivation INSTANCE;
    public *** deriveAesKey(...);
}

-keep public class paspo.id.ssoprovider.shared.models.internal.** { *; }

-keep public class paspo.id.ssoprovider.shared.models.** { *; }
-keepclassmembers class paspo.id.ssoprovider.shared.models.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class paspo.id.ssoprovider.shared.models.**$$serializer { *; }

-keep public class paspo.id.ssoprovider.shared.models.PaspoConstants {
    public static final <fields>;
}

-keep public class paspo.id.ssoprovider.shared.models.PaspoSsoProviderException {
    public <init>(...);
    public *** getCode();
    public *** getMessage();
}
-keep public class paspo.id.ssoprovider.shared.models.SsoCancelledException { *; }
-keep public class paspo.id.ssoprovider.shared.models.PaspoNotInstalledException { *; }
