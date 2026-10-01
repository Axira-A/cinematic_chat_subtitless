param([Parameter(Mandatory=$true)][string]$Minecraft)
$ErrorActionPreference='Stop'
$workspace=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$target=(Get-Content (Join-Path $workspace 'gradle/targets.json') -Raw | ConvertFrom-Json).targets | Where-Object {$_.minecraft -eq $Minecraft -and $_.loader -eq 'neoforge'} | Select-Object -First 1
if(!$target){throw 'Unknown matrix target'}
$prefix=if($target.java -eq 25){'minecraft-patched'}else{'neoforge'}
$source=Join-Path $workspace "targets/$Minecraft/minecraft/build/moddev/artifacts/$prefix-$($target.neoForge)-sources.jar"
$destination=Join-Path $PSScriptRoot '.cache'
$archive=[IO.Compression.ZipFile]::OpenRead($source)
try {
    foreach($class in @('client/gui/components/ChatComponent','client/multiplayer/chat/ChatListener','client/multiplayer/chat/GuiMessageSource','client/multiplayer/chat/GuiMessage','client/multiplayer/chat/ChatAbilities','client/gui/screens/Screen','client/gui/screens/ChatScreen','client/gui/GuiGraphics','client/gui/GuiGraphicsExtractor','client/Minecraft','client/KeyMapping','client/sounds/SoundEngine','client/sounds/ChannelAccess','network/chat/Style','network/chat/FontDescription','world/level/LevelSettings','world/level/gamerules/GameRules','world/level/gamerules/GameRuleMap')) {
        $entry=$archive.GetEntry("net/minecraft/$class.java")
        if(!$entry){continue}
        $reader=[IO.StreamReader]::new($entry.Open())
        try { $reader.ReadToEnd() | Set-Content -LiteralPath (Join-Path $destination "$Minecraft-$($class.Split('/')[-1]).java") -Encoding utf8 } finally {$reader.Dispose()}
    }
} finally {$archive.Dispose()}
Write-Output "Extracted $Minecraft API sources into the ignored local cache for signature review."
