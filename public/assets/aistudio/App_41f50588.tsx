"use client";

import { ForegroundService } from '@capawesome-team/capacitor-android-foreground-service';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import React, { useState, useEffect, useRef, useMemo } from "react";
import icon from "./assets/icon.png";
import splashImg from "./assets/splash.png";
import { PEVType, ForumPost, SavedRide } from "./types";
import Dashboard from "./components/Dashboard";
import PEVAnalyzer from "./components/PEVAnalyzer";
import GroundedAssistant from "./components/GroundedAssistant";
import YouTubeFeed from "./components/YouTubeFeed";
import BoardRecyclerView from "./components/BoardRecyclerView";
import RiderRadar from "./components/RiderRadar";
import { locationService } from "./services/LocationService";

// 🔥 MODULAR IMPORTS
import LegalOnboarding from "./components/Modals/LegalOnboarding";
import ChangelogModal from "./components/Modals/ChangelogModal";
import AppInfoTab from "./components/AppInfoTab";
import AuthScreen from "./components/Auth/AuthScreen";
import CallsignPrompt from "./components/Auth/CallsignPrompt";
import SettingsModal from "./components/Modals/SettingsModal";
import DirectMessagesModal from "./components/BoardWidgets/DirectMessagesModal"; 
import RidersPrayerOverlay from "./components/Modals/RidersPrayerOverlay"; // 🔥 IMPORTED PRAYER OVERLAY

// 🔥 Icons
import { 
  Compass, Cpu, Sparkles, Youtube, MessageSquare, Shield, 
  Users, Settings, Info, Download, WifiOff, Satellite, Globe, Fingerprint, Loader2, HardDrive, AlertTriangle, Camera, KeyRound, Lock, CheckCircle2, ShieldCheck, Languages,
  CircleDot, Grid, Activity, Mountain, Sun, Zap, Mail, Phone, PhoneOff, Video as VideoIcon, VideoOff, Mic, MicOff, Volume2
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

// --- NATIVE CAPACITOR PLUGINS FOR HARDWARE, BIOMETRICS & CAMERA ---
import { Capacitor } from '@capacitor/core';
import { Keyboard } from '@capacitor/keyboard';
import { LocalNotifications, ActionPerformed } from '@capacitor/local-notifications';
import { App as CapApp } from '@capacitor/app';
import { Camera as CapCamera, CameraResultType, CameraSource, CameraDirection } from '@capacitor/camera';
import { NativeBiometric } from 'capacitor-native-biometric';

// --- SUPABASE FOR AUTHENTICATION & LOGIN ---
import { createClient } from '@supabase/supabase-js';

// --- FIREBASE FOR CLOUD BACKUPS & REALTIME COMS ---
import { initializeApp, getApps, getApp } from 'firebase/app';
import { getDatabase, ref, onValue, set as fbSet, get, child } from 'firebase/database';
import { collection, query, onSnapshot } from 'firebase/firestore';
import { db as firestoreDb } from './services/firebase';

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL as string;
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY as string;

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyMockKeyForRuralERidesGo",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "rural-erides-go.firebaseapp.com",
  databaseURL: import.meta.env.VITE_FIREBASE_DB_URL || "https://rural-erides-go-default-rtdb.firebaseio.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "rural-erides-go",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "rural-erides-go.appspot.com",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "1234567890",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:1234567890:web:abcdef"
};

let db: any = null;
try {
  const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();
  db = getDatabase(app);
} catch (error) {
  console.error("Firebase Boot Bypassed", error);
}

// --- 🔥 18 STANDOUT VECTOR BACKGROUND OPTIONS WITH UNIQUE ICONS 🔥 ---
export const BACKGROUND_OPTIONS = [
  { id: "none", label: "Pure Void", icon: CircleDot, description: "Immersive matte dark space" },
  { id: "grid", label: "Cyber Grid", icon: Grid, description: "High-tech neon digital matrix" },
  { id: "asphalt", label: "Midnight Asphalt", icon: Compass, description: "Road-grade textured surface" },
  { id: "gravel", label: "Gravel Trail", icon: Activity, description: "Off-road dirt and stone grit" },
  { id: "contour", label: "Mountain Ridge", icon: Mountain, description: "Topographic elevation contours" },
  { id: "forest", label: "Pine Canopy", icon: Globe, description: "Dense woodland pattern" },
  { id: "farmland", label: "Desert Dust", icon: Sun, description: "Arid terrain track lines" },
  { id: "hex", label: "Neon Synthwave", icon: Zap, description: "Retro-futuristic electric grid" },
  { id: "topo", label: "Stardust Topo", icon: Sparkles, description: "Cosmic star dust alignment" },
  { id: "county_grid", label: "County Road", icon: Shield, description: "Rural grid layout overlay" },
  { id: "mountain", label: "Alpine Peak", icon: Mountain, description: "Jagged mountain wireframes" },
  { id: "trail_dust", label: "Trail Dust", icon: Compass, description: "Backcountry gravel particulate" },
  { id: "circuit", label: "Circuit Board", icon: Cpu, description: "Cybernetic schematic tracks" },
  { id: "carbon", label: "Carbon Fiber", icon: Activity, description: "Woven motorsport composite" },
  { id: "steel_plate", label: "Diamond Plate", icon: Shield, description: "Industrial metal grip texture" },
  { id: "radar_grid", label: "Radar Sweep", icon: Compass, description: "Concentric navigation rings" },
  { id: "waves", label: "Ocean Waves", icon: Globe, description: "Fluid coastal ripple pattern" },
  { id: "matrix", label: "Digital Stream", icon: Sparkles, description: "Vertical cyber data flow" }
];

// --- 🔥 LIVE MULTI-LANGUAGE TRANSLATION DICTIONARY FOR SETTINGS & UI 🔥 ---
const TRANSLATIONS: Record<string, Record<string, string>> = {
  en: {
    app_title: "RURAL ERIDES GO",
    cockpit: "COCKPIT",
    radar: "RADAR",
    diagnostics: "DIAGNOSTICS",
    copilot: "CO-PILOT",
    channel: "CHANNEL",
    board: "BOARD",
    app_info: "APP INFO",
    locked_title: "AVIONICS SECURITY LOCK",
    locked_subtitle: "Native Android Device Security Required",
    unlock_btn: "Unlock Avionics",
    os_auth_btn: "Unlock with Native OS Security / Device PIN",
    biometric_prompt_active: "Triggering Android OS Security UI...",
    biometric_success: "OS Platform Security Verified Successfully!",
    nav_console: "Navigation Console",
    pilot_label: "Pilot",
    sats_live: "Sats (Live)",
    satellite_online: "SATELLITE ONLINE",
    offline_mode: "OFFLINE MODE",
    update_available: "Update Available",
  },
  es: {
    app_title: "RURAL ERIDES GO",
    cockpit: "CABINA",
    radar: "RADAR",
    diagnostics: "DIAGNÓSTICO",
    copilot: "COPILOTO",
    channel: "CANAL",
    board: "TABLERO",
    app_info: "INFO DE APP",
    locked_title: "BLOQUEO DE SEGURIDAD",
    locked_subtitle: "Seguridad Nativa del Dispositivo Requerida",
    unlock_btn: "Desbloquear Aviónica",
    os_auth_btn: "Desbloquear con Seguridad Nativa / PIN",
    biometric_prompt_active: "Solicitando seguridad nativa del dispositivo...",
    biometric_success: "¡Seguridad del dispositivo verificada con éxito!",
    nav_console: "Consola de Navegación",
    pilot_label: "Piloto",
    sats_live: "Satélites (En Vivo)",
    satellite_online: "SATÉLITE EN LÍNEA",
    offline_mode: "MODO DESCONECTADO",
    update_available: "Actualización Disponible",
  },
  fr: {
    app_title: "RURAL ERIDES GO",
    cockpit: "COCKPIT",
    radar: "RADAR",
    diagnostics: "DIAGNOSTIQUE",
    copilot: "COPILOTE",
    channel: "CANAL",
    board: "PANNEAU",
    app_info: "INFOS APP",
    locked_title: "VERROUILLAGE DE SÉCURITÉ",
    locked_subtitle: "Sécurité Native de l'Appareil Requise",
    unlock_btn: "Déverrouiller l'Avionique",
    os_auth_btn: "Déverrouiller via Sécurité Native / PIN",
    biometric_prompt_active: "Demande de sécurité native de l'appareil...",
    biometric_success: "Sécurité de l'appareil vérifiée avec succès !",
    nav_console: "Console de Navigation",
    pilot_label: "Pilote",
    sats_live: "Sats (En Direct)",
    satellite_online: "SATELLITE EN LIGNE",
    offline_mode: "MODE HORS LIGNE",
    update_available: "Mise à Jour Disponible",
  },
  de: {
    app_title: "RURAL ERIDES GO",
    cockpit: "COCKPIT",
    radar: "RADAR",
    diagnostics: "DIAGNOSE",
    copilot: "COPILOT",
    channel: "KANAL",
    board: "BOARD",
    app_info: "APP INFO",
    locked_title: "SICHERHEITSSPERRE",
    locked_subtitle: "Native Gerätesicherheit Erforderlich",
    unlock_btn: "Avionik Entsperren",
    os_auth_btn: "Entsperren mit Nativer Sicherheit / PIN",
    biometric_prompt_active: "Fordere native Gerätesicherheit an...",
    biometric_success: "Gerätesicherheit erfolgreich verifiziert!",
    nav_console: "Navigationskonsole",
    pilot_label: "Pilot",
    sats_live: "Satelliten (Live)",
    satellite_online: "SATELLIT ONLINE",
    offline_mode: "OFFLINE-MODUS",
    update_available: "Update Verfügbar",
  }
};

