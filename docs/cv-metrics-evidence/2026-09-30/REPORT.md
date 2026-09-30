# CV için doğrulanabilir proje metrikleri

Ham kanıtlar ve script'ler [evidence.zip](evidence.zip) arşivindedir. Kanıt bağlantılarının yanındaki yollar arşiv içindeki konumu gösterir; açma ve doğrulama adımları [REPRODUCE.md](REPRODUCE.md) içindedir.

Bu inceleme Java/Backend ve genel Software Engineer başvuruları için kaynak koddan doğrulanabilen kapsamı, mevcut testlerin sonuçlarını ve yerel dönüşüm sürelerini belgeler. En güçlü adaylar **103 geçen test**, **4 ilişkili Excel sayfası** ve **10.000 fatura / 50.000 satır üzerinde doğrulanmış yerel dönüşüm ölçümü**. Tüm uygulamanın satır coverage'ı **%78,07377049**; UI dahil edilmiştir. Üretim kullanımı veya önce/sonra iyileşme ölçülmedi.

İnceleme tarihi 30 Eylül 2026, Europe/Istanbul. Commit `334f89ca0553962a7ecd5c144be5508faa581f84`. Çalışma kopyasındaki iki mevcut örnek dosyası değişikliği ölçümlere dahildir. Başlangıç durumu [environment.json](evidence.zip) (`environment.json`), tam fark [baseline.diff](evidence.zip) (`raw/baseline.diff`), örnek kopyaları [baseline-examples](evidence.zip) (`raw/baseline-examples/`) içinde korunmuştur. Son kontrol, başlangıçtaki **128 izlenen dosyanın tamamının hash'inin değişmediğini** doğruladı; uygulama, API, XSD ve mevcut testlere müdahale edilmedi. [Kontrol sonucu](evidence.zip) (`verification.json`)

## A Kaynaktan doğrulanan kapsam metrikleri

**Scope** mevcut yetenek/kapsamı, **Measured Result** belirli ortamda ölçülen sonucu, **Impact** ise kullanıcıya veya işletmeye etkisi ölçülmüş sonucu ifade eder. Bu incelemede Impact sınıfında doğrulanmış metrik yoktur. Aşağıdaki CV değeri değerlendirmeleri editoryal öneridir.

