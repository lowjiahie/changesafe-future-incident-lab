<#
.SYNOPSIS
    ChangeSafe output-contract structural validator.

.DESCRIPTION
    Validates a ChangeSafe run directory (or template directory) for structural compliance
    with the Output Contract. Checks headings, status vocabulary, AC-ID consistency, relative
    link resolution, JSON key types, and evidence file presence.

    Two modes:
        Default (completed-run):  unresolved placeholders (<...>) are rejected.
                                  Explicit NOT VERIFIED / NOT RUN values ARE allowed.
        --Template mode:          placeholder text is permitted; checks structure only.

    What this validator CANNOT certify:
        - Business correctness or whether the described outcome is accurate.
        - Human approval authenticity.
        - Screenshot content or whether PNG files show real Bob sessions.
        - Whether test results match actual executions.

    Exit codes:
        0  All checks passed.
        1  One or more checks failed.

.PARAMETER RunDir
    Path to the run directory (e.g. changesafe/evidence/checkout-retry-01) or
    the templates directory (changesafe/templates) when used with --Template.

.PARAMETER Template
    Switch. When present, runs in template-validation mode (placeholders allowed).

.EXAMPLE
    .\validate-output.ps1 -RunDir "changesafe/evidence/checkout-retry-01"
    .\validate-output.ps1 -RunDir "changesafe/templates" -Template
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$RunDir,

    [Parameter(Mandatory = $false)]
    [switch]$Template
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# -- Configuration -------------------------------------------------------------

# Required Markdown files and their mandatory headings (in order).
$RequiredMarkdownFiles = @{
    "risk-report.md" = @(
        "## 1. Goal and scope",
        "## 2. Cheap test gate",
        "## 3. Change-impact map",
        "## 4. Future incidents",
        "## 5. Selected safety contract",
        "## 6. Implementation and verification",
        "## 7. Goal check and remaining risk",
        "## 8. Evidence provenance"
    )
    "comparison.md" = @(
        "## Before / after results",
        "## Goal check",
        "## What the evidence proves",
        "## Remaining risk",
        "## Presentation takeaway"
    )
    "change-brief.md" = @(
        "## 1. Problem and goal",
        "## 2. Scope and exclusions",
        "## 3. Business rules and open questions",
        "## 4. Acceptance criteria",
        "## 5. Implementation and verification plan",
        "## 6. Decisions and approval"
    )
}

# Status vocabulary for completed-run validation.
$AllowedTestStatuses      = @("PASS", "FAIL", "UNKNOWN", "NOT RUN")
$AllowedCriterionStatuses = @("MET", "NOT MET", "NOT VERIFIED")
$AllowedIncidentStatuses  = @("HYPOTHESIS", "EVIDENCE-BACKED", "REPRODUCED", "PREVENTED", "CONTAINED", "UNRESOLVED")
$AllowedGoalStatuses      = @("MET", "NOT MET", "NOT VERIFIED")
$AllowedReportStatuses    = @("COMPLETE", "NEEDS REVIEW", "INCOMPLETE")

# Required JSON keys and their expected types.
$RequiredJsonKeys = @{
    "schemaVersion"   = "number"
    "runId"           = "string"
    "phase"           = "string"
    "capturedAt"      = "string"
    "sourceState"     = "string"
    "command"         = "string"
    "javaVersion"     = "string"
    "testProfile"     = "string"
    "result"          = "string"
    "exitCode"        = "number"
    "testsRun"        = "nullable"
    "failures"        = "nullable"
    "errors"          = "nullable"
    "skipped"         = "nullable"
    "durationSeconds" = "nullable"
    "logPath"         = "string"
}

# -- Helper: result tracking ----------------------------------------------------

$CheckResults = [System.Collections.Generic.List[hashtable]]::new()
$PassCount    = 0
$FailCount    = 0

