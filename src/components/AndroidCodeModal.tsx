import React, { useState } from 'react';
import { Check, Copy, FileCode2, X } from 'lucide-react';

import mainActivityKt from '../../android-app/app/src/main/java/com/cosmicminer/app/MainActivity.kt?raw';
import cosmicMinerScreenKt from '../../android-app/app/src/main/java/com/cosmicminer/app/ui/CosmicMinerScreen.kt?raw';
import gameViewModelKt from '../../android-app/app/src/main/java/com/cosmicminer/app/ui/GameViewModel.kt?raw';
import crystalCanvasKt from '../../android-app/app/src/main/java/com/cosmicminer/app/ui/components/CrystalCanvas.kt?raw';
import cardsKt from '../../android-app/app/src/main/java/com/cosmicminer/app/ui/components/UpgradeAndAchievementCards.kt?raw';
import settingsDialogKt from '../../android-app/app/src/main/java/com/cosmicminer/app/ui/components/SettingsDialog.kt?raw';
import gameModelsKt from '../../android-app/app/src/main/java/com/cosmicminer/app/model/GameModels.kt?raw';
import repoKt from '../../android-app/app/src/main/java/com/cosmicminer/app/data/GamePreferencesRepository.kt?raw';
import formatterKt from '../../android-app/app/src/main/java/com/cosmicminer/app/util/NumberFormatter.kt?raw';
import soundKt from '../../android-app/app/src/main/java/com/cosmicminer/app/util/SoundEffects.kt?raw';
import updateCheckerKt from '../../android-app/app/src/main/java/com/cosmicminer/app/util/UpdateChecker.kt?raw';
import backupCodecKt from '../../android-app/app/src/main/java/com/cosmicminer/app/util/BackupCodec.kt?raw';
import themeKt from '../../android-app/app/src/main/java/com/cosmicminer/app/ui/theme/Theme.kt?raw';
import appGradle from '../../android-app/app/build.gradle.kts?raw';
import manifestXml from '../../android-app/app/src/main/AndroidManifest.xml?raw';

interface AndroidCodeModalProps {
  onClose: () => void;
}

const ANDROID_FILES = [
  { name: 'CosmicMinerScreen.kt', path: 'ui/CosmicMinerScreen.kt', content: cosmicMinerScreenKt },
  { name: 'GameViewModel.kt', path: 'ui/GameViewModel.kt', content: gameViewModelKt },
  { name: 'CrystalCanvas.kt', path: 'ui/components/CrystalCanvas.kt', content: crystalCanvasKt },
  { name: 'UpgradeAndAchievementCards.kt', path: 'ui/components/UpgradeAndAchievementCards.kt', content: cardsKt },
  { name: 'SettingsDialog.kt', path: 'ui/components/SettingsDialog.kt', content: settingsDialogKt },
  { name: 'GameModels.kt', path: 'model/GameModels.kt', content: gameModelsKt },
  { name: 'GamePreferencesRepository.kt', path: 'data/GamePreferencesRepository.kt', content: repoKt },
  { name: 'UpdateChecker.kt', path: 'util/UpdateChecker.kt', content: updateCheckerKt },
  { name: 'BackupCodec.kt', path: 'util/BackupCodec.kt', content: backupCodecKt },
  { name: 'NumberFormatter.kt', path: 'util/NumberFormatter.kt', content: formatterKt },
  { name: 'SoundEffects.kt', path: 'util/SoundEffects.kt', content: soundKt },
  { name: 'Theme.kt', path: 'ui/theme/Theme.kt', content: themeKt },
  { name: 'MainActivity.kt', path: 'MainActivity.kt', content: mainActivityKt },
  { name: 'build.gradle.kts', path: 'app/build.gradle.kts', content: appGradle },
  { name: 'AndroidManifest.xml', path: 'app/src/main/AndroidManifest.xml', content: manifestXml },
];

export const AndroidCodeModal: React.FC<AndroidCodeModalProps> = ({ onClose }) => {
  const [activeIndex, setActiveIndex] = useState(0);
  const [copied, setCopied] = useState(false);

  const activeFile = ANDROID_FILES[activeIndex];

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(activeFile.content);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      // Fallback ignored
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="android-code-title"
    >
      <div className="w-full max-w-5xl h-[85vh] flex flex-col rounded-3xl bg-[#0D1228] border border-violet-400/25 overflow-hidden shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5 min-w-0">
            <FileCode2 className="w-5 h-5 text-cyan-300 shrink-0" />
            <div className="min-w-0">
              <h2 id="android-code-title" className="font-display text-base font-bold text-slate-100 truncate">
                Исходный код Android (Kotlin + Jetpack Compose) · v1.1.1
              </h2>
              <p className="text-xs text-slate-400 truncate">
                Папка проекта: android-app/ · Material 3 · DataStore · AudioTrack · ViewModel
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Закрыть просмотр кода"
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 cursor-pointer transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 min-h-0 flex flex-col md:flex-row">
          <div className="md:w-64 border-b md:border-b-0 md:border-r border-slate-800 overflow-x-auto md:overflow-y-auto flex md:flex-col gap-1 p-2.5 shrink-0">
            {ANDROID_FILES.map((file, idx) => (
              <button
                key={file.name}
                type="button"
                onClick={() => {
                  setActiveIndex(idx);
                  setCopied(false);
                }}
                className={`px-3 py-2 rounded-xl text-left text-xs font-medium whitespace-nowrap shrink-0 cursor-pointer transition-colors ${
                  idx === activeIndex
                    ? 'bg-violet-500/25 text-cyan-300 border border-cyan-400/30'
                    : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/50'
                }`}
              >
                {file.name}
              </button>
            ))}
          </div>

          <div className="flex-1 min-w-0 flex flex-col bg-[#070A18]">
            <div className="flex items-center justify-between px-4 py-2.5 border-b border-slate-800/80">
              <span className="text-xs font-mono-num text-slate-400 truncate">
                {activeFile.path}
              </span>
              <button
                type="button"
                onClick={handleCopy}
                className="min-h-[36px] flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium cursor-pointer transition-colors shrink-0"
              >
                {copied ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                    <span>Скопировано</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    <span>Копировать</span>
                  </>
                )}
              </button>
            </div>
            <pre className="flex-1 overflow-auto p-4 text-xs font-mono-num text-slate-200 leading-relaxed">
              <code>{activeFile.content}</code>
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};
