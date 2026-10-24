param(
    [string]$JdkHome = $env:JAVA_HOME,
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Version = '1.0.0'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'dist'
$stage = Join-Path $output 'package-input'
$frontendBuild = Join-Path $output 'frontend-build'
$image = Join-Path $output 'HanasakiAdvisor'
$zip = Join-Path $output "HanasakiAdvisor-$Version-windows.zip"

function Invoke-Checked([string]$Command, [string[]]$Arguments) {
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Command failed (exit $LASTEXITCODE)" }
}

if (-not $JdkHome) {
    $JdkHome = Split-Path -Parent (Split-Path -Parent (Get-Command javac.exe -ErrorAction Stop).Source)
}
$JdkHome = (Resolve-Path -LiteralPath $JdkHome).Path
$jpackage = Join-Path $JdkHome 'bin/jpackage.exe'
if (-not (Test-Path -LiteralPath $jpackage)) { throw 'A Windows JDK 21+ with jpackage is required. Use -JdkHome.' }
$jdkVersion = & $jpackage --version
if ($LASTEXITCODE -ne 0 -or [int]($jdkVersion.Split('.')[0]) -lt 21) { throw 'JDK 21 or newer is required.' }
$env:JAVA_HOME = $JdkHome
$env:PATH = (Join-Path $JdkHome 'bin') + ';' + $env:PATH

New-Item -ItemType Directory -Force -Path $output | Out-Null
# Only delete fixed build-output folders after checking their absolute locations.
foreach ($path in @($stage, $image)) {
    $absolute = [IO.Path]::GetFullPath($path)
    if ((Split-Path -Parent $absolute) -ne [IO.Path]::GetFullPath($output)) { throw "Unsafe output path: $absolute" }
    if (Test-Path -LiteralPath $absolute) { Remove-Item -LiteralPath $absolute -Recurse -Force }
}

New-Item -ItemType Directory -Force -Path $frontendBuild | Out-Null
foreach ($name in @('package.json', 'package-lock.json', 'index.html', 'vite.config.js')) {
    Copy-Item -LiteralPath (Join-Path $root "frontend/$name") -Destination $frontendBuild -Force
}
$source = Join-Path $frontendBuild 'src'
if (Test-Path -LiteralPath $source) {
    $resolvedSource = (Resolve-Path -LiteralPath $source).Path
    if ($resolvedSource -ne (Join-Path $frontendBuild 'src')) { throw "Unsafe source path: $resolvedSource" }
    Remove-Item -LiteralPath $resolvedSource -Recurse -Force
}
Copy-Item -LiteralPath (Join-Path $root 'frontend/src') -Destination $frontendBuild -Recurse
Push-Location $frontendBuild
try {
    Invoke-Checked 'npm.cmd' @('ci', '--no-audit', '--no-fund')
    Invoke-Checked 'npm.cmd' @('run', 'build')
} finally { Pop-Location }

Push-Location (Join-Path $root 'backend')
try {
    Invoke-Checked 'mvn.cmd' @('-B', '-Pdistribution', "-Dfrontend.dist=$frontendBuild/dist", 'clean', 'verify')
} finally { Pop-Location }

New-Item -ItemType Directory -Force -Path $stage | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'backend/target/saitama-school-advisor-0.0.1-SNAPSHOT.jar') -Destination (Join-Path $stage 'advisor.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $stage 'advisor.jar'))
try {
    $names = @($archive.Entries | ForEach-Object { $_.FullName })
    foreach ($required in @('BOOT-INF/classes/static/index.html', 'BOOT-INF/classes/school-master.csv', 'BOOT-INF/classes/application.properties')) {
        if ($names -notcontains $required) { throw "Missing packaged resource: $required" }
    }
    if ($names -match 'application-local|legacy-data|BOOT-INF/classes/(data|schema)\.sql|BOOT-INF/lib/(postgresql|h2-|HikariCP)') {
        throw 'Development or database resources found in distribution.'
    }
} finally { $archive.Dispose() }
Invoke-Checked $jpackage @(
    '--type', 'app-image', '--name', 'HanasakiAdvisor', '--app-version', $Version,
    '--vendor', 'Hanasaki School', '--dest', $output, '--input', $stage,
    '--main-jar', 'advisor.jar', '--main-class', 'org.springframework.boot.loader.launch.JarLauncher',
    '--java-options', '-Dfile.encoding=UTF-8', '--java-options', '-Djava.awt.headless=false',
    '--add-modules', 'java.se,jdk.unsupported,jdk.crypto.ec,jdk.localedata,jdk.charsets',
    '--arguments', '--advisor.desktop=true'
)
Copy-Item -LiteralPath (Join-Path $root 'docs/distribution-user-guide.txt') -Destination (Join-Path $image 'READ-ME.txt')
Compress-Archive -LiteralPath $image -DestinationPath $zip -Force
Write-Host "Distribution ready: $zip"
