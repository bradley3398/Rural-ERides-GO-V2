"use client";

import React, { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { RadioReceiver, Sliders, Zap, Signal, Mic, Activity, Globe, ShieldAlert, Cpu, Radio } from "lucide-react";
import { doc, onSnapshot, setDoc, updateDoc, serverTimestamp, collection, query, where, getDocs, deleteDoc } from "firebase/firestore";
import { db } from "../../services/firebase";

// WebRTC Configuration
const RTC_CONFIG: RTCConfiguration = { 
  iceServers: [{ urls: ["stun:stun.l.google.com:19302"] }], 
  iceCandidatePoolSize: 10 
};

export default function WalkieTalkieWidget({
  activeChannel, setActiveChannel, squelchLevel, setSquelchLevel,
  micBoost, setMicBoost, walkieFeed = [], getTimeAgo, 
  isRecording, startRecording, stopRecording,
  t, bgPanel, brd, txtMain, txtMuted, bgInput, bgList, bgCard,
  globalVolume = 100, glassBlurClass = "backdrop-blur-2xl", cornerStyle = "rounded-3xl",
  username = "UnknownPilot"
}: any) {
  const [localVolume, setLocalVolume] = useState<number>(globalVolume);

  // --- LIVE WEBRTC PTT STATES ---
  const [isTransmitting, setIsTransmitting] = useState(false);
  const [activeSpeaker, setActiveSpeaker] = useState<string | null>(null);
  const [livePeersCount, setLivePeersCount] = useState(0);
  
  const localStreamRef = useRef<MediaStream | null>(null);
  const peerConnectionsRef = useRef<{ [peerId: string]: RTCPeerConnection }>({});
  const channelRef = useRef(activeChannel);

  useEffect(() => { channelRef.current = activeChannel; }, [activeChannel]);

  useEffect(() => {
    const handleSettingsSync = () => setLocalVolume(Number(localStorage.getItem("rural_global_vol") ?? globalVolume));
    window.addEventListener('theme-sync', handleSettingsSync);
    window.addEventListener('settings-sync', handleSettingsSync);
    window.addEventListener('storage', handleSettingsSync);
    handleSettingsSync();
    return () => {
      window.removeEventListener('theme-sync', handleSettingsSync);
      window.removeEventListener('settings-sync', handleSettingsSync);
      window.removeEventListener('storage', handleSettingsSync);
    };
  }, [globalVolume]);

  const getFrequency = (ch: string) => {
    if (ch.includes("CH-1")) return "462.5625 MHz";
    if (ch.includes("CH-2")) return "462.5875 MHz";
    if (ch.includes("CH-3")) return "462.6125 MHz";
    if (ch.includes("CH-4")) return "462.6375 MHz";
    return "SCANNING...";
  };

  useEffect(() => {
    let unsubscribePresence: () => void;
    
    const joinChannel = async () => {
      if (!username) return;
      
      const myPresenceRef = doc(db, `channels/${activeChannel}/presence`, username);
      await setDoc(myPresenceRef, { active: true, lastSeen: serverTimestamp() });

      const q = query(collection(db, `channels/${activeChannel}/presence`), where("active", "==", true));
      unsubscribePresence = onSnapshot(q, (snap) => {
        setLivePeersCount(snap.docs.filter(d => d.id !== username).length);
      });

      const pttRef = doc(db, `channels/${activeChannel}/state`, "currentSpeaker");
      onSnapshot(pttRef, (docSnap) => {
        if (docSnap.exists()) {
          const data = docSnap.data();
          const isFresh = data.timestamp && (Date.now() - data.timestamp.toMillis()) < 5000;
          if (data.speaker && data.speaker !== username && isFresh) {
            setActiveSpeaker(data.speaker);
          } else {
            setActiveSpeaker(null);
          }
        }
      });
    };

    joinChannel();

    return () => {
      if (unsubscribePresence) unsubscribePresence();
      if (username) {
        deleteDoc(doc(db, `channels/${activeChannel}/presence`, username)).catch(() => {});
      }
      Object.values(peerConnectionsRef.current).forEach(pc => pc.close());
      peerConnectionsRef.current = {};
      if (localStreamRef.current) {
        localStreamRef.current.getTracks().forEach(t => t.stop());
        localStreamRef.current = null;
      }
    };
  }, [activeChannel, username]);

  const handlePttDown = async () => {
    if (activeSpeaker) return;
    if (startRecording) startRecording();
    setIsTransmitting(true);

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      localStreamRef.current = stream;

      await setDoc(doc(db, `channels/${activeChannel}/state`, "currentSpeaker"), {
        speaker: username,
        timestamp: serverTimestamp()
      });
    } catch (e) {
      console.warn("PTT WebRTC Mic Error:", e);
      setIsTransmitting(false);
    }
  };

  const handlePttUp = async () => {
    if (!isTransmitting) return;
    if (stopRecording) stopRecording();
    setIsTransmitting(false);

    if (localStreamRef.current) {
      localStreamRef.current.getTracks().forEach(t => t.stop());
      localStreamRef.current = null;
    }

    try {
      await deleteDoc(doc(db, `channels/${activeChannel}/state`, "currentSpeaker"));
    } catch(e) {}
  };

  return (
    <div className={`app-card ${glassBlurClass} ${cornerStyle} ${bgPanel} border ${brd} p-4 flex flex-col shadow-2xl relative overflow-hidden font-sans space-y-3`}>
      <div className={`absolute top-0 left-0 w-full h-1 ${t.bg} shadow-[0_0_10px_${t.hex}]`} />
      
      <div className={`flex items-center justify-between border-b ${brd} pb-2.5`}>
        <div className="flex items-center gap-2">
          <RadioReceiver className={`w-4 h-4 ${t.text} ${activeSpeaker || isTransmitting ? 'animate-pulse' : ''}`} />
          <h3 className={`${txtMain} font-black text-xs tracking-wider uppercase font-mono`}>P2P TAC-RADIO</h3>
        </div>
        <span className={`text-[9px] font-mono font-bold px-2 py-0.5 rounded border ${brd} ${livePeersCount > 0 ? 'bg-emerald-500/20 text-emerald-400' : `${bgInput}${txtMuted}`}`}>
          {livePeersCount > 0 ? `${livePeersCount} Operators Linked` : 'Scanning Grid...'}
        </span>
      </div>

      <div className="space-y-2">
        <select 
          value={activeChannel} 
          onChange={(e) => setActiveChannel(e.target.value)}
          className={`w-full min-h-[42px] ${bgInput} text-[11px] font-mono font-black rounded-xl px-2.5 py-2 outline-none cursor-pointer shadow-inner`}
        >
          <option value="CH-1: GENERAL FLEET">CH-1: GENERAL FLEET</option>
          <option value="CH-2: TRAIL CREW">CH-2: TRAIL CREW</option>
          <option value="CH-3: EMERGENCY TAC">CH-3: EMERGENCY TAC</option>
          <option value="CH-4: NIGHT RECON">CH-4: NIGHT RECON</option>
        </select>

        <div className={`w-full bg-black border ${brd} rounded-xl p-3 shadow-inner flex items-center justify-between`}>
           <div className="flex flex-col">
              <span className="text-[8px] uppercase tracking-widest text-zinc-500 font-bold mb-0.5">UHF Freq Band</span>
              <span className={`text-sm font-mono font-black ${t.text} drop-shadow-md`}>{getFrequency(activeChannel)}</span>
           </div>
           
           <div className="flex gap-1.5 items-end h-full pt-2">
             <div className={`w-1.5 h-2 rounded-sm ${activeSpeaker || isTransmitting ? t.bg : 'bg-white/10'}`} />
             <div className={`w-1.5 h-3.5 rounded-sm ${activeSpeaker || isTransmitting ? t.bg : 'bg-white/10'}`} />
             <div className={`w-1.5 h-5 rounded-sm ${livePeersCount > 0 ? t.bg : 'bg-white/10'}`} />
             <div className={`w-1.5 h-6 rounded-sm ${livePeersCount > 1 ? t.bg : 'bg-white/10'}`} />
             <div className={`w-1.5 h-8 rounded-sm ${livePeersCount > 2 ? t.bg : 'bg-white/10'}`} />
           </div>
        </div>

        <div className="grid grid-cols-2 gap-2">
          <button 
            type="button"
            onClick={() => setSquelchLevel((prev: number) => (prev >= 5 ? 1 : prev + 1))}
            className={`min-h-[38px] py-1.5 ${bgList} border ${brd} ${txtMuted} hover:${txtMain} rounded-xl text-[9px] font-mono font-bold uppercase flex items-center justify-center gap-1 transition-all active:scale-95 cursor-pointer shadow-inner`}
          >
            <Sliders className="w-3 h-3 text-cyan-400" /> Squelch: {squelchLevel}
          </button>
          <button 
            type="button"
            onClick={() => setMicBoost(!micBoost)}
            className={`min-h-[38px] py-1.5 border rounded-xl text-[9px] font-mono font-black uppercase flex items-center justify-center gap-1 transition-all active:scale-95 cursor-pointer shadow-inner ${micBoost ? 'bg-amber-500/20 border-amber-500/50 text-amber-500' : `${bgList} ${brd}${txtMuted}`}`}
          >
            <Zap className="w-3 h-3" /> Boost: {micBoost ? 'ON' : 'OFF'}
          </button>
        </div>
      </div>

      <div className={`${bgCard} border ${brd} rounded-2xl p-3 h-[140px] overflow-y-auto space-y-2 custom-scrollbar shadow-inner relative`}>
        <AnimatePresence>
          {(activeSpeaker || isTransmitting) && (
             <motion.div initial={{ opacity: 0, y: -5 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -5 }} className="absolute top-2 left-2 right-2 bg-emerald-950/80 border border-emerald-500/50 p-2 rounded-xl backdrop-blur-md z-10 shadow-lg flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Activity className="w-4 h-4 text-emerald-400 animate-pulse" />
                  <span className="text-[10px] font-mono font-black text-emerald-300 uppercase tracking-widest">
                    {isTransmitting ? "You Are Transmitting" : `Receiving: ${activeSpeaker}`}
                  </span>
                </div>
                <div className="flex gap-0.5">
                  <span className="w-1 h-3 bg-emerald-500 rounded-sm animate-[bounce_0.8s_infinite]" />
                  <span className="w-1 h-4 bg-emerald-500 rounded-sm animate-[bounce_1.2s_infinite]" />
                  <span className="w-1 h-2 bg-emerald-500 rounded-sm animate-[bounce_1s_infinite]" />
                </div>
             </motion.div>
          )}
        </AnimatePresence>

        <div className={`flex justify-between items-center text-[9px] ${txtMuted} font-mono mb-1 ${activeSpeaker || isTransmitting ? 'pt-10' : ''}`}>
          <span>Async Logs ({walkieFeed.length}) - 1m Expiry</span>
          <span className="text-zinc-500 flex items-center gap-1"><ShieldAlert className="w-2.5 h-2.5" /> SECURE</span>
        </div>

        {walkieFeed.length === 0 ? (
          <div className="h-16 flex flex-col items-center justify-center text-center p-2">
            <span className="text-zinc-600 text-[9px] font-mono font-bold uppercase tracking-widest">
              No Async Voice Logs Detected
            </span>
          </div>
        ) : (
          walkieFeed.map((item: any) => (
            <div key={item.id} className={`p-2 rounded-xl ${bgList} border ${brd} flex flex-col gap-1.5 shadow-inner`}>
              <div className="flex justify-between items-center text-[9px] font-mono">
                <span className={`font-bold ${txtMain}`}>{item.sender}</span>
                <span className={txtMuted}>{getTimeAgo(item.timestamp)}</span>
              </div>
              <audio 
                controls 
                src={item.audioData} 
                className="w-full h-7 outline-none rounded-lg" 
                controlsList="nodownload noplaybackrate" 
                onPlay={(e) => {
                  (e.currentTarget as HTMLAudioElement).volume = localVolume / 100;
                }}
              />
            </div>
          ))
        )}
      </div>

      <div className="flex flex-col items-center pt-1 relative">
        {activeSpeaker && !isTransmitting && (
           <div className="absolute -top-6 text-[9px] font-black uppercase text-rose-500 font-mono flex items-center gap-1">
             <ShieldAlert className="w-3 h-3" /> Channel Locked (Half-Duplex)
           </div>
        )}
        
        <motion.button 
          disabled={activeSpeaker !== null}
          onMouseDown={handlePttDown}
          onMouseUp={handlePttUp}
          onMouseLeave={handlePttUp}
          onTouchStart={handlePttDown}
          onTouchEnd={handlePttUp}
          animate={isTransmitting ? { scale: [1, 1.03, 1] } : { scale: 1 }}
          transition={isTransmitting ? { repeat: Infinity, duration: 0.6 } : {}}
          className={`w-full min-h-[52px] rounded-2xl border-2 flex items-center justify-center gap-2 font-black uppercase font-mono tracking-widest transition-all shadow-lg select-none active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed ${
            isTransmitting 
              ? 'bg-rose-600 border-white text-white shadow-[0_0_20px_rgba(244,63,94,0.6)] animate-pulse cursor-pointer' 
              : `bg-black/60 ${t.border} ${t.text} hover:${t.bg} hover:text-black cursor-pointer`
          }`}
        >
          {isTransmitting ? <Radio className="w-4 h-4 animate-ping" /> : <Mic className="w-4 h-4" />}
          <span>{isTransmitting ? 'TRANSMITTING...' : 'HOLD TO TALK (PTT)'}</span>
        </motion.button>
      </div>
    </div>
  );
}