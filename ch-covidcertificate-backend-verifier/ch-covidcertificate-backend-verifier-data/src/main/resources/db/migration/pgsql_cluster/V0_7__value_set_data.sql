CREATE TABLE t_value_set_data
(
    pk_value_set_data_id serial                   NOT NULL,
    value_set_id         character varying(100)   NOT NULL,
    value_set_date       date                     NOT NULL,
    created_at           timestamp with time zone NOT NULL DEFAULT now(),
    json_blob            text                     NOT NULL,
    CONSTRAINT PK_t_value_set_data PRIMARY KEY (pk_value_set_data_id)
);

CREATE INDEX idx_value_set_data_created_at ON t_value_set_data (created_at);
CREATE INDEX idx_value_set_data_value_set_id ON t_value_set_data (value_set_id);
