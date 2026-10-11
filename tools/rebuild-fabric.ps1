$ErrorActionPreference = 'Continue'
$proj  = 'D:\dsh-build\diamondhud-fabric'
$build = "$proj\build"
$stage = "$build\stage"
$java  = 'C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon\bin\java.exe'
$javac = 'C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon\bin\javac.exe'
$libs  = 'D:\.minecraft\libraries'

$rtCp = @(
    'D:\dsh-build\rtool',
    "$libs\net\fabricmc\fabric-loader\0.19.5\fabric-loader-0.19.5.jar",
    "$libs\org\ow2\asm\asm\9.10.1\asm-9.10.1.jar",
    "$libs\org\ow2\asm\asm-tree\9.10.1\asm-tree-9.10.1.jar",
    "$libs\org\ow2\asm\asm-util\9.10.1\asm-util-9.10.1.jar",
    "$libs\org\ow2\asm\asm-commons\9.10.1\asm-commons-9.10.1.jar",
    "$libs\org\ow2\asm\asm-analysis\9.10.1\asm-analysis-9.10.1.jar"
) -join ';'

# --- the classpath for each pass must be in that pass's SOURCE namespace,
# --- or tiny-remapper silently skips every member rename.
$cpMojang   = "$libs\net\neoforged\minecraft-client-patched\21.11.42\minecraft-client-patched-21.11.42.jar"
$cpOfficial  = 'D:\.minecraft\versions\1.21.11\1.21.11.jar'

$mojJar = "$build\diamondhud-mojang.jar"
$midJar = "$build\diamondhud-official.jar"
$finJar = "$build\diamondhud-0.6.1-fabric.jar"

Add-Type -AssemblyName System.IO.Compression.FileSystem

Write-Host '### 1/5 compile'
# The argfile is regenerated every run from whatever is in src: a stale one silently
# omits files added later, and javac then reports them as "cannot find symbol".
$src = "$proj\src\dev\dsh\diamondhud"
$cp = (Get-Content "$build\cp.txt" -Raw).Trim() -replace '\\', '/'
$files = Get-ChildItem $src -Filter *.java | Sort-Object Name
if (Test-Path "$build\classes") { Remove-Item "$build\classes" -Recurse -Force }
$argsTxt = @('-nowarn', '-encoding', 'UTF-8', '-d', "$proj/build/classes", '-cp', "`"$cp`"")
foreach ($f in $files) { $argsTxt += ($f.FullName -replace '\\', '/') }
[System.IO.File]::WriteAllText("$build\javac.args", ($argsTxt -join "`n"),
    (New-Object System.Text.UTF8Encoding($false)))
& $javac "@$build\javac.args" 2>&1 | Select-Object -First 20
if ($LASTEXITCODE -ne 0) { Write-Host "javac FAILED"; exit 1 }
Write-Host ("  sources={0}  classes={1}" -f $files.Count,
    (Get-ChildItem "$build\classes" -Recurse -File).Count)

Write-Host '### 2/5 stage (classes + resources)'
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Force "$stage\dev\dsh\diamondhud" | Out-Null
Copy-Item "$build\classes\dev\dsh\diamondhud\*.class" "$stage\dev\dsh\diamondhud\" -Force
Copy-Item "$proj\resources\*" $stage -Recurse -Force
foreach ($f in @($mojJar, $midJar, $finJar)) { if (Test-Path $f) { Remove-Item $f -Force } }
[System.IO.Compression.ZipFile]::CreateFromDirectory($stage, $mojJar)
Write-Host ("  mojang jar: {0} bytes" -f (Get-Item $mojJar).Length)

Write-Host '### 3/5 pass 1  mojang -> official'
& $java -cp $rtCp RemapTool $mojJar $midJar 'D:\dsh-build\mojmap-official.tiny' mojang official $cpMojang 2>&1 |
    Where-Object { $_ -match 'ERROR|Exception|conflict' } | Select-Object -First 10
Write-Host ("  exit=$LASTEXITCODE  -> {0} bytes" -f (Get-Item $midJar).Length)

Write-Host '### 4/5 pass 2  official -> intermediary'
& $java -cp $rtCp RemapTool $midJar $finJar 'D:\dsh-build\inter\mappings.tiny' official intermediary $cpOfficial 2>&1 |
    Where-Object { $_ -match 'ERROR|Exception|conflict' } | Select-Object -First 10
Write-Host ("  exit=$LASTEXITCODE  -> {0} bytes" -f (Get-Item $finJar).Length)

Write-Host '### 5/5 verify'
# copying into the profile's mods folder is install.ps1's job: the folder name is
# non-ASCII, and a literal here gets mangled because PowerShell reads a BOM-less
# .ps1 as ANSI.

# every type/member the mod touches must now be spelled the way Fabric loads it
$bad = @('net/minecraft/resources/', 'net/minecraft/client/gui/GuiGraphics',
         'fromNamespaceAndPath', 'net/minecraft/world/item/Items',
         'net/minecraft/core/BlockPos', 'net/minecraft/client/DeltaTracker')
$z = [System.IO.Compression.ZipFile]::OpenRead($finJar)
foreach ($e in $z.Entries | Where-Object { $_.FullName -like '*.class' }) {
    $ms = New-Object System.IO.MemoryStream; $e.Open().CopyTo($ms); $b = $ms.ToArray()
    $txt = [System.Text.Encoding]::ASCII.GetString($b)
    $leftover = @()
    foreach ($p in $bad) { if ($txt.Contains($p)) { $leftover += $p } }
    $inter = @()
    foreach ($p in @('net/minecraft/class_', 'method_', 'field_')) {
        $inter += ("{0}x{1}" -f $p, ([regex]::Matches($txt, [regex]::Escape($p))).Count)
    }
    "{0,-40} {1}   {2}" -f $e.Name, ($inter -join ' '),
        $(if ($leftover) { "!! LEFTOVER: " + ($leftover -join ',') } else { "clean" })
}
$z.Dispose()
