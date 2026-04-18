# Packs files needed for Docker Compose deployment into a timestamped .zip
# Run from anywhere:  powershell -ExecutionPolicy Bypass -File scripts\package-docker-deploy.ps1
# Excludes: .git, build caches, local data, IDE folders (same idea as .dockerignore)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$FolderName = Split-Path -Leaf $RepoRoot

$StagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("expirement-docker-staging-" + [Guid]::NewGuid().ToString())
$StagingProject = Join-Path $StagingRoot $FolderName
$OutDir = Join-Path $RepoRoot "deploy-packages"
$ZipName = "expirement-docker-deploy-{0:yyyyMMdd-HHmmss}.zip" -f (Get-Date)
$ZipPath = Join-Path $OutDir $ZipName

New-Item -ItemType Directory -Force -Path $StagingProject | Out-Null
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$ExcludeDirs = @(".git", "node_modules", "target", "dist", "data", ".idea", ".vscode", ".cursor", "deploy-packages")

$RobocopyArgs = @(
    $RepoRoot,
    $StagingProject,
    "/E",
    "/NFL", "/NDL", "/NJH", "/NJS", "/NC", "/NS"
)
foreach ($d in $ExcludeDirs) {
    $RobocopyArgs += "/XD"
    $RobocopyArgs += $d
}

& robocopy @RobocopyArgs
$rc = $LASTEXITCODE
if ($rc -ge 8) {
    throw "robocopy failed with exit code $rc"
}

if (Test-Path -LiteralPath $ZipPath) {
    Remove-Item -LiteralPath $ZipPath -Force
}

Compress-Archive -Path $StagingProject -DestinationPath $ZipPath -Force

Remove-Item -LiteralPath $StagingRoot -Recurse -Force

Write-Host "OK: $ZipPath"
