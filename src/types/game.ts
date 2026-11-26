export type UpgradeType = 'CLICK_POWER' | 'PASSIVE_INCOME';

export interface UpgradeDefinition {
  id: string;
  title: string;
  description: string;
  type: UpgradeType;
  baseCost: number;
  baseGain: number;
  costMultiplier: number;
}

export interface MinerRank {
  level: number;
  title: string;
  xpThreshold: number;
  multiplier: number;
}

export type AchievementMetric =
  | 'TOTAL_CLICKS'
  | 'TOTAL_CRYSTALS_EARNED'
  | 'TOTAL_UPGRADES_BOUGHT'
  | 'PASSIVE_PER_SEC'
  | 'CRITICAL_HITS'
  | 'COMETS_CAUGHT'
  | 'PRESTIGE_COUNT';

export interface AchievementDefinition {
  id: string;
  title: string;
  description: string;
  metric: AchievementMetric;
  targetValue: number;
  rewardCrystals: number;
}

export interface FloatingNumber {
  id: number;
  text: string;
  x: number;
  y: number;
  isCritical: boolean;
  isComet?: boolean;
}

export interface OfflineReward {
  elapsedSeconds: number;
  crystalsEarned: number;
}

export interface PersistedGameState {
  crystals: number;
  runCrystalsEarned: number;
  totalCrystalsEarned: number;
  experience: number;
  totalClicks: number;
  criticalHits: number;
  cometsCaught: number;
  darkMatter: number;
  prestigeCount: number;
  upgradeLevels: Record<string, number>;
  claimedAchievements: string[];
  isVibrationEnabled: boolean;
  isSoundEnabled: boolean;
  lastActiveTimestampMs: number;
}

export const MAX_PLAYER_LEVEL = 200;
export const MAX_UPGRADE_LEVEL = 200;
export const PRESTIGE_BASE_THRESHOLD = 100_000;
export const OVERCHARGE_DURATION_SEC = 10;
export const OVERCHARGE_MULTIPLIER = 5;
export const MAX_OFFLINE_SECONDS = 12 * 3600;

export function calculatePotentialDarkMatter(runCrystalsEarned: number): number {
  if (runCrystalsEarned < PRESTIGE_BASE_THRESHOLD) return 0;
  return Math.floor(Math.sqrt(runCrystalsEarned / PRESTIGE_BASE_THRESHOLD));
}

export function nextDarkMatterRequirement(currentPotential: number): number {
  const next = currentPotential + 1;
  return next * next * PRESTIGE_BASE_THRESHOLD;
}

function titleForLevel(level: number): string {
  if (level <= 15) return 'Новичок';
  if (level <= 30) return 'Стажёр орбиты';
  if (level <= 49) return 'Шахтёр';
  if (level <= 69) return 'Рейнджер пояса';
  if (level <= 89) return 'Мастер';
  if (level <= 109) return 'Звёздный магнат';
  if (level <= 129) return 'Космический барон';
  if (level <= 149) return 'Повелитель сингулярности';
  if (level <= 169) return 'Хранитель туманности';
  if (level <= 184) return 'Архитектор галактик';
  if (level <= 199) return 'Покоритель пустоты';
  return 'Бог кристаллов';
}

export const RANKS: MinerRank[] = Array.from({ length: MAX_PLAYER_LEVEL }, (_, i) => {
  const level = i + 1;
  const xpThreshold =
    level === 1
      ? 0
      : level === MAX_PLAYER_LEVEL
      ? 600_000_000
      : Math.floor(25 * Math.pow(24_000_000, (level - 2) / 198));
  const t = (level - 1) / (MAX_PLAYER_LEVEL - 1);
  const multiplier = Math.round((1 + 11 * Math.pow(t, 2.2)) * 100) / 100;
  return {
    level,
    title: titleForLevel(level),
    xpThreshold,
    multiplier,
  };
});

export const UPGRADES: UpgradeDefinition[] = [
  {
    id: 'laser_pickaxe',
    title: 'Лазерная кирка',
    description: '+1 к силе клика',
    type: 'CLICK_POWER',
    baseCost: 15,
    baseGain: 1,
    costMultiplier: 1.15,
  },
  {
    id: 'plasma_drill',
    title: 'Плазменный бур',
    description: '+5 к силе клика',
    type: 'CLICK_POWER',
    baseCost: 120,
    baseGain: 5,
    costMultiplier: 1.15,
  },
  {
    id: 'quantum_amplifier',
    title: 'Квантовый усилитель',
    description: '+25 к силе клика',
    type: 'CLICK_POWER',
    baseCost: 1100,
    baseGain: 25,
    costMultiplier: 1.15,
  },
  {
    id: 'gravity_hammer',
    title: 'Гравитационный молот',
    description: '+120 к силе клика',
    type: 'CLICK_POWER',
    baseCost: 12000,
    baseGain: 120,
    costMultiplier: 1.15,
  },
  {
    id: 'core_annihilator',
    title: 'Аннигилятор коры',
    description: '+600 к силе клика',
    type: 'CLICK_POWER',
    baseCost: 140000,
    baseGain: 600,
    costMultiplier: 1.15,
  },
  {
    id: 'supernova_prism',
    title: 'Сверхновая призма',
    description: '+3.5K к силе клика',
    type: 'CLICK_POWER',
    baseCost: 1800000,
    baseGain: 3500,
    costMultiplier: 1.15,
  },
  {
    id: 'mini_drone',
    title: 'Мини-дрон',
    description: '+2 крист./сек',
    type: 'PASSIVE_INCOME',
    baseCost: 25,
    baseGain: 2,
    costMultiplier: 1.15,
  },
  {
    id: 'orbital_station',
    title: 'Орбитальная станция',
    description: '+15 крист./сек',
    type: 'PASSIVE_INCOME',
    baseCost: 220,
    baseGain: 15,
    costMultiplier: 1.15,
  },
  {
    id: 'singularity_reactor',
    title: 'Сингулярный реактор',
    description: '+100 крист./сек',
    type: 'PASSIVE_INCOME',
    baseCost: 2400,
    baseGain: 100,
    costMultiplier: 1.15,
  },
  {
    id: 'dyson_sphere',
    title: 'Сфера Дайсона',
    description: '+650 крист./сек',
    type: 'PASSIVE_INCOME',
    baseCost: 28000,
    baseGain: 650,
    costMultiplier: 1.15,
  },
  {
    id: 'star_forge',
    title: 'Звёздная кузница',
    description: '+4.2K крист./сек',
    type: 'PASSIVE_INCOME',
    baseCost: 350000,
    baseGain: 4200,
    costMultiplier: 1.15,
  },
  {
    id: 'multiverse_matrix',
    title: 'Матрица вселенной',
    description: '+30K крист./сек',
    type: 'PASSIVE_INCOME',
    baseCost: 4500000,
    baseGain: 30000,
    costMultiplier: 1.15,
  },
];

