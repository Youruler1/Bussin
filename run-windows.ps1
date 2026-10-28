<#
.SYNOPSIS
    One-Click Windows Launcher for Bussin' Bus Ticket Booking Platform.
.DESCRIPTION
    Automatically detects JDK 17+ and Node.js on Windows, configures environment
    variables (including the bundled .m2 Maven cache), and starts both:
      1. Spring Boot Backend on http://localhost:8080
      2. React / Vite Frontend on http://localhost:5173
.PARAMETER Mode
    'local' (default): Zero-setup H2 in-memory PostgreSQL-compatible mode with full demo seed data.
    'postgres': Connects to PostgreSQL on localhost:5432 (runs Flyway migrations + demo trip seeding).
#>
param(
    [ValidateSet("local", "postgres")]
    [string]$Mode = "local"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  Bussin' Bus Booking Platform - Windows Launcher ($Mode mode)" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

# 1. Auto-detect JDK 17+
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $candidates = @()
    foreach ($dir in @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\OpenLogic",
        "C:\Program Files\Microsoft"
    )) {
        if (Test-Path $dir) {
            $candidates += Get-ChildItem -Path $dir -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match "jdk-17|jdk-21|jdk" }
        }
    }
    $validJdk = $candidates | Where-Object { Test-Path "$($_.FullName)\bin\java.exe" } | Select-Object -First 1
    if ($validJdk) {
        $env:JAVA_HOME = $validJdk.FullName
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Error "JDK 17+ was not found. Please install JDK 17 (e.g. 'winget install EclipseAdoptium.Temurin.17.JDK') or set JAVA_HOME."
    exit 1
}
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Host "[OK] Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green

# 2. Auto-detect Node.js
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    $nodeCandidates = @(
        "C:\Program Files\nodejs",
        "$env:USERPROFILE\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin"
    )
    foreach ($nd in $nodeCandidates) {
        if (Test-Path "$nd\node.exe") {
            $env:PATH = "$nd;$env:PATH"
            break
        }
    }
}

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Error "Node.js was not found. Please install Node.js LTS ('winget install OpenJS.NodeJS.LTS')."
    exit 1
}
$nodeVersion = & node -v
Write-Host "[OK] Using Node.js:   $nodeVersion" -ForegroundColor Green

# 3. Configure bundled offline Maven repository if present (ZIP distribution),
#    otherwise Maven uses the standard $HOME\.m2 cache (Git clone distribution)
$mavenHomeInit = ""
if (Test-Path "$ProjectRoot\.m2") {
    $env:MAVEN_USER_HOME = "$ProjectRoot\.m2"
    $mavenHomeInit = "`$env:MAVEN_USER_HOME = '$ProjectRoot\.m2'"
    Write-Host "[OK] Using bundled Maven cache: $env:MAVEN_USER_HOME" -ForegroundColor Green
} else {
    Write-Host "[OK] Using default user Maven cache ($env:USERPROFILE\.m2)" -ForegroundColor Green
}

# 4. Launch Backend in a new PowerShell window using SPRING_PROFILES_ACTIVE
#    (Avoids PowerShell -Ddot.property quoting issues across child processes)
$springProfile = if ($Mode -eq "local") { "local" } else { "default" }

$backendCmd = @"
`$env:JAVA_HOME = '$env:JAVA_HOME'
`$env:PATH = '$env:JAVA_HOME\bin;' + `$env:PATH
$mavenHomeInit
`$env:SPRING_PROFILES_ACTIVE = '$springProfile'
Set-Location '$ProjectRoot'
Write-Host 'Starting Spring Boot Backend (SPRING_PROFILES_ACTIVE=$springProfile) on http://localhost:8080 ...' -ForegroundColor Cyan
.\mvnw.cmd spring-boot:run
"@

Write-Host "`n[1/2] Launching Backend Server (port 8080)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $backendCmd

# Wait up to 45 seconds for Backend port 8080 to become ready before starting Frontend
# so Vite never hits ECONNREFUSED proxy errors on initial page load
Write-Host "      Waiting for Backend on http://localhost:8080/api/v1/health " -NoNewline -ForegroundColor Gray
$maxRetries = 45
$backendReady = $false
for ($i = 0; $i -lt $maxRetries; $i++) {
    try {
        $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health" -Method Get -TimeoutSec 1 -ErrorAction Stop
        if ($resp.status -eq "UP") {
            $backendReady = $true
            break
        }
    } catch {
        Write-Host "." -NoNewline -ForegroundColor Gray
        Start-Sleep -Seconds 1
    }
}
Write-Host ""
if ($backendReady) {
    Write-Host "[OK] Backend is UP on http://localhost:8080!" -ForegroundColor Green
} else {
    Write-Host "[WARN] Backend is still starting up in its window; proceeding to start Frontend..." -ForegroundColor Yellow
}

# 5. Launch Frontend in a new PowerShell window
$frontendDir = Join-Path $ProjectRoot "frontend"
$frontendCmd = @"
`$env:PATH = '$env:PATH'
Set-Location '$frontendDir'
if (-not (Test-Path 'node_modules\vite\bin\vite.js')) {
    Write-Host 'Installing frontend npm dependencies...' -ForegroundColor Yellow
    npm install
}
Write-Host 'Starting Vite Frontend Dev Server on http://localhost:5173 ...' -ForegroundColor Cyan
node .\node_modules\vite\bin\vite.js --host localhost --port 5173
"@

Write-Host "[2/2] Launching Frontend Dev Server (port 5173)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $frontendCmd

Write-Host "`n================================================================" -ForegroundColor Green
Write-Host "  Both servers are running!" -ForegroundColor Green
Write-Host "  - Frontend UI:    http://localhost:5173" -ForegroundColor White
Write-Host "  - Backend Health: http://localhost:8080/api/v1/health" -ForegroundColor White
Write-Host "  - Demo Search:    BOM (Mumbai) -> PNQ (Pune) on 2026-11-01" -ForegroundColor White
Write-Host "================================================================" -ForegroundColor Green
