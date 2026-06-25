-- ==========================================
-- Add archive.payments_archive table for DLM worker
-- ==========================================

CREATE TABLE IF NOT EXISTS archive.payments_archive (
    id UUID PRIMARY KEY,
<<<<<<< HEAD
    ride_id NUMERIC NOT NULL,
    user_id NUMERIC NOT NULL,
=======
    ride_id UUID NOT NULL,
    user_id UUID NOT NULL,
>>>>>>> parent of bf41481 (Fixed database directory files)
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
<<<<<<< HEAD

CREATE INDEX IF NOT EXISTS idx_archive_payments_ride_id ON archive.payments_archive(ride_id);
CREATE INDEX IF NOT EXISTS idx_archive_payments_user_id ON archive.payments_archive(user_id);
CREATE INDEX IF NOT EXISTS idx_archive_payments_status ON archive.payments_archive(status);
=======
>>>>>>> parent of bf41481 (Fixed database directory files)
