import React, { useEffect, useRef, useState } from 'react';
import { FloatingNumber, formatRate } from '../types/game';

interface CrystalCanvasProps {
  onTapCrystal: () => { gained: number; isCritical: boolean };
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
  onTapCrystal,
  isVibrationEnabled,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const scaleRef = useRef<number>(1);
  const scaleVelocityRef = useRef<number>(0);
  const particlesRef = useRef<BurstParticle[]>([]);
  const [floatingNumbers, setFloatingNumbers] = useState<FloatingNumber[]>([]);
  const nextIdRef = useRef<number>(1);

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

      // Spring physics for scale bounce
      const stiffness = 260;
      const damping = 16;
      const force = (1 - scaleRef.current) * stiffness;
      scaleVelocityRef.current += force * dt;
      scaleVelocityRef.current *= Math.exp(-damping * dt);
      scaleRef.current += scaleVelocityRef.current * dt;

      const cx = logicalSize / 2;
      const cy = logicalSize / 2;
      const t = now * 0.001;
      const pulse = 1 + Math.sin(t * 2.2) * 0.04;
      const baseR = 96;

      // Ambient cosmic glow
      const glow = ctx.createRadialGradient(cx, cy, 8, cx, cy, baseR * 1.4 * pulse);
      glow.addColorStop(0, 'rgba(139, 92, 246, 0.42)');
      glow.addColorStop(0.55, 'rgba(34, 211, 238, 0.16)');
      glow.addColorStop(1, 'rgba(6, 8, 20, 0)');
      ctx.fillStyle = glow;
      ctx.beginPath();
      ctx.arc(cx, cy, baseR * 1.4 * pulse, 0, Math.PI * 2);
      ctx.fill();

      // Rotating outer orbital ring
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(t * 0.55);
      const ringGrad = ctx.createLinearGradient(-baseR, -baseR, baseR, baseR);
      ringGrad.addColorStop(0, 'rgba(34, 211, 238, 0.85)');
      ringGrad.addColorStop(0.5, 'rgba(139, 92, 246, 0.15)');
      ringGrad.addColorStop(1, 'rgba(167, 139, 250, 0.85)');
      ctx.strokeStyle = ringGrad;
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      ctx.arc(0, 0, baseR * 1.06, 0, Math.PI * 2);
      ctx.stroke();

      // Orbiting energy nodes
      const nodeX = Math.cos(0.6) * baseR * 1.06;
      const nodeY = Math.sin(0.6) * baseR * 1.06;
      ctx.fillStyle = '#22D3EE';
      ctx.beginPath();
      ctx.arc(nodeX, nodeY, 4.5, 0, Math.PI * 2);
      ctx.fill();

      const node2X = Math.cos(3.74) * baseR * 1.06;
      const node2Y = Math.sin(3.74) * baseR * 1.06;
      ctx.fillStyle = '#FBBF24';
      ctx.beginPath();
      ctx.arc(node2X, node2Y, 3.2, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

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

      // Main crystal fill
      const bodyGrad = ctx.createLinearGradient(top.x, top.y, bottom.x, bottom.y);
      bodyGrad.addColorStop(0, '#67E8F9');
      bodyGrad.addColorStop(0.45, '#8B5CF6');
      bodyGrad.addColorStop(1, '#3B0764');

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

      // Bright left facet
      ctx.beginPath();
      ctx.moveTo(top.x, top.y);
      ctx.lineTo(core.x, core.y);
      ctx.lineTo(lowerLeft.x, lowerLeft.y);
      ctx.lineTo(upperLeft.x, upperLeft.y);
      ctx.closePath();
      ctx.fillStyle = 'rgba(34, 211, 238, 0.25)';
      ctx.fill();

      // Deep shadow right facet
      ctx.beginPath();
      ctx.moveTo(core.x, core.y);
      ctx.lineTo(upperRight.x, upperRight.y);
      ctx.lineTo(lowerRight.x, lowerRight.y);
      ctx.lineTo(bottom.x, bottom.y);
      ctx.closePath();
      ctx.fillStyle = 'rgba(15, 23, 42, 0.38)';
      ctx.fill();

      // Inner facet lines
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.38)';
      ctx.lineWidth = 1.4;
      [top, upperLeft, upperRight, lowerLeft, lowerRight, bottom].forEach((pt) => {
        ctx.beginPath();
        ctx.moveTo(core.x, core.y);
        ctx.lineTo(pt.x, pt.y);
        ctx.stroke();
      });

