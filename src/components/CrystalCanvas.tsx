import React, { useEffect, useRef, useState } from 'react';
import {
  BossUiState,
  CrystalSkinId,
  FloatingNumber,
  SpaceLocationId,
  formatCrystalCount,
  formatRate,
} from '../types/game';

interface CrystalCanvasProps {
  rankLevel: number;
  overchargeSecondsLeft: number;
  feverMeter: number;
  feverSecondsLeft: number;
  comboCount: number;
  isCometVisible: boolean;
  cometSeed: number;
  visualDroneCount: number;
  selectedSkinId: CrystalSkinId;
  selectedLocationId: SpaceLocationId;
  bossState: BossUiState;
  onTapCrystal: () => { gained: number; isCritical: boolean; isFever: boolean };
  onCatchComet: () => number;
  isVibrationEnabled: boolean;
}

type ParticleStyle = 'SPARK' | 'INWARD_VOID' | 'MATRIX_BIT' | 'SNOWFLAKE';

interface BurstParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  tx?: number;
  ty?: number;
  life: number;
  maxLife: number;
  color: string;
  size: number;
  style: ParticleStyle;
}

interface BgStar {
  xFrac: number;
  yFrac: number;
  size: number;
  depth: number;
  alpha: number;
  tint: string;
}

const ARENA_W = 320;
const ARENA_H = 300;
const CRYSTAL_CY = 178;

