@echo off
rem Offscreen Swing interaction checks (Windows). Run build.bat first.
cd /d "%~dp0"
java -Djava.awt.headless=true -cp "build\classes;build\test-classes" com.eduludo.Model.Iteration1Check || exit /b 1
