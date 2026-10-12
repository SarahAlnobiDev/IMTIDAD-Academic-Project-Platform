$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$outDir = Join-Path $projectRoot 'out\production\untitled'
$javaFxHome = $env:JAVAFX_HOME
if ([string]::IsNullOrWhiteSpace($javaFxHome)) {
    $candidates = @(
        (Join-Path $HOME 'Downloads\javafx-sdk-26.0.2'),
        (Join-Path $HOME 'Downloads\openjfx-26.0.2_windows-x64_bin-sdk\javafx-sdk-26.0.2')
    )
    $javaFxHome = $candidates | Where-Object { Test-Path (Join-Path $_ 'lib') } | Select-Object -First 1
}
if ([string]::IsNullOrWhiteSpace($javaFxHome)) { Write-Host '❌ Set JAVAFX_HOME first.'; exit 1 }
$javaFxLib = Join-Path $javaFxHome 'lib'
$ojdbcJar = Join-Path $HOME '.m2\repository\com\oracle\database\jdbc\ojdbc11\21.11.0.0\ojdbc11-21.11.0.0.jar'

if (!(Test-Path $outDir)) { & (Join-Path $projectRoot 'compile.ps1') }
$fxJars = (Get-ChildItem -Path $javaFxLib -Filter '*.jar' | ForEach-Object { $_.FullName }) -join ';'
$classpath = "$outDir;$fxJars;$ojdbcJar"
& java --module-path $javaFxLib --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp $classpath com.imtidad.Main
