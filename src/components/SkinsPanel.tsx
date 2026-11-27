import React from 'react';
import { Lock, Sparkles } from 'lucide-react';
import {
  CRYSTAL_SKINS,
  CrystalSkinId,
  SPACE_LOCATIONS,
  SpaceLocationId,
} from '../types/game';

interface SkinsPanelProps {
  selectedSkinId: CrystalSkinId;
  selectedLocationId: SpaceLocationId;
  unlockedAchievementIds: string[];
  onSelectSkin: (skinId: CrystalSkinId) => void;
  onSelectLocation: (locationId: SpaceLocationId) => void;
}

export const SkinsPanel: React.FC<SkinsPanelProps> = ({
  selectedSkinId,
  selectedLocationId,
  unlockedAchievementIds,
  onSelectSkin,
  onSelectLocation,
}) => {
  return (
    <div className="space-y-4">
      <div className="space-y-2.5">
        <h2 className="text-xs font-bold text-cyan-300 px-1">
          Коллекция объектов (открываются за достижения)
        </h2>
        <div className="space-y-2.5">
          {CRYSTAL_SKINS.map((skin) => {
            const isUnlocked =
              !skin.requiredAchievementId ||
              unlockedAchievementIds.includes(skin.requiredAchievementId);
            const isSelected = selectedSkinId === skin.id;

            return (
              <div
                key={skin.id}
                className={`flex items-center gap-3 p-3.5 rounded-2xl bg-[#121833]/90 border transition-colors ${
                  isSelected
                    ? 'border-cyan-400/75'
                    : isUnlocked
                    ? 'border-slate-800/80'
                    : 'border-slate-800/40 opacity-80'
                }`}
              >
                <div
                  className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                    isUnlocked ? 'bg-cyan-400/15 text-cyan-300' : 'bg-slate-800 text-slate-500'
                  }`}
                >
                  {isUnlocked ? <Sparkles className="w-5 h-5" /> : <Lock className="w-5 h-5" />}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="text-sm font-bold text-slate-100">{skin.title}</div>
                  <div
                    className={`text-xs mt-0.5 ${
                      isUnlocked ? 'text-slate-400' : 'text-amber-300/90'
                    }`}
                  >
                    {isUnlocked ? skin.subtitle : skin.unlockHint}
                  </div>
                </div>

                {isUnlocked && (
                  <button
                    type="button"
                    disabled={isSelected}
                    onClick={() => onSelectSkin(skin.id)}
                    className={`min-h-[38px] px-3 rounded-xl text-xs font-bold shrink-0 transition-colors ${
                      isSelected
                        ? 'bg-emerald-500/20 text-emerald-300 cursor-default'
                        : 'bg-cyan-400 hover:bg-cyan-300 text-slate-950 cursor-pointer'
                    }`}
                  >
                    {isSelected ? 'Выбрано' : 'Выбрать'}
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </div>

      <div className="space-y-2.5 pt-1">
        <h2 className="text-xs font-bold text-violet-300 px-1">Космические локации</h2>
        <div className="space-y-2.5">
          {SPACE_LOCATIONS.map((loc) => {
            const isUnlocked =
              !loc.requiredAchievementId ||
              unlockedAchievementIds.includes(loc.requiredAchievementId);
            const isSelected = selectedLocationId === loc.id;

            return (
              <div
                key={loc.id}
                className={`flex items-center gap-3 p-3.5 rounded-2xl bg-[#121833]/90 border transition-colors ${
                  isSelected
                    ? 'border-violet-400/75'
                    : isUnlocked
                    ? 'border-slate-800/80'
                    : 'border-slate-800/40 opacity-80'
                }`}
              >
                <div
                  className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                    isUnlocked ? 'bg-violet-500/15 text-violet-300' : 'bg-slate-800 text-slate-500'
                  }`}
                >
                  {isUnlocked ? <Sparkles className="w-5 h-5" /> : <Lock className="w-5 h-5" />}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="text-sm font-bold text-slate-100">{loc.title}</div>
                  <div
                    className={`text-xs mt-0.5 ${
                      isUnlocked ? 'text-slate-400' : 'text-amber-300/90'
                    }`}
                  >
                    {isUnlocked ? loc.subtitle : loc.unlockHint}
                  </div>
                </div>

                {isUnlocked && (
                  <button
                    type="button"
                    disabled={isSelected}
                    onClick={() => onSelectLocation(loc.id)}
                    className={`min-h-[38px] px-3 rounded-xl text-xs font-bold shrink-0 transition-colors ${
                      isSelected
                        ? 'bg-emerald-500/20 text-emerald-300 cursor-default'
                        : 'bg-violet-500 hover:bg-violet-400 text-white cursor-pointer'
                    }`}
                  >
                    {isSelected ? 'Выбрано' : 'Выбрать'}
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
