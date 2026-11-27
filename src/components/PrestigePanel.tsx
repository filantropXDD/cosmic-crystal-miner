import React, { useState } from 'react';
import {
  PRESTIGE_BASE_THRESHOLD,
  TALENTS,
  TalentBranch,
  formatCrystalCount,
  formatRate,
  getTalentCost,
} from '../types/game';

interface PrestigePanelProps {
  darkMatter: number;
  prestigeCount: number;
  prestigeMultiplier: number;
  potentialDarkMatter: number;
  runCrystalsEarned: number;
  nextDarkMatterThreshold: number;
  talentLevels: Record<string, number>;
  onBuyTalent: (talentId: string) => void;
  onPerformCollapse: () => void;
}

const BRANCH_META: { id: TalentBranch; title: string; colorClass: string; btnClass: string }[] = [
  {
    id: 'ACTIVE_CLICKER',
    title: 'Активный кликер',
    colorClass: 'text-amber-300',
    btnClass: 'bg-amber-400 hover:bg-amber-300 text-slate-950',
  },
  {
    id: 'TYCOON',
    title: 'Магнат',
    colorClass: 'text-violet-300',
    btnClass: 'bg-violet-500 hover:bg-violet-400 text-white',
  },
];

export const PrestigePanel: React.FC<PrestigePanelProps> = ({
  darkMatter,
  prestigeCount,
  prestigeMultiplier,
  potentialDarkMatter,
  runCrystalsEarned,
  nextDarkMatterThreshold,
  talentLevels,
  onBuyTalent,
  onPerformCollapse,
}) => {
  const [confirmOpen, setConfirmOpen] = useState(false);
  const progress = Math.min(1, runCrystalsEarned / Math.max(1, nextDarkMatterThreshold));

  return (
    <div className="space-y-4">
      <div className="p-4 rounded-2xl bg-[#121833]/90 border border-violet-400/35 space-y-3">
        <div>
          <div className="text-xs font-semibold text-violet-300">
            Система престижа · Коллапс
          </div>
          <p className="text-xs text-slate-400 mt-1 leading-relaxed">
            Сбросьте кристаллы, ранг и улучшения текущего захода, чтобы получить{' '}
            <span className="text-violet-200 font-semibold">Тёмную материю</span>.
            Каждая единица навсегда даёт <span className="text-amber-300 font-semibold">+50%</span> ко всему доходу.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-3 pt-1">
          <div className="p-3 rounded-xl bg-[#0B0F23] border border-slate-800/80">
            <div className="text-[11px] text-slate-400">Тёмная материя</div>
            <div className="font-mono-num text-xl font-extrabold text-violet-300 mt-0.5">
              {darkMatter} ТМ
            </div>
          </div>
          <div className="p-3 rounded-xl bg-[#0B0F23] border border-slate-800/80">
            <div className="text-[11px] text-slate-400">Постоянный бонус</div>
            <div className="font-mono-num text-xl font-extrabold text-amber-300 mt-0.5">
              x{formatRate(prestigeMultiplier)}
            </div>
          </div>
        </div>

        <div className="text-xs text-slate-400">
          Совершено перезагрузок: <span className="font-mono-num text-slate-200">{prestigeCount}</span>
        </div>
      </div>

      <div
        className={`p-4 rounded-2xl bg-[#121833]/90 border space-y-3 ${
          potentialDarkMatter > 0 ? 'border-amber-400/50' : 'border-slate-800/80'
        }`}
      >
        <div className="text-sm font-bold text-slate-100">
          {potentialDarkMatter > 0 ? (
            <span className="text-amber-300">
              Доступно к получению: +{potentialDarkMatter} ТМ
            </span>
          ) : (
            <span>
              Накопите {formatCrystalCount(PRESTIGE_BASE_THRESHOLD)} кристаллов за заход
            </span>
          )}
        </div>

        <div className="space-y-1.5">
          <div className="h-2 rounded-full bg-slate-800 overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-violet-500 to-amber-400 transition-all duration-200"
              style={{ width: `${Math.round(progress * 100)}%` }}
            />
          </div>
          <div className="flex justify-between text-[11px] font-mono-num text-slate-400">
            <span>Следующая ТМ</span>
            <span>
              {formatCrystalCount(runCrystalsEarned)} / {formatCrystalCount(nextDarkMatterThreshold)}
            </span>
          </div>
        </div>

        {!confirmOpen ? (
          <button
            type="button"
            disabled={potentialDarkMatter <= 0}
            onClick={() => setConfirmOpen(true)}
            className={`w-full min-h-[44px] px-4 py-2.5 rounded-xl text-sm font-bold transition-all cursor-pointer ${
              potentialDarkMatter > 0
                ? 'bg-violet-500 hover:bg-violet-400 active:scale-[0.98] text-white shadow-sm shadow-violet-500/25'
                : 'bg-slate-800/70 text-slate-500 opacity-60 cursor-not-allowed'
            }`}
          >
            {potentialDarkMatter > 0
              ? `Совершить Коллапс (+${potentialDarkMatter} ТМ)`
              : 'Недостаточно кристаллов'}
          </button>
        ) : (
          <div className="p-3.5 rounded-xl bg-violet-950/40 border border-violet-400/40 space-y-3">
            <p className="text-xs text-slate-200 leading-relaxed">
              Сбросить текущие кристаллы, ранг и улучшения ради +{potentialDarkMatter} ТМ? Достижения сохранятся.
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setConfirmOpen(false)}
                className="flex-1 min-h-[40px] rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 cursor-pointer"
              >
                Отмена
              </button>
              <button
                type="button"
                onClick={() => {
                  setConfirmOpen(false);
                  onPerformCollapse();
                }}
                className="flex-1 min-h-[40px] rounded-xl bg-violet-500 hover:bg-violet-400 text-xs font-bold text-white cursor-pointer"
              >
                Запустить!
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Talent Tree */}
      <div className="p-4 rounded-2xl bg-[#121833]/90 border border-cyan-400/30 space-y-3">
        <div>
          <h3 className="text-sm font-bold text-cyan-300">
            Дерево талантов (за Тёмную материю)
          </h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Потраченная ТМ сохраняет постоянный бонус к доходу +50%.
          </p>
        </div>

        {BRANCH_META.map((branch) => {
          const branchTalents = TALENTS.filter((t) => t.branch === branch.id);
          return (
            <div key={branch.id} className="space-y-2 pt-1">
              <div className={`text-xs font-bold ${branch.colorClass}`}>
                Ветка: {branch.title}
              </div>
              {branchTalents.map((talent) => {
                const level = talentLevels[talent.id] ?? 0;
                const isMaxed = level >= talent.maxLevel;
                const cost = getTalentCost(talent, level);
                const canAfford = !isMaxed && darkMatter >= cost;

                return (
                  <div
                    key={talent.id}
                    className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-[#0B0F23] border border-slate-800/80"
                  >
                    <div className="min-w-0">
                      <div className="text-xs font-semibold text-slate-100">
                        {talent.title} ({level}/{talent.maxLevel})
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        {talent.description}
                      </div>
                    </div>

                    <button
                      type="button"
                      disabled={!canAfford}
                      onClick={() => onBuyTalent(talent.id)}
                      className={`min-h-[36px] min-w-[74px] px-3 rounded-lg font-mono-num text-xs font-bold shrink-0 transition-colors ${
                        canAfford
                          ? `${branch.btnClass} cursor-pointer`
                          : 'bg-slate-800 text-slate-500 opacity-65 cursor-not-allowed'
                      }`}
                    >
                      {isMaxed ? 'МАКС.' : `${cost} ТМ`}
                    </button>
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
};
