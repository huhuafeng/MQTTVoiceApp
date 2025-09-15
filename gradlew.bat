@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to Gradle separately.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Split up the JVM options passed via JAVA_OPTS and GRADLE_OPTS
set JVM_OPTS=
set JAVA_OPTS=%DEFAULT_JVM_OPTS% %JAVA_OPTS%
:javaOptsLoop
if "%1"=="" goto endJavaOptsLoop
if /i "%1" == "-D" goto jvmOpt
if /i "%1" == "-X" goto jvmOpt
if /i "%1" == "--add-opens" goto jvmOpt
if /i "%1" == "--add-exports" goto jvmOpt
if /i "%1" == "--patch-module" goto jvmOpt
goto endJavaOptsLoop
:jvmOpt
set JVM_OPTS=%JVM_OPTS% %1 %2
shift
shift
goto javaOptsLoop
:endJavaOptsLoop

set CMD_LINE_ARGS=
:argsLoop
if "%1"=="" goto endArgsLoop
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto argsLoop
:endArgsLoop

"%JAVA_EXE%" %JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.launcher.GradleMain %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code.
if not "" == "%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
