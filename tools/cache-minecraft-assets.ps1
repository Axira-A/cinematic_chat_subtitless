param([Parameter(Mandatory=$true)][string]$Minecraft,[string]$NeoFormCache=(Join-Path $env:USERPROFILE '.gradle/caches/neoformruntime'))
$ErrorActionPreference='Stop'
if($Minecraft -notmatch '^[0-9]+\.[0-9]+(?:\.[0-9]+)?$'){throw 'Invalid Minecraft version'}
$metadata=Join-Path $NeoFormCache "artifacts/minecraft_${Minecraft}_version_manifest.json"
$manifest=Get-Content -LiteralPath $metadata -Raw | ConvertFrom-Json
$root=Join-Path $NeoFormCache 'assets'
$index=Join-Path $root "indexes/$($manifest.assetIndex.id).json"
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($index)) | Out-Null
if(!(Test-Path -LiteralPath $index)){Invoke-WebRequest -Uri $manifest.assetIndex.url -OutFile $index -TimeoutSec 60}
if((Get-FileHash -LiteralPath $index -Algorithm SHA1).Hash.ToLowerInvariant() -ne $manifest.assetIndex.sha1){throw 'Asset index checksum mismatch'}
$assets=Get-Content -LiteralPath $index -Raw | ConvertFrom-Json
$missing=$assets.objects.PSObject.Properties.Value | Where-Object {!(Test-Path -LiteralPath (Join-Path $root "objects/$($_.hash.Substring(0,2))/$($_.hash)"))} | Sort-Object hash -Unique
$missing | ForEach-Object -Parallel {
    $hash=$_.hash;if($hash -notmatch '^[a-f0-9]{40}$'){throw 'Invalid content hash'}
    $target=Join-Path $using:root "objects/$($hash.Substring(0,2))/$hash"
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
    $temporary="$target.download-$([guid]::NewGuid().ToString('N'))"
    try{
        for($attempt=0;$attempt -lt 3;$attempt++){
            try{Invoke-WebRequest -Uri "https://resources.download.minecraft.net/$($hash.Substring(0,2))/$hash" -OutFile $temporary -TimeoutSec 30;break}catch{if($attempt -eq 2){throw}}
        }
        if((Get-FileHash -LiteralPath $temporary -Algorithm SHA1).Hash.ToLowerInvariant() -ne $hash){throw 'Asset content checksum mismatch'}
        if(!(Test-Path -LiteralPath $target)){Move-Item -LiteralPath $temporary -Destination $target}
    }finally{if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary}}
} -ThrottleLimit 12
& (Join-Path $PSScriptRoot 'reuse-development-assets.ps1') -Minecraft $Minecraft -Index $manifest.assetIndex.id -NeoFormCache $NeoFormCache
Write-Output "Cached $(@($missing).Count) missing assets for $Minecraft; asset index and new content hashes verified."
