"use client";

import React, { useState, useEffect, useRef, useMemo } from "react";
import { 
  Youtube, X, Sliders, Volume1, Volume2, VolumeX, Disc, 
  Shuffle, FastForward, Rewind, Play, Pause, Music, Search, Loader2, 
  ListPlus, Trash2, Link as LinkIcon, Minimize2, Maximize2,
  Clock, Timer, History, Sparkles, Volume, Radio, SlidersHorizontal, Waves, Globe, Users, Plus, Check,
  ShieldAlert, Compass, Settings2, SlidersVertical, Bookmark, BookmarkCheck, Share2, Download
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { getYouTubeApiKey } from "../../services/CoPilotService";

declare global {
  interface Window {
    YT: any;
    onYouTubeIframeAPIReady: () => void;
  }
}

interface PresetChannel {
  label: string;
  query: string;
  category: "Global" | "Custom";
}

const DEFAULT_GLOBAL_PRESETS: PresetChannel[] = [
  { label: "🌆 Synthwave Highway", query: "Retro Synthwave Outrun Cyberpunk Mix", category: "Global" },
  { label: "⚡ EDM Festival Bass", query: "EDM Festival Bass Boosted Car Riding Mix", category: "Global" },
  { label: "🎸 Classic Rock Anthems", query: "70s 80s Classic Hard Rock Riding Anthems", category: "Global" },
  { label: "☕ Lo-Fi Ride Beats", query: "Lo-Fi Beats Chill Study Ride Music", category: "Global" },
  { label: "🎧 90s Hip Hop Inst.", query: "90s Hip Hop Instrumentals Bass Boosted", category: "Global" },
  { label: "🎹 Classical Piano", query: "Classical Piano Solo Performance Music", category: "Global" },
  { label: "🤠 Classic Country", query: "Classic Country Music 50s 60s 70s", category: "Global" },
  { label: "🔥 Heavy Metal Riffs", query: "Heavy Metal Riding Guitar Riffs Mix", category: "Global" },
  { label: "🎻 Titanic Era Strings", query: "Titanic Era String Quartet Chamber Music", category: "Global" },
  { label: "🚀 Johann Strauss Waltzes", query: "Johann Strauss Waltzes Classical Orchestra", category: "Global" },
  { label: "🎬 Epic Soundtracks", query: "Epic Movie Soundtracks Cinematic Orchestral Music", category: "Global" },
  { label: "🎷 Smooth Jazz Ride", query: "Smooth Jazz Saxophone Instrumental Chill Mix", category: "Global" },
  { label: "🌴 Reggae Surf Rock", query: "Reggae Surf Rock Instrumental Summer Mix", category: "Global" },
  { label: "⚡ Electro House Anthem", query: "Electro House Dance Music Ride Mix", category: "Global" },
  { label: "🌌 Ambient Space Chill", query: "Ambient Space Chillout Electronic Music", category: "Global" },
  { label: "🪩 80s Synthpop Hits", query: "80s Synthpop Electronic Highway Hits Mix", category: "Global" }
];

export default function YoutubeDeck({ ui, tx }: { ui: any; tx: any }) {
  const [ytSearchQuery, setYtSearchQuery] = useState("");
  const [ytResults, setYtResults] = useState<any[]>([]);
  const [ytQueue, setYtQueue] = useState<{ id: string; title: string; thumb: string; channel?: string }[]>([]);
  const [recentHistory, setRecentHistory] = useState<{ id: string; title: string; thumb: string; channel?: string }[]>([]);
  const [savedFavorites, setSavedFavorites] = useState<{ id: string; title: string; thumb: string; channel?: string }[]>([]);
  
  // Community / Custom Presets State
  const [customPresets, setCustomPresets] = useState<PresetChannel[]>([]);
  const [newPresetLabel, setNewPresetLabel] = useState("");
  const [newPresetQuery, setNewPresetQuery] = useState("");
  const [showAddPresetModal, setShowAddPresetModal] = useState(false);

  const [showYtSettings, setShowYtSettings] = useState(false);
  const [showQueueList, setShowQueueList] = useState(false);
  const [activeTab, setActiveTab] = useState<"presets" | "results" | "history" | "favorites">("presets");

  // Advanced Deck Controls & Expanded Settings State
  const [ytLoop, setYtLoop] = useState(false);
  const [ytPlaybackRate, setYtPlaybackRate] = useState<number>(1);
  const [ytVolume, setYtVolume] = useState<number>(() => {
    try {
      return Number(localStorage.getItem("rural_global_vol") ?? localStorage.getItem("rural_yt_vol") ?? 100);
    } catch(e) { return 100; }
  });
  const [isMuted, setIsMuted] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMiniPlayer, setIsMiniPlayer] = useState(false);
  const [isFetchingYt, setIsFetchingYt] = useState(false);
  const [ytError, setYtError] = useState<string | null>(null);

  // --- MASTER SETTINGS SYNC ENGINE ---
  useEffect(() => {
    const handleSettingsSync = () => {
      const masterVol = Number(localStorage.getItem("rural_global_vol") ?? 100);
      setYtVolume(masterVol);
      if (playerRef.current?.setVolume) {
        playerRef.current.setVolume(masterVol);
      }
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

  // New Expanded Settings
  const [crossfadeSeconds, setCrossfadeSeconds] = useState<number>(0);
  const [speedPitchLock, setSpeedPitchLock] = useState<boolean>(true);
  const [backgroundThrottleGuard, setBackgroundThrottleGuard] = useState<boolean>(true);
  const [hdStreamQuality, setHdStreamQuality] = useState<boolean>(true);

  const [audioProfile, setAudioProfile] = useState<"Flat" | "Bass Boost" | "Vocal" | "Helmet Wind">("Flat");
  const [sleepTimerRemaining, setSleepTimerRemaining] = useState<number | null>(null);

  const playerRef = useRef<any>(null);
  const queueRef = useRef(ytQueue);
  const loopRef = useRef(ytLoop);

  useEffect(() => {
    queueRef.current = ytQueue;
    try { localStorage.setItem("rural_yt_queue", JSON.stringify(ytQueue)); } catch (e) {}
  }, [ytQueue]);

  useEffect(() => {
    loopRef.current = ytLoop;
    localStorage.setItem("rural_yt_loop", ytLoop ? "true" : "false");
  }, [ytLoop]);

  // Load persistent configurations, history, favorites, & custom presets
  useEffect(() => {
    try {
      const savedQueue = localStorage.getItem("rural_yt_queue");
      if (savedQueue) setYtQueue(JSON.parse(savedQueue));
      const savedHistory = localStorage.getItem("rural_yt_history");
      if (savedHistory) setRecentHistory(JSON.parse(savedHistory));
      const savedFavs = localStorage.getItem("rural_yt_favorites");
      if (savedFavs) setSavedFavorites(JSON.parse(savedFavs));
      const savedVol = localStorage.getItem("rural_global_vol") || localStorage.getItem("rural_yt_vol");
      if (savedVol) setYtVolume(Number(savedVol));
      setYtLoop(localStorage.getItem("rural_yt_loop") === "true");
      setIsMiniPlayer(localStorage.getItem("rural_yt_mini") === "true");

      const savedCustom = localStorage.getItem("rural_yt_custom_presets");
      if (savedCustom) setCustomPresets(JSON.parse(savedCustom));

      const savedCrossfade = localStorage.getItem("rural_yt_crossfade");
      if (savedCrossfade) setCrossfadeSeconds(Number(savedCrossfade));

      const savedPitchLock = localStorage.getItem("rural_yt_pitch_lock");
      if (savedPitchLock !== null) setSpeedPitchLock(savedPitchLock === "true");

      const savedThrottle = localStorage.getItem("rural_yt_throttle_guard");
      if (savedThrottle !== null) setBackgroundThrottleGuard(savedThrottle === "true");

      const savedHd = localStorage.getItem("rural_yt_hd");
      if (savedHd !== null) setHdStreamQuality(savedHd === "true");
    } catch (e) {}
  }, []);

  const combinedPresets = useMemo(() => {
    return [...DEFAULT_GLOBAL_PRESETS, ...customPresets];
  }, [customPresets]);

  const handleAddCustomPreset = () => {
    if (!newPresetLabel.trim() || !newPresetQuery.trim()) return;
    const newEntry: PresetChannel = {
      label: newPresetLabel.trim(),
      query: newPresetQuery.trim(),
      category: "Custom"
    };
    const updated = [...customPresets, newEntry];
    setCustomPresets(updated);
    localStorage.setItem("rural_yt_custom_presets", JSON.stringify(updated));
    setNewPresetLabel("");
    setNewPresetQuery("");
    setShowAddPresetModal(false);
  };

  const handleRemoveCustomPreset = (queryStr: string) => {
    const updated = customPresets.filter(p => p.query !== queryStr);
    setCustomPresets(updated);
    localStorage.setItem("rural_yt_custom_presets", JSON.stringify(updated));
  };

  const toggleFavoriteTrack = (item: { id: string; title: string; thumb: string; channel?: string }) => {
    const exists = savedFavorites.some(f => f.id === item.id);
    let updated: typeof savedFavorites;
    if (exists) {
      updated = savedFavorites.filter(f => f.id !== item.id);
    } else {
      updated = [item, ...savedFavorites];
    }
    setSavedFavorites(updated);
    try { localStorage.setItem("rural_yt_favorites", JSON.stringify(updated)); } catch(e) {}
  };

  const exportHistoryLog = () => {
    const content = `# Rural ERides GO - Media Sortie History\nExported: ${new Date().toLocaleString()}\n\n` +
      recentHistory.map((h, i) => `${i + 1}. [${h.title}] (Channel: ${h.channel || 'N/A'}) - https://youtu.be/${h.id}`).join("\n");
    const blob = new Blob([content], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `Media_History_${Date.now()}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  useEffect(() => {
    if (sleepTimerRemaining === null || sleepTimerRemaining <= 0) return;
    const interval = setInterval(() => {
      setSleepTimerRemaining(prev => {
        if (prev && prev > 1) return prev - 1;
        if (playerRef.current?.pauseVideo) {
          playerRef.current.pauseVideo();
          setIsPlaying(false);
        }
        return null;
      });
    }, 60000);
    return () => clearInterval(interval);
  }, [sleepTimerRemaining]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (["input", "textarea"].includes((e.target as HTMLElement).tagName.toLowerCase())) return;
      if (e.code === "Space") {
        e.preventDefault();
        togglePlayPause();
      } else if (e.code === "ArrowRight") {
        handleSeekOffset(10);
      } else if (e.code === "ArrowLeft") {
        handleSeekOffset(-10);
      } else if (e.code === "KeyN") {
        handleNextTrack();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isPlaying]);

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden && backgroundThrottleGuard && playerRef.current?.pauseVideo) {
        playerRef.current.pauseVideo();
        setIsPlaying(false);
      }
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => document.removeEventListener("visibilitychange", handleVisibilityChange);
  }, [backgroundThrottleGuard]);

  useEffect(() => {
    if (!window.YT) {
      const tag = document.createElement("script");
      tag.src = "https://www.youtube.com/iframe_api";
      const firstScriptTag = document.getElementsByTagName("script")[0];
      firstScriptTag?.parentNode?.insertBefore(tag, firstScriptTag);
    }
  }, []);

  useEffect(() => {
    if (!ytQueue.length) {
      if (playerRef.current) {
        try { playerRef.current.destroy(); } catch (e) {}
        playerRef.current = null;
      }
      setIsPlaying(false);
      return;
    }

    const currentVideoId = ytQueue[0].id;

    setRecentHistory(prev => {
      const filtered = prev.filter(p => p.id !== currentVideoId);
      const updated = [ytQueue[0], ...filtered].slice(0, 25);
      try { localStorage.setItem("rural_yt_history", JSON.stringify(updated)); } catch(e) {}
      return updated;
    });

    const initPlayer = () => {
      if (!window.YT || !window.YT.Player) {
        setTimeout(initPlayer, 200);
        return;
      }

      if (playerRef.current) {
        try {
          playerRef.current.loadVideoById(currentVideoId);
          playerRef.current.setVolume(ytVolume);
          playerRef.current.setPlaybackRate(ytPlaybackRate);
          return;
        } catch (e) {}
      }

      playerRef.current = new window.YT.Player("youtube-active-player-frame", {
        videoId: currentVideoId,
        playerVars: {
          autoplay: 1,
          playsinline: 1,
          controls: 1,
          rel: 0,
          modestbranding: 1,
          vq: hdStreamQuality ? "hd1080" : "large"
        },
        events: {
          onReady: (event: any) => {
            event.target.setVolume(ytVolume);
            event.target.setPlaybackRate(ytPlaybackRate);
            event.target.playVideo();
            setIsPlaying(true);
          },
          onStateChange: (event: any) => {
            if (event.data === window.YT.PlayerState.PLAYING) {
              setIsPlaying(true);
            } else if (event.data === window.YT.PlayerState.PAUSED) {
              setIsPlaying(false);
            } else if (event.data === window.YT.PlayerState.ENDED) {
              handleAutoAdvance();
            }
          },
          onError: () => {
            setYtError("Video unavailable or embedding restricted. Advancing queue...");
            setTimeout(handleNextTrack, 1500);
          }
        }
      });
    };

    initPlayer();
  }, [ytQueue[0]?.id]);

  const handleAutoAdvance = () => {
    const currentQ = queueRef.current;
    if (currentQ.length > 1) {
      setYtQueue(prev => prev.slice(1));
    } else if (currentQ.length === 1 && loopRef.current) {
      if (playerRef.current?.seekTo) {
        playerRef.current.seekTo(0);
        playerRef.current.playVideo();
      }
    } else {
      setYtQueue([]);
      setIsPlaying(false);
    }
  };

  const handleNextTrack = () => {
    if (ytQueue.length > 1) {
      setYtQueue(prev => prev.slice(1));
    } else if (ytQueue.length === 1 && ytLoop) {
      if (playerRef.current?.seekTo) {
        playerRef.current.seekTo(0);
        playerRef.current.playVideo();
      }
    } else {
      setYtQueue([]);
      setIsPlaying(false);
    }
  };

  const handleVolumeChange = (newVol: number) => {
    setYtVolume(newVol);
    localStorage.setItem("rural_yt_vol", newVol.toString());
    localStorage.setItem("rural_global_vol", newVol.toString());
    if (playerRef.current?.setVolume) {
      playerRef.current.setVolume(newVol);
      if (newVol > 0 && isMuted) {
        playerRef.current.unMute();
        setIsMuted(false);
      }
    }
    window.dispatchEvent(new Event("settings-sync"));
  };

  const toggleMute = () => {
    if (!playerRef.current) return;
    if (isMuted) {
      playerRef.current.unMute();
      setIsMuted(false);
    } else {
      playerRef.current.mute();
      setIsMuted(true);
    }
  };

  const togglePlayPause = () => {
    if (!playerRef.current) return;
    if (isPlaying) {
      playerRef.current.pauseVideo();
      setIsPlaying(false);
    } else {
      playerRef.current.playVideo();
      setIsPlaying(true);
    }
  };

  const handleSeekOffset = (seconds: number) => {
    if (!playerRef.current?.getCurrentTime || !playerRef.current?.seekTo) return;
    const current = playerRef.current.getCurrentTime();
    playerRef.current.seekTo(Math.max(0, current + seconds), true);
  };

  const extractYoutubeId = (url: string) => {
    const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
    const match = url.match(regExp);
    return (match && match[2].length === 11) ? match[2] : null;
  };

  const handleYoutubeSearch = async (overrideQuery?: string) => {
    const q = (overrideQuery || ytSearchQuery).trim();
    if (!q) return;

    const directId = extractYoutubeId(q);
    if (directId) {
      const directItem = {
        id: directId,
        title: `Direct Link Stream (${directId})`,
        thumb: `https://img.youtube.com/vi/${directId}/hqdefault.jpg`,
        channel: "Direct Channel Stream"
      };
      setYtQueue(prev => [directItem, ...prev]);
      setYtSearchQuery("");
      return;
    }

    setIsFetchingYt(true);
    setYtError(null);
    try {
      const ytKey = getYouTubeApiKey();
      const res = await fetch(`https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=20&q=${encodeURIComponent(q)}&type=video&key=${ytKey}`);
      const data = await res.json();
      if (data.items && data.items.length > 0) {
        setYtResults(data.items.map((i: any) => ({
          id: { videoId: i.id.videoId },
          snippet: i.snippet
        })));
        setActiveTab("results");
      } else {
        setYtError(data.error?.message || "No results found for this query.");
      }
    } catch (err) {
      setYtError("Media telemetry uplink offline.");
    } finally {
      setIsFetchingYt(false);
    }
  };

  const handleShuffleQueue = () => {
    if (ytQueue.length <= 1) return;
    const currentTrack = ytQueue[0];
    const rest = [...ytQueue.slice(1)];
    for (let i = rest.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [rest[i], rest[j]] = [rest[j], rest[i]];
    }
    setYtQueue([currentTrack, ...rest]);
  };

  const removeQueueItem = (idx: number) => {
    setYtQueue(prev => prev.filter((_, i) => i !== idx));
  };

  return (
    <div className={`${ui.bgPanel} p-3.5 sm:p-6 rounded-3xl shadow-xl border transition-colors space-y-4 sm:space-y-5 ${ui.brd}`}>
      
      {/* HEADER BAR */}
      <div className={`flex flex-col sm:flex-row justify-between items-stretch sm:items-center border-b ${ui.brd} pb-3.5 gap-3 w-full`}>
        <div className="flex items-center gap-2.5">
          <div className="bg-rose-600 p-2.5 rounded-2xl text-white shadow-md shrink-0">
            <Youtube className="w-5 h-5" />
          </div>
          <div>
            <h3 className={`${ui.txtMain} font-black uppercase tracking-widest text-xs sm:text-sm flex items-center gap-2`}>
              {tx('audio')} <span className="text-[9px] text-emerald-400 font-mono">COMMUNITY MEDIA DECK</span>
            </h3>
            <p className={`text-[8px] sm:text-[8.5px] ${ui.txtMuted} font-mono uppercase mt-0.5`}>
              {ytQueue.length} In Queue • {isPlaying ? "Streaming" : "Standby"} {sleepTimerRemaining ? `• Timer: ${sleepTimerRemaining}m` : ""}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-3 sm:flex sm:flex-wrap gap-2 w-full sm:w-auto">
          {ytQueue.length > 0 && (
            <button 
              onClick={() => setShowQueueList(!showQueueList)} 
              className={`min-h-[44px] px-3 text-[9px] sm:text-[9.5px] font-mono font-black border rounded-xl transition-colors flex items-center justify-center gap-1 cursor-pointer active:scale-95 touch-manipulation ${showQueueList ? `${ui.t.bg} text-black border-transparent shadow-md` : `${ui.bgBase} ${ui.brd} ${ui.txtMuted} hover:${ui.txtMain}`}`}
            >
              <ListPlus className="w-3.5 h-3.5 shrink-0"/> Queue ({ytQueue.length})
            </button>
          )}

          <button 
            onClick={() => { setYtQueue([]); setIsPlaying(false); }} 
            className={`min-h-[44px] px-3 text-[9px] sm:text-[9.5px] font-mono font-black border rounded-xl transition-colors flex items-center justify-center gap-1 cursor-pointer active:scale-95 touch-manipulation ${ui.bgBase} ${ui.brd} ${ui.txtMuted} hover:text-rose-500 hover:border-rose-900`}
          >
            <X className="w-3.5 h-3.5 shrink-0"/> {tx('clear')}
          </button>

          <button 
            onClick={() => setShowYtSettings(!showYtSettings)} 
            className={`min-h-[44px] px-3 text-[9px] sm:text-[9.5px] font-mono font-black border rounded-xl transition-colors flex items-center justify-center gap-1 cursor-pointer active:scale-95 touch-manipulation ${showYtSettings ? 'bg-zinc-800 border-zinc-700 text-white' : `${ui.bgBase} ${ui.brd} ${ui.txtMuted} hover:border-zinc-500`}`}
          >
            <Sliders className="w-3.5 h-3.5 shrink-0"/> Settings
          </button>
        </div>
      </div>

      {/* EXPANDED SETTINGS PANEL */}
      <AnimatePresence>
        {showYtSettings && (
          <motion.div 
            initial={{ opacity: 0, height: 0 }} 
            animate={{ opacity: 1, height: "auto" }} 
            exit={{ opacity: 0, height: 0 }} 
            className={`${ui.bgCard} p-3.5 rounded-2xl border ${ui.brd} grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2.5 text-xs overflow-hidden`}
          >
            {/* Loop Toggle */}
            <div className={`flex justify-between items-center ${ui.bgList} px-3 min-h-[44px] rounded-xl border ${ui.brd}`}>
              <span className={`text-[8.5px] font-black uppercase ${ui.txtMuted} tracking-wider`}>Loop Active</span>
              <button 
                onClick={() => {
                  const val = !ytLoop;
                  setYtLoop(val);
                }} 
                className={`min-h-[32px] px-2.5 text-[8px] font-mono font-black border rounded-lg uppercase cursor-pointer transition-colors active:scale-95 touch-manipulation ${ytLoop ? 'bg-rose-950/40 border-rose-900 text-rose-500' : `${ui.bgBase} ${ui.brd} ${ui.txtMuted}`}`}
              >
                {ytLoop ? 'ON' : 'OFF'}
              </button>
            </div>

            {/* Mini Player Toggle */}
            <div className={`flex justify-between items-center ${ui.bgList} px-3 min-h-[44px] rounded-xl border ${ui.brd}`}>
              <span className={`text-[8.5px] font-black uppercase ${ui.txtMuted} tracking-wider flex items-center gap-1`}>
                {isMiniPlayer ? <Minimize2 className="w-3 h-3"/> : <Maximize2 className="w-3 h-3"/>} Player Mode
              </span>
              <button 
                onClick={() => {
                  const val = !isMiniPlayer;
                  setIsMiniPlayer(val);
                  localStorage.setItem("rural_yt_mini", val ? "true" : "false");
                }} 
                className={`min-h-[32px] px-2.5 text-[8px] font-mono font-black border rounded-lg uppercase cursor-pointer transition-colors active:scale-95 touch-manipulation ${isMiniPlayer ? 'bg-cyan-950/40 border-cyan-900 text-cyan-400' : `${ui.bgBase} ${ui.brd} ${ui.txtMuted}`}`}
              >
                {isMiniPlayer ? 'MINI' : 'FULL'}
              </button>
            </div>

            {/* HD Stream Quality */}
            <div className={`flex justify-between items-center ${ui.bgList} px-3 min-h-[44px] rounded-xl border ${ui.brd}`}>
              <span className={`text-[8.5px] font-black uppercase ${ui.txtMuted} tracking-wider`}>HD 1080p Stream</span>
              <button 
                onClick={() => {
                  const val = !hdStreamQuality;
                  setHdStreamQuality(val);
                  localStorage.setItem("rural_yt_hd", val ? "true" : "false");
                }} 
                className={`min-h-[32px] px-2.5 text-[8px] font-mono font-black border rounded-lg uppercase cursor-pointer transition-colors active:scale-95 touch-manipulation ${hdStreamQuality ? 'bg-emerald-950/40 border-emerald-900 text-emerald-400' : `${ui.bgBase} ${ui.brd} ${ui.txtMuted}`}`}
              >
                {hdStreamQuality ? 'HD ON' : 'SD'}
              </button>
            </div>

            {/* Sleep Timer */}
            <div className={`flex justify-between items-center ${ui.bgList} px-3 min-h-[44px] rounded-xl border ${ui.brd}`}>
              <span className={`text-[8.5px] font-black uppercase ${ui.txtMuted} tracking-wider flex items-center gap-1`}>
                <Timer className="w-3 h-3 text-amber-400" /> Sleep Timer
              </span>
              <div className="flex gap-1">
                {[15, 30, 60].map(mins => (
                  <button
                    key={mins}
                    onClick={() => setSleepTimerRemaining(sleepTimerRemaining === mins ? null : mins)}
                    className={`min-h-[32px] px-2 text-[7.5px] font-mono font-bold rounded border cursor-pointer active:scale-95 touch-manipulation ${sleepTimerRemaining === mins ? 'bg-amber-500 text-black font-black' : `${ui.bgBase} ${ui.brd} ${ui.txtMuted}`}`}
                  >
                    {mins}m
                  </button>
                ))}
              </div>
            </div>

            {/* Crossfade Duration */}
            <div className={`flex justify-between items-center ${ui.bgList} px-3 min-h-[44px] rounded-xl border ${ui.brd}`}>
              <span className={`text-[8.5px] font-black uppercase ${ui.txtMuted} tracking-wider`}>Crossfade</span>
              <div className="flex gap-1">
                {[0, 2, 5].map(secs => (
                  <button
                    key={secs}
                    onClick={() => {
                      setCrossfadeSeconds(secs);
                      localStorage.setItem("rural_yt_crossfade", secs.toString());
                    }}
                    className={`min-h-[32px] px-2 text-[7.5px] font-mono font-bold rounded border cursor-pointer active:scale-95 touch-manipulation ${crossfadeSeconds === secs ? 'bg-cyan-500 text-black font-black' : `${ui.bgBase} ${ui.brd} ${ui.txtMuted}`}`}
                  >
                    {secs}s
                  </button>
                ))}
              </div>
            </div>

            {/* Background Throttle Guard */}
            <div className={`flex justify-between items-center ${ui.bgList} px-3 min-h-[44px] rounded-xl border ${ui.brd}`}>
              <span className={`text-[8.5px] font-black uppercase ${ui.txtMuted} tracking-wider`} title="Pause playback when app is backgrounded">Background Guard</span>
              <button 
                onClick={() => {
                  const val = !backgroundThrottleGuard;
                  setBackgroundThrottleGuard(val);
                  localStorage.setItem("rural_yt_throttle_guard", val ? "true" : "false");
                }} 
                className={`min-h-[32px] px-2.5 text-[8px] font-mono font-black border rounded-lg uppercase cursor-pointer transition-colors active:scale-95 touch-manipulation ${backgroundThrottleGuard ? 'bg-cyan-950/40 border-cyan-900 text-cyan-400' : `${ui.bgBase} ${ui.brd} ${ui.txtMuted}`}`}
              >
                {backgroundThrottleGuard ? 'GUARD ON' : 'OFF'}
              </button>
            </div>

            {/* Sound EQ Preset */}
            <div className={`flex justify-between items-center ${ui.bgList} px-3 min-h-[44px] rounded-xl border ${ui.brd} col-span-1 sm:col-span-2 md:col-span-3`}>
              <span className={`text-[8.5px] font-black uppercase ${ui.txtMuted} tracking-wider flex items-center gap-1`}>
                <SlidersHorizontal className="w-3 h-3 text-purple-400" /> Sound EQ Profile
              </span>
              <select
                value={audioProfile}
                onChange={e => setAudioProfile(e.target.value as any)}
                className="bg-black text-[8.5px] font-mono font-bold text-purple-300 border border-zinc-700 px-2 py-1 rounded outline-none cursor-pointer"
              >
                <option value="Flat">Flat Reference</option>
                <option value="Bass Boost">Bass Boost (Sub & Kick)</option>
                <option value="Vocal">Vocal Focus (Podcasts/Talk)</option>
                <option value="Helmet Wind">Helmet Highs (Wind Cut)</option>
              </select>
            </div>

            {/* Master Volume Control Bar */}
            <div className={`flex flex-col justify-center ${ui.bgList} px-3.5 py-2.5 rounded-xl border ${ui.brd} gap-1.5 col-span-1 sm:col-span-2 md:col-span-3`}>
              <div className="flex items-center justify-between">
                <span className={`text-[8.5px] font-black uppercase ${ui.txtMuted} tracking-wider flex items-center gap-1.5 font-mono`}>
                  {isMuted || ytVolume === 0 ? <VolumeX className="w-3 h-3 text-rose-500"/> : ytVolume < 50 ? <Volume1 className="w-3 h-3 text-cyan-400"/> : <Volume2 className="w-3 h-3 text-emerald-400"/>}
                  Master Output Gain
                </span>
                <div className="flex items-center gap-2 font-mono">
                  <button onClick={toggleMute} className={`min-h-[32px] px-2 text-[7.5px] font-bold uppercase rounded border cursor-pointer active:scale-95 touch-manipulation ${isMuted ? 'bg-rose-950/40 border-rose-900 text-rose-400' : `${ui.bgBase} ${ui.brd} ${ui.txtMuted}`}`}>
                    {isMuted ? "UNMUTE" : "MUTE"}
                  </button>
                  <span className={`${ui.txtMain} font-bold text-xs tabular-nums`}>{isMuted ? "0" : ytVolume}%</span>
                </div>
              </div>
              <input 
                type="range" min="0" max="100" 
                value={isMuted ? 0 : ytVolume} 
                onChange={e => handleVolumeChange(Number(e.target.value))} 
                className="w-full accent-rose-500 cursor-pointer h-2 bg-zinc-800 rounded-lg touch-manipulation" 
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ADD COMMUNITY PRESET MODAL */}
      <AnimatePresence>
        {showAddPresetModal && (
          <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }} className={`${ui.bgPanel} border ${ui.brd} max-w-md w-full rounded-3xl p-5 space-y-3.5 shadow-2xl`}>
              <div className="flex justify-between items-center border-b border-zinc-800 pb-3">
                <h4 className="font-mono font-black text-xs uppercase text-white flex items-center gap-2">
                  <Globe className="w-4 h-4 text-cyan-400" /> Add Community Preset Channel
                </h4>
                <button onClick={() => setShowAddPresetModal(false)} className="min-h-[36px] min-w-[36px] flex items-center justify-center rounded-xl bg-zinc-900 text-zinc-400 hover:text-white cursor-pointer active:scale-95">
                  <X className="w-4 h-4"/>
                </button>
              </div>
              <p className="text-[10px] font-mono text-zinc-400 leading-relaxed">
                Publish a custom search preset to your local device so all community riders can access it instantly in their Mission Presets deck.
              </p>
              <input 
                value={newPresetLabel}
                onChange={(e) => setNewPresetLabel(e.target.value)}
                placeholder="Preset Name (e.g., 🌊 Coastal Sunset Cruise)..."
                className={`w-full min-h-[44px] ${ui.bgBase} border ${ui.brd} rounded-xl px-3 text-xs font-mono font-bold text-white outline-none`}
              />
              <input 
                value={newPresetQuery}
                onChange={(e) => setNewPresetQuery(e.target.value)}
                placeholder="YouTube Search Query (e.g., Chill Reggae Surf Rock)..."
                className={`w-full min-h-[44px] ${ui.bgBase} border ${ui.brd} rounded-xl px-3 text-xs font-mono font-bold text-white outline-none`}
              />
              <div className="flex gap-2 pt-2">
                <button 
                  onClick={handleAddCustomPreset}
                  className="flex-1 min-h-[44px] bg-cyan-600 hover:bg-cyan-500 text-black text-[10px] font-mono font-black uppercase rounded-xl transition-all active:scale-95 cursor-pointer shadow-md"
                >
                  Save Community Preset
                </button>
                <button 
                  onClick={() => setShowAddPresetModal(false)}
                  className={`min-h-[44px] px-4 ${ui.bgList} border ${ui.brd} ${ui.txtMain} text-[10px] font-mono font-bold uppercase rounded-xl cursor-pointer active:scale-95`}
                >
                  Cancel
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showQueueList && ytQueue.length > 0 && (
          <motion.div 
            initial={{ opacity: 0, height: 0 }} 
            animate={{ opacity: 1, height: "auto" }} 
            exit={{ opacity: 0, height: 0 }} 
            className={`${ui.bgCard} p-3.5 rounded-2xl border ${ui.brd} space-y-3 overflow-hidden`}
          >
            <div className="flex justify-between items-center border-b border-zinc-800 pb-2">
              <span className={`text-[8.5px] font-black uppercase tracking-widest ${ui.txtMain} flex items-center gap-1.5 font-mono`}>
                <Disc className="w-3.5 h-3.5 text-rose-500 animate-spin"/> Queued Dispatches ({ytQueue.length})
              </span>
              <button onClick={() => setYtQueue([])} className={`text-[8px] font-mono font-bold text-rose-400 hover:text-rose-300 uppercase cursor-pointer`}>
                Clear All
              </button>
            </div>
            <div className="max-h-[220px] overflow-y-auto custom-scrollbar space-y-1.5 pr-1">
              {ytQueue.map((item, index) => (
                <div 
                  key={`${item.id}-${index}`} 
                  className={`flex items-center justify-between p-2 rounded-xl border ${index === 0 ? 'bg-rose-950/30 border-rose-900/60' : `${ui.bgList} ${ui.brd}`} text-xs gap-3`}
                >
                  <div className="flex items-center gap-2.5 truncate min-w-0">
                    <span className={`text-[8.5px] font-mono font-bold ${index === 0 ? 'text-rose-400 font-black' : ui.txtMuted}`}>
                      {index === 0 ? "PLAYING" : `#${index + 1}`}
                    </span>
                    <img src={item.thumb} alt="" className="w-9 h-7 object-cover rounded-md border border-zinc-800 shrink-0" />
                    <span className={`truncate font-bold text-[11px] ${index === 0 ? 'text-white' : ui.txtMain}`}>{item.title}</span>
                  </div>
                  <button 
                    onClick={() => removeQueueItem(index)}
                    className="min-h-[36px] min-w-[36px] flex items-center justify-center p-1.5 text-zinc-500 hover:text-rose-400 transition-colors cursor-pointer shrink-0 active:scale-95 touch-manipulation"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 w-full">
        <div className={`lg:col-span-5 flex flex-col justify-between space-y-4 ${ui.bgCard} border ${ui.brd} p-3.5 sm:p-5 rounded-3xl shadow-inner w-full`}>
          {ytQueue.length > 0 ? (
            <div className="space-y-3.5 w-full">
              <div 
                className={`rounded-2xl overflow-hidden border ${ui.brd} shadow-2xl bg-black relative transition-all duration-300 ${isMiniPlayer ? 'w-48 aspect-video mx-auto border-cyan-500/50 shadow-cyan-950/30' : 'w-full aspect-video'}`}
              >
                <div id="youtube-active-player-frame" className="w-full h-full"></div>
              </div>

              <div className="bg-black/60 border border-zinc-800 rounded-xl p-2.5 flex items-end justify-between h-10 gap-1 overflow-hidden shadow-inner">
                {Array.from({ length: 24 }).map((_, i) => (
                  <motion.div
                    key={i}
                    animate={{
                      height: isPlaying ? [`${Math.max(15, (i % 6) * 15)}%`, `${Math.max(20, Math.random() * 100)}%`, `${Math.max(10, (i % 4) * 20)}%`] : "4px"
                    }}
                    transition={{
                      repeat: isPlaying ? Infinity : 0,
                      duration: 0.4 + (i * 0.03),
                      ease: "easeInOut"
                    }}
                    className={`w-full rounded-full transition-colors ${isPlaying ? 'bg-gradient-to-t from-rose-600 to-amber-400' : 'bg-zinc-800'}`}
                  />
                ))}
              </div>

              <div className={`${ui.bgList} border ${ui.brd} rounded-2xl p-3.5 flex flex-col gap-3 shadow-inner`}>
                <div className="text-center px-1">
                  <div className={`text-[11px] font-bold ${ui.txtMain} truncate`}>{ytQueue[0].title}</div>
                  <div className={`text-[8.5px] ${ui.txtMuted} font-mono uppercase mt-0.5`}>
                    {ytQueue[0].channel || "Active Telemetry Audio Track"}
                  </div>
                </div>

                <div className={`flex justify-center items-center gap-1.5 pt-2 border-t ${ui.brd}`}>
                  <button 
                    onClick={() => handleSeekOffset(-10)} 
                    className={`min-h-[44px] min-w-[44px] flex items-center justify-center ${ui.bgBase} border ${ui.brd} rounded-xl ${ui.txtMuted} hover:${ui.txtMain} transition-colors cursor-pointer active:scale-95 touch-manipulation`}
                    title="Rewind 10 Seconds"
                  >
                    <Rewind className="w-4 h-4"/>
                  </button>

                  <button 
                    onClick={togglePlayPause} 
                    className={`flex-1 min-h-[44px] flex items-center justify-center gap-1.5 px-3 rounded-xl text-[9.5px] font-mono font-black uppercase transition-all shadow-md cursor-pointer active:scale-95 touch-manipulation ${isPlaying ? `${ui.t.bg} text-black font-black` : 'bg-zinc-800 text-white border border-zinc-700'}`}
                  >
                    {isPlaying ? <Pause className="w-3.5 h-3.5 fill-current"/> : <Play className="w-3.5 h-3.5 fill-current"/>}
                    {isPlaying ? "PAUSE" : "RESUME"}
                  </button>

                  <button 
                    onClick={() => handleSeekOffset(10)} 
                    className={`min-h-[44px] min-w-[44px] flex items-center justify-center ${ui.bgBase} border ${ui.brd} rounded-xl ${ui.txtMuted} hover:${ui.txtMain} transition-colors cursor-pointer active:scale-95 touch-manipulation`}
                    title="Forward 10 Seconds"
                  >
                    <FastForward className="w-4 h-4"/>
                  </button>

                  <button 
                    onClick={handleShuffleQueue} 
                    className={`min-h-[44px] min-w-[44px] flex items-center justify-center ${ui.bgBase} border ${ui.brd} rounded-xl ${ui.txtMuted} hover:${ui.txtMain} transition-colors cursor-pointer active:scale-95 touch-manipulation`}
                    title="Shuffle Sequence"
                  >
                    <Shuffle className="w-4 h-4"/>
                  </button>

                  <button 
                    onClick={handleNextTrack} 
                    className={`min-h-[44px] px-3 bg-rose-950/40 border border-rose-900/50 text-rose-500 rounded-xl text-[8.5px] font-mono font-black uppercase hover:bg-rose-600 hover:text-white transition-colors cursor-pointer active:scale-95 touch-manipulation`}
                    title="Next Track"
                  >
                    NEXT
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className={`h-full min-h-[240px] flex flex-col items-center justify-center text-center p-6 border border-dashed ${ui.brd} rounded-2xl`}>
              <Music className={`w-10 h-10 ${ui.txtMuted} animate-pulse mb-3`} />
              <span className={`text-[11px] font-mono ${ui.txtMuted} uppercase tracking-widest font-bold`}>Deck Standby</span>
              <p className={`text-[8px] ${ui.txtMuted} uppercase mt-1 font-mono`}>Select a music preset or search for audio below</p>
            </div>
          )}
        </div>

        <div className="lg:col-span-7 space-y-4 w-full">
          {/* Fully Wrapping Header Controls & Tabs */}
          <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center border-b border-zinc-800 pb-3 gap-2">
            <div className="grid grid-cols-2 sm:flex sm:flex-wrap gap-1.5 w-full sm:w-auto">
              {[
                { id: "presets", label: `Music (${combinedPresets.length})`, icon: Radio },
                { id: "results", label: `Search (${ytResults.length})`, icon: Search },
                { id: "favorites", label: `Favorites (${savedFavorites.length})`, icon: Bookmark },
                { id: "history", label: `History (${recentHistory.length})`, icon: History },
              ].map(tab => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`min-h-[40px] px-2.5 sm:px-3 rounded-xl text-[8px] sm:text-[9px] font-mono font-black uppercase tracking-wider flex items-center justify-center gap-1 cursor-pointer active:scale-95 transition-all ${activeTab === tab.id ? 'bg-rose-600 text-white font-black shadow-md' : `${ui.bgList} ${ui.txtMuted} border ${ui.brd}`}`}
                >
                  <tab.icon className="w-3.5 h-3.5 shrink-0" /> <span className="truncate">{tab.label}</span>
                </button>
              ))}
            </div>

            <div className="flex gap-1.5 w-full sm:w-auto">
              {activeTab === "history" && recentHistory.length > 0 && (
                <button
                  onClick={exportHistoryLog}
                  className="min-h-[40px] px-3 rounded-xl bg-zinc-900 border border-zinc-700 text-emerald-400 text-[9px] font-mono font-black uppercase flex items-center justify-center gap-1.5 cursor-pointer active:scale-95 touch-manipulation flex-1 sm:flex-none"
                  title="Export History Log"
                >
                  <Download className="w-3.5 h-3.5 shrink-0" /> Export Log
                </button>
              )}
              <button
                onClick={() => setShowAddPresetModal(true)}
                className="min-h-[40px] px-3 rounded-xl bg-cyan-950/50 border border-cyan-800 text-cyan-400 text-[9px] font-mono font-black uppercase flex items-center justify-center gap-1.5 shrink-0 cursor-pointer active:scale-95 touch-manipulation flex-1 sm:flex-none"
                title="Add Community Music Preset"
              >
                <Plus className="w-3.5 h-3.5 shrink-0" /> Add Preset
              </button>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-2.5">
            <div className="relative flex-1">
              <input 
                value={ytSearchQuery} 
                onChange={(e) => setYtSearchQuery(e.target.value)} 
                onKeyDown={(e) => e.key === "Enter" && handleYoutubeSearch()} 
                placeholder="Search music mix or paste direct YouTube link / video ID..." 
                className={`w-full min-h-[48px] ${ui.bgList} border ${ui.brd} rounded-xl pl-10 pr-4 ${ui.txtMain} text-xs font-mono font-bold outline-none focus:border-rose-500 transition-colors shadow-inner`} 
              />
              <LinkIcon className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            </div>
            <button 
              onClick={() => handleYoutubeSearch()} 
              disabled={isFetchingYt}
              className="min-h-[48px] w-full sm:w-auto bg-rose-600 hover:bg-rose-500 text-white px-5 rounded-xl text-[10px] font-mono font-black uppercase shadow-lg flex items-center justify-center gap-2 shrink-0 cursor-pointer disabled:opacity-50 transition-all active:scale-95 touch-manipulation"
            >
              {isFetchingYt ? <Loader2 className="w-4 h-4 animate-spin"/> : <Search className="w-4 h-4"/>}
              Query
            </button>
          </div>

          {ytError && (
            <div className="p-3 bg-rose-950/50 border border-rose-900 text-rose-400 text-[10px] font-mono font-bold rounded-xl">
              {ytError}
            </div>
          )}

          {activeTab === "presets" && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 max-h-[360px] overflow-y-auto custom-scrollbar pr-1">
              {combinedPresets.map(genre => (
                <div key={genre.label} className="relative group flex items-center">
                  <button 
                    onClick={() => { setYtSearchQuery(genre.query); handleYoutubeSearch(genre.query); }} 
                    className={`w-full min-h-[48px] px-3.5 ${ui.bgList} border ${ui.brd} hover:border-rose-500/50 ${ui.txtMuted} hover:${ui.txtMain} text-[9px] sm:text-[9.5px] font-mono font-black uppercase rounded-xl text-left truncate shadow-inner cursor-pointer transition-all active:scale-95 touch-manipulation flex items-center justify-between`}
                  >
                    <span className="truncate pr-4">{genre.label}</span>
                    {genre.category === "Custom" && <span className="text-[7px] bg-cyan-950 text-cyan-400 px-1 py-0.5 rounded ml-1 shrink-0">COMMUNITY</span>}
                  </button>
                  {genre.category === "Custom" && (
                    <button
                      onClick={(e) => { e.stopPropagation(); handleRemoveCustomPreset(genre.query); }}
                      className="absolute right-2 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-rose-400 p-1.5 cursor-pointer active:scale-95"
                      title="Delete Custom Preset"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}

          {activeTab === "results" && (
            <div className={`${ui.bgCard} border ${ui.brd} rounded-3xl p-3 max-h-[360px] overflow-y-auto custom-scrollbar space-y-2`}>
              {ytResults.length === 0 ? (
                <div className="text-center py-8 text-xs font-mono text-zinc-500 uppercase">
                  Execute a search query above to load music dispatches
                </div>
              ) : (
                ytResults.map((vid, idx) => {
                  const item = {
                    id: vid.id.videoId,
                    title: vid.snippet.title,
                    thumb: vid.snippet.thumbnails.default.url,
                    channel: vid.snippet.channelTitle
                  };
                  const isFav = savedFavorites.some(f => f.id === item.id);

                  return (
                    <div 
                      key={`${vid.id.videoId}-${idx}`} 
                      className={`${ui.bgList} border ${ui.brd} p-2.5 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:border-rose-500/50 transition-colors`}
                    >
                      <div className="flex gap-3 items-center truncate">
                        <img 
                          src={vid.snippet.thumbnails.default.url} 
                          alt="" 
                          className={`w-16 h-12 object-cover rounded-xl border ${ui.brd} shrink-0 bg-black`} 
                        />
                        <div className="truncate">
                          <h4 className={`text-xs font-bold ${ui.txtMain} truncate`}>{vid.snippet.title}</h4>
                          <p className={`text-[8.5px] ${ui.txtMuted} font-mono uppercase truncate mt-0.5`}>{vid.snippet.channelTitle}</p>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5 shrink-0 w-full sm:w-auto justify-end">
                        <button
                          onClick={() => toggleFavoriteTrack(item)}
                          className={`min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl border ${isFav ? 'bg-amber-950/50 border-amber-800 text-amber-400' : `${ui.bgBase} border-zinc-800 text-zinc-400 hover:text-white`}`}
                          title={isFav ? "Remove Favorite" : "Save Favorite"}
                        >
                          {isFav ? <BookmarkCheck className="w-4 h-4"/> : <Bookmark className="w-4 h-4"/>}
                        </button>
                        <button 
                          onClick={() => setYtQueue([item])} 
                          className="min-h-[40px] px-4 bg-rose-600 text-white rounded-xl text-[9px] font-mono font-black uppercase hover:bg-rose-500 transition-colors shadow-md cursor-pointer active:scale-95 touch-manipulation"
                        >
                          {tx('play')}
                        </button>
                        <button 
                          onClick={() => setYtQueue(prev => [...prev, item])} 
                          className={`min-h-[40px] px-3 ${ui.bgBase} border ${ui.brd} ${ui.txtMuted} rounded-xl text-[9px] font-mono font-bold hover:${ui.txtMain} flex items-center justify-center gap-1 cursor-pointer active:scale-95 touch-manipulation`}
                        >
                          <ListPlus className="w-3.5 h-3.5"/> {tx('queue')}
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          )}

          {activeTab === "favorites" && (
            <div className={`${ui.bgCard} border ${ui.brd} rounded-3xl p-3 max-h-[360px] overflow-y-auto custom-scrollbar space-y-2`}>
              {savedFavorites.length === 0 ? (
                <div className="text-center py-8 text-xs font-mono text-zinc-500 uppercase">
                  No favorite music tracks bookmarked yet
                </div>
              ) : (
                savedFavorites.map((item, idx) => (
                  <div 
                    key={`${item.id}-${idx}`} 
                    className={`${ui.bgList} border ${ui.brd} p-2 rounded-xl flex items-center justify-between gap-3`}
                  >
                    <div className="flex gap-2.5 items-center truncate min-w-0">
                      <img src={item.thumb} alt="" className="w-12 h-9 object-cover rounded-lg border border-zinc-800 shrink-0" />
                      <div className="truncate">
                        <h5 className="text-[11px] font-bold text-white truncate">{item.title}</h5>
                        <p className="text-[8px] font-mono text-zinc-400 uppercase truncate">{item.channel}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        onClick={() => toggleFavoriteTrack(item)}
                        className="min-h-[36px] min-w-[36px] flex items-center justify-center rounded-lg bg-amber-950/50 border border-amber-800 text-amber-400"
                        title="Remove Favorite"
                      >
                        <BookmarkCheck className="w-3.5 h-3.5"/>
                      </button>
                      <button
                        onClick={() => setYtQueue(prev => [...prev, item])}
                        className="min-h-[36px] px-3 rounded-lg bg-zinc-900 border border-zinc-700 text-cyan-400 text-[8.5px] font-mono font-black uppercase flex items-center gap-1 cursor-pointer active:scale-95"
                      >
                        <ListPlus className="w-3 h-3"/> Queue
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {activeTab === "history" && (
            <div className={`${ui.bgCard} border ${ui.brd} rounded-3xl p-3 max-h-[360px] overflow-y-auto custom-scrollbar space-y-2`}>
              {recentHistory.length === 0 ? (
                <div className="text-center py-8 text-xs font-mono text-zinc-500 uppercase">
                  No played tracks recorded in recent sorties
                </div>
              ) : (
                recentHistory.map((item, idx) => (
                  <div 
                    key={`${item.id}-${idx}`} 
                    className={`${ui.bgList} border ${ui.brd} p-2 rounded-xl flex items-center justify-between gap-3`}
                  >
                    <div className="flex gap-2.5 items-center truncate min-w-0">
                      <img src={item.thumb} alt="" className="w-12 h-9 object-cover rounded-lg border border-zinc-800 shrink-0" />
                      <div className="truncate">
                        <h5 className="text-[11px] font-bold text-white truncate">{item.title}</h5>
                        <p className="text-[8px] font-mono text-zinc-400 uppercase truncate">{item.channel}</p>
                      </div>
                    </div>
                    <button
                      onClick={() => setYtQueue(prev => [...prev, item])}
                      className="min-h-[36px] px-3 rounded-lg bg-zinc-900 border border-zinc-700 text-cyan-400 text-[8.5px] font-mono font-black uppercase flex items-center gap-1 cursor-pointer active:scale-95 shrink-0"
                    >
                      <ListPlus className="w-3.5 h-3.5"/> Queue
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}