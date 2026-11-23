"use client";

import React, { useState, useEffect, useMemo, useRef } from "react";
import { 
  Newspaper, RefreshCw, Loader2, Bookmark, BookmarkCheck, 
  Share2, Volume2, VolumeX, ExternalLink, Search, Sparkles, 
  Filter, Clock, Eye, Trash2, BookOpen, Layers, 
  CheckCircle2, ChevronRight, X, AlertCircle, Radio, Tag,
  Play, Pause, SkipForward, Download, TrendingUp, ThumbsUp, Check, BrainCircuit
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { TextToSpeech } from "@capacitor-community/text-to-speech";
import { getNewsApiKey, getGeminiApiKey } from "../../services/CoPilotService";

export interface PEVNewsFeedProps {
  localeCode?: string;
  ui: any;
  triggerHaptic?: () => void;
  tx?: any;
  globalVolume?: number;
  theme?: string;
}

export interface NewsArticle {
  title: string;
  description: string;
  url: string;
  urlToImage?: string;
  publishedAt: string;
  source: { name: string };
  content?: string;
  category?: string;
}

const NEWS_CATEGORIES = [
  "Micro-Mobility", 
  "E-Bikes", 
  "Electric Scooters", 
  "Battery Technology", 
  "PEV Legislation", 
  "Tech & Gadgets",
  "Saved Bookmarks"
];

const TRENDING_TAGS = [
  "Solid-State", "FOC Controllers", "BMS Safety", "Corridor Laws", "Fast Charging", "Hub Motors"
];

const FALLBACK_DISPATCHES: NewsArticle[] = [
  {
    title: "Solid-State Battery Breakthroughs Promise 40% Weight Reduction for Personal Electric Vehicles",
    description: "Next-generation solid-state chemistry demonstrates higher thermal stability and rapid charging profiles suited for lightweight micro-mobility platforms.",
    url: "https://electrek.co",
    publishedAt: new Date().toISOString(),
    source: { name: "MicroMobility Tech" },
    category: "Battery Technology"
  },
  {
    title: "Global Micro-Mobility Infrastructure Expands: New Protected Corridor Safety Standards",
    description: "Municipal transit planners accelerate adoption of dedicated multi-use lanes designed specifically for 20-30 mph personal electric vehicle commuting.",
    url: "https://zagdaily.com",
    publishedAt: new Date(Date.now() - 3600000 * 4).toISOString(),
    source: { name: "Transit Velocity" },
    category: "PEV Legislation"
  },
  {
    title: "High-Efficiency FOC Motor Controllers Revolutionize Hub & Mid-Drive Thermal Management",
    description: "Field-Oriented Control sine-wave controllers reduce stator coil heat generation while improving regenerative braking energy recovery on steep inclines.",
    url: "https://electricbikereview.com",
    publishedAt: new Date(Date.now() - 3600000 * 8).toISOString(),
    source: { name: "PEV Engineering" },
    category: "Tech & Gadgets"
  }
];

export default function PEVNewsFeed({ 
  localeCode = "en", 
  ui, 
  triggerHaptic = () => {}, 
  tx,
  globalVolume = 100,
  theme = "lime"
}: PEVNewsFeedProps) {
  const [localVolume, setLocalVolume] = useState<number>(globalVolume);
  const [localTheme, setLocalTheme] = useState<string>(theme);

  // --- MASTER SETTINGS SYNC ENGINE ---
  useEffect(() => {
    const handleSettingsSync = () => {
      const masterVol = Number(localStorage.getItem("rural_global_vol") ?? globalVolume);
      setLocalVolume(masterVol);

      const savedTheme = localStorage.getItem("rural_theme") || localStorage.getItem("rt_theme") || theme;
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
  }, [globalVolume, theme]);

  const [newsCategory, setNewsCategory] = useState("Micro-Mobility");
  const [selectedTag, setSelectedTag] = useState<string | null>(null);
  const [newsSearchFilter, setNewsSearchFilter] = useState("");
  const [liveNews, setLiveNews] = useState<NewsArticle[]>([]);
  const [isLoadingNews, setIsLoadingNews] = useState(true);
  const [bookmarkedUrls, setBookmarkedUrls] = useState<string[]>([]);
  const [selectedArticle, setSelectedArticle] = useState<NewsArticle | null>(null);
  
  const [speechActive, setSpeechActive] = useState(false);
  const [isRadioMode, setIsRadioMode] = useState(false);
  const [currentRadioIndex, setCurrentRadioIndex] = useState(0);

  const [aiSummaryMap, setAiSummaryMap] = useState<Record<string, string>>({});
  const [isSummarizing, setIsSummarizing] = useState<string | null>(null);
  const [feedError, setFeedError] = useState<string | null>(null);

  const activeUtteranceRef = useRef<SpeechSynthesisUtterance | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const availableVoicesRef = useRef<SpeechSynthesisVoice[]>([]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const loadVoices = () => {
      if ("speechSynthesis" in window) {
        availableVoicesRef.current = window.speechSynthesis.getVoices();
      }
    };
    loadVoices();
    if ("speechSynthesis" in window && window.speechSynthesis.onvoiceschanged !== undefined) {
      window.speechSynthesis.onvoiceschanged = loadVoices;
    }
  }, []);

  const playAudioTone = () => {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      if (!audioCtxRef.current) audioCtxRef.current = new AudioCtx();
      const ctx = audioCtxRef.current;
      if (ctx.state === "suspended") ctx.resume();

      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = "sine";
      osc.frequency.setValueAtTime(750, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(1100, ctx.currentTime + 0.08);

      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.1);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.1);
    } catch (e) {}
  };

  const speakArticle = async (text: string, onEndCallback?: () => void, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    triggerHaptic();

    if (speechActive && !onEndCallback) {
      stopSpeech();
      return;
    }

    const cleanText = text.replace(/[*_#`~[\]]/g, "").trim();
    if (!cleanText) return;

    playAudioTone();
    setSpeechActive(true);

    const activeVol = Number(localStorage.getItem("rural_global_vol") ?? localVolume);

    try {
      await TextToSpeech.speak({
        text: cleanText,
        lang: "en-US",
        rate: 1.05,
        pitch: 1.0,
        volume: activeVol / 100,
        category: "ambient"
      });
      setSpeechActive(false);
      if (onEndCallback) onEndCallback();
      return;
    } catch (nativeErr) {}

    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      try {
        const synth = window.speechSynthesis;
        synth.cancel();
        if (synth.paused) synth.resume();

        const utterance = new SpeechSynthesisUtterance(cleanText);
        utterance.lang = "en-US";
        utterance.rate = 1.05;
        utterance.pitch = 1.0;
        utterance.volume = activeVol / 100;

        const voices = availableVoicesRef.current.length > 0 
          ? availableVoicesRef.current 
          : synth.getVoices();

        const matchVoice = voices.find(v => v.lang.startsWith("en") && (v.name.includes("Google") || v.name.includes("Natural") || v.name.includes("Samantha"))) 
          || voices.find(v => v.lang.startsWith("en"));
        
        if (matchVoice) utterance.voice = matchVoice;

        utterance.onstart = () => setSpeechActive(true);
        utterance.onend = () => {
          setSpeechActive(false);
          activeUtteranceRef.current = null;
          if (onEndCallback) onEndCallback();
        };
        utterance.onerror = () => {
          setSpeechActive(false);
          activeUtteranceRef.current = null;
        };

        activeUtteranceRef.current = utterance;
        synth.speak(utterance);
      } catch (err) {
        setSpeechActive(false);
      }
    } else {
      setSpeechActive(false);
    }
  };

  const stopSpeech = () => {
    try {
      TextToSpeech.stop().catch(() => {});
      if (typeof window !== "undefined" && "speechSynthesis" in window) {
        window.speechSynthesis.cancel();
      }
    } catch (e) {}
    setSpeechActive(false);
    setIsRadioMode(false);
    activeUtteranceRef.current = null;
  };

  const playContinuousRadio = (startIndex = 0) => {
    if (!filteredNews.length) return;
    setIsRadioMode(true);
    setCurrentRadioIndex(startIndex);

    const article = filteredNews[startIndex];
    if (!article) {
      stopSpeech();
      return;
    }

    const broadcastScript = `Dispatch ${startIndex + 1} of ${filteredNews.length}. From ${article.source.name}. ${article.title}. ${article.description || ""}`;
    speakArticle(broadcastScript, () => {
      if (startIndex + 1 < filteredNews.length) {
        playContinuousRadio(startIndex + 1);
      } else {
        setIsRadioMode(false);
        setSpeechActive(false);
      }
    });
  };

  const generateAiDigest = async (article: NewsArticle, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    triggerHaptic();
    setIsSummarizing(article.url);

    try {
      const apiKey = getGeminiApiKey() || (import.meta as any).env?.VITE_GEMINI_API_KEY;
      if (!apiKey) {
        const sentences = (article.description || article.title).split(". ");
        const offlineSummary = `• Core Focus: ${article.title}\n• Impact: ${sentences[0] || "Advanced hardware expansion."}\n• Key Vector: Suited for daily micro-mobility commutes.`;
        setAiSummaryMap(prev => ({ ...prev, [article.url]: offlineSummary }));
        return;
      }

      const payload = {
        contents: [{
          parts: [{
            text: `Provide a sharp, technical 3-bullet-point executive summary for PEV riders for this article:
            Title: ${article.title}
            Description: ${article.description}
            Task: Focus on battery impact, rider safety, or performance engineering.`
          }]
        }],
        generationConfig: { maxOutputTokens: 150, temperature: 0.2 }
      };

      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      const data = await res.json();
      if (data?.candidates?.[0]?.content?.parts?.[0]?.text) {
        setAiSummaryMap(prev => ({ ...prev, [article.url]: data.candidates[0].content.parts[0].text.replace(/\*/g, '') }));
      }
    } catch (err: any) {
      setAiSummaryMap(prev => ({ ...prev, [article.url]: `Summary unavailable: ${err.message}` }));
    } finally {
      setIsSummarizing(null);
    }
  };

  const getReadingTime = (text?: string) => {
    if (!text) return "1 min";
    const words = text.split(/\s+/).length;
    const mins = Math.max(1, Math.round(words / 40));
    return `${mins} min read`;
  };

  useEffect(() => {
    try {
      const savedBookmarks = localStorage.getItem("rt_news_bookmarks");
      if (savedBookmarks) setBookmarkedUrls(JSON.parse(savedBookmarks));

      const cachedFeed = localStorage.getItem(`rt_news_cache_${newsCategory}`);
      if (cachedFeed) {
        setLiveNews(JSON.parse(cachedFeed));
        setIsLoadingNews(false);
      }
    } catch (e) {}
  }, [newsCategory]);

  const toggleBookmark = (article: NewsArticle, e: React.MouseEvent) => {
    e.stopPropagation();
    triggerHaptic();
    const isSaved = bookmarkedUrls.includes(article.url);
    const updated = isSaved 
      ? bookmarkedUrls.filter(u => u !== article.url) 
      : [...bookmarkedUrls, article.url];

    setBookmarkedUrls(updated);
    localStorage.setItem("rt_news_bookmarks", JSON.stringify(updated));

    try {
      const vault = JSON.parse(localStorage.getItem("rt_news_vault") || "[]");
      const updatedVault = isSaved 
        ? vault.filter((a: NewsArticle) => a.url !== article.url)
        : [article, ...vault.filter((a: NewsArticle) => a.url !== article.url)];
      localStorage.setItem("rt_news_vault", JSON.stringify(updatedVault));
    } catch (err) {}
  };

  const fetchPEVNews = async () => {
    if (newsCategory === "Saved Bookmarks") {
      setIsLoadingNews(true);
      try {
        const vault = JSON.parse(localStorage.getItem("rt_news_vault") || "[]");
        setLiveNews(vault);
      } catch (err) {
        setLiveNews([]);
      } finally {
        setIsLoadingNews(false);
      }
      return;
    }

    setIsLoadingNews(true);
    setFeedError(null);

    try {
      let q = '"electric scooter" OR ebike OR "e-bike" OR "micro-mobility"';
      if (newsCategory === "E-Bikes") q = 'ebike OR "e-bike" OR "electric bicycle"';
      if (newsCategory === "Electric Scooters") q = '"electric scooter" OR "e-scooter"';
      if (newsCategory === "Battery Technology") q = '"solid state battery" OR "lithium ion battery" OR "LFP battery"';
      if (newsCategory === "PEV Legislation") q = '"electric scooter" OR ebike laws legislation regulation speed';
      if (newsCategory === "Tech & Gadgets") q = 'micro-mobility technology OR "smart helmet" OR "electric unicycle"';

      const newsKey = getNewsApiKey() || "";
      if (!newsKey) throw new Error("API Key Unconfigured");

      const url = `https://newsapi.org/v2/everything?q=${encodeURIComponent(q)}&sortBy=publishedAt&language=en&pageSize=25&apiKey=${newsKey}`;
      const res = await fetch(url);
      const data = await res.json();

      if (data.articles && data.articles.length > 0) {
        const valid = data.articles
          .filter((article: any) => article.title && article.title !== "[Removed]" && !article.title.includes("Removed"))
          .map((a: any) => ({ ...a, category: newsCategory }));
        setLiveNews(valid);
        localStorage.setItem(`rt_news_cache_${newsCategory}`, JSON.stringify(valid));
      } else {
        throw new Error(data.message || "Empty Dispatch Stream");
      }
    } catch (err: any) {
      setFeedError(err.message || "Offline Stream Active");
      setLiveNews(FALLBACK_DISPATCHES.filter(d => newsCategory === "Micro-Mobility" || d.category === newsCategory || FALLBACK_DISPATCHES));
    } finally {
      setIsLoadingNews(false);
    }
  };

  useEffect(() => {
    fetchPEVNews();
  }, [newsCategory]);

  const filteredNews = useMemo(() => {
    return liveNews.filter(n => {
      const matchSearch = (n.title || "").toLowerCase().includes(newsSearchFilter.toLowerCase()) ||
                          (n.description || "").toLowerCase().includes(newsSearchFilter.toLowerCase()) ||
                          (n.source?.name || "").toLowerCase().includes(newsSearchFilter.toLowerCase());
      
      const matchTag = !selectedTag || (n.title + " " + (n.description || "")).toLowerCase().includes(selectedTag.toLowerCase().replace("-", " "));
      return matchSearch && matchTag;
    });
  }, [liveNews, newsSearchFilter, selectedTag]);

  const industrySentiment = useMemo(() => {
    let positive = 0;
    let regulation = 0;
    filteredNews.forEach(n => {
      const text = (n.title + " " + (n.description || "")).toLowerCase();
      if (text.includes("breakthrough") || text.includes("expand") || text.includes("revolution") || text.includes("efficient") || text.includes("record")) positive++;
      if (text.includes("ban") || text.includes("law") || text.includes("limit") || text.includes("regulate") || text.includes("safe")) regulation++;
    });
    return {
      growthRate: Math.min(100, Math.round((positive / Math.max(1, filteredNews.length)) * 100)),
      complianceRate: Math.min(100, Math.round((regulation / Math.max(1, filteredNews.length)) * 100))
    };
  }, [filteredNews]);

  const exportBriefing = () => {
    triggerHaptic();
    const content = `# Rural ERides Micro-Mobility Morning Intelligence Briefing\nDate: ${new Date().toLocaleDateString()}\nCategory: ${newsCategory}\n\n` + 
      filteredNews.slice(0, 10).map((a, i) => `### ${i + 1}. ${a.title}\nSource: ${a.source.name} | Published: ${a.publishedAt}\n${a.description || "N/A"}\nURL: ${a.url}\n\n`).join("");
    
    const blob = new Blob([content], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `PEV_Intelligence_Brief_${Date.now()}.md`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className={`${ui?.bgPanel || 'bg-[#0d0e15]'} p-4 sm:p-6 rounded-3xl shadow-2xl relative overflow-hidden flex flex-col border transition-colors ${ui?.brd || 'border-zinc-800'} space-y-4`}>
      
      <div className={`flex flex-col sm:flex-row justify-between items-start sm:items-center border-b ${ui?.brd || 'border-zinc-800'} pb-3.5 gap-3 w-full`}>
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-2xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 shadow-md">
            <Newspaper className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <h3 className={`${ui?.txtMain || 'text-white'} font-black uppercase tracking-widest text-xs sm:text-sm flex items-center gap-2`}>
              Global Micro-Mobility Intelligence Matrix
            </h3>
            <p className={`text-[8.5px] sm:text-[9px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase mt-0.5`}>
              Live Dispatches • Continuous Radio Deck • AI Executive Summaries
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <button
            onClick={() => isRadioMode ? stopSpeech() : playContinuousRadio(0)}
            className={`min-h-[44px] px-3.5 rounded-xl border text-[10px] font-mono font-black uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer active:scale-95 touch-manipulation ${isRadioMode ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-lg` : 'bg-zinc-900 border-zinc-700 text-cyan-400'}`}
          >
            {isRadioMode ? <Pause className="w-3.5 h-3.5 fill-current" /> : <Play className="w-3.5 h-3.5 fill-current" />}
            <span>{isRadioMode ? `Radio: #${currentRadioIndex + 1}` : "Radio Deck"}</span>
          </button>

          <button
            onClick={exportBriefing}
            className={`min-h-[44px] px-3 rounded-xl bg-zinc-900 border border-zinc-700 text-zinc-300 hover:text-white text-[10px] font-mono font-black uppercase flex items-center gap-1.5 cursor-pointer active:scale-95 touch-manipulation`}
            title="Download Intelligence Briefing"
          >
            <Download className="w-3.5 h-3.5 text-emerald-400" />
          </button>

          <button 
            onClick={fetchPEVNews} 
            disabled={isLoadingNews}
            className={`min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl ${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} text-cyan-400 hover:text-white transition-colors cursor-pointer active:scale-95 touch-manipulation`} 
            title="Force Matrix Sync"
          >
            <RefreshCw className={`w-4 h-4 ${isLoadingNews ? 'animate-spin' : ''}`}/>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 font-mono text-[9px]">
        <div className="bg-black/40 border border-zinc-800 p-2.5 rounded-xl flex items-center justify-between">
          <span className="text-zinc-500 uppercase font-black">Innovation Velocity</span>
          <span className="text-cyan-400 font-black">{industrySentiment.growthRate}% Index</span>
        </div>
        <div className="bg-black/40 border border-zinc-800 p-2.5 rounded-xl flex items-center justify-between">
          <span className="text-zinc-500 uppercase font-black">Policy Activity</span>
          <span className="text-amber-400 font-black">{industrySentiment.complianceRate}% Metric</span>
        </div>
        <div className="bg-black/40 border border-zinc-800 p-2.5 rounded-xl flex items-center justify-between">
          <span className="text-zinc-500 uppercase font-black">Live Dispatches</span>
          <span className="text-white font-black">{filteredNews.length} Stories</span>
        </div>
        <div className="bg-black/40 border border-zinc-800 p-2.5 rounded-xl flex items-center justify-between">
          <span className="text-zinc-500 uppercase font-black">Saved Vault</span>
          <span className="text-emerald-400 font-black">{bookmarkedUrls.length} Articles</span>
        </div>
      </div>

      <div className="flex gap-1.5 overflow-x-auto custom-scrollbar pb-1 -mx-1 px-1 touch-pan-x">
        {NEWS_CATEGORIES.map((cat) => (
          <button
            key={cat}
            onClick={() => { triggerHaptic(); setNewsCategory(cat); setSelectedTag(null); }}
            className={`min-h-[44px] px-3.5 rounded-xl text-[9.5px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 shrink-0 cursor-pointer active:scale-95 touch-manipulation ${newsCategory === cat ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-lg` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} border ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
          >
            {cat === "Saved Bookmarks" && <Bookmark className="w-3.5 h-3.5" />}
            <span>{cat}</span>
          </button>
        ))}
      </div>

      <div className="flex items-center gap-1.5 overflow-x-auto custom-scrollbar pb-0.5">
        <span className="text-[8px] font-mono uppercase font-black text-zinc-500 flex items-center gap-1 shrink-0">
          <Tag className="w-3 h-3 text-cyan-400" /> Topic Tags:
        </span>
        {TRENDING_TAGS.map((tag) => (
          <button
            key={tag}
            onClick={() => { triggerHaptic(); setSelectedTag(selectedTag === tag ? null : tag); }}
            className={`min-h-[32px] px-2.5 rounded-lg text-[8.5px] font-mono font-bold uppercase transition-all shrink-0 cursor-pointer ${selectedTag === tag ? 'bg-cyan-500 text-black font-black' : 'bg-zinc-900 border border-zinc-800 text-zinc-400 hover:text-white'}`}
          >
            #{tag}
          </button>
        ))}
      </div>

      <div className="relative w-full">
        <input 
          value={newsSearchFilter} 
          onChange={(e) => setNewsSearchFilter(e.target.value)}
          placeholder="Search headlines, hardware models, or tech keywords..."
          className={`w-full min-h-[48px] ${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-2xl pl-10 pr-4 ${ui?.txtMain || 'text-white'} text-xs font-mono font-bold outline-none focus:border-cyan-500 transition-colors shadow-inner`}
        />
        <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
        {newsSearchFilter && (
          <button 
            onClick={() => setNewsSearchFilter("")}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-mono text-zinc-400 hover:text-white"
          >
            CLEAR
          </button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto custom-scrollbar space-y-3 max-h-[460px] pr-1">
        {isLoadingNews ? (
          <div className="flex flex-col justify-center items-center h-48 gap-3">
            <Loader2 className={`w-8 h-8 animate-spin text-cyan-400`} />
            <span className="text-[10px] font-mono text-zinc-400 uppercase tracking-widest">
              Scanning Satellite News Feeds...
            </span>
          </div>
        ) : filteredNews.length === 0 ? (
          <div className={`text-center py-12 ${ui?.bgCard || 'bg-black/60'} border border-dashed ${ui?.brd || 'border-zinc-800'} rounded-2xl space-y-2`}>
            <Newspaper className="w-8 h-8 text-zinc-500 mx-auto" />
            <div className={`${ui?.txtMuted || 'text-zinc-400'} font-mono text-xs uppercase font-bold tracking-widest`}>
              No Dispatches Matching Search Parameters
            </div>
          </div>
        ) : (
          filteredNews.map((news, idx) => {
            const isBookmarked = bookmarkedUrls.includes(news.url);
            const isCurrentlyPlaying = isRadioMode && currentRadioIndex === idx;

            return (
              <div 
                key={`${news.url}-${idx}`} 
                onClick={() => setSelectedArticle(news)}
                className={`group ${ui?.bgList || 'bg-[#121318]'} border ${isCurrentlyPlaying ? 'border-cyan-400 ring-2 ring-cyan-500/20 bg-cyan-950/20' : (ui?.brd || 'border-zinc-800')} p-4 rounded-2xl transition-all cursor-pointer shadow-inner hover:border-cyan-500/60 active:scale-[0.99] touch-manipulation space-y-2.5`}
              >
                <div className="flex justify-between items-center text-[8.5px] font-mono font-bold">
                  <div className="flex items-center gap-2 truncate pr-2">
                    <span className="text-cyan-400 uppercase tracking-wider truncate">
                      {news.source?.name || "Global Dispatch"}
                    </span>
                    <span className="text-zinc-500">•</span>
                    <span className="text-zinc-400 flex items-center gap-1">
                      <Clock className="w-3 h-3" /> {getReadingTime(news.description || news.title)}
                    </span>
                  </div>
                  <span className={`${ui?.txtMuted || 'text-zinc-500'} shrink-0`}>
                    {new Date(news.publishedAt).toLocaleDateString(localeCode, { month: 'short', day: 'numeric' })}
                  </span>
                </div>

                <div className="flex items-start justify-between gap-3">
                  <h4 className={`text-xs font-bold ${ui?.txtMain || 'text-white'} leading-relaxed group-hover:text-cyan-300 transition-colors line-clamp-2`}>
                    {news.title}
                  </h4>
                  {news.urlToImage && (
                    <img 
                      src={news.urlToImage} 
                      alt="" 
                      className={`w-14 h-14 object-cover rounded-xl border ${ui?.brd || 'border-zinc-800'} shrink-0 bg-black`} 
                      onError={(e) => (e.currentTarget.style.display = "none")}
                    />
                  )}
                </div>

                {aiSummaryMap[news.url] && (
                  <div className="p-3 rounded-xl bg-black/60 border border-cyan-900/60 font-mono text-[9px] text-cyan-200 space-y-1">
                    <span className="text-cyan-400 font-black uppercase flex items-center gap-1">
                      <BrainCircuit className="w-3 h-3" /> AI Key Takeaways
                    </span>
                    <p className="whitespace-pre-line leading-relaxed">{aiSummaryMap[news.url]}</p>
                  </div>
                )}

                {!aiSummaryMap[news.url] && news.description && (
                  <p className={`text-[10px] ${ui?.txtMuted || 'text-zinc-400'} font-mono line-clamp-2 leading-normal`}>
                    {news.description}
                  </p>
                )}

                <div className="flex items-center justify-between pt-2 border-t border-zinc-800/80">
                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={(e) => speakArticle(`${news.title}. ${news.description || ""}`, undefined, e)}
                      className={`min-h-[38px] px-3 rounded-xl text-[9px] font-mono uppercase font-bold flex items-center gap-1.5 transition-colors cursor-pointer active:scale-95 ${speechActive ? 'bg-cyan-600 text-black' : 'bg-zinc-900 hover:bg-zinc-800 text-zinc-300'}`}
                      title="Listen to Article"
                    >
                      {speechActive ? <VolumeX className="w-3.5 h-3.5 text-black" /> : <Volume2 className="w-3.5 h-3.5 text-cyan-400" />} 
                      {speechActive ? "Stop" : "Listen"}
                    </button>

                    <button
                      onClick={(e) => generateAiDigest(news, e)}
                      disabled={isSummarizing === news.url}
                      className="min-h-[38px] px-3 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-[9px] font-mono uppercase font-bold text-cyan-300 border border-cyan-950 flex items-center gap-1.5 transition-colors cursor-pointer active:scale-95"
                      title="Generate 3-Point AI Summary"
                    >
                      {isSummarizing === news.url ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Sparkles className="w-3.5 h-3.5 text-yellow-400" />}
                      AI Takeaways
                    </button>
                  </div>

                  <div className="flex items-center gap-2">
                    <button 
                      onClick={(e) => toggleBookmark(news, e)}
                      className={`min-h-[38px] min-w-[38px] flex items-center justify-center rounded-xl border transition-colors cursor-pointer active:scale-95 ${isBookmarked ? 'bg-amber-950/40 border-amber-800 text-amber-400' : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:text-white'}`}
                      title={isBookmarked ? "Remove Bookmark" : "Save Bookmark"}
                    >
                      {isBookmarked ? <BookmarkCheck className="w-4 h-4" /> : <Bookmark className="w-4 h-4" />}
                    </button>
                    <button 
                      onClick={(e) => { e.stopPropagation(); window.open(news.url, "_blank"); }}
                      className="min-h-[38px] min-w-[38px] flex items-center justify-center rounded-xl bg-cyan-950/40 border border-cyan-800 text-cyan-400 hover:bg-cyan-600 hover:text-black transition-colors cursor-pointer active:scale-95"
                      title="Open Source in New Tab"
                    >
                      <ExternalLink className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      <AnimatePresence>
        {selectedArticle && (
          <motion.div 
            initial={{ opacity: 0 }} 
            animate={{ opacity: 1 }} 
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4"
            onClick={() => setSelectedArticle(null)}
          >
            <motion.div 
              initial={{ scale: 0.95, y: 10 }} 
              animate={{ scale: 1, y: 0 }} 
              exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className={`${ui?.bgPanel || 'bg-[#0d0e15]'} border ${ui?.brd || 'border-zinc-800'} max-w-lg w-full rounded-3xl p-5 sm:p-6 space-y-4 shadow-2xl max-h-[85vh] flex flex-col justify-between`}
            >
              <div className="flex justify-between items-start gap-3 border-b border-zinc-800 pb-3">
                <div>
                  <span className="text-[9px] font-mono text-cyan-400 font-bold uppercase">
                    {selectedArticle.source?.name}
                  </span>
                  <span className="text-[8px] font-mono text-zinc-500 block">
                    {new Date(selectedArticle.publishedAt).toLocaleString()}
                  </span>
                </div>
                <button 
                  onClick={() => setSelectedArticle(null)}
                  className="min-h-[38px] min-w-[38px] flex items-center justify-center rounded-full bg-zinc-900 border border-zinc-800 text-zinc-400 hover:text-white cursor-pointer active:scale-95"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-3 overflow-y-auto custom-scrollbar pr-1 flex-1">
                {selectedArticle.urlToImage && (
                  <img 
                    src={selectedArticle.urlToImage} 
                    alt="" 
                    className="w-full h-44 object-cover rounded-2xl border border-zinc-800" 
                  />
                )}
                <h3 className="text-base font-bold text-white leading-snug">
                  {selectedArticle.title}
                </h3>
                <p className="text-xs font-mono text-zinc-300 leading-relaxed">
                  {selectedArticle.description || selectedArticle.content || "Full text available on official publication channel."}
                </p>
              </div>

              <div className="pt-3 border-t border-zinc-800 flex flex-wrap items-center gap-2">
                <button
                  onClick={() => speakArticle(`${selectedArticle.title}. ${selectedArticle.description || ""}`)}
                  className={`flex-1 min-h-[44px] rounded-xl border text-[10px] font-mono font-black uppercase flex items-center justify-center gap-2 transition-colors cursor-pointer active:scale-95 ${speechActive ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black` : 'bg-zinc-900 border-zinc-700 text-white'}`}
                >
                  <Volume2 className="w-4 h-4 text-cyan-400" /> {speechActive ? "Stop Audio" : "Read Out Loud"}
                </button>
                <button
                  onClick={() => window.open(selectedArticle.url, "_blank")}
                  className="flex-1 min-h-[44px] rounded-xl bg-cyan-600 hover:bg-cyan-500 text-black text-[10px] font-mono font-black uppercase flex items-center justify-center gap-2 transition-colors cursor-pointer active:scale-95"
                >
                  <ExternalLink className="w-4 h-4" /> Open Full Source
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
}