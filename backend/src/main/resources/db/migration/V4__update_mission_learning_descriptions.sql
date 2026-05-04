UPDATE missions
SET description = CASE mission_code
    WHEN 'S0101' THEN '생활 속 AI 도구와 AI의 기본 개념을 배워요'
    WHEN 'S0102' THEN '규칙 기반 도구와 학습 기반 AI의 차이를 배워요'
    WHEN 'S0103' THEN 'AI가 데이터를 보고 배우는 방식을 배워요'
    WHEN 'S0104' THEN '딥러닝이 이미지와 소리를 인식하는 방식을 배워요'
    WHEN 'S0105' THEN 'AI 답변도 틀릴 수 있음을 알고 확인 습관을 배워요'
    WHEN 'S0201' THEN '목적이 드러나는 좋은 질문을 만드는 법을 배워요'
    WHEN 'S0202' THEN '조건을 구체적으로 담아 AI에게 질문하는 법을 배워요'
    WHEN 'S0203' THEN '개인정보와 생체정보를 안전하게 지키는 법을 배워요'
    WHEN 'S0204' THEN '사진, 음성, 데이터를 바르게 모으고 사용하는 법을 배워요'
    WHEN 'S0205' THEN 'AI 도구를 실험하고 결과를 고쳐 보는 법을 배워요'
    WHEN 'S0206' THEN 'AI 도움을 참고해 내 답으로 정리하는 법을 배워요'
    WHEN 'S0301' THEN 'AI 답변을 바로 믿지 않고 사실을 확인하는 법을 배워요'
    WHEN 'S0302' THEN '출처와 근거를 비교해 정보의 믿을 만함을 판단해요'
    WHEN 'S0303' THEN '데이터와 AI 결과에 숨어 있는 편향을 찾아봐요'
    WHEN 'S0304' THEN 'AI 기술의 좋은 점과 조심할 점을 함께 생각해요'
    WHEN 'S0305' THEN 'AI 윤리 딜레마에서 공정한 선택을 고민해요'
    ELSE description
END
WHERE mission_code IN (
    'S0101', 'S0102', 'S0103', 'S0104', 'S0105',
    'S0201', 'S0202', 'S0203', 'S0204', 'S0205', 'S0206',
    'S0301', 'S0302', 'S0303', 'S0304', 'S0305'
);
