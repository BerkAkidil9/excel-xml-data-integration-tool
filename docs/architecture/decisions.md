# Project Decisions

Bu belge 14 Ekim 2026 tarihinde mevcut kod, `pom.xml` ve CI yapılandırmasıyla karşılaştırılarak güncellenmiştir.

## 1. Proje amacı

Bu proje, fatura verilerini önceden tanımlanmış bir Excel çalışma kitabı ile önceden tanımlanmış bir XML sözleşmesi arasında çift yönlü dönüştüren Java masaüstü uygulamasıdır.

Amaçlar:

- Java ile katmanlı ve test edilebilir tasarım göstermek
- Apache POI ile `.xlsx` okuma/yazma yapmak
- Jakarta XML Binding ile XML marshalling/unmarshalling yapmak
- XSD ve iş kurallarıyla veri doğrulamak
- Swing ile kullanılabilir masaüstü arayüzü sunmak
- Her özellik geliştirilirken otomatik test yazmak ve çalıştırmak

## 2. Neden doğrudan UBL veya UBL-TR değil?

UBL, OASIS tarafından tanımlanan ve Invoice dahil birçok ticari belgeyi kapsayan geniş bir XML şema ailesidir. Peppol BIS Billing, UBL üzerine ek zorunluluklar, kod listeleri ve iş kuralları uygular. Türkiye'deki e-Fatura teknik yapısı ise GİB'in UBL-TR profilleri ve sektörel kurallarıyla düzenlenir.

Bu standartları tam olarak desteklemek aşağıdaki ek kapsamı doğurur:

- Çok sayıda zorunlu ve koşullu alan
- Vergi, istisna, tevkifat ve fatura senaryosu kuralları
- Harici kod listeleri
- Schematron kontrolleri
- Sürüm ve ülke profili yönetimi
- GİB entegrasyonu, mali mühür veya imza süreçleri

Bu nedenle uygulama **UBL kavramlarından esinlenen, fakat UBL/UBL-TR uyumluluğu iddia etmeyen özel bir XML formatı** kullanır: `invoice-data-v1`.

Proje tek Maven projesidir; alt modül veya `ubl-adapter` içermez. Domain modeli dosya formatlarından bağımsız tutulur; gelecekte başka bir format desteği ayrı bir tasarım kararı gerektirir.

## 3. Teknoloji kararları

- Java 21
- Apache Maven 3.9+
- Java Swing
- Apache POI OOXML
- Jakarta XML Binding 4.x ve JAXB Runtime 4.x
- JUnit Jupiter
- XML Schema 1.0

### Yapılandırılmış sürümler

- Proje: `0.1.0-SNAPSHOT`
- Maven Wrapper dağıtımı: `3.9.11`
- Apache POI: `5.5.1`
- Jakarta XML Binding API: `4.0.5`
- JAXB Runtime: `4.0.9`
- JUnit Jupiter: `6.1.2`
- Maven Compiler Plugin: `3.15.0`
- Maven Surefire Plugin: `3.5.5`
- Maven Shade Plugin: `3.6.1`
- Maven Enforcer Plugin: `3.6.3`
- Exec Maven Plugin: `3.5.0`

Bağımlılık ve eklenti sürümleri [pom.xml](../../pom.xml) properties bölümünde, Maven sürümü [wrapper yapılandırmasında](../../.mvn/wrapper/maven-wrapper.properties) tutulur.

## 4. Neden Java 21?

Derleme ve test için Java 21 seçilmiştir. Maven Compiler `release=21` kullanır; Enforcer yalnız `[21,22)` aralığındaki Java sürümlerini ve Maven `3.9.0+` sürümlerini kabul eder. Dolayısıyla daha yeni bir JDK'ya geçiş yalnızca ortam değişikliği değildir; build yapılandırmasının da güncellenmesini gerektirir.

## 5. Excel tasarımı

Tek satırda bütün verileri tekrar eden düz bir sayfa yerine dört ilişkili sayfa kullanılır:

- `Parties`
- `PaymentAccounts`
- `Invoices`
- `InvoiceLines`

Bu tasarım:

- aynı müşteri veya satıcı bilgisinin tekrarını azaltır,
- referans bütünlüğünü test etmeyi sağlar,
- gerçek bir veri entegrasyonu senaryosuna daha çok benzer,
- Excel ile XML arasında anlamlı mapping gösterir.

## 6. XML tasarımı

XML kökü `InvoiceBatch` ve sürümlü namespace şöyledir:

`urn:berk:excel-xml-integration:invoice:v1`

XML içinde ortak taraflar ve ödeme hesapları bir kez tanımlanır; faturalar bunlara ID ile referans verir. Bu yapı Excel'deki çok sayfalı sözleşmeyle doğrudan uyumludur.

XML, uygulamaya gömülü [XSD](../../src/main/resources/schema/invoice-data-v1.xsd) ile doğrulanır. `SecureXml` DOCTYPE ve dış entity çözümlemesini kapatır; şema yükleyici dış DTD/şema erişimini engeller. JAXB DTO'ları `xml/dto` paketindedir; mapper sınıfları bunları domain kayıtlarına dönüştürür. XML okunurken önce güvenli parser ile XSD kontrolü, sonra JAXB okuma, domain doğrulaması ve hesaplanan tutarların karşılaştırılması yapılır.

