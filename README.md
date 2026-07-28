# Excel/XML Data Integration Tool

A desktop and command-line application for converting invoice data between the project's predefined Excel workbook format and its matching XML format.

The XML format is versioned as `invoice-data-v1` and defined by the repository schema. The application is intentionally not a generic Excel/XML mapper and does not claim legal e-invoice, UBL, UBL-TR, Peppol, or GIB compliance.

## Status

The first supported version is implemented:

- Excel `.xlsx` format validation, reading, and writing with Apache POI.
- XML reading and writing with Jakarta XML Binding.
- XSD validation for the project's XML format.
- Domain and business validation for parties, payment accounts, invoices, references, dates, currencies, and invoice lines.
- Central monetary calculation with `BigDecimal`, scale 2, and `RoundingMode.HALF_UP`.
- Structured conversion errors with stable codes, categories, source locations, fields, record identifiers, and offending values where available.
- Excel to XML and XML to Excel conversion services with temporary output handling.
- CLI support for conversion, CSV error reports with summary and detail sections, and blank Excel template generation.
- Swing UI for local desktop use.
- Swing file chooser navigation, reset behavior, automatic output suggestions, and structured error review helpers.
- Maven packaging for a runnable application jar.
- GitHub Actions verification with the Maven Wrapper.

The application is packaged as a runnable local desktop application. It is not distributed as a signed installer and does not include auto-update support.

## Features

- Bidirectional conversion:
  - Excel to XML
  - XML to Excel
- Required Excel workbook sheets:
  - `Parties`
  - `PaymentAccounts`
  - `Invoices`
  - `InvoiceLines`
- XML namespace used by this project:
  - `urn:berk:excel-xml-integration:invoice:v1`
- Secure XML parsing:
  - DOCTYPE rejected
  - external entities disabled
  - external DTD/schema access disabled
  - no network schema fetching
- Validation:
  - required fields
  - duplicate identifiers
  - missing references
  - supplier/customer role correctness
  - due date rules
  - currency consistency
  - positive quantities
  - non-negative prices
  - tax-rate boundaries
  - calculated total mismatches
- Output helpers:
  - runnable application jar
  - CSV error report export with grouped summary and detail rows
  - blank Excel template generation
  - sample valid Excel and XML files
- Desktop workflow helpers:
  - reset button for clearing the current UI state
  - automatic output path suggestions that update when the input changes while the output is still auto-generated
  - file chooser folder Back/Forward navigation
  - compact grouped error summary above the error table

## Technology Stack

- Java 21
- Apache Maven
- Java Swing
- Apache POI `5.5.1`
- Jakarta XML Binding API `4.0.5`
- JAXB Runtime `4.0.9`
- JUnit Jupiter `6.1.2`
- Maven Compiler Plugin `3.15.0`
- Maven Surefire Plugin `3.5.5`
- Maven Shade Plugin `3.6.1`
- Maven Enforcer Plugin `3.6.3`

## Architecture

The implementation keeps domain logic independent from file formats and UI concerns.

```text
src/main/java/com/berk/dataintegration/
  app/
  calculation/
  cli/
  domain/
  excel/
  service/
  ui/
  validation/
  xml/
```

High-level dependency flow:

```text
Swing UI / CLI
  -> conversion service
      -> Excel reader/writer
      -> XML reader/writer
      -> validation
      -> calculation
          -> domain model
```

The domain layer does not depend on Apache POI, JAXB, Swing, or service orchestration.

More detail:

- Architecture decisions: `docs/architecture/decisions.md`
- Testing strategy: `docs/development/testing-strategy.md`

## Data Formats

The repository defines one invoice data format with matching Excel and XML representations:

- Shared domain and validation rules: `docs/data-format/data-contract.md`
- Excel workbook format: `docs/data-format/excel-format.md`
- XML format: `docs/data-format/xml-format.md`
- XML Schema: `src/main/resources/schema/invoice-data-v1.xsd`

Useful files:

- Blank Excel template: `templates/invoice-data-template.xlsx`
- Valid Excel example: `examples/valid-invoice-data.xlsx`
- Valid XML example: `examples/valid-invoice-data.xml`
- Test fixtures: `src/test/resources/fixtures/`

## Build

Java 21 is required.

```bash
git clone https://github.com/BerkAkidil9/excel-xml-data-integration-tool.git
cd excel-xml-data-integration-tool
./mvnw verify
```

On a fresh machine, the Maven Wrapper downloads Maven when needed, and Maven downloads project dependencies.

Create the regular jar and runnable application jar:

```bash
./mvnw package
```

Runnable jar:

```text
target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar
```

## Run

Run the Swing application during development:

```bash
./mvnw exec:java -Dexec.mainClass=com.berk.dataintegration.app.DataIntegrationToolApp
```

Run the packaged Swing application after `./mvnw package`:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar
```

Convert Excel to XML:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar \
  convert \
  --direction excel-to-xml \
  --input examples/valid-invoice-data.xlsx \
  --output /tmp/valid-invoice-data.xml
```

Convert XML to Excel:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar \
  convert \
  --direction xml-to-excel \
  --input examples/valid-invoice-data.xml \
  --output /tmp/valid-invoice-data.xlsx
```

Write a CSV error report:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar \
  convert \
  --direction xml-to-excel \
  --input broken.xml \
  --output fixed.xlsx \
  --error-report errors.csv
```

The generated CSV starts with a total error count and grouped error summary, then includes the detailed structured error rows.
One `--error-report` path writes all errors from that conversion into the same CSV file; it does not create one report per error.

Create a blank Excel workbook template:

```bash
java -jar target/excel-xml-data-integration-tool-0.1.0-SNAPSHOT-app.jar \
  template \
  --output invoice-data-template.xlsx
```

CLI exit codes:

- `0`: conversion completed
- `1`: conversion failed because of input validation or conversion errors
- `2`: command-line usage error

## Desktop Usage

The Swing UI supports:

- direction selection
- input and output file choosers
- file chooser folder Back/Forward navigation
- automatic output path suggestions that update after input changes unless the output was manually customized
- reset button for clearing selected paths, status, counts, and errors
- overwrite confirmation
- background conversion work
- progress and status display
- record counts
- structured validation error table with a compact grouped summary
- error filtering and selected-error details
- selected-error copy
- CSV error report export with grouped summary and details
- Excel template saving

`Save Error Report` writes the full current error list into one CSV file. The file contains a total count, grouped summary rows, and detailed rows for every displayed error.

The application validates input before writing output. Failed conversions should not leave partial output behind.

## Testing

Run the unit test suite:

```bash
./mvnw test
```

Run the full verification build:

```bash
./mvnw verify
```

The automated tests cover calculation, validation, Excel reader/writer behavior, XML reader/writer behavior, conversion services, CLI behavior, error reports, and stable Swing controller/view-model/UI helper behavior.

## Project Structure

```text
.
├── LICENSE
├── README.md
├── docs/
│   ├── architecture/
│   ├── data-format/
│   └── development/
├── examples/
├── src/
│   ├── main/
│   └── test/
└── templates/
```

## Limitations

- Not a legally compliant e-invoice product.
- No UBL, UBL-TR, Peppol, or GIB integration.
- No digital signature, fiscal signing, or government service integration.
- No installer, code signing, or auto-update.
- No database or network service.
- No generic column mapping screen.
- No full IBAN checksum, national tax-number algorithm, or complete ISO currency database.
- No discounts, allowances, charges, withholding, exemptions, shipping data, attachments, or multiple tax types per line in the first supported version.

## License

This project is licensed under the MIT License. See `LICENSE`.
