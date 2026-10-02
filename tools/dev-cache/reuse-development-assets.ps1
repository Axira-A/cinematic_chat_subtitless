param([Parameter(Mandatory=$true)][string]$Minecraft,[Parameter(Mandatory=$true)][int]$Index,[string]$NeoFormCache='')
. (Join-Path $PSScriptRoot 'paths.ps1')
if(!$NeoFormCache){$NeoFormCache=Join-Path (Get-CinematicGradleHome) 'caches/neoformruntime'}
$ErrorActionPreference='Stop'
if($Minecraft -notmatch '^[0-9]+\.[0-9]+(\.[0-9]+)?$' -or $Index -lt 0){throw 'Invalid Minecraft asset index'}
$gradleCache=Join-Path (Get-CinematicGradleHome) 'caches'
$sourceRoot=Join-Path $NeoFormCache 'assets'
$destinationRoot=Join-Path $gradleCache 'fabric-loom/assets'
$sourceIndex=Join-Path $sourceRoot "indexes/$Index.json"
if(!(Test-Path -LiteralPath $sourceIndex)){throw "NeoForm index $Index has not been downloaded"}
[IO.Directory]::CreateDirectory((Join-Path $destinationRoot 'indexes')) | Out-Null
Copy-Item -LiteralPath $sourceIndex -Destination (Join-Path $destinationRoot "indexes/$Minecraft-$Index.json")
$spec=Get-Content -LiteralPath $sourceIndex -Raw | ConvertFrom-Json -AsHashtable
$count=0
foreach($item in $spec.objects.Values){
    $hash=$item.hash;if($hash -notmatch '^[a-f0-9]{40}$'){throw 'Invalid content hash in official index'}
    $relative="objects/$($hash.Substring(0,2))/$hash"
    $destination=Join-Path $destinationRoot $relative
    if(Test-Path -LiteralPath $destination){continue}
    $source=Join-Path $sourceRoot $relative
    if(!(Test-Path -LiteralPath $source)){continue}
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
    Copy-Item -LiteralPath $source -Destination $destination
    $count++
}
Write-Output "Reused $count official content-addressed assets for Minecraft $Minecraft. Loom verifies its index on launch."
