$ErrorActionPreference = 'Stop'

<#
  Build a zip/jar whose entry names always use forward slashes.

  .NET Framework's ZipFile.CreateFromDirectory writes entry names with the platform
  separator, so on Windows a jar built that way contains "META-INF\neoforge.mods.toml".
  FML looks for "META-INF/neoforge.mods.toml", does not find it, and rejects the whole
  jar as "not a valid mod". Fabric tolerates the backslash form, which is why only the
  NeoForge build ever failed from this.

  So build the archive here, entry by entry, always with '/'.
#>
function New-CleanZip {
    param(
        [Parameter(Mandatory)][string] $SourceDir,
        [Parameter(Mandatory)][string] $ZipPath
    )
    # ZipArchiveMode lives in System.IO.Compression, ZipFile in ...FileSystem; both are
    # needed and neither is loaded by default in Windows PowerShell 5.1
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    if (Test-Path -LiteralPath $ZipPath) { Remove-Item -LiteralPath $ZipPath -Force }
    $root = (Resolve-Path -LiteralPath $SourceDir).Path.TrimEnd('\')
    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($f in Get-ChildItem -LiteralPath $root -Recurse -File | Sort-Object FullName) {
            $rel = $f.FullName.Substring($root.Length + 1).Replace('\', '/')
            $entry = $zip.CreateEntry($rel, [System.IO.Compression.CompressionLevel]::Optimal)
            $out = $entry.Open()
            $in = [System.IO.File]::OpenRead($f.FullName)
            try { $in.CopyTo($out) } finally { $in.Dispose(); $out.Dispose() }
        }
    } finally {
        $zip.Dispose()
    }
}

<# How many entries still carry a backslash. 0 means the archive is safe for FML. #>
function Get-ZipBackslashCount {
    param([Parameter(Mandatory)][string] $ZipPath)
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $z = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
    try { return @($z.Entries | Where-Object { $_.FullName -like '*\*' }).Count }
    finally { $z.Dispose() }
}
