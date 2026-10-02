param([Parameter(Mandatory=$true)][string]$RelativePath,[ValidateSet('neoforge','fabric','mojang-meta')][string]$Repository='neoforge')
. (Join-Path $PSScriptRoot 'paths.ps1')
$ErrorActionPreference = 'Stop'
$cacheRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../.cache/maven'))
$target = [IO.Path]::GetFullPath((Join-Path $cacheRoot $RelativePath))
if (!$target.StartsWith($cacheRoot + [IO.Path]::DirectorySeparatorChar)) { throw 'Path escapes dependency cache' }
New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
$baseUrl=if($Repository -eq 'fabric'){'https://maven.fabricmc.net/'}elseif($Repository -eq 'mojang-meta'){'https://maven.neoforged.net/mojang-meta/'}else{'https://maven.neoforged.net/releases/'}
$temporary="$target.download-$([guid]::NewGuid().ToString('N'))"
try{
    for($attempt=0;$attempt -lt 3;$attempt++){
        try{Invoke-WebRequest -Uri ($baseUrl + $RelativePath.Replace('\','/')) -OutFile $temporary -TimeoutSec 60;break}
        catch{if($attempt -eq 2){throw}}
    }
    Move-Item -LiteralPath $temporary -Destination $target -Force
}finally{if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary}}
Write-Output $RelativePath
