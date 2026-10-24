# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve source file and line numbers for Play Console crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Shizuku IPC & UserService classes instantiated across process boundaries
-keep class com.grinch.rivo4.IShellService** { *; }
-keep class com.grinch.rivo4.controller.shizuku.ShellService { *; }

# Keep Kotlinx Serialization models & generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.grinch.rivo4.modal.data.**$$serializer { *; }
-keepclassmembers class com.grinch.rivo4.modal.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.grinch.rivo4.modal.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
