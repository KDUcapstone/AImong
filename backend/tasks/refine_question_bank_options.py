import itertools
import json
import re
import sys
from importlib import util
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
QUESTION_BANK = ROOT / "_generated" / "question-bank" / "question-bank-1056-starlevel-edits.json"
OUTPUT_SQL = ROOT / "_generated" / "question-bank" / "question-bank-1056-starlevel-seed.sql"
OUTPUT_REPORT = ROOT / "_generated" / "question-bank" / "question-bank-1056-starlevel-edits-report.md"
REVISION_SCRIPT = ROOT / "tasks" / "revise_ai_literacy_question_bank.py"


GENERAL_ACTION_DISTRACTORS = [
    "겉모습이 익숙하면 자세히 살피지 않고 맞다고 봐요.",
    "친구가 먼저 골랐다는 이유만으로 같은 선택을 해요.",
    "결과가 빨리 나오면 확인 과정은 줄여도 된다고 생각해요.",
    "설명이 길어 보이면 근거가 충분하다고 판단해요.",
    "처음 떠오른 방법을 기준 없이 그대로 사용해요.",
    "재미있는 기능을 먼저 정하고 필요한 사람은 나중에 생각해요.",
]

GENERAL_TERM_DISTRACTORS = [
    "겉모습",
    "속도",
    "인기",
    "분위기",
    "이름",
    "꾸미기",
    "첫인상",
    "편리함",
]
VERBAL_NOUN_DISTRACTORS = [
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
]


def looks_like_phrase(text):
    stripped = text.strip()
    sentence_endings = ("요", "요.", "다", "다.", "죠", "죠.", "까", "까?")
    return not stripped.endswith(sentence_endings)


def fill_suffix(question):
    text = question.get("question", "")
    if "____" not in text:
        return ""
    after = text.split("____", 1)[1].lstrip()
    match = re.match(r"([^\s,.?!?]+)", after)
    return match.group(1) if match else ""


def fill_requires_verbal_noun(question):
    return fill_suffix(question).startswith(("해", "해야", "해요", "하는", "하는지"))


