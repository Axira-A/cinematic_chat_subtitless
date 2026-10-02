param([Parameter(Mandatory=$true)][string]$Coordinate,[ValidateSet('fabric','neoforge','forge','mojang','central','gradle')][string]$Repository='central',[ValidateSet('jar','zip')][string]$ArtifactType='jar',[string]$Classifier='',[switch]$Parents)
. (Join-Path $PSScriptRoot 'paths.ps1')
$ErrorActionPreference='Stop'
$parts=$Coordinate.Split(':')
if($parts.Count -ne 3 -or ($parts | Where-Object {$_ -notmatch '^[0-9A-Za-z+_.-]+$'})){throw 'Use group:artifact:version'}
if($Classifier -notmatch '^[A-Za-z0-9_.-]*$'){throw 'Invalid classifier'}
$group,$artifact,$version=$parts
$cacheRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../.cache/maven'))
$relative=$group.Replace('.','')
$directory=Join-Path $cacheRoot ($group.Replace('.','/')+'/'+$artifact+'/'+$version)
if(![IO.Path]::GetFullPath($directory).StartsWith($cacheRoot+[IO.Path]::DirectorySeparatorChar)){throw 'Dependency path escapes cache'}
[IO.Directory]::CreateDirectory($directory) | Out-Null
$gradleCache=Join-Path (Get-CinematicGradleHome) "caches/modules-2/files-2.1/$group/$artifact/$version"
if(Test-Path -LiteralPath $gradleCache){Get-ChildItem -LiteralPath $gradleCache -Recurse -File | ForEach-Object {$destination=Join-Path $directory $_.Name;if(!(Test-Path -LiteralPath $destination)){Copy-Item -LiteralPath $_.FullName -Destination $destination}}}
$base=@{fabric='https://maven.fabricmc.net/';neoforge='https://maven.neoforged.net/releases/';forge='https://maven.minecraftforge.net/';mojang='https://libraries.minecraft.net/';central='https://repo.maven.apache.org/maven2/';gradle='https://plugins.gradle.org/m2/'}[$Repository]
foreach($extension in @('pom','module',$ArtifactType)){
    if($extension -in @('jar','zip')){[xml]$description=Get-Content (Join-Path $directory "$artifact-$version.pom") -Raw;if($description.project.packaging -eq 'pom'){continue}}
    $suffix=if($extension -in @('jar','zip') -and $Classifier){"-$Classifier"}else{''}
    $name="$artifact-$version$suffix.$extension";$target=Join-Path $directory $name
    if(!(Test-Path -LiteralPath $target)){
        $temporary="$target.download-$([guid]::NewGuid().ToString('N'))"
        try{
            $notPublished=$false
            for($attempt=0;$attempt -lt 3;$attempt++){
                try{Invoke-WebRequest -Uri ($base+$group.Replace('.','/')+"/$artifact/$version/$name") -OutFile $temporary -TimeoutSec 60;break}
                catch{
                    if($extension -eq 'module' -and $_.Exception.Response.StatusCode -eq 404){$notPublished=$true;break}
                    if($attempt -eq 2){throw}
                }
            }
            if(!$notPublished){Move-Item -LiteralPath $temporary -Destination $target -Force}
        }finally{if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary}}
    }
}
Write-Output "Cached $Coordinate"
if($Parents){
    for($depth=0;$depth -lt 8;$depth++){
        $path=Join-Path $cacheRoot ($group.Replace('.','/')+"/$artifact/$version/$artifact-$version.pom")
        [xml]$pom=Get-Content -LiteralPath $path -Raw
        if(!$pom.project.parent){break}
        $group=[string]$pom.project.parent.groupId;$artifact=[string]$pom.project.parent.artifactId;$version=[string]$pom.project.parent.version
        if($version -notmatch '^[0-9A-Za-z+_.-]+$'){throw 'Unresolved parent POM property'}
        $parentRepository=if($group -like 'net.neoforged*'){'neoforge'}elseif($group -like 'net.fabricmc*'){'fabric'}else{'central'}
        & $PSCommandPath -Coordinate "$group`:$artifact`:$version" -Repository $parentRepository
    }
}
