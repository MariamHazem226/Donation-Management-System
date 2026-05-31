# Database schema (SQL Server)

Schema is managed **only** through SQL scripts in this folder. The application does **not** run `ALTER TABLE` at runtime.

## Gmail notifications

Set environment variables before starting the backend:

```powershell
$env:MAIL_USERNAME="your.email@gmail.com"
$env:MAIL_PASSWORD="your-google-app-password"
```

See `backend/application.properties.example` for details.

## Fix an existing database (most common)

If you see `Invalid column name 'allow_volunteers'`:

1. Open **SQL Server Management Studio**
2. Connect to `localhost`
3. Open and execute **`fix-schema.sql`** against `givinghands_db`
4. Restart the Spring Boot application

## New database

1. Create database: `CREATE DATABASE givinghands_db;`
2. Execute **`schema.sql`**

## Files

| File | Purpose |
|------|---------|
| `schema.sql` | Full schema for a new database |
| `fix-schema.sql` | Idempotent repair for outdated databases |
| `queries.sql` | Optional reporting queries |

## Campaign table (matches `Campaign.java`)

| Column | Type | Notes |
|--------|------|--------|
| `allow_volunteers` | `BIT NOT NULL` | Default `1` (true) |
| `image_path` | `NVARCHAR(500)` | Nullable |
| `status` | `NVARCHAR(20)` | `PENDING`, `ACTIVE`, `APPROVED`, `REJECTED` |