PROFILES = {
    "S0101": {
        "phrases": [
            "정해진 시간에만 울리는 알람",
            "사람이 직접 고른 음악 목록",
            "버튼 순서대로 움직이는 장난감",
            "사용자가 적은 순서표를 그대로 보여 주는 앱",
            "미리 저장된 문장만 읽어 주는 안내판",
            "온도가 높으면 켜지는 단순 선풍기",
            "사람이 분류해 둔 사진 폴더",
            "번호를 누르면 연결되는 전화기",
            "정해진 답만 보여 주는 자동 응답판",
            "종이에 적힌 설명을 그대로 따라 하는 활동",
            "사람이 만든 추천 목록을 보여 주는 게시판",
            "한 가지 규칙만 반복하는 계산 도구",
        ],
        "actions": [
            "사람이 정한 순서만 따라가는 도구도 스스로 판단한다고 봐요.",
            "화면에 글자가 많으면 인공지능이라고 판단해요.",
            "자동으로 켜지는 기능만 보고 인공지능이라고 생각해요.",
            "추천 결과가 있어도 어떤 자료를 보는지는 살피지 않아요.",
            "센서가 있다는 이유만으로 학습 기능이 있다고 말해요.",
            "사람이 직접 고른 목록을 AI 추천으로 착각해요.",
            "도구 이름에 스마트가 붙으면 기능을 확인하지 않아요.",
            "정해진 버튼을 누르는 도구와 판단하는 도구를 섞어 생각해요.",
            "자료를 비교하지 않는 계산 기능도 인공지능이라고 봐요.",
            "생활 속 예시를 찾을 때 무엇을 도와주는지보다 모양을 먼저 봐요.",
            "기계가 움직이면 모두 비슷한 방식으로 배운다고 생각해요.",
            "사용자가 직접 입력한 규칙을 AI가 스스로 배운 것으로 오해해요.",
        ],
        "terms": ["겉모습", "전원", "버튼 수", "자동 실행", "제품 이름", "화면 크기", "센서 모양", "소리 크기", "디자인", "가격", "설명서", "리모컨"],
    },
    "S0102": {
        "actions": [
            "정해진 규칙을 빠르게 실행하는 것도 데이터를 보고 배운다고 생각해요.",
            "버튼이 많으면 인공지능과 일반 프로그램을 구분하지 않아도 된다고 봐요.",
            "계산 결과가 정확하면 학습한 AI라고 바로 판단해요.",
            "사람이 미리 넣은 조건과 AI가 찾은 기준을 헷갈려요.",
            "자료를 보지 않아도 이름이 멋지면 AI라고 말해요.",
            "규칙이 바뀌어도 스스로 고치지 못하는 도구를 AI라고 착각해요.",
            "정해진 순서대로만 움직이는 프로그램도 판단한다고 봐요.",
            "입력 자료를 비교하는 과정 없이 결과만 보고 AI라고 해요.",
            "사용자가 직접 고른 값을 AI가 추천한 값으로 오해해요.",
            "반복 작업을 빠르게 하면 데이터를 배운 것이라고 생각해요.",
            "조건문으로 나눈 결과와 학습으로 찾은 기준을 같은 것으로 봐요.",
            "화면에 그림이 나오면 어떤 원리인지 확인하지 않아요.",
        ],
        "terms": ["정해진 규칙", "반복 실행", "계산 결과", "버튼 수", "화면 모양", "사용자 선택", "조건문", "속도", "제품 이름", "자동 재생", "입력값", "순서표"],
    },
    "S0103": {
        "actions": [
            "자료가 적어도 결과가 그럴듯하면 맞을 가능성이 크다고 봐요.",
            "친구 한 명의 자료를 전체 친구들의 모습처럼 사용해요.",
            "틀린 이름표가 섞였는지 확인하지 않고 그대로 둬요.",
            "한쪽 자료가 많이 빠져도 결과에는 큰 차이가 없다고 생각해요.",
            "비슷한 예시만 모아도 충분히 다양하다고 말해요.",
            "자료를 모은 까닭을 적지 않고 결과만 비교해요.",
            "새로운 예시로 다시 확인하지 않아도 된다고 봐요.",
            "잘못 분류된 자료를 고치지 않고 수만 늘려요.",
            "보기 쉬운 자료만 골라도 괜찮다고 생각해요.",
            "자료의 출처보다 결과가 마음에 드는지를 먼저 봐요.",
            "빠진 경우가 있는지 묻지 않고 평균만 확인해요.",
            "낡은 자료와 새 자료를 섞어도 표시하지 않아요.",
        ],
        "terms": ["비슷한 예시", "한쪽 자료", "틀린 이름표", "빠진 자료", "자료 출처", "새 예시", "낡은 자료", "친구 한 명", "보기 쉬움", "자료 수", "분류 실수", "평균"],
    },
    "S0104": {
        "actions": [
            "흐린 사진도 결과가 나오면 그대로 믿어도 된다고 봐요.",
            "소리가 작거나 주변이 시끄러워도 인식 결과를 확인하지 않아요.",
            "글자가 비슷하게 생기면 AI가 뜻까지 제대로 안다고 생각해요.",
            "사진 속 물체를 맞혔다는 이유로 상황 전체를 이해했다고 봐요.",
            "인식 결과가 낯설어도 입력 자료의 상태를 살피지 않아요.",
            "사람의 표정을 맞혔다고 마음까지 읽었다고 말해요.",
            "카메라가 본 장면이면 배경과 빛은 중요하지 않다고 생각해요.",
            "음성 인식이 틀려도 말한 사람 탓으로만 돌려요.",
            "글자 인식 결과를 읽고 원본과 비교하지 않아요.",
            "한 번 맞힌 경험만으로 다음 결과도 비슷하다고 봐요.",
            "입력 자료가 부족해도 인식 도구가 알아서 보완한다고 생각해요.",
            "사진, 소리, 글을 모두 같은 방법으로 판단한다고 오해해요.",
        ],
        "terms": ["흐린 사진", "주변 소음", "비슷한 글자", "빛의 방향", "입력 상태", "원본 비교", "배경", "표정", "소리 크기", "카메라 각도", "인식 실수", "자료 부족"],
    },
    "S0105": {
        "reasons": [
            "질문을 짧게 쓰면 AI가 성의 없이 답하기 때문이에요.",
            "앱 화면 색이 어두우면 답도 어둡게 나오기 때문이에요.",
            "답을 빨리 달라고 하면 AI가 일부러 헷갈리기 때문이에요.",
            "사용자가 고맙다고 말하지 않으면 답이 나빠지기 때문이에요.",
            "글자가 많아 보이면 AI가 내용을 읽지 않기 때문이에요.",
            "질문에 그림을 넣지 않으면 어떤 문제든 틀리기 때문이에요.",
            "같은 질문을 두 번 하면 AI가 장난으로 바꾸기 때문이에요.",
            "기계가 오래 켜져 있으면 지쳐서 틀리기 때문이에요.",
            "어려운 낱말을 쓰면 AI가 감정을 상해서 틀리기 때문이에요.",
            "답이 짧으면 좋은 정보가 들어갈 수 없기 때문이에요.",
            "질문한 사람이 초등학생이면 답을 대충 하기 때문이에요.",
            "앱 이름이 낯설면 정확한 답을 만들 수 없기 때문이에요.",
        ],
        "actions": [
            "AI 답이 자연스럽게 들리면 근거를 더 찾지 않아도 된다고 봐요.",
            "새로운 사건도 예전 자료만으로 충분히 맞힐 수 있다고 생각해요.",
            "틀릴 수 있다는 설명은 읽지 않고 답만 가져와요.",
            "출처가 없어도 문장이 길면 믿을 만하다고 판단해요.",
            "모르는 내용은 AI가 알아서 정확히 채워 줄 것이라고 기대해요.",
            "사람이 확인할 필요 없이 AI 답을 그대로 발표해요.",
            "질문이 애매해도 첫 답을 최종 답으로 삼아요.",
            "서로 다른 자료와 비교하지 않고 한 답만 봐요.",
            "날짜가 중요한 정보인데 언제 자료인지 확인하지 않아요.",
            "오류를 발견해도 다시 질문하거나 고치지 않아요.",
            "자신이 아는 내용과 다르지만 설명이 그럴듯하면 넘어가요.",
            "답이 빠르게 나오면 정확성도 함께 높다고 생각해요.",
        ],
        "terms": ["그럴듯한 문장", "출처 없음", "오래된 자료", "첫 답", "애매한 질문", "사람 확인", "날짜", "비교 자료", "오류 표시", "최종 발표", "빠른 답", "근거 부족"],
    },
    "S0201": {
        "actions": [
            "무엇을 원하는지 말하지 않고 빨리 해 달라고만 요청해요.",
            "숙제 전체를 대신 끝내 달라고 요청해요.",
            "친구 이름과 연락처를 넣으면 더 정확해진다고 생각해요.",
            "원하는 형식을 말하지 않고 답이 마음에 안 든다고만 해요.",
            "대상 독자를 알려 주지 않고 어려운 말로 써 달라고 해요.",
            "개인정보가 들어가도 예시가 자세하면 괜찮다고 봐요.",
            "조건을 많이 숨겨야 AI가 더 잘 맞힌다고 생각해요.",
            "목적보다 멋진 표현을 먼저 요구해요.",
            "질문을 짧게만 쓰면 늘 좋은 답이 나온다고 기대해요.",
            "필요한 범위를 정하지 않고 모든 내용을 다 써 달라고 해요.",
            "내가 이해할 수준을 말하지 않고 답만 길게 요청해요.",
            "예시를 줄 때 사실과 상상을 구분하지 않아요.",
        ],
        "terms": ["목적 없음", "대상 빠짐", "형식 빠짐", "개인정보", "숙제 대신", "조건 숨김", "긴 답", "멋진 표현", "수준 빠짐", "범위 없음", "예시 혼합", "빠른 요청"],
    },
    "S0202": {
        "actions": [
            "답이 마음에 들지 않으면 어떤 부분이 부족한지 말하지 않고 포기해요.",
            "조건을 확인하지 않고 AI가 틀렸다고만 해요.",
            "틀린 내용도 보기 좋으면 그대로 베껴 써요.",
            "어려운 말이 있으면 더 어려운 말로 바꿔 달라고 해요.",
            "목적과 맞지 않는 부분을 찾지 않고 답 전체를 버려요.",
            "빠진 조건을 더하지 않고 같은 질문만 반복해요.",
            "친구에게 설명할 말인지 발표문인지 구분하지 않아요.",
            "처음 답을 고칠 때 근거 확인은 생략해요.",
            "답이 길어지면 저절로 좋아졌다고 판단해요.",
            "잘못된 예시를 발견해도 예시만 빼고 끝내요.",
            "내 질문이 애매했는지 돌아보지 않아요.",
            "고친 답과 원래 목적을 다시 비교하지 않아요.",
        ],
        "terms": ["조건 빠짐", "같은 질문", "전체 포기", "보기 좋은 문장", "근거 생략", "목적 어긋남", "애매한 질문", "긴 답", "예시 오류", "대상 혼동", "비교 생략", "반복 요청"],
    },
    "S0203": {
        "actions": [
            "이름이나 주소가 있어야 AI가 더 친절하게 답한다고 생각해요.",
            "친구 얼굴 사진은 친한 사이면 허락 없이 써도 된다고 봐요.",
            "위치 정보가 들어가도 학교 과제라면 괜찮다고 말해요.",
            "목소리 파일은 사진보다 덜 중요하다고 생각해요.",
            "전화번호를 넣으면 더 정확한 답이 나온다고 믿어요.",
            "개인정보를 지우면 답 품질이 크게 떨어진다고 걱정해요.",
            "별명만 쓰면 실제 사람을 알아볼 수 없다고 단정해요.",
            "단체 사진에서 한 사람만 작게 보이면 허락이 필요 없다고 봐요.",
            "생일과 학교 이름은 평범한 정보라서 그대로 넣어요.",
            "AI 채팅방이면 친구와 나눈 이야기를 옮겨도 된다고 생각해요.",
            "보호자나 선생님 확인 없이 민감한 정보를 올려요.",
            "나중에 지우면 먼저 올려도 문제가 없다고 봐요.",
        ],
        "terms": ["이름", "주소", "얼굴 사진", "위치 정보", "목소리", "전화번호", "학교 이름", "생일", "별명", "단체 사진", "채팅 내용", "보호자 확인"],
    },
    "S0204": {
        "actions": [
            "한 반 자료만 모아도 학교 전체를 대표한다고 말해요.",
            "인터넷에서 본 사진은 출처를 확인하지 않아도 된다고 봐요.",
            "틀린 이름표가 있어도 결과에는 별 영향이 없다고 생각해요.",
            "보기 좋은 자료만 골라 넣어도 좋은 자료라고 말해요.",
            "자료를 누가 언제 모았는지 기록하지 않아요.",
            "비슷한 예시가 많으면 다양한 자료라고 판단해요.",
            "빠진 집단이 있어도 평균만 보면 된다고 생각해요.",
            "오래된 자료와 새 자료를 구분하지 않아요.",
            "허락받지 않은 자료도 학습에 도움이 되면 써도 된다고 봐요.",
            "자료가 많으면 잘못된 자료가 조금 섞여도 괜찮다고 해요.",
            "기준을 정하지 않고 마음에 드는 자료부터 모아요.",
            "결과가 좋아 보이면 자료 품질을 따로 확인하지 않아요.",
        ],
        "terms": ["한 반 자료", "출처 없음", "틀린 이름표", "보기 좋은 자료", "수집 날짜", "비슷한 예시", "빠진 집단", "오래된 자료", "허락 없는 자료", "잘못된 자료", "기준 없음", "자료 품질"],
    },
    "S0205": {
        "actions": [
            "AI 결과가 예상과 다르면 이유를 찾지 않고 바로 버려요.",
            "실험 조건을 바꾸고도 기록하지 않아요.",
            "한 번 맞은 결과만 보고 도구가 충분히 좋다고 판단해요.",
            "틀린 결과를 발견해도 어떤 자료에서 틀렸는지 살피지 않아요.",
            "친구 의견을 듣지 않고 내 결과만 기준으로 삼아요.",
            "결과가 재미있으면 실제 문제 해결에 맞는지는 보지 않아요.",
            "고친 뒤 다시 시험하지 않아도 좋아졌다고 생각해요.",
            "틀린 예시를 숨기고 맞은 예시만 발표해요.",
            "성공한 방법을 기록하지 않고 감으로 계속 바꿔요.",
            "문제 원인을 자료, 질문, 기준으로 나누어 보지 않아요.",
            "빠르게 나온 결과를 더 신뢰해요.",
            "오류를 고칠 때 무엇을 바꾸었는지 친구와 공유하지 않아요.",
        ],
        "terms": ["기록 없음", "한 번 성공", "틀린 예시", "다시 시험", "친구 의견", "조건 변경", "원인 구분", "빠른 결과", "맞은 예시만", "감으로 수정", "문제 해결", "공유 생략"],
    },
    "S0206": {
        "actions": [
            "AI 문장을 그대로 붙여 넣고 내가 쓴 글처럼 제출해요.",
            "모르는 말이 있어도 멋져 보이면 그대로 사용해요.",
            "참고한 부분을 밝히지 않아도 결과만 좋으면 된다고 생각해요.",
            "내 생각과 AI 도움을 구분하지 않고 섞어 써요.",
            "출처를 확인하지 않은 예시를 그대로 넣어요.",
            "친구에게 설명할 때 AI가 썼다는 사실을 숨겨요.",
            "어려운 표현을 쉬운 말로 바꾸지 않고 발표해요.",
            "저작권이 있는 그림을 확인 없이 마음대로 사용해요.",
            "내가 배운 점을 정리하지 않고 결과만 제출해요.",
            "AI 추천을 참고했는지 표시하지 않아도 된다고 봐요.",
            "책임은 도구에 있으니 사용자는 확인하지 않아도 된다고 생각해요.",
            "문장을 조금만 바꾸면 모두 내 생각이 된다고 말해요.",
        ],
        "terms": ["그대로 제출", "참고 표시", "내 생각", "어려운 표현", "출처 확인", "저작권", "배운 점", "도움 표시", "책임", "쉬운 말", "예시 확인", "문장 바꾸기"],
    },
    "S0301": {
        "actions": [
            "AI가 자신 있게 말하면 사실 확인을 줄여도 된다고 봐요.",
            "날짜가 중요한 정보인데 최신인지 확인하지 않아요.",
            "공식 자료와 다른데도 첫 답을 그대로 사용해요.",
            "출처가 없는 설명을 친구 말처럼 믿어요.",
            "두 자료가 다를 때 어느 쪽 근거가 강한지 비교하지 않아요.",
            "이미 아는 내용과 비슷하면 확인하지 않고 넘어가요.",
            "검색 결과 제목만 보고 내용을 읽지 않아요.",
            "주장이 숫자로 보이면 계산 방법은 묻지 않아요.",
            "새로 바뀐 규칙도 예전 답으로 발표해요.",
            "오류를 발견했지만 시간이 없어 수정하지 않아요.",
            "공식 기관 이름이 비슷하면 같은 자료라고 생각해요.",
            "확인한 날짜를 적지 않고 정보만 가져와요.",
        ],
        "terms": ["자신 있는 말투", "최신 날짜", "공식 자료", "출처 없음", "근거 비교", "검색 제목", "계산 방법", "예전 답", "오류 수정", "비슷한 기관명", "확인 날짜", "첫 답"],
    },
    "S0302": {
        "actions": [
            "제목이 눈에 띄면 근거를 보지 않고 믿을 만하다고 봐요.",
            "조회수가 높으면 내용도 충분히 정확하다고 생각해요.",
            "내가 좋아하는 자료만 골라 발표해요.",
            "출처와 날짜가 없어도 설명이 자세하면 사용해요.",
            "주장과 근거를 나누지 않고 한 문장으로 읽어요.",
            "광고인지 정보인지 확인하지 않아요.",
            "서로 다른 자료가 말하는 차이를 비교하지 않아요.",
            "누가 썼는지보다 그림이 예쁜지를 먼저 봐요.",
            "근거가 약해도 친구들이 많이 본 자료면 괜찮다고 해요.",
            "공식 기관 자료와 개인 의견을 같은 무게로 봐요.",
            "자료가 오래되었는지 확인하지 않고 인용해요.",
            "자료가 어디서 왔는지 설명하지 않고 결론만 말해요.",
        ],
        "terms": ["제목", "조회수", "좋아하는 자료", "출처 날짜", "주장", "근거", "광고", "작성자", "그림", "공식 기관", "오래된 자료", "인용"],
    },
    "S0303": {
        "actions": [
            "많이 모은 자료면 한쪽으로 몰려도 괜찮다고 생각해요.",
            "빠진 사람이 있어도 결과에는 큰 영향이 없다고 봐요.",
            "우리 반 의견만으로 학교 전체를 정해요.",
            "불리한 결과가 나온 친구가 있는지 살피지 않아요.",
            "지역이나 성별이 빠졌는지 확인하지 않아요.",
            "추천 결과가 비슷한 사람에게만 유리한지 묻지 않아요.",
            "자료를 모으기 쉬운 사람들의 의견만 넣어요.",
            "결과가 공평해 보이면 기준을 설명하지 않아도 된다고 봐요.",
            "소수 의견은 수가 적으니 빼도 된다고 생각해요.",
            "한 번 정한 기준이 누구에게 불리한지 다시 보지 않아요.",
            "평균 결과만 보고 개인 차이는 확인하지 않아요.",
            "AI가 만든 순위를 친구들이 모두 받아들여야 한다고 말해요.",
        ],
        "terms": ["한쪽 자료", "빠진 사람", "우리 반 의견", "불리한 결과", "지역", "성별", "추천 기준", "쉬운 자료", "공평해 보임", "소수 의견", "개인 차이", "순위"],
    },
    "S0304": {
        "phrases": [
            "친구의 표정을 몰래 점수로 매기는 것",
            "광고를 더 많이 보이게 하려고 감정을 추측하는 것",
            "기분이 안 좋아 보이는 친구를 놀리는 데 쓰는 것",
            "동의 없이 얼굴 변화를 계속 기록하는 것",
            "감정 결과만 보고 친구 성격을 정하는 것",
            "수업 태도를 자동으로 벌점화하는 것",
            "친구가 싫어하는 별명을 추천하는 것",
            "사람의 설명 없이 감정 점수만 보여 주는 것",
            "민감한 표정 사진을 오래 저장하는 것",
            "기계 판단으로 친구 마음을 단정하는 것",
            "도움이 필요한지 묻지 않고 감시부터 하는 것",
            "감정 결과를 친구들 앞에 공개하는 것",
        ],
        "actions": [
            "새 기술이면 걱정되는 점보다 좋은 점만 말하면 된다고 봐요.",
            "편리하면 개인정보 문제는 나중에 생각해도 된다고 해요.",
            "도움을 받는 사람과 피해를 볼 사람을 나누어 보지 않아요.",
            "기계가 결정하면 책임질 사람이 없어도 된다고 생각해요.",
            "감시가 늘어도 안전해지면 괜찮다고만 말해요.",
            "차별 가능성은 실제 일이 생긴 뒤에만 살피면 된다고 봐요.",
            "좋은 효과가 있으면 부작용은 작게 봐도 된다고 생각해요.",
            "사용하지 않는 사람의 입장은 고려하지 않아요.",
            "AI가 추천했다는 이유로 선택 이유를 묻지 않아요.",
            "규칙을 정하지 않고 먼저 써 본 뒤 고치면 된다고 해요.",
            "문제가 생기면 만든 사람만 책임지면 된다고 봐요.",
            "친구들이 좋아하면 걱정할 점은 줄어든다고 생각해요.",
        ],
        "terms": ["좋은 점만", "개인정보", "피해를 볼 사람", "책임", "감시", "차별 가능성", "부작용", "사용하지 않는 사람", "선택 이유", "사용 규칙", "만든 사람", "친구 반응"],
    },
    "S0305": {
        "actions": [
            "AI 추천 결과가 나오면 이유를 설명하지 않아도 된다고 봐요.",
            "빠른 결정을 위해 불편한 사람의 의견은 나중에 들어요.",
            "친한 친구에게 유리한 기준을 먼저 넣어요.",
            "평균 점수만 보고 누구에게 불리한지 살피지 않아요.",
            "추천을 받지 못한 사람에게 이유를 알려 주지 않아요.",
            "기준을 정할 때 영향을 받는 사람과 이야기하지 않아요.",
            "AI가 고른 결과라서 모두에게 공평하다고 말해요.",
            "소수 의견은 전체 결정에 큰 영향을 주지 않는다고 봐요.",
            "결과가 편리하면 절차는 자세히 보지 않아도 된다고 생각해요.",
            "불만이 생긴 뒤에야 기준을 공개해요.",
            "사람이 다시 살펴볼 기회를 만들지 않아요.",
            "선택받은 사람만 만족하면 충분하다고 봐요.",
        ],
        "terms": ["이유 설명", "불편한 사람", "친한 친구", "평균 점수", "탈락 이유", "영향받는 사람", "공평한 기준", "소수 의견", "절차", "기준 공개", "다시 살펴보기", "만족한 사람"],
    },
}


