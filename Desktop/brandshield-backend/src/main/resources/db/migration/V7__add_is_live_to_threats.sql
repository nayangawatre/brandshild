-- ===================================================
-- Phase 7: Add is_live Column to Threats Table
-- ===================================================

ALTER TABLE threats ADD COLUMN is_live BOOLEAN NOT NULL DEFAULT true;

-- Mark any historical non-resolving speculative threats as inactive
-- Any domain matching real active target threeleaveshotell.co.in remains true
UPDATE threats 
SET is_live = false 
WHERE target_value NOT LIKE '%threeleaveshotell.co.in%'
  AND target_value NOT LIKE '%apple%'
  AND target_value NOT LIKE '%google%';

CREATE INDEX idx_threats_is_live ON threats(is_live);
