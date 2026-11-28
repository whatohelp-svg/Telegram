#requires -Version 5.1
[CmdletBinding()]
param([switch]$Setup, [switch]$Once)
$ErrorActionPreference = 'Stop'
$architecture = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
if ($env:OS -ne 'Windows_NT' -or $architecture -ne 'AMD64') {
    throw 'This development profile requires Windows x64.'
}
$root = (Resolve-Path "$PSScriptRoot/../..").Path
Set-Location -LiteralPath $root
$state = Join-Path $env:LOCALAPPDATA 'RiseGramDev'
$avd = 'RiseGram_API_36'
$image = 'system-images;android-36;google_apis;x86_64'
$local = Join-Path $root 'local.properties'
function Property([string]$key) {
    if (Test-Path $local) {
        foreach ($line in [IO.File]::ReadAllLines($local)) {
            if ($line -match ('^\s*' + [regex]::Escape($key) + '\s*[=:]\s*(.*?)\s*$')) { return $Matches[1] }
        }
    }
}
function Check([string]$step) {
    if ($LASTEXITCODE -ne 0) { throw "$step failed (exit $LASTEXITCODE)." }
}
function Unzip([string]$url,[string]$hash,[string]$algorithm,[string]$child,[string]$target) {
    $tmp = Join-Path $env:TEMP ('risegram-' + [guid]::NewGuid())
    New-Item -ItemType Directory $tmp | Out-Null
    try {
        $archive = Join-Path $tmp 'download.zip'
        Invoke-WebRequest -UseBasicParsing $url -OutFile $archive
        if ((Get-FileHash $archive -Algorithm $algorithm).Hash -ne $hash) { throw "Checksum mismatch for $url" }
        Expand-Archive $archive "$tmp/extracted"
        $dirs = @(Get-ChildItem "$tmp/extracted" -Directory -Filter $child)
        if ($dirs.Count -ne 1) { throw 'Unexpected archive layout.' }
        New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
        Move-Item -LiteralPath $dirs[0].FullName -Destination $target
    } finally { Remove-Item -LiteralPath $tmp -Recurse -Force }
}
$jdk = Join-Path $state 'jdk17'
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/release") -and
    (Select-String -Quiet -Path "$env:JAVA_HOME/release" -Pattern '^JAVA_VERSION="17\.')) { $jdk = $env:JAVA_HOME }
$sdk = Property 'sdk.dir'
if ($sdk) { $sdk = $sdk.Replace('\:',':').Replace('\\','\') }
elseif ($env:ANDROID_HOME) { $sdk = $env:ANDROID_HOME }
elseif ($env:ANDROID_SDK_ROOT) { $sdk = $env:ANDROID_SDK_ROOT }
else { $sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$sdk\platform-tools;$sdk\emulator;$env:Path"
$manager = "$sdk/cmdline-tools/16.0/bin/sdkmanager.bat"
$creator = "$sdk/cmdline-tools/16.0/bin/avdmanager.bat"
$adb = "$sdk/platform-tools/adb.exe"
$emu = "$sdk/emulator/emulator.exe"
if ($Setup) {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    if (!(Test-Path "$jdk/bin/javac.exe")) {
        $api = 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
        $package = (Invoke-RestMethod $api)[0].binary.package
        Unzip $package.link $package.checksum SHA256 'jdk-*' $jdk
    }
    if (!(Test-Path $manager)) {
        Unzip 'https://dl.google.com/android/repository/commandlinetools-win-12266719_latest.zip' '32787c10f55911fd109848906b1275723e79f659' SHA1 'cmdline-tools' "$sdk/cmdline-tools/16.0"
    }
    & $manager "--sdk_root=$sdk" --licenses
    Check 'Android SDK licenses'
    & $manager "--sdk_root=$sdk" 'platform-tools' 'emulator' 'platforms;android-36' 'build-tools;36.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1' $image
    Check 'SDK packages'
    & git -c core.longpaths=true submodule update --init --recursive
    Check 'Git submodules'
    $names = @(& $emu -list-avds)
    Check 'List AVDs'
    if ($names -notcontains $avd) {
        'no' | & $creator create avd --name $avd --package $image --device pixel_6
        Check 'Create RiseGram AVD'
    }
}
if (!(Test-Path "$jdk/bin/javac.exe") -or !(Test-Path $emu) -or !(Test-Path $adb)) { throw 'Run this script with -Setup first.' }
foreach ($key in @('RISEGRAM_API_ID','RISEGRAM_API_HASH')) {
    $v = [Environment]::GetEnvironmentVariable($key)
    if (!$v) { $v = Property $key }
    if (!$v) {
        $secret = Read-Host "Enter $key for this session" -AsSecureString
        $v = [Net.NetworkCredential]::new('', $secret).Password
    }
    $valid = if ($key -eq 'RISEGRAM_API_ID') { $v -match '^[1-9][0-9]*$' -and [long]$v -le 2147483647 } else { $v -match '^[a-fA-F0-9]{32}$' }
    if (!$valid) { throw "Invalid $key." }
    [Environment]::SetEnvironmentVariable($key,$v)
}
& $adb start-server
Check 'ADB'
function Find-AVD {
    $devices = @(& $adb devices)
    foreach ($d in $devices) {
        if ($d -match '^(emulator-\d+)\s+device\s*$') {
            $id = $Matches[1]
            if (@(& $adb -s $id emu avd name) -contains $avd) { return $id }
        }
    }
}
$serial = Find-AVD
if (!$serial) {
    if (@(& $emu -list-avds) -notcontains $avd) { throw 'Missing RiseGram AVD. Run with -Setup.' }
    & $emu -accel-check
    Check 'Emulator acceleration (enable virtualization in UEFI and Windows Hypervisor Platform)'
    $process = Start-Process -FilePath $emu -ArgumentList @('-avd',$avd,'-no-boot-anim') -PassThru
}
$until = (Get-Date).AddMinutes(6)
do {
    if (!$serial) { $serial = Find-AVD }
    if ($serial -and ((& $adb -s $serial shell getprop sys.boot_completed) -join '') -eq '1') { break }
    if ($process -and $process.HasExited -and !$serial) { throw 'Emulator exited during startup.' }
    if ((Get-Date) -ge $until) { throw 'Timed out waiting for the RiseGram emulator to boot.' }
    Start-Sleep 2
} while ($true)
$env:RISEGRAM_DEV_SERIAL = $serial
$env:RISEGRAM_DEV_ADB = $adb
# Probe only the native DC configured in the checked-out client.
$source = Get-Content -Raw 'TMessagesProj/jni/tgnet/ConnectionsManager.cpp'
$dc = [regex]::Match($source,'(?:risegramHost|RISEGRAM_DC_HOST(?:\[\])?)\s*=\s*"([a-zA-Z0-9.-]+)"').Groups[1].Value
$port = [regex]::Match($source,'(?:risegramPort|RISEGRAM_DC_PORT)\s*=\s*(\d+)').Groups[1].Value
if ($dc -and $port) {
    & $adb -s $serial shell "toybox nc -w 5 -q 1 $dc $port < /dev/null"
    if ($LASTEXITCODE) { Write-Warning "TCP probe to ${dc}:$port failed. Check emulator DNS/network." }
    else { Write-Host "RiseGram DC reachable from $serial at ${dc}:$port" }
}
$arguments = @('-classpath', "$root/gradle/wrapper/gradle-wrapper.jar", 'org.gradle.wrapper.GradleWrapperMain',
    '--init-script', "$PSScriptRoot/emulator.gradle", '-Pandroid.injected.build.abi=x86_64', ':TMessagesProj_App:runRiseGramEmulator')
if (!$Once) { $arguments += '--continuous' }
& "$jdk/bin/java.exe" @arguments
Check 'Gradle development build'
