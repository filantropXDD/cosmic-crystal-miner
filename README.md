<div align="center">

<img src="public/assets/aistudio/file_00000000c1c08210b0958731db328baa_2741ed36.png" alt="Cosmic Crystal Miner Icon" width="140" />

# Cosmic Crystal Miner

**Космическая игра-кликер для Android (Kotlin + Jetpack Compose, Material 3) с интерактивной веб-версией**

![Version](https://img.shields.io/badge/Version-v1.2.0-8B5CF6?style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Language](https://img.shields.io/badge/Язык-Русский-22D3EE?style=flat-square)

</div>

---

## О проекте

**Cosmic Crystal Miner** — динамичный космический кликер в тёмном неоновом стиле. Добывайте кристаллы вручную, заряжайте шкалу **Космической ярости**, сражайтесь с боссами-астероидами, отправляйте флот в экспедиции, открывайте уникальные скины и космические сектора за достижения и прокачивайте дерево талантов за Тёмную материю.

### Основные возможности (v1.2.0)

- **Скины объектов и космические локации:** 6 визуальных объектов (*Космический кристалл*, *Чёрная дыра*, *Солнечное ядро*, *Кибер-куб*, *Ледяная реликвия*, *Радужная призма*) со своими частицами и физикой, плюс 4 локации космоса — открываются исключительно за достижения.
- **Живой мир и параллакс:** вокруг кристалла кружат купленные дроны и периодически стреляют лазерами, а фоновые звёзды и туманность реагируют на наклон устройства (`SensorManager` на Android / движение курсора и гироскоп в браузере).
- **Кометы без случайных нажатий:** обновлённая траектория в отдельной верхней полосе арены вдали от зоны тапа по кристаллу (Перегрузка `x5`).
- **Космическая ярость (Fever Mode):** серия быстрых тапов заполняет круговой индикатор нагрева до 100%, после чего каждый клик становится золотым критом `x3`.
- **Босс-астероид и экспедиции флота:** 30-секундные сражения с Левиафанами ради сундуков с кристаллами и шансом Тёмной материи, 3 типа автономных экспедиций (5/15/30 мин) и 3 ежедневных мини-квеста.
- **Дерево талантов (Тёмная материя):** 6 перков в ветках *Активный кликер* и *Магнат* с сохранением постоянного бонуса +50% к доходу даже от потраченной ТМ.
- **Прогрессия до 200 уровня и статистика:** 200 уровней ранга, 12 улучшений до 200 уровня, 17 достижений, подробная статистика добытчика с рекордом CPS, полуночный бонус (`00:00–04:00`) и секретная пасхалка.
- **Прямые обновления без потери прогресса:** постоянный ключ подписи (`signing-debug.txt`), встроенная проверка новых релизов GitHub и перенос прогресса по коду (`CCM1:...`).

---

## Структура проекта

```text
├── android-app/                      # Нативное Android-приложение (Kotlin + Compose)
│   ├── app/src/main/java/com/cosmicminer/app/
│   │   ├── MainActivity.kt
│   │   ├── data/GamePreferencesRepository.kt
│   │   ├── model/GameModels.kt
│   │   ├── util/
│   │   │   ├── BackupCodec.kt
│   │   │   ├── NumberFormatter.kt
│   │   │   ├── SoundEffects.kt
│   │   │   └── UpdateChecker.kt
│   │   └── ui/
│   │       ├── CosmicMinerScreen.kt
│   │       ├── GameViewModel.kt
│   │       ├── components/
│   │       │   ├── CrystalCanvas.kt
│   │       │   ├── FleetAndSkinsPanels.kt
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
- При публикации релиза с тегом версии готовый **`CosmicCrystalMiner.apk`** автоматически прикрепляется к странице **Releases**.

### Веб-версия (локально)
```bash
npm install
npm run dev
```
