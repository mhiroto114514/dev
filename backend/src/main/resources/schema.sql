CREATE TABLE if NOT EXISTS student (
    id serial PRIMARY KEY,
    student_id INTEGER NOT NULL,
    name varchar(20) NOT NULL
);

CREATE TABLE if NOT EXISTS school (
    id serial PRIMARY KEY,
    name varchar(20) NOT NULL,
    deviation INTEGER NOT NULL
);

CREATE TABLE if NOT EXISTS result (
    id serial PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES student(id),
    japanese INTEGER,
    math INTEGER,
    english INTEGER,
    science INTEGER,
    socialscience INTEGER,
    deviation_three INTEGER,
    deviation_five INTEGER,
    fisrt_choice INTEGER REFERENCES school(id),
    second_choice INTEGER REFERENCES school(id),
    third_choice INTEGER REFERENCES school(id)
);
