CREATE TABLE if NOT EXISTS student (
    id serial PRIMARY KEY,
    student_id INTEGER NOT NULL UNIQUE,
    name varchar(120) NOT NULL
);

CREATE TABLE if NOT EXISTS school (
    id serial PRIMARY KEY,
    name varchar(120) NOT NULL,
    deviation INTEGER NOT NULL,
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
    deviation_japanese INTEGER,
    deviation_math INTEGER,
    deviation_english INTEGER,
    deviation_science INTEGER,
    deviation_socialstudies INTEGER,
    deviation_three INTEGER,
    deviation_five INTEGER,
    saitama_deviation_three INTEGER,
    saitama_deviation_five INTEGER,
    first_choice INTEGER REFERENCES school(id),
    second_choice INTEGER REFERENCES school(id),
    third_choice INTEGER REFERENCES school(id)
);
