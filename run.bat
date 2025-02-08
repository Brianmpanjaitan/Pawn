@echo off
set JAVA_HOME=%~dp0java
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVAFX_PATH=%~dp0javafx\lib
java --module-path %JAVAFX_PATH% --add-modules javafx.controls,javafx.fxml -jar Pawn.jar
pause