function Add-CheckResult {
    param([string]$Check, [bool]$Passed, [string]$Detail = "")
    $script:CheckResults.Add(@{ Check = $Check; Passed = $Passed; Detail = $Detail })
    if ($Passed) { $script:PassCount++ } else { $script:FailCount++ }
}

function Test-PlaceholderFree {
    param([string]$Content)
    return $Content -notmatch '<[^>]+>'
}

# -- Resolve run directory ------------------------------------------------------

if (-not (Test-Path $RunDir)) {
    Write-Error "Run directory not found: $RunDir"
    exit 1
}
$RunDir = (Resolve-Path $RunDir).Path

# Detect project root (two levels up from changesafe/scripts/).
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "../../")).Path.TrimEnd('\', '/')

# -- Check 1: Required Markdown files present ----------------------------------

foreach ($FileName in $RequiredMarkdownFiles.Keys) {
    $FilePath = Join-Path $RunDir $FileName
    $Exists   = Test-Path $FilePath
    Add-CheckResult `
        -Check "File present: $FileName" `
        -Passed $Exists `
        -Detail $(if (-not $Exists) { "Missing: $FilePath" } else { "" })
}

# -- Check 2: Required headings present and in order ---------------------------

foreach ($Entry in $RequiredMarkdownFiles.GetEnumerator()) {
    $FileName  = $Entry.Key
    $Headings  = $Entry.Value
    $FilePath  = Join-Path $RunDir $FileName

    if (-not (Test-Path $FilePath)) { continue }

    $Content   = Get-Content $FilePath -Raw
    $LastIndex = -1
    $AllFound  = $true
    $Missing   = @()

    foreach ($Heading in $Headings) {
        $Idx = $Content.IndexOf($Heading)
        if ($Idx -lt 0) {
            $AllFound = $false
            $Missing += $Heading
        } elseif ($Idx -le $LastIndex) {
            $AllFound = $false
            $Missing += "$Heading (out of order)"
        } else {
            $LastIndex = $Idx
        }
    }

    Add-CheckResult `
        -Check "Headings correct: $FileName" `
        -Passed $AllFound `
        -Detail $(if (-not $AllFound) { "Missing or out of order: $($Missing -join ', ')" } else { "" })
}

# -- Check 3: No unresolved placeholders (completed-run mode only) -------------

if (-not $Template) {
    foreach ($FileName in $RequiredMarkdownFiles.Keys) {
        $FilePath = Join-Path $RunDir $FileName
        if (-not (Test-Path $FilePath)) { continue }

        $Content     = Get-Content $FilePath -Raw
        $HasPlaceholders = $Content -match '<[a-zA-Z][^>]{0,80}>'
        Add-CheckResult `
            -Check "No unresolved placeholders: $FileName" `
            -Passed (-not $HasPlaceholders) `
            -Detail $(if ($HasPlaceholders) { "Found <...> placeholder text; replace or mark NOT VERIFIED" } else { "" })
    }
}

# -- Check 4: Status vocabulary ------------------------------------------------

if (-not $Template) {
    # Check report status field in risk-report.md
    $RiskFile = Join-Path $RunDir "risk-report.md"
    if (Test-Path $RiskFile) {
        $RiskContent = Get-Content $RiskFile -Raw
        $ReportStatusMatch = [regex]::Match($RiskContent, '\|\s*Report status\s*\|\s*([^|]+)\|')
        if ($ReportStatusMatch.Success) {
            $ReportStatus = $ReportStatusMatch.Groups[1].Value.Trim()
            $Valid = $AllowedReportStatuses -contains $ReportStatus
            Add-CheckResult `
                -Check "Report status vocabulary" `
                -Passed $Valid `
                -Detail $(if (-not $Valid) { "Got '$ReportStatus'; expected one of: $($AllowedReportStatuses -join ', ')" } else { "" })
        }

        # Check Overall goal status
        $GoalMatch = [regex]::Match($RiskContent, '\*\*Overall goal:\*\*\s+(MET|NOT MET|NOT VERIFIED)')
        if ($GoalMatch.Success) {
            $GoalStatus = $GoalMatch.Groups[1].Value
            $Valid = $AllowedGoalStatuses -contains $GoalStatus
            Add-CheckResult `
                -Check "Overall goal status vocabulary (risk-report)" `
                -Passed $Valid `
                -Detail $(if (-not $Valid) { "Got '$GoalStatus'" } else { "" })
        }
    }

    # Check comparison.md overall goal
    $CompFile = Join-Path $RunDir "comparison.md"
    if (Test-Path $CompFile) {
        $CompContent = Get-Content $CompFile -Raw
        $CompGoalMatch = [regex]::Match($CompContent, '\*\*Overall goal:\*\*\s+(MET|NOT MET|NOT VERIFIED)')
        if ($CompGoalMatch.Success) {
            $CompGoalStatus = $CompGoalMatch.Groups[1].Value
            $Valid = $AllowedGoalStatuses -contains $CompGoalStatus
            Add-CheckResult `
                -Check "Overall goal status vocabulary (comparison)" `
                -Passed $Valid `
                -Detail $(if (-not $Valid) { "Got '$CompGoalStatus'" } else { "" })
        }
    }
}

