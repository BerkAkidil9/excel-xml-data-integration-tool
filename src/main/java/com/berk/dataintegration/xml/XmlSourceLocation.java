package com.berk.dataintegration.xml;

record XmlSourceLocation(
        String sourceFile,
        String xmlPath,
        String field,
        String value
) {
}