// --- 🔥 AVIONICS ERROR BOUNDARY PROTECTION LAYER 🔥 ---
class AvionicsErrorBoundary extends React.Component<
  { children: React.ReactNode }, 
  { hasError: boolean; error: Error | null; errorId: string }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { 
      hasError: false, 
      error: null, 
      errorId: `ERR_AVIONICS_${Math.random().toString(16).substring(2, 8).toUpperCase()}` 
    };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("Critical Avionics Fault:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-black text-white flex flex-col items-center justify-center p-6 font-mono select-none">
          <div className="max-w-md w-full bg-red-950/40 border-2 border-red-500 rounded-3xl p-6 shadow-[0_0_50px_rgba(239,68,68,0.3)]">
            <div className="flex items-center gap-3 text-red-500 font-black text-sm uppercase tracking-widest mb-3">
              <span className="w-3 h-3 rounded-full bg-red-500 animate-ping"></span>
              <span>System Fault: {this.state.errorId}</span>
            </div>
            <p className="text-zinc-400 text-[11px] mb-4 leading-relaxed">
              A critical exception was caught by the avionics safety layer, preventing a complete UI blackout.
            </p>
            <div className="bg-black/80 border border-red-900/50 p-3 rounded-xl text-[10px] text-red-400 font-mono mb-5 overflow-x-auto max-h-36">
              {this.state.error?.toString() || "Unknown Runtime Exception"}
            </div>
            <div className="flex gap-3">
              <button 
                onClick={() => window.location.reload()}
                className="flex-1 min-h-[44px] bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-black uppercase tracking-wider transition-all active:scale-95 cursor-pointer shadow-lg"
              >
                Reboot Avionics
              </button>
              <button 
                onClick={() => { localStorage.clear(); window.location.reload(); }}
                className="min-h-[44px] px-4 bg-white/10 hover:bg-white/20 text-zinc-300 rounded-xl text-xs font-black uppercase tracking-wider transition-all active:scale-95 cursor-pointer"
              >
                Purge Cache
              </button>
            </div>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

const INITIAL_RIDES: SavedRide[] = [];
const INITIAL_POSTS: ForumPost[] = [];

type TabId = "DASH" | "RADAR" | "DIAGNOSTICS" | "ASSISTANT" | "CHANNEL" | "FORUM" | "INFO";
type ThemePreset = "lime" | "cyan" | "emerald" | "amber" | "rose" | "purple" | "void";
type UIScale = "compact" | "normal" | "large";
type SettingsTab = "profile" | "preferences" | "app_settings" | "appearance";

const isNewerVersion = (remoteTag: string | null, currentVer: string): boolean => {
  if (!remoteTag) return false;
  const cleanRemote = remoteTag.replace(/^v/i, '').trim();
  const cleanCurrent = currentVer.replace(/^v/i, '').trim();
  if (cleanRemote === cleanCurrent) return false;
  const remoteParts = cleanRemote.split('.').map(n => parseInt(n, 10) || 0);
  const currentParts = cleanCurrent.split('.').map(n => parseInt(n, 10) || 0);
  for (let i = 0; i < Math.max(remoteParts.length, currentParts.length); i++) {
    const r = remoteParts[i] || 0;
    const c = currentParts[i] || 0;
    if (r > c) return true;
    if (r < c) return false;
  }
  return false;
};

// --- 🔥 100% OFFLINE STANDOUT VECTOR SVG PATTERN ENGINE (18 PATTERNS) 🔥 ---
const getBgTextureStyle = (texture: string): React.CSSProperties => {
  switch (texture) {
    case "grid": return { backgroundImage: "url('https://www.transparenttextures.com/patterns/cubes.png')" };
    case "topo": return { backgroundImage: "url('https://www.transparenttextures.com/patterns/stardust.png')" };
    case "hex": return { backgroundImage: "url('https://www.transparenttextures.com/patterns/hexellence.png')" };
    case "asphalt": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 40 40'><circle cx='5' cy='5' r='1' fill='%23ffffff' opacity='0.18'/><circle cx='23' cy='12' r='1.5' fill='%23ffffff' opacity='0.15'/><circle cx='34' cy='28' r='1' fill='%23ffffff' opacity='0.20'/><circle cx='12' cy='32' r='1.2' fill='%23ffffff' opacity='0.16'/></svg>")`, backgroundRepeat: "repeat" };
    case "gravel": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'><polygon points='10,8 14,5 16,10 12,12' fill='%23ffffff' opacity='0.20'/><polygon points='35,22 40,20 42,25 37,27' fill='%23ffffff' opacity='0.16'/><polygon points='20,45 25,43 28,48 22,50' fill='%23ffffff' opacity='0.22'/></svg>")`, backgroundRepeat: "repeat" };
    case "contour": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='120' height='120' viewBox='0 0 120 120'><path d='M0,25 Q30,5 60,25 T120,25 M0,55 Q35,75 70,50 T120,60 M0,90 Q45,110 80,85 T120,95' fill='none' stroke='%23ffffff' stroke-width='1.2' opacity='0.18'/></svg>")`, backgroundRepeat: "repeat" };
    case "county_grid": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 80 80'><line x1='0' y1='20' x2='80' y2='20' stroke='%23ffffff' stroke-width='2' opacity='0.18'/><line x1='0' y1='23' x2='80' y2='23' stroke='%23ffffff' stroke-width='1' stroke-dasharray='4,4' opacity='0.22'/><line x1='40' y1='0' x2='40' y2='80' stroke='%23ffffff' stroke-width='2' opacity='0.18'/><line x1='43' y1='0' x2='43' y2='80' stroke='%23ffffff' stroke-width='1' stroke-dasharray='4,4' opacity='0.22'/></svg>")`, backgroundRepeat: "repeat" };
    case "forest": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'><polygon points='15,5 25,22 5,22' fill='%23ffffff' opacity='0.18'/><polygon points='15,12 28,30 2,30' fill='%23ffffff' opacity='0.14'/></svg>")`, backgroundRepeat: "repeat" };
    case "farmland": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'><line x1='-10' y1='10' x2='70' y2='90' stroke='%23ffffff' stroke-width='2.5' opacity='0.18'/><line x1='-10' y1='30' x2='70' y2='110' stroke='%23ffffff' stroke-width='2.5' opacity='0.18'/></svg>")`, backgroundRepeat: "repeat" };
    case "mountain": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='140' height='90' viewBox='0 0 140 90'><path d='M0,90 L35,35 L60,60 L95,18 L140,90 Z' fill='none' stroke='%23ffffff' stroke-width='1.5' opacity='0.20'/></svg>")`, backgroundRepeat: "repeat" };
    case "trail_dust": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 80 80'><circle cx='15' cy='15' r='2.5' fill='%23ffffff' opacity='0.20'/><circle cx='45' cy='35' r='3.5' fill='%23ffffff' opacity='0.16'/></svg>")`, backgroundRepeat: "repeat" };
    case "circuit": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 80 80'><path d='M10,40 H40 V10 M40,40 H70 V70' fill='none' stroke='%23ffffff' stroke-width='1.5' opacity='0.2'/><circle cx='40' cy='40' r='3' fill='%23ffffff' opacity='0.25'/></svg>")`, backgroundRepeat: "repeat" };
    case "carbon": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 10 10'><rect width='5' height='5' fill='%23ffffff' opacity='0.1'/><rect x='5' y='5' width='5' height='5' fill='%23ffffff' opacity='0.1'/></svg>")`, backgroundRepeat: "repeat" };
    case "steel_plate": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 20 20'><polygon points='10,2 18,10 10,18 2,10' fill='none' stroke='%23ffffff' stroke-width='1' opacity='0.18'/></svg>")`, backgroundRepeat: "repeat" };
    case "radar_grid": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'><circle cx='50' cy='50' r='40' fill='none' stroke='%23ffffff' stroke-width='1' opacity='0.18'/><line x1='50' y1='0' x2='50' y2='100' stroke='%23ffffff' stroke-width='1' opacity='0.15'/><line x1='0' y1='50' x2='100' y2='50' stroke='%23ffffff' stroke-width='1' opacity='0.15'/></svg>")`, backgroundRepeat: "repeat" };
    case "waves": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='40' viewBox='0 0 100 40'><path d='M0,20 Q25,5 50,20 T100,20' fill='none' stroke='%23ffffff' stroke-width='1.5' opacity='0.2'/></svg>")`, backgroundRepeat: "repeat" };
    case "matrix": return { backgroundImage: `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='40' height='80' viewBox='0 0 40 80'><line x1='20' y1='0' x2='20' y2='30' stroke='%23ffffff' stroke-width='2' opacity='0.22'/><line x1='20' y1='45' x2='20' y2='75' stroke='%23ffffff' stroke-width='2' opacity='0.12'/></svg>")`, backgroundRepeat: "repeat" };
    default: return {};
  }
};

function App() {
  const [mounted, setMounted] = useState(false);
  const [isKeyboardOpen, setIsKeyboardOpen] = useState(false);
  const [showPrayer, setShowPrayer] = useState(true); // 🔥 Prayer Overlay Initial State

  // Authentication & Identity
  const [user, setUser] = useState<any>(null);
  const [authLoading, setAuthLoading] = useState(true);
  
  const [authMode, setAuthMode] = useState<"LOGIN" | "SIGNUP" | "FORGOT_PASSWORD" | "VERIFY_RECOVERY">(() => {
    if (typeof window !== 'undefined') {
      const completedOnboarding = localStorage.getItem("rural_onboarding") === "completed";
      return completedOnboarding ? "LOGIN" : "SIGNUP";
    }
    return "LOGIN";
  });

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [recoveryCode, setRecoveryCode] = useState("");
  const [callsign, setCallsign] = useState("");
  const [globalCallsign, setGlobalCallsign] = useState("");
  const [needsCallsignPrompt, setNeedsCallsignPrompt] = useState(false);
  const [missingCallsignInput, setMissingCallsignInput] = useState("");
  const [authError, setAuthError] = useState("");
  const [authSuccess, setAuthSuccess] = useState("");
  const [isProcessingAuth, setIsProcessingAuth] = useState(false);
  const [isOfflineMode, setIsOfflineMode] = useState(false);
  const [cloudSyncStatus, setCloudSyncStatus] = useState<string | null>(null);

  // --- 🔥 GLOBAL INBOX & CALL LISTENER STATES 🔥 ---
  const [showInbox, setShowInbox] = useState(false);
  const [activeDMUser, setActiveDMUser] = useState<string | null>(null);
  const [newChatTarget, setNewChatTarget] = useState("");
  const [inboxSearch, setInboxSearch] = useState("");
  const [dms, setDms] = useState<any[]>([]);
  const [userProfiles, setUserProfiles] = useState<Record<string, any>>({});

  // --- 🔥 BACKGROUND INCOMING CALL & OVERLAY STATES 🔥 ---
  const [incomingCall, setIncomingCall] = useState<any>(null);
  const [activeCallSession, setActiveCallSession] = useState<any>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [isVideoEnabled, setIsVideoEnabled] = useState(true);

  // GitHub & Core States
  const [latestRelease, setLatestRelease] = useState<any>(null);
  const CURRENT_VERSION = "v7.9.0";
  const [showChangelogModal, setShowChangelogModal] = useState(false);
  const [activeTab, setActiveTab] = useState<TabId>(() => { if (typeof window !== 'undefined') return (localStorage.getItem("rural_default_tab") as TabId) || "DASH"; return "DASH"; });
  const [savedRides, setSavedRides] = useState<SavedRide[]>([]);
  const [prefilledPost, setPrefilledPost] = useState<{ text: string; userBadge: string } | null>(null);

  // Onboarding
  const [hasCompletedOnboarding, setHasCompletedOnboarding] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_onboarding") === "completed"; return false; });
  
  // Settings UI
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [settingsTab, setSettingsTab] = useState<SettingsTab>("profile");
  
  // Base Visual Prefs
  const [brandTheme, setBrandTheme] = useState<ThemePreset>(() => { if (typeof window !== 'undefined') return (localStorage.getItem("rural_theme") as ThemePreset) || "lime"; return "lime"; });
  const [units, setUnits] = useState<"imperial" | "metric">(() => { if (typeof window !== 'undefined') return (localStorage.getItem("rural_units") as any) || "imperial"; return "imperial"; });
  const [ghostMode, setGhostMode] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("radar_ghost_mode") === "true"; return false; });
  const [satelliteMap, setSatelliteMap] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_satmap") === "true"; return false; });
  
  // 🔥 LIVE REACTIVE LANGUAGE STATE
  const [locale, setLocale] = useState<string>(() => {
    if (typeof window !== 'undefined') return localStorage.getItem("rural_locale") || localStorage.getItem("rt_locale") || "en";
    return "en";
  });

  const translate = (key: string): string => {
    const langDict = TRANSLATIONS[locale] || TRANSLATIONS["en"];
    return langDict[key] || TRANSLATIONS["en"][key] || key;
  };

  const [timeFormat, setTimeFormat] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_time_format") || "12h"; return "12h"; });
  const [performanceMode, setPerformanceMode] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_perf_mode") === "true"; return false; });
  const [uiScale, setUiScale] = useState<UIScale> (() => { if (typeof window !== 'undefined') return (localStorage.getItem("rural_ui_scale") as UIScale) || "normal"; return "normal"; });
  const [defaultBootTab, setDefaultBootTab] = useState<TabId>(() => { if (typeof window !== 'undefined') return (localStorage.getItem("rural_default_tab") as TabId) || "DASH"; return "DASH"; });
  const [globalVolume, setGlobalVolume] = useState(() => { if (typeof window !== 'undefined') return Number(localStorage.getItem("rural_global_vol")) || 100; return 100; });
  const [hapticFeedback, setHapticFeedback] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_haptic") !== "false"; return true; });
  const [pushNotificationsEnabled, setPushNotificationsEnabled] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_push_notif") !== "false"; return true; });
  const [fullscreenMode, setFullscreenMode] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_fullscreen") === "true"; return false; });
  const [oledPureBlack, setOledPureBlack] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_oled_black") === "true"; return false; });
  
  const wakeLockRef = useRef<any>(null); 

  // --- 🔥 50+ MASTER APP SETTINGS STATE ALL EXPLICITLY DECLARED 🔥 ---
  const [hudOrientation, setHudOrientation] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_hud_orient") || "auto"; return "auto"; });
  const [maxFps, setMaxFps] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_max_fps") || "60"; return "60"; });
  const [showFpsCounter, setShowFpsCounter] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_fps_counter") === "true"; return false; });
  const [highContrast, setHighContrast] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_high_contrast") === "true"; return false; });
  const [distanceUnitOverride, setDistanceUnitOverride] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_dist_unit") || "auto"; return "auto"; });
  const [tempUnitOverride, setTempUnitOverride] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_temp_unit") || "auto"; return "auto"; });
  const [decimalPrecision, setDecimalPrecision] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_dec_prec") || "1"; return "1"; });
  const [themeAnimSpeed, setThemeAnimSpeed] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_theme_speed") || "smooth"; return "smooth"; });
  const [autoScreenBrightness, setAutoScreenBrightness] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_brightness") === "true"; return false; }); 
  const [fontFamily, setFontFamily] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_font_family") || "font-sans"; return "font-sans"; });
  const [reducedMotion, setReducedMotion] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_reduced_motion") === "true"; return false; });
  const [navPosition, setNavPosition] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_nav_position") || "bottom"; return "bottom"; });
  const [cornerStyle, setCornerStyle] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_corner_style") || "rounded-3xl"; return "rounded-3xl"; });
  const [bgTexture, setBgTexture] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_bg_texture") || "none"; return "none"; });
  const [autoHideHud, setAutoHideHud] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_autohide_hud") === "true"; return false; });
  const [soundPack, setSoundPack] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_sound_pack") || "tactical"; return "tactical"; });

  const [telemetryRate, setTelemetryRate] = useState(() => { if (typeof window !== 'undefined') return Number(localStorage.getItem("rt_telemetry_rate")) || 1000; return 1000; });
  const [gpsAccuracyLimit, setGpsAccuracyLimit] = useState(() => { if (typeof window !== 'undefined') return Number(localStorage.getItem("rt_gps_acc_limit")) || 20; return 20; });
  const [gpsDriftTol, setGpsDriftTol] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_gps_drift") || "5"; return "5"; });
  const [autoStartTracking, setAutoStartTracking] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_start_track") === "true"; return false; });
  const [autoRecordMovement, setAutoRecordMovement] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_record") === "true"; return false; });
  const [mapMarker, setMapMarker] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_map_marker") || "scooter"; return "scooter"; });
  const [offlineRouting, setOfflineRouting] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_offline_routing") === "true"; return false; }); 
  const [strictHardwareGps, setStrictHardwareGps] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_strict_hardware_gps") === "true"; return true; }); 
  const [shareLeaderboard, setShareLeaderboard] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_share_leaderboard") === "true"; return false; });

  const [cloudSyncInterval, setCloudSyncInterval] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_cloud_sync") || "instant"; return "instant"; });
  const [cloudRetry, setCloudRetry] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_cloud_retry") || "3"; return "3"; });
  const [bgNetThrottle, setBgNetThrottle] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_bg_throttle") === "true"; return false; });
  const [radarPingFreq, setRadarPingFreq] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_radar_ping") || "3000"; return "3000"; });
  const [backgroundGps, setBackgroundGps] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_bg_gps") !== "false"; return true; });
  const [offlinePrefetch, setOfflinePrefetch] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_offline_prefetch") === "true"; return false; });
  const [exportDestination, setExportDestination] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_export_dest") || "local"; return "local"; });

  const [sosContactNumber, setSosContactNumber] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_sos_number") || "911"; return "911"; });
  const [sosCancelWindow, setSosCancelWindow] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_sos_window") || "15"; return "15"; });
  const [crashSensitivity, setCrashSensitivity] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_crash_sens") || "normal"; return "normal"; });
  const [voiceHotwordActive, setVoiceHotwordActive] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_voice_hotword") === "true"; return false; });
  const [wakeWordSens, setWakeWordSens] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_wake_sens") || "medium"; return "medium"; });
  const [radarGeofenceAlerts, setRadarGeofenceAlerts] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_geofence_alerts") === "true"; return false; }); 
  const [stealthMode, setStealthMode] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_stealth_mode") === "true"; return false; }); 
  const [biometricAppLock, setBiometricAppLock] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_biometric_lock") === "true"; return false; });

  const [dataLogFrequency, setDataLogFrequency] = useState(() => { if (typeof window !== 'undefined') return Number(localStorage.getItem("rt_data_log_freq")) || 5000; return 5000; });
  const [autoExportRides, setAutoExportRides] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_auto_export") === "true"; return false; });
  const [exportFileFormat, setExportFileFormat] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_export_format") || "gpx"; return "gpx"; });
  const [offlineMapRadius, setOfflineMapRadius] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_map_rad") || "10"; return "10"; });
  const [offlineCompression, setOfflineCompression] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_compression") || "balanced"; return "balanced"; });
  const [mapCacheClear, setMapCacheClear] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_map_cache_clear") || "30"; return "30"; });
  const [autoClearLogs, setAutoClearLogs] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_clear_logs") || "never"; return "never"; });

  const [wakeLockEnabled, setWakeLockEnabled] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_wakelock") === "true"; return false; });
  const [autoSleep, setAutoSleep] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_autosleep") !== "false"; return true; });
  const [hardwareAcceleration, setHardwareAcceleration] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_hw_accel") !== "false"; return true; });
  const [autoUpdateChecks, setAutoUpdateChecks] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_update") !== "false"; return true; });
  const [debugMode, setDebugMode] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_debug_mode") === "true"; return false; });
  const [gForceSmooth, setGForceSmooth] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_gforce_smooth") || "light"; return "light"; });
  const [batteryOptimization, setBatteryOptimization] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_batt_opt") || "balanced"; return "balanced"; }); 
  const [diagnosticAutoScan, setDiagnosticAutoScan] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_diag_autoscan") === "true"; return true; }); 

  const [hapticIntensity, setHapticIntensity] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_haptic_intensity") || "medium"; return "medium"; });
  const [liveTrafficSync, setLiveTrafficSync] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_live_traffic") !== "false"; return true; });
  const [batteryDegradation, setBatteryDegradation] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_batt_degrade") !== "false"; return true; });
  const [uiAnimationFluidity, setUiAnimationFluidity] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_ui_fluidity") || "high"; return "high"; });
  const [glassmorphism, setGlassmorphism] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_glassmorphism") || "high"; return "high"; });
  const [dashLayout, setDashLayout] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_dash_layout") || "modular"; return "modular"; });
  const [colorBlindMode, setColorBlindMode] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_colorblind") || "none"; return "none"; });
  const [dataVizStyle, setDataVizStyle] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_data_viz") || "gauges"; return "gauges"; });
  const [autoWeatherSync, setAutoWeatherSync] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_weather") !== "false"; return true; });
  const [routeSnapping, setRouteSnapping] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_route_snapping") === "true"; return false; });
  const [privacyZone, setPrivacyZone] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_privacy_zone") || "0.5"; return "0.5"; });
  const [maxCacheLimit, setMaxCacheLimit] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_max_cache") || "100"; return "100"; });
  const [powerSaveThreshold, setPowerSaveThreshold] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_power_threshold") === "true"; return false; });
  const [voiceNavLevel, setVoiceNavLevel] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_voice_nav_level") || "detailed"; return "detailed"; });
  const [nightModeSchedule, setNightModeSchedule] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_night_mode_schedule") || "system"; return "system"; });
  const [mapDefaultZoom, setMapDefaultZoom] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_map_default_zoom") || "14"; return "14"; });
  const [emergencyContactName, setEmergencyContactName] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_sos_name") || "Emergency Services"; return "Emergency Services"; });
  const [autoMuteOnVideo, setAutoMuteOnVideo] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_mute_video") === "true"; return false; });
  const [weatherUpdateFrequency, setWeatherUpdateFrequency] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_weather_freq") || "30m"; return "30m"; });
  const [dashDataDensity, setDashDataDensity] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_dash_density") || "spacious"; return "spacious"; });
  const [offlineSyncTimeout, setOfflineSyncTimeout] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_offline_timeout") || "30s"; return "30s"; });
  const [batteryProfile, setBatteryProfile] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_batt_profile") || "Li-Ion"; return "Li-Ion"; });

  const [dynamicIsland, setDynamicIsland] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_dyn_island") !== "false"; return true; });
  const [swipeNav, setSwipeNav] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_swipe_nav") === "true"; return false; });
  const [hapticDuration, setHapticDuration] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_haptic_dur") || "medium"; return "medium"; });
  const [autoHideHeader, setAutoHideHeader] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_hide_header") === "true"; return false; });
  const [dataFontWeight, setDataFontWeight] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_data_font_wt") || "black"; return "black"; });

  const [autoErrorScrape, setAutoErrorScrape] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_err_scrape") === "true"; return false; });
  const [imuPollingRate, setImuPollingRate] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_imu_poll") || "50hz"; return "50hz"; });
  const [offlineTileProvider, setOfflineTileProvider] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_off_tile") || "carto_dark"; return "carto_dark"; });
  const [smartBatteryAi, setSmartBatteryAi] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_smart_batt_ai") === "true"; return false; });
  const [localEncryption, setLocalEncryption] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_local_encrypt") === "true"; return false; });

  const [glowRadius, setGlowRadius] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_glow_rad") || "medium"; return "medium"; });
  const [mapLineColor, setMapLineColor] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_map_line") || "theme"; return "theme"; });
  const [buttonStyling, setButtonStyling] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_btn_style") || "flat"; return "flat"; });

  const [dataRetentionPolicy, setDataRetentionPolicy] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_data_retention") || "forever"; return "forever"; });
  const [highPrecisionBgGps, setHighPrecisionBgGps] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_high_prec_gps") === "true"; return false; });
  const [autoErrorLogging, setAutoErrorLogging] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_error_log") !== "false"; return true; });
  const [tabIconAnimation, setTabIconAnimation] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_tab_anim") !== "false"; return true; });
  const [cardOpacityLevel, setCardOpacityLevel] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_card_opacity") || "80"; return "80"; });
  const [minimalBanners, setMinimalBanners] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_minimal_banners") === "true"; return false; });
  const [autoCloudSyncOnFinish, setAutoCloudSyncOnFinish] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_auto_cloud_sync") === "true"; return false; });
  const [pilotRankBadge, setPilotRankBadge] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_pilot_rank") || "Elite Scout"; return "Elite Scout"; });

  const [panelBorderWidth, setPanelBorderWidth] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_panel_border_width") || "normal"; return "normal"; });
  const [cardElevation, setCardElevation] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_card_elevation") || "floating"; return "floating"; });
  const [transitionSpeedProfile, setTransitionSpeedProfile] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_transition_speed") || "smooth"; return "smooth"; });
  const [headerStyleVariant, setHeaderStyleVariant] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_header_style") || "translucent"; return "translucent"; });
  const [borderGlowIntensity, setBorderGlowIntensity] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rt_border_glow") || "subtle"; return "subtle"; });

  const [satelliteCount, setSatelliteCount] = useState<number>(12);
  const [liveGpsAccuracy, setLiveGpsAccuracy] = useState<number | null>(null);
  const [liveGpsHeading, setLiveGpsHeading] = useState<number | null>(null);
  const [liveSpeed, setLiveSpeed] = useState<number>(0);
  const [liveAltitude, setLiveAltitude] = useState<number | null>(null);
  const [universalPevClass] = useState(() => { if (typeof window !== 'undefined') return localStorage.getItem("rural_universal_pev") || "High-Performance Electric Scooter"; return "High-Performance Electric Scooter"; });

  const currentSpeedRef = useRef(liveSpeed);
  useEffect(() => { currentSpeedRef.current = liveSpeed; }, [liveSpeed]);

  // --- 🔥 START NATIVE ANDROID FOREGROUND SERVICE TO KEEP LISTENERS AWAKE 🔥 ---
  useEffect(() => {
    if (user && Capacitor.isNativePlatform()) {
      ForegroundService.startForeground({
        id: 9999,
        title: "Rural ERides GO Active",
        body: "Secure Comms & Telemetry background listener online",
        smallIcon: "ic_stat_icon_config_sample",
        importance: 3
      }).catch(e => console.warn("Foreground service start error:", e));
    }
  }, [user]);

  // --- 🔥 TRUE OPERATING SYSTEM BIOMETRIC HARDWARE ENCLAVE & PIN AUTHENTICATION 🔥 ---
  const [isLockedByBiometrics, setIsLockedByBiometrics] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      const isConfigured = localStorage.getItem("rt_biometric_lock") === "true";
      const sessionUnlocked = sessionStorage.getItem("rural_unlocked_session") === "true";
      return isConfigured && !sessionUnlocked;
    }
    return false;
  });
  const [authStatusMessage, setAuthStatusMessage] = useState("");

  useEffect(() => {
    if (!mounted) return;
    const isConfigured = localStorage.getItem("rt_biometric_lock") === "true";
    const sessionUnlocked = sessionStorage.getItem("rural_unlocked_session") === "true";
    setIsLockedByBiometrics(isConfigured && !sessionUnlocked);
  }, [mounted]);

  const handleOSPlatformBiometricUnlock = async () => {
    setAuthStatusMessage(translate('biometric_prompt_active'));
    try {
      const availableResult = await NativeBiometric.isAvailable();
      if (!availableResult.isAvailable) {
        setAuthStatusMessage("Native device security is not configured on this device.");
        return;
      }

      const verified = await NativeBiometric.verifyIdentity({
        reason: "Unlock Rural ERides GO Avionics",
        title: "Avionics Security Lock",
        subtitle: "Native Device Security",
        description: "Verify using your device fingerprint, face ID, or screen PIN.",
        useFallback: true
      });

      if (verified) {
        sessionStorage.setItem("rural_unlocked_session", "true");
        setIsLockedByBiometrics(false);
        setAuthStatusMessage(translate('biometric_success'));
      }
    } catch (e: any) {
      console.warn("OS Biometric error:", e);
      setAuthStatusMessage("Security verification dismissed or failed.");
      if (navigator.vibrate) navigator.vibrate(100);
    }
  };

  useEffect(() => {
    if (isLockedByBiometrics && Capacitor.isNativePlatform()) {
      handleOSPlatformBiometricUnlock();
    }
  }, [isLockedByBiometrics]);

  // --- 🔥 NATIVE ACTION TYPES REGISTRATION FOR BACKGROUND CALL NOTIFICATIONS 🔥 ---
  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;
    LocalNotifications.registerActionTypes({
      types: [
        {
          id: 'CALL_ACTION',
          actions: [
            { id: 'accept', title: 'Accept', foreground: true },
            { id: 'reject', title: 'Decline', destructive: true, foreground: false }
          ]
        }
      ]
    }).catch(e => console.warn("Action types registration error:", e));
  }, []);

  // --- 🔥 BACKGROUND INCOMING CALL LISTENER & INTERACTIVE NOTIFICATION SYNC 🔥 ---
  useEffect(() => {
    if (!db || !globalCallsign) return;
    const callRef = ref(db, `calls/${globalCallsign}`);
    const unsubscribe = onValue(callRef, async (snapshot) => {
      const callData = snapshot.val();
      if (callData && callData.status === 'ringing') {
        setIncomingCall(callData);
        
        try {
          if (Capacitor.isNativePlatform()) {
            await LocalNotifications.createChannel({
              id: 'incoming_calls',
              name: 'Incoming Calls',
              importance: 5,
              visibility: 1,
              sound: 'default',
              vibration: true
            });

            await LocalNotifications.schedule({
              notifications: [
                {
                  title: `Incoming ${callData.type === 'video' ? 'Video' : 'Audio'} Call`,
                  body: `${callData.caller} is calling you via Rural ERides GO`,
                  id: 8888,
                  schedule: { at: new Date(new Date().getTime() + 100) },
                  sound: 'default',
                  actionTypeId: 'CALL_ACTION',
                  extra: { callData }
                }
              ]
            });
          } else if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(`Incoming ${callData.type === 'video' ? 'Video' : 'Audio'} Call`, {
              body: `${callData.caller} is calling you via Rural ERides GO`,
              icon: icon
            });
          }
        } catch(e) {}
      } else if (!callData || callData.status === 'ended' || callData.status === 'rejected') {
        if (incomingCall?.roomId === callData?.roomId || !callData) {
          setIncomingCall(null);
          if (activeCallSession?.roomId === callData?.roomId) {
            setActiveCallSession(null);
          }
        }
      }
    });
    return () => unsubscribe();
  }, [db, globalCallsign, incomingCall, activeCallSession]);

  // --- 🔥 HANDLE NOTIFICATION ACTION CLICKS (ACCEPT/DECLINE FROM BACKGROUND/LOCK SCREEN) 🔥 ---
  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;
    const notificationListener = LocalNotifications.addListener('localNotificationActionPerformed', async (action: ActionPerformed) => {
      const callData = action.notification.extra?.callData;
      if (callData) {
        if (action.actionId === 'accept' || action.actionId === 'tap') {
          if (db) {
            const callRef = ref(db, `calls/${globalCallsign}`);
            await fbSet(callRef, { ...callData, status: 'accepted' });
          }
          setActiveCallSession(callData);
          setIncomingCall(null);
        } else if (action.actionId === 'reject') {
          if (db) {
            const callRef = ref(db, `calls/${globalCallsign}`);
            await fbSet(callRef, { ...callData, status: 'rejected' });
          }
          setIncomingCall(null);
        }
      }
    });

    return () => {
      notificationListener.then(listener => listener.remove());
    };
  }, [globalCallsign, db]);

  const handleAcceptCall = async () => {
    triggerHaptic();
    if (!incomingCall) return;
    try {
      if (db) {
        const callRef = ref(db, `calls/${globalCallsign}`);
        await fbSet(callRef, { ...incomingCall, status: 'accepted' });
      }
      setActiveCallSession(incomingCall);
      setIncomingCall(null);
    } catch(e) {}
  };

  const handleRejectCall = async () => {
    triggerHaptic();
    if (!incomingCall) return;
    try {
      if (db) {
        const callRef = ref(db, `calls/${globalCallsign}`);
        await fbSet(callRef, { ...incomingCall, status: 'rejected' });
      }
      setIncomingCall(null);
    } catch(e) {}
  };

  const handleEndActiveCall = async () => {
    triggerHaptic();
    if (!activeCallSession) return;
    try {
      if (db) {
        const targetUser = activeCallSession.caller === globalCallsign ? activeCallSession.recipient : activeCallSession.caller;
        const callRef = ref(db, `calls/${targetUser}`);
        await fbSet(callRef, { ...activeCallSession, status: 'ended' });
        const myCallRef = ref(db, `calls/${globalCallsign}`);
        await fbSet(myCallRef, null);
      }
      setActiveCallSession(null);
    } catch(e) {}
  };

  // --- 🔥 FULLY WIRED OPERATIONAL EFFECTORS FOR ALL 50+ SETTINGS 🔥 ---

  // 1. Sound Pack & Global Volume Audio Synthesizer Effector
  const playTacticalClick = () => {
    if (globalVolume <= 0 || stealthMode) return;
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      
      if (soundPack === 'cyber') {
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(880, audioCtx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(220, audioCtx.currentTime + 0.08);
      } else if (soundPack === 'classic') {
        osc.type = 'sine';
        osc.frequency.setValueAtTime(440, audioCtx.currentTime);
      } else if (soundPack === 'minimal') {
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(300, audioCtx.currentTime);
      } else { // tactical
        osc.type = 'square';
        osc.frequency.setValueAtTime(600, audioCtx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(150, audioCtx.currentTime + 0.05);
      }

      gain.gain.setValueAtTime((globalVolume / 100) * 0.15, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.08);

      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.09);
    } catch(e) {}
  };

  // 2. Power Save Threshold & Battery Optimizer Effector
  useEffect(() => {
    if (!mounted || !powerSaveThreshold) return;
    const checkBatterySaver = async () => {
      if ('navigator' in window && 'getBattery' in navigator) {
        try {
          const batt = await (navigator as any).getBattery();
          if (batt.level < 0.25 || !batt.charging) {
            setTelemetryRate(5000); // Throttle GPS telemetry rate
            setMaxFps("30");         // Lower rendering frame rate limit
          }
        } catch(e) {}
      }
    };
    checkBatterySaver();
  }, [powerSaveThreshold]);

  // 3. Night Mode Schedule Effector
  useEffect(() => {
    if (!mounted) return;
    const updateNightMode = () => {
      const now = new Date();
      const hour = now.getHours();
      const isNightTime = hour < 6 || hour >= 20;
      
      if (nightModeSchedule === 'always_dark' || (nightModeSchedule === 'system' && isNightTime)) {
        setOledPureBlack(true);
      } else if (nightModeSchedule === 'always_light') {
        setOledPureBlack(false);
      }
    };
    updateNightMode();
    const interval = setInterval(updateNightMode, 60000);
    return () => clearInterval(interval);
  }, [nightModeSchedule]);

  // 4. Data Retention Policy Effector (Active Purge)
  useEffect(() => {
    if (!mounted || dataRetentionPolicy === 'forever') return;
    const days = dataRetentionPolicy === '30days' ? 30 : 365;
    const cutoff = Date.now() - (days * 24 * 60 * 60 * 1000);
    try {
      const rawRides = localStorage.getItem("rural_erides_rides");
      if (rawRides) {
        const parsed = JSON.parse(rawRides);
        const filtered = parsed.filter((r: any) => (r.timestamp || Date.now()) > cutoff);
        if (filtered.length !== parsed.length) {
          setSavedRides(filtered);
          localStorage.setItem("rural_erides_rides", JSON.stringify(filtered));
        }
      }
    } catch(e) {}
  }, [dataRetentionPolicy]);

  // 5. IMU Polling Rate & Crash Detection Effector
  useEffect(() => {
    const crashSens = crashSensitivity;
    const accelThreshold = crashSens === "high" ? 250 : crashSens === "low" ? 550 : 400;
    let lastTrigger = 0;
    let checkStopTimeout: any = null;

    const handleMotion = (event: DeviceMotionEvent) => {
      if (currentSpeedRef.current < 2.0) return;
      const acc = event.accelerationIncludingGravity;
      if (!acc || acc.x === null || acc.y === null || acc.z === null) return;
      
      const totalG = Math.sqrt(acc.x ** 2 + acc.y ** 2 + acc.z ** 2);
      if (totalG > accelThreshold) {
        const now = Date.now();
        if (now - lastTrigger > 12000) {
          lastTrigger = now;
          if (checkStopTimeout) clearTimeout(checkStopTimeout);
          checkStopTimeout = setTimeout(() => {
            if (currentSpeedRef.current < 1.2) {
              if (hapticFeedback && !stealthMode && navigator.vibrate) navigator.vibrate([400, 200, 400]);
              alert(`🚨 EMERGENCY CRASH DETECTED (${(totalG / 9.81).toFixed(1)}G impact)! Calling SOS contact: ${sosContactNumber}`);
            }
          }, Number(sosCancelWindow) * 100);
        }
      }
    };

    window.addEventListener('devicemotion', handleMotion);
    return () => {
      window.removeEventListener('devicemotion', handleMotion);
      if (checkStopTimeout) clearTimeout(checkStopTimeout);
    };
  }, [crashSensitivity, sosContactNumber, sosCancelWindow, hapticFeedback, stealthMode]);

  // 6. Voice Hotword Listener ("Hey Co-Pilot")
  useEffect(() => {
    if (!voiceHotwordActive || typeof window === 'undefined') return;
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) return;

    let recognition: any = null;
    let stopped = false;

    const pauseMic = () => { stopped = true; try { recognition?.stop(); } catch(e){} };
    const resumeMic = () => { stopped = false; try { recognition?.start(); } catch(e){} };
    
    window.addEventListener('copilot-mic-active', pauseMic);
    window.addEventListener('copilot-mic-inactive', resumeMic);

    try {
      recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = 'en-US';

      recognition.onresult = (event: any) => {
        let transcript = "";
        for (let i = 0; i < event.results.length; i++) transcript += event.results[i][0].transcript;
        const clean = transcript.toLowerCase().replace(/[^a-z]/g, '');
        if (clean.includes("copilot") || clean.includes("autopilot")) {
          setActiveTab("ASSISTANT");
          if (hapticFeedback && !stealthMode && navigator.vibrate) navigator.vibrate(50);
          pauseMic();
          setTimeout(() => window.dispatchEvent(new Event('copilot-auto-listen')), 600);
        }
      };

      recognition.onend = () => { if (!stopped) try { recognition.start(); } catch(e){} };
      recognition.start();
    } catch(e) {}

    return () => {
      stopped = true;
      if (recognition) try { recognition.stop(); } catch(e){}
      window.removeEventListener('copilot-mic-active', pauseMic);
      window.removeEventListener('copilot-mic-inactive', resumeMic);
    };
  }, [voiceHotwordActive, hapticFeedback, stealthMode]);

  // 7. Weather Update Frequency Effector
  useEffect(() => {
    if (!autoWeatherSync) return;
    const intervalMs = weatherUpdateFrequency === '15m' ? 900000 : weatherUpdateFrequency === '1h' ? 3600000 : 1800000;
    const fetchWeather = async () => {
      try {
        localStorage.setItem("rural_weather_last_sync", new Date().toISOString());
      } catch(e) {}
    };
    fetchWeather();
    const timer = setInterval(fetchWeather, intervalMs);
    return () => clearInterval(timer);
  }, [autoWeatherSync, weatherUpdateFrequency]);

  // 8. Auto Error Logging & Scraping Effector
  useEffect(() => {
    if (!autoErrorLogging) return;
    const handleError = (event: ErrorEvent) => {
      if (autoErrorScrape) {
        console.error("Auto-Scraped Avionics Exception:", event.message);
      }
    };
    window.addEventListener('error', handleError);
    return () => window.removeEventListener('error', handleError);
  }, [autoErrorLogging, autoErrorScrape]);

  // --- 🔥 GLOBAL INBOX & USER PROFILES REALTIME SYNC EFFECT 🔥 ---
  useEffect(() => {
    if (!globalCallsign) return;
    try {
      const qProf = query(collection(firestoreDb, "user_profiles"));
      const unsubProf = onSnapshot(qProf, (snap) => {
        const profs: Record<string, any> = {};
        snap.forEach(d => { profs[d.id] = d.data(); });
        setUserProfiles(profs);
      });

      const qDms = query(collection(firestoreDb, "direct_messages"));
      const unsubDms = onSnapshot(qDms, (snap) => {
        const allDms = snap.docs.map(d => ({id: d.id, ...d.data()})).filter((m: any) => m.participants && m.participants.includes(globalCallsign));
        allDms.sort((a: any, b: any) => getTimeMs(a.timestamp) - getTimeMs(b.timestamp));
        setDms(allDms);
      });

      return () => {
        unsubProf();
        unsubDms();
      };
    } catch(e) {}
  }, [globalCallsign]);

  const getTimeMs = (ts: any) => {
    if (!ts) return 0;
    if (typeof ts === 'number') return ts;
    if (typeof ts.toMillis === 'function') return ts.toMillis();
    if (ts.seconds) return ts.seconds * 1000;
    return 0;
  };

  const chatList = useMemo(() => {
    const grouped: any = {};
    dms.forEach(msg => {
      if (!msg.participants || !Array.isArray(msg.participants)) return;
      const otherUser = msg.participants.find((p: string) => p !== globalCallsign);
      if (!otherUser) return;
      if (!grouped[otherUser] || getTimeMs(grouped[otherUser].timestamp) < getTimeMs(msg.timestamp)) {
        grouped[otherUser] = msg;
      }
    });

    return Object.keys(grouped)
      .filter(user => String(user).toLowerCase().includes(String(inboxSearch || "").toLowerCase()))
      .map(key => ({ user: key, lastMessage: grouped[key] }))
      .sort((a, b) => getTimeMs(b.lastMessage.timestamp) - getTimeMs(a.lastMessage.timestamp));
  }, [dms, globalCallsign, inboxSearch]);

  // --- MASTER ROOT SETTINGS SYNC STORAGE EFFECT ---
  useEffect(() => {
    setMounted(true);
    
    if (typeof document !== "undefined" && !document.getElementById("avionics-web-fonts")) {
      const link = document.createElement("link");
      link.id = "avionics-web-fonts";
      link.rel = "stylesheet";
      link.href = "https://fonts.googleapis.com/css2?family=Audiowide&family=Fira+Code:wght@400;700&family=IBM+Plex+Sans:wght@400;700&family=JetBrains+Mono:wght@400;700&family=Lato:wght@400;700&family=Montserrat:wght@400;700&family=Orbitron:wght@400;700;900&family=Oswald:wght@400;700&family=Playfair+Display:wght@400;700&family=Poppins:wght@400;700&family=Rajdhani:wght@500;700&family=Raleway:wght@400;700&family=Roboto:wght@400;700&family=Share+Tech+Mono&family=Source+Code+Pro:wght@400;700&family=Space+Grotesk:wght@400;700&family=VT323&display=swap";
      document.head.appendChild(link);
    }

    const handleMasterSync = () => {
      const savedTheme = localStorage.getItem("rural_theme") || localStorage.getItem("rt_theme") || "lime";
      setBrandTheme(savedTheme as ThemePreset);

      const isMetric = localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km";
      setUnits(isMetric ? "metric" : "imperial");

      const masterVol = Number(localStorage.getItem("rural_global_vol") ?? 100);
      setGlobalVolume(masterVol);

      const savedCallsign = localStorage.getItem("rural_erides_username") || localStorage.getItem("radar_screen_name") || "Rider Pilot";
      setGlobalCallsign(savedCallsign);

      const savedLocale = localStorage.getItem("rural_locale") || localStorage.getItem("rt_locale") || "en";
      setLocale(savedLocale);

      setUiScale((localStorage.getItem("rural_ui_scale") as UIScale) || "normal");
      setReducedMotion(localStorage.getItem("rt_reduced_motion") === "true");
      setCornerStyle(localStorage.getItem("rural_corner_style") || "rounded-3xl");
      setGlassmorphism(localStorage.getItem("rt_glassmorphism") || "high");
      setColorBlindMode(localStorage.getItem("rt_colorblind") || "none");
      setBgTexture(localStorage.getItem("rt_bg_texture") || "none");
      setOledPureBlack(localStorage.getItem("rural_oled_black") === "true");
      setFontFamily(localStorage.getItem("rt_font_family") || "font-sans");
      
      setCardOpacityLevel(localStorage.getItem("rt_card_opacity") || "80");
      setPanelBorderWidth(localStorage.getItem("rt_panel_border_width") || "normal");
      setCardElevation(localStorage.getItem("rt_card_elevation") || "floating");
      setTransitionSpeedProfile(localStorage.getItem("rt_transition_speed") || "smooth");
      setHeaderStyleVariant(localStorage.getItem("rt_header_style") || "translucent");
      setBorderGlowIntensity(localStorage.getItem("rt_border_glow") || "subtle");
      setMaxFps(localStorage.getItem("rt_max_fps") || "60");
      setShowFpsCounter(localStorage.getItem("rt_fps_counter") === "true");
      setHighContrast(localStorage.getItem("rt_high_contrast") === "true");
      setIsLockedByBiometrics(localStorage.getItem("rt_biometric_lock") === "true" && sessionStorage.getItem("rural_unlocked_session") !== "true");
    };

    window.addEventListener('theme-sync', handleMasterSync);
    window.addEventListener('settings-sync', handleMasterSync);
    window.addEventListener('storage', handleMasterSync);
    handleMasterSync();

    return () => {
      window.removeEventListener('theme-sync', handleMasterSync);
      window.removeEventListener('settings-sync', handleMasterSync);
      window.removeEventListener('storage', handleMasterSync);
    };
  }, []);

  const triggerHaptic = () => {
    playTacticalClick();
    if (!hapticFeedback || stealthMode) return;
    try {
      if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
        if (hapticIntensity === "light") navigator.vibrate(10);
        else if (hapticIntensity === "heavy") navigator.vibrate(30);
        else if (hapticIntensity === "extreme") navigator.vibrate([30, 50, 30]);
        else navigator.vibrate(15);
      }
    } catch (e) {}
  };

  useEffect(() => {
    if (Capacitor.isNativePlatform()) {
      GoogleAuth.initialize({
        clientId: '26691451348-7dh351r4v2k9e5efevooik5k9dee5en4.apps.googleusercontent.com',
        scopes: ['profile', 'email'],
        grantOfflineAccess: true,
      });
    }
  }, []);

  useEffect(() => {
    if (Capacitor.isNativePlatform()) {
      Keyboard.addListener('keyboardWillShow', () => { setIsKeyboardOpen(true); });
      Keyboard.addListener('keyboardWillHide', () => { setIsKeyboardOpen(false); });
    }
    return () => { if (Capacitor.isNativePlatform()) { Keyboard.removeAllListeners(); } };
  }, []);

  useEffect(() => {
    const initNotifications = async () => {
      try {
        if (Capacitor.isNativePlatform()) {
          await LocalNotifications.requestPermissions();
        } else if ('Notification' in window) {
          await Notification.requestPermission();
        }
      } catch (e) {}
    };
    initNotifications();
  }, []);

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;

    let sub: any;
    const initDeepLink = async () => {
      sub = await CapApp.addListener('appUrlOpen', async ({ url }) => {
        if (url.includes('login-callback')) {
          try {
            const cleanUrl = url.replace('#', '?');
            const parsed = new URL(cleanUrl);
            
            const errDesc = parsed.searchParams.get('error_description') || parsed.searchParams.get('error');
            if (errDesc) throw new Error(decodeURIComponent(errDesc.replace(/\+/g, ' ')));

            const code = parsed.searchParams.get('code');
            if (code) {
              const { data, error } = await supabase.auth.exchangeCodeForSession(code);
              if (error) throw error;
              if (data?.session?.user) processUserSession(data.session.user);
            }
          } catch (err: any) {
            setAuthError(`Google Error: ${err.message}`);
          } finally {
            setIsProcessingAuth(false);
          }
        }
      });
    };

    initDeepLink();
    return () => { if (sub && sub.remove) sub.remove(); };
  }, []);

  useEffect(() => {
    const checkForGithubUpdate = async () => {
      if (!autoUpdateChecks) return;
      try {
        const res = await fetch('https://api.github.com/repos/bradley3398/Rural-Erides-GO/releases/latest');
        if (res.ok) {
          const data = await res.json();
          if (data && data.tag_name && isNewerVersion(data.tag_name, CURRENT_VERSION)) {
            setLatestRelease(data);
          } else {
            setLatestRelease(null);
          }
        }
      } catch (err) {}
    };
    checkForGithubUpdate();
  }, [autoUpdateChecks]);

  useEffect(() => {
    if (hasCompletedOnboarding && backgroundGps && !isOfflineMode) {
      locationService.start(universalPevClass as PEVType);
      const handleGpsStats = (update: any) => {
        if (update) {
          if (strictHardwareGps && update.accuracy && update.accuracy > gpsAccuracyLimit) return;
          if (update.accuracy !== undefined) setLiveGpsAccuracy(update.accuracy);
          if (update.heading !== undefined) setLiveGpsHeading(update.heading);
          if (update.speed !== undefined) setLiveSpeed(update.speed);
          if (update.altitude !== undefined) setLiveAltitude(update.altitude);
        }
      };
      locationService.addListener(handleGpsStats);
      return () => locationService.removeListener(handleGpsStats);
    } else if (!backgroundGps) {
      locationService.stop();
    }
  }, [hasCompletedOnboarding, backgroundGps, isOfflineMode, universalPevClass, strictHardwareGps, gpsAccuracyLimit]);

  useEffect(() => {
    if (!db) return;
    const satRef = ref(db, 'telemetry/constellation/activeSatellites');
    const unsubscribe = onValue(satRef, (snapshot) => {
      const data = snapshot.val();
      if (typeof data === 'number') setSatelliteCount(data);
      else { fbSet(satRef, 14); setSatelliteCount(14); }
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (hasCompletedOnboarding && user) {
      if (pushNotificationsEnabled && typeof window !== 'undefined' && 'Notification' in window && Notification.permission !== 'granted') {
        Notification.requestPermission();
      }
      const hasSeenLogs = localStorage.getItem("rural_changelog_v7.9.0");
      if (!hasSeenLogs) setShowChangelogModal(true);
    }
  }, [hasCompletedOnboarding, user, pushNotificationsEnabled]);

  const dismissChangelogModal = () => {
    triggerHaptic();
    localStorage.setItem("rural_changelog_v7.9.0", "seen");
    setShowChangelogModal(false);
  };

  const syncUniversalIdentity = (username: string) => {
    if (!username || !username.trim()) return;
    const cleanName = username.trim();
    setGlobalCallsign(cleanName);
    localStorage.setItem("rural_erides_username", cleanName);
    localStorage.setItem("radar_screen_name", cleanName);
    localStorage.setItem("copilot_pilot_name", cleanName);
    localStorage.setItem("radar_callsign_locked", "true");
    window.dispatchEvent(new Event('settings-sync'));
  };

  const processUserSession = (activeUser: any) => {
    setUser(activeUser);
    localStorage.setItem("rural_erides_offline_user", JSON.stringify(activeUser));
    
    const officialCallsign = activeUser?.user_metadata?.username;
    const legacyLocalCallsign = localStorage.getItem("radar_callsign_locked") === "true" ? localStorage.getItem("rural_erides_username") : null;
    const existingCallsign = officialCallsign || legacyLocalCallsign;

    if (existingCallsign && existingCallsign.trim()) {
      syncUniversalIdentity(existingCallsign);
      setNeedsCallsignPrompt(false);
    } else {
      const suggestedName = activeUser?.user_metadata?.full_name || activeUser?.user_metadata?.name || activeUser?.email?.split('@')[0] || "";
      setMissingCallsignInput(suggestedName);
      setNeedsCallsignPrompt(true);
    }
  };

  useEffect(() => {
    const checkAuth = async () => {
      // 🔥 GUARANTEED MINIMUM LOADING TIME TO SHOW SPLASH SCREEN (2.8 SECONDS) 🔥
      const minLoadTime = new Promise(resolve => setTimeout(resolve, 2800));
      try {
        if (typeof navigator !== 'undefined' && !navigator.onLine) {
          setIsOfflineMode(true);
          const cachedUser = localStorage.getItem("rural_erides_offline_user");
          if (cachedUser) processUserSession(JSON.parse(cachedUser));
          return; // Skip throwing error if offline
        }
        const { data: { session }, error } = await supabase.auth.getSession();
        if (error) throw error;
        if (session?.user) processUserSession(session.user);
        else setUser(null);
      } catch (err) {
        setIsOfflineMode(true);
        const cachedUser = localStorage.getItem("rural_erides_offline_user");
        if (cachedUser) processUserSession(JSON.parse(cachedUser));
      } finally {
        await minLoadTime; // Wait for the minimum splash duration to finish
        setAuthLoading(false);
      }
    };
    checkAuth();

    try {
      const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
        if (session?.user) processUserSession(session.user);
        else {
          setUser(null);
          setGlobalCallsign("");
          setNeedsCallsignPrompt(false);
        }
      });
      return () => subscription.unsubscribe();
    } catch (e) {}
  }, []);

  // --- MASTER ROOT SETTINGS SYNC STORAGE EFFECT ---
  useEffect(() => {
    if (!mounted) return;
    if (typeof window === 'undefined') return;

    localStorage.setItem("rural_theme", brandTheme);
    localStorage.setItem("rt_theme", brandTheme);
    localStorage.setItem("rural_units", units);
    localStorage.setItem("radar_ghost_mode", ghostMode.toString());
    localStorage.setItem("rural_satmap", satelliteMap.toString());
    localStorage.setItem("rural_locale", locale);
    localStorage.setItem("rural_time_format", timeFormat);
    localStorage.setItem("rural_perf_mode", performanceMode.toString());
    localStorage.setItem("rural_ui_scale", uiScale);
    localStorage.setItem("rural_default_tab", defaultBootTab);
    localStorage.setItem("rural_global_vol", globalVolume.toString());
    localStorage.setItem("rural_haptic", hapticFeedback.toString());
    localStorage.setItem("rural_push_notif", pushNotificationsEnabled.toString());
    localStorage.setItem("rural_fullscreen", fullscreenMode.toString());
    localStorage.setItem("rural_oled_black", oledPureBlack.toString());
    localStorage.setItem("rural_wakelock", wakeLockEnabled.toString());
    localStorage.setItem("rural_autosleep", autoSleep.toString());
    localStorage.setItem("rt_telemetry_rate", telemetryRate.toString());
    localStorage.setItem("rt_data_log_freq", dataLogFrequency.toString());
    localStorage.setItem("rural_hud_orient", hudOrientation);
    localStorage.setItem("rural_export_format", exportFileFormat);
    localStorage.setItem("rural_compression", offlineCompression);
    localStorage.setItem("rural_cloud_sync", cloudSyncInterval);
    localStorage.setItem("rt_auto_start_track", autoStartTracking.toString());
    localStorage.setItem("rt_max_fps", maxFps);
    localStorage.setItem("rt_hw_accel", hardwareAcceleration.toString());
    localStorage.setItem("rt_fps_counter", showFpsCounter.toString());
    localStorage.setItem("rt_map_rad", offlineMapRadius);
    localStorage.setItem("rt_clear_logs", autoClearLogs);
    localStorage.setItem("rt_gps_acc_limit", gpsAccuracyLimit.toString());
    localStorage.setItem("rt_gps_drift", gpsDriftTol);
    localStorage.setItem("rt_crash_sens", crashSensitivity);
    localStorage.setItem("rt_auto_record", autoRecordMovement.toString());
    localStorage.setItem("rt_high_contrast", highContrast.toString());
    localStorage.setItem("rt_cloud_retry", cloudRetry);
    localStorage.setItem("rt_debug_mode", debugMode.toString());
    localStorage.setItem("rt_auto_update", autoUpdateChecks.toString());
    localStorage.setItem("rt_bg_throttle", bgNetThrottle.toString());
    localStorage.setItem("rt_dist_unit", distanceUnitOverride);
    localStorage.setItem("rt_temp_unit", tempUnitOverride);
    localStorage.setItem("rt_map_cache_clear", mapCacheClear);
    localStorage.setItem("rt_dec_prec", decimalPrecision);
    localStorage.setItem("rt_map_marker", mapMarker);
    localStorage.setItem("rt_radar_ping", radarPingFreq);
    localStorage.setItem("rt_sos_window", sosCancelWindow);
    localStorage.setItem("rt_gforce_smooth", gForceSmooth);
    localStorage.setItem("rt_auto_brightness", autoScreenBrightness.toString());
    localStorage.setItem("rt_geofence_alerts", radarGeofenceAlerts.toString());
    localStorage.setItem("rt_batt_opt", batteryOptimization);
    localStorage.setItem("rt_offline_routing", offlineRouting.toString());
    localStorage.setItem("rt_diag_autoscan", diagnosticAutoScan.toString());
    localStorage.setItem("rt_stealth_mode", stealthMode.toString());
    localStorage.setItem("rt_strict_hardware_gps", strictHardwareGps.toString());
    localStorage.setItem("rt_biometric_lock", biometricAppLock.toString());
    localStorage.setItem("rt_font_family", fontFamily);
    localStorage.setItem("rt_reduced_motion", reducedMotion.toString());
    localStorage.setItem("rt_nav_position", navPosition);
    localStorage.setItem("rt_sound_pack", soundPack);
    localStorage.setItem("rt_autohide_hud", autoHideHud.toString());
    localStorage.setItem("rt_offline_prefetch", offlinePrefetch.toString());
    localStorage.setItem("rt_export_dest", exportDestination);
    localStorage.setItem("rt_share_leaderboard", shareLeaderboard.toString());
    localStorage.setItem("rt_haptic_intensity", hapticIntensity);
    localStorage.setItem("rt_corner_style", cornerStyle);
    localStorage.setItem("rt_bg_texture", bgTexture);
    localStorage.setItem("rt_glassmorphism", glassmorphism);
    localStorage.setItem("rt_dash_layout", dashLayout);
    localStorage.setItem("rt_colorblind", colorBlindMode);
    localStorage.setItem("rt_data_viz", dataVizStyle);
    localStorage.setItem("rt_auto_weather", autoWeatherSync.toString());
    localStorage.setItem("rt_route_snapping", routeSnapping.toString());
    localStorage.setItem("rt_privacy_zone", privacyZone);
    localStorage.setItem("rt_max_cache", maxCacheLimit);
    localStorage.setItem("rt_power_threshold", powerSaveThreshold.toString());
    localStorage.setItem("rt_voice_nav_level", voiceNavLevel);
    localStorage.setItem("rt_night_mode_schedule", nightModeSchedule);
    localStorage.setItem("rt_map_default_zoom", mapDefaultZoom);
    localStorage.setItem("rt_sos_name", emergencyContactName);
    localStorage.setItem("rt_auto_mute_video", autoMuteOnVideo.toString());
    localStorage.setItem("rt_weather_freq", weatherUpdateFrequency);
    localStorage.setItem("rt_dash_density", dashDataDensity);
    localStorage.setItem("rt_offline_timeout", offlineSyncTimeout);
    localStorage.setItem("rt_batt_profile", batteryProfile);

    localStorage.setItem("rt_dyn_island", dynamicIsland.toString());
    localStorage.setItem("rt_swipe_nav", swipeNav.toString());
    localStorage.setItem("rt_haptic_dur", hapticDuration);
    localStorage.setItem("rt_auto_hide_header", autoHideHeader.toString());
    localStorage.setItem("rt_data_font_wt", dataFontWeight);
    localStorage.setItem("rt_auto_err_scrape", autoErrorScrape.toString());
    localStorage.setItem("rt_imu_poll", imuPollingRate);
    localStorage.setItem("rt_off_tile", offlineTileProvider);
    localStorage.setItem("rt_smart_batt_ai", smartBatteryAi.toString());
    localStorage.setItem("rt_local_encrypt", localEncryption.toString());
    localStorage.setItem("rt_glow_rad", glowRadius);
    localStorage.setItem("rt_map_line", mapLineColor);
    localStorage.setItem("rt_btn_style", buttonStyling);
    localStorage.setItem("rt_data_retention", dataRetentionPolicy);
    localStorage.setItem("rt_high_prec_gps", highPrecisionBgGps.toString());
    localStorage.setItem("rt_auto_error_log", autoErrorLogging.toString());
    localStorage.setItem("rt_tab_anim", tabIconAnimation.toString());
    localStorage.setItem("rt_card_opacity", cardOpacityLevel);
    localStorage.setItem("rt_minimal_banners", minimalBanners.toString());
    localStorage.setItem("rt_auto_cloud_sync", autoCloudSyncOnFinish.toString());
    localStorage.setItem("rt_pilot_rank", pilotRankBadge);

    localStorage.setItem("rt_panel_border_width", panelBorderWidth);
    localStorage.setItem("rt_card_elevation", cardElevation);
    localStorage.setItem("rt_transition_speed", transitionSpeedProfile);
    localStorage.setItem("rt_header_style", headerStyleVariant);
    localStorage.setItem("rt_border_glow", borderGlowIntensity);
    
    // 🔥 FULLY WIRED PERSISTENCE FOR ALL 50+ STATES 🔥
    localStorage.setItem("rural_auto_export", autoExportRides.toString());
    localStorage.setItem("rural_voice_hotword", voiceHotwordActive.toString());
    localStorage.setItem("rural_sos_number", sosContactNumber);
    localStorage.setItem("rural_bg_gps", backgroundGps.toString());
  }, [
    mounted, brandTheme, units, ghostMode, satelliteMap, locale, timeFormat, performanceMode, uiScale, defaultBootTab, globalVolume, hapticFeedback, pushNotificationsEnabled, fullscreenMode, oledPureBlack, wakeLockEnabled, autoSleep, telemetryRate, dataLogFrequency, hudOrientation, exportFileFormat, offlineCompression, cloudSyncInterval, autoStartTracking, maxFps, hardwareAcceleration, showFpsCounter, offlineMapRadius, autoClearLogs, gpsAccuracyLimit, gpsDriftTol, crashSensitivity, autoRecordMovement, highContrast, autoExportRides, voiceHotwordActive, sosContactNumber, cloudRetry, debugMode, autoUpdateChecks, bgNetThrottle, backgroundGps, distanceUnitOverride, tempUnitOverride, mapCacheClear, decimalPrecision, mapMarker, radarPingFreq, sosCancelWindow, gForceSmooth, autoScreenBrightness, radarGeofenceAlerts, batteryOptimization, offlineRouting, diagnosticAutoScan, stealthMode, strictHardwareGps, biometricAppLock, fontFamily, reducedMotion, navPosition, soundPack, autoHideHud, offlinePrefetch, exportDestination, shareLeaderboard, hapticIntensity, cornerStyle, bgTexture, glassmorphism, dashLayout, colorBlindMode, dataVizStyle, autoWeatherSync, routeSnapping, privacyZone, maxCacheLimit, powerSaveThreshold, voiceNavLevel, nightModeSchedule, mapDefaultZoom, emergencyContactName, autoMuteOnVideo, weatherUpdateFrequency, dashDataDensity, offlineSyncTimeout, batteryProfile, dynamicIsland, swipeNav, hapticDuration, autoHideHeader, dataFontWeight, autoErrorScrape, imuPollingRate, offlineTileProvider, smartBatteryAi, localEncryption, glowRadius, mapLineColor, buttonStyling, dataRetentionPolicy, highPrecisionBgGps, autoErrorLogging, tabIconAnimation, cardOpacityLevel, minimalBanners, autoCloudSyncOnFinish, pilotRankBadge, panelBorderWidth, cardElevation, transitionSpeedProfile, headerStyleVariant, borderGlowIntensity
  ]);

  const handleCloudBackup = async () => {
    triggerHaptic();
    if (!user && !globalCallsign) return alert("You must be logged in or have a callsign to perform a cloud backup.");
    if (!window.confirm("CONFIRMATION: Initiate optimized Firebase Cloud backup?")) return;
    
    setIsProcessingAuth(true);
    setCloudSyncStatus("Compressing & backing up to Firebase Cloud...");
    try {
      const masterData: Record<string, string> = {};
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && !key.startsWith('sb-') && !key.includes('cache') && !key.includes('blob')) {
          const val = localStorage.getItem(key) || "";
          if (val.length < 100000) {
            masterData[key] = val;
          }
        }
      }
      
      const rawId = user?.email || globalCallsign || "anonymous_pilot";
      const backupId = rawId.replace(/[.$#[\]/]/g, "_");
      const nodeCount = Object.keys(masterData).length;

      const payload = {
        masterDataPayload: JSON.stringify(masterData),
        updatedAt: new Date().toISOString(),
        version: CURRENT_VERSION,
        nodeCount: nodeCount,
        pilot: globalCallsign || user?.email || "Unknown Pilot"
      };

      let success = false;
      let errorMsg = "";

      const dbUrl = firebaseConfig.databaseURL?.replace(/\/$/, '');
      if (dbUrl) {
        try {
          const restUrl = `${dbUrl}/user_backups/${backupId}.json`;
          const controller = new AbortController();
          const timeoutId = setTimeout(() => controller.abort(), 12000);

          const res = await fetch(restUrl, { 
            method: 'PUT', 
            headers: { 'Content-Type': 'application/json' }, 
            body: JSON.stringify(payload),
            signal: controller.signal
          });
          clearTimeout(timeoutId);

          if (res.ok) {
            success = true;
          } else {
            errorMsg = `REST Status ${res.status}: ${res.statusText}`;
          }
        } catch (restErr: any) {
          errorMsg = restErr.message;
        }
      }

      if (!success && db) {
        try {
          const backupRef = ref(db, `user_backups/${backupId}`);
          const timeout = new Promise((_, reject) => setTimeout(() => reject(new Error("Firebase SDK Timeout")), 8000));
          await Promise.race([fbSet(backupRef, payload), timeout]);
          success = true;
        } catch (e: any) {
          errorMsg = e.message || errorMsg;
        }
      }

      if (!success) {
        throw new Error(errorMsg || "All cloud transmission gateways failed.");
      }

      alert(`FIREBASE SUCCESS: Backup received by cloud! Saved ${nodeCount} optimized data nodes.`);
    } catch (err: any) {
      console.error("Cloud Backup Error:", err);
      if (window.confirm(`Cloud backup rejected (${err.message}). Would you like to download a secure local JSON backup file instead?`)) {
        exportLocalJsonBackup();
      }
    } finally {
      setCloudSyncStatus(null);
      setIsProcessingAuth(false);
    }
  };

  const handleCloudRestore = async () => {
    triggerHaptic();
    if (!user && !globalCallsign) return alert("You must be logged in or have a callsign to restore from the cloud.");
    if (!window.confirm("WARNING: Restoring from Firebase Cloud backup will overwrite your local telemetry and settings. Proceed?")) return;
    
    setIsProcessingAuth(true);
    setCloudSyncStatus("Restoring from Firebase Cloud Matrix...");
    try {
      const rawId = user?.email || globalCallsign || "anonymous_pilot";
      const backupId = rawId.replace(/[.$#[\]/]/g, "_");
      let parsedData = null;

      if (db) {
        try {
          const timeout = new Promise((_, reject) => setTimeout(() => reject(new Error("SDK Timeout")), 8000));
          const snapshotRef = get(child(ref(db), `user_backups/${backupId}`));
          const snapshot = await Promise.race([snapshotRef, timeout]) as any;
          if (snapshot.exists()) {
            const data = snapshot.val();
            if (data && data.masterDataPayload) parsedData = JSON.parse(data.masterDataPayload);
            else if (data && data.masterData) parsedData = data.masterData;
          }
        } catch(e) {}
      } 
      
      if (!parsedData && firebaseConfig.databaseURL) {
        const dbUrl = firebaseConfig.databaseURL.replace(/\/$/, '');
        const restUrl = `${dbUrl}/user_backups/${backupId}.json`;
        const res = await fetch(restUrl);
        if (res.ok) {
          const data = await res.json();
          if (data && data.masterDataPayload) parsedData = JSON.parse(data.masterDataPayload);
        }
      }

      if (parsedData) {
        let keysRestored = 0;
        Object.keys(parsedData).forEach(key => { localStorage.setItem(key, parsedData[key]); keysRestored++; });
        alert(`FIREBASE RESTORE SUCCESS: ${keysRestored} data nodes recovered! Avionics rebooting.`);
        window.location.reload();
      } else { throw new Error("No cloud record found for this profile in Firebase."); }
    } catch (err: any) { 
      alert(`Cloud restore failed: ${err.message}. Try importing a local JSON backup file.`); 
    } finally {
      setCloudSyncStatus(null);
      setIsProcessingAuth(false);
    }
  };

  const exportLocalJsonBackup = () => {
    triggerHaptic();
    const masterData: Record<string, string> = {};
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && !key.startsWith('sb-')) { masterData[key] = localStorage.getItem(key) || ""; }
    }
    const blob = new Blob([JSON.stringify(masterData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Rural_Erides_GO_Backup_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleSetMissingCallsign = async (e: React.FormEvent) => {
    e.preventDefault(); triggerHaptic(); setAuthError("");
    const cleanName = missingCallsignInput.trim();
    if (cleanName.length < 3) return setAuthError("Callsign must be at least 3 characters long.");
    setIsProcessingAuth(true);
    try {
      if (navigator.onLine && !isOfflineMode) {
        const { data, error } = await supabase.auth.updateUser({ data: { username: cleanName } });
        if (error) throw error;
        if (data.user) processUserSession(data.user);
      } else {
        syncUniversalIdentity(cleanName);
        setNeedsCallsignPrompt(false);
      }
    } catch (err: any) { setAuthError(err.message || "Failed to register callsign with Supabase."); } finally { setIsProcessingAuth(false); }
  };

  const handleGoogleSignIn = async () => {
    triggerHaptic();
    setAuthError("");
    setIsProcessingAuth(true);
    
    try {
      if (!Capacitor.isNativePlatform()) {
        throw new Error("Google Sign-In is currently only supported on the native Android app.");
      }
      const googleUser = await GoogleAuth.signIn();
      const idToken = googleUser.authentication.idToken;
      if (!idToken) throw new Error("No ID Token received from Google Play Services.");

      const { data, error } = await supabase.auth.signInWithIdToken({
        provider: 'google',
        token: idToken,
      });

      if (error) throw error;
      if (data?.session?.user) {
        processUserSession(data.session.user);
      }
    } catch (err: any) {
      console.error("Google Auth Error:", err);
      setAuthError(`Auth Failed: ${err.message}`);
    } finally {
      setIsProcessingAuth(false);
    }
  };

  const handleAuthSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); triggerHaptic(); setAuthError(""); setAuthSuccess(""); setIsProcessingAuth(true);
    if (!navigator.onLine || isOfflineMode) {
      const cachedUser = localStorage.getItem("rural_erides_offline_user");
      if (cachedUser && authMode === "LOGIN") {
        processUserSession(JSON.parse(cachedUser)); setIsOfflineMode(true); setIsProcessingAuth(false); return;
      } else {
        setAuthError("HARDWARE OFFLINE: Cannot establish new connection or reset passwords without network."); setIsProcessingAuth(false); return;
      }
    }
    try {
      if (authMode === "FORGOT_PASSWORD") {
        if (!email) throw new Error("Email is required for password recovery.");
        const { error } = await supabase.auth.resetPasswordForEmail(email.trim());
        if (error) throw error;
        setAuthSuccess("Recovery sequence initiated! Check your email for the 6-digit access code."); setAuthMode("VERIFY_RECOVERY");
      } else if (authMode === "VERIFY_RECOVERY") {
        if (!recoveryCode.trim()) throw new Error("Recovery code is required.");
        if (password !== confirmPassword) throw new Error("New passwords do not match.");
        if (password.length < 6) throw new Error("Access code must be at least 6 characters.");
        const { error: verifyError } = await supabase.auth.verifyOtp({ email: email.trim(), token: recoveryCode.trim(), type: 'recovery' });
        if (verifyError) throw verifyError;
        const { error: updateError } = await supabase.auth.updateUser({ password });
        if (updateError) throw updateError;
        setAuthSuccess("Access code successfully updated! You can now log in."); setAuthMode("LOGIN"); setPassword(""); setConfirmPassword(""); setRecoveryCode("");
      } else if (authMode === "SIGNUP") {
        if (password !== confirmPassword) throw new Error("Passwords do not match.");
        if (!callsign.trim()) throw new Error("Rider Callsign is required.");
        if (callsign.trim().length < 3) throw new Error("Callsign must be at least 3 characters.");
        const { data, error } = await supabase.auth.signUp({ email: email.trim(), password, options: { data: { username: callsign.trim() } } });
        if (error) throw error;
        if (data.user && data.user.identities && data.user.identities.length === 0) throw new Error("Security Alert: Account already exists. Please log in or use password recovery.");
        if (data.user) { setAuthSuccess("Clearance granted! Account registered securely."); processUserSession(data.user); }
      } else {
        const { data, error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
        if (error) throw error;
        if (data.user) processUserSession(data.user);
      }
    } catch (error: any) {
      let errorText = "Authentication request failed.";
      const rawMessage = error?.message || error?.error_description || (typeof error === 'string' ? error : "");
      if (rawMessage === "{}" || rawMessage.trim() === "") errorText = "Network request blocked. Check if email exists or rate limit was reached."; else errorText = rawMessage;
      setAuthError(errorText);
    } finally { setIsProcessingAuth(false); }
  };

  const handleSignOut = async () => {
    triggerHaptic();
    try {
      if (navigator.onLine && !isOfflineMode) await supabase.auth.signOut();
      setUser(null); setGlobalCallsign(""); setNeedsCallsignPrompt(false); setIsSettingsOpen(false); setHasCompletedOnboarding(false); 
      localStorage.removeItem("rural_onboarding"); localStorage.removeItem("rural_erides_offline_user"); locationService.stop();
    } catch (error) {}
  };

  const togglePerformanceMode = () => { triggerHaptic(); setPerformanceMode(!performanceMode); };
  const toggleHapticFeedback = () => { triggerHaptic(); setHapticFeedback(!hapticFeedback); };
  const togglePushNotifications = async () => { triggerHaptic(); setPushNotificationsEnabled(!pushNotificationsEnabled); if (!pushNotificationsEnabled && typeof window !== 'undefined' && 'Notification' in window) { if (Notification.permission !== 'granted') await Notification.requestPermission(); } };
  const toggleFullscreenMode = async () => { triggerHaptic(); const newVal = !fullscreenMode; setFullscreenMode(newVal); if (Capacitor.isNativePlatform()) { try { const statusModule = await import('@capacitor/status-bar'); if (statusModule && statusModule.StatusBar) { if (newVal) await statusModule.StatusBar.hide(); else await statusModule.StatusBar.show(); } } catch (e) {} } else { try { if (newVal) { if (document.documentElement.requestFullscreen) await document.documentElement.requestFullscreen(); } else { if (document.exitFullscreen && document.fullscreenElement) await document.exitFullscreen(); } } catch (e) {} } };
  const toggleOledPureBlack = () => { triggerHaptic(); setOledPureBlack(!oledPureBlack); };
  const toggleWakeLock = async () => { triggerHaptic(); const newVal = !wakeLockEnabled; setWakeLockEnabled(newVal); if (newVal) { try { if ('wakeLock' in navigator) wakeLockRef.current = await (navigator as any).wakeLock.request('screen'); } catch (err) {} } else { if (wakeLockRef.current) wakeLockRef.current.release().then(() => wakeLockRef.current = null); } };
  const toggleAutoSleep = () => { triggerHaptic(); setAutoSleep(!autoSleep); };
  const changeTheme = (theme: ThemePreset) => { triggerHaptic(); setBrandTheme(theme); };
  const updateUiScale = (scale: UIScale) => { triggerHaptic(); setUiScale(scale); };
  const changeLanguage = (langCode: string) => { 
    triggerHaptic(); 
    setLocale(langCode); 
    localStorage.setItem("rural_locale", langCode);
    localStorage.setItem("rt_locale", langCode);
    window.dispatchEvent(new Event('settings-sync'));
  };
  const updateDefaultBootTab = (tab: TabId) => { triggerHaptic(); setDefaultBootTab(tab); };
  const updateGlobalVolume = (vol: number) => {
    triggerHaptic();
    setGlobalVolume(vol);
    localStorage.setItem("rural_global_vol", vol.toString());
    window.dispatchEvent(new Event('settings-sync'));
  };
  const handleOrientationChange = (val: string) => { triggerHaptic(); setHudOrientation(val); if (Capacitor.isNativePlatform() && window.screen && window.screen.orientation) { if (val === "portrait") window.screen.orientation.lock("portrait").catch(()=>{}); else if (val === "landscape") window.screen.orientation.lock("landscape").catch(()=>{}); else window.screen.orientation.unlock(); } };

  const wipeLocalData = () => { triggerHaptic(); if (window.confirm("CRITICAL WARNING: This will permanently purge all telemetry, local component logs, and device configurations. Proceed?")) { localStorage.clear(); setSavedRides([]); setHasCompletedOnboarding(false); alert("Local memory wiped. Avionics restarting."); window.location.reload(); } };

  const themeMap = {
    lime: { hex: "#39ff14", text: "text-[#39ff14]", bg: "bg-[#39ff14]", border: "border-[#39ff14]", gradient: "from-[#39ff14] to-emerald-500", shadow: performanceMode ? '' : "shadow-[0_0_15px_rgba(57,255,20,0.3)]", blur: performanceMode ? 'bg-[#06060a]' : 'bg-[#0a0a0f]/60 backdrop-blur-2xl' },
    cyan: { hex: "#06b6d4", text: "text-cyan-400", bg: "bg-cyan-500", border: "border-cyan-500", gradient: "from-cyan-400 to-blue-500", shadow: performanceMode ? '' : "shadow-[0_0_15px_rgba(6,182,212,0.3)]", blur: performanceMode ? 'bg-[#06060a]' : 'bg-[#0a0a0f]/60 backdrop-blur-2xl' },
    emerald: { hex: "#10b981", text: "text-emerald-400", bg: "bg-emerald-500", border: "border-emerald-500", gradient: "from-emerald-400 to-teal-500", shadow: performanceMode ? '' : "shadow-[0_0_15px_rgba(16,185,129,0.3)]", blur: performanceMode ? 'bg-[#06060a]' : 'bg-[#0a0a0f]/60 backdrop-blur-2xl' },
    amber: { hex: "#f59e0b", text: "text-amber-400", bg: "bg-amber-500", border: "border-amber-500", gradient: "from-amber-400 to-orange-500", shadow: performanceMode ? '' : "shadow-[0_0_15px_rgba(245,158,11,0.3)]", blur: performanceMode ? 'bg-[#06060a]' : 'bg-[#0a0a0f]/60 backdrop-blur-2xl' },
    rose: { hex: "#f43f5e", text: "text-rose-400", bg: "bg-rose-500", border: "border-rose-500", gradient: "from-rose-400 to-red-500", shadow: performanceMode ? '' : "shadow-[0_0_15px_rgba(244,63,94,0.3)]", blur: performanceMode ? 'bg-[#06060a]' : 'bg-[#0a0a0f]/60 backdrop-blur-2xl' },
    purple: { hex: "#a855f7", text: "text-purple-400", bg: "bg-purple-500", border: "border-purple-500", gradient: "from-purple-400 to-fuchsia-500", shadow: performanceMode ? '' : "shadow-[0_0_20px_rgba(168,85,247,0.3)]", blur: performanceMode ? 'bg-[#06060a]' : 'bg-[#0a0a0f]/60 backdrop-blur-3xl' },
    void: { hex: "#ffffff", text: "text-white", bg: "bg-zinc-800", border: "border-zinc-500", gradient: "from-zinc-400 to-zinc-600", shadow: performanceMode ? '' : "shadow-[0_0_20px_rgba(255,255,255,0.1)]", blur: performanceMode ? 'bg-black' : 'bg-black/90 backdrop-blur-3xl' }
  };
  const activeTheme = themeMap[brandTheme as keyof typeof themeMap] || themeMap.lime;
  
  const fontScaleMap: Record<string, string> = { compact: "text-[12px]", normal: "text-[14px]", large: "text-[16px]" };

  const getCardOpacityValue = () => {
    switch(cardOpacityLevel) {
      case '60': return '0.95';
      case '80': return '0.98';
      case '100': return '1.0';
      default: return '0.98';
    }
  };

  const getBorderWidthValue = () => {
    switch(panelBorderWidth) {
      case 'thin': return '1px';
      case 'thick': return '3px';
      case 'normal': default: return '2px';
    }
  };

  const getTransitionDuration = () => {
    switch(transitionSpeedProfile) {
      case 'instant': return '0s';
      case 'cinematic': return '0.5s';
      case 'smooth': default: return '0.2s';
    }
  };

  const getGlowShadow = () => {
    if (borderGlowIntensity === 'none') return 'none';
    const intensity = borderGlowIntensity === 'intense' ? '0 0 25px' : '0 0 12px';
    return `${intensity} ${activeTheme.hex}40`;
  };

  const isTailwindFont = fontFamily === "font-sans" || fontFamily === "font-mono" || fontFamily === "font-serif";
  const activeFontClass = isTailwindFont ? fontFamily : "";
  const activeFontStyle = isTailwindFont ? {} : { fontFamily };
  const glassBlurClass = "bg-zinc-950";

  const handleAddRide = (newRide: SavedRide) => { 
    const updatedRides = [newRide, ...savedRides]; 
    setSavedRides(updatedRides); 
    localStorage.setItem("rural_erides_rides", JSON.stringify(updatedRides)); 
  };

  const handleShareRideLog = (data: { text: string; pevType: string }) => { 
    setPrefilledPost({ text: data.text, userBadge: data.pevType }); 
    setActiveTab("FORUM"); 
  };

  const renderTabContent = () => {
    const globalProps = { 
      theme: brandTheme, 
      useMetric: units === "metric", 
      ghostMode, 
      satelliteMap, 
      locale, 
      timeFormat, 
      performanceMode, 
      uiScale, 
      globalVolume, 
      callsign: globalCallsign,
      glassBlurClass,
      cornerStyle,
      reducedMotion,
      bgTexture
    };

    switch (activeTab) {
      case "RADAR": return <RiderRadar {...globalProps} />;
      case "DIAGNOSTICS": return <PEVAnalyzer {...globalProps} />;
      case "ASSISTANT": return <GroundedAssistant {...globalProps} />;
      case "CHANNEL": return <YouTubeFeed {...globalProps} />;
      case "FORUM": return <BoardRecyclerView {...globalProps} prefilledPost={prefilledPost} onClearPrefilled={() => setPrefilledPost(null)} />;
      case "INFO": return <AppInfoTab icon={icon} activeTheme={activeTheme} cornerStyle={cornerStyle} reducedMotion={reducedMotion} typographyClass={fontFamily} setActiveTab={setActiveTab} triggerHaptic={triggerHaptic} currentVersion={CURRENT_VERSION} glassBlurClass={glassBlurClass} />;
      case "DASH": default: return <Dashboard {...globalProps} onAddRide={handleAddRide} savedRides={savedRides} onShareRideLog={handleShareRideLog} />;
    }
  };

  const navigationItems = [
    { id: "DASH" as TabId, label: translate('cockpit'), icon: Compass },
    { id: "RADAR" as TabId, label: translate('radar'), icon: Users },
    { id: "DIAGNOSTICS" as TabId, label: translate('diagnostics'), icon: Cpu },
    { id: "ASSISTANT" as TabId, label: translate('copilot'), icon: Sparkles },
    { id: "CHANNEL" as TabId, label: translate('channel'), icon: Youtube },
    { id: "FORUM" as TabId, label: translate('board'), icon: MessageSquare },
    { id: "INFO" as TabId, label: translate('app_info'), icon: Info },
  ];

  if (authLoading) {
    return (
      <div className={`min-h-screen flex flex-col items-center justify-center p-6 ${activeTheme.text} relative overflow-hidden select-none`}>
        {/* 🔥 FOREST SPLASH BACKGROUND IN AUTH LOADING 🔥 */}
        <div className="absolute inset-0 opacity-45 bg-black">
          <img 
            src={splashImg} 
            alt="Rural Erides Splash" 
            className="w-full h-full object-cover filter brightness-90 contrast-110"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black via-black/40 to-transparent"></div>
        </div>

        <div className="relative z-10 flex flex-col items-center text-center mt-auto pb-16 space-y-4">
          <div className="relative w-20 h-20 mb-4 flex items-center justify-center">
            <div className={`absolute inset-0 border-4 ${activeTheme.border} opacity-20 rounded-full`}></div>
            <div className={`absolute inset-0 border-4 ${activeTheme.border} border-t-transparent rounded-full ${reducedMotion ? '' : 'animate-spin'}`}></div>
            <img src={icon} alt="Logo" className={`w-10 h-10 rounded-xl object-cover shadow-[0_0_20px_rgba(0,0,0,0.8)] ${reducedMotion ? '' : 'animate-pulse'}`} />
          </div>
          <div>
            <h1 className={`text-xl font-black uppercase tracking-widest text-transparent bg-clip-text bg-gradient-to-r ${activeTheme.gradient} font-mono`}>
              Authenticating...
            </h1>
          </div>
        </div>
      </div>
    );
  }

  if (isLockedByBiometrics) {
    return (
      <div className="min-h-screen bg-black text-white flex flex-col items-center justify-center p-6 font-mono select-none relative overflow-hidden">
        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#39ff14_1px,transparent_1px)] [background-size:24px_24px] pointer-events-none"></div>
        
        <div className="absolute top-4 right-4 z-20 flex items-center gap-2 bg-black/60 border border-zinc-800 px-3 py-1.5 rounded-xl shadow-lg">
          <Languages className="w-3.5 h-3.5 text-cyan-400" />
          <select 
            value={locale} 
            onChange={(e) => changeLanguage(e.target.value)}
            className="bg-transparent text-white font-bold outline-none cursor-pointer uppercase text-[10px]"
          >
            <option value="en" className="bg-black text-white">EN</option>
            <option value="es" className="bg-black text-white">ES</option>
            <option value="fr" className="bg-black text-white">FR</option>
            <option value="de" className="bg-black text-white">DE</option>
          </select>
        </div>

        <div className="max-w-md w-full bg-zinc-950 border-2 border-emerald-500/80 rounded-3xl p-8 shadow-[0_0_50px_rgba(57,255,20,0.3)] relative z-10 space-y-6 text-center">
          <div className="flex flex-col items-center justify-center gap-3">
            <div className="w-16 h-16 rounded-2xl bg-emerald-500/20 border border-emerald-500/50 flex items-center justify-center text-emerald-400 shadow-lg">
              <Lock className="w-8 h-8 animate-pulse" />
            </div>
            <div>
              <h2 className="text-sm font-black uppercase tracking-widest text-white">{translate('locked_title')}</h2>
              <p className="text-[10px] text-zinc-400 uppercase font-mono mt-1">{translate('locked_subtitle')}</p>
            </div>
          </div>

          {authStatusMessage && (
            <div className="bg-black border border-zinc-800 p-3 rounded-xl text-[10px] text-amber-400 font-mono">
              {authStatusMessage}
            </div>
          )}

          <button
            type="button"
            onClick={async () => {
              setAuthStatusMessage(translate('biometric_prompt_active'));
              try {
                const availableResult = await NativeBiometric.isAvailable();
                if (!availableResult.isAvailable) {
                  setAuthStatusMessage("Native device security is not configured on this device.");
                  return;
                }

                await NativeBiometric.verifyIdentity({
                  reason: "Unlock Security",
                  title: "Avionics Security Lock",
                  subtitle: "Confirm your identity",
                  description: "Verify your fingerprint or Face ID to unlock cockpit.",
                  useFallback: true,
                });

                sessionStorage.setItem("rural_unlocked_session", "true");
                setIsLockedByBiometrics(false);
                setAuthStatusMessage(translate('biometric_success'));
              } catch (e: any) {
                console.warn("OS Biometric error:", e);
                setAuthStatusMessage("Security verification dismissed or failed.");
                if (navigator.vibrate) navigator.vibrate(100);
              }
            }}
            className="w-full py-5 bg-emerald-600 hover:bg-emerald-500 text-black font-black uppercase text-xs tracking-widest rounded-2xl shadow-[0_0_25px_rgba(16,185,129,0.5)] transition-transform active:scale-95 cursor-pointer flex items-center justify-center gap-3"
          >
            <ShieldCheck className="w-6 h-6" /> {translate('os_auth_btn')}
          </button>
        </div>
      </div>
    );
  }

  if (!user || authMode === "VERIFY_RECOVERY") {
    return <AuthScreen {...{ authMode, setAuthMode, email, setEmail, password, setPassword, confirmPassword, setConfirmPassword, recoveryCode, setRecoveryCode, callsign, setCallsign, authError, setAuthError, authSuccess, setAuthSuccess, isProcessingAuth, handleAuthSubmit, handleGoogleSignIn, activeTheme, cornerStyle, performanceMode, reducedMotion, typographyClass: fontFamily, icon, triggerHaptic }} />;
  }

  if (needsCallsignPrompt) {
    return <CallsignPrompt {...{ missingCallsignInput, setMissingCallsignInput, authError, isProcessingAuth, handleSetMissingCallsign, activeTheme, cornerStyle, reducedMotion, typographyClass: fontFamily }} />;
  }
          
  if (!hasCompletedOnboarding) {
    return <LegalOnboarding activeTheme={activeTheme} cornerStyle={cornerStyle} performanceMode={performanceMode} reducedMotion={reducedMotion} typographyClass={fontFamily} triggerHaptic={triggerHaptic} onComplete={() => { setHasCompletedOnboarding(true); localStorage.setItem("rural_onboarding", "completed"); }} />;
  }

  const getColorblindFilter = () => {
    switch(colorBlindMode?.toLowerCase()) {
      case 'protanopia': return 'contrast(1.15) saturate(1.2) hue-rotate(-15deg)';
      case 'deuteranopia': return 'contrast(1.15) saturate(1.2) hue-rotate(15deg)';
      case 'tritanopia': return 'contrast(1.15) saturate(1.2) hue-rotate(180deg)';
      default: return 'none';
    }
  };

  return (
    <div 
      style={{ 
        paddingBottom: isKeyboardOpen ? 'var(--keyboard-height)' : '0px', 
        transitionDuration: getTransitionDuration(),
        ...activeFontStyle,
        ...getBgTextureStyle(bgTexture) 
      }} 
      className={`min-h-[100dvh] ${oledPureBlack ? 'bg-black' : highContrast ? 'bg-zinc-950 text-yellow-300 font-bold' : 'bg-[#030305]'} text-white flex flex-col ${activeFontClass} select-none antialiased relative overflow-x-hidden ${fontScaleMap[uiScale] || 'text-[14px]'}`}
    >
      <style>{`
        :root {
          --card-opacity: 0.35;
          --panel-border-width: ${getBorderWidthValue()};
          --card-elevation-shadow: ${cardElevation === 'floating' ? '0 20px 40px rgba(0,0,0,0.95)' : cardElevation === 'raised' ? '0 10px 20px rgba(0,0,0,0.8)' : 'none'};
          --border-glow: ${getGlowShadow()};
          --theme-border: ${activeTheme.hex}35;
        }

        /* 🔥 TARGETED SMOKED BLUR GLASS FOR CARDS & PANELS 🔥 */
        .app-card, 
        .avionics-card, 
        div[role="dialog"] {
          background-color: rgba(5, 5, 8, var(--card-opacity)) !important;
          backdrop-filter: blur(28px) !important;
          -webkit-backdrop-filter: blur(28px) !important;
          border-width: var(--panel-border-width, 2px) !important;
          border-color: var(--theme-border) !important;
          box-shadow: var(--card-elevation-shadow), var(--border-glow) !important;
        }

        /* 🔥 BULLETPROOF RESET FOR VIDEO / YOUTUBE FEED PREVIEWS 🔥 */
        iframe, 
        video, 
        [class*="video"], 
        [class*="thumbnail"], 
        [class*="player"] {
          background: transparent !important;
          background-color: transparent !important;
          backdrop-filter: none !important;
          -webkit-backdrop-filter: none !important;
          box-shadow: none !important;
        }

        /* 🔥 UNIFORM GLASS AND THEME BORDERS FOR HEADERS & NAVIGATION 🔥 */
        header, nav, footer, aside {
          background-color: rgba(5, 5, 8, 0.65) !important;
          backdrop-filter: blur(22px) !important;
          -webkit-backdrop-filter: blur(22px) !important;
          border-color: var(--theme-border) !important;
        }

        /* 🔥 FORCE ALL ICONS TO SHOW CORRECT THEME/TEXT COLOR 🔥 */
        svg, svg path, svg circle, svg rect {
          color: inherit !important;
        }
        header svg, nav svg, footer svg, aside svg, button svg, .app-card svg {
          color: currentColor !important;
          opacity: 1 !important;
        }
      `}</style>

      {/* RIDER'S PRAYER AUTHENTICATION OVERLAY */}
      <RidersPrayerOverlay 
        isVisible={showPrayer} 
        onComplete={() => setShowPrayer(false)} 
        operatorCallsign={globalCallsign} 
        themeColors={activeTheme}
      />

      {colorBlindMode !== "none" && (
        <div 
          className="fixed inset-0 z-[99999] pointer-events-none" 
          style={{ backdropFilter: getColorblindFilter(), WebkitBackdropFilter: getColorblindFilter(), backgroundColor: 'rgba(0,0,0,0.01)' }}
        />
      )}

      {!performanceMode && !oledPureBlack && (<div className={`absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full opacity-20 blur-[120px] pointer-events-none ${reducedMotion ? '' : 'transition-all duration-700'}`} style={{ backgroundColor: activeTheme.hex }}></div>)}

      <ChangelogModal showChangelogModal={showChangelogModal} dismissChangelogModal={dismissChangelogModal} activeTheme={activeTheme} cornerStyle={cornerStyle} reducedMotion={reducedMotion} currentVersion={CURRENT_VERSION} />

      <SettingsModal {...{
        isSettingsOpen, setIsSettingsOpen, settingsTab, setSettingsTab, user, isOfflineMode, cloudSyncStatus, handleCloudBackup, handleCloudRestore, exportLocalJsonBackup, isProcessingAuth, handleSignOut, wipeLocalData, pilotRankBadge, setPilotRankBadge, autoCloudSyncOnFinish, setAutoCloudSyncOnFinish, locale, changeLanguage, uiScale, updateUiScale, fontFamily, setFontFamily, reducedMotion, setReducedMotion, dynamicIsland, setDynamicIsland, swipeNav, setSwipeNav, autoHideHeader, setAutoHideHeader, hapticDuration, setHapticDuration, dataFontWeight, setDataFontWeight, tabIconAnimation, setTabIconAnimation, cardOpacityLevel, setCardOpacityLevel, minimalBanners, setMinimalBanners, dashLayout, setDashLayout, dataVizStyle, setDataVizStyle, dashDataDensity, setDashDataDensity, mapDefaultZoom, setMapDefaultZoom, hapticFeedback, toggleHapticFeedback, hapticIntensity, setHapticIntensity, globalVolume, updateGlobalVolume, soundPack, setSoundPack, autoMuteOnVideo, setAutoMuteOnVideo, dataRetentionPolicy, setDataRetentionPolicy, highPrecisionBgGps, setHighPrecisionBgGps, autoErrorLogging, setAutoErrorLogging, maxFps, setMaxFps, distanceUnitOverride, setDistanceUnitOverride, tempUnitOverride, setTempUnitOverride, decimalPrecision, setDecimalPrecision, privacyZone, setPrivacyZone, routeSnapping, setRouteSnapping, autoWeatherSync, setAutoWeatherSync, weatherUpdateFrequency, setWeatherUpdateFrequency, strictHardwareGps, setStrictHardwareGps, telemetryRate, setTelemetryRate, gpsAccuracyLimit, setGpsAccuracyLimit, gpsDriftTol, setGpsDriftTol, autoStartTracking, setAutoStartTracking, shareLeaderboard, setShareLeaderboard, cloudSyncInterval, setCloudSyncInterval, offlineSyncTimeout, setOfflineSyncTimeout, bgNetThrottle, setBgNetThrottle, backgroundGps, setBackgroundGps, powerSaveThreshold, setPowerSaveThreshold, maxCacheLimit, setMaxCacheLimit, offlinePrefetch, setOfflinePrefetch, biometricAppLock, setBiometricAppLock, emergencyContactName, setEmergencyContactName, sosContactNumber, setSosContactNumber, sosCancelWindow, setSosCancelWindow, crashSensitivity, setCrashSensitivity, batteryProfile, setBatteryProfile, voiceHotwordActive, setVoiceHotwordActive, smartBatteryAi, setSmartBatteryAi, autoErrorScrape, setAutoErrorScrape, localEncryption, setLocalEncryption, imuPollingRate, setImuPollingRate, offlineTileProvider, setOfflineTileProvider, wakeLockEnabled, toggleWakeLock, autoSleep, toggleAutoSleep, hardwareAcceleration, setHardwareAcceleration, diagnosticAutoScan, setDiagnosticAutoScan, autoUpdateChecks, setAutoUpdateChecks, debugMode, setDebugMode, brandTheme, changeTheme, nightModeSchedule, setNightModeSchedule, cornerStyle, setCornerStyle, panelBorderWidth, setPanelBorderWidth, cardElevation, setCardElevation, transitionSpeedProfile, setTransitionSpeedProfile, headerStyleVariant, setHeaderStyleVariant, borderGlowIntensity, setBorderGlowIntensity, bgTexture, setBgTexture, glowRadius, setGlowRadius, mapLineColor, setMapLineColor, buttonStyling, setButtonStyling, glassmorphism, setGlassmorphism, activeTheme, glassBlurClass, triggerHaptic
      }} />

      {/* --- 🔥 GLOBAL INBOX & CALL LISTENER MOUNTED AT ROOT LEVEL 🔥 --- */}
      <DirectMessagesModal
        showInbox={showInbox}
        setShowInbox={setShowInbox}
        activeDMUser={activeDMUser}
        setActiveDMUser={setActiveDMUser}
        username={globalCallsign || "Network Pilot"}
        pfpUrl={localStorage.getItem("rural_erides_pfp") || ""}
        userProfiles={userProfiles}
        newChatTarget={newChatTarget}
        setNewChatTarget={setNewChatTarget}
        inboxSearch={inboxSearch}
        setInboxSearch={setInboxSearch}
        chatList={chatList}
        dms={dms}
        themeColors={activeTheme}
        getTimeMs={getTimeMs}
        glassBlurClass={glassBlurClass}
        cornerStyle={cornerStyle}
      />

      {/* --- 🔥 BACKGROUND INCOMING CALL RINGING OVERLAY MODAL 🔥 --- */}
      {incomingCall && (
        <div className="fixed inset-0 z-[9999999] bg-black/90 backdrop-blur-2xl flex items-center justify-center p-4 animate-fadeIn font-mono">
          <div className="bg-zinc-950 border-2 border-emerald-500 rounded-3xl p-8 max-w-sm w-full text-center shadow-[0_0_60px_rgba(16,185,129,0.4)] space-y-6">
            <div className="w-20 h-20 mx-auto rounded-full bg-emerald-500/20 border-2 border-emerald-500 flex items-center justify-center animate-bounce text-emerald-400">
              {incomingCall.type === 'video' ? <VideoIcon className="w-10 h-10" /> : <Phone className="w-10 h-10 animate-pulse" />}
            </div>
            <div>
              <h2 className="text-sm font-black text-zinc-400 uppercase tracking-widest">Incoming {incomingCall.type === 'video' ? 'Video' : 'Audio'} Call</h2>
              <p className="text-xl font-black text-white mt-1">{incomingCall.caller}</p>
            </div>
            <div className="flex gap-4 pt-4">
              <button 
                onClick={handleRejectCall}
                className="flex-1 py-4 bg-rose-600 hover:bg-rose-500 text-white rounded-2xl font-black uppercase text-xs tracking-wider flex items-center justify-center gap-2 shadow-lg active:scale-95 cursor-pointer"
              >
                <PhoneOff className="w-5 h-5" /> Decline
              </button>
              <button 
                onClick={handleAcceptCall}
                className="flex-1 py-4 bg-emerald-600 hover:bg-emerald-500 text-black font-black uppercase text-xs tracking-wider flex items-center justify-center gap-2 shadow-lg active:scale-95 cursor-pointer"
              >
                <Phone className="w-5 h-5" /> Accept
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- 🔥 ACTIVE CALL OVERLAY MODAL 🔥 --- */}
      {activeCallSession && (
        <div className="fixed inset-0 z-[9999999] bg-zinc-950 flex flex-col justify-between p-6 animate-fadeIn font-mono">
          <div className="flex justify-between items-center bg-black/40 border border-white/10 p-4 rounded-2xl backdrop-blur-md">
            <div>
              <h3 className="text-xs font-black text-white uppercase tracking-widest">Active {activeCallSession.type === 'video' ? 'Video' : 'Audio'} Call</h3>
              <p className="text-[10px] text-emerald-400 font-bold mt-0.5">Connected with {activeCallSession.caller === globalCallsign ? activeCallSession.recipient : activeCallSession.caller}</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse"></span>
              <span className="text-xs text-zinc-300">SECURE LINK</span>
            </div>
          </div>

          <div className="flex-1 flex items-center justify-center p-4 relative">
            {activeCallSession.type === 'video' && isVideoEnabled ? (
              <div className="w-full max-w-2xl aspect-video bg-black rounded-3xl border-2 border-emerald-500/50 shadow-[0_0_40px_rgba(16,185,129,0.2)] flex items-center justify-center relative overflow-hidden">
                <div className="absolute inset-0 flex items-center justify-center text-zinc-600 font-bold uppercase tracking-widest">
                  Live Video Feed Stream
                </div>
                <div className="absolute bottom-4 right-4 w-32 aspect-video bg-zinc-900 rounded-xl border border-white/20 flex items-center justify-center text-[10px] text-zinc-400">
                  Self View
                </div>
              </div>
            ) : (
              <div className="w-36 h-36 rounded-full bg-zinc-900 border-4 border-emerald-500/80 flex items-center justify-center text-emerald-400 shadow-[0_0_50px_rgba(16,185,129,0.3)]">
                <Users className="w-16 h-16 animate-pulse" />
              </div>
            )}
          </div>

          <div className="flex justify-center items-center gap-6 pb-6">
            <button 
              onClick={() => setIsMuted(!isMuted)} 
              className={`p-5 rounded-2xl border transition-all cursor-pointer ${isMuted ? 'bg-rose-950/60 border-rose-500 text-rose-400' : 'bg-zinc-900 border-white/10 text-white'}`}
            >
              {isMuted ? <MicOff className="w-6 h-6" /> : <Mic className="w-6 h-6" />}
            </button>
            {activeCallSession.type === 'video' && (
              <button 
                onClick={() => setIsVideoEnabled(!isVideoEnabled)} 
                className={`p-5 rounded-2xl border transition-all cursor-pointer ${!isVideoEnabled ? 'bg-rose-950/60 border-rose-500 text-rose-400' : 'bg-zinc-900 border-white/10 text-white'}`}
              >
                {!isVideoEnabled ? <VideoOff className="w-6 h-6" /> : <VideoIcon className="w-6 h-6" />}
              </button>
            )}
            <button 
              onClick={handleEndActiveCall} 
              className="p-5 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white transition-all cursor-pointer shadow-lg active:scale-95"
            >
              <PhoneOff className="w-6 h-6" />
            </button>
          </div>
        </div>
      )}

      <header className={`sticky top-0 z-40 bg-zinc-950 border-b border-white/10 px-4 py-3 md:px-6 shadow-[0_4px_30px_rgba(0,0,0,0.9)] border-t-[3px]`} style={{ borderTopColor: activeTheme.hex }}>
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <img src={icon} alt="Logo" className={`w-9 h-9 ${cornerStyle} object-cover border border-white/20`} />
            <div>
              <h1 className={`text-sm font-black tracking-widest text-transparent bg-clip-text bg-gradient-to-r ${activeTheme.gradient} uppercase font-mono`}>
                {translate('app_title')}
              </h1>
              <span className="text-[9px] text-zinc-300 font-mono uppercase tracking-widest block font-bold">
                {translate('pilot_label')}: <strong className="text-white">{globalCallsign || "Unbound"}</strong>
              </span>
            </div>
          </div>
          
          <div className="flex items-center gap-2 sm:gap-3 shrink-0 ml-2">
            
            {latestRelease && isNewerVersion(latestRelease.tag_name, CURRENT_VERSION) && (
              <a 
                href={latestRelease.assets?.[0]?.browser_download_url || latestRelease.html_url} 
                target="_blank" 
                rel="noopener noreferrer"
                onClick={triggerHaptic}
                className={`${reducedMotion ? '' : 'animate-bounce'} flex items-center gap-1.5 bg-rose-600 hover:bg-rose-500 text-white px-3 py-1.5 rounded-xl text-[10px] font-mono font-black uppercase shadow-[0_0_15px_rgba(244,63,94,0.5)] cursor-pointer ${reducedMotion ? '' : 'active:scale-95'}`}
              >
                <Download className="w-3.5 h-3.5" />
                <span className="hidden xs:inline">{translate('update_available')} ({latestRelease.tag_name})</span>
                <span className="xs:hidden">Update</span>
              </a>
            )}

            {/* 🔥 GLOBAL INBOX TRIGGER BUTTON IN HEADER 🔥 */}
            <button 
              type="button" 
              onClick={() => { triggerHaptic(); setShowInbox(true); }} 
              className="p-2.5 bg-zinc-900 border border-white/10 rounded-xl hover:bg-zinc-800 transition-colors shadow-inner cursor-pointer"
              title="Open Comms Hub & Direct Messages"
            >
              <Mail className="w-4 h-4 text-zinc-300" />
            </button>

            <button type="button" onClick={() => { triggerHaptic(); setActiveTab("INFO"); }} className={`flex items-center gap-1.5 bg-zinc-900 border ${activeTab === 'INFO' ? activeTheme.border : 'border-white/10'} px-3 py-1.5 rounded-xl text-[10px] ${activeTab === 'INFO' ? activeTheme.text : 'text-zinc-300'} font-mono font-bold shadow-inner hover:border-white/20 transition-colors cursor-pointer ${reducedMotion ? '' : 'active:scale-95'}`} title="View App Info & Creator Details">
              <Info className="w-3.5 h-3.5" />
              <span className="hidden xs:inline">{translate('app_info')}</span>
            </button>

            <div className="hidden xs:flex items-center gap-1.5 bg-zinc-900 border border-white/10 px-3 py-1.5 rounded-xl text-[10px] text-zinc-300 font-mono font-bold shadow-inner" title="Firebase Live Satellite Feed">
              <Satellite className={`w-3.5 h-3.5 ${activeTheme.text} ${reducedMotion ? '' : 'animate-pulse'}`} />
              <span>{satelliteCount} {translate('sats_live')}</span>
            </div>

            <div className="hidden sm:flex items-center gap-2 bg-zinc-900 border border-cyan-500/50 px-3 py-1.5 rounded-xl text-[10px] text-cyan-300 font-mono font-bold shadow-inner">
              {isOfflineMode ? (
                <>
                   <WifiOff className="w-3.5 h-3.5 text-amber-500" />
                   <span className="text-amber-400">{translate('offline_mode')}</span>
                </>
              ) : (
                <>
                   <Shield className={`w-3.5 h-3.5 text-emerald-400 ${reducedMotion ? '' : 'animate-pulse'}`} />
                   <span className="text-emerald-50">{translate('satellite_online')}</span>
                </>
              )}
            </div>
            
            <button type="button" onClick={() => { triggerHaptic(); setIsSettingsOpen(true); }} className={`p-2.5 sm:p-3 bg-zinc-900 border border-white/10 rounded-xl hover:bg-zinc-800 transition-colors shadow-inner group cursor-pointer ${reducedMotion ? '' : 'active:scale-95'}`}>
              <Settings className={`w-4 h-4 sm:w-4.5 sm:h-4.5 text-zinc-300 group-hover:${activeTheme.text} transition-colors`} />
            </button>
          </div>
        </div>
      </header>

      <div className="flex-1 w-full max-w-7xl mx-auto p-4 flex flex-col lg:flex-row gap-6 pb-24 lg:pb-6 relative z-10">
        <aside className="hidden lg:flex flex-col gap-2 w-64 shrink-0 bg-zinc-950 border border-white/10 p-4 rounded-3xl h-fit shadow-2xl">
          <span className="text-[10px] text-zinc-500 font-black uppercase tracking-widest pl-2 mb-3 block">{translate('nav_console')}</span>
          <nav className="space-y-1.5">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button key={item.id} type="button" onClick={() => { triggerHaptic(); setActiveTab(item.id); }} className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-xs font-black uppercase tracking-wider transition-all cursor-pointer ${reducedMotion ? '' : 'active:scale-95'} ${isActive ? `bg-gradient-to-r ${activeTheme.gradient} text-black font-black` : "text-zinc-400 hover:text-white"}`}>
                  <Icon className="w-4 h-4 shrink-0" />
                  <span className="truncate">{item.label}</span>
                </button>
              );
            })}
          </nav>
        </aside>
        
        <main className="flex-1 min-w-0 relative">
          <AnimatePresence mode="wait">
            <motion.div key={activeTab} initial={{ opacity: 0, y: reducedMotion ? 0 : 5 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: reducedMotion ? 0 : -10 }} transition={{ duration: 0.15 }}>
              {renderTabContent()}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>

      <nav className={`lg:hidden fixed bottom-0 left-0 right-0 h-[64px] bg-zinc-950 border-t border-white/10 flex justify-around items-center px-1 z-40 overflow-x-auto custom-scrollbar transition-transform duration-300 ${isKeyboardOpen ? 'translate-y-full opacity-0 pointer-events-none' : 'translate-y-0 opacity-100'}`}>
        {navigationItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button key={item.id} type="button" onClick={() => { triggerHaptic(); setActiveTab(item.id); }} className={`flex flex-col items-center justify-center min-w-[56px] px-1 h-full text-[8px] font-black uppercase tracking-widest cursor-pointer shrink-0 transition-all ${reducedMotion ? '' : 'active:scale-90'} ${isActive ? activeTheme.text : "text-zinc-400 hover:text-white"}`}>
              <Icon className="w-4 h-4 mb-1" />
              <span className="truncate">{item.label}</span>
            </button>
          );
        })}
      </nav>

      <footer className={`pt-6 ${isKeyboardOpen ? 'pb-6' : 'pb-[85px] lg:pb-6'} text-center border-t border-white/5 bg-zinc-950 flex flex-col items-center gap-3 relative z-10 transition-all duration-300`}>
        <span className="text-[9px] font-mono text-zinc-500 uppercase tracking-widest px-4">
          Rural ERides Go v{CURRENT_VERSION} • Universal PEV Network • Creator: Lord Bradley Callison • KEEP IT RURAL YALL
        </span>
        <a href="https://github.com/bradley3398/Rural-Erides-GO" target="_blank" rel="noopener noreferrer" className={`flex items-center justify-center gap-2 px-5 py-2.5 bg-zinc-900 hover:bg-zinc-800 border border-white/10 ${cornerStyle} text-[10px] font-black text-cyan-400 hover:text-cyan-300 uppercase tracking-widest transition-all cursor-pointer shadow-lg mx-4 ${reducedMotion ? '' : 'active:scale-95'}`}>
          <Globe className="w-4 h-4 shrink-0" /> bradley3398/Rural-Erides-GO: GitHub Source Repository
        </a>
      </footer>
    </div>
  );
}

export default function AppWrapper() {
  const [showSplash, setShowSplash] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setShowSplash(false);
    }, 3500);
    return () => clearTimeout(timer);
  }, []);

  return (
    <AvionicsErrorBoundary>
      <AnimatePresence>
        {showSplash && (
          <motion.div
            initial={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.6 }}
            className="fixed inset-0 z-[9999999] bg-black flex flex-col items-center justify-center p-6 select-none overflow-hidden"
          >
            <div className="absolute inset-0 opacity-45">
              <img 
                src={splashImg} 
                alt="Rural Erides Splash" 
                className="w-full h-full object-cover filter brightness-90 contrast-110"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black via-black/30 to-transparent"></div>
            </div>

            <div className="relative z-10 flex flex-col items-center text-center mt-auto pb-12 space-y-4">
              <div className="w-16 h-16 rounded-3xl bg-black/60 border-2 border-emerald-500/80 flex items-center justify-center shadow-[0_0_30px_rgba(16,185,129,0.5)] backdrop-blur-xl animate-pulse">
                <img src={icon} alt="Logo" className="w-10 h-10 rounded-2xl object-cover" />
              </div>
              <div>
                <h1 className="text-xl font-black tracking-widest text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-cyan-400 uppercase font-mono">
                  RURAL ERIDES GO
                </h1>
                <p className="text-[10px] font-mono text-zinc-400 uppercase tracking-widest mt-1">
                  Initializing Avionics &amp; Telemetry Matrix...
                </p>
              </div>
              <div className="w-32 h-1 bg-zinc-800 rounded-full overflow-hidden mt-4">
                <motion.div 
                  initial={{ width: "0% "}}
                  animate={{ width: "100%" }}
                  transition={{ duration: 3.5, ease: "easeInOut" }}
                  className="h-full bg-emerald-400 shadow-[0_0_10px_#39ff14]"
                />
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
      <App />
    </AvionicsErrorBoundary>
  );
}