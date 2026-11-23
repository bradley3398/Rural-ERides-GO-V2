"use client";

import React, { useState, useMemo, useRef, useEffect } from "react";
import { 
  Award, Filter, Map as MapIcon, Timer, Download, ChevronUp, ChevronDown, 
  Trash2, Gauge, Zap, Battery, ZapOff, TrendingUp, Scale, Orbit, ShieldAlert, 
  Mountain, Activity, DollarSign, CloudRain, Disc, BrainCircuit, BookOpen, 
  Save, Send, Search, ArrowUpDown, FileText, Upload, CheckCircle2, 
  Share2, Sparkles, Fuel, Leaf, Flame, HelpCircle, Tag, Plus, Check
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const safeNum = (val: any, fallback = 0): number => {
  const parsed = Number(val);
  return isNaN(parsed) || !isFinite(parsed) ? fallback : parsed;
};

const safePct = (val: any): number => {
  const num = safeNum(val);
  return Math.max(0, Math.min(100, num));
};

export default function MissionLogs(props: any) {
  const { 
    safeRides = [], logFilter = "ALL", setLogFilter, expandedRideId, setExpandedRideId, 
    lifetimeSeconds = 0, totalFinancialSavings = 0, lifetimeTopSpeed = 0, speedLabel = "MPH", distLabel = "MI", 
    useMetric = false, exportRideGPX, setSavedRides, handleBroadcastRideToBoard, 
    triggerHistoricalAiDebrief, isFetchingHistoricalAi, historicalAiAnalysis = {}, 
    updateRideNote, tirePsi = 35, callsign = "Pilot", ui, tx 
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

  const activeDistLabel = localMetric ? "KM" : "MI";
  const activeSpeedLabel = localMetric ? "KM/H" : "MPH";

  // --- ADVANCED FILTERING, SORTING & SEARCH ---
  const [searchQuery, setSearchQuery] = useState("");
  const [sortBy, setSortBy] = useState<"newest" | "oldest" | "distance" | "speed" | "efficiency">("newest");
  const [vehicleFilter, setVehicleFilter] = useState<string>("ALL");
  const [showBatchTools, setShowBatchTools] = useState(false);
  const [activeRideSubTab, setActiveRideSubTab] = useState<Record<string, "telemetry" | "avionics" | "ai" | "notes">>({});
  
  const [editingTagRideId, setEditingTagRideId] = useState<string | null>(null);
  const [newTagInput, setNewTagInput] = useState("");

  const fileInputRef = useRef<HTMLInputElement>(null);

  const uniqueVehicles = useMemo(() => {
    const models = new Set<string>();
    safeRides.forEach((r: any) => {
      if (r?.vehicleModel) models.add(r.vehicleModel);
      else if (r?.pevType) models.add(r.pevType);
    });
    return Array.from(models);
  }, [safeRides]);

  const personalBests = useMemo(() => {
    if (!safeRides.length) return { maxDist: 0, maxSpd: 0, bestEff: 999 };
    let maxDist = 0;
    let maxSpd = 0;
    let bestEff = 999;
    safeRides.forEach((r: any) => {
      if (safeNum(r?.distance) > maxDist) maxDist = safeNum(r?.distance);
      if (safeNum(r?.maxSpeed) > maxSpd) maxSpd = safeNum(r?.maxSpeed);
      const eff = safeNum(r?.efficiencyWhPerMile);
      if (eff > 5 && eff < bestEff) bestEff = eff;
    });
    return { maxDist, maxSpd, bestEff };
  }, [safeRides]);

  const filteredRides = useMemo(() => {
    return safeRides
      .filter((ride: any) => {
        if (logFilter === "SPEED" && safeNum(ride?.maxSpeed) < 20) return false;
        if (logFilter === "LONG" && safeNum(ride?.distance) < 5) return false;
        if (logFilter === "ECO" && safeNum(ride?.efficiencyWhPerMile, 999) > 22) return false;

        if (vehicleFilter !== "ALL" && ride?.vehicleModel !== vehicleFilter && ride?.pevType !== vehicleFilter) {
          return false;
        }

        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchModel = (ride?.vehicleModel || "").toLowerCase().includes(q);
          const matchNotes = (ride?.rideNote || "").toLowerCase().includes(q);
          const matchDate = (ride?.date || "").toLowerCase().includes(q);
          const matchTags = Array.isArray(ride?.tags) && ride.tags.some((t: string) => t.toLowerCase().includes(q));
          if (!matchModel && !matchNotes && !matchDate && !matchTags) return false;
        }

        return true;
      })
      .sort((a: any, b: any) => {
        if (sortBy === "newest") return safeNum(b?.id || b?.duration) - safeNum(a?.id || a?.duration);
        if (sortBy === "oldest") return safeNum(a?.id || a?.duration) - safeNum(b?.id || b?.duration);
        if (sortBy === "distance") return safeNum(b?.distance) - safeNum(a?.distance);
        if (sortBy === "speed") return safeNum(b?.maxSpeed) - safeNum(a?.maxSpeed);
        if (sortBy === "efficiency") return safeNum(a?.efficiencyWhPerMile, 99) - safeNum(b?.efficiencyWhPerMile, 99);
        return 0;
      });
  }, [safeRides, logFilter, vehicleFilter, searchQuery, sortBy]);

  const lifetimeStats = useMemo(() => {
    let totalDist = 0;
    let totalWh = 0;
    let totalAh = 0;
    let totalClimb = 0;
    let totalBumps = 0;
    let totalHardBrakes = 0;

    safeRides.forEach((r: any) => {
      totalDist += safeNum(r?.distance);
      totalWh += safeNum(r?.totalWhConsumed);
      totalAh += safeNum(r?.totalAhConsumed);
      totalClimb += safeNum(r?.elevationGain);
      totalBumps += safeNum(r?.suspensionBumps);
      totalHardBrakes += safeNum(r?.hardBrakes);
    });

    const avgWhPerMile = totalDist > 0 ? totalWh / totalDist : 25;
    const totalCo2Saved = totalDist * (localMetric ? 0.55 : 0.89);

    return { totalDist, totalWh, totalAh, totalClimb, totalBumps, totalHardBrakes, avgWhPerMile, totalCo2Saved };
  }, [safeRides, localMetric]);

  const handleExportJSON = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(safeRides, null, 2));
    const downloadAnchor = document.createElement("a");
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", `Rural_ERides_MissionLogs_${Date.now()}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  const handleImportJSON = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const imported = JSON.parse(event.target?.result as string);
        if (Array.isArray(imported)) {
          const merged = [...imported, ...safeRides.filter((sr: any) => !imported.some((ir: any) => ir.id === sr.id))];
          setSavedRides(merged);
          localStorage.setItem("universal_erides_rides", JSON.stringify(merged));
          alert(`Successfully imported and merged ${imported.length} mission logs![cite: 15]`);
        } else {
          alert("Invalid mission log backup format.");
        }
      } catch (err) {
        alert("Failed to parse JSON backup file.");
      }
    };
    reader.readAsText(file);
  };

  const handleAddCustomTag = (rideId: string) => {
    if (!newTagInput.trim()) return;
    const updated = safeRides.map((r: any) => {
      if (String(r.id) === String(rideId)) {
        const currentTags = Array.isArray(r.tags) ? r.tags : [];
        if (!currentTags.includes(newTagInput.trim())) {
          return { ...r, tags: [...currentTags, newTagInput.trim()] };
        }
      }
      return r;
    });
    setSavedRides(updated);
    localStorage.setItem("universal_erides_rides", JSON.stringify(updated));
    setNewTagInput("");
    setEditingTagRideId(null);
  };

  const handleRemoveTag = (rideId: string, tagToRemove: string) => {
    const updated = safeRides.map((r: any) => {
      if (String(r.id) === String(rideId)) {
        const currentTags = Array.isArray(r.tags) ? r.tags : [];
        return { ...r, tags: currentTags.filter((t: string) => t !== tagToRemove) };
      }
      return r;
    });
    setSavedRides(updated);
    localStorage.setItem("universal_erides_rides", JSON.stringify(updated));
  };

  return (
    <div className={`${ui?.bgPanel || 'bg-[#0d0e15]'} p-3.5 sm:p-6 rounded-3xl border shadow-2xl transition-colors space-y-4 sm:space-y-5 ${ui?.brd || 'border-zinc-800'}`}>
      
      <div className={`flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-b ${ui?.brd || 'border-zinc-800'} pb-4`}>
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-[#39ff14]/10 border border-[#39ff14]/40 text-[#39ff14] shadow-md">
            <Award className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <h3 className={`${ui?.txtMain || 'text-white'} font-black flex items-center gap-2 uppercase tracking-widest text-sm sm:text-base`}>
              {tx ? tx('logs') : "Mission Telemetry Archives"}
            </h3>
            <p className={`text-[9px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase mt-0.5`}>
              {safeRides.length} Recorded Sorties • {(lifetimeStats.totalDist * (localMetric ? 1.60934 : 1.0)).toFixed(1)} {activeDistLabel} Total Vector
            </p>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 w-full lg:w-auto">
          <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center font-mono`}>
            <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-black block`}>Total Duration</span>
            <span className={`${ui?.t?.text || 'text-[#39ff14]'} text-xs font-black tabular-nums`}>
              {Math.floor(lifetimeSeconds / 3600)}h {Math.floor((lifetimeSeconds % 3600) / 60)}m
            </span>
          </div>
          <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center font-mono`}>
            <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-black block`}>Gas & Wear Saved</span>
            <span className="text-emerald-400 text-xs font-black tabular-nums">
              ${safeNum(totalFinancialSavings).toFixed(2)}
            </span>
          </div>
          <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center font-mono`}>
            <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-black block`}>Lifetime Max</span>
            <span className="text-cyan-400 text-xs font-black tabular-nums">
              {localMetric ? (safeNum(lifetimeTopSpeed) * 1.60934).toFixed(1) : safeNum(lifetimeTopSpeed).toFixed(1)} {activeSpeedLabel}
            </span>
          </div>
          <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center font-mono`}>
            <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-black block`}>Avg Efficiency</span>
            <span className="text-purple-400 text-xs font-black tabular-nums">
              {(lifetimeStats.avgWhPerMile * (localMetric ? 0.621371 : 1.0)).toFixed(1)} Wh/{activeDistLabel.toLowerCase()}
            </span>
          </div>
        </div>
      </div>

      <div className="space-y-2.5">
        <div className="flex flex-col sm:flex-row gap-2">
          <div className="relative flex-1">
            <input 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by model, notes, tags, or date..."
              className={`w-full min-h-[48px] ${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl pl-10 pr-4 ${ui?.txtMain || 'text-white'} text-xs font-bold outline-none focus:border-cyan-500 transition-colors shadow-inner`}
            />
            <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            {searchQuery && (
              <button 
                onClick={() => setSearchQuery("")}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-mono text-zinc-500 hover:text-white"
              >
                CLEAR
              </button>
            )}
          </div>

          <div className="flex gap-2">
            <div className="relative flex-1 sm:w-44">
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className={`w-full min-h-[48px] ${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl px-3 ${ui?.txtMain || 'text-white'} text-[10px] font-mono font-black uppercase outline-none cursor-pointer`}
              >
                <option value="newest">📅 Newest First</option>
                <option value="oldest">📅 Oldest First</option>
                <option value="distance">📏 Longest Distance</option>
                <option value="speed">⚡ Fastest Speed</option>
                <option value="efficiency">🌿 Best Efficiency</option>
              </select>
            </div>

            {uniqueVehicles.length > 0 && (
              <div className="relative flex-1 sm:w-48">
                <select
                  value={vehicleFilter}
                  onChange={(e) => setVehicleFilter(e.target.value)}
                  className={`w-full min-h-[48px] ${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl px-3 ${ui?.txtMain || 'text-white'} text-[10px] font-mono font-black uppercase outline-none cursor-pointer truncate`}
                >
                  <option value="ALL">🛵 All Vehicles</option>
                  {uniqueVehicles.map(v => (
                    <option key={v} value={v}>{v}</option>
                  ))}
                </select>
              </div>
            )}

            <button
              onClick={() => setShowBatchTools(!showBatchTools)}
              className={`min-h-[48px] px-3.5 rounded-2xl border text-[10px] font-mono font-black uppercase tracking-wider flex items-center justify-center gap-1.5 transition-all cursor-pointer active:scale-95 touch-manipulation ${showBatchTools ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} ${ui?.txtMuted || 'text-zinc-400'}`}`}
              title="Backup & Database Utilities"
            >
              <FileText className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-1 -mx-1 px-1 touch-pan-x">
          {[
            { id: "ALL", label: "All Sorties", count: safeRides.length },
            { id: "SPEED", label: `Speed Runs (≥${localMetric ? '32' : '20'}${localMetric ? 'km/h' : 'mph'})`, count: safeRides.filter((r: any) => safeNum(r?.maxSpeed) >= (localMetric ? 12.5 : 20)).length },
            { id: "LONG", label: `Long Range (≥${localMetric ? '8' : '5'}${activeDistLabel.toLowerCase()})`, count: safeRides.filter((r: any) => safeNum(r?.distance) >= (localMetric ? 8 : 5)).length },
            { id: "ECO", label: `Eco Miler (≤${localMetric ? '14' : '22'}Wh/${activeDistLabel.toLowerCase()})`, count: safeRides.filter((r: any) => safeNum(r?.efficiencyWhPerMile, 999) <= (localMetric ? 14 : 22)).length },
          ].map((cat) => (
            <button
              key={cat.id}
              onClick={() => setLogFilter(cat.id)}
              className={`min-h-[44px] px-4 rounded-xl text-[10px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-2 shrink-0 cursor-pointer active:scale-95 touch-manipulation ${logFilter === cat.id ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-lg` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} border ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
            >
              <span>{cat.label}</span>
              <span className={`px-1.5 py-0.5 rounded text-[8px] font-bold ${logFilter === cat.id ? 'bg-black/30 text-black' : 'bg-zinc-800 text-zinc-300'}`}>
                {cat.count}
              </span>
            </button>
          ))}
        </div>

        <AnimatePresence>
          {showBatchTools && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              className={`p-4 rounded-2xl ${ui?.bgCard || 'bg-black/60'} border border-zinc-800 flex flex-col sm:flex-row items-center justify-between gap-3 overflow-hidden shadow-inner`}
            >
              <div className="flex items-center gap-2 text-xs font-mono font-bold text-zinc-300">
                <FileText className="w-4 h-4 text-cyan-400" />
                <span>Mission Database Vault ({safeRides.length} records)[cite: 15]</span>
              </div>
              <div className="flex flex-wrap gap-2 w-full sm:w-auto">
                <button
                  onClick={handleExportJSON}
                  className="flex-1 sm:flex-none min-h-[44px] px-4 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-black text-[10px] font-mono font-black uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer active:scale-95 touch-manipulation"
                >
                  <Download className="w-3.5 h-3.5" /> Backup Database (.JSON)
                </button>
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="flex-1 sm:flex-none min-h-[44px] px-4 rounded-xl border border-zinc-700 bg-zinc-800 hover:bg-zinc-700 text-white text-[10px] font-mono font-black uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer active:scale-95 touch-manipulation"
                >
                  <Upload className="w-3.5 h-3.5 text-emerald-400" /> Restore Backup
                </button>
                <input 
                  type="file" 
                  ref={fileInputRef} 
                  onChange={handleImportJSON} 
                  accept=".json" 
                  className="hidden" 
                />
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {filteredRides.length === 0 ? (
        <div className={`text-center py-12 ${ui?.bgCard || 'bg-black/60'} border border-dashed ${ui?.brd || 'border-zinc-800'} rounded-3xl space-y-3`}>
          <MapIcon className={`w-10 h-10 ${ui?.txtMuted || 'text-zinc-500'} mx-auto animate-pulse`} />
          <div className={`${ui?.txtMain || 'text-white'} font-mono text-xs uppercase font-bold tracking-widest`}>
            No Mission Logs Matched Current Parameters
          </div>
          <p className={`text-[9px] ${ui?.txtMuted || 'text-zinc-500'} font-mono uppercase`}>
            Try clearing search keywords or category filters
          </p>
        </div>
      ) : (
        <div className="space-y-3.5 max-h-[750px] overflow-y-auto custom-scrollbar pr-1">
          {filteredRides.map((ride: any, index: number) => {
            const rideId = String(ride?.id || index);
            const isExpanded = expandedRideId === rideId;
            const subTab = activeRideSubTab[rideId] || "telemetry";

            const setSubTab = (tab: "telemetry" | "avionics" | "ai" | "notes") => {
              setActiveRideSubTab(prev => ({ ...prev, [rideId]: tab }));
            };

            const rawDist = safeNum(ride?.distance);
            const rawMaxSpd = safeNum(ride?.maxSpeed);
            const rawAvgSpd = safeNum(ride?.avgSpeed);
            const rawEff = safeNum(ride?.efficiencyWhPerMile);

            const rideDist = localMetric ? (rawDist * 1.60934).toFixed(2) : rawDist.toFixed(2);
            const topSpeed = localMetric ? (rawMaxSpd * 1.60934).toFixed(1) : rawMaxSpd.toFixed(1);
            const avgSpeed = localMetric ? (rawAvgSpd * 1.60934).toFixed(1) : rawAvgSpd.toFixed(1);
            const effValue = localMetric ? (rawEff * 0.621371).toFixed(1) : rawEff.toFixed(1);

            const isPersonalDistBest = rawDist === personalBests.maxDist && personalBests.maxDist > 0;
            const isPersonalSpeedBest = rawMaxSpd === personalBests.maxSpd && personalBests.maxSpd > 0;
            const isPersonalEffBest = rawEff === personalBests.bestEff && personalBests.bestEff < 999;

            return (
              <div 
                key={rideId} 
                className={`${ui?.bgBase || 'bg-[#06060a]'} rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex flex-col shadow-inner overflow-hidden group hover:border-zinc-600 transition-colors`}
              >
                <div 
                  className="p-4 sm:p-5 flex flex-col sm:flex-row justify-between sm:items-center gap-4 cursor-pointer touch-manipulation active:bg-zinc-900/30 transition-colors"
                  onClick={() => setExpandedRideId(isExpanded ? null : rideId)}
                >
                  <div className="flex items-center gap-3.5">
                    <div className={`w-12 h-12 shrink-0 rounded-2xl flex items-center justify-center font-black text-xl border-2 shadow-md ${
                      ride.rideScore === 'S+' ? 'bg-purple-950/50 border-purple-500 text-purple-400 shadow-purple-950/40' :
                      ride.rideScore === 'S' ? 'bg-emerald-950/50 border-emerald-500 text-emerald-400 shadow-emerald-950/40' :
                      ride.rideScore === 'A' ? 'bg-[#39ff14]/10 border-[#39ff14]/50 text-[#39ff14]' :
                      'bg-amber-950/40 border-amber-500/50 text-amber-400'
                    }`}>
                      {ride.rideScore || 'A'}
                    </div>

                    <div>
                      <div className="flex items-center gap-2 mb-0.5 flex-wrap">
                        <span className={`${ui?.txtMain || 'text-white'} font-black text-lg sm:text-xl tabular-nums`}>
                          {rideDist} <span className={`text-[10px] ${ui?.txtMuted || 'text-zinc-400'} font-mono`}>{activeDistLabel}</span>
                        </span>
                        <span className={`text-[8px] font-black uppercase tracking-wider px-2 py-0.5 border rounded-lg ${ui?.t?.dim || 'bg-[#39ff14]/10 text-[#39ff14]'}`}>
                          {ride.powerMode || "Standard"}
                        </span>
                        
                        {isPersonalDistBest && (
                          <span className="text-[7.5px] font-mono font-black bg-cyan-950 text-cyan-400 border border-cyan-800 px-1.5 py-0.5 rounded">
                            🏆 PB Distance
                          </span>
                        )}
                        {isPersonalSpeedBest && (
                          <span className="text-[7.5px] font-mono font-black bg-amber-950 text-amber-400 border border-amber-800 px-1.5 py-0.5 rounded">
                            ⚡ PB Speed
                          </span>
                        )}
                        {isPersonalEffBest && (
                          <span className="text-[7.5px] font-mono font-black bg-purple-950 text-purple-400 border border-purple-800 px-1.5 py-0.5 rounded">
                            🌿 PB Efficiency
                          </span>
                        )}
                      </div>

                      <div className={`${ui?.txtMuted || 'text-zinc-400'} font-bold text-[10px] uppercase tracking-wide flex flex-wrap gap-2 items-center`}>
                        <span className={ui?.t?.text || 'text-[#39ff14]'}>{callsign}</span>
                        <span>•</span>
                        <span className={ui?.txtMain || 'text-white'}>{ride.vehicleModel || "Universal PEV"}</span>
                        <span>•</span>
                        <span>{ride.date} ({ride.startTime || "00:00"} - {ride.endTime || "00:00"})</span>
                        <span>•</span>
                        <span className="flex items-center gap-1 font-mono">
                          <Timer className="w-3 h-3 text-cyan-400"/> {Math.floor(safeNum(ride.duration) / 60)}m {Math.floor(safeNum(ride.duration) % 60)}s
                        </span>
                      </div>
                    </div>
                  </div>
                  
                  <div className={`flex items-center justify-between sm:justify-end gap-3 sm:border-l ${ui?.brd || 'border-zinc-800'} sm:pl-6 pt-2 sm:pt-0 border-t sm:border-t-0`}>
                    <div className="text-left sm:text-right font-mono">
                      <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest`}>Top Velocity</div>
                      <div className={`${ui?.txtMain || 'text-white'} font-bold text-sm tabular-nums`}>{topSpeed} {activeSpeedLabel}</div>
                    </div>

                    <button 
                      onClick={(e) => { e.stopPropagation(); exportRideGPX(ride); }} 
                      className={`min-h-[44px] min-w-[44px] flex items-center justify-center ${ui?.bgList || 'bg-[#121318]'} ${ui?.txtMuted || 'text-zinc-400'} hover:text-cyan-400 border ${ui?.brd || 'border-zinc-800'} rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation`}
                      title="Export GPX Satellite Track"
                    >
                      <Download className="w-4 h-4"/>
                    </button>

                    <div className={`min-h-[44px] min-w-[44px] flex items-center justify-center ${ui?.bgList || 'bg-[#121318]'} ${ui?.txtMuted || 'text-zinc-400'} border ${ui?.brd || 'border-zinc-800'} rounded-xl`}>
                      {isExpanded ? <ChevronUp className="w-4 h-4"/> : <ChevronDown className="w-4 h-4"/>}
                    </div>

                    <button 
                      onClick={(e) => { 
                        e.stopPropagation(); 
                        if (confirm(`Delete mission log from ${ride.date}?`)) {
                          const newRides = safeRides.filter((r: any) => r.id !== ride.id);
                          setSavedRides(newRides); 
                          localStorage.setItem("universal_erides_rides", JSON.stringify(newRides)); 
                        }
                      }} 
                      className={`min-h-[44px] min-w-[44px] flex items-center justify-center ${ui?.bgList || 'bg-[#121318]'} ${ui?.txtMuted || 'text-zinc-400'} border ${ui?.brd || 'border-zinc-800'} rounded-xl hover:bg-rose-950/50 hover:text-rose-400 hover:border-rose-900 transition-colors cursor-pointer active:scale-95 touch-manipulation`}
                      title="Delete Log"
                    >
                      <Trash2 className="w-4 h-4"/>
                    </button>
                  </div>
                </div>

                <AnimatePresence>
                  {isExpanded && (
                    <motion.div 
                      initial={{ height: 0, opacity: 0 }} 
                      animate={{ height: "auto", opacity: 1 }} 
                      exit={{ height: 0, opacity: 0 }} 
                      className="overflow-hidden border-t border-zinc-800"
                    >
                      <div className={`${ui?.bgCard || 'bg-black/60'} p-4 sm:p-6 space-y-4`}>
                        
                        <div className="flex gap-2 border-b border-zinc-800 pb-3 overflow-x-auto custom-scrollbar">
                          {[
                            { id: "telemetry", label: "Telemetry & Power", icon: Zap },
                            { id: "avionics", label: "IMU & Dynamics", icon: Scale },
                            { id: "ai", label: "AI Neural Debrief", icon: BrainCircuit },
                            { id: "notes", label: "Pilot Notes & Broadcast", icon: BookOpen },
                          ].map((t) => (
                            <button
                              key={t.id}
                              onClick={() => setSubTab(t.id as any)}
                              className={`min-h-[44px] px-3.5 rounded-xl text-[10px] font-mono font-black uppercase tracking-wider flex items-center gap-1.5 shrink-0 transition-all cursor-pointer active:scale-95 touch-manipulation ${subTab === t.id ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-md` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.txtMuted || 'text-zinc-400'} border ${ui?.brd || 'border-zinc-800'}`}`}
                            >
                              <t.icon className="w-3.5 h-3.5" /> {t.label}
                            </button>
                          ))}
                        </div>

                        <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-3 rounded-2xl grid grid-cols-2 sm:grid-cols-5 gap-2 font-mono text-[9.5px]`}>
                          <div><span className={ui?.txtMuted || 'text-zinc-400'}>PACK:</span> <strong className="text-white ml-1">{ride.batteryVoltage || 48}V ({ride.batteryTopology || '14S'})</strong></div>
                          <div><span className={ui?.txtMuted || 'text-zinc-400'}>CAPACITY:</span> <strong className="text-white ml-1">{ride.batteryCapacity || 15} Ah</strong></div>
                          <div><span className={ui?.txtMuted || 'text-zinc-400'}>MOTOR:</span> <strong className="text-white ml-1">{ride.motorWattage || 1000} W</strong></div>
                          <div><span className={ui?.txtMuted || 'text-zinc-400'}>ESC LIMIT:</span> <strong className="text-white ml-1">{ride.controllerAmps || 25} A</strong></div>
                          <div><span className={ui?.txtMuted || 'text-zinc-400'}>WHEEL:</span> <strong className="text-white ml-1">{ride.wheelSize || 10}"</strong></div>
                        </div>

                        {subTab === "telemetry" && (
                          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
                            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2.5">
                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner flex flex-col justify-center`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <Gauge className="w-3 h-3 text-cyan-400"/> Avg Velocity
                                </div>
                                <div className={`text-xs font-mono font-black ${ui?.txtMain || 'text-white'}`}>{avgSpeed} {activeSpeedLabel}</div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner flex flex-col justify-center`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <Zap className="w-3 h-3 text-amber-400"/> Peak Output
                                </div>
                                <div className={`text-xs font-mono font-black ${ui?.txtMain || 'text-white'}`}>{safeNum(ride.peakWatts).toFixed(0)} W</div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner flex flex-col justify-center`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <Battery className="w-3 h-3 text-cyan-400"/> Ah Consumed
                                </div>
                                <div className={`text-xs font-mono font-black ${ui?.txtMain || 'text-white'}`}>{safeNum(ride.totalAhConsumed).toFixed(2)} Ah</div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner flex flex-col justify-center`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <ZapOff className="w-3 h-3 text-rose-400"/> Max Sag Voltage
                                </div>
                                <div className={`text-xs font-mono font-black ${ui?.txtMain || 'text-white'}`}>{safeNum(ride.minVoltage, safeNum(ride.batteryVoltage, 48)).toFixed(1)} V</div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner flex flex-col justify-center`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <Activity className={`w-3 h-3 ${ui?.t?.text || 'text-[#39ff14]'}`}/> Efficiency
                                </div>
                                <div className={`text-xs font-mono font-black ${ui?.t?.text || 'text-[#39ff14]'}`}>
                                  {effValue} <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'}`}>Wh/{activeDistLabel.toLowerCase()}</span>
                                </div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner flex flex-col justify-center`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <DollarSign className="w-3 h-3 text-emerald-400"/> Gas Saved
                                </div>
                                <div className="text-xs font-mono font-black text-emerald-400">${safeNum(ride.financialSavings).toFixed(2)}</div>
                              </div>
                            </div>

                            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl p-4 space-y-2`}>
                              <div className="flex justify-between text-[9px] font-mono font-bold uppercase text-zinc-300">
                                <span>Starting Charge: {safePct(ride.startingBattery)}%</span>
                                <span className="text-rose-400">Drained: -{safeNum(ride.batteryPercentDropped, safePct(ride.startingBattery) - safePct(ride.endingBattery)).toFixed(1)}%</span>
                                <span>Ending Charge: {safePct(ride.endingBattery)}%</span>
                              </div>
                              <div className="w-full bg-zinc-900 h-2.5 rounded-full overflow-hidden flex">
                                <div className="h-full bg-emerald-500" style={{ width: `${safePct(ride.endingBattery)}%` }}></div>
                                <div className="h-full bg-rose-500/60" style={{ width: `${safePct(safeNum(ride.startingBattery, 100) - safeNum(ride.endingBattery, 0))}%` }}></div>
                              </div>
                            </div>
                          </motion.div>
                        )}

                        {subTab === "avionics" && (
                          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <TrendingUp className="w-3 h-3 text-emerald-400"/> Max Incline
                                </div>
                                <div className="text-xs font-mono font-black text-white">{safeNum(ride.maxGrade).toFixed(1)}%</div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <Scale className="w-3 h-3 text-purple-400"/> Max G-Force
                                </div>
                                <div className="text-xs font-mono font-black text-white">{safeNum(ride.maxGForce).toFixed(2)}G</div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <ShieldAlert className="w-3 h-3 text-rose-400"/> Hard Braking
                                </div>
                                <div className="text-xs font-mono font-black text-rose-400">{safeNum(ride.hardBrakes, 0)} events</div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center shadow-inner`}>
                                <div className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-1 flex items-center justify-center gap-1`}>
                                  <Mountain className="w-3 h-3 text-amber-400"/> Suspension Hits
                                </div>
                                <div className="text-xs font-mono font-black text-amber-400">{safeNum(ride.suspensionBumps, 0)} bumps</div>
                              </div>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                              <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl p-3.5 flex items-center gap-3`}>
                                <CloudRain className="w-5 h-5 text-cyan-400 shrink-0" />
                                <div>
                                  <div className={`text-[8px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Atmospheric Snapshot</div>
                                  <div className="text-xs font-bold text-white mt-0.5">{ride.weatherCondition || "Offline telemetry snapshot"}</div>
                                </div>
                              </div>

                              <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl p-3.5 flex items-center gap-3`}>
                                <Disc className="w-5 h-5 text-amber-400 shrink-0" />
                                <div>
                                  <div className={`text-[8px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Hardware Payload</div>
                                  <div className="text-xs font-bold text-white mt-0.5">{ride.tirePsiLogged || tirePsi} PSI • {ride.towingTrailer ? "Trailer Attached" : "Solo Vehicle"}</div>
                                </div>
                              </div>
                            </div>
                          </motion.div>
                        )}

                        {subTab === "ai" && (
                          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
                            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl p-4 shadow-inner space-y-3`}>
                              <div className="flex items-center justify-between">
                                <span className={`text-[9px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'} flex items-center gap-1.5`}>
                                  <BrainCircuit className="w-4 h-4 text-cyan-400"/> Gemini AI Tactical Debrief
                                </span>
                                <button 
                                  onClick={() => triggerHistoricalAiDebrief(ride)} 
                                  disabled={isFetchingHistoricalAi === ride.id}
                                  className={`min-h-[44px] px-4 rounded-xl text-[9px] font-mono font-black uppercase tracking-widest transition-all cursor-pointer active:scale-95 touch-manipulation ${isFetchingHistoricalAi === ride.id ? 'bg-zinc-800 text-zinc-500' : `${ui?.t?.bg || 'bg-[#39ff14]'} text-black shadow-md`}`}
                                >
                                  {isFetchingHistoricalAi === ride.id ? "Analyzing..." : "Generate AI Debrief"}
                                </button>
                              </div>

                              {historicalAiAnalysis[ride.id] ? (
                                <motion.div initial={{ opacity: 0, y: -5 }} animate={{ opacity: 1, y: 0 }} className="p-3.5 rounded-xl bg-black/60 border border-purple-900/60 text-xs font-mono font-bold text-purple-100 leading-relaxed shadow-inner">
                                  <span className="text-purple-400 font-black">AI COACH: </span> {historicalAiAnalysis[ride.id]}
                                </motion.div>
                              ) : (
                                <div className="text-[10px] font-mono text-zinc-500 uppercase">
                                  Tap button above to generate a comprehensive AI debrief regarding power drain, rider aggression, and terrain impact.
                                </div>
                              )}
                            </div>
                          </motion.div>
                        )}

                        {subTab === "notes" && (
                          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
                            <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl p-4 shadow-inner space-y-3`}>
                              
                              <div className="space-y-1.5">
                                <div className="flex justify-between items-center">
                                  <span className={`text-[8.5px] font-mono font-black uppercase ${ui?.txtMuted || 'text-zinc-400'} flex items-center gap-1.5`}>
                                    <Tag className="w-3 h-3 text-cyan-400" /> Mission Tags
                                  </span>
                                  {editingTagRideId !== rideId && (
                                    <button 
                                      onClick={() => setEditingTagRideId(rideId)}
                                      className="text-[8.5px] font-mono text-cyan-400 hover:underline uppercase font-bold"
                                    >
                                      + Add Tag
                                    </button>
                                  )}
                                </div>

                                <div className="flex flex-wrap gap-1.5 items-center">
                                  {Array.isArray(ride.tags) && ride.tags.map((tStr: string) => (
                                    <span key={tStr} className="text-[9px] font-mono font-bold bg-zinc-800 text-cyan-300 border border-zinc-700 px-2 py-0.5 rounded-lg flex items-center gap-1">
                                      {tStr}
                                      <button onClick={() => handleRemoveTag(ride.id, tStr)} className="text-zinc-500 hover:text-rose-400 ml-0.5">×</button>
                                    </span>
                                  ))}

                                  {editingTagRideId === rideId && (
                                    <div className="flex items-center gap-1 mt-1 w-full sm:w-auto">
                                      <input 
                                        type="text" 
                                        value={newTagInput}
                                        onChange={(e) => setNewTagInput(e.target.value)}
                                        placeholder="e.g. Commute, Trail" 
                                        className="min-h-[36px] bg-black border border-cyan-500 rounded-lg px-2.5 text-xs font-mono font-bold text-white outline-none"
                                        autoFocus
                                      />
                                      <button 
                                        onClick={() => handleAddCustomTag(ride.id)}
                                        className="min-h-[36px] px-3 bg-cyan-600 text-black rounded-lg text-[9px] font-mono font-black uppercase"
                                      >
                                        Save
                                      </button>
                                      <button 
                                        onClick={() => setEditingTagRideId(null)}
                                        className="min-h-[36px] px-2 text-zinc-400 text-xs"
                                      >
                                        Cancel
                                      </button>
                                    </div>
                                  )}
                                </div>
                              </div>

                              <div className="flex items-center justify-between pt-2">
                                <span className={`text-[9px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'} flex items-center gap-1.5`}>
                                  <BookOpen className="w-4 h-4 text-amber-400"/> Mission Notes & Journal
                                </span>
                                <Save className="w-3.5 h-3.5 text-zinc-500" />
                              </div>

                              <textarea 
                                value={ride.rideNote || ""} 
                                onChange={(e) => updateRideNote(ride.id, e.target.value)}
                                placeholder="Log road conditions, tire pressure adjustments, battery performance, or mod feelings..."
                                className={`w-full ${ui?.bgBase || 'bg-[#06060a]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 text-xs font-mono font-bold ${ui?.txtMain || 'text-white'} outline-none focus:border-cyan-500 transition-colors custom-scrollbar resize-none h-24`}
                              />

                              <button 
                                onClick={() => handleBroadcastRideToBoard(ride)}
                                className="w-full min-h-[48px] rounded-xl font-mono font-black uppercase tracking-widest text-[10px] flex items-center justify-center gap-2 shadow-lg transition-all active:scale-95 touch-manipulation cursor-pointer bg-cyan-600 hover:bg-cyan-500 text-black"
                              >
                                <Send className="w-4 h-4"/> Broadcast Mission Run to Community Board
                              </button>
                            </div>
                          </motion.div>
                        )}

                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}