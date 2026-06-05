param(
    [string]$InputPath = "_generated/question-bank/question-bank-1056-starlevel-ultra-diverse.json",
    [string]$ReviewPath = "_generated/question-bank/question-bank-review-20260601-2108.json",
    [string]$OutputPath = "_generated/question-bank/question-bank-1056-starlevel-ultra-diverse-patched.json",
    [string]$ReportPath = "_generated/question-bank/question-bank-1056-starlevel-ultra-diverse-sweep-report.md"
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
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText((Resolve-Path -LiteralPath $parent).Path + [System.IO.Path]::DirectorySeparatorChar + (Split-Path -Leaf $Path), $Text, $encoding)
}

function Answer-Indexes($Question) {
    if ($Question.type -eq "FILL") {
        return @($Question.answer)
    }
    if ($Question.type -in @("MULTIPLE", "SITUATION")) {
        return @($Question.answer)
    }
    return @()
}

function Correct-Option($Question) {
    $indexes = @(Answer-Indexes $Question)
    if ($indexes.Count -ne 1 -or $null -eq $Question.options) {
        return ""
    }
    $index = [int]$indexes[0]
    if ($index -lt 0 -or $index -ge $Question.options.Count) {
        return ""
    }
    return [string]$Question.options[$index]
}

function Set-QuestionPatch($Index, [string]$ExternalId, [hashtable]$Patch, [System.Collections.Generic.List[object]]$Changes) {
    if (-not $Index.ContainsKey($ExternalId)) {
        throw "Missing question id: $ExternalId"
    }

    $question = $Index[$ExternalId]
    $beforeQuestion = $question.question
    $beforeExplanation = $question.explanation

    if ($Patch.ContainsKey("question")) {
        $question.question = $Patch["question"]
    }
    if ($Patch.ContainsKey("options")) {
        $question.options = @($Patch["options"])
    }
    if ($Patch.ContainsKey("answer")) {
        $question.answer = $Patch["answer"]
    }
    if ($Patch.ContainsKey("explanation")) {
        $question.explanation = $Patch["explanation"]
    }

    $Changes.Add([pscustomobject]@{
        externalId = $ExternalId
        kind = "direct-review-patch"
        beforeQuestion = $beforeQuestion
        afterQuestion = $question.question
        beforeExplanation = $beforeExplanation
        afterExplanation = $question.explanation
    })
}

