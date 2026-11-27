import React from 'react';
import { BarChart3, X } from 'lucide-react';
import { formatCrystalCount, formatDuration } from '../types/game';

interface StatsModalProps {
  totalClicks: number;
  bestCps: number;
  currentCps: number;
  criticalHits: number;
  cometsCaught: number;
  bossesDefeated: number;
  feverActivations: number;
  playTimeSeconds: number;
  totalCrystalsEarned: number;
  darkMatter: number;
  prestigeCount: number;
  onClose: () => void;
}

export const StatsModal: React.FC<StatsModalProps> = ({
  totalClicks,
  bestCps,
  currentCps,
  criticalHits,
  cometsCaught,
  bossesDefeated,
  feverActivations,
  playTimeSeconds,
  totalCrystalsEarned,
  darkMatter,
  prestigeCount,
  onClose,
}) => {
  const rows = [
    { label: 'Всего кликов', value: totalClicks.toString(), accent: 'text-slate-100' },
    {
      label: 'Рекорд CPS (кликов/сек)',
      value: `${bestCps} (сейчас ${currentCps})`,
      accent: 'text-cyan-300',
    },
    { label: 'Выбито критов (x3)', value: criticalHits.toString(), accent: 'text-amber-300' },
    { label: 'Поймано комет', value: cometsCaught.toString(), accent: 'text-cyan-300' },
    { label: 'Побеждено боссов', value: bossesDefeated.toString(), accent: 'text-red-300' },
    { label: 'Активаций Ярости', value: feverActivations.toString(), accent: 'text-amber-300' },
    { label: 'Время в игре', value: formatDuration(playTimeSeconds), accent: 'text-slate-100' },
    {
      label: 'Добыто кристаллов',
      value: formatCrystalCount(totalCrystalsEarned),
      accent: 'text-cyan-300',
    },
    {
      label: 'Тёмная материя / Коллапсов',
      value: `${darkMatter} ТМ · ${prestigeCount}`,
      accent: 'text-violet-300',
    },
  ];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="stats-modal-title"
    >
      <div className="w-full max-w-md max-h-[90vh] overflow-y-auto rounded-3xl bg-[#111732] border border-cyan-400/25 p-6 shadow-2xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <BarChart3 className="w-5 h-5 text-cyan-300" />
            <h2 id="stats-modal-title" className="font-display text-lg font-bold text-slate-100">
              Статистика добытчика
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Закрыть статистику"
            className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 cursor-pointer transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="mt-4 space-y-2">
          {rows.map((row) => (
            <div
              key={row.label}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-xl bg-[#0B1024] border border-slate-800/80 text-xs"
            >
              <span className="text-slate-400">{row.label}</span>
              <span className={`font-mono-num font-bold ${row.accent}`}>{row.value}</span>
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={onClose}
          className="mt-5 w-full min-h-[42px] rounded-xl bg-cyan-400 hover:bg-cyan-300 text-slate-950 text-xs font-bold cursor-pointer transition-colors"
        >
          Закрыть
        </button>
      </div>
    </div>
  );
};
