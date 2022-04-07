CREATE TABLE t_foreign_rules
(
    pk_id SERIAL NOT NULL,
    country CHAR(2) NOT NULL ,
    rule_id VARCHAR(64) NOT NULL,
    rule_version VARCHAR(32) NOT NULL,
    rule_content TEXT NOT NULL,
    valid_from timestamp with time zone,
    valid_until timestamp with time zone,
    inserted_at timestamp with time zone,
    CONSTRAINT rules_pk_id PRIMARY KEY (pk_id)
);