| Metrik ve exact değer | CV gösterimi ve anlam | Kanıt ve yöntem | Sınıf | CV değeri ve sınırlama |
|---|---|---|---|---|
| 4 zorunlu sayfa; sütun dağılımı 10+6+8+8=32 | “4 linked worksheets”; istenirse “32 columns” | [32 sütun ve kaynak satırları](evidence.zip) (`excel-columns.csv`), [XML eşlemeleri](evidence.zip) (`contract-mapping.csv`); ExcelSheetDefinition.java 7–47 sayıldı | Scope | Yüksek. Parties, PaymentAccounts, Invoices, InvoiceLines; database tablosu değil. 32, sayfa içindeki sütun konumudur; benzersiz alan adı sayısı değil |
| 2 dönüşüm yönü | “bidirectional Excel/XML conversion” | InvoiceConversionService.java 46–51; gerçek reader/writer çağrıları ve iki yönlü test/benchmark | Scope | Yüksek. Sabit proje sözleşmesi; genel amaçlı mapping veya UBL uyumluluğu değil |
| 2 giriş arayüzü | “Swing desktop and CLI” | DataIntegrationToolApp.java 21–32; CommandLineApp ve SwingConversionFrame incelendi | Scope | Orta/yüksek. REST API veya web frontend değil; görsel kullanım testi yapılmadı |
| 44 ValidationCode enum sabiti | Sayıyı kullanmadan “structured validation errors” | [Adları, declaration satırları ve kullanım yerleri](evidence.zip) (`validation-codes.json`); enum sabitleri sayıldı | Scope | Düşük sayısal değer, yüksek teknik değer. İş kuralı/kontrol sayısı değildir; I/O ve parse kodları da içerir |
| 1 XSD; 13 named simpleType, 3 key, 3 keyref | “XSD and cross-record validation” | [XSD envanteri](evidence.zip) (`xsd-inventory.json`), [adlar ve örtüşmeler](validation-inventory.md); şema bildirimleri sayıldı | Scope | Teknik kapsam değerli; sayılar CV'yi güçlendirmez. Java kurallarıyla tekrar eden kısıtlar toplanmadı |
| 10 Java package, 78 main Java dosyası (1 package-info dahil); 1 Maven projesi, 0 child module | “separated domain, conversion, validation, and presentation concerns” | [Package içindeki tüm dosyalar](evidence.zip) (`package-inventory.json`), [type/import envanteri](evidence.zip) (`source-inventory.json`), pom.xml | Scope | Package/dosya sayısını çıkart. Domain'deki 8 record + 1 enum + 1 helper mimari başarı metriği değildir; domain import'ları yalnız JDK |
| 1 CI workflow, 1 job, 3 step; 2 event türü | “Maven and GitHub Actions CI verification” | [.github/workflows/verify.yml](../../../.github/workflows/verify.yml), pom.xml; tetikleyici, job ve komut ayrı okundu | Scope | Orta/yüksek teknik değer. Push yalnız main, pull_request filtresiz. Çalıştırılan komut `./mvnw verify`; deployment/coverage gate yok. Uzak Actions koşusu bu incelemede çalıştırılmadı |
| Normal JAR 176.476 byte; bağımlılıkları içeren app JAR 21.297.575 byte | Gerekirse “runnable application JAR”; boyutu kullanma | [Exact boyut, MiB ve SHA-256](evidence.zip) (`artifact-sizes.json`); temiz verify sonrasında filesystem ölçümü | Measured Result | Düşük. Sırasıyla 0,16830063 ve 20,31095028 MiB; küçülme/bellek kullanımını göstermez |

Validation katmanları, tüm alan koşulları, dört referans ilişkisi, XML tutarları, güvenlik kontrolleri, domain/servis/UI isimleri ve “10 documented business-rule checks” denetimi [validation-inventory.md](validation-inventory.md) içindedir. Dokümandaki on madde normalizasyon, izin politikası ve birbiriyle örtüşen referans maddeleri içerir. **On bağımsız validation kontrolü iddiası çıkarılmalıdır.**

## B Ölçülen sonuçların CV değeri

| Metrik ve exact değer | CV gösterimi ve anlam | Kanıt ve yöntem | Sınıf | CV değeri ve sınırlama |
|---|---|---|---|---|
| 103 declaration, 103 execution, 103 passed; 0 failure/error/skipped; 21 suite | “103 passing JUnit tests” | [Temiz verify konsolu](evidence.zip) (`raw/clean-verify.log`), [Surefire XML](evidence.zip) (`raw/surefire/`), [metot ve kategori envanteri](evidence.zip) (`test-inventory.csv`); XML testcase ve suite toplamları uzlaştırıldı | Measured Result | Yüksek. Bu çalışma kopyasının başarılı koşusu; 103 unit test veya 103 E2E test değil |
| Tüm uygulama LINE 1905/2440=%78,07377049; BRANCH 558/905=%61,65745856 | İstenirse “78% application line coverage” | [JaCoCo XML sayaçları](evidence.zip) (`raw/jacoco.xml`), [özet](evidence.zip) (`coverage-summary.json`); mevcut 103 test, izole kopya, hiçbir uygulama paketi dışlanmadı | Measured Result | Orta. Branch %61,66; coverage doğruluk veya kusursuzluk kanıtı değil. “78% test coverage” yerine line kapsamını belirt |
| Core LINE 1441/1619=%89,00555899; BRANCH 375/554=%67,68953069 | CV'de tek başına kullanma; gerekirse “89% core line coverage” ve kapsamı açıkla | [Özet ve core package listesi](evidence.zip) (`coverage-summary.json`); domain, calculation, validation, excel, xml, xml/dto, service sayaç toplamı | Measured Result | İkincil. UI, CLI ve app hariçtir; genel coverage gibi sunmak yanıltır |
| UI LINE 341/662=%51,51057402; BRANCH 133/279=%47,67025090 | Sayıyı CV'ye ekleme; kapsam sınırı olarak sakla | [Paket/sınıf sayaçları](evidence.zip) (`coverage-summary.json`); ui paketinin bütün sınıfları | Measured Result | Düşük. Ana pencere hiç çalışmıyor; headless yardımcı testleri görsel E2E değildir |
| Tüm uygulama METHOD 412/483=%85,30020704; INSTRUCTION 9328/11923=%78,23534345 | CV'de line coverage yeterli | [JaCoCo XML](evidence.zip) (`raw/jacoco.xml`); kendi counter türleriyle hesaplandı | Measured Result | Düşük ek değer. Instruction coverage statement coverage değildir |
| 6 benchmark senaryosu, her birinde 15 ölçüm; 18 JVM, 54 ısınma + 90 ölçüm; 144 doğrulanmış çıktı | “10,000 invoices / 50,000 line items”; seçilirse C'deki medyanlar | [Ham tekrarlar ve exit code'lar](evidence.zip) (`raw/benchmark/`), [özet](evidence.zip) (`benchmark-summary.json`), [harness](evidence.zip) (`scripts/ConversionBenchmark.java`); detaylı protokol C'de | Measured Result | Yüksek, yerel/sentetik sınırıyla. Tüm senaryolar başarılı; üretim kapasitesi veya maksimum desteklenen boyut değil |

