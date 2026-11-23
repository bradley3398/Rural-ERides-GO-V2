"use client";

import React, { useState, useEffect, useMemo } from "react";
import { 
  Wrench, CircleDashed, Disc, Cog, Battery, ShieldAlert, 
  CheckCircle2, Clock, AlertTriangle, Plus, Trash2, History, 
  Sparkles, Activity, Flame, Droplets, Scale, RefreshCw, 
  Layers, CheckSquare, Square, ChevronRight, Zap, ShieldCheck, WrenchIcon
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const safeNum = (val: any, fallback = 0): number => {
  const parsed = Number(val);
  return isNaN(parsed) || !isFinite(parsed) ? fallback : parsed;
};

export interface PredictiveMaintenanceProps {
  totalMiles: number;
  safeRides?: any[];
  triggerHaptic?: () => void;
  ui: any;
  useMetric?: boolean;
}

interface ServiceRecord {
  id: string;
  date: string;
  mileage: number;
  component: string;
  notes: string;
  cost?: number;
}

export default function PredictiveMaintenance({ 
  totalMiles = 0, 
  safeRides = [], 
  triggerHaptic = () => {}, 
  ui,
  useMetric = false
}: PredictiveMaintenanceProps) {
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
  const distanceMultiplier = localMetric ? 1.60934 : 1.0;

  // Navigation State
  const [activeTab, setActiveTab] = useState<"wear" | "battery" | "checklist" | "torque" | "trailpack" | "logbook">("wear");

  // Offset States (Mileage when replaced)
  const [tireOdoOffset, setTireOdoOffset] = useState<number>(0);
  const [brakeOdoOffset, setBrakeOdoOffset] = useState<number>(0);
  const [chainOdoOffset, setChainOdoOffset] = useState<number>(0);
  const [spokeOdoOffset, setSpokeOdoOffset] = useState<number>(0);
  const [suspensionOdoOffset, setSuspensionOdoOffset] = useState<number>(0);
  const [fluidFlushOffset, setFluidFlushOffset] = useState<number>(0);

  // Custom Threshold Limits
  const [tireLimit, setTireLimit] = useState<number>(1500);
  const [brakeLimit, setBrakeLimit] = useState<number>(600);
  const [chainLimit, setChainLimit] = useState<number>(800);

  // Service History Logbook & Checklists
  const [serviceLogs, setServiceLogs] = useState<ServiceRecord[]>([]);
  const [newLogComponent, setNewLogComponent] = useState("Tire Replacement");
  const [newLogNotes, setNewLogNotes] = useState("");
  const [newLogCost, setNewLogCost] = useState<string>("");
  const [showAddLogModal, setShowAddLogModal] = useState(false);

  const [preFlightChecks, setPreFlightChecks] = useState<Record<string, boolean>>({
    tirePressure: false,
    brakeBite: false,
    stemTight: false,
    batteryLatch: false,
    lightsOperational: false
  });

  const [trailPackChecks, setTrailPackChecks] = useState<Record<string, boolean>>({
    spareTube: false,
    tireLevers: false,
    multiTool: false,
    miniPump: false,
    zipTies: false,
    patchKit: false
  });

  // Load Saved Offsets, Thresholds & Service History
  useEffect(() => {
    try {
      setTireOdoOffset(Number(localStorage.getItem("rt_tire_offset")) || 0);
      setBrakeOdoOffset(Number(localStorage.getItem("rt_brake_offset")) || 0);
      setChainOdoOffset(Number(localStorage.getItem("rt_chain_offset")) || 0);
      setSpokeOdoOffset(Number(localStorage.getItem("rt_spoke_offset")) || 0);
      setSuspensionOdoOffset(Number(localStorage.getItem("rt_suspension_offset")) || 0);
      setFluidFlushOffset(Number(localStorage.getItem("rt_fluid_offset")) || 0);

      setTireLimit(Number(localStorage.getItem("rt_tire_limit")) || 1500);
      setBrakeLimit(Number(localStorage.getItem("rt_brake_limit")) || 600);
      setChainLimit(Number(localStorage.getItem("rt_chain_limit")) || 800);

      const savedLogs = localStorage.getItem("rt_service_logbook");
      if (savedLogs) setServiceLogs(JSON.parse(savedLogs));
    } catch (e) {}
  }, []);

  const updateOffset = (key: string, value: number, setter: React.Dispatch<React.SetStateAction<number>>, componentName: string) => {
    triggerHaptic();
    setter(value);
    localStorage.setItem(key, value.toString());

    const newEntry: ServiceRecord = {
      id: Date.now().toString(),
      date: new Date().toLocaleDateString(),
      mileage: Math.round(totalMiles),
      component: componentName,
      notes: `Service reset logged at ${Math.round(totalMiles * distanceMultiplier)} ${distLabel.toLowerCase()}.`
    };

    const updatedLogs = [newEntry, ...serviceLogs];
    setServiceLogs(updatedLogs);
    localStorage.setItem("rt_service_logbook", JSON.stringify(updatedLogs));
  };

  const handleAddManualLog = () => {
    triggerHaptic();
    if (!newLogNotes.trim()) return;

    const newEntry: ServiceRecord = {
      id: Date.now().toString(),
      date: new Date().toLocaleDateString(),
      mileage: Math.round(totalMiles),
      component: newLogComponent,
      notes: newLogNotes.trim(),
      cost: newLogCost ? Number(newLogCost) : undefined
    };

    const updated = [newEntry, ...serviceLogs];
    setServiceLogs(updated);
    localStorage.setItem("rt_service_logbook", JSON.stringify(updated));

    setNewLogNotes("");
    setNewLogCost("");
    setShowAddLogModal(false);
  };

  const handleDeleteLog = (id: string) => {
    triggerHaptic();
    const updated = serviceLogs.filter(l => l.id !== id);
    setServiceLogs(updated);
    localStorage.setItem("rt_service_logbook", JSON.stringify(updated));
  };

  const toggleChecklist = (key: string) => {
    triggerHaptic();
    setPreFlightChecks(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const toggleTrailPack = (key: string) => {
    triggerHaptic();
    setTrailPackChecks(prev => ({ ...prev, [key]: !prev[key] }));
  };

  // Hardware Life Metrics
  const miles = safeNum(totalMiles, 0);
  const activeTireMiles = Math.max(0, Math.round(miles - tireOdoOffset));
  const activeBrakeMiles = Math.max(0, Math.round(miles - brakeOdoOffset));
  const activeChainMiles = Math.max(0, Math.round(miles - chainOdoOffset));
  const activeSpokeMiles = Math.max(0, Math.round(miles - spokeOdoOffset));
  const activeSuspensionMiles = Math.max(0, Math.round(miles - suspensionOdoOffset));
  const activeFluidMiles = Math.max(0, Math.round(miles - fluidFlushOffset));

  const fleetHardBrakes = safeRides.reduce((acc, r) => acc + safeNum(r?.hardBrakes, 0), 0);
  const fleetSuspensionBumps = safeRides.reduce((acc, r) => acc + safeNum(r?.suspensionBumps, 0), 0);

  const brakeWearPct = Math.min(100, Math.round(((activeBrakeMiles + (fleetHardBrakes * 0.5)) / brakeLimit) * 100));
  const tireWearPct = Math.min(100, Math.round((activeTireMiles / tireLimit) * 100));
  const chainWearPct = Math.min(100, Math.round((activeChainMiles / chainLimit) * 100));
  const spokeStressPct = Math.min(100, Math.round(((activeSpokeMiles + (fleetSuspensionBumps * 0.2)) / 1200) * 100));
  const suspensionWearPct = Math.min(100, Math.round((activeSuspensionMiles / 1000) * 100));
  const hydraulicFluidWearPct = Math.min(100, Math.round((activeFluidMiles / 1800) * 100));

  // Battery Pack Degradation & Health Diagnostics
  const batteryStats = useMemo(() => {
    const ridesWithVoltage = safeRides.filter((r: any) => r.minVoltage && r.minVoltage !== 999 && r.batteryVoltage);
    const nominal = ridesWithVoltage.length > 0 ? (ridesWithVoltage[0].batteryVoltage || 48) : 48;
    const capacity = ridesWithVoltage.length > 0 ? (ridesWithVoltage[0].batteryCapacity || 15) : 15;
    const packWh = nominal * capacity;

    const estimatedFullCycles = packWh > 0 ? (miles * 25) / packWh : 0;
    const estimatedSOH = Math.max(70, Math.min(100, Math.round(100 - (estimatedFullCycles * 0.05))));

    let maxSagObserved = 0;
    let healthStatus = "Optimal";
    let healthColor = "text-emerald-400";

    if (ridesWithVoltage.length > 0) {
      const latest = ridesWithVoltage[0];
      const peak = nominal * 1.1666;
      const minV = nominal * 0.8125;
      const resting = minV + ((peak - minV) * ((latest.startingBattery || 100) / 100));
      maxSagObserved = Math.max(0, resting - latest.minVoltage);

      if (maxSagObserved > (nominal * 0.15)) {
        healthStatus = "Minor Imbalance Sag";
        healthColor = "text-amber-400";
      }
      if (maxSagObserved > (nominal * 0.25)) {
        healthStatus = "High Load Sagging";
        healthColor = "text-rose-400";
      }
    }

    return {
      nominal,
      capacity,
      packWh,
      estimatedFullCycles: estimatedFullCycles.toFixed(1),
      estimatedSOH,
      maxSagObserved: maxSagObserved.toFixed(1),
      healthStatus,
      healthColor
    };
  }, [safeRides, miles]);

  return (
    <div className={`${ui?.bgPanel || 'bg-[#0d0e15]'} p-4 sm:p-6 rounded-3xl border shadow-2xl transition-colors space-y-4 sm:space-y-5 ${ui?.brd || 'border-zinc-800'}`}>
      
      {/* HEADER BAR */}
      <div className={`flex flex-col sm:flex-row sm:items-center justify-between border-b ${ui?.brd || 'border-zinc-800'} pb-4 gap-3`}>
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-[#39ff14]/10 border border-[#39ff14]/40 text-[#39ff14] shadow-md">
            <Wrench className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <h3 className={`${ui?.txtMain || 'text-white'} font-black uppercase tracking-widest text-sm flex items-center gap-2`}>
              Predictive Lifecycle Maintenance
            </h3>
            <p className={`text-[9px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase mt-0.5`}>
              Telemetry Stress Degradation • Fleet Service Vault ({(miles * distanceMultiplier).toFixed(1)} {distLabel} Logged)
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 font-mono">
          <span className="text-[9px] uppercase px-2.5 py-1 rounded-xl border border-zinc-700 bg-black/60 text-cyan-400 font-bold">
            HEALTH INDEX: {batteryStats.estimatedSOH}% SOH
          </span>
        </div>
      </div>

      {/* HORIZONTAL NAVIGATION TABS */}
      <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-1 -mx-1 px-1 touch-pan-x">
        {[
          { id: "wear", label: "Mechanical Wear", icon: Disc },
          { id: "battery", label: "Battery Diagnostics", icon: Battery },
          { id: "checklist", label: "Pre-Flight Checklist", icon: CheckSquare },
          { id: "torque", label: "Torque Specs", icon: WrenchIcon },
          { id: "trailpack", label: "Trail Repair Pack", icon: ShieldCheck },
          { id: "logbook", label: `Service Log (${serviceLogs.length})`, icon: History },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => { triggerHaptic(); setActiveTab(tab.id as any); }}
            className={`min-h-[48px] px-4 rounded-2xl text-[10px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-2 shrink-0 cursor-pointer active:scale-95 touch-manipulation ${activeTab === tab.id ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-md ring-1 ring-white/20` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} border ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
          >
            <tab.icon className="w-4 h-4 shrink-0" /> {tab.label}
          </button>
        ))}
      </div>

      {/* --- TAB 1: MECHANICAL COMPONENT WEAR --- */}
      {activeTab === "wear" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            
            {/* 1. Tire Tread */}
            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-4 rounded-2xl shadow-inner space-y-2.5`}>
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <CircleDashed className="w-4 h-4 text-cyan-400" />
                  <span className={`text-[10px] font-black uppercase tracking-widest ${ui?.txtMain || 'text-white'}`}>
                    Tire Tread & Casing Life
                  </span>
                </div>
                <span className="text-[10px] font-mono font-bold text-zinc-400">
                  {Math.round(activeTireMiles * distanceMultiplier)} / {Math.round(tireLimit * distanceMultiplier)} {distLabel.toLowerCase()} ({tireWearPct}%)
                </span>
              </div>
              <div className="w-full bg-zinc-900 h-2.5 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-300 ${tireWearPct > 85 ? 'bg-rose-500' : tireWearPct > 60 ? 'bg-amber-400' : 'bg-cyan-500'}`} 
                  style={{ width: `${tireWearPct}%` }}
                />
              </div>
              <div className="flex justify-between items-center pt-1">
                <span className="text-[8px] font-mono text-zinc-500 uppercase">Threshold: {Math.round(tireLimit * distanceMultiplier)} {distLabel.toLowerCase()}</span>
                <button 
                  onClick={() => updateOffset("rt_tire_offset", miles, setTireOdoOffset, "Tire Replacement")}
                  className="min-h-[40px] px-3 rounded-xl border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-[9px] font-mono font-bold uppercase text-zinc-200 active:scale-95 touch-manipulation cursor-pointer shadow-sm"
                >
                  Log Tire Change
                </button>
              </div>
            </div>

            {/* 2. Brake Pads */}
            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-4 rounded-2xl shadow-inner space-y-2.5`}>
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <Disc className="w-4 h-4 text-rose-400" />
                  <span className={`text-[10px] font-black uppercase tracking-widest ${ui?.txtMain || 'text-white'}`}>
                    Brake Pad Thickness
                  </span>
                </div>
                <span className="text-[10px] font-mono font-bold text-zinc-400">
                  {Math.round(activeBrakeMiles * distanceMultiplier)} / {Math.round(brakeLimit * distanceMultiplier)} {distLabel.toLowerCase()} ({brakeWearPct}%)
                </span>
              </div>
              <div className="w-full bg-zinc-900 h-2.5 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-300 ${brakeWearPct > 85 ? 'bg-rose-500' : brakeWearPct > 60 ? 'bg-amber-400' : 'bg-rose-400'}`} 
                  style={{ width: `${brakeWearPct}%` }}
                />
              </div>
              <div className="flex justify-between items-center pt-1">
                <span className="text-[8px] font-mono text-zinc-500 uppercase">{fleetHardBrakes} hard deceleration events</span>
                <button 
                  onClick={() => updateOffset("rt_brake_offset", miles, setBrakeOdoOffset, "Brake Pad Replacement")}
                  className="min-h-[40px] px-3 rounded-xl border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-[9px] font-mono font-bold uppercase text-zinc-200 active:scale-95 touch-manipulation cursor-pointer shadow-sm"
                >
                  Log Brake Pads
                </button>
              </div>
            </div>

            {/* 3. Chain / Belt Lube */}
            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-4 rounded-2xl shadow-inner space-y-2.5`}>
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <Cog className="w-4 h-4 text-amber-400" />
                  <span className={`text-[10px] font-black uppercase tracking-widest ${ui?.txtMain || 'text-white'}`}>
                    Chain / Belt Lube & Tension
                  </span>
                </div>
                <span className="text-[10px] font-mono font-bold text-zinc-400">
                  {Math.round(activeChainMiles * distanceMultiplier)} / {Math.round(chainLimit * distanceMultiplier)} {distLabel.toLowerCase()} ({chainWearPct}%)
                </span>
              </div>
              <div className="w-full bg-zinc-900 h-2.5 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-300 ${chainWearPct > 80 ? 'bg-rose-500' : 'bg-amber-400'}`} 
                  style={{ width: `${chainWearPct}%` }}
                />
              </div>
              <div className="flex justify-between items-center pt-1">
                <span className="text-[8px] font-mono text-zinc-500 uppercase">Lube interval every 150 mi equivalent</span>
                <button 
                  onClick={() => updateOffset("rt_chain_offset", miles, setChainOdoOffset, "Chain Service & Lube")}
                  className="min-h-[40px] px-3 rounded-xl border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-[9px] font-mono font-bold uppercase text-zinc-200 active:scale-95 touch-manipulation cursor-pointer shadow-sm"
                >
                  Log Chain Lube
                </button>
              </div>
            </div>

            {/* 4. Spoke Tension */}
            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-4 rounded-2xl shadow-inner space-y-2.5`}>
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <Scale className="w-4 h-4 text-purple-400" />
                  <span className={`text-[10px] font-black uppercase tracking-widest ${ui?.txtMain || 'text-white'}`}>
                    Spoke Tension & Rim Truing
                  </span>
                </div>
                <span className="text-[10px] font-mono font-bold text-zinc-400">
                  {Math.round(activeSpokeMiles * distanceMultiplier)} / {Math.round(1200 * distanceMultiplier)} {distLabel.toLowerCase()} ({spokeStressPct}%)
                </span>
              </div>
              <div className="w-full bg-zinc-900 h-2.5 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-300 ${spokeStressPct > 80 ? 'bg-rose-500' : 'bg-purple-400'}`} 
                  style={{ width: `${spokeStressPct}%` }}
                />
              </div>
              <div className="flex justify-between items-center pt-1">
                <span className="text-[8px] font-mono text-zinc-500 uppercase">{fleetSuspensionBumps} heavy impact shocks</span>
                <button 
                  onClick={() => updateOffset("rt_spoke_offset", miles, setSpokeOdoOffset, "Wheel Truing & Spoke Tension")}
                  className="min-h-[40px] px-3 rounded-xl border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-[9px] font-mono font-bold uppercase text-zinc-200 active:scale-95 touch-manipulation cursor-pointer shadow-sm"
                >
                  Log Wheel Tune
                </button>
              </div>
            </div>

            {/* 5. Suspension */}
            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-4 rounded-2xl shadow-inner space-y-2.5`}>
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <Activity className="w-4 h-4 text-emerald-400" />
                  <span className={`text-[10px] font-black uppercase tracking-widest ${ui?.txtMain || 'text-white'}`}>
                    Suspension Fork & Pivot Bushings
                  </span>
                </div>
                <span className="text-[10px] font-mono font-bold text-zinc-400">
                  {Math.round(activeSuspensionMiles * distanceMultiplier)} / {Math.round(1000 * distanceMultiplier)} {distLabel.toLowerCase()} ({suspensionWearPct}%)
                </span>
              </div>
              <div className="w-full bg-zinc-900 h-2.5 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-300 ${suspensionWearPct > 80 ? 'bg-rose-500' : 'bg-emerald-400'}`} 
                  style={{ width: `${suspensionWearPct}%` }}
                />
              </div>
              <div className="flex justify-between items-center pt-1">
                <span className="text-[8px] font-mono text-zinc-500 uppercase">Stanchion oiling & wipe down</span>
                <button 
                  onClick={() => updateOffset("rt_suspension_offset", miles, setSuspensionOdoOffset, "Suspension Fork Service")}
                  className="min-h-[40px] px-3 rounded-xl border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-[9px] font-mono font-bold uppercase text-zinc-200 active:scale-95 touch-manipulation cursor-pointer shadow-sm"
                >
                  Log Suspension
                </button>
              </div>
            </div>

            {/* 6. Hydraulic Fluid */}
            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-4 rounded-2xl shadow-inner space-y-2.5`}>
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <Droplets className="w-4 h-4 text-blue-400" />
                  <span className={`text-[10px] font-black uppercase tracking-widest ${ui?.txtMain || 'text-white'}`}>
                    Hydraulic Brake Fluid Flush
                  </span>
                </div>
                <span className="text-[10px] font-mono font-bold text-zinc-400">
                  {Math.round(activeFluidMiles * distanceMultiplier)} / {Math.round(1800 * distanceMultiplier)} {distLabel.toLowerCase()} ({hydraulicFluidWearPct}%)
                </span>
              </div>
              <div className="w-full bg-zinc-900 h-2.5 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-300 ${hydraulicFluidWearPct > 85 ? 'bg-rose-500' : 'bg-blue-400'}`} 
                  style={{ width: `${hydraulicFluidWearPct}%` }}
                />
              </div>
              <div className="flex justify-between items-center pt-1">
                <span className="text-[8px] font-mono text-zinc-500 uppercase">Annual bleed or lever fade</span>
                <button 
                  onClick={() => updateOffset("rt_fluid_offset", miles, setFluidFlushOffset, "Hydraulic Brake Fluid Flush")}
                  className="min-h-[40px] px-3 rounded-xl border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-[9px] font-mono font-bold uppercase text-zinc-200 active:scale-95 touch-manipulation cursor-pointer shadow-sm"
                >
                  Log Fluid Flush
                </button>
              </div>
            </div>

          </div>
        </motion.div>
      )}

      {/* --- TAB 2: BATTERY SOH & VOLTAGE SAG DIAGNOSTICS --- */}
      {activeTab === "battery" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
          
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-4 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1 shadow-inner`}>
              <Battery className="w-5 h-5 text-emerald-400 mx-auto mb-1" />
              <span className={`text-[8.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>State of Health (SOH)</span>
              <span className="text-2xl font-mono font-black text-emerald-400">{batteryStats.estimatedSOH}%</span>
              <span className="text-[8px] font-mono text-zinc-500 block">Estimated capacity retention</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-4 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1 shadow-inner`}>
              <RefreshCw className="w-5 h-5 text-cyan-400 mx-auto mb-1" />
              <span className={`text-[8.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>Equivalent Cycles</span>
              <span className="text-2xl font-mono font-black text-cyan-400">{batteryStats.estimatedFullCycles} Cyc</span>
              <span className="text-[8px] font-mono text-zinc-500 block">Based on {batteryStats.packWh}Wh pack size</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-4 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1 shadow-inner`}>
              <Zap className="w-5 h-5 text-amber-400 mx-auto mb-1" />
              <span className={`text-[8.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>Recent Peak Sag</span>
              <span className={`text-2xl font-mono font-black ${batteryStats.healthColor}`}>-{batteryStats.maxSagObserved}V</span>
              <span className="text-[8px] font-mono text-zinc-500 block">{batteryStats.healthStatus}</span>
            </div>
          </div>

          <div className={`${ui?.bgCard || 'bg-black/60'} p-4 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2`}>
            <div className="flex items-center gap-2 text-xs font-mono font-bold text-white">
              <Sparkles className="w-4 h-4 text-cyan-400" />
              <span>Cell Imbalance & Load Sag Analysis</span>
            </div>
            <p className="text-[9px] font-mono text-zinc-400 leading-relaxed">
              When full-throttle current causes voltage dips exceeding 15% of nominal pack rating, cell series resistance is rising. Avoid storing battery at 100% in hot conditions to preserve cell longevity.
            </p>
          </div>
        </motion.div>
      )}

      {/* --- TAB 3: PRE-FLIGHT CHECKLIST --- */}
      {activeTab === "checklist" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          <div className="space-y-2">
            {[
              { key: "tirePressure", label: "Tire PSI & Bead Inspection", desc: "Check tire firmness and look for embedded thorns, wire, or low pressure." },
              { key: "brakeBite", label: "Brake Lever Bite & Cable Tension", desc: "Squeeze levers firmly; confirm pads engage before levers reach the handlebar grips." },
              { key: "stemTight", label: "Stem Folding Clamp & Quick Release", desc: "Ensure the folding hinge mechanism and latch safety collar are locked." },
              { key: "batteryLatch", label: "Battery Lock & Key Engagement", desc: "Confirm battery pack is clicked into rail mount and key cylinder is secured." },
              { key: "lightsOperational", label: "Headlight & Tail Flasher Verification", desc: "Verify front projection beam and rear brake lights activate properly." }
            ].map((item) => (
              <div 
                key={item.key}
                onClick={() => toggleChecklist(item.key)}
                className={`p-3.5 rounded-2xl border ${preFlightChecks[item.key] ? 'bg-emerald-950/20 border-emerald-900/60' : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'}`} flex items-center justify-between gap-3 cursor-pointer transition-colors active:scale-[0.99] touch-manipulation`}
              >
                <div className="flex items-start gap-3">
                  <div className={`mt-0.5 ${preFlightChecks[item.key] ? 'text-emerald-400' : 'text-zinc-500'}`}>
                    {preFlightChecks[item.key] ? <CheckSquare className="w-5 h-5" /> : <Square className="w-5 h-5" />}
                  </div>
                  <div>
                    <span className={`text-xs font-mono font-bold block ${preFlightChecks[item.key] ? 'text-emerald-300' : 'text-white'}`}>
                      {item.label}
                    </span>
                    <span className="text-[8.5px] font-mono text-zinc-400 block mt-0.5">
                      {item.desc}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <button
            onClick={() => {
              triggerHaptic();
              setPreFlightChecks({
                tirePressure: true,
                brakeBite: true,
                stemTight: true,
                batteryLatch: true,
                lightsOperational: true
              });
            }}
            className="w-full min-h-[48px] rounded-2xl bg-zinc-900 hover:bg-zinc-800 border border-zinc-700 text-white text-[10px] font-mono font-black uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer active:scale-95 touch-manipulation shadow-sm"
          >
            <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Pass All Safety Checks
          </button>
        </motion.div>
      )}

      {/* --- TAB 4: TORQUE SPECS & FASTENER MATRIX --- */}
      {activeTab === "torque" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          <div className="bg-black/40 p-3 rounded-2xl border border-zinc-800 text-[9px] font-mono text-zinc-400">
            <span className="text-cyan-400 font-bold uppercase block mb-0.5">⚠️ Critical Fastener Reference Guide</span>
            Always use a calibrated torque wrench on high-vibration PEV components. Apply medium-strength threadlocker (Blue Loctite) on axle nuts and suspension pivot bolts.
          </div>

          <div className="space-y-2 max-h-[320px] overflow-y-auto custom-scrollbar pr-1">
            {[
              { part: "Front / Rear Axle Nuts", torque: "45 - 55 Nm", tool: "18mm / 19mm Deep Socket", notes: "Crucial for high torque hub motors to prevent axle spin-out." },
              { part: "Handlebar Stem Clamp Bolts", torque: "10 - 12 Nm", tool: "4mm / 5mm Hex Allen", notes: "Tighten in alternating 'X' pattern to distribute clamping pressure evenly." },
              { part: "Brake Rotor Caliper Bolts", torque: "6 - 8 Nm", tool: "T25 Torx Key", notes: "Always use threadlocker. Check after first 50 miles of riding." },
              { part: "Suspension Swingarm Pivot", torque: "25 - 30 Nm", tool: "6mm Hex Allen", notes: "Check regularly for side-to-side play or bushing wear." },
              { part: "Handlebar Clamp / Faceplate", torque: "5 - 7 Nm", tool: "4mm Hex Allen", notes: "Equal gap on top and bottom bolts during tightening." }
            ].map((spec, idx) => (
              <div key={idx} className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-3.5 rounded-xl space-y-1 shadow-inner`}>
                <div className="flex justify-between items-center">
                  <span className="text-xs font-mono font-black text-white">{spec.part}</span>
                  <span className="text-xs font-mono font-black text-[#39ff14] bg-[#39ff14]/10 border border-[#39ff14]/30 px-2 py-0.5 rounded">
                    {spec.torque}
                  </span>
                </div>
                <div className="text-[9px] font-mono text-cyan-400">Tool: {spec.tool}</div>
                <div className="text-[8.5px] font-mono text-zinc-400">{spec.notes}</div>
              </div>
            ))}
          </div>
        </motion.div>
      )}

      {/* --- TAB 5: TRAIL REPAIR PACK INVENTORY --- */}
      {activeTab === "trailpack" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          <div className="space-y-2">
            {[
              { key: "spareTube", label: "Heavy-Duty Inner Tube / Slime", desc: "Matched to your exact wheel size and valve type (Schrader/Presta)." },
              { key: "tireLevers", label: "Reinforced Tire Levers (2x)", desc: "Essential for unseating stiff pneumatic tire beads on aluminum rims." },
              { key: "multiTool", label: "Folding Hex & Torx Multi-Tool", desc: "Includes 3mm, 4mm, 5mm, 6mm hex keys and T25 Torx driver." },
              { key: "miniPump", label: "High-Pressure Portable Hand Pump", desc: "Compact pump capable of reaching 40-50 PSI on the trail." },
              { key: "zipTies", label: "Heavy-Duty Zip Ties & Duct Tape", desc: "Temporary fix for loose wiring harnesses, fenders, or dangling cables." },
              { key: "patchKit", label: "Glueless Patch Kit & Sandpaper", desc: "Quick puncture repair for inner tubes without messy liquid glue." }
            ].map((item) => (
              <div 
                key={item.key}
                onClick={() => toggleTrailPack(item.key)}
                className={`p-3.5 rounded-2xl border ${trailPackChecks[item.key] ? 'bg-cyan-950/20 border-cyan-900/60' : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'}`} flex items-center justify-between gap-3 cursor-pointer transition-colors active:scale-[0.99] touch-manipulation`}
              >
                <div className="flex items-start gap-3">
                  <div className={`mt-0.5 ${trailPackChecks[item.key] ? 'text-cyan-400' : 'text-zinc-500'}`}>
                    {trailPackChecks[item.key] ? <CheckSquare className="w-5 h-5" /> : <Square className="w-5 h-5" />}
                  </div>
                  <div>
                    <span className={`text-xs font-mono font-bold block ${trailPackChecks[item.key] ? 'text-cyan-300' : 'text-white'}`}>
                      {item.label}
                    </span>
                    <span className="text-[8.5px] font-mono text-zinc-400 block mt-0.5">
                      {item.desc}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      )}

      {/* --- TAB 6: SERVICE LOGBOOK VAULT --- */}
      {activeTab === "logbook" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          
          <div className="flex justify-between items-center">
            <span className={`text-[9px] font-mono font-black uppercase text-zinc-400 tracking-wider`}>
              Service History Ledger ({serviceLogs.length} entries)
            </span>
            <button
              onClick={() => setShowAddLogModal(!showAddLogModal)}
              className="min-h-[40px] px-3.5 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-black text-[9px] font-mono font-black uppercase tracking-wider flex items-center gap-1.5 cursor-pointer active:scale-95 touch-manipulation shadow-sm"
            >
              <Plus className="w-3.5 h-3.5" /> Log Custom Service
            </button>
          </div>

          {/* Add Service Modal */}
          <AnimatePresence>
            {showAddLogModal && (
              <motion.div 
                initial={{ opacity: 0, height: 0 }} 
                animate={{ opacity: 1, height: "auto" }} 
                exit={{ opacity: 0, height: 0 }}
                className={`${ui?.bgCard || 'bg-black/60'} p-4 rounded-2xl border border-zinc-700 space-y-3 overflow-hidden shadow-inner`}
              >
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  <select
                    value={newLogComponent}
                    onChange={(e) => setNewLogComponent(e.target.value)}
                    className="min-h-[44px] bg-black text-xs font-mono font-bold text-white border border-zinc-700 rounded-xl px-3 outline-none"
                  >
                    <option value="Tire Replacement">Tire Replacement</option>
                    <option value="Inner Tube Patch / Replacement">Inner Tube Patch / Replacement</option>
                    <option value="Brake Pad Replacement">Brake Pad Replacement</option>
                    <option value="Brake Bleed / Cable Adjustment">Brake Bleed / Cable Adjustment</option>
                    <option value="Chain Lube & Tensioning">Chain Lube & Tensioning</option>
                    <option value="Spoke Truing & Tuning">Spoke Truing & Tuning</option>
                    <option value="Fork Suspension Service">Fork Suspension Service</option>
                    <option value="Electrical / BMS Inspection">Electrical / BMS Inspection</option>
                  </select>

                  <input
                    type="number"
                    placeholder="Optional Cost ($)"
                    value={newLogCost}
                    onChange={(e) => setNewLogCost(e.target.value)}
                    className="min-h-[44px] bg-black text-xs font-mono font-bold text-white border border-zinc-700 rounded-xl px-3 outline-none"
                  />
                </div>

                <textarea
                  placeholder="Service notes, parts installed, brand used..."
                  value={newLogNotes}
                  onChange={(e) => setNewLogNotes(e.target.value)}
                  className="w-full bg-black border border-zinc-700 rounded-xl p-3 text-xs font-mono font-bold text-white outline-none resize-none h-20"
                />

                <div className="flex gap-2 justify-end">
                  <button
                    onClick={() => setShowAddLogModal(false)}
                    className="min-h-[40px] px-4 rounded-xl text-[9px] font-mono font-bold text-zinc-400 hover:text-white uppercase"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleAddManualLog}
                    className="min-h-[40px] px-5 rounded-xl bg-[#39ff14] text-black text-[9px] font-mono font-black uppercase tracking-wider"
                  >
                    Save Entry
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Log Entries List */}
          {serviceLogs.length === 0 ? (
            <div className={`text-center py-10 ${ui?.bgCard || 'bg-black/60'} border border-dashed ${ui?.brd || 'border-zinc-800'} rounded-2xl space-y-1`}>
              <History className="w-7 h-7 text-zinc-500 mx-auto" />
              <div className="text-xs font-mono uppercase text-zinc-400 font-bold">No Service Records Logged</div>
              <p className="text-[8.5px] text-zinc-500 font-mono">Service events and component resets will appear here</p>
            </div>
          ) : (
            <div className="space-y-2 max-h-[350px] overflow-y-auto custom-scrollbar pr-1">
              {serviceLogs.map((log) => (
                <div 
                  key={log.id}
                  className={`p-3.5 rounded-2xl border ${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} flex items-center justify-between gap-3 shadow-inner`}
                >
                  <div className="space-y-1 truncate">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-mono font-black text-white truncate">{log.component}</span>
                      <span className="text-[8px] font-mono px-2 py-0.5 rounded bg-zinc-800 text-cyan-400">
                        {Math.round(log.mileage * distanceMultiplier)} {distLabel.toLowerCase()}
                      </span>
                      {log.cost !== undefined && (
                        <span className="text-[8px] font-mono px-2 py-0.5 rounded bg-emerald-950 text-emerald-400">
                          ${log.cost}
                        </span>
                      )}
                    </div>
                    <p className="text-[9px] font-mono text-zinc-400 truncate">{log.notes}</p>
                    <span className="text-[7.5px] font-mono text-zinc-500 block">{log.date}</span>
                  </div>

                  <button
                    onClick={() => handleDeleteLog(log.id)}
                    className="min-h-[36px] min-w-[36px] flex items-center justify-center rounded-xl bg-rose-950/40 border border-rose-900/60 text-rose-400 hover:bg-rose-600 hover:text-white transition-colors shrink-0"
                    title="Delete Entry"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </motion.div>
      )}

    </div>
  );
}