# -- Check 5: AC-ID consistency across files ------------------------------------

if (-not $Template) {
    $BriefFile  = Join-Path $RunDir "change-brief.md"
    $RiskFile   = Join-Path $RunDir "risk-report.md"
    $CompFile   = Join-Path $RunDir "comparison.md"

    function Get-AcIds {
        param([string]$FilePath)
        if (-not (Test-Path $FilePath)) { return @() }
        $Content = Get-Content $FilePath -Raw
        [regex]::Matches($Content, '\|\s*(AC-\d+)\s*\|') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
    }

    $BriefAcIds = Get-AcIds $BriefFile
    $RiskAcIds  = Get-AcIds $RiskFile
    $CompAcIds  = Get-AcIds $CompFile

    if ($BriefAcIds.Count -gt 0 -and $RiskAcIds.Count -gt 0) {
        $BriefSet = [System.Collections.Generic.HashSet[string]]$BriefAcIds
        $RiskSet  = [System.Collections.Generic.HashSet[string]]$RiskAcIds
        $Match    = $BriefSet.SetEquals($RiskSet)
        Add-CheckResult `
            -Check "AC IDs match: change-brief vs risk-report" `
            -Passed $Match `
            -Detail $(if (-not $Match) { "Brief: $($BriefAcIds -join ',') | Risk: $($RiskAcIds -join ',')" } else { "" })
    }

    if ($RiskAcIds.Count -gt 0 -and $CompAcIds.Count -gt 0) {
        $RiskSet = [System.Collections.Generic.HashSet[string]]$RiskAcIds
        $CompSet = [System.Collections.Generic.HashSet[string]]$CompAcIds
        $Match   = $RiskSet.SetEquals($CompSet)
        Add-CheckResult `
            -Check "AC IDs match: risk-report vs comparison" `
            -Passed $Match `
            -Detail $(if (-not $Match) { "Risk: $($RiskAcIds -join ',') | Comparison: $($CompAcIds -join ',')" } else { "" })
    }
}

# -- Check 6: Relative Markdown links resolve ----------------------------------

