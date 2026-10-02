param([string]$Minecraft='1.21.1',[string]$DependencyCache='',[switch]$Offline,[switch]$Game)
# Run alone, after all other Gradle/client processes have finished their tasks.
# The reviewed file is restored byte-for-byte even when a probe fails.
$ErrorActionPreference='Stop'
$workspace=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$kind=if($Game){'game'}else{'mappings'}
$path=Join-Path $workspace "gradle/generated-$kind-checksums.json"
$original=[IO.File]::ReadAllBytes($path)
$baseline=[Text.Encoding]::UTF8.GetString($original) | ConvertFrom-Json -AsHashtable
if(!$baseline.targets.ContainsKey($Minecraft)){throw "Missing mapping baseline for $Minecraft"}
$task=if($Game){'verifyGeneratedGame'}else{'verifyGeneratedMappings'}
$arguments=@('-p',(Join-Path $workspace 'legacy'),"-PmcVersion=$Minecraft",'-Ploader=fabric','--no-parallel',":minecraft-adapter:$task")
if($DependencyCache){$arguments+="-PdependencyCache=$DependencyCache"}
if($Offline){$arguments+='--offline'}
$cache=Join-Path $workspace 'tools/.cache'
[IO.Directory]::CreateDirectory($cache) | Out-Null
$records=@()
try {
    $noun=if($Game){'game'}else{'mapping'}
    foreach($probe in @(
        @{field=if($Game){'contentSha256'}else{'tinySha256'};value=('0'*64);message="Generated $noun content checksum mismatch"},
        @{field='clientSha1';value=('0'*40);message="Mojang client $noun input checksum mismatch"}
    )){
        $changed=[Text.Encoding]::UTF8.GetString($original) | ConvertFrom-Json -AsHashtable
        if($Game -and $probe.field -eq 'contentSha256'){$changed.targets[$Minecraft].artifacts[0].contentSha256=$probe.value}
        else{$changed.targets[$Minecraft][$probe.field]=$probe.value}
        [IO.File]::WriteAllText($path,($changed | ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
        $log=Join-Path $cache "$kind-verification-$($probe.field).log"
        & (Join-Path $workspace 'legacy/gradlew.bat') @arguments 2>&1 | Tee-Object -FilePath $log
        $exitCode=$LASTEXITCODE
        if($exitCode -eq 0 -or !((Get-Content -LiteralPath $log -Raw).Contains($probe.message))){throw "Verification did not reject $($probe.field) as expected"}
        $records+=@{probe=$probe.field;expectedFailure=$true;exitCode=$exitCode;message=$probe.message;logSha256=(Get-FileHash -LiteralPath $log -Algorithm SHA256).Hash.ToLowerInvariant()}
    }
} finally {
    [IO.File]::WriteAllBytes($path,$original)
}
$healthyLog=Join-Path $cache "$kind-verification-restored.log"
& (Join-Path $workspace 'legacy/gradlew.bat') @arguments 2>&1 | Tee-Object -FilePath $healthyLog
if($LASTEXITCODE -ne 0){throw 'Restored reviewed mapping baseline failed verification'}
$records+=@{probe='restored';passed=$true;exitCode=0;logSha256=(Get-FileHash -LiteralPath $healthyLog -Algorithm SHA256).Hash.ToLowerInvariant()}
$report=@{minecraft=$Minecraft;testedAt=[DateTime]::UtcNow.ToString('o');command="legacy/gradlew.bat $($arguments -join ' ')";baselineSha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant();checks=$records}
$destination=Join-Path $workspace 'docs/validation'
[IO.Directory]::CreateDirectory($destination) | Out-Null
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $destination "generated-$kind.json") -Encoding utf8
