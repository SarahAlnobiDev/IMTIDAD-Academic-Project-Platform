$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcRoot = Join-Path $projectRoot 'src'
$outDir = Join-Path $projectRoot 'out\production\untitled'

# Prefer JAVAFX_HOME, otherwise try the common JavaFX 26.0.2 download locations.
$javaFxHome = $env:JAVAFX_HOME
if ([string]::IsNullOrWhiteSpace($javaFxHome)) {
    $candidates = @(
        (Join-Path $HOME 'Downloads\javafx-sdk-26.0.2'),
        (Join-Path $HOME 'Downloads\openjfx-26.0.2_windows-x64_bin-sdk\javafx-sdk-26.0.2')
    )
    $javaFxHome = $candidates | Where-Object { Test-Path (Join-Path $_ 'lib') } | Select-Object -First 1
}
if ([string]::IsNullOrWhiteSpace($javaFxHome) -or !(Test-Path (Join-Path $javaFxHome 'lib'))) {
    Write-Host '❌ JavaFX 26.0.2 was not found. Set JAVAFX_HOME to the javafx-sdk-26.0.2 folder or load pom.xml as a Maven project.'
    exit 1
}
$javaFxLib = Join-Path $javaFxHome 'lib'

$ojdbcJar = Join-Path $HOME '.m2\repository\com\oracle\database\jdbc\ojdbc11\21.11.0.0\ojdbc11-21.11.0.0.jar'
if (!(Test-Path $ojdbcJar)) {
    Write-Host '❌ Oracle JDBC was not found in the local Maven repository. Load pom.xml in IntelliJ once to download it.'
    exit 1
}

$fxJars = (Get-ChildItem -Path $javaFxLib -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
$classpath = "$srcRoot;$outDir;$fxJars;$ojdbcJar"

if (Test-Path $outDir) { Remove-Item $outDir -Recurse -Force }
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$javaFiles = Get-ChildItem -Path $srcRoot -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
$arguments = @('-d', $outDir, '-cp', $classpath) + $javaFiles
& javac @arguments 2>&1
if ($LASTEXITCODE -ne 0) { Write-Host '❌ Compilation failed.'; exit 1 }

Get-ChildItem -Path $srcRoot -Recurse | Where-Object { !$_.PSIsContainer -and $_.Extension -ne '.java' } | ForEach-Object {
    $relative = $_.FullName.Substring($srcRoot.Length + 1)
    $dest = Join-Path $outDir $relative
    $destDir = Split-Path $dest -Parent
    if (!(Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    Copy-Item $_.FullName -Destination $dest -Force
}
Write-Host '✅ Compilation succeeded and resources were copied.'
