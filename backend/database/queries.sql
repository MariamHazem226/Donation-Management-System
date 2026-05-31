USE givinghands_db;

SELECT * FROM users;
SELECT * FROM campaigns;
SELECT * FROM volunteers;

-- Foreign Key Relationships

SELECT
    fk.name        AS FK_Name,
    tp.name        AS Parent_Table,
    cp.name        AS Parent_Column,
    tr.name        AS Referenced_Table,
    cr.name        AS Referenced_Column
FROM sys.foreign_keys fk
JOIN sys.tables tp          ON fk.parent_object_id     = tp.object_id
JOIN sys.tables tr          ON fk.referenced_object_id = tr.object_id
JOIN sys.foreign_key_columns fkc ON fk.object_id       = fkc.constraint_object_id
JOIN sys.columns cp ON fkc.parent_object_id            = cp.object_id
                    AND fkc.parent_column_id            = cp.column_id
JOIN sys.columns cr ON fkc.referenced_object_id        = cr.object_id
                    AND fkc.referenced_column_id        = cr.column_id;

-- Sample Data (for testing)

-- Insert Admin user
INSERT INTO users (name, email, password, role)
VALUES ('Admin', 'admin@givinghands.com', 'admin123', 'ADMIN');

INSERT INTO users (name, email, password, role)
VALUES ('Admin', 'mariamhazem226@gmail.com', 'mariam123', 'ADMIN');

-- Insert Organization user
INSERT INTO users (name, email, password, role)
VALUES ('Helping Hands Org', 'org@givinghands.com', 'org123', 'ORGANIZATION');

-- Insert regular User
INSERT INTO users (name, email, password, role)
VALUES ('John Doe', 'john@givinghands.com', 'user123', 'USER');

-- Insert sample Campaign
INSERT INTO campaigns (title, description, goal_amount, deadline, creator_id)
VALUES ('Food Drive 2025', 'Collecting food for families in need.',
        5000.00, '2025-12-31', 2);

-- Insert sample Volunteer application
INSERT INTO volunteers (user_id, campaign_id, status, applied_date)
VALUES (3, 1, 'PENDING', CAST(GETDATE() AS DATE));

-- New Users
INSERT INTO users (name, email, password, role)
VALUES ('Hope Foundation', 'hope@org.com', 'org123', 'ORGANIZATION');

INSERT INTO users (name, email, password, role)
VALUES ('Green Earth Org', 'green@org.com', 'org123', 'ORGANIZATION');

INSERT INTO users (name, email, password, role)
VALUES ('Sara Ahmed', 'sara@gmail.com', 'user123', 'USER');

INSERT INTO users (name, email, password, role)
VALUES ('Mohamed Ali', 'mohamed@gmail.com', 'user123', 'USER');

INSERT INTO users (name, email, password, role)
VALUES ('Nour Hassan', 'nour@gmail.com', 'user123', 'USER');
