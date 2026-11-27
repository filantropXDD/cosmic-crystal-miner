export type UpgradeType = 'CLICK_POWER' | 'PASSIVE_INCOME';

export type BuyQuantity = 'X1' | 'X10' | 'X100' | 'MAX';

export interface UpgradePurchasePlan {
  levelsToBuy: number;
  totalCost: number;
  canAfford: boolean;
}

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
  | 'PRESTIGE_COUNT'
  | 'BOSSES_DEFEATED'
  | 'FEVER_COUNT'
  | 'SECRET_HEADER_TAP';

export interface AchievementDefinition {
  id: string;
  title: string;
  description: string;
  metric: AchievementMetric;
  targetValue: number;
  rewardCrystals: number;
  isSecret?: boolean;
}

export type CrystalSkinId =
  | 'classic'
  | 'black_hole'
  | 'solar_core'
  | 'cyber_cube'
  | 'ice_relic'
  | 'rainbow_prism';

export interface CrystalSkinDefinition {
  id: CrystalSkinId;
  title: string;
  subtitle: string;
  unlockHint: string;
  requiredAchievementId: string | null;
}

export type SpaceLocationId = 'nebula' | 'red_giant' | 'asteroid_belt' | 'deep_void';

export interface SpaceLocationDefinition {
  id: SpaceLocationId;
  title: string;
  subtitle: string;
  unlockHint: string;
  requiredAchievementId: string | null;
}

export type TalentBranch = 'ACTIVE_CLICKER' | 'TYCOON';

export interface TalentDefinition {
  id: string;
  title: string;
  description: string;
  branch: TalentBranch;
  maxLevel: number;
  baseCostDm: number;
  costStepDm: number;
}

export interface ExpeditionPreset {
  id: string;
  title: string;
  subtitle: string;
  durationSeconds: number;
  crystalSecondsEquivalent: number;
  darkMatterChance: number;
}

export type DailyQuestType = 'CLICKS' | 'UPGRADES' | 'FEVER';

export interface DailyQuestDefinition {
  id: string;
  title: string;
  type: DailyQuestType;
  target: number;
  rewardDarkMatter: number;
  rewardCrystalsFactor: number;
}

export interface BossUiState {
  isActive: boolean;
  name: string;
  currentHp: number;
  maxHp: number;
  remainingSeconds: number;
  cooldownSeconds: number;
  lastResultMessage: string | null;
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
  bossesDefeated: number;
  feverActivations: number;
  bestCps: number;
  playTimeSeconds: number;
  darkMatter: number;
  spentDarkMatter: number;
  prestigeCount: number;
  upgradeLevels: Record<string, number>;
  talentLevels: Record<string, number>;
  claimedAchievements: string[];
  secretHeaderUnlocked: boolean;
  selectedSkinId: CrystalSkinId;
  selectedLocationId: SpaceLocationId;
  activeExpeditionId: string | null;
  expeditionEndMs: number;
  dailyDateKey: string;
  dailyClicks: number;
  dailyUpgrades: number;
  dailyFever: number;
  dailyClaimedIds: string[];
  isVibrationEnabled: boolean;
  isSoundEnabled: boolean;
  isPotatoMode: boolean;
  lastActiveTimestampMs: number;
}

export const MAX_PLAYER_LEVEL = 200;
export const MAX_UPGRADE_LEVEL = 200;
export const PRESTIGE_BASE_THRESHOLD = 100_000;
export const OVERCHARGE_DURATION_SEC = 10;
export const OVERCHARGE_MULTIPLIER = 5;
export const FEVER_DURATION_SEC = 10;
export const BOSS_DURATION_SEC = 30;
export const BOSS_AUTO_SPAWN_SEC = 600;
export const BOSS_MANUAL_COOLDOWN_SEC = 45;
export const MAX_OFFLINE_SECONDS = 12 * 3600;

export function calculatePotentialDarkMatter(runCrystalsEarned: number): number {
  if (runCrystalsEarned < PRESTIGE_BASE_THRESHOLD) return 0;
  return Math.floor(Math.sqrt(runCrystalsEarned / PRESTIGE_BASE_THRESHOLD));
}

export function nextDarkMatterRequirement(currentPotential: number): number {
  const next = currentPotential + 1;
  return next * next * PRESTIGE_BASE_THRESHOLD;
}

export function getTalentCost(talent: TalentDefinition, level: number): number {
  return talent.baseCostDm + level * talent.costStepDm;
}

export function getUpgradeCost(
  upgrade: UpgradeDefinition,
  level: number,
  discountFraction = 0
): number {
  const raw = Math.floor(upgrade.baseCost * Math.pow(upgrade.costMultiplier, level));
  const clamped = Math.min(0.5, Math.max(0, discountFraction));
  return Math.max(1, Math.floor(raw * (1 - clamped)));
}

