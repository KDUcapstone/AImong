param(
    [string]$InputPath = "_generated/question-bank/question-bank-1056-starlevel-ultra-diverse-patched.json",
    [string]$ReportPath = "_generated/question-bank/question-bank-1056-starlevel-ultra-diverse-patched-similarity-report.md",
    [double]$Threshold = 0.85,
    [int]$TopPairLimit = 80
)

$ErrorActionPreference = "Stop"

function Read-Json($Path) {
    Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | ConvertFrom-Json
}

function Write-Utf8($Path, $Text) {
    $parent = Split-Path -Parent $Path
    if ($parent) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    $fullPath = Join-Path (Get-Location) $Path
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($fullPath, $Text, $encoding)
}

function Normalize-Text([string]$Text) {
    if ($null -eq $Text) {
        return ""
    }
    return ($Text.ToLowerInvariant() -replace "[^\p{L}\p{Nd}\s]", " " -replace "\s+", " ").Trim()
}

function Get-Tokens([string]$Text) {
    $normalized = Normalize-Text $Text
    if ([string]::IsNullOrWhiteSpace($normalized)) {
        return @()
    }
    return @($normalized -split "\s+" | Where-Object { $_ -ne "" } | Select-Object -Unique)
}

function Get-Ngrams([string]$Text, [int]$Size) {
    $normalized = (Normalize-Text $Text) -replace "\s+", ""
    if ($normalized.Length -eq 0) {
        return @()
    }
    if ($normalized.Length -lt $Size) {
        return @($normalized)
    }
    $grams = New-Object System.Collections.Generic.List[string]
    for ($i = 0; $i -le $normalized.Length - $Size; $i++) {
        $grams.Add($normalized.Substring($i, $Size))
    }
    return @($grams.ToArray() | Select-Object -Unique)
}

function Jaccard([string[]]$Left, [string[]]$Right) {
    if ($Left.Count -eq 0 -or $Right.Count -eq 0) {
        return 0.0
    }
    $leftSet = @{}
    foreach ($item in $Left) { $leftSet[$item] = $true }
    $rightSet = @{}
    foreach ($item in $Right) { $rightSet[$item] = $true }

    $intersection = 0
    foreach ($item in $leftSet.Keys) {
        if ($rightSet.ContainsKey($item)) {
            $intersection++
        }
    }
    $union = $leftSet.Count + $rightSet.Count - $intersection
    if ($union -eq 0) {
        return 0.0
    }
    return $intersection / [double]$union
}

function Dice([string[]]$Left, [string[]]$Right) {
    if ($Left.Count -eq 0 -or $Right.Count -eq 0) {
        return 0.0
    }
    $leftSet = @{}
    foreach ($item in $Left) { $leftSet[$item] = $true }
    $rightSet = @{}
    foreach ($item in $Right) { $rightSet[$item] = $true }

    $intersection = 0
    foreach ($item in $leftSet.Keys) {
        if ($rightSet.ContainsKey($item)) {
            $intersection++
        }
    }
    return (2.0 * $intersection) / [double]($leftSet.Count + $rightSet.Count)
}

function Build-QuestionInfo($Question) {
    $tokens = @(Get-Tokens ([string]$Question.question))
    $ngrams = @(Get-Ngrams ([string]$Question.question) 2)
    return [pscustomobject]@{
        externalId = [string]$Question.externalId
        missionCode = [string]$Question.missionCode
        type = [string]$Question.type
        packNo = $Question.packNo
        question = [string]$Question.question
        tokens = $tokens
        ngrams = $ngrams
    }
}

function Compare-Pair($Left, $Right) {
    $tokenScore = Jaccard $Left.tokens $Right.tokens
    $ngramScore = Dice $Left.ngrams $Right.ngrams
    $score = [Math]::Max($tokenScore, $ngramScore)
    return [pscustomobject]@{
        leftId = $Left.externalId
        rightId = $Right.externalId
        missionCode = $Left.missionCode
        leftType = $Left.type
        rightType = $Right.type
        leftPack = $Left.packNo
        rightPack = $Right.packNo
        score = $score
        tokenJaccard = $tokenScore
        ngramDice = $ngramScore
        leftQuestion = $Left.question
        rightQuestion = $Right.question
    }
}

