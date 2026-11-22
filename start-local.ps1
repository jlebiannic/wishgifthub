$ErrorActionPreference = 'Stop'

$frontend = Join-Path $PSScriptRoot 'wishgifthub-ui'
$envFile = Join-Path $PSScriptRoot 'rssh/.env'
$jar = Join-Path $PSScriptRoot 'wishgifthub-api/target/wishgifthub-api-1.0-SNAPSHOT.jar'
$java = 'D:/Dev/tools/openjdk-21.0.2/bin/java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    if ($env:JAVA_HOME) { $java = Join-Path $env:JAVA_HOME 'bin/java.exe' }
    else { $java = (Get-Command java.exe).Source }
}

$node = Join-Path $PSScriptRoot 'rssh/build-tools/node_modules/node/bin/node.exe'
if (-not (Test-Path -LiteralPath $node)) { $node = (Get-Command node.exe).Source }

foreach ($file in @($envFile, $jar, $java, (Join-Path $frontend 'node_modules/vite/bin/vite.js'))) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw "Fichier requis introuvable : $file" }
}

$settings = @{}
foreach ($line in (Get-Content -LiteralPath $envFile -Encoding UTF8)) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
        $name = $Matches[1]
        $value = $Matches[2].Trim()
        if ($value -match '^"(.*)"$|^''(.*)''$') {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $settings[$name] = $value
    }
}

foreach ($name in @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'WISHGIFTHUB_JWT_SECRET', 'WISHGIFTHUB_JWT_EXPIRATION_MS')) {
    if (-not $settings[$name]) { throw "Variable requise absente ou vide dans rssh/.env : $name" }
}

$logs = Join-Path (Split-Path $jar) 'local-run'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
$vite = $null
$previous = @{}

Push-Location $frontend
try {
    & $node scripts/generate-version.js
    if ($LASTEXITCODE -ne 0) { throw 'La generation de la version du front a echoue.' }

    $vite = Start-Process -FilePath $node `
        -ArgumentList '--dns-result-order=ipv6first node_modules/vite/bin/vite.js --host 127.0.0.1 --port 3000 --strictPort' `
        -WorkingDirectory $frontend -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logs 'ui.log') `
        -RedirectStandardError (Join-Path $logs 'ui.err.log')

    foreach ($name in $settings.Keys) {
        $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process')
    }

    Set-Location $PSScriptRoot
    Write-Host 'WishGiftHub : http://127.0.0.1:3000 (disponible quand Spring Boot a demarre)'
    Write-Host "Journaux du front : $logs"
    Write-Host 'Le backend utilise la base configuree dans rssh/.env. Ctrl+C arrete les deux serveurs.'
    & $java -jar $jar '--server.address=::1' '--server.port=8080'
    if ($LASTEXITCODE -ne 0) { throw "Le backend s'est arrete avec le code $LASTEXITCODE." }
} finally {
    if ($vite -and -not $vite.HasExited) { Stop-Process -Id $vite.Id -ErrorAction SilentlyContinue }
    foreach ($name in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
    Pop-Location
}
