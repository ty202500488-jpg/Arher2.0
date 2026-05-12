@echo off
echo ==============================
echo   ArcherDuel - Build & Run
echo ==============================

where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: javac not found. Please install Java JDK 11+
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

echo Java version:
java -version 2>&1 | findstr /i "version"

if not exist bin mkdir bin

echo.
echo Compiling...
javac -d bin src\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Starting game...
java -cp bin Main
pause
