import collections
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
QUESTION_BANK = ROOT / "_generated" / "question-bank" / "question-bank-1056-starlevel-edits.json"

OPTION_TYPES = {"MULTIPLE", "SITUATION", "FILL"}
ABSOLUTE_CUES = ("무조건", "항상", "언제나", "절대", "반드시")
WEAK_DISTRACTORS = {
    "빈 공책",
    "바람개비 장난감",
    "색종이를 접는 종이 설명서",
    "전기 없이도 움직여요.",
    "전기가 흐르면 정답이 달라지기 때문이에요.",
    "화면이 밝으면 틀린 답이 나오기 때문이에요.",
    "종이가 두꺼우면 인공 지능이에요.",
    "포장",
    "색칠",
    "숨김",
    "복사",
    "삭제",
}
VERBAL_NOUN_OPTIONS = {
    "판단",
    "확인",
    "비교",
    "기록",
    "검토",
    "수정",
    "요청",
    "입력",
    "설명",
    "선택",
    "분석",
    "구분",
    "검증",
    "정리",
    "인식",
    "반응",
    "분류",
    "해결",
    "테스트",
    "실험",
    "관찰",
    "연습",
}


def iter_option_questions(data):
    for question in data["questions"]:
        if question["type"] in OPTION_TYPES:
            yield question


def answer_indexes(question):
    answer = question["answer"]
    if question["type"] in {"MULTIPLE", "SITUATION"}:
        return [answer] if isinstance(answer, int) else []
    if question["type"] == "FILL":
        return answer if isinstance(answer, list) else []
    return []


def fill_suffix(question):
    text = question.get("question", "")
    if "____" not in text:
        return ""
    after = text.split("____", 1)[1].lstrip()
    match = re.match(r"([^\s,.?!?]+)", after)
    return match.group(1) if match else ""


def fill_requires_verbal_noun(question):
    suffix = fill_suffix(question)
    return suffix.startswith(("해", "해야", "해요", "하는", "하는지"))


def validate(data):
    errors = []
    repeated_sets = collections.defaultdict(list)

    for question in iter_option_questions(data):
        external_id = question.get("externalId", "<unknown>")
        options = question.get("options")
        if not isinstance(options, list) or len(options) != 4:
            errors.append(f"{external_id}: option list must contain exactly 4 items")
            continue

        indexes = answer_indexes(question)
        if len(indexes) != 1 or indexes[0] < 0 or indexes[0] >= len(options):
            errors.append(f"{external_id}: invalid answer index shape {question.get('answer')!r}")
            continue

        correct_index = indexes[0]
        correct = options[correct_index]
        distractors = [option for i, option in enumerate(options) if i != correct_index]

        if len(set(options)) != len(options):
            errors.append(f"{external_id}: duplicate options inside one question")
        if not isinstance(correct, str) or not correct.strip():
            errors.append(f"{external_id}: blank correct option")
        if any((not isinstance(option, str) or not option.strip()) for option in distractors):
            errors.append(f"{external_id}: blank distractor")

        weak_hits = [option for option in distractors if option in WEAK_DISTRACTORS]
        if weak_hits:
            errors.append(f"{external_id}: weak distractor(s) {weak_hits}")

        cue_hits = [option for option in options if any(cue in option for cue in ABSOLUTE_CUES)]
        if cue_hits:
            errors.append(f"{external_id}: absolute-word cue(s) {cue_hits}")

        if question["type"] == "FILL" and fill_requires_verbal_noun(question):
            incompatible = [option for option in options if option not in VERBAL_NOUN_OPTIONS]
            if incompatible:
                errors.append(
                    f"{external_id}: fill option(s) do not combine with verbal suffix "
                    f"{fill_suffix(question)!r}: {incompatible}"
                )

        key = (question["missionCode"], question["type"], tuple(sorted(options)))
        repeated_sets[key].append(external_id)

    for (mission_code, question_type, _), ids in repeated_sets.items():
        if len(ids) > 1:
            sample = ", ".join(ids[:4])
            errors.append(
                f"{mission_code}/{question_type}: repeated option set used {len(ids)} times ({sample})"
            )

    return errors


def main():
    data = json.loads(QUESTION_BANK.read_text(encoding="utf-8"))
    errors = validate(data)
    if errors:
        print(f"FAILED: {len(errors)} option-quality issue(s)")
        for error in errors[:120]:
            print(f"- {error}")
        if len(errors) > 120:
            print(f"- ... {len(errors) - 120} more")
        raise SystemExit(1)
    print("OK: option quality validation passed")


if __name__ == "__main__":
    sys.exit(main())
