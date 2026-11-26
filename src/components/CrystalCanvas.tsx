import React, { useEffect, useRef, useState } from 'react';
import { FloatingNumber, formatCrystalCount, formatRate } from '../types/game';

interface CrystalCanvasProps {
  rankLevel: number;
  overchargeSecondsLeft: number;
  isCometVisible: boolean;
  cometSeed: number;
  onTapCrystal: () => { gained: number; isCritical: boolean };
  onCatchComet: () => number;
  isVibrationEnabled: boolean;
}

interface BurstParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
  size: number;
}

export const CrystalCanvas: React.FC<CrystalCanvasProps> = ({
  rankLevel,
  overchargeSecondsLeft,
  isCometVisible,
  cometSeed,
  onTapCrystal,
  onCatchComet,
  isVibrationEnabled,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const scaleRef = useRef<number>(1);
  const scaleVelocityRef = useRef<number>(0);
  const flashAlphaRef = useRef<number>(0);
  const shakeRef = useRef<{ x: number; y: number; time: number }>({ x: 0, y: 0, time: 0 });
  const particlesRef = useRef<BurstParticle[]>([]);
  const [floatingNumbers, setFloatingNumbers] = useState<FloatingNumber[]>([]);
  const [cometPos, setCometPos] = useState<{ x: number; y: number; fromLeft: boolean }>({
    x: -40,
    y: 40,
    fromLeft: true,
  });
  const nextIdRef = useRef<number>(1);

  const rankRef = useRef(rankLevel);
  rankRef.current = rankLevel;
  const overchargeRef = useRef(overchargeSecondsLeft);
  overchargeRef.current = overchargeSecondsLeft;

  // Animate flying comet across the arena when active
  useEffect(() => {
    if (!isCometVisible) return;
    const fromLeft = cometSeed % 2 === 0;
    const baseY = 30 + (cometSeed % 140);
    const start = performance.now();
    const duration = 6200;
    let frameId = 0;

    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      const x = fromLeft ? -20 + t * 310 : 290 - t * 310;
      const y = baseY - Math.sin(t * Math.PI) * 38;
      setCometPos({ x, y, fromLeft });
      if (t < 1) {
        frameId = requestAnimationFrame(tick);
      }
    };

    frameId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frameId);
  }, [isCometVisible, cometSeed]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId = 0;
    let startTime = performance.now();

    const render = (now: number) => {
      const dt = Math.min((now - startTime) / 1000, 0.05);
      startTime = now;

      const dpr = window.devicePixelRatio || 1;
      const logicalSize = 270;
      if (canvas.width !== logicalSize * dpr || canvas.height !== logicalSize * dpr) {
        canvas.width = logicalSize * dpr;
        canvas.height = logicalSize * dpr;
      }

      ctx.save();
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, logicalSize, logicalSize);

      // Screen shake decay
      let sx = 0;
      let sy = 0;
      if (shakeRef.current.time > 0) {
        shakeRef.current.time -= dt;
        const mag = (shakeRef.current.time / 0.18) * 6;
        sx = (Math.random() - 0.5) * 2 * mag;
        sy = (Math.random() - 0.5) * 2 * mag;
      }

      // Flash decay
      if (flashAlphaRef.current > 0) {
        flashAlphaRef.current = Math.max(0, flashAlphaRef.current - dt * 3.5);
      }

      // Spring physics for scale bounce
      const stiffness = 260;
      const damping = 16;
      const force = (1 - scaleRef.current) * stiffness;
      scaleVelocityRef.current += force * dt;
      scaleVelocityRef.current *= Math.exp(-damping * dt);
      scaleRef.current += scaleVelocityRef.current * dt;

      const cx = logicalSize / 2 + sx;
      const cy = logicalSize / 2 + sy;
      const t = now * 0.001;
      const pulse = 1 + Math.sin(t * 2.2) * 0.04;
      const baseR = 92;

      const lvl = rankRef.current;
      const evolutionStage = lvl >= 150 ? 4 : lvl >= 100 ? 3 : lvl >= 50 ? 2 : 1;
      const isOvercharged = overchargeRef.current > 0;

      // Stage 4: Black Hole Accretion Halo
      if (evolutionStage >= 4) {
        const bhGlow = ctx.createRadialGradient(cx, cy, 10, cx, cy, baseR * 1.55 * pulse);
        bhGlow.addColorStop(0, 'rgba(3, 2, 10, 0.95)');
        bhGlow.addColorStop(0.45, 'rgba(88, 28, 135, 0.72)');
        bhGlow.addColorStop(0.72, 'rgba(251, 191, 36, 0.34)');
        bhGlow.addColorStop(1, 'rgba(6, 8, 20, 0)');
        ctx.fillStyle = bhGlow;
        ctx.beginPath();
        ctx.arc(cx, cy, baseR * 1.55 * pulse, 0, Math.PI * 2);
        ctx.fill();
      } else {
        const glow = ctx.createRadialGradient(cx, cy, 8, cx, cy, baseR * 1.45 * pulse);
        glow.addColorStop(
          0,
          isOvercharged
            ? 'rgba(251, 191, 36, 0.55)'
            : evolutionStage >= 3
            ? 'rgba(139, 92, 246, 0.52)'
            : 'rgba(139, 92, 246, 0.42)'
        );
        glow.addColorStop(0.55, 'rgba(34, 211, 238, 0.18)');
        glow.addColorStop(1, 'rgba(6, 8, 20, 0)');
        ctx.fillStyle = glow;
        ctx.beginPath();
        ctx.arc(cx, cy, baseR * 1.45 * pulse, 0, Math.PI * 2);
        ctx.fill();
      }

      // Primary Orbital Ring
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(t * 0.55);
      const ringGrad = ctx.createLinearGradient(-baseR, -baseR, baseR, baseR);
      ringGrad.addColorStop(0, isOvercharged ? 'rgba(251, 191, 36, 0.95)' : 'rgba(34, 211, 238, 0.85)');
      ringGrad.addColorStop(0.5, 'rgba(139, 92, 246, 0.15)');
      ringGrad.addColorStop(1, 'rgba(167, 139, 250, 0.85)');
      ctx.strokeStyle = ringGrad;
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.arc(0, 0, baseR * 1.08, 0, Math.PI * 2);
      ctx.stroke();

      const nodeX = Math.cos(0.6) * baseR * 1.08;
      const nodeY = Math.sin(0.6) * baseR * 1.08;
      ctx.fillStyle = isOvercharged ? '#FBBF24' : '#22D3EE';
      ctx.beginPath();
      ctx.arc(nodeX, nodeY, 4.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

      // Stage 2+: Second Counter-Rotating Elliptical Gold Ring
      if (evolutionStage >= 2) {
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(-t * 0.72);
        const secGrad = ctx.createLinearGradient(-baseR, 0, baseR, 0);
        secGrad.addColorStop(0, 'rgba(251, 191, 36, 0.85)');
        secGrad.addColorStop(0.5, 'rgba(34, 211, 238, 0.15)');
        secGrad.addColorStop(1, 'rgba(34, 211, 238, 0.65)');
        ctx.strokeStyle = secGrad;
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.ellipse(0, 0, baseR * 1.24, baseR * 0.84, 0, 0, Math.PI * 2);
        ctx.stroke();

        if (evolutionStage >= 3) {
          ctx.fillStyle = '#FBBF24';
          ctx.beginPath();
          ctx.arc(-baseR * 1.24, 0, 3.8, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.restore();
      }

      // Crystal shape with spring scale
      ctx.save();
      ctx.translate(cx, cy + Math.sin(t * 1.8) * 3);
      ctx.scale(scaleRef.current, scaleRef.current);

      const top = { x: 0, y: -baseR * 0.86 };
      const upperRight = { x: baseR * 0.56, y: -baseR * 0.26 };
      const lowerRight = { x: baseR * 0.46, y: baseR * 0.44 };
      const bottom = { x: 0, y: baseR * 0.88 };
      const lowerLeft = { x: -baseR * 0.46, y: baseR * 0.44 };
      const upperLeft = { x: -baseR * 0.56, y: -baseR * 0.26 };
      const core = { x: 0, y: -baseR * 0.04 };

      const bodyGrad = ctx.createLinearGradient(top.x, top.y, bottom.x, bottom.y);
      if (isOvercharged) {
        bodyGrad.addColorStop(0, '#FEF08A');
        bodyGrad.addColorStop(0.45, '#F59E0B');
        bodyGrad.addColorStop(1, '#78350F');
      } else if (evolutionStage >= 4) {
        bodyGrad.addColorStop(0, '#22D3EE');
        bodyGrad.addColorStop(0.5, '#6D28D9');
        bodyGrad.addColorStop(1, '#090518');
      } else if (evolutionStage >= 3) {
        bodyGrad.addColorStop(0, '#FDE68A');
        bodyGrad.addColorStop(0.48, '#8B5CF6');
        bodyGrad.addColorStop(1, '#312E81');
      } else {
        bodyGrad.addColorStop(0, '#67E8F9');
        bodyGrad.addColorStop(0.45, '#8B5CF6');
        bodyGrad.addColorStop(1, '#3B0764');
      }

      ctx.beginPath();
      ctx.moveTo(top.x, top.y);
      ctx.lineTo(upperRight.x, upperRight.y);
      ctx.lineTo(lowerRight.x, lowerRight.y);
      ctx.lineTo(bottom.x, bottom.y);
      ctx.lineTo(lowerLeft.x, lowerLeft.y);
      ctx.lineTo(upperLeft.x, upperLeft.y);
      ctx.closePath();
      ctx.fillStyle = bodyGrad;
      ctx.fill();

      // Left facet
      ctx.beginPath();
      ctx.moveTo(top.x, top.y);
      ctx.lineTo(core.x, core.y);
      ctx.lineTo(lowerLeft.x, lowerLeft.y);
      ctx.lineTo(upperLeft.x, upperLeft.y);
      ctx.closePath();
      ctx.fillStyle = evolutionStage >= 3 ? 'rgba(251, 191, 36, 0.22)' : 'rgba(34, 211, 238, 0.25)';
      ctx.fill();

      // Right facet
      ctx.beginPath();
      ctx.moveTo(core.x, core.y);
      ctx.lineTo(upperRight.x, upperRight.y);
      ctx.lineTo(lowerRight.x, lowerRight.y);
      ctx.lineTo(bottom.x, bottom.y);
      ctx.closePath();
      ctx.fillStyle = 'rgba(15, 23, 42, 0.38)';
      ctx.fill();

      // Inner facet lines
      ctx.strokeStyle = evolutionStage >= 2 ? 'rgba(251, 191, 36, 0.55)' : 'rgba(255, 255, 255, 0.38)';
      ctx.lineWidth = 1.5;
      [top, upperLeft, upperRight, lowerLeft, lowerRight, bottom].forEach((pt) => {
        ctx.beginPath();
        ctx.moveTo(core.x, core.y);
        ctx.lineTo(pt.x, pt.y);
        ctx.stroke();
      });

      // Outer neon edge
      ctx.beginPath();
      ctx.moveTo(top.x, top.y);
      ctx.lineTo(upperRight.x, upperRight.y);
      ctx.lineTo(lowerRight.x, lowerRight.y);
      ctx.lineTo(bottom.x, bottom.y);
      ctx.lineTo(lowerLeft.x, lowerLeft.y);
      ctx.lineTo(upperLeft.x, upperLeft.y);
      ctx.closePath();
      ctx.strokeStyle =
        isOvercharged || evolutionStage >= 3
          ? 'rgba(251, 191, 36, 0.92)'
          : evolutionStage >= 2
          ? 'rgba(251, 191, 36, 0.68)'
          : 'rgba(103, 232, 249, 0.85)';
      ctx.lineWidth = 2.4;
      ctx.stroke();

      ctx.restore();

      // Critical Flash overlay
      if (flashAlphaRef.current > 0.01) {
        const flashGrad = ctx.createRadialGradient(cx, cy, 4, cx, cy, baseR * 1.45);
        flashGrad.addColorStop(0, `rgba(255, 255, 255, ${flashAlphaRef.current})`);
        flashGrad.addColorStop(0.5, `rgba(251, 191, 36, ${flashAlphaRef.current * 0.6})`);
        flashGrad.addColorStop(1, 'rgba(251, 191, 36, 0)');
        ctx.fillStyle = flashGrad;
        ctx.beginPath();
        ctx.arc(cx, cy, baseR * 1.45, 0, Math.PI * 2);
        ctx.fill();
      }

      // Sparks
      const alive: BurstParticle[] = [];
      for (const p of particlesRef.current) {
        p.life += dt;
        if (p.life < p.maxLife) {
          p.x += p.vx * dt;
          p.y += p.vy * dt;
          p.vx *= 0.94;
          p.vy *= 0.94;
          const alpha = 1 - p.life / p.maxLife;
          ctx.globalAlpha = alpha;
          ctx.fillStyle = p.color;
          ctx.beginPath();
          ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
          ctx.fill();
          ctx.globalAlpha = 1;
          alive.push(p);
        }
      }
      particlesRef.current = alive;

      ctx.restore();
      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, []);

  const spawnFloatingLabel = (text: string, x: number, y: number, isCritical: boolean, isComet = false) => {
    const id = nextIdRef.current++;
    setFloatingNumbers((prev) => [
      ...prev.slice(-14),
      { id, text, x, y, isCritical, isComet },
    ]);
    window.setTimeout(() => {
      setFloatingNumbers((prev) => prev.filter((item) => item.id !== id));
    }, 860);
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const { gained, isCritical } = onTapCrystal();

    if (isVibrationEnabled && typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(isCritical ? [18, 20, 26] : 12);
    }

    scaleRef.current = isCritical ? 0.79 : 0.85;
    scaleVelocityRef.current = 0;

    if (isCritical) {
      flashAlphaRef.current = 0.75;
      shakeRef.current = { x: 0, y: 0, time: 0.18 };
    }

    const count = isCritical ? 14 : 6;
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count + Math.random() * 0.4;
      const speed = (isCritical ? 145 : 75) + Math.random() * 75;
      particlesRef.current.push({
        x,
        y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        life: 0,
        maxLife: 0.45 + Math.random() * 0.25,
        color: isCritical ? '#FBBF24' : i % 2 === 0 ? '#22D3EE' : '#A78BFA',
        size: isCritical ? 3.2 : 2.1,
      });
    }

    const label = isCritical
      ? `+${formatRate(gained)} КРИТ!`
      : `+${formatRate(gained)}`;
    spawnFloatingLabel(label, x, y, isCritical);
  };

  const handleCometClick = (e: React.PointerEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    const bonus = onCatchComet();
    if (bonus <= 0) return;
    if (isVibrationEnabled && typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate([20, 30, 30]);
    }
    spawnFloatingLabel(`КОМЕТА! +${formatCrystalCount(bonus)} · x5`, 135, 60, true, true);
  };

  return (
    <div className="relative flex flex-col items-center justify-center select-none py-2">
      {overchargeSecondsLeft > 0 && (
        <div className="mb-1 px-3 py-1 rounded-xl bg-amber-400/15 border border-amber-400/40 text-xs font-mono-num font-bold text-amber-300">
          Перегрузка x5 · {overchargeSecondsLeft}с
        </div>
      )}

      <div
        ref={containerRef}
        role="button"
        tabIndex={0}
        aria-label="Добыть космический кристалл"
        onPointerDown={handlePointerDown}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            const { gained, isCritical } = onTapCrystal();
            scaleRef.current = 0.84;
            if (isCritical) {
              flashAlphaRef.current = 0.75;
              shakeRef.current = { x: 0, y: 0, time: 0.18 };
            }
            const label = isCritical
              ? `+${formatRate(gained)} КРИТ!`
              : `+${formatRate(gained)}`;
            spawnFloatingLabel(label, 135, 110, isCritical);
          }
        }}
        className="relative w-[270px] h-[270px] cursor-pointer touch-manipulation rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-400 overflow-visible"
      >
        <canvas
          ref={canvasRef}
          className="w-[270px] h-[270px] block pointer-events-none"
        />

        {isCometVisible && (
          <button
            type="button"
            onPointerDown={handleCometClick}
            aria-label="Поймать космическую комету"
            style={{
              left: `${cometPos.x}px`,
              top: `${cometPos.y}px`,
            }}
            className="absolute z-20 -translate-x-1/2 -translate-y-1/2 w-14 h-14 flex items-center justify-center cursor-pointer group"
          >
            <span
              className={`absolute w-10 h-1.5 rounded-full bg-gradient-to-r from-amber-300 to-transparent blur-[1px] ${
                cometPos.fromLeft ? '-left-5 rotate-12' : '-right-5 -rotate-12 rotate-180'
              }`}
            />
            <span className="w-6 h-6 rounded-full bg-amber-300 shadow-[0_0_20px_rgba(251,191,36,0.95)] flex items-center justify-center group-hover:scale-110 transition-transform">
              <span className="w-3 h-3 rounded-full bg-white" />
            </span>
          </button>
        )}

        {floatingNumbers.map((item) => (
          <span
            key={item.id}
            style={{ left: item.x, top: item.y }}
            className={`pointer-events-none absolute font-display font-extrabold whitespace-nowrap animate-float-up drop-shadow-[0_2px_10px_rgba(0,0,0,0.8)] ${
              item.isComet
                ? 'text-lg text-amber-200'
                : item.isCritical
                ? 'text-xl text-amber-300'
                : 'text-base text-cyan-300'
            }`}
          >
            {item.text}
          </span>
        ))}
      </div>

      <p className="text-xs text-slate-400 mt-1">
        Нажмите на кристалл · Ловите кометы для x5
      </p>
    </div>
  );
};
