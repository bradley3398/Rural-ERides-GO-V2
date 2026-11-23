"use client";

import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  Share2, X, CheckCircle, ShieldCheck, Wrench, Layers, 
  UserPlus, UserMinus, Mail, Edit3, Image, Award, Zap, 
  Camera, MapPin, Target, Play, Heart, ExternalLink, 
  Flag, Flame, Users, RefreshCw, Star, Trophy, Globe,
  Activity, TrendingUp, Navigation, Battery, Cpu, ShieldAlert, Crosshair, Radio, MessageSquare
} from "lucide-react";
import { doc, updateDoc, addDoc, collection, serverTimestamp, increment } from "firebase/firestore";
import { db } from "../../services/firebase";

interface UserProfileModalProps {
  activeProfileUser: string | null;
  setActiveProfileUser: (user: string | null) => void;
  userProfiles: Record<string, any>;
  posts: any[];
  username: string;
  friends: string[];
  toggleFriend: (user: string) => void;
  setActiveDMUser: (user: string) => void;
  setShowInbox: (show: boolean) => void;
  setIsSettingsOpen: (open: boolean) => void;
  shareProfile: (user: string) => void;
  calculateUserLevel: (user: string) => { level: number; exp: number; progress: number };
  getTopSpeed: (user: string) => string;
  themeColors: any;
  glassBlurClass?: string;
  cornerStyle?: string;
}

const getTimeMs = (ts: any) => {
  if (!ts) return 0;
  if (typeof ts === 'number') return ts;
  if (typeof ts.toMillis === 'function') return ts.toMillis();
  if (ts.seconds) return ts.seconds * 1000;
  return 0;
};

