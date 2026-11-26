<div align="center">

<img src="public/assets/aistudio/file_00000000c1c08210b0958731db328baa_2741ed36.png" alt="Cosmic Crystal Miner Icon" width="140" />

# Cosmic Crystal Miner

**Космическая игра-кликер для Android (Kotlin + Jetpack Compose, Material 3) с интерактивной веб-версией**

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Language](https://img.shields.io/badge/Язык-Русский-22D3EE?style=flat-square)

</div>

---

## О проекте

**Cosmic Crystal Miner** — динамичный космический кликер в тёмном неоновом стиле. Добывайте кристаллы вручную, ловите критические удары `x3`, прокачивайте буры и флот автономных дронов, повышайте ранг добытчика и забирайте награды за достижения.

### Основные возможности

- **Тёмный космический дизайн (Material 3):** глубокий космический градиент, фиолетовые, голубые и золотые неоновые акценты.
- **Интерактивный кристалл на Canvas:** многогранный кристалл с орбитальным кольцом, плавным сжатием и пружинным отскоком при нажатии, виброоткликом (`HapticFeedback`) и всплывающими цифрами урона.
- **Система рангов:** 8 званий — от *Новичка* (`x1.0`) до *Космического барона* (`x3.2`) и *Повелителя сингулярности* (`x4.0`), каждое из которых усиливает весь доход.
- **Критические клики:** `10%` шанс выбить критический удар (`x3`), подсвеченный золотым цветом.
- **Магазин улучшений (8 позиций):**
  - *Усилители клика:* Лазерная кирка, Плазменный бур, Квантовый усилитель, Гравитационный молот.
  - *Автодобыча:* Мини-дрон, Орбитальная станция, Сингулярный реактор, Сфера Дайсона.
  - Экспоненциальный рост стоимости (`базовая_цена × 1.15^уровень`).
- **Достижения:** 5 испытаний с прогресс-барами и кнопкой **«Забрать награду»**.
- **Автосохранение:** весь прогресс сохраняется локально через `Jetpack DataStore` (Android) и `localStorage` (веб-версия).
- **Настройки:** переключатель вибрации и сброс прогресса с подтверждением.

---

## Структура проекта

```text
├── android-app/                      # Нативное Android-приложение (Kotlin + Compose)
│   ├── app/src/main/java/com/cosmicminer/app/
│   │   ├── MainActivity.kt
│   │   ├── data/GamePreferencesRepository.kt
│   │   ├── model/GameModels.kt
│   │   ├── util/NumberFormatter.kt
│   │   └── ui/
│   │       ├── CosmicMinerScreen.kt
│   │       ├── GameViewModel.kt
│   │       ├── components/
│   │       │   ├── CrystalCanvas.kt
│   │       │   ├── SettingsDialog.kt
│   │       │   └── UpgradeAndAchievementCards.kt
│   │       └── theme/Theme.kt
│   └── app/src/main/res/             # Иконки (mipmap-*), строки и темы
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
- При каждом обновлении кода в ветках `main` / `master` рабочий процесс `.github/workflows/build-apk.yml` автоматически собирает `app-debug.apk` и сохраняет его в **Actions → Artifacts**.
- При создании релиза с тегом вида `v1.0.0` готовый файл **`CosmicCrystalMiner.apk`** автоматически прикрепляется прямо к странице **Releases** для скачивания в один клик.

### Веб-версия (локально)
```bash
npm install
npm run dev
```
