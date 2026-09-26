<#
.SYNOPSIS
    ChangeSafe deterministic test gate for Java/Maven projects.

.DESCRIPTION
    Runs explicitly selected Maven tests with the H2 test profile, saves a non-overwriting
    timestamped log, writes a JSON summary using the Output Contract schema, and prints a
    short PASS/FAIL/UNKNOWN summary.

    Exit codes:
        0  PASS   -- all selected tests ran and passed
        1  FAIL   -- one or more tests failed or had errors
        2  UNKNOWN -- tests could not be verified (compile error, no matching tests, parse failure)

    Limitations:
        - Never sends output to an external service.
        - Never treats a skipped test as PASS.
        - If no Surefire XML is found, counts are null and result is UNKNOWN.
        - Does not merge counts from separate Maven invocations.

.PARAMETER RunId
    Kebab-case identifier for this run (e.g. checkout-retry-01).
    Evidence is written to changesafe/evidence/<RunId>/.

.PARAMETER Tests
    Comma-separated list of test class names or class#method patterns passed to -Dtest=.
    Example: "OrderWorkFlowTest" or "PlaceOrderTest#order_placed_raises_an_event"

.PARAMETER Phase
    "before-fix" or "after-fix". Determines the JSON summary filename.

.PARAMETER ProjectRoot
    Optional. Path to the repository root containing mvnw.cmd. Defaults to the directory
    two levels above this script (i.e., the repo root when the script is at changesafe/scripts/).

.EXAMPLE
    .\run-targeted-tests.ps1 -RunId checkout-retry-01 -Tests "OrderWorkFlowTest" -Phase before-fix
    .\run-targeted-tests.ps1 -RunId checkout-retry-01 -Tests "PlaceOrderTest,PaymentTest" -Phase after-fix
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$RunId,

    [Parameter(Mandatory = $true)]
    [string]$Tests,

    [Parameter(Mandatory = $true)]
    [ValidateSet("before-fix", "after-fix")]
    [string]$Phase,

    [Parameter(Mandatory = $false)]
    [string]$ProjectRoot = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# -- Resolve paths -------------------------------------------------------------

