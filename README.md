# Excel/XML Data Integration Tool

## Project Overview

Excel/XML Data Integration Tool is a fixed-contract Java project for converting invoice data between a repository-defined Excel workbook format and a repository-defined XML `invoice-data-v1` format.

The project is intentionally not a generic Excel/XML mapper and does not claim legal e-invoice, UBL, UBL-TR, Peppol, or GIB compliance.

## Current Status

Phases 1, 2, 3, and 4 are implemented:

- Maven project foundation for Java 21.
- Maven Wrapper support.
- Immutable domain model for invoice batches, parties, payment accounts, invoices, and invoice lines.
- Central monetary calculation service.
- Structured validation model with stable error codes.
- Batch validation for field rules, duplicate identifiers, references, roles, dates, line rules, and currency consistency.
- Excel `.xlsx` contract validation for required sheets and exact headers.
- Excel `.xlsx` reader using Apache POI, including intentional cell-type parsing, Excel date cells, ISO date text, blank-row handling, and structured source locations.
- XML DTOs, explicit domain-to-XML mapping, JAXB XML writer, and secure XSD validation for generated XML.
- Secure XML reader using JAXB, XSD validation, XML-to-domain mapping, and business consistency validation for imported totals.
- JUnit tests for calculation, validation, Excel reader, XML writer, and XML reader behavior.

Excel writing, conversion orchestration, and Swing UI are planned but not implemented yet.

## Features

Implemented:

- Contract-compatible domain values under `com.berk.dataintegration.domain`.
- Monetary calculation under `com.berk.dataintegration.calculation`.
- Structured validation under `com.berk.dataintegration.validation`.
- Excel workbook contract validation and reading under `com.berk.dataintegration.excel`.
- XML reading and writing under `com.berk.dataintegration.xml`.
- JUnit test coverage for the current domain, calculation, validation, Excel reader, XML writer, and XML reader behavior.

Planned:

- Excel `.xlsx` writer using Apache POI.
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

Apache POI is used by the Excel reader. Jakarta XML Binding is used by the XML writer.

## Architecture

The current implementation uses these packages:

```text
src/main/java/com/berk/dataintegration/
  calculation/
  domain/
  excel/
  xml/
  validation/
```

Planned packages for later phases include:

```text
src/main/java/com/berk/dataintegration/
  app/
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

There is no runnable desktop application, CLI, or conversion service yet. The current project state is a tested domain, calculation, validation, Excel reader, XML writer, and XML reader foundation.

## Project Structure

```text
.
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
2. Excel contract validation and reader: implemented.
3. XML DTOs, XSD validation, and writer: implemented.
4. XML reader and domain mapping: implemented.
5. Excel writer: planned.
6. Conversion services and round-trip tests: planned.
7. Structured error reporting improvements: planned.
8. Swing user interface: planned.
9. Packaging and project polish: planned.

See `docs/roadmap.md` for more detail.

## Limitations

- Not a legally compliant e-invoice product.
- No UBL, UBL-TR, Peppol, or GIB integration.
- No Excel writer yet.
- No UI or conversion orchestration yet.
- No full IBAN checksum, national tax-number algorithm, or complete ISO currency database.

## Contributing

Contributions should keep the project aligned with the fixed contracts and incremental roadmap. See `CONTRIBUTING.md`.

## License

This project is licensed under the MIT License. See `LICENSE`.
