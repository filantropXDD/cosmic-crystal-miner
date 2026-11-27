import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { BarChart3, Code2, Settings } from 'lucide-react';
import {
  ACHIEVEMENTS,
  BOSS_AUTO_SPAWN_SEC,
  BOSS_DURATION_SEC,
  BOSS_MANUAL_COOLDOWN_SEC,
  BossUiState,
  BuyQuantity,
  CrystalSkinId,
  DAILY_QUESTS,
  DailyQuestType,
  EXPEDITIONS,
  FEVER_DURATION_SEC,
  MAX_OFFLINE_SECONDS,
  MAX_UPGRADE_LEVEL,
  OVERCHARGE_DURATION_SEC,
  OVERCHARGE_MULTIPLIER,
  OfflineReward,
  PersistedGameState,
  RANKS,
  SpaceLocationId,
  TALENTS,
  UPGRADES,
  calculatePotentialDarkMatter,
  calculateUpgradePurchase,
  formatCrystalCount,
  formatDuration,
  formatRate,
  getTalentCost,
  nextDarkMatterRequirement,
} from './types/game';
import { CrystalCanvas } from './components/CrystalCanvas';
import { UpgradesPanel } from './components/UpgradesPanel';
import { FleetPanel } from './components/FleetPanel';
import { SkinsPanel } from './components/SkinsPanel';
import { AchievementsPanel } from './components/AchievementsPanel';
import { PrestigePanel } from './components/PrestigePanel';
import { SettingsModal } from './components/SettingsModal';
import { StatsModal } from './components/StatsModal';
import { AndroidCodeModal } from './components/AndroidCodeModal';
import {
  playBossDefeatedSound,
  playCometSound,
  playCrystalTapSound,
  playFeverStartSound,
  playPrestigeSound,
  playUpgradeSound,
} from './utils/audio';

const STORAGE_KEY = 'cosmic_miner_save_v1';

