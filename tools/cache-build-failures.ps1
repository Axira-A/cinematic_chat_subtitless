param([Parameter(Mandatory=$true)][string]$LogPath)
$ErrorActionPreference='Stop'
$matches=[regex]::Matches((Get-Content -LiteralPath $LogPath -Raw),'Could not (?:resolve |download [^\r\n()]+\()([A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[A-Za-z0-9+_.-]+)[.)]')
$coordinates=@($matches | ForEach-Object {$_.Groups[1].Value} | Sort-Object -Unique)
foreach($coordinate in $coordinates){
    $group=$coordinate.Split(':')[0]
    $repository=if($group -like 'gradle.plugin.*'){'gradle'}elseif($group -like 'net.minecraftforge*'){'forge'}elseif($group -like 'net.neoforged*' -or $group -like 'cpw.mods*'){'neoforge'}elseif($group -like 'net.fabricmc*'){'fabric'}elseif($group -like 'com.mojang*'){'mojang'}else{'central'}
    & (Join-Path $PSScriptRoot 'cache-maven-coordinate.ps1') -Coordinate $coordinate -Repository $repository -Parents
}
Write-Output "Cached $($coordinates.Count) coordinates explicitly reported by Gradle."
