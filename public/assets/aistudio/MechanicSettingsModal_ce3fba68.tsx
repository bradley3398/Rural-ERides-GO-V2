"use client";

import React, { useState, useEffect } from "react";
import { 
  X, Settings2, User, Cpu, Volume2, ShoppingBag, BatteryCharging, Sparkles,
  ShieldAlert, Activity, Network, HardDrive, Radar, Zap, Cloud, Smartphone
} from "lucide-react";

export default function MechanicSettingsModal({
  showSettings, setShowSettings, callsign, userFleet, setUserFleet,
  aiModelVersion, setAiModelVersion, safeSearchLevel, setSafeSearchLevel,
  maxSpecResultsCount, setMaxSpecResultsCount, autoClearPhoto, setAutoClearPhoto,
  ttsRate, setTtsRate, ttsPitch, setTtsPitch, autoSaveLogs, setAutoSaveLogs,
  autoReadAloud, setAutoReadAloud, aiDetailLevel, setAiDetailLevel,
  searchScope, setSearchScope, userRegion, setUserRegion, preferredMarketplace,
  setPreferredMarketplace, unitSystem, setUnitSystem, theme, setTheme,
  saveConfiguration, themeColors
}: any) {
  if (!showSettings) return null;
  const t = themeColors;

  const broadcastSync = (key: string, value: string) => {
    try {
      localStorage.setItem(key, value);
      window.dispatchEvent(new Event("settings-sync"));
      window.dispatchEvent(new Event("theme-sync"));
    } catch (e) {}
  };

  // Base Settings & Dynamic Fleet States
  const [fleetMakeInput, setFleetMakeInput] = useState<string>("");
  const [fleetModelInput, setFleetModelInput] = useState<string>("");

  const [batteryChemistry, setBatteryChemistry] = useState<string>("Li-ion (NMC 21700/18650)");
  const [voltageCutoffBuffer, setVoltageCutoffBuffer] = useState<string>("Standard (20% Reserve)");
  const [ridingClassMode, setRidingClassMode] = useState<string>("unrestricted");
  const [torqueUnits, setTorqueUnits] = useState<string>("Nm");
  const [pressureUnits, setPressureUnits] = useState<string>("PSI");
  const [defaultVoltageBase, setDefaultVoltageBase] = useState<string>("72V");

  // Advanced Engineering & Telemetry Settings
  const [uiDensity, setUiDensity] = useState<string>("compact");
  const [aiTemperature, setAiTemperature] = useState<string>("analytical");
  const [telemetryPingRate, setTelemetryPingRate] = useState<string>("1000ms");
  const [offlineCacheLimit, setOfflineCacheLimit] = useState<string>("500MB");
  const [crashSensitivity, setCrashSensitivity] = useState<string>("high");
  const [stealthMode, setStealthMode] = useState<boolean>(false);
  const [hapticIntensity, setHapticIntensity] = useState<string>("medium");
  const [cloudSyncInterval, setCloudSyncInterval] = useState<string>("instant");

  useEffect(() => {
    // Load local hardware settings on mount & parse initial fleet input
    setTelemetryPingRate(localStorage.getItem("rt_telemetry_rate") || "1000ms");
    setOfflineCacheLimit(localStorage.getItem("rt_max_cache") || "500MB");
    setCrashSensitivity(localStorage.getItem("rt_crash_sens") || "high");
    setStealthMode(localStorage.getItem("rt_stealth_mode") === "true");
    setHapticIntensity(localStorage.getItem("rt_haptic_intensity") || "medium");
    setCloudSyncInterval(localStorage.getItem("rural_cloud_sync") || "instant");
    setUiDensity(localStorage.getItem("rt_dash_density") || "compact");

    // Force AI model to Gemini 3.1 Flash Lite as requested
    if (setAiModelVersion) {
      setAiModelVersion("gemini-3.1-flash-lite");
    }

    if (userFleet && userFleet.includes(" / ")) {
      const parts = userFleet.split(" / ");
      setFleetMakeInput(parts[0] || "");
      setFleetModelInput(parts[1] || "");
    } else if (userFleet) {
      setFleetMakeInput(userFleet);
    }
  }, [userFleet, setAiModelVersion]);

  const handleAdvancedSync = (key: string, value: string, setter: any) => {
    setter(value);
    broadcastSync(key, value);
  };

  const handleToggleSync = (key: string, value: boolean, setter: any) => {
    setter(value);
    broadcastSync(key, value.toString());
  };

  const updateFleetString = (make: string, model: string) => {
    const combined = `${make.trim()} ${model.trim()}`.trim() || "Universal Performance PEV Fleet";
    setUserFleet(combined);
    broadcastSync("pev_fleet", combined);
    broadcastSync("rural_erides_fleet", combined);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-md animate-fadeIn font-sans">
      <div className="bg-[#0a0a0f]/95 backdrop-blur-3xl border border-white/15 w-full max-w-5xl rounded-3xl p-4 sm:p-5 shadow-2xl relative z-10 flex flex-col max-h-[92vh]">
        
        {/* HEADER */}
        <div className="flex justify-between items-center border-b border-white/10 pb-3 mb-3 shrink-0">
          <div className="flex items-center gap-2">
            <div className={`p-2 rounded-xl bg-white/5 border border-white/10 ${t.text}`}>
              <Settings2 className="w-4 h-4" />
            </div>
            <div>
              <h3 className={`text-xs font-black uppercase tracking-widest ${t.text}`}>Master Terminal Configuration</h3>
              <p className="text-[8px] text-zinc-400 font-mono">Calibrate AI parameters, voice synthesis, hardware units &amp; system telemetry</p>
            </div>
          </div>
          <button 
            type="button" 
            onClick={() => setShowSettings(false)} 
            className="bg-white/5 border border-white/10 text-zinc-400 hover:text-white p-2 rounded-xl transition-colors cursor-pointer active:scale-95 min-h-[36px] min-w-[36px] flex items-center justify-center"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* SCROLLABLE SETTINGS CONTENT */}
        <div className="overflow-y-auto space-y-4 pr-1 custom-scrollbar">

          {/* SECTION 1: PILOT IDENTITY & VEHICLE FLEET (DYNAMIC MAKE/MODEL INPUT) */}
          <div className="space-y-2">
            <span className="text-[9px] font-mono font-black text-zinc-400 uppercase tracking-widest flex items-center gap-1.5 border-b border-white/5 pb-1">
              <User className={`w-3 h-3 ${t.text}`} /> 1. Operator Identity &amp; Active PEV Fleet Matrix
            </span>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-2.5">
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Operator Callsign</label>
                <input type="text" value={callsign} disabled={true} className="w-full bg-black/60 border border-white/10 text-[9px] sm:text-[10px] text-zinc-400 rounded-lg px-2.5 py-1.5 outline-none cursor-not-allowed font-mono font-bold min-h-[36px] shadow-inner" />
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">PEV Make</label>
                <input 
                  type="text" 
                  value={fleetMakeInput} 
                  onChange={(e) => { 
                    setFleetMakeInput(e.target.value); 
                    updateFleetString(e.target.value, fleetModelInput); 
                  }} 
                  placeholder="e.g. Dualtron, Sur-Ron..." 
                  className="w-full bg-black/50 border border-white/10 text-[9px] sm:text-[10px] text-white rounded-lg px-2.5 py-1.5 outline-none focus:border-white/30 transition-colors min-h-[36px] font-bold shadow-inner" 
                />
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">PEV Model</label>
                <input 
                  type="text" 
                  value={fleetModelInput} 
                  onChange={(e) => { 
                    setFleetModelInput(e.target.value); 
                    updateFleetString(fleetMakeInput, e.target.value); 
                  }} 
                  placeholder="e.g. Storm, Light Bee X..." 
                  className="w-full bg-black/50 border border-white/10 text-[9px] sm:text-[10px] text-white rounded-lg px-2.5 py-1.5 outline-none focus:border-white/30 transition-colors min-h-[36px] font-bold shadow-inner" 
                />
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Riding Class Regulation</label>
                <select value={ridingClassMode} onChange={(e) => setRidingClassMode(e.target.value)} className="w-full bg-black/50 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="unrestricted">Unrestricted / Off-Road Power</option>
                  <option value="class3">Class 3 (28 MPH Limit)</option>
                  <option value="class2">Class 2 (20 MPH Limit)</option>
                  <option value="eu_25kmh">EU Compliance (25 km/h)</option>
                </select>
              </div>
            </div>
          </div>

          {/* SECTION 2: AI REASONING & WEB SEARCH ENGINE (LOCKED TO GEMINI 3.1 FLASH LITE) */}
          <div className="space-y-2">
            <span className="text-[9px] font-mono font-black text-zinc-400 uppercase tracking-widest flex items-center gap-1.5 border-b border-white/5 pb-1">
              <Cpu className={`w-3 h-3 ${t.text}`} /> 2. AI Reasoning Core &amp; Web Scraper
            </span>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5 bg-black/40 p-3 rounded-xl border border-white/5">
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">AI Model Core</label>
                <select disabled={true} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-emerald-400 font-black outline-none font-mono cursor-not-allowed min-h-[36px]">
                  <option>Gemini 3.1 Flash-Lite (Active)</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Diagnostic Detail Depth</label>
                <select value={aiDetailLevel} onChange={(e) => setAiDetailLevel(e.target.value)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="compact">Compact Paragraph</option>
                  <option value="standard">Standard Format</option>
                  <option value="exhaustive">Exhaustive Teardown</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">AI Temperature Mode</label>
                <select value={aiTemperature} onChange={(e) => setAiTemperature(e.target.value)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="analytical">Strict Analytical (0.2)</option>
                  <option value="balanced">Balanced Default (0.4)</option>
                  <option value="creative">Creative / Brainstorm (0.7)</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Max Results Capacity</label>
                <select value={maxSpecResultsCount} onChange={(e) => setMaxSpecResultsCount(Number(e.target.value))} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value={12}>12 Records Pool</option>
                  <option value={24}>24 Records Pool</option>
                  <option value={50}>50 Max-Capacity Pool</option>
                  <option value={100}>100 Ultimate Nodes</option>
                </select>
              </div>
            </div>
          </div>

          {/* SECTION 3: VOICE SYNTHESIS & AUDIO ENGINE (TTS) */}
          <div className="space-y-2">
            <span className="text-[9px] font-mono font-black text-zinc-400 uppercase tracking-widest flex items-center gap-1.5 border-b border-white/5 pb-1">
              <Volume2 className={`w-3 h-3 ${t.text}`} /> 3. Audio Telemetry &amp; Voice Synthesis (TTS)
            </span>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5 bg-black/40 p-3 rounded-xl border border-white/5">
              <div className="space-y-1.5">
                <div className="flex justify-between items-center"><label className="text-[8px] text-zinc-400 uppercase font-black font-mono">Voice Speed</label><span className="text-white font-mono font-bold text-[9px]">{ttsRate.toFixed(1)}x</span></div>
                <input type="range" min="0.5" max="2.0" step="0.1" value={ttsRate} onChange={(e) => setTtsRate(Number(e.target.value))} className="w-full accent-emerald-500 cursor-pointer h-1.5" />
              </div>
              <div className="space-y-1.5">
                <div className="flex justify-between items-center"><label className="text-[8px] text-zinc-400 uppercase font-black font-mono">Voice Pitch</label><span className="text-white font-mono font-bold text-[9px]">{ttsPitch.toFixed(1)}</span></div>
                <input type="range" min="0.5" max="1.5" step="0.1" value={ttsPitch} onChange={(e) => setTtsPitch(Number(e.target.value))} className="w-full accent-cyan-500 cursor-pointer h-1.5" />
              </div>
              <div className="flex justify-between items-center p-2 bg-black/50 border border-white/5 rounded-lg min-h-[36px]">
                <div><span className="text-[8px] text-zinc-200 font-bold uppercase block font-mono">Auto Read-Aloud</span><span className="text-[7px] text-zinc-500 uppercase font-mono">Read AI replies</span></div>
                <button type="button" onClick={() => handleToggleSync("rt_auto_read", !autoReadAloud, setAutoReadAloud)} className={`relative inline-flex h-[18px] w-[34px] items-center rounded-full transition-colors cursor-pointer border border-white/10 shrink-0 ${autoReadAloud ? t.bg : 'bg-zinc-800'}`}>
                  <span className={`inline-block h-3 w-3 transform rounded-full bg-white transition shadow-sm ${autoReadAloud ? 'translate-x-[18px]' : 'translate-x-[2px]'}`} />
                </button>
              </div>
              <div className="flex justify-between items-center p-2 bg-black/50 border border-white/5 rounded-lg min-h-[36px]">
                <div><span className="text-[8px] text-zinc-200 font-bold uppercase block font-mono">Auto-Save Logs</span><span className="text-[7px] text-zinc-500 uppercase font-mono">Preserve history</span></div>
                <button type="button" onClick={() => handleToggleSync("rt_auto_save_logs", !autoSaveLogs, setAutoSaveLogs)} className={`relative inline-flex h-[18px] w-[34px] items-center rounded-full transition-colors cursor-pointer border border-white/10 shrink-0 ${autoSaveLogs ? t.bg : 'bg-zinc-800'}`}>
                  <span className={`inline-block h-3 w-3 transform rounded-full bg-white transition shadow-sm ${autoSaveLogs ? 'translate-x-[18px]' : 'translate-x-[2px]'}`} />
                </button>
              </div>
            </div>
          </div>

          {/* SECTION 4: HARDWARE SOURCING & MARKETPLACE CHANNELS */}
          <div className="space-y-2">
            <span className="text-[9px] font-mono font-black text-zinc-400 uppercase tracking-widest flex items-center gap-1.5 border-b border-white/5 pb-1">
              <ShoppingBag className={`w-3 h-3 ${t.text}`} /> 4. Component Sourcing &amp; Regional Trade Networks
            </span>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5">
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Preferred Sourcing Hub</label>
                <select value={preferredMarketplace} onChange={(e) => setPreferredMarketplace(e.target.value)} className="w-full bg-black/50 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="aggregated">Aggregated Multi-Platform</option>
                  <option value="amazon">Amazon Sourcing Priority</option>
                  <option value="ebay">eBay Direct Priority</option>
                  <option value="official">Official Brand Direct</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Component Tier Scope</label>
                <select value={searchScope} onChange={(e) => setSearchScope(e.target.value)} className="w-full bg-black/50 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="all">All Tiers (OEM+Aftermarket)</option>
                  <option value="oem_only">OEM Genuine Stock Only</option>
                  <option value="aftermarket_only">High-Performance Upgrades</option>
                  <option value="third_party">3rd Party Low-Cost Clones</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Geographic Trade Zone</label>
                <select value={userRegion} onChange={(e) => setUserRegion(e.target.value)} className="w-full bg-black/50 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="US">United States (USD)</option>
                  <option value="CA">Canada (CAD)</option>
                  <option value="UK">United Kingdom (GBP)</option>
                  <option value="EU">Eurozone (EUR)</option>
                  <option value="AU">Australia (AUD)</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">UI Density Formatting</label>
                <select value={uiDensity} onChange={(e) => handleAdvancedSync("rt_dash_density", e.target.value, setUiDensity)} className="w-full bg-black/50 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="compact">Compact Mobile (Max Data)</option>
                  <option value="standard">Standard Desktop Layout</option>
                </select>
              </div>
            </div>
          </div>

          {/* SECTION 5: ELECTRICAL METRICS & KINEMATICS MATRIX */}
          <div className="space-y-2">
            <span className="text-[9px] font-mono font-black text-zinc-400 uppercase tracking-widest flex items-center gap-1.5 border-b border-white/5 pb-1">
              <BatteryCharging className={`w-3 h-3 ${t.text}`} /> 5. Electrical Calibration &amp; Unit Standards
            </span>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-2.5 bg-black/40 p-3 rounded-xl border border-white/5">
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Measurement System</label>
                <div className="flex bg-black p-0.5 rounded-lg border border-white/10 min-h-[36px]">
                  <button type="button" onClick={() => { setUnitSystem("imperial"); broadcastSync("rt_use_metric", "false"); broadcastSync("rt_dist_unit", "mi"); }} className={`flex-1 text-[8px] font-mono font-black uppercase rounded cursor-pointer transition-all ${unitSystem === 'imperial' ? `${t.bg} text-black` : 'text-zinc-400 hover:text-white'}`}>Imperial</button>
                  <button type="button" onClick={() => { setUnitSystem("metric"); broadcastSync("rt_use_metric", "true"); broadcastSync("rt_dist_unit", "km"); }} className={`flex-1 text-[8px] font-mono font-black uppercase rounded cursor-pointer transition-all ${unitSystem === 'metric' ? `${t.bg} text-black` : 'text-zinc-400 hover:text-white'}`}>Metric</button>
                </div>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Battery Cell Chemistry</label>
                <select value={batteryChemistry} onChange={(e) => setBatteryChemistry(e.target.value)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="Li-ion (NMC 21700/18650)">Li-Ion NMC (3.6V/4.2V)</option>
                  <option value="LiFePO4 (LFP)">LiFePO4 (3.2V/3.65V)</option>
                  <option value="LTO (Lithium Titanate)">LTO Titanate (2.4V/2.8V)</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">System Default Voltage</label>
                <select value={defaultVoltageBase} onChange={(e) => setDefaultVoltageBase(e.target.value)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="36V">36V System</option>
                  <option value="48V">48V System</option>
                  <option value="52V">52V System</option>
                  <option value="60V">60V Performance</option>
                  <option value="72V">72V Hyper-Class</option>
                  <option value="84V">84V Extreme Mod</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">LVC Cutoff Buffer</label>
                <select value={voltageCutoffBuffer} onChange={(e) => setVoltageCutoffBuffer(e.target.value)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="Standard (20% Reserve)">Cons. 20% Reserve</option>
                  <option value="Aggressive (10% Reserve)">Aggr. 10% Reserve</option>
                  <option value="Ultra Safe (30% Reserve)">Safe 30% Buffer</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Torque Unit</label>
                <select value={torqueUnits} onChange={(e) => setTorqueUnits(e.target.value)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="Nm">Nm (Metric)</option>
                  <option value="Ft-Lbs">Ft-Lbs (US)</option>
                </select>
              </div>
            </div>
          </div>

          {/* SECTION 6: ADVANCED SYSTEM TELEMETRY */}
          <div className="space-y-2">
            <span className="text-[9px] font-mono font-black text-zinc-400 uppercase tracking-widest flex items-center gap-1.5 border-b border-white/5 pb-1">
              <Radar className={`w-3 h-3 ${t.text}`} /> 6. Advanced Hardware Telemetry Config
            </span>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5 bg-black/40 p-3 rounded-xl border border-white/5">
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">GPS Ping Rate</label>
                <select value={telemetryPingRate} onChange={(e) => handleAdvancedSync("rt_telemetry_rate", e.target.value, setTelemetryPingRate)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="500ms">Aggressive (500ms)</option>
                  <option value="1000ms">Standard (1s)</option>
                  <option value="3000ms">Battery Saver (3s)</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Offline Map Cache</label>
                <select value={offlineCacheLimit} onChange={(e) => handleAdvancedSync("rt_max_cache", e.target.value, setOfflineCacheLimit)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="100MB">100MB (Local Area)</option>
                  <option value="500MB">500MB (Regional)</option>
                  <option value="2GB">2GB (Full State)</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">IMU Crash Sensitivity</label>
                <select value={crashSensitivity} onChange={(e) => handleAdvancedSync("rt_crash_sens", e.target.value, setCrashSensitivity)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="high">High (Trail Riding)</option>
                  <option value="normal">Normal (Street Commute)</option>
                  <option value="low">Low (Avoid False Positives)</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Haptic Intensity</label>
                <select value={hapticIntensity} onChange={(e) => handleAdvancedSync("rt_haptic_intensity", e.target.value, setHapticIntensity)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="light">Light Tap</option>
                  <option value="medium">Medium Pulse</option>
                  <option value="heavy">Heavy Feedback</option>
                </select>
              </div>
              <div className="space-y-1">
                <label className="text-[8px] text-zinc-400 font-black uppercase font-mono">Cloud Sync Interval</label>
                <select value={cloudSyncInterval} onChange={(e) => handleAdvancedSync("rural_cloud_sync", e.target.value, setCloudSyncInterval)} className="w-full bg-black/60 border border-white/10 rounded-lg px-2 py-1.5 text-[9px] text-white font-bold outline-none font-mono cursor-pointer min-h-[36px]">
                  <option value="instant">Instant (Real-time)</option>
                  <option value="hourly">Hourly Batch</option>
                  <option value="daily">Daily Backup</option>
                </select>
              </div>
              <div className="flex justify-between items-center p-2 bg-black/50 border border-white/5 rounded-lg min-h-[36px] mt-4 col-span-2">
                <div><span className="text-[8px] text-zinc-200 font-bold uppercase block font-mono">Stealth Mode</span><span className="text-[7px] text-zinc-500 uppercase font-mono">Mute all system audio & haptics</span></div>
                <button type="button" onClick={() => handleToggleSync("rt_stealth_mode", !stealthMode, setStealthMode)} className={`relative inline-flex h-[18px] w-[34px] items-center rounded-full transition-colors cursor-pointer border border-white/10 shrink-0 ${stealthMode ? t.bg : 'bg-zinc-800'}`}>
                  <span className={`inline-block h-3 w-3 transform rounded-full bg-white transition shadow-sm ${stealthMode ? 'translate-x-[18px]' : 'translate-x-[2px]'}`} />
                </button>
              </div>
            </div>
          </div>

          {/* SECTION 7: UI THEME ACCENTS */}
          <div className="space-y-2">
            <span className="text-[10px] font-mono font-black text-zinc-400 uppercase tracking-widest flex items-center gap-1.5 border-b border-white/5 pb-1"><Sparkles className={`w-3 h-3 ${t.text}`} /> 7. Terminal Accent &amp; UI Color Profile</span>
            <div className="flex items-center gap-2 pt-1 flex-wrap">
              {[
                { id: 'cyan', label: 'Cyan High-Vis', color: 'bg-cyan-500' },
                { id: 'emerald', label: 'Emerald Pure', color: 'bg-emerald-500' },
                { id: 'amber', label: 'Amber Trail', color: 'bg-amber-500' },
                { id: 'rose', label: 'Rose Critical', color: 'bg-rose-500' },
                { id: 'purple', label: 'Violet Core', color: 'bg-purple-500' },
                { id: 'void', label: 'Monochrome Void', color: 'bg-zinc-200' },
                { id: 'lime', label: 'Lime High-Vis', color: 'bg-[#39ff14]' },
              ].map(th => (
                <button
                  key={th.id} type="button"
                  onClick={() => { setTheme(th.id as ThemeColor); broadcastSync("pev_theme", th.id); broadcastSync("rt_theme", th.id); }}
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-xl border transition-all cursor-pointer min-h-[36px] ${theme === th.id ? `${t.dim} font-black shadow-md` : 'bg-black/50 border-white/10 text-zinc-400 hover:text-white'}`}
                >
                  <span className={`w-3 h-3 rounded-full ${th.color} shadow-sm`} />
                  <span className="text-[9px] font-mono uppercase font-bold">{th.label}</span>
                </button>
              ))}
            </div>
          </div>

        </div>

        {/* FOOTER ACTIONS */}
        <div className="mt-4 pt-3 border-t border-white/10 flex justify-end gap-2.5 shrink-0">
          <button type="button" onClick={() => setShowSettings(false)} className="bg-white/5 border border-white/10 text-zinc-300 hover:text-white px-5 min-h-[44px] rounded-xl font-black uppercase font-mono text-[9px] cursor-pointer active:scale-95">Cancel/Close</button>
          <button type="button" onClick={saveConfiguration} className={`${t.bg} text-black px-6 min-h-[44px] rounded-xl font-black uppercase font-mono tracking-widest text-[10px] shadow-lg cursor-pointer active:scale-95 transition-all`}>Save &amp; Sync Matrix</button>
        </div>

      </div>
    </div>
  );
}