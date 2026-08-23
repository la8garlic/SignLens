param(
    [string]$PaperJar = "",
    [int]$PaperPort = 25565,
    [switch]$KeepServer
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    $javaHome = "C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot"
}
$java = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path -LiteralPath $java)) {
    throw "Java 25 was not found at $java"
}

if ([string]::IsNullOrWhiteSpace($PaperJar)) {
    $PaperJar = Join-Path $env:TEMP "paper-26.2-112.jar"
    if (-not (Test-Path -LiteralPath $PaperJar)) {
        $builds = Invoke-RestMethod -Uri "https://fill.papermc.io/v3/projects/paper/versions/26.2/builds"
        $build = $builds | Where-Object { $_.id -eq 112 } | Select-Object -First 1
        if ($null -eq $build) {
            throw "Paper 26.2 build 112 was not found"
        }
        Invoke-WebRequest `
            -Uri $build.downloads.'server:default'.url `
            -OutFile $PaperJar
    }
}
if (-not (Test-Path -LiteralPath $PaperJar)) {
    throw "Paper jar was not found at $PaperJar"
}

$env:JAVA_HOME = $javaHome
& (Join-Path $repoRoot "gradlew.bat") build integrationProbe --console=plain --no-daemon
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed"
}

$serverRoot = Join-Path $env:TEMP ("signlens-paper-integration-" + [guid]::NewGuid().ToString("N"))
$pluginsRoot = Join-Path $serverRoot "plugins"
$resultPath = Join-Path $serverRoot "raytrace-result.txt"
$serverLog = Join-Path $serverRoot "server.log"
$serverError = Join-Path $serverRoot "server-error.log"
New-Item -ItemType Directory -Path $pluginsRoot | Out-Null

Set-Content -LiteralPath (Join-Path $serverRoot "eula.txt") -Value "eula=true" -Encoding ASCII
Set-Content -LiteralPath (Join-Path $serverRoot "server.properties") -Value @(
    "online-mode=false",
    "server-port=$PaperPort",
    "spawn-protection=0",
    "view-distance=4",
    "simulation-distance=4",
    "enable-command-block=false"
) -Encoding ASCII

$pluginJar = Get-ChildItem -LiteralPath (Join-Path $repoRoot "build\libs") -Filter "SignLens-*.jar" | Select-Object -First 1
$probeJar = Get-ChildItem -LiteralPath (Join-Path $repoRoot "build\libs") -Filter "signlens-integration-probe-*.jar" | Select-Object -First 1
if ($null -eq $pluginJar -or $null -eq $probeJar) {
    throw "Expected SignLens and integration probe jars were not built"
}
Copy-Item -LiteralPath $pluginJar.FullName -Destination $pluginsRoot
Copy-Item -LiteralPath $probeJar.FullName -Destination $pluginsRoot

$server = Start-Process `
    -FilePath $java `
    -ArgumentList @(
        "-Dsignlens.integration.result=$resultPath",
        "-jar", $PaperJar,
        "--nogui"
    ) `
    -WorkingDirectory $serverRoot `
    -RedirectStandardOutput $serverLog `
    -RedirectStandardError $serverError `
    -WindowStyle Hidden `
    -PassThru

try {
    $deadline = (Get-Date).AddSeconds(120)
    do {
        Start-Sleep -Seconds 2
        if ($server.HasExited) {
            throw "Paper exited before startup. See $serverLog"
        }
        $logText = if (Test-Path -LiteralPath $serverLog) { Get-Content -LiteralPath $serverLog -Raw } else { "" }
    } while ($logText -notmatch "Done \(")

    Push-Location (Join-Path $repoRoot "tools\paper-integration")
    try {
        if (-not (Test-Path -LiteralPath "node_modules\minecraft-protocol")) {
            npm install --ignore-scripts --no-audit --no-fund
            if ($LASTEXITCODE -ne 0) {
                throw "npm install failed"
            }
        }
        node (Join-Path $repoRoot "tools\paper-integration\paper-raytrace-client.mjs") "127.0.0.1" "$PaperPort"
        if ($LASTEXITCODE -ne 0) {
            throw "The protocol client could not connect to Paper"
        }
    } finally {
        Pop-Location
    }

    $deadline = (Get-Date).AddSeconds(15)
    while (-not (Test-Path -LiteralPath $resultPath) -and (Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 250
    }
    if (-not (Test-Path -LiteralPath $resultPath)) {
        throw "The probe produced no result. See $serverLog"
    }

    $result = Get-Content -LiteralPath $resultPath -Raw
    Write-Host "Paper ray-trace integration result: $result"
    if ($result -notmatch "^PASS ") {
        throw "Paper ray-trace integration failed"
    }
} finally {
    if (-not $KeepServer -and -not $server.HasExited) {
        Stop-Process -Id $server.Id -Force
    }
    Write-Host "Paper integration logs: $serverRoot"
    if ($KeepServer) {
        Write-Host "Paper server kept running on port $PaperPort"
    }
    if (-not $KeepServer -and (Test-Path -LiteralPath $serverRoot)) {
        Remove-Item -LiteralPath $serverRoot -Recurse -Force
    }
}
