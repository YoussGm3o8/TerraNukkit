@echo off
title Terra Nukkit Server
setlocal enabledelayedexpansion

REM Optimized JVM parameters for Terra world generation
set MEMORY=2G
set GC_OPTS=-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem
set PERFORMANCE_OPTS=-XX:+OptimizeStringConcat -XX:MaxInlineLevel=9

echo Starting Terra Nukkit server with optimized settings...
echo Memory: %MEMORY%
echo.

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo Java not found! Please install Java to run the server.
    pause
    exit /b
)

REM Start the server with optimized settings
java -Xmx%MEMORY% -Xms%MEMORY% %GC_OPTS% %PERFORMANCE_OPTS% -jar nukkit.jar --bootstrap plugins/TerraNukkit.jar

echo Server stopped.
pause 