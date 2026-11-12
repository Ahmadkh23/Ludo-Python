@echo off
rem ============================================================================
rem  EduLudo build (Windows). Needs only a JDK 17 or newer - no Maven, no internet.
rem    build.bat          compile, run all tests, create dist\EduLudo.jar
rem    build.bat exe      same, then create dist\EduLudo\EduLudo.exe (Java bundled)
rem ============================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

where javac >nul 2>nul || (echo [ERROR] javac not found. Install a JDK 17+ and add it to PATH. & exit /b 1)

echo === Cleaning
if exist build rmdir /s /q build
if exist dist\EduLudo.jar del /q dist\EduLudo.jar
mkdir build\classes build\test-classes 2>nul
if not exist dist mkdir dist

echo === Compiling application
rem javac argument files treat "\" as an escape character, so write the paths with "/"
(for /r src\main\java %%f in (*.java) do @(set "p=%%f" & echo "!p:\=/!")) > build\main-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -d build\classes @build\main-sources.txt || (echo [ERROR] Compilation failed & exit /b 1)
xcopy /e /i /q /y src\main\resources build\classes >nul

echo === Compiling tests
(for /r src\test\java %%f in (*.java) do @(set "p=%%f" & echo "!p:\=/!")) > build\test-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -cp build\classes -d build\test-classes @build\test-sources.txt || (echo [ERROR] Test compilation failed & exit /b 1)

echo === Running tests
java -Djava.awt.headless=true -cp "build\classes;build\test-classes" com.eduludo.testing.TestRunner --scan build\test-classes || (echo [ERROR] Tests failed - the JAR was not built & exit /b 1)

echo === Creating dist\EduLudo.jar
jar --create --file dist\EduLudo.jar --main-class com.eduludo.App -C build\classes . || (echo [ERROR] jar failed & exit /b 1)
echo Done: dist\EduLudo.jar   (run with: java -jar dist\EduLudo.jar, or double-click it)

if /i "%~1"=="exe" (
    echo === Creating dist\EduLudo\EduLudo.exe with a bundled Java runtime
    where jpackage >nul 2>nul || (echo [ERROR] jpackage not found - it ships with JDK 17+ & exit /b 1)
    if exist dist\EduLudo rmdir /s /q dist\EduLudo
    if exist build\jpackage-input rmdir /s /q build\jpackage-input
    mkdir build\jpackage-input
    copy /y dist\EduLudo.jar build\jpackage-input\ >nul
    jpackage --type app-image --name EduLudo --app-version 1.0.0 --vendor "EduLudo Team" ^
        --input build\jpackage-input --main-jar EduLudo.jar --main-class com.eduludo.App ^
        --add-modules java.desktop,java.prefs --dest dist || (echo [ERROR] jpackage failed & exit /b 1)
    echo Done: dist\EduLudo\EduLudo.exe   ^(copy the whole dist\EduLudo folder - no Java install needed^)
)
endlocal

