-- Install only on the disposable JMeter database. Slow writes enough for the requests to overlap.
CREATE TABLE lab2_isolation_observation (
    name TEXT NOT NULL,
    operation TEXT NOT NULL,
    isolation TEXT NOT NULL
);

CREATE FUNCTION lab2_observe_write() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF lower(btrim(NEW.name)) LIKE 'lab2-%' THEN
        IF TG_OP = 'INSERT' OR NEW.name IS DISTINCT FROM OLD.name THEN
            PERFORM pg_sleep(0.10);
        END IF;
        INSERT INTO lab2_isolation_observation
        VALUES (NEW.name, TG_OP, current_setting('transaction_isolation'));
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER lab2_observe_write BEFORE INSERT OR UPDATE ON human_being
FOR EACH ROW EXECUTE FUNCTION lab2_observe_write();
