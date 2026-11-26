import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Code2, Settings } from 'lucide-react';
import {
  ACHIEVEMENTS,
  MAX_OFFLINE_SECONDS,
  OVERCHARGE_DURATION_SEC,
  OVERCHARGE_MULTIPLIER,
  OfflineReward,
  PersistedGameState,
  RANKS,
  UPGRADES,
  calculatePotentialDarkMatter,
  formatCrystalCount,
  formatDuration,
  formatRate,
  getUpgradeCost,
  nextDarkMatterRequirement,
} from './types/game';
import { CrystalCanvas } from './components/CrystalCanvas';
import { UpgradesPanel } from './components/UpgradesPanel';
import { AchievementsPanel } from './components/AchievementsPanel';
import { PrestigePanel } from './components/PrestigePanel';
import { SettingsModal } from './components/SettingsModal';
import { AndroidCodeModal } from './components/AndroidCodeModal';
import {
  playCometSound,
  playCrystalTapSound,
  playPrestigeSound,
  playUpgradeSound,
} from './utils/audio';

const STORAGE_KEY = 'cosmic_miner_save_v1';

const INITIAL_STATE: PersistedGameState = {
  crystals: 0,
  runCrystalsEarned: 0,
  totalCrystalsEarned: 0,
  experience: 0,
  totalClicks: 0,
  criticalHits: 0,
  cometsCaught: 0,
  darkMatter: 0,
  prestigeCount: 0,
  upgradeLevels: {},
  claimedAchievements: [],
  isVibrationEnabled: true,
  isSoundEnabled: true,
  lastActiveTimestampMs: 0,
};

function computeRank(experience: number) {
  let curr = RANKS[0];
  let next: (typeof RANKS)[number] | null = RANKS[1] ?? null;
  for (let i = 0; i < RANKS.length; i++) {
    if (experience >= RANKS[i].xpThreshold) {
      curr = RANKS[i];
      next = RANKS[i + 1] ?? null;
    }
  }
  return { curr, next };
}

function loadInitialData(): { state: PersistedGameState; offline: OfflineReward | null } {
  if (typeof window === 'undefined') return { state: INITIAL_STATE, offline: null };
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return { state: INITIAL_STATE, offline: null };
    const parsed = JSON.parse(raw) as Partial<PersistedGameState>;
    const merged: PersistedGameState = {
      ...INITIAL_STATE,
      ...parsed,
      runCrystalsEarned: parsed.runCrystalsEarned ?? parsed.totalCrystalsEarned ?? 0,
      upgradeLevels: parsed.upgradeLevels ?? {},
      claimedAchievements: Array.isArray(parsed.claimedAchievements)
        ? parsed.claimedAchievements
        : [],
    };

    let offline: OfflineReward | null = null;
    const now = Date.now();
    if (merged.lastActiveTimestampMs > 0) {
      const elapsedSec = Math.min(
        MAX_OFFLINE_SECONDS,
        Math.max(0, Math.floor((now - merged.lastActiveTimestampMs) / 1000))
      );
      const { curr } = computeRank(merged.experience);
      const prestigeMult = 1 + merged.darkMatter * 0.5;
      const passiveBonus = UPGRADES.filter((u) => u.type === 'PASSIVE_INCOME').reduce(
        (sum, u) => sum + (merged.upgradeLevels[u.id] ?? 0) * u.baseGain,
        0
      );
      const passivePerSec = passiveBonus * curr.multiplier * prestigeMult;

      if (elapsedSec >= 10 && passivePerSec > 0) {
        const earned = passivePerSec * elapsedSec;
        merged.crystals += earned;
        merged.runCrystalsEarned += earned;
        merged.totalCrystalsEarned += earned;
        merged.experience += earned;
        offline = { elapsedSeconds: elapsedSec, crystalsEarned: earned };
      }
    }

    merged.lastActiveTimestampMs = now;
    return { state: merged, offline };
  } catch {
    return { state: INITIAL_STATE, offline: null };
  }
}

