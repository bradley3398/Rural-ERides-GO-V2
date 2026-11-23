"use client";

import React, { useState, useEffect, useMemo } from "react";
import { 
  ShieldAlert, ShieldCheck, Users, Radio, Zap, AlertTriangle, 
  Trash2, Ban, CheckCircle2, Search, RefreshCw, Send, 
  Eye, Lock, Terminal, BarChart3, Globe, Award, MapPin, 
  Sliders, MessageSquare, Minimize2, Square, X, ExternalLink, Filter
} from "lucide-react";
import { 
  collection, onSnapshot, query, orderBy, doc, 
  updateDoc, deleteDoc, addDoc, serverTimestamp, increment 
} from "firebase/firestore";
import { db } from "./services/firebase";

export default function AdminCommandCenter() {
  const [activeTab, setActiveTab] = useState<"overview" | "moderation" | "pilots" | "broadcast" | "traps">("overview");
  const [posts, setPosts] = useState<any[]>([]);
  const [profiles, setProfiles] = useState<Record<string, any>>({});
  const [activeCallSessions, setActiveCallSessions] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedPost, setSelectedPost] = useState<any | null>(null);
  const [selectedPilot, setSelectedPilot] = useState<string | null>(null);
  const [broadcastMessage, setBroadcastMessage] = useState("");
  const [broadcastCategory, setBroadcastCategory] = useState("🚨 SYSTEM ALERT");
  const [isBroadcasting, setIsBroadcasting] = useState(false);

  // --- 1. REAL-TIME FIRESTORE DATA STREAMS ---
  useEffect(() => {
    // Stream all board posts
    const qPosts = query(collection(db, "board_posts"), orderBy("timestamp_epoch", "desc"));
    const unsubPosts = onSnapshot(qPosts, (snapshot) => {
      const livePosts = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
      setPosts(livePosts);
    });

    // Stream user profiles & statuses
    const unsubProfiles = onSnapshot(collection(db, "user_profiles"), (snapshot) => {
      const profMap: Record<string, any> = {};
      snapshot.docs.forEach(d => { profMap[d.id] = d.data(); });
      setProfiles(profMap);
    });

    // Stream active WebRTC calls
    const unsubCalls = onSnapshot(collection(db, "call_sessions"), (snapshot) => {
      const calls = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
      setActiveCallSessions(calls.filter((c: any) => c.status === "offering" || c.status === "answered"));
    });

    return () => {
      unsubPosts();
      unsubProfiles();
      unsubCalls();
    };
  }, []);

  // --- 2. ADMIN MUTATION PIPELINES ---
  const handlePurgePost = async (postId: string) => {
    if (!window.confirm(`[ADMIN CONFIRM] Permanently purge transmission ${postId} from Universal Feed?`)) return;
    try {
      await deleteDoc(doc(db, "board_posts", postId));
      setSelectedPost(null);
    } catch (e) {
      alert("Purge operation failed over network.");
    }
  };

  const handleToggleVerified = async (callsign: string, currentStatus: boolean) => {
    try {
      await updateDoc(doc(db, "user_profiles", callsign), {
        isVerifiedRider: !currentStatus,
        verifiedGrantedAt: serverTimestamp()
      });
    } catch (e) {
      alert("Failed to update verification status.");
    }
  };

  const handleSetTrustScore = async (callsign: string, score: number) => {
    try {
      await updateDoc(doc(db, "user_profiles", callsign), {
        trustScore: score
      });
    } catch (e) {
      alert("Failed to adjust trust score.");
    }
  };

  const handleTogglePilotBan = async (callsign: string, isBanned: boolean) => {
    const action = isBanned ? "PARDON" : "GROUND / BAN";
    if (!window.confirm(`[AUTHORITY CONFIRM] ${action} operator ${callsign}?`)) return;
    try {
      await updateDoc(doc(db, "user_profiles", callsign), {
        isGrounded: !isBanned,
        groundedReason: isBanned ? null : "Violation of Sector Flight Safety Protocols",
        groundedTimestamp: serverTimestamp()
      });
    } catch (e) {
      alert("Failed to modify pilot grounding lock.");
    }
  };

  const handleSendNetworkAlert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!broadcastMessage.trim() || isBroadcasting) return;
    setIsBroadcasting(true);

    try {
      // 1. Post to Board as High-Priority Banner
      await addDoc(collection(db, "board_posts"), {
        username: "SYSTEM_DISPATCH",
        pfpUrl: "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?q=80&w=200&auto=format&fit=crop",
        category: broadcastCategory,
        content: broadcastMessage.trim(),
        volts: 999,
        timestamp: new Date().toLocaleString(),
        timestamp_epoch: serverTimestamp(),
        isSystemAlert: true,
        reactions: { fire: 0, zap: 0, heart: 0, battery: 0 },
        comments: []
      });

      // 2. Transmit notification blast
      Object.keys(profiles).forEach(async (pilot) => {
        await addDoc(collection(db, "user_notifications"), {
          recipient: pilot,
          sender: "COMMAND_CENTER",
          type: "EMERGENCY_BROADCAST",
          content: `[ALERT]: ${broadcastMessage.trim()}`,
          read: false,
          timestamp: serverTimestamp()
        });
      });

      setBroadcastMessage("");
      alert("System broadcast successfully relayed across all pilot terminals.");
    } catch (e) {
      alert("Broadcast transmission failed.");
    } finally {
      setIsBroadcasting(false);
    }
  };

  // Filtered metrics
  const flaggedPosts = useMemo(() => posts.filter(p => p.isHelpNeeded || p.category === "Trail Rescue Bounty" || p.flagCount > 0), [posts]);
  const activeSpeedTraps = useMemo(() => posts.filter(p => p.isSpeedTrap || p.category === "Speed Trap Warning"), [posts]);

  return (
    <div className="flex flex-col h-screen w-screen bg-[#090a0f] text-zinc-200 select-none overflow-hidden font-sans">
      
      {/* WINDOWS TITLEBAR (Tauri / Electron Draggable Header) */}
      <div className="h-8 bg-black/90 border-b border-white/10 flex items-center justify-between px-3 shrink-0 text-xs font-mono select-none">
        <div className="flex items-center gap-2">
          <Terminal className="w-3.5 h-3.5 text-[#39ff14]" />
          <span className="font-black text-white tracking-widest uppercase">RURAL E-RIDES GO // WINDOWS ADMIN STATION</span>
          <span className="text-[10px] text-zinc-500 font-bold ml-2">NODE: OK-STIGLER-SAT-01</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <span className="text-[10px] text-emerald-400 font-bold uppercase">Uplink Nominal</span>
          <div className="flex items-center ml-4">
            <button className="h-8 w-10 flex items-center justify-center hover:bg-white/10 transition-colors"><Minimize2 className="w-3 h-3"/></button>
            <button className="h-8 w-10 flex items-center justify-center hover:bg-white/10 transition-colors"><Square className="w-3 h-3"/></button>
            <button className="h-8 w-10 flex items-center justify-center hover:bg-rose-600 hover:text-white transition-colors"><X className="w-3 h-3"/></button>
          </div>
        </div>
      </div>

      {/* WORKSPACE AREA */}
      <div className="flex flex-1 overflow-hidden">
        
        {/* SIDEBAR NAVIGATION */}
        <div className="w-64 bg-black/70 border-r border-white/10 flex flex-col justify-between shrink-0 p-3 space-y-4">
          <div className="space-y-1">
            <div className="px-3 py-2 text-[10px] font-black uppercase text-zinc-500 tracking-widest font-mono">Control Authority</div>
            
            <button onClick={() => setActiveTab("overview")} className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${activeTab === "overview" ? "bg-[#39ff14] text-black font-black shadow-lg" : "hover:bg-white/5 text-zinc-400 hover:text-white"}`}>
              <BarChart3 className="w-4 h-4"/> Mission Overview
            </button>
            
            <button onClick={() => setActiveTab("moderation")} className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${activeTab === "moderation" ? "bg-[#39ff14] text-black font-black shadow-lg" : "hover:bg-white/5 text-zinc-400 hover:text-white"}`}>
              <span className="flex items-center gap-3"><Radio className="w-4 h-4"/> Feed Purge &amp; Flags</span>
              {flaggedPosts.length > 0 && <span className="px-2 py-0.5 rounded-full text-[9px] font-black bg-rose-600 text-white font-mono">{flaggedPosts.length}</span>}
            </button>

            <button onClick={() => setActiveTab("pilots")} className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${activeTab === "pilots" ? "bg-[#39ff14] text-black font-black shadow-lg" : "hover:bg-white/5 text-zinc-400 hover:text-white"}`}>
              <Users className="w-4 h-4"/> Pilot Registry &amp; Bans
            </button>

            <button onClick={() => setActiveTab("broadcast")} className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${activeTab === "broadcast" ? "bg-[#39ff14] text-black font-black shadow-lg" : "hover:bg-white/5 text-zinc-400 hover:text-white"}`}>
              <Send className="w-4 h-4"/> Emergency Broadcast
            </button>

            <button onClick={() => setActiveTab("traps")} className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${activeTab === "traps" ? "bg-[#39ff14] text-black font-black shadow-lg" : "hover:bg-white/5 text-zinc-400 hover:text-white"}`}>
              <span className="flex items-center gap-3"><AlertTriangle className="w-4 h-4"/> Radar Speed Traps</span>
              {activeSpeedTraps.length > 0 && <span className="px-2 py-0.5 rounded-full text-[9px] font-black bg-amber-500 text-black font-mono">{activeSpeedTraps.length}</span>}
            </button>
          </div>

          <div className="bg-white/5 border border-white/10 rounded-2xl p-3 space-y-2 font-mono text-[10px]">
            <div className="flex justify-between"><span className="text-zinc-500">DATABASE:</span><span className="text-emerald-400 font-bold">CONNECTED</span></div>
            <div className="flex justify-between"><span className="text-zinc-500">ACTIVE P2P CALLS:</span><span className="text-cyan-400 font-bold">{activeCallSessions.length}</span></div>
            <div className="flex justify-between"><span className="text-zinc-500">TOTAL FEED POSTS:</span><span className="text-white font-bold">{posts.length}</span></div>
          </div>
        </div>

        {/* MAIN DISPLAY WORKSPACE */}
        <div className="flex-1 flex flex-col min-w-0 overflow-hidden bg-black/40">
          
          {/* ========================================================================= */}
          {/* TAB 1: OVERVIEW METRICS */}
          {/* ========================================================================= */}
          {activeTab === "overview" && (
            <div className="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar">
              <h2 className="text-lg font-black uppercase text-white tracking-wider flex items-center gap-2">
                <BarChart3 className="w-5 h-5 text-[#39ff14]"/> Global Sector Fleet Analytics
              </h2>

              <div className="grid grid-cols-4 gap-4">
                <div className="bg-zinc-950 border border-white/10 rounded-2xl p-4 space-y-1">
                  <span className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-widest block">Total Registered Pilots</span>
                  <span className="text-2xl font-mono font-black text-white">{Object.keys(profiles).length}</span>
                </div>
                <div className="bg-zinc-950 border border-white/10 rounded-2xl p-4 space-y-1">
                  <span className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-widest block">Active SOS Rescue Requests</span>
                  <span className="text-2xl font-mono font-black text-rose-500">{flaggedPosts.length}</span>
                </div>
                <div className="bg-zinc-950 border border-white/10 rounded-2xl p-4 space-y-1">
                  <span className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-widest block">Live Speed Traps Logged</span>
                  <span className="text-2xl font-mono font-black text-amber-400">{activeSpeedTraps.length}</span>
                </div>
                <div className="bg-zinc-950 border border-white/10 rounded-2xl p-4 space-y-1">
                  <span className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-widest block">WebRTC Video Streams</span>
                  <span className="text-2xl font-mono font-black text-cyan-400">{activeCallSessions.length}</span>
                </div>
              </div>

              {/* LIVE RECENT BROADCAST INGESTION STREAM */}
              <div className="bg-zinc-950 border border-white/10 rounded-2xl p-5 space-y-3">
                <span className="text-xs font-black uppercase tracking-widest text-zinc-400 font-mono block">Real-Time Transmission Telemetry Stream</span>
                <div className="space-y-2 max-h-96 overflow-y-auto custom-scrollbar pr-2">
                  {posts.slice(0, 10).map((post) => (
                    <div key={post.id} className="p-3 bg-white/5 border border-white/5 rounded-xl flex items-center justify-between gap-4 font-mono text-xs">
                      <div className="flex items-center gap-3 min-w-0">
                        <span className="px-2 py-0.5 rounded bg-black text-[#39ff14] text-[10px] font-bold shrink-0">{post.category || "General"}</span>
                        <span className="font-bold text-white shrink-0">{post.username}:</span>
                        <span className="text-zinc-400 truncate">{post.content}</span>
                      </div>
                      <div className="flex items-center gap-3 shrink-0 text-zinc-500 text-[10px]">
                        <span>⚡ {post.volts || 0}V</span>
                        <span>{post.timestamp || "Live"}</span>
                        <button onClick={() => handlePurgePost(post.id)} className="p-1 text-zinc-500 hover:text-rose-500"><Trash2 className="w-3.5 h-3.5"/></button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* ========================================================================= */}
          {/* TAB 2: FEED MODERATION & PURGE QUEUE */}
          {/* ========================================================================= */}
          {activeTab === "moderation" && (
            <div className="flex-1 flex min-h-0 overflow-hidden">
              <div className="w-1/2 border-r border-white/10 p-4 flex flex-col space-y-3">
                <div className="flex items-center justify-between shrink-0">
                  <span className="text-xs font-black uppercase text-zinc-400 font-mono">Transmission Queue ({posts.length})</span>
                  <div className="relative w-48">
                    <Search className="w-3.5 h-3.5 text-zinc-500 absolute left-3 top-2.5"/>
                    <input type="text" placeholder="Filter posts..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} className="w-full bg-black border border-white/10 rounded-lg pl-8 pr-2 py-1.5 text-xs text-white outline-none"/>
                  </div>
                </div>

                <div className="flex-1 overflow-y-auto space-y-2 custom-scrollbar pr-1">
                  {posts.filter(p => (p.content || "").toLowerCase().includes(searchQuery.toLowerCase()) || (p.username || "").toLowerCase().includes(searchQuery.toLowerCase())).map((post) => (
                    <div 
                      key={post.id} 
                      onClick={() => setSelectedPost(post)}
                      className={`p-3 rounded-xl border cursor-pointer transition-all ${selectedPost?.id === post.id ? "bg-white/10 border-[#39ff14]" : "bg-zinc-950 border-white/5 hover:border-white/20"}`}
                    >
                      <div className="flex justify-between items-center text-[10px] font-mono text-zinc-500 mb-1">
                        <span className="font-bold text-white">{post.username}</span>
                        <span>{post.timestamp}</span>
                      </div>
                      <p className="text-xs text-zinc-300 line-clamp-2 font-sans">{post.content}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* POST INSPECTION & PURGE DRAWER */}
              <div className="w-1/2 p-6 flex flex-col justify-between overflow-y-auto custom-scrollbar">
                {selectedPost ? (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between border-b border-white/10 pb-3">
                      <div>
                        <h3 className="text-sm font-black text-white uppercase">{selectedPost.username}</h3>
                        <span className="text-[10px] font-mono text-zinc-500">ID: {selectedPost.id}</span>
                      </div>
                      <span className="px-2.5 py-1 rounded-full text-xs font-mono font-black bg-white/10 text-cyan-400">{selectedPost.category || "General"}</span>
                    </div>

                    <div className="bg-black border border-white/10 p-4 rounded-xl text-sm font-sans text-white whitespace-pre-wrap leading-relaxed">
                      {selectedPost.content}
                    </div>

                    {selectedPost.imageUrl && (
                      <div className="rounded-xl overflow-hidden border border-white/10 max-h-60 bg-black">
                        <img src={selectedPost.imageUrl} alt="Attached Media" className="w-full h-full object-cover"/>
                      </div>
                    )}

                    <div className="grid grid-cols-2 gap-3 text-xs font-mono">
                      <div className="bg-zinc-900 p-3 rounded-xl border border-white/5">
                        <span className="text-[10px] text-zinc-500 block uppercase">Accumulated Volts</span>
                        <span className="text-lg font-black text-amber-400 font-mono">{selectedPost.volts || 0} ⚡</span>
                      </div>
                      <div className="bg-zinc-900 p-3 rounded-xl border border-white/5">
                        <span className="text-[10px] text-zinc-500 block uppercase">Reported Speed</span>
                        <span className="text-lg font-black text-emerald-400 font-mono">{selectedPost.telemetry?.speed || "None"}</span>
                      </div>
                    </div>

                    <div className="pt-4 border-t border-white/10 flex gap-3">
                      <button onClick={() => handlePurgePost(selectedPost.id)} className="flex-1 py-3 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-black uppercase tracking-wider transition-all flex items-center justify-center gap-2 cursor-pointer shadow-lg active:scale-95">
                        <Trash2 className="w-4 h-4"/> Purge From Network
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center h-full text-zinc-600 font-mono text-xs uppercase tracking-widest text-center">
                    Select a transmission from the left to inspect metadata or execute purge.
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ========================================================================= */}
          {/* TAB 3: PILOT AUTHORITY & REGISTRY */}
          {/* ========================================================================= */}
          {activeTab === "pilots" && (
            <div className="flex-1 flex min-h-0 overflow-hidden">
              <div className="w-1/2 border-r border-white/10 p-4 flex flex-col space-y-3">
                <span className="text-xs font-black uppercase text-zinc-400 font-mono">Registered Callsigns ({Object.keys(profiles).length})</span>
                <div className="flex-1 overflow-y-auto space-y-2 custom-scrollbar pr-1">
                  {Object.keys(profiles).map(callsign => {
                    const prof = profiles[callsign];
                    return (
                      <div 
                        key={callsign}
                        onClick={() => setSelectedPilot(callsign)}
                        className={`p-3 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${selectedPilot === callsign ? "bg-white/10 border-[#39ff14]" : "bg-zinc-950 border-white/5 hover:border-white/20"}`}
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-black border border-white/20 flex items-center justify-center font-bold text-white overflow-hidden text-xs">
                            {prof.pfpUrl ? <img src={prof.pfpUrl} alt="" className="w-full h-full object-cover"/> : callsign.charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <span className="text-xs font-black text-white flex items-center gap-1.5">
                              {callsign}
                              {prof.isVerifiedRider && <CheckCircle2 className="w-3.5 h-3.5 text-blue-400 fill-blue-400"/>}
                              {prof.isGrounded && <span className="px-1.5 py-0.2 rounded bg-rose-600 text-[8px] font-black text-white">GROUNDED</span>}
                            </span>
                            <span className="text-[9px] font-mono text-zinc-500">{prof.userFleet || "Universal PEV"}</span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* PILOT PRIVILEGES & PENALTY DRAWER */}
              <div className="w-1/2 p-6 overflow-y-auto custom-scrollbar">
                {selectedPilot && profiles[selectedPilot] ? (
                  <div className="space-y-5">
                    <div className="flex items-center justify-between border-b border-white/10 pb-4">
                      <div>
                        <h2 className="text-xl font-black text-white">{selectedPilot}</h2>
                        <span className="text-xs font-mono text-zinc-500">Profile Registry Node</span>
                      </div>
                      <button 
                        onClick={() => handleToggleVerified(selectedPilot, !!profiles[selectedPilot].isVerifiedRider)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-black uppercase transition-all cursor-pointer ${profiles[selectedPilot].isVerifiedRider ? "bg-blue-600 text-white shadow-md" : "bg-white/5 border border-white/10 text-zinc-400 hover:text-white"}`}
                      >
                        {profiles[selectedPilot].isVerifiedRider ? "Verified Rider: Active" : "Grant Verified Badge"}
                      </button>
                    </div>

                    <div className="space-y-2">
                      <span className="text-xs font-mono text-zinc-400 uppercase tracking-widest block">Trust Score Authority</span>
                      <div className="flex gap-2">
                        {[50, 75, 90, 99.9].map(score => (
                          <button 
                            key={score}
                            onClick={() => handleSetTrustScore(selectedPilot, score)}
                            className="px-3 py-1.5 bg-white/5 hover:bg-emerald-950/40 border border-white/10 hover:border-emerald-500/50 rounded-xl text-xs font-mono text-emerald-300 transition-all cursor-pointer"
                          >
                            Set {score}%
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="bg-zinc-950 border border-white/10 p-4 rounded-xl space-y-2 font-mono text-xs">
                      <span className="text-zinc-500 uppercase block font-bold text-[10px]">Registered Hardware Build</span>
                      <p className="text-zinc-300 italic">{profiles[selectedPilot].userBio || "No bio filed."}</p>
                      <div className="text-zinc-500 pt-2 border-t border-white/5">Fleet: {profiles[selectedPilot].userFleet || "Unspecified"}</div>
                    </div>

                    <div className="pt-4 border-t border-white/10">
                      <button 
                        onClick={() => handleTogglePilotBan(selectedPilot, !!profiles[selectedPilot].isGrounded)}
                        className={`w-full py-3 rounded-xl text-xs font-black uppercase tracking-wider transition-all flex items-center justify-center gap-2 cursor-pointer shadow-lg active:scale-95 ${
                          profiles[selectedPilot].isGrounded ? "bg-emerald-600 hover:bg-emerald-500 text-black" : "bg-rose-600 hover:bg-rose-500 text-white"
                        }`}
                      >
                        <Ban className="w-4 h-4"/> {profiles[selectedPilot].isGrounded ? "Pardon & Restore Access" : "Ground Pilot / Lock Comms"}
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center h-full text-zinc-600 font-mono text-xs uppercase tracking-widest text-center">
                    Select a pilot callsign to adjust authority parameters or issue penalties.
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ========================================================================= */}
          {/* TAB 4: EMERGENCY BROADCAST CONSOLE */}
          {/* ========================================================================= */}
          {activeTab === "broadcast" && (
            <div className="p-8 max-w-xl mx-auto w-full space-y-6">
              <div>
                <h2 className="text-lg font-black uppercase text-white flex items-center gap-2">
                  <Send className="w-5 h-5 text-[#39ff14]"/> Emergency Network Broadcast Transmitter
                </h2>
                <p className="text-xs text-zinc-500 font-mono mt-1">
                  Relays high-priority alerts to every active pilot terminal and pushes a priority banner to the Universal Feed.
                </p>
              </div>

              <form onSubmit={handleSendNetworkAlert} className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-mono text-zinc-400 uppercase font-black tracking-widest">Alert Classification</label>
                  <select 
                    value={broadcastCategory} 
                    onChange={e => setBroadcastCategory(e.target.value)}
                    className="w-full bg-zinc-950 border border-white/10 rounded-xl px-4 py-2.5 text-xs text-white outline-none font-bold"
                  >
                    <option value="🚨 SYSTEM ALERT">🚨 SYSTEM ALERT (Severe Weather / Sector Threat)</option>
                    <option value="⚠️ TRAIL CLOSURE">⚠️ TRAIL CLOSURE (Washout / Forestry Warning)</option>
                    <option value="⚡ VOLT BOUNTY EVENT">⚡ VOLT BOUNTY EVENT (Community XP Bonus)</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-mono text-zinc-400 uppercase font-black tracking-widest">Broadcast Transmission Content</label>
                  <textarea 
                    rows={5}
                    value={broadcastMessage}
                    onChange={e => setBroadcastMessage(e.target.value)}
                    placeholder="Enter urgent notification payload to blast across network..."
                    className="w-full bg-zinc-950 border border-white/10 rounded-xl p-4 text-sm text-white font-sans outline-none focus:border-[#39ff14] transition-colors"
                  />
                </div>

                <button 
                  type="submit" 
                  disabled={!broadcastMessage.trim() || isBroadcasting}
                  className="w-full py-3.5 bg-[#39ff14] hover:bg-[#32e012] text-black rounded-xl text-xs font-black uppercase tracking-widest transition-all cursor-pointer active:scale-98 shadow-xl disabled:opacity-50 flex items-center justify-center gap-2"
                >
                  <Send className="w-4 h-4"/> {isBroadcasting ? "Transmitting Blast..." : "Deploy Sector Broadcast"}
                </button>
              </form>
            </div>
          )}

          {/* ========================================================================= */}
          {/* TAB 5: RADAR SPEED TRAPS OVERVIEW */}
          {/* ========================================================================= */}
          {activeTab === "traps" && (
            <div className="p-6 space-y-4 overflow-y-auto custom-scrollbar">
              <h2 className="text-base font-black uppercase text-white flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-amber-400"/> Live Radar Speed Traps &amp; Hazards
              </h2>

              <div className="grid grid-cols-1 gap-3">
                {activeSpeedTraps.length === 0 ? (
                  <div className="p-8 text-center text-zinc-600 font-mono text-xs uppercase">No active radar speed traps reported in sector.</div>
                ) : (
                  activeSpeedTraps.map(trap => (
                    <div key={trap.id} className="p-4 bg-amber-950/20 border border-amber-500/30 rounded-xl flex items-center justify-between gap-4 font-mono text-xs">
                      <div>
                        <span className="text-amber-400 font-black uppercase block">Speed Trap Warning Reported</span>
                        <p className="text-zinc-300 font-sans mt-0.5">{trap.content}</p>
                        <span className="text-[10px] text-zinc-500 mt-1 block">Reported by @{trap.username} at {trap.timestamp}</span>
                      </div>
                      <button onClick={() => handlePurgePost(trap.id)} className="px-3 py-1.5 bg-rose-600 hover:bg-rose-500 text-white rounded-lg font-black uppercase text-[10px] tracking-wider transition-all cursor-pointer">
                        Clear Trap
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}