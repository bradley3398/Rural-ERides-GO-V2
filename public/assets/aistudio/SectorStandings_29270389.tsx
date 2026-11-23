"use client";

import React, { useState, useMemo } from "react";
import { Trophy, Gauge, Compass, Zap, Activity, ChevronRight, ShieldCheck, User, CalendarClock, Mountain, Wrench, Camera } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface SectorStandingsProps {
  topSpeedRecord: { user: string; speed: number; vehicle: string };
  topScout: string;
  scoutCount: number;
  mvpUser: string;
  mvpVolts: number;
  isGpsLocked: boolean;
  useMetric: boolean;
  themeColors: any;
  onOpenProfile: (username: string) => void;
  glassBlurClass?: string;
  cornerStyle?: string;
  userProfiles?: Record<string, any>;
  posts?: any[];
}

export default function SectorStandings({
  topSpeedRecord, topScout, scoutCount, mvpUser, mvpVolts,
  isGpsLocked, useMetric, themeColors: t, onOpenProfile,
  glassBlurClass = "backdrop-blur-2xl", cornerStyle = "rounded-3xl", userProfiles = {}, posts = []
}: SectorStandingsProps) {
  const [timeFilter, setTimeFilter] = useState<"24h" | "7d" | "all">("all");
  
  const speedUnit = useMetric ? "km/h" : "mph";
  const displaySpeed = useMetric ? (topSpeedRecord.speed * 1.60934).toFixed(1) : topSpeedRecord.speed.toFixed(1);

  const topSpeedPfp = userProfiles[topSpeedRecord.user]?.pfpUrl;
  const mvpPfp = userProfiles[mvpUser]?.pfpUrl;

  // 🔥 DETERMINISTIC LEADER CALCULATOR (NO MOCKS)
  const computedLeaders = useMemo(() => {
    let hillKing = { user: "Awaiting Data", maxSpeedOrElev: 0 };
    let mechMaster = { user: "Awaiting Data", count: 0 };

    const mechCounts: Record<string, number> = {};

    posts.forEach(p => {
      // 1. King of the hill (Highest telemetry.speed integer if elevation is unavailable)
      if (p.telemetry?.speed) {
         const sVal = parseFloat(p.telemetry.speed);
         if (!isNaN(sVal) && sVal > hillKing.maxSpeedOrElev) {
            hillKing = { user: p.username, maxSpeedOrElev: sVal };
         }
      }

      // 2. Master Mechanic (Most Custom Builds / Diagnostics posts)
      if (p.category === "Custom Builds" || p.category === "Hardware Diagnostics") {
         mechCounts[p.username] = (mechCounts[p.username] || 0) + 1;
      }
    });

    Object.keys(mechCounts).forEach(u => {
      if (mechCounts[u] > mechMaster.count) {
         mechMaster = { user: u, count: mechCounts[u] };
      }
    });

    return { hillKing, mechMaster };
  }, [posts]);

  return (
    <div className={`app-card ${glassBlurClass} ${cornerStyle} bg-black/60 border border-white/10 p-5 sm:p-6 shadow-2xl relative overflow-hidden font-sans space-y-4`}>
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-white/10 pb-4">
        <div className="flex items-center gap-3">
          <div className={`${t.bg} p-2.5 rounded-xl text-black shadow-md`}>
            <Trophy className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-sm font-black uppercase tracking-widest text-white flex items-center gap-2">
              Sector Telemetry Standings
            </h3>
            <span className="text-[10px] font-mono text-zinc-400 uppercase tracking-wide block mt-0.5">
              Regional Leaderboard (50-Mile Radius)
            </span>
          </div>
        </div>

        {/* TIME FILTER CHIPS */}
        <div className="flex items-center gap-1.5 bg-black/50 border border-white/10 p-1 rounded-xl shadow-inner">
          <button onClick={() => setTimeFilter("24h")} className={`px-3 py-1 rounded-lg text-[9px] font-black uppercase transition-colors ${timeFilter === "24h" ? "bg-white/20 text-white" : "text-zinc-500 hover:text-white"}`}>24H</button>
          <button onClick={() => setTimeFilter("7d")} className={`px-3 py-1 rounded-lg text-[9px] font-black uppercase transition-colors ${timeFilter === "7d" ? "bg-white/20 text-white" : "text-zinc-500 hover:text-white"}`}>7D</button>
          <button onClick={() => setTimeFilter("all")} className={`px-3 py-1 rounded-lg text-[9px] font-black uppercase transition-colors ${timeFilter === "all" ? "bg-white/20 text-white" : "text-zinc-500 hover:text-white"}`}>ALL</button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
        {/* TOP SPEED RECORD */}
        <div onClick={() => topSpeedRecord.user !== "Awaiting Data" && onOpenProfile(topSpeedRecord.user)} className="bg-gradient-to-br from-black/80 to-rose-950/20 hover:from-black hover:to-rose-900/30 border border-white/10 hover:border-rose-500/50 rounded-2xl p-4 flex flex-col justify-between shadow-inner transition-all cursor-pointer group active:scale-98">
          <div className="flex items-center justify-between mb-3">
            <span className="text-[10px] font-mono font-black text-rose-400 uppercase tracking-widest flex items-center gap-1.5"><Gauge className="w-4 h-4" /> Sector V-Max</span>
            <div className="w-8 h-8 rounded-full bg-black border border-white/20 flex items-center justify-center overflow-hidden shrink-0 shadow-sm relative">
               {topSpeedPfp ? <img src={topSpeedPfp} className="w-full h-full object-cover"/> : <User className="w-4 h-4 text-zinc-500"/>}
               <div className="absolute -bottom-1 -right-1 text-base">🥇</div>
            </div>
          </div>
          <div className="space-y-1 mt-auto">
            <div className="text-2xl sm:text-3xl font-black font-mono text-white tabular-nums tracking-tight drop-shadow-md">{topSpeedRecord.speed > 0 ? `${displaySpeed} ${speedUnit}` : "--"}</div>
            <div className="text-sm font-bold text-zinc-300 uppercase truncate">{topSpeedRecord.user}</div>
            <div className="text-[10px] font-mono text-zinc-500 truncate">{topSpeedRecord.vehicle}</div>
          </div>
        </div>

        {/* MOST VALUED PILOT (VOLTS) */}
        <div onClick={() => mvpUser !== "Awaiting Data" && onOpenProfile(mvpUser)} className="bg-gradient-to-br from-black/80 to-emerald-950/20 hover:from-black hover:to-emerald-900/30 border border-white/10 hover:border-emerald-500/50 rounded-2xl p-4 flex flex-col justify-between shadow-inner transition-all cursor-pointer group active:scale-98">
          <div className="flex items-center justify-between mb-3">
            <span className={`text-[10px] font-mono font-black ${t.text} uppercase tracking-widest flex items-center gap-1.5`}><Zap className="w-4 h-4 fill-current" /> High Voltage MVP</span>
            <div className="w-8 h-8 rounded-full bg-black border border-white/20 flex items-center justify-center overflow-hidden shrink-0 shadow-sm relative">
               {mvpPfp ? <img src={mvpPfp} className="w-full h-full object-cover"/> : <User className="w-4 h-4 text-zinc-500"/>}
               <div className="absolute -bottom-1 -right-1 text-base">👑</div>
            </div>
          </div>
          <div className="space-y-1 mt-auto">
            <div className="text-2xl sm:text-3xl font-black font-mono text-white tabular-nums tracking-tight drop-shadow-md">{mvpVolts > 0 ? `${mvpVolts} Volts` : "--"}</div>
            <div className="text-sm font-bold text-zinc-300 uppercase truncate">{mvpUser}</div>
            <div className="text-[10px] font-mono text-zinc-500 truncate">Fleet Network Resonance</div>
          </div>
        </div>

        {/* MASTER MECHANIC */}
        <div onClick={() => computedLeaders.mechMaster.user !== "Awaiting Data" && onOpenProfile(computedLeaders.mechMaster.user)} className="bg-gradient-to-br from-black/80 to-amber-950/20 hover:from-black hover:to-amber-900/30 border border-white/10 hover:border-amber-500/50 rounded-2xl p-4 flex flex-col justify-between shadow-inner transition-all cursor-pointer group active:scale-98">
          <div className="flex items-center justify-between mb-3">
            <span className="text-[10px] font-mono font-black text-amber-400 uppercase tracking-widest flex items-center gap-1.5"><Wrench className="w-4 h-4" /> Master Mechanic</span>
            <div className="w-8 h-8 rounded-full bg-black border border-white/20 flex items-center justify-center overflow-hidden shrink-0 shadow-sm relative">
               {userProfiles[computedLeaders.mechMaster.user]?.pfpUrl ? <img src={userProfiles[computedLeaders.mechMaster.user].pfpUrl} className="w-full h-full object-cover"/> : <User className="w-4 h-4 text-zinc-500"/>}
               <div className="absolute -bottom-1 -right-1 text-base">🔧</div>
            </div>
          </div>
          <div className="space-y-1 mt-auto">
            <div className="text-2xl sm:text-3xl font-black font-mono text-white tabular-nums tracking-tight drop-shadow-md">{computedLeaders.mechMaster.count > 0 ? `${computedLeaders.mechMaster.count} Builds` : "--"}</div>
            <div className="text-sm font-bold text-zinc-300 uppercase truncate">{computedLeaders.mechMaster.user}</div>
            <div className="text-[10px] font-mono text-zinc-500 truncate">Hardware Modifications</div>
          </div>
        </div>
      </div>
    </div>
  );
}