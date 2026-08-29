-- HTTP session storage for the BFF (ADR-0038, supersedes ADR-0011: Redis).
--
-- The DDL below is a verbatim copy of Spring Session's official PostgreSQL
-- schema, `org/springframework/session/jdbc/schema-postgresql.sql`, as shipped
-- in spring-session-jdbc 4.1.1. It is reproduced here -- rather than executed
-- by `spring.session.jdbc.initialize-schema` -- so that Flyway remains the
-- single owner of the schema, consistent with every other table in this
-- database. `initialize-schema: never` in application.yml enforces that.
--
-- Do NOT reformat or rename anything: JdbcIndexedSessionRepository issues
-- hard-coded SQL against these exact table, column and index names. When
-- spring-session-jdbc is upgraded, diff the jar's schema-postgresql.sql
-- against this file and add a new migration if it changed.
--
-- Unlike the business tables, these rows are ephemeral infrastructure: they
-- hold no user-owned data model, carry no `owner_id`, and are excluded from
-- any data-retention reasoning. Expired rows are deleted by the periodic
-- cleanup job of spring-session-jdbc, not by application code.

CREATE TABLE SPRING_SESSION (
	PRIMARY_ID CHAR(36) NOT NULL,
	SESSION_ID CHAR(36) NOT NULL,
	CREATION_TIME BIGINT NOT NULL,
	LAST_ACCESS_TIME BIGINT NOT NULL,
	MAX_INACTIVE_INTERVAL INT NOT NULL,
	EXPIRY_TIME BIGINT NOT NULL,
	PRINCIPAL_NAME VARCHAR(100),
	CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
	SESSION_PRIMARY_ID CHAR(36) NOT NULL,
	ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
	ATTRIBUTE_BYTES BYTEA NOT NULL,
	CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
	CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
