# SOURCES — منبع و مجوز هر داده و فونت

تاریخ دانلود: **2026-11-05** (UTC)

---

## 1) متن قرآن — Tanzil (Uthmani, Version 1.1)

| مورد | مقدار |
|---|---|
| منبع | https://tanzil.net/download/ (فرم رسمی دانلود) |
| فایل | `data/tanzil/quran-uthmani.txt` |
| نسخه | Uthmani — Version 1.1 (منتشرشده February 2021 طبق صفحهٔ دانلود) |
| پارامترهای دانلود | `quranType=uthmani&outType=txt-2&marks=true&sajdah=true&tatweel=true&agree=true` (پیش‌فرض‌های فرم) |
| فرمت | `سوره\|آیه\|متن` + بلوک کپی‌رایت در انتهای فایل (دست‌نخورده) |
| md5 | `19fad945cd55e75f46ea27e9942bf1ed` |
| sha256 (۳۲ کاراکتر اول) | `6933e133dd56db778c801bf738848454` |
| اسکریپت بازتولید | `scripts/fetch_data.sh` |

### متن دقیق و کامل شرایط استفادهٔ Tanzil (کلمه‌به‌کلمه، بدون ترجمه)

منبع رسمی این متن: https://tanzil.net/docs/Text_License  (و همان متن در صفحهٔ https://tanzil.net/download/ تکرار شده است.)

> The following copyright notice is applied to, and is included in all copies of Tanzil Quran text:
>
> Tanzil Quran Text
> Copyright (C) 2007-2021 Tanzil Project
> License: Creative Commons Attribution 3.0
> This copy of the Quran text is carefully produced, highly verified and continuously monitored by a group of specialists in Tanzil Project.
>
> TERMS OF USE:
>
> - Permission is granted to copy and distribute verbatim copies of this text, but CHANGING IT IS NOT ALLOWED.
>
> - This Quran text can be used in any website or application, provided that its source (Tanzil Project) is clearly indicated, and a link is made to tanzil.net to enable users to keep track of changes.
>
> - This copyright notice shall be included in all verbatim copies of the text, and shall be reproduced appropriately in all files derived from or containing substantial portion of this text.
>
> Please check updates at: http://tanzil.net/updates/

### رعایت هر شرط در این پروژه

| شرط (خلاصهٔ انگلیسی همان شرط) | کجا رعایت شده |
|---|---|
| «copy and distribute verbatim copies … CHANGING IT IS NOT ALLOWED» | متن آیه‌ها **عیناً** از فایل دانلودشده در ستون `ayahs.text` ذخیره می‌شود (`scripts/build_db.py` هیچ تغییری در متن نمی‌دهد). اپ هیچ مسیر ویرایش متن ندارد. تنها در **لایهٔ نمایش** پیشوند بسم‌الله از آیهٔ ۱ جدا می‌شود (`QuranText.displayAyahText`) — متن ذخیره‌شده دست‌نخورده می‌ماند و اطلاعیهٔ Tanzil هم صریحاً «verbatim copies» را مجاز می‌داند. ستون اضافی `text_simple` فقط برای جستجو است و ستون متن اصلی را تغییر نمی‌دهد. |
| «its source (Tanzil Project) is clearly indicated, and a link is made to tanzil.net» | صفحهٔ «درباره و منابع» داخل اپ (`AboutScreen`): ذکر صریح «پروژهٔ Tanzil» + دکمهٔ پیوند `https://tanzil.net` + جملهٔ «برای پیگیری به‌روزرسانی متن: tanzil.net». همین اطلاعات در این فایل نیز آمده است. |
| «This copyright notice shall be included in all verbatim copies … reproduced appropriately in all files derived from or containing substantial portion of this text» | (الف) بلوک کپی‌رایت در انتهای `data/tanzil/quran-uthmani.txt` دست‌نخورده مانده است؛ (ب) همین اطلاعیه **کلمه‌به‌کلمه از خود فایل دانلودشده** استخراج و داخل خود دیتابیس اپ در جدول `meta` (کلید `tanzil_copyright_notice`) ذخیره شده تا هر کپی از فایل db آن را حمل کند؛ (ج) اطلاعیهٔ کامل در همین فایل (SOURCES.md) و در صفحهٔ دربارهٔ اپ ارجاع داده شده است. |

### نکات داده (بدون هیچ ادعای اضافه)

