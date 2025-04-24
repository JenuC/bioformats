@echo off 
set CLASSPATH=lib\bioformats_package.jar;build 
java -cp "%CLASSPATH%" BioFormatsSDTWrapper %* 
