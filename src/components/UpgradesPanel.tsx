import React from 'react';
import {
  Cpu,
  Flame,
  Hammer,
  MousePointerClick,
  Orbit,
  Radio,
  Rocket,
  Sparkles,
  Sun,
  Zap,
  Atom,
  Network,
} from 'lucide-react';
import {
  BuyQuantity,
  MAX_UPGRADE_LEVEL,
  UPGRADES,
  UpgradeDefinition,
  calculateUpgradePurchase,
  formatCrystalCount,
} from '../types/game';

interface UpgradesPanelProps {
  crystals: number;
  upgradeLevels: Record<string, number>;
  discountFraction?: number;
  buyQuantity: BuyQuantity;
  onBuyQuantityChange: (quantity: BuyQuantity) => void;
  onBuyUpgrade: (upgradeId: string) => void;
}

const BUY_OPTIONS: { id: BuyQuantity; label: string }[] = [
  { id: 'X1', label: 'x1' },
  { id: 'X10', label: 'x10' },
  { id: 'X100', label: 'x100' },
  { id: 'MAX', label: 'MAX' },
];

const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  laser_pickaxe: MousePointerClick,
  plasma_drill: Hammer,
  quantum_amplifier: Zap,
  gravity_hammer: Flame,
  core_annihilator: Atom,
  supernova_prism: Sun,
  mini_drone: Cpu,
  orbital_station: Radio,
  singularity_reactor: Rocket,
  dyson_sphere: Orbit,
  star_forge: Sparkles,
  multiverse_matrix: Network,
};

export const UpgradesPanel: React.FC<UpgradesPanelProps> = ({
  crystals,
  upgradeLevels,
  discountFraction = 0,
  buyQuantity,
  onBuyQuantityChange,
  onBuyUpgrade,
}) => {
  const clickUpgrades = UPGRADES.filter((u) => u.type === 'CLICK_POWER');
  const passiveUpgrades = UPGRADES.filter((u) => u.type === 'PASSIVE_INCOME');

  const renderCard = (upgrade: UpgradeDefinition) => {
    const level = upgradeLevels[upgrade.id] ?? 0;
    const isMaxLevel = level >= MAX_UPGRADE_LEVEL;
    const plan = calculateUpgradePurchase(
      upgrade,
      level,
      crystals,
      discountFraction,
      buyQuantity
    );
    const canAfford = !isMaxLevel && plan.canAfford;
    const isClick = upgrade.type === 'CLICK_POWER';
    const Icon = ICON_MAP[upgrade.id] ?? Zap;

    return (
      <div
        key={upgrade.id}
        className={`flex items-center gap-3 p-3.5 rounded-2xl bg-[#121833]/90 border transition-colors ${
          canAfford
            ? isClick
              ? 'border-cyan-400/35'
              : 'border-violet-400/35'
            : 'border-slate-800/80'
        }`}
      >
        <div
          className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${
            isClick
              ? 'bg-cyan-400/15 text-cyan-300'
              : 'bg-violet-500/15 text-violet-300'
          }`}
        >
          <Icon className="w-5 h-5" />
        </div>

        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-semibold text-slate-100 leading-snug">
            {upgrade.title}
          </h3>
          <p className="text-xs text-slate-400 mt-0.5 leading-snug">
            <span
              className={`font-mono-num font-bold ${
                isClick ? 'text-cyan-300' : 'text-violet-300'
              }`}
            >
              Ур. {level}/{MAX_UPGRADE_LEVEL}
            </span>{' '}
            · {upgrade.description}
          </p>
        </div>

        <button
          type="button"
          onClick={() => onBuyUpgrade(upgrade.id)}
          disabled={!canAfford}
          className={`min-h-[44px] min-w-[88px] px-3.5 py-1.5 rounded-xl font-mono-num text-xs font-bold whitespace-nowrap shrink-0 flex flex-col items-center justify-center leading-tight transition-all ${
            canAfford
              ? isClick
                ? 'bg-cyan-400 text-slate-950 hover:bg-cyan-300 active:scale-95 shadow-sm shadow-cyan-500/20 cursor-pointer'
                : 'bg-violet-500 text-white hover:bg-violet-400 active:scale-95 shadow-sm shadow-violet-500/20 cursor-pointer'
              : isMaxLevel
              ? 'bg-slate-800/80 text-slate-400 cursor-default'
              : 'bg-slate-800/70 text-slate-500 opacity-60 cursor-not-allowed'
          }`}
        >
          {isMaxLevel ? (
            <span>МАКС.</span>
          ) : (
            <>
              <span>{formatCrystalCount(plan.totalCost)}</span>
              {plan.levelsToBuy > 1 && (
                <span className="text-[10px] opacity-85">+{plan.levelsToBuy}</span>
              )}
            </>
          )}
        </button>
      </div>
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3 px-1">
        <span className="text-xs font-medium text-slate-400">Количество покупки</span>
        <div className="inline-flex items-center gap-1 p-1 rounded-xl bg-[#121833] border border-slate-800">
          {BUY_OPTIONS.map((opt) => {
            const selected = opt.id === buyQuantity;
            return (
              <button
                key={opt.id}
                type="button"
                onClick={() => onBuyQuantityChange(opt.id)}
                className={`px-3 py-1 rounded-lg text-xs font-mono-num font-bold cursor-pointer transition-colors ${
                  selected
                    ? 'bg-cyan-400 text-slate-950'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {opt.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="space-y-2.5">
        <h2 className="text-xs font-semibold text-cyan-300/90 px-1">
          Усилители клика
        </h2>
        <div className="space-y-2.5">{clickUpgrades.map(renderCard)}</div>
      </div>

      <div className="space-y-2.5 pt-1">
        <h2 className="text-xs font-semibold text-violet-300/90 px-1">
          Автодобыча и реакторы
        </h2>
        <div className="space-y-2.5">{passiveUpgrades.map(renderCard)}</div>
      </div>
    </div>
  );
};
