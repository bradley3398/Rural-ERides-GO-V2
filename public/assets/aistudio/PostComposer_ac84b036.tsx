"use client";

import React, { useState, useRef } from "react";
import { 
  Send, ImagePlus, Camera, Video, Sparkles, Mic, MicOff, 
  HelpCircle, ShoppingBag, BarChart2, SlidersHorizontal, 
  X, Check, AlertTriangle, Plus, Trash2, Cpu, Zap, Radio, Globe, Tag, BatteryCharging, Smile, MapPin, Users,
  Music, CalendarClock, Route, Coins, ImagePlay, Clock, ShieldCheck, Share2, Eye, Compass, Layers, CheckCircle2
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

export default function PostComposer({
  isComposerOpen, setIsComposerOpen, postText, setPostText, postCategory, setPostCategory,
  pevType, setPevType, postTemplate, setPostTemplate, isHelpNeeded, setIsHelpNeeded,
  itemPrice, setItemPrice, itemCondition, setItemCondition, modTags, setModTags,
  username, pfpUrl, useMetric, handlePublish, isPublishing, safetyWarning, setSafetyWarning,
  mediaPreview, setMediaFile, setMediaPreview, videoPreview, setVideoFile, setVideoPreview,
  fileInputRef, cameraInputRef, videoInputRef, handleMediaPick, handleCameraSnap, handleVideoPick,
  showAdvanced, setShowAdvanced, showPollBuilder, setShowPollBuilder, showCustomizer, setShowCustomizer,
  youtubeUrl, setYoutubeUrl, topSpeed, setTopSpeed, motorTemp, setMotorTemp,
  pollQuestion, setPollQuestion, pollOptions, setPollOptions, isDictating, toggleVoiceDictation,
  enhanceWithAI, isEnhancing, triggerHaptic, themeColors: t, PEV_TEMPLATES, CATEGORIES, PEV_TYPES, CONDITION_TYPES, userProfiles
}: any) {
  const gpxInputRef = useRef<HTMLInputElement>(null);

  const handleAddPollOption = () => {
    if (pollOptions.length >= 4) return;
    if (typeof triggerHaptic === "function") triggerHaptic("LIGHT");
    setPollOptions([...pollOptions, { id: Date.now(), text: "", votes: 0 }]);
  };

  const handleRemovePollOption = (id: number) => {
    if (pollOptions.length <= 2) return;
    if (typeof triggerHaptic === "function") triggerHaptic("LIGHT");
    setPollOptions(pollOptions.filter((opt: any) => opt.id !== id));
  };

  const handlePollTextChange = (id: number, text: string) => {
    setPollOptions(pollOptions.map((opt: any) => opt.id === id ? { ...opt, text } : opt));
  };

  const [postPrivacy, setPostPrivacy] = useState("Public");
  const [feeling, setFeeling] = useState("");
  const [showFeelingPicker, setShowFeelingPicker] = useState(false);
  
  const [showStickerTray, setShowStickerTray] = useState(false);
  const [gpxFileName, setGpxFileName] = useState<string | null>(null);

  const [scheduledTime, setScheduledTime] = useState("");
  const [showSchedulePicker, setShowSchedulePicker] = useState(false);
  const [selectedTrack, setSelectedTrack] = useState("");
  const [showTrackPicker, setShowTrackPicker] = useState(false);
  const [collabTags, setCollabTags] = useState("");
  const [showCollabInput, setShowCollabInput] = useState(false);
  const [watermarkTelemetry, setWatermarkTelemetry] = useState(true);

  // --- 🔥 FACEBOOK STYLE GROUP RIDE EVENT PLANNER STATES 🔥 ---
  const [eventDate, setEventDate] = useState("");
  const [eventLocation, setEventLocation] = useState("");

  const activePfp = pfpUrl || userProfiles?.[username]?.pfpUrl;

  const FEELING_OPTIONS = [
    { label: "⚡ Energized", icon: "⚡" },
    { label: "🌲 Exploring", icon: "🌲" },
    { label: "🔧 Modding", icon: "🔧" },
    { label: "🏁 Speed Running", icon: "🏁" },
    { label: "🔋 Charging", icon: "🔋" },
    { label: "☕ Cruising", icon: "☕" }
  ];

  const STICKER_ASSETS = [
    { label: "⚡ Volt Up", url: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=300&auto=format&fit=crop" },
    { label: "🌲 Trail Scout", url: "https://images.unsplash.com/photo-1511497584788-876761142197?q=80&w=300&auto=format&fit=crop" },
    { label: "🚀 Send It", url: "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?q=80&w=300&auto=format&fit=crop" },
    { label: "🔥 Full Throttle", url: "https://images.unsplash.com/photo-1555680202-c86f0e12f086?q=80&w=300&auto=format&fit=crop" }
  ];

  const AUDIO_TRACKS = [
    "Synthwave Cyber Glide - 120BPM",
    "Trail Singletrack Ambient - 95BPM",
    "Heavy Bassline VESC Drop - 140BPM",
    "Lo-Fi Night Asphalt Cruise - 85BPM"
  ];

  const handleGpxFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setGpxFileName(file.name);
      setPostText((prev: string) => prev ? `${prev}\n\n📍 [Attached GPX Route: ${file.name}]` : `📍 [Attached GPX Route: ${file.name}]`);
      if (typeof triggerHaptic === "function") triggerHaptic("MEDIUM");
    }
  };

  return (
    <div className={`app-card bg-black/70 border border-white/10 rounded-3xl p-4 sm:p-5 shadow-2xl relative overflow-hidden backdrop-blur-3xl`}>
      <input type="file" accept=".gpx,.kml,.geojson" ref={gpxInputRef} onChange={handleGpxFileChange} className="hidden" />

      {!isComposerOpen ? (
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-3">
            <div className={`w-11 h-11 rounded-full bg-black border ${t.borderSubtle} flex items-center justify-center font-black ${t.text} text-sm shrink-0 shadow-inner overflow-hidden cursor-pointer hover:border-white/40 transition-colors`}>
              {activePfp && !activePfp.startsWith("blob:") ? <img src={activePfp} alt={username} className="w-full h-full object-cover" /> : (username ? username.charAt(0).toUpperCase() : "P")}
            </div>
            <div onClick={() => { if (typeof triggerHaptic === "function") triggerHaptic("LIGHT"); setIsComposerOpen(true); }} className="flex-1 min-h-[46px] bg-white/5 hover:bg-white/10 border border-white/10 rounded-full px-5 flex items-center text-sm font-medium text-zinc-400 cursor-pointer transition-all shadow-inner">
              What's on your mind, {username}?
            </div>
          </div>
          <div className="flex items-center justify-around border-t border-white/10 pt-3 mt-1">
            <button type="button" onClick={() => { setIsComposerOpen(true); setTimeout(() => fileInputRef.current?.click(), 100); }} className="min-h-[44px] flex items-center justify-center gap-2 text-zinc-300 hover:text-white font-bold text-xs flex-1 py-2 rounded-xl hover:bg-white/5 transition-colors cursor-pointer"><ImagePlus className="w-5 h-5 text-emerald-400" /> Photo/Video</button>
            <button type="button" onClick={() => { setIsComposerOpen(true); setIsHelpNeeded(!isHelpNeeded); }} className="min-h-[44px] flex items-center justify-center gap-2 text-zinc-300 hover:text-white font-bold text-xs flex-1 py-2 rounded-xl hover:bg-white/5 transition-colors cursor-pointer"><AlertTriangle className="w-5 h-5 text-rose-500" /> SOS Beacon</button>
            <button type="button" onClick={() => { setIsComposerOpen(true); setPostCategory("Group Ride"); }} className="min-h-[44px] flex items-center justify-center gap-2 text-zinc-300 hover:text-white font-bold text-xs flex-1 py-2 rounded-xl hover:bg-white/5 transition-colors cursor-pointer"><Users className="w-5 h-5 text-purple-400" /> Event</button>
          </div>
        </div>
      ) : (
        <form onSubmit={handlePublish} className="space-y-4">
          <div className="flex items-center justify-between border-b border-white/10 pb-3">
            <div className="flex items-center gap-2.5">
              <span className="text-sm font-black uppercase tracking-widest text-white flex items-center gap-2"><Globe className="w-4 h-4 text-emerald-400"/> Create Broadcast</span>
            </div>
            <button type="button" onClick={() => { if (typeof triggerHaptic === "function") triggerHaptic("LIGHT"); setIsComposerOpen(false); }} className="min-h-[44px] min-w-[44px] flex items-center justify-center text-zinc-500 hover:text-white p-2.5 rounded-full bg-white/5 border border-white/10 transition-colors cursor-pointer active:scale-95"><X className="w-4 h-4" /></button>
          </div>

          <div className="flex items-center gap-3 mb-2">
             <div className={`w-11 h-11 rounded-full bg-black border ${t.borderSubtle} flex items-center justify-center font-black ${t.text} text-sm shrink-0 shadow-inner overflow-hidden`}>
                {activePfp && !activePfp.startsWith("blob:") ? <img src={activePfp} alt={username} className="w-full h-full object-cover" /> : username.charAt(0).toUpperCase()}
             </div>
             <div className="flex-1 min-w-0">
               <h4 className="text-sm font-black text-white truncate">{username} {feeling && <span className="font-normal text-zinc-400">is feeling {feeling}</span>}</h4>
               <div className="flex items-center gap-2 mt-1">
                 <select value={postPrivacy} onChange={(e) => setPostPrivacy(e.target.value)} className="bg-white/10 border border-white/10 text-[10px] text-zinc-200 rounded-md px-2.5 py-1 outline-none cursor-pointer font-bold">
                    <option value="Public">🌎 Public Network</option>
                    <option value="Crew Only">👥 Crew Squad Only</option>
                    <option value="Private">🔒 Secure Vault</option>
                 </select>
                 {scheduledTime && <span className="text-[9px] font-mono bg-purple-950/60 border border-purple-500/40 text-purple-300 px-2 py-0.5 rounded-md flex items-center gap-1"><CalendarClock className="w-3 h-3"/> Scheduled</span>}
                 {gpxFileName && <span className="text-[9px] font-mono bg-sky-950/60 border border-sky-500/40 text-sky-300 px-2 py-0.5 rounded-md flex items-center gap-1 truncate max-w-[140px]"><Route className="w-3 h-3"/> {gpxFileName}</span>}
               </div>
             </div>
          </div>

          {safetyWarning && (
            <div className="bg-red-950/40 border border-red-500/50 p-3.5 rounded-2xl flex items-center gap-3 text-xs text-red-200 shadow-inner"><AlertTriangle className="w-5 h-5 text-red-400 shrink-0" /><span>{safetyWarning}</span></div>
          )}

          <div className="relative">
            <textarea
              rows={4} value={postText} onChange={(e) => setPostText(e.target.value)}
              placeholder={`What's happening on the trails, ${username}?`}
              className="w-full bg-transparent border-none p-2 text-lg sm:text-[17px] text-white placeholder:text-zinc-500 outline-none resize-none leading-relaxed font-medium" autoFocus
            />
            <div className="flex justify-between items-center px-2 text-[10px] font-mono text-zinc-500">
               <span>{postText.length} / 500 chars</span>
               {isDictating && <span className="text-rose-500 uppercase tracking-widest animate-pulse flex items-center gap-1"><Mic className="w-3 h-3" /> Dictating...</span>}
            </div>
          </div>

          {/* --- 🔥 FACEBOOK STYLE GROUP RIDE EVENT PLANNER BUILDER 🔥 --- */}
          {postCategory === "Group Ride" && (
            <div className="p-4 bg-purple-950/30 border border-purple-500/40 rounded-2xl space-y-3">
              <span className="text-[10px] font-black uppercase tracking-widest text-purple-300 flex items-center gap-1.5">
                <CalendarClock className="w-4 h-4 text-purple-400" /> Facebook-Style Group Ride Event Planner
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <input type="datetime-local" value={eventDate} onChange={(e) => setEventDate(e.target.value)} className="min-h-[44px] bg-black/60 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none" />
                <input type="text" value={eventLocation} onChange={(e) => setEventLocation(e.target.value)} placeholder="Meeting Point / Trailhead..." className="min-h-[44px] bg-black/60 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none" />
              </div>
            </div>
          )}

          <AnimatePresence>
            {showStickerTray && (
              <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="p-3 bg-white/5 border border-white/10 rounded-2xl space-y-2 overflow-hidden">
                <div className="flex justify-between items-center px-1">
                  <span className="text-[10px] font-black uppercase text-pink-400 tracking-wider">Select Broadcast Badge / Sticker</span>
                  <button type="button" onClick={() => setShowStickerTray(false)} className="text-zinc-500 hover:text-white"><X className="w-3.5 h-3.5"/></button>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {STICKER_ASSETS.map((sticker) => (
                    <div key={sticker.label} onClick={() => { setPostText((prev: string) => prev ? `${prev}\n\n🏷️ [Attached: ${sticker.label}]` : `🏷️ [Attached: ${sticker.label}]`); setShowStickerTray(false); }} className="group relative rounded-xl overflow-hidden border border-white/10 hover:border-pink-500/50 cursor-pointer aspect-video bg-black/60 shadow-sm">
                      <img src={sticker.url} alt="" className="w-full h-full object-cover group-hover:scale-105 transition-transform opacity-70 group-hover:opacity-100" />
                      <span className="absolute bottom-1 left-1.5 text-[9px] font-black uppercase text-white bg-black/70 px-1.5 py-0.5 rounded backdrop-blur-sm">{sticker.label}</span>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <AnimatePresence>
            {showFeelingPicker && (
              <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="flex flex-wrap gap-2 p-3 bg-white/5 border border-white/10 rounded-2xl overflow-hidden">
                {FEELING_OPTIONS.map((f) => (
                  <button key={f.label} type="button" onClick={() => { setFeeling(feeling === f.label ? "" : f.label); setShowFeelingPicker(false); }} className={`min-h-[36px] px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${feeling === f.label ? `${t.bg} text-black font-black` : "bg-black/50 text-zinc-300 hover:text-white"}`}>{f.label}</button>
                ))}
              </motion.div>
            )}
          </AnimatePresence>

          <AnimatePresence>
            {showSchedulePicker && (
              <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="p-4 bg-purple-950/30 border border-purple-500/40 rounded-2xl space-y-3">
                <span className="text-[10px] font-black uppercase tracking-widest text-purple-300 flex items-center gap-1.5"><CalendarClock className="w-4 h-4"/> Schedule Broadcast Release</span>
                <input type="datetime-local" value={scheduledTime} onChange={(e) => setScheduledTime(e.target.value)} className="w-full min-h-[44px] bg-black/60 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none"/>
              </motion.div>
            )}
          </AnimatePresence>

          <AnimatePresence>
            {showTrackPicker && (
              <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="p-4 bg-fuchsia-950/30 border border-fuchsia-500/40 rounded-2xl space-y-2">
                <span className="text-[10px] font-black uppercase tracking-widest text-fuchsia-300 flex items-center gap-1.5"><Music className="w-4 h-4"/> Select Background Track</span>
                <div className="space-y-1.5">
                   {AUDIO_TRACKS.map(track => (
                      <div key={track} onClick={() => { setSelectedTrack(track); setShowTrackPicker(false); }} className={`p-2.5 rounded-xl text-xs font-bold cursor-pointer transition-colors flex items-center justify-between ${selectedTrack === track ? 'bg-fuchsia-600 text-white' : 'bg-black/50 text-zinc-300 hover:bg-white/10'}`}>
                         <span>{track}</span>
                         {selectedTrack === track && <Check className="w-4 h-4"/>}
                      </div>
                   ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <AnimatePresence>
            {showCollabInput && (
              <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="p-4 bg-emerald-950/30 border border-emerald-500/40 rounded-2xl space-y-2">
                <span className="text-[10px] font-black uppercase tracking-widest text-emerald-300 flex items-center gap-1.5"><Users className="w-4 h-4"/> Co-Author &amp; Tag Crew</span>
                <input type="text" value={collabTags} onChange={(e) => setCollabTags(e.target.value)} placeholder="e.g. @Rider93, @NeonX" className="w-full min-h-[44px] bg-black/60 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none"/>
              </motion.div>
            )}
          </AnimatePresence>

          {(mediaPreview || videoPreview) && (
            <div className="relative inline-block rounded-3xl overflow-hidden border border-white/20 max-h-64 shadow-lg bg-black w-full">
              {mediaPreview && <img src={mediaPreview} alt="" className="w-full h-full object-cover max-h-64" />}
              {videoPreview && <video src={videoPreview} controls className="max-h-64 w-full" />}
              <button type="button" onClick={() => { setMediaFile(null); setMediaPreview(null); setVideoFile(null); setVideoPreview(null); }} className="absolute top-3 right-3 min-h-[40px] min-w-[40px] flex items-center justify-center bg-black/80 hover:bg-rose-600 text-white rounded-full p-1.5 transition-colors cursor-pointer"><X className="w-4 h-4" /></button>
            </div>
          )}

          <div className="border border-white/10 rounded-2xl p-2.5 flex flex-col gap-2.5 shadow-inner bg-black/40">
             <div className="flex items-center justify-between w-full px-2">
               <span className="text-[10px] font-black uppercase text-zinc-400 tracking-widest hidden sm:block">Primary Media</span>
               <div className="flex items-center gap-1 overflow-x-auto custom-scrollbar flex-1 sm:justify-end">
                  <input type="file" accept="image/*" ref={fileInputRef} onChange={handleMediaPick} className="hidden" />
                  <button type="button" onClick={() => fileInputRef.current?.click()} className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl hover:bg-white/10 text-emerald-400 transition-colors cursor-pointer" title="Attach Photo/Video"><ImagePlus className="w-5 h-5" /></button>
                  <input type="file" accept="image/*" capture="environment" ref={cameraInputRef} onChange={handleCameraSnap} className="hidden" />
                  <button type="button" onClick={() => cameraInputRef.current?.click()} className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl hover:bg-white/10 text-cyan-400 transition-colors cursor-pointer" title="Camera Snap"><Camera className="w-5 h-5" /></button>
                  <button type="button" onClick={() => setShowStickerTray(!showStickerTray)} className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl hover:bg-white/10 text-pink-400 transition-colors cursor-pointer" title="Stickers & Badges"><ImagePlay className="w-5 h-5" /></button>
                  <button type="button" onClick={() => setShowFeelingPicker(!showFeelingPicker)} className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl hover:bg-white/10 text-amber-400 transition-colors cursor-pointer" title="Feeling/Activity"><Smile className="w-5 h-5" /></button>
                  <button type="button" onClick={toggleVoiceDictation} className={`min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl transition-colors cursor-pointer ${isDictating ? "bg-rose-950/80 text-rose-500 animate-pulse" : "hover:bg-white/10 text-rose-400"}`} title="Voice Dictation">{isDictating ? <Mic className="w-5 h-5" /> : <MicOff className="w-5 h-5" />}</button>
                  <button type="button" onClick={() => { if (typeof triggerHaptic === "function") triggerHaptic("LIGHT"); setShowAdvanced(!showAdvanced); }} className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl hover:bg-white/10 text-indigo-400 transition-colors cursor-pointer" title="Telemetry"><SlidersHorizontal className="w-5 h-5" /></button>
                  <button type="button" onClick={() => { if (typeof triggerHaptic === "function") triggerHaptic("LIGHT"); setShowPollBuilder(!showPollBuilder); }} className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl hover:bg-white/10 text-orange-400 transition-colors cursor-pointer" title="Poll"><BarChart2 className="w-5 h-5" /></button>
                  <button type="button" onClick={() => { if (typeof triggerHaptic === "function") triggerHaptic("LIGHT"); setIsHelpNeeded(!isHelpNeeded); }} className={`min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl transition-colors cursor-pointer ${isHelpNeeded ? "bg-red-600 text-white animate-pulse" : "hover:bg-white/10 text-red-500"}`} title="SOS Signal"><HelpCircle className="w-5 h-5" /></button>
               </div>
             </div>

             <div className="flex items-center gap-1.5 overflow-x-auto custom-scrollbar border-t border-white/5 pt-2.5 px-1">
                <button type="button" onClick={() => setShowTrackPicker(!showTrackPicker)} className="min-h-[36px] px-3.5 rounded-xl text-[9px] font-black uppercase text-zinc-300 hover:text-white bg-white/5 hover:bg-white/10 flex items-center gap-1.5 whitespace-nowrap transition-colors cursor-pointer"><Music className="w-3 h-3 text-fuchsia-400"/> {selectedTrack ? "Change Track" : "Add Track"}</button>
                <button type="button" onClick={() => gpxInputRef.current?.click()} className="min-h-[36px] px-3.5 rounded-xl text-[9px] font-black uppercase text-zinc-300 hover:text-white bg-white/5 hover:bg-white/10 flex items-center gap-1.5 whitespace-nowrap transition-colors cursor-pointer"><Route className="w-3 h-3 text-sky-400"/> {gpxFileName ? "Replace GPX" : "Attach GPX"}</button>
                <button type="button" onClick={() => setShowCollabInput(!showCollabInput)} className="min-h-[36px] px-3.5 rounded-xl text-[9px] font-black uppercase text-zinc-300 hover:text-white bg-white/5 hover:bg-white/10 flex items-center gap-1.5 whitespace-nowrap transition-colors cursor-pointer"><Users className="w-3 h-3 text-emerald-400"/> Collab Tag</button>
                <button type="button" onClick={() => setShowSchedulePicker(!showSchedulePicker)} className="min-h-[36px] px-3.5 rounded-xl text-[9px] font-black uppercase text-zinc-300 hover:text-white bg-white/5 hover:bg-white/10 flex items-center gap-1.5 whitespace-nowrap transition-colors cursor-pointer"><CalendarClock className="w-3 h-3 text-blue-400"/> Schedule</button>
                <button type="button" onClick={() => setWatermarkTelemetry(!watermarkTelemetry)} className={`min-h-[36px] px-3.5 rounded-xl text-[9px] font-black uppercase flex items-center gap-1.5 whitespace-nowrap transition-colors cursor-pointer ${watermarkTelemetry ? 'bg-cyan-950/60 border border-cyan-500/40 text-cyan-300' : 'bg-white/5 text-zinc-400'}`}><Compass className="w-3 h-3"/> Telemetry Stamp</button>
             </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-[9px] font-mono font-black uppercase text-zinc-400 tracking-widest block pl-1">Broadcast Channel</label>
              <select value={postCategory} onChange={(e) => setPostCategory(e.target.value)} className="w-full min-h-[44px] bg-black/50 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none focus:border-white/30 cursor-pointer shadow-inner">
                {CATEGORIES.map((cat: string) => <option key={cat} value={cat} className="bg-zinc-900 text-white">{cat}</option>)}
              </select>
            </div>
            <div className="space-y-1">
              <label className="text-[9px] font-mono font-black uppercase text-zinc-400 tracking-widest block pl-1">Vehicle Classification</label>
              <select value={pevType} onChange={(e) => setPevType(e.target.value)} className="w-full min-h-[44px] bg-black/50 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none focus:border-white/30 cursor-pointer shadow-inner">
                {PEV_TYPES.map((type: string) => <option key={type} value={type} className="bg-zinc-900 text-white">{type}</option>)}
              </select>
            </div>
          </div>

          {(postCategory === "Marketplace" || postTemplate === "market") && (
            <div className="p-4 bg-emerald-950/20 border border-emerald-500/30 rounded-2xl space-y-3">
              <span className="text-[10px] font-black text-emerald-400 uppercase tracking-widest block flex items-center gap-1.5"><ShoppingBag className="w-4 h-4" /> Classified Listing</span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <input type="text" value={itemPrice} onChange={(e) => setItemPrice(e.target.value)} placeholder="Price (e.g. $450 or Trade)" className="min-h-[44px] bg-black/60 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none focus:border-emerald-500 shadow-inner" />
                <select value={itemCondition} onChange={(e) => setItemCondition(e.target.value)} className="min-h-[44px] bg-black/60 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none focus:border-emerald-500 shadow-inner cursor-pointer">
                  {CONDITION_TYPES.map((cond: string) => <option key={cond} value={cond} className="bg-zinc-900 text-white">{cond}</option>)}
                </select>
              </div>
            </div>
          )}

          {showPollBuilder && (
            <div className="p-4 bg-black/60 border border-white/10 rounded-2xl space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-black text-cyan-400 uppercase tracking-widest flex items-center gap-1.5"><BarChart2 className="w-4 h-4" /> Interactive Poll Suite</span>
                {pollOptions.length < 4 && <button type="button" onClick={handleAddPollOption} className="min-h-[36px] px-3 text-[10px] font-black text-cyan-400 hover:text-white uppercase flex items-center gap-1 cursor-pointer"><Plus className="w-3.5 h-3.5" /> Add Choice</button>}
              </div>
              <input type="text" value={pollQuestion} onChange={(e) => setPollQuestion(e.target.value)} placeholder="Ask a question..." className="w-full min-h-[44px] bg-black/70 border border-white/10 rounded-xl px-4 text-sm font-bold text-white outline-none focus:border-cyan-400 shadow-inner" />
              <div className="space-y-2">
                {pollOptions.map((opt: any, idx: number) => (
                  <div key={opt.id} className="flex items-center gap-2">
                    <input type="text" value={opt.text} onChange={(e) => handlePollTextChange(opt.id, e.target.value)} placeholder={`Option ${idx + 1}`} className="flex-1 min-h-[44px] bg-black/50 border border-white/10 rounded-xl px-4 text-sm text-white outline-none focus:border-cyan-400 shadow-inner" />
                    {pollOptions.length > 2 && <button type="button" onClick={() => handleRemovePollOption(opt.id)} className="min-h-[40px] min-w-[40px] flex items-center justify-center text-zinc-500 hover:text-rose-400 p-2.5 cursor-pointer"><Trash2 className="w-4 h-4" /></button>}
                  </div>
                ))}
              </div>
            </div>
          )}

          {showAdvanced && (
            <div className="p-4 bg-black/60 border border-white/10 rounded-2xl space-y-3 font-mono">
              <span className="text-[10px] font-sans font-black text-amber-400 uppercase tracking-widest block flex items-center gap-1.5"><SlidersHorizontal className="w-4 h-4" /> Telemetry &amp; Mod Tags</span>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <input type="number" value={topSpeed} onChange={(e) => setTopSpeed(e.target.value)} placeholder={`Top Speed (${useMetric ? 'km/h' : 'mph'})`} className="min-h-[44px] bg-black/70 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none focus:border-amber-400 shadow-inner" />
                <input type="number" value={motorTemp} onChange={(e) => setMotorTemp(e.target.value)} placeholder="Motor Temp (°C)" className="min-h-[44px] bg-black/70 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none focus:border-amber-400 shadow-inner" />
                <input type="text" value={youtubeUrl} onChange={(e) => setYoutubeUrl(e.target.value)} placeholder="YouTube Link" className="min-h-[44px] bg-black/70 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none focus:border-amber-400 shadow-inner" />
              </div>
              <input type="text" value={modTags} onChange={(e) => setModTags(e.target.value)} placeholder="Tags (e.g. 72V, Black Diamond, Blue Square, 16x4.0 CST)" className="w-full min-h-[44px] bg-black/70 border border-white/10 rounded-xl px-4 text-xs font-bold text-white outline-none focus:border-amber-400 shadow-inner" />
            </div>
          )}

          <div className="flex gap-2">
             <button type="button" onClick={enhanceWithAI} disabled={isEnhancing || !postText.trim()} className={`min-h-[48px] px-4 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-cyan-400 hover:text-cyan-300 text-xs font-black uppercase tracking-wider flex items-center gap-2 cursor-pointer active:scale-95 transition-all disabled:opacity-40 shadow-inner flex-1 justify-center`} title="Enhance with AI"><Sparkles className={`w-4 h-4 ${isEnhancing ? "animate-spin" : ""}`} /><span>AI Polish</span></button>
             <button type="submit" disabled={isPublishing || !postText.trim()} className={`min-h-[48px] px-8 rounded-xl ${t.bg} text-black font-black uppercase text-sm tracking-wider transition-all active:scale-95 cursor-pointer shadow-[0_0_20px_rgba(255,255,255,0.2)] disabled:opacity-40 flex items-center justify-center gap-2 flex-[2]`}><span>{isPublishing ? "Broadcasting..." : "Post"}</span></button>
          </div>
        </form>
      )}
    </div>
  );
}