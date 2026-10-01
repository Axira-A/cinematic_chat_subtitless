param([Parameter(Mandatory=$true)][string]$Minecraft)
$ErrorActionPreference='Stop'
if($Minecraft -notmatch '^[0-9]+\.[0-9]+(?:\.[0-9]+)?$'){throw 'Invalid Minecraft version'}
$metadata=Join-Path $env:USERPROFILE ".gradle/caches/fabric-loom/$Minecraft/mojang_minecraft_info.json"
$manifest=Get-Content -LiteralPath $metadata -Raw | ConvertFrom-Json
$script=Join-Path $PSScriptRoot 'cache-maven-coordinate.ps1'
$cache=Join-Path $PSScriptRoot '.cache/maven'
$manifest.libraries | ForEach-Object -Parallel {
    $parts=$_.name.Split(':');$coordinate=$parts[0..2] -join ':'
    $repository=if($parts[0] -like 'com.mojang*'){'mojang'}else{'central'}
    $classifier=if($parts.Count -eq 4){$parts[3]}else{''}
    if($classifier -like '*-patch'){$repository='mojang'}
    & $using:script -Coordinate $coordinate -Classifier $classifier -Repository $repository
    # Maven needs the parent descriptors even when the binary is already cached by NeoForm.
    $group,$artifact,$version=$parts[0..2]
    for($depth=0;$depth -lt 8;$depth++){
        $path=Join-Path $using:cache ($group.Replace('.','/')+"/$artifact/$version/$artifact-$version.pom")
        [xml]$pom=Get-Content -LiteralPath $path -Raw
        if(!$pom.project.parent){break}
        $group=[string]$pom.project.parent.groupId;$artifact=[string]$pom.project.parent.artifactId;$version=[string]$pom.project.parent.version
        if($version -notmatch '^[0-9A-Za-z+_.-]+$'){break}
        & $using:script -Coordinate "$group`:$artifact`:$version" -Repository central
    }
} -ThrottleLimit 1
Write-Output "Cached the libraries selected by Mojang's $Minecraft version manifest."
