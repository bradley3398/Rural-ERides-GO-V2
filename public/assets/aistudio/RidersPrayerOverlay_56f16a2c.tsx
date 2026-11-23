"use client";

import React, { useState, useEffect } from "react";
import { ShieldCheck, ArrowRight, Wind, Loader2, BookOpen, ChevronLeft, Volume2, Bookmark, RefreshCw, Check, AlertTriangle } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { TextToSpeech } from '@capacitor-community/text-to-speech';

// Safely checks localStorage, Vite env variables, and Next.js env variables for your key
const getGeminiApiKey = () => {
  try { 
    return localStorage.getItem("rural_copilot_api_key") || 
           import.meta.env.VITE_GEMINI_API_KEY || 
           import.meta.env.NEXT_PUBLIC_GEMINI_API_KEY || 
           (typeof process !== 'undefined' ? process.env.NEXT_PUBLIC_GEMINI_API_KEY : "") || 
           ""; 
  } catch(e) { return ""; }
};

const getGeminiModel = () => {
  try {
    return import.meta.env.NEXT_PUBLIC_GEMINI_MODEL || 
           import.meta.env.VITE_GEMINI_MODEL || 
           (typeof process !== 'undefined' ? process.env.NEXT_PUBLIC_GEMINI_MODEL : "") || 
           "gemini-3.1-flash-lite";
  } catch(e) {
    return "gemini-3.1-flash-lite";
  }
};

const SINNERS_PRAYER = `Dear Lord Jesus, I come to You as a sinner. I believe that You died on the cross for my sins and rose from the dead. I ask You to forgive me and wash me in Your precious blood. I turn from my sins and invite You into my heart and life to be my Lord and Savior. I give my life to You. In Jesus' name, Amen.`;

interface RidersPrayerOverlayProps {
  isVisible: boolean;
  onComplete: () => void;
  operatorCallsign?: string;
  themeColors?: any;
}

