$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\zip-helper.ps1"

<#
  Pack the NeoForge build.

  NeoForge runs Mojang names in production, so the classes that javac produced go in
  as-is -- no remapping step, unlike the Fabric build.

  The jar is assembled by New-CleanZip rather than ZipFile.CreateFromDirectory on
  purpose: see the note in zip-helper.ps1, and the 0.6.0 release whose NeoForge asset
  was rejected as "not a valid mod" because every entry name used a backslash.

  Inputs (edit the paths for your machine):
    $neo\build\classes             compiled classes
    $neo\resources                 logo, texture, lang, META-INF/neoforge.mods.toml
#>
$neo    = 'C:\Users\Administrator\.dsh\mcmod\diamondhud'
$build  = "$neo\build"
$stage  = "$build\stage-neoforge"
$version = '0.6.1'

if (-not (Test-Path "$build\classes\dev\dsh\diamondhud")) {
    throw "compile first: $build\classes\dev\dsh\diamondhud is missing"
}

if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Force "$stage\dev\dsh\diamondhud" | Out-Null
Copy-Item "$build\classes\dev\dsh\diamondhud\*.class" "$stage\dev\dsh\diamondhud\" -Force
Copy-Item "$neo\resources\logo.png" $stage -Force
New-Item -ItemType Directory -Force "$stage\assets\diamondhud\textures\gui" | Out-Null
Copy-Item "$neo\resources\assets\diamondhud\textures\gui\placement_indicator.png" `
    "$stage\assets\diamondhud\textures\gui\" -Force
New-Item -ItemType Directory -Force "$stage\assets\diamondhud\lang" | Out-Null
Copy-Item "$neo\resources\assets\diamondhud\lang\*.json" "$stage\assets\diamondhud\lang\" -Force
New-Item -ItemType Directory -Force "$stage\META-INF" | Out-Null
Copy-Item "$neo\resources\META-INF\neoforge.mods.toml" "$stage\META-INF\" -Force

# FML does not require a manifest, but every jar.exe-built mod has one and its absence is
# one more thing to rule out when a loader says "not a valid mod"
$mf = @('Manifest-Version: 1.0', 'Created-By: DSH build script', '') -join "`r`n"
[System.IO.File]::WriteAllText("$stage\META-INF\MANIFEST.MF", $mf,
    (New-Object System.Text.UTF8Encoding($false)))

$jar = "$build\diamondhud-$version-neoforge.jar"
New-CleanZip -SourceDir $stage -ZipPath $jar

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($jar)
$names = $z.Entries | ForEach-Object { $_.FullName }
$count = $z.Entries.Count
$hasToml = $names -contains 'META-INF/neoforge.mods.toml'
$z.Dispose()

Write-Host ("{0}  {1:N0} bytes" -f (Split-Path $jar -Leaf), (Get-Item $jar).Length)
Write-Host ("  entries={0}  backslashes={1}  META-INF/neoforge.mods.toml={2}" -f
    $count, (Get-ZipBackslashCount -ZipPath $jar), $hasToml)
if ((Get-ZipBackslashCount -ZipPath $jar) -ne 0 -or -not $hasToml) {
    throw 'archive is not loadable by FML: entry names or mods.toml are wrong'
}
Write-Host '  ok'
