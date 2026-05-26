@echo off
title VinRECIPE App Launcher

echo [1/2] Dang khoi dong MySQL...
start /B "" "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --defaults-file="C:\Users\Admin\my.ini" >nul 2>&1
timeout /t 3 /nobreak >nul

echo [2/2] Dang khoi dong ung dung VinRECIPE (JavaFX)...
set PATH=%PATH%;C:\Users\Admin\maven\apache-maven-3.9.6\bin
call mvn javafx:run
