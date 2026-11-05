<#
.SYNOPSIS
    Builds Immersive HUD with plain javac - no Gradle, no network.

.DESCRIPTION
    Compiles the sources against the exact jar list a running NeoForge 1.21.1 client uses,
    then packs a jar. This mirrors how the mod is developed; a normal NeoGradle project can
    also compile these sources if you prefer an IDE.

.PARAMETER ClasspathFile
    Semicolon separated jar list produced by your launcher.

.PARAMETER LibrariesRoot
    Folder holding the launcher libraries (net/minecraft/client, net/neoforged/neoforge).
    Defaults to the "libs" folder next to ClasspathFile.

.PARAMETER JdkHome
    JDK 21 home. Defaults to $env:JAVA_HOME.

.PARAMETER DeployTo
    Optional mods folder; the finished jar is copied there.

.EXAMPLE
    .\build.ps1 -ClasspathFile C:\mc\classpath.txt -DeployTo C:\mc\mods
#>
param(
    [Parameter(Mandatory = $true)][string]$ClasspathFile,
    [string]$LibrariesRoot = "",
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$DeployTo = ""
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not $JdkHome) { throw 'set JAVA_HOME or pass -JdkHome (JDK 21 required)' }
$javac = Join-Path $JdkHome 'bin\javac.exe'
if (-not (Test-Path $javac)) { throw "javac not found: $javac" }
if (-not (Test-Path $ClasspathFile)) { throw "missing classpath file: $ClasspathFile" }
if (-not $LibrariesRoot) { $LibrariesRoot = Join-Path (Split-Path -Parent $ClasspathFile) 'libs' }

# ---- classpath: the launcher's list, plus the client jar and the 21.1 neoforge jar
$cpRaw = [System.IO.File]::ReadAllText($ClasspathFile, [System.Text.Encoding]::UTF8).Trim()
$entries = @()
foreach ($p in ($cpRaw -split ';')) {
    if ($p.Trim()) { $entries += $p.Trim().Trim('"') }
}

$clientJar = Get-ChildItem (Join-Path $LibrariesRoot 'net\minecraft\client') -Recurse -Filter 'client-1.21.1-*-srg.jar' -ErrorAction SilentlyContinue |
    Select-Object -First 1
# only 21.1.x: a shared libraries folder often also holds 26.x jars built for a newer Java
$neoforgeJar = Get-ChildItem (Join-Path $LibrariesRoot 'net\neoforged\neoforge') -Recurse -Filter '*universal.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Directory.Name -match '^21\.1\.\d+$' } |
    Sort-Object DirectoryName | Select-Object -Last 1

if (-not $clientJar) { throw "vanilla client jar not found under $LibrariesRoot\net\minecraft\client" }
if (-not $neoforgeJar) { throw "neoforge 21.1.x universal jar not found under $LibrariesRoot\net\neoforged\neoforge" }
$entries += $clientJar.FullName
$entries += $neoforgeJar.FullName

$cp = ($entries | Where-Object { Test-Path $_ }) -join ';'
Write-Host ("classpath entries: {0}  client: {1}  neoforge: {2}" -f $entries.Count, $clientJar.Name, $neoforgeJar.Name)

# ---- compile
$build = Join-Path $here 'build'
$out = Join-Path $build 'classes'
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Force -Path $out | Out-Null

$files = Get-ChildItem (Join-Path $here 'src') -Recurse -Filter *.java -ErrorAction SilentlyContinue |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
if (-not $files) { throw 'no java sources under src' }

$argsFile = Join-Path $build 'javac.args'
$line = '-d "' + $out.Replace('\', '/') + '" -classpath "' + $cp.Replace('\', '/') +
        '" -encoding UTF-8 -proc:none --release 21 -nowarn ' + ($files -join ' ')
[System.IO.File]::WriteAllText($argsFile, $line, (New-Object System.Text.UTF8Encoding($false)))

Write-Host 'compiling...'
$ErrorActionPreference = 'Continue'
$javacOut = (& $javac "@$argsFile" 2>&1 | Out-String)
$code = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($javacOut.Trim()) { Write-Host $javacOut.Trim() }
if ($code -ne 0) { throw "javac failed: $code" }

# ---- pack
$pkg = Join-Path $build 'stage'
if (Test-Path $pkg) { Remove-Item $pkg -Recurse -Force }
New-Item -ItemType Directory -Force -Path $pkg | Out-Null
Get-ChildItem $out -Directory | ForEach-Object { Copy-Item $_.FullName (Join-Path $pkg $_.Name) -Recurse -Force }
Get-ChildItem $out -File -Filter *.class | ForEach-Object { Copy-Item $_.FullName $pkg -Force }
$res = Join-Path $here 'resources'
if (Test-Path $res) { Get-ChildItem $res -Force | ForEach-Object { Copy-Item $_.FullName $pkg -Recurse -Force } }

$version = '1.0.0'
$toml = Join-Path $pkg 'META-INF\neoforge.mods.toml'
if (Test-Path $toml) {
    $m = Select-String -Path $toml -Pattern '^\s*version\s*=\s*"([^"]+)"' | Select-Object -First 1
    if ($m) { $version = $m.Matches[0].Groups[1].Value }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar = Join-Path $build ("immersivehud-" + $version + ".jar")
if (Test-Path $jar) { Remove-Item $jar -Force }
$zip = [System.IO.Compression.ZipFile]::Open($jar, 'Create')
foreach ($f in (Get-ChildItem $pkg -Recurse -File)) {
    $rel = $f.FullName.Substring($pkg.Length + 1).Replace('\', '/')
    $e = $zip.CreateEntry($rel, [System.IO.Compression.CompressionLevel]::Optimal)
    $s = $e.Open(); $fs = [System.IO.File]::OpenRead($f.FullName)
    $fs.CopyTo($s); $fs.Dispose(); $s.Dispose()
}
$zip.Dispose()
Write-Host ("jar: {0} ({1} KB)" -f $jar, [math]::Round((Get-Item $jar).Length / 1KB, 1))

if ($DeployTo) {
    if (Test-Path $DeployTo) {
        Copy-Item $jar (Join-Path $DeployTo (Split-Path -Leaf $jar)) -Force
        Write-Host ("copied to " + $DeployTo)
    } else {
        Write-Host ("deploy folder not found, skipped: " + $DeployTo)
    }
}
