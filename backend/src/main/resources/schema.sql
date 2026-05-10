CREATE TABLE if NOT EXISTS student (
    id serial PRIMARY KEY,
    student_id INTEGER NOT NULL UNIQUE,
    name varchar(20) NOT NULL
);

CREATE TABLE if NOT EXISTS school (
    id serial PRIMARY KEY,
    name varchar(120) NOT NULL,
    deviation INTEGER NOT NULL
);

ALTER TABLE school
    ALTER COLUMN name TYPE varchar(120);

CREATE TABLE if NOT EXISTS result (
    id serial PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES student(id),
    times INTEGER NOT NULL,
    japanese INTEGER,
    math INTEGER,
    english INTEGER,
    science INTEGER,
    socialscience INTEGER,
    deviation_japanese INTEGER,
    deviation_math INTEGER,
    deviation_english INTEGER,
    deviation_science INTEGER,
    deviation_socialscience INTEGER,
    deviation_three INTEGER,
    deviation_five INTEGER,
    first_choice INTEGER REFERENCES school(id),
    second_choice INTEGER REFERENCES school(id),
    third_choice INTEGER REFERENCES school(id)
);
