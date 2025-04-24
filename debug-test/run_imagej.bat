@echo off 
set CLASSPATH=lib\bioformats_package.jar;imagej\ij.jar;build 
java -Xmx1024m -cp "%CLASSPATH%" ij.ImageJ 