export function calculateUpgradePurchase(
  upgrade: UpgradeDefinition,
  currentLevel: number,
  crystals: number,
  discountFraction: number,
  quantity: BuyQuantity
): UpgradePurchasePlan {
  const remaining = Math.max(0, MAX_UPGRADE_LEVEL - currentLevel);
  if (remaining <= 0) {
    return { levelsToBuy: 0, totalCost: 0, canAfford: false };
  }

  const discountFactor = 1 - Math.min(0.5, Math.max(0, discountFraction));
  let raw = upgrade.baseCost * Math.pow(upgrade.costMultiplier, currentLevel);

  if (quantity === 'X1') {
    const cost = Math.max(1, Math.floor(Math.floor(raw) * discountFactor));
    return { levelsToBuy: 1, totalCost: cost, canAfford: crystals >= cost };
  }

  if (quantity === 'X10' || quantity === 'X100') {
    const target = Math.min(quantity === 'X10' ? 10 : 100, remaining);
    let sum = 0;
    for (let i = 0; i < target; i++) {
      sum += Math.max(1, Math.floor(Math.floor(raw) * discountFactor));
      raw *= upgrade.costMultiplier;
    }
    return { levelsToBuy: target, totalCost: sum, canAfford: crystals >= sum };
  }

  let sum = 0;
  let count = 0;
  let firstCost = 0;
  for (let i = 0; i < remaining; i++) {
    const stepCost = Math.max(1, Math.floor(Math.floor(raw) * discountFactor));
    if (i === 0) firstCost = stepCost;
    if (sum + stepCost <= crystals) {
      sum += stepCost;
      count++;
      raw *= upgrade.costMultiplier;
    } else {
      break;
    }
  }
  if (count > 0) {
    return { levelsToBuy: count, totalCost: sum, canAfford: true };
  }
  return { levelsToBuy: 1, totalCost: firstCost, canAfford: false };
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

export function resolveRankFast(experience: number): {
  curr: MinerRank;
  next: MinerRank | null;
} {
  let low = 0;
  let high = RANKS.length - 1;
  let idx = 0;
  while (low <= high) {
    const mid = (low + high) >>> 1;
    if (experience >= RANKS[mid].xpThreshold) {
      idx = mid;
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }
  return { curr: RANKS[idx], next: RANKS[idx + 1] ?? null };
}

export const CRYSTAL_SKINS: CrystalSkinDefinition[] = [
  {
    id: 'classic',
    title: 'Космический кристалл',
    subtitle: 'Неоновые грани и орбитальные кольца',
    unlockHint: 'Доступен с начала игры',
    requiredAchievementId: null,
  },
  {
    id: 'black_hole',
    title: 'Чёрная дыра',
    subtitle: 'Сфера горизонта и частицы, затягиваемые внутрь',
    unlockHint: 'Совершите 1-й Коллапс (достижение «Повелитель Коллапса»)',
    requiredAchievementId: 'prestige_1',
  },
  {
    id: 'solar_core',
    title: 'Солнечное ядро',
    subtitle: 'Огненная звезда с короной и протуберанцами',
    unlockHint: 'Поймайте 15 комет (достижение «Звёздный перехватчик»)',
    requiredAchievementId: 'comets_15',
  },
  {
    id: 'cyber_cube',
    title: 'Кибер-куб',
    subtitle: 'Неоновый тессеракт и зелёные биты Матрицы',
    unlockHint: 'Купите 100 улучшений за заход («Архитектор флота»)',
    requiredAchievementId: 'upgrades_100',
  },
  {
    id: 'ice_relic',
    title: 'Ледяная реликвия',
    subtitle: 'Кристалл вечной мерзлоты и снежинки-осколки',
    unlockHint: 'Победите первого босса-астероида («Охотник на Левиафанов»)',
    requiredAchievementId: 'boss_1',
  },
  {
    id: 'rainbow_prism',
    title: 'Радужная призма',
    subtitle: 'Спектральная грань — меняет цвет при каждом тапе',
    unlockHint: 'Добудьте 1M кристаллов («Миллионер орбиты»)',
    requiredAchievementId: 'crystals_1m',
  },
];

export const SPACE_LOCATIONS: SpaceLocationDefinition[] = [
  {
    id: 'nebula',
    title: 'Фиолетовая туманность',
    subtitle: 'Мягкое эфирное свечение и россыпь звёзд',
    unlockHint: 'Доступна с начала игры',
    requiredAchievementId: null,
  },
  {
    id: 'red_giant',
    title: 'Система Красного гиганта',
    subtitle: 'Багровый горизонт и раскалённая звёздная пыль',
    unlockHint: 'Сделайте 1 000 кликов («Ударная волна»)',
    requiredAchievementId: 'clicks_1000',
  },
  {
    id: 'asteroid_belt',
    title: 'Пояс астероидов',
    subtitle: 'Бирюзовая бездна и дрейфующие каменные глыбы',
    unlockHint: 'Победите первого босса-астероида («Охотник на Левиафанов»)',
    requiredAchievementId: 'boss_1',
  },
  {
    id: 'deep_void',
    title: 'Глубокая пустота',
    subtitle: 'Абсолютный мрак с падающими метеорами',
    unlockHint: 'Активируйте Космическую ярость 5 раз («Вспышка сверхновой»)',
    requiredAchievementId: 'fever_5',
  },
];

export const TALENTS: TalentDefinition[] = [
  {
    id: 'crit_edge',
    title: 'Резонансное лезвие',
    description: '+3% к шансу крита x3 за уровень',
    branch: 'ACTIVE_CLICKER',
    maxLevel: 5,
    baseCostDm: 1,
    costStepDm: 1,
  },
  {
    id: 'fever_surge',
    title: 'Ядерный перегрев',
    description: '+25% быстрее нагрев комбо и +1с к Ярости',
    branch: 'ACTIVE_CLICKER',
    maxLevel: 5,
    baseCostDm: 2,
    costStepDm: 1,
  },
  {
    id: 'comet_magnet',
    title: 'Притяжение комет',
    description: 'Кометы чаще и приносят +50% кристаллов',
    branch: 'ACTIVE_CLICKER',
    maxLevel: 3,
    baseCostDm: 3,
    costStepDm: 2,
  },
  {
    id: 'drone_overclock',
    title: 'Разгон флота',
    description: '+25% к доходу дронов и станций за уровень',
    branch: 'TYCOON',
    maxLevel: 5,
    baseCostDm: 1,
    costStepDm: 1,
  },
  {
    id: 'stellar_discount',
    title: 'Космические субсидии',
    description: '-5% к цене всех улучшений за уровень',
    branch: 'TYCOON',
    maxLevel: 5,
    baseCostDm: 2,
    costStepDm: 1,
  },
  {
    id: 'cryo_vault',
    title: 'Квантовое хранилище',
    description: '+35% к офлайн-добыче за уровень',
    branch: 'TYCOON',
    maxLevel: 3,
    baseCostDm: 3,
    costStepDm: 2,
  },
];

export const EXPEDITIONS: ExpeditionPreset[] = [
  {
    id: 'scout_5',
    title: 'Разведка орбиты',
    subtitle: 'Быстрая вылазка за обломками',
    durationSeconds: 5 * 60,
    crystalSecondsEquivalent: 90,
    darkMatterChance: 0.25,
  },
  {
    id: 'anomaly_15',
    title: 'Пояс аномалий',
    subtitle: 'Богатая жила и шанс Тёмной материи',
    durationSeconds: 15 * 60,
    crystalSecondsEquivalent: 320,
    darkMatterChance: 0.65,
  },
  {
    id: 'deep_sector_30',
    title: 'Дальний сектор',
    subtitle: 'Крупный груз и гарантированная ТМ',
    durationSeconds: 30 * 60,
    crystalSecondsEquivalent: 750,
    darkMatterChance: 1.0,
  },
];

export const DAILY_QUESTS: DailyQuestDefinition[] = [
  {
    id: 'daily_clicks_300',
    title: 'Тапнуть 300 раз',
    type: 'CLICKS',
    target: 300,
    rewardDarkMatter: 1,
    rewardCrystalsFactor: 40,
  },
  {
    id: 'daily_upgrades_5',
    title: 'Купить 5 улучшений',
    type: 'UPGRADES',
    target: 5,
    rewardDarkMatter: 1,
    rewardCrystalsFactor: 55,
  },
  {
    id: 'daily_fever_3',
    title: 'Включить Ярость 3 раза',
    type: 'FEVER',
    target: 3,
    rewardDarkMatter: 2,
    rewardCrystalsFactor: 80,
  },
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
    description: 'Сделать 1 000 кликов (открывает локацию)',
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
    id: 'upgrades_100',
    title: 'Архитектор флота',
    description: 'Купить 100 улучшений за заход (открывает Кибер-куб)',
    metric: 'TOTAL_UPGRADES_BOUGHT',
    targetValue: 100,
    rewardCrystals: 40000,
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
    id: 'comets_15',
    title: 'Звёздный перехватчик',
    description: 'Поймать 15 комет (открывает Солнечное ядро)',
    metric: 'COMETS_CAUGHT',
    targetValue: 15,
    rewardCrystals: 120000,
  },
  {
    id: 'fever_5',
    title: 'Вспышка сверхновой',
    description: 'Включить Космическую ярость 5 раз (открывает Пустоту)',
    metric: 'FEVER_COUNT',
    targetValue: 5,
    rewardCrystals: 60000,
  },
  {
    id: 'boss_1',
    title: 'Охотник на Левиафанов',
    description: 'Победить босса-астероида (открывает Ледяную реликвию)',
    metric: 'BOSSES_DEFEATED',
    targetValue: 1,
    rewardCrystals: 80000,
  },
  {
    id: 'crystals_1m',
    title: 'Миллионер орбиты',
    description: 'Добыть 1M кристаллов (открывает Радужную призму)',
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
    description: 'Совершить 1-й Коллапс (открывает Чёрную дыру)',
    metric: 'PRESTIGE_COUNT',
    targetValue: 1,
    rewardCrystals: 500000,
  },
  {
    id: 'secret_header_tap',
    title: 'Тайна заголовка',
    description: 'Секрет: нажать 7 раз на слово «Кристаллы»',
    metric: 'SECRET_HEADER_TAP',
    targetValue: 1,
    rewardCrystals: 25000,
    isSecret: true,
  },
];

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