export const ACHIEVEMENTS: AchievementDefinition[] = [
  {
    id: 'clicks_100',
    title: 'Первый импульс',
    description: 'Сделать 100 кликов по кристаллу',
    metric: 'TOTAL_CLICKS',
    targetValue: 100,
    rewardCrystals: 50,
  },
  {
    id: 'crystals_1k',
    title: 'Тысяча граней',
    description: 'Добыть 1 000 кристаллов за всё время',
    metric: 'TOTAL_CRYSTALS_EARNED',
    targetValue: 1000,
    rewardCrystals: 250,
  },
  {
    id: 'upgrades_10',
    title: 'Инженер флота',
    description: 'Купить 10 любых улучшений',
    metric: 'TOTAL_UPGRADES_BOUGHT',
    targetValue: 10,
    rewardCrystals: 500,
  },
  {
    id: 'passive_50',
    title: 'Автономная сеть',
    description: 'Достичь пассивного дохода 50/сек',
    metric: 'PASSIVE_PER_SEC',
    targetValue: 50,
    rewardCrystals: 1500,
  },
  {
    id: 'crits_25',
    title: 'Охотник за критами',
    description: 'Выбить 25 критических кликов (x3)',
    metric: 'CRITICAL_HITS',
    targetValue: 25,
    rewardCrystals: 2500,
  },
  {
    id: 'clicks_1000',
    title: 'Ударная волна',
    description: 'Сделать 1 000 кликов по кристаллу',
    metric: 'TOTAL_CLICKS',
    targetValue: 1000,
    rewardCrystals: 5000,
  },
  {
    id: 'upgrades_50',
    title: 'Промышленный гигант',
    description: 'Купить 50 улучшений за один заход',
    metric: 'TOTAL_UPGRADES_BOUGHT',
    targetValue: 50,
    rewardCrystals: 15000,
  },
  {
    id: 'comets_3',
    title: 'Ловец комет',
    description: 'Поймать 3 пролетающие кометы',
    metric: 'COMETS_CAUGHT',
    targetValue: 3,
    rewardCrystals: 25000,
  },
  {
    id: 'crystals_1m',
    title: 'Миллионер орбиты',
    description: 'Добыть 1M кристаллов за всё время',
    metric: 'TOTAL_CRYSTALS_EARNED',
    targetValue: 1000000,
    rewardCrystals: 50000,
  },
  {
    id: 'passive_5k',
    title: 'Космический магнат',
    description: 'Достичь пассивного дохода 5K/сек',
    metric: 'PASSIVE_PER_SEC',
    targetValue: 5000,
    rewardCrystals: 150000,
  },
  {
    id: 'crits_150',
    title: 'Мастер резонанса',
    description: 'Выбить 150 критических ударов',
    metric: 'CRITICAL_HITS',
    targetValue: 150,
    rewardCrystals: 300000,
  },
  {
    id: 'prestige_1',
    title: 'Повелитель Коллапса',
    description: 'Совершить свой первый Коллапс (престиж)',
    metric: 'PRESTIGE_COUNT',
    targetValue: 1,
    rewardCrystals: 500000,
  },
];

export function getUpgradeCost(upgrade: UpgradeDefinition, level: number): number {
  return Math.floor(upgrade.baseCost * Math.pow(upgrade.costMultiplier, level));
}

const SUFFIXES = ['', 'K', 'M', 'B', 'T', 'Qa', 'Qi'];

export function formatCrystalCount(value: number): string {
  if (!Number.isFinite(value) || value < 0) return '0';
  if (value < 1000) return Math.floor(value).toString();

  let scaled = value;
  let tier = 0;
  while (scaled >= 1000 && tier < SUFFIXES.length - 1) {
    scaled /= 1000;
    tier++;
  }

  const formatted =
    scaled >= 100 || scaled % 1 === 0
      ? Math.floor(scaled).toString()
      : scaled.toFixed(1).replace(/\.0$/, '');

  return `${formatted}${SUFFIXES[tier]}`;
}

export function formatRate(value: number): string {
  if (value > 0 && value < 10 && value % 1 !== 0) {
    return value.toFixed(1);
  }
  return formatCrystalCount(value);
}

export function formatDuration(totalSeconds: number): string {
  const clamped = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(clamped / 3600);
  const minutes = Math.floor((clamped % 3600) / 60);
  const seconds = clamped % 60;

  if (hours > 0) return `${hours} ч ${minutes} мин`;
  if (minutes > 0) return `${minutes} мин ${seconds} сек`;
  return `${seconds} сек`;
}