def load_revision_module():
    spec = util.spec_from_file_location("revise_ai_literacy_question_bank", REVISION_SCRIPT)
    module = util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def answer_index(question):
    if question["type"] in {"MULTIPLE", "SITUATION"}:
        return question["answer"]
    if question["type"] == "FILL":
        return question["answer"][0]
    raise ValueError(f"unsupported type: {question['type']}")


def option_pool(question):
    profile = PROFILES[question["missionCode"]]
    correct = question["options"][answer_index(question)]
    if question["type"] == "FILL":
        if fill_requires_verbal_noun(question):
            return VERBAL_NOUN_DISTRACTORS
        pool = profile["terms"]
        return pool if len(pool) >= 4 else pool + GENERAL_TERM_DISTRACTORS
    if question["type"] == "MULTIPLE":
        if (
            "사례" in question["question"]
            or "사용 예" in question["question"]
            or "도구" in question["question"]
            or looks_like_phrase(correct)
        ) and "phrases" in profile:
            return profile["phrases"]
        if "이유" in question["question"] and "reasons" in profile:
            return profile["reasons"]
    pool = profile["actions"]
    return pool if len(pool) >= 4 else pool + GENERAL_ACTION_DISTRACTORS


def candidate_combinations(question, correct):
    pool = []
    seen = {correct}
    for option in option_pool(question):
        if option not in seen:
            pool.append(option)
            seen.add(option)
    return list(itertools.combinations(pool, 3))