Benchmark sürelerinin ve fatura/saniye değerlerinin her biri C tablosunda exact olarak gösterilir; aynı yöntem, sınıf ve sınırlamalar altı satıra da uygulanır. Küçük senaryolar için CV'ye ayrı sayı önerilmez. Büyük senaryo için CV'de **1,8 s / 8,2 s medyan** biçimi yeterlidir; yön ve donanım bilgisi korunmalıdır.

## C Yeni ölçümler ve tekrar üretilebilirlik

### Test sonucu ve sınıflandırma

`./mvnw -o clean verify` başarılı. 103 `@Test` bildirimi 103 Surefire testcase ile bire bir eşleşiyor. Parameterized/dynamic test genişlemesi yok; 22 test Java dosyasından biri `TestBatches` yardımcısı, 21'i test sınıfıdır. Eski target raporlarının mtime/hash'leri manifest'te kaydedildi; temiz koşuyla yeniden üretilen raporlar kullanıldı. Kaynak test metotları ve açıklamalı sınıflandırma [test-inventory.csv](evidence.zip) (`test-inventory.csv`) içinde yer alır.

| Ana kategori | Test | Kapsam |
|---|---:|---|
| Unit | 57 | Bellek içi hesaplama, validator, durum ve yardımcı mantık; bazı collaborator'lar stub |
| Component | 37 | Gerçek Excel/XML reader/writer, CSV yazıcı, CLI help/usage, Swing bileşeni ve EDT davranışı |
| Integration | 6 | 4 conversion service testi; gerçek CSV/template yazan 2 controller testi |
| Uygulama akışı | 3 | CommandLineApp.run üzerinden dönüşüm, hatalı input/CSV, template; aynı JVM içinde |
| Toplam | 103 | Her test tek ana kategoride |

UI ve security etiketleri bu toplama eklenmez. CLI testleri process başlatmaz, `main`/JAR dağıtım yolunu test etmez. Desktop pencere, görsel uçtan uca ve erişilebilirlik kapsamı yoktur. Testing Strategy dokümanının hedeflediği tüm round-trip ve `*IT` kapsamının mevcut olduğu varsayılmadı; POM'da Failsafe yoktur.

### Coverage

