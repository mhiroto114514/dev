CREATE TABLE if NOT EXISTS student (
    id serial PRIMARY KEY,
    student_id INTEGER NOT NULL UNIQUE,
    name varchar(120) NOT NULL
);

CREATE TABLE if NOT EXISTS school (
    id serial PRIMARY KEY,
    name varchar(120) NOT NULL,
    deviation NUMERIC(4,1) NOT NULL,
    school_category varchar(10) NOT NULL DEFAULT 'PUBLIC'
);
CREATE TABLE if NOT EXISTS result (
    id serial PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES student(id),
    times INTEGER NOT NULL,
    japanese INTEGER,
    math INTEGER,
    english INTEGER,
    science INTEGER,
    socialstudies INTEGER,
    deviation_japanese NUMERIC(4,1),
    deviation_math NUMERIC(4,1),
    deviation_english NUMERIC(4,1),
    deviation_science NUMERIC(4,1),
    deviation_socialstudies NUMERIC(4,1),
    deviation_three NUMERIC(4,1),
    deviation_five NUMERIC(4,1),
    saitama_deviation_three NUMERIC(4,1),
    saitama_deviation_five NUMERIC(4,1),
    first_choice INTEGER REFERENCES school(id),
    second_choice INTEGER REFERENCES school(id),
    third_choice INTEGER REFERENCES school(id)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'school' AND column_name = 'deviation'
    ) THEN
        EXECUTE 'ALTER TABLE school ALTER COLUMN deviation TYPE NUMERIC(4,1) USING ROUND(deviation::numeric, 1)';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'deviation_japanese'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN deviation_japanese TYPE NUMERIC(4,1) USING ROUND(deviation_japanese::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'deviation_math'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN deviation_math TYPE NUMERIC(4,1) USING ROUND(deviation_math::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'deviation_english'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN deviation_english TYPE NUMERIC(4,1) USING ROUND(deviation_english::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'deviation_science'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN deviation_science TYPE NUMERIC(4,1) USING ROUND(deviation_science::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'deviation_socialstudies'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN deviation_socialstudies TYPE NUMERIC(4,1) USING ROUND(deviation_socialstudies::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'deviation_socialscience'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN deviation_socialscience TYPE NUMERIC(4,1) USING ROUND(deviation_socialscience::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'deviation_three'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN deviation_three TYPE NUMERIC(4,1) USING ROUND(deviation_three::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'deviation_five'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN deviation_five TYPE NUMERIC(4,1) USING ROUND(deviation_five::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'saitama_deviation_three'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN saitama_deviation_three TYPE NUMERIC(4,1) USING ROUND(saitama_deviation_three::numeric, 1)';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'result' AND column_name = 'saitama_deviation_five'
    ) THEN
        EXECUTE 'ALTER TABLE result ALTER COLUMN saitama_deviation_five TYPE NUMERIC(4,1) USING ROUND(saitama_deviation_five::numeric, 1)';
    END IF;
END $$;
