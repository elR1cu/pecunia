-- Baseline migration. Pins the database default timezone to UTC so that
-- timestamp arithmetic, now() and current_timestamp behave identically in
-- dev, test (Testcontainers) and prod regardless of the host's locale.
-- current_database() is used instead of a hardcoded name so the migration
-- applies to whichever database Flyway is connected to.
DO $$
BEGIN
    EXECUTE format('ALTER DATABASE %I SET timezone TO ''UTC''', current_database());
END $$;
