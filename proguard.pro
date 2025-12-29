-injars      test.jar  
-outjars     test_processed.jar  
-libraryjars C:\IdeaProjects\j2me_sdk\lib\midpapi21.jar  
-libraryjars C:\IdeaProjects\j2me_sdk\lib\cldcapi11.jar
-libraryjars C:\IdeaProjects\j2me_sdk\lib\jsr75.jar
  
-microedition
-optimizationpasses 3
-overloadaggressively
-repackageclasses ''
-allowaccessmodification

-keep public class * extends javax.microedition.midlet.MIDlet

-keep class cc.nnproject.json.** { *; }

-printmapping out.map
-keepattributes SourceFile,LineNumberTable
-optimizations !code/simplification/object