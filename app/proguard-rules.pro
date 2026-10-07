# Proguard rules for TreP Password Manager
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @org.jetbrains.annotations.* <fields>;
    @org.jetbrains.annotations.* <methods>;
}
-keepclassmembers class kotlinx.serialization.** {
    *;
}
