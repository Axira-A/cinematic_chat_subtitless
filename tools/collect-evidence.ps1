$ErrorActionPreference='Stop'
$workspace=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
& python (Join-Path $workspace 'tools/write-matrix-report.py') --archive
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
