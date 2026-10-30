# kotlinx.serialization DTOs
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers @kotlinx.serialization.Serializable class com.potentplum.dextracker.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.potentplum.dextracker.**$$serializer { *; }

# Retrofit service interfaces use generic signatures at runtime
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
