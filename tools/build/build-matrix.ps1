param([switch]$Smoke,[string[]]$Minecraft=@(),[ValidateSet('fabric','neoforge')][string[]]$Loader=@('neoforge','fabric'),[string]$DependencyCache='',[string]$NeoFormCache='',[switch]$Offline,[switch]$UpdateDependencyBaseline,[switch]$PreserveResults)
$ErrorActionPreference='Stop'
$workspace=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$baseline=Get-Content (Join-Path $workspace 'gradle/targets.json') -Raw | ConvertFrom-Json
$distribution=Join-Path $workspace 'dist'
[IO.Directory]::CreateDirectory($distribution) | Out-Null
$results=@()
if($PreserveResults -and (Test-Path -LiteralPath (Join-Path $distribution 'matrix-report.json'))){$results=@(Get-Content -LiteralPath (Join-Path $distribution 'matrix-report.json') -Raw | ConvertFrom-Json -AsHashtable)}
foreach($target in $baseline.targets){
    if($Minecraft.Count -and $target.minecraft -notin $Minecraft){continue}
    if($target.loader -notin $Loader){continue}
    $started=[DateTime]::UtcNow.ToString('o')
    $entry=if($target.java -eq 25){'modern'}else{'legacy'}
    $arguments=@('-p',(Join-Path $workspace $entry),"-PmcVersion=$($target.minecraft)","-Ploader=$($target.loader)",'--no-parallel',":$($target.loader):build",'check')
    if($DependencyCache){$arguments+="-PdependencyCache=$DependencyCache"}
    if($NeoFormCache){$arguments+="-PneoFormCache=$NeoFormCache"}
    if($Offline){$arguments+='--offline'}
    if($UpdateDependencyBaseline){$arguments+=@('--write-locks','--write-verification-metadata','sha256')}
    $name="mc$($target.minecraft)-$($target.loader)"
    $log=Join-Path $distribution "$name-build.log"
    & (Join-Path $workspace "$entry/gradlew.bat") @arguments 2>&1 | Tee-Object -FilePath $log
    $built=$LASTEXITCODE -eq 0
    $row=[ordered]@{minecraft=$target.minecraft;loader=$target.loader;build=if($built){'Pass'}else{'Fail'};launch='Not run';gameplay='Not run';supported=$false;startedAt=$started;buildCommand="$entry/gradlew.bat $($arguments -join ' ')";signedChat='Pending: authenticated client and server required'}
    if($built){
        $jar=Join-Path $workspace "targets/$($target.minecraft)/$($target.loader)/build/libs/$($target.artifact)"
        if(!(Test-Path -LiteralPath $jar)){throw "Successful build did not produce $jar"}
        Copy-Item -LiteralPath $jar -Destination $distribution
        $row.sha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
        if($Smoke){
            $runArgs=$arguments | Where-Object {$_ -notmatch '^:[^:]+:build$' -and $_ -ne 'check'}
            $runArgs+=":$($target.loader):runSmoke"
            $row.clientCommand="$entry/gradlew.bat $($runArgs -join ' ')"
            & (Join-Path $workspace "$entry/gradlew.bat") @runArgs 2>&1 | Tee-Object -FilePath (Join-Path $distribution "$name-client.log")
            $passed=$LASTEXITCODE -eq 0
            $report=Join-Path $workspace "targets/$($target.minecraft)/$($target.loader)/run-smoke/smoke-report.json"
            if(Test-Path -LiteralPath $report){
                $scenario=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
                $passed=$passed -and $scenario.passed -eq $true
                $row.assertions=@($scenario.PSObject.Properties | Where-Object {$_.Value -is [bool] -and $_.Name -ne 'passed'}).Count
                Copy-Item -LiteralPath $report -Destination (Join-Path $distribution "$name-scenario.json")
            }else{$passed=$false}
            $row.launch=if($passed){'Pass'}else{'Fail or incomplete'};$row.gameplay=if($passed){'Pass (automated scenario)'}else{'Fail or incomplete'}
        }
    }
    $row.finishedAt=[DateTime]::UtcNow.ToString('o')
    $results=@($results | Where-Object {$_.minecraft -ne $target.minecraft -or $_.loader -ne $target.loader})+@($row)
    ConvertTo-Json -InputObject @($results) -Depth 8 | Set-Content -LiteralPath (Join-Path $distribution 'matrix-report.json') -Encoding utf8
}
if($results | Where-Object {$_.build -eq 'Fail' -or $_.launch -eq 'Fail or incomplete'}){exit 1}
