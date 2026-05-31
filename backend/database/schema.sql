-- =============================================================================
-- GivingHands — full database schema (SQL Server)
-- Matches JPA entities. Run on a fresh database only.
-- For an existing outdated database, use fix-schema.sql instead.
-- =============================================================================
USE givinghands_db;
GO

-- -----------------------------------------------------------------------------
-- users
-- -----------------------------------------------------------------------------
IF OBJECT_ID('users', 'U') IS NULL
BEGIN
    CREATE TABLE users (
        id              BIGINT IDENTITY(1,1) PRIMARY KEY,
        name            NVARCHAR(100)  NOT NULL,
        email           NVARCHAR(150)  NOT NULL,
        password        NVARCHAR(255)  NULL,
        role            NVARCHAR(20)   NOT NULL,
        google_id       NVARCHAR(255)  NULL,
        oauth_provider  NVARCHAR(50)   NULL,
        avatar_url      NVARCHAR(500)  NULL,
        cover_url       NVARCHAR(500)  NULL,
        CONSTRAINT uq_users_email UNIQUE (email),
        CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ORGANIZATION', 'ADMIN'))
    );
END
GO

-- -----------------------------------------------------------------------------
-- campaigns (must match Campaign.java)
-- -----------------------------------------------------------------------------
IF OBJECT_ID('campaigns', 'U') IS NULL
BEGIN
    CREATE TABLE campaigns (
        id                BIGINT IDENTITY(1,1) PRIMARY KEY,
        title             NVARCHAR(200)   NOT NULL,
        description       NVARCHAR(MAX)   NOT NULL,
        category          NVARCHAR(100)   NULL,
        goal_amount       FLOAT           NOT NULL,
        current_amount    FLOAT           NOT NULL CONSTRAINT DF_campaigns_current_amount DEFAULT 0,
        deadline          DATE            NOT NULL,
        status            NVARCHAR(20)    NOT NULL CONSTRAINT DF_campaigns_status DEFAULT 'PENDING',
        organization_id   BIGINT          NULL,
        image_path        NVARCHAR(500)   NULL,
        allow_volunteers  BIT             NOT NULL CONSTRAINT DF_campaigns_allow_volunteers DEFAULT 1,
        creator_id        BIGINT          NOT NULL,
        CONSTRAINT fk_campaigns_creator FOREIGN KEY (creator_id) REFERENCES users(id),
        CONSTRAINT ck_campaigns_status CHECK (status IN ('PENDING', 'ACTIVE', 'APPROVED', 'REJECTED'))
    );
END
GO

-- -----------------------------------------------------------------------------
-- donations
-- -----------------------------------------------------------------------------
IF OBJECT_ID('donations', 'U') IS NULL
BEGIN
    CREATE TABLE donations (
        id          BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id     BIGINT         NOT NULL,
        campaign_id BIGINT         NOT NULL,
        amount      FLOAT          NOT NULL,
        date        DATE           NOT NULL,
        status      NVARCHAR(20)   NOT NULL CONSTRAINT DF_donations_status DEFAULT 'COMPLETED',
        CONSTRAINT fk_donations_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_donations_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
        CONSTRAINT ck_donations_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'))
    );
END
GO

-- -----------------------------------------------------------------------------
-- volunteers
-- -----------------------------------------------------------------------------
IF OBJECT_ID('volunteers', 'U') IS NULL
BEGIN
    CREATE TABLE volunteers (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id      BIGINT       NOT NULL,
        campaign_id  BIGINT       NOT NULL,
        status       NVARCHAR(20) NOT NULL CONSTRAINT DF_volunteers_status DEFAULT 'PENDING',
        applied_date DATE         NOT NULL,
        why_join     NVARCHAR(MAX) NULL,
        skills       NVARCHAR(MAX) NULL,
        availability NVARCHAR(100) NULL,
        experience   NVARCHAR(MAX) NULL,
        phone        NVARCHAR(50)  NULL,
        CONSTRAINT fk_volunteers_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_volunteers_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
        CONSTRAINT uq_volunteers_user_campaign UNIQUE (user_id, campaign_id),
        CONSTRAINT ck_volunteers_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'ACCEPTED'))
    );
END
GO
