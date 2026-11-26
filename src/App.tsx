import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Code2, Settings } from 'lucide-react';
import {
  ACHIEVEMENTS,
  PersistedGameState,
  RANKS,
  UPGRADES,
  formatCrystalCount,
  formatRate,
  getUpgradeCost,
} from './types/game';
import { CrystalCanvas } from './components/CrystalCanvas';
import { UpgradesPanel } from './components/UpgradesPanel';
import { AchievementsPanel } from './components/AchievementsPanel';
import { SettingsModal } from './components/SettingsModal';
import { AndroidCodeModal } from './components/AndroidCodeModal';
import { playCrystalTapSound, playUpgradeSound } from './utils/audio';

const STORAGE_KEY = 'cosmic_miner_save_v1';

const INITIAL_STATE: PersistedGameState = {
  crystals: 0,
  totalCrystalsEarned: 0,
  experience: 0,
  totalClicks: 0,
  criticalHits: 0,
  upgradeLevels: {},
  claimedAchievements: [],
  isVibrationEnabled: true,
  isSoundEnabled: true,
};

function loadInitialState(): PersistedGameState {
  if (typeof window === 'undefined') return INITIAL_STATE;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return INITIAL_STATE;
    const parsed = JSON.parse(raw) as Partial<PersistedGameState>;
    return {
      ...INITIAL_STATE,
      ...parsed,
      upgradeLevels: parsed.upgradeLevels ?? {},
      claimedAchievements: Array.isArray(parsed.claimedAchievements)
        ? parsed.claimedAchievements
        : [],
    };
  } catch {
    return INITIAL_STATE;
  }
}