foreach ($FileName in $RequiredMarkdownFiles.Keys) {
    $FilePath = Join-Path $RunDir $FileName
    if (-not (Test-Path $FilePath)) { continue }

    $Content     = Get-Content $FilePath -Raw
    $LinkMatches = [regex]::Matches($Content, '\[([^\]]+)\]\(([^)]+)\)')
    $BrokenLinks = @()

    foreach ($Match in $LinkMatches) {
        $Target = $Match.Groups[2].Value
        # Skip anchors, external URLs, and mailto links.
        if ($Target -match '^(#|https?://|mailto:)') { continue }
        # Resolve relative to the run directory.
        $Resolved = Join-Path $RunDir $Target
        if (-not (Test-Path $Resolved)) {
            $BrokenLinks += $Target
        }
    }

    $NoBroken = $BrokenLinks.Count -eq 0
    Add-CheckResult `
        -Check "Relative links resolve: $FileName" `
        -Passed $NoBroken `
        -Detail $(if (-not $NoBroken) { "Broken: $($BrokenLinks -join ', ')" } else { "" })
}

# -- Check 7: JSON summary files (when present) --------------------------------

$JsonPhases = @("before-fix", "after-fix")
foreach ($JsonPhase in $JsonPhases) {
    $JsonFile = Join-Path $RunDir "${JsonPhase}-test-summary.json"
    if (-not (Test-Path $JsonFile)) { continue }

    try {
        $JsonContent = Get-Content $JsonFile -Raw | ConvertFrom-Json -ErrorAction Stop
    } catch {
        Add-CheckResult -Check "JSON parseable: ${JsonPhase}-test-summary.json" -Passed $false -Detail "Parse error: $_"
        continue
    }

    Add-CheckResult -Check "JSON parseable: ${JsonPhase}-test-summary.json" -Passed $true

    # Check required keys and types.
    $JsonHash = @{}
    $JsonContent.PSObject.Properties | ForEach-Object { $JsonHash[$_.Name] = $_.Value }

    foreach ($Key in $RequiredJsonKeys.Keys) {
        $KeyPresent = $JsonHash.ContainsKey($Key)
        Add-CheckResult `
            -Check "JSON key present: $Key" `
            -Passed $KeyPresent `
            -Detail $(if (-not $KeyPresent) { "Missing key '$Key' in $JsonPhase summary" } else { "" })

        if ($KeyPresent) {
            $Val          = $JsonHash[$Key]
            $ExpectedType = $RequiredJsonKeys[$Key]
            $TypeOk       = $true
            $TypeDetail   = ""
            switch ($ExpectedType) {
                "string"   { if ($null -ne $Val -and $Val -isnot [string])  { $TypeOk = $false; $TypeDetail = "Expected string, got $($Val.GetType().Name)" } }
                "number"   { if ($null -ne $Val -and $Val -isnot [int] -and $Val -isnot [long] -and $Val -isnot [double]) { $TypeOk = $false; $TypeDetail = "Expected number, got $($Val.GetType().Name)" } }
                "nullable" { } # null or any numeric -- both valid per contract
            }
            if (-not $TypeOk) {
                Add-CheckResult -Check "JSON type: $Key" -Passed $false -Detail $TypeDetail
            }
        }
    }

    # Check allowed result values.
    if ($JsonHash.ContainsKey("result")) {
        $JsonResult = $JsonHash["result"]
        $ValidResult = @("PASS", "FAIL", "UNKNOWN") -contains $JsonResult
        Add-CheckResult `
            -Check "JSON result vocabulary: $JsonPhase" `
            -Passed $ValidResult `
            -Detail $(if (-not $ValidResult) { "Got '$JsonResult'; expected PASS, FAIL, or UNKNOWN" } else { "" })
    }

    # Check allowed phase values.
    if ($JsonHash.ContainsKey("phase")) {
        $JsonPhasVal = $JsonHash["phase"]
        $ValidPhase  = @("before-fix", "after-fix") -contains $JsonPhasVal
        Add-CheckResult `
            -Check "JSON phase vocabulary: $JsonPhase" `
            -Passed $ValidPhase `
            -Detail $(if (-not $ValidPhase) { "Got '$JsonPhasVal'; expected before-fix or after-fix" } else { "" })
    }
}

# -- Check 8: Before and after log/summary files are distinct -----------------

