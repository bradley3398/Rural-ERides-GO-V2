"use client";

import React from "react";
import { Zap, ShieldCheck, Award, Star, Infinity, Lock, Flame } from "lucide-react";

interface RiderLevelBadgeProps {
  username: string;
  postsCount: number;
  totalVolts: number;
  themeColors: any;
  userProfiles?: Record<string, any>; // Passed in to de-mock streak data
}

export default function RiderLevelBadge({ username, postsCount, totalVolts, themeColors: t, userProfiles = {} }: RiderLevelBadgeProps) {
  const exp = (postsCount * 10) + (totalVolts * 5);
  const level = Math.floor(exp / 100) + 1;
  const progress = exp % 100;
  const prestige = Math.floor(level / 50);
  const displayLevel = level > 50 ? level % 50 : level;
  
  // 🔥 NEW: Contextual Achievements
  const isNightRider = userProfiles[username]?.nightRiderUnlocked || postsCount > 15;
  const isSectorVip = totalVolts > 500;

  const tierName = level > 50 ? "Prestige Master" : isNightRider ? "🌙 Night Rider Elite" : level > 20 ? "Apex Carver" : level > 10 ? "Trail Veteran" : level > 5 ? "Sector Scout" : "Rookie Rider";
  const nextUnlock = level < 5 ? "Custom Garage Unlock at L5" : level < 10 ? "Video Duets Unlock at L10" : level < 20 ? "Speed Trophies Unlock at L20" : "All Avionics Unlocked";

  // 🔥 DETERMINISTIC STREAK COUNTER (NO MOCKS)
  const actualStreak = userProfiles[username]?.dailyStreak || 1; 

  return (
    <div className={`bg-black/60 border border-white/10 rounded-3xl p-5 flex items-center gap-5 shadow-xl backdrop-blur-xl relative overflow-hidden group ${level > 20 ? 'hover:shadow-[0_0_30px_rgba(255,255,255,0.15)]' : ''} ${isSectorVip ? 'ring-1 ring-amber-500/50 shadow-[0_0_25px_rgba(245,158,11,0.15)]' : ''}`}>
      {/* HOLOGRAPHIC SHINE EFFECT FOR HIGH LEVELS */}
      {level > 20 && (
        <div className="absolute inset-0 bg-gradient-to-tr from-transparent via-white/10 to-transparent -translate-x-full group-hover:animate-[shimmer_1.5s_infinite] pointer-events-none" />
      )}

      <div className={`w-16 h-16 rounded-2xl ${t.bg} text-black flex flex-col items-center justify-center font-black shadow-lg shrink-0 z-10 relative`}>
        {prestige > 0 && <span className="absolute -top-2 -right-2 bg-rose-600 text-white text-[9px] px-1.5 py-0.5 rounded-full flex items-center gap-0.5 shadow-md"><Star className="w-2.5 h-2.5 fill-current"/>P{prestige}</span>}
        <span className="text-[10px] uppercase tracking-tighter -mb-1">LVL</span>
        <span className="text-2xl font-mono">{displayLevel}</span>
      </div>

      <div className="flex-1 min-w-0 z-10">
        <div className="flex items-center justify-between mb-1.5">
          <div>
            <span className="text-sm font-black uppercase text-white tracking-widest truncate flex items-center gap-2">
              {username} 
              {/* 🔥 NEW: Blue "Hot Streak" for 7+ days */}
              {actualStreak >= 3 && (
                <span className={`text-[11px] flex items-center px-2 py-0.5 rounded-md shadow-inner ${actualStreak >= 7 ? 'bg-cyan-950/60 border border-cyan-500/40 text-cyan-400' : 'bg-orange-950/40 border border-orange-500/30 text-orange-400'}`}>
                  {actualStreak}<Flame className="w-3.5 h-3.5 ml-0.5 fill-current"/>
                </span>
              )}
              {prestige > 0 && <Infinity className="w-3.5 h-3.5 text-cyan-400" title="Prestige Pilot"/>}
              {/* 🔥 NEW: Sector VIP Badge */}
              {isSectorVip && <Award className="w-3.5 h-3.5 text-amber-400" title="Sector VIP (>500 Volts)"/>}
            </span>
            <span className="text-[10px] font-mono text-zinc-400 uppercase tracking-wider">{tierName}</span>
          </div>
          <div className="text-right">
            <span className={`text-[11px] font-mono font-black ${t.text} block mb-0.5`}>{exp} XP</span>
            <span className="text-[9px] text-zinc-500 font-mono uppercase flex items-center gap-1 justify-end"><Lock className="w-2.5 h-2.5"/> {nextUnlock}</span>
          </div>
        </div>
        <div className="w-full bg-zinc-800 h-2.5 rounded-full overflow-hidden border border-white/10 mt-2 relative">
          <div className={`h-full ${t.bg} transition-all duration-500`} style={{ width: `${progress}%` }} />
          <div className="absolute inset-0 bg-[linear-gradient(90deg,transparent_0%,rgba(255,255,255,0.2)_50%,transparent_100%)] bg-[length:20px_100%] animate-[slide_2s_linear_infinite]" />
        </div>
      </div>
      <div className={`absolute -right-6 -bottom-6 opacity-10 ${t.text} pointer-events-none`}><Award className="w-32 h-32" /></div>
    </div>
  );
}