# Validation ve veri sözleşmesi envanteri

Ham kanıtlar ve script'ler [evidence.zip](evidence.zip) arşivindedir. Kanıt bağlantılarının yanındaki yollar arşiv içindeki konumu gösterir; açma ve doğrulama adımları [REPRODUCE.md](REPRODUCE.md) içindedir.

Bu envanter kontrol katmanlarını ayırır. Aynı alanın Java ve XSD kısıtları bağımsız iş kuralı olarak toplanmaz. Kaynak yolları depo köküne göredir; hata kodlarının bütün kullanım yerleri [validation-codes.json](evidence.zip) (`validation-codes.json`), XSD bildirimlerinin satırları [xsd-inventory.json](evidence.zip) (`xsd-inventory.json`) içindedir.

## Alan kuralları

Temel kaynak: `src/main/java/com/berk/dataintegration/validation/InvoiceBatchValidator.java`. Aşağıdaki satırlar validator çağrılarını gösterir. Required alan boş/null olamaz; optional metin boşsa domain kurucuları bunu null yapar. Uzunluk ve biçim kısıtları ayrı koşullardır; tablodaki satır sayısı kontrol sayısı değildir.

| Varlık ve alanlar | Uygulanan alan koşulları | Kaynak satırları |
|---|---|---|
| Party.partyId, name | Zorunlu, en çok 40 ve 200 karakter | 54, 63 |
| Party.role | Null olamaz; Excel enum parser SUPPLIER/CUSTOMER kabul eder | 59; ExcelCellParser.java 75 |
| Party.taxNumber, phone, email | İsteğe bağlı; en çok 30, 30, 320; email temel regex | 64–70 |
| Address | Null olamaz; street ≤250, city zorunlu ≤100, postalCode ≤20, countryCode zorunlu iki büyük harf | 75–89 |
| PaymentAccount.paymentAccountId | Zorunlu ≤40 | 101 |
| accountHolderName, bankName | Zorunlu ≤200 | 106–107 |
| iban | Zorunlu; ham metin ≤34; boşlukları çıkarılmış değer 5–34 büyük harf/rakam | 108–113 |
| swiftCode | İsteğe bağlı; 8 veya 11 büyük harf/rakam | 115–119 |
| PaymentAccount.currencyCode | Zorunlu üç büyük harf | 120–121 |
| Invoice.invoiceNumber, issueDate | Numara zorunlu ≤50; tarih null olamaz | 134–142 |
| Invoice.currencyCode | Zorunlu üç büyük harf | 147–148 |
| supplierId, customerId, paymentAccountId | Zorunlu ≤40 | 149–151 |
| note | İsteğe bağlı ≤1000 | 152 |
| InvoiceLine.lineNumber | Pozitif tam sayı | 173–175 |
| itemCode, description | İlki isteğe bağlı ≤50; ikincisi zorunlu ≤500 | 180–181 |
| quantity | Zorunlu, >0, etkin scale ≤4 | 182–188 |
| unitCode | Zorunlu, 1–8 büyük harf/rakam | 190–193 |
| unitPrice | Zorunlu, ≥0, etkin scale ≤4 | 195–201 |
| taxRate | Zorunlu, 0–100 dahil, etkin scale ≤2 | 203–209 |

`validateMaxScale` trailing zero'ları ayıklar (331–342). IBAN checksum, ülke/para birimi kod listesi ve ulusal vergi numarası doğrulaması yoktur. Null batch/record korumaları aynı validator içindedir; bazı null liste elemanları domain `List.copyOf` tarafından daha önce reddedilir. Bunlar ek kullanıcı iş kuralı gibi sayılmaz.

## İlişkisel ve iş kuralları

