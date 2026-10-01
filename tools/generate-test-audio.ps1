# Original synthesized test tone, MIT, Cinematic Chat contributors. No recordings or external samples.
$ErrorActionPreference = 'Stop'
$sampleRate = 22050
$sampleCount = 2205
$destination = Join-Path $PSScriptRoot '../targets/1.21.1/minecraft/src/main/resources/assets/cinematic_chat/audio/test-blip.wav'
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($destination))) | Out-Null
$memory = [IO.MemoryStream]::new()
$writer = [IO.BinaryWriter]::new($memory)
$writer.Write([Text.Encoding]::ASCII.GetBytes('RIFF'))
$writer.Write([int](36 + $sampleCount * 2))
$writer.Write([Text.Encoding]::ASCII.GetBytes('WAVEfmt '))
$writer.Write([int]16)
$writer.Write([int16]1)
$writer.Write([int16]1)
$writer.Write([int]$sampleRate)
$writer.Write([int]($sampleRate * 2))
$writer.Write([int16]2)
$writer.Write([int16]16)
$writer.Write([Text.Encoding]::ASCII.GetBytes('data'))
$writer.Write([int]($sampleCount * 2))
for ($index = 0; $index -lt $sampleCount; $index++) {
    $time = $index / $sampleRate
    $envelope = [Math]::Sin([Math]::PI * $index / $sampleCount) * [Math]::Exp(-22 * $time)
    $sample = 6000 * $envelope * ([Math]::Sin(2 * [Math]::PI * 440 * $time) + 0.25 * [Math]::Sin(2 * [Math]::PI * 880 * $time))
    $writer.Write([int16]$sample)
}
$writer.Flush()
[IO.File]::WriteAllBytes([IO.Path]::GetFullPath($destination), $memory.ToArray())
$writer.Dispose()
$memory.Dispose()