export default function RidersPrayerOverlay({
  isVisible,
  onComplete,
  operatorCallsign = "Pilot",
  themeColors
}: RidersPrayerOverlayProps) {
  const [step, setStep] = useState(0);
  const [isGenerating, setIsGenerating] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [viewMode, setViewMode] = useState<"daily" | "sinners" | "journal">("daily");
  
  const [aiVerse, setAiVerse] = useState("");
  const [aiReference, setAiReference] = useState("");
  const [aiPrayer, setAiPrayer] = useState("");
  const [safetyTip, setSafetyTip] = useState("");
  
  const [savedPrayers, setSavedPrayers] = useState<Array<{verse: string, reference: string, prayer: string}>>([]);
  const [isBookmarked, setIsBookmarked] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem("rural_saved_prayers");
      if (stored) setSavedPrayers(JSON.parse(stored));
    } catch(e) {}
  }, []);

  const fetchAiBlessing = async (isManualRefresh = false) => {
    setIsGenerating(true);
    setHasError(false);
    setErrorMessage("");
    setStep(0);
    setIsBookmarked(false);

    const apiKey = getGeminiApiKey();
    const modelName = getGeminiModel();

    if (!apiKey) {
      setIsGenerating(false);
      setHasError(true);
      setErrorMessage("Missing API Key. Ensure VITE_GEMINI_API_KEY is configured in your .env file.");
      return;
    }

    try {
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${apiKey}`;
      
      const themes = ["strength", "guidance", "protection", "endurance", "peace", "courage", "wisdom", "the journey", "light", "faith in the storm"];
      const randomTheme = themes[Math.floor(Math.random() * themes.length)];
      const randomSeed = Math.floor(Math.random() * 999999) + (isManualRefresh ? Date.now() % 1000 : 0);

      const systemPrompt = `
        You are the UNIVERSAL PILOT onboard AI. (Session Seed: ${randomSeed})
        Generate a short, inspiring, and completely unique daily protection prayer for an electric vehicle (PEV/E-bike) rider named "${operatorCallsign}".
        Focus today's prayer specifically around the theme of: ${randomTheme}.
        
        Also, select a completely RANDOM, non-copyrighted public domain Bible verse (like KJV, WEB, or ASV) related to ${randomTheme}, safety, travel, or guidance. Avoid Psalm 91:11.
        Also provide 1 brief, practical PEV/trail safety tip.
        
        CRITICAL: You MUST respond ONLY with a valid, parseable JSON object in this exact format. Do not include markdown formatting or backticks:
        {
          "verse": "The scripture text here...",
          "reference": "Book Chapter:Verse (Translation)",
          "prayer": "A short, 2-3 sentence prayer for the rider's safety on the trails today.",
          "safetyTip": "A brief pre-flight mechanical or trail safety recommendation."
        }
      `;

      const payload = {
        contents: [{ role: "user", parts: [{ text: systemPrompt }] }],
        generationConfig: { temperature: 0.95, responseMimeType: "application/json" }
      };

      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const errData = await response.json();
        throw new Error(errData?.error?.message || `HTTP Status ${response.status}`);
      }
      
      const data = await response.json();
      const textResponse = data.candidates?.[0]?.content?.parts?.[0]?.text;
      
      if (textResponse) {
        const cleanText = textResponse.replace(/```json/gi, '').replace(/```/g, '').trim();
        const parsed = JSON.parse(cleanText);
        
        if (parsed.verse && parsed.reference && parsed.prayer) {
          setAiVerse(`"${parsed.verse}"`);
          setAiReference(parsed.reference);
          setAiPrayer(parsed.prayer);
          if (parsed.safetyTip) setSafetyTip(parsed.safetyTip);
          
          setIsGenerating(false);
          setTimeout(() => setStep(1), 400);
          setTimeout(() => setStep(2), 2200);
          setTimeout(() => setStep(3), 4000);
        } else {
          throw new Error("Invalid response format received from Gemini.");
        }
      } else {
        throw new Error("Empty candidate payload returned from AI.");
      }
    } catch (error: any) {
      console.error("AI Prayer Generation Error:", error);
      setIsGenerating(false);
      setHasError(true);
      setErrorMessage(error.message || "Failed to communicate with Gemini satellite uplink.");
    }
  };

  useEffect(() => {
    if (!isVisible) {
      setStep(0);
      setIsGenerating(true);
      setHasError(false);
      setViewMode("daily");
      if (isSpeaking) {
        TextToSpeech.stop().catch(() => {});
        setIsSpeaking(false);
      }
      return;
    }
    fetchAiBlessing(false);
  }, [isVisible, operatorCallsign]);

  const handleReadAloud = async () => {
    try {
      if (isSpeaking) {
        await TextToSpeech.stop();
        setIsSpeaking(false);
        return;
      }

      setIsSpeaking(true);
      const speechText = viewMode === 'daily' 
        ? `${aiReference}. ${aiVerse}. Prayer: ${aiPrayer}. Safety Tip: ${safetyTip}` 
        : SINNERS_PRAYER;

      const cleanSpeech = speechText.replace(/[*#_`]/g, '');
      await TextToSpeech.speak({
        text: cleanSpeech,
        lang: 'en-US',
        rate: 1.0,
        pitch: 1.0,
        volume: 1.0,
        category: 'ambient'
      });
      setIsSpeaking(false);
    } catch (e) {
      setIsSpeaking(false);
    }
  };

  const handleSaveToJournal = () => {
    try {
      const entry = { verse: aiVerse, reference: aiReference, prayer: aiPrayer };
      const updated = [entry, ...savedPrayers.filter(p => p.reference !== aiReference)];
      setSavedPrayers(updated);
      localStorage.setItem("rural_saved_prayers", JSON.stringify(updated));
      setIsBookmarked(true);
    } catch(e) {}
  };

  if (!isVisible) return null;

  const t = themeColors || { text: 'text-emerald-400', bg: 'bg-emerald-500', border: 'border-emerald-500' };

  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0, scale: 1.05 }}
          transition={{ duration: 0.6, ease: "easeInOut" }}
          className="fixed inset-0 z-[999999] bg-black/95 backdrop-blur-3xl flex flex-col items-center justify-center p-6 font-sans text-center overflow-y-auto custom-scrollbar"
        >
          {/* Ambient Glow */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[350px] h-[350px] bg-white/5 rounded-full blur-[120px] pointer-events-none"></div>

          <div className="max-w-md w-full space-y-8 relative z-10 py-8">
            
            {/* Header Icon & Loading State */}
            <motion.div 
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ duration: 0.8, ease: "easeOut" }}
              className="flex flex-col items-center gap-3"
            >
              <div className={`p-4 rounded-full bg-white/5 border border-white/10 shadow-[0_0_30px_rgba(255,255,255,0.05)] relative`}>
                {hasError ? <AlertTriangle className="w-10 h-10 text-rose-500 animate-pulse" /> : <ShieldCheck className={`w-10 h-10 ${t.text}`} />}
                {isGenerating && (
                  <div className="absolute -inset-2 border border-white/10 rounded-full animate-spin [animation-duration:3s]" />
                )}
              </div>
              
              {isGenerating ? (
                <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-zinc-500 flex items-center gap-2 animate-pulse">
                  <Loader2 className="w-3 h-3 animate-spin" /> Uplinking Daily Blessings via Gemini...
                </div>
              ) : hasError ? (
                <div className="text-[9px] font-mono uppercase tracking-[0.2em] text-rose-500">
                  Uplink Fault Detected
                </div>
              ) : (
                <div className="flex items-center gap-3">
                  <span className="text-[9px] font-mono uppercase tracking-[0.3em] text-zinc-500">
                    {viewMode === "daily" ? "Operator Protection Protocol" : viewMode === "sinners" ? "The Sinner's Prayer" : "Saved Prayer Journal"}
                  </span>
                  {viewMode === "daily" && !isGenerating && !hasError && (
                    <div className="flex items-center gap-1.5">
                      <button 
                        onClick={() => fetchAiBlessing(true)} 
                        title="Reroll AI Blessing" 
                        className="p-1.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-lg text-zinc-300 hover:text-white transition-all cursor-pointer active:scale-95"
                      >
                        <RefreshCw className="w-3.5 h-3.5" />
                      </button>
                      <button 
                        onClick={handleSaveToJournal} 
                        title="Bookmark to Journal" 
                        className={`p-1.5 border rounded-lg transition-all cursor-pointer active:scale-95 ${isBookmarked ? 'bg-emerald-500/20 border-emerald-500/50 text-emerald-400' : 'bg-white/5 border-white/10 text-zinc-300 hover:text-white'}`}
                      >
                        {isBookmarked ? <Check className="w-3.5 h-3.5" /> : <Bookmark className="w-3.5 h-3.5" />}
                      </button>
                      <button 
                        onClick={handleReadAloud} 
                        title="Read Aloud" 
                        className={`p-1.5 border rounded-lg transition-all cursor-pointer active:scale-95 ${isSpeaking ? 'bg-amber-500/20 border-amber-500/50 text-amber-400 animate-pulse' : 'bg-white/5 border-white/10 text-zinc-300 hover:text-white'}`}
                      >
                        <Volume2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}
                </div>
              )}
            </motion.div>

            {/* Dynamic Content View / Error Screen */}
            {hasError ? (
              <div className="p-5 bg-rose-950/40 border border-rose-500/40 rounded-2xl space-y-4 text-center font-mono">
                <p className="text-xs text-rose-300 font-bold leading-relaxed">{errorMessage}</p>
                <button
                  onClick={() => fetchAiBlessing(true)}
                  className="px-6 py-3 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-[10px] font-black uppercase tracking-widest cursor-pointer shadow-lg active:scale-95 transition-all"
                >
                  Retry Satellite Uplink
                </button>
              </div>
            ) : (
              <AnimatePresence mode="wait">
                {viewMode === "daily" ? (
                  <motion.div
                    key="daily-view"
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 20 }}
                    transition={{ duration: 0.4 }}
                    className="space-y-6"
                  >
                    <AnimatePresence>
                      {step >= 1 && !isGenerating && (
                        <motion.div
                          initial={{ y: 20, opacity: 0 }}
                          animate={{ y: 0, opacity: 1 }}
                          transition={{ duration: 0.8 }}
                          className="space-y-3"
                        >
                          <blockquote className="text-xl sm:text-2xl font-serif italic text-zinc-200 leading-relaxed px-2 drop-shadow-md">
                            {aiVerse}
                          </blockquote>
                          <p className={`text-xs font-black uppercase tracking-widest ${t.text}`}>
                            — {aiReference}
                          </p>
                        </motion.div>
                      )}
                    </AnimatePresence>

                    <AnimatePresence>
                      {step >= 2 && !isGenerating && (
                        <motion.div
                          initial={{ y: 20, opacity: 0 }}
                          animate={{ y: 0, opacity: 1 }}
                          transition={{ duration: 0.8 }}
                          className="pt-4 border-t border-white/10 space-y-3"
                        >
                          <p className="text-sm sm:text-base font-medium text-zinc-300 leading-relaxed px-2">
                            {aiPrayer}
                          </p>
                          
                          {safetyTip && (
                            <div className="bg-white/5 border border-white/10 rounded-xl p-3 text-left font-mono text-[9px] text-zinc-400">
                              <span className={`${t.text} font-black uppercase tracking-wider block mb-0.5`}>🛡️ Pre-Flight Safety Tip:</span>
                              {safetyTip}
                            </div>
                          )}
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </motion.div>
                ) : viewMode === "sinners" ? (
                  <motion.div
                    key="sinners-view"
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -20 }}
                    transition={{ duration: 0.4 }}
                    className="space-y-6"
                  >
                    <motion.div className="pt-2 border-t border-white/10 relative">
                      <BookOpen className="w-8 h-8 text-rose-500 mx-auto absolute -top-4 left-1/2 -translate-x-1/2 bg-black/95 px-2" />
                      <p className="text-base sm:text-lg font-serif italic text-zinc-200 leading-loose px-4 pt-6">
                        {SINNERS_PRAYER}
                      </p>
                    </motion.div>
                  </motion.div>
                ) : (
                  <motion.div
                    key="journal-view"
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    transition={{ duration: 0.3 }}
                    className="space-y-3 max-h-[220px] overflow-y-auto custom-scrollbar pr-1 text-left"
                  >
                    <h4 className="text-[10px] font-mono font-black text-zinc-400 uppercase tracking-widest px-1">Saved Journal Entries ({savedPrayers.length})</h4>
                    {savedPrayers.length === 0 ? (
                      <p className="text-[10px] text-zinc-500 font-mono text-center py-6">No saved prayers yet. Click the bookmark icon on any daily blessing to store it here.</p>
                    ) : (
                      savedPrayers.map((item, idx) => (
                        <div key={idx} onClick={() => { setAiVerse(item.verse); setAiReference(item.reference); setAiPrayer(item.prayer); setViewMode("daily"); }} className="p-3 bg-white/5 border border-white/10 hover:border-white/30 rounded-xl cursor-pointer transition-all space-y-1">
                          <span className={`text-[9px] font-black uppercase tracking-wider ${t.text}`}>{item.reference}</span>
                          <p className="text-xs text-zinc-300 font-serif italic truncate">{item.verse}</p>
                        </div>
                      ))
                    )}
                  </motion.div>
                )}
              </AnimatePresence>
            )}

            {/* Action Buttons */}
            {(!hasError || viewMode !== "daily") && (
              <AnimatePresence>
                {(step >= 3 && !isGenerating) || viewMode !== "daily" ? (
                  <motion.div
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    transition={{ duration: 0.5 }}
                    className="pt-4 flex flex-col gap-2.5"
                  >
                    <button
                      onClick={() => {
                        if (isSpeaking) TextToSpeech.stop().catch(()=>{});
                        onComplete();
                      }}
                      className={`w-full max-w-[280px] min-h-[52px] px-8 rounded-2xl ${t.bg} text-black font-black uppercase tracking-widest text-[11px] shadow-[0_0_20px_rgba(255,255,255,0.1)] hover:scale-105 active:scale-95 transition-all duration-300 flex items-center justify-center gap-3 mx-auto cursor-pointer`}
                    >
                      <Wind className="w-4 h-4"/> Mount Up &amp; Ride <ArrowRight className="w-4 h-4"/>
                    </button>

                    <div className="flex gap-2 justify-center max-w-[280px] mx-auto w-full">
                      <button
                        onClick={() => setViewMode(viewMode === "daily" ? "sinners" : "daily")}
                        className="flex-1 min-h-[42px] px-3 rounded-xl bg-transparent border border-white/10 text-zinc-400 hover:text-white hover:bg-white/5 font-bold uppercase tracking-widest text-[9px] active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer truncate"
                      >
                        {viewMode === "daily" ? <><BookOpen className="w-3 h-3"/> Sinner's Prayer</> : <><ChevronLeft className="w-3 h-3"/> Daily Blessing</>}
                      </button>
                      
                      <button
                        onClick={() => setViewMode(viewMode === "journal" ? "daily" : "journal")}
                        className="min-h-[42px] px-3 rounded-xl bg-transparent border border-white/10 text-zinc-400 hover:text-white hover:bg-white/5 font-bold uppercase tracking-widest text-[9px] active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                      >
                        <Bookmark className="w-3 h-3" /> Journal ({savedPrayers.length})
                      </button>
                    </div>
                  </motion.div>
                ) : null}
              </AnimatePresence>
            )}

          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}