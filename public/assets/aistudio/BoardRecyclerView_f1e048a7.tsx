"use client";

import React, { useState, useEffect, useRef, useMemo, useCallback, createElement } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import { locationService } from "../services/LocationService";
import { getGeminiApiKey, fetchWithRetry } from "../services/CoPilotService";
import { LocalNotifications } from '@capacitor/local-notifications';

// --- FIREBASE REALTIME ENGINE ---
import { 
  collection, query, onSnapshot, doc, updateDoc, increment, 
  serverTimestamp, deleteDoc, addDoc, setDoc, arrayUnion, where, limit, orderBy
} from "firebase/firestore";
import { db } from "../services/firebase"; 

// --- MODULAR BOARD WIDGETS ---
import FeedPost from "./BoardWidgets/FeedPost";
import SectorStandings from "./BoardWidgets/SectorStandings";
import UserProfileModal from "./BoardWidgets/UserProfileModal";
import DirectMessagesModal from "./BoardWidgets/DirectMessagesModal";
import PostComposer from "./BoardWidgets/PostComposer";
import BoardSettingsModal from "./BoardWidgets/BoardSettingsModal";
import LiveRadarView from "./BoardWidgets/LiveRadarView"; 
import RiderLevelBadge from "./BoardWidgets/RiderLevelBadge";

