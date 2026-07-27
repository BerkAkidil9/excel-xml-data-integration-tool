# Excel/XML Data Integration Tool

## Project Overview

Excel/XML Data Integration Tool is a fixed-contract Java project for converting invoice data between a repository-defined Excel workbook format and a repository-defined XML `invoice-data-v1` format.

The project is intentionally not a generic Excel/XML mapper and does not claim legal e-invoice, UBL, UBL-TR, Peppol, or GIB compliance.

## Current Status

Phase 1 is implemented:

- Maven project foundation for Java 21.
- Maven Wrapper support.
- Immutable domain model for invoice batches, parties, payment accounts, invoices, and invoice lines.
- Central monetary calculation service.
- Structured validation model with stable error codes.
- Batch validation for field rules, duplicate identifiers, references, roles, dates, line rules, and currency consistency.
- JUnit tests for calculation and validation.

Excel reading/writing, XML reading/writing, XSD validation, conversion orchestration, and Swing UI are planned but not implemented yet.

## Features

Implemented:

- Contract-compatible domain values under `com.berk.dataintegration.domain`.
- Monetary calculation under `com.berk.dataintegration.calculation`.
- Structured validation under `com.berk.dataintegration.validation`.
- JUnit test coverage for the current domain, calculation, and validation behavior.

Planned:

- Excel `.xlsx` reader and writer using Apache POI.
- XML reader and writer using Jakarta XML Binding.
- XSD validation for `invoice-data-v1`.
- Bidirectional conversion services.
- Swing desktop UI.

## Technology Stack

- Java 21
- Apache Maven
- Apache POI `5.5.1`
- Jakarta XML Binding API `4.0.5`
- JAXB Runtime `4.0.9`
- JUnit Jupiter `6.1.2`
- Maven Compiler Plugin `3.15.0`
- Maven Surefire Plugin `3.5.5`

Apache POI and JAXB dependencies are already declared for later phases, but Phase 1 production code does not use them yet.

## Architecture

The current implementation uses these packages:

```text
src/main/java/com/berk/dataintegration/
  calculation/
  domain/
  validation/
```

Planned packages for later phases include:

```text
src/main/java/com/berk/dataintegration/
  app/
  excel/
  xml/
  service/
  ui/
  exception/
```

Domain classes remain independent from Apache POI, JAXB, and Swing.

## Data Formats

The repository defines one logical invoice data contract:

- Excel workbook contract: `docs/data-format/excel-format.md`
- XML contract: `docs/data-format/xml-format.md`
- Shared domain and validation contract: `docs/data-format/data-contract.md`
- XML Schema: `src/main/resources/schema/invoice-data-v1.xsd`

Useful sample files:

- `templates/invoice-data-template.xlsx`
- `examples/valid-invoice-data.xlsx`
- `examples/valid-invoice-data.xml`
- `src/test/resources/fixtures/`

## Build

Java 21 is required.

```bash
./mvnw compile
```

On a fresh machine, the Maven Wrapper downloads Maven on first use, and Maven downloads project dependencies.

## Test

Run the unit test suite:

```bash
./mvnw test
```

Run the full verification build:

```bash
./mvnw verify
```

## Run

There is no runnable desktop application, CLI, or conversion service yet. The current project state is a tested domain, calculation, and validation foundation.

## Project Structure

```text
.
├── AGENTS.md
├── CONTRIBUTING.md
├── LICENSE
├── README.md
├── docs/
│   ├── architecture/
│   ├── data-format/
│   ├── development/
│   ├── references.md
│   └── roadmap.md
├── examples/
├── src/
│   ├── main/
│   └── test/
└── templates/
```

## Roadmap

1. Foundation, domain, calculation, and validation: implemented.
2. Excel contract validation and reader: planned.
3. XML DTOs, XSD validation, and writer: planned.
4. XML reader and domain mapping: planned.
5. Excel writer: planned.
6. Conversion services and round-trip tests: planned.
7. Structured error reporting improvements: planned.
8. Swing user interface: planned.
9. Packaging and project polish: planned.

See `docs/roadmap.md` for more detail.

## Limitations

- Not a legally compliant e-invoice product.
- No UBL, UBL-TR, Peppol, or GIB integration.
- No Excel reader/writer yet.
- No XML reader/writer or XSD validation service yet.
- No UI or conversion orchestration yet.
- No full IBAN checksum, national tax-number algorithm, or complete ISO currency database.

## Contributing

Contributions should keep the project aligned with the fixed contracts and incremental roadmap. See `CONTRIBUTING.md`.

## License

This project is licensed under the MIT License. See `LICENSE`.
