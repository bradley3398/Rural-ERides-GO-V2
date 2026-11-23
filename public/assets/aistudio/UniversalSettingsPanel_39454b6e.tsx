"use client";

import React from "react";
import { SlidersHorizontal, Globe, Volume2, Radar, RefreshCw, Search, X, Crosshair } from "lucide-react";

export default function UniversalSettingsPanel({
  t,
  bgPanel,
  brd,
  txtMain,
  txtMuted,
  bgList,
  bgCard,
  bgInput,
  bgBase,
  useMetric,
  setUseMetric,
  autoPlayAudio,
  setAutoPlayAudio,
  proximityAlerts,
  setProximityAlerts,
  radarRadius,
  setRadarRadius,
  geoFenceRadius,
  setGeoFenceRadius,
  elevationHistory = [],
  searchQuery,
  setSearchQuery,
  handleSearchLocation,
  isSearching,
  customPin,
  setCustomPin,
  compassBearingToTarget,
  calculateDistance,
  userLat,
  userLng,
  recenterMap,
  glassBlurClass = "backdrop-blur-2xl",
  cornerStyle = "rounded-3xl"
}: any) {
  return (
    <div className={`app-card ${glassBlurClass} ${cornerStyle} ${bgPanel} border ${brd} p-5 shadow-2xl relative overflow-hidden flex flex-col justify-between font-sans`}>
      <div className={`absolute top-0 left-0 w-full h-1 ${t.bg}`} />
      
      <div className="space-y-4">
        <h3 className={`${txtMain} font-black text-sm tracking-widest flex items-center gap-2 uppercase font-mono`}>
          <SlidersHorizontal className={`w-4 h-4 ${t.text}`} /> UNIVERSAL SETTINGS
        </h3>

        <div className={`flex items-center justify-between ${bgList} border ${brd} p-2.5 rounded-2xl shadow-inner`}>
          <span className={`text-[10px] ${txtMuted} uppercase font-bold tracking-wider flex items-center gap-1.5`}>
            <Globe className={`w-3.5 h-3.5 ${t.text}`} /> Measurement Units
          </span>
          <div className={`flex ${bgCard} rounded-xl p-0.5 border ${brd}`}>
            <button 
              type="button"
              onClick={() => setUseMetric(false)} 
              className={`min-h-[32px] px-3 text-[9px] font-black uppercase rounded-lg transition-all cursor-pointer ${!useMetric ? `${t.bg} text-black font-black shadow-sm` : txtMuted}`}
            >
              Imperial
            </button>
            <button 
              type="button"
              onClick={() => setUseMetric(true)} 
              className={`min-h-[32px] px-3 text-[9px] font-black uppercase rounded-lg transition-all cursor-pointer ${useMetric ? `${t.bg} text-black font-black shadow-sm` : txtMuted}`}
            >
              Metric
            </button>
          </div>
        </div>

        <div className={`flex items-center justify-between ${bgList} border ${brd} p-2.5 rounded-2xl shadow-inner`}>
          <span className={`text-[10px] ${txtMuted} uppercase font-bold tracking-wider flex items-center gap-1.5`}>
            <Volume2 className={`w-3.5 h-3.5 ${t.text}`} /> Walkie Auto-Play
          </span>
          <button 
            type="button"
            onClick={() => setAutoPlayAudio(!autoPlayAudio)} 
            className={`relative inline-flex min-h-[26px] min-w-[46px] items-center rounded-full transition-colors shadow-inner cursor-pointer ${autoPlayAudio ? t.bg : "bg-zinc-800"}`}
          >
            <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition shadow-sm ${autoPlayAudio ? "translate-x-5" : "translate-x-1"}`} />
          </button>
        </div>

        <div className={`flex items-center justify-between ${bgList} border ${brd} p-2.5 rounded-2xl shadow-inner`}>
          <span className={`text-[10px] ${txtMuted} uppercase font-bold tracking-wider flex items-center gap-1.5`}>
            <Radar className={`w-3.5 h-3.5 ${t.text}`} /> Proximity Sonar Alerts
          </span>
          <button 
            type="button"
            onClick={() => setProximityAlerts(!proximityAlerts)} 
            className={`relative inline-flex min-h-[26px] min-w-[46px] items-center rounded-full transition-colors shadow-inner cursor-pointer ${proximityAlerts ? t.bg : "bg-zinc-800"}`}
          >
            <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition shadow-sm ${proximityAlerts ? "translate-x-5" : "translate-x-1"}`} />
          </button>
        </div>

        <div>
          <div className="flex justify-between items-center mb-1">
            <label className={`text-[9px] ${txtMuted} uppercase tracking-widest block font-mono font-black`}>Comms & Radar Sweep Radius</label>
            <span className={`text-[10px] font-black ${t.text}`}>{radarRadius} {useMetric ? 'km' : 'mi'}</span>
          </div>
          <input 
            type="range" 
            min="1" 
            max="250" 
            step="1" 
            value={radarRadius} 
            onChange={(e) => setRadarRadius(parseInt(e.target.value))} 
            className={`w-full h-1.5 bg-zinc-800 rounded-lg outline-none accent-current ${t.text} cursor-pointer`} 
          />
        </div>

        <div>
          <div className="flex justify-between items-center mb-1">
            <label className={`text-[9px] ${txtMuted} uppercase tracking-widest block font-mono font-black`}>Geo-Fence Alarm Boundary</label>
            <span className={`text-[10px] font-black ${t.text}`}>{geoFenceRadius} {useMetric ? 'km' : 'mi'}</span>
          </div>
          <input 
            type="range" 
            min="1" 
            max="50" 
            step="1" 
            value={geoFenceRadius} 
            onChange={(e) => setGeoFenceRadius(parseInt(e.target.value))} 
            className={`w-full h-1.5 bg-zinc-800 rounded-lg outline-none accent-current ${t.text} cursor-pointer`} 
          />
        </div>

        <div className={`p-3 rounded-2xl ${bgList} border ${brd} space-y-1 shadow-inner`}>
          <span className="text-[8px] text-zinc-400 font-mono font-black uppercase tracking-widest block">Elevation Profile</span>
          <div className="flex items-end gap-1.5 h-10 pt-2">
            {elevationHistory.map((alt: number, idx: number) => (
              <div 
                key={idx} 
                className={`flex-1 ${t.bg} opacity-50 hover:opacity-100 rounded-t transition-all`} 
                style={{ height: `${Math.max(10, Math.min(100, (alt / 300) * 100))}%` }} 
                title={`${alt} ft`} 
              />
            ))}
          </div>
        </div>

        <div className={`pt-2 border-t ${brd}`}>
          <form onSubmit={handleSearchLocation} className="relative mt-2">
            <input 
              type="text" 
              value={searchQuery} 
              onChange={(e) => setSearchQuery(e.target.value)} 
              placeholder="Search Target City or Town..." 
              className={`w-full min-h-[44px] ${bgInput} text-xs ${txtMain} rounded-xl pl-3 pr-12 py-2.5 outline-none focus:${t.border} transition-colors font-bold shadow-inner`} 
            />
            <button 
              type="submit" 
              disabled={isSearching || !searchQuery} 
              className={`absolute right-1.5 top-1/2 -translate-y-1/2 min-h-[36px] min-w-[36px] flex items-center justify-center ${t.text} hover:${txtMain} disabled:opacity-50 transition-all cursor-pointer`}
            >
              {isSearching ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
            </button>
          </form>
        </div>

        {customPin && (
          <div className={`${t.dim} rounded-2xl p-3 mb-2 flex flex-col gap-2 shadow-sm`}>
            <div className="flex justify-between items-center">
              <div className="truncate pr-2">
                <span className="text-xs text-white truncate block mt-0.5 font-bold">🎯 {customPin.name}</span>
                {compassBearingToTarget !== null && (
                  <span className="text-[10px] font-mono text-cyan-300 block">
                    Bearing: {compassBearingToTarget}° | Dist: {calculateDistance(userLat, userLng, customPin.lat, customPin.lng, useMetric).toFixed(1)} {useMetric ? 'km' : 'mi'}
                  </span>
                )}
              </div>
              <button 
                type="button" 
                onClick={() => setCustomPin(null)} 
                className={`min-h-[36px] min-w-[36px] flex items-center justify-center ${bgBase} rounded-xl text-zinc-500 hover:text-red-400 shrink-0 border ${brd} transition-all cursor-pointer`}
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>
      
      <button 
        type="button"
        onClick={recenterMap} 
        className={`mt-4 w-full min-h-[44px] py-2.5 rounded-2xl font-black uppercase tracking-widest text-xs ${bgList} hover:bg-white/10 ${txtMain} border ${brd} transition-all active:scale-95 flex justify-center items-center gap-2 shadow-md cursor-pointer`}
      >
        <Crosshair className="w-4 h-4" /> Snap Map to Hardware Location
      </button>
    </div>
  );
}