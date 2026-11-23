"use client";

import React, { useEffect, useRef } from "react";
import { Radar, Compass, AlertTriangle, Users, Radio } from "lucide-react";

export default function RadarSectorMonitor({ posts = [], userProfiles = {} }: any) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animId: number;
    let angle = 0;

    const render = () => {
      const w = canvas.width;
      const h = canvas.height;
      const cx = w / 2;
      const cy = h / 2;
      const radius = Math.min(cx, cy) - 20;

      ctx.clearRect(0, 0, w, h);

      // Grid Rings
      ctx.strokeStyle = "rgba(255, 255, 255, 0.1)";
      ctx.lineWidth = 1;
      for (let i = 1; i <= 4; i++) {
        ctx.beginPath();
        ctx.arc(cx, cy, (radius / 4) * i, 0, Math.PI * 2);
        ctx.stroke();
      }

      // Crosshairs
      ctx.beginPath();
      ctx.moveTo(cx - radius, cy); ctx.lineTo(cx + radius, cy);
      ctx.moveTo(cx, cy - radius); ctx.lineTo(cx, cy + radius);
      ctx.stroke();

      // Sweep Beam
      const gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius);
      gradient.addColorStop(0, "rgba(57, 255, 20, 0.25)");
      gradient.addColorStop(1, "rgba(57, 255, 20, 0)");

      ctx.save();
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, radius, angle - 0.4, angle);
      ctx.closePath();
      ctx.fillStyle = gradient;
      ctx.fill();
      ctx.restore();

      // Plot Blips from Posts
      posts.forEach((p, idx) => {
        if (!p.location) return;
        const offsetX = Math.sin(idx * 45) * (radius * 0.7);
        const offsetY = Math.cos(idx * 33) * (radius * 0.7);
        const blipX = cx + offsetX;
        const blipY = cy + offsetY;

        ctx.beginPath();
        ctx.arc(blipX, blipY, p.isHelpNeeded ? 6 : 4, 0, Math.PI * 2);
        ctx.fillStyle = p.isHelpNeeded ? "#ef4444" : p.isSpeedTrap ? "#f59e0b" : "#39ff14";
        ctx.fill();
      });

      angle += 0.025;
      animId = requestAnimationFrame(render);
    };

    render();
    return () => cancelAnimationFrame(animId);
  }, [posts]);

  return (
    <div className="bg-black/90 border border-white/10 rounded-3xl p-5 flex flex-col items-center justify-center relative overflow-hidden shadow-2xl">
      <div className="w-full flex items-center justify-between mb-3">
        <span className="text-xs font-mono font-black uppercase tracking-widest text-[#39ff14] flex items-center gap-2">
          <Radar className="w-4 h-4 animate-spin" style={{ animationDuration: "5s" }} /> Tactical Radar Oversight
        </span>
        <span className="text-[10px] font-mono text-zinc-500 uppercase">Live GPS Grid</span>
      </div>
      <div className="relative aspect-square w-full max-w-[320px] bg-zinc-950 rounded-2xl border border-white/10 overflow-hidden flex items-center justify-center">
        <canvas ref={canvasRef} width={300} height={300} className="w-full h-full" />
      </div>
    </div>
  );
}