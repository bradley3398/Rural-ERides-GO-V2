"use client";

import React, { useState, useEffect } from "react";
import { 
  Info, Globe, Shield, Cpu, Zap, Award, Sparkles, Terminal, 
  ExternalLink, RefreshCw, Layers, Activity, Wifi, HardDrive, 
  Smartphone, Battery, Compass, Volume2, CloudRain, Map, Radio 
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { Device } from '@capacitor/device';
import { Network as CapNetwork } from '@capacitor/network';
import { Capacitor } from '@capacitor/core';

export default function AppInfoTab({
  icon,
  activeTheme,
  cornerStyle,
  reducedMotion,
  typographyClass,
  setActiveTab,
  triggerHaptic,
  glassBlurClass
}: any) {
  const [easterEggActive, setEasterEggActive] = useState(false);
  const [diagnosticPing, setDiagnosticPing] = useState<number | null>(null);
  const [isDiagnosticRunning, setIsDiagnosticRunning] = useState(false);
  
  const [phoneData, setPhoneData] = useState({
    model: "Scanning...",
    manufacturer: "Scanning...",
    os: "Scanning...",
    osVersion: "Scanning...",
    batteryPct: null as number | null,
    isCharging: null as boolean | null,
    networkStatus: "Active",
    screenResolution: typeof window !== 'undefined' ? `${window.innerWidth}x${window.innerHeight}` : "N/A"
  });

  useEffect(() => {
    const loadPhoneData = async () => {
      try {
        const info = await Device.getInfo();
        const battery = await Device.getBatteryInfo();
        const net = await CapNetwork.getStatus();
        
        setPhoneData({
          model: info.model || "Generic PEV Terminal",
          manufacturer: info.manufacturer || "Universal",
          os: info.operatingSystem || "PWA Core",
          osVersion: info.osVersion || "Latest",
          batteryPct: battery.batteryLevel !== undefined ? Math.round(battery.batteryLevel * 100) : null,
          isCharging: battery.isCharging ?? null,
          networkStatus: net.connectionType ? net.connectionType.toUpperCase() : "Online",
          screenResolution: `${window.innerWidth}x${window.innerHeight} (${window.devicePixelRatio || 1}x DPR)`
        });
      } catch (e) {
        setPhoneData({
          model: navigator.userAgent.includes("Mobile") ? "Mobile Web Client" : "Desktop Workstation",
          manufacturer: "Standard Host",
          os: navigator.platform || "Web",
          osVersion: "HTML5 Standard",
          batteryPct: null,
          isCharging: null,
          networkStatus: navigator.onLine ? "ONLINE" : "OFFLINE",
          screenResolution: `${window.innerWidth}x${window.innerHeight}`
        });
      }
    };
    loadPhoneData();
  }, []);

  const runAvionicsDiagnostic = () => {
    triggerHaptic("HEAVY");
    setIsDiagnosticRunning(true);
    setDiagnosticPing(null);
    setTimeout(() => {
      const mockPing = Math.floor(Math.random() * 8) + 12;
      setDiagnosticPing(mockPing);
      setIsDiagnosticRunning(false);
    }, 800);
  };

  const coreFeatures = [
    { icon: Compass, title: "9-DOF AHRS IMU Fusion", desc: "Compensated complementary filter processing real-time lean angles, pitch, G-forces, and road jitter." },
    { icon: Map, title: "Web Speed Limit Geocoding", desc: "Automated Overpass and TomTom infrastructure querying for live speed limits and active school zones." },
    { icon: CloudRain, title: "Live Doppler Rain Radar", desc: "Precipitation map overlays powered by RainViewer matrix tiles for severe weather avoidance." },
    { icon: Volume2, title: "AI Voice Mechanic & Beeps", desc: "Synthesized audio warnings and speech alerts when crossing speed thresholds or high thermal loads." },
    { icon: Radio, title: "Multi-Rider Mesh Radar", desc: "Firebase Firestore real-time coordinate streaming for group rides and convoy tracking." },
    { icon: Zap, title: "Dynamic Battery & Sag Math", desc: "Internal resistance load calculations predicting real-time voltage sag and amp-hour consumption." }
  ];

  return (
    <div className={`space-y-6 max-w-4xl mx-auto p-4 sm:p-6 ${typographyClass}`}>
      
      {/* MAIN CONTAINER */}
      <div className={`app-card ${glassBlurClass} ${cornerStyle} p-6 sm:p-10 relative overflow-hidden ${reducedMotion ? '' : 'transition-all duration-300'}`}>
        
        {/* BACKGROUND GLOW ACCENT */}
        <div className={`absolute top-0 right-0 w-96 h-96 ${activeTheme.bg} opacity-15 rounded-full blur-[140px] pointer-events-none`}></div>
        <div className="absolute top-0 right-0 p-8 opacity-5 pointer-events-none"><Info className={`w-48 h-48 ${activeTheme.text}`} /></div>
        
        <div className="relative z-10 space-y-8">
          
          {/* HEADER ROW */}
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5 border-b border-white/10 pb-6">
            <motion.div 
              whileHover={{ scale: 1.05, rotate: 2 }}
              onClick={() => { triggerHaptic("LIGHT"); setEasterEggActive(!easterEggActive); }}
              className="cursor-pointer relative group shrink-0"
              title="Tap for Avionics Easter Egg"
            >
              <img src={icon} alt="App Logo" className={`w-20 h-20 sm:w-24 sm:h-24 ${cornerStyle} border-2 border-white/20 object-cover shadow-2xl group-hover:border-cyan-400 transition-colors`} />
              <div className="absolute inset-0 bg-cyan-500/20 opacity-0 group-hover:opacity-100 rounded-2xl transition-opacity flex items-center justify-center">
                <Sparkles className="w-6 h-6 text-cyan-400 animate-spin" />
              </div>
            </motion.div>

            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2.5 flex-wrap">
                <h2 className={`text-2xl sm:text-4xl font-black uppercase tracking-widest text-transparent bg-clip-text bg-gradient-to-r ${activeTheme.gradient}`}>RURAL ERIDES GO</h2>
              </div>
              <p className="text-[10px] text-zinc-400 font-mono uppercase tracking-widest mt-2 flex items-center gap-2">
                <Cpu className="w-4 h-4 text-cyan-400"/> Universal Avionics &amp; Telemetry Core for Personal Electric Vehicles
              </p>
            </div>

            <button 
              type="button" 
              onClick={() => { triggerHaptic(); setActiveTab("DASH"); }} 
              className={`min-h-[48px] px-7 py-3.5 ${cornerStyle} text-[10px] font-black uppercase tracking-widest border ${reducedMotion ? '' : 'transition-all active:scale-95'} ${activeTheme.bg} text-black border-transparent shadow-[0_0_25px_rgba(255,255,255,0.25)] cursor-pointer`}
            >
              Return to Cockpit
            </button>
          </div>

          {/* EASTER EGG CONSOLE (TRIGGERED BY LOGO CLICK) */}
          <AnimatePresence>
            {easterEggActive && (
              <motion.div 
                initial={{ opacity: 0, height: 0 }} 
                animate={{ opacity: 1, height: "auto" }} 
                exit={{ opacity: 0, height: 0 }} 
                className="bg-black/95 border border-cyan-500/60 p-5 rounded-2xl font-mono text-xs text-cyan-400 space-y-3 shadow-[0_0_40px_rgba(6,182,212,0.3)] overflow-hidden"
              >
                <div className="flex justify-between items-center border-b border-cyan-500/30 pb-2">
                   <span className="font-black uppercase flex items-center gap-2"><Terminal className="w-4 h-4"/> RURAL_OS // KERNEL DIAGNOSTIC CONSOLE</span>
                   <button type="button" onClick={() => setEasterEggActive(false)} className="text-zinc-500 hover:text-white cursor-pointer px-2 py-1">[CLOSE]</button>
                </div>
                <div className="space-y-1.5 text-zinc-300">
                  <p>&gt; System initialized on 9-DOF AHRS sensor fusion matrix.</p>
                  <p>&gt; Host Device: <span className="text-white font-bold">{phoneData.model}</span> ({phoneData.os})</p>
                  <p>&gt; Telemetry loop latency: <span className="text-emerald-400 font-bold">{diagnosticPing ? `${diagnosticPing}ms` : "1000ms nominal"}</span></p>
                  <p>&gt; Lead Architect &amp; Maintainer: <strong className="text-amber-400">Lord Bradley Callison</strong></p>
                  <p className="text-cyan-300 italic">&gt; "Keep it rural y'all!"</p>
                </div>
                <div className="pt-2 flex gap-2">
                   <button 
                     type="button" 
                     onClick={runAvionicsDiagnostic} 
                     disabled={isDiagnosticRunning}
                     className="min-h-[40px] px-5 py-2.5 bg-cyan-500/20 hover:bg-cyan-500/30 border border-cyan-500/40 text-cyan-300 rounded-xl font-bold uppercase text-[9px] cursor-pointer active:scale-95 transition-all flex items-center gap-2 disabled:opacity-50"
                   >
                     <RefreshCw className={`w-3.5 h-3.5 ${isDiagnosticRunning ? 'animate-spin' : ''}`}/> 
                     {isDiagnosticRunning ? "Running Subsystem Diagnostic..." : "Run Hardware Ping Test"}
                   </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* CREATOR & MISSION CARD */}
          <div className={`bg-black/50 border border-white/10 p-6 sm:p-8 ${cornerStyle} shadow-inner space-y-6 relative backdrop-blur-md`}>
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-white/10 pb-5">
              <div>
                <span className="text-[9px] font-black uppercase tracking-widest text-zinc-500 block mb-1">Project Leadership &amp; Vision</span>
                <div className="text-xl sm:text-2xl font-black text-white uppercase tracking-wider flex items-center gap-2">
                  Lord Bradley Callison <span className="text-base">👑</span>
                </div>
              </div>
              <div className={`text-base sm:text-lg font-black uppercase tracking-widest ${activeTheme.text} ${reducedMotion ? '' : 'animate-pulse'} drop-shadow-[0_0_12px_currentColor] bg-white/5 border border-white/15 px-5 py-2.5 ${cornerStyle}`}>
                KEEP IT RURAL YALL
              </div>
            </div>

            <div className="space-y-3.5 text-xs text-zinc-300 font-medium leading-relaxed">
              <p>
                <strong>The Mission:</strong> To elevate personal electric vehicle (PEV) riding from a simple pastime into a connected, data-driven experience. Designed and engineered for riders worldwide who rely on e-bikes, e-trikes, and scooters for daily utility, trail scouting, and delivery work—proving that advanced micro-mobility thrives just as naturally on rural roads and backcountry trails as it does in big cities.
              </p>
              <p>
                From precise GPS speed tracking and offline-ready weather forecasting to real-time community radar and AI mechanical diagnostics, Rural ERides GO provides an uncompromised avionics experience right from your mobile device.
              </p>
            </div>

            {/* CORE FEATURE MATRIX */}
            <div className="space-y-3 pt-3">
               <span className="text-[9px] font-black uppercase tracking-widest text-zinc-400 block">Core Avionics Feature Matrix</span>
               <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {coreFeatures.map((feat, idx) => (
                    <div key={idx} className="bg-white/5 border border-white/10 p-4 rounded-2xl space-y-1 shadow-inner">
                       <div className="flex items-center gap-2 text-white font-black text-xs uppercase tracking-wide">
                          <feat.icon className={`w-4 h-4 ${activeTheme.text}`} /> {feat.title}
                       </div>
                       <p className="text-[11px] text-zinc-400 font-medium leading-snug">{feat.desc}</p>
                    </div>
                  ))}
               </div>
            </div>

            {/* PHONE & HOST HARDWARE DATA PANEL */}
            <div className="space-y-3 pt-3">
               <span className="text-[9px] font-black uppercase tracking-widest text-zinc-400 block flex items-center gap-1.5">
                 <Smartphone className="w-3.5 h-3.5 text-cyan-400"/> Host Phone &amp; Device Diagnostics
               </span>
               <div className="bg-black/70 border border-white/10 p-4 sm:p-5 rounded-2xl grid grid-cols-2 sm:grid-cols-4 gap-4 font-mono text-[10px]">
                  <div className="space-y-0.5">
                     <span className="text-zinc-500 uppercase font-bold block text-[8px]">Device Model</span>
                     <span className="text-white font-bold truncate block">{phoneData.model}</span>
                  </div>
                  <div className="space-y-0.5">
                     <span className="text-zinc-500 uppercase font-bold block text-[8px]">Operating System</span>
                     <span className="text-cyan-400 font-bold truncate block">{phoneData.os} ({phoneData.osVersion})</span>
                  </div>
                  <div className="space-y-0.5">
                     <span className="text-zinc-500 uppercase font-bold block text-[8px]">Pack Power</span>
                     <span className="text-emerald-400 font-bold truncate block">
                       {phoneData.batteryPct !== null ? `${phoneData.batteryPct}% ${phoneData.isCharging ? '⚡ (Charging)' : ''}` : 'AC Powered / External'}
                     </span>
                  </div>
                  <div className="space-y-0.5">
                     <span className="text-zinc-500 uppercase font-bold block text-[8px]">Display Viewport</span>
                     <span className="text-amber-400 font-bold truncate block">{phoneData.screenResolution}</span>
                  </div>
               </div>
            </div>

            {/* OPEN SOURCE PROJECT LINK */}
            <div className="mt-6 pt-5 border-t border-white/10 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
              <a 
                href="https://github.com/bradley3398/Rural-Erides-GO" 
                target="_blank" 
                rel="noopener noreferrer" 
                className={`min-h-[48px] flex items-center justify-center gap-2 text-[11px] sm:text-xs font-black text-black bg-cyan-400 hover:bg-cyan-300 py-3.5 px-6 ${cornerStyle} ${reducedMotion ? '' : 'active:scale-95 transition-all'} shadow-[0_0_20px_rgba(6,182,212,0.4)] cursor-pointer uppercase tracking-wider w-full`}
              >
                <Globe className="w-4 h-4" /> bradley3398/Rural-Erides-GO: For ebike enthusiast <ExternalLink className="w-3.5 h-3.5 ml-1"/>
              </a>
            </div>

          </div>

        </div>
      </div>
    </div>
  );
}