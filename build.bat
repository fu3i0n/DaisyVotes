@echo off
SETLOCAL

set plugin=DaisyVotes
set version=1.4
set target="D:\Desktop\DaisyVotes\jar
set source=.\build\libs\%plugin%-%version%-shaded.jar
set destination=%target%\%plugin%-%version%.jar

echo Building %plugin% plugin...
call .\gradlew shadowJar
if %ERRORLEVEL% neq 0 (
    echo Build failed! Check the errors above.
    exit /b %ERRORLEVEL%
)

echo Copying JAR to server plugins folder...
copy /y "%source%" "%destination%"
if %ERRORLEVEL% neq 0 (
    echo Failed to copy JAR file!
    exit /b %ERRORLEVEL%
)

echo Cleaning up configuration files...
del /f "%target%\%plugin%\config.yml"

echo.
echo Build completed successfully!
echo JAR location: %destination%
for %%I in ("%source%") do echo JAR size: %%~zI bytes

ENDLOCAL
