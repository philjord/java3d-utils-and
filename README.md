Java3D Utils Android Readme
===
This code is a collection of altered java classes that override those in the java3d-core project to make it fully non-AWT dependent. This means the generated code can be used on Android.

It also includes compressed texture support. 

## Including Java3D-Android

Java3D-Android 1.7.2 is now mavenised on Maven Central just include a dependency in your build.gradle like so

	 dependencies {
    	...
    	implementation ('org.jogamp.java3d:java3d-utils-and:1.7.2')
    }

 
 