# AGENTS.md — Rules for AI Coding Assistants

**Read this file completely before making any change to this repository.**

<!-- RULES :  -->

1. Everything should be evironment driven, use environment variables.
2. Don't do any inline styling.
3. Do not modify unrelated files.
4. Do not push directly to main.
5. Never take access of .env file.
6. Don't do anything hardcoded.
7. Read and understand the existing code before making any changes.
8. Make only the changes required for the requested task. Do not modify unrelated files or functionality.
9. Follow the existing project structure, architecture, naming conventions, and coding style.
10. Use environment variables for configuration. Never hardcode secrets, credentials, API keys, database URLs, or environment-specific values.
11. Never read, expose, modify, or commit `.env` files or their secret values.
12. Do not add, remove, or upgrade dependencies unless they are required for the requested task.
13. Never perform destructive database operations or delete existing user data without explicit approval.
14. After making changes, run the relevant tests, build, lint, or validation checks and fix errors caused by the changes.
15. Do not commit, push, force-push, or modify Git history unless explicitly requested. Never push directly to `main`.
16. Do not overwrite or discard existing user changes. Preserve existing functionality unless the task explicitly requires changing it.