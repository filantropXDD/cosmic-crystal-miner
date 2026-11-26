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
  | 'CRITICAL_HITS';

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
}

export interface PersistedGameState {
  crystals: number;
  totalCrystalsEarned: number;
  experience: number;
  totalClicks: number;
  criticalHits: number;
  upgradeLevels: Record<string, number>;
  claimedAchievements: string[];
  isVibrationEnabled: boolean;
  isSoundEnabled: boolean;
}

export const RANKS: MinerRank[] = [
  { level: 1, title: 'Новичок', xpThreshold: 0, multiplier: 1.0 },
  { level: 2, title: 'Стажёр орбиты', xpThreshold: 100, multiplier: 1.15 },
  { level: 3, title: 'Шахтёр', xpThreshold: 600, multiplier: 1.35 },
  { level: 4, title: 'Рейнджер пояса', xpThreshold: 2500, multiplier: 1.6 },
  { level: 5, title: 'Мастер', xpThreshold: 10000, multiplier: 2.0 },
  { level: 6, title: 'Звёздный магнат', xpThreshold: 45000, multiplier: 2.5 },
  { level: 7, title: 'Космический барон', xpThreshold: 180000, multiplier: 3.2 },
  { level: 8, title: 'Повелитель сингулярности', xpThreshold: 750000, multiplier: 4.0 },
];

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
