DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'profile_image_type_enum'
    ) THEN
        CREATE TYPE profile_image_type_enum AS ENUM (
            'DEFAULT',
            'SPROUT',
            'EXPLORER',
            'CRITIC',
            'GUARDIAN'
        );
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'child_profiles'
          AND column_name = 'profile_image_type'
          AND udt_name <> 'profile_image_type_enum'
    ) THEN
        ALTER TABLE child_profiles
            ALTER COLUMN profile_image_type DROP DEFAULT;

        ALTER TABLE child_profiles
            ALTER COLUMN profile_image_type TYPE profile_image_type_enum
            USING profile_image_type::profile_image_type_enum;
    END IF;
END $$;

ALTER TABLE child_profiles
    ALTER COLUMN profile_image_type SET DEFAULT 'DEFAULT'::profile_image_type_enum;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'question_bank'
          AND column_name = 'options_json'
          AND data_type <> 'jsonb'
    ) THEN
        ALTER TABLE question_bank
            ALTER COLUMN options_json TYPE JSONB
            USING options_json::jsonb;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'private'
          AND table_name = 'question_answer_keys'
          AND column_name = 'answer_payload'
          AND data_type <> 'jsonb'
    ) THEN
        ALTER TABLE private.question_answer_keys
            ALTER COLUMN answer_payload TYPE JSONB
            USING answer_payload::jsonb;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'quiz_attempts'
          AND column_name = 'question_ids_json'
          AND data_type <> 'jsonb'
    ) THEN
        ALTER TABLE quiz_attempts
            ALTER COLUMN question_ids_json TYPE JSONB
            USING question_ids_json::jsonb;
    END IF;
END $$;
