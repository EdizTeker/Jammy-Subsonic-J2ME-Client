-injars      test.jar  
-outjars     test_processed.jar  
-libraryjars C:\IdeaProjects\j2me_sdk\lib\midpapi21.jar  
-libraryjars C:\IdeaProjects\j2me_sdk\lib\cldcapi11.jar
-libraryjars C:\IdeaProjects\j2me_sdk\lib\jsr75.jar
  
-dontshrink  
-dontoptimize  
-dontobfuscate  
-microedition  
  
-keep public class MainMIDlet  
-keep public class * extends javax.microedition.midlet.MIDlet