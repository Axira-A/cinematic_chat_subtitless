param([Parameter(Mandatory=$true)][string]$Minecraft)
$ErrorActionPreference='Stop'
if($Minecraft -notmatch '^[0-9]+\.[0-9]+(?:\.[0-9]+)?$'){throw 'Invalid Minecraft version'}
$gradleCache=Join-Path $env:USERPROFILE '.gradle/caches'
$loom=Join-Path $gradleCache "fabric-loom/$Minecraft"
$metadata=Join-Path $loom 'mojang_minecraft_info.json'
if(!(Test-Path -LiteralPath $metadata)){throw 'Run Loom configuration once to obtain the official version manifest'}
$manifest=Get-Content -LiteralPath $metadata -Raw | ConvertFrom-Json
foreach($kind in @('client','server')){
    $source=Join-Path $gradleCache "neoformruntime/artifacts/minecraft_${Minecraft}_${kind}.jar"
    $target=Join-Path $loom "minecraft-$kind.jar"
    if(!(Test-Path -LiteralPath $source)){throw "Missing NeoForm artifact $source"}
    if((Get-FileHash -LiteralPath $source -Algorithm SHA1).Hash.ToLowerInvariant() -ne $manifest.downloads.$kind.sha1){throw "Official manifest checksum mismatch: $source"}
    if(!(Test-Path -LiteralPath $target)){Copy-Item -LiteralPath $source -Destination $target}
    if((Get-FileHash -LiteralPath $target -Algorithm SHA1).Hash.ToLowerInvariant() -ne $manifest.downloads.$kind.sha1){throw "Loom artifact checksum mismatch: $target"}
}
Write-Output "Reused official client/server JARs for $Minecraft; both SHA-1 values match Mojang's version manifest."
if($manifest.downloads.client_mappings){
    $mappingRoot=Join-Path $loom 'layered/working_dir/layered_hash_2198/mojang'
    [IO.Directory]::CreateDirectory($mappingRoot) | Out-Null
    foreach($kind in @('client','server')){
        $description=$manifest.downloads."${kind}_mappings"
        $target=Join-Path $mappingRoot "$kind.txt"
        if(!(Test-Path -LiteralPath $target)){
            $source=Join-Path $gradleCache "neoformruntime/artifacts/minecraft_${Minecraft}_${kind}_mappings.txt"
            if(Test-Path -LiteralPath $source){Copy-Item -LiteralPath $source -Destination $target}
            else {Invoke-WebRequest -Uri $description.url -OutFile $target -TimeoutSec 60}
        }
        if((Get-FileHash -LiteralPath $target -Algorithm SHA1).Hash.ToLowerInvariant() -ne $description.sha1){throw "Official mapping checksum mismatch: $target"}
    }
    $intermediary=Join-Path $PSScriptRoot ".cache/maven/net/fabricmc/intermediary/$Minecraft/intermediary-$Minecraft-v2.jar"
    if(Test-Path -LiteralPath $intermediary){
        $archive=[IO.Compression.ZipFile]::OpenRead($intermediary)
        try{[IO.Compression.ZipFileExtensions]::ExtractToFile($archive.GetEntry('mappings/mappings.tiny'),(Join-Path $loom 'intermediary-v2.tiny'),$true)}finally{$archive.Dispose()}
    }
    Write-Output 'Reused Mojang official mappings and locally cached Fabric intermediary mappings.'
}
