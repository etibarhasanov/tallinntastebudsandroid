# kotlinx.serialization keeps its generated serializers on the companion, which
# a shrinker cannot see is used. Release builds do not shrink today, but a rule
# that is already right costs nothing and stops a future flag from breaking the
# decoders quietly.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class ee.tallinntastebuds.** {
    *** Companion;
}
-keepclasseswithmembers class ee.tallinntastebuds.** {
    kotlinx.serialization.KSerializer serializer(...);
}