def stable_offset(question, combo_count):
    seed = f"{question['externalId']}|{question['type']}|{question['missionCode']}"
    return sum((index + 1) * ord(char) for index, char in enumerate(seed)) % combo_count


def make_options(question, used_sets):
    old_options = question["options"]
    correct_pos = answer_index(question)
    correct = old_options[correct_pos]
    combos = candidate_combinations(question, correct)
    offset = stable_offset(question, len(combos))

    for step in range(len(combos)):
        combo = combos[(offset + step) % len(combos)]
        next_options = []
        combo_iter = iter(combo)
        for index in range(4):
            next_options.append(correct if index == correct_pos else next(combo_iter))

        key = (question["missionCode"], question["type"], tuple(sorted(next_options)))
        if key not in used_sets:
            used_sets.add(key)
            return next_options

    raise RuntimeError(f"could not create unique option set for {question['externalId']}")


def refine(data):
    used_sets = set()
    changed = 0
    for question in data["questions"]:
        if question["type"] not in {"MULTIPLE", "SITUATION", "FILL"}:
            continue
        old_options = list(question["options"])
        question["options"] = make_options(question, used_sets)
        if question["options"] != old_options:
            changed += 1
    return changed


def update_metadata(data, changed):
    data["generationVersion"] = "AImong-KERIS-ai-literacy-v9-1056-option-refined"
    quality = data.setdefault("qualityRefinement", {})
    changes = quality.setdefault("changes", [])
    changes[:] = [item for item in changes if not item.startswith("Refined distractor")]
    option_question_count = sum(
        1 for question in data["questions"] if question["type"] in {"MULTIPLE", "SITUATION", "FILL"}
    )
    note = (
        f"Refined distractor pools for all {option_question_count} option-bearing questions using mission-specific "
        "elementary misconception pools and grammar-compatible FILL options while preserving correct answer positions."
    )
    if note not in changes:
        changes.append(note)
    quality["optionRefinement"] = {
        "rule": "Correct option text is preserved; distractors are replaced with plausible misconceptions; answer indexes remain attached to the same correct text.",
        "optionBearingQuestions": option_question_count,
        "changedInLastRun": changed,
    }


def main():
    data = json.loads(QUESTION_BANK.read_text(encoding="utf-8"))
    changed = refine(data)
    update_metadata(data, changed)

    revision = load_revision_module()
    structural_errors = revision.validate(data)
    if structural_errors:
        raise SystemExit("\n".join(structural_errors))

    QUESTION_BANK.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    OUTPUT_SQL.write_text(revision.export_seed(data), encoding="utf-8")
    OUTPUT_REPORT.write_text(revision.render_report(data, structural_errors), encoding="utf-8")
    print(f"OK: refined {changed} option-bearing questions")


if __name__ == "__main__":
    sys.exit(main())
