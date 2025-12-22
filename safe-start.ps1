# Script khởi động an toàn cho WebDemo
# Kiểm tra và khởi động backend trước, sau đó frontend

Write-Host "🚀 WebDemo Safe Startup Script" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra xem backend có đang chạy không
Write-Host "🔍 Checking if backend is running..." -ForegroundColor Yellow
$javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue

if ($javaProcess) {
    Write-Host "✅ Backend is already running (PID: $($javaProcess.Id))" -ForegroundColor Green
    Write-Host ""

    $response = Read-Host "Do you want to restart backend? (y/N)"
    if ($response -eq 'y' -or $response -eq 'Y') {
        Write-Host "⏹️  Stopping backend..." -ForegroundColor Yellow
        Stop-Process -Id $javaProcess.Id -Force
        Start-Sleep -Seconds 2

        Write-Host "🔄 Starting backend..." -ForegroundColor Yellow
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; ./mvnw spring-boot:run"
        Write-Host "⏳ Waiting 30 seconds for backend to start..." -ForegroundColor Yellow
        Start-Sleep -Seconds 30
    }
} else {
    Write-Host "❌ Backend is not running" -ForegroundColor Red
    Write-Host "🔄 Starting backend..." -ForegroundColor Yellow

    # Khởi động backend trong terminal mới
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; ./mvnw spring-boot:run"

    Write-Host "⏳ Waiting 30 seconds for backend to start..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
}

# Kiểm tra backend health
Write-Host "🏥 Checking backend health..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/public/health" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Backend is healthy!" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Backend health check failed, but continuing..." -ForegroundColor Yellow
    Write-Host "   (This is OK if backend is still starting up)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "🎨 Starting frontend..." -ForegroundColor Yellow

# Kiểm tra xem Node process có đang chạy npm start không
$nodeProcess = Get-Process -Name "node" -ErrorAction SilentlyContinue | Where-Object {$_.Path -like "*node_modules*"}

if ($nodeProcess) {
    Write-Host "⚠️  Frontend appears to be running already" -ForegroundColor Yellow
    $response = Read-Host "Do you want to continue anyway? (y/N)"
    if ($response -ne 'y' -and $response -ne 'Y') {
        Write-Host "❌ Aborted" -ForegroundColor Red
        exit
    }
}

# Khởi động frontend
Write-Host "🚀 Launching frontend..." -ForegroundColor Green
Write-Host ""
Write-Host "📝 Tips:" -ForegroundColor Cyan
Write-Host "   - Frontend will be available at: http://localhost:9001" -ForegroundColor Gray
Write-Host "   - Backend API is at: http://localhost:8080" -ForegroundColor Gray
Write-Host "   - Press Ctrl+C in frontend terminal to stop" -ForegroundColor Gray
Write-Host ""

# Chạy npm start trong terminal hiện tại
npm start