function Run-Validation($Bank) {
    $shapeErrors = New-Object System.Collections.Generic.List[string]
    $semanticWarnings = New-Object System.Collections.Generic.List[string]
    $weakFillWarnings = New-Object System.Collections.Generic.List[string]
    $prefixWarnings = New-Object System.Collections.Generic.List[string]

    $questionGroups = @{}
    foreach ($question in $Bank.questions) {
        if (-not $question.externalId -or -not $question.missionCode -or -not $question.type -or -not $question.question) {
            $shapeErrors.Add("$($question.externalId): required field is blank")
        }

        if ($question.type -in @("MULTIPLE", "SITUATION", "FILL")) {
            if ($null -eq $question.options -or $question.options.Count -ne 4) {
                $shapeErrors.Add("$($question.externalId): option list must contain exactly 4 items")
            } else {
                $indexes = @(Answer-Indexes $question)
                if ($indexes.Count -ne 1) {
                    $shapeErrors.Add("$($question.externalId): answer index shape is invalid")
                } else {
                    $answerIndex = [int]$indexes[0]
                    if ($answerIndex -lt 0 -or $answerIndex -ge $question.options.Count) {
                        $shapeErrors.Add("$($question.externalId): answer index is out of range")
                    }
                }

                $optionSet = @{}
                foreach ($option in $question.options) {
                    $key = [string]$option
                    if ($optionSet.ContainsKey($key)) {
                        $shapeErrors.Add("$($question.externalId): duplicate option '$key'")
                    }
                    $optionSet[$key] = $true
                }
            }
        }

        $questionKey = [string]$question.question
        if (-not $questionGroups.ContainsKey($questionKey)) {
            $questionGroups[$questionKey] = New-Object System.Collections.Generic.List[string]
        }
        $questionGroups[$questionKey].Add([string]$question.externalId)

        foreach ($prefix in @("도서관에서 ", "스마트 스피커 사례에서 ", "번역 앱을 살펴볼 때 ", "우리 주변의 AI 찾기 활동에서 ")) {
            if ($question.question.StartsWith($prefix)) {
                $prefixWarnings.Add("$($question.externalId): weak context prefix '$prefix'")
            }
        }

        if ($question.type -in @("MULTIPLE", "SITUATION")) {
            $correct = Correct-Option $question
            $prompt = [string]$question.question
            $badCuePattern = "그대로|무조건|보지 않고|확인하지|허락 없이|모두 AI|바로 믿|정답처럼|빼고|늘려|광고|이름만|기능을 보지|전기로|전원을 켜|저절로|틀려도|살피지 않|고치지 않|묻지 않|읽지 않|모른 척|내 생각처럼|한 장의|조건 없이"

            if ($prompt -like "*피해야 할 행동*" -and $correct -notmatch $badCuePattern) {
                $semanticWarnings.Add("$($question.externalId): '피해야 할 행동' question has non-negative correct option '$correct'")
            }
            if ($prompt -match "실천한 모습|바르게 판단한 행동|알맞은 태도|좋은 조언" -and $correct -match $badCuePattern) {
                $semanticWarnings.Add("$($question.externalId): positive-action question has negative correct option '$correct'")
            }
            if ($prompt -match "까닭|이유" -and $correct -match "광고|이름만|전기|멋진|모양만") {
                $semanticWarnings.Add("$($question.externalId): reason question correct option looks off-topic '$correct'")
            }
            if ($prompt -like "*무엇을 더 분명히*" -and $correct -notmatch "예시|조건|목적|대상|쉬운 말|바꿔|원하는|누가|읽을|형식|표|목록|근거|자료|알려") {
                $semanticWarnings.Add("$($question.externalId): prompt-repair question has weak correct option '$correct'")
            }
        }

        if ($question.type -eq "FILL" -and $null -ne $question.options) {
            $indexes = @(Answer-Indexes $question)
            if ($indexes.Count -eq 1) {
                $answerIndex = [int]$indexes[0]
                $weakTerms = @("종이", "전선", "자석", "무게", "포장", "소리", "색연필", "책상", "가방", "상자", "충전", "버리기")
                for ($i = 0; $i -lt $question.options.Count; $i++) {
                    if ($i -ne $answerIndex -and $weakTerms -contains ([string]$question.options[$i])) {
                        $weakFillWarnings.Add("$($question.externalId): weak fill distractor '$($question.options[$i])'")
                    }
                }
            }
        }
    }

    $duplicatePromptGroups = New-Object System.Collections.Generic.List[object]
    foreach ($entry in $questionGroups.GetEnumerator()) {
        if ($entry.Value.Count -gt 1) {
            $duplicatePromptGroups.Add([pscustomobject]@{
                question = $entry.Key
                ids = @($entry.Value)
            })
        }
    }

    $explanationClusters = New-Object System.Collections.Generic.List[object]
    $Bank.questions |
        Group-Object explanation |
        Where-Object { $_.Count -ge 5 } |
        Sort-Object Count -Descending |
        ForEach-Object {
            $explanationClusters.Add([pscustomobject]@{
                count = $_.Count
                ids = @($_.Group | Select-Object -ExpandProperty externalId)
                explanation = $_.Name
            })
        }

    $result = "" | Select-Object shapeErrors, semanticWarnings, weakFillWarnings, prefixWarnings, duplicatePromptGroups, explanationClusters
    $result.shapeErrors = $shapeErrors.ToArray()
    $result.semanticWarnings = $semanticWarnings.ToArray()
    $result.weakFillWarnings = $weakFillWarnings.ToArray()
    $result.prefixWarnings = $prefixWarnings.ToArray()
    $result.duplicatePromptGroups = $duplicatePromptGroups.ToArray()
    $result.explanationClusters = $explanationClusters.ToArray()
    return $result
}

$bank = Read-Json $InputPath
$review = Read-Json $ReviewPath

$index = @{}
foreach ($question in $bank.questions) {
    $index[[string]$question.externalId] = $question
}

$beforeValidation = Run-Validation $bank
$changes = New-Object System.Collections.Generic.List[object]
$sweepChanges = New-Object System.Collections.Generic.List[object]