| Anlam | Koşul veya hata kodu | Kaynak |
|---|---|---|
| Batch içinde party, hesap, fatura numarası tekilliği | DUPLICATE_PARTY_ID, DUPLICATE_PAYMENT_ACCOUNT_ID, DUPLICATE_INVOICE_NUMBER | InvoiceBatchValidator.java 55, 102, 135 |
| Fatura içinde satır numarası tekilliği | DUPLICATE_LINE_NUMBER | InvoiceBatchValidator.java 176 |
| Vade sırası | DUE_DATE_BEFORE_ISSUE_DATE | InvoiceBatchValidator.java 143 |
| En az bir fatura satırı | INVOICE_WITHOUT_LINES | InvoiceBatchValidator.java 159 |
| Satıcı ve müşteri referansının varlığı | MISSING_SUPPLIER_REFERENCE, MISSING_CUSTOMER_REFERENCE | InvoiceBatchValidator.java 227–245 |
| Referansın doğru ticari role gitmesi | INVALID_SUPPLIER_ROLE, INVALID_CUSTOMER_ROLE | InvoiceBatchValidator.java 232, 242 |
| Satıcı ve müşteri kimliğinin farklılığı | SUPPLIER_CUSTOMER_SAME | InvoiceBatchValidator.java 247 |
| Hesap referansının varlığı | MISSING_PAYMENT_ACCOUNT_REFERENCE | InvoiceBatchValidator.java 251–257 |
| Fatura ve hesabın para birimi eşitliği | CURRENCY_MISMATCH | InvoiceBatchValidator.java 258–264 |
| Excel satırının mevcut faturaya bağlanması | MISSING_INVOICE_REFERENCE | ExcelInvoiceReader.java, validation-codes.json içindeki kullanım |
| XML satır tutarı doğrulaması | LineNetAmount ve LineTaxAmount ayrı karşılaştırılır; ikisi aynı XML_LINE_TOTAL_MISMATCH kodunu kullanır | XmlBusinessConsistencyValidator.java 53–80 |
| XML fatura tutarı doğrulaması | TaxExclusiveAmount, TaxAmount, PayableAmount ayrı karşılaştırılır; üçü aynı XML_INVOICE_TOTAL_MISMATCH kodunu kullanır | XmlBusinessConsistencyValidator.java 91–129 |

Kod adedi ile koşul adedi eşit değildir. Bu nedenle CV'de toplam validation sayısı önerilmez.

## On dokümante madde denetimi

`docs/data-format/data-contract.md` bölüm 8'deki on madde:

| Madde | Değerlendirme |
|---|---|
| IDs compared after trimming | DomainText ve record kurucularında normalizasyon; bağımsız red kontrolü değil |
| IDs case-sensitive | String/Map/Set eşitliği politikası; bağımsız red kontrolü değil |
| Duplicate IDs are errors | Birden fazla kayıt türüne uygulanan tek dokümantasyon maddesi |
| Missing references are errors | Birden fazla referansa uygulanan genel madde |
| Supplier and customer differ | Bağımsız iş koşulu |
| Supplier role is SUPPLIER | Bağımsız rol koşulu |
| Customer role is CUSTOMER | Bağımsız rol koşulu |
| Invoice has at least one line | Bağımsız iş koşulu |
| Excel line points to invoice | Eksik referans genel maddesinin özel durumu; ikinci kez sayılmaz |
| Orphan parties/accounts allowed | İzin politikası; red kontrolü değil |

“10 documented business-rule checks” çıkarılmalıdır. Aynı belge dışında vade, para birimi ve XML tutar kuralları da vardır; bu on madde bütün validation kapsamını temsil etmez.

## Excel ve XML sözleşmesi

Zorunlu sayfalar ve tam header listesi [excel-columns.csv](evidence.zip) (`excel-columns.csv`) içinde: Parties 10, PaymentAccounts 6, Invoices 8, InvoiceLines 8; toplam **32 sütun konumu**. Bunlar 32 benzersiz alan adı değildir. Tüm domain ve XML yolları [contract-mapping.csv](evidence.zip) (`contract-mapping.csv`) içinde gösterilir.

Referanslar: `Invoices.supplier_id → Parties.party_id`, `Invoices.customer_id → Parties.party_id`, `Invoices.payment_account_id → PaymentAccounts.payment_account_id`, `InvoiceLines.invoice_number → Invoices.invoice_number`. Sonuncusu XML'de Line'ın üst Invoice öğesine yerleştirilmesini sağlar; Line içinde tekrarlanan InvoiceNumber alanı yoktur. Orijinal dokümandaki eşleme tablosu bu join sütununu atladığı için 31 satırdır. XML ayrıca iki satır tutarı ve üç fatura toplamı taşır; Excel bunları sütun olarak taşımaz.

`ExcelWorkbookContractValidator.java` 21–90: sayfa varlığı, sıralı/exact header eşleşmesi, fazladan dolu header reddi. `ExcelCellParser.java`: boş satır atlama, kısmi satırın korunması, metin/enum/tarih/decimal/integer türleri. Kaynak formül hücrelerini cached değerinden bağımsız reddeder; Excel format dokümanındaki daha yumuşak ifade uygulamanın davranışı değildir.

XSD'nin **13 isimli simpleType** tanımı: NonBlankText, Id40, InvoiceNumberType, CurrencyCodeType, CountryCodeType, RoleType, IbanType, SwiftCodeType, UnitCodeType, NonNegativeMoneyType, PositiveQuantityType, NonNegativeUnitPriceType, TaxRateType. Ayrıca element sırası, zorunluluk/çokluk, tarih/pozitif tamsayı türleri, sabit version=1.0, **3 key** (PartyIdKey, PaymentAccountIdKey, InvoiceNumberKey) ve **3 keyref** (SupplierPartyRef, CustomerPartyRef, PaymentAccountRef) vardır. XSD kısıtları Java alan/ilişki kurallarıyla örtüşür; toplama eklenmez. NonBlankText minLength=1 kullanır; whitespace-only metnin reddini ayrıca domain validator sağlar.

Örnek, şablon ve fixture XLSX dosyalarının bütün hücreleri OOXML ZIP içeriğinden okunup [workbook-inventory.json](evidence.zip) (`workbook-inventory.json`) içine kaydedildi. XML ve CSV fixture'ların kaynak hash'leri environment manifest'inde bulunur.

## Hata raporlama ve güvenlik

**44 ValidationCode enum sabiti** [validation-codes.json](evidence.zip) (`validation-codes.json`) içinde adlarıyla ve kaynak kullanım yerleriyle listelenir. Parse, contract, field, reference, business rule ve I/O kodları birlikte bulunur. Bu sayı güvenlik açığı sayısı veya 44 iş kuralı değildir. `ValidationError` kod, kategori, mesaj, alan, kayıt ID'si, dosya, sheet/XML path, row, column ve value bağlamını tutar. `ConversionErrorReportWriter.java` 15–34 CSV detayında 10 sütun, ayrıca özet ve gruplu özet yazar; bunlar validation koşulları değildir.

Güvenlik yapılandırmaları:

- `SecureXml.java` 18–25: XInclude kapalı; secure processing açık; DOCTYPE reddi; external general/parameter entities ve external DTD yüklemesi kapalı; boş entity resolver.
- `XmlSchemaProvider.java` 20–28: secure processing; external DTD ve schema erişimi boş allowlist; yalnızca gömülü XSD.
- `XmlSchemaValidator.java` 37–39 ve `XmlInvoiceReader.java` 77–78 aynı güvenli reader'ı kullanır. Aynı kontrol iki güvenlik kazanımı sayılmaz.
- `XmlInvoiceReaderTest.rejectsXxePayloadBeforeEntityResolution` tek XXE regresyon senaryosudur. Kapsamlı penetration test, bağımlılık taraması veya “sıfır açık” kanıtı değildir.

`InvoiceConversionService.java` 76–95 ve 120–134 geçici çıktı yazıp taşıyarak doğrulama hatasında mevcut çıktıyı korur; atomic move desteklenmezse normal replace fallback vardır. Her dosya sisteminde mutlak atomiklik iddia edilmez. CSV writer standart ayraç/tırnak escaping yapar; CSV formula injection koruması iddia edilmez.

## Mimari ve kapsam sınırları

[package-inventory.json](evidence.zip) (`package-inventory.json`), her package altındaki dosyaları adlarıyla verir. Domain içindeki **8 record**: Address, Invoice, InvoiceBatch, InvoiceLine, InvoiceLineAmounts, InvoiceTotals, Party, PaymentAccount; ayrıca PartyRole enum ve DomainText yardımcısı. Domain import'ları yalnız JDK'dır. Bu 10 dosya, 10 domain entity veya Maven modülü değildir.

Service paketindeki **2 davranış sınıfı**: InvoiceConversionService ve ConversionErrorReportWriter. Kalan dosyalar ConversionErrorView, ConversionRecordCounts, ConversionResult, ConversionReport, ConversionErrorType, ConversionErrorPhase, ConversionStatus, ConversionErrorGroup veri/sunum tipleridir. MoneyCalculationService ayrı calculation paketindedir; servis sayısını package dosya sayısıyla eşitlemeyin.

UI paketindeki 17 dosyanın tamamı görsel bileşen değildir. SwingConversionFrame ana pencere, NavigationFileChooser dosya seçici; ConversionController ve ConversionViewModel durum/akış yönetir; SwingWorkerConversionExecutor background iş yürütür. ErrorTableModel, filtre, formatter, direction/request/severity ve interface tipleri destek kodudur. Supplier/customer ticari veri rolleridir; kullanıcı yetkilendirmesi değildir.

Tek Maven projesi, sıfır child module. CI: `push` yalnız `main`; `pull_request` için branch filtresi yok; bir `verify` job, üç step (checkout, Java kurulumu, `./mvnw verify`). Surefire testleri ve Shade paketleme çalışır; coverage gate, deployment, Failsafe veya ayrı integration-test job yoktur. Testing Strategy belgesindeki `*IT`/Failsafe açıklaması uygulanmış yapılandırma sayılmaz.
