param([Parameter(Mandatory=$true)][string[]]$Minecraft)
$ErrorActionPreference='Stop'
$artifactRoot=Join-Path $env:USERPROFILE '.gradle/caches/neoformruntime/artifacts'
[IO.Directory]::CreateDirectory($artifactRoot) | Out-Null
$index=Invoke-RestMethod 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json' -TimeoutSec 60
foreach($version in $Minecraft){
    if($version -notmatch '^[0-9]+\.[0-9]+(?:\.[0-9]+)?$'){throw 'Invalid Minecraft version'}
    $description=$index.versions | Where-Object {$_.id -eq $version} | Select-Object -First 1
    if(!$description){throw "Version not in Mojang's manifest: $version"}
    $metadata=Join-Path $artifactRoot "minecraft_${version}_version_manifest.json"
    if(!(Test-Path -LiteralPath $metadata)){Invoke-WebRequest -Uri $description.url -OutFile $metadata -TimeoutSec 60}
    if((Get-FileHash -LiteralPath $metadata -Algorithm SHA1).Hash.ToLowerInvariant() -ne $description.sha1){throw "Version manifest checksum mismatch: $version"}
    $manifest=Get-Content -LiteralPath $metadata -Raw | ConvertFrom-Json
    foreach($kind in @('client','server','client_mappings','server_mappings')){
        $download=$manifest.downloads.$kind;if(!$download){continue}
        $extension=if($kind.EndsWith('mappings')){'txt'}else{'jar'}
        $target=Join-Path $artifactRoot "minecraft_${version}_${kind}.$extension"
        if(!(Test-Path -LiteralPath $target)){
            $temporary="$target.download-$([guid]::NewGuid().ToString('N'))"
            try{
                for($attempt=0;$attempt -lt 3;$attempt++){
                    try{Invoke-WebRequest -Uri $download.url -OutFile $temporary -TimeoutSec 180 -Resume;break}catch{if($attempt -eq 2){throw}}
                }
                if((Get-FileHash -LiteralPath $temporary -Algorithm SHA1).Hash.ToLowerInvariant() -ne $download.sha1){throw "Download checksum mismatch: $version/$kind"}
                Move-Item -LiteralPath $temporary -Destination $target -Force
            }finally{if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary}}
        }
        if((Get-FileHash -LiteralPath $target -Algorithm SHA1).Hash.ToLowerInvariant() -ne $download.sha1){throw "Cached checksum mismatch: $version/$kind"}
    }
    $loom=Join-Path $env:USERPROFILE ".gradle/caches/fabric-loom/$version"
    [IO.Directory]::CreateDirectory($loom) | Out-Null
    Copy-Item -LiteralPath $metadata -Destination (Join-Path $loom 'mojang_minecraft_info.json')
    Write-Output "Cached official Minecraft $version downloads and checked every SHA-1 against Mojang's manifests."
}
