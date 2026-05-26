@echo off
title VinRECIPE App Launcher

echo ===================================================
echo     VinRECIPE - Smart Recipe & Grocery Planner
echo ===================================================
echo.
echo [1/2] Compiling project files...
call mvn compile
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Compilation failed! 
    echo Please ensure Java 17+ and Maven are installed and configured on your PATH.
    echo.
    pause
    exit /b %errorlevel%
)

echo.
echo [2/2] Launching VinRECIPE (JavaFX)...
call mvn javafx:run
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Application failed to launch or exited with errors.
    echo.
    pause
    exit /b %errorlevel%
)
