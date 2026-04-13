DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pet_grade_enum') THEN
        CREATE TYPE pet_grade_enum AS ENUM ('NORMAL', 'RARE', 'EPIC', 'LEGEND');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pet_stage_enum') THEN
        CREATE TYPE pet_stage_enum AS ENUM ('EGG', 'GROWTH', 'AIMONG');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pet_mood_enum') THEN
        CREATE TYPE pet_mood_enum AS ENUM ('HAPPY', 'IDLE', 'SAD_LIGHT', 'SAD_DEEP');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'crown_type_enum') THEN
        CREATE TYPE crown_type_enum AS ENUM ('silver', 'gold', 'jewel', 'shining');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ticket_type_enum') THEN
        CREATE TYPE ticket_type_enum AS ENUM ('NORMAL', 'RARE', 'EPIC');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS pets (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    pet_type TEXT NOT NULL,
    grade pet_grade_enum NOT NULL,
    xp INT NOT NULL DEFAULT 0 CHECK (xp >= 0),
    stage pet_stage_enum NOT NULL DEFAULT 'EGG',
    mood pet_mood_enum NOT NULL DEFAULT 'IDLE',
    crown_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    crown_type crown_type_enum,
    obtained_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id, child_id),
    UNIQUE (child_id, pet_type),
    CHECK (
        (crown_unlocked = FALSE AND crown_type IS NULL)
        OR (crown_unlocked = TRUE AND crown_type IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS equipped_pets (
    child_id UUID PRIMARY KEY REFERENCES child_profiles(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL UNIQUE,
    equipped_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_equipped_pet_child_pet FOREIGN KEY (pet_id, child_id)
        REFERENCES pets(id, child_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS fragments (
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    grade pet_grade_enum NOT NULL,
    count INT NOT NULL DEFAULT 0 CHECK (count >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (child_id, grade)
);

CREATE TABLE IF NOT EXISTS gacha_pulls (
    id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    ticket_type ticket_type_enum NOT NULL,
    result_pet_code TEXT NOT NULL,
    grade pet_grade_enum NOT NULL,
    is_new BOOLEAN NOT NULL,
    granted_pet_id UUID,
    fragments_got INT NOT NULL DEFAULT 0 CHECK (fragments_got >= 0),
    sr_miss_before INT NOT NULL DEFAULT 0 CHECK (sr_miss_before >= 0),
    pulled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_gacha_pull_child_pet FOREIGN KEY (granted_pet_id, child_id)
        REFERENCES pets(id, child_id),
    CHECK (
        (is_new = TRUE AND granted_pet_id IS NOT NULL AND fragments_got = 0)
        OR (is_new = FALSE AND granted_pet_id IS NULL AND fragments_got > 0)
    )
);
