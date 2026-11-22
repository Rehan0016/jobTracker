# ===================================================================
# Safe Standalone Supabase Connection & Query Probe
# Runs completely outside Spring Boot to isolate database credentials.
# NEVER prints database passwords or secrets.
# ===================================================================

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "JobTracker: Safe Supabase Database Connection & Query Test" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. Source .env variables into the current process if present
$envFile = Join-Path $ProjectRoot ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment from .env file..." -ForegroundColor Gray
    Get-Content $envFile | Where-Object { $_ -match '^\s*([^#][^=]*?)\s*=\s*(.*)$' } | ForEach-Object {
        $k = $matches[1].Trim()
        $v = $matches[2].Trim().Trim('"').Trim("'")
        [System.Environment]::SetEnvironmentVariable($k, $v, "Process")
    }
}

# 2. Check Postgres JDBC driver location
$pgJar = Get-ChildItem -Path "$HOME\.m2\repository\org\postgresql\postgresql" -Recurse -Filter "postgresql-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $pgJar) {
    Write-Host "Compiling test dependencies with Maven..." -ForegroundColor Yellow
    & ".\mvnw.cmd" test-compile -q
    $pgJar = Get-ChildItem -Path "$HOME\.m2\repository\org\postgresql\postgresql" -Recurse -Filter "postgresql-*.jar" | Select-Object -First 1
}

# 3. Ensure test classes are compiled
if (-not (Test-Path "target\test-classes\com\rehan\jobtracker\config\SupabaseConnectionProbe.class")) {
    Write-Host "Compiling probe class..." -ForegroundColor Gray
    & ".\mvnw.cmd" test-compile -q
}

# 4. Run the standalone probe
$cp = "target\test-classes;target\classes;$($pgJar.FullName)"
& java -cp $cp com.rehan.jobtracker.config.SupabaseConnectionProbe
