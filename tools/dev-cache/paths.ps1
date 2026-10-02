function Get-CinematicGradleHome {
    if ($env:GRADLE_USER_HOME) { return [IO.Path]::GetFullPath($env:GRADLE_USER_HOME) }
    return Join-Path $env:USERPROFILE '.gradle'
}