function todayDateKey(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function isMidnightHours(): boolean {
  const h = new Date().getHours();
  return h >= 0 && h <= 3;
}

const INITIAL_STATE: PersistedGameState = {
  crystals: 0,
  runCrystalsEarned: 0,
  totalCrystalsEarned: 0,
  experience: 0,
  totalClicks: 0,
  criticalHits: 0,
  cometsCaught: 0,
  bossesDefeated: 0,
  feverActivations: 0,
  bestCps: 0,
  playTimeSeconds: 0,
  darkMatter: 0,
  spentDarkMatter: 0,
  prestigeCount: 0,
  upgradeLevels: {},
  talentLevels: {},
  claimedAchievements: [],
  secretHeaderUnlocked: false,
  selectedSkinId: 'classic',
  selectedLocationId: 'nebula',
  activeExpeditionId: null,
  expeditionEndMs: 0,
  dailyDateKey: todayDateKey(),
  dailyClicks: 0,
  dailyUpgrades: 0,
  dailyFever: 0,
  dailyClaimedIds: [],
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
    const today = todayDateKey();
    const sameDay = parsed.dailyDateKey === today;

    const merged: PersistedGameState = {
      ...INITIAL_STATE,
      ...parsed,
      runCrystalsEarned: parsed.runCrystalsEarned ?? parsed.totalCrystalsEarned ?? 0,
      upgradeLevels: parsed.upgradeLevels ?? {},
      talentLevels: parsed.talentLevels ?? {},
      claimedAchievements: Array.isArray(parsed.claimedAchievements)
        ? parsed.claimedAchievements
        : [],
      dailyDateKey: today,
      dailyClicks: sameDay ? parsed.dailyClicks ?? 0 : 0,
      dailyUpgrades: sameDay ? parsed.dailyUpgrades ?? 0 : 0,
      dailyFever: sameDay ? parsed.dailyFever ?? 0 : 0,
      dailyClaimedIds: sameDay && Array.isArray(parsed.dailyClaimedIds)
        ? parsed.dailyClaimedIds
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
      const prestigeMult = 1 + (merged.darkMatter + merged.spentDarkMatter) * 0.5;
      const droneMult = 1 + (merged.talentLevels.drone_overclock ?? 0) * 0.25;
      const offlineVaultMult = 1 + (merged.talentLevels.cryo_vault ?? 0) * 0.35;
      const midnightMult = isMidnightHours() ? 1.2 : 1;
      const passiveBonus = UPGRADES.filter((u) => u.type === 'PASSIVE_INCOME').reduce(
        (sum, u) => sum + (merged.upgradeLevels[u.id] ?? 0) * u.baseGain,
        0
      );
      const passivePerSec =
        passiveBonus * curr.multiplier * prestigeMult * droneMult * midnightMult * offlineVaultMult;

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

type ActiveTab = 'UPGRADES' | 'FLEET' | 'SKINS' | 'ACHIEVEMENTS' | 'PRESTIGE';

export default function App() {
  const initialData = useMemo(() => loadInitialData(), []);
  const [game, setGame] = useState<PersistedGameState>(initialData.state);
  const [offlineReward, setOfflineReward] = useState<OfflineReward | null>(initialData.offline);
  const [activeTab, setActiveTab] = useState<ActiveTab>('UPGRADES');
  const [buyQuantity, setBuyQuantity] = useState<BuyQuantity>('X1');
  const [showSettings, setShowSettings] = useState(false);
  const [showStats, setShowStats] = useState(false);
  const [showAndroidCode, setShowAndroidCode] = useState(false);

  const [overchargeSecondsLeft, setOverchargeSecondsLeft] = useState(0);
  const [isCometVisible, setIsCometVisible] = useState(false);
  const [cometSeed, setCometSeed] = useState(1);

  // Fever / Combo mode
  const [feverMeter, setFeverMeter] = useState(0);
  const [feverSecondsLeft, setFeverSecondsLeft] = useState(0);
  const [comboCount, setComboCount] = useState(0);
  const lastTapMsRef = useRef<number>(0);

  // CPS & Midnight Bonus & Secret Easter Egg
  const recentTapsRef = useRef<number[]>([]);
  const [currentCps, setCurrentCps] = useState(0);
  const [midnightBonus, setMidnightBonus] = useState(() => isMidnightHours());
  const headerTapCountRef = useRef(0);
  const [secretToast, setSecretToast] = useState<string | null>(null);

  // Boss Battle state
  const [bossState, setBossState] = useState<BossUiState>({
    isActive: false,
    name: 'Бронированный Левиафан',
    currentHp: 0,
    maxHp: 1,
    remainingSeconds: 0,
    cooldownSeconds: 0,
    lastResultMessage: null,
  });
  const bossAutoTimerRef = useRef<number>(BOSS_AUTO_SPAWN_SEC);

  // Expeditions countdown UI state
  const [expeditionLastMessage, setExpeditionLastMessage] = useState<string | null>(null);
  const [nowMs, setNowMs] = useState(() => Date.now());

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

  const prestigeMultiplier = useMemo(
    () => 1 + (game.darkMatter + game.spentDarkMatter) * 0.5,
    [game.darkMatter, game.spentDarkMatter]
  );

  const discountFraction = useMemo(
    () => (game.talentLevels.stellar_discount ?? 0) * 0.05,
    [game.talentLevels]
  );

  const midnightMult = midnightBonus ? 1.2 : 1;

  const baseClickPower = useMemo(() => {
    const bonus = UPGRADES.filter((u) => u.type === 'CLICK_POWER').reduce(
      (sum, u) => sum + (game.upgradeLevels[u.id] ?? 0) * u.baseGain,
      0
    );
    return (1 + bonus) * currentRank.multiplier * prestigeMultiplier * midnightMult;
  }, [game.upgradeLevels, currentRank.multiplier, prestigeMultiplier, midnightMult]);

  const clickPower = useMemo(
    () => baseClickPower * (overchargeSecondsLeft > 0 ? OVERCHARGE_MULTIPLIER : 1),
    [baseClickPower, overchargeSecondsLeft]
  );

  const passivePerSecond = useMemo(() => {
    const droneMult = 1 + (game.talentLevels.drone_overclock ?? 0) * 0.25;
    const bonus = UPGRADES.filter((u) => u.type === 'PASSIVE_INCOME').reduce(
      (sum, u) => sum + (game.upgradeLevels[u.id] ?? 0) * u.baseGain,
      0
    );
    return bonus * currentRank.multiplier * prestigeMultiplier * droneMult * midnightMult;
  }, [game.upgradeLevels, game.talentLevels, currentRank.multiplier, prestigeMultiplier, midnightMult]);

  const visualDroneCount = useMemo(() => {
    const passiveSum = UPGRADES.filter((u) => u.type === 'PASSIVE_INCOME').reduce(
      (sum, u) => sum + (game.upgradeLevels[u.id] ?? 0),
      0
    );
    return passiveSum <= 0 ? 0 : Math.min(10, 1 + Math.floor(passiveSum / 3));
  }, [game.upgradeLevels]);

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

  const achievementStatus = useMemo(() => {
    const unlockedIds: string[] = [];
    let unclaimed = 0;

    ACHIEVEMENTS.forEach((ach) => {
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
        case 'BOSSES_DEFEATED':
          val = game.bossesDefeated;
          break;
        case 'FEVER_COUNT':
          val = game.feverActivations;
          break;
        case 'SECRET_HEADER_TAP':
          val = game.secretHeaderUnlocked ? 1 : 0;
          break;
      }
      if (val >= ach.targetValue) {
        unlockedIds.push(ach.id);
        if (!game.claimedAchievements.includes(ach.id)) {
          unclaimed++;
        }
      }
    });

    return { unlockedIds, unclaimed };
  }, [
    game.claimedAchievements,
    game.totalClicks,
    game.totalCrystalsEarned,
    game.criticalHits,
    game.cometsCaught,
    game.prestigeCount,
    game.bossesDefeated,
    game.feverActivations,
    game.secretHeaderUnlocked,
    totalUpgradesBought,
    passivePerSecond,
  ]);

  const unclaimedQuestsCount = useMemo(() => {
    return DAILY_QUESTS.filter((q) => {
      if (game.dailyClaimedIds.includes(q.id)) return false;
      const val =
        q.type === 'CLICKS'
          ? game.dailyClicks
          : q.type === 'UPGRADES'
          ? game.dailyUpgrades
          : game.dailyFever;
      return val >= q.target;
    }).length;
  }, [game.dailyClicks, game.dailyUpgrades, game.dailyFever, game.dailyClaimedIds]);

  const expeditionRemainingSec = game.activeExpeditionId
    ? Math.max(0, Math.floor((game.expeditionEndMs - nowMs) / 1000))
    : 0;

  const baseClickRef = useRef(baseClickPower);
  baseClickRef.current = baseClickPower;
  const passiveRef = useRef(passivePerSecond);
  passiveRef.current = passivePerSecond;

  const spawnBossInternal = useCallback(() => {
    const names = [
      'Бронированный Левиафан',
      'Кварцевый Дредноут',
      'Титан Пояса Койпера',
      'Осколок Сверхновой',
    ];
    const name = names[Math.floor(Math.random() * names.length)];
    const expectedDps = baseClickRef.current * 6.5 + passiveRef.current;
    const maxHp = Math.max(600, expectedDps * 18);
    setBossState({
      isActive: true,
      name,
      currentHp: maxHp,
      maxHp,
      remainingSeconds: BOSS_DURATION_SEC,
      cooldownSeconds: 0,
      lastResultMessage: null,
    });
  }, []);

  const damageBoss = useCallback((damage: number) => {
    if (damage <= 0) return;
    setBossState((prev) => {
      if (!prev.isActive) return prev;
      const nextHp = Math.max(0, prev.currentHp - damage);
      if (nextHp <= 0) {
        const chest = Math.max(prev.maxHp * 2.2, passiveRef.current * 45, 500);
        const firstBoss = gameRef.current.bossesDefeated === 0;
        const dmBonus = firstBoss || Math.random() < 0.45 ? 1 : 0;

        if (gameRef.current.isSoundEnabled) {
          playBossDefeatedSound();
        }
        setGame((g) => ({
          ...g,
          crystals: g.crystals + chest,
          runCrystalsEarned: g.runCrystalsEarned + chest,
          totalCrystalsEarned: g.totalCrystalsEarned + chest,
          experience: g.experience + chest,
          darkMatter: g.darkMatter + dmBonus,
          bossesDefeated: g.bossesDefeated + 1,
        }));
        bossAutoTimerRef.current = BOSS_AUTO_SPAWN_SEC;

        return {
          ...prev,
          isActive: false,
          currentHp: 0,
          remainingSeconds: 0,
          cooldownSeconds: BOSS_MANUAL_COOLDOWN_SEC,
          lastResultMessage:
            dmBonus > 0
              ? `Победа! Сундук: +${formatCrystalCount(chest)} крист. и +${dmBonus} ТМ`
              : `Победа! Сундук: +${formatCrystalCount(chest)} крист.`,
        };
      }
      return { ...prev, currentHp: nextHp };
    });
  }, []);

  // Smooth passive income & Fever heat decay (10 ticks / sec)
  const feverSecondsRef = useRef(feverSecondsLeft);
  feverSecondsRef.current = feverSecondsLeft;

  useEffect(() => {
    const interval = window.setInterval(() => {
      const now = Date.now();
      // CPS prune
      recentTapsRef.current = recentTapsRef.current.filter((ts) => now - ts <= 1000);
      setCurrentCps(recentTapsRef.current.length);

      // Passive gain
      const pps = passiveRef.current;
      if (pps > 0) {
        const delta = pps / 10;
        setGame((prev) => ({
          ...prev,
          crystals: prev.crystals + delta,
          runCrystalsEarned: prev.runCrystalsEarned + delta,
          totalCrystalsEarned: prev.totalCrystalsEarned + delta,
          experience: prev.experience + delta,
          lastActiveTimestampMs: now,
        }));
        damageBoss(delta);
      }

      // Fever heat decay if inactive
      if (feverSecondsRef.current <= 0 && now - lastTapMsRef.current > 1600) {
        setFeverMeter((m) => {
          const next = Math.max(0, m - 0.045);
          if (next === 0) setComboCount(0);
          return next;
        });
      }
    }, 100);

    return () => window.clearInterval(interval);
  }, [damageBoss]);

  // 1-second timer: Overcharge, Fever, Boss countdown/autospawn, PlayTime, Daily reset, Midnight check
  useEffect(() => {
    const timer = window.setInterval(() => {
      setNowMs(Date.now());
      setMidnightBonus(isMidnightHours());

      setOverchargeSecondsLeft((s) => Math.max(0, s - 1));
      setFeverSecondsLeft((s) => {
        if (s <= 0) return 0;
        const next = s - 1;
        const dur = FEVER_DURATION_SEC + (gameRef.current.talentLevels.fever_surge ?? 0);
        setFeverMeter(next > 0 ? next / dur : 0);
        if (next === 0) setComboCount(0);
        return next;
      });

      setBossState((prev) => {
        if (prev.isActive) {
          if (prev.remainingSeconds <= 1) {
            bossAutoTimerRef.current = BOSS_AUTO_SPAWN_SEC;
            return {
              ...prev,
              isActive: false,
              remainingSeconds: 0,
              cooldownSeconds: BOSS_MANUAL_COOLDOWN_SEC,
              lastResultMessage: 'Босс улетел в гиперпространство',
            };
          }
          return { ...prev, remainingSeconds: prev.remainingSeconds - 1 };
        }
        return {
          ...prev,
          cooldownSeconds: Math.max(0, prev.cooldownSeconds - 1),
        };
      });

      bossAutoTimerRef.current -= 1;
      if (bossAutoTimerRef.current <= 0) {
        bossAutoTimerRef.current = BOSS_AUTO_SPAWN_SEC;
        spawnBossInternal();
      }

      setGame((prev) => {
        const today = todayDateKey();
        if (prev.dailyDateKey !== today) {
          return {
            ...prev,
            playTimeSeconds: prev.playTimeSeconds + 1,
            dailyDateKey: today,
            dailyClicks: 0,
            dailyUpgrades: 0,
            dailyFever: 0,
            dailyClaimedIds: [],
          };
        }
        return { ...prev, playTimeSeconds: prev.playTimeSeconds + 1 };
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [spawnBossInternal]);

  // Random cosmic comet event (accelerated by comet_magnet talent)
  const cometMagnetLevel = game.talentLevels.comet_magnet ?? 0;
  useEffect(() => {
    if (isCometVisible) {
      const hideTimer = window.setTimeout(() => {
        setIsCometVisible(false);
      }, 6300);
      return () => window.clearTimeout(hideTimer);
    } else {
      const reduction = cometMagnetLevel * 6500;
      const minMs = Math.max(18000, 38000 - reduction);
      const spanMs = Math.max(10000, 18000 - reduction * 0.4);
      const delayMs = minMs + Math.floor(Math.random() * spanMs);
      const spawnTimer = window.setTimeout(() => {
        setCometSeed(Date.now());
        setIsCometVisible(true);
      }, delayMs);
      return () => window.clearTimeout(spawnTimer);
    }
  }, [isCometVisible, cometMagnetLevel]);

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
    const now = Date.now();
    lastTapMsRef.current = now;
    recentTapsRef.current = [...recentTapsRef.current.filter((t) => now - t <= 1000), now];
    const cps = recentTapsRef.current.length;
    setCurrentCps(cps);
    setComboCount((c) => c + 1);

    const g = gameRef.current;
    let feverActiveNow = feverSecondsRef.current > 0;
    let triggeredFever = false;

    if (!feverActiveNow) {
      const feverSurgeLvl = g.talentLevels.fever_surge ?? 0;
      const heatStep = 0.045 * (1 + feverSurgeLvl * 0.25);
      setFeverMeter((prev) => {
        const next = Math.min(1, prev + heatStep);
        if (next >= 1) {
          triggeredFever = true;
          const dur = FEVER_DURATION_SEC + feverSurgeLvl;
          setFeverSecondsLeft(dur);
          if (g.isSoundEnabled) {
            playFeverStartSound();
          }
        }
        return next;
      });
    }

    if (triggeredFever) feverActiveNow = true;

    const critChance = 0.1 + (g.talentLevels.crit_edge ?? 0) * 0.03;
    const isCritical = feverActiveNow || Math.random() < critChance;
    const gained = isCritical ? clickPower * 3 : clickPower;

    if (g.isSoundEnabled) {
      playCrystalTapSound(isCritical, feverActiveNow);
    }

    damageBoss(gained);

    setGame((prev) => ({
      ...prev,
      crystals: prev.crystals + gained,
      runCrystalsEarned: prev.runCrystalsEarned + gained,
      totalCrystalsEarned: prev.totalCrystalsEarned + gained,
      experience: prev.experience + gained,
      totalClicks: prev.totalClicks + 1,
      dailyClicks: prev.dailyClicks + 1,
      bestCps: Math.max(prev.bestCps, cps),
      criticalHits: prev.criticalHits + (isCritical ? 1 : 0),
      feverActivations: prev.feverActivations + (triggeredFever ? 1 : 0),
      dailyFever: prev.dailyFever + (triggeredFever ? 1 : 0),
      lastActiveTimestampMs: now,
    }));

    return { gained, isCritical, isFever: feverActiveNow };
  }, [clickPower, damageBoss]);

  const handleCatchComet = useCallback(() => {
    if (!isCometVisible) return 0;
    setIsCometVisible(false);
    setOverchargeSecondsLeft(OVERCHARGE_DURATION_SEC);

    const cometTalentMult = 1 + (gameRef.current.talentLevels.comet_magnet ?? 0) * 0.5;
    const bonus = Math.max(baseClickPower * 25, passivePerSecond * 20, 100) * cometTalentMult;

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

  const handleBuyUpgrade = useCallback(
    (upgradeId: string) => {
      const def = UPGRADES.find((u) => u.id === upgradeId);
      if (!def) return;

      setGame((prev) => {
        const level = prev.upgradeLevels[upgradeId] ?? 0;
        const discount = (prev.talentLevels.stellar_discount ?? 0) * 0.05;
        const plan = calculateUpgradePurchase(def, level, prev.crystals, discount, buyQuantity);
        if (!plan.canAfford || plan.levelsToBuy <= 0) return prev;

        if (prev.isSoundEnabled) {
          playUpgradeSound();
        }

        return {
          ...prev,
          crystals: prev.crystals - plan.totalCost,
          dailyUpgrades: prev.dailyUpgrades + plan.levelsToBuy,
          upgradeLevels: {
            ...prev.upgradeLevels,
            [upgradeId]: Math.min(MAX_UPGRADE_LEVEL, level + plan.levelsToBuy),
          },
          lastActiveTimestampMs: Date.now(),
        };
      });
    },
    [buyQuantity]
  );

  const handleBuyTalent = useCallback((talentId: string) => {
    const def = TALENTS.find((t) => t.id === talentId);
    if (!def) return;

    setGame((prev) => {
      const level = prev.talentLevels[talentId] ?? 0;
      if (level >= def.maxLevel) return prev;
      const cost = getTalentCost(def, level);
      if (prev.darkMatter < cost) return prev;

      if (prev.isSoundEnabled) playUpgradeSound();
      return {
        ...prev,
        darkMatter: prev.darkMatter - cost,
        spentDarkMatter: prev.spentDarkMatter + cost,
        talentLevels: {
          ...prev.talentLevels,
          [talentId]: level + 1,
        },
        lastActiveTimestampMs: Date.now(),
      };
    });
  }, []);

  const handleStartExpedition = useCallback((presetId: string) => {
    const preset = EXPEDITIONS.find((e) => e.id === presetId);
    if (!preset) return;
    setExpeditionLastMessage(null);
    setGame((prev) => {
      if (prev.activeExpeditionId) return prev;
      if (prev.isSoundEnabled) playUpgradeSound();
      return {
        ...prev,
        activeExpeditionId: preset.id,
        expeditionEndMs: Date.now() + preset.durationSeconds * 1000,
      };
    });
  }, []);

  const handleClaimExpedition = useCallback(() => {
    const activeId = gameRef.current.activeExpeditionId;
    if (!activeId) return;
    const preset = EXPEDITIONS.find((e) => e.id === activeId);
    if (!preset || Date.now() < gameRef.current.expeditionEndMs) return;

    const rewardCrystals = Math.max(
      passiveRef.current * preset.crystalSecondsEquivalent,
      baseClickRef.current * 35,
      250
    );
    const dmFound = Math.random() < preset.darkMatterChance ? 1 : 0;

    if (preset.id === 'deep_sector_30') {
      setOverchargeSecondsLeft((s) => Math.max(s, 15));
    }
    if (gameRef.current.isSoundEnabled) playCometSound();

    setExpeditionLastMessage(
      dmFound > 0
        ? `Добыто +${formatCrystalCount(rewardCrystals)} крист. и +${dmFound} ТМ!`
        : `Флот вернулся: +${formatCrystalCount(rewardCrystals)} крист.`
    );

    setGame((prev) => ({
      ...prev,
      crystals: prev.crystals + rewardCrystals,
      runCrystalsEarned: prev.runCrystalsEarned + rewardCrystals,
      totalCrystalsEarned: prev.totalCrystalsEarned + rewardCrystals,
      experience: prev.experience + rewardCrystals,
      darkMatter: prev.darkMatter + dmFound,
      activeExpeditionId: null,
      expeditionEndMs: 0,
      lastActiveTimestampMs: Date.now(),
    }));
  }, []);

  const handleClaimDailyQuest = useCallback((questId: string) => {
    const def = DAILY_QUESTS.find((q) => q.id === questId);
    if (!def) return;

    setGame((prev) => {
      if (prev.dailyClaimedIds.includes(questId)) return prev;
      const progress =
        def.type === 'CLICKS'
          ? prev.dailyClicks
          : def.type === 'UPGRADES'
          ? prev.dailyUpgrades
          : prev.dailyFever;
      if (progress < def.target) return prev;

      const bonusCrystals = Math.max(
        passiveRef.current * def.rewardCrystalsFactor,
        baseClickRef.current * 20,
        200
      );
      if (prev.isSoundEnabled) playUpgradeSound();
      return {
        ...prev,
        crystals: prev.crystals + bonusCrystals,
        runCrystalsEarned: prev.runCrystalsEarned + bonusCrystals,
        totalCrystalsEarned: prev.totalCrystalsEarned + bonusCrystals,
        experience: prev.experience + bonusCrystals,
        darkMatter: prev.darkMatter + def.rewardDarkMatter,
        dailyClaimedIds: [...prev.dailyClaimedIds, questId],
        lastActiveTimestampMs: Date.now(),
      };
    });
  }, []);

  const handleSelectSkin = useCallback(
    (skinId: CrystalSkinId) => {
      if (gameRef.current.isSoundEnabled) playUpgradeSound();
      setGame((prev) => ({ ...prev, selectedSkinId: skinId }));
    },
    []
  );

  const handleSelectLocation = useCallback(
    (locationId: SpaceLocationId) => {
      if (gameRef.current.isSoundEnabled) playUpgradeSound();
      setGame((prev) => ({ ...prev, selectedLocationId: locationId }));
    },
    []
  );

  const handleSecretHeaderTap = useCallback(() => {
    if (gameRef.current.secretHeaderUnlocked) return;
    headerTapCountRef.current += 1;
    if (headerTapCountRef.current >= 7) {
      if (gameRef.current.isSoundEnabled) playCometSound();
      setSecretToast('Секретное достижение «Тайна заголовка» открыто!');
      setGame((prev) => ({ ...prev, secretHeaderUnlocked: true }));
    }
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
    setFeverSecondsLeft(0);
    setFeverMeter(0);
    setComboCount(0);
    setBossState((b) => ({ ...b, isActive: false }));
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
    setFeverSecondsLeft(0);
    setFeverMeter(0);
    setComboCount(0);
    setOfflineReward(null);
    setExpeditionLastMessage(null);
    setBossState((b) => ({ ...b, isActive: false, lastResultMessage: null, cooldownSeconds: 0 }));
    headerTapCountRef.current = 0;

    setGame((prev) => {
      const resetState: PersistedGameState = {
        ...INITIAL_STATE,
        dailyDateKey: todayDateKey(),
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
      bossesDefeated: g.bossesDefeated,
      feverActivations: g.feverActivations,
      bestCps: g.bestCps,
      playTimeSeconds: g.playTimeSeconds,
      darkMatter: g.darkMatter,
      spentDarkMatter: g.spentDarkMatter,
      prestigeCount: g.prestigeCount,
      secretHeaderUnlocked: g.secretHeaderUnlocked,
      selectedSkinId: g.selectedSkinId,
      selectedLocationId: g.selectedLocationId,
      activeExpeditionId: g.activeExpeditionId,
      expeditionEndMs: g.expeditionEndMs,
      dailyDateKey: g.dailyDateKey,
      dailyClicks: g.dailyClicks,
      dailyUpgrades: g.dailyUpgrades,
      dailyFever: g.dailyFever,
      dailyClaimedIds: g.dailyClaimedIds,
      isVibrationEnabled: g.isVibrationEnabled,
      isSoundEnabled: g.isSoundEnabled,
      upgrades: g.upgradeLevels,
      talents: g.talentLevels,
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
      const parsed = JSON.parse(new TextDecoder().decode(bytes)) as Record<string, unknown>;
      const rawUpgrades = (parsed.upgrades as Record<string, number>) ?? {};
      const clampedUpgrades: Record<string, number> = {};
      Object.entries(rawUpgrades).forEach(([id, lvl]) => {
        clampedUpgrades[id] = Math.min(MAX_UPGRADE_LEVEL, Math.max(0, Math.floor(Number(lvl) || 0)));
      });
      const rawTalents = (parsed.talents as Record<string, number>) ?? {};
      const clampedTalents: Record<string, number> = {};
      TALENTS.forEach((t) => {
        const val = Math.floor(Number(rawTalents[t.id] ?? 0));
        if (val > 0) clampedTalents[t.id] = Math.min(t.maxLevel, val);
      });

      const restored: PersistedGameState = {
        ...INITIAL_STATE,
        crystals: Math.max(0, Number(parsed.crystals ?? 0)),
        runCrystalsEarned: Math.max(0, Number(parsed.runCrystalsEarned ?? 0)),
        totalCrystalsEarned: Math.max(0, Number(parsed.totalCrystalsEarned ?? 0)),
        experience: Math.max(0, Number(parsed.experience ?? 0)),
        totalClicks: Math.max(0, Number(parsed.totalClicks ?? 0)),
        criticalHits: Math.max(0, Number(parsed.criticalHits ?? 0)),
        cometsCaught: Math.max(0, Number(parsed.cometsCaught ?? 0)),
        bossesDefeated: Math.max(0, Number(parsed.bossesDefeated ?? 0)),
        feverActivations: Math.max(0, Number(parsed.feverActivations ?? 0)),
        bestCps: Math.max(0, Number(parsed.bestCps ?? 0)),
        playTimeSeconds: Math.max(0, Number(parsed.playTimeSeconds ?? 0)),
        darkMatter: Math.max(0, Number(parsed.darkMatter ?? 0)),
        spentDarkMatter: Math.max(0, Number(parsed.spentDarkMatter ?? 0)),
        prestigeCount: Math.max(0, Number(parsed.prestigeCount ?? 0)),
        secretHeaderUnlocked: Boolean(parsed.secretHeaderUnlocked ?? false),
        selectedSkinId: (parsed.selectedSkinId as CrystalSkinId) ?? 'classic',
        selectedLocationId: (parsed.selectedLocationId as SpaceLocationId) ?? 'nebula',
        activeExpeditionId: (parsed.activeExpeditionId as string | null) ?? null,
        expeditionEndMs: Math.max(0, Number(parsed.expeditionEndMs ?? 0)),
        dailyDateKey: (parsed.dailyDateKey as string) || todayDateKey(),
        dailyClicks: Math.max(0, Number(parsed.dailyClicks ?? 0)),
        dailyUpgrades: Math.max(0, Number(parsed.dailyUpgrades ?? 0)),
        dailyFever: Math.max(0, Number(parsed.dailyFever ?? 0)),
        dailyClaimedIds: Array.isArray(parsed.dailyClaimedIds) ? (parsed.dailyClaimedIds as string[]) : [],
        isVibrationEnabled: (parsed.isVibrationEnabled as boolean) ?? true,
        isSoundEnabled: (parsed.isSoundEnabled as boolean) ?? true,
        upgradeLevels: clampedUpgrades,
        talentLevels: clampedTalents,
        claimedAchievements: Array.isArray(parsed.achievements) ? (parsed.achievements as string[]) : [],
        lastActiveTimestampMs: Date.now(),
      };
      setGame(restored);
      setOfflineReward(null);
      setOverchargeSecondsLeft(0);
      setFeverSecondsLeft(0);
      setFeverMeter(0);
      setBossState((b) => ({ ...b, isActive: false }));
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
            onClick={() => setShowStats(true)}
            aria-label="Статистика"
            className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl text-cyan-300 hover:bg-slate-800/70 cursor-pointer transition-colors"
          >
            <BarChart3 className="w-5 h-5" />
          </button>

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
            <div className="flex flex-wrap items-center gap-2.5">
              <button
                type="button"
                onClick={handleSecretHeaderTap}
                className="text-xs font-medium text-slate-400 hover:text-slate-300 cursor-default select-none"
              >
                Кристаллы
              </button>
              {game.darkMatter > 0 && (
                <span className="text-xs font-mono-num font-bold text-violet-300">
                  {game.darkMatter} ТМ (x{formatRate(prestigeMultiplier)})
                </span>
              )}
              {midnightBonus && (
                <span className="px-2 py-0.5 rounded-md bg-amber-400/15 text-amber-300 text-[10px] font-bold">
                  Полуночный +20%
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
                Уровень {currentRank.level} / 200 · {currentRank.title}
              </div>
              <div className="mt-0.5 font-mono-num text-[11px] text-slate-400">
                {nextRank
                  ? `Множитель x${currentRank.multiplier} · ${formatCrystalCount(game.experience)} / ${formatCrystalCount(nextRank.xpThreshold)}`
                  : `Макс. уровень · Множитель x${currentRank.multiplier}`}
              </div>
              <div className="mt-2 h-2 w-full rounded-full bg-slate-800/90 overflow-hidden">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-violet-500 via-cyan-400 to-amber-400 transition-all duration-200"
                  style={{ width: `${Math.round(rankProgress * 100)}%` }}
                />
              </div>
            </div>
          </div>

          <div className="my-auto py-2">
            <CrystalCanvas
              rankLevel={currentRank.level}
              overchargeSecondsLeft={overchargeSecondsLeft}
              feverMeter={feverMeter}
              feverSecondsLeft={feverSecondsLeft}
              comboCount={comboCount}
              isCometVisible={isCometVisible}
              cometSeed={cometSeed}
              visualDroneCount={visualDroneCount}
              selectedSkinId={game.selectedSkinId}
              selectedLocationId={game.selectedLocationId}
              bossState={bossState}
              onTapCrystal={handleTapCrystal}
              onCatchComet={handleCatchComet}
              isVibrationEnabled={game.isVibrationEnabled}
            />
          </div>
        </section>

        {/* Right / Bottom Zone: 5 Tabs */}
        <section className="lg:col-span-6 flex flex-col rounded-3xl bg-[#0B0F23]/95 border border-slate-800/90 overflow-hidden min-h-[480px] lg:max-h-[700px]">
          <div className="flex overflow-x-auto p-1.5 gap-1 bg-[#0E142D] border-b border-slate-800/80 shrink-0">
            <button
              type="button"
              onClick={() => setActiveTab('UPGRADES')}
              className={`min-h-[42px] px-3 flex-1 rounded-2xl text-xs font-semibold whitespace-nowrap cursor-pointer transition-colors ${
                activeTab === 'UPGRADES'
                  ? 'bg-[#1A2246] text-cyan-300 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Улучшения
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('FLEET')}
              className={`min-h-[42px] px-3 flex-1 rounded-2xl text-xs font-semibold whitespace-nowrap cursor-pointer transition-colors flex items-center justify-center gap-1 ${
                activeTab === 'FLEET'
                  ? 'bg-[#1A2246] text-cyan-300 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <span>Флот</span>
              {unclaimedQuestsCount > 0 && (
                <span className="font-mono-num text-[11px] font-bold text-cyan-300">
                  ({unclaimedQuestsCount})
                </span>
              )}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('SKINS')}
              className={`min-h-[42px] px-3 flex-1 rounded-2xl text-xs font-semibold whitespace-nowrap cursor-pointer transition-colors ${
                activeTab === 'SKINS'
                  ? 'bg-[#1A2246] text-violet-300 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Скины
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('ACHIEVEMENTS')}
              className={`min-h-[42px] px-3 flex-1 rounded-2xl text-xs font-semibold whitespace-nowrap cursor-pointer transition-colors flex items-center justify-center gap-1 ${
                activeTab === 'ACHIEVEMENTS'
                  ? 'bg-[#1A2246] text-amber-300 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <span>Ачивки</span>
              {achievementStatus.unclaimed > 0 && (
                <span className="font-mono-num text-[11px] font-bold text-amber-300">
                  ({achievementStatus.unclaimed})
                </span>
              )}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('PRESTIGE')}
              className={`min-h-[42px] px-3 flex-1 rounded-2xl text-xs font-semibold whitespace-nowrap cursor-pointer transition-colors flex items-center justify-center gap-1 ${
                activeTab === 'PRESTIGE'
                  ? 'bg-[#1A2246] text-violet-300 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <span>Коллапс</span>
              {potentialDarkMatter > 0 && (
                <span className="font-mono-num text-[11px] font-bold text-amber-300">
                  (+{potentialDarkMatter})
                </span>
              )}
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-4">
            {activeTab === 'UPGRADES' && (
              <UpgradesPanel
                crystals={game.crystals}
                upgradeLevels={game.upgradeLevels}
                discountFraction={discountFraction}
                buyQuantity={buyQuantity}
                onBuyQuantityChange={setBuyQuantity}
                onBuyUpgrade={handleBuyUpgrade}
              />
            )}
            {activeTab === 'FLEET' && (
              <FleetPanel
                bossState={bossState}
                activeExpeditionId={game.activeExpeditionId}
                expeditionRemainingSec={expeditionRemainingSec}
                expeditionLastMessage={expeditionLastMessage}
                dailyClicks={game.dailyClicks}
                dailyUpgrades={game.dailyUpgrades}
                dailyFever={game.dailyFever}
                dailyClaimedIds={game.dailyClaimedIds}
                onSummonBoss={() => {
                  if (!bossState.isActive && bossState.cooldownSeconds === 0) {
                    spawnBossInternal();
                  }
                }}
                onStartExpedition={handleStartExpedition}
                onClaimExpedition={handleClaimExpedition}
                onClaimDailyQuest={handleClaimDailyQuest}
              />
            )}
            {activeTab === 'SKINS' && (
              <SkinsPanel
                selectedSkinId={game.selectedSkinId}
                selectedLocationId={game.selectedLocationId}
                unlockedAchievementIds={achievementStatus.unlockedIds}
                onSelectSkin={handleSelectSkin}
                onSelectLocation={handleSelectLocation}
              />
            )}
            {activeTab === 'ACHIEVEMENTS' && (
              <AchievementsPanel
                totalClicks={game.totalClicks}
                totalCrystalsEarned={game.totalCrystalsEarned}
                totalUpgradesBought={totalUpgradesBought}
                passivePerSecond={passivePerSecond}
                criticalHits={game.criticalHits}
                cometsCaught={game.cometsCaught}
                prestigeCount={game.prestigeCount}
                bossesDefeated={game.bossesDefeated}
                feverActivations={game.feverActivations}
                secretHeaderUnlocked={game.secretHeaderUnlocked}
                claimedAchievements={game.claimedAchievements}
                onClaimAchievement={handleClaimAchievement}
              />
            )}
            {activeTab === 'PRESTIGE' && (
              <PrestigePanel
                darkMatter={game.darkMatter}
                prestigeCount={game.prestigeCount}
                prestigeMultiplier={prestigeMultiplier}
                potentialDarkMatter={potentialDarkMatter}
                runCrystalsEarned={game.runCrystalsEarned}
                nextDarkMatterThreshold={nextDmThreshold}
                talentLevels={game.talentLevels}
                onBuyTalent={handleBuyTalent}
                onPerformCollapse={handlePerformCollapse}
              />
            )}
          </div>
        </section>
      </main>

      {secretToast && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4"
          role="dialog"
          aria-modal="true"
        >
          <div className="w-full max-w-sm rounded-3xl bg-[#111732] border border-amber-400/45 p-6 shadow-2xl space-y-4">
            <h2 className="font-display text-xl font-bold text-amber-300">Пасхалка найдена!</h2>
            <p className="text-sm text-slate-300 leading-relaxed">
              {secretToast} Заберите награду во вкладке «Ачивки».
            </p>
            <button
              type="button"
              onClick={() => setSecretToast(null)}
              className="w-full min-h-[42px] rounded-xl bg-amber-400 hover:bg-amber-300 text-slate-950 font-bold text-sm cursor-pointer transition-colors"
            >
              Круто!
            </button>
          </div>
        </div>
      )}

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

      {showStats && (
        <StatsModal
          totalClicks={game.totalClicks}
          bestCps={game.bestCps}
          currentCps={currentCps}
          criticalHits={game.criticalHits}
          cometsCaught={game.cometsCaught}
          bossesDefeated={game.bossesDefeated}
          feverActivations={game.feverActivations}
          playTimeSeconds={game.playTimeSeconds}
          totalCrystalsEarned={game.totalCrystalsEarned}
          darkMatter={game.darkMatter}
          prestigeCount={game.prestigeCount}
          onClose={() => setShowStats(false)}
        />
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
          onOpenStats={() => setShowStats(true)}
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
