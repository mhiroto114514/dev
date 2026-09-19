-- Run once on existing databases. Safe to run again; existing rows are retained.
BEGIN;
ALTER TABLE public.result ADD COLUMN IF NOT EXISTS fourth_choice INTEGER REFERENCES public.school(id);
ALTER TABLE public.result ADD COLUMN IF NOT EXISTS fifth_choice INTEGER REFERENCES public.school(id);
COMMIT;
