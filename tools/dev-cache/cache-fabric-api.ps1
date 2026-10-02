param([string]$Version='0.116.17+1.21.1')
. (Join-Path $PSScriptRoot 'paths.ps1')
$ErrorActionPreference='Stop'
if($Version -notmatch '^[0-9A-Za-z+_.-]+$'){throw 'Invalid Fabric API version'}
$cacheRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../.cache/maven'))
$aggregate="net/fabricmc/fabric-api/fabric-api/$Version/fabric-api-$Version"
& (Join-Path $PSScriptRoot 'cache-official-dependency.ps1') -RelativePath "$aggregate.pom" -Repository fabric
[xml]$pom=Get-Content (Join-Path $cacheRoot "$aggregate.pom") -Raw
$artifacts=@([PSCustomObject]@{groupId='net.fabricmc.fabric-api';artifactId='fabric-api';version=$Version}) + @($pom.project.dependencies.dependency | ForEach-Object {[PSCustomObject]@{groupId=[string]$_.groupId;artifactId=[string]$_.artifactId;version=[string]$_.version}})
$deprecated="net/fabricmc/fabric-api/fabric-api-deprecated/$Version/fabric-api-deprecated-$Version.pom"
& (Join-Path $PSScriptRoot 'cache-official-dependency.ps1') -RelativePath $deprecated -Repository fabric
[xml]$deprecatedPom=Get-Content (Join-Path $cacheRoot $deprecated) -Raw
$artifacts+=@($deprecatedPom.project.dependencies.dependency | Where-Object {$_.artifactId -ne 'fabric-api'} | ForEach-Object {[PSCustomObject]@{groupId=[string]$_.groupId;artifactId=[string]$_.artifactId;version=[string]$_.version}})
$artifacts | ForEach-Object -Parallel {
    $root=$using:cacheRoot
    $relative=($_.groupId.Replace('.','/')+'/'+$_.artifactId+'/'+$_.version+'/'+$_.artifactId+'-'+$_.version)
    foreach($extension in @('pom','module','jar')){
        if($extension -eq 'jar'){[xml]$description=Get-Content (Join-Path $root "$relative.pom") -Raw;if($description.project.packaging -eq 'pom'){continue}}
        $target=[IO.Path]::GetFullPath((Join-Path $root "$relative.$extension"))
        if(!$target.StartsWith($root+[IO.Path]::DirectorySeparatorChar)){throw 'Dependency path escapes cache'}
        if(Test-Path -LiteralPath $target){continue}
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
        $temporary="$target.download-$([guid]::NewGuid().ToString('N'))"
        try{
            try { Invoke-WebRequest -Uri ("https://maven.fabricmc.net/$relative.$extension") -OutFile $temporary -TimeoutSec 60 }
            catch {
                if($extension -eq 'module' -and $_.Exception.Response.StatusCode -eq 404){continue}
                throw "Cannot download $relative.$extension : $_"
            }
            Move-Item -LiteralPath $temporary -Destination $target -Force
        }finally{if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary}}
    }
} -ThrottleLimit 6
Write-Output "Cached $($artifacts.Count) Fabric API artifacts from the official repository."
