param([switch]$Smoke,[switch]$List,[string[]]$Minecraft=@(),[string[]]$Loader=@('neoforge','fabric'))
$ErrorActionPreference='Stop'
if($PSBoundParameters.ContainsKey('Minecraft') -and !$Minecraft.Count){throw 'Minecraft selection must not be empty'}
if(!$Loader.Count){throw 'Loader selection must not be empty'}
$matrixArguments=@()
if($Smoke){$matrixArguments+='--smoke'}
if($List){$matrixArguments+='--list'}
if($Minecraft.Count){$matrixArguments+='--minecraft';foreach($selection in $Minecraft){$matrixArguments+=($selection -split ',')}}
$matrixArguments+='--loader';foreach($selection in $Loader){$matrixArguments+=($selection.ToLowerInvariant() -split ',')}
& python (Join-Path $PSScriptRoot 'build_matrix.py') @matrixArguments
exit $LASTEXITCODE
