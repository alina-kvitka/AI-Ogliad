@echo off
cd /d "%~dp0"
java -jar AI-Ogliad.jar
if errorlevel 1 pause
