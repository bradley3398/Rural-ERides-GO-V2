"use client";

import React, { useState, useEffect } from "react";
import { 
  Truck, Save, Play, Plus, Minus, Trash2, Zap, Battery, 
  Cpu, Layers, Sliders, Shield, Disc, Scale, Flame, Wrench, Wind
} from "lucide-react";
import { motion } from "framer-motion";

const safeNum = (val: any, fallback = 0): number => {
  const parsed = Number(val);
  return isNaN(parsed) || !isFinite(parsed) ? fallback : parsed;
};

export interface PEVProfileSetupProps {
  isTracking?: boolean;
  handleSaveCurrentPevProfile?: () => void;
  savedPevProfiles?: any[];
  handleLoadPevProfile?: (profile: any) => void;
  customVehicleName?: string;
  setCustomVehicleName?: (name: string) => void;
  pevType?: string;
  setPevType?: (type: string) => void;
  wheelSize?: number;
  setWheelSize?: (size: number) => void;
  batteryPercent?: number;
  setBatteryPercent?: (pct: number) => void;
  batteryCapacity?: number;
  setBatteryCapacity?: (ah: number) => void;
  peakVoltage?: number | null;
  setPeakVoltage?: (v: number) => void;
  motorWattage?: number;
  setMotorWattage?: (w: number) => void;
  controllerAmps?: number;
  setControllerAmps?: (a: number) => void;
  batteryVoltage?: number;
  setBatteryVoltage?: (v: number) => void;
  startTracking?: () => void;
  triggerHaptic?: () => void;
  ui?: any;
  tx?: any;
  useMetric?: boolean;
}

