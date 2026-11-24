param([switch]$Offline,[string]$JavaHome='C:\Program Files\Java\jdk-25.0.2')
$ErrorActionPreference='Stop'
& (Join-Path $PSScriptRoot 'fabric-mod\build.ps1') -Offline:$Offline -JavaHome $JavaHome
