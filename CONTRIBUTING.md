# Contributing

## Setup

1. Fork the repository.
2. Clone your fork and navigate to the project.
3. Install Java 25 and Maven 3.9+.
4. Run `./mvnw clean install`.

## Branching

- Use feature branches: `feature/<name>` or `fix/<name>`.
- Keep commits focused and small.

## Code style

- Keep packages and modules as in current structure.
- Follow standard Java conventions (naming, formatting, Javadoc for public APIs).
- Use `@Component` for Spring beans and `@Service` for application services.

## Tests

- Add unit tests for each new control and evaluator.
- Add integration tests under profile `integration-tests` when needed.
- Run `./mvnw test` and verify all tests pass before PR.

## Pull requests

- Target main branch.
- Use clear title and description.
- Include rationale and test evidence.
- Ensure the PR includes README or docs updates when behavior changes.
