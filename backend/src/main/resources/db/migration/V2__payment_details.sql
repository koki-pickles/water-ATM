-- Why a payment ended (e.g. "Request cancelled by user"), and fast lookups for the expiry job.
ALTER TABLE payment ADD COLUMN result_description VARCHAR(255);

CREATE INDEX idx_payment_order ON payment (order_id);
CREATE INDEX idx_payment_status_created ON payment (status, created_at);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);
