-- =============================================================================
-- GivingHands — manual schema repair (SQL Server)
-- Run this script in SSMS against givinghands_db when you see:
--   "Invalid column name 'allow_volunteers'"
--
-- This script is IDEMPOTENT: safe to run multiple times.
-- Does NOT drop data. Only adds missing columns and relaxes status checks.
-- =============================================================================
USE givinghands_db;
GO

PRINT '=== Fixing users table ===';

IF COL_LENGTH('users', 'google_id') IS NULL
    ALTER TABLE users ADD google_id NVARCHAR(255) NULL;
GO

IF COL_LENGTH('users', 'oauth_provider') IS NULL
    ALTER TABLE users ADD oauth_provider NVARCHAR(50) NULL;
GO

IF COL_LENGTH('users', 'avatar_url') IS NULL
    ALTER TABLE users ADD avatar_url NVARCHAR(500) NULL;
GO

IF COL_LENGTH('users', 'cover_url') IS NULL
    ALTER TABLE users ADD cover_url NVARCHAR(500) NULL;
GO

PRINT '=== Fixing campaigns table ===';

IF COL_LENGTH('campaigns', 'category') IS NULL
    ALTER TABLE campaigns ADD category NVARCHAR(100) NULL;
GO

IF COL_LENGTH('campaigns', 'organization_id') IS NULL
    ALTER TABLE campaigns ADD organization_id BIGINT NULL;
GO

IF COL_LENGTH('campaigns', 'image_path') IS NULL
    ALTER TABLE campaigns ADD image_path NVARCHAR(500) NULL;
GO

IF COL_LENGTH('campaigns', 'allow_volunteers') IS NULL
BEGIN
    ALTER TABLE campaigns ADD allow_volunteers BIT NULL;
    UPDATE campaigns SET allow_volunteers = 1 WHERE allow_volunteers IS NULL;
    ALTER TABLE campaigns ALTER COLUMN allow_volunteers BIT NOT NULL;
    IF NOT EXISTS (
        SELECT 1 FROM sys.default_constraints
        WHERE parent_object_id = OBJECT_ID('campaigns')
          AND COL_NAME(parent_object_id, parent_column_id) = 'allow_volunteers'
    )
        ALTER TABLE campaigns ADD CONSTRAINT DF_campaigns_allow_volunteers DEFAULT 1 FOR allow_volunteers;
END
GO

-- Replace outdated status CHECK (old scripts used APPROVED only, entity uses ACTIVE)
DECLARE @campaignStatusConstraint NVARCHAR(256);
SELECT @campaignStatusConstraint = cc.name
FROM sys.check_constraints cc
INNER JOIN sys.columns c ON c.object_id = cc.parent_object_id AND c.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID('campaigns') AND c.name = 'status';

IF @campaignStatusConstraint IS NOT NULL
BEGIN
    DECLARE @dropCampaignStatus NVARCHAR(400) =
        N'ALTER TABLE campaigns DROP CONSTRAINT ' + QUOTENAME(@campaignStatusConstraint);
    EXEC sp_executesql @dropCampaignStatus;
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('campaigns') AND name = 'ck_campaigns_status'
)
    ALTER TABLE campaigns ADD CONSTRAINT ck_campaigns_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'APPROVED', 'REJECTED'));
GO

PRINT '=== Fixing volunteers table ===';

IF COL_LENGTH('volunteers', 'why_join') IS NULL
    ALTER TABLE volunteers ADD why_join NVARCHAR(MAX) NULL;
GO

IF COL_LENGTH('volunteers', 'skills') IS NULL
    ALTER TABLE volunteers ADD skills NVARCHAR(MAX) NULL;
GO

IF COL_LENGTH('volunteers', 'availability') IS NULL
    ALTER TABLE volunteers ADD availability NVARCHAR(100) NULL;
GO

IF COL_LENGTH('volunteers', 'experience') IS NULL
    ALTER TABLE volunteers ADD experience NVARCHAR(MAX) NULL;
GO

IF COL_LENGTH('volunteers', 'phone') IS NULL
    ALTER TABLE volunteers ADD phone NVARCHAR(50) NULL;
GO

DECLARE @volunteerStatusConstraint NVARCHAR(256);
SELECT @volunteerStatusConstraint = cc.name
FROM sys.check_constraints cc
INNER JOIN sys.columns c ON c.object_id = cc.parent_object_id AND c.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID('volunteers') AND c.name = 'status';

IF @volunteerStatusConstraint IS NOT NULL
BEGIN
    DECLARE @dropVolunteerStatus NVARCHAR(400) =
        N'ALTER TABLE volunteers DROP CONSTRAINT ' + QUOTENAME(@volunteerStatusConstraint);
    EXEC sp_executesql @dropVolunteerStatus;
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('volunteers') AND name = 'ck_volunteers_status'
)
    ALTER TABLE volunteers ADD CONSTRAINT ck_volunteers_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'ACCEPTED'));
GO

PRINT '=== Verifying required columns ===';

SELECT TABLE_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE (TABLE_NAME = 'campaigns' AND COLUMN_NAME IN ('allow_volunteers', 'image_path', 'category', 'organization_id'))
   OR (TABLE_NAME = 'users' AND COLUMN_NAME IN ('google_id', 'oauth_provider', 'avatar_url', 'cover_url'))
ORDER BY TABLE_NAME, COLUMN_NAME;

PRINT '=== Done. Restart the Spring Boot application. ===';
GO
