package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionResult;
import com.berk.dataintegration.service.InvoiceConversionService;

public final class DefaultConversionUseCase implements ConversionUseCase {
    private final InvoiceConversionService conversionService;

    public DefaultConversionUseCase() {
        this(new InvoiceConversionService());
    }

    DefaultConversionUseCase(InvoiceConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public ConversionResult convert(ConversionRequest request) {
        return switch (request.direction()) {
            case EXCEL_TO_XML -> conversionService.convertExcelToXml(request.inputPath(), request.outputPath());
            case XML_TO_EXCEL -> conversionService.convertXmlToExcel(request.inputPath(), request.outputPath());
        };
    }
}
