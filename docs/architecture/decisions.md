# Project Decisions

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

Bu nedenle ilk sürümde **UBL kavramlarından esinlenen, fakat UBL/UBL-TR uyumluluğu iddia etmeyen özel bir canonical XML formatı** kullanılacaktır.

Gelecekte ayrı bir `ubl-adapter` modülü eklenebilir. Ana domain modeli ve dönüşüm servisleri buna hazırlanacak, ancak ilk sürümün kapsamı büyütülmeyecektir.

## 3. Teknoloji kararları

- Java 21
- Apache Maven 3.9+
- Java Swing
- Apache POI OOXML
- Jakarta XML Binding 4.x ve JAXB Runtime 4.x
- JUnit Jupiter
- XML Schema 1.0

### Önerilen sabit sürümler

- Apache POI: `5.5.1`
- Jakarta XML Binding API: `4.0.5`
- JAXB Runtime: `4.0.9`
- JUnit Jupiter: `6.1.2`
- Maven Compiler Plugin: `3.15.0`
- Maven Surefire Plugin: `3.5.5`

Sürümler `pom.xml` properties bölümünde merkezi olarak tutulmalıdır.

## 4. Neden Java 21?

Java 21 uzun süre desteklenen bir sürümdür ve proje için yeterince modern olup Swing, Apache POI ve Jakarta XML Binding ile düşük riskli bir taban sağlar. Daha yeni bir JDK kullanmak mümkün olsa da portfolyo projesi için Java 21 daha geniş uyumluluk ve daha az sürpriz sunar.

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

XML kökü `InvoiceBatch` olacaktır ve sürümlü namespace kullanacaktır:

`urn:berk:excel-xml-integration:invoice:v1`

XML içinde ortak taraflar ve ödeme hesapları bir kez tanımlanır; faturalar bunlara ID ile referans verir. Bu yapı Excel'deki çok sayfalı sözleşmeyle doğrudan uyumludur.

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

## 9. Kaynaklar

- OASIS UBL 2.4: https://docs.oasis-open.org/ubl/UBL-2.4.html
- OASIS UBL Invoice schema: https://docs.oasis-open.org/ubl/os-UBL-2.4/xsd/maindoc/UBL-Invoice-2.4.xsd
- Peppol BIS Billing 3.0: https://docs.peppol.eu/poacc/billing/3.0/
- GİB e-Belge: https://ebelge.gib.gov.tr/efaturamevzuat.html
- OpenJDK 21: https://openjdk.org/projects/jdk/21/
- Apache POI: https://poi.apache.org/
- Jakarta XML Binding: https://jakarta.ee/specifications/xml-binding/
- JUnit: https://junit.org/
- Apache Maven: https://maven.apache.org/
