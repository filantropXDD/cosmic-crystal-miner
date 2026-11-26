<div align="center">

<img src="public/assets/aistudio/file_00000000c1c08210b0958731db328baa_2741ed36.png" alt="Cosmic Crystal Miner Icon" width="140" />

# Cosmic Crystal Miner

**Космическая игра-кликер для Android (Kotlin + Jetpack Compose, Material 3) с интерактивной веб-версией**

![Version](https://img.shields.io/badge/Version-v1.1.1-8B5CF6?style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Language](https://img.shields.io/badge/Язык-Русский-22D3EE?style=flat-square)

</div>

---

## О проекте

**Cosmic Crystal Miner** — динамичный космический кликер в тёмном неоновом стиле. Добывайте кристаллы вручную, ловите критические удары `x3` и пролетающие кометы, прокачивайте буры и флот автономных дронов, запускайте «Коллапс» ради Тёмной материи и открывайте все 12 рангов и достижений.

### Основные возможности (v1.1.1)

- **Прямые обновления без потери прогресса:** единый постоянный ключ подписи (`debug.keystore`), встроенная проверка новых релизов GitHub (**«Проверить обновления»**) и перенос прогресса по коду (`CCM1:...`) в настройках.

- **Система престижа («Коллапс»):** сбросьте текущий прогресс захода ради редкой **Тёмной материи**, где каждая единица навсегда увеличивает весь доход на **+50%**.
- **Офлайн-добыча:** флот и реакторы продолжают добывать кристаллы, пока игра закрыта (до 12 часов), с отчётом при следующем входе.
- **Космические кометы:** каждые 35–60 секунд по небу пролетает светящаяся комета — тап по ней приносит кучу кристаллов и активирует **Перегрузку `x5`** на 10 секунд.
- **Эволюция кристалла:** 4 визуальные стадии по мере роста ранга — от сине-фиолетового ядра до золотых граней, двойных орбит и эффекта чёрной дыры на высших рангах.
- **Сочный Game Feel:** разлетающиеся искры при кликах, вспышка и мини-тряска при золотых критах `x3`, виброотклик и встроенный синтезатор звуковых эффектов (`AudioTrack` / Web Audio) с переключателем в настройках.
- **Глубокая прогрессия:** 12 рангов добытчика (до *Бога кристаллов*), 12 улучшений (6 усилителей клика и 6 автодобытчиков) и 12 достижений с наградами.
- **Автосохранение:** весь прогресс и время последнего визита сохраняются локально через `Jetpack DataStore` (Android) и `localStorage` (веб-версия).

---

## Структура проекта

```text
├── android-app/                      # Нативное Android-приложение (Kotlin + Compose)
│   ├── app/src/main/java/com/cosmicminer/app/
│   │   ├── MainActivity.kt
│   │   ├── data/GamePreferencesRepository.kt
│   │   ├── model/GameModels.kt
│   │   ├── util/
│   │   │   ├── NumberFormatter.kt
│   │   │   └── SoundEffects.kt
│   │   └── ui/
│   │       ├── CosmicMinerScreen.kt
│   │       ├── GameViewModel.kt
│   │       ├── components/
│   │       │   ├── CrystalCanvas.kt
│   │       │   ├── SettingsDialog.kt
│   │       │   └── UpgradeAndAchievementCards.kt
│   │       └── theme/Theme.kt
│   └── app/src/main/res/             # Иконки (mipmap-*), цвета, строки и темы
├── .github/workflows/build-apk.yml   # Автосборка APK и публикация в GitHub Releases
└── src/                              # Интерактивная веб-версия (React + TypeScript + Vite)
```

---

## Сборка и запуск

### Android-приложение (Android Studio)
1. Откройте папку `android-app/` в **Android Studio** (JDK 17, Android SDK 35).
2. Дождитесь завершения **Gradle Sync**.
3. Нажмите **Run (▶)** для запуска на эмуляторе или подключённом телефоне, либо **Build → Build APK(s)**.

### Облачная сборка и релизы (GitHub Actions)
- При обновлении ветки `main` рабочий процесс `.github/workflows/build-apk.yml` собирает `CosmicCrystalMiner.apk` в **Actions → Artifacts**.
- При публикации релиза с тегом `v1.1.0` готовый **`CosmicCrystalMiner.apk`** автоматически прикрепляется к странице **Releases**.

### Веб-версия (локально)
```bash
npm install
npm run dev
```