$directPatches = @{
    "S0101-P1-01" = @{
        question = "카메라 앱처럼 생활 속 도구도 정보를 보고 판단하는 AI 기능을 가질 수 있어요."
        explanation = "맞아요. 카메라 앱은 사진 속 특징을 보고 꽃이나 사물을 구별하는 데 AI를 쓸 수 있어요."
    }
    "S0101-P1-03" = @{
        question = "AI인지 보려면 겉모습보다 어떤 일을 하는지 살펴야 해요."
        explanation = "맞아요. 이름이나 장소보다 정보를 보고 판단하는 기능이 있는지 확인해야 해요."
    }
    "S0101-P1-04" = @{
        question = "AI 기능은 사람을 도울 수 있지만 항상 완벽한 답을 내는 것은 아니에요."
        explanation = "맞아요. AI 결과도 틀릴 수 있으므로 필요한 경우 다시 확인해야 해요."
    }
    "S0101-P1-05" = @{
        question = "생활 속 도구가 AI인지 헷갈릴 때 좋은 조언은 무엇일까요?"
        options = @("단순 전자기기와 AI 기능을 나누어 봐요.", "한 번 써 보고 모든 상황에 맞다고 말해요.", "광고 문구만 보고 AI 여부를 정해요.", "멋진 이름이면 기능을 확인하지 않아요.")
        answer = 0
        explanation = "맞아요. AI인지 보려면 겉모습이나 이름보다 어떤 정보를 보고 판단하는지 살펴야 해요."
    }
    "S0101-P1-07" = @{
        question = "생활 속 도구가 AI인지 살필 때 피해야 할 행동은 무엇일까요?"
        explanation = "겉모양만 보고 판단하면 AI 기능인지 단순 기능인지 구별하기 어려워요. 어떤 정보를 보고 판단하는지 먼저 살펴야 해요."
    }
    "S0101-P1-08" = @{
        question = "다음 중 생활 속 AI를 바르게 살펴본 모습은 무엇일까요?"
        options = @("기능을 보지 않고 모양만 보고 판단해요.", "전자기기라는 이유만으로 AI라고 말해요.", "이름만 멋지면 AI 기능이라고 믿어요.", "어떤 정보를 보고 판단하는 기능인지 살펴봐요.")
        answer = 3
        explanation = "맞아요. 생활 속 AI는 겉모습보다 정보를 보고 판단하거나 추천하는 기능이 있는지 살펴봐야 해요."
    }
    "S0101-P1-09" = @{
        question = "모든 전자기기가 AI는 아니므로 어떤 ____을 하는지 구별해야 해요."
        options = @("기능", "버튼", "전원", "모양")
        answer = @(0)
        explanation = "어떤 기능을 하는지 보면 AI 여부를 더 잘 판단할 수 있어요."
    }
    "S0101-P1-10" = @{
        question = "모든 전자기기가 AI는 아니므로 어떤 ____을 하는지 살펴야 해요."
        options = @("전원", "기능", "버튼", "모양")
        answer = @(1)
        explanation = "어떤 기능을 하는지 보면 AI 여부를 더 잘 판단할 수 있어요."
    }
    "S0101-P2-02" = @{
        question = "계산기처럼 정해진 계산만 하는 도구는 언제나 AI예요."
        explanation = "아니에요. 정해진 계산만 빠르게 하는 도구는 AI가 아니라 일반 프로그램이나 전자기기일 수 있어요."
    }
    "S0101-P2-03" = @{
        question = "AI는 사람처럼 직접 경험하고 마음으로 판단해요."
        explanation = "아니에요. AI는 마음으로 판단하지 않고 입력된 자료와 배운 규칙이나 패턴을 바탕으로 결과를 내요."
    }
    "S0101-P2-04" = @{
        question = "정해진 계산만 빠르게 하는 기능은 언제나 AI라고 볼 수 있어요."
        explanation = "아니에요. 단순히 정해진 기능만 수행한다면 AI보다 일반 프로그램에 가까울 수 있어요."
    }
    "S0101-P2-05" = @{
        question = "생활 속 AI를 찾을 때 알맞은 태도는 무엇일까요?"
        explanation = "맞아요. 전자기기라고 모두 AI는 아니므로 기능과 판단 방식을 함께 살펴야 해요."
    }
    "S0101-P2-06" = @{
        question = "생활 속 도구가 AI 기능인지 살펴볼 때 알맞은 행동은 무엇일까요?"
        explanation = "맞아요. AI 기능인지 보려면 입력된 정보로 판단하거나 분류하는지 살펴봐야 해요."
    }
    "S0101-P2-08" = @{
        question = "AI 답에 어려운 말이 많아 이해하기 어려워요. 어떻게 다시 요청하면 좋을까요?"
        options = @("원하는 대상을 말하지 않고 다시 물어요.", "원하는 점은 빼고 분량만 늘려 달라고 해요.", "개인정보를 더 넣으면 좋아질 거라고 생각해요.", "어려운 말은 쉬운 말로 바꿔 달라고 해요.")
        answer = 3
        explanation = "맞아요. 이해하기 어려울 때는 쉬운 말로 바꾸어 달라고 다시 요청할 수 있어요."
    }
    "S0101-P2-09" = @{
        question = "그림 인식 게임처럼 자료를 보고 알맞은 답을 고르는 기능은 ____와 관련이 있어요."
        options = @("단순 저장", "정해진 계산", "AI", "화면 꾸미기")
        answer = @(2)
        explanation = "AI는 자료를 바탕으로 분류하거나 추천하는 데 쓰일 수 있어요."
    }
}

