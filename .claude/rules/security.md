# Security

## Credentials
- NEVER hardcode credentials, API keys, or secrets in source files.
- Database credentials and connection strings belong in `.env` (gitignored).
- If the user asks to commit a file containing secrets, warn them before proceeding.

## Environment
- Reference `.env` for local configuration. Do not echo or log credential values.
- Use environment variables in code (`os.getenv()`, `process.env`).
