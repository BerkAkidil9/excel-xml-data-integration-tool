# Excel/XML Data Integration Tool

A desktop and command-line application for converting invoice data between a fixed Excel workbook contract and a fixed XML `invoice-data-v1` contract.

The application focuses on a predefined invoice data exchange format. It is intentionally not a generic Excel/XML mapper and does not claim legal e-invoice, UBL, UBL-TR, Peppol, or GIB compliance.

## Status

The v1 scope is implemented:

- Excel `.xlsx` contract validation, reading, and writing with Apache POI.
- XML reading and writing with Jakarta XML Binding.
- XSD validation for the repository-defined `invoice-data-v1` XML format.
- Domain and business validation for parties, payment accounts, invoices, references, dates, currencies, and invoice lines.
- Central monetary calculation with `BigDecimal`, scale 2, and `RoundingMode.HALF_UP`.
- Structured conversion errors with stable codes, categories, source locations, fields, record identifiers, and offending values where available.
- Excel to XML and XML to Excel conversion services with temporary output handling.
- CLI support for conversion, CSV error reports, and blank Excel template generation.
- Swing UI for local desktop use.
- Maven packaging for a runnable application jar.
- GitHub Actions verification with Java 21 and `./mvnw verify`.

The application is packaged as a runnable local desktop application. It is not distributed as a signed installer and does not include auto-update support.

## Features

- Bidirectional conversion:
  - Excel to XML
  - XML to Excel
- Fixed Excel workbook structure:
  - `Parties`
  - `PaymentAccounts`
  - `Invoices`
  - `InvoiceLines`
- Fixed XML namespace:
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
  - CSV error report export
  - blank Excel template generation
  - sample valid Excel and XML files

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

The repository defines one logical invoice data contract:

- Shared domain and validation contract: `docs/data-format/data-contract.md`
- Excel workbook contract: `docs/data-format/excel-format.md`
- XML contract: `docs/data-format/xml-format.md`
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
- automatic output path suggestions
- overwrite confirmation
- background conversion work
- progress and status display
- record counts
- structured validation error table
- error filtering and selected-error details
- selected-error copy
- CSV error report export
- Excel template saving

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

The automated tests cover calculation, validation, Excel reader/writer behavior, XML reader/writer behavior, conversion services, CLI behavior, and stable Swing controller/view-model behavior.

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
- No discounts, allowances, charges, withholding, exemptions, shipping data, attachments, or multiple tax types per line in v1.

## License

This project is licensed under the MIT License. See `LICENSE`.
