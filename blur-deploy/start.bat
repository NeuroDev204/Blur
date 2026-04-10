@echo off
chcp 65001 >nul
echo ╔════════════════════════════════════════╗
echo ║     Blur Social Network - Deploy       ║
echo ╚════════════════════════════════════════╝
echo.

REM Check Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker not found!
    echo 👉 Install Docker Desktop: https://docs.docker.com/desktop/install/windows-install/
    pause
    exit /b 1
)

echo ✅ Docker found
echo.

REM Pull images
echo 📥 Pulling latest images from Docker Hub...
echo    This may take a few minutes on first run...
docker compose pull

echo.

REM Stop old
echo 🛑 Stopping old containers...
docker compose down 2>nul

echo.

REM Start
echo ▶️  Starting all services...
docker compose up -d

echo.

REM Wait
echo ⏳ Waiting for services to start (90 seconds)...
timeout /t 90 /nobreak >nul

echo.

REM Status
echo 📊 Service Status:
docker compose ps

echo.
echo ╔════════════════════════════════════════╗
echo ║            ✅ READY TO USE!            ║
echo ╚════════════════════════════════════════╝
echo.
echo 🌐 Access Application:
echo    → http://localhost
echo.
echo 🔧 Admin Panels:
echo    → Neo4j: http://localhost:7474 (neo4j/12345678)
echo.
echo 📝 Useful Commands:
echo    Stop:  stop.bat
echo    Logs:  docker compose logs -f
echo    Clean: docker compose down -v
echo.
pause