## 7. Hesaplama kararı

Kullanıcı aşağıdaki temel değerleri girer:

- miktar
- birim fiyat
- vergi oranı

Uygulama aşağıdaki tutarları kendisi hesaplar:

- satır net tutarı
- satır vergi tutarı
- fatura vergi hariç toplamı
- fatura vergi toplamı
- ödenecek toplam

Yuvarlama: iki ondalık basamak, `RoundingMode.HALF_UP`.

Satır vergisi, yuvarlanmış net tutar yerine `quantity × unitPrice` çarpımının yuvarlanmamış değerinden hesaplanır. Net ve vergi tutarları ayrı ayrı yuvarlanır; fatura toplamları yuvarlanmış satır değerlerinin toplamıdır. Ayrıntılı formüller [veri sözleşmesinde](../data-format/data-contract.md) bulunur.

Hesaplanan tutarlar XML'de yazılır ve XML okunurken yeniden hesaplanıp doğrulanır. Tutarsız XML iş kuralı hatası üretir.

## 8. İlk sürüm dışında kalanlar

- UBL, UBL-TR veya Peppol uyumluluğu
- GİB bağlantısı
- Elektronik imza ve mali mühür
- İndirim ve ek ücretler
- Bir faturada birden fazla vergi türü
- Tevkifat, istisna ve iade senaryoları
- Döviz kuru hesaplama
- Veritabanı
- Kullanıcı girişi
- Serbest sütun eşleme ekranı
- Makro içeren Excel dosyaları

## 9. Katmanlar, çıktı ve hata yönetimi

- `domain`: JDK tipleri ve kayıtları; Apache POI, JAXB veya Swing bağımlılığı yoktur.
- `calculation` ve `validation`: parasal hesaplamalar, alan ve kayıtlar arası kurallar.
- `excel` ve `xml`: dosya sözleşmesi, okuma/yazma, mapping ve kaynak konumu bilgileri.
- `service`: iki dönüşüm yönünü yöneten `InvoiceConversionService`, sonuç/rapor tipleri ve `ConversionErrorReportWriter`.
- `cli` ve `app`: komut satırı ayrıştırması ve uygulama başlangıcı. Argümansız başlangıç Swing'i, argümanlı başlangıç CLI'ı seçer.
- `ui`: Swing pencere, controller/view-model, dosya seçici ve sunum yardımcıları. Dönüşüm `SwingWorker` ile EDT dışında yapılır; sonuçlar EDT üzerinde işlenir.

Servis, doğrulama başarılı olduğunda hedef dizinde geçici çıktı oluşturur ve tamamlanan dosyayı hedefe taşır. Atomik taşıma desteklenmezse normal replace kullanılır; hata durumunda geçici dosya temizliği denenir. Her dosya sisteminde mutlak atomiklik iddiası yoktur.

`ValidationError`, kod, kategori, alan, kayıt kimliği ve mevcut olduğunda dosya/sayfa/XML yolu, satır, sütun ve hatalı değeri taşır. CLI ve Swing aynı sonuçları sunar. CSV hata raporu toplam, gruplu özet ve on sütunlu detay bölümü içerir.

## 10. Test ve CI

Mevcut 103 JUnit testi 21 `*Test` sınıfında Surefire ile çalışır. Component ve servis entegrasyon senaryoları da bu suite içindedir; Failsafe veya ayrı `*IT` çalıştırması yapılandırılmamıştır. Testler headless çalışır; görünür Swing pencere ve görsel erişilebilirlik doğrulaması sağlamaz.

[Verify workflow](../../.github/workflows/verify.yml), bu Mac'e kurulu iki macOS ARM64 self-hosted runner kullanır. `Tests` ve `Package and CLI` işleri paraleldir; son `verify` işi ikisinin de başarılı olmasını ister. Paket kontrolü gerçek uygulama JAR'ı ile şablon ve dönüşüm komutlarını çalıştırır. Docker, GitHub-hosted build makinesi, deployment, coverage gate veya bağımlılık açığı taraması kullanılmaz.

Tetikleyiciler ve runner servis yönetimi [CI belgesinde](../development/ci.md), test kapsamı [test stratejisinde](../development/testing-strategy.md) açıklanır. [30 Eylül 2026 kanıt paketi](../cv-metrics-evidence/2026-09-30/REPORT.md) tarihsel ölçümdür; güncel CI veya coverage sonucu olarak yorumlanmaz.

## 11. Kaynaklar

- OASIS UBL 2.4: https://docs.oasis-open.org/ubl/UBL-2.4.html
- OASIS UBL Invoice schema: https://docs.oasis-open.org/ubl/os-UBL-2.4/xsd/maindoc/UBL-Invoice-2.4.xsd
- Peppol BIS Billing 3.0: https://docs.peppol.eu/poacc/billing/3.0/
- GİB e-Belge: https://ebelge.gib.gov.tr/efaturamevzuat.html
- OpenJDK 21: https://openjdk.org/projects/jdk/21/
- Apache POI: https://poi.apache.org/
- Jakarta XML Binding: https://jakarta.ee/specifications/xml-binding/
- JUnit: https://junit.org/
- Apache Maven: https://maven.apache.org/
