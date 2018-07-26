DROP TABLE IF EXISTS "balance";

DROP TABLE IF EXISTS "order";

CREATE TABLE IF NOT EXISTS "balance" (
  id BIGINT NOT NULL,
  token VARCHAR(10) NOT NULL,
  user_id BIGINT NOT NULL,
  available DECIMAL NOT NULL,
  withdraw DECIMAL NOT NULL,

  CONSTRAINT pk_t_balance PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS i_balance_user_id ON "balance" (user_id);
CREATE INDEX IF NOT EXISTS i_balance_token ON "balance" (token);

CREATE SEQUENCE IF NOT EXISTS s_balnce_id;


CREATE TABLE IF NOT EXISTS "order" (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  token VARCHAR(10) NOT NULL,
  status VARCHAR(10) NOT NULL,
  amount DECIMAL NOT NULL,

  CONSTRAINT pk_t_order PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS i_order_user_id ON "order" (user_id);
CREATE INDEX IF NOT EXISTS i_order_token ON "order" (token);
CREATE SEQUENCE IF NOT EXISTS s_order_id;
