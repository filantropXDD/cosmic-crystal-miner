import React, { useState } from 'react';
import { Check, Copy, Download, Smartphone, Trash2, Volume2, X } from 'lucide-react';
import { formatCrystalCount } from '../types/game';

interface SettingsModalProps {
  isVibrationEnabled: boolean;
  isSoundEnabled: boolean;
  totalClicks: number;
  criticalHits: number;
  cometsCaught: number;
  darkMatter: number;
  totalCrystalsEarned: number;
  onToggleVibration: (enabled: boolean) => void;
  onToggleSound: (enabled: boolean) => void;
  onExportBackup: () => string;
  onImportBackup: (code: string) => boolean;
  onResetProgress: () => void;
  onClose: () => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isVibrationEnabled,
  isSoundEnabled,
  totalClicks,
  criticalHits,
  cometsCaught,
  darkMatter,
  totalCrystalsEarned,
  onToggleVibration,
  onToggleSound,
  onExportBackup,
  onImportBackup,
  onResetProgress,
  onClose,
}) => {
  const [confirmReset, setConfirmReset] = useState(false);
  const [showBackup, setShowBackup] = useState(false);
  const [backupInput, setBackupInput] = useState('');
  const [backupFeedback, setBackupFeedback] = useState<string | null>(null);
  const [copiedBackup, setCopiedBackup] = useState(false);

  const handleCopyBackup = async () => {
    const code = onExportBackup();
    try {
      await navigator.clipboard.writeText(code);
      setCopiedBackup(true);
      setBackupFeedback('Код сохранения скопирован в буфер');
      window.setTimeout(() => setCopiedBackup(false), 1800);
    } catch {
      setBackupInput(code);
      setBackupFeedback('Скопируйте код из поля ниже');
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="settings-title"
    >
      <div className="w-full max-w-md max-h-[90vh] overflow-y-auto rounded-3xl bg-[#111732] border border-violet-400/25 p-6 shadow-2xl">
        <div className="flex items-center justify-between">
          <h2 id="settings-title" className="font-display text-xl font-bold text-slate-100">
            Настройки · v1.1.1
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Закрыть настройки"
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 cursor-pointer transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="mt-5 space-y-3.5">
          <div className="flex items-center justify-between gap-4 p-3.5 rounded-2xl bg-[#182044]/70">
            <div className="flex items-center gap-3">
              <Smartphone className="w-5 h-5 text-violet-300 shrink-0" />
              <div>
                <div className="text-sm font-semibold text-slate-100">
                  Вибрация при клике
                </div>
                <div className="text-xs text-slate-400">
                  Тактильный отклик и тряска при крите
                </div>
              </div>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={isVibrationEnabled}
              onClick={() => onToggleVibration(!isVibrationEnabled)}
              className={`w-12 h-7 rounded-full p-1 transition-colors cursor-pointer shrink-0 ${
                isVibrationEnabled ? 'bg-violet-500' : 'bg-slate-700'
              }`}
            >
              <span
                className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                  isVibrationEnabled ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          <div className="flex items-center justify-between gap-4 p-3.5 rounded-2xl bg-[#182044]/70">
            <div className="flex items-center gap-3">
              <Volume2 className="w-5 h-5 text-cyan-300 shrink-0" />
              <div>
                <div className="text-sm font-semibold text-slate-100">
                  Звуковые эффекты
                </div>
                <div className="text-xs text-slate-400">
                  Клик, звонкий крит и покупка
                </div>
              </div>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={isSoundEnabled}
              onClick={() => onToggleSound(!isSoundEnabled)}
              className={`w-12 h-7 rounded-full p-1 transition-colors cursor-pointer shrink-0 ${
                isSoundEnabled ? 'bg-cyan-500' : 'bg-slate-700'
              }`}
            >
              <span
                className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                  isSoundEnabled ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          <div className="p-3.5 rounded-2xl bg-[#0B1024] border border-slate-800/80 text-xs text-slate-400 space-y-1.5">
            <div className="flex justify-between">
              <span>Всего кликов</span>
              <span className="font-mono-num text-slate-200">{totalClicks}</span>
            </div>
            <div className="flex justify-between">
              <span>Критических ударов</span>
              <span className="font-mono-num text-amber-300">{criticalHits}</span>
            </div>
            <div className="flex justify-between">
              <span>Комет поймано</span>
              <span className="font-mono-num text-cyan-300">{cometsCaught}</span>
            </div>
            <div className="flex justify-between">
              <span>Тёмная материя</span>
              <span className="font-mono-num text-violet-300">{darkMatter} ТМ</span>
            </div>
            <div className="flex justify-between">
              <span>Добыто за всё время</span>
              <span className="font-mono-num text-cyan-300">
                {formatCrystalCount(totalCrystalsEarned)}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setShowBackup((v) => !v);
              setBackupFeedback(null);
            }}
            className="w-full min-h-[44px] flex items-center justify-between px-4 py-2.5 rounded-xl bg-[#182044]/75 hover:bg-[#1E2956] border border-slate-700/60 text-sm font-semibold text-violet-300 cursor-pointer transition-colors"
          >
            <span className="flex items-center gap-2.5">
              <Copy className="w-4 h-4 shrink-0" />
              <span>Перенос прогресса (код)</span>
            </span>
            <span className="text-xs text-slate-400">{showBackup ? 'Скрыть' : 'Открыть'}</span>
          </button>

          {showBackup && (
            <div className="p-3.5 rounded-2xl bg-[#0B1024] border border-violet-400/30 space-y-2.5">
              <button
                type="button"
                onClick={handleCopyBackup}
                className="w-full min-h-[40px] flex items-center justify-center gap-2 rounded-xl bg-cyan-400 hover:bg-cyan-300 text-slate-950 text-xs font-bold cursor-pointer transition-colors"
              >
                {copiedBackup ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                <span>Скопировать код сохранения</span>
              </button>
              <input
                type="text"
                value={backupInput}
                onChange={(e) => {
                  setBackupInput(e.target.value);
                  setBackupFeedback(null);
                }}
                placeholder="Вставьте код CCM1:... для восстановления"
                className="w-full px-3 py-2 rounded-xl bg-[#121833] border border-slate-700 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-violet-400"
              />
              <button
                type="button"
                disabled={!backupInput.trim()}
                onClick={() => {
                  const ok = onImportBackup(backupInput);
                  if (ok) {
                    setBackupInput('');
                    setBackupFeedback('Прогресс восстановлен!');
                  } else {
                    setBackupFeedback('Неверный код сохранения');
                  }
                }}
                className="w-full min-h-[40px] flex items-center justify-center gap-2 rounded-xl bg-violet-500 hover:bg-violet-400 disabled:opacity-50 text-white text-xs font-bold cursor-pointer transition-colors"
              >
                <Download className="w-4 h-4" />
                <span>Восстановить прогресс</span>
              </button>
              {backupFeedback && (
                <p className="text-xs text-cyan-300 font-medium">{backupFeedback}</p>
              )}
            </div>
          )}

          <a
            href="https://github.com/filantropXDD/cosmic-crystal-miner/releases/latest"
            target="_blank"
            rel="noopener noreferrer"
            className="w-full min-h-[44px] flex items-center justify-between px-4 py-2.5 rounded-xl bg-[#182044]/75 hover:bg-[#1E2956] border border-slate-700/60 text-sm font-semibold text-amber-300 transition-colors"
          >
            <span className="flex items-center gap-2.5">
              <Download className="w-4 h-4 shrink-0" />
              <span>Скачать Android APK</span>
            </span>
            <span className="text-xs text-slate-400">Releases ↗</span>
          </a>

          <a
            href="https://github.com/filantropXDD/cosmic-crystal-miner"
            target="_blank"
            rel="noopener noreferrer"
            className="w-full min-h-[44px] flex items-center justify-between px-4 py-2.5 rounded-xl bg-[#182044]/75 hover:bg-[#1E2956] border border-slate-700/60 text-sm font-semibold text-cyan-300 transition-colors"
          >
            <span className="flex items-center gap-2.5">
              <svg
                viewBox="0 0 24 24"
                fill="currentColor"
                className="w-5 h-5 text-slate-100 shrink-0"
                aria-hidden="true"
              >
                <path d="M12 1.27a11 11 0 0 0-3.48 21.46c.55.09.73-.24.73-.53v-1.85c-3.03.66-3.67-1.45-3.67-1.45-.5-1.26-1.21-1.6-1.21-1.6-.99-.68.07-.67.07-.67 1.09.08 1.67 1.12 1.67 1.12.97 1.66 2.54 1.18 3.16.9.1-.7.38-1.18.69-1.45-2.42-.27-4.96-1.21-4.96-5.4 0-1.19.43-2.17 1.12-2.93-.11-.28-.49-1.39.11-2.89 0 0 .92-.29 3 1.12a10.4 10.4 0 0 1 5.5 0c2.08-1.41 3-1.12 3-1.12.6 1.5.22 2.61.11 2.89.7.76 1.12 1.74 1.12 2.93 0 4.2-2.55 5.13-4.98 5.4.39.34.74 1.01.74 2.03v3.01c0 .29.19.63.74.53A11 11 0 0 0 12 1.27z" />
              </svg>
              <span>Исходный код</span>
            </span>
            <span className="text-xs text-slate-400">GitHub ↗</span>
          </a>

          {!confirmReset ? (
            <button
              type="button"
              onClick={() => setConfirmReset(true)}
              className="w-full min-h-[44px] flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-red-950/60 hover:bg-red-900/70 text-red-300 border border-red-500/30 text-sm font-semibold cursor-pointer transition-colors"
            >
              <Trash2 className="w-4 h-4" />
              Сбросить прогресс
            </button>
          ) : (
            <div className="p-4 rounded-2xl bg-red-950/40 border border-red-500/40 space-y-3">
              <p className="text-xs text-red-200 leading-relaxed">
                Вы уверены? Все кристаллы, Тёмная материя, ранг, улучшения и достижения будут удалены без возможности восстановления.
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setConfirmReset(false)}
                  className="flex-1 min-h-[44px] px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold cursor-pointer transition-colors"
                >
                  Отмена
                </button>
                <button
                  type="button"
                  onClick={() => {
                    onResetProgress();
                    setConfirmReset(false);
                    onClose();
                  }}
                  className="flex-1 min-h-[44px] px-3 py-2 rounded-xl bg-red-500 hover:bg-red-400 text-white text-xs font-bold cursor-pointer transition-colors"
                >
                  Подтвердить сброс
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
