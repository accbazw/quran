# BUILD NOTES — باز کردن، بیلد و عیب‌یابی

## ۰) بازبینی پیش از بیلد (بازبینی دوم، خط‌به‌خط)

مشکل‌هایی که در بازبینی پیدا و **اصلاح شد**:

1. **SearchScreen — روی‌هم‌افتادگی هدر و فیلد جستجو:** سه فرزند داخل `Surface` بدون `Column` بودند؛ `Surface` تک‌جایگاهی است و فرزندان stack می‌شدند. → داخل `Column` پیچیده شد.
2. **ReaderScreen — `return@Scaffold`:** ساختار به `if/else` بازسازی شد (کاملاً بدون اتکا به رفتار early-return لاندای کامپوزیبل).
3. **SettingsScreen — `BuildConfig.VERSION_NAME`:** به‌جای آن نسخه با `PackageManager` خوانده می‌شود (AGP 9 در آینده BuildConfig.VERSION_NAME را حذف می‌کند)؛ فلگ `buildConfig` هم از gradle حذف شد.
4. **BookmarksScreen — رشتهٔ هاردکد «آیه»:** به resource `ayah_label` منتقل شد (فارسی/عربی).
5. **FontTestScreen — سایه‌انداختن نام متغیر `items` روی تابع `items` لایزی:** به `testItems` تغییر نام داد.
6. **importهای بی‌استفاده** پاک شدند (`width`/`RoundedCornerShape` در دو فایل).

راستی‌آزمایی‌های بازبینی:

- **نسخه‌های پین‌شده، همگی در مخازن رسمی تأیید شدند:** AGP 8.11.1، Compose BOM 2025.06.01، Room 2.7.1، Navigation 2.9.0، Lifecycle 2.9.1، DataStore 1.1.6، core-ktx 1.16.0، activity-compose 1.10.1، hilt-navigation-compose 1.2.0 (مستقیم از maven-metadata گوگل)؛ Kotlin 2.1.21 و KSP 2.1.21-2.0.1 و Hilt 2.56.2 و coroutines 1.10.1 (تگ‌های رسمی GitHub پروژه‌ها). junit 4.13.2 شناخته‌شده است.
- **اسکیمای Room ↔ دیتابیس پیش‌ساخته:** `scripts/verify_room_schema.py` (جدید) ستون/نوع/NOT NULL/جایگاه PK/نام و ستون‌ها و uniqueness ایندکس‌ها را برای هر ۵ جدول با PRAGMA واقعی مقایسه کرد — **همه PASS**؛ `bookmarks` هم طبق انتظار در فایل پیش‌ساخته نیست (Room خودش می‌سازد). این چک را در پایپ‌لاین نگه دارید: بعد از هر تغییر Entities اجرایش کنید.
- توازن آکولاد/پرانتز همهٔ فایل‌های کاتلین، برابری کامل strings فارسی/عربی، و وجود همهٔ resourceهای ارجاع‌شده — PASS.


## ۱) سازگاری Android Studio

| مورد | مقدار |
|---|---|
| حداقل Android Studio | **Narwhal (2025.1.1) یا جدیدتر** — چون compileSdk 36 و AGP 8.11 دارد و Studioهای قدیمی‌تر AGP 8.11 را بلد نیستند |
| پیشنهادی | جدیدترین نسخهٔ پایدار از canary/stable channel رسمی |
| JDK | 17+ (Studio داخلیش دارد؛ برای بیلد ترمینالی JDK 17 یا 21 نصب باشد) |

> قاعدهٔ کلی سازگاری Studio: هر Studio حداقلِ یک نسخهٔ AGP را «کاملاً» پشتیبانی می‌کند و معمولاً یک نسخهٔ بالاتر را با هشدار. اگر Studio قدیمی است، از منوی متن پروژه گزینهٔ Upgrade Assistant را بزنید یا Studio را آپدیت کنید.

## ۲) باز کردن پروژه

1. `git`/کپی پوشهٔ `quran-android` را باز کنید: **File → Open → پوشهٔ ریشه** (جایی که `settings.gradle.kts` است).
2. صبر کنید Sync تمام شود (اولین بار Gradle 8.13 و وابستگی‌ها را دانلود می‌کند).
3. دکمهٔ Run (⚡) با یک دستگاه API 24+.

### نکتهٔ gradle-wrapper
در این مخزن `gradle/wrapper/gradle-wrapper.properties` هست ولی فایل باینری `gradle-wrapper.jar` و اسکریپت‌های `gradlew` نه (در سندباکس ساخت، Gradle نصب نبود). دو راه:
- **Android Studio:** خودش هنگام Sync wrapper را می‌سازد/پیشنهاد می‌دهد؛ معمولاً هیچ کاری لازم نیست.
- **ترمینال (یک‌بار):** اگر Gradle روی سیستم نصب است: `gradle wrapper --gradle-version 8.13` — سپس `./gradlew` در دسترس است.
- یا فایل‌های استاندارد `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` را از هر پروژهٔ Gradle 8.x موجود کپی کنید (محتوای jar مستقل از پروژه است).

## ۳) خطاهای محتمل و راه‌حل

