# CV metriklerini yeniden ölçme

GitHub'da bu inceleme dört dosya olarak tutulur: [sonuç raporu](REPORT.md), [validation envanteri](validation-inventory.md), bu kılavuz ve [kanıt arşivi](evidence.zip). Arşiv, orijinal incelemenin 160 dosyasını eksiksiz içerir: ham test/coverage raporları, benchmark tekrarları, ortam ve kaynak manifest'leri, envanterler, script'ler, ilk raporların kopyaları ve SHA-256 listesi. Ölçüm değerleri paketleme sırasında değiştirilmedi.

Arşiv boyutu **288.497 byte**. SHA-256:

```text
2e365bfbe431a6a80ae7297ed71b914f7dd4cb78d6048ea13dbc653318fe315b
```

## Kanıtları açma ve doğrulama

Aşağıdaki komutları depo kökünde çalıştırın. Çıktılar geçici dizine açılır; repo tekrar yüzlerce dosyayla dolmaz.

```sh
audit_bundle="$PWD/docs/cv-metrics-evidence/2026-09-30/evidence.zip"
audit_root=$(mktemp -d /tmp/cv-metrics-review.XXXXXX)
unzip -q "$audit_bundle" -d "$audit_root/evidence"
(cd "$audit_root/evidence" && shasum -a 256 -c checksums.sha256)
```

`checksums.sha256`, kendisi dışındaki 159 dosyayı doğrular. Arşivdeki Markdown belgeleri ölçüm anındaki sürümlerdir; GitHub'daki belgeler arşiv bağlantılarını ve bu paketleme kılavuzunu içerir.

| Arşiv içindeki dosya veya klasör | İçerik |
|---|---|
| `environment.json` | Commit, başlangıçtaki kaynak hash'leri, CPU/RAM, JDK/Maven ve bağımlılık sürümleri |
| `test-summary.json`, `test-inventory.csv` | 103 testin sonuçları, metotları, kaynak satırları ve kategorileri |
| `coverage-summary.json`, `raw/jacoco.*` | Tüm uygulama, core, UI ve sınıf coverage sayaçları; ham JaCoCo verileri |
| `benchmark-summary.json`, `raw/benchmark/` | Altı senaryo, 18 JVM, 90 ölçüm, 144 doğrulanmış çıktı ve ham süreler |
| `raw/surefire/`, `raw/coverage-surefire/` | Normal ve coverage koşularının ayrı test raporları |
| `raw/baseline-examples/`, `raw/baseline.diff` | Ölçümde kullanılan değiştirilmiş örnek dosyaları ve başlangıç farkı |
| `scripts/` | Java harness, coverage ve benchmark runner'ları, envanter ve uzlaştırma script'leri |
| `verification.json` | İlk incelemedeki kaynak koruma ve metrik uzlaştırma sonucu |
| `REPRODUCE.md` | İlk ölçümün ayrıntılı yöntem ve komutları |

## Yeni ölçüm için izole çalışma kopyası

İlk ölçüm commit'i `334f89ca0553962a7ecd5c144be5508faa581f84`. O koşu iki değiştirilmiş örnek veri dosyasını da kullanır; yalnız commit'i checkout etmek aynı girdileri oluşturmaz. Mevcut çalışma kopyasına reset uygulamak yerine, yukarıda oluşturulan geçici dizinde ayrı bir clone hazırlayın:

```sh
git clone --no-hardlinks . "$audit_root/project"
git -C "$audit_root/project" checkout --detach 334f89ca0553962a7ecd5c144be5508faa581f84
mkdir -p "$audit_root/project/docs/cv-metrics-evidence/2026-09-30"
unzip -q "$audit_bundle" -d "$audit_root/project/docs/cv-metrics-evidence/2026-09-30"
cp "$audit_root/evidence/raw/baseline-examples/valid-invoice-data.xlsx" "$audit_root/project/examples/valid-invoice-data.xlsx"
cp "$audit_root/evidence/raw/baseline-examples/valid-invoice-data.xml" "$audit_root/project/examples/valid-invoice-data.xml"
cd "$audit_root/project"
```

Bundan sonra açılmış `docs/cv-metrics-evidence/2026-09-30/REPRODUCE.md` içindeki komutları izleyin: temiz Maven doğrulaması, JaCoCo indirme/coverage, benchmark, envanter ve son uzlaştırma. Java 21 ve Python 3.9+ gerekir. Tarihsel koşu JDK 21.0.12, Maven 3.9.11, JaCoCo 0.8.12 kullandı. Offline Maven komutu için bağımlılıklar önceden cache'de bulunmalıdır.

Script'ler ölçüm dosyalarını günceller; ilk kanıtı `evidence.zip` ve ayrı açılan `evidence/` dizininde koruyun. Yeni koşunun JDK/OS ve zamanını ayrıca kaydedin. Yeniden build edilen JAR'ın timestamp ve hash'i değişebilir: yeni ölçümlerden sonra `inventory.py`, ardından `verify_evidence.py` çalıştırın. Son script tarihsel kaynak commit'ini, korunmuş örnekleri ve güncel envanterdeki JAR hash'lerini bekler; doğrudan güncel `main` üzerinde çalıştırılmak için tasarlanmamıştır.

## Ölçüm sınırları

Benchmark sıralı yerel dosya dönüşümüdür: 100, 1.000 ve 10.000 fatura, fatura başına 5 satır; her yön/boyut için üç JVM, JVM başına üç ısınma ve beş ölçüm. JVM `-Xms512m -Xmx2g -Djava.awt.headless=true`; coverage agent kapalıdır. Veri üretimi, JVM başlangıcı, servis kurulumu ve çıktı kontrolleri zamanlama dışındadır. Dosya okuma, validation, dönüşüm ve çıktı yazma dahildir. Cache sıcaktır; üretim kapasitesi veya hızlanma iddiası oluşturmaz.

Coverage, geçici proje kopyasında mevcut 103 testi çalıştırır; UI dahil bütün uygulama sınıfları raporlanır. Benchmark kontrolleri bu test sayısına eklenmez. Ham benchmark stdout'undaki Log4j bildirimi korunmuştur; CSV okuyucusu gerçek başlığı bulur. İlk özetleyici hatası ve düzeltmenin açıklaması raporda yer alır.
