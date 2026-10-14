# Testing Strategy

## Current suite

Reviewed on 2026-10-14: **103 JUnit tests in 21 `*Test` classes**, with zero failures, errors, or skipped tests in the verification run. All classes run through Maven Surefire. `TestBatches` supplies reusable domain fixtures and is not a test suite.

The suite mixes unit, component, and service integration scenarios. The total should not be described as 103 unit tests or 103 end-to-end tests.

| Area | Implemented checks |
|---|---|
| Calculation and validation | `BigDecimal` arithmetic, line rounding, sums, required fields, duplicate identifiers, references, party roles, due dates, currencies, quantity/price/tax constraints |
| Excel | Required sheets and exact headers, blank versus partial rows, dates and decimals, formula rejection, source context, writer cell types/formatting, writer-to-reader round trip, blank template |
| XML | Namespace/order/XSD, optional omission, UTF-8, calculated amounts, malformed XML, missing references, wrong totals, DOCTYPE/XXE rejection |
| Conversion services | Both directions with domain-field comparisons and record counts, validation failure preserving an existing output or avoiding a new output, temporary-file cleanup |
| CLI and reports | Conversion/help/usage/template paths in the same JVM, structured errors, CSV escaping, total/grouped/detail report sections |
| Swing support | Controller/view-model state, output suggestions, reset, error filtering/formatting, directory history, chooser controls, background work and EDT callbacks |

Sources are under [src/test/java](../../src/test/java/). Excel/XML readers and writers use real POI/JAXB implementations in component tests. Some controller tests use stub executors; they do not launch the full application window.

## Maven execution

Run commands from the repository root with JDK 21. Use `mvnw.cmd` on Windows.

| Command | Current behavior |
|---|---|
| `./mvnw test` | Compiles sources and runs the entire Surefire suite |
| `./mvnw verify` | Runs the same suite, then builds the regular jar and shaded runnable application jar |
| `./mvnw clean verify` | Deletes previous build outputs before the verification build; useful for a fresh report |
| `./mvnw -DskipTests verify` | Compiles test sources and packages the application, but does not execute tests |

Surefire sets `-Djava.awt.headless=true` and disables the module path. Test results are written to `target/surefire-reports/` as XML and text. Read reports from the current run; older build outputs may remain after running an individual test.

There is no Maven Failsafe plugin or separate `*IT` execution. `verify` does not add a separate integration-test suite. If Failsafe is introduced later, its naming and lifecycle configuration must be documented together.

## CI and packaged application checks

The [Verify workflow](../../.github/workflows/verify.yml) runs two jobs in parallel on this Mac's two repository-specific self-hosted runners:

- `Tests`: runs `./mvnw --batch-mode --no-transfer-progress -Dmaven.repo.local="$RUNNER_WORKSPACE/.m2/repository" test`.
- `Package and CLI`: runs the same wrapper with `-DskipTests verify`, then starts the application jar in separate JVMs to generate a template, convert the supplied Excel and XML examples in both directions, and convert the generated workbook back to XML.

The packaged CLI checks require successful exit codes and nonempty output files. They exercise the jar's entry point and bundled dependencies, but do not perform a complete semantic diff of the two XML documents. They are additional CI checks and are not included in the 103 JUnit test count.

The final `verify` job fails if either job did not succeed. Both jobs use Java 21, and each runner keeps its own Maven repository. [CI and runner operations](ci.md) describes triggers, services, local reports, and troubleshooting.

## Fixtures and repeatability

- [examples](../../examples/) contains the user-facing valid Excel/XML data used by some tests and the packaged CLI checks.
- [src/test/resources/fixtures](../../src/test/resources/fixtures/) contains valid and invalid XML, an Excel workbook, and CSV fixture data. CSV fixtures do not mean the application supports CSV invoice import.
- Writers and conversion tests create outputs in JUnit `@TempDir` directories rather than replacing the checked-in examples.
- Tests compare decimal values numerically where scale is not part of the behavior being checked, and explicitly check rounding/scale where it is.
- Test methods describe behavior in English; all currently executed suite classes use the `*Test` suffix.

## Coverage and limits

The [2026-09-30 evidence report](../cv-metrics-evidence/2026-09-30/REPORT.md) records a separate JaCoCo run and conversion benchmarks. Its coverage percentages and timing results apply to that documented snapshot and dataset. Neither JaCoCo coverage nor the benchmark harness runs in the normal Maven build or CI, and no coverage threshold is enforced.

Headless tests do not verify the visible Swing window, visual appearance, or accessibility. Current CI verifies macOS ARM64 execution; it is not a Windows/Linux test matrix. XML security regression tests are not a dependency vulnerability scan; Dependency-Check is not configured.

When adding regressions, retain both conversion directions, monetary boundary cases, schema/business-error separation, formula rejection, source locations, and output preservation. Add focused scenarios for any uncovered branch or bug rather than inferring coverage from the total test count.
