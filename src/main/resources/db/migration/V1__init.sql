CREATE TABLE asset (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       customer_id VARCHAR(50) NOT NULL,
                       asset_name VARCHAR(50) NOT NULL,
                       size BIGINT NOT NULL,
                       usable_size BIGINT NOT NULL,
                       version BIGINT DEFAULT 0,
                       CONSTRAINT uq_asset UNIQUE (customer_id, asset_name)
);

CREATE TABLE orders (
                        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        customer_id VARCHAR(50) NOT NULL,
                        asset_name VARCHAR(50) NOT NULL,
                        side VARCHAR(10) NOT NULL,
                        size BIGINT NOT NULL,
                        price BIGINT NOT NULL,
                        status VARCHAR(15) NOT NULL,
                        create_date TIMESTAMP NOT NULL,
                        version BIGINT DEFAULT 0
);