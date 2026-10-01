param([Parameter(Mandatory=$true)][string]$Minecraft)
$ErrorActionPreference='Stop'
$workspace=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$baseline=Get-Content (Join-Path $workspace 'gradle/targets.json') -Raw | ConvertFrom-Json
$target=$baseline.targets | Where-Object {$_.minecraft -eq $Minecraft -and $_.loader -eq 'neoforge'} | Select-Object -First 1
if(!$target){throw 'Minecraft is not in the pinned target matrix'}
$version=$target.neoForge
$cache=Join-Path $PSScriptRoot '.cache/maven'
foreach($suffix in @('pom','module','userdev.jar','universal.jar','sources.jar','moddev-config.json')){
    $name=if($suffix.Contains('.')){"neoforge-$version-$suffix"}else{"neoforge-$version.$suffix"}
    & (Join-Path $PSScriptRoot 'cache-official-dependency.ps1') -RelativePath "net/neoforged/neoforge/$version/$name" -Repository neoforge
}
$module=Get-Content (Join-Path $cache "net/neoforged/neoforge/$version/neoforge-$version.module") -Raw | ConvertFrom-Json
$bundle=$module.variants | Where-Object {$_.name -eq 'modDevBundle'}
$neoForm=$bundle.dependencies | Where-Object {$_.module -eq 'neoform'} | Select-Object -First 1
$modules=@($module)
if($neoForm){
    $nf=$neoForm.version.requires
    foreach($extension in @('pom','module','zip')){& (Join-Path $PSScriptRoot 'cache-official-dependency.ps1') -RelativePath "net/neoforged/neoform/$nf/neoform-$nf.$extension" -Repository neoforge}
    $modules+=Get-Content (Join-Path $cache "net/neoforged/neoform/$nf/neoform-$nf.module") -Raw | ConvertFrom-Json
    $archive=[IO.Compression.ZipFile]::OpenRead((Join-Path $cache "net/neoforged/neoform/$nf/neoform-$nf.zip"))
    try{
        $reader=[IO.StreamReader]::new($archive.GetEntry('config.json').Open())
        try{$pipeline=$reader.ReadToEnd() | ConvertFrom-Json}finally{$reader.Dispose()}
        foreach($function in $pipeline.functions.PSObject.Properties.Value){
            $coordinates=if($function.version){@($function.version)}else{@($function.classpath)}
            foreach($coordinate in $coordinates){
                if(!$coordinate){continue}
                $parts=$coordinate.Split(':');if($parts.Count -lt 3 -or $parts[0] -notmatch '^net.neoforged'){continue}
                $classifier=if($parts.Count -eq 4){$parts[3]}else{''}
                & (Join-Path $PSScriptRoot 'cache-maven-coordinate.ps1') -Coordinate ($parts[0..2] -join ':') -Classifier $classifier -Repository neoforge
            }
        }
    }finally{$archive.Dispose()}
    & (Join-Path $PSScriptRoot 'cache-official-dependency.ps1') -RelativePath "net/neoforged/minecraft-dependencies/$Minecraft/minecraft-dependencies-$Minecraft.module" -Repository mojang-meta
}
$queue=[Collections.Generic.Queue[object]]::new()
foreach($description in $modules){foreach($variant in $description.variants){foreach($dependency in $variant.dependencies){
    if($dependency.module -in @('neoform','minecraft-dependencies')){continue}
    $v=$dependency.version.requires;if(!$v){$v=$dependency.version.strictly}
    if($dependency.group -match '^(net.neoforged|net.minecraftforge|cpw.mods|io.github.llamalad7|net.fabricmc)' -and $v -match '^[0-9A-Za-z+_.-]+$'){
        $queue.Enqueue(@{Group=$dependency.group;Artifact=$dependency.module;Version=$v;Classifier=[string]$dependency.thirdPartyCompatibility.artifactSelector.classifier})
    }
}}}
$seen=@{}
while($queue.Count){
    $item=$queue.Dequeue();$coordinate="$($item.Group):$($item.Artifact):$($item.Version)";$key="$coordinate/$($item.Classifier)"
    if($seen.ContainsKey($key)){continue};$seen[$key]=$true
    & (Join-Path $PSScriptRoot 'cache-maven-coordinate.ps1') -Coordinate $coordinate -Classifier $item.Classifier -Repository neoforge
    $relative=$item.Group.Replace('.','/')+"/$($item.Artifact)/$($item.Version)/$($item.Artifact)-$($item.Version).pom"
    [xml]$pom=Get-Content (Join-Path $cache $relative) -Raw
    foreach($dependency in $pom.project.dependencies.dependency){
        $v=[string]$dependency.version
        if($v -eq '${project.version}'){$v=$item.Version}
        if($dependency.groupId -match '^(net.neoforged|net.minecraftforge|cpw.mods|io.github.llamalad7|net.fabricmc)' -and $v -match '^[0-9A-Za-z+_.-]+$'){
            $queue.Enqueue(@{Group=[string]$dependency.groupId;Artifact=[string]$dependency.artifactId;Version=$v;Classifier=[string]$dependency.classifier})
        }
    }
}
Write-Output "Cached pinned NeoForge bundle and $($seen.Count) related official coordinates for $Minecraft."