export default function PEVProfileSetup(props: PEVProfileSetupProps) {
  const {
    isTracking = false,
    handleSaveCurrentPevProfile,
    savedPevProfiles = [],
    handleLoadPevProfile,
    customVehicleName = "",
    setCustomVehicleName = () => {},
    pevType = "Scooter",
    setPevType = () => {},
    wheelSize = 10,
    setWheelSize = () => {},
    batteryPercent = 100,
    setBatteryPercent = () => {},
    batteryCapacity = 15,
    setBatteryCapacity = () => {},
    peakVoltage = 54.6,
    setPeakVoltage = () => {},
    motorWattage = 1000,
    setMotorWattage = () => {},
    controllerAmps = 25,
    setControllerAmps = () => {},
    batteryVoltage = 48,
    setBatteryVoltage = () => {},
    startTracking = () => {},
    triggerHaptic = () => {},
    ui,
    tx,
    useMetric = false
  } = props;

  const [localMetric, setLocalMetric] = useState<boolean>(useMetric);

  // --- MASTER SETTINGS SYNC ENGINE ---
  useEffect(() => {
    const handleSettingsSync = () => {
      const isMetric = localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km";
      setLocalMetric(isMetric);
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
  }, []);

  useEffect(() => {
    if (useMetric !== undefined) setLocalMetric(useMetric);
  }, [useMetric]);

  const distLabel = localMetric ? "KM" : "MI";

  const [activeSetupTab, setActiveSetupTab] = useState<"garage" | "profile" | "powertrain" | "battery" | "chassis">("garage");
  const [localProfiles, setLocalProfiles] = useState<any[]>(savedPevProfiles);
  
  useEffect(() => {
    setLocalProfiles(savedPevProfiles);
  }, [savedPevProfiles]);

  const [driveTopology, setDriveTopology] = useState<string>("Single Rear Hub");
  const [brakeType, setBrakeType] = useState<string>("Hydraulic Dual Piston");
  const [tireCompound, setTireCompound] = useState<string>("Street Pneumatic");
  const [tirePressurePsi, setTirePressurePsi] = useState<number>(35);
  const [regenBrakeStrength, setRegenBrakeStrength] = useState<number>(15);
  const [batteryChemistry, setBatteryChemistry] = useState<"Li-Ion" | "LiFePO4" | "LTO">("Li-Ion");
  const [cutoffVoltage, setCutoffVoltage] = useState<number>(39.0);
  const [towingPayload, setTowingPayload] = useState<boolean>(false);
  const [motorKvRating, setMotorKvRating] = useState<number>(15);
  const [aeroStance, setAeroStance] = useState<string>("Upright Commuter");
  const [isSavedSuccess, setIsSavedSuccess] = useState(false);

  if (isTracking) return null;

  const totalWattHours = safeNum(batteryVoltage) * safeNum(batteryCapacity);
  const peakPowerWatts = safeNum(batteryVoltage) * safeNum(controllerAmps);
  const continuousHP = (safeNum(motorWattage) / 745.7).toFixed(2);
  const peakHP = (peakPowerWatts / 745.7).toFixed(2);
  
  const baseEfficiencyWh = pevType === "Bike" ? 18 : pevType === "Unicycle" ? 22 : 25;
  const towingDragMultiplier = towingPayload ? 1.45 : 1.0;
  const tireDragMultiplier = tireCompound.includes("Knobby") ? 1.15 : 1.0;
  const aeroMultiplier = aeroStance.includes("Tuck") ? 0.82 : aeroStance.includes("Cargo") ? 1.35 : 1.0;
  const effectiveWhPerUnit = baseEfficiencyWh * towingDragMultiplier * tireDragMultiplier * aeroMultiplier * (localMetric ? 0.621371 : 1.0);
  const projectedMaxRange = (totalWattHours * (safeNum(batteryPercent, 100) / 100)) / effectiveWhPerUnit;

  const adjustValue = (setter: (val: number) => void, current: number, delta: number, min = 1, max = 9999) => {
    triggerHaptic();
    const nextVal = Math.max(min, Math.min(max, Math.round((current + delta) * 10) / 10));
    setter(nextVal);
  };

  const handleApplyChemistry = (chem: "Li-Ion" | "LiFePO4" | "LTO") => {
    triggerHaptic();
    setBatteryChemistry(chem);
    const nominal = safeNum(batteryVoltage, 48);
    
    if (chem === "Li-Ion") {
      setPeakVoltage(Math.round(nominal * 1.1666 * 10) / 10);
      setCutoffVoltage(Math.round(nominal * 0.8125 * 10) / 10);
    } else if (chem === "LiFePO4") {
      setPeakVoltage(Math.round(nominal * 1.1406 * 10) / 10);
      setCutoffVoltage(Math.round(nominal * 0.8750 * 10) / 10);
    } else if (chem === "LTO") {
      setPeakVoltage(Math.round(nominal * 1.2000 * 10) / 10);
      setCutoffVoltage(Math.round(nominal * 0.7916 * 10) / 10);
    }
  };

  const handleUpdateVehicleBattery = (id: string, newPct: number) => {
    triggerHaptic();
    const updated = localProfiles.map(p => p.id === id ? { ...p, batteryPercent: newPct } : p);
    setLocalProfiles(updated);
    try {
      localStorage.setItem("rural_saved_pev_profiles", JSON.stringify(updated));
    } catch (e) {}
  };

  const handleSaveProfile = () => {
    triggerHaptic();
    const profileName = customVehicleName.trim() || `Custom ${pevType}`;
    const uniqueId = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    const fullProfile = {
      id: uniqueId,
      name: profileName,
      type: pevType,
      wheels: wheelSize,
      ah: batteryCapacity,
      volts: batteryVoltage,
      peak: peakVoltage || (batteryVoltage * 1.166),
      cutoff: cutoffVoltage,
      watts: motorWattage,
      amps: controllerAmps,
      chemistry: batteryChemistry,
      driveTopology,
      brakeType,
      tireCompound,
      tirePressurePsi,
      regenBrakeStrength,
      towingPayload,
      motorKvRating,
      aeroStance,
      batteryPercent: safeNum(batteryPercent, 100)
    };

    try {
      const existing = JSON.parse(localStorage.getItem("rural_saved_pev_profiles") || "[]");
      const updated = [...existing.filter((p: any) => p.name !== profileName), fullProfile];
      localStorage.setItem("rural_saved_pev_profiles", JSON.stringify(updated));
      setLocalProfiles(updated);
      setIsSavedSuccess(true);
      setTimeout(() => setIsSavedSuccess(false), 2200);
      if (handleSaveCurrentPevProfile) handleSaveCurrentPevProfile();
    } catch (e) {
      alert("Failed to save profile.");
    }
  };

  const loadSavedProfile = (p: any) => {
    triggerHaptic();
    if (handleLoadPevProfile) handleLoadPevProfile(p);
    if (p.batteryPercent !== undefined && setBatteryPercent) setBatteryPercent(p.batteryPercent);
    if (p.driveTopology) setDriveTopology(p.driveTopology);
    if (p.brakeType) setBrakeType(p.brakeType);
    if (p.tireCompound) setTireCompound(p.tireCompound);
    if (p.tirePressurePsi) setTirePressurePsi(p.tirePressurePsi);
    if (p.regenBrakeStrength !== undefined) setRegenBrakeStrength(p.regenBrakeStrength);
    if (p.chemistry) setBatteryChemistry(p.chemistry);
    if (p.cutoff) setCutoffVoltage(p.cutoff);
    if (p.towingPayload !== undefined) setTowingPayload(p.towingPayload);
    if (p.motorKvRating) setMotorKvRating(p.motorKvRating);
    if (p.aeroStance) setAeroStance(p.aeroStance);
  };

  return (
    <div className={`p-3 sm:p-4 rounded-2xl border ${ui?.bgPanel || 'bg-[#0d0e15]'} shadow-xl space-y-3 ${ui?.brd || 'border-zinc-800'}`}>
      
      <div className={`flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 border-b ${ui?.brd || 'border-zinc-800'} pb-2.5`}>
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 shadow-sm">
            <Truck className="w-4 h-4 animate-pulse" />
          </div>
          <div>
            <h3 className={`${ui?.txtMain || 'text-white'} font-black uppercase tracking-widest text-[11px] sm:text-xs flex items-center gap-1.5`}>
              PEV Powertrain & Hardware Profile ({distLabel})
            </h3>
            <p className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase mt-0.5`}>
              Custom Specifications & Energy Modeling
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 w-full sm:w-auto">
          <button
            onClick={handleSaveProfile}
            className={`flex-1 sm:flex-none min-h-[36px] px-3 rounded-lg text-[9.5px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 cursor-pointer active:scale-95 touch-manipulation shadow-sm ${isSavedSuccess ? 'bg-emerald-500 text-black border-emerald-400' : 'bg-cyan-600 hover:bg-cyan-500 text-black border-cyan-500'}`}
          >
            <Save className="w-3.5 h-3.5"/> {isSavedSuccess ? "Saved!" : "Save Profile"}
          </button>
        </div>
      </div>

      <div className="flex gap-1.5 overflow-x-auto custom-scrollbar pb-1 -mx-1 px-1 touch-pan-x">
        {[
          { id: "garage", label: `Garage (${localProfiles.length})`, icon: Layers },
          { id: "profile", label: "Overview", icon: Sliders },
          { id: "powertrain", label: "Motor & ESC", icon: Cpu },
          { id: "battery", label: "Battery", icon: Battery },
          { id: "chassis", label: "Chassis", icon: Disc },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => { triggerHaptic(); setActiveSetupTab(tab.id as any); }}
            className={`min-h-[36px] px-3 rounded-lg text-[9.5px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 shrink-0 cursor-pointer active:scale-95 touch-manipulation ${activeSetupTab === tab.id ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-md ring-1 ring-white/20` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} border ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
          >
            <tab.icon className="w-3.5 h-3.5 shrink-0" /> {tab.label}
          </button>
        ))}
      </div>

      {activeSetupTab === "garage" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-2.5">
          {localProfiles.length === 0 ? (
            <div className={`text-center py-6 ${ui?.bgCard || 'bg-black/60'} border border-dashed ${ui?.brd || 'border-zinc-800'} rounded-2xl space-y-1.5`}>
              <Truck className="w-6 h-6 text-zinc-500 mx-auto" />
              <div className="text-[10px] font-mono uppercase text-zinc-400 font-bold">No Custom Profiles Saved</div>
              <p className="text-[8px] text-zinc-500 font-mono">Configure specs under Overview and tap Save Profile</p>
            </div>
          ) : (
            <div className="space-y-2 max-h-[300px] overflow-y-auto custom-scrollbar pr-1">
              {localProfiles.map((p: any) => (
                <div 
                  key={p.id}
                  className={`p-3 rounded-xl border ${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} space-y-2 shadow-inner`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2.5 truncate cursor-pointer" onClick={() => loadSavedProfile(p)}>
                      <div className="w-8 h-8 rounded-lg bg-cyan-950/40 border border-cyan-800/60 text-cyan-400 flex items-center justify-center font-bold text-xs shrink-0">
                        ⚡
                      </div>
                      <div className="truncate">
                        <div className="text-[11px] font-mono font-black text-white truncate">{p.name}</div>
                        <div className="text-[8px] font-mono text-zinc-400 mt-0.5">
                          {p.watts}W • {p.volts}V ({p.ah}Ah) • {p.wheels}"
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      <button 
                        onClick={() => loadSavedProfile(p)}
                        className="min-h-[32px] px-2.5 rounded-lg text-[8.5px] font-mono font-black text-[#39ff14] bg-[#39ff14]/10 border border-[#39ff14]/30 cursor-pointer active:scale-95"
                      >
                        LOAD
                      </button>
                      <button 
                        onClick={() => {
                          triggerHaptic();
                          if (confirm(`Delete profile "${p.name}"?`)) {
                            const existing = JSON.parse(localStorage.getItem("rural_saved_pev_profiles") || "[]");
                            const filtered = existing.filter((item: any) => item.id !== p.id);
                            localStorage.setItem("rural_saved_pev_profiles", JSON.stringify(filtered));
                            setLocalProfiles(filtered);
                          }
                        }}
                        className="min-h-[32px] min-w-[32px] flex items-center justify-center rounded-lg bg-rose-950/40 border border-rose-900/60 text-rose-400 hover:bg-rose-600 hover:text-white transition-colors cursor-pointer"
                        title="Delete Profile"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    </div>
                  </div>

                  <div className="bg-black/50 p-2.5 rounded-lg border border-zinc-800/80 space-y-1">
                    <div className="flex justify-between items-center text-[8.5px] font-mono">
                      <span className="text-zinc-400 uppercase font-bold flex items-center gap-1">
                        <Battery className="w-3 h-3 text-emerald-400" /> Battery Charge:
                      </span>
                      <span className="text-[#39ff14] font-black">{safeNum(p.batteryPercent, 100)}%</span>
                    </div>
                    <input 
                      type="range" min="5" max="100" step="1"
                      value={safeNum(p.batteryPercent, 100)} 
                      onChange={(e) => handleUpdateVehicleBattery(p.id, Number(e.target.value))}
                      className="w-full accent-[#39ff14] h-1.5 bg-zinc-900 rounded-lg cursor-pointer"
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </motion.div>
      )}

      {activeSetupTab === "profile" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>
                Vehicle Signature
              </label>
              <input 
                type="text" 
                value={customVehicleName} 
                onChange={e => setCustomVehicleName(e.target.value)} 
                placeholder="e.g. Custom Build V1" 
                className={`w-full min-h-[38px] ${ui?.bgInput || 'bg-black'} p-2.5 rounded-lg font-mono font-bold text-xs text-white border ${ui?.brd || 'border-zinc-800'} focus:border-cyan-500 outline-none`} 
              />
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>
                Class Architecture
              </label>
              <select 
                value={pevType} 
                onChange={e => setPevType(e.target.value)} 
                className={`w-full min-h-[38px] ${ui?.bgInput || 'bg-black'} p-2.5 rounded-lg font-mono font-bold text-xs text-white border ${ui?.brd || 'border-zinc-800'} cursor-pointer outline-none`}
              >
                <option value="Scooter">🛴 Electric Scooter</option>
                <option value="Bike">🚲 Electric Bicycle</option>
                <option value="Unicycle">⚡ Electric Unicycle (EUC)</option>
                <option value="Skateboard">🛹 Electric Skateboard</option>
              </select>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <div className="flex justify-between items-center">
                <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>
                  Starting Battery (%)
                </label>
                <span className="text-[10px] font-mono font-black text-[#39ff14]">{safeNum(batteryPercent, 100)}%</span>
              </div>
              <div className="flex items-center gap-2 pt-1">
                <input 
                  type="range" min="5" max="100" step="1"
                  value={safeNum(batteryPercent, 100)} 
                  onChange={e => setBatteryPercent(Number(e.target.value))}
                  className="w-full accent-[#39ff14] h-1.5 bg-zinc-900 rounded-lg cursor-pointer"
                />
              </div>
            </div>
          </div>

          <div className={`p-3 rounded-xl bg-black/60 border ${ui?.brd || 'border-zinc-800'} shadow-inner grid grid-cols-2 sm:grid-cols-4 gap-2 text-center`}>
            <div>
              <span className="text-[7.5px] font-mono uppercase text-zinc-500 block">Mechanical Output</span>
              <span className="text-xs font-mono font-black text-cyan-400">{continuousHP} HP ({peakHP} HP Peak)</span>
            </div>
            <div>
              <span className="text-[7.5px] font-mono uppercase text-zinc-500 block">Pack Capacity</span>
              <span className="text-xs font-mono font-black text-amber-400">{totalWattHours} Wh</span>
            </div>
            <div>
              <span className="text-[7.5px] font-mono uppercase text-zinc-500 block">Calculated Drag</span>
              <span className="text-xs font-mono font-black text-purple-400">{effectiveWhPerUnit.toFixed(1)} Wh/{distLabel.toLowerCase()}</span>
            </div>
            <div>
              <span className="text-[7.5px] font-mono uppercase text-zinc-500 block">Estimated Range</span>
              <span className="text-xs font-mono font-black text-[#39ff14]">~{projectedMaxRange.toFixed(1)} {distLabel.toLowerCase()}</span>
            </div>
          </div>
        </motion.div>
      )}

      {activeSetupTab === "powertrain" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>
                Drive Architecture
              </label>
              <select 
                value={driveTopology} 
                onChange={e => setDriveTopology(e.target.value)} 
                className={`w-full min-h-[38px] ${ui?.bgInput || 'bg-black'} p-2.5 rounded-lg font-mono font-bold text-xs text-white border ${ui?.brd || 'border-zinc-800'} cursor-pointer outline-none`}
              >
                <option value="Single Rear Hub">Single Rear Direct-Drive Hub</option>
                <option value="Front Direct Drive Hub">Front Hub Motor</option>
                <option value="Dual Hub All-Wheel Drive">Dual Hub AWD</option>
                <option value="Mid-Drive Crank Motor">Mid-Drive Crank Motor</option>
                <option value="Dual Belt Outrunner">Dual Belt Outrunner</option>
              </select>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>
                Motor KV Rating
              </label>
              <div className="flex items-center gap-2 pt-1">
                <input 
                  type="range" min="8" max="45" step="1"
                  value={motorKvRating} 
                  onChange={e => setMotorKvRating(Number(e.target.value))}
                  className="w-full accent-cyan-400 h-1.5 bg-zinc-900 rounded-lg cursor-pointer"
                />
                <span className="text-[10px] font-mono font-black text-cyan-400 w-10 text-right">{motorKvRating}K</span>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>
                Regenerative Braking
              </label>
              <div className="flex items-center gap-2 pt-1">
                <input 
                  type="range" min="0" max="40" step="5"
                  value={regenBrakeStrength} 
                  onChange={e => setRegenBrakeStrength(Number(e.target.value))}
                  className="w-full accent-emerald-400 h-1.5 bg-zinc-900 rounded-lg cursor-pointer"
                />
                <span className="text-[10px] font-mono font-black text-emerald-400 w-10 text-right">{regenBrakeStrength}%</span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between space-y-1.5`}>
              <div className="flex justify-between items-center">
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>Motor Output (Watts)</span>
                <span className="text-[11px] font-mono font-black text-amber-400">{motorWattage}W</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjustValue(setMotorWattage, safeNum(motorWattage), -100, 250, 10000)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input type="number" value={motorWattage} onChange={e => setMotorWattage(Number(e.target.value))} className={`flex-1 min-h-[38px] bg-black text-center text-xs font-mono font-black text-white rounded-lg border ${ui?.brd || 'border-zinc-800'} outline-none`} />
                <button onClick={() => adjustValue(setMotorWattage, safeNum(motorWattage), 100, 250, 10000)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between space-y-1.5`}>
              <div className="flex justify-between items-center">
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>ESC Current Limit (Amps)</span>
                <span className="text-[11px] font-mono font-black text-rose-400">{controllerAmps}A</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjustValue(setControllerAmps, safeNum(controllerAmps), -2, 5, 150)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input type="number" value={controllerAmps} onChange={e => setControllerAmps(Number(e.target.value))} className={`flex-1 min-h-[38px] bg-black text-center text-xs font-mono font-black text-white rounded-lg border ${ui?.brd || 'border-zinc-800'} outline-none`} />
                <button onClick={() => adjustValue(setControllerAmps, safeNum(controllerAmps), 2, 5, 150)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        </motion.div>
      )}

      {activeSetupTab === "battery" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center bg-black/40 p-3 rounded-xl border border-zinc-800 gap-2">
            <div>
              <span className="text-[9.5px] font-black uppercase tracking-widest text-white flex items-center gap-1.5">
                <Shield className="w-3.5 h-3.5 text-cyan-400" /> Battery Chemistry Tuning
              </span>
              <p className="text-[8px] text-zinc-400 font-mono mt-0.5">
                Configures voltage curves and cutoff thresholds
              </p>
            </div>
            <div className="flex gap-1">
              {(["Li-Ion", "LiFePO4", "LTO"] as const).map((chem) => (
                <button
                  key={chem}
                  onClick={() => handleApplyChemistry(chem)}
                  className={`min-h-[34px] px-3 rounded-lg text-[8.5px] font-mono font-black uppercase transition-all cursor-pointer ${batteryChemistry === chem ? 'bg-cyan-600 text-black shadow-sm' : 'bg-zinc-900 border border-zinc-800 text-zinc-400'}`}
                >
                  {chem}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between space-y-1.5`}>
              <div className="flex justify-between items-center">
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>Nominal Voltage</span>
                <span className="text-[11px] font-mono font-black text-cyan-400">{batteryVoltage}V</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjustValue(setBatteryVoltage, safeNum(batteryVoltage), -4, 24, 120)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input type="number" value={batteryVoltage} onChange={e => setBatteryVoltage(Number(e.target.value))} className={`flex-1 min-h-[38px] bg-black text-center text-xs font-mono font-black text-white rounded-lg border ${ui?.brd || 'border-zinc-800'} outline-none`} />
                <button onClick={() => adjustValue(setBatteryVoltage, safeNum(batteryVoltage), 4, 24, 120)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between space-y-1.5`}>
              <div className="flex justify-between items-center">
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>Capacity (Ah)</span>
                <span className="text-[11px] font-mono font-black text-emerald-400">{batteryCapacity}Ah</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjustValue(setBatteryCapacity, safeNum(batteryCapacity), -1, 2, 100)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input type="number" value={batteryCapacity} onChange={e => setBatteryCapacity(Number(e.target.value))} className={`flex-1 min-h-[38px] bg-black text-center text-xs font-mono font-black text-white rounded-lg border ${ui?.brd || 'border-zinc-800'} outline-none`} />
                <button onClick={() => adjustValue(setBatteryCapacity, safeNum(batteryCapacity), 1, 2, 100)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between space-y-1.5`}>
              <div className="flex justify-between items-center">
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>Cutoff Threshold</span>
                <span className="text-[11px] font-mono font-black text-rose-400">{cutoffVoltage}V</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjustValue(setCutoffVoltage, safeNum(cutoffVoltage), -0.5, 20, 100)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input type="number" step="0.1" value={cutoffVoltage} onChange={e => setCutoffVoltage(Number(e.target.value))} className={`flex-1 min-h-[38px] bg-black text-center text-xs font-mono font-black text-white rounded-lg border ${ui?.brd || 'border-zinc-800'} outline-none`} />
                <button onClick={() => adjustValue(setCutoffVoltage, safeNum(cutoffVoltage), 0.5, 20, 100)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        </motion.div>
      )}

      {activeSetupTab === "chassis" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>
                Braking Hardware System
              </label>
              <select 
                value={brakeType} 
                onChange={e => setBrakeType(e.target.value)} 
                className={`w-full min-h-[38px] ${ui?.bgInput || 'bg-black'} p-2.5 rounded-lg font-mono font-bold text-xs text-white border ${ui?.brd || 'border-zinc-800'} cursor-pointer outline-none`}
              >
                <option value="Hydraulic Dual Piston">Mineral Oil Hydraulic Dual-Piston</option>
                <option value="Cable-Actuated Hydraulic">Cable-Actuated Hydraulic</option>
                <option value="Mechanical Wire Disc">Mechanical Cable Disc</option>
                <option value="Electronic KERS Regen">Electronic Motor Regen Only</option>
              </select>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>
                Tire Tread Profile
              </label>
              <select 
                value={tireCompound} 
                onChange={e => setTireCompound(e.target.value)} 
                className={`w-full min-h-[38px] ${ui?.bgInput || 'bg-black'} p-2.5 rounded-lg font-mono font-bold text-xs text-white border ${ui?.brd || 'border-zinc-800'} cursor-pointer outline-none`}
              >
                <option value="Street Pneumatic">Street Pneumatic (Low Drag)</option>
                <option value="All-Terrain Knobby">All-Terrain Knobby (+15% Drag)</option>
                <option value="Semi-Slick Dual-Sport">Semi-Slick Dual-Sport</option>
                <option value="Solid Airless">Solid Airless Compound</option>
              </select>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <label className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>
                Aerodynamic Stance
              </label>
              <select 
                value={aeroStance} 
                onChange={e => setAeroStance(e.target.value)} 
                className={`w-full min-h-[38px] ${ui?.bgInput || 'bg-black'} p-2.5 rounded-lg font-mono font-bold text-xs text-white border ${ui?.brd || 'border-zinc-800'} cursor-pointer outline-none`}
              >
                <option value="Upright Commuter">Upright Commuter</option>
                <option value="Aero Tuck">Racing Aero Tuck (-18% Drag)</option>
                <option value="Cargo Packed">Cargo Packed (+35% Drag)</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between space-y-1.5`}>
              <div className="flex justify-between items-center">
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>Wheel Diameter</span>
                <span className="text-[11px] font-mono font-black text-purple-400">{wheelSize}"</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjustValue(setWheelSize, safeNum(wheelSize), -1, 6, 32)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input type="number" step="0.5" value={wheelSize} onChange={e => setWheelSize(Number(e.target.value))} className={`flex-1 min-h-[38px] bg-black text-center text-xs font-mono font-black text-white rounded-lg border ${ui?.brd || 'border-zinc-800'} outline-none`} />
                <button onClick={() => adjustValue(setWheelSize, safeNum(wheelSize), 1, 6, 32)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between space-y-1.5`}>
              <div className="flex justify-between items-center">
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>Tire Pressure (PSI)</span>
                <span className="text-[11px] font-mono font-black text-amber-400">{tirePressurePsi} PSI</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjustValue(setTirePressurePsi, safeNum(tirePressurePsi), -1, 10, 65)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input type="number" value={tirePressurePsi} onChange={e => setTirePressurePsi(Number(e.target.value))} className={`flex-1 min-h-[38px] bg-black text-center text-xs font-mono font-black text-white rounded-lg border ${ui?.brd || 'border-zinc-800'} outline-none`} />
                <button onClick={() => adjustValue(setTirePressurePsi, safeNum(tirePressurePsi), 1, 10, 65)} className="min-h-[38px] min-w-[38px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg flex items-center justify-center font-bold text-sm active:scale-95 cursor-pointer border border-zinc-800">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>

          <div className="flex justify-between items-center bg-black/40 p-3 rounded-xl border border-zinc-800">
            <div>
              <span className="text-[9px] font-black uppercase text-white tracking-widest block">
                Towing Trailer / Cargo Payload
              </span>
              <p className="text-[7.5px] text-zinc-400 font-mono mt-0.5">
                Simulates +40% aero drag factor in range estimation
              </p>
            </div>
            <button
              onClick={() => {
                triggerHaptic();
                setTowingPayload(!towingPayload);
              }}
              className={`min-h-[38px] px-3 rounded-lg text-[9px] font-mono font-black uppercase transition-all cursor-pointer ${towingPayload ? 'bg-amber-500 text-black shadow-sm' : 'bg-zinc-900 text-zinc-400 border border-zinc-800'}`}
            >
              {towingPayload ? "ATTACHED" : "SOLO"}
            </button>
          </div>
        </motion.div>
      )}

      <div className="pt-1">
        <button
          onClick={() => { triggerHaptic(); startTracking(); }}
          className={`w-full min-h-[48px] rounded-xl font-black uppercase tracking-widest text-[11px] sm:text-xs flex items-center justify-center gap-2 shadow-xl transition-all cursor-pointer active:scale-95 touch-manipulation ${ui?.t?.bg || 'bg-[#39ff14]'} text-black hover:opacity-90 ${ui?.t?.shadow || 'shadow-[0_0_15px_rgba(57,255,20,0.3)]'}`}
        >
          <Play className="w-4 h-4 fill-current"/> {tx ? tx('start_run') : "START LIVE GPS MISSION RUN"}
        </button>
      </div>

    </div>
  );
}