Java 21 uyumlu sabit **JaCoCo 0.8.12**, geçici proje kopyasında aynı testlere bağlandı. Headless Surefire ayarı korundu. [Resmi Java destek geçmişi](https://www.jacoco.org/jacoco/trunk/doc/changes.html), [yalnız geçici POM farkı](evidence.zip) (`raw/coverage-pom.patch`), [coverage koşu manifest'i](evidence.zip) (`raw/coverage-run.json`). Kaynak hash'leri ana kopyayla eşleşiyor. Denetim harness'i ve evidence kontrolleri bu suite'e eklenmedi.

| Kapsam | Line covered / total | Line % | Branch covered / total | Branch % |
|---|---:|---:|---:|---:|
| Tüm uygulama | 1905 / 2440 | 78,07377049 | 558 / 905 | 61,65745856 |
| Core | 1441 / 1619 | 89,00555899 | 375 / 554 | 67,68953069 |
| UI | 341 / 662 | 51,51057402 | 133 / 279 | 47,67025090 |
| CLI | 120 / 141 | 85,10638298 | 50 / 70 | 71,42857143 |
| App giriş noktası | 3 / 18 | 16,66666667 | 0 / 2 | 0 |

Tüm uygulama missed değerleri: line **535**, branch **347**, method **71**, instruction **2595**. Exact karşılıklar ve bütün sınıf/paketler [coverage-summary.json](evidence.zip) (`coverage-summary.json`) içinde. JaCoCo executable bytecode sayar; yorum/boş satırlar paydaya girmez, compiler-generated bazı yapılar araç tarafından filtrelenir. 93 raporlanabilir bytecode sınıfı ile 78 Java dosyası aynı ölçü değildir.

Gizlenmeyen sıfır satır coverage'lı sınıflar: `SwingConversionFrame` 0/250, `SwingConversionFrame$1` 0/2, `DefaultConversionUseCase` 0/8, `XmlReadException` 0/2. **Statement coverage ölçülmedi.**

### Yerel dönüşüm benchmark sonuçları

Apple M5, 10 logical CPU, 16 GiB RAM, macOS 27.0 arm64, Homebrew OpenJDK 21.0.12. JVM `-Xms512m -Xmx2g -Djava.awt.headless=true`; varsayılan GC; coverage agent yok. Maven 3.9.11, POI 5.5.1, JAXB API 4.0.5/runtime 4.0.9, JUnit 6.1.2, Surefire 3.5.5. Diğer sabitlenmiş plugin ve resolved test classpath sürümleri [environment.json](evidence.zip) (`environment.json`) içinde.

Her senaryoda **3 ayrı JVM × (3 ısınma + 5 ölçüm)**. Tablodaki medyan, 15 ölçümün havuzlanmış medyanıdır; saniyeler ham nanosecond değerlerinden hesaplandı. Üç fork'ın ayrı medyanları [benchmark-summary.json](evidence.zip) (`benchmark-summary.json`) içinde. Fatura/saniye = N / medyan saniye; satır/saniye veya HTTP request/saniye değildir.

| Yön | Fatura | Satır | Medyan saniye | Min saniye | Max saniye | Fatura/saniye |
|---|---:|---:|---:|---:|---:|---:|
| Excel → XML | 100 | 500 | 0.034829667 | 0.029145084 | 0.050073875 | 2871.115592 |
| XML → Excel | 100 | 500 | 0.105756833 | 0.090639625 | 0.144330750 | 945.565380 |
| Excel → XML | 1000 | 5000 | 0.146275166 | 0.138890834 | 0.165975750 | 6836.430457 |
| XML → Excel | 1000 | 5000 | 0.783984458 | 0.742846208 | 0.846510916 | 1275.535490 |
| Excel → XML | 10000 | 50000 | 1.816660917 | 1.720930250 | 1.968879334 | 5504.604578 |
| XML → Excel | 10000 | 50000 | 8.155078375 | 8.070736417 | 8.545815209 | 1226.229785 |

Veri üretimi, servis kurulumu ve çıktı doğrulaması zamanlamanın dışında; dosya okuma, validation, dönüşüm, hesaplama, yazma ve geçici dosyadan taşıma içeride. Üç veri boyutu, her biri 2 party/1 hesap ve fatura başına 5 satır kullanır. Tarihler, kimlikler, UTF-8 alanlar ve parasal değerler deterministiktir. Her faturada net 132.50, vergi 26.50, ödenecek 159.00; en büyük batch ödenecek toplamı 1.590.000,00 TRY.

Her iterasyonda kayıt sayıları, bütün domain alanları ve parasal toplamlar doğrulandı. XML serialized tutarları ayrıca bağımsız StAX kontrolünden geçti. Raw CSV'deki `validated=true` yalnız bu kontrollerden sonra yazılır. **6/6 senaryo, 18/18 JVM başarılı; timeout veya OOM yok.** 20 dakikalık timeout/fork tanımlıydı. Bu, yalnız denenmiş boyutlar için sonuçtur.

Bu protokol sıralı yerel dosya dönüşümüdür. Input önceden üretilip okunur; cache sıcak, disk cache flush ve fsync yoktur. Çıktı kontrolü iterasyonlar arasında heap/cache davranışını etkiler. JVM'ler arası başlangıç/JIT farklılıkları özellikle 100 faturalık senaryoda görünür. Data çeşitliliği sınırlıdır; concurrency, production workload veya maksimum bellek testi yapılmadı. 2 GiB heap ayarı ölçülmüş peak bellek değildir.

Ölçüm sırasında Log4j provider bulunamadığı mesajı JVM stdout'una CSV başlığından önce yazıldı. Tüm fork'lar exit 0 ile bitmesine rağmen ilk özetleyici bu ön satırı header sanıp `KeyError: phase` verdi. [İlk log](evidence.zip) (`raw/benchmark-run.log`) korunmuştur. Sadece audit CSV okuyucusu gerçek header'ı bulacak biçimde düzeltildi; ham süreler ve uygulama kodu değişmedi. Özet aynı 18 koşudan yeniden üretildi, yeni koşu seçilmedi.

Tekrar çalıştırma komutları ve tam sınırlar [REPRODUCE.md](REPRODUCE.md) içinde. Bağımlılıklar cache'deyken tek temiz build alındı; log'daki süre bir build performans başarısı olarak kullanılmadı. Üç build benchmark'ı yapılmadı.

## D Kullanılmaması gereken iddialar

- **“10 documented business-rule checks”**: On doküman maddesi on bağımsız kontrol değil. Yerine “XSD and cross-record validation” kullanın.
- **“CI/CD verification”**: Deployment yok; “CI verification” kullanın.
- **“103 unit tests” veya “103 E2E tests”**: Karışık suite; “103 passing JUnit tests” doğru.
- **“89% application coverage” veya “78% statement coverage”**: %89 yalnız core line coverage; statement ölçülmedi.
- **“Production throughput of 5,505 invoices/s”**, “supports unlimited invoices”, “X times faster”: Yerel benchmark, baseline veya yük testi değildir. Küçük senaryodaki daha yüksek hız büyük veriye taşınamaz.
- **“Zero errors”, “100% accurate”, “eliminated manual work”**: Test/benchmark başarısı bütün olası girdiler veya kullanıcı etkisi için kanıt değildir.
- **“Secure application”, “zero vulnerabilities”, “CSV injection protection”**: Belirli XML parser ayarları ve bir XXE regresyonu var; kapsamlı güvenlik denetimi yok.
- **“Financially lossless Excel conversion”**: Domain hesaplamaları BigDecimal olsa da ExcelInvoiceWriter.java 163–167 numeric hücreye `doubleValue()` yazar. Her büyüklükte ondalık hassasiyet korunduğu iddia edilemez.
- **“UBL/UBL-TR/Peppol/GİB compliant”**, “IBAN checksum validation”: Uygulama özel şema ve biçim kontrolleri kullanır; bu uyumluluklar yoktur.
- **“10 modules”, “4 database tables”, “2 user access roles”**: Package, worksheet ve ticari roller farklı kavramlardır.

## E Mevcut dört bullet ile eşleştirme

Plan, mevcut dört bullet'ın konularını veriyor; tam özgün İngilizce metinler sağlanmadığı için aşağıdaki değerlendirme anlam/kapsam düzeyindedir.

| Mevcut konu | Korunan teknik katkı | Düzenleme |
|---|---|---|
| Çift yönlü desktop/CLI dönüşümü | Java 21, Swing, CLI, Apache POI, JAXB, dört ilişkili sayfa | 4 sayfa ana metrik; 32 sütun isteğe bağlı. Benchmark'ı ayrı bullet olarak sunmak cümleyi okunur tutar |
| Validation, güvenlik, BigDecimal | XSD, referans/iş kuralları, DOCTYPE/XXE önlemleri, scale 2 HALF_UP, bağlamlı hatalar, CSV export ve template | “10” kaldırılır; finansal mutlak doğruluk veya üretim hata azalması eklenmez |
| Katmanlı mimari | Domain bağımsızlığı, conversion service ve presentation ayrımı | Package sayısı çıkartılır. Domain import kontrolü somut kanıt; “microservices” veya “multi-module” eklenmez |
| JUnit, Maven, GitHub Actions | 103 geçen test, paketleme ve doğrulama akışı | “CI verification”; coverage kullanılırsa tüm uygulama için “78% line coverage”. Sayı audit sonrası ölçülen mevcut durumdur; tarihsel CI gate iddiası değildir |

## F Önerilen İngilizce CV bulletları

Beş bullet birlikte kullanılabilir; daha kısa CV için dördüncü, benchmark bullet'ı çıkarılabilir. Kanıt satırları CV metnine dahil edilmez. Birinci şahıs katkı fiilleri mevcut dört bullet'taki sahiplik varsayımını korur; bu audit Git geçmişinden bireysel yazarlık oranını ölçmez.

1. **Built a Java 21 desktop and CLI tool for bidirectional Excel/XML invoice conversion using Swing, Apache POI, and JAXB across 4 linked worksheets and 32 columns.**

   Kanıt: [sütun envanteri](evidence.zip) (`excel-columns.csv`), [eşleme](evidence.zip) (`contract-mapping.csv`), InvoiceConversionService.java 46–51, DataIntegrationToolApp.java 21–32. Scope.

2. **Implemented XSD and cross-record validation, hardened XML parsing, and BigDecimal calculations with HALF_UP rounding, with structured errors, CSV error export, and Excel template generation.**

   Kanıt: [validation ve güvenlik kaynakları](validation-inventory.md); MoneyCalculationService.java 14–51, ConversionErrorReportWriter.java, ExcelTemplateWriter.java. “Hardened” yalnız belgelenmiş parser kontrollerini anlatır. Scope.

3. **Separated domain models and monetary calculations from file adapters, conversion orchestration, and presentation, sharing conversion services between Swing and CLI workflows.**

   Kanıt: [import/type envanteri](evidence.zip) (`source-inventory.json`), InvoiceConversionService, CommandLineApp, DefaultConversionUseCase. Domain'de POI/JAXB/Swing bağımlılığı yok; package sayısı kullanılmadı. Scope.

4. **Benchmarked local conversion of 10,000 synthetic invoices with 50,000 line items at median times of 1.8 s for Excel-to-XML and 8.2 s for XML-to-Excel on Apple M5, validating output fields and totals.**

   Kanıt: [18 JVM / ham ölçümler](evidence.zip) (`raw/benchmark/`), [özet](evidence.zip) (`benchmark-summary.json`), [harness](evidence.zip) (`scripts/ConversionBenchmark.java`). Her yön 15 measured iteration; 2 GiB max heap; ısınmış sıralı yerel dosya dönüşümü. Measured Result.

5. **Verified 103 passing JUnit tests with 78% application line coverage and configured Maven builds with GitHub Actions CI verification.**

   Kanıt: [Surefire toplamları](evidence.zip) (`test-summary.json`), [coverage sayaçları](evidence.zip) (`coverage-summary.json`), [CI workflow](../../../.github/workflows/verify.yml). Coverage bu audit'te ölçüldü; GitHub Actions içinde JaCoCo gate bulunduğu anlamına gelmez. Measured Result + Scope.

## G Bulunamayan veya ölçülmeyen sonuçlar

| Konu | Durum | Gerekçe |
|---|---|---|
| Web frontend, Lighthouse, HTTP response time, REST endpoint | Uygulanamaz | Bu sürüm yerel Swing/CLI uygulaması; HTTP hizmeti yok |
| Database sorgu/saklama performansı | Uygulanamaz | Database entegrasyonu yok; worksheet referansları SQL foreign key değildir |
| Kullanıcı yetkilendirmesi / access role sayısı | Uygulanamaz | Login/authorization yok; supplier/customer veri rolüdür |
| Üretim trafiği, aktif kullanıcı, uptime/SLA | Kanıt yok | Telemetri veya deployment verisi sağlanmadı |
| Zaman/maliyet tasarrufu, hata oranı azalması, önce/sonra hızlanma | Ölçülmedi | Karşılaştırma baseline'ı veya kullanıcı çalışması yok |
| Peak RSS/heap, eşzamanlı yük, soğuk başlangıç | Ölçülmedi | Sabit heap limiti bellek kullanımı değildir; JVM startup süre dışında |
| Statement coverage | Araç çıktısı yok | JaCoCo instruction counter'ı yerine kullanılamaz |
| UI görünümü ve erişilebilirlik | Ölçülmedi | Headless testler görünür pencere veya assistive technology test etmez |
| Windows/Linux çalışma zamanı uyumu | Ölçülmedi | Workflow ubuntu-latest tanımlar; yerel audit yalnız macOS'ta çalıştı |
| Güvenlik açığı / dependency vulnerability sayısı | Ölçülmedi | SAST/SCA veya penetration testi yapılmadı |
| Build hızı iyileşmesi, JAR küçülmesi | Kanıt yok | Tek temiz build ve anlık boyut; karşılaştırmalı ölçüm yok |
| CSV import | Mevcut değil | CSV fixture'lar var; uygulama CSV hata raporu export eder, CSV dönüşüm yönü sağlamaz |

Bu eksikler sıfır değer olarak raporlanmaz. Kaynakta görülen belge/uygulama farkları (formül reddi, Failsafe hedefi, eksik join mapping satırı) inceleme kapsamında düzeltilmedi; [validation envanterinde](validation-inventory.md) kaydedildi.

## H CV için seçilen metrikler

Öncelik sırasıyla dört metrik grubu:

1. **103 passing JUnit tests** — en güçlü kalite kanıtı; 21 suite bilgisi mülakat/evidence için saklanabilir. [Exact sonuç](evidence.zip) (`test-summary.json`)
2. **4 linked worksheets, 32 columns** — ilişkili veri modelinin somut kapsamı. CV dar ise yalnız dört sayfa yeterli. [Adlar ve satırlar](evidence.zip) (`excel-columns.csv`)
3. **10,000 invoices / 50,000 line items; 1.8 s ve 8.2 s local median conversion** — yalnız yön, sentetik veri ve Apple M5 bilgisiyle birlikte kullanın. [Exact ölçümler](evidence.zip) (`benchmark-summary.json`)
4. **78% application line coverage** — isteğe bağlı destek metriği; %61,66 branch ve düşük UI kapsamı evidence'da açık. “89%” core sonucunu bunun yerine geçirmeyin. [Covered/missed sayaçları](evidence.zip) (`coverage-summary.json`)

Bu dört grup dışında package, sınıf, hata kodu ve JAR boyutu sayılarını CV'ye doldurmak yerine teknik katkıları anlatmak daha değerlidir. Uygulama kaynakları ve mevcut örnek değişiklikleri korundu; bu klasör yalnız rapor, ölçüm harness'i ve doğrulanabilir evidence ekler.