// --- ICONS & ANIMATIONS ---
import { 
  Globe, Bell, Mail, Search, Settings, X, WifiOff, 
  TrendingUp, Clock, RefreshCw, Loader2, ShieldCheck, User, MessageSquare, 
  Flame, Zap, Plus, Video, Map, Target, HeartHandshake, History, Users, 
  Sparkles, Award, Radio, Trophy, Mic2, MapPin, Sliders, Star, Share2, 
  HelpCircle, CheckCircle2, Bookmark, Navigation2, AlertTriangle, 
  Activity, Wind, Thermometer, ShieldAlert, Cpu, Wrench, BarChart2, CornerDownRight, 
  ThumbsUp, Rocket, Heart, Smile, MoreHorizontal, Copy, Repeat, Coins, Mic, Languages, 
  BarChart3, Music, Code, ChevronRight, Image as ImageIcon, Lock, Pin, Navigation, Sun
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

// --- CAPACITOR NATIVE PLUGINS ---
import { Capacitor } from '@capacitor/core';
import { Device } from '@capacitor/device';
import { Network as CapNetwork } from '@capacitor/network';
import { Haptics } from '@capacitor/haptics';

// --- ACCURATE LEAFLET SOCIAL MAP ---
interface SocialPin {
  id: string;
  lat: number;
  lng: number;
  title: string;
  category: "Meetup" | "Photo" | "Outlet" | "Hazard" | "Confession";
  note: string;
  creator: string;
  volts: number;
}

function OpenSourceSocialMap({ 
  pins, 
  onSelectPin, 
  onDropPin, 
  onMapClick,
  draftPinCoords,
  themeColors: t, 
  userLoc, 
  useMetric, 
  onDeletePin, 
  currentUsername 
}: any) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const tileLayerRef = useRef<L.TileLayer | null>(null);
  const userMarkerRef = useRef<L.Marker | null>(null);
  const draftMarkerRef = useRef<L.Marker | null>(null);
  const markersRef = useRef<{ [key: string]: L.Marker }>({});

  const [filterCat, setFilterCat] = useState("All");
  const [mapLayer, setMapLayer] = useState<"dark" | "osm" | "topo">("dark");

  const centerLat = (userLoc && !isNaN(userLoc.lat) && userLoc.lat !== 0) ? userLoc.lat : 35.289;
  const centerLng = (userLoc && !isNaN(userLoc.lng) && userLoc.lng !== 0) ? userLoc.lng : -95.124;

  const filteredPins = useMemo(() => {
    if (filterCat === "All") return pins;
    return pins.filter((p: SocialPin) => p.category === filterCat);
  }, [pins, filterCat]);

  useEffect(() => {
    if (!mapContainerRef.current) return;

    const map = L.map(mapContainerRef.current, {
      center: [centerLat, centerLng],
      zoom: 14,
      zoomControl: false,
      attributionControl: false,
    });
    mapRef.current = map;

    const getTileUrl = (layer: string) => {
      if (layer === "topo") return "https://mt1.google.com/vt/lyrs=p&x={x}&y={y}&z={z}";
      return "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}";
    };

    const tileLayer = L.tileLayer(getTileUrl(mapLayer), { maxZoom: 19 }).addTo(map);
    tileLayerRef.current = tileLayer;

    const pane = map.getPanes().tilePane;
    if (pane) {
      pane.style.filter = mapLayer === "dark" 
        ? "invert(100%) hue-rotate(180deg) brightness(85%) contrast(120%) saturate(130%)" 
        : "none";
    }

    map.on("click", (e: L.LeafletMouseEvent) => {
      const { lat, lng } = e.latlng;
      if (onMapClick) onMapClick(lat, lng);
    });

    const timer = setTimeout(() => {
      if (mapRef.current) mapRef.current.invalidateSize();
    }, 250);

    return () => {
      clearTimeout(timer);
      map.remove();
      mapRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current || !tileLayerRef.current) return;
    const url = mapLayer === "topo"
      ? "https://mt1.google.com/vt/lyrs=p&x={x}&y={y}&z={z}"
      : "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}";
    tileLayerRef.current.setUrl(url);

    const pane = mapRef.current.getPanes().tilePane;
    if (pane) {
      pane.style.filter = mapLayer === "dark" 
        ? "invert(100%) hue-rotate(180deg) brightness(85%) contrast(120%) saturate(130%)" 
        : "none";
    }
  }, [mapLayer]);

  useEffect(() => {
    if (!mapRef.current) return;
    if (!userLoc || isNaN(userLoc.lat) || isNaN(userLoc.lng) || userLoc.lat === 0) return;

    const userIcon = L.divIcon({
      className: "custom-user-marker",
      html: `<div class="relative flex items-center justify-center">
               <span class="absolute inline-flex h-8 w-8 rounded-full ${t.bg} opacity-40 animate-ping"></span>
               <span class="relative inline-flex rounded-full h-5 w-5 ${t.bg} border-2 border-black flex items-center justify-center shadow-lg" style="box-shadow: 0 0 15px ${t.hex}">
                 <span class="w-1.5 h-1.5 bg-black rounded-full"></span>
               </span>
             </div>`,
      iconSize: [32, 32],
      iconAnchor: [16, 16]
    });

    if (userMarkerRef.current) {
      userMarkerRef.current.setLatLng([userLoc.lat, userLoc.lng]).setIcon(userIcon);
    } else {
      userMarkerRef.current = L.marker([userLoc.lat, userLoc.lng], { icon: userIcon, zIndexOffset: 900 }).addTo(mapRef.current);
    }
  }, [userLoc, t]);

  useEffect(() => {
    if (!mapRef.current) return;
    if (draftPinCoords && !isNaN(draftPinCoords.lat)) {
      const draftIcon = L.divIcon({
        className: "custom-draft-marker",
        html: `<div class="relative flex items-center justify-center -translate-y-2">
                 <div class="p-2.5 rounded-full border-2 ${t.border} ${t.bgSubtle} ${t.text} animate-bounce shadow-xl backdrop-blur-md">
                   <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>
                 </div>
               </div>`,
        iconSize: [40, 40],
        iconAnchor: [20, 40]
      });

      if (draftMarkerRef.current) {
        draftMarkerRef.current.setLatLng([draftPinCoords.lat, draftPinCoords.lng]).setIcon(draftIcon);
      } else {
        draftMarkerRef.current = L.marker([draftPinCoords.lat, draftPinCoords.lng], { icon: draftIcon, zIndexOffset: 1000 }).addTo(mapRef.current);
      }
    } else if (draftMarkerRef.current) {
      draftMarkerRef.current.remove();
      draftMarkerRef.current = null;
    }
  }, [draftPinCoords, t]);

  useEffect(() => {
    if (!mapRef.current) return;
    const currentMarkerIds = Object.keys(markersRef.current);
    const activeIds = filteredPins.map((p: SocialPin) => p.id);

    currentMarkerIds.forEach((id) => {
      if (!activeIds.includes(id)) {
        markersRef.current[id].remove();
        delete markersRef.current[id];
      }
    });

    filteredPins.forEach((pin: SocialPin) => {
      const colorMap: Record<string, { bg: string; border: string; glow: string }> = { 
        Meetup: { bg: "#a855f7", border: "#d8b4fe", glow: "#a855f7" }, 
        Photo: { bg: "#06b6d4", border: "#67e8f9", glow: "#06b6d4" }, 
        Outlet: { bg: "#f59e0b", border: "#fcd34d", glow: "#f59e0b" }, 
        Hazard: { bg: "#e11d48", border: "#fda4af", glow: "#e11d48" }, 
        Confession: { bg: "#10b981", border: "#6ee7b7", glow: "#10b981" } 
      };
      const c = colorMap[pin.category] || { bg: "#ffffff", border: "#ffffff", glow: "#ffffff" };

      const pinIcon = L.divIcon({
        className: "custom-social-pin",
        html: `<div class="relative flex items-center justify-center hover:scale-125 transition-transform" style="filter: drop-shadow(0 0 10px ${c.glow});">
                 <div style="background-color: ${c.bg}; border-color: ${c.border};" class="p-2 rounded-full border-2 text-white flex items-center justify-center shadow-lg">
                   <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>
                 </div>
               </div>`,
        iconSize: [32, 32],
        iconAnchor: [16, 32]
      });

      if (markersRef.current[pin.id]) {
        markersRef.current[pin.id].setLatLng([pin.lat, pin.lng]).setIcon(pinIcon);
      } else {
        const marker = L.marker([pin.lat, pin.lng], { icon: pinIcon, zIndexOffset: 500 }).addTo(mapRef.current!);
        marker.on("click", (e) => {
          L.DomEvent.stopPropagation(e);
          onSelectPin(pin);
        });
        markersRef.current[pin.id] = marker;
      }
    });
  }, [filteredPins]);

  const recenterMap = () => {
    if (userLoc && mapRef.current && userLoc.lat !== 0) {
      mapRef.current.setView([userLoc.lat, userLoc.lng], 15, { animate: true });
    }
  };

  return (
    <div className="relative w-full aspect-[4/3] sm:aspect-[16/9] bg-[#111111] border border-white/10 rounded-3xl overflow-hidden shadow-2xl flex flex-col group select-none">
      <div className="absolute top-3 left-3 right-3 z-[400] flex items-center justify-between gap-3 pointer-events-auto">
        <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-2 pr-4">
          {["All", "Meetup", "Photo", "Outlet", "Hazard", "Confession"].map((cat) => (
            <button 
              key={cat} 
              onClick={() => setFilterCat(cat)} 
              className={`px-4 py-2 rounded-full text-[10px] font-black uppercase tracking-wider border cursor-pointer active:scale-95 transition-all shrink-0 whitespace-nowrap shadow-lg ${
                filterCat === cat ? `${t.bg} text-black${t.border}` : "bg-black/80 text-zinc-300 border-white/10 backdrop-blur-md hover:bg-white/10"
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
        <div className="flex items-center shrink-0">
          <button 
            onClick={onDropPin} 
            className={`${t.bg} text-black px-4 py-2 rounded-full text-[10px] font-black uppercase tracking-wider flex items-center gap-1.5 shadow-lg active:scale-95 transition-all cursor-pointer whitespace-nowrap`}
          >
            <Plus className="w-3.5 h-3.5" /> Drop Pin
          </button>
        </div>
      </div>

      <div ref={mapContainerRef} className="w-full h-full relative z-10 cursor-crosshair bg-[#111111]" />

      <div className="absolute bottom-3 left-3 z-[400] flex gap-2 bg-black/80 border border-white/10 p-1.5 rounded-2xl backdrop-blur-md shadow-xl">
        <button onClick={() => setMapLayer("dark")} className={`px-4 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-wider transition-all cursor-pointer active:scale-95 ${mapLayer === "dark" ? `${t.bg} text-black font-black shadow-md` : "text-zinc-400 hover:text-white"}`}>Dark</button>
        <button onClick={() => setMapLayer("osm")} className={`px-4 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-wider transition-all cursor-pointer active:scale-95 ${mapLayer === "osm" ? `${t.bg} text-black font-black shadow-md` : "text-zinc-400 hover:text-white"}`}>Street</button>
        <button onClick={() => setMapLayer("topo")} className={`px-4 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-wider transition-all cursor-pointer active:scale-95 ${mapLayer === "topo" ? `${t.bg} text-black font-black shadow-md` : "text-zinc-400 hover:text-white"}`}>Topo</button>
      </div>

      <div className="absolute bottom-3 right-3 z-[400] flex flex-col gap-2 bg-black/80 border border-white/10 p-1.5 rounded-2xl backdrop-blur-md shadow-xl">
        <button onClick={recenterMap} className={`w-10 h-10 flex items-center justify-center text-zinc-300 hover:${t.text} rounded-xl transition-colors cursor-pointer bg-white/5 active:scale-95`} title="Recenter GPS"><Navigation2 className="w-4 h-4" /></button>
        <button onClick={() => mapRef.current?.zoomIn()} className="w-10 h-10 flex items-center justify-center text-white hover:bg-white/20 rounded-xl text-xl font-black transition-colors cursor-pointer bg-white/5 active:scale-95">+</button>
        <button onClick={() => mapRef.current?.zoomOut()} className="w-10 h-10 flex items-center justify-center text-white hover:bg-white/20 rounded-xl text-xl font-black transition-colors cursor-pointer bg-white/5 active:scale-95">-</button>
      </div>
    </div>
  );
}

const CATEGORIES = ["Universal Feed", "Group Ride", "Hardware Diagnostics", "Custom Builds", "Trail Reports", "Marketplace", "Video Requests", "Trail Rescue Bounty", "🌲 Trail Confessions", "🏎️ Virtual Garage", "🚨 STOLEN PEV ALERT"];
const PEV_TYPES = ["Class 1 E-Bike", "Class 2 E-Bike", "Class 3 E-Bike", "Electric Scooter", "Electric Moped", "Electric Unicycle", "Electric Trike", "Onewheel"];
const CONDITION_TYPES = ["Brand New", "Like New", "Used - Good", "For Parts / Repair"];
const DISALLOWED_KEYWORDS = ["abuse", "idiot", "jerk", "asshole", "bitch", "crap", "damn", "fuck", "shit", "bastard", "trash", "hate", "kill", "stupid", "moron", "spam", "scam"];

const PEV_TEMPLATES = [
  { id: "none", name: "Standard Matrix", bgUrl: null, css: "bg-black/80 border-white/10 shadow-xl", icon: "⬛ CORE MATRIX", badgeBg: "bg-white/10 text-zinc-300 border-white/10" },
  { id: "rural", name: "🌾 Rural Vibe", bgUrl: "https://images.unsplash.com/photo-1500382017468-9049fed747ef?q=80&w=1000&auto=format&fit=crop", css: "border-lime-500/40 shadow-lg", icon: "🌾 RURAL VIBE", badgeBg: "bg-lime-500/20 text-lime-300 border-lime-500/40" },
  { id: "trail", name: "🌲 Forest Singletrack", bgUrl: "https://images.unsplash.com/photo-1511497584788-876761142197?q=80&w=1000&auto=format&fit=crop", css: "border-amber-500/40 shadow-lg", icon: "🌲 MUD & GRAVEL", badgeBg: "bg-amber-500/20 text-amber-300 border-amber-500/40" },
  { id: "speed", name: "⚡ Full Throttle Red", bgUrl: "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?q=80&w=1000&auto=format&fit=crop", css: "border-red-500/40 shadow-lg", icon: "⚡ MAX VELOCITY", badgeBg: "bg-red-500/20 text-red-300 border-red-500/40" },
  { id: "cyber", name: "🌆 Cyberpunk Grid", bgUrl: "https://images.unsplash.com/photo-1555680202-c86f0e12f086?q=80&w=1000&auto=format&fit=crop", css: "border-cyan-500/40 shadow-lg", icon: "🌆 NEON CITY", badgeBg: "bg-cyan-500/20 text-cyan-300 border-cyan-500/40" }
];

const BOUNTIES = [
  { title: "Weekly Community Bounty", tag: "#MuddyMonster", xp: 1000, icon: createElement(Target, { className: "w-6 h-6 text-purple-400" }), containerClass: "bg-purple-950/40 border border-purple-500/40 shadow-[0_0_25px_rgba(168,85,247,0.2)]", iconClass: "bg-purple-500/20 border-purple-500/50", textClass: "text-purple-300", btnClass: "bg-purple-600 hover:bg-purple-500" },
  { title: "Speed Demon Challenge", tag: "#TopSpeedRun", xp: 1500, icon: createElement(Flame, { className: "w-6 h-6 text-red-400" }), containerClass: "bg-red-950/40 border border-red-500/40 shadow-[0_0_25px_rgba(239,68,68,0.2)]", iconClass: "bg-red-500/20 border-red-500/50", textClass: "text-red-300", btnClass: "bg-red-600 hover:bg-red-500" },
  { title: "Night Ops Patrol", tag: "#NightRider", xp: 800, icon: createElement(Sun, { className: "w-6 h-6 text-amber-400" }), containerClass: "bg-amber-950/40 border border-amber-500/40 shadow-[0_0_25px_rgba(245,158,11,0.2)]", iconClass: "bg-amber-500/20 border-amber-500/50", textClass: "text-amber-300", btnClass: "bg-amber-600 hover:bg-amber-500" },
  { title: "Backcountry Pioneer", tag: "#TrailScout", xp: 1200, icon: createElement(Map, { className: "w-6 h-6 text-emerald-400" }), containerClass: "bg-emerald-950/40 border border-emerald-500/40 shadow-[0_0_25px_rgba(16,185,129,0.2)]", iconClass: "bg-emerald-500/20 border-emerald-500/50", textClass: "text-emerald-300", btnClass: "bg-emerald-600 hover:bg-emerald-500" }
];

const PEV_SLANG_DICTIONARY = [
  { term: "VESC", def: "Vedder Electronic Speed Controller - open source, highly programmable PEV motor driver." },
  { term: "BMS", def: "Battery Management System - protects cells against over-voltage, low-voltage, and balance drain." },
  { term: "Death Wobble", def: "High-speed resonance oscillation causing steering instability. Lean forward and throttle out gently!" },
  { term: "Regen", def: "Regenerative braking - feeds kinetic energy back into battery cells during deceleration." },
  { term: "Ghosting", def: "A runaway vehicle malfunction caused by stuck footpad sensors or throttle trigger faults." },
  { term: "Send It", def: "Commit fully to a jump, trick, or sketch trail section." }
];

const TRENDING_TAGS = [
  { tag: "#VESC", posts: "1.4k posts" },
  { tag: "#TopSpeedRun", posts: "890 posts" },
  { tag: "#MuddyMonster", posts: "650 posts" },
  { tag: "#NightRider", posts: "420 posts" },
  { tag: "#BatteryMod", posts: "310 posts" }
];

const getYoutubeId = (url: string) => { const match = url.match(/^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/); return (match && match[2].length === 11) ? match[2] : null; };
const getTimeMs = (ts: any, backupTs?: any) => { if (ts) { if (typeof ts === 'number') return ts; if (typeof ts.toMillis === 'function') return ts.toMillis(); if (ts.seconds) return ts.seconds * 1000; } if (backupTs && typeof backupTs === 'string') { const parsed = Date.parse(backupTs); if (!isNaN(parsed)) return parsed; } return 0; };
function checkContentSafety(text: string) { const normalized = text.toLowerCase(); for (const word of DISALLOWED_KEYWORDS) { if (normalized.includes(word)) return { safe: false, blockedWord: word }; } return { safe: true }; }
function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number, useMetric: boolean) { const R = useMetric ? 6371.0 : 3958.8; const dLat = ((lat2 - lat1) * Math.PI) / 180; const dLon = ((lon2 - lon1) * Math.PI) / 180; const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2); return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))); }

const compressImage = (file: File | Blob, maxWidth: number, maxHeight: number, quality = 0.65): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader(); reader.readAsDataURL(file);
    reader.onload = (event) => {
      const img = new (window as any).Image(); img.src = event.target?.result as string;
      img.onload = () => {
        let width = img.width; let height = img.height;
        if (width > height) { if (width > maxWidth) { height = Math.round((height * maxWidth) / width); width = maxWidth; } } else { if (height > maxHeight) { width = Math.round((width * maxHeight) / height); height = maxHeight; } }
        const canvas = document.createElement("canvas"); canvas.width = width; canvas.height = height;
        const ctx = canvas.getContext("2d"); if (!ctx) { reject(new Error("Canvas context failed")); return; }
        ctx.drawImage(img, 0, 0, width, height); resolve(canvas.toDataURL("image/jpeg", quality));
      };
      img.onerror = (error: any) => reject(error);
    };
    reader.onerror = (error) => reject(error);
  });
};

interface BoardProps {
  theme?: string; useMetric?: boolean; ghostMode?: boolean; satelliteMap?: boolean; locale?: string;
  timeFormat?: string; performanceMode?: boolean; uiScale?: string; globalVolume?: number; callsign?: string;
  prefilledPost?: { text: string; userBadge: string } | null; onClearPrefilled?: () => void;
  glassBlurClass?: string; cornerStyle?: string; reducedMotion?: boolean;
}

export default function BoardRecyclerView(props: BoardProps) {
  const [mounted, setMounted] = useState(false);
  const [posts, setPosts] = useState<any[]>([]);
  
  const [localTheme, setLocalTheme] = useState<string>(() => props.theme || (typeof window !== 'undefined' ? localStorage.getItem("soc_theme") || "lime" : "lime"));
  const [localMetric, setLocalMetric] = useState<boolean>(() => props.useMetric !== undefined ? props.useMetric : (typeof window !== 'undefined' ? localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km" : false));

  const currentLoc = locationService.getCurrentUpdate();
  const isGpsLocked = !!currentLoc && typeof currentLoc.lat === 'number' && currentLoc.lat !== 0;

  const [weatherData, setWeatherData] = useState<{temp: string, wind: string, sunset: string} | null>(null);

  const getTheme = () => {
    const baseTheme = localTheme; const perf = props.performanceMode;
    const themes: Record<string, any> = {
      lime: { bg: 'bg-[#39ff14]', text: 'text-[#39ff14]', border: 'border-[#39ff14]', glow: perf ? '' : 'shadow-[0_0_20px_rgba(57,255,20,0.3)]', bgSubtle: 'bg-[#39ff14]/10', borderSubtle: 'border-[#39ff14]/30', blur: perf ? 'bg-black/95' : 'bg-black/40 backdrop-blur-2xl', hex: "#39ff14" },
      cyan: { bg: 'bg-cyan-500', text: 'text-cyan-400', border: 'border-cyan-500', glow: perf ? '' : 'shadow-[0_0_20px_rgba(6,182,212,0.3)]', bgSubtle: 'bg-cyan-950/30', borderSubtle: 'border-cyan-900/50', blur: perf ? 'bg-black/95' : 'bg-black/40 backdrop-blur-2xl', hex: "#06b6d4" },
      emerald: { bg: 'bg-emerald-500', text: 'text-emerald-400', border: 'border-emerald-500', glow: perf ? '' : 'shadow-[0_0_20px_rgba(16,185,129,0.3)]', bgSubtle: 'bg-emerald-950/30', borderSubtle: 'border-emerald-900/50', blur: perf ? 'bg-black/95' : 'bg-black/40 backdrop-blur-2xl', hex: "#10b981" },
      amber: { bg: 'bg-amber-400', text: 'text-amber-400', border: 'border-amber-400', glow: perf ? '' : 'shadow-[0_0_20px_rgba(251,191,36,0.3)]', bgSubtle: 'bg-amber-950/30', borderSubtle: 'border-amber-900/50', blur: perf ? 'bg-black/95' : 'bg-black/40 backdrop-blur-2xl', hex: "#f59e0b" },
      rose: { bg: 'bg-rose-500', text: 'text-rose-400', border: 'border-rose-500', glow: perf ? '' : 'shadow-[0_0_20px_rgba(244,63,94,0.3)]', bgSubtle: 'bg-rose-950/30', borderSubtle: 'border-rose-900/50', blur: perf ? 'bg-black/95' : 'bg-black/40 backdrop-blur-2xl', hex: "#f43f5e" },
      purple: { bg: 'bg-purple-500', text: 'text-purple-400', border: 'border-purple-500', glow: perf ? '' : 'shadow-[0_0_20px_rgba(168,85,247,0.3)]', bgSubtle: 'bg-purple-950/30', borderSubtle: 'border-purple-900/50', blur: perf ? 'bg-black/95' : 'bg-black/40 backdrop-blur-2xl', hex: "#a855f7" }
    };
    return themes[baseTheme] || themes.lime;
  };
  const t = getTheme();
  
  const fontScaleMap = { compact: "text-[13px]", normal: "text-[15px]", large: "text-[17px]" };
  const scaleClass = fontScaleMap[props.uiScale as keyof typeof fontScaleMap] || "text-[15px]";

  const triggerHaptic = async (style: any = "LIGHT") => {
    try {
      if (Capacitor.isPluginAvailable('Haptics')) await Haptics.impact({ style });
      else if (typeof navigator !== 'undefined' && 'vibrate' in navigator) navigator.vibrate(style === "HEAVY" ? 50 : 15);
    } catch (e) {}
  };

  const playSynthesizedTaunt = (freq: number, type: any) => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator(); const gain = audioCtx.createGain();
      osc.type = type; osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
      if (type === 'sawtooth') { osc.frequency.exponentialRampToValueAtTime(30, audioCtx.currentTime + 0.4); } 
      else { osc.frequency.linearRampToValueAtTime(freq * 1.5, audioCtx.currentTime + 0.2); osc.frequency.linearRampToValueAtTime(10, audioCtx.currentTime + 0.4); }
      gain.gain.setValueAtTime(0.2, audioCtx.currentTime); gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.4);
      osc.connect(gain); gain.connect(audioCtx.destination);
      osc.start(); osc.stop(audioCtx.currentTime + 0.4); triggerHaptic("HEAVY");
    } catch (e) {}
  };

  const toggleVoiceDictation = () => {
    triggerHaptic("LIGHT");
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) return alert("Voice recognition core not supported.");
    const recognition = new SpeechRecognition(); recognition.lang = 'en-US';
    recognition.onstart = () => setIsDictating(true);
    recognition.onresult = (event: any) => { setPostText(prev => prev ? `${prev} ${event.results[0][0].transcript}` : event.results[0][0].transcript); triggerHaptic("HEAVY"); };
    recognition.onerror = () => setIsDictating(false); recognition.onend = () => setIsDictating(false);
    recognition.start();
  };

  useEffect(() => {
    const handleSettingsSync = () => {
      setLocalTheme(localStorage.getItem("soc_theme") || localStorage.getItem("rt_theme") || props.theme || "lime");
      setLocalMetric(localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km");
      setUsername(props.callsign || localStorage.getItem("soc_username") || "Network Pilot");
    };
    window.addEventListener('theme-sync', handleSettingsSync); window.addEventListener('settings-sync', handleSettingsSync); window.addEventListener('storage', handleSettingsSync);
    handleSettingsSync();
    return () => { window.removeEventListener('theme-sync', handleSettingsSync); window.removeEventListener('settings-sync', handleSettingsSync); window.removeEventListener('storage', handleSettingsSync); };
  }, [props.theme, props.callsign, props.useMetric]);

  const [username, setUsername] = useState<string>(() => props.callsign || (typeof window !== 'undefined' ? localStorage.getItem("soc_username") || "Network Pilot" : "Network Pilot"));
  const [pfpUrl, setPfpUrl] = useState<string>(() => { if (typeof window !== 'undefined') { const saved = localStorage.getItem("soc_pfp") || localStorage.getItem("rural_erides_pfp") || ""; return saved.startsWith("blob:") ? "" : saved; } return ""; });
  const [userFleet, setUserFleet] = useState<string>("");
  const [userBio, setUserBio] = useState<string>("");
  const [privacyMode, setPrivacyMode] = useState(false);
  const [dataSaverMode, setDataSaverMode] = useState<boolean>(() => typeof window !== 'undefined' ? localStorage.getItem("soc_data_saver") === "true" : false);
  const [defaultPostCategory, setDefaultPostCategory] = useState<string>(() => typeof window !== 'undefined' ? localStorage.getItem("soc_default_category") || "Universal Feed" : "Universal Feed");
  
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [activeProfileUser, setActiveProfileUser] = useState<string | null>(null);
  const [userProfiles, setUserProfiles] = useState<Record<string, any>>({});
  const [friends, setFriends] = useState<string[]>(() => { try { if (typeof window !== 'undefined') return JSON.parse(localStorage.getItem("soc_friends") || "[]"); } catch(e) {} return []; });

  const [isAppOnline, setIsAppOnline] = useState<boolean>(true);
  const [deviceNetworkType, setDeviceNetworkType] = useState("Online");
  const [deviceBattLevel, setDeviceBattLevel] = useState<number | null>(null);
  const [isDictating, setIsDictating] = useState(false);

  // Composer States
  const [isComposerOpen, setIsComposerOpen] = useState(false);
  const [postText, setPostText] = useState("");
  const [postCategory, setPostCategory] = useState(defaultPostCategory);
  const [pevType, setPevType] = useState("Electric Scooter");
  const [postTemplate, setPostTemplate] = useState("none");
  const [safetyWarning, setSafetyWarning] = useState<string | null>(null);
  const [isHelpNeeded, setIsHelpNeeded] = useState(false);

  const [itemPrice, setItemPrice] = useState("");
  const [itemCondition, setItemCondition] = useState("Like New");
  const [modTags, setModTags] = useState("");

  const [feedView, setFeedView] = useState<"universal" | "saved" | "help" | "Marketplace" | "crew" | "Group Ride" | "map" | "confessions" | "VirtualGarage">("universal");
  const [searchQuery, setSearchQuery] = useState("");
  const [savedPosts, setSavedPosts] = useState<string[]>([]);
  const [flaggedIds, setFlaggedIds] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState<"newest" | "trending">("newest");
  
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [showPollBuilder, setShowPollBuilder] = useState(false);
  const [showCustomizer, setShowCustomizer] = useState(false);
  const [showSoundboard, setShowSoundboard] = useState(false);
  const [crewStoriesOnly, setCrewStoriesOnly] = useState(false);
  const [showSlangDecoder, setShowSlangDecoder] = useState(false);

  const [socialPins, setSocialPins] = useState<SocialPin[]>([]);
  const [selectedPin, setSelectedPin] = useState<SocialPin | null>(null);
  const [showDropPinModal, setShowDropPinModal] = useState(false);
  const [draftPinCoords, setDraftPinCoords] = useState<{lat: number, lng: number} | null>(null);
  const [newPinCategory, setNewPinCategory] = useState<any>("Meetup");
  const [newPinTitle, setNewPinTitle] = useState("");
  const [newPinNote, setNewPinNote] = useState("");

  const dayOfYear = Math.floor((Date.now() - new Date(new Date().getFullYear(), 0, 0).getTime()) / 86400000);
  const activeBounty = BOUNTIES[dayOfYear % BOUNTIES.length];

  useEffect(() => {
    const q = query(collection(db, "social_pins"));
    const unsub = onSnapshot(q, (snapshot) => {
      const loadedPins = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })) as SocialPin[];
      setSocialPins(loadedPins);
    });
    return () => unsub();
  }, []);

  const handleSavePin = async () => {
    if (!newPinTitle.trim()) return;
    const currentGPS = locationService.getCurrentUpdate();
    const finalLat = draftPinCoords ? draftPinCoords.lat : (currentGPS ? currentGPS.lat : 35.289);
    const finalLng = draftPinCoords ? draftPinCoords.lng : (currentGPS ? currentGPS.lng : -95.124);

    const newPin = {
      lat: finalLat, lng: finalLng, title: newPinTitle.trim(), category: newPinCategory,
      note: newPinNote.trim(), creator: username, volts: 1, timestamp: serverTimestamp()
    };
    try {
      await addDoc(collection(db, "social_pins"), newPin);
      setShowDropPinModal(false); setDraftPinCoords(null); setNewPinTitle(""); setNewPinNote("");
      triggerHaptic("HEAVY");
    } catch (e) { alert("Failed to drop pin."); }
  };

  const handleBoostPin = async () => {
    if (!selectedPin) return;
    triggerHaptic("HEAVY"); triggerNeonBurst();
    try {
      await updateDoc(doc(db, "social_pins", selectedPin.id), { volts: increment(1) });
      setSelectedPin({ ...selectedPin, volts: selectedPin.volts + 1 });
    } catch (e) {}
  };

  const handleDeletePin = async (pinId: string) => {
    if (!window.confirm("Remove this pin from the map?")) return;
    try {
      await deleteDoc(doc(db, "social_pins", pinId));
      setSelectedPin(null); triggerHaptic("HEAVY");
    } catch(e) {}
  };

  const handleQuickSosBeacon = async () => {
    triggerHaptic("HEAVY");
    const gps = locationService.getCurrentUpdate();
    const coordsStr = gps ? `[${gps.lat.toFixed(4)}, ${gps.lng.toFixed(4)}]` : "[Live Sector]";
    const sosPayload = {
      username: username.trim(), pfpUrl: pfpUrl || null, fleetSignature: userFleet || null,
      content: `🚨 SOS EMERGENCY BEACON TRIGGERED: Pilot down / disabled vehicle. Immediate trail assistance needed at Coordinates: ${coordsStr}!`,
      category: "Trail Rescue Bounty", pevType: pevType || "PEV", template: "none", imageUrl: null, videoUrl: null,
      isHelpNeeded: true, batteryLevel: deviceBattLevel, volts: 10, timestamp: new Date().toLocaleString(props.locale || "en"),
      timestamp_epoch: serverTimestamp(), location: gps ? { lat: gps.lat, lng: gps.lng } : null
    };
    try {
      await addDoc(collection(db, "board_posts"), sosPayload);
      setFeedView("help");
      alert("EMERGENCY BEACON TRANSMITTED ACROSS SECTOR NETWORK!");
    } catch(e) { alert("Failed to transmit emergency signal."); }
  };

  const [neonParticles, setNeonParticles] = useState<number[]>([]);
  const triggerNeonBurst = () => {
    const burstId = Date.now();
    setNeonParticles(prev => [...prev, burstId]);
    setTimeout(() => setNeonParticles(prev => prev.filter(id => id !== burstId)), 1400);
  };

  const [youtubeUrl, setYoutubeUrl] = useState("");
  const [topSpeed, setTopSpeed] = useState("");
  const [motorTemp, setMotorTemp] = useState("");

  const [pollQuestion, setPollQuestion] = useState("");
  const [pollOptions, setPollOptions] = useState<{id: number, text: string, votes: number}[]>([{ id: 1, text: "", votes: 0 }, { id: 2, text: "", votes: 0 }]);

  const [mediaFile, setMediaFile] = useState<File | null>(null);
  const [mediaPreview, setMediaPreview] = useState<string | null>(null);
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const [videoPreview, setVideoPreview] = useState<string | null>(null);
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const videoInputRef = useRef<HTMLInputElement>(null);
  const pfpInputRef = useRef<HTMLInputElement>(null);

  const [isPublishing, setIsPublishing] = useState(false);
  const [isEnhancing, setIsEnhancing] = useState(false);
  const [aiRunningId, setAiRunningId] = useState<string | null>(null);

  const [notifications, setNotifications] = useState<any[]>([]);
  const [unreadNotifs, setUnreadNotifs] = useState(0);
  const [showNotifs, setShowNotifs] = useState(false);

  const [dms, setDms] = useState<any[]>([]);
  const [unreadDms, setUnreadDms] = useState(0);
  const [showInbox, setShowInbox] = useState(false);
  const [activeDMUser, setActiveDMUser] = useState<string | null>(null);
  const [newChatTarget, setNewChatTarget] = useState("");
  const [inboxSearch, setInboxSearch] = useState<string>("");

  const [globalLightbox, setGlobalLightbox] = useState<string | null>(null);

  useEffect(() => {
    setMounted(true);
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
      setIsAppOnline(navigator.onLine);
      const handleOnline = () => setIsAppOnline(true); const handleOffline = () => setIsAppOnline(false);
      window.addEventListener('online', handleOnline); window.addEventListener('offline', handleOffline);
      return () => { window.removeEventListener('online', handleOnline); window.removeEventListener('offline', handleOffline); }
    }
  }, []);

  useEffect(() => {
    let networkListener: any;
    const initDeviceHardware = async () => {
      try {
        if (Capacitor.isNativePlatform()) {
          const batt = await Device.getBatteryInfo(); 
          if (batt.batteryLevel !== undefined) setDeviceBattLevel(Math.round(batt.batteryLevel * 100));
          const net = await CapNetwork.getStatus(); 
          setDeviceNetworkType(net.connectionType || "Mobile");
          networkListener = await CapNetwork.addListener('networkStatusChange', status => {
            setDeviceNetworkType(status.connectionType || "Mobile");
            setIsAppOnline(status.connected);
          });
        }
      } catch (e) {}
    };
    initDeviceHardware();
    return () => { if (networkListener) networkListener.remove(); };
  }, []);

  useEffect(() => {
    if (!isGpsLocked || !currentLoc) return;
    const fetchLiveWeather = async () => {
      try {
        const tempUnit = localMetric ? 'celsius' : 'fahrenheit';
        const windUnit = localMetric ? 'kmh' : 'mph';
        const res = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${currentLoc.lat}&longitude=${currentLoc.lng}&current_weather=true&daily=sunset&timezone=auto&temperature_unit=${tempUnit}&windspeed_unit=${windUnit}`);
        const data = await res.json();
        const dirs = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
        const dirStr = dirs[Math.round((data.current_weather.winddirection || 0) / 45) % 8];
        const sunsetDate = new Date(data.daily?.sunset?.[0] || Date.now());
        setWeatherData({
            temp: `${Math.round(data.current_weather.temperature)}°${localMetric ? 'C' : 'F'}`,
            wind: `${Math.round(data.current_weather.windspeed)} ${localMetric ? 'km/h' : 'mph'} ${dirStr}`,
            sunset: sunsetDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        });
      } catch(e) {}
    };
    fetchLiveWeather();
  }, [isGpsLocked, localMetric, currentLoc?.lat]); 

  useEffect(() => {
    try {
      const q = query(collection(db, "board_posts"));
      const unsubPosts = onSnapshot(q, (snapshot) => {
        const loadedPosts = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        loadedPosts.sort((a: any, b: any) => getTimeMs(b.timestamp_epoch, b.timestamp) - getTimeMs(a.timestamp_epoch, a.timestamp));
        setPosts(loadedPosts);
      });
      return () => { unsubPosts(); };
    } catch (e) {}
  }, []);

  useEffect(() => {
    if (!username) return;
    try {
      if (pfpUrl && !pfpUrl.startsWith("blob:")) { setDoc(doc(db, "user_profiles", username), { pfpUrl, userFleet, userBio, lastActive: serverTimestamp() }, { merge: true }); } 
      else { setDoc(doc(db, "user_profiles", username), { userFleet, userBio, lastActive: serverTimestamp() }, { merge: true }); }
    } catch (e) {}
  }, [username, pfpUrl, userFleet, userBio]);

  useEffect(() => {
    try {
      const unsub = onSnapshot(collection(db, "user_profiles"), (snap) => {
        const profs: Record<string, any> = {}; snap.forEach(d => { profs[d.id] = d.data(); }); setUserProfiles(profs);
      });
      return unsub;
    } catch (e) {}
  }, []);

  useEffect(() => {
    if (!username) return;
    try {
      const q = query(collection(db, "user_notifications"), where("recipient", "==", username), limit(50));
      const unsub = onSnapshot(q, (snap) => {
        const notifs = snap.docs.map(d => ({id: d.id, ...d.data()}));
        notifs.sort((a: any, b: any) => getTimeMs(b.timestamp) - getTimeMs(a.timestamp));
        setNotifications(notifs);
        let unreadCount = 0; notifs.forEach(n => { if (!n.read) unreadCount++; });
        setUnreadNotifs(unreadCount);
      });
      return unsub;
    } catch (e) {}
  }, [username]);

  useEffect(() => {
    if (!username) return;
    try {
      const q = query(collection(db, "direct_messages"), where("participants", "array-contains", username), limit(150));
      const unsub = onSnapshot(q, (snap) => {
        const allDms = snap.docs.map(d => ({id: d.id, ...d.data()}));
        allDms.sort((a: any, b: any) => getTimeMs(a.timestamp) - getTimeMs(b.timestamp));
        setDms(allDms);
        let unreadCount = 0; allDms.forEach((m: any) => { if (m.sender !== username && (!m.readBy || !m.readBy.includes(username))) unreadCount++; });
        setUnreadDms(unreadCount);
      });
      return unsub;
    } catch (e) {}
  }, [username]);

  const handleMediaPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) { triggerHaptic("LIGHT"); setVideoFile(null); setVideoPreview(null); setMediaFile(e.target.files[0]); setMediaPreview(URL.createObjectURL(e.target.files[0])); setIsComposerOpen(true); }
  };

  const handleCameraSnap = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) { triggerHaptic("LIGHT"); setVideoFile(null); setVideoPreview(null); setMediaFile(e.target.files[0]); setMediaPreview(URL.createObjectURL(e.target.files[0])); setIsComposerOpen(true); }
  };

  const handlePfpUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      try {
        triggerHaptic("LIGHT"); const base64Avatar = await compressImage(file, 256, 256, 0.85);
        setPfpUrl(base64Avatar); localStorage.setItem("soc_pfp", base64Avatar); localStorage.setItem("rural_erides_pfp", base64Avatar);
        window.dispatchEvent(new Event("settings-sync")); triggerHaptic("HEAVY");
      } catch (err) { alert("Failed to process avatar file."); }
    }
  };

  const toggleBookmark = (postId: string) => {
    triggerHaptic("LIGHT");
    setSavedPosts(prev => {
      const isSaved = prev.includes(postId); const newSaved = isSaved ? prev.filter(id => id !== postId) : [...prev, postId];
      localStorage.setItem("soc_post_bookmarks", JSON.stringify(newSaved)); return newSaved;
    });
  };

  const handleFlagPost = (postId: string) => {
    setFlaggedIds(prev => {
      const newFlags = [...prev, postId]; localStorage.setItem("soc_post_flags", JSON.stringify(newFlags)); return newFlags;
    });
  };

  const enhanceWithAI = async () => {
    if (!postText.trim() || isEnhancing) return;
    triggerHaptic("LIGHT"); setIsEnhancing(true);
    const geminiKey = getGeminiApiKey();
    if (geminiKey) {
      try {
        const prompt = `Rewrite and enhance this rider social post to be punchy, clear, and authentic for an outdoor PEV/e-mobility enthusiast community. Keep it short (under 3 sentences):\n"${postText}"`;
        const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${geminiKey}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }) });
        const data = await res.json(); const enhanced = data.candidates?.[0]?.content?.parts?.[0]?.text;
        if (enhanced) { setPostText(enhanced.trim()); triggerHaptic("HEAVY"); setIsEnhancing(false); return; }
      } catch (e) {}
    }
    setTimeout(() => { setPostText(prev => prev.trim() + "\n\n⚡ Ride safe and keep carving!"); setIsEnhancing(false); triggerHaptic("HEAVY"); }, 800);
  };

  const handlePublish = async (e: React.FormEvent) => {
    e.preventDefault(); if (!username.trim() || !postText.trim() || isPublishing) return;
    const nameSafety = checkContentSafety(username); if (!nameSafety.safe) return setSafetyWarning("Rider Callsign contains an inappropriate term.");
    const messageSafety = checkContentSafety(postText); if (!messageSafety.safe) return setSafetyWarning(`Transmission contains restricted term ("${messageSafety.blockedWord}").`);
    setSafetyWarning(null); setIsPublishing(true); triggerHaptic("HEAVY");
    
    try {
      let imageUrl: string | null = null; let ytbId = youtubeUrl ? getYoutubeId(youtubeUrl) : null;
      const cleanPfp = pfpUrl && !pfpUrl.startsWith("blob:") ? pfpUrl : null;
      if (mediaFile) { try { imageUrl = await compressImage(mediaFile, 800, 800, 0.65); } catch (err) { imageUrl = null; } }

      let pollData = null;
      if (showPollBuilder && pollQuestion.trim()) {
        const validOptions = pollOptions.filter((o: any) => o.text.trim());
        if (validOptions.length >= 2) pollData = { question: pollQuestion.trim(), options: validOptions, votedUsers: [] };
      }

      let marketData = null;
      if (postCategory === "Marketplace" || postTemplate === "market") { marketData = { price: itemPrice.trim() || "Trade / Offer", condition: itemCondition }; }

      const locUpdate = locationService.getCurrentUpdate();
      await addDoc(collection(db, "board_posts"), {
        username: username.trim(), pfpUrl: cleanPfp, fleetSignature: userFleet || null,
        content: postText.trim(), category: postCategory || "Universal Feed", pevType: pevType || "PEV", template: postTemplate || "none",
        imageUrl: imageUrl, videoUrl: null, isHelpNeeded: isHelpNeeded || false, youtubeId: ytbId || null,
        poll: pollData || null, marketplace: marketData || null,
        modTags: modTags ? modTags.split(',').map((t: string) => t.trim()).filter(Boolean) : [],
        location: locUpdate ? { lat: locUpdate.lat, lng: locUpdate.lng } : null,
        telemetry: privacyMode ? null : { speed: topSpeed ? `${topSpeed} ${localMetric ? 'km/h' : 'mph'}` : null, temp: motorTemp ? `${motorTemp} °C` : null },
        isSpeedTrap: postText.toLowerCase().includes("speed trap") || postText.toLowerCase().includes("cop") || postText.toLowerCase().includes("radar"),
        weather: "☀️ Optimal Riding Conditions", batteryLevel: deviceBattLevel !== null ? deviceBattLevel : null,
        rsvpCount: postCategory === "Group Ride" ? 1 : 0, rsvpUsers: postCategory === "Group Ride" ? [username.trim()] : [],
        difficulty: modTags.includes("black diamond") ? "Black Diamond" : modTags.includes("blue") ? "Blue Square" : "Green Circle",
        volts: 0, reactions: { fire: 0, zap: 0, skull: 0, battery: 0, rocket: 0, wrench: 0, heart: 0, haha: 0 }, comments: [],
        timestamp: new Date().toLocaleString(props.locale || "en"), timestamp_epoch: serverTimestamp(), isEdited: false
      });

      setPostText(""); localStorage.removeItem("soc_post_draft"); setMediaFile(null); setMediaPreview(null); setVideoFile(null); setVideoPreview(null); setIsHelpNeeded(false);
      setItemPrice(""); setItemCondition("Like New"); setModTags(""); setYoutubeUrl(""); setTopSpeed(""); setMotorTemp(""); 
      setShowAdvanced(false); setShowPollBuilder(false); setShowCustomizer(false); setPostTemplate("none"); setIsComposerOpen(false);
      setPollQuestion(""); setPollOptions([{ id: 1, text: "", votes: 0 }, { id: 2, text: "", votes: 0 }]);
    } catch (error: any) { setSafetyWarning("Broadcast failed: " + error.message); } finally { setIsPublishing(false); }
  };

  const handleTriggerAISolver = async (postId: string) => {
    setAiRunningId(postId); triggerHaptic("LIGHT");
    const targetPost = posts.find(p => p.id === postId); const geminiKey = getGeminiApiKey();
    if (geminiKey && targetPost) {
      try {
        const prompt = `You are an AI Co-Pilot for personal electric vehicles. An operator posted a hardware SOS: Title/Issue: "${targetPost.content}" Vehicle: "${targetPost.pevType || "PEV"}" Provide an immediate, exact 3-step technical troubleshooting fix. Avoid markdown stars. Keep it direct and clear.`;
        const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${geminiKey}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }) });
        const data = await res.json(); const aiReply = data.candidates?.[0]?.content?.parts?.[0]?.text || "Inspect battery voltage and harness continuity.";
        await updateDoc(doc(db, "board_posts", postId), { comments: arrayUnion({ id: Date.now().toString(), author: "AI CO-PILOT", authorPfp: null, text: `🛠️ AI DIAGNOSTIC TRIAGE:\n${aiReply}`, time: new Date().toLocaleTimeString(props.locale || "en", { hour: '2-digit', minute: '2-digit' }), isAI: true }) });
        triggerHaptic("HEAVY"); setAiRunningId(null); return;
      } catch (err) {}
    }
    setAiRunningId(null);
  };

  const handleVolt = async (postId: string, postUsername: string) => {
    triggerHaptic("MEDIUM"); triggerNeonBurst();
    await updateDoc(doc(db, "board_posts", postId), { volts: increment(1) });
    if (postUsername !== username) {
      await addDoc(collection(db, "user_notifications"), { recipient: postUsername, sender: username, type: 'VOLT', content: `injected 1 Volt into your transmission`, postId: postId, read: false, timestamp: serverTimestamp() });
    }
  };

  const handleRSVP = async (postId: string) => {
    triggerHaptic("MEDIUM"); const postRef = doc(db, "board_posts", postId);
    await updateDoc(postRef, { rsvpCount: increment(1), rsvpUsers: arrayUnion(username) });
  };

  const handleDeletePost = async (postId: string, postUsername: string) => {
    const isOwner = username.trim().toLowerCase() === postUsername.trim().toLowerCase() || !postUsername;
    if (isOwner || window.confirm("Delete this broadcast?")) { try { await deleteDoc(doc(db, "board_posts", postId)); triggerHaptic("HEAVY"); } catch (err) {} }
  };

  const handleNotifClick = async (n: any) => {
    triggerHaptic("LIGHT"); await updateDoc(doc(db, "user_notifications", n.id), { read: true }); setShowNotifs(false);
    if (n.type === 'DM') { setActiveDMUser(n.sender); setShowInbox(true); } else if (n.type === 'FRIEND_ADD') { setActiveProfileUser(n.sender); } else if (n.postId) { const el = document.getElementById(`post-${n.postId}`); if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' }); }
  };

  const toggleFriend = async (targetUser: string) => {
    triggerHaptic("MEDIUM");
    setFriends(prev => { const isFriend = prev.includes(targetUser); const newFriends = isFriend ? prev.filter(f => f !== targetUser) : [...prev, targetUser]; localStorage.setItem("soc_friends", JSON.stringify(newFriends)); return newFriends; });
    if (!friends.includes(targetUser)) { await addDoc(collection(db, "user_notifications"), { recipient: targetUser, sender: username, type: 'FRIEND_ADD', content: `added you to their riding crew!`, read: false, timestamp: serverTimestamp() }); }
  };

  const displayedPosts = useMemo(() => {
    return posts.filter(post => {
      if (flaggedIds.includes(post.id)) return false;
      if (feedView === "saved") return savedPosts.includes(post.id);
      if (feedView === "help") return post.isHelpNeeded || post.category === "Hardware Diagnostics" || post.category === "Trail Rescue Bounty";
      if (feedView === "Marketplace") return post.category === "Marketplace" || post.template === "market";
      if (feedView === "Group Ride") return post.category === "Group Ride";
      if (feedView === "crew") return friends.includes(post.username);
      if (feedView === "confessions") return post.category === "🌲 Trail Confessions";
      if (feedView === "VirtualGarage") return post.category === "🏎️ Virtual Garage" || post.template === "garage";
      if (feedView === "map") {
        if (!isGpsLocked || !post.location || typeof post.location.lat !== 'number') return true; 
        return calculateDistance(currentLoc.lat, currentLoc.lng, post.location.lat, post.location.lng, localMetric) <= 50; 
      }
      const safeContent = post.content ? String(post.content).toLowerCase() : ""; const safeUser = post.username ? String(post.username).toLowerCase() : ""; const queryStr = String(searchQuery || "").toLowerCase();
      return (safeContent.includes(queryStr) || safeUser.includes(queryStr)) && (feedView === "universal" || post.category === feedView);
    }).sort((a, b) => {
      if (sortBy === "trending") return (b.volts || 0) - (a.volts || 0);
      return getTimeMs(b.timestamp_epoch, b.timestamp) - getTimeMs(a.timestamp_epoch, a.timestamp); 
    });
  }, [posts, flaggedIds, feedView, savedPosts, friends, searchQuery, sortBy, username]);

  const groupedChats = useMemo(() => {
    return dms.reduce((acc: any, msg: any) => {
      if (!msg.participants || !Array.isArray(msg.participants)) return acc;
      const otherUser = msg.participants.find((p: string) => p !== username);
      if (!otherUser) return acc;
      if (!acc[otherUser] || getTimeMs(acc[otherUser].timestamp) < getTimeMs(msg.timestamp)) { acc[otherUser] = msg; }
      return acc;
    }, {});
  }, [dms, username]);

  const chatList = useMemo(() => {
    return Object.keys(groupedChats)
      .filter(user => String(user).toLowerCase().includes(String(inboxSearch || "").toLowerCase()))
      .map(key => ({ user: key, lastMessage: groupedChats[key] }))
      .sort((a, b) => getTimeMs(b.lastMessage.timestamp) - getTimeMs(a.lastMessage.timestamp));
  }, [groupedChats, inboxSearch]);

  const onlinePilots = useMemo(() => {
    const fifteenMinsAgo = Date.now() - 900000;
    return Object.keys(userProfiles).filter(user => { const prof = userProfiles[user]; const lastActiveMs = getTimeMs(prof.lastActive); return lastActiveMs > fifteenMinsAgo; });
  }, [userProfiles]);

  const allUniqueRiders = useMemo(() => { return Array.from(new Set(posts.map(p => p.username).filter(Boolean))) as string[]; }, [posts]);
  const matchingRiders = useMemo(() => { return (searchQuery || "").trim() ? allUniqueRiders.filter(r => String(r).toLowerCase().includes(String(searchQuery).toLowerCase()) && String(r).toLowerCase() !== String(username || "").toLowerCase()) : []; }, [allUniqueRiders, searchQuery, username]);

  const shareProfile = (targetUser: string) => {
    triggerHaptic("LIGHT"); const link = `Check out ${targetUser}'s riding profile on the network!`;
    if (navigator.share) navigator.share({ title: `${targetUser} - Profile`, text: link }); else { navigator.clipboard.writeText(link); alert("Profile link copied!"); }
  };

  const calculateUserLevel = useCallback((user: string) => {
    const userPosts = posts.filter(p => p.username === user);
    const postCount = userPosts.length;
    const totalVolts = userPosts.reduce((acc, p) => acc + (p.volts || 0), 0);
    const exp = (postCount * 10) + (totalVolts * 5);
    const level = Math.floor(exp / 100) + 1;
    const progress = exp % 100;
    return { level, exp, progress };
  }, [posts]);

  const getTopSpeed = useCallback((user: string) => {
    const userPosts = posts.filter(p => p.username === user && p.telemetry && p.telemetry.speed);
    if (userPosts.length === 0) return "N/A";
    const speeds = userPosts.map(p => parseFloat(String(p.telemetry.speed).split(" ")[0])).filter(s => !isNaN(s));
    if (speeds.length === 0) return "N/A";
    return `${Math.max(...speeds)} ${localMetric ? 'km/h' : 'mph'}`;
  }, [posts, localMetric]);

  const nearbyPosts = useMemo(() => {
    return posts.filter(p => {
      if (!isGpsLocked || !p.location || typeof p.location.lat !== 'number') return true; 
      return calculateDistance(currentLoc.lat, currentLoc.lng, p.location.lat, p.location.lng, localMetric) <= 50; 
    });
  }, [posts, isGpsLocked, currentLoc, localMetric]);

  const standings = useMemo(() => {
    let topSpd = { user: "Awaiting Data", speed: 0, vehicle: "N/A" };
    nearbyPosts.forEach(p => {
      if (p.telemetry?.speed) {
        const speedVal = parseFloat(String(p.telemetry.speed).split(" ")[0]);
        if (!isNaN(speedVal) && speedVal > topSpd.speed) { topSpd = { user: p.username || "Unknown", speed: speedVal, vehicle: p.fleetSignature || p.pevType || "PEV" }; }
      }
    });

    const userVolts: Record<string, number> = {};
    nearbyPosts.forEach(p => { if (p.volts && p.username) userVolts[p.username] = (userVolts[p.username] || 0) + p.volts; });
    const mvpUser = Object.keys(userVolts).sort((a, b) => userVolts[b] - userVolts[a])[0] || "Awaiting Data";
    const mvpVolts = mvpUser !== "Awaiting Data" ? userVolts[mvpUser] : 0;

    const userScoutCount: Record<string, number> = {};
    nearbyPosts.forEach(p => { if (p.category === "Trail Reports" && p.username) userScoutCount[p.username] = (userScoutCount[p.username] || 0) + 1; });
    const topScout = Object.keys(userScoutCount).sort((a, b) => userScoutCount[b] - userScoutCount[a])[0] || "Awaiting Data";
    const scoutCount = topScout !== "Awaiting Data" ? userScoutCount[topScout] : 0;

    return { topSpeedRecord: topSpd, topScout, scoutCount, mvpUser, mvpVolts };
  }, [nearbyPosts]);

  const liveStories = useMemo(() => {
    let storyPosts = posts.filter(p => (p.imageUrl && !p.imageUrl.startsWith("blob:")) || (p.videoUrl && !p.videoUrl.startsWith("blob:")));
    if (crewStoriesOnly) { storyPosts = storyPosts.filter(p => friends.includes(p.username) || p.username === username); }
    storyPosts = storyPosts.slice(0, 20);

    const uniqueUsers = new Set(); const stories = [];
    const cleanUserPfp = pfpUrl && !pfpUrl.startsWith("blob:") ? pfpUrl : null;
    stories.push({ isAdd: true, user: "Create Reel", pfp: cleanUserPfp, imageUrl: null });

    for (const p of storyPosts) {
      if (!uniqueUsers.has(p.username)) {
        uniqueUsers.add(p.username);
        const uPfp = userProfiles[p.username]?.pfpUrl;
        stories.push({ isAdd: false, user: p.username, pfp: uPfp && !uPfp.startsWith("blob:") ? uPfp : null, imageUrl: p.imageUrl || p.videoUrl });
      }
    }
    return stories;
  }, [posts, userProfiles, pfpUrl, crewStoriesOnly, friends, username]);

  if (!mounted) {
    return <div className="h-screen w-full bg-[#030303] flex items-center justify-center text-zinc-500 font-black tracking-widest uppercase text-xs animate-pulse">Initializing Universal Network...</div>;
  }

  const suggestedCrew = allUniqueRiders.filter(r => r !== username && !friends.includes(r)).slice(0, 5);
  const myPostsCount = posts.filter(p => String(p.username).toLowerCase() === String(username).toLowerCase()).length;
  const myTotalVolts = posts.filter(p => String(p.username).toLowerCase() === String(username).toLowerCase()).reduce((acc, p) => acc + (p.volts || 0), 0);

  return (
    <div className={`min-h-screen bg-transparent text-zinc-200 font-sans pb-24 selection:bg-white/20 relative z-10 ${scaleClass}`}>
      
      <AnimatePresence>
        {neonParticles.map(id => (
          <motion.div key={id} initial={{ y: '80vh', x: `${Math.random() * 80 + 10}vw`, scale: 0, opacity: 1 }} animate={{ y: '20vh', scale: 2.5, opacity: 0 }} exit={{ opacity: 0 }} transition={{ duration: 1.2, ease: "easeOut" }} className="fixed z-[99999] pointer-events-none text-4xl drop-shadow-[0_0_15px_rgba(57,255,20,0.8)]">
             {['⚡', '🔥', '🎉', '🚀', '❤️'][Math.floor(Math.random()*5)]}
          </motion.div>
        ))}
      </AnimatePresence>

      {!props.performanceMode && (
        <div className="absolute top-[5%] right-[10%] w-[600px] h-[600px] rounded-full opacity-10 blur-[150px] pointer-events-none -z-10" style={{ backgroundColor: t.hex }} />
      )}

      {/* DROP PIN MODAL */}
      <AnimatePresence>
        {showDropPinModal && (
          <div className="fixed inset-x-0 bottom-0 sm:inset-0 z-[300] bg-black/80 backdrop-blur-2xl flex items-end sm:items-center justify-center p-0 pb-24 sm:p-4">
            <motion.div initial={{ y: "100%", opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: "100%", opacity: 0 }} className="bg-zinc-950 border-t sm:border border-white/10 rounded-t-3xl sm:rounded-3xl p-6 w-full max-w-sm space-y-4 shadow-[0_-10px_40px_rgba(0,0,0,0.8)] sm:shadow-2xl">
              <div className="flex justify-between items-center">
                <div>
                  <h3 className="text-sm font-black uppercase text-white tracking-widest flex items-center gap-2"><MapPin className={`w-4 h-4 ${t.text}`}/> Drop Social Pin</h3>
                  <span className="text-[9px] font-mono text-zinc-500 block mt-0.5">
                    {draftPinCoords ? `Custom Position: ${draftPinCoords.lat.toFixed(4)}, ${draftPinCoords.lng.toFixed(4)}` : "Position: Live GPS"}
                  </span>
                </div>
                <button onClick={() => { setShowDropPinModal(false); setDraftPinCoords(null); }} className="p-1 rounded-full text-zinc-500 hover:text-white"><X className="w-4 h-4"/></button>
              </div>

              <div className="space-y-3 text-xs">
                <div>
                  <label className="text-[9px] font-mono text-zinc-400 uppercase tracking-widest block mb-1">Category</label>
                  <select value={newPinCategory} onChange={(e: any) => setNewPinCategory(e.target.value)} className="w-full min-h-[44px] bg-black/60 border border-white/10 p-2.5 rounded-xl text-white font-bold outline-none">
                    <option value="Meetup">📍 Meetup Spot</option>
                    <option value="Photo">📸 Photo Drop</option>
                    <option value="Outlet">⚡ Charging Outlet</option>
                    <option value="Hazard">⚠️ Hazard Alert</option>
                    <option value="Confession">🌲 Trail Confession</option>
                  </select>
                </div>

                <div>
                  <label className="text-[9px] font-mono text-zinc-400 uppercase tracking-widest block mb-1">Pin Title</label>
                  <input type="text" placeholder="e.g. Hidden Creek Jump" value={newPinTitle} onChange={(e) => setNewPinTitle(e.target.value)} className="w-full min-h-[44px] bg-black/60 border border-white/10 p-2.5 rounded-xl text-white font-bold outline-none" />
                </div>

                <div>
                  <label className="text-[9px] font-mono text-zinc-400 uppercase tracking-widest block mb-1">Notes / Description</label>
                  <textarea placeholder="Trail conditions, outlet location..." value={newPinNote} onChange={(e) => setNewPinNote(e.target.value)} rows={3} className="w-full min-h-[44px] bg-black/60 border border-white/10 p-2.5 rounded-xl text-white font-bold outline-none resize-none" />
                </div>

                <button onClick={handleSavePin} disabled={!newPinTitle.trim()} className={`w-full min-h-[44px] ${t.bg} text-black font-black uppercase tracking-widest rounded-xl shadow-lg active:scale-95 transition-all cursor-pointer disabled:opacity-40`}>
                  Broadcast Pin to Map
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* PIN INSPECTOR DRAWER */}
      <AnimatePresence>
        {selectedPin && (
          <div className="fixed inset-x-0 bottom-0 sm:inset-0 z-[300] bg-black/60 backdrop-blur-md flex items-end sm:items-center justify-center p-0 pb-24 sm:p-4" onClick={() => setSelectedPin(null)}>
            <motion.div initial={{ y: "100%", opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: "100%", opacity: 0 }} onClick={(e) => e.stopPropagation()} className="bg-zinc-950 border-t sm:border border-white/10 rounded-t-3xl sm:rounded-3xl p-6 w-full max-w-sm space-y-4 shadow-[0_-10px_40px_rgba(0,0,0,0.8)] sm:shadow-2xl">
              <div className="flex justify-between items-center">
                <span className="text-[9px] font-mono font-black uppercase text-cyan-400 tracking-widest">{selectedPin.category} Pin</span>
                <button onClick={() => setSelectedPin(null)} className="p-1 text-zinc-500 hover:text-white"><X className="w-4 h-4"/></button>
              </div>
              <div>
                <h3 className="text-base font-black text-white">{selectedPin.title}</h3>
                <p className="text-xs text-zinc-400 mt-1">{selectedPin.note}</p>
                {currentLoc && (
                  <span className="text-[10px] font-mono text-emerald-400 font-bold block mt-2 flex items-center gap-1">
                    <Navigation className="w-3 h-3" />
                    {calculateDistance(currentLoc.lat, currentLoc.lng, selectedPin.lat, selectedPin.lng, localMetric).toFixed(1)} {localMetric ? 'km' : 'miles'} away
                  </span>
                )}
              </div>
              <div className="flex justify-between items-center pt-3 border-t border-white/10 text-xs">
                <span className="text-[10px] font-mono text-zinc-500">Pinned by: <strong className="text-white">{selectedPin.creator}</strong></span>
                <div className="flex items-center gap-2">
                  {selectedPin.creator === username && (
                    <button onClick={() => handleDeletePin(selectedPin.id)} className="px-3 min-h-[36px] bg-rose-950/60 border border-rose-500/40 text-rose-300 font-black uppercase tracking-widest rounded-xl active:scale-95 shadow-sm">
                      Delete
                    </button>
                  )}
                  <button onClick={handleBoostPin} className={`px-4 min-h-[36px] ${t.bg} text-black font-black uppercase tracking-widest rounded-xl flex items-center gap-1.5 active:scale-95 shadow-sm`}>
                    <Zap className="w-3 h-3"/> {selectedPin.volts} Boosts
                  </button>
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* PEV SLANG DECODER DRAWER */}
      <AnimatePresence>
        {showSlangDecoder && (
          <div className="fixed inset-x-0 bottom-0 sm:inset-0 z-[250] bg-black/80 backdrop-blur-2xl flex items-end sm:items-center justify-center p-0 pb-24 sm:p-4">
            <motion.div initial={{ y: "100%", opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: "100%", opacity: 0 }} className="bg-zinc-950 border-t sm:border border-white/10 rounded-t-3xl sm:rounded-3xl p-6 w-full max-w-md shadow-[0_-10px_40px_rgba(0,0,0,0.8)] sm:shadow-2xl space-y-4">
              <div className="flex justify-between items-center pb-3 border-b border-white/10">
                <h3 className="text-sm font-black uppercase text-white tracking-widest flex items-center gap-2"><HelpCircle className="w-4 h-4 text-amber-400"/> PEV Slang Decoder</h3>
                <button onClick={() => setShowSlangDecoder(false)} className="p-2 text-zinc-500 hover:text-white rounded-full bg-white/5"><X className="w-4 h-4"/></button>
              </div>
              <div className="space-y-3 max-h-80 overflow-y-auto custom-scrollbar pr-1 pb-4">
                {PEV_SLANG_DICTIONARY.map(item => (
                  <div key={item.term} className="bg-black/50 border border-white/5 p-4 rounded-2xl shadow-inner">
                    <span className="text-xs font-black uppercase text-emerald-400 tracking-wider block mb-1">{item.term}</span>
                    <p className="text-xs text-zinc-300 font-medium leading-relaxed">{item.def}</p>
                  </div>
                ))}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* GLOBAL LIGHTBOX */}
      <AnimatePresence>
        {globalLightbox && (
          <div className="fixed inset-0 z-[99999] bg-black/95 flex items-center justify-center p-4 backdrop-blur-2xl" onClick={() => setGlobalLightbox(null)}>
            <motion.div initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.9, opacity: 0 }} className="relative max-w-full max-h-[90vh]">
              <img src={globalLightbox} alt="Expanded Story Media" className="max-w-full max-h-full object-contain rounded-2xl border border-white/20 shadow-[0_0_50px_rgba(0,0,0,0.9)]" />
              <button 
                onClick={(e) => { e.stopPropagation(); triggerHaptic("HEAVY"); setGlobalLightbox(null); setIsComposerOpen(true); setPostCategory("Video Requests"); setPostText(`🎬 REEL DUET REMIX with original creator! Catch my split reaction:`); }}
                className="absolute bottom-6 left-1/2 -translate-x-1/2 bg-rose-600 hover:bg-rose-500 text-white px-6 min-h-[44px] rounded-full font-black uppercase tracking-widest text-xs shadow-2xl flex items-center justify-center gap-2 transition-all active:scale-95 cursor-pointer"
              >
                <Video className="w-4 h-4"/> Remix &amp; Duet
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* NOTIFICATIONS MODAL */}
      <AnimatePresence>
        {showNotifs && (
          <div className="fixed inset-x-0 bottom-0 sm:inset-0 z-[250] bg-black/80 backdrop-blur-2xl flex items-end sm:items-center justify-center p-0 sm:p-4 pb-safe">
            <motion.div initial={{ y: "100%", opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: "100%", opacity: 0 }} className={`app-card ${props.glassBlurClass || ''} bg-black/95 border-t sm:border border-white/10 w-full max-w-md h-[85vh] sm:h-[80vh] flex flex-col overflow-hidden shadow-[0_-10px_60px_rgba(0,0,0,0.8)] sm:shadow-2xl relative rounded-t-3xl sm:${props.cornerStyle || 'rounded-3xl'}`}>
              <div className="flex justify-between items-center p-5 border-b border-white/10 shrink-0">
                <h3 className={`text-sm font-black uppercase tracking-widest ${t.text} flex items-center gap-2`}><Bell className="w-4 h-4"/> System Alerts</h3>
                <button type="button" onClick={() => setShowNotifs(false)} className="text-zinc-500 hover:text-white p-2.5 bg-black/50 rounded-full border border-white/10 cursor-pointer active:scale-95"><X className="w-4 h-4"/></button>
              </div>
              <div className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-3 overscroll-contain">
                {notifications.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-zinc-600 font-mono text-[10px] uppercase font-bold tracking-widest">No notifications.</div>
                ) : (
                  notifications.map(n => (
                    <div key={n.id} onClick={() => handleNotifClick(n)} className={`p-4 rounded-2xl border transition-all cursor-pointer ${n.read ? 'bg-black/40 border-white/5' : `${t.bgSubtle} ${t.borderSubtle}`}`}>
                      <div className="flex items-center gap-2 mb-1.5"><span className="text-[10px] font-black uppercase text-zinc-300">{n.sender}</span></div>
                      <p className="text-xs text-zinc-200 font-medium leading-relaxed">{n.content}</p>
                    </div>
                  ))
                )}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      <DirectMessagesModal 
        showInbox={showInbox} setShowInbox={setShowInbox} activeDMUser={activeDMUser} setActiveDMUser={setActiveDMUser} 
        username={username} pfpUrl={pfpUrl} userProfiles={userProfiles} newChatTarget={newChatTarget} setNewChatTarget={setNewChatTarget} 
        inboxSearch={inboxSearch} setInboxSearch={setInboxSearch} chatList={chatList} dms={dms} themeColors={t} getTimeMs={getTimeMs}
        glassBlurClass={props.glassBlurClass} cornerStyle={props.cornerStyle}
      />

      <UserProfileModal 
        activeProfileUser={activeProfileUser} setActiveProfileUser={setActiveProfileUser} userProfiles={userProfiles} posts={posts} 
        username={username} friends={friends} toggleFriend={toggleFriend} setActiveDMUser={setActiveDMUser} setShowInbox={setShowInbox}
        setIsSettingsOpen={setIsSettingsOpen} shareProfile={shareProfile} calculateUserLevel={calculateUserLevel} getTopSpeed={getTopSpeed} 
        themeColors={t} glassBlurClass={props.glassBlurClass} cornerStyle={props.cornerStyle}
      />

      <BoardSettingsModal
        isSettingsOpen={isSettingsOpen} setIsSettingsOpen={setIsSettingsOpen} username={username} pfpUrl={pfpUrl} userFleet={userFleet} 
        setUserFleet={setUserFleet} userBio={userBio} setUserBio={setUserBio} defaultPostCategory={defaultPostCategory} 
        setDefaultPostCategory={setDefaultPostCategory} dataSaverMode={dataSaverMode} setDataSaverMode={setDataSaverMode} 
        privacyMode={privacyMode} setPrivacyMode={setPrivacyMode} pfpInputRef={pfpInputRef} handlePfpUpload={handlePfpUpload} 
        triggerHaptic={triggerHaptic} themeColors={t} CATEGORIES={CATEGORIES} glassBlurClass={props.glassBlurClass} cornerStyle={props.cornerStyle}
      />

      {/* TOPBAR HUD */}
      <div className="sticky top-0 z-40 p-3 sm:p-4 pb-1 sm:pb-2 pt-safe">
        <nav className={`w-full ${t.blur} border border-white/10 rounded-3xl flex flex-col px-4 sm:px-5 py-3 sm:py-4 shadow-[0_8px_32px_0_rgba(0,0,0,0.36)] backdrop-blur-2xl`}>
          <div className="flex items-center justify-between mb-3 sm:mb-4 max-w-4xl mx-auto w-full gap-2">
            <div className="flex items-center gap-3 min-w-0">
              <div className={`${t.bg} p-2 rounded-xl ${t.glow} relative shrink-0`}>
                <Globe className="w-5 h-5 text-black relative z-10" />
              </div>
              <div className="min-w-0">
                 <h1 className="text-sm font-black uppercase tracking-widest text-white drop-shadow-md truncate">Global Network</h1>
                 <div className="flex items-center gap-2 mt-0.5">
                   <p className={`text-[9px] ${t.text} font-bold uppercase tracking-wider flex items-center gap-1 truncate`}>
                     Universal Avionics
                     {!isAppOnline && <span className="bg-rose-600 text-white px-1.5 py-0.5 rounded text-[7px] animate-pulse ml-1 shrink-0"><WifiOff className="w-2 h-2 inline"/> OFFLINE</span>}
                   </p>
                   <div className="hidden sm:flex items-center gap-1.5 px-2 py-0.5 bg-black/50 border border-white/10 rounded-md text-[7px] font-mono text-zinc-400 shrink-0">
                      <span>{deviceNetworkType}</span>
                      {deviceBattLevel !== null && <span>{deviceBattLevel}%</span>}
                   </div>
                 </div>
              </div>
            </div>
            
            <div className="flex items-center gap-1 sm:gap-2 shrink-0">
              <button 
                onClick={handleQuickSosBeacon}
                className="h-10 sm:h-11 px-3 sm:px-4 bg-rose-600 hover:bg-rose-500 text-white rounded-xl sm:rounded-2xl text-[10px] font-black uppercase tracking-wider flex items-center justify-center gap-1.5 shadow-[0_0_20px_rgba(225,29,72,0.5)] active:scale-95 transition-all shrink-0 cursor-pointer"
                title="Broadcast SOS Beacon"
              >
                <ShieldAlert className="w-4 h-4 animate-bounce" /> <span className="hidden sm:inline">SOS</span>
              </button>

              <button onClick={() => setShowNotifs(true)} className="relative w-10 h-10 sm:w-11 sm:h-11 flex items-center justify-center bg-black/50 border border-white/10 rounded-xl sm:rounded-2xl cursor-pointer active:scale-95 transition-all shadow-inner shrink-0">
                <Bell className="w-4 h-4 text-zinc-300" />
                {unreadNotifs > 0 && <span className={`absolute -top-1 -right-1 rounded-full h-4 w-4 ${t.bg} text-black text-[8px] font-black flex items-center justify-center`}>{unreadNotifs}</span>}
              </button>

              <button onClick={() => setShowInbox(true)} className="relative w-10 h-10 sm:w-11 sm:h-11 flex items-center justify-center bg-black/50 border border-white/10 rounded-xl sm:rounded-2xl cursor-pointer active:scale-95 transition-all shadow-inner shrink-0">
                <Mail className="w-4 h-4 text-zinc-300" />
                {unreadDms > 0 && <span className={`absolute -top-1 -right-1 rounded-full h-4 w-4 ${t.bg} text-black text-[8px] font-black flex items-center justify-center`}>{unreadDms}</span>}
              </button>

              <button type="button" onClick={() => setIsSettingsOpen(true)} className="h-10 sm:h-11 flex items-center gap-2 bg-black/50 border border-white/10 p-1 sm:px-3 sm:py-1.5 rounded-xl sm:rounded-2xl cursor-pointer active:scale-95 transition-all shadow-inner shrink-0">
                <span className="text-[10px] font-black uppercase text-zinc-100 hidden sm:block">{username}</span>
                <div className={`w-8 h-8 sm:w-9 sm:h-9 rounded-lg bg-black border ${t.border} flex items-center justify-center font-black ${t.text} text-xs overflow-hidden shadow-sm relative shrink-0`}>
                  {pfpUrl && !pfpUrl.startsWith("blob:") ? <img src={pfpUrl} alt={username} className="w-full h-full object-cover"/> : (username ? username.charAt(0).toUpperCase() : <Settings className="w-3.5 h-3.5"/>)}
                  <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-emerald-500 rounded-full border-2 border-black" title="Online" />
                </div>
              </button>
            </div>
          </div>

          <div className="max-w-4xl mx-auto w-full mb-3">
            <RiderLevelBadge username={username} postsCount={myPostsCount} totalVolts={myTotalVolts} themeColors={t} userProfiles={userProfiles} />
          </div>

          {/* --- 🔥 TRENDING HASHTAG ANALYTICS TICKER (TWITTER/X STYLE) 🔥 --- */}
          <div className="max-w-4xl mx-auto w-full mb-3 flex items-center gap-2 overflow-x-auto custom-scrollbar pb-1">
            <span className="text-[9px] font-mono font-black uppercase text-zinc-500 shrink-0 flex items-center gap-1">
              <TrendingUp className="w-3 h-3 text-cyan-400"/> Trending:
            </span>
            {TRENDING_TAGS.map(tItem => (
              <button
                key={tItem.tag}
                onClick={() => setSearchQuery(tItem.tag)}
                className="px-3 py-1 bg-white/5 hover:bg-white/10 border border-white/10 rounded-full text-[9px] font-mono font-bold text-zinc-300 shrink-0 cursor-pointer active:scale-95 transition-all flex items-center gap-1"
              >
                <span className="text-cyan-400">{tItem.tag}</span>
                <span className="text-[7px] text-zinc-500">({tItem.posts})</span>
              </button>
            ))}
          </div>

          {/* ADVANCED MODULE HUB */}
          <div className="max-w-4xl mx-auto w-full mb-3 flex items-center gap-2 overflow-x-auto custom-scrollbar pb-1">
             <button type="button" onClick={() => { triggerHaptic("LIGHT"); setShowSoundboard(!showSoundboard); }} className={`flex items-center justify-center min-h-[36px] px-4 rounded-full text-[10px] font-bold transition-colors cursor-pointer shrink-0 ${showSoundboard ? 'bg-fuchsia-600 text-white shadow-lg' : 'bg-white/5 hover:bg-white/10 text-zinc-300 border border-white/5'}`}><Radio className="w-3.5 h-3.5 mr-1.5 text-fuchsia-400"/> Soundboard</button>
             <button type="button" onClick={() => { triggerHaptic("LIGHT"); setShowSlangDecoder(true); }} className="flex items-center justify-center min-h-[36px] px-4 rounded-full text-[10px] font-bold transition-colors cursor-pointer shrink-0 bg-white/5 hover:bg-white/10 text-zinc-300 border border-white/5"><HelpCircle className="w-3.5 h-3.5 mr-1.5 text-amber-400"/> PEV Decoder</button>
             <button type="button" onClick={() => { if(typeof triggerHaptic==='function') triggerHaptic("LIGHT"); setFeedView("Group Ride"); }} className={`flex items-center justify-center min-h-[36px] px-4 rounded-full text-[10px] font-bold transition-colors cursor-pointer shrink-0 ${feedView === 'Group Ride' ? 'bg-purple-600 text-white shadow-lg' : 'bg-white/5 hover:bg-white/10 text-zinc-300 border border-white/5'}`}><Users className="w-3.5 h-3.5 mr-1.5 text-purple-400"/> Ride Match</button>
             <button type="button" onClick={() => { if(typeof triggerHaptic==='function') triggerHaptic("LIGHT"); setFeedView("help"); }} className={`flex items-center justify-center min-h-[36px] px-4 rounded-full text-[10px] font-bold transition-colors cursor-pointer shrink-0 ${feedView === 'help' ? 'bg-rose-600 text-white shadow-lg' : 'bg-white/5 hover:bg-white/10 text-zinc-300 border border-white/5'}`}><ShieldCheck className="w-3.5 h-3.5 mr-1.5 text-rose-400"/> SOS Bounties</button>

<Map className="w-3.5 h-3.5 mr-1.5 text-blue-400"/>
<button type="button" onClick={() => { if(typeof triggerHaptic==='function') triggerHaptic("LIGHT"); setFeedView("map"); }} className={`flex items-center justify-center min-h-[36px] px-4 rounded-full text-[10px] font-bold transition-colors cursor-pointer shrink-0 ${feedView === 'map' ? 'bg-blue-600 text-white shadow-lg' : 'bg-white/5 hover:bg-white/10 text-zinc-300 border border-white/5'}`}> Community Map</button>
             <button type="button" onClick={() => { if(typeof triggerHaptic==='function') triggerHaptic("LIGHT"); setFeedView("Marketplace"); }} className={`flex items-center justify-center min-h-[36px] px-4 rounded-full text-[10px] font-bold transition-colors cursor-pointer shrink-0 ${feedView === 'Marketplace' ? 'bg-emerald-600 text-white shadow-lg' : 'bg-white/5 hover:bg-white/10 text-zinc-300 border border-white/5'}`}><Search className="w-3.5 h-3.5 mr-1.5 text-emerald-400"/> Marketplace</button>
          </div>
          
          <AnimatePresence>
            {showSoundboard && (
              <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="max-w-4xl mx-auto w-full mb-3 overflow-hidden">
                <div className="flex gap-2 overflow-x-auto pb-2 custom-scrollbar">
                  <button onClick={() => playSynthesizedTaunt(150, 'sawtooth')} className="min-h-[40px] px-4 bg-black/60 border border-fuchsia-500/30 rounded-full text-[10px] font-black uppercase text-amber-400 hover:bg-white/10 transition-colors shadow-lg shrink-0 flex items-center gap-1.5 cursor-pointer"><Mic2 className="w-3 h-3"/> Airhorn</button>
                  <button onClick={() => playSynthesizedTaunt(50, 'square')} className="min-h-[40px] px-4 bg-black/60 border border-fuchsia-500/30 rounded-full text-[10px] font-black uppercase text-red-400 hover:bg-white/10 transition-colors shadow-lg shrink-0 flex items-center gap-1.5 cursor-pointer"><Flame className="w-3 h-3"/> Rev Engine</button>
                  <button onClick={() => playSynthesizedTaunt(400, 'triangle')} className="min-h-[40px] px-4 bg-black/60 border border-fuchsia-500/30 rounded-full text-[10px] font-black uppercase text-cyan-400 hover:bg-white/10 transition-colors shadow-lg shrink-0 flex items-center gap-1.5 cursor-pointer"><Zap className="w-3 h-3"/> Turbo</button>
                  <button onClick={() => playSynthesizedTaunt(900, 'sawtooth')} className="min-h-[40px] px-4 bg-black/60 border border-fuchsia-500/30 rounded-full text-[10px] font-black uppercase text-purple-400 hover:bg-white/10 transition-colors shadow-lg shrink-0 flex items-center gap-1.5 cursor-pointer"><Radio className="w-3 h-3"/> Laser</button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <div className="max-w-4xl mx-auto w-full mb-3 pb-3 border-b border-white/10 flex items-center gap-3 overflow-x-auto custom-scrollbar">
            <div className="flex items-center gap-1.5 text-[9px] font-mono font-black uppercase text-emerald-400 shrink-0">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" /> Live Pilots ({onlinePilots.length})
            </div>
            {onlinePilots.length === 0 ? (
              <span className="text-[9px] font-mono text-zinc-500 uppercase">No active operators on board</span>
            ) : (
              onlinePilots.map(pilot => {
                const pProfile = userProfiles[pilot] || {};
                const pfp = pProfile.pfpUrl && !pProfile.pfpUrl.startsWith("blob:") ? pProfile.pfpUrl : null;
                return (
                  <button
                    key={pilot}
                    type="button"
                    onClick={() => { triggerHaptic("LIGHT"); setPostText(prev => `${prev} @${pilot} `); setIsComposerOpen(true); }}
                    className="flex items-center gap-2 px-2.5 py-1.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-[10px] font-bold text-white shrink-0 cursor-pointer active:scale-95 transition-all shadow-sm"
                    title="Click to tag in post"
                  >
                    <div className="relative w-5 h-5 rounded-full bg-zinc-800 border border-white/10 flex items-center justify-center text-[8px] overflow-hidden shrink-0">
                      {pfp ? <img src={pfp} alt={pilot} className="w-full h-full object-cover" /> : pilot.charAt(0).toUpperCase()}
                      <span className="absolute bottom-0 right-0 w-1.5 h-1.5 rounded-full bg-emerald-500 border border-black" />
                    </div>
                    <span>@{pilot}</span>
                  </button>
                );
              })
            )}
          </div>

          <div className="relative flex flex-col gap-2 max-w-4xl mx-auto w-full">
            <div className="flex-1 relative flex items-center gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-4 top-3.5 w-4 h-4 text-zinc-500" />
                <input type="text" placeholder="Search riders, fleets, or posts..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="w-full min-h-[44px] sm:min-h-[48px] bg-black/50 border border-white/10 rounded-full py-2.5 pl-11 pr-4 text-xs font-bold text-white outline-none shadow-inner" />
              </div>
              <button 
                type="button" 
                onClick={() => { triggerHaptic("LIGHT"); setSortBy(prev => prev === "newest" ? "trending" : "newest"); }}
                className="min-h-[44px] sm:min-h-[48px] px-5 bg-black/50 border border-white/10 text-zinc-300 rounded-full text-[10px] font-mono font-black uppercase flex items-center justify-center gap-1.5 shrink-0 cursor-pointer active:scale-95 transition-all shadow-inner"
              >
                {sortBy === "newest" ? <><Clock className="w-3.5 h-3.5 text-cyan-400"/> Newest</> : <><TrendingUp className="w-3.5 h-3.5 text-amber-400"/> Trending</>}
              </button>
            </div>
            {matchingRiders.length > 0 && (
              <div className="flex items-center gap-2 overflow-x-auto custom-scrollbar pb-1 pt-1">
                <span className="text-[9px] font-mono font-black text-zinc-500 uppercase shrink-0 pl-1">Matching Riders:</span>
                {matchingRiders.map((riderName: string) => (
                  <button key={riderName} type="button" onClick={() => setActiveProfileUser(riderName)} className={`min-h-[36px] px-4 bg-black/60 border ${t.borderSubtle} ${t.text} rounded-full text-[9px] font-black uppercase tracking-widest shrink-0 cursor-pointer active:scale-95 shadow-sm`}>
                    {riderName}
                  </button>
                ))}
              </div>
            )}
          </div>
        </nav>
      </div>

      <div className="p-3 sm:p-4 space-y-5 sm:space-y-6 max-w-3xl mx-auto mt-2">

        {/* LIVE SECTOR TELEMETRY & CONDITIONS BANNER */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 grid grid-cols-3 gap-3 text-center shadow-inner font-mono">
          <div className="flex flex-col items-center">
            <span className="text-[9px] uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1 mb-1">
              <Thermometer className="w-3.5 h-3.5 text-amber-400"/> Ambient
            </span>
            <span className="text-xs font-black text-white">
              {weatherData ? weatherData.temp : <Loader2 className="w-3 h-3 animate-spin text-zinc-500"/>}
            </span>
          </div>
          <div className="flex flex-col items-center border-x border-white/10">
            <span className="text-[9px] uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1 mb-1">
              <Wind className="w-3.5 h-3.5 text-sky-400"/> Wind Speed
            </span>
            <span className="text-xs font-black text-white">
              {weatherData ? weatherData.wind : <Loader2 className="w-3 h-3 animate-spin text-zinc-500"/>}
            </span>
          </div>
          <div className="flex flex-col items-center">
            <span className="text-[9px] uppercase tracking-wider text-zinc-500 font-bold flex items-center gap-1 mb-1">
              <Sun className="w-3.5 h-3.5 text-yellow-400"/> Sunset
            </span>
            <span className="text-xs font-black text-emerald-400">
              {weatherData ? weatherData.sunset : <Loader2 className="w-3 h-3 animate-spin text-zinc-500"/>}
            </span>
          </div>
        </div>

        {/* DYNAMIC WEEKLY HASHTAG BOUNTY */}
        {feedView === "universal" && activeBounty && (
          <div className={`rounded-3xl p-5 flex items-center justify-between transition-all shadow-lg mb-4 mt-2 ${activeBounty.containerClass}`}>
            <div className="flex items-center gap-3">
              <div className={`w-12 h-12 rounded-full flex items-center justify-center border ${activeBounty.iconClass}`}>
                {activeBounty.icon}
              </div>
              <div>
                <span className={`text-[10px] font-black uppercase tracking-widest block mb-0.5 ${activeBounty.textClass}`}>{activeBounty.title}</span>
                <h3 className="text-base font-black text-white uppercase tracking-wider">{activeBounty.tag}</h3>
              </div>
            </div>
            <button 
              onClick={() => { triggerHaptic("LIGHT"); setPostText(`${activeBounty.tag} `); setIsComposerOpen(true); }}
              className={`px-5 min-h-[44px] text-white rounded-full text-[10px] font-black uppercase tracking-widest shadow-md active:scale-95 transition-all flex items-center justify-center gap-1.5 cursor-pointer ${activeBounty.btnClass}`}
            >
              <Zap className="w-3 h-3"/> +{activeBounty.xp} XP Enter
            </button>
          </div>
        )}

        {/* QUICK COMPOSE BAR */}
        {feedView === "universal" && (
          <div className="bg-black/60 border border-white/10 rounded-3xl p-4 shadow-xl backdrop-blur-md mb-4 mt-2">
            <div 
              onClick={() => { triggerHaptic("LIGHT"); setIsComposerOpen(true); }}
              className="flex items-center gap-3 cursor-pointer group"
            >
              <div className={`w-10 h-10 rounded-full bg-black border ${t.borderSubtle} flex items-center justify-center font-black ${t.text} text-xs overflow-hidden shrink-0 shadow-inner group-hover:scale-105 transition-transform`}>
                {pfpUrl && !pfpUrl.startsWith("blob:") ? <img src={pfpUrl} alt={username} className="w-full h-full object-cover"/> : username.charAt(0).toUpperCase()}
              </div>
              <div className="flex-1 min-h-[46px] bg-white/5 border border-white/5 group-hover:border-white/20 transition-colors rounded-full px-4 flex items-center shadow-inner">
                <span className="text-xs font-bold text-zinc-400">What's on your mind, {username.toUpperCase()}?</span>
              </div>
            </div>

            <div className="flex items-center justify-between gap-2 mt-4 pt-3 border-t border-white/10 px-1">
              <button 
                onClick={() => { triggerHaptic("LIGHT"); setIsComposerOpen(true); setTimeout(() => fileInputRef.current?.click(), 100); }} 
                className="flex items-center gap-1.5 text-[10px] font-black uppercase tracking-wider text-zinc-300 hover:text-white transition-colors cursor-pointer active:scale-95"
              >
                <ImageIcon className="w-4 h-4" /> Photo/Video
              </button>
              
              <button 
                onClick={handleQuickSosBeacon}
                className="flex items-center gap-1.5 text-[10px] font-black uppercase tracking-wider text-rose-400 hover:text-rose-300 transition-colors cursor-pointer active:scale-95"
              >
                <ShieldAlert className="w-4 h-4" /> SOS Beacon
              </button>

              <button 
                onClick={() => { triggerHaptic("LIGHT"); setPostCategory("Group Ride"); setIsComposerOpen(true); }} 
                className="flex items-center gap-1.5 text-[10px] font-black uppercase tracking-wider text-zinc-300 hover:text-white transition-colors cursor-pointer active:scale-95"
              >
                <Users className="w-4 h-4" /> Event
              </button>
            </div>
          </div>
        )}

        {/* STORY REELS TRAY */}
        {feedView === "universal" && (
          <div className="mb-2">
            <div className="flex justify-between items-end mb-3 px-2">
              <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Active Pilot Reels (24h Ephemeral)</span>
              <button onClick={() => { triggerHaptic("LIGHT"); setCrewStoriesOnly(!crewStoriesOnly); }} className={`text-[9px] font-black uppercase tracking-widest px-4 min-h-[36px] rounded-full border transition-all cursor-pointer ${crewStoriesOnly ? 'bg-purple-600 text-white border-purple-500 shadow-lg' : 'bg-black/50 text-zinc-400 border-white/10 hover:text-white'}`}>
                {crewStoriesOnly ? "Crew Squad Only" : "All Network"}
              </button>
            </div>
            <div className="flex gap-3 overflow-x-auto custom-scrollbar pb-2 px-1">
              {liveStories.map((story, idx) => (
                <div 
                  key={idx} 
                  onClick={() => { if (story.isAdd) { setIsComposerOpen(true); setTimeout(() => fileInputRef.current?.click(), 100); } else if (story.imageUrl) { setGlobalLightbox(story.imageUrl); } }}
                  className={`relative w-28 h-44 sm:w-32 sm:h-52 rounded-3xl overflow-hidden shrink-0 shadow-lg cursor-pointer ${story.isAdd ? 'bg-zinc-900 border border-white/20' : `bg-black border-2 transition-colors ${friends.includes(story.user) ? 'border-purple-500' : 'border-transparent hover:border-emerald-400'}`}`}
                >
                  {story.isAdd ? (
                    <div className="w-full h-full flex flex-col items-center justify-center gap-3">
                      <div className="w-12 h-12 rounded-full bg-white text-black flex items-center justify-center font-black"><Plus className="w-6 h-6"/></div>
                      <span className="text-[10px] font-black uppercase text-white tracking-widest text-center px-2">Create Reel</span>
                    </div>
                  ) : (
                    <>
                      <img src={story.imageUrl} alt="" onError={(e) => { (e.target as HTMLElement).style.opacity = '0.2'; }} className="w-full h-full object-cover opacity-60 hover:opacity-100 transition-opacity" />
                      <div className="absolute top-3 left-3 w-10 h-10 rounded-full border-2 border-emerald-500 bg-black flex items-center justify-center overflow-hidden">
                        {story.pfp && !story.pfp.startsWith("blob:") ? <img src={story.pfp} className="w-full h-full object-cover"/> : <span className="text-xs font-bold text-white">{story.user.charAt(0)}</span>}
                      </div>
                      <span className="absolute bottom-3 left-3 text-[11px] font-bold text-white shadow-black drop-shadow-md truncate w-[85%]">{story.user}</span>
                      {friends.includes(story.user) && <span className="absolute top-3 right-3 w-2.5 h-2.5 rounded-full bg-purple-500 animate-pulse shadow-[0_0_10px_#a855f7]" />}
                    </>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {feedView === "map" && (
          <OpenSourceSocialMap 
            pins={socialPins} 
            userLoc={currentLoc} 
            onSelectPin={(pin: SocialPin) => setSelectedPin(pin)} 
            onDropPin={() => { setDraftPinCoords(null); setShowDropPinModal(true); }} 
            onMapClick={(lat: number, lng: number) => { setDraftPinCoords({ lat, lng }); setShowDropPinModal(true); }}
            draftPinCoords={draftPinCoords} themeColors={t} useMetric={localMetric} onDeletePin={handleDeletePin} currentUsername={username}
          />
        )}

        {feedView === "Group Ride" && (
          <button 
            onClick={() => { triggerHaptic("HEAVY"); setPostCategory("Group Ride"); const gps = locationService.getCurrentUpdate(); setPostText(`📍 PIT STOP PIN DROPPED: Regrouping! Coordinates: [${gps ? `${gps.lat.toFixed(4)}, ${gps.lng.toFixed(4)}` : "Live Sector"}]. Roll up and form the crew!`); setIsComposerOpen(true); }} 
            className="w-full py-5 bg-purple-600 hover:bg-purple-500 text-white rounded-3xl font-black uppercase text-sm tracking-widest shadow-[0_0_30px_rgba(147,51,234,0.4)] flex items-center justify-center gap-3 transition-transform active:scale-95 cursor-pointer"
          >
            <MapPin className="w-5 h-5"/> Drop Live Group Pit Stop Pin
          </button>
        )}

        <PostComposer 
          isComposerOpen={isComposerOpen} setIsComposerOpen={setIsComposerOpen} postText={postText} setPostText={setPostText} 
          postCategory={postCategory} setPostCategory={setPostCategory} pevType={pevType} setPevType={setPevType} 
          postTemplate={postTemplate} setPostTemplate={setPostTemplate} isHelpNeeded={isHelpNeeded} setIsHelpNeeded={setIsHelpNeeded} 
          itemPrice={itemPrice} setItemPrice={setItemPrice} itemCondition={itemCondition} setItemCondition={setItemCondition} 
          modTags={modTags} setModTags={setModTags} username={username} pfpUrl={pfpUrl} useMetric={localMetric} 
          handlePublish={handlePublish} isPublishing={isPublishing} safetyWarning={safetyWarning} setSafetyWarning={setSafetyWarning} 
          mediaPreview={mediaPreview} setMediaFile={setMediaFile} setMediaPreview={setMediaPreview} videoPreview={videoPreview} 
          setVideoFile={setVideoFile} setVideoPreview={setVideoPreview} fileInputRef={fileInputRef} cameraInputRef={cameraInputRef} 
          handleMediaPick={handleMediaPick} handleCameraSnap={handleCameraSnap} showAdvanced={showAdvanced} setShowAdvanced={setShowAdvanced} 
          showPollBuilder={showPollBuilder} setShowPollBuilder={setShowPollBuilder} showCustomizer={showCustomizer} setShowCustomizer={setShowCustomizer} 
          youtubeUrl={youtubeUrl} setYoutubeUrl={setYoutubeUrl} topSpeed={topSpeed} setTopSpeed={setTopSpeed} motorTemp={motorTemp} 
          setMotorTemp={setMotorTemp} pollQuestion={pollQuestion} setPollQuestion={setPollQuestion} pollOptions={pollOptions} 
          setPollOptions={setPollOptions} isDictating={isDictating} toggleVoiceDictation={toggleVoiceDictation} enhanceWithAI={enhanceWithAI} 
          isEnhancing={isEnhancing} triggerHaptic={triggerHaptic} themeColors={t} PEV_TEMPLATES={PEV_TEMPLATES} CATEGORIES={CATEGORIES} 
          PEV_TYPES={PEV_TYPES} CONDITION_TYPES={CONDITION_TYPES} glassBlurClass={props.glassBlurClass} cornerStyle={props.cornerStyle} userProfiles={userProfiles}
        />

        {/* FEED FILTER TABS */}
        <div className="flex gap-2 overflow-x-auto pb-2 pt-2 custom-scrollbar items-center whitespace-nowrap px-1">
          <button type="button" onClick={() => setFeedView("universal")} className={`flex-shrink-0 min-h-[44px] px-5 rounded-full text-[10px] font-black uppercase tracking-widest border cursor-pointer active:scale-95 transition-all ${feedView === "universal" ? `${t.bg} text-black ${t.border}` : "bg-black/40 text-zinc-400 border-white/10"}`}>Universal Feed</button>
          <button type="button" onClick={() => setFeedView("crew")} className={`flex-shrink-0 min-h-[44px] px-5 rounded-full text-[10px] font-black uppercase tracking-widest border cursor-pointer active:scale-95 transition-all ${feedView === "crew" ? `${t.bg} text-black ${t.border}` : "bg-black/40 text-zinc-400 border-white/10"}`}>My Crew ({friends.length})</button>
          <button type="button" onClick={() => setFeedView("Group Ride")} className={`flex-shrink-0 min-h-[44px] px-5 rounded-full text-[10px] font-black uppercase tracking-widest border cursor-pointer active:scale-95 transition-all ${feedView === "Group Ride" ? "bg-purple-600 text-white border-purple-500 font-black" : "bg-black/40 text-purple-400 border-white/10"}`}>Group Rides</button>
          <button type="button" onClick={() => setFeedView("Marketplace")} className={`flex-shrink-0 min-h-[44px] px-5 rounded-full text-[10px] font-black uppercase tracking-widest border cursor-pointer active:scale-95 transition-all ${feedView === "Marketplace" ? "bg-emerald-500 text-black border-emerald-400 font-black" : "bg-black/40 text-emerald-400 border-white/10"}`}>Marketplace</button>
          <button type="button" onClick={() => setFeedView("confessions")} className={`flex-shrink-0 min-h-[44px] px-5 rounded-full text-[10px] font-black uppercase tracking-widest border cursor-pointer active:scale-95 transition-all ${feedView === "confessions" ? "bg-amber-500 text-black border-amber-400 font-black" : "bg-black/40 text-amber-400 border-white/10"}`}>Trail Confessions</button>
          <button type="button" onClick={() => setFeedView("VirtualGarage")} className={`flex-shrink-0 min-h-[44px] px-5 rounded-full text-[10px] font-black uppercase tracking-widest border cursor-pointer active:scale-95 transition-all ${feedView === "VirtualGarage" ? "bg-slate-500 text-white border-slate-400 font-black" : "bg-black/40 text-slate-400 border-white/10"}`}>Virtual Garage</button>
          <button type="button" onClick={() => setFeedView("saved")} className={`flex-shrink-0 min-h-[44px] px-5 rounded-full text-[10px] font-black uppercase tracking-widest border cursor-pointer active:scale-95 transition-all ${feedView === "saved" ? `${t.bgSubtle} ${t.text}` : "bg-black/40 text-zinc-400 border-white/10"}`}>Saved Vault</button>
          <button type="button" onClick={() => setFeedView("help")} className={`flex-shrink-0 min-h-[44px] px-5 rounded-full text-[10px] font-black uppercase tracking-widest border cursor-pointer active:scale-95 transition-all ${feedView === "help" ? "bg-red-600 text-white font-black" : "bg-black/40 text-red-400 border-white/10"}`}>SOS Board</button>
        </div>

        {suggestedCrew.length > 0 && feedView === 'universal' && (
          <div className="py-2 mb-2">
            <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-3 block pl-2">Suggested Crew Profiles</span>
            <div className="flex gap-3 overflow-x-auto custom-scrollbar pb-2 px-1">
              {suggestedCrew.map(su => (
                <div key={su} className="bg-black/40 border border-white/10 rounded-3xl p-4 min-w-[150px] flex flex-col items-center gap-3 shadow-sm shrink-0">
                  <div className="relative w-14 h-14 rounded-full overflow-hidden border-2 border-white/20">
                     {userProfiles[su]?.pfpUrl && !userProfiles[su].pfpUrl.startsWith("blob:") ? (
                       <img src={userProfiles[su].pfpUrl} alt="" className="w-full h-full object-cover"/>
                     ) : (
                       <div className="w-full h-full flex items-center justify-center text-lg font-black bg-zinc-800">{su.charAt(0).toUpperCase()}</div>
                     )}
                  </div>
                  <span className="text-sm font-bold text-white truncate w-full text-center">{su}</span>
                  <button onClick={() => setActiveProfileUser(su)} className={`w-full min-h-[36px] rounded-full text-[9px] font-black uppercase tracking-widest ${t.bgSubtle} ${t.text} transition-colors active:scale-95 cursor-pointer`}>View Profile</button>
                </div>
              ))}
            </div>
          </div>
        )}

        {feedView === "universal" && (
          <div className="relative">
            <SectorStandings 
              topSpeedRecord={standings.topSpeedRecord} topScout={standings.topScout} scoutCount={standings.scoutCount}
              mvpUser={standings.mvpUser} mvpVolts={standings.mvpVolts} isGpsLocked={isGpsLocked} useMetric={localMetric}
              themeColors={t} onOpenProfile={(user: string) => setActiveProfileUser(user)}
              glassBlurClass={props.glassBlurClass} cornerStyle={props.cornerStyle} userProfiles={userProfiles} posts={posts}
            />
          </div>
        )}

        <div className="space-y-6 pt-2 pb-safe">
          <AnimatePresence>
            {displayedPosts.length === 0 ? (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className={`app-card ${props.glassBlurClass || ''} ${props.cornerStyle || 'rounded-3xl'} bg-black/40 border border-white/10 p-16 text-center shadow-xl`}>
                <ShieldCheck className="w-12 h-12 text-zinc-700 mx-auto mb-4" />
                <h3 className="text-zinc-400 font-black uppercase tracking-widest text-sm mb-2">No Transmissions Found</h3>
                <p className="text-xs text-zinc-600 font-medium">Try adjusting your filters or be the first to broadcast.</p>
              </motion.div>
            ) : (
              displayedPosts.map((post) => (
                <FeedPost 
                  key={post.id} post={post} currentUser={username} userProfiles={userProfiles} dataSaverMode={dataSaverMode}
                  onVolt={() => handleVolt(post.id, post.username)} onDelete={() => handleDeletePost(post.id, post.username)}
                  onAiTrigger={() => handleTriggerAISolver(post.id)} onRSVP={() => handleRSVP(post.id)} aiRunning={aiRunningId === post.id}
                  themeColors={t} isSaved={savedPosts.includes(post.id)} onToggleSave={() => toggleBookmark(post.id)}
                  onFlag={() => { if (window.confirm("Report post?")) handleFlagPost(post.id); }} onOpenProfile={(targetUser: string) => setActiveProfileUser(targetUser)}
                  onOpenDM={(targetUser: string) => { setActiveDMUser(targetUser); setShowInbox(true); }} useMetric={localMetric}
                  onLocate={() => setFeedView("map")}
                  PEV_TEMPLATES={PEV_TEMPLATES} glassBlurClass={props.glassBlurClass} cornerStyle={props.cornerStyle}
                />
              ))
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}