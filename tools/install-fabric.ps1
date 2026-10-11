$ErrorActionPreference = 'Stop'
$finJar = 'D:\dsh-build\diamondhud-fabric\build\diamondhud-0.6.0-fabric.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem

# The version folder name is non-ASCII (a Chinese word), and a .ps1 without a BOM
# is read as ANSI, which mangles it. Locate the profile by a file only it has
# instead of spelling the name out.
$verDir = Get-ChildItem 'D:\.minecraft\versions' -Directory |
    Where-Object { @(Get-ChildItem (Join-Path $_.FullName 'mods') -Filter '*litematica*' -ErrorAction SilentlyContinue).Count -gt 0 } |
    Select-Object -First 1
if (-not $verDir) { throw 'profile mods folder not found' }
$target = Join-Path $verDir.FullName 'mods\diamondhud-0.6.0-fabric.jar'
Write-Host ("profile : " + $verDir.Name)
Copy-Item $finJar $target -Force
Get-Item $target | ForEach-Object { "installed: {0} bytes  {1}" -f $_.Length, $_.LastWriteTime }

Write-Host ''
Write-Host '--- what the entrypoint class actually calls now ---'
$z = [System.IO.Compression.ZipFile]::OpenRead($target)
foreach ($name in @('dev/dsh/diamondhud/DiamondHudMod.class', 'dev/dsh/diamondhud/DiamondHudLayer.class')) {
    $e = $z.Entries | Where-Object { $_.FullName -eq $name }
    $ms = New-Object System.IO.MemoryStream; $e.Open().CopyTo($ms); $b = $ms.ToArray()
    $txt = [System.Text.Encoding]::ASCII.GetString($b)
    "== $name"
    foreach ($p in @('class_2960', 'method_60655', 'HudElementRegistry', 'addLast',
                     'HudElement', 'class_332', 'class_9779', 'render',
                     'fromNamespaceAndPath', 'net/minecraft/resources/')) {
        $c = ([regex]::Matches($txt, [regex]::Escape($p))).Count
        if ($c -gt 0) { "     {0,-24} x{1}" -f $p, $c }
    }
}
$z.Dispose()