      // Outer neon stroke
      ctx.beginPath();
      ctx.moveTo(top.x, top.y);
      ctx.lineTo(upperRight.x, upperRight.y);
      ctx.lineTo(lowerRight.x, lowerRight.y);
      ctx.lineTo(bottom.x, bottom.y);
      ctx.lineTo(lowerLeft.x, lowerLeft.y);
      ctx.lineTo(upperLeft.x, upperLeft.y);
      ctx.closePath();
      ctx.strokeStyle = 'rgba(103, 232, 249, 0.85)';
      ctx.lineWidth = 2.2;
      ctx.stroke();

      ctx.restore();

      // Update & draw tap burst particles
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

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const { gained, isCritical } = onTapCrystal();

    if (isVibrationEnabled && typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(isCritical ? [18, 20, 24] : 12);
    }

    // Trigger scale squish
    scaleRef.current = 0.84;
    scaleVelocityRef.current = 0;

    // Spawn burst particles at tap coordinate
    const count = isCritical ? 14 : 7;
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count + Math.random() * 0.4;
      const speed = (isCritical ? 140 : 85) + Math.random() * 80;
      particlesRef.current.push({
        x,
        y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        life: 0,
        maxLife: 0.45 + Math.random() * 0.25,
        color: isCritical ? '#FBBF24' : i % 2 === 0 ? '#22D3EE' : '#A78BFA',
        size: isCritical ? 3.2 : 2.2,
      });
    }

    const id = nextIdRef.current++;
    const label = isCritical
      ? `+${formatRate(gained)} КРИТ!`
      : `+${formatRate(gained)}`;

    setFloatingNumbers((prev) => [
      ...prev.slice(-14),
      { id, text: label, x, y, isCritical },
    ]);

    window.setTimeout(() => {
      setFloatingNumbers((prev) => prev.filter((item) => item.id !== id));
    }, 860);
  };

  return (
    <div className="relative flex flex-col items-center justify-center select-none py-2">
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
            const id = nextIdRef.current++;
            const label = isCritical
              ? `+${formatRate(gained)} КРИТ!`
              : `+${formatRate(gained)}`;
            setFloatingNumbers((prev) => [
              ...prev.slice(-14),
              { id, text: label, x: 135, y: 110, isCritical },
            ]);
            window.setTimeout(() => {
              setFloatingNumbers((prev) => prev.filter((item) => item.id !== id));
            }, 860);
          }
        }}
        className="relative w-[270px] h-[270px] cursor-pointer touch-manipulation rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-400"
      >
        <canvas
          ref={canvasRef}
          className="w-[270px] h-[270px] block pointer-events-none"
        />

        {floatingNumbers.map((item) => (
          <span
            key={item.id}
            style={{ left: item.x, top: item.y }}
            className={`pointer-events-none absolute font-display font-extrabold whitespace-nowrap animate-float-up drop-shadow-[0_2px_10px_rgba(0,0,0,0.8)] ${
              item.isCritical
                ? 'text-xl text-amber-300'
                : 'text-base text-cyan-300'
            }`}
          >
            {item.text}
          </span>
        ))}
      </div>
      <p className="text-xs text-slate-400 mt-1">
        Нажмите на кристалл · 10% шанс крита x3
      </p>
    </div>
  );
};