foreach ($id in $directPatches.Keys) {
    Set-QuestionPatch $index $id $directPatches[$id] $changes
}

# Targeted follow-up patches found by the same prefix and explanation-scope sweep.
$followUpPatches = @{
    "S0101-P3-02" = @{
        question = "AI가 한 일을 설명할 때는 어떤 자료를 보았는지 떠올리면 좋아요."
        explanation = "맞아요. AI가 어떤 자료를 보고 결과를 냈는지 떠올리면 기능을 더 잘 설명할 수 있어요."
    }
    "S0101-P4-03" = @{
        question = "카메라 꽃 이름 찾기를 사용할 때 생활 속 AI를 바르게 살펴보는 행동은 무엇일까요?"
        options = @("기능을 보지 않고 모양만 보고 판단해요.", "추천 결과를 모두 정답처럼 받아들여요.", "무엇을 보고 꽃을 구별하는지 살펴봐요.", "불이 켜지면 AI 기능이라고 정해요.")
        answer = 2
        explanation = "맞아요. 카메라 앱이 어떤 정보를 보고 꽃을 구별하는지 살펴보면 AI 기능을 더 잘 이해할 수 있어요."
    }
    "S0101-P5-06" = @{
        question = "숙제를 하다가 카메라 꽃 이름 찾기 AI의 도움을 받았어요. 마지막에는 어떻게 해야 할까요?"
        explanation = "맞아요. AI 도움을 받은 뒤에는 내 말로 고치고 사실을 확인해야 해요. AI 결과보다 확인 과정이 더 중요해요."
    }
    "S0101-P5-10" = @{
        question = "친구들이 자동화와 AI 구분을 두고 의견이 달라졌어요. 가장 좋은 해결 방법은 무엇일까요?"
        explanation = "맞아요. 각자 근거를 듣고 자동화인지 AI인지 살펴볼 기준을 함께 정해요."
    }
}

foreach ($id in $followUpPatches.Keys) {
    Set-QuestionPatch $index $id $followUpPatches[$id] $changes
}

# Rule-based wording sweep: keep the concrete context but remove the duplicated mission-title clause.
foreach ($question in $bank.questions) {
    $before = [string]$question.question
    if ($before -match "^모둠 활동에서 (.+?)(을|를) 정리하려고 해요\. .+에 맞는 태도는 무엇일까요\?$") {
        $question.question = "모둠 활동에서 $($Matches[1])$($Matches[2]) 정리할 때 알맞은 태도는 무엇일까요?"
        $sweepChanges.Add([pscustomobject]@{
            externalId = $question.externalId
            kind = "mission-title-clause-trim"
            beforeQuestion = $before
            afterQuestion = $question.question
        })
    }
}

$bank.generationVersion = "$($bank.generationVersion)-patched-20260601-sweep4"
$bank.normalizationNote = "$($bank.normalizationNote) 2026-06-01 검수 리뷰를 반영해 S0101 초반 문항의 불필요한 상황 접두사, 문제-선지-해설 불일치, 빈칸 오답 품질을 보정하고 4단계 스윕 규칙으로 구조/의미 경고를 점검했습니다."

$afterValidation = Run-Validation $bank

$json = $bank | ConvertTo-Json -Depth 100
Write-Utf8 $OutputPath $json

$reviewedIds = @($review.issues | Select-Object -ExpandProperty externalId)
$remainingReviewedTextMatches = 0
foreach ($issue in $review.issues) {
    $patched = $index[[string]$issue.externalId]
    if ($patched.question -eq $issue.question) {
        $remainingReviewedTextMatches++
    }
}

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Ultra-diverse Question Bank Sweep Report")
$report.Add("")
$report.Add("- input: ``$InputPath``")
$report.Add("- review: ``$ReviewPath``")
$report.Add("- output: ``$OutputPath``")
$report.Add("- generatedAt: $(Get-Date -Format s)")
$report.Add("- total questions: $($bank.questions.Count)")
$report.Add("- review issue count: $($review.issues.Count)")
$report.Add("- reviewed questions still matching original reviewed text: $remainingReviewedTextMatches")
$report.Add("")
$report.Add("## Applied Changes")
$report.Add("")
$report.Add("- direct review patches: $($changes.Count)")
$report.Add("- rule-based wording sweep changes: $($sweepChanges.Count)")
$report.Add("")
$report.Add("## Validation Summary")
$report.Add("")
$report.Add("| Check | Before | After |")
$report.Add("| --- | ---: | ---: |")
$report.Add("| shape errors | $($beforeValidation.shapeErrors.Count) | $($afterValidation.shapeErrors.Count) |")
$report.Add("| duplicate prompt groups | $($beforeValidation.duplicatePromptGroups.Count) | $($afterValidation.duplicatePromptGroups.Count) |")
$report.Add("| weak context prefix warnings | $($beforeValidation.prefixWarnings.Count) | $($afterValidation.prefixWarnings.Count) |")
$report.Add("| semantic warnings | $($beforeValidation.semanticWarnings.Count) | $($afterValidation.semanticWarnings.Count) |")
$report.Add("| weak fill distractor warnings | $($beforeValidation.weakFillWarnings.Count) | $($afterValidation.weakFillWarnings.Count) |")
$report.Add("| repeated explanation clusters >= 5 | $($beforeValidation.explanationClusters.Count) | $($afterValidation.explanationClusters.Count) |")
$report.Add("")
$report.Add("## Direct Review Patch IDs")
$report.Add("")
foreach ($change in $changes) {
    $report.Add("- ``$($change.externalId)``: $($change.beforeQuestion) -> $($change.afterQuestion)")
}
$report.Add("")
$report.Add("## Rule-based Wording Sweep IDs")
$report.Add("")
foreach ($change in $sweepChanges) {
    $report.Add("- ``$($change.externalId)``: $($change.beforeQuestion) -> $($change.afterQuestion)")
}
$report.Add("")
$report.Add("## Remaining Warnings")
$report.Add("")
$report.Add("### Shape Errors")
if ($afterValidation.shapeErrors.Count -eq 0) {
    $report.Add("- none")
} else {
    foreach ($item in @($afterValidation.shapeErrors | Select-Object -First 50)) {
        $report.Add("- $item")
    }
}
$report.Add("")
$report.Add("### Semantic Warnings")
if ($afterValidation.semanticWarnings.Count -eq 0) {
    $report.Add("- none")
} else {
    foreach ($item in @($afterValidation.semanticWarnings | Select-Object -First 80)) {
        $report.Add("- $item")
    }
    if ($afterValidation.semanticWarnings.Count -gt 80) {
        $report.Add("- ... $($afterValidation.semanticWarnings.Count - 80) more")
    }
}
$report.Add("")
$report.Add("### Weak Fill Distractor Warnings")
if ($afterValidation.weakFillWarnings.Count -eq 0) {
    $report.Add("- none")
} else {
    foreach ($item in @($afterValidation.weakFillWarnings | Select-Object -First 80)) {
        $report.Add("- $item")
    }
    if ($afterValidation.weakFillWarnings.Count -gt 80) {
        $report.Add("- ... $($afterValidation.weakFillWarnings.Count - 80) more")
    }
}
$report.Add("")
$report.Add("### Repeated Explanation Clusters")
if ($afterValidation.explanationClusters.Count -eq 0) {
    $report.Add("- none")
} else {
    foreach ($cluster in @($afterValidation.explanationClusters | Select-Object -First 30)) {
        $ids = (@($cluster.ids) | Select-Object -First 8) -join ", "
        $report.Add("- count $($cluster.count), sample ``$ids``: $($cluster.explanation)")
    }
}

Write-Utf8 $ReportPath ($report -join [Environment]::NewLine)

Write-Output "patched=$OutputPath"
Write-Output "report=$ReportPath"
Write-Output "directPatches=$($changes.Count)"
Write-Output "sweepChanges=$($sweepChanges.Count)"
Write-Output "shapeErrorsBefore=$($beforeValidation.shapeErrors.Count)"
Write-Output "shapeErrorsAfter=$($afterValidation.shapeErrors.Count)"
Write-Output "semanticWarningsBefore=$($beforeValidation.semanticWarnings.Count)"
Write-Output "semanticWarningsAfter=$($afterValidation.semanticWarnings.Count)"
Write-Output "weakFillWarningsBefore=$($beforeValidation.weakFillWarnings.Count)"
Write-Output "weakFillWarningsAfter=$($afterValidation.weakFillWarnings.Count)"
Write-Output "repeatedExplanationClustersBefore=$($beforeValidation.explanationClusters.Count)"
Write-Output "repeatedExplanationClustersAfter=$($afterValidation.explanationClusters.Count)"