- **بسم‌الله:** در داده، بسم‌الله ضمیمهٔ آیهٔ اول است (سورهٔ ۱ خودش آیهٔ اولش بسم‌الله است؛ سورهٔ ۹ بسم‌الله ندارد). در داده **دو شکل دقیق** برای پیشوند بسم‌الله وجود دارد: فرم ۱ (۱۱۰ سوره) و فرم ۲ (سوره‌های ۹۵ و ۹۷ — **در داده با این شکل آمده است**، با تشدید روی حرف اول). هر دو رشته عیناً از داده استخراج و در `data/QuranText.kt` نگهداری می‌شوند؛ `scripts/build_db.py` در هر اجرا برابری بایت‌به‌بایت این ثابت‌ها با داده را چک می‌کند.
- **نام فارسی سوره‌ها:** در فایل متادیتای Tanzil نام فارسی وجود ندارد؛ `name_persian` در دیتابیس به‌صورت **قطعی و مستند** از نام عربی همان فایل تولید می‌شود (حذف «ال» ابتدایی به‌جز «آل»؛ نگاشت أ إ آ ٱ→ا، ة→ه، ي ى→ی، ك→ک) — منطق آن در `scripts/build_db.py` است.
- **اجزا (juz):** از فایل رسمی متادیتا گرفته می‌شود (۳۰ جزء).
- **حزب/حزب‌ربع:** فایل متادیتای رسمی Tanzil مرزبندی حزب/ربع را جداگانه نمی‌دهد؛ در این نسخه از اپ ذخیره نشده و به‌عنوان محدودیت مستند شده است (حدس زده نشده).
- **صفحات مصحف (page) و منازل (manzil) و سجده‌ها:** در فایل متادیتا موجودند؛ در این MVP فقط juz ذخیره شده و بقیه برای نسخه‌های بعد مانده‌اند (خواندنشان در اسکریپت ممکن است).
- نسخهٔ متادیتا: `quran-data.xml` (ver 1.0، از https://tanzil.net/docs/Quran_Metadata)؛ md5: `4a20103f26a968a78573ab0328f2f63b`

---

## 2) فونت نمایش — Amiri Quran 1.003

| مورد | مقدار |
|---|---|
| منبع رسمی | https://github.com/aliftype/amiri (Release 1.003 — فایل رسمی `Amiri-1.003.zip`) |
| فایل استفاده‌شده | `AmiriQuran.ttf` → `app/src/main/res/font/amiri_quran.ttf` |
| نسخهٔ دست‌نخوردهٔ آرشیو | `data/fonts/AmiriQuran-1.003.ttf` |
| مجوز | **SIL Open Font License, Version 1.1** |
| متن مجوز (اصلی، از خود Release) | `data/fonts/OFL-AmiriQuran-1.003.txt` |
| متن مجوز داخل اپ | `app/src/main/assets/OFL-AmiriQuran.txt` (همراه فونت داخل APK — طبق الزام OFL نسخهٔ مجوز همراه فونت توزیع شود) |
| sha256 فونت | `e2a47644762d16bdfb6d33e0d8db8c6ff30beae84150ef5a705316bbd829455c` |
| نگارندهٔ فونت | The Amiri Project Authors (© 2010–2022) |

نکته: فونت KFGQPC Uthmanic Hafs (مجمع ملک فهد) بررسی شد اما سایت رسمی آن (`fonts.qurancomplex.gov.sa`) از محیط ساخت این پروژه در دسترس نبود؛ بنابراین مطابق درخواست، فونت متن‌باز با مجوز قابل راستی‌آزمایی (Amiri Quran / SIL OFL 1.1) جایگزین شد. **کیفیت رندر نهایی روی دستگاه باید از صفحهٔ «آزمایش نمایش فونت» داخل اپ بررسی شود** (توسعه‌دهندهٔ این بررسی را انجام نداده — تست‌نشده).

---

## 3) سایر اجزا

- **آیکون‌های لانچر:** تولیدشده با `scripts/gen_icons.py` (PIL، هندسی و قطعی) — مالکیت با نگهدارندهٔ پروژه؛ منبع خارجی ندارند.
- **کد پروژه:** نوشته‌شده برای همین پروژه؛ منبع دادهٔ قرآنی ندارد.
- **کتابخانه‌ها:** فهرست و نسخه‌ها در `gradle/libs.versions.toml` (مجوزهای استاندارد AndroidX/Compose/Hilt/Room/DataStore — Apache 2.0 برای اجزای اصلی androidx و dagger).