export default function App() {
  const initialData = useMemo(() => loadInitialData(), []);
  const [game, setGame] = useState<PersistedGameState>(initialData.state);
  const [offlineReward, setOfflineReward] = useState<OfflineReward | null>(initialData.offline);
  const [activeTab, setActiveTab] = useState<'UPGRADES' | 'ACHIEVEMENTS' | 'PRESTIGE'>('UPGRADES');
  const [showSettings, setShowSettings] = useState(false);
  const [showAndroidCode, setShowAndroidCode] = useState(false);

  const [overchargeSecondsLeft, setOverchargeSecondsLeft] = useState(0);
  const [isCometVisible, setIsCometVisible] = useState(false);
  const [cometSeed, setCometSeed] = useState(1);

  const gameRef = useRef(game);
  gameRef.current = game;

  const { currentRank, nextRank, rankProgress } = useMemo(() => {
    const { curr, next } = computeRank(game.experience);
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

  const prestigeMultiplier = useMemo(() => 1 + game.darkMatter * 0.5, [game.darkMatter]);

  const baseClickPower = useMemo(() => {
    const bonus = UPGRADES.filter((u) => u.type === 'CLICK_POWER').reduce(
      (sum, u) => sum + (game.upgradeLevels[u.id] ?? 0) * u.baseGain,
      0
    );
    return (1 + bonus) * currentRank.multiplier * prestigeMultiplier;
  }, [game.upgradeLevels, currentRank.multiplier, prestigeMultiplier]);

  const clickPower = useMemo(
    () => baseClickPower * (overchargeSecondsLeft > 0 ? OVERCHARGE_MULTIPLIER : 1),
    [baseClickPower, overchargeSecondsLeft]
  );

  const passivePerSecond = useMemo(() => {
    const bonus = UPGRADES.filter((u) => u.type === 'PASSIVE_INCOME').reduce(
      (sum, u) => sum + (game.upgradeLevels[u.id] ?? 0) * u.baseGain,
      0
    );
    return bonus * currentRank.multiplier * prestigeMultiplier;
  }, [game.upgradeLevels, currentRank.multiplier, prestigeMultiplier]);

  const totalUpgradesBought = useMemo(
    () => Object.values(game.upgradeLevels).reduce((sum, lvl) => sum + lvl, 0),
    [game.upgradeLevels]
  );

  const potentialDarkMatter = useMemo(
    () => calculatePotentialDarkMatter(game.runCrystalsEarned),
    [game.runCrystalsEarned]
  );

  const nextDmThreshold = useMemo(
    () => nextDarkMatterRequirement(potentialDarkMatter),
    [potentialDarkMatter]
  );

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
        case 'COMETS_CAUGHT':
          val = game.cometsCaught;
          break;
        case 'PRESTIGE_COUNT':
          val = game.prestigeCount;
          break;
      }
      return val >= ach.targetValue;
    }).length;
  }, [
    game.claimedAchievements,
    game.totalClicks,
    game.totalCrystalsEarned,
    game.criticalHits,
    game.cometsCaught,
    game.prestigeCount,
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
        runCrystalsEarned: prev.runCrystalsEarned + delta,
        totalCrystalsEarned: prev.totalCrystalsEarned + delta,
        experience: prev.experience + delta,
        lastActiveTimestampMs: Date.now(),
      }));
    }, 100);

    return () => window.clearInterval(interval);
  }, []);

  // Overcharge 1s countdown
  useEffect(() => {
    if (overchargeSecondsLeft <= 0) return;
    const timer = window.setTimeout(() => {
      setOverchargeSecondsLeft((s) => Math.max(0, s - 1));
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [overchargeSecondsLeft]);

  // Random cosmic comet event every 35-55 seconds
  useEffect(() => {
    if (isCometVisible) {
      const hideTimer = window.setTimeout(() => {
        setIsCometVisible(false);
      }, 6300);
      return () => window.clearTimeout(hideTimer);
    } else {
      const delayMs = 35000 + Math.floor(Math.random() * 20000);
      const spawnTimer = window.setTimeout(() => {
        setCometSeed(Date.now());
        setIsCometVisible(true);
      }, delayMs);
      return () => window.clearTimeout(spawnTimer);
    }
  }, [isCometVisible]);

  // Persist progress periodically & on unload
  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        window.localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({ ...gameRef.current, lastActiveTimestampMs: Date.now() })
        );
      } catch {
        // Ignore storage quota issues
      }
    }, 350);
    return () => window.clearTimeout(timer);
  }, [game]);

  useEffect(() => {
    const saveOnUnload = () => {
      try {
        window.localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({ ...gameRef.current, lastActiveTimestampMs: Date.now() })
        );
      } catch {
        // Ignore
      }
    };
    window.addEventListener('beforeunload', saveOnUnload);
    return () => window.removeEventListener('beforeunload', saveOnUnload);
  }, []);

  const handleTapCrystal = useCallback(() => {
    const isCritical = Math.random() < 0.1;
    const gained = isCritical ? clickPower * 3 : clickPower;

    if (gameRef.current.isSoundEnabled) {
      playCrystalTapSound(isCritical);
    }

    setGame((prev) => ({
      ...prev,
      crystals: prev.crystals + gained,
      runCrystalsEarned: prev.runCrystalsEarned + gained,
      totalCrystalsEarned: prev.totalCrystalsEarned + gained,
      experience: prev.experience + gained,
      totalClicks: prev.totalClicks + 1,
      criticalHits: prev.criticalHits + (isCritical ? 1 : 0),
      lastActiveTimestampMs: Date.now(),
    }));

    return { gained, isCritical };
  }, [clickPower]);

  const handleCatchComet = useCallback(() => {
    if (!isCometVisible) return 0;
    setIsCometVisible(false);
    setOverchargeSecondsLeft(OVERCHARGE_DURATION_SEC);

    const bonus = Math.max(baseClickPower * 25, passivePerSecond * 20, 100);
    if (gameRef.current.isSoundEnabled) {
      playCometSound();
    }

    setGame((prev) => ({
      ...prev,
      crystals: prev.crystals + bonus,
      runCrystalsEarned: prev.runCrystalsEarned + bonus,
      totalCrystalsEarned: prev.totalCrystalsEarned + bonus,
      experience: prev.experience + bonus,
      cometsCaught: prev.cometsCaught + 1,
      lastActiveTimestampMs: Date.now(),
    }));

    return bonus;
  }, [isCometVisible, baseClickPower, passivePerSecond]);

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
        lastActiveTimestampMs: Date.now(),
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
        runCrystalsEarned: prev.runCrystalsEarned + def.rewardCrystals,
        totalCrystalsEarned: prev.totalCrystalsEarned + def.rewardCrystals,
        experience: prev.experience + def.rewardCrystals,
        claimedAchievements: [...prev.claimedAchievements, achievementId],
        lastActiveTimestampMs: Date.now(),
      };
    });
  }, []);

  const handlePerformCollapse = useCallback(() => {
    const gainedDm = calculatePotentialDarkMatter(gameRef.current.runCrystalsEarned);
    if (gainedDm <= 0) return;

    if (gameRef.current.isSoundEnabled) {
      playPrestigeSound();
    }

    setOverchargeSecondsLeft(0);
    setActiveTab('UPGRADES');
    setGame((prev) => ({
      ...prev,
      crystals: 0,
      runCrystalsEarned: 0,
      experience: 0,
      upgradeLevels: {},
      darkMatter: prev.darkMatter + gainedDm,
      prestigeCount: prev.prestigeCount + 1,
      lastActiveTimestampMs: Date.now(),
    }));
  }, []);

  const handleResetProgress = useCallback(() => {
    setOverchargeSecondsLeft(0);
    setOfflineReward(null);
    setGame((prev) => {
      const resetState: PersistedGameState = {
        ...INITIAL_STATE,
        isVibrationEnabled: prev.isVibrationEnabled,
        isSoundEnabled: prev.isSoundEnabled,
        lastActiveTimestampMs: Date.now(),
      };
      try {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(resetState));
      } catch {
        // Ignore
      }
      return resetState;
    });
  }, []);

  const handleExportBackup = useCallback(() => {
    const g = gameRef.current;
    const payload = {
      crystals: g.crystals,
      runCrystalsEarned: g.runCrystalsEarned,
      totalCrystalsEarned: g.totalCrystalsEarned,
      experience: g.experience,
      totalClicks: g.totalClicks,
      criticalHits: g.criticalHits,
      cometsCaught: g.cometsCaught,
      darkMatter: g.darkMatter,
      prestigeCount: g.prestigeCount,
      isVibrationEnabled: g.isVibrationEnabled,
      isSoundEnabled: g.isSoundEnabled,
      upgrades: g.upgradeLevels,
      achievements: g.claimedAchievements,
    };
    const bytes = new TextEncoder().encode(JSON.stringify(payload));
    let bin = '';
    bytes.forEach((b) => {
      bin += String.fromCharCode(b);
    });
    return 'CCM1:' + btoa(bin);
  }, []);

  const handleImportBackup = useCallback((rawCode: string): boolean => {
    try {
      const trimmed = rawCode.trim();
      if (!trimmed.startsWith('CCM1:')) return false;
      const bin = atob(trimmed.slice(5));
      const bytes = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++) {
        bytes[i] = bin.charCodeAt(i);
      }
      const parsed = JSON.parse(new TextDecoder().decode(bytes)) as {
        crystals?: number;
        runCrystalsEarned?: number;
        totalCrystalsEarned?: number;
        experience?: number;
        totalClicks?: number;
        criticalHits?: number;
        cometsCaught?: number;
        darkMatter?: number;
        prestigeCount?: number;
        isVibrationEnabled?: boolean;
        isSoundEnabled?: boolean;
        upgrades?: Record<string, number>;
        achievements?: string[];
      };
      const restored: PersistedGameState = {
        ...INITIAL_STATE,
        crystals: Math.max(0, Number(parsed.crystals ?? 0)),
        runCrystalsEarned: Math.max(0, Number(parsed.runCrystalsEarned ?? 0)),
        totalCrystalsEarned: Math.max(0, Number(parsed.totalCrystalsEarned ?? 0)),
        experience: Math.max(0, Number(parsed.experience ?? 0)),
        totalClicks: Math.max(0, Number(parsed.totalClicks ?? 0)),
        criticalHits: Math.max(0, Number(parsed.criticalHits ?? 0)),
        cometsCaught: Math.max(0, Number(parsed.cometsCaught ?? 0)),
        darkMatter: Math.max(0, Number(parsed.darkMatter ?? 0)),
        prestigeCount: Math.max(0, Number(parsed.prestigeCount ?? 0)),
        isVibrationEnabled: parsed.isVibrationEnabled ?? true,
        isSoundEnabled: parsed.isSoundEnabled ?? true,
        upgradeLevels: parsed.upgrades ?? {},
        claimedAchievements: Array.isArray(parsed.achievements) ? parsed.achievements : [],
        lastActiveTimestampMs: Date.now(),
      };
      setGame(restored);
      setOfflineReward(null);
      setOverchargeSecondsLeft(0);
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(restored));
      return true;
    } catch {
      return false;
    }
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-b from-[#060814] via-[#0B1026] to-[#080B1A] text-slate-100 flex flex-col">
      {/* Compact Header */}
      <header className="w-full max-w-6xl mx-auto px-4 sm:px-6 py-3 flex items-center justify-between gap-4 border-b border-slate-800/60">
        <h1 className="font-display text-base sm:text-lg font-bold tracking-tight text-slate-100 whitespace-nowrap truncate">
          Cosmic Crystal Miner
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
          <div>
            <div className="flex items-center gap-3">
              <span className="text-xs font-medium text-slate-400">Кристаллы</span>
              {game.darkMatter > 0 && (
                <span className="text-xs font-mono-num font-bold text-violet-300">
                  {game.darkMatter} ТМ (x{formatRate(prestigeMultiplier)})
                </span>
              )}
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
              <div className="text-xs font-bold text-amber-300 leading-snug">
                Ранг {currentRank.level} · {currentRank.title}
              </div>
              <div className="mt-0.5 font-mono-num text-[11px] text-slate-400">
                {nextRank
                  ? `Множитель x${currentRank.multiplier} · ${formatCrystalCount(game.experience)} / ${formatCrystalCount(nextRank.xpThreshold)}`
                  : `Макс. ранг · Множитель x${currentRank.multiplier}`}
              </div>
              <div className="mt-2 h-2 w-full rounded-full bg-slate-800/90 overflow-hidden">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-violet-500 via-cyan-400 to-amber-400 transition-all duration-200"
                  style={{ width: `${Math.round(rankProgress * 100)}%` }}
                />
              </div>
            </div>
          </div>

          <div className="my-auto py-3">
            <CrystalCanvas
              rankLevel={currentRank.level}
              overchargeSecondsLeft={overchargeSecondsLeft}
              isCometVisible={isCometVisible}
              cometSeed={cometSeed}
              onTapCrystal={handleTapCrystal}
              onCatchComet={handleCatchComet}
              isVibrationEnabled={game.isVibrationEnabled}
            />
          </div>
        </section>

        {/* Right / Bottom Zone: Upgrades, Achievements & Prestige Tabs */}
        <section className="lg:col-span-6 flex flex-col rounded-3xl bg-[#0B0F23]/95 border border-slate-800/90 overflow-hidden min-h-[460px] lg:max-h-[680px]">
          <div className="grid grid-cols-3 p-1.5 bg-[#0E142D] border-b border-slate-800/80 shrink-0">
            <button
              type="button"
              onClick={() => setActiveTab('UPGRADES')}
              className={`min-h-[44px] rounded-2xl text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer transition-colors ${
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
              className={`min-h-[44px] rounded-2xl text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer transition-colors flex items-center justify-center gap-1.5 ${
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
            <button
              type="button"
              onClick={() => setActiveTab('PRESTIGE')}
              className={`min-h-[44px] rounded-2xl text-xs sm:text-sm font-semibold whitespace-nowrap cursor-pointer transition-colors flex items-center justify-center gap-1.5 ${
                activeTab === 'PRESTIGE'
                  ? 'bg-[#1A2246] text-violet-300 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <span>Коллапс</span>
              {potentialDarkMatter > 0 && (
                <span className="font-mono-num text-xs font-bold text-amber-300">
                  (+{potentialDarkMatter})
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
            ) : activeTab === 'ACHIEVEMENTS' ? (
              <AchievementsPanel
                totalClicks={game.totalClicks}
                totalCrystalsEarned={game.totalCrystalsEarned}
                totalUpgradesBought={totalUpgradesBought}
                passivePerSecond={passivePerSecond}
                criticalHits={game.criticalHits}
                cometsCaught={game.cometsCaught}
                prestigeCount={game.prestigeCount}
                claimedAchievements={game.claimedAchievements}
                onClaimAchievement={handleClaimAchievement}
              />
            ) : (
              <PrestigePanel
                darkMatter={game.darkMatter}
                prestigeCount={game.prestigeCount}
                prestigeMultiplier={prestigeMultiplier}
                potentialDarkMatter={potentialDarkMatter}
                runCrystalsEarned={game.runCrystalsEarned}
                nextDarkMatterThreshold={nextDmThreshold}
                onPerformCollapse={handlePerformCollapse}
              />
            )}
          </div>
        </section>
      </main>

      {offlineReward && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="offline-title"
        >
          <div className="w-full max-w-sm rounded-3xl bg-[#111732] border border-cyan-400/35 p-6 shadow-2xl space-y-4">
            <h2 id="offline-title" className="font-display text-xl font-bold text-slate-100">
              С возвращением!
            </h2>
            <p className="text-sm text-slate-300 leading-relaxed">
              Пока вас не было (
              <span className="font-semibold text-cyan-300">
                {formatDuration(offlineReward.elapsedSeconds)}
              </span>
              ), ваш флот добыл{' '}
              <span className="font-mono-num font-bold text-amber-300">
                +{formatCrystalCount(offlineReward.crystalsEarned)}
              </span>{' '}
              кристаллов!
            </p>
            <button
              type="button"
              onClick={() => setOfflineReward(null)}
              className="w-full min-h-[44px] rounded-xl bg-cyan-400 hover:bg-cyan-300 text-slate-950 font-bold text-sm cursor-pointer transition-colors"
            >
              Забрать
            </button>
          </div>
        </div>
      )}

      {showSettings && (
        <SettingsModal
          isVibrationEnabled={game.isVibrationEnabled}
          isSoundEnabled={game.isSoundEnabled}
          totalClicks={game.totalClicks}
          criticalHits={game.criticalHits}
          cometsCaught={game.cometsCaught}
          darkMatter={game.darkMatter}
          totalCrystalsEarned={game.totalCrystalsEarned}
          onToggleVibration={(enabled) =>
            setGame((prev) => ({ ...prev, isVibrationEnabled: enabled }))
          }
          onToggleSound={(enabled) =>
            setGame((prev) => ({ ...prev, isSoundEnabled: enabled }))
          }
          onExportBackup={handleExportBackup}
          onImportBackup={handleImportBackup}
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