export default function App() {
  const [game, setGame] = useState<PersistedGameState>(loadInitialState);
  const [activeTab, setActiveTab] = useState<'UPGRADES' | 'ACHIEVEMENTS'>('UPGRADES');
  const [showSettings, setShowSettings] = useState(false);
  const [showAndroidCode, setShowAndroidCode] = useState(false);

  const gameRef = useRef(game);
  gameRef.current = game;

  // Resolve current and next rank
  const { currentRank, nextRank, rankProgress } = useMemo(() => {
    let curr = RANKS[0];
    let next: (typeof RANKS)[number] | null = RANKS[1] ?? null;

    for (let i = 0; i < RANKS.length; i++) {
      if (game.experience >= RANKS[i].xpThreshold) {
        curr = RANKS[i];
        next = RANKS[i + 1] ?? null;
      }
    }

    const progress = next
      ? Math.min(
          1,
          Math.max(
            0,
            (game.experience - curr.xpThreshold) /
              Math.max(1, next.xpThreshold - curr.xpThreshold)
          )
        )
      : 1;

    return { currentRank: curr, nextRank: next, rankProgress: progress };
  }, [game.experience]);

  // Derived click power & passive income per second
  const clickPower = useMemo(() => {
    const bonus = UPGRADES.filter((u) => u.type === 'CLICK_POWER').reduce(
      (sum, u) => sum + (game.upgradeLevels[u.id] ?? 0) * u.baseGain,
      0
    );
    return (1 + bonus) * currentRank.multiplier;
  }, [game.upgradeLevels, currentRank.multiplier]);

  const passivePerSecond = useMemo(() => {
    const bonus = UPGRADES.filter((u) => u.type === 'PASSIVE_INCOME').reduce(
      (sum, u) => sum + (game.upgradeLevels[u.id] ?? 0) * u.baseGain,
      0
    );
    return bonus * currentRank.multiplier;
  }, [game.upgradeLevels, currentRank.multiplier]);

  const totalUpgradesBought = useMemo(
    () => Object.values(game.upgradeLevels).reduce((sum, lvl) => sum + lvl, 0),
    [game.upgradeLevels]
  );

  // Unclaimed ready achievements counter
  const unclaimedCount = useMemo(() => {
    return ACHIEVEMENTS.filter((ach) => {
      if (game.claimedAchievements.includes(ach.id)) return false;
      let val = 0;
      switch (ach.metric) {
        case 'TOTAL_CLICKS':
          val = game.totalClicks;
          break;
        case 'TOTAL_CRYSTALS_EARNED':
          val = game.totalCrystalsEarned;
          break;
        case 'TOTAL_UPGRADES_BOUGHT':
          val = totalUpgradesBought;
          break;
        case 'PASSIVE_PER_SEC':
          val = passivePerSecond;
          break;
        case 'CRITICAL_HITS':
          val = game.criticalHits;
          break;
      }
      return val >= ach.targetValue;
    }).length;
  }, [
    game.claimedAchievements,
    game.totalClicks,
    game.totalCrystalsEarned,
    game.criticalHits,
    totalUpgradesBought,
    passivePerSecond,
  ]);

  // Smooth passive income tick (10 times per second)
  const passiveRef = useRef(passivePerSecond);
  passiveRef.current = passivePerSecond;

  useEffect(() => {
    const interval = window.setInterval(() => {
      const pps = passiveRef.current;
      if (pps <= 0) return;
      const delta = pps / 10;
      setGame((prev) => ({
        ...prev,
        crystals: prev.crystals + delta,
        totalCrystalsEarned: prev.totalCrystalsEarned + delta,
        experience: prev.experience + delta,
      }));
    }, 100);

    return () => window.clearInterval(interval);
  }, []);

  // Persist progress periodically & on state changes (throttled cleanly)
  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(gameRef.current));
      } catch {
        // Ignore storage quota issues
      }
    }, 350);
    return () => window.clearTimeout(timer);
  }, [game]);

  const handleTapCrystal = useCallback(() => {
    const isCritical = Math.random() < 0.1;
    const gained = isCritical ? clickPower * 3 : clickPower;

    if (gameRef.current.isSoundEnabled) {
      playCrystalTapSound(isCritical);
    }

    setGame((prev) => ({
      ...prev,
      crystals: prev.crystals + gained,
      totalCrystalsEarned: prev.totalCrystalsEarned + gained,
      experience: prev.experience + gained,
      totalClicks: prev.totalClicks + 1,
      criticalHits: prev.criticalHits + (isCritical ? 1 : 0),
    }));

    return { gained, isCritical };
  }, [clickPower]);

  const handleBuyUpgrade = useCallback((upgradeId: string) => {
    const def = UPGRADES.find((u) => u.id === upgradeId);
    if (!def) return;

    setGame((prev) => {
      const level = prev.upgradeLevels[upgradeId] ?? 0;
      const cost = getUpgradeCost(def, level);
      if (prev.crystals < cost) return prev;

      if (prev.isSoundEnabled) {
        playUpgradeSound();
      }

      return {
        ...prev,
        crystals: prev.crystals - cost,
        upgradeLevels: {
          ...prev.upgradeLevels,
          [upgradeId]: level + 1,
        },
      };
    });
  }, []);

  const handleClaimAchievement = useCallback((achievementId: string) => {
    const def = ACHIEVEMENTS.find((a) => a.id === achievementId);
    if (!def) return;

    setGame((prev) => {
      if (prev.claimedAchievements.includes(achievementId)) return prev;
      if (prev.isSoundEnabled) {
        playUpgradeSound();
      }
      return {
        ...prev,
        crystals: prev.crystals + def.rewardCrystals,
        totalCrystalsEarned: prev.totalCrystalsEarned + def.rewardCrystals,
        experience: prev.experience + def.rewardCrystals,
        claimedAchievements: [...prev.claimedAchievements, achievementId],
      };
    });
  }, []);

  const handleResetProgress = useCallback(() => {
    setGame((prev) => {
      const resetState: PersistedGameState = {
        ...INITIAL_STATE,
        isVibrationEnabled: prev.isVibrationEnabled,
        isSoundEnabled: prev.isSoundEnabled,
      };
      try {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(resetState));
      } catch {
        // Ignore
      }
      return resetState;
    });
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-b from-[#060814] via-[#0B1026] to-[#080B1A] text-slate-100 flex flex-col">
      {/* Compact Header */}
      <header className="w-full max-w-6xl mx-auto px-4 sm:px-6 py-3 flex items-center justify-between gap-4 border-b border-slate-800/60">
        <h1 className="font-display text-base sm:text-lg font-bold tracking-tight text-slate-100 whitespace-nowrap truncate">
          Космический добытчик
        </h1>

        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => setShowAndroidCode(true)}
            className="min-h-[40px] flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900/90 hover:bg-slate-800 border border-slate-700/70 text-xs font-semibold text-cyan-300 whitespace-nowrap cursor-pointer transition-colors"
          >
            <Code2 className="w-4 h-4 shrink-0" />
            <span>Kotlin код</span>
          </button>

          <button
            type="button"
            onClick={() => setShowSettings(true)}
            aria-label="Настройки"
            className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-slate-300 hover:text-white hover:bg-slate-800/70 cursor-pointer transition-colors"
          >
            <Settings className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* Main Responsive Arena + Shop Layout */}
      <main className="flex-1 w-full max-w-6xl mx-auto px-4 sm:px-6 py-4 lg:py-6 grid grid-cols-1 lg:grid-cols-12 gap-5 lg:gap-8 items-stretch">
        {/* Left / Top Zone: Currency HUD, Rank Bar, Interactive Crystal */}
        <section className="lg:col-span-6 flex flex-col justify-between rounded-3xl bg-[#0D132B]/75 border border-violet-400/15 p-5 sm:p-6">
          {/* Currency Counter */}
          <div>
            <div className="text-xs font-medium text-slate-400">
              Кристаллы
            </div>
            <div className="font-display font-mono-num text-4xl sm:text-5xl font-extrabold text-white tracking-tight mt-0.5">
              {formatCrystalCount(game.crystals)}
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm font-mono-num font-semibold">
              <span className="text-violet-300">
                +{formatRate(passivePerSecond)} / сек
              </span>
              <span className="text-slate-600" aria-hidden="true">
                ·
              </span>
              <span className="text-cyan-300">
                +{formatRate(clickPower)} за клик
              </span>
            </div>

            {/* Rank & XP Bar */}
            <div className="mt-5">
              <div className="flex items-center justify-between gap-2 text-xs">
                <span className="font-bold text-amber-300">
                  Ранг {currentRank.level} · {currentRank.title}
                </span>
                <span className="font-mono-num text-slate-400">
                  {nextRank
                    ? `x${currentRank.multiplier} · ${formatCrystalCount(game.experience)} / ${formatCrystalCount(nextRank.xpThreshold)}`
                    : `Макс. ранг · x${currentRank.multiplier}`}
                </span>
              </div>
              <div className="mt-2 h-2 w-full rounded-full bg-slate-800/90 overflow-hidden">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-violet-500 via-cyan-400 to-amber-400 transition-all duration-200"
                  style={{ width: `${Math.round(rankProgress * 100)}%` }}
                />
              </div>
            </div>
          </div>

          {/* Central Interactive Crystal Canvas */}
          <div className="my-auto py-3">
            <CrystalCanvas
              onTapCrystal={handleTapCrystal}
              isVibrationEnabled={game.isVibrationEnabled}
            />
          </div>
        </section>

        {/* Right / Bottom Zone: Upgrades & Achievements Tabs */}
        <section className="lg:col-span-6 flex flex-col rounded-3xl bg-[#0B0F23]/95 border border-slate-800/90 overflow-hidden min-h-[440px] lg:max-h-[680px]">
          <div className="grid grid-cols-2 p-1.5 bg-[#0E142D] border-b border-slate-800/80 shrink-0">
            <button
              type="button"
              onClick={() => setActiveTab('UPGRADES')}
              className={`min-h-[44px] rounded-2xl text-sm font-semibold whitespace-nowrap cursor-pointer transition-colors ${
                activeTab === 'UPGRADES'
                  ? 'bg-[#1A2246] text-cyan-300 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Улучшения
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('ACHIEVEMENTS')}
              className={`min-h-[44px] rounded-2xl text-sm font-semibold whitespace-nowrap cursor-pointer transition-colors flex items-center justify-center gap-2 ${
                activeTab === 'ACHIEVEMENTS'
                  ? 'bg-[#1A2246] text-amber-300 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <span>Достижения</span>
              {unclaimedCount > 0 && (
                <span className="font-mono-num text-xs font-bold text-amber-300">
                  ({unclaimedCount})
                </span>
              )}
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-4">
            {activeTab === 'UPGRADES' ? (
              <UpgradesPanel
                crystals={game.crystals}
                upgradeLevels={game.upgradeLevels}
                onBuyUpgrade={handleBuyUpgrade}
              />
            ) : (
              <AchievementsPanel
                totalClicks={game.totalClicks}
                totalCrystalsEarned={game.totalCrystalsEarned}
                totalUpgradesBought={totalUpgradesBought}
                passivePerSecond={passivePerSecond}
                criticalHits={game.criticalHits}
                claimedAchievements={game.claimedAchievements}
                onClaimAchievement={handleClaimAchievement}
              />
            )}
          </div>
        </section>
      </main>

      {showSettings && (
        <SettingsModal
          isVibrationEnabled={game.isVibrationEnabled}
          isSoundEnabled={game.isSoundEnabled}
          totalClicks={game.totalClicks}
          criticalHits={game.criticalHits}
          totalCrystalsEarned={game.totalCrystalsEarned}
          onToggleVibration={(enabled) =>
            setGame((prev) => ({ ...prev, isVibrationEnabled: enabled }))
          }
          onToggleSound={(enabled) =>
            setGame((prev) => ({ ...prev, isSoundEnabled: enabled }))
          }
          onResetProgress={handleResetProgress}
          onClose={() => setShowSettings(false)}
        />
      )}

      {showAndroidCode && (
        <AndroidCodeModal onClose={() => setShowAndroidCode(false)} />
      )}
    </div>
  );
}
