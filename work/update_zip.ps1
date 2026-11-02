param(
    [Parameter(Mandatory = $true)][string]$Archive,
    [Parameter(Mandatory = $true)][string]$Source,
    [switch]$KeepBackup
)
# KeepBackup：先把原包复制一份再替换。
# 不管有没有它，出错时都不会动原包：这里先写临时文件、全部成功才 Move 过去。
# （旧版是原地 Open(Update)，抛错时归档已经被改坏了。）

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

# PowerShell 5.1 跑在 .NET Framework 上，没有 [System.IO.Path]::GetRelativePath
# （那是 .NET Core 才加的，旧版脚本在这里直接炸）。这里自己算：
# 源目录前缀截掉，分隔符统一成 zip 里的 '/'。
function Get-RelativeEntryPath {
    param([string]$Root, [string]$Full)

    # TrimEnd/TrimStart 要的是 char[]：直接给 '\', '/' 会被当成 string[]
    $trim = [char[]]@('\', '/')
    $rootTrimmed = $Root.TrimEnd($trim)
    if (-not $Full.StartsWith($rootTrimmed, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "源文件不在源目录下：$Full"
    }
    return $Full.Substring($rootTrimmed.Length).TrimStart($trim).Replace('\', '/')
}

$archivePath = (Resolve-Path -LiteralPath $Archive).Path
$sourcePath = (Resolve-Path -LiteralPath $Source).Path
$dir = [System.IO.Path]::GetDirectoryName($archivePath)
$name = [System.IO.Path]::GetFileNameWithoutExtension($archivePath)
$ext = [System.IO.Path]::GetExtension($archivePath)
$tempPath = Join-Path $dir ($name + ".update-tmp" + $ext)

Copy-Item -LiteralPath $archivePath -Destination $tempPath -Force
$zip = $null
try {
    $zip = [System.IO.Compression.ZipFile]::Open($tempPath, [System.IO.Compression.ZipArchiveMode]::Update)
    $updated = 0
    $added = 0
    Get-ChildItem -LiteralPath $sourcePath -Recurse -File | ForEach-Object {
        $relative = Get-RelativeEntryPath -Root $sourcePath -Full $_.FullName
        if ($relative -eq "META-INF/MANIFEST.MF") {
            return
        }
        $entry = $zip.GetEntry($relative)
        if ($null -ne $entry) {
            $entry.Delete()
        } else {
            $added++
        }
        $entry = $zip.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
        $input = [System.IO.File]::OpenRead($_.FullName)
        $output = $entry.Open()
        try {
            $input.CopyTo($output)
        } finally {
            $output.Dispose()
            $input.Dispose()
        }
        $updated++
    }
    $zip.Dispose()
    $zip = $null

    if ($KeepBackup) {
        $backup = $archivePath + ".bak-" + (Get-Date -Format "yyyyMMdd-HHmmss")
        Copy-Item -LiteralPath $archivePath -Destination $backup -Force
        Write-Output "backup=$backup"
    }
    Move-Item -LiteralPath $tempPath -Destination $archivePath -Force
    Write-Output "updated=$updated added=$added"
} catch {
    if ($null -ne $zip) { $zip.Dispose() }
    if (Test-Path -LiteralPath $tempPath) { Remove-Item -LiteralPath $tempPath -Force }
    throw
}
