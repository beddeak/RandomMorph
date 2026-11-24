param([switch]$Offline, [string]$JavaHome='C:\Program Files\Java\jdk-25.0.2')
$ErrorActionPreference='Stop'
if (!(Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) { throw 'Java 25 JDK required. Pass -JavaHome.' }
$morphPreviousJavaHome=$env:JAVA_HOME
$morphPreviousOptions=$env:JAVA_TOOL_OPTIONS
$morphTemp=Join-Path $PSScriptRoot 'work\tmp'
New-Item -ItemType Directory -Path $morphTemp -Force | Out-Null
try {
    $env:JAVA_HOME=$JavaHome
    $env:JAVA_TOOL_OPTIONS=($morphPreviousOptions+' -Djdk.net.unixdomain.tmpdir='+$morphTemp+' -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE').Trim()
    Push-Location $PSScriptRoot
    try {
        $morphArgs=@('--no-daemon','--console','plain')
        if ($Offline) { $morphArgs+='--offline' }
        $morphArgs+=@('clean','test','build')
        & (Join-Path $PSScriptRoot 'gradlew.bat') @morphArgs
        if ($LASTEXITCODE -ne 0) { throw ('Fabric build failed: '+$LASTEXITCODE) }
    } finally { Pop-Location }
} finally { $env:JAVA_HOME=$morphPreviousJavaHome; $env:JAVA_TOOL_OPTIONS=$morphPreviousOptions }
