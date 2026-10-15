# Excel/XML Data Integration Tool

A Java desktop and command-line application for converting invoice data between structured Excel workbooks (`.xlsx`) and the project's versioned XML format. It validates the data and reports errors with their source locations so they can be corrected before conversion.

## Features

- **Bidirectional conversion:** convert Excel to XML and XML to Excel, preserving supported party, payment-account, invoice, and line-item data.
- **Desktop and CLI workflows:** convert files through a Swing interface or command-line arguments. The desktop interface runs conversions in the background and displays status and record counts.
- **Data validation:** check workbook structure, required fields, duplicate identifiers, references, party roles, dates, currencies, and numeric limits. XML is validated against the bundled XSD.
- **Monetary calculations:** calculate line and invoice totals with decimal arithmetic and two-decimal `HALF_UP` rounding; reject imported XML totals that differ from recalculation.
- **Secure XML reading:** reject DOCTYPE declarations and disable external entity resolution and external schema fetching.
- **Structured error reports:** inspect and filter errors with field, record, and available sheet/cell or XML location details; export the complete error list as CSV with grouped summaries.
- **Excel templates:** generate a blank workbook containing the required sheets and headers.

## Getting Started

### Requirements

- JDK 21. Other Java major versions are rejected by the build. If multiple JDKs are installed, set `JAVA_HOME` to JDK 21.
- Git to clone the repository.
- Internet access for the initial Maven and dependency downloads.

The included Maven Wrapper selects Maven `3.9.11`; a separate Maven installation is not required. Opening the desktop interface requires a graphical desktop. The CLI can run headlessly.

### Install and build

Clone the repository and enter its directory:

```bash
git clone https://github.com/BerkAkidil9/excel-xml-data-integration-tool.git
cd excel-xml-data-integration-tool
```

Build the application and run its tests:

```bash
./mvnw verify
```

This creates the runnable application jar, including its dependencies:

```text
target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar
```

To build the jars through the package phase, which also runs the tests:

```bash
./mvnw package
```

On Windows PowerShell, replace `./mvnw` with `.\mvnw.cmd`, for example `.\mvnw.cmd verify`. Run the following application commands from the repository root. Multiline CLI examples use Bash line continuations; in PowerShell, enter each command on a single line without the trailing backslashes.

## Run

### Desktop interface

Launch the packaged application without arguments:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar
```

Choose a conversion direction, input file, and output location, then start the conversion. The interface suggests an output path, asks before overwriting an existing output, and displays validation errors for review. You can export the errors as CSV or save a blank Excel template from the interface.

### Command line

Show usage:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar --help
```

Convert the supplied Excel example to XML:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar \
  convert \
  --direction excel-to-xml \
  --input examples/valid-invoice-data.xlsx \
  --output target/output/from-excel.xml
```

Convert the supplied XML example to Excel:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar \
  convert \
  --direction xml-to-excel \
  --input examples/valid-invoice-data.xml \
  --output target/output/from-xml.xlsx
```

Add `--error-report target/output/errors.csv` to either conversion command to write a UTF-8 CSV report with a total error count, grouped summaries, and detailed errors. A successful conversion produces a zero-error report. CSV is an error-report output format, not an invoice input format.

Generate a blank Excel template:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar \
  template \
  --output target/output/invoice-data-template.xlsx
```

CLI output paths are replaced without an interactive prompt. Input validation errors are reported before a conversion output is written.

Exit codes:

- `0`: conversion or template generation succeeded, or help was displayed.
- `1`: conversion, template generation, or requested error-report writing failed.
- `2`: invalid command-line usage.

## Data Format and Scope

The application uses the fixed `invoice-data-v1` contract. Excel input must contain the `Parties`, `PaymentAccounts`, `Invoices`, and `InvoiceLines` sheets with the documented headers in order. Start with the [blank template](templates/invoice-data-template.xlsx), [Excel example](examples/valid-invoice-data.xlsx), or [XML example](examples/valid-invoice-data.xml).

Excel accepts real date cells or ISO date text (`yyyy-MM-dd`), and numeric cells or decimal text using `.`. Formula cells are rejected even when a cached result exists. Conversion preserves supported data fields rather than the original workbook's custom formatting or extra sheets.

The XML schema is bundled with the application. This project-specific format does not implement UBL, UBL-TR, Peppol, or GİB e-Fatura compliance. IBAN checksums, national tax-number algorithms, discounts, withholding, and multiple tax types per line are outside the current scope.

Format references:

- [Shared data contract and calculation rules](docs/data-format/data-contract.md)
- [Excel sheets, headers, and accepted cell types](docs/data-format/excel-format.md)
- [XML structure and validation](docs/data-format/xml-format.md)
- [XML Schema](src/main/resources/schema/invoice-data-v1.xsd)

## Testing

Run the JUnit Jupiter suite:

```bash
./mvnw test
```

Use `./mvnw verify` for a complete test and packaging build. Surefire runs unit, component, and service integration scenarios covering calculations, validation, Excel/XML readers and writers, conversion services, CLI behavior, CSV reports, and Swing controller/view-model helpers.

Tests run headlessly and do not verify the full visible Swing application. Results are written to `target/surefire-reports/`. See the [testing strategy](docs/development/testing-strategy.md) for detailed coverage and limitations.

## Technology Stack

- Java 21 and Java Swing
- Apache POI `5.5.1` for Excel reading and writing
- Jakarta XML Binding API `4.0.5` and JAXB Runtime `4.0.9` for XML mapping
- Maven Wrapper and Maven Shade Plugin for building the runnable jar
- JUnit Jupiter `6.1.2` and Maven Surefire for automated tests

Dependency and plugin versions are defined in [pom.xml](pom.xml); the Maven distribution is pinned in [maven-wrapper.properties](.mvn/wrapper/maven-wrapper.properties).

## Project Structure

```text
src/main/java/              Application, domain, conversion, validation, CLI, and Swing code
src/main/resources/schema/ Bundled XML schema
src/test/                  Automated tests and fixtures
examples/                  Valid Excel and XML sample data
templates/                 Blank Excel workbook template
docs/                      Architecture, format, and development documentation
pom.xml                    Dependencies and build configuration
```

The domain model is independent of the Excel/XML adapters and presentation layers. See the [architecture decisions](docs/architecture/decisions.md) for the design and format choices.

## License

Licensed under the [MIT License](LICENSE).