| خطا | علت | راه‌حل |
|---|---|---|
| `Could not resolve com.android.application:8.11.1` / `Failed to resolve: androidx.compose:compose-bom:2025.06.01` | نسخه در مخازن ماشین/منطقهٔ شما هنوز نبود یا شبکه | از [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) و [AGP releases](https://developer.android.com/build/releases) آخرین نسخهٔ پایدار را بگیرید و در `gradle/libs.versions.toml` فقط همان یک مقدار را bump کنید. کد طوری نوشته شده که با bump جزئی نمی‌شکند. |
| `Unsupported class file major version` / خطای JDK | JDK نامناسب | JDK 17 یا 21: `File → Settings → Build Tools → Gradle → Gradle JDK` |
| `compileSdk 36 … not tested with AGP x` | AGP قدیمی‌تر از 8.11 | یا AGP را به 8.11.1 ببرید، یا موقتاً در `gradle.properties` بیفزایید: `android.suppressUnsupportedCompileSdk=36` |
| خطای KSP/Room/Hilt هنگام کامپایل | ناهماهنگی KSP با Kotlin | KSP باید دقیقاً با پیشوند Kotlin برابر باشد: Kotlin `2.1.21` ⇔ KSP `2.1.21-x` |
| `Schema export directory was not provided` | فقط اگر `ksp { arg(...) }` از `app/build.gradle.kts` حذف شود | همان بلاک را برگردانید (اسکیما در `app/schemas` ذخیره می‌شود) |
| خطای Room هنگام اولین اجرا: «Migration didn't properly handle …» یا validation failed | ناسازگاری اسکیمای db پیش‌ساخته با Entities (اگر Entities را تغییر داده‌اید ولی db را بازنساخته‌اید) | `python3 scripts/build_db.py` را دوباره اجرا کنید؛ table/column ها را هم‌نام با `Entities.kt` نگه دارید |
| اپ اجرا می‌شود ولی متن‌ها ناخوانا/به‌هم‌ریخته | فونت Amiri Quran با متن عثمانی | صفحهٔ «تنظیمات → صفحهٔ آزمایش نمایش فونت» را ببینید؛ در صورت نیاز فونت دیگری (مثلاً KFGQPC HAFS از منبع رسمی، با بررسی مجوزش) در `res/font` بگذارید و `QuranFontFamily` را عوض کنید |
| `unresolved reference BuildConfig` | اگر `buildFeatures.buildConfig` را بردارید | همان فلگ را در `app/build.gradle.kts` نگه دارید |

## ۴) بیلد Release و AAB

```bash
# امضا: keystore را NEVER commit کنید؛ از متغیرها استفاده کنید:
QURAN_STORE_FILE=/path/keystore.jks \
QURAN_STORE_PASSWORD=*** \
QURAN_KEY_ALIAS=upload \
QURAN_KEY_PASSWORD=*** \
./gradlew bundleRelease
# خروجی: app/build/outputs/bundle/release/app-release.aab
```

- R8/minify + shrinkResources از قبل روشن است (`proguard-rules.pro` حداقلی است چون Hilt/Room خودشان قواعد consumer دارند).
- قبل از آپلود: `applicationId` را عوض کنید، `versionCode/Name` را ست کنید، و «صفحهٔ درباره» را یک‌بار چشم‌آزمایی کنید (الزام منبع‌ذکری مجوز Tanzil).

## ۵) وضعیت تست‌ها — چه چیز واقعاً تست شده؟

### ✅ اجراشده و PASS در سندباکس ساخت (بدون Android SDK)
- دانلود داده از Tanzil + ثبت هش‌ها
- `scripts/validate_quran.py`: ۱۱۴ سوره، ۶۲۳۶ آیه، شمارش هر سوره = متادیتا، شماره‌گذاری پیوسته، بدون آیهٔ خالی/تکراری، انسجام متادیتا (start تجمعی، order یکتا، ۳۰ جزء)، قرارداد بسم‌الله (۲ فرم دقیق)
- `scripts/build_db.py`: ساخت + خودآزمایی db (همهٔ چک‌ها PASS؛ شامل برابری بایت‌به‌بایت ثابت‌های بسم‌الله کاتلین با داده — این چک واقعاً یک باگ ترتیب اعراب را در تایپ دستی پیدا و اصلاح کرد)
- صحت فایل فونت (magic sfnt) + sha256 + نصب مجوز OFL کنار فونت
- تولید آیکون‌ها

### ⛔ تست‌نشده (اینها را شما باید روی ماشین/دستگاه چک کنید)
- کامپایل کاتلین و رزولوشن وابستگی‌ها (سندباکس Android SDK ندارد)
- اجرای `./gradlew test` (تست‌های واحد نوشته شده‌اند ولی اجرا نشده‌اند)
- عملکرد زمان اجرا: Room/createFromAsset، جستجو، نشانه‌گذاری، ذخیرهٔ آخرین جای خواندن
- رندر فونت و اعراب (صفحهٔ آزمایشی داخل اپ برای همین کار است)
- RTL در حالت واقعی، حالت شب، فلیپ زبان fa/ar
- بیلد release/AAB و minify

## ۶) چیزهایی که از کار کردنشان مطمئن نیستم (فهرست صادقانه)

1. رندر کامل علائم وقف و علامت‌های قرآنی U+06D6..06ED با فونت Amiri Quran روی Android — باید از صفحهٔ آزمایش فونت دیده شود.
2. شکستن خطوط آیه‌های بلند با lineHeight 1.9× و فونت عثمانی — ظرافتی است که فقط چشم، تصمیم می‌گیرد.
3. رفتار `createFromAsset` با version بعدی Room 2.7.1 اگر وابستگی‌ها را bump کردید (مهاجرت schema جدید لازم خواهد شد).
4. ذخیرهٔ دقیق «آخرین جای خواندن» در اسکرول سریع (debounce 800ms) — ممکن است در خروج سریع یک-دو آیه جابه‌جا ذخیره شود.
5. هایلایت جستجو در آیه‌هایی که چند بار تطبیق دارند و تطبیق‌ها هم‌مرزند (caseهای خیلی خاص).
6. کلیدهای فونت `res/font` با AAPT2 روی همهٔ نسخه‌های AGP 8.x (استاندارد است، ولی ما build نگرفته‌ایم).