$bank = Read-Json $InputPath
$questions = @($bank.questions | ForEach-Object { Build-QuestionInfo $_ })

$sameMissionViolations = New-Object System.Collections.Generic.List[object]
$sameMissionTop = New-Object System.Collections.Generic.List[object]

$byMission = $questions | Group-Object missionCode
foreach ($missionGroup in $byMission) {
    $items = @($missionGroup.Group)
    for ($left = 0; $left -lt $items.Count; $left++) {
        for ($right = $left + 1; $right -lt $items.Count; $right++) {
            $pair = Compare-Pair $items[$left] $items[$right]
            if ($pair.score -ge $Threshold) {
                $sameMissionViolations.Add($pair)
            }
            if ($pair.score -ge 0.75) {
                $sameMissionTop.Add($pair)
            }
        }
    }
}

$globalTop = New-Object System.Collections.Generic.List[object]
for ($left = 0; $left -lt $questions.Count; $left++) {
    for ($right = $left + 1; $right -lt $questions.Count; $right++) {
        $pair = Compare-Pair $questions[$left] $questions[$right]
        if ($pair.score -ge $Threshold) {
            $globalTop.Add($pair)
        }
    }
}

$sameMissionTopSorted = @($sameMissionTop | Sort-Object score -Descending | Select-Object -First $TopPairLimit)
$globalTopSorted = @($globalTop | Sort-Object score -Descending | Select-Object -First $TopPairLimit)

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Question Bank Similarity Report")
$report.Add("")
$report.Add("- input: ``$InputPath``")
$report.Add("- generatedAt: $(Get-Date -Format s)")
$report.Add("- threshold: $Threshold")
$report.Add("- question count: $($questions.Count)")
$report.Add("- mission count: $($byMission.Count)")
$report.Add("- same-mission pair count: $(($questions | Group-Object missionCode | ForEach-Object { $n = $_.Count; ($n * ($n - 1)) / 2 } | Measure-Object -Sum).Sum)")
$report.Add("- same-mission violations >= ${Threshold}: $($sameMissionViolations.Count)")
$report.Add("- global pairs >= ${Threshold}: $($globalTop.Count)")
$report.Add("")

if ($sameMissionViolations.Count -eq 0) {
    $report.Add("## Verdict")
    $report.Add("")
    $report.Add("PASS: same-mission question-text similarity is below $Threshold for every pair under the local token/ngram proxy.")
} else {
    $report.Add("## Verdict")
    $report.Add("")
    $report.Add("FAIL: at least one same-mission pair is >= $Threshold.")
}

$report.Add("")
$report.Add("## Same-mission Top Pairs")
$report.Add("")
if ($sameMissionTopSorted.Count -eq 0) {
    $report.Add("- none at or above 0.75")
} else {
    foreach ($pair in $sameMissionTopSorted) {
        $report.Add(("- {0} ``{1}`` vs ``{2}`` [{3}]" -f [Math]::Round($pair.score, 4), $pair.leftId, $pair.rightId, $pair.missionCode))
        $report.Add("  - $($pair.leftQuestion)")
        $report.Add("  - $($pair.rightQuestion)")
    }
}

$report.Add("")
$report.Add("## Global Pairs At Or Above Threshold")
$report.Add("")
$report.Add("These are cross-mission included for reference. The generation gate only enforces the threshold inside each mission.")
if ($globalTopSorted.Count -eq 0) {
    $report.Add("- none")
} else {
    foreach ($pair in $globalTopSorted) {
        $report.Add(("- {0} ``{1}`` vs ``{2}`` [{3}]" -f [Math]::Round($pair.score, 4), $pair.leftId, $pair.rightId, $pair.missionCode))
        $report.Add("  - $($pair.leftQuestion)")
        $report.Add("  - $($pair.rightQuestion)")
    }
}

Write-Utf8 $ReportPath ($report -join [Environment]::NewLine)

Write-Output "report=$ReportPath"
Write-Output "threshold=$Threshold"
Write-Output "questionCount=$($questions.Count)"
Write-Output "sameMissionViolations=$($sameMissionViolations.Count)"
Write-Output "globalPairsAtOrAboveThreshold=$($globalTop.Count)"
