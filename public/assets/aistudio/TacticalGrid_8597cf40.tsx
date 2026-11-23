"use client";

import React, { useState } from "react";
import { Globe, ChevronDown, ChevronUp, Compass, Wrench, ShieldAlert, Camera } from "lucide-react";

interface TacticalGridProps {
  showTactical: boolean;
  setShowTactical: (show: boolean) => void;
  tacticalFeatures: Array<{ id: string; icon: any; label: string; prompt: string }>;
  handleSearch: (prompt: string) => void;
  themeColors: any;
}

export default function TacticalGrid({ 
  showTactical, 
  setShowTactical, 
  tacticalFeatures = [], 
  handleSearch, 
  themeColors: t 
}: TacticalGridProps) {
  
  // --- 🔥 NEW: TABBED TACTICAL CATEGORIES 🔥 ---
  const [activeTab, setActiveTab] = useState<"Nav" | "Recon" | "Survive" | "Mech">("Nav");

  const categorizeFeature = (label: string) => {
    const l = label.toLowerCase();
    if (l.includes("scout") || l.includes("locator") || l.includes("history") || l.includes("route")) return "Nav";
    if (l.includes("weather") || l.includes("wind") || l.includes("wet") || l.includes("night") || l.includes("photos") || l.includes("vlog")) return "Recon";
    if (l.includes("defense") || l.includes("sos") || l.includes("panic") || l.includes("legal") || l.includes("ordinance")) return "Survive";
    return "Mech"; 
  };

  const filteredFeatures = tacticalFeatures.filter(f => categorizeFeature(f.label) === activeTab);

  return (
    <>
      <button 
        type="button"
        onClick={() => setShowTactical(!showTactical)}
        className="w-full min-h-[48px] flex items-center justify-between bg-white/5 hover:bg-white/10 border border-white/10 px-5 rounded-2xl mb-3 text-zinc-300 hover:text-white transition-all duration-300 active:scale-95 shadow-md shrink-0 cursor-pointer backdrop-blur-md"
      >
        <span className="text-[10px] font-black uppercase tracking-widest flex items-center gap-2.5">
          <Globe className={`w-4 h-4 ${t.text}`} /> Tactical Quick Actions
        </span>
        {showTactical ? <ChevronUp className="w-5 h-5 text-zinc-400" /> : <ChevronDown className="w-5 h-5 text-zinc-400" />}
      </button>

      {showTactical && (
        <div className="mb-3 shrink-0 bg-black/40 border border-white/10 rounded-2xl p-2 shadow-inner backdrop-blur-md">
          {/* TABS */}
          <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-2 mb-2 border-b border-white/10 px-1">
             <button onClick={() => setActiveTab("Nav")} className={`min-h-[36px] px-4 rounded-xl text-[10px] font-black uppercase flex items-center gap-1.5 transition-colors cursor-pointer ${activeTab === "Nav" ? `${t.bg} text-black shadow-md` : 'bg-white/5 text-zinc-400 hover:text-white'}`}><Compass className="w-3.5 h-3.5"/> Nav</button>
             <button onClick={() => setActiveTab("Recon")} className={`min-h-[36px] px-4 rounded-xl text-[10px] font-black uppercase flex items-center gap-1.5 transition-colors cursor-pointer ${activeTab === "Recon" ? `${t.bg} text-black shadow-md` : 'bg-white/5 text-zinc-400 hover:text-white'}`}><Camera className="w-3.5 h-3.5"/> Recon</button>
             <button onClick={() => setActiveTab("Mech")} className={`min-h-[36px] px-4 rounded-xl text-[10px] font-black uppercase flex items-center gap-1.5 transition-colors cursor-pointer ${activeTab === "Mech" ? `${t.bg} text-black shadow-md` : 'bg-white/5 text-zinc-400 hover:text-white'}`}><Wrench className="w-3.5 h-3.5"/> Mech</button>
             <button onClick={() => setActiveTab("Survive")} className={`min-h-[36px] px-4 rounded-xl text-[10px] font-black uppercase flex items-center gap-1.5 transition-colors cursor-pointer ${activeTab === "Survive" ? `bg-rose-600 text-white shadow-md` : 'bg-white/5 text-zinc-400 hover:text-white'}`}><ShieldAlert className="w-3.5 h-3.5"/> Survive</button>
          </div>

          {/* FILTERED GRID */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2.5 overflow-y-auto max-h-[200px] custom-scrollbar px-1">
            {filteredFeatures.map((feature) => (
              <button 
                key={feature.id}
                type="button"
                onClick={() => handleSearch(feature.prompt)} 
                className={`min-h-[48px] flex items-center gap-3 bg-black/60 border border-white/5 hover:bg-white/10 text-zinc-300 hover:text-white text-[10px] uppercase font-black px-4 py-2 rounded-xl transition-all active:scale-95 text-left cursor-pointer shadow-sm ${activeTab === "Survive" ? 'hover:border-rose-500/50' : 'hover:border-white/20'}`}
              >
                <feature.icon className={`w-4 h-4 shrink-0 ${activeTab === "Survive" ? 'text-rose-400' : t.text}`} />
                <span className="truncate tracking-wide">{feature.label}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </>
  );
}