"use client";

import React from "react";
import { motion } from "framer-motion";
import { X, Settings2, BrainCircuit, Palette } from "lucide-react";

interface AssistantSettingsModalProps {
  showSettings: boolean;
  setShowSettings: (show: boolean) => void;
  googleCseId: string;
  setGoogleCsetId: (id: string) => void;
  persona: string;
  setPersona: (p: string) => void;
  maxImageCount: number;
  setMaxImageCount: (c: number) => void;
  useGoogleEngine: boolean;
  setUseGoogleEngine: (val: boolean) => void;
  useBraveEngine: boolean;
  setUseBraveEngine: (val: boolean) => void;
  useTavilyEngine: boolean;
  setUseTavilyEngine: (val: boolean) => void;
  enableYouTubeSearch: boolean;
  setEnableYouTubeSearch: (val: boolean) => void;
  hudOpacity: string;
  setHudOpacity: (opacity: string) => void;
  customDirective: string;
  setCustomDirective: (directive: string) => void;
  isLocationLocked: boolean;
  toggleLocationLock: () => void;
  baseZone: string;
  setBaseZone: (zone: string) => void;
  privacyMode: boolean;
  setPrivacyMode: (val: boolean) => void;
  autoClearImages: boolean;
  setAutoClearImages: (val: boolean) => void;
  enableImageSearch: boolean;
  setEnableImageSearch: (val: boolean) => void;
  exportChatHistory: () => void;
  clearMemory: () => void;
  themeColors: any;
  hudOpacityOptions: Array<{ val: string; label: string }>;
  personaOptions: Array<{ id: string; label: string }>;
  maxImageOptions: number[];
}

export default function AssistantSettingsModal({
  showSettings, 
  setShowSettings, 
  googleCseId, 
  setGoogleCsetId,
  persona, 
  setPersona, 
  maxImageCount, 
  setMaxImageCount,
  useGoogleEngine, 
  setUseGoogleEngine, 
  useBraveEngine, 
  setUseBraveEngine,
  useTavilyEngine, 
  setUseTavilyEngine, 
  enableYouTubeSearch, 
  setEnableYouTubeSearch,
  hudOpacity, 
  setHudOpacity, 
  customDirective, 
  setCustomDirective,
  isLocationLocked, 
  toggleLocationLock, 
  baseZone, 
  setBaseZone,
  privacyMode, 
  setPrivacyMode, 
  autoClearImages, 
  setAutoClearImages,
  enableImageSearch, 
  setEnableImageSearch, 
  exportChatHistory, 
  clearMemory,
  themeColors: t, 
  hudOpacityOptions = [], 
  personaOptions = [], 
  maxImageOptions = []
}: AssistantSettingsModalProps) {
  if (!showSettings) return null;

  return (
    <div className="fixed inset-0 z-[999999] bg-black/80 backdrop-blur-xl flex items-center justify-center p-4 font-sans">
      <motion.div 
        initial={{ scale: 0.95, opacity: 0 }} 
        animate={{ scale: 1, opacity: 1 }} 
        exit={{ scale: 0.95, opacity: 0 }} 
        className="bg-black/60 backdrop-blur-3xl border border-white/10 w-full max-w-3xl rounded-3xl p-6 shadow-[0_8px_32px_0_rgba(0,0,0,0.36)] flex flex-col max-h-[85vh] overflow-hidden"
      >
        
        <div className="flex justify-between items-center border-b border-white/10 pb-4 mb-4">
          <h3 className={`text-sm font-black uppercase tracking-widest flex items-center gap-2 ${t.text}`}>
            <Settings2 className="w-5 h-5" /> Universal Matrix Settings &amp; Advanced Engines
          </h3>
          <button 
            type="button"
            onClick={() => setShowSettings(false)}
            className="bg-white/5 border border-white/10 text-zinc-400 p-2.5 rounded-xl cursor-pointer hover:bg-white/10 hover:text-white transition-all active:scale-95 min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-y-auto flex flex-col gap-5 pr-2 custom-scrollbar">
          
          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner">
            <label className="text-[11px] text-zinc-400 font-black uppercase block mb-3 tracking-widest">Google Custom Search Engine (CSE) ID</label>
            <input 
              type="text" 
              value={googleCseId} 
              onChange={(e) => setGoogleCsetId(e.target.value)} 
              placeholder="e.g. 0123456789abcdef0:xyz123"
              className="w-full min-h-[48px] bg-black/50 border border-white/10 rounded-xl px-4 text-sm font-bold text-white outline-none focus:border-white/30 transition-colors shadow-inner"
            />
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner flex flex-col gap-3">
            <span className="text-[11px] text-zinc-400 font-black uppercase tracking-widest flex items-center gap-2">
              <BrainCircuit className="w-4 h-4" /> Override Primary Copilot Persona
            </span>
            <select 
              value={persona} 
              onChange={(e) => setPersona(e.target.value)}
              className="bg-black/50 border border-white/10 text-white text-xs font-bold rounded-xl px-4 py-3 outline-none cursor-pointer shadow-inner w-full uppercase tracking-wider min-h-[48px]"
            >
              {personaOptions.map((p) => <option key={p.id} value={p.id}>{p.label}</option>)}
            </select>
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner flex flex-col gap-4">
            <span className="text-[11px] text-zinc-400 font-black uppercase tracking-widest">Image &amp; Research Engine Configuration</span>
            
            <div className="flex justify-between items-center flex-wrap gap-3">
              <span className="text-[10px] text-zinc-500 uppercase font-bold tracking-widest">Max Image Results (Up to 20):</span>
              <select 
                value={maxImageCount} 
                onChange={(e) => setMaxImageCount(Number(e.target.value))}
                className={`bg-black/50 ${t.text} border border-white/10 rounded-xl px-4 py-2 text-xs font-bold outline-none cursor-pointer shadow-inner min-h-[48px]`}
              >
                {maxImageOptions.map((m) => <option key={m} value={m}>{m} Results</option>)}
              </select>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 pt-3 border-t border-white/5">
              <label className="text-[10px] min-h-[44px] flex items-center bg-white/5 px-3 rounded-lg border border-white/10 text-zinc-300 gap-2 cursor-pointer font-bold uppercase tracking-wider active:scale-95 transition-transform">
                <input type="checkbox" checked={useGoogleEngine} onChange={(e) => setUseGoogleEngine(e.target.checked)} className="accent-current w-4 h-4" /> Google CSE
              </label>
              <label className="text-[10px] min-h-[44px] flex items-center bg-white/5 px-3 rounded-lg border border-white/10 text-zinc-300 gap-2 cursor-pointer font-bold uppercase tracking-wider active:scale-95 transition-transform">
                <input type="checkbox" checked={useBraveEngine} onChange={(e) => setUseBraveEngine(e.target.checked)} className="accent-current w-4 h-4" /> Brave Images
              </label>
              <label className="text-[10px] min-h-[44px] flex items-center bg-white/5 px-3 rounded-lg border border-white/10 text-zinc-300 gap-2 cursor-pointer font-bold uppercase tracking-wider active:scale-95 transition-transform">
                <input type="checkbox" checked={useTavilyEngine} onChange={(e) => setUseTavilyEngine(e.target.checked)} className="accent-current w-4 h-4" /> Tavily Deep
              </label>
              <label className="text-[10px] min-h-[44px] flex items-center bg-cyan-950/40 px-3 rounded-lg border border-cyan-900/50 text-cyan-400 gap-2 cursor-pointer font-bold uppercase tracking-wider drop-shadow-sm active:scale-95 transition-transform">
                <input type="checkbox" checked={enableYouTubeSearch} onChange={(e) => setEnableYouTubeSearch(e.target.checked)} className="accent-cyan-400 w-4 h-4" /> YouTube Fetch
              </label>
            </div>
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner flex flex-col gap-3">
            <span className="text-[11px] text-zinc-400 font-black uppercase tracking-widest flex items-center gap-2">
              <Palette className="w-4 h-4" /> Glassmorphic HUD Opacity
            </span>
            <select 
              value={hudOpacity} 
              onChange={(e) => setHudOpacity(e.target.value)}
              className="bg-black/50 border border-white/10 text-white text-xs font-bold rounded-xl px-4 py-3 outline-none cursor-pointer shadow-inner w-full uppercase tracking-wider min-h-[48px]"
            >
              {hudOpacityOptions.map((opt) => <option key={opt.val} value={opt.val}>{opt.label}</option>)}
            </select>
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner">
            <label className="text-[11px] text-zinc-400 font-black uppercase block mb-3 tracking-widest">Hardware &amp; Weight Specs (AI Context)</label>
            <textarea 
              value={customDirective} 
              onChange={(e) => setCustomDirective(e.target.value)} 
              placeholder="Enter custom hardware parameters..."
              className="w-full min-h-[80px] bg-black/50 border border-white/10 rounded-xl p-4 text-sm text-white outline-none focus:border-white/30 resize-none transition-colors shadow-inner leading-relaxed"
            />
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner flex flex-col gap-4">
            <div className="flex justify-between items-center flex-wrap gap-3">
              <div>
                <span className="text-[11px] text-zinc-400 font-black uppercase tracking-widest block">Location Target Lock</span>
                <span className="text-[9px] text-zinc-500 uppercase font-bold">Lock current GPS or custom zone universally</span>
              </div>
              <button 
                type="button"
                onClick={toggleLocationLock}
                className={`min-h-[44px] px-5 rounded-xl text-[10px] font-black uppercase tracking-widest border transition-all duration-300 active:scale-95 cursor-pointer ${
                  isLocationLocked ? `bg-white/10 border-white/20 ${t.text}` : 'bg-black/50 border-white/10 text-zinc-500 hover:text-white shadow-inner'
                }`}
              >
                {isLocationLocked ? "LOCKED" : "GPS AUTO"}
              </button>
            </div>
            
            <div className="pt-3 border-t border-white/5">
              <label className="text-[10px] text-zinc-400 font-black uppercase tracking-widest block mb-2">Custom Base Zone / City (Universal Fallback)</label>
              <input 
                type="text" 
                value={baseZone} 
                onChange={(e) => setBaseZone(e.target.value)} 
                placeholder="e.g. Denver, CO or London, UK"
                className="w-full min-h-[48px] bg-black/50 border border-white/10 rounded-xl px-4 text-sm font-bold text-white outline-none focus:border-white/30 transition-colors shadow-inner"
              />
            </div>
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner flex justify-between items-center gap-4 flex-wrap">
            <div>
              <span className="text-[11px] text-rose-400 font-black uppercase tracking-widest block">Incognito Privacy Mode</span>
              <span className="text-[9px] text-zinc-500 uppercase font-bold">Disables local storage conversation logging</span>
            </div>
            <button 
              type="button"
              onClick={() => setPrivacyMode(!privacyMode)} 
              className={`relative inline-flex min-h-[32px] min-w-[56px] items-center rounded-full transition-colors cursor-pointer shadow-inner border border-white/10 ${
                privacyMode ? 'bg-rose-500' : 'bg-zinc-800'
              }`}
            >
              <span className={`inline-block h-6 w-6 transform rounded-full bg-white transition shadow-sm ${privacyMode ? 'translate-x-7' : 'translate-x-1.5'}`} />
            </button>
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner flex justify-between items-center gap-4 flex-wrap">
            <div>
              <span className="text-[11px] text-zinc-400 font-black uppercase tracking-widest block">Auto-Clear Optical Feed</span>
              <span className="text-[9px] text-zinc-500 uppercase font-bold">Clears uploaded image after every prompt</span>
            </div>
            <button 
              type="button"
              onClick={() => setAutoClearImages(!autoClearImages)} 
              className={`relative inline-flex min-h-[32px] min-w-[56px] items-center rounded-full transition-colors cursor-pointer shadow-inner border border-white/10 ${
                autoClearImages ? t.bg : 'bg-zinc-800'
              }`}
            >
              <span className={`inline-block h-6 w-6 transform rounded-full bg-white transition shadow-sm ${autoClearImages ? 'translate-x-7' : 'translate-x-1.5'}`} />
            </button>
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner flex justify-between items-center gap-4 flex-wrap">
            <span className="text-[11px] text-zinc-400 font-black uppercase tracking-widest">Fetch Town / Trail Pictures</span>
            <button 
              type="button"
              onClick={() => setEnableImageSearch(!enableImageSearch)} 
              className={`relative inline-flex min-h-[32px] min-w-[56px] items-center rounded-full transition-colors cursor-pointer shadow-inner border border-white/10 ${
                enableImageSearch ? t.bg : 'bg-zinc-800'
              }`}
            >
              <span className={`inline-block h-6 w-6 transform rounded-full bg-white transition shadow-sm ${enableImageSearch ? 'translate-x-7' : 'translate-x-1.5'}`} />
            </button>
          </div>

          <div className="bg-black/40 p-5 rounded-2xl border border-white/10 shadow-inner flex justify-between items-center gap-4 flex-wrap">
            <span className="text-[11px] text-zinc-400 font-black uppercase tracking-widest">Active Log Maintenance</span>
            <div className="flex gap-3">
              <button 
                type="button" 
                onClick={exportChatHistory} 
                className="px-4 min-h-[44px] bg-white/5 hover:bg-white/10 border border-white/10 text-white rounded-xl text-[10px] font-black uppercase tracking-widest cursor-pointer transition-all active:scale-95 shadow-sm"
              >
                Export Log
              </button>
              <button 
                type="button" 
                onClick={clearMemory} 
                className="px-4 min-h-[44px] bg-rose-950/40 hover:bg-rose-900/60 border border-rose-900/50 text-rose-400 rounded-xl text-[10px] font-black uppercase tracking-widest cursor-pointer transition-all active:scale-95 shadow-sm"
              >
                Clear Log
              </button>
            </div>
          </div>
        </div>

        <div className="mt-5 pt-4 border-t border-white/10 flex justify-end">
          <button 
            type="button"
            onClick={() => setShowSettings(false)}
            className="bg-white/10 hover:bg-white/20 text-white px-8 min-h-[48px] rounded-xl font-black uppercase tracking-widest text-xs border border-white/10 cursor-pointer transition-all active:scale-95 shadow-lg"
          >
            Close Window
          </button>
        </div>

      </motion.div>
    </div>
  );
}