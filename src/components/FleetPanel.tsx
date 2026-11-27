import React from 'react';
import { CheckCircle2, Compass, Flame, ListChecks } from 'lucide-react';
import {
  BossUiState,
  DAILY_QUESTS,
  DailyQuestType,
  EXPEDITIONS,
  formatDuration,
} from '../types/game';

interface FleetPanelProps {
  bossState: BossUiState;
  activeExpeditionId: string | null;
  expeditionRemainingSec: number;
  expeditionLastMessage: string | null;
  dailyClicks: number;
  dailyUpgrades: number;
  dailyFever: number;
  dailyClaimedIds: string[];
  onSummonBoss: () => void;
  onStartExpedition: (presetId: string) => void;
  onClaimExpedition: () => void;
  onClaimDailyQuest: (questId: string) => void;
}

export const FleetPanel: React.FC<FleetPanelProps> = ({
  bossState,
  activeExpeditionId,
  expeditionRemainingSec,
  expeditionLastMessage,
  dailyClicks,
  dailyUpgrades,
  dailyFever,
  dailyClaimedIds,
  onSummonBoss,
  onStartExpedition,
  onClaimExpedition,
  onClaimDailyQuest,
}) => {
  const canSummonBoss = !bossState.isActive && bossState.cooldownSeconds === 0;
  const activePreset = EXPEDITIONS.find((e) => e.id === activeExpeditionId);
  const expeditionDone = Boolean(activeExpeditionId) && expeditionRemainingSec <= 0;

  const dailyValue = (type: DailyQuestType): number => {
    switch (type) {
      case 'CLICKS':
        return dailyClicks;
      case 'UPGRADES':
        return dailyUpgrades;
      case 'FEVER':
        return dailyFever;
    }
  };

  return (
    <div className="space-y-4">
      {/* Boss Asteroid Card */}
      <div className="p-4 rounded-2xl bg-[#121833]/90 border border-red-400/35 space-y-2.5">
        <div className="flex items-center gap-2">
          <Flame className="w-4 h-4 text-red-400 shrink-0" />
          <h2 className="text-sm font-bold text-slate-100">Босс-астероид (30 сек)</h2>
        </div>
        <p className="text-xs text-slate-400 leading-relaxed">
          Разбейте бронированного Левиафана быстрыми кликами и дронами, чтобы получить сундук с кристаллами и шанс на Тёмную материю.
        </p>
        {bossState.lastResultMessage && (
          <div className="text-xs font-semibold text-amber-300">
            {bossState.lastResultMessage}
          </div>
        )}
        <button
          type="button"
          onClick={onSummonBoss}
          disabled={!canSummonBoss}
          className={`w-full min-h-[42px] rounded-xl text-xs font-bold transition-all ${
            canSummonBoss
              ? 'bg-red-600 hover:bg-red-500 active:scale-[0.98] text-white cursor-pointer'
              : 'bg-slate-800/75 text-slate-400 cursor-not-allowed'
          }`}
        >
          {bossState.isActive
            ? `Бой уже идёт (${bossState.remainingSeconds}с)`
            : bossState.cooldownSeconds > 0
            ? `Перезарядка: ${bossState.cooldownSeconds}с`
            : 'Вызвать босса'}
        </button>
      </div>

      {/* Expeditions Card */}
      <div className="p-4 rounded-2xl bg-[#121833]/90 border border-cyan-400/30 space-y-2.5">
        <div className="flex items-center gap-2">
          <Compass className="w-4 h-4 text-cyan-300 shrink-0" />
          <h2 className="text-sm font-bold text-slate-100">Экспедиции флота</h2>
        </div>

        {expeditionLastMessage && (
          <div className="text-xs font-semibold text-emerald-300">
            {expeditionLastMessage}
          </div>
        )}

        {activeExpeditionId ? (
          <div className="space-y-2.5 pt-1">
            <div className="text-sm font-semibold text-cyan-300">
              {activePreset?.title ?? 'Экспедиция'}
            </div>
            {expeditionDone ? (
              <button
                type="button"
                onClick={onClaimExpedition}
                className="w-full min-h-[42px] rounded-xl bg-emerald-400 hover:bg-emerald-300 text-slate-950 text-xs font-bold cursor-pointer transition-colors"
              >
                Забрать находки
              </button>
            ) : (
              <div className="text-xs font-mono-num text-slate-300">
                До возвращения: {formatDuration(expeditionRemainingSec)}
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-2">
            {EXPEDITIONS.map((preset) => (
              <div
                key={preset.id}
                className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-[#0B0F23] border border-slate-800/80"
              >
                <div className="min-w-0">
                  <div className="text-xs font-semibold text-slate-100">
                    {preset.title} · {Math.round(preset.durationSeconds / 60)} мин
                  </div>
                  <div className="text-[11px] text-slate-400 mt-0.5">
                    {preset.subtitle}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => onStartExpedition(preset.id)}
                  className="min-h-[36px] px-3.5 rounded-lg bg-cyan-400 hover:bg-cyan-300 text-slate-950 text-xs font-bold shrink-0 cursor-pointer transition-colors"
                >
                  Старт
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Daily Mini-Quests */}
      <div className="space-y-2">
        <div className="flex items-center gap-2 px-1">
          <ListChecks className="w-4 h-4 text-amber-300" />
          <h2 className="text-xs font-bold text-amber-300">Ежедневные мини-квесты</h2>
        </div>

        {DAILY_QUESTS.map((quest) => {
          const current = dailyValue(quest.type);
          const progress = Math.min(1, current / quest.target);
          const isCompleted = current >= quest.target;
          const isClaimed = dailyClaimedIds.includes(quest.id);

          return (
            <div
              key={quest.id}
              className={`p-3 rounded-2xl bg-[#121833]/90 border space-y-2 ${
                isCompleted && !isClaimed ? 'border-amber-400/60' : 'border-slate-800/80'
              }`}
            >
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2.5 min-w-0">
                  <CheckCircle2
                    className={`w-4 h-4 shrink-0 ${
                      isClaimed ? 'text-emerald-400' : 'text-amber-300'
                    }`}
                  />
                  <div className="min-w-0">
                    <div className="text-xs font-semibold text-slate-100">
                      {quest.title}
                    </div>
                    <div className="text-[11px] text-slate-400">
                      Награда: +{quest.rewardDarkMatter} ТМ и кристаллы
                    </div>
                  </div>
                </div>

                {isClaimed ? (
                  <span className="text-xs font-bold text-emerald-300 shrink-0">
                    Выполнено
                  </span>
                ) : isCompleted ? (
                  <button
                    type="button"
                    onClick={() => onClaimDailyQuest(quest.id)}
                    className="min-h-[34px] px-3 rounded-lg bg-amber-400 hover:bg-amber-300 text-slate-950 text-xs font-bold shrink-0 cursor-pointer transition-colors"
                  >
                    Забрать
                  </button>
                ) : (
                  <span className="text-xs font-mono-num font-bold text-cyan-300 shrink-0">
                    {Math.min(current, quest.target)}/{quest.target}
                  </span>
                )}
              </div>

              <div className="h-1.5 rounded-full bg-slate-800 overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-200 ${
                    isCompleted ? 'bg-amber-400' : 'bg-violet-500'
                  }`}
                  style={{ width: `${Math.round(progress * 100)}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