if (-not $Template) {
    $BeforeJson = Join-Path $RunDir "before-fix-test-summary.json"
    $AfterJson  = Join-Path $RunDir "after-fix-test-summary.json"

    if ((Test-Path $BeforeJson) -and (Test-Path $AfterJson)) {
        $BeforeHash = (Get-FileHash $BeforeJson -Algorithm SHA256).Hash
        $AfterHash  = (Get-FileHash $AfterJson  -Algorithm SHA256).Hash
        $Distinct   = $BeforeHash -ne $AfterHash
        Add-CheckResult `
            -Check "Before and after summaries are distinct" `
            -Passed $Distinct `
            -Detail $(if (-not $Distinct) { "Files have identical content -- before and after may have been overwritten" } else { "" })
    }

    # Check log files in logs/ subdirectory.
    $LogDir      = Join-Path $RunDir "logs"
    $BeforeLogs  = @()
    $AfterLogs   = @()
    if (Test-Path $LogDir) {
        $BeforeLogs = @(Get-ChildItem $LogDir -Filter "before-fix-*.log" -ErrorAction SilentlyContinue)
        $AfterLogs  = @(Get-ChildItem $LogDir -Filter "after-fix-*.log"  -ErrorAction SilentlyContinue)
    }

    if ($BeforeLogs.Count -gt 0 -and $AfterLogs.Count -gt 0) {
        # Take the most recent of each.
        $LatestBefore = ($BeforeLogs | Sort-Object Name | Select-Object -Last 1).FullName
        $LatestAfter  = ($AfterLogs  | Sort-Object Name | Select-Object -Last 1).FullName
        $LogHashB     = (Get-FileHash $LatestBefore -Algorithm SHA256).Hash
        $LogHashA     = (Get-FileHash $LatestAfter  -Algorithm SHA256).Hash
        $Distinct     = $LogHashB -ne $LogHashA
        Add-CheckResult `
            -Check "Before and after log files are distinct" `
            -Passed $Distinct `
            -Detail $(if (-not $Distinct) { "Log files have identical content" } else { "" })
    }
}

# -- Check 9: bob_sessions/ contains at least one PNG -------------------------

if (-not $Template) {
    $BobSessionsDir = Join-Path $ProjectRoot "bob_sessions"
    $PngFiles       = @()
    if (Test-Path $BobSessionsDir) {
        $PngFiles = @(Get-ChildItem $BobSessionsDir -Filter "*.png" -ErrorAction SilentlyContinue)
    }
    $HasPng = $PngFiles.Count -gt 0
    Add-CheckResult `
        -Check "bob_sessions/ has at least one consumption screenshot" `
        -Passed $HasPng `
        -Detail $(if (-not $HasPng) { "NOT VERIFIED -- no PNG files found in bob_sessions/; capture screenshots manually" } else { "$($PngFiles.Count) PNG file(s) found" })
}

# -- Print results -------------------------------------------------------------

$ModeLabel = if ($Template) { "TEMPLATE" } else { "COMPLETED-RUN" }

Write-Host ""
Write-Host "ChangeSafe Output Validator  [$ModeLabel]"
Write-Host "Run directory: $RunDir"
Write-Host "---------------------------------------------------------"
Write-Host ("{0,-6} {1,-55} {2}" -f "Status", "Check", "Detail")
Write-Host "---------------------------------------------------------"

foreach ($Item in $CheckResults) {
    $StatusLabel = if ($Item.Passed) { "PASS" } else { "FAIL" }
    $DetailText  = if ($Item.Detail) { $Item.Detail } else { "" }
    $Line = "{0,-6} {1,-55} {2}" -f $StatusLabel, $Item.Check, $DetailText
    Write-Host $Line
}

Write-Host "---------------------------------------------------------"
Write-Host "Total: $PassCount passed, $FailCount failed"

$OverallPassed = $FailCount -eq 0
Write-Host ""
if ($OverallPassed) {
    Write-Host "OVERALL: PASS -- structural validation complete."
    Write-Host "Note: this validator checks structure only. Human review is still required."
} else {
    Write-Host "OVERALL: FAIL -- $FailCount check(s) failed. Fix issues before marking report COMPLETE."
}
Write-Host ""

if ($OverallPassed) { exit 0 } else { exit 1 }
