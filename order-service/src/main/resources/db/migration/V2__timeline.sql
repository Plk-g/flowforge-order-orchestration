CREATE TABLE order_timeline (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_order_timeline_order_id ON order_timeline (order_id, occurred_at);