if ($ProjectRoot -eq "") {
    # Script lives at changesafe/scripts/; repo root is two levels up.
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "../../")).Path
}
$ProjectRoot = $ProjectRoot.TrimEnd('\', '/')

$EvidenceDir  = Join-Path $ProjectRoot "changesafe/evidence/$RunId"
$LogDir       = Join-Path $EvidenceDir "logs"
$Timestamp    = Get-Date -Format "yyyyMMdd-HHmmss"
$LogFile      = Join-Path $LogDir "${Phase}-${Timestamp}.log"
$SummaryFile  = Join-Path $EvidenceDir "${Phase}-test-summary.json"
$MvnCmd       = Join-Path $ProjectRoot "mvnw.cmd"
$SurefireDir  = Join-Path $ProjectRoot "target/surefire-reports"

# -- Validate prerequisites ----------------------------------------------------

if (-not (Test-Path $MvnCmd)) {
    Write-Error "mvnw.cmd not found at: $MvnCmd"
    exit 2
}

# Do not overwrite an existing summary for this phase in this run.
if (Test-Path $SummaryFile) {
    Write-Warning "Summary already exists: $SummaryFile"
    Write-Warning "Create a new run directory for a new before/after pair, or use a unique run ID."
    exit 2
}

New-Item -ItemType Directory -Path $LogDir -Force | Out-Null

# -- Capture Java version ------------------------------------------------------

$JavaVersion = "UNKNOWN"
try {
    $jv = & java -version 2>&1 | Select-String 'version' | Select-Object -First 1
    if ($jv) { $JavaVersion = $jv.Line.Trim() }
} catch { }

# -- Build and run Maven command -----------------------------------------------

$MavenArgs = @(
    "test",
    "-Dspring.profiles.active=test",
    "-Dtest=$Tests",
    "--no-transfer-progress"
)
$CommandString = ".\mvnw.cmd $($MavenArgs -join ' ')"

Write-Host ""
Write-Host "ChangeSafe Test Gate"
Write-Host "===================="
Write-Host "Run ID   : $RunId"
Write-Host "Phase    : $Phase"
Write-Host "Tests    : $Tests"
Write-Host "Command  : $CommandString"
Write-Host "Log      : $LogFile"
Write-Host ""

$Stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

# Capture all output (stdout + stderr) to the log file; also stream to console.
$ExitCode = 0
try {
    & cmd /c "`"$MvnCmd`" $($MavenArgs -join ' ') 2>&1" | Tee-Object -FilePath $LogFile
    $ExitCode = $LASTEXITCODE
} catch {
    "SCRIPT ERROR: $_" | Add-Content -Path $LogFile
    $ExitCode = 2
}

$Stopwatch.Stop()
$DurationSeconds = [math]::Round($Stopwatch.Elapsed.TotalSeconds, 2)

# -- Parse Surefire XML for test counts ---------------------------------------

$TestsRun = $null
$Failures = $null
$Errors   = $null
$Skipped  = $null

if (Test-Path $SurefireDir) {
    $XmlFiles = Get-ChildItem -Path $SurefireDir -Filter "*.xml" -ErrorAction SilentlyContinue
    if ($XmlFiles.Count -gt 0) {
        $TestsRun = 0; $Failures = 0; $Errors = 0; $Skipped = 0
        foreach ($XmlFile in $XmlFiles) {
            try {
                [xml]$Xml = Get-Content $XmlFile.FullName -Raw
                $Suite = $Xml.testsuite
                if ($null -ne $Suite) {
                    $TestsRun += [int]($Suite.tests   -as [int])
                    $Failures += [int]($Suite.failures -as [int])
                    $Errors   += [int]($Suite.errors   -as [int])
                    $Skipped  += [int]($Suite.skipped  -as [int])
                }
            } catch {
                # Parse failure on one file: keep nulls, flag UNKNOWN.
                $TestsRun = $null; $Failures = $null; $Errors = $null; $Skipped = $null
                break
            }
        }
    }
    # No XML files found means no tests matched or compile failure -- counts stay null.
}

# -- Determine result ----------------------------------------------------------

$Result = "UNKNOWN"
if ($ExitCode -eq 0) {
    if ($null -eq $TestsRun -or $TestsRun -eq 0) {
        # Process exited 0 but no tests ran -- cannot claim PASS.
        $Result = "UNKNOWN"
    } elseif ($Failures -eq 0 -and $Errors -eq 0) {
        $Result = "PASS"
    } else {
        $Result = "FAIL"
    }
} elseif ($ExitCode -eq 1) {
    $Result = "FAIL"
} else {
    $Result = "UNKNOWN"
}

# -- Get source state ---------------------------------------------------------

$SourceState = "UNKNOWN"
try {
    $GitHash = & git -C $ProjectRoot rev-parse HEAD 2>$null
    $GitStatus = & git -C $ProjectRoot status --short 2>$null
    if ($LASTEXITCODE -eq 0 -and $GitHash) {
        $SourceState = $GitHash.Trim()
        if ($GitStatus) { $SourceState += " (working tree has uncommitted changes)" }
    }
} catch { }

# -- Write JSON summary (Output Contract ?4) -----------------------------------

$CapturedAt = (Get-Date -Format "o")  # ISO 8601 with offset
$RelativeLogPath = "logs/${Phase}-${Timestamp}.log"

$Summary = [ordered]@{
    schemaVersion   = 1
    runId           = $RunId
    phase           = $Phase
    capturedAt      = $CapturedAt
    sourceState     = $SourceState
    command         = $CommandString
    javaVersion     = $JavaVersion
    testProfile     = "test"
    result          = $Result
    exitCode        = $ExitCode
    testsRun        = $TestsRun
    failures        = $Failures
    errors          = $Errors
    skipped         = $Skipped
    durationSeconds = $DurationSeconds
    logPath         = $RelativeLogPath
}

$JsonOutput = $Summary | ConvertTo-Json -Depth 3
Set-Content -Path $SummaryFile -Value $JsonOutput -Encoding UTF8

# -- Print short summary -------------------------------------------------------

$PassFail = switch ($Result) {
    "PASS"    { "PASS" }
    "FAIL"    { "FAIL" }
    default   { "UNKNOWN" }
}

Write-Host ""
Write-Host "-----------------------------------------"
Write-Host "ChangeSafe Test Gate Result"
Write-Host "-----------------------------------------"
Write-Host "Result     : $PassFail"
Write-Host "Exit code  : $ExitCode"
Write-Host "Tests run  : $(if ($null -eq $TestsRun) { 'unknown' } else { $TestsRun })"
Write-Host "Failures   : $(if ($null -eq $Failures) { 'unknown' } else { $Failures })"
Write-Host "Errors     : $(if ($null -eq $Errors)   { 'unknown' } else { $Errors })"
Write-Host "Skipped    : $(if ($null -eq $Skipped)  { 'unknown' } else { $Skipped })"
Write-Host "Duration   : ${DurationSeconds}s"
Write-Host "Log        : $LogFile"
Write-Host "Summary    : $SummaryFile"
Write-Host "-----------------------------------------"
Write-Host ""

# -- Exit with meaningful code -------------------------------------------------

switch ($Result) {
    "PASS"    { exit 0 }
    "FAIL"    { exit 1 }
    default   { exit 2 }
}
