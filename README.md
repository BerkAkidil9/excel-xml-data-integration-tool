# Excel/XML Data Integration Tool

## Project Overview

Excel/XML Data Integration Tool is a fixed-contract Java project for converting invoice data between a repository-defined Excel workbook format and a repository-defined XML `invoice-data-v1` format.

The project is intentionally not a generic Excel/XML mapper and does not claim legal e-invoice, UBL, UBL-TR, Peppol, or GIB compliance.

## Current Status

Phases 1 through 9 are implemented:

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
- Excel `.xlsx` writer using Apache POI, including exact sheets/headers, real date cells, numeric decimal cells, header styling, freeze panes, filters, and useful validation lists.
- Excel-to-XML and XML-to-Excel conversion orchestration services with temporary output files, final-output replacement, record counts, and structured errors.
- Presentation-friendly conversion reports with success/failure state, record counts, ordered errors, grouped errors, and stable error categories.
- Small Swing UI over the tested conversion services, with direction selection, file choosers, progress/status, counts, and structured error display.
- Maven packaging for a runnable Swing application jar.
- JUnit tests for calculation, validation, Excel reader, Excel writer, XML writer, XML reader, conversion behavior, and stable UI controller/view-model behavior.

The project is now packaged as a local desktop application, not as an installer or signed release artifact.

## Features

Implemented:

- Contract-compatible domain values under `com.berk.dataintegration.domain`.
- Monetary calculation under `com.berk.dataintegration.calculation`.
- Structured validation under `com.berk.dataintegration.validation`.
- Excel workbook contract validation, reading, and writing under `com.berk.dataintegration.excel`.
- XML reading and writing under `com.berk.dataintegration.xml`.
- Conversion orchestration and presentation-friendly conversion reports under `com.berk.dataintegration.service`.
- Swing UI under `com.berk.dataintegration.ui`, launched from `com.berk.dataintegration.app`.
- Runnable application packaging through Maven.
- JUnit test coverage for the current domain, calculation, validation, Excel reader, Excel writer, XML writer, XML reader, conversion behavior, and stable UI controller/view-model behavior.

Planned:

- Installer/signing work, if needed for a future release.
- Optional UBL adapter research only after the fixed-contract v1 is complete.

## Technology Stack

- Java 21
- Apache Maven
- Apache POI `5.5.1`
- Jakarta XML Binding API `4.0.5`
- JAXB Runtime `4.0.9`
- JUnit Jupiter `6.1.2`
- Maven Compiler Plugin `3.15.0`
- Maven Surefire Plugin `3.5.5`
- Maven Shade Plugin `3.6.1`

Apache POI is used by the Excel reader and writer. Jakarta XML Binding is used by the XML reader and writer.

## Architecture

The current implementation uses these packages:

```text
src/main/java/com/berk/dataintegration/
  app/
  calculation/
  domain/
  excel/
  service/
  ui/
  xml/
  validation/
```

Domain classes remain independent from Apache POI, JAXB, and Swing.

High-level flow:

```text
Swing UI
  -> conversion service
      -> Excel reader/writer
      -> XML reader/writer
      -> validation
      -> calculation
          -> domain model
```

Excel and XML infrastructure depend on the domain model, validation, and calculation services. The domain model does not depend on Apache POI, JAXB, Swing, or service orchestration.

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

Clone and build:

```bash
git clone https://github.com/BerkAkidil9/excel-xml-data-integration-tool.git
cd excel-xml-data-integration-tool
./mvnw compile
```

On a fresh machine, the Maven Wrapper downloads Maven on first use, and Maven downloads project dependencies.

Create the regular jar and runnable application jar:

```bash
./mvnw package
```

The runnable jar is created at:

```text
target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar
```

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

Run the Swing application during development:

```bash
./mvnw exec:java -Dexec.mainClass=com.berk.dataintegration.app.DataIntegrationToolApp
```

Run the packaged application after `./mvnw package`:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar
```

There is no CLI yet. The current user-facing entry point is the Swing application.

## Usage

1. Choose the conversion direction: Excel to XML or XML to Excel.
2. Select an input file matching the repository contract.
3. Select the output file location.
4. Run the conversion.
5. Review record counts and structured errors if validation fails.

The application validates input before writing output. Failed conversions should not leave partial output behind.

## Screenshots

Actual screenshots are not committed yet. Placeholder capture list:

- Main conversion window.
- Successful Excel to XML conversion result.
- Validation failure with structured error rows.

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
5. Excel writer: implemented.
6. Conversion services and round-trip tests: implemented.
7. Structured error reporting improvements: implemented.
8. Swing user interface: implemented.
9. Packaging and project polish: implemented.

See `docs/roadmap.md` for more detail.

## Limitations

- Not a legally compliant e-invoice product.
- No UBL, UBL-TR, Peppol, or GIB integration.
- No installer, code signing, auto-update, or CLI yet.
- No full IBAN checksum, national tax-number algorithm, or complete ISO currency database.
- Screenshots are documented as placeholders until real UI screenshots are captured.

## Contributing

Contributions should keep the project aligned with the fixed contracts and incremental roadmap. See `CONTRIBUTING.md`.

## License

This project is licensed under the MIT License. See `LICENSE`.
