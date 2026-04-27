ALTER TABLE public.missions
    ADD COLUMN IF NOT EXISTS mission_code VARCHAR(16);

UPDATE public.missions
SET mission_code = CASE
    WHEN stage = 1 AND title = 'AI는 어디에 있을까?' THEN 'S0101'
    WHEN stage = 1 AND title = 'AI와 계산기는 어떻게 다를까?' THEN 'S0102'
    WHEN stage = 1 AND title = 'AI는 데이터를 보고 배워요' THEN 'S0103'
    WHEN stage = 1 AND title = '딥러닝과 인식' THEN 'S0104'
    WHEN stage = 1 AND title = 'AI도 틀릴 수 있어요' THEN 'S0105'
    WHEN stage = 2 AND title = '좋은 질문은 목적이 보여요' THEN 'S0201'
    WHEN stage = 2 AND title = '좋은 질문은 조건이 구체적이에요' THEN 'S0202'
    WHEN stage = 2 AND title = '개인정보와 생체정보를 지켜요' THEN 'S0203'
    WHEN stage = 2 AND title = '사진·음성·데이터를 바르게 모아요' THEN 'S0204'
    WHEN stage = 2 AND title = 'AI 도구를 실험하고 고쳐요' THEN 'S0205'
    WHEN stage = 2 AND title = 'AI 도움을 받고 내 답으로 정리해요' THEN 'S0206'
    WHEN stage = 3 AND title = '팩트체크 첫걸음' THEN 'S0301'
    WHEN stage = 3 AND title = '출처와 근거를 비교해요' THEN 'S0302'
    WHEN stage = 3 AND title = '편향을 찾아봐요' THEN 'S0303'
    WHEN stage = 3 AND title = 'AI의 양면성을 생각해요' THEN 'S0304'
    WHEN stage = 3 AND title = '딜레마와 공정한 선택' THEN 'S0305'
    ELSE mission_code
END
WHERE mission_code IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_missions_mission_code
    ON public.missions (mission_code)
    WHERE mission_code IS NOT NULL;
