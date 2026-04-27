CREATE SCHEMA IF NOT EXISTS private;

CREATE TABLE IF NOT EXISTS missions (
    id UUID PRIMARY KEY,
    stage SMALLINT NOT NULL CHECK (stage IN (1, 2, 3)),
    title TEXT NOT NULL,
    description TEXT,
    unlock_condition TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS question_bank (
    id UUID PRIMARY KEY,
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    question_type VARCHAR(20) NOT NULL,
    prompt TEXT NOT NULL,
    options_json TEXT,
    source_type VARCHAR(20) NOT NULL DEFAULT 'STATIC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE question_bank
    ADD COLUMN IF NOT EXISTS mission_id UUID;

ALTER TABLE question_bank
    ADD COLUMN IF NOT EXISTS question_type VARCHAR(20);

ALTER TABLE question_bank
    ADD COLUMN IF NOT EXISTS prompt TEXT;

ALTER TABLE question_bank
    ADD COLUMN IF NOT EXISTS options_json TEXT;

ALTER TABLE question_bank
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT 'STATIC';

ALTER TABLE question_bank
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE question_bank
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS private.question_answer_keys (
    question_id UUID PRIMARY KEY REFERENCES question_bank(id) ON DELETE CASCADE,
    answer_payload TEXT NOT NULL,
    explanation TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    question_ids_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_child_created_at
    ON quiz_attempts (child_id, created_at DESC);

CREATE TABLE IF NOT EXISTS mission_attempts (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    attempt_date DATE NOT NULL,
    attempt_no INT NOT NULL CHECK (attempt_no >= 1),
    score INT NOT NULL CHECK (score >= 0),
    total INT NOT NULL CHECK (total > 0),
    xp_earned INT NOT NULL CHECK (xp_earned >= 0),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (child_id, mission_id, attempt_date, attempt_no),
    CHECK (score <= total)
);

CREATE INDEX IF NOT EXISTS idx_mission_attempts_child_date
    ON mission_attempts (child_id, attempt_date);

INSERT INTO missions (id, stage, title, description, unlock_condition, is_active) VALUES
    ('11111111-1111-1111-1111-111111111111', 1, 'AI Basics', 'First mission about what AI is', NULL, TRUE),
    ('11111111-1111-1111-1111-111111111112', 1, 'Check AI Answers', 'Practice verifying AI output', NULL, TRUE),
    ('11111111-1111-1111-1111-111111111113', 1, 'Ask Better Questions', 'Learn how to write better prompts', NULL, TRUE),
    ('22222222-2222-2222-2222-222222222221', 2, 'Prompt Refinement', 'Advanced mission for more specific prompts', 'Complete 3 stage 1 missions', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_bank (id, mission_id, question_type, prompt, options_json, source_type, is_active) VALUES
    ('aaaaaaa1-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'OX', 'AI is always correct.', NULL, 'STATIC', TRUE),
    ('aaaaaaa1-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'MULTIPLE', 'What should you do first if an AI answer looks strange?', '["Check another source","Submit it right away","Only trust a friend"]', 'STATIC', TRUE),
    ('aaaaaaa1-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'FILL', 'A sentence used to ask AI for something specific is called a _____.', '["prompt","password","ad"]', 'STATIC', TRUE),
    ('aaaaaaa1-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'SITUATION', 'A friend wants to submit AI output as homework. What is the best advice?', '["Use it as a hint and rewrite it","Submit it as-is","Say nothing"]', 'STATIC', TRUE),
    ('aaaaaaa1-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'MULTIPLE', 'What is the best attitude when using AI?', '["Check critically","Believe everything","Type private data"]', 'STATIC', TRUE),

    ('aaaaaaa2-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111112', 'OX', 'If the internet and AI disagree, you should just trust AI.', NULL, 'STATIC', TRUE),
    ('aaaaaaa2-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111112', 'MULTIPLE', 'What is a good way to verify an AI answer?', '["Look up an official site","Read it once and stop","Only ask a friend"]', 'STATIC', TRUE),
    ('aaaaaaa2-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111112', 'FILL', 'When AI says something false like it is true, that can be called a _____.', '["hallucination","review","security"]', 'STATIC', TRUE),
    ('aaaaaaa2-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111112', 'SITUATION', 'AI gave the wrong date. What should you do?', '["Check a calendar or search again","Trust it anyway","Spread it to friends"]', 'STATIC', TRUE),
    ('aaaaaaa2-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111112', 'MULTIPLE', 'Why is verification important?', '["AI can make mistakes","AI is perfect","Verification wastes time"]', 'STATIC', TRUE),

    ('aaaaaaa3-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111113', 'OX', 'Specific questions usually lead to more useful AI answers.', NULL, 'STATIC', TRUE),
    ('aaaaaaa3-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111113', 'MULTIPLE', 'Which is the best example of a good question?', '["Explain it in 3 easy sentences for an elementary student","Say anything","Fast"]', 'STATIC', TRUE),
    ('aaaaaaa3-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111113', 'FILL', 'If you tell AI the output format you want, the answer can be more _____.', '["custom","random","unsafe"]', 'STATIC', TRUE),
    ('aaaaaaa3-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111113', 'SITUATION', 'The explanation is too long and hard. What should you ask next?', '["Explain it again with shorter and easier words","Stop reading","Send private data"]', 'STATIC', TRUE),
    ('aaaaaaa3-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111113', 'MULTIPLE', 'What is good to include in a prompt?', '["Audience and goal","Password","Home address"]', 'STATIC', TRUE),

    ('bbbbbbb1-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222221', 'OX', 'Adding constraints to a prompt can improve the result.', NULL, 'STATIC', TRUE),
    ('bbbbbbb1-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222221', 'MULTIPLE', 'Which request is the most specific?', '["Summarize it in 5 lines for a school presentation","Explain it","Do it somehow"]', 'STATIC', TRUE),
    ('bbbbbbb1-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222221', 'FILL', 'When you specify length, format, and audience, it is easier to get the _____ result.', '["desired","random","unsafe"]', 'STATIC', TRUE),
    ('bbbbbbb1-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222221', 'SITUATION', 'The AI answer is too hard. What is the best follow-up?', '["Explain it again for an elementary student","Make it much longer","Add a friends name"]', 'STATIC', TRUE),
    ('bbbbbbb1-0000-0000-0000-000000000005', '22222222-2222-2222-2222-222222222221', 'MULTIPLE', 'Which is not part of a good prompt?', '["Private data","A clear goal","A response format"]', 'STATIC', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO private.question_answer_keys (question_id, answer_payload, explanation) VALUES
    ('aaaaaaa1-0000-0000-0000-000000000001', '"false"', 'AI can be wrong, so answers should be checked.'),
    ('aaaaaaa1-0000-0000-0000-000000000002', '"Check another source"', 'Cross-checking with another source is a safe habit.'),
    ('aaaaaaa1-0000-0000-0000-000000000003', '"prompt"', 'A prompt is the request you send to AI.'),
    ('aaaaaaa1-0000-0000-0000-000000000004', '"Use it as a hint and rewrite it"', 'AI should help with ideas, not replace your own work.'),
    ('aaaaaaa1-0000-0000-0000-000000000005', '"Check critically"', 'AI is useful, but you should not trust it blindly.'),

    ('aaaaaaa2-0000-0000-0000-000000000001', '"false"', 'When sources disagree, verify with reliable information.'),
    ('aaaaaaa2-0000-0000-0000-000000000002', '"Look up an official site"', 'Official or trustworthy sources are best for checking facts.'),
    ('aaaaaaa2-0000-0000-0000-000000000003', '"hallucination"', 'Hallucination means AI presents false information confidently.'),
    ('aaaaaaa2-0000-0000-0000-000000000004', '"Check a calendar or search again"', 'Dates and numbers should be verified carefully.'),
    ('aaaaaaa2-0000-0000-0000-000000000005', '"AI can make mistakes"', 'Verification matters because AI is not perfect.'),

    ('aaaaaaa3-0000-0000-0000-000000000001', '"true"', 'Specific prompts give AI better context.'),
    ('aaaaaaa3-0000-0000-0000-000000000002', '"Explain it in 3 easy sentences for an elementary student"', 'A target audience and format make the prompt clearer.'),
    ('aaaaaaa3-0000-0000-0000-000000000003', '"custom"', 'Giving format guidance helps AI tailor its answer.'),
    ('aaaaaaa3-0000-0000-0000-000000000004', '"Explain it again with shorter and easier words"', 'You can ask AI to adjust the explanation level.'),
    ('aaaaaaa3-0000-0000-0000-000000000005', '"Audience and goal"', 'Audience and goal help AI shape the response.'),

    ('bbbbbbb1-0000-0000-0000-000000000001', '"true"', 'Prompt constraints can move the result closer to what you want.'),
    ('bbbbbbb1-0000-0000-0000-000000000002', '"Summarize it in 5 lines for a school presentation"', 'This request clearly defines format and purpose.'),
    ('bbbbbbb1-0000-0000-0000-000000000003', '"desired"', 'The more clearly you specify the output, the easier it is to get the desired result.'),
    ('bbbbbbb1-0000-0000-0000-000000000004', '"Explain it again for an elementary student"', 'Changing audience level is a good follow-up request.'),
    ('bbbbbbb1-0000-0000-0000-000000000005', '"Private data"', 'Private data should never be part of a prompt.')
ON CONFLICT (question_id) DO NOTHING;
