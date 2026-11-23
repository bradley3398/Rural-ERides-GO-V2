"use client";

import React, { useState, useEffect, useRef } from "react";
import { Play, Square, MapPin, Gauge, Timer, Activity, Zap, Mountain, Bike, Globe } from "lucide-react";
import { Capacitor } from '@capacitor/core';
import { Haptics } from '@capacitor/haptics';

// --- UNIVERSAL PEV LIST ---
const PEV_TYPES = [
  "Electric Scooter",
  "Electric Bike",
  "Electric Trike",
  "Electric Moped",
  "EUC / Unicycle",
  "Other / Custom"
];

interface RideLoggerProps {
  theme?: string;
  useMetric?: boolean;
  locale?: string;
  timeFormat?: string;
  performanceMode?: boolean;
  uiScale?: string;
  globalVolume?: number;
  glassBlurClass?: string;
  cornerStyle?: string;
  reducedMotion?: boolean;
}

export default function RideLogger(props: RideLoggerProps) {
  const [isRiding, setIsRiding] = useState(false);
  const [activePEV, setActivePEV] = useState(PEV_TYPES[0]);
  const [localTheme, setLocalTheme] = useState<string>(() => props.theme || (typeof window !== 'undefined' ? localStorage.getItem("rural_theme") || localStorage.getItem("rt_theme") || 'lime' : 'lime'));
  
  const glassBlurClass = props.glassBlurClass || "backdrop-blur-2xl";
  const cornerStyle = props.cornerStyle || "rounded-3xl";

  // --- MASTER SETTINGS SYNC ENGINE ---
  const [useMetric, setUseMetric] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("radar_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km";
    }
    return props.useMetric ?? false;
  });

  useEffect(() => {
    const handleSettingsSync = () => {
      const isMetric = localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("radar_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km";
      setUseMetric(isMetric);

      const savedTheme = localStorage.getItem("rural_theme") || localStorage.getItem("rt_theme") || props.theme || 'lime';
      setLocalTheme(savedTheme);
    };

    window.addEventListener('theme-sync', handleSettingsSync);
    window.addEventListener('settings-sync', handleSettingsSync);
    window.addEventListener('storage', handleSettingsSync);
    handleSettingsSync();

    return () => {
      window.removeEventListener('theme-sync', handleSettingsSync);
      window.removeEventListener('settings-sync', handleSettingsSync);
      window.removeEventListener('storage', handleSettingsSync);
    };
  }, [props.theme]);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      localStorage.setItem("radar_use_metric", useMetric ? "true" : "false");
      localStorage.setItem("rt_use_metric", useMetric ? "true" : "false");
    }
  }, [useMetric]);

  const [rideStats, setRideStats] = useState({ 
    distance: 0, 
    speed: 0, 
    maxSpeed: 0, 
    duration: 0, 
    altitude: 0 
  });
  
  const lastCoords = useRef<GeolocationPosition | null>(null);
  const watchId = useRef<number | null>(null);
  const timerId = useRef<NodeJS.Timeout | null>(null);

  const triggerHaptic = async (style: any = "LIGHT") => {
    try {
      if (Capacitor.isPluginAvailable('Haptics')) await Haptics.impact({ style });
      else if (typeof navigator !== 'undefined' && 'vibrate' in navigator) navigator.vibrate(style === "HEAVY" ? 40 : 15);
    } catch (e) {}
  };

  const getTheme = () => {
    const baseTheme = localTheme;
    const themes: Record<string, any> = {
      lime: { primary: 'bg-[#39ff14]', text: 'text-[#39ff14]', border: 'border-[#39ff14]', glow: 'shadow-[0_0_20px_rgba(57,255,20,0.3)]', bgSubtle: 'bg-[#39ff14]/10', borderSubtle: 'border-[#39ff14]/30' },
      cyan: { primary: 'bg-cyan-500', text: 'text-cyan-400', border: 'border-cyan-500', glow: 'shadow-[0_0_20px_rgba(6,182,212,0.3)]', bgSubtle: 'bg-cyan-950/30', borderSubtle: 'border-cyan-900/50' },
      emerald: { primary: 'bg-emerald-500', text: 'text-emerald-400', border: 'border-emerald-500', glow: 'shadow-[0_0_20px_rgba(16,185,129,0.3)]', bgSubtle: 'bg-emerald-950/30', borderSubtle: 'border-emerald-900/50' },
      amber: { primary: 'bg-amber-400', text: 'text-amber-400', border: 'border-amber-400', glow: 'shadow-[0_0_20px_rgba(251,191,36,0.3)]', bgSubtle: 'bg-amber-950/30', borderSubtle: 'border-amber-900/50' },
      rose: { primary: 'bg-rose-500', text: 'text-rose-400', border: 'border-rose-500', glow: 'shadow-[0_0_20px_rgba(244,63,94,0.3)]', bgSubtle: 'bg-rose-950/30', borderSubtle: 'border-rose-900/50' },
      purple: { primary: 'bg-purple-500', text: 'text-purple-400', border: 'border-purple-500', glow: 'shadow-[0_0_20px_rgba(168,85,247,0.3)]', bgSubtle: 'bg-purple-950/30', borderSubtle: 'border-purple-900/50' },
      void: { primary: 'bg-zinc-800', text: 'text-white', border: 'border-zinc-500', glow: 'shadow-[0_0_20px_rgba(255,255,255,0.1)]', bgSubtle: 'bg-zinc-900/50', borderSubtle: 'border-zinc-700/50' }
    };
    return themes[baseTheme] || themes.lime;
  };
  const t = getTheme();

  useEffect(() => {
    return () => {
      if (watchId.current) navigator.geolocation.clearWatch(watchId.current);
      if (timerId.current) clearInterval(timerId.current);
    };
  }, []);

  const startRide = () => {
    triggerHaptic("HEAVY");
    setIsRiding(true);
    setRideStats({ distance: 0, speed: 0, maxSpeed: 0, duration: 0, altitude: 0 });
    lastCoords.current = null; 
    
    timerId.current = setInterval(() => {
      setRideStats(prev => ({ ...prev, duration: prev.duration + 1 }));
    }, 1000);

    watchId.current = navigator.geolocation.watchPosition(
      (position) => {
        if (lastCoords.current) {
          const dist = calculateDistance(
            lastCoords.current.coords.latitude, lastCoords.current.coords.longitude,
            position.coords.latitude, position.coords.longitude
          );
          
          const speedMultiplier = useMetric ? 3.6 : 2.23694;
          const altMultiplier = useMetric ? 1 : 3.28084;

          const currentSpeed = (position.coords.speed || 0) * speedMultiplier;
          const currentAlt = position.coords.altitude ? position.coords.altitude * altMultiplier : 0;
          
          setRideStats(prev => ({
            ...prev,
            distance: prev.distance + dist,
            speed: currentSpeed,
            maxSpeed: Math.max(prev.maxSpeed, currentSpeed),
            altitude: currentAlt
          }));
        }
        lastCoords.current = position;
      },
      (err) => console.error("GPS Lock Error", err),
      { enableHighAccuracy: true, maximumAge: 2000, timeout: 5000 }
    );
  };

  const stopRide = () => {
    triggerHaptic("HEAVY");
    if (watchId.current) navigator.geolocation.clearWatch(watchId.current);
    if (timerId.current) clearInterval(timerId.current);
    setIsRiding(false);
  };

  const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number) => {
    const R = useMetric ? 6371.0 : 3958.8; 
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon/2) * Math.sin(dLon/2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  };

  const formatTime = (totalSeconds: number) => {
    const m = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
    const s = (totalSeconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  const avgSpeed = rideStats.duration > 0 ? (rideStats.distance / (rideStats.duration / 3600)) : 0;

  const speedLabel = useMetric ? "KM/H" : "MPH";
  const distLabel = useMetric ? "KM" : "MI";
  const altLabel = useMetric ? "M" : "FT";

  return (
    <div className={`app-card ${glassBlurClass} ${cornerStyle} p-6 border border-white/10 shadow-[0_8px_32px_0_rgba(0,0,0,0.36)] space-y-6 relative z-10 font-sans`}>
      
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-white/10 pb-4">
        <div className="space-y-3">
          <div className="flex items-center gap-4">
            <h2 className="text-white font-black text-sm uppercase tracking-widest flex items-center gap-2">
              <Activity className={`w-4 h-4 ${t.text}`} /> Ride Telemetry
            </h2>
            
            <button 
              type="button"
              onClick={() => {
                if (!isRiding) {
                  triggerHaptic("LIGHT");
                  setUseMetric(!useMetric);
                }
              }}
              disabled={isRiding}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-[9px] font-black uppercase tracking-widest border transition-all duration-300 active:scale-95 cursor-pointer ${isRiding ? 'opacity-50 cursor-not-allowed' : ''} ${useMetric ? `${t.bgSubtle} ${t.text} ${t.borderSubtle}` : 'bg-white/5 text-zinc-300 border-white/10'}`}
              title="Toggle Unit System"
            >
              <Globe className="w-3 h-3"/> {useMetric ? 'Metric' : 'Imperial'}
            </button>
          </div>
          
          <div className="flex items-center gap-2">
            <Bike className="w-4 h-4 text-zinc-400" />
            <select 
              value={activePEV} 
              onChange={(e) => {
                triggerHaptic("LIGHT");
                setActivePEV(e.target.value);
              }}
              disabled={isRiding}
              className={`bg-black/50 border border-white/10 text-xs ${t.text} rounded-xl px-3 py-1.5 outline-none font-bold cursor-pointer disabled:opacity-50 shadow-inner`}
            >
              {PEV_TYPES.map(pev => (
                <option key={pev} value={pev} className="bg-black text-white">{pev}</option>
              ))}
            </select>
          </div>
        </div>

        <button 
          type="button"
          onClick={isRiding ? stopRide : startRide}
          className={`px-6 py-3.5 rounded-xl text-xs font-black uppercase tracking-widest transition-all duration-300 active:scale-95 shadow-lg flex items-center gap-2 cursor-pointer ${
            isRiding 
              ? "bg-rose-950/60 hover:bg-rose-900 text-rose-300 border border-rose-900/50 shadow-[0_0_15px_rgba(244,63,94,0.3)]" 
              : `${t.primary} hover:opacity-90 text-black ${t.glow}`
          }`}
        >
          {isRiding ? <><Square className="w-4 h-4 fill-current"/> End Logging</> : <><Play className="w-4 h-4 fill-current"/> Start Run</>}
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
        <div className="bg-black/50 backdrop-blur-md p-4 rounded-2xl border border-white/10 text-center relative overflow-hidden shadow-inner">
          <div className={`absolute top-0 left-0 w-full h-0.5 ${t.primary} opacity-50`}></div>
          <p className="text-[10px] text-zinc-400 uppercase flex items-center justify-center gap-1.5 font-bold mb-1">
            <Gauge className={`w-3.5 h-3.5 ${t.text}`}/> Current Speed
          </p>
          <p className="text-3xl font-black font-mono text-white tracking-tight">
            {rideStats.speed.toFixed(1)} <span className="text-xs text-zinc-400 font-sans tracking-normal font-bold">{speedLabel}</span>
          </p>
        </div>

        <div className="bg-black/50 backdrop-blur-md p-4 rounded-2xl border border-white/10 text-center relative overflow-hidden shadow-inner">
          <div className="absolute top-0 left-0 w-full h-0.5 bg-amber-500/50"></div>
          <p className="text-[10px] text-zinc-400 uppercase flex items-center justify-center gap-1.5 font-bold mb-1">
            <Zap className="w-3.5 h-3.5 text-amber-400"/> Top Speed
          </p>
          <p className="text-3xl font-black font-mono text-amber-400 tracking-tight">
            {rideStats.maxSpeed.toFixed(1)} <span className="text-xs text-amber-700 font-sans tracking-normal font-bold">{speedLabel}</span>
          </p>
        </div>

        <div className="bg-black/50 backdrop-blur-md p-4 rounded-2xl border border-white/10 text-center relative overflow-hidden shadow-inner">
          <div className="absolute top-0 left-0 w-full h-0.5 bg-emerald-500/50"></div>
          <p className="text-[10px] text-zinc-400 uppercase flex items-center justify-center gap-1.5 font-bold mb-1">
            <MapPin className="w-3.5 h-3.5 text-emerald-400"/> Distance
          </p>
          <p className="text-3xl font-black font-mono text-white tracking-tight">
            {rideStats.distance.toFixed(2)} <span className="text-xs text-zinc-400 font-sans tracking-normal font-bold">{distLabel}</span>
          </p>
        </div>
        
        <div className="bg-black/50 backdrop-blur-md p-4 rounded-2xl border border-white/10 text-center relative overflow-hidden shadow-inner">
          <p className="text-[10px] text-zinc-400 uppercase flex items-center justify-center gap-1.5 font-bold mb-1">
            Avg Pace
          </p>
          <p className="text-xl font-black font-mono text-zinc-200 tracking-tight">
            {avgSpeed.toFixed(1)} <span className="text-[10px] text-zinc-400 font-sans tracking-normal font-bold">{speedLabel}</span>
          </p>
        </div>

        <div className="bg-black/50 backdrop-blur-md p-4 rounded-2xl border border-white/10 text-center relative overflow-hidden shadow-inner">
          <p className="text-[10px] text-zinc-400 uppercase flex items-center justify-center gap-1.5 font-bold mb-1">
            <Mountain className="w-3.5 h-3.5 text-purple-400"/> Altitude
          </p>
          <p className="text-xl font-black font-mono text-zinc-200 tracking-tight">
            {rideStats.altitude.toFixed(0)} <span className="text-[10px] text-zinc-400 font-sans tracking-normal font-bold">{altLabel}</span>
          </p>
        </div>

        <div className="bg-black/50 backdrop-blur-md p-4 rounded-2xl border border-white/10 text-center relative overflow-hidden shadow-inner">
          <p className="text-[10px] text-zinc-400 uppercase flex items-center justify-center gap-1.5 font-bold mb-1">
            <Timer className="w-3.5 h-3.5 text-rose-400"/> Duration
          </p>
          <p className={`text-xl font-black font-mono tracking-tight ${isRiding ? "text-rose-400 animate-pulse" : "text-white"}`}>
            {formatTime(rideStats.duration)}
          </p>
        </div>

      </div>
    </div>
  );
}