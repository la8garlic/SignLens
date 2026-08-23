param(
    [ValidateSet("ordinary", "churn", "idle", "sign-density")]
    [string]$Scenario = "idle",
    [int]$PlayerCount = 0,
    [int]$DurationSeconds = 20,
    [bool]$SignLensEnabled = $true,
    [string]$PaperJar = "",
    [int]$PaperPort = 25566,
    [switch]$KeepServer
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$java = $null
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $configuredJava = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path -LiteralPath $configuredJava) {
        $java = $configuredJava
    }
}
if ($null -eq $java) {
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($null -ne $javaCommand) {
        $java = $javaCommand.Source
    }
}
if ($null -eq $java) {
    throw "Java 25 was not found. Set JAVA_HOME or add java.exe to PATH."
}
$javaVersion = (& $java -version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch 'version "25\.') {
    throw "SignLens requires Java 25, but '$java' reported: $javaVersion"
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

if ($PlayerCount -le 0) {
    $PlayerCount = switch ($Scenario) {
        "ordinary" { 20 }
        "churn" { 100 }
        "idle" { 100 }
        "sign-density" { 1 }
    }
}
if ($DurationSeconds -le 0) {
    throw "DurationSeconds must be greater than zero"
}

& (Join-Path $repoRoot "gradlew.bat") build integrationProbe --console=plain --no-daemon
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed"
}

$serverRoot = Join-Path $env:TEMP ("signlens-paper-performance-" + [guid]::NewGuid().ToString("N"))
$pluginsRoot = Join-Path $serverRoot "plugins"
$signLensConfigRoot = Join-Path $pluginsRoot "SignLens"
$resultPath = Join-Path $serverRoot "performance-result.txt"
$serverLog = Join-Path $serverRoot "server.log"
$serverError = Join-Path $serverRoot "server-error.log"
New-Item -ItemType Directory -Path $signLensConfigRoot | Out-Null

Set-Content -LiteralPath (Join-Path $serverRoot "eula.txt") -Value "eula=true" -Encoding ASCII
Set-Content -LiteralPath (Join-Path $serverRoot "server.properties") -Value @(
    "online-mode=false",
    "server-port=$PaperPort",
    "max-players=$($PlayerCount + 10)",
    "spawn-protection=0",
    "view-distance=4",
    "simulation-distance=4",
    "allow-flight=true",
    "enable-command-block=false"
) -Encoding ASCII

Set-Content -LiteralPath (Join-Path $signLensConfigRoot "config.yml") -Value @"
enabled: $($SignLensEnabled.ToString().ToLowerInvariant())

detection:
  max-distance: 8.0
  scan-period-ticks: 2
  position-threshold: 0.02
  rotation-threshold-degrees: 1.0

focus:
  dwell-millis: 200
  lost-grace-millis: 300

render:
  mode: action-bar
  soft-limit: 96
  max-length: 120
  keepalive-millis: 2500

performance:
  idle-probe-ticks: 10

debug:
  enabled: true
"@ -Encoding UTF8

$pluginJar = Get-ChildItem -LiteralPath (Join-Path $repoRoot "build\libs") -Filter "SignLens-*.jar" | Select-Object -First 1
$probeJar = Get-ChildItem -LiteralPath (Join-Path $repoRoot "build\libs") -Filter "signlens-integration-probe-*.jar" | Select-Object -First 1
if ($null -eq $pluginJar -or $null -eq $probeJar) {
    throw "Expected SignLens and integration probe jars were built"
}
Copy-Item -LiteralPath $pluginJar.FullName -Destination $pluginsRoot
Copy-Item -LiteralPath $probeJar.FullName -Destination $pluginsRoot

$server = Start-Process `
    -FilePath $java `
    -ArgumentList @(
        "-Dsignlens.integration.mode=performance",
        "-Dsignlens.integration.result=$resultPath",
        "-Dsignlens.performance.scenario=$Scenario",
        "-Dsignlens.performance.duration-seconds=$DurationSeconds",
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
        node (Join-Path $repoRoot "tools\paper-integration\performance-client.mjs") `
            "127.0.0.1" "$PaperPort" "$Scenario" "$PlayerCount" "$DurationSeconds"
        if ($LASTEXITCODE -ne 0) {
            throw "The performance protocol clients did not all complete successfully"
        }
    } finally {
        Pop-Location
    }

    $deadline = (Get-Date).AddSeconds(15)
    while (-not (Test-Path -LiteralPath $resultPath) -and (Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 250
    }
    if (-not (Test-Path -LiteralPath $resultPath)) {
        throw "The performance probe produced no result. See $serverLog"
    }

    $result = Get-Content -LiteralPath $resultPath -Raw
    Write-Host "Paper performance result: $result"
    if ($result -notmatch "^PERFORMANCE PASS ") {
        throw "Paper performance probe failed"
    }
} finally {
    if (-not $KeepServer -and -not $server.HasExited) {
        Stop-Process -Id $server.Id -Force
    }
    Write-Host "Paper performance logs: $serverRoot"
    if ($KeepServer) {
        Write-Host "Paper server kept running on port $PaperPort"
    }
    if (-not $KeepServer -and (Test-Path -LiteralPath $serverRoot)) {
        Remove-Item -LiteralPath $serverRoot -Recurse -Force
    }
}
