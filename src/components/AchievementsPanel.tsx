import React from 'react';
import { CheckCircle2, Trophy } from 'lucide-react';
import {
  ACHIEVEMENTS,
  AchievementMetric,
  formatCrystalCount,
} from '../types/game';

interface AchievementsPanelProps {
  totalClicks: number;
  totalCrystalsEarned: number;
  totalUpgradesBought: number;
  passivePerSecond: number;
  criticalHits: number;
  cometsCaught: number;
  prestigeCount: number;
  bossesDefeated: number;
  feverActivations: number;
  secretHeaderUnlocked: boolean;
  claimedAchievements: string[];
  onClaimAchievement: (achievementId: string) => void;
}

export const AchievementsPanel: React.FC<AchievementsPanelProps> = ({
  totalClicks,
  totalCrystalsEarned,
  totalUpgradesBought,
  passivePerSecond,
  criticalHits,
  cometsCaught,
  prestigeCount,
  bossesDefeated,
  feverActivations,
  secretHeaderUnlocked,
  claimedAchievements,
  onClaimAchievement,
}) => {
  const getMetricValue = (metric: AchievementMetric): number => {
    switch (metric) {
      case 'TOTAL_CLICKS':
        return totalClicks;
      case 'TOTAL_CRYSTALS_EARNED':
        return totalCrystalsEarned;
      case 'TOTAL_UPGRADES_BOUGHT':
        return totalUpgradesBought;
      case 'PASSIVE_PER_SEC':
        return passivePerSecond;
      case 'CRITICAL_HITS':
        return criticalHits;
      case 'COMETS_CAUGHT':
        return cometsCaught;
      case 'PRESTIGE_COUNT':
        return prestigeCount;
      case 'BOSSES_DEFEATED':
        return bossesDefeated;
      case 'FEVER_COUNT':
        return feverActivations;
      case 'SECRET_HEADER_TAP':
        return secretHeaderUnlocked ? 1 : 0;
    }
  };

  return (
    <div className="space-y-2.5">
      {ACHIEVEMENTS.map((item) => {
        const current = getMetricValue(item.metric);
        const progress = Math.min(1, current / item.targetValue);
        const isUnlocked = current >= item.targetValue;
        const isClaimed = claimedAchievements.includes(item.id);
        const isReady = isUnlocked && !isClaimed;
        const hideSecret = Boolean(item.isSecret && !isUnlocked);

        return (
          <div
            key={item.id}
            className={`p-3.5 rounded-2xl bg-[#121833]/90 border transition-colors ${
              isReady
                ? 'border-amber-400/70'
                : isClaimed
                ? 'border-emerald-400/35'
                : 'border-slate-800/80'
            }`}
          >
            <div className="flex items-center gap-3">
              <div
                className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                  isClaimed
                    ? 'bg-emerald-400/15 text-emerald-300'
                    : 'bg-amber-400/15 text-amber-300'
                }`}
              >
                {isClaimed ? (
                  <CheckCircle2 className="w-5 h-5" />
                ) : (
                  <Trophy className="w-5 h-5" />
                )}
              </div>

              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold text-slate-100 truncate">
                  {hideSecret ? 'Секретное достижение' : item.title}
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  {hideSecret
                    ? 'Откройте это достижение во время игры'
                    : item.description}
                </p>
              </div>

              <div className="shrink-0">
                {isClaimed ? (
                  <span className="text-xs font-semibold text-emerald-300">
                    Получено
                  </span>
                ) : isReady ? (
                  <button
                    type="button"
                    onClick={() => onClaimAchievement(item.id)}
                    className="min-h-[44px] px-3.5 py-2 rounded-xl bg-amber-400 hover:bg-amber-300 active:scale-95 text-slate-950 font-mono-num text-xs font-bold whitespace-nowrap cursor-pointer transition-all"
                  >
                    Забрать (+{formatCrystalCount(item.rewardCrystals)})
                  </button>
                ) : (
                  <span className="text-xs font-mono-num font-semibold text-amber-300/80">
                    +{formatCrystalCount(item.rewardCrystals)}
                  </span>
                )}
              </div>
            </div>

            <div className="mt-3 flex items-center gap-3">
              <div className="flex-1 h-1.5 rounded-full bg-slate-800 overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-200 ${
                    isUnlocked ? 'bg-amber-400' : 'bg-violet-500'
                  }`}
                  style={{ width: `${Math.round(progress * 100)}%` }}
                />
              </div>
              <span className="text-[11px] font-mono-num text-slate-400 shrink-0">
                {formatCrystalCount(Math.min(current, item.targetValue))} /{' '}
                {formatCrystalCount(item.targetValue)}
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
};
