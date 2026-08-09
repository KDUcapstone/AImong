DO $$ BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'parent_custom_quests'
          AND column_name = 'quest_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'parent_custom_quests'
          AND column_name = 'id'
    ) THEN
        ALTER TABLE public.parent_custom_quests RENAME COLUMN quest_id TO id;
    END IF;
END $$;
