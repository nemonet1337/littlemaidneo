# Java 25 Modern Coding Standards

## General Principles
- Prefer clarity, readability, and maintainability.
- Keep methods short and focused on a single responsibility.
- Favor composition over inheritance.
- Avoid deep nesting; prefer early returns and guard clauses.

## Modern Java Language Features (Java 21–25)
- Prefer records for immutable data carriers.
- Use sealed classes/interfaces when the set of subtypes is known and finite.
- Prefer pattern matching for `instanceof` and switch expressions/statements.
- Use text blocks for multi-line strings.
- Prefer `var` for local variables when the type is obvious from the right-hand side.
- Use unnamed patterns and variables (`_`) where the value is intentionally unused.
- Leverage sequenced collections and new Collection factory methods where appropriate.
- Prefer structured concurrency and virtual threads for concurrent workloads when suitable.

## Null Handling & Optionality
- Minimize use of `null`. Prefer `Optional` for return types that may be absent (especially in public APIs).
- Do not use `Optional` for fields or method parameters in most cases.
- Use `Objects.requireNonNull` at boundaries when null is unacceptable.

## Error Handling
- Prefer specific exception types over generic ones.
- Use try-with-resources for all resources that implement `AutoCloseable`.
- Avoid catching generic `Exception` or `Throwable` unless rethrowing or at the top level.
- Provide meaningful exception messages.

## API & Design
- Prefer immutable objects where practical.
- Make classes final by default unless designed for extension.
- Limit the public surface area of classes and packages.
- Use interfaces to define contracts; keep implementation details hidden.

## Concurrency
- Prefer higher-level concurrency utilities over raw threads and synchronized blocks.
- Consider virtual threads for high-throughput I/O-bound workloads.
- Document thread-safety guarantees of public classes.