export const CrystalCanvas: React.FC<CrystalCanvasProps> = ({
  rankLevel,
  overchargeSecondsLeft,
  feverMeter,
  feverSecondsLeft,
  comboCount,
  isCometVisible,
  cometSeed,
  visualDroneCount,
  selectedSkinId,
  selectedLocationId,
  bossState,
  onTapCrystal,
  onCatchComet,
  isVibrationEnabled,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const scaleRef = useRef<number>(1);
  const scaleVelocityRef = useRef<number>(0);
  const flashAlphaRef = useRef<number>(0);
  const shakeRef = useRef<{ time: number }>({ time: 0 });
  const particlesRef = useRef<BurstParticle[]>([]);
  const rainbowHueRef = useRef<number>(190);
  const tiltRef = useRef<{ x: number; y: number; targetX: number; targetY: number }>({
    x: 0,
    y: 0,
    targetX: 0,
    targetY: 0,
  });

  const [floatingNumbers, setFloatingNumbers] = useState<FloatingNumber[]>([]);
  const [cometPos, setCometPos] = useState<{ x: number; y: number; fromLeft: boolean }>({
    x: -50,
    y: 28,
    fromLeft: true,
  });
  const nextIdRef = useRef<number>(1);

  const starsRef = useRef<BgStar[]>(
    Array.from({ length: 42 }, (_, i) => ({
      xFrac: ((i * 73 + 19) % 100) / 100,
      yFrac: ((i * 41 + 37) % 100) / 100,
      size: 0.9 + (i % 3) * 0.65,
      depth: 0.4 + (i % 4) * 0.35,
      alpha: 0.3 + (i % 5) * 0.14,
      tint: i % 5 === 0 ? '#67E8F9' : i % 3 === 0 ? '#C4B5FD' : '#FFFFFF',
    }))
  );

  // Latest state refs for smooth 60fps canvas loop
  const propsRef = useRef({
    rankLevel,
    overchargeSecondsLeft,
    feverMeter,
    feverSecondsLeft,
    visualDroneCount,
    selectedSkinId,
    selectedLocationId,
    bossIsActive: bossState.isActive,
  });
  propsRef.current = {
    rankLevel,
    overchargeSecondsLeft,
    feverMeter,
    feverSecondsLeft,
    visualDroneCount,
    selectedSkinId,
    selectedLocationId,
    bossIsActive: bossState.isActive,
  };

  // Device orientation & pointer parallax
  useEffect(() => {
    const handleOrientation = (event: DeviceOrientationEvent) => {
      if (event.gamma === null || event.beta === null) return;
      const gx = Math.max(-1, Math.min(1, event.gamma / 35));
      const gy = Math.max(-1, Math.min(1, (event.beta - 35) / 35));
      tiltRef.current.targetX = gx * 18;
      tiltRef.current.targetY = gy * 18;
    };
    window.addEventListener('deviceorientation', handleOrientation);
    return () => window.removeEventListener('deviceorientation', handleOrientation);
  }, []);

  // Dedicated TOP-LANE Comet trajectory (well above crystal tap zone)
  useEffect(() => {
    if (!isCometVisible) return;
    const fromLeft = cometSeed % 2 === 0;
    const laneY = 26 + (cometSeed % 16); // Strict top lane 26..42px
    const start = performance.now();
    const duration = 6200;
    let frameId = 0;

    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      const x = fromLeft ? -24 + t * (ARENA_W + 48) : ARENA_W + 24 - t * (ARENA_W + 48);
      const y = laneY + Math.sin(t * Math.PI * 2) * 5;
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

    const drawHexCrystal = (
      x: number,
      y: number,
      baseR: number,
      topHex: string,
      midHex: string,
      botHex: string,
      edgeHex: string,
      facetLeftHex: string,
      lineHex: string
    ) => {
      const top = { x: 0, y: -baseR * 0.86 };
      const ur = { x: baseR * 0.56, y: -baseR * 0.26 };
      const lr = { x: baseR * 0.46, y: baseR * 0.44 };
      const bot = { x: 0, y: baseR * 0.88 };
      const ll = { x: -baseR * 0.46, y: baseR * 0.44 };
      const ul = { x: -baseR * 0.56, y: -baseR * 0.26 };
      const core = { x: 0, y: -baseR * 0.04 };

      ctx.save();
      ctx.translate(x, y);
      const bodyGrad = ctx.createLinearGradient(0, top.y, 0, bot.y);
      bodyGrad.addColorStop(0, topHex);
      bodyGrad.addColorStop(0.48, midHex);
      bodyGrad.addColorStop(1, botHex);

      ctx.beginPath();
      ctx.moveTo(top.x, top.y);
      ctx.lineTo(ur.x, ur.y);
      ctx.lineTo(lr.x, lr.y);
      ctx.lineTo(bot.x, bot.y);
      ctx.lineTo(ll.x, ll.y);
      ctx.lineTo(ul.x, ul.y);
      ctx.closePath();
      ctx.fillStyle = bodyGrad;
      ctx.fill();

      // Left highlight facet
      ctx.beginPath();
      ctx.moveTo(top.x, top.y);
      ctx.lineTo(core.x, core.y);
      ctx.lineTo(ll.x, ll.y);
      ctx.lineTo(ul.x, ul.y);
      ctx.closePath();
      ctx.fillStyle = facetLeftHex;
      ctx.fill();

      // Right shadow facet
      ctx.beginPath();
      ctx.moveTo(core.x, core.y);
      ctx.lineTo(ur.x, ur.y);
      ctx.lineTo(lr.x, lr.y);
      ctx.lineTo(bot.x, bot.y);
      ctx.closePath();
      ctx.fillStyle = 'rgba(15, 23, 42, 0.36)';
      ctx.fill();

      // Inner facet lines
      ctx.strokeStyle = lineHex;
      ctx.lineWidth = 1.4;
      [top, ul, ur, ll, lr, bot].forEach((pt) => {
        ctx.beginPath();
        ctx.moveTo(core.x, core.y);
        ctx.lineTo(pt.x, pt.y);
        ctx.stroke();
      });

      // Outer edge
      ctx.beginPath();
      ctx.moveTo(top.x, top.y);
      ctx.lineTo(ur.x, ur.y);
      ctx.lineTo(lr.x, lr.y);
      ctx.lineTo(bot.x, bot.y);
      ctx.lineTo(ll.x, ll.y);
      ctx.lineTo(ul.x, ul.y);
      ctx.closePath();
      ctx.strokeStyle = edgeHex;
      ctx.lineWidth = 2.4;
      ctx.stroke();
      ctx.restore();
    };

    const render = (now: number) => {
      const dt = Math.min((now - startTime) / 1000, 0.05);
      startTime = now;

      const dpr = window.devicePixelRatio || 1;
      if (canvas.width !== ARENA_W * dpr || canvas.height !== ARENA_H * dpr) {
        canvas.width = ARENA_W * dpr;
        canvas.height = ARENA_H * dpr;
      }

      const {
        rankLevel: lvl,
        overchargeSecondsLeft: overchargeSec,
        feverMeter: fMeter,
        feverSecondsLeft: fSec,
        visualDroneCount: drones,
        selectedSkinId: skinId,
        selectedLocationId: locId,
        bossIsActive,
      } = propsRef.current;

      // Smooth parallax interpolation
      tiltRef.current.x += (tiltRef.current.targetX - tiltRef.current.x) * 0.12;
      tiltRef.current.y += (tiltRef.current.targetY - tiltRef.current.y) * 0.12;

      ctx.save();
      ctx.scale(dpr, dpr);
      ctx.clearRect(0, 0, ARENA_W, ARENA_H);

      // Shake decay
      let sx = 0;
      let sy = 0;
      if (shakeRef.current.time > 0) {
        shakeRef.current.time -= dt;
        const mag = (shakeRef.current.time / 0.18) * 5.5;
        sx = (Math.random() - 0.5) * 2 * mag;
        sy = (Math.random() - 0.5) * 2 * mag;
      }

      if (flashAlphaRef.current > 0) {
        flashAlphaRef.current = Math.max(0, flashAlphaRef.current - dt * 3.4);
      }

      // Spring physics
      const stiffness = 260;
      const damping = 16;
      const force = (1 - scaleRef.current) * stiffness;
      scaleVelocityRef.current += force * dt;
      scaleVelocityRef.current *= Math.exp(-damping * dt);
      scaleRef.current += scaleVelocityRef.current * dt;

      const t = now * 0.001;
      const cx = ARENA_W / 2 + sx;
      const cy = CRYSTAL_CY + sy;
      const baseR = 76;
      const pulse = 1 + Math.sin(t * 2.2) * 0.04;
      const isOvercharged = overchargeSec > 0;
      const isFever = fSec > 0;
      const evolutionStage = lvl >= 150 ? 4 : lvl >= 100 ? 3 : lvl >= 50 ? 2 : 1;

      // 1. Space Location Background & Parallax Starfield
      const locCenter = { x: cx + tiltRef.current.x * 0.6, y: cy + tiltRef.current.y * 0.6 };
      if (locId === 'nebula') {
        const g = ctx.createRadialGradient(locCenter.x, locCenter.y, 12, locCenter.x, locCenter.y, 185);
        g.addColorStop(0, 'rgba(139, 92, 246, 0.24)');
        g.addColorStop(0.55, 'rgba(34, 211, 238, 0.09)');
        g.addColorStop(1, 'rgba(6, 8, 20, 0)');
        ctx.fillStyle = g;
        ctx.fillRect(0, 0, ARENA_W, ARENA_H);
      } else if (locId === 'red_giant') {
        const g = ctx.createRadialGradient(
          locCenter.x - 30,
          locCenter.y - 20,
          14,
          locCenter.x,
          locCenter.y,
          190
        );
        g.addColorStop(0, 'rgba(239, 68, 68, 0.30)');
        g.addColorStop(0.5, 'rgba(249, 115, 22, 0.12)');
        g.addColorStop(1, 'rgba(6, 8, 20, 0)');
        ctx.fillStyle = g;
        ctx.fillRect(0, 0, ARENA_W, ARENA_H);
      } else if (locId === 'asteroid_belt') {
        const g = ctx.createRadialGradient(locCenter.x, locCenter.y, 10, locCenter.x, locCenter.y, 185);
        g.addColorStop(0, 'rgba(20, 184, 166, 0.18)');
        g.addColorStop(0.6, 'rgba(56, 189, 248, 0.07)');
        g.addColorStop(1, 'rgba(6, 8, 20, 0)');
        ctx.fillStyle = g;
        ctx.fillRect(0, 0, ARENA_W, ARENA_H);

        // Drifting background asteroids
        for (let i = 0; i < 6; i++) {
          const ax = ((i * 68 + t * (9 + i * 2)) % (ARENA_W + 40)) - 20 + tiltRef.current.x * 0.4;
          const ay = 55 + ((i * 47) % (ARENA_H - 90)) + tiltRef.current.y * 0.4;
          ctx.save();
          ctx.translate(ax, ay);
          ctx.rotate(t * 0.4 + i);
          ctx.fillStyle = 'rgba(148, 163, 184, 0.18)';
          ctx.strokeStyle = 'rgba(148, 163, 184, 0.28)';
          ctx.lineWidth = 1;
          ctx.beginPath();
          const r = 6 + (i % 3) * 3;
          for (let v = 0; v < 6; v++) {
            const ang = (Math.PI * 2 * v) / 6;
            const vr = r * (v % 2 === 0 ? 1 : 0.72);
            const vx = Math.cos(ang) * vr;
            const vy = Math.sin(ang) * vr;
            if (v === 0) ctx.moveTo(vx, vy);
            else ctx.lineTo(vx, vy);
          }
          ctx.closePath();
          ctx.fill();
          ctx.stroke();
          ctx.restore();
        }
      } else if (locId === 'deep_void') {
        // Periodic shooting stars in Deep Void
        const cycle = (t * 0.45) % 1;
        if (cycle < 0.35) {
          const p = cycle / 0.35;
          const sx0 = 40 + p * 220;
          const sy0 = 45 + p * 90;
          const grad = ctx.createLinearGradient(sx0 - 34, sy0 - 14, sx0, sy0);
          grad.addColorStop(0, 'rgba(255,255,255,0)');
          grad.addColorStop(1, 'rgba(186,230,253,0.85)');
          ctx.strokeStyle = grad;
          ctx.lineWidth = 1.6;
          ctx.beginPath();
          ctx.moveTo(sx0 - 34, sy0 - 14);
          ctx.lineTo(sx0, sy0);
          ctx.stroke();
        }
      }

      // Parallax stars
      starsRef.current.forEach((star, idx) => {
        const px =
          (star.xFrac * ARENA_W + tiltRef.current.x * star.depth + ARENA_W) % ARENA_W;
        const py =
          (star.yFrac * ARENA_H + tiltRef.current.y * star.depth + ARENA_H) % ARENA_H;
        const twinkle = 0.65 + 0.35 * Math.sin(t * 2.5 + idx);
        ctx.globalAlpha = star.alpha * twinkle;
        ctx.fillStyle =
          locId === 'red_giant' && idx % 3 === 0 ? '#FCA5A5' : star.tint;
        ctx.beginPath();
        ctx.arc(px, py, star.size, 0, Math.PI * 2);
        ctx.fill();
      });
      ctx.globalAlpha = 1;

      // 2. Fever Heat Circular Ring around crystal
      const ringR = baseR * 1.28;
      ctx.beginPath();
      ctx.arc(cx, cy, ringR, 0, Math.PI * 2);
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.07)';
      ctx.lineWidth = 4;
      ctx.stroke();

      if (fMeter > 0.01) {
        const startAng = -Math.PI / 2;
        const endAng = startAng + Math.PI * 2 * Math.min(1, fMeter);
        ctx.beginPath();
        ctx.arc(cx, cy, ringR, startAng, endAng);
        ctx.strokeStyle = isFever ? '#FBBF24' : '#22D3EE';
        ctx.lineWidth = isFever ? 5 : 4;
        ctx.lineCap = 'round';
        ctx.stroke();
        ctx.lineCap = 'butt';
      }

      // 3. Ambient Aura Glow
      const auraGlow = ctx.createRadialGradient(cx, cy, 8, cx, cy, baseR * 1.48 * pulse);
      if (isFever || isOvercharged) {
        auraGlow.addColorStop(0, 'rgba(251, 191, 36, 0.56)');
        auraGlow.addColorStop(0.55, 'rgba(249, 115, 22, 0.20)');
        auraGlow.addColorStop(1, 'rgba(6, 8, 20, 0)');
      } else if (bossIsActive) {
        auraGlow.addColorStop(0, 'rgba(239, 68, 68, 0.52)');
        auraGlow.addColorStop(0.6, 'rgba(127, 29, 29, 0.18)');
        auraGlow.addColorStop(1, 'rgba(6, 8, 20, 0)');
      } else {
        auraGlow.addColorStop(0, 'rgba(139, 92, 246, 0.42)');
        auraGlow.addColorStop(0.55, 'rgba(34, 211, 238, 0.15)');
        auraGlow.addColorStop(1, 'rgba(6, 8, 20, 0)');
      }
      ctx.fillStyle = auraGlow;
      ctx.beginPath();
      ctx.arc(cx, cy, baseR * 1.48 * pulse, 0, Math.PI * 2);
      ctx.fill();

      // Orbital ring (classic & evolution stage)
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(t * 0.55);
      ctx.strokeStyle = isFever || isOvercharged
        ? 'rgba(251, 191, 36, 0.78)'
        : 'rgba(34, 211, 238, 0.55)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(0, 0, baseR * 1.1, 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();

      if (evolutionStage >= 2) {
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(-t * 0.72);
        ctx.strokeStyle = 'rgba(251, 191, 36, 0.48)';
        ctx.lineWidth = 1.6;
        ctx.beginPath();
        ctx.ellipse(0, 0, baseR * 1.24, baseR * 0.82, 0, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();
      }

      // 4. Orbiting Drones + Periodic Laser Beams
      if (drones > 0) {
        const orbitR = baseR * 1.38;
        for (let i = 0; i < drones; i++) {
          const ang = t * (0.85 + (i % 3) * 0.18) + (Math.PI * 2 * i) / drones;
          const dx = cx + Math.cos(ang) * orbitR;
          const dy = cy + Math.sin(ang) * orbitR * 0.84;

          // Periodic laser burst
          const beamPhase = (t * 1.3 + i * 0.73) % 2.4;
          if (beamPhase < 0.22) {
            ctx.strokeStyle = bossIsActive
              ? 'rgba(248, 113, 113, 0.85)'
              : 'rgba(34, 211, 238, 0.78)';
            ctx.lineWidth = 1.6;
            ctx.beginPath();
            ctx.moveTo(dx, dy);
            ctx.lineTo(cx, cy);
            ctx.stroke();
          }

          ctx.fillStyle = i % 2 === 0 ? '#22D3EE' : '#A78BFA';
          ctx.beginPath();
          ctx.arc(dx, dy, 3.6, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#FFFFFF';
          ctx.beginPath();
          ctx.arc(dx, dy, 1.4, 0, Math.PI * 2);
          ctx.fill();
        }
      }

      // 5. Center Object: Boss Asteroid OR Chosen Crystal Skin
      const bobY = Math.sin(t * 1.8) * 2.8;
      ctx.save();
      ctx.translate(cx, cy + bobY);
      ctx.scale(scaleRef.current, scaleRef.current);

      if (bossIsActive) {
        // Armored Leviathan Boss Asteroid
        ctx.rotate(t * 0.28);
        const verts = 9;
        ctx.beginPath();
        for (let i = 0; i < verts; i++) {
          const ang = (Math.PI * 2 * i) / verts;
          const rad = baseR * (i % 2 === 0 ? 0.92 : 0.76);
          const vx = Math.cos(ang) * rad;
          const vy = Math.sin(ang) * rad;
          if (i === 0) ctx.moveTo(vx, vy);
          else ctx.lineTo(vx, vy);
        }
        ctx.closePath();
        const bGrad = ctx.createRadialGradient(-14, -14, 8, 0, 0, baseR);
        bGrad.addColorStop(0, '#FCA5A5');
        bGrad.addColorStop(0.45, '#DC2626');
        bGrad.addColorStop(1, '#450A0A');
        ctx.fillStyle = bGrad;
        ctx.fill();
        ctx.strokeStyle = '#FBBF24';
        ctx.lineWidth = 2.6;
        ctx.stroke();

        // Glowing core crack
        ctx.fillStyle = 'rgba(251, 191, 36, 0.75)';
        ctx.beginPath();
        ctx.arc(0, 0, baseR * 0.24, 0, Math.PI * 2);
        ctx.fill();
      } else if (skinId === 'black_hole') {
        // Rotating accretion disk
        ctx.save();
        ctx.rotate(t * 1.15);
        for (let ring = 0; ring < 2; ring++) {
          ctx.strokeStyle = ring === 0 ? 'rgba(251, 191, 36, 0.85)' : 'rgba(167, 139, 250, 0.65)';
          ctx.lineWidth = ring === 0 ? 5 : 3;
          ctx.beginPath();
          ctx.ellipse(0, 0, baseR * (1.02 - ring * 0.14), baseR * (0.44 - ring * 0.08), ring * 0.35, 0, Math.PI * 2);
          ctx.stroke();
        }
        ctx.restore();

        // Photon sphere halo + Pitch black core
        const bhRim = ctx.createRadialGradient(0, 0, baseR * 0.4, 0, 0, baseR * 0.74);
        bhRim.addColorStop(0, '#020206');
        bhRim.addColorStop(0.74, '#090514');
        bhRim.addColorStop(0.92, 'rgba(251, 191, 36, 0.85)');
        bhRim.addColorStop(1, 'rgba(139, 92, 246, 0)');
        ctx.fillStyle = bhRim;
        ctx.beginPath();
        ctx.arc(0, 0, baseR * 0.74, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#030308';
        ctx.beginPath();
        ctx.arc(0, 0, baseR * 0.5, 0, Math.PI * 2);
        ctx.fill();
      } else if (skinId === 'solar_core') {
        // Pulsing corona rays
        ctx.save();
        ctx.rotate(t * 0.45);
        for (let i = 0; i < 12; i++) {
          const ang = (Math.PI * 2 * i) / 12;
          const rayLen = baseR * (0.94 + Math.sin(t * 4 + i) * 0.12);
          ctx.strokeStyle = 'rgba(251, 191, 36, 0.65)';
          ctx.lineWidth = 3;
          ctx.beginPath();
          ctx.moveTo(Math.cos(ang) * baseR * 0.56, Math.sin(ang) * baseR * 0.56);
          ctx.lineTo(Math.cos(ang) * rayLen, Math.sin(ang) * rayLen);
          ctx.stroke();
        }
        ctx.restore();

        const sunGrad = ctx.createRadialGradient(-10, -10, 6, 0, 0, baseR * 0.72);
        sunGrad.addColorStop(0, '#FEF9C3');
        sunGrad.addColorStop(0.45, '#FBBF24');
        sunGrad.addColorStop(0.85, '#EA580C');
        sunGrad.addColorStop(1, '#9A3412');
        ctx.fillStyle = sunGrad;
        ctx.beginPath();
        ctx.arc(0, 0, baseR * 0.72, 0, Math.PI * 2);
        ctx.fill();
      } else if (skinId === 'cyber_cube') {
        // Rotating neon Matrix tesseract
        ctx.save();
        ctx.rotate(t * 0.7);
        const sOuter = baseR * 0.58;
        const sInner = baseR * 0.31;
        ctx.strokeStyle = '#34D399';
        ctx.lineWidth = 2.4;
        ctx.fillStyle = 'rgba(6, 78, 59, 0.55)';
        ctx.fillRect(-sOuter, -sOuter, sOuter * 2, sOuter * 2);
        ctx.strokeRect(-sOuter, -sOuter, sOuter * 2, sOuter * 2);

        ctx.rotate(-t * 1.1);
        ctx.strokeStyle = '#6EE7B7';
        ctx.lineWidth = 1.8;
        ctx.strokeRect(-sInner, -sInner, sInner * 2, sInner * 2);

        // Diagonal tesseract links
        ctx.strokeStyle = 'rgba(52, 211, 153, 0.55)';
        [
          [-1, -1],
          [1, -1],
          [1, 1],
          [-1, 1],
        ].forEach(([dx, dy]) => {
          ctx.beginPath();
          ctx.moveTo(dx * sInner, dy * sInner);
          ctx.lineTo(dx * sOuter * 0.9, dy * sOuter * 0.9);
          ctx.stroke();
        });
        ctx.restore();
      } else if (skinId === 'ice_relic') {
        drawHexCrystal(
          0,
          0,
          baseR,
          '#E0F2FE',
          '#38BDF8',
          '#0369A1',
          '#BAE6FD',
          'rgba(224, 242, 254, 0.38)',
          'rgba(255, 255, 255, 0.65)'
        );
      } else if (skinId === 'rainbow_prism') {
        const h = rainbowHueRef.current;
        drawHexCrystal(
          0,
          0,
          baseR,
          `hsl(${h}, 95%, 76%)`,
          `hsl(${(h + 55) % 360}, 88%, 56%)`,
          `hsl(${(h + 120) % 360}, 78%, 26%)`,
          `hsl(${(h + 30) % 360}, 95%, 72%)`,
          `hsla(${(h + 90) % 360}, 90%, 75%, 0.32)`,
          'rgba(255, 255, 255, 0.55)'
        );
      } else {
        // Classic skin with evolution colors
        if (isFever || isOvercharged) {
          drawHexCrystal(
            0,
            0,
            baseR,
            '#FEF08A',
            '#F59E0B',
            '#78350F',
            '#FBBF24',
            'rgba(254, 240, 138, 0.28)',
            'rgba(255, 255, 255, 0.45)'
          );
        } else if (evolutionStage >= 4) {
          drawHexCrystal(
            0,
            0,
            baseR,
            '#22D3EE',
            '#6D28D9',
            '#090518',
            '#FBBF24',
            'rgba(251, 191, 36, 0.22)',
            'rgba(251, 191, 36, 0.5)'
          );
        } else {
          drawHexCrystal(
            0,
            0,
            baseR,
            '#67E8F9',
            '#8B5CF6',
            '#3B0764',
            evolutionStage >= 2 ? 'rgba(251, 191, 36, 0.85)' : 'rgba(103, 232, 249, 0.88)',
            'rgba(34, 211, 238, 0.24)',
            'rgba(255, 255, 255, 0.38)'
          );
        }
      }
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

      // 6. Skin-specific Particle Physics
      const alive: BurstParticle[] = [];
      for (const p of particlesRef.current) {
        p.life += dt;
        if (p.life < p.maxLife) {
          const frac = p.life / p.maxLife;
          if (p.style === 'INWARD_VOID' && p.tx !== undefined && p.ty !== undefined) {
            p.x += (p.tx - p.x) * Math.min(1, dt * 6.5);
            p.y += (p.ty - p.y) * Math.min(1, dt * 6.5);
          } else {
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 0.94;
            p.vy *= 0.94;
          }
          ctx.globalAlpha = 1 - frac;
          ctx.fillStyle = p.color;

          if (p.style === 'MATRIX_BIT') {
            ctx.fillRect(p.x - p.size, p.y - p.size, p.size * 2, p.size * 2);
          } else if (p.style === 'SNOWFLAKE') {
            ctx.strokeStyle = p.color;
            ctx.lineWidth = 1.3;
            for (let k = 0; k < 3; k++) {
              const a = (Math.PI * k) / 3;
              ctx.beginPath();
              ctx.moveTo(p.x - Math.cos(a) * p.size * 1.4, p.y - Math.sin(a) * p.size * 1.4);
              ctx.lineTo(p.x + Math.cos(a) * p.size * 1.4, p.y + Math.sin(a) * p.size * 1.4);
              ctx.stroke();
            }
          } else {
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
          }
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

  const spawnFloatingLabel = (
    text: string,
    x: number,
    y: number,
    isCritical: boolean,
    isComet = false
  ) => {
    const id = nextIdRef.current++;
    setFloatingNumbers((prev) => [...prev.slice(-14), { id, text, x, y, isCritical, isComet }]);
    window.setTimeout(() => {
      setFloatingNumbers((prev) => prev.filter((item) => item.id !== id));
    }, 860);
  };

  const spawnParticlesForTap = (tapX: number, tapY: number, isCritical: boolean) => {
    const count = isCritical ? 14 : 7;
    if (selectedSkinId === 'black_hole') {
      // Pull particles inward from outer perimeter toward black hole center
      for (let i = 0; i < count; i++) {
        const ang = Math.random() * Math.PI * 2;
        const dist = 75 + Math.random() * 48;
        particlesRef.current.push({
          x: ARENA_W / 2 + Math.cos(ang) * dist,
          y: CRYSTAL_CY + Math.sin(ang) * dist,
          vx: 0,
          vy: 0,
          tx: ARENA_W / 2,
          ty: CRYSTAL_CY,
          life: 0,
          maxLife: 0.42 + Math.random() * 0.2,
          color: isCritical ? '#FBBF24' : '#C084FC',
          size: isCritical ? 3.2 : 2.3,
          style: 'INWARD_VOID',
        });
      }
      return;
    }

    const style: ParticleStyle =
      selectedSkinId === 'cyber_cube'
        ? 'MATRIX_BIT'
        : selectedSkinId === 'ice_relic'
        ? 'SNOWFLAKE'
        : 'SPARK';

    for (let i = 0; i < count; i++) {
      const ang = (Math.PI * 2 * i) / count + Math.random() * 0.4;
      const speed = (isCritical ? 145 : 78) + Math.random() * 72;
      let color = isCritical ? '#FBBF24' : i % 2 === 0 ? '#22D3EE' : '#A78BFA';
      if (selectedSkinId === 'cyber_cube') color = isCritical ? '#FBBF24' : '#34D399';
      else if (selectedSkinId === 'ice_relic') color = isCritical ? '#FBBF24' : '#BAE6FD';
      else if (selectedSkinId === 'solar_core') color = isCritical ? '#FEF08A' : '#FB923C';
      else if (selectedSkinId === 'rainbow_prism') {
        color = `hsl(${(rainbowHueRef.current + i * 32) % 360}, 95%, 65%)`;
      }

      particlesRef.current.push({
        x: tapX,
        y: tapY,
        vx: Math.cos(ang) * speed,
        vy: Math.sin(ang) * speed,
        life: 0,
        maxLife: 0.45 + Math.random() * 0.24,
        color,
        size: isCritical ? 3.2 : 2.2,
        style,
      });
    }
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    const nx = ((e.clientX - rect.left) / rect.width - 0.5) * 2;
    const ny = ((e.clientY - rect.top) / rect.height - 0.5) * 2;
    tiltRef.current.targetX = nx * 16;
    tiltRef.current.targetY = ny * 16;
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    // Ignore taps in top comet lane so players can't miss-tap crystal into sky lane
    if (y < 52) return;

    if (selectedSkinId === 'rainbow_prism') {
      rainbowHueRef.current = (rainbowHueRef.current + 36) % 360;
    }

    const { gained, isCritical, isFever } = onTapCrystal();

    if (isVibrationEnabled && typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(isCritical ? [18, 20, 26] : 12);
    }

    scaleRef.current = isCritical ? 0.79 : 0.85;
    scaleVelocityRef.current = 0;

    if (isCritical) {
      flashAlphaRef.current = 0.75;
      shakeRef.current = { time: 0.18 };
    }

    spawnParticlesForTap(x, y, isCritical);

    const label = isFever
      ? `+${formatRate(gained)} ЯРОСТЬ!`
      : isCritical
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
    spawnFloatingLabel(`КОМЕТА! +${formatCrystalCount(bonus)} · x5`, 160, 44, true, true);
  };

  return (
    <div className="relative flex flex-col items-center justify-center select-none py-1">
      {/* Status Pills */}
      <div className="flex flex-wrap items-center justify-center gap-2 min-h-[26px] mb-1">
        {overchargeSecondsLeft > 0 && (
          <div className="px-2.5 py-0.5 rounded-lg bg-amber-400/15 border border-amber-400/40 text-[11px] font-mono-num font-bold text-amber-300">
            Перегрузка x5 · {overchargeSecondsLeft}с
          </div>
        )}
        {feverSecondsLeft > 0 ? (
          <div className="px-2.5 py-0.5 rounded-lg bg-orange-500/20 border border-amber-300/60 text-[11px] font-mono-num font-bold text-amber-200 animate-pulse">
            КОСМИЧЕСКАЯ ЯРОСТЬ · {feverSecondsLeft}с
          </div>
        ) : (
          comboCount > 1 && (
            <div className="px-2.5 py-0.5 rounded-lg bg-cyan-400/10 border border-cyan-400/30 text-[11px] font-mono-num font-bold text-cyan-300">
              Комбо x{comboCount} · Нагрев {Math.round(feverMeter * 100)}%
            </div>
          )
        )}
      </div>

      {/* Boss HP HUD */}
      {bossState.isActive && (
        <div className="w-full max-w-[300px] mb-1.5 px-3 py-1.5 rounded-xl bg-red-950/55 border border-red-400/45 space-y-1">
          <div className="flex items-center justify-between text-[11px] font-bold">
            <span className="text-red-200 truncate">{bossState.name}</span>
            <span className="font-mono-num text-amber-300 shrink-0">
              {bossState.remainingSeconds}с
            </span>
          </div>
          <div className="h-1.5 w-full rounded-full bg-slate-900 overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-red-500 to-amber-400 transition-all duration-100"
              style={{
                width: `${Math.max(0, Math.min(100, (bossState.currentHp / Math.max(1, bossState.maxHp)) * 100))}%`,
              }}
            />
          </div>
          <div className="text-[10px] font-mono-num text-red-200/80 text-right">
            HP: {formatCrystalCount(bossState.currentHp)} / {formatCrystalCount(bossState.maxHp)}
          </div>
        </div>
      )}

      <div
        ref={containerRef}
        role="button"
        tabIndex={0}
        aria-label="Добыть космический кристалл"
        onPointerMove={handlePointerMove}
        onPointerLeave={() => {
          tiltRef.current.targetX = 0;
          tiltRef.current.targetY = 0;
        }}
        onPointerDown={handlePointerDown}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (selectedSkinId === 'rainbow_prism') {
              rainbowHueRef.current = (rainbowHueRef.current + 36) % 360;
            }
            const { gained, isCritical, isFever } = onTapCrystal();
            scaleRef.current = 0.84;
            if (isCritical) {
              flashAlphaRef.current = 0.75;
              shakeRef.current = { time: 0.18 };
            }
            spawnParticlesForTap(ARENA_W / 2, CRYSTAL_CY, isCritical);
            const label = isFever
              ? `+${formatRate(gained)} ЯРОСТЬ!`
              : isCritical
              ? `+${formatRate(gained)} КРИТ!`
              : `+${formatRate(gained)}`;
            spawnFloatingLabel(label, ARENA_W / 2, CRYSTAL_CY - 20, isCritical);
          }
        }}
        className="relative w-[320px] h-[300px] cursor-pointer touch-manipulation rounded-3xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-400 overflow-hidden"
      >
        <canvas
          ref={canvasRef}
          className="w-[320px] h-[300px] block pointer-events-none"
        />

        {/* Top-lane Comet (isolated from central crystal tap zone) */}
        {isCometVisible && (
          <button
            type="button"
            onPointerDown={handleCometClick}
            aria-label="Поймать космическую комету"
            style={{
              left: `${cometPos.x}px`,
              top: `${cometPos.y}px`,
            }}
            className="absolute z-20 -translate-x-1/2 -translate-y-1/2 w-12 h-12 flex items-center justify-center cursor-pointer group"
          >
            {/* Outer plasma tail */}
            <span
              className={`absolute w-14 h-2.5 rounded-full bg-gradient-to-r from-amber-300 via-cyan-300/70 to-transparent blur-[2px] ${
                cometPos.fromLeft ? '-left-10' : '-right-10 rotate-180'
              }`}
            />
            {/* Inner bright tail */}
            <span
              className={`absolute w-9 h-1 rounded-full bg-gradient-to-r from-white to-transparent ${
                cometPos.fromLeft ? '-left-6' : '-right-6 rotate-180'
              }`}
            />
            {/* Halo + Core */}
            <span className="w-6 h-6 rounded-full bg-amber-300 shadow-[0_0_22px_rgba(251,191,36,0.98)] flex items-center justify-center group-hover:scale-110 transition-transform">
              <span className="w-3 h-3 rounded-full bg-white shadow-[0_0_8px_#FFFFFF]" />
            </span>
          </button>
        )}

        {floatingNumbers.map((item) => (
          <span
            key={item.id}
            style={{ left: item.x, top: item.y }}
            className={`pointer-events-none absolute font-display font-extrabold whitespace-nowrap animate-float-up drop-shadow-[0_2px_10px_rgba(0,0,0,0.8)] ${
              item.isComet
                ? 'text-base text-amber-200'
                : item.isCritical
                ? 'text-xl text-amber-300'
                : 'text-base text-cyan-300'
            }`}
          >
            {item.text}
          </span>
        ))}
      </div>

      <p className="text-[11px] text-slate-400 mt-1 text-center">
        Быстрые тапы заряжают Ярость · Кометы пролетают вверху арены
      </p>
    </div>
  );
};
