-- ===================================================
-- Phase 5: Create Alerts and Notifications Schema
-- ===================================================

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    brand_id UUID NOT NULL,
    threat_id UUID,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    sent_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    CONSTRAINT fk_alerts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_threat FOREIGN KEY (threat_id) REFERENCES threats(id) ON DELETE SET NULL
);

CREATE INDEX idx_alerts_user_id ON alerts(user_id);
CREATE INDEX idx_alerts_brand_id ON alerts(brand_id);
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_severity ON alerts(severity);
CREATE INDEX idx_alerts_sent_at ON alerts(sent_at DESC);