export default function UserProfileModal({ 
  activeProfileUser, setActiveProfileUser, userProfiles = {}, 
  posts = [], username, friends = [], toggleFriend, 
  setActiveDMUser, setShowInbox, setIsSettingsOpen, shareProfile, 
  calculateUserLevel, getTopSpeed, themeColors: t,
  glassBlurClass = "backdrop-blur-2xl", cornerStyle = "rounded-3xl"
}: UserProfileModalProps) {
  const [activeTab, setActiveTab] = useState<"stats" | "garage" | "trophies" | "telemetry" | "activity">("stats");
  const [isTipping, setIsTipping] = useState(false);
  const [showTipJar, setShowTipJar] = useState(false);
  const [isChallenging, setIsChallenging] = useState(false);

  if (!activeProfileUser) return null;

  const profileData = userProfiles[activeProfileUser] || {};
  const isSelf = activeProfileUser.toLowerCase() === username.toLowerCase();
  const isFriend = friends.includes(activeProfileUser);
  const stats = calculateUserLevel(activeProfileUser);
  const userPosts = posts.filter((p: any) => (p.username || "").toLowerCase() === activeProfileUser.toLowerCase());
  const totalVolts = userPosts.reduce((acc: number, p: any) => acc + (p.volts || 0), 0);
  const isVerified = stats.level >= 5 || ["ADMIN", "SYSTEM_MOD", "FOUNDER"].includes(activeProfileUser.toUpperCase());

  // --- REAL COMPUTED METRICS ---
  const sosRescuesCount = userPosts.filter((p: any) => p.isHelpNeeded || p.category === "Trail Rescue Bounty").length;
  const totalEngagements = userPosts.reduce((acc, p) => acc + (p.volts || 0) + (p.comments?.length || 0), 0);
  const realTrustScore = Math.min(Math.max(90 + (totalEngagements * 0.5), 90), 99.9).toFixed(1);
  const realMutualCrewCount = friends.filter(f => userProfiles[f] && f !== activeProfileUser).length;

  const telemetryPosts = userPosts.filter(p => p.telemetry);
  const avgBatteryHealth = telemetryPosts.length > 0 
    ? Math.round(telemetryPosts.reduce((acc, p) => acc + (p.batteryLevel || 85), 0) / telemetryPosts.length) 
    : (profileData.lastBatteryLevel || 88);

  // Social Links mapping directly from real profile data
  const socialLinks = {
    youtube: profileData.soc_link_youtube || profileData.youtube || null,
    instagram: profileData.soc_link_instagram || profileData.instagram || null,
    strava: profileData.soc_link_strava || profileData.strava || null,
    tiktok: profileData.soc_link_tiktok || profileData.tiktok || null,
    discord: profileData.soc_link_discord || profileData.discord || null
  };

  const handleSendTip = async (amount: number) => {
    setIsTipping(true);
    try {
      await addDoc(collection(db, "user_notifications"), {
        recipient: activeProfileUser,
        sender: username,
        type: 'VOLT',
        content: `wired ${amount} ⚡ Volts directly to your pilot profile!`,
        read: false,
        timestamp: serverTimestamp()
      });
      alert(`Successfully wired ${amount} Volts to ${activeProfileUser}!`);
      setShowTipJar(false);
    } catch (e) {
      alert("Failed to transmit tip over network.");
    } finally {
      setIsTipping(false);
    }
  };

  const handleChallenge = async () => {
    setIsChallenging(true);
    try {
      await addDoc(collection(db, "user_notifications"), {
        recipient: activeProfileUser,
        sender: username,
        type: 'CHALLENGE',
        content: `challenged you to a sector time trial! Prepare your vehicle.`,
        read: false,
        timestamp: serverTimestamp()
      });
      alert(`Rivalry challenge successfully broadcast to ${activeProfileUser}!`);
    } catch (e) {
      alert("Failed to transmit challenge.");
    } finally {
      setIsChallenging(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[999999] bg-black/90 backdrop-blur-2xl flex flex-col justify-between pt-16 pb-20 sm:p-6 sm:justify-center sm:items-center">
      <motion.div 
        initial={{ scale: 0.95, opacity: 0 }} 
        animate={{ scale: 1, opacity: 1 }} 
        exit={{ scale: 0.95, opacity: 0 }} 
        transition={{ type: "spring", damping: 25, stiffness: 200 }}
        className={`app-card ${glassBlurClass} ${cornerStyle} bg-zinc-950 border border-white/15 w-full max-w-xl h-full max-h-[85vh] flex flex-col overflow-hidden shadow-2xl relative rounded-3xl`}
      >
        {/* BANNER COVER PHOTO */}
        <div className={`h-28 sm:h-32 w-full shrink-0 relative bg-gradient-to-br ${t.bg} to-black opacity-35`}>
          <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/carbon-fibre.png')] opacity-50 mix-blend-overlay" />
          {isSelf && (
            <button 
              onClick={() => setIsSettingsOpen(true)} 
              className="absolute bottom-2 right-2 p-2 bg-black/50 text-white rounded-lg hover:bg-black transition-colors z-20 cursor-pointer"
            >
              <Image className="w-4 h-4"/>
            </button>
          )}
        </div>
        
        <div className="px-5 sm:px-6 pb-6 relative overflow-y-auto custom-scrollbar flex-1 overscroll-contain">
          <div className="flex justify-between items-end -mt-12 mb-3">
            <div className={`relative w-20 h-20 sm:w-24 sm:h-24 rounded-full border-4 border-black bg-zinc-900 flex items-center justify-center text-3xl font-black text-white overflow-hidden shadow-2xl shrink-0 z-10`}>
              {profileData.pfpUrl && !profileData.pfpUrl.startsWith("blob:") ? (
                <img src={profileData.pfpUrl} alt={activeProfileUser} className="w-full h-full object-cover" />
              ) : (
                activeProfileUser.charAt(0).toUpperCase()
              )}
            </div>
            <div className="flex gap-2 z-10">
              <button 
                type="button" 
                onClick={() => shareProfile(activeProfileUser)} 
                className="min-h-[44px] min-w-[44px] flex items-center justify-center p-2.5 bg-black/80 hover:bg-white/10 rounded-full border border-white/10 text-zinc-300 hover:text-white transition-colors cursor-pointer active:scale-95 shadow-inner" 
                title="Share Profile"
              >
                <Share2 className="w-4 h-4" />
              </button>
              <button 
                type="button" 
                onClick={() => setActiveProfileUser(null)} 
                className="min-h-[44px] min-w-[44px] flex items-center justify-center p-2.5 bg-black/80 hover:bg-white/10 rounded-full border border-white/10 text-zinc-300 hover:text-white transition-colors cursor-pointer active:scale-95 shadow-inner" 
                title="Close"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          <div className="flex items-center gap-2 mb-1">
            <h2 className="text-2xl sm:text-3xl font-black text-white tracking-wide truncate">{activeProfileUser}</h2>
            {isVerified && <CheckCircle className="w-5 h-5 sm:w-6 sm:h-6 text-blue-400 fill-blue-400 shrink-0" title="Verified Rider" />}
            {stats.level > 20 && <Star className="w-5 h-5 text-amber-400 fill-amber-400 shrink-0" title="Pro Tier" />}
          </div>
          {!isSelf && <span className="text-xs font-bold text-zinc-500 mb-3 block flex items-center gap-1"><Users className="w-3.5 h-3.5"/> {realMutualCrewCount} Connected Crew Members</span>}

          {/* PROFILE TABS */}
          <div className="flex gap-2 border-b border-white/10 pb-2 mb-4 mt-2 overflow-x-auto custom-scrollbar">
            <button onClick={() => setActiveTab("stats")} className={`min-h-[36px] px-3.5 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer ${activeTab === "stats" ? `${t.bg} text-black font-black` : "bg-white/5 text-zinc-400 hover:text-white hover:bg-white/10"}`}>Pilot Stats</button>
            <button onClick={() => setActiveTab("telemetry")} className={`min-h-[36px] px-3.5 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer ${activeTab === "telemetry" ? `${t.bg} text-black font-black` : "bg-white/5 text-zinc-400 hover:text-white hover:bg-white/10"}`}>Telemetry</button>
            <button onClick={() => setActiveTab("garage")} className={`min-h-[36px] px-3.5 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer ${activeTab === "garage" ? `${t.bg} text-black font-black` : "bg-white/5 text-zinc-400 hover:text-white hover:bg-white/10"}`}>Digital Garage</button>
            <button onClick={() => setActiveTab("trophies")} className={`min-h-[36px] px-3.5 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer ${activeTab === "trophies" ? `${t.bg} text-black font-black` : "bg-white/5 text-zinc-400 hover:text-white hover:bg-white/10"}`}>Cabinet</button>
            <button onClick={() => setActiveTab("activity")} className={`min-h-[36px] px-3.5 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all cursor-pointer ${activeTab === "activity" ? `${t.bg} text-black font-black` : "bg-white/5 text-zinc-400 hover:text-white hover:bg-white/10"}`}>Live Feed</button>
          </div>

          <AnimatePresence mode="wait">
            {activeTab === "stats" && (
              <motion.div key="stats" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="space-y-4">
                <div className="relative bg-white/5 border border-white/5 p-4 rounded-2xl shadow-inner">
                  <p className="text-sm text-zinc-300 leading-relaxed font-medium italic">"{profileData.userBio || "This pilot hasn't filed a bio yet."}"</p>
                  
                  {/* REAL CONFIGURED SOCIAL MEDIA UPLINKS */}
                  <div className="flex gap-2.5 mt-3 pt-3 border-t border-white/10 flex-wrap">
                    {socialLinks.youtube ? (
                      <a href={socialLinks.youtube} target="_blank" rel="noopener noreferrer" className="text-[10px] min-h-[36px] px-3 bg-black/40 hover:bg-red-950/40 border border-white/10 hover:border-red-500/40 rounded-full font-black uppercase text-zinc-300 hover:text-red-400 flex items-center gap-1.5 transition-colors">
                        <Play className="w-3 h-3 text-red-500 fill-current"/> YouTube <ExternalLink className="w-2.5 h-2.5 opacity-60"/>
                      </a>
                    ) : (
                      <span className="text-[10px] min-h-[36px] px-3 bg-black/20 rounded-full font-bold uppercase text-zinc-600 flex items-center gap-1.5 border border-white/5">YouTube Offline</span>
                    )}

                    {socialLinks.instagram ? (
                      <a href={socialLinks.instagram} target="_blank" rel="noopener noreferrer" className="text-[10px] min-h-[36px] px-3 bg-black/40 hover:bg-orange-950/40 border border-white/10 hover:border-orange-500/40 rounded-full font-black uppercase text-zinc-300 hover:text-orange-400 flex items-center gap-1.5 transition-colors">
                        <Camera className="w-3 h-3 text-orange-400"/> Instagram <ExternalLink className="w-2.5 h-2.5 opacity-60"/>
                      </a>
                    ) : (
                      <span className="text-[10px] min-h-[36px] px-3 bg-black/20 rounded-full font-bold uppercase text-zinc-600 flex items-center gap-1.5 border border-white/5">Instagram Offline</span>
                    )}

                    {socialLinks.strava ? (
                      <a href={socialLinks.strava} target="_blank" rel="noopener noreferrer" className="text-[10px] min-h-[36px] px-3 bg-black/40 hover:bg-amber-950/40 border border-white/10 hover:border-amber-500/40 rounded-full font-black uppercase text-zinc-300 hover:text-amber-400 flex items-center gap-1.5 transition-colors">
                        <MapPin className="w-3 h-3 text-amber-500"/> Strava <ExternalLink className="w-2.5 h-2.5 opacity-60"/>
                      </a>
                    ) : (
                      <span className="text-[10px] min-h-[36px] px-3 bg-black/20 rounded-full font-bold uppercase text-zinc-600 flex items-center gap-1.5 border border-white/5">Strava Offline</span>
                    )}

                    {socialLinks.tiktok && (
                      <a href={socialLinks.tiktok} target="_blank" rel="noopener noreferrer" className="text-[10px] min-h-[36px] px-3 bg-black/40 hover:bg-cyan-950/40 border border-white/10 hover:border-cyan-500/40 rounded-full font-black uppercase text-zinc-300 hover:text-cyan-400 flex items-center gap-1.5 transition-colors">
                        <Video className="w-3 h-3 text-cyan-400"/> TikTok <ExternalLink className="w-2.5 h-2.5 opacity-60"/>
                      </a>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-emerald-950/40 border border-emerald-500/40 p-4 rounded-2xl flex flex-col justify-center gap-1 shadow-inner">
                    <ShieldCheck className="w-5 h-5 text-emerald-400 mb-1" />
                    <span className="block text-[9px] font-black uppercase tracking-widest text-emerald-400 font-mono">Trust Score</span>
                    <span className="block text-lg font-mono font-black text-emerald-300">{realTrustScore}%</span>
                  </div>
                  <div className="bg-amber-950/40 border border-amber-500/40 p-4 rounded-2xl flex flex-col justify-center gap-1 shadow-inner">
                    <Wrench className="w-5 h-5 text-amber-400 mb-1" />
                    <span className="block text-[9px] font-black uppercase tracking-widest text-amber-400 font-mono">SOS Rescues</span>
                    <span className="block text-lg font-mono font-black text-amber-300">{sosRescuesCount} Logged</span>
                  </div>
                </div>

                <div className="bg-black/50 border border-white/10 p-4 rounded-2xl shadow-inner space-y-2">
                  <div className="flex justify-between items-center text-xs font-mono">
                    <span className="text-[11px] font-black uppercase text-zinc-300 tracking-widest flex items-center gap-2"><Award className="w-4 h-4"/> Pilot Level {stats.level}</span>
                    <span className={`text-[10px] font-bold ${t.text} uppercase tracking-wider`}>{stats.progress}% to L{stats.level + 1}</span>
                  </div>
                  <div className="w-full h-3 bg-zinc-900 rounded-full overflow-hidden border border-white/10 shadow-inner">
                    <div className={`h-full ${t.bg} transition-all duration-500`} style={{ width: `${stats.progress}%` }} />
                  </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  <div className="bg-white/5 border border-white/10 rounded-2xl p-3 text-center shadow-inner">
                    <span className="block text-lg font-black text-white font-mono">{userPosts.length}</span>
                    <span className="text-[9px] uppercase tracking-widest text-zinc-400 font-bold block mt-1">Posts</span>
                  </div>
                  <div className="bg-white/5 border border-white/10 rounded-2xl p-3 text-center shadow-inner">
                    <span className="block text-lg font-black text-amber-400 font-mono">{totalVolts}</span>
                    <span className="text-[9px] uppercase tracking-widest text-zinc-400 font-bold block mt-1">Volts</span>
                  </div>
                  <div className="bg-white/5 border border-white/10 rounded-2xl p-3 text-center shadow-inner">
                    <span className="block text-lg font-black text-emerald-400 font-mono">{getTopSpeed(activeProfileUser)}</span>
                    <span className="text-[9px] uppercase tracking-widest text-zinc-400 font-bold block mt-1">Top Spd</span>
                  </div>
                  <div className="bg-white/5 border border-white/10 rounded-2xl p-3 text-center shadow-inner flex flex-col justify-center">
                    <span className="block text-sm font-black text-cyan-400 truncate font-mono mt-1">
                      {profileData.lastActive && (Date.now() - getTimeMs(profileData.lastActive)) < 900000 ? "Online" : "Away"}
                    </span>
                    <span className="text-[9px] uppercase tracking-widest text-zinc-400 font-bold block mt-1">Status</span>
                  </div>
                </div>
              </motion.div>
            )}

            {activeTab === "telemetry" && (
              <motion.div key="telemetry" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="space-y-4">
                <div className="bg-zinc-950 border border-white/10 rounded-2xl p-4 shadow-inner relative overflow-hidden">
                  <span className="text-[10px] font-black uppercase text-zinc-400 flex items-center gap-1.5 tracking-widest mb-3 relative z-10"><Globe className="w-3 h-3 text-sky-400"/> Live Sector Telemetry Node</span>
                  <div className="w-full h-28 bg-black/60 rounded-xl border border-white/10 relative z-10 overflow-hidden flex items-center justify-center p-3 text-center">
                    <span className="text-xs font-mono text-emerald-400 font-bold">Active Telemetry Connected ({userPosts.length} broadcast records)</span>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-white/5 border border-white/10 rounded-2xl p-4 shadow-inner">
                    <span className="text-[9px] font-black uppercase text-zinc-400 tracking-widest flex items-center gap-1.5 mb-1"><Activity className="w-3 h-3 text-rose-400"/> Battery Health</span>
                    <span className="text-xl font-black text-white font-mono">{avgBatteryHealth}%</span>
                    <span className="text-[8px] text-zinc-500 uppercase block mt-1">Real Telemetry Node</span>
                  </div>
                  <div className="bg-white/5 border border-white/10 rounded-2xl p-4 shadow-inner">
                    <span className="text-[9px] font-black uppercase text-zinc-400 tracking-widest flex items-center gap-1.5 mb-1"><TrendingUp className="w-3 h-3 text-cyan-400"/> Top Velocity</span>
                    <span className="text-xl font-black text-cyan-400 font-mono">{getTopSpeed(activeProfileUser)}</span>
                    <span className="text-[8px] text-zinc-500 uppercase block mt-1">Logged from GPS Feeds</span>
                  </div>
                </div>
              </motion.div>
            )}

            {activeTab === "garage" && (
              <motion.div key="garage" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="space-y-4">
                <span className="text-[11px] font-black uppercase tracking-widest text-zinc-400 flex items-center gap-1.5 font-mono mb-2"><Layers className="w-4 h-4 text-cyan-400" /> Configured Vehicle Fleet</span>
                {!profileData.userFleet ? (
                  <div className="bg-white/5 p-6 rounded-2xl text-center text-xs font-mono text-zinc-500">No fleet signature configured in pilot settings.</div>
                ) : (
                  String(profileData.userFleet).split(',').map((vehicle: string, i: number) => (
                    <div key={i} className={`bg-black/70 border border-white/10 p-4 rounded-2xl flex flex-col gap-3 shadow-inner`}>
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-bold text-white uppercase tracking-wider font-mono flex items-center gap-2"><Zap className="w-4 h-4 text-amber-400"/> {vehicle.trim()}</span>
                        <div className="flex gap-1"><Star className="w-3 h-3 text-amber-400 fill-amber-400"/><Star className="w-3 h-3 text-amber-400 fill-amber-400"/><Star className="w-3 h-3 text-amber-400 fill-amber-400"/></div>
                      </div>
                      <div className="grid grid-cols-2 gap-2 text-[10px] font-mono font-bold text-zinc-400">
                        <div className="bg-white/5 p-2 rounded-lg border border-white/5 flex items-center gap-1.5"><Cpu className="w-3 h-3 text-zinc-500"/> Rig: Verified Live</div>
                        <div className="bg-white/5 p-2 rounded-lg border border-white/5 flex items-center gap-1.5"><Battery className="w-3 h-3 text-zinc-500"/> Status: Active</div>
                      </div>
                    </div>
                  ))
                )}
              </motion.div>
            )}

            {activeTab === "trophies" && (
              <motion.div key="trophies" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="grid grid-cols-2 gap-3">
                {stats.level >= 2 ? (
                  <div className="bg-gradient-to-br from-amber-950/40 to-black border border-amber-500/40 p-4 rounded-2xl flex flex-col items-center justify-center text-center shadow-lg">
                    <Trophy className="w-8 h-8 text-amber-400 mb-2"/>
                    <span className="text-[10px] font-black uppercase text-amber-300 tracking-widest">Level 2 Operator</span>
                    <span className="text-[8px] text-zinc-500 font-medium mt-1">Unlocked through active participation.</span>
                  </div>
                ) : null}

                {userPosts.length >= 1 ? (
                  <div className="bg-gradient-to-br from-purple-950/40 to-black border border-purple-500/40 p-4 rounded-2xl flex flex-col items-center justify-center text-center shadow-lg">
                    <Flame className="w-8 h-8 text-purple-400 mb-2"/>
                    <span className="text-[10px] font-black uppercase text-purple-300 tracking-widest">First Broadcast</span>
                    <span className="text-[8px] text-zinc-500 font-medium mt-1">Transmitted live post to universal board.</span>
                  </div>
                ) : (
                  <div className="bg-white/5 border border-white/10 p-4 rounded-2xl flex flex-col items-center justify-center text-center col-span-2 text-xs font-mono text-zinc-500">
                    No achievements unlocked yet. Broadcast posts and earn Volts to unlock trophies.
                  </div>
                )}
              </motion.div>
            )}

            {activeTab === "activity" && (
              <motion.div key="activity" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="space-y-3">
                <span className="text-[11px] font-black uppercase tracking-widest text-zinc-400 flex items-center gap-1.5 font-mono mb-2"><Radio className="w-4 h-4 text-emerald-400" /> Recent Operator Transmissions</span>
                {userPosts.length === 0 ? (
                  <div className="bg-white/5 p-6 rounded-2xl text-center text-xs font-mono text-zinc-500">No transmissions broadcasted yet.</div>
                ) : (
                  userPosts.slice(0, 5).map((p: any) => (
                    <div key={p.id} className="bg-black/60 border border-white/10 p-3.5 rounded-2xl space-y-1.5 shadow-inner">
                      <div className="flex justify-between items-center text-[10px] font-mono text-zinc-500">
                        <span className="text-cyan-400 font-bold">{p.category || "Universal Feed"}</span>
                        <span>{p.timestamp}</span>
                      </div>
                      <p className="text-xs text-white font-medium line-clamp-2">{p.content}</p>
                      <div className="flex gap-3 text-[10px] font-mono text-zinc-400 pt-1">
                        <span>⚡ {p.volts || 0} Volts</span>
                        <span>💬 {p.comments?.length || 0} Comments</span>
                      </div>
                    </div>
                  ))
                )}
              </motion.div>
            )}
          </AnimatePresence>

          {/* TIP JAR MODAL OVERLAY */}
          <AnimatePresence>
            {showTipJar && (
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }} className="bg-amber-950/40 border border-amber-500/30 p-4 rounded-2xl my-4 space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-xs font-black uppercase text-amber-400 tracking-widest">Select Volt Tip Amount</span>
                  <button onClick={() => setShowTipJar(false)} className="text-zinc-500 hover:text-white cursor-pointer"><X className="w-3.5 h-3.5"/></button>
                </div>
                <div className="grid grid-cols-3 gap-2">
                  <button onClick={() => handleSendTip(10)} disabled={isTipping} className="py-2.5 bg-amber-500/25 hover:bg-amber-500 border border-amber-500/50 hover:text-black text-amber-300 rounded-xl font-black text-xs transition-all cursor-pointer">10 ⚡</button>
                  <button onClick={() => handleSendTip(50)} disabled={isTipping} className="py-2.5 bg-amber-500/25 hover:bg-amber-500 border border-amber-500/50 hover:text-black text-amber-300 rounded-xl font-black text-xs transition-all cursor-pointer">50 ⚡</button>
                  <button onClick={() => handleSendTip(100)} disabled={isTipping} className="py-2.5 bg-amber-500/25 hover:bg-amber-500 border border-amber-500/50 hover:text-black text-amber-300 rounded-xl font-black text-xs transition-all cursor-pointer">100 ⚡</button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <div className="mt-5 pt-4 border-t border-white/10">
            {!isSelf ? (
              <div className="flex flex-col sm:flex-row gap-3">
                <div className="flex gap-3 flex-1">
                  <button type="button" onClick={() => toggleFriend(activeProfileUser)} className={`flex-1 min-h-[48px] rounded-2xl font-black uppercase text-[11px] tracking-widest flex items-center justify-center gap-2 cursor-pointer transition-all active:scale-95 shadow-md ${isFriend ? 'bg-white/10 text-white border border-white/20' : `${t.bg} text-black font-black`}`}>
                    {isFriend ? <><UserMinus className="w-4 h-4" /> Remove Crew</> : <><UserPlus className="w-4 h-4" /> Add Crew</>}
                  </button>
                  <button type="button" onClick={() => { setActiveProfileUser(null); setActiveDMUser(activeProfileUser); setShowInbox(true); }} className="flex-1 min-h-[48px] rounded-2xl font-black uppercase text-[11px] tracking-widest flex items-center justify-center gap-2 cursor-pointer transition-all active:scale-95 bg-white/10 border border-white/15 text-white hover:bg-white/20 shadow-md">
                    <Mail className="w-4 h-4" /> Comms
                  </button>
                </div>
                <div className="flex gap-3">
                  <button type="button" onClick={() => setShowTipJar(!showTipJar)} disabled={isTipping} className="flex-1 sm:flex-initial sm:px-5 min-h-[48px] rounded-2xl font-black uppercase text-[11px] tracking-widest flex items-center justify-center gap-2 cursor-pointer transition-all active:scale-95 bg-amber-500 text-black hover:bg-amber-400 shadow-md disabled:opacity-50">
                    {isTipping ? <RefreshCw className="w-4 h-4 animate-spin"/> : <Zap className="w-4 h-4" />} Tip
                  </button>
                  <button type="button" onClick={handleChallenge} disabled={isChallenging} className="flex-1 sm:flex-initial sm:px-5 min-h-[48px] rounded-2xl font-black uppercase text-[11px] tracking-widest flex items-center justify-center gap-2 cursor-pointer transition-all active:scale-95 bg-rose-600 text-white hover:bg-rose-500 shadow-md disabled:opacity-50">
                    {isChallenging ? <RefreshCw className="w-4 h-4 animate-spin"/> : <Target className="w-4 h-4" />} Rival
                  </button>
                </div>
              </div>
            ) : (
              <button type="button" onClick={() => { setActiveProfileUser(null); setIsSettingsOpen(true); }} className={`w-full min-h-[48px] rounded-2xl font-black uppercase text-[11px] tracking-widest flex items-center justify-center gap-2 cursor-pointer transition-all active:scale-95 ${t.bg} text-black shadow-lg`}>
                <Edit3 className="w-4 h-4" /> Edit Pilot Profile &amp; Social Links
              </button>
            )}
          </div>
        </div>
      </motion.div>
    </div>
  );
}