-- ===================================================
-- Phase 3: Create Brands and Brand Assets Schema
-- ===================================================

CREATE TABLE brands (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    industry VARCHAR(100),
    official_website VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_brands_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_brand_name_per_user UNIQUE (user_id, name)
);

CREATE INDEX idx_brands_user_id ON brands(user_id);
CREATE INDEX idx_brands_status ON brands(status);

CREATE TABLE brand_keywords (
    brand_id UUID NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    PRIMARY KEY (brand_id, keyword),
    CONSTRAINT fk_brand_keywords_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE CASCADE
);

CREATE INDEX idx_brand_keywords_brand_id ON brand_keywords(brand_id);

CREATE TABLE brand_official_domains (
    brand_id UUID NOT NULL,
    domain VARCHAR(255) NOT NULL,
    PRIMARY KEY (brand_id, domain),
    CONSTRAINT fk_brand_official_domains_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE CASCADE
);

CREATE INDEX idx_brand_official_domains_brand_id ON brand_official_domains(brand_id);
