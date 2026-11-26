import React from 'react';
import {
  Cpu,
  Flame,
  Hammer,
  MousePointerClick,
  Orbit,
  Radio,
  Rocket,
  Zap,
} from 'lucide-react';
import {
  UPGRADES,
  UpgradeDefinition,
  formatCrystalCount,
  getUpgradeCost,
} from '../types/game';

interface UpgradesPanelProps {
  crystals: number;
  upgradeLevels: Record<string, number>;
  onBuyUpgrade: (upgradeId: string) => void;
}

const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  laser_pickaxe: MousePointerClick,
  plasma_drill: Hammer,
  quantum_amplifier: Zap,
  gravity_hammer: Flame,
  mini_drone: Cpu,
  orbital_station: Radio,
  singularity_reactor: Rocket,
  dyson_sphere: Orbit,
};

export const UpgradesPanel: React.FC<UpgradesPanelProps> = ({
  crystals,
  upgradeLevels,
  onBuyUpgrade,
}) => {
  const clickUpgrades = UPGRADES.filter((u) => u.type === 'CLICK_POWER');
  const passiveUpgrades = UPGRADES.filter((u) => u.type === 'PASSIVE_INCOME');

  const renderCard = (upgrade: UpgradeDefinition) => {
    const level = upgradeLevels[upgrade.id] ?? 0;
    const cost = getUpgradeCost(upgrade, level);
    const canAfford = crystals >= cost;
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
          <div className="flex items-baseline gap-2">
            <h3 className="text-sm font-semibold text-slate-100 truncate">
              {upgrade.title}
            </h3>
            <span
              className={`text-xs font-mono-num font-bold shrink-0 ${
                isClick ? 'text-cyan-300' : 'text-violet-300'
              }`}
            >
              Ур. {level}
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5 truncate">
            {upgrade.description} · {isClick ? 'Клик' : 'Авто'}
          </p>
        </div>

        <button
          type="button"
          onClick={() => onBuyUpgrade(upgrade.id)}
          disabled={!canAfford}
          className={`min-h-[44px] min-w-[84px] px-3.5 py-2 rounded-xl font-mono-num text-xs font-bold whitespace-nowrap shrink-0 transition-all cursor-pointer ${
            canAfford
              ? isClick
                ? 'bg-cyan-400 text-slate-950 hover:bg-cyan-300 active:scale-95 shadow-sm shadow-cyan-500/20'
                : 'bg-violet-500 text-white hover:bg-violet-400 active:scale-95 shadow-sm shadow-violet-500/20'
              : 'bg-slate-800/70 text-slate-500 opacity-60 cursor-not-allowed'
          }`}
        >
          {formatCrystalCount(cost)}
        </button>
      </div>
    );
  };

  return (
    <div className="space-y-4">
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
