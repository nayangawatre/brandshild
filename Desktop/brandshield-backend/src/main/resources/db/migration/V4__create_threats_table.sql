-- ===================================================
-- Phase 4: Create Threats and Threat Detection Schema
-- ===================================================

CREATE TABLE threats (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    threat_type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DETECTED',
    target_value VARCHAR(500) NOT NULL,
    source VARCHAR(50) NOT NULL,
    threat_score INTEGER NOT NULL,
    details TEXT,
    detected_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_threats_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE CASCADE,
    CONSTRAINT uq_threat_brand_target UNIQUE (brand_id, target_value)
);

CREATE INDEX idx_threats_brand_id ON threats(brand_id);
CREATE INDEX idx_threats_severity ON threats(severity);
CREATE INDEX idx_threats_status ON threats(status);
CREATE INDEX idx_threats_threat_score ON threats(threat_score);
CREATE INDEX idx_threats_detected_at ON threats(detected_at DESC);
