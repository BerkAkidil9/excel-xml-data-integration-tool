# Testing Strategy

## Testing pyramid

### Unit tests

- domain construction
- field validation
- reference validation
- monetary calculation and rounding
- Excel cell parsers
- XML/domain mappers

### Component tests

- Apache POI reader against fixture workbooks
- Apache POI writer against generated temporary workbooks
- JAXB writer and reader
- XSD validator

### End-to-end tests

- valid Excel → XML
- valid XML → Excel
- Excel → XML → Excel round trip
- XML → Excel → XML semantic round trip

## Naming

- Unit tests: `*Test`
- Integration/component tests: `*IT`
- Test methods describe behavior in English.

## Maven execution

- `mvn test`: unit tests
- `mvn verify`: unit + integration tests

Use Maven Surefire for `*Test` and Maven Failsafe for `*IT` when integration tests are introduced.

## Required regression scenarios

- `0.005` rounding behavior under HALF_UP
- multiple line totals
- duplicate IDs
- missing reference
- wrong party role
- blank and partially populated Excel rows
- real Excel dates and ISO date strings
- formula cell with and without cached value
- malformed XML
- XXE/DOCTYPE rejection
- XSD-invalid XML
- XSD-valid but wrong totals
- UTF-8 Turkish characters
- optional fields omitted in XML
