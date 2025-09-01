# Project Guidelines - Nota

## Key Style Guidelines
- Use Kotlin idioms and conventions
- Do not use the non-null asserted operator (!!)
- Maintain consistent indentation (4 spaces)
- Follow Spring Boot naming conventions
- Use meaningful, descriptive names for variables, functions, and classes
- Add appropriate documentation for public APIs
- Prefer immutability; use data classes and val over var when possible
- Avoid nullable types when not necessary; model nullability explicitly and handle it safely
- Use structured logging (SLF4J); do not use println for application logs

## General Instructions
- Write different kinds of tests for new functionality, including unit, integration, and end-to-end (UI) tests.
- Only use Kotest for test assertions; use MockK for mocking.
- Delete unnecessary temporary files before committing.
- Build the project before finishing any task that changes code.
- Execute all tests before finishing a task that changes code.
- Do not commit changes before running all tests and static code analysis.
- Use Conventional Commits for all commit messages. Include the project issue identifier in square brackets at the beginning, e.g., [NOTA-123] type(scope)!: short description. Format: type(scope)!: short description. Common types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert. Use imperative present tense, no trailing period; reference issues when relevant; include BREAKING CHANGE footer when applicable. In the commit body, focus on why the change is necessary (context, motivation, trade-offs) rather than what changed—the "what" should be clear from the subject and the diff. Example: explain the underlying issue, constraints, and alternative approaches considered.

## Local Quality and Tooling
- Use the Makefile targets to keep commands consistent:
  - make build — build the project with Amper
  - make test — run backend tests
  - make test-ui — run Playwright UI tests
  - make check — run ktlint and detekt
  - make ktlint / make ktlint-fix — run or auto-fix style issues
  - make detekt / make detekt-fix — run or auto-correct static analysis
  - make pre-commit — run non-interactive quality checks
  - make install-hooks — install git pre-commit hook
- Alternatively, you can run tools directly: tools/ktlint.sh and tools/detekt.sh

## Pull Requests and Code Review
- Link the related issue and ensure commits follow [NOTA-xxx] Conventional Commits format
- Include a clear description of the change, motivation, and scope
- Ensure CI is green (build, tests, ktlint, detekt)
- Include or update tests and documentation for any behavior change

## Security and Configuration
- Never commit secrets or credentials. Use environment variables or external configuration
- Validate and sanitize any user-provided input; prefer established libraries already in use
- Keep dependencies up to date and prefer stable versions; avoid unused dependencies
