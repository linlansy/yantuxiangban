@rem Minimal Gradle Wrapper launcher for Windows.
@if "%DEBUG%"=="" @echo off
setlocal
set "APP_HOME=%~dp0"
set "APP_BASE_NAME=%~n0"
set "CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar;%APP_HOME%gradle\wrapper\gradle-cli.jar"
if defined JAVA_HOME goto findJavaFromJavaHome
set "JAVA_EXE=java.exe"
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%"=="0" goto execute
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
goto fail
:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if exist "%JAVA_EXE%" goto execute
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
goto fail
:execute
"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
:end
endlocal
exit /b %ERRORLEVEL%
:fail
endlocal
exit /b 1
