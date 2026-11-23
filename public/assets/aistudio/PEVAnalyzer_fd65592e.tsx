"use client";

import React, { useState, useEffect, useRef, useMemo, useCallback } from "react";
import icon from "../assets/icon.png";
import { locationService } from "../services/LocationService";

import { 
  Wrench, Globe, Search, Layers, LifeBuoy, Activity,
  Settings, Settings2, RefreshCw, Eye, BookOpen, ShieldAlert,
  MapPin, X, Play, Maximize2, Loader2, AlertTriangle, Cpu, HardDrive,
  ExternalLink
} from "lucide-react";

import { Capacitor } from "@capacitor/core";
import { Haptics } from "@capacitor/haptics";
import { TextToSpeech } from "@capacitor-community/text-to-speech";

import { getGeminiApiKey, getTavilyApiKey } from "../services/CoPilotService";

import MechanicChat from "./PEVAnalyzerWidgets/MechanicChat";
import SpecEngineTab from "./PEVAnalyzerWidgets/SpecEngineTab";
import PartsFinderTab from "./PEVAnalyzerWidgets/PartsFinderTab";
import EngineeringToolsTab from "./PEVAnalyzerWidgets/EngineeringToolsTab";
import MechanicSettingsModal from "./PEVAnalyzerWidgets/MechanicSettingsModal";

export type ThemeColor = "cyan" | "emerald" | "amber" | "rose" | "purple" | "void" | "lime" | "rural";

export interface PevSpecItem {
  name: string; brand: string; category: string; price: string; topSpeed: string;
  range: string; motorPower: string; battery: string; nominalVoltage?: number;
  weight: string; chargingTime: string; efficiency: string; siteUrl: string;
  brakingSystem: string; suspensionType: string; tireProfile: string;
  ipRating: string; maxLoad: string; videoReviewUrl?: string;
  youtubeVideos?: Array<{ title: string; url: string; type?: string }>;
  imageUrls: string[]; dimensionsFolded?: string; dimensionsUnfolded?: string;
  controllerAmps?: string; frameMaterial?: string; fastenerTorque?: string;
  suspensionTravel?: string; cellBrand?: string; controllerType?: string; chargePortType?: string;
  motorMagnetSize?: string; peakSystemWatts?: string; displayType?: string;
}

export interface PevPartItem {
  partName: string; category: string; compatibility: string; exactDimensions?: string;
  estimatedPrice: string; recommendedBrands: string; partUrl: string; partType: string;
  sourcePlatform: string; installationDifficulty: string; weightLbs: string; imageUrl?: string;
}

export interface ChatMessage {
  id: string; role: "user" | "ai" | "system"; text: string; timestamp?: string; image?: string | null; checklist?: Array<{ text: string; completed: boolean }>;
}

export interface DiagnosticSession { id: string; timestamp: string; title: string; messages: ChatMessage[]; }
export interface BudgetItem { id: string; name: string; cost: number; }
export interface ThemeConfig { text: string; bg: string; border: string; shadow: string; dim: string; hex: string; borderSubtle: string; }

class ErrorBoundary extends React.Component<{ children: React.ReactNode }, { hasError: boolean; error: Error | null }> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("[PEVAnalyzer Crash Handler]:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="p-4 bg-red-950/95 text-red-200 font-mono rounded-3xl m-3 border-2 border-red-500 shadow-2xl backdrop-blur-xl">
          <div className="flex items-center gap-2 text-red-400 font-black uppercase text-xs">
            <AlertTriangle className="w-4 h-4 animate-pulse" />
            <span>Terminal Intercepted Exception</span>
          </div>
          <p className="mt-2 text-[10px] font-bold text-white bg-black/60 p-3 rounded-xl border border-red-800/80 break-words">
            {this.state.error?.message || "Unknown Application Fault"}
          </p>
          <div className="mt-3">
            <button 
              type="button" 
              onClick={() => window.location.reload()} 
              className="bg-red-600 hover:bg-red-500 text-white font-black text-[10px] uppercase px-4 py-2 rounded-xl tracking-wider cursor-pointer shadow-lg transition-transform active:scale-95 min-h-[36px]"
            >
              Reboot
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

interface AnalyzerProps {
  theme?: string;
  useMetric?: boolean;
  locale?: string;
  timeFormat?: string;
  performanceMode?: boolean;
  uiScale?: string;
  globalVolume?: number;
  callsign?: string;
  glassBlurClass?: string;
  cornerStyle?: string;
  reducedMotion?: boolean;
}

export default function PEVAnalyzerContainer(props: AnalyzerProps) {
  return (
    <ErrorBoundary>
      <PEVAnalyzer {...props} />
    </ErrorBoundary>
  );
}

function PEVAnalyzer(props: AnalyzerProps) {
  const [mounted, setMounted] = useState<boolean>(false);
  const [activeSessionId, setActiveSessionId] = useState<string>("");
  const glassBlurClass = props.glassBlurClass || "backdrop-blur-2xl";
  const cornerStyle = props.cornerStyle || "rounded-3xl";

  const [activeTab, setActiveTab] = useState<"chat" | "database">("chat");
  const [dbSubTab, setDbSubTab] = useState<"specs" | "parts" | "tools">("parts");

  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [image, setImage] = useState<string | null>(null);
  const [mimeType, setMimeType] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState<string>("");
  const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [scanHistory, setScanHistory] = useState<DiagnosticSession[]>([]);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const [currentlyReadingId, setCurrentlyReadingId] = useState<string | null>(null);
  const [isListening, setIsListening] = useState<boolean>(false);
  const [ttsRate, setTtsRate] = useState<number>(1.0);
  const [ttsPitch, setTtsPitch] = useState<number>(1.0);
  const [autoReadAloud, setAutoReadAloud] = useState<boolean>(false);

  const [callsign, setCallsign] = useState<string>(() => props.callsign || (typeof window !== 'undefined' ? localStorage.getItem("pev_username") || localStorage.getItem("radar_screen_name") || "RIDER01" : "RIDER01"));
  const [userFleet, setUserFleet] = useState<string>(() => typeof window !== 'undefined' ? localStorage.getItem("pev_fleet") || "Universal Performance PEV Fleet" : "Universal Performance PEV Fleet");
  const [theme, setTheme] = useState<ThemeColor>(() => (props.theme as ThemeColor) || (typeof window !== 'undefined' ? (localStorage.getItem("pev_theme") as ThemeColor) || "cyan" : "cyan"));
  const [unitSystem, setUnitSystem] = useState<"imperial" | "metric">(() => typeof window !== 'undefined' ? (localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km" ? "metric" : "imperial") : "imperial");

  const triggerHaptic = async (style: any = "LIGHT") => {
    try {
      if (Capacitor.isPluginAvailable('Haptics')) await Haptics.impact({ style });
      else if (typeof navigator !== 'undefined' && 'vibrate' in navigator) navigator.vibrate(style === "HEAVY" ? 40 : 15);
    } catch (e) {}
  };

  useEffect(() => {
    const handleSettingsSync = () => {
      setTheme((localStorage.getItem("pev_theme") || localStorage.getItem("rt_theme") || props.theme || "cyan") as ThemeColor);
      setUnitSystem(localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km" ? "metric" : "imperial");
      setCallsign(props.callsign || localStorage.getItem("pev_username") || localStorage.getItem("radar_screen_name") || "RIDER01");
      setUserFleet(localStorage.getItem("pev_fleet") || "Universal Performance PEV Fleet");
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
  }, [props.theme, props.callsign]);

  const [nightWrenchMode, setNightWrenchMode] = useState<boolean>(false);
  const [showSettings, setShowSettings] = useState<boolean>(false);
  const [aiDetailLevel, setAiDetailLevel] = useState<string>("standard");
  const [aiModelVersion, setAiModelVersion] = useState<string>("gemini-3.1-flash-lite");
  const [safeSearchLevel, setSafeSearchLevel] = useState<string>("moderate");
  const [maxSpecResultsCount, setMaxSpecResultsCount] = useState<number>(100);
  const [autoClearPhoto, setAutoClearPhoto] = useState<boolean>(true);
  const [autoSaveLogs, setAutoSaveLogs] = useState<boolean>(true);
  const [searchScope, setSearchScope] = useState<string>("all");
  const [preferredMarketplace, setPreferredMarketplace] = useState<string>("aggregated");
  const [userRegion, setUserRegion] = useState<string>("US");

  const [dbSearch, setDbSearch] = useState<string>("");
  const [specMake, setSpecMake] = useState<string>("all");
  const [specModel, setSpecModel] = useState<string>("");
  const [specVehicleType, setSpecVehicleType] = useState<string>("all");
  
  const [livePevResults, setLivePevResults] = useState<PevSpecItem[]>([]);
  const [isSearchingDb, setIsSearchingDb] = useState<boolean>(false);
  const [hasSearchedDb, setHasSearchedDb] = useState<boolean>(false);
  const [expandedPevIdx, setExpandedPevIdx] = useState<number | null>(null);
  const [displayCountSpecs, setDisplayCountSpecs] = useState<number>(100);
  const [specCategoryFilter, setSpecCategoryFilter] = useState<string>("all");
  const [specSortBy, setSpecSortBy] = useState<string>("default");
  const [compareList, setCompareList] = useState<PevSpecItem[]>([]);
  
  const [activeVideoUrl, setActiveVideoUrl] = useState<string | null>(null);
  const [activeVideoTitle, setActiveVideoTitle] = useState<string | null>(null);
  const [activeImageIndices, setActiveImageIndices] = useState<Record<number, number>>({});
  const [lightboxState, setLightboxState] = useState<{ pevIdx: number; imgIdx: number } | null>(null);

  const [partsMake, setPartsMake] = useState<string>("all");
  const [partsModel, setPartsModel] = useState<string>("");
  const [partsTireSize, setPartsTireSize] = useState<string>("");
  const [partsCategory, setPartsCategory] = useState<string>("all");
  const [universalPartQuery, setUniversalPartQuery] = useState<string>("");
  const [livePartsResults, setLivePartsResults] = useState<PevPartItem[]>([]);
  const [isSearchingParts, setIsSearchingParts] = useState<boolean>(false);
  const [hasSearchedParts, setHasSearchedParts] = useState<boolean>(false);
  const [displayCountParts, setDisplayCountParts] = useState<number>(100);
  const [partsDifficultyFilter, setPartsDifficultyFilter] = useState<string>("all");
  const [buildManifest, setBuildManifest] = useState<PevPartItem[]>([]);

  // Telemetry Passes
  const [calcVoltage, setCalcVoltage] = useState<number>(52);
  const [nominalVoltage, setNominalVoltage] = useState<number>(52);
  const [calcAh, setCalcAh] = useState<number>(20);
  const [calcWhPerMile, setCalcWhPerMile] = useState<number>(25);
  const [riderWeight, setRiderWeight] = useState<number>(180);
  const [vehicleWeight, setVehicleWeight] = useState<number>(65);
  const [oldTireSize, setOldTireSize] = useState<number>(10);
  const [newTireSize, setNewTireSize] = useState<number>(11);
  const [currentSpeed, setCurrentSpeed] = useState<number>(30);
  const [stockAmps, setStockAmps] = useState<number>(25);
  const [shuntSolderPercent, setShuntSolderPercent] = useState<number>(20);
  const [selectedBoltSize, setSelectedBoltSize] = useState<string>("M6");
  const [customWireAmps, setCustomWireAmps] = useState<number>(30);
  const [selectedErrorCode, setSelectedErrorCode] = useState<string>("E06");
  const [factoryPeakVolts, setFactoryPeakVolts] = useState<number>(54.6);
  const [measuredLoadVolts, setMeasuredLoadVolts] = useState<number>(48.0);
  const [testLoadAmps, setTestLoadAmps] = useState<number>(20.0);
  const [selectedSymptom, setSelectedSymptom] = useState<string>("motor_stutter");
  const [kwhRate, setKwhRate] = useState<number>(0.14);
  const [chargerAmps, setChargerAmps] = useState<number>(3);
  const [hillGradePercent, setHillGradePercent] = useState<number>(15);
  const [targetClimbSpeed, setTargetClimbSpeed] = useState<number>(15);
  const [ambientTemp, setAmbientTemp] = useState<number>(45);
  const [ambientTempF, setAmbientTempF] = useState<number>(45);
  const [brakeFluidType, setBrakeFluidType] = useState<string>("Mineral Oil");
  const [budgetItems, setBudgetItems] = useState<BudgetItem[]>([
    { id: "1", name: "Hydraulic Brake Kit", cost: 120 }
  ]);
  const [newItemName, setNewItemName] = useState("");
  const [newItemCost, setNewItemCost] = useState("");

  const galleryInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMounted(true);
    const sid = `SESSION-${Date.now().toString(36).toUpperCase()}`;
    setActiveSessionId(sid);

    try {
      const savedLogs = localStorage.getItem("pev_chat_logs");
      if (savedLogs) setScanHistory(JSON.parse(savedLogs));
      const savedConfig = localStorage.getItem("PEV_CONFIG_STORAGE");
      if (savedConfig) {
        const parsed = JSON.parse(savedConfig);
        if (parsed.callsign) setCallsign(parsed.callsign);
        if (parsed.userFleet) setUserFleet(parsed.userFleet);
        if (parsed.theme) setTheme(parsed.theme);
      }
    } catch (e) { console.warn("Storage load exception:", e); }
  }, []);

  useEffect(() => {
    if (!mounted || messages.length === 0 || !autoSaveLogs) return;

    const currentSession: DiagnosticSession = {
      id: activeSessionId,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' }),
      title: messages[0]?.text?.substring(0, 36) + "..." || "Diagnostic Session",
      messages
    };

    setScanHistory(prevHistory => {
      const existingIdx = prevHistory.findIndex(s => s.id === activeSessionId);
      let updatedHistory;
      if (existingIdx >= 0) {
        updatedHistory = [...prevHistory];
        updatedHistory[existingIdx] = currentSession;
      } else {
        updatedHistory = [currentSession, ...prevHistory].slice(0, 30);
      }
      try {
        localStorage.setItem("pev_chat_logs", JSON.stringify(updatedHistory));
      } catch (e) {
        console.warn("Log persistence error:", e);
      }
      return updatedHistory;
    });
  }, [messages, activeSessionId, mounted, autoSaveLogs]);

  const saveConfiguration = () => {
    triggerHaptic("MEDIUM");
    setShowSettings(false);
  };

  const t: ThemeConfig = useMemo(() => {
    if (nightWrenchMode) {
      return {
        text: "text-amber-500",
        bg: "bg-amber-600",
        border: "border-amber-700",
        shadow: "shadow-[0_0_20px_rgba(245,158,11,0.5)]",
        dim: "bg-amber-950/40 text-amber-500 border-amber-900",
        hex: "#d97706",
        borderSubtle: "border-amber-900/40"
      };
    }
    const themes: Record<ThemeColor, ThemeConfig> = {
      cyan: { text: "text-cyan-400", bg: "bg-cyan-500", border: "border-cyan-500", shadow: "shadow-[0_0_15px_rgba(6,182,212,0.3)]", dim: "bg-cyan-950/30 text-cyan-400 border-cyan-900/50", hex: "#06b6d4", borderSubtle: "border-cyan-900/40" },
      emerald: { text: "text-emerald-400", bg: "bg-emerald-500", border: "border-emerald-500", shadow: "shadow-[0_0_15px_rgba(16,185,129,0.3)]", dim: "bg-emerald-950/30 text-emerald-400 border-emerald-900/50", hex: "#10b981", borderSubtle: "border-emerald-900/40" },
      amber: { text: "text-amber-400", bg: "bg-amber-500", border: "border-amber-500", shadow: "shadow-[0_0_15px_rgba(245,158,11,0.3)]", dim: "bg-amber-950/30 text-amber-400 border-amber-900/50", hex: "#f59e0b", borderSubtle: "border-amber-900/40" },
      rose: { text: "text-rose-400", bg: "bg-rose-500", border: "border-rose-500", shadow: "shadow-[0_0_15px_rgba(244,63,94,0.3)]", dim: "bg-rose-950/30 text-rose-400 border-rose-900/50", hex: "#f43f5e", borderSubtle: "border-rose-900/40" },
      purple: { text: "text-purple-400", bg: "bg-purple-500", border: "border-purple-500", shadow: "shadow-[0_0_20px_rgba(168,85,247,0.3)]", dim: "bg-purple-950/30 text-purple-400 border-purple-900/50", hex: "#a855f7", borderSubtle: "border-purple-900/40" },
      void: { text: "text-white", bg: "bg-zinc-800", border: "border-zinc-500", shadow: "shadow-[0_0_20px_rgba(255,255,255,0.1)]", dim: "bg-zinc-900/50 text-white border-zinc-700/50", hex: "#ffffff", borderSubtle: "border-zinc-700/40" },
      lime: { text: "text-[#39ff14]", bg: "bg-[#39ff14]", border: "border-[#39ff14]", shadow: "shadow-[0_0_15px_rgba(57,255,20,0.3)]", dim: "bg-[#39ff14]/10 text-[#39ff14] border-[#39ff14]/50", hex: "#39ff14", borderSubtle: "border-[#39ff14]/30" },
      rural: { text: "text-[#39ff14]", bg: "bg-[#39ff14]", border: "border-[#39ff14]", shadow: "shadow-[0_0_15px_rgba(57,255,20,0.3)]", dim: "bg-[#39ff14]/10 text-[#39ff14] border-[#39ff14]/50", hex: "#39ff14", borderSubtle: "border-[#39ff14]/30" }
    };
    return themes[theme] || themes.cyan;
  }, [theme, nightWrenchMode]);

  const getDynamicTorqueSpec = useCallback((bolt: string) => {
    const specs: Record<string, { nm: string; ftlbs: string; usage: string }> = {
      M4: { nm: "2.0 - 4.0 Nm", ftlbs: "1.5 - 3.0 ft-lbs", usage: "Display mounts, brake lever clamps" },
      M5: { nm: "4.5 - 6.0 Nm", ftlbs: "3.3 - 4.4 ft-lbs", usage: "Brake rotor Torx bolts, fender stays" },
      M6: { nm: "9.0 - 12.0 Nm", ftlbs: "6.6 - 8.8 ft-lbs", usage: "Caliper brackets, stem pinch bolts" },
      M8: { nm: "20.0 - 25.0 Nm", ftlbs: "14.8 - 18.4 ft-lbs", usage: "Suspension pivots, folding mechs" },
      M10: { nm: "35.0 - 45.0 Nm", ftlbs: "25.8 - 33.2 ft-lbs", usage: "Suspension eyelets, main frames" },
      M12: { nm: "65.0 - 85.0 Nm", ftlbs: "48.0 - 62.7 ft-lbs", usage: "Solid axle hub motor nuts" },
      M14: { nm: "85.0 - 110.0 Nm", ftlbs: "62.7 - 81.1 ft-lbs", usage: "High-power motor axles" }
    };
    return specs[bolt] || { nm: "8.0 - 10.0 Nm", ftlbs: "5.9 - 7.4 ft-lbs", usage: "Standard PEV fastener" };
  }, []);

  const getRecommendedAWG = useCallback((amps: number) => {
    if (amps <= 15) return { gauge: "16 AWG", temp: "Cool (<35°C)", connector: "XT30" };
    if (amps <= 30) return { gauge: "14 AWG", temp: "Nominal (40°C)", connector: "XT60" };
    if (amps <= 50) return { gauge: "12 AWG", temp: "Moderate (45°C)", connector: "XT60H" };
    if (amps <= 80) return { gauge: "10 AWG", temp: "Warm (55°C)", connector: "XT90 / QS8" };
    if (amps <= 120) return { gauge: "8 AWG", temp: "Hot (65°C)", connector: "QS8" };
    return { gauge: "6-7 AWG", temp: "Extreme Load", connector: "AS150" };
  }, []);

  const calculateBatteryHealth = useCallback(() => {
    const vDrop = Math.max(0.01, factoryPeakVolts - measuredLoadVolts);
    const irCalculated = ((vDrop / Math.max(1, testLoadAmps)) * 1000).toFixed(1);
    const irNum = parseFloat(irCalculated);
    let healthPercent = Math.max(10, Math.min(100, Math.round(100 - (irNum / 45) * 50)));
    let status = "Healthy / Low IR";
    if (irNum > 35) status = "Moderate Degradation";
    if (irNum > 60) status = "High Cell Sag / End of Life";
    return { ir: irCalculated, health: healthPercent, status };
  }, [factoryPeakVolts, measuredLoadVolts, testLoadAmps]);

  const getErrorCodeDetails = useCallback((code: string) => {
    const clean = code.toUpperCase().trim();
    const codes: Record<string, { fault: string; component: string; fix: string }> = {
      E06: { fault: "Low Battery Undervoltage Protection", component: "BMS / Battery", fix: "Charge pack. Verify individual cell voltage balance." },
      E07: { fault: "Motor Phase Line Fault", component: "Hub Motor", fix: "Inspect 3-phase bullets for melted connectors or shorts." },
      E08: { fault: "Throttle Hall Failure", component: "Throttle Unit", fix: "Verify 5V input, GND, and 0.8-4.2V signal lead." },
      E09: { fault: "Controller Gate Driver Malfunction", component: "Main Controller", fix: "Check MOSFETs for shorts between phase lines and GND." },
      E10: { fault: "UART/Display Communication Dropout", component: "Harness/Display", fix: "Check RX/TX wiring in stem harness for pinches." }
    };
    return codes[clean] || { fault: "Manufacturer Specific Code", component: "System Controller", fix: "Refer to live web diagnostic lookup." };
  }, []);

  const getSymptomTriageGuide = useCallback((symptom: string) => {
    const triages: Record<string, { title: string; steps: string[] }> = {
      motor_stutter: { title: "Motor Cogging / Shudder Under Load", steps: ["1. Disconnect motor. Spin wheel by hand.", "2. Check phase wire connectors.", "3. Test 5V hall sensors."] }
    };
    return triages[symptom] || { title: "General Telemetry Triage", steps: ["1. Verify full battery charge.", "2. Check all couplers.", "3. Inspect controller FETs."] };
  }, []);

  const getBrakeFluidInfo = useCallback((fluid: string) => {
    if (fluid.includes("DOT 5.1")) return { boiling: "260°C (500°F)", compatible: "SRAM, Avid", warning: "Corrosive to paint." };
    if (fluid.includes("DOT 4")) return { boiling: "230°C (446°F)", compatible: "SRAM, Formula", warning: "Hydrophilic. Absorb moisture." };
    return { boiling: "200°C - 230°C", compatible: "Shimano, Magura, Nutt", warning: "Non-corrosive mineral base." };
  }, []);

  const extractJSON = (text: string): string => {
    try {
      const match = text.match(/\[[\s\S]*\]/);
      if (match) return match[0];
      const singleMatch = text.match(/\{[\s\S]*\}/);
      if (singleMatch) return `[${singleMatch[0]}]`;
    } catch (e) {}
    return "[]";
  };

  const copyToClipboard = async (text: string, id?: string) => {
    try {
      await navigator.clipboard.writeText(text);
      if (id) { setCopiedId(id); setTimeout(() => setCopiedId(null), 2000); }
    } catch (err) {}
  };

  const handleTTS = async (text: string, id: string) => {
    const activeMasterVol = Number(localStorage.getItem("pev_global_vol") ?? props.globalVolume ?? 100);
    if (currentlyReadingId === id) {
      if (Capacitor.isNativePlatform()) await TextToSpeech.stop();
      else if (typeof window !== "undefined" && "speechSynthesis" in window) window.speechSynthesis.cancel();
      setCurrentlyReadingId(null);
      return;
    }
    setCurrentlyReadingId(id);
    try {
      if (Capacitor.isNativePlatform()) {
        await TextToSpeech.speak({ text, rate: ttsRate, pitch: ttsPitch, volume: activeMasterVol / 100, lang: "en-US" });
        setCurrentlyReadingId(null);
      } else if (typeof window !== "undefined" && "speechSynthesis" in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = ttsRate; utterance.pitch = ttsPitch; utterance.volume = activeMasterVol / 100;
        utterance.onend = () => setCurrentlyReadingId(null);
        window.speechSynthesis.speak(utterance);
      }
    } catch (e) { setCurrentlyReadingId(null); }
  };

  const toggleListening = () => {
    if (!("webkitSpeechRecognition" in window || "SpeechRecognition" in window)) return alert("Speech recognition is not supported in this browser.");
    triggerHaptic("LIGHT"); setIsListening(prev => !prev);
  };

  const clearImage = () => {
    setPreviewUrl(null); setImage(null); setMimeType(null);
    if (galleryInputRef.current) galleryInputRef.current.value = "";
    if (cameraInputRef.current) cameraInputRef.current.value = "";
  };

  const startNewSession = () => {
    triggerHaptic("LIGHT"); clearImage(); setMessages([]); setQuestion("");
    setActiveSessionId(`SESSION-${Date.now().toString(36).toUpperCase()}`);
  };

  const loadPreviousSession = (session: DiagnosticSession) => {
    triggerHaptic("LIGHT"); clearImage(); setQuestion(""); setActiveSessionId(session.id);
    setMessages(session.messages || []); setActiveTab("chat");
  };

  const clearAllHistory = () => {
    if (window.confirm("Are you sure you want to clear all diagnostic logs?")) {
      triggerHaptic("HEAVY"); setScanHistory([]); localStorage.removeItem("pev_chat_logs"); startNewSession();
    }
  };

  const exportToCSV = (type: string) => {
    triggerHaptic("LIGHT");
    let csvContent = "data:text/csv;charset=utf-8,";
    if (type === "specs") {
      csvContent += "Brand,Name,Category,Price,Top Speed,Range,Battery,Motor Power,Tires,Charge Port,Controller,Suspension,Cell Brand\n";
      livePevResults.forEach(p => { csvContent += `"${p.brand}","${p.name}","${p.category}","${p.price}","${p.topSpeed}","${p.range}","${p.battery}","${p.motorPower}","${p.tireProfile}","${p.chargePortType || ''}","${p.controllerType || ''}","${p.suspensionTravel || ''}","${p.cellBrand || ''}"\n`; });
    } else {
      csvContent += "Part Name,Category,Exact Dimensions,Compatibility,Price,Brand/Type,Platform\n";
      livePartsResults.forEach(p => { csvContent += `"${p.partName}","${p.category}","${p.exactDimensions || ''}","${p.compatibility}","${p.estimatedPrice}","${p.recommendedBrands}","${p.sourcePlatform}"\n`; });
    }
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a"); link.setAttribute("href", encodedUri); link.setAttribute("download", `PEV_${type.toUpperCase()}_EXPORT.csv`);
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
  };

  const getRegionalDomainSuffix = () => ({
    amazon: userRegion === "UK" ? "amazon.co.uk" : userRegion === "EU" ? "amazon.de" : "amazon.com",
    ebay: userRegion === "UK" ? "ebay.co.uk" : userRegion === "EU" ? "ebay.de" : "ebay.com",
    currency: userRegion === "UK" ? "GBP (£)" : userRegion === "EU" ? "EUR (€)" : "USD ($)"
  });

  const parseDynamicChecklist = (text: string): Array<{ text: string; completed: boolean }> => {
    const lines = text.split("\n"); const checklist: Array<{ text: string; completed: boolean }> = [];
    lines.forEach(line => {
      const trimmed = line.trim();
      if (trimmed.startsWith("- [ ]") || trimmed.startsWith("* [ ]") || trimmed.startsWith("- []")) {
        const itemText = trimmed.replace(/^([-*]\s*\[\s*\]\s*)/, "").trim();
        if (itemText) checklist.push({ text: itemText, completed: false });
      } else if (/^\d+\.\s+\[\s*\]/.test(trimmed)) {
        const itemText = trimmed.replace(/^\d+\.\s+\[\s*\]\s*/, "").trim();
        if (itemText) checklist.push({ text: itemText, completed: false });
      } else if (/^(Step\s+\d+:|Check\s+\d+:|\d+\.)\s+/i.test(trimmed) && trimmed.length < 120) {
        const itemText = trimmed.replace(/^(Step\s+\d+:|Check\s+\d+:|\d+\.)\s+/i, "").trim();
        if (itemText && !itemText.endsWith(":") && itemText.length > 10) checklist.push({ text: itemText, completed: false });
      }
    });
    return checklist.slice(0, 8);
  };

  const analyzeImage = async (overridePrompt?: string) => {
    const activeQuery = (overridePrompt || question).trim();
    if (!activeQuery && !previewUrl) return setErrorMsg("Please enter a diagnostic question or select a quick preset.");
    triggerHaptic("HEAVY"); setIsAnalyzing(true); setErrorMsg(null); setQuestion("");

    const timestampStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userMsg: ChatMessage = { id: `USER-${Date.now()}`, role: "user", text: activeQuery || "Optical Hardware Inspection Request", timestamp: timestampStr, image: previewUrl };
    const updatedMessages = [...messages, userMsg]; setMessages(updatedMessages);

    try {
      const geminiKey = getGeminiApiKey();
      const systemInstruction = `You are a seasoned, veteran workshop chief and PEV master mechanic with decades of hands-on experience in high-power micro-mobility, e-bikes, and PEV powertrains. Talk straight, respect the hardware, and speak like an experienced workshop chief who knows his way around multimeters, soldering irons, phase wires, and battery BMS systems assisting operator ${callsign}.
Active Fleet: ${userFleet}. Regional Units: ${unitSystem}. Diagnostic Detail Mode: ${aiDetailLevel}.
CRITICAL OUTPUT INSTRUCTIONS:
1. Deliver verified technical diagnostics, testing voltages, wire color codes, and torque specs in a grounded, seasoned workshop mechanic tone.
2. YOU MUST ALWAYS INCLUDE AN ACTIONABLE STEP-BY-STEP TROUBLESHOOTING CHECKLIST using markdown checkbox format:
- [ ] Step 1: Specific test or measurement
- [ ] Step 2: Specific inspection or fix
- [ ] Step 3: Verification step`;

      const contentsPayload: any[] = updatedMessages.slice(-8).map(m => ({ role: m.role === "ai" ? "model" : "user", parts: [{ text: m.text }] }));
      if (image && mimeType) contentsPayload[contentsPayload.length - 1].parts.push({ inlineData: { mimeType, data: image } });

      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${aiModelVersion}:generateContent?key=${geminiKey}`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ system_instruction: { parts: [{ text: systemInstruction }] }, contents: contentsPayload, generationConfig: { temperature: 0.3 } })
      });

      const data = await response.json();
      if (!response.ok) throw new Error(data.error?.message || `AI Terminal Error (${response.status})`);
      const rawAiText = data?.candidates?.[0]?.content?.parts?.[0]?.text || "Diagnostics payload received.";
      const aiMsg: ChatMessage = { id: `AI-${Date.now()}`, role: "ai", text: rawAiText, timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }), checklist: parseDynamicChecklist(rawAiText) };
      setMessages([...updatedMessages, aiMsg]);
      if (autoReadAloud) handleTTS(rawAiText, aiMsg.id);
    } catch (err: any) { setErrorMsg(`AI Diagnostic Error: ${err.message || String(err)}`); } finally { setIsAnalyzing(false); if (autoClearPhoto) clearImage(); }
  };

  const searchLiveSpecs = async (customQuery?: string) => {
    const makeQuery = specMake !== "all" ? specMake : "";
    const modelQuery = specModel.trim();
    const typeQuery = specVehicleType !== "all" ? specVehicleType : "";
    const searchQuery = customQuery || dbSearch || `${makeQuery} ${modelQuery} ${typeQuery} electric scooter ebike`.trim();
    if (!searchQuery) return;
    
    triggerHaptic("LIGHT"); setIsSearchingDb(true); setHasSearchedDb(true); setErrorMsg(null);

    try {
      const tavilyKey = getTavilyApiKey();
      const geminiKey = getGeminiApiKey();
      const tavilyQuery = `${searchQuery} official manufacturer technical specifications battery volts range motor watts top speed dimensions review teardown video`;
      const tavilyResponse = await fetch("https://api.tavily.com/search", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ api_key: tavilyKey, query: tavilyQuery, search_depth: "advanced", max_results: 100 })
      });
      const tavilyData = await tavilyResponse.json();
      const webContext = JSON.stringify(tavilyData.results || []);

      const prompt = `Extract up to 100 accurate Personal Electric Vehicle (PEV) specifications for: "${searchQuery}". EXCLUDE AUTOMOBILES.
      Web Data: ${webContext}
      Return ONLY a clean JSON array matching:
      [
        {
          "name": "Model Name", "brand": "Brand", "category": "E-Scooter / E-Bike / EUC", "price": "$999", "topSpeed": "35 MPH",
          "range": "40 Miles", "motorPower": "1200W Nominal (2500W Peak)", "battery": "52V 20Ah", "nominalVoltage": 52,
          "weight": "65 lbs", "chargingTime": "6 Hours", "efficiency": "25 Wh/mi", "siteUrl": "https://...",
          "brakingSystem": "Dual Hydraulic Disc (160mm)", "suspensionType": "Dual Spring Hydraulic", "tireProfile": "10x3.0 Pneumatic",
          "ipRating": "IPX5", "maxLoad": "330 lbs", "controllerAmperage": "Dual 30A Sine-Wave", "frameMaterial": "6061-T6 Aluminum",
          "fastenerTorque": "Axle: 85 Nm | Stem: 10 Nm", "videoReviewUrl": "https://...",
          "suspensionTravel": "100mm Coil", "cellBrand": "Samsung 50E", "controllerType": "Sine-Wave / FOC", "chargePortType": "LP-20 3-Pin",
          "motorMagnetSize": "50H", "peakSystemWatts": "3500W", "displayType": "TFT Display",
          "youtubeVideos": [ { "title": "Review & Dyno", "url": "https://...", "type": "review" } ]
        }
      ]`;

      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${aiModelVersion}:generateContent?key=${geminiKey}`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contents: [{ role: "user", parts: [{ text: prompt }] }], generationConfig: { responseMimeType: "application/json" } })
      });
      const data = await res.json();
      const parsed = JSON.parse(extractJSON(data?.candidates?.[0]?.content?.parts?.[0]?.text || "[]"));

      const formatted: PevSpecItem[] = parsed.map((p: any) => {
        const baseQuery = `${p.brand || ""} ${p.name || ""}`;
        const photoAngles = [
          "official product profile white background isolated", "front view cockpit handlebar display screen",
          "rear motor suspension close up", "hydraulic disc brake caliper rotor", "deck standing platform grip tape",
          "folded compact transport view", "front headlight suspension fork", "rear tire motor hub close up"
        ];
        const fullImages = photoAngles.map(angle => `https://tse1.mm.bing.net/th?q=${encodeURIComponent(baseQuery + " " + angle)}&w=600&h=600&c=7&rs=1&p=0`);
        return {
          ...p, name: p.name || p.nom || "Universal PEV", brand: p.brand || "OEM", category: p.category || "E-Mobility",
          price: p.price || "N/A", topSpeed: p.topSpeed || "Unknown", range: p.range || "Unknown", motorPower: p.motorPower || "Standard",
          battery: p.battery || "Universal", nominalVoltage: p.nominalVoltage || 52, weight: p.weight || "N/A", chargingTime: p.chargingTime || "N/A",
          efficiency: p.efficiency || "25 Wh/mi", siteUrl: p.siteUrl || "https://google.com", brakingSystem: p.brakingSystem || "Mechanical Disc",
          suspensionType: p.suspensionType || "Rigid", tireProfile: p.tireProfile || "Standard", ipRating: p.ipRating || "IP54", maxLoad: p.maxLoad || "265 lbs",
          suspensionTravel: p.suspensionTravel || "Standard", cellBrand: p.cellBrand || "Generic Li-ion", controllerType: p.controllerType || "Square Wave",
          chargePortType: p.chargePortType || "DC Standard", motorMagnetSize: p.motorMagnetSize || "Standard", peakSystemWatts: p.peakSystemWatts || "Unknown",
          displayType: p.displayType || "Standard LCD", videoReviewUrl: p.videoReviewUrl || "", youtubeVideos: p.youtubeVideos || [], imageUrls: fullImages
        };
      });
      setLivePevResults(formatted);
    } catch (e: any) { setErrorMsg(`Spec Query Interrupted: ${e.message || e}`); setLivePevResults([]); } finally { setIsSearchingDb(false); }
  };

  const searchLiveParts = async () => {
    triggerHaptic("LIGHT"); setIsSearchingParts(true); setHasSearchedParts(true); setErrorMsg(null);
    try {
      const tavilyKey = getTavilyApiKey(); const geminiKey = getGeminiApiKey();
      const searchTerms = [partsTireSize.trim() ? `"${partsTireSize.trim()}"` : "", universalPartQuery.trim(), partsMake !== "all" ? partsMake : "", partsModel, partsCategory !== "all" ? partsCategory : "", "electric scooter ebike replacement part tire tube brake controller"].filter(Boolean).join(" ");
      const tavilyQuery = `${searchTerms} (site:amazon.com OR site:ebay.com OR site:aliexpress.com OR site:walmart.com OR store buy online) -automotive -car -truck`;
      const tavilyResponse = await fetch("https://api.tavily.com/search", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ api_key: tavilyKey, query: tavilyQuery, search_depth: "advanced", max_results: 100 }) });
      const tavilyData = await tavilyResponse.json();
      const prompt = `You are a precision PEV replacement parts parser. Web Context Data: ${JSON.stringify(tavilyData.results || [])}
      Return a valid JSON array up to 100 items matching:
      [ { "partName": "CST 16x4.0 Tire", "category": "Tires", "exactDimensions": "16x4.0", "compatibility": "Universal Fat Tire", "estimatedPrice": "$42.99", "recommendedBrands": "CST", "partUrl": "https://amazon.com/...", "partType": "OEM Stock", "sourcePlatform": "Amazon", "installationDifficulty": "Beginner", "weightLbs": "2.8" } ]`;

      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${aiModelVersion}:generateContent?key=${geminiKey}`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ contents: [{ role: "user", parts: [{ text: prompt }] }], generationConfig: { responseMimeType: "application/json" } })
      });
      const data = await res.json();
      const parsed = JSON.parse(extractJSON(data?.candidates?.[0]?.content?.parts?.[0]?.text || "[]"));
      const formatted: PevPartItem[] = parsed.map((p: any) => ({
        partName: p.partName || p.nom || "Universal PEV Component", category: p.category || "Hardware", exactDimensions: p.exactDimensions || partsTireSize || "Universal", compatibility: p.compatibility || "Universal PEV", estimatedPrice: p.estimatedPrice || "$0.00", recommendedBrands: p.recommendedBrands || "OEM / Aftermarket", partUrl: p.partUrl || "https://amazon.com", partType: p.partType || "Stock", sourcePlatform: p.sourcePlatform || "Multi-Vendor", installationDifficulty: p.installationDifficulty || "Beginner", weightLbs: p.weightLbs || "1.5", imageUrl: `https://tse1.mm.bing.net/th?q=${encodeURIComponent((p.recommendedBrands || "") + " " + (p.partName || "") + " replacement part photo")}&w=300&h=300&c=7&rs=1&p=0`
      }));
      setLivePartsResults(formatted);
    } catch (e: any) { setErrorMsg(`Parts Query Interrupted: ${e.message || e}`); setLivePartsResults([]); } finally { setIsSearchingParts(false); }
  };

  const addToManifest = (part: PevPartItem) => { triggerHaptic("LIGHT"); setBuildManifest(prev => [...prev, part]); };
  const clearManifest = () => { triggerHaptic("LIGHT"); setBuildManifest([]); };
  const copyManifest = () => { triggerHaptic("LIGHT"); copyToClipboard(buildManifest.map(p => `${p.partName} (${p.estimatedPrice}) | Platform: ${p.sourcePlatform}`).join("\n")); };
  const toggleCompare = (pev: PevSpecItem) => { triggerHaptic("LIGHT"); if (compareList.some(p => p.name === pev.name)) setCompareList(compareList.filter(p => p.name !== pev.name)); else if (compareList.length < 4) setCompareList([...compareList, pev]); };

  const filteredAndSortedSpecs = useMemo(() => { return (livePevResults || []).filter(pev => specCategoryFilter === "all" || (pev.category || "").toLowerCase().includes(specCategoryFilter.toLowerCase())); }, [livePevResults, specCategoryFilter]);
  const filteredParts = useMemo(() => { return (livePartsResults || []).filter(part => partsDifficultyFilter === "all" || (part.installationDifficulty || "").toLowerCase().includes(partsDifficultyFilter.toLowerCase())); }, [livePartsResults, partsDifficultyFilter]);

  if (!mounted) return <div className="p-8 text-center text-zinc-500 font-mono flex flex-col items-center justify-center min-h-[50vh]"><Loader2 className={`w-8 h-8 animate-spin mb-4 ${t.text}`} />Initializing Human Mechanic Matrix...</div>;

  return (
    <div className={`space-y-4 p-2 sm:p-4 max-w-7xl mx-auto font-sans text-zinc-100 relative z-10 transition-colors duration-300 ${nightWrenchMode ? "bg-[#100700]" : ""}`}>
      
      {/* GLOBAL HUD HEADER */}
      <header className={`flex flex-col ${nightWrenchMode ? "bg-[#200d00] border-amber-800/80" : `app-card ${glassBlurClass} ${cornerStyle} border-white/10`} p-3 sm:p-4 border shadow-2xl gap-2.5 sm:gap-3`}>
        <div className="flex justify-between items-center flex-wrap gap-2">
          <div className="flex items-center gap-2 sm:gap-3">
            <div className={`${t.bg} p-2 sm:p-2.5 rounded-xl sm:rounded-2xl flex items-center justify-center shrink-0 shadow-lg`}><Wrench className="w-4 h-4 sm:w-5 sm:h-5 text-black" /></div>
            <div>
              <h1 className={`${t.text} font-black text-[11px] sm:text-sm uppercase flex items-center gap-1.5 sm:gap-2 tracking-wider font-mono`}>
                PEV MECHANIC <span className={`px-1.5 py-0.5 rounded-full ${t.dim} text-[7px] sm:text-[8px] font-mono border`}>LABS PRO</span>
              </h1>
              <div className="flex items-center gap-1.5 sm:gap-2 text-[8px] sm:text-[9px] text-zinc-400 font-mono mt-0.5 flex-wrap">
                <span>OPERATOR: <strong className="text-white">{callsign}</strong></span><span>•</span><span>FLEET: <strong className="text-white">{userFleet}</strong></span><span>•</span>
                <span className="text-emerald-400 flex items-center gap-1"><span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span> ONLINE</span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-1.5">
            <button type="button" onClick={() => { triggerHaptic("LIGHT"); setNightWrenchMode(!nightWrenchMode); }} className={`px-2.5 sm:px-3 min-h-[36px] rounded-lg text-[8px] sm:text-[9px] font-black uppercase border transition-all cursor-pointer ${nightWrenchMode ? "bg-amber-500 text-black border-amber-400" : "bg-white/5 border-white/10 text-amber-400 hover:bg-amber-400/10"}`}>Night Wrench</button>
            <button type="button" onClick={() => { triggerHaptic("LIGHT"); setShowSettings(true); }} className={`px-2.5 sm:px-3 min-h-[36px] rounded-lg text-[8px] sm:text-[9px] font-black uppercase border bg-white/5 border-white/10 ${t.text} hover:bg-white/10 transition-all cursor-pointer`}>Matrix Config</button>
          </div>
        </div>

        {/* PRIMARY ROUTING SELECTORS */}
        <div className="flex items-center gap-1.5 pt-1.5 sm:pt-2 border-t border-white/10 flex-wrap">
          <button type="button" onClick={() => { triggerHaptic("LIGHT"); setActiveTab("chat"); }} className={`flex-1 sm:flex-none px-3 min-h-[36px] rounded-lg text-[8px] sm:text-[9px] font-black uppercase tracking-wider cursor-pointer transition-all active:scale-95 flex items-center justify-center gap-1.5 ${activeTab === "chat" ? `${t.bg} text-black shadow-md` : "text-zinc-400 hover:text-white bg-white/5 border border-white/10"}`}><Cpu className="w-3.5 h-3.5" /><span>Diagnostics Copilot</span></button>
          <button type="button" onClick={() => { triggerHaptic("LIGHT"); setActiveTab("database"); }} className={`flex-1 sm:flex-none px-3 min-h-[36px] rounded-lg text-[8px] sm:text-[9px] font-black uppercase tracking-wider cursor-pointer transition-all active:scale-95 flex items-center justify-center gap-1.5 ${activeTab === "database" ? `${t.bg} text-black shadow-md` : "text-zinc-400 hover:text-white bg-white/5 border border-white/10"}`}><HardDrive className="w-3.5 h-3.5" /><span>Engineering &amp; Spec Matrix</span></button>
        </div>
      </header>

      {/* ERROR ALERT */}
      {errorMsg && (
        <div className="p-3 bg-red-950/80 border border-red-500/50 rounded-xl flex items-center justify-between text-[9px] sm:text-[10px] text-red-200">
          <div className="flex items-center gap-1.5 font-mono"><AlertTriangle className="w-3.5 h-3.5 text-red-400" /><span>{errorMsg}</span></div>
          <button type="button" onClick={() => setErrorMsg(null)} className="text-red-400 hover:text-white font-black uppercase px-2 min-h-[36px] cursor-pointer">Dismiss</button>
        </div>
      )}

      {/* SETTINGS MODAL */}
      <MechanicSettingsModal 
        showSettings={showSettings} setShowSettings={setShowSettings} callsign={callsign} userFleet={userFleet} setUserFleet={setUserFleet}
        aiModelVersion={aiModelVersion} setAiModelVersion={setAiModelVersion} safeSearchLevel={safeSearchLevel} setSafeSearchLevel={setSafeSearchLevel}
        maxSpecResultsCount={maxSpecResultsCount} setMaxSpecResultsCount={setMaxSpecResultsCount} autoClearPhoto={autoClearPhoto} setAutoClearPhoto={setAutoClearPhoto}
        ttsRate={ttsRate} setTtsRate={setTtsRate} ttsPitch={ttsPitch} setTtsPitch={setTtsPitch} autoSaveLogs={autoSaveLogs} setAutoSaveLogs={setAutoSaveLogs}
        autoReadAloud={autoReadAloud} setAutoReadAloud={setAutoReadAloud} aiDetailLevel={aiDetailLevel} setAiDetailLevel={setAiDetailLevel}
        searchScope={searchScope} setSearchScope={setSearchScope} userRegion={userRegion} setUserRegion={setUserRegion} preferredMarketplace={preferredMarketplace}
        setPreferredMarketplace={setPreferredMarketplace} unitSystem={unitSystem} setUnitSystem={setUnitSystem} theme={theme} setTheme={setTheme}
        saveConfiguration={saveConfiguration} themeColors={t}
      />

      {/* VIEW ROUTING CONTAINER */}
      {activeTab === "chat" ? (
        <MechanicChat 
          scanHistory={scanHistory} clearAllHistory={clearAllHistory} loadPreviousSession={loadPreviousSession} activeSessionId={activeSessionId}
          galleryInputRef={galleryInputRef} cameraInputRef={cameraInputRef} handleFileChange={(e: any) => { const f = e.target.files?.[0]; if (f) { setPreviewUrl(URL.createObjectURL(f)); setMimeType(f.type); } }} 
          previewUrl={previewUrl} clearImage={clearImage} analyzeImage={analyzeImage} isAnalyzing={isAnalyzing} messages={messages} setMessages={setMessages}
          errorMsg={errorMsg} question={question} setQuestion={setQuestion} toggleListening={toggleListening} isListening={isListening} currentlyReadingId={currentlyReadingId}
          handleTTS={handleTTS} copyToClipboard={copyToClipboard} copiedId={copiedId} chatEndRef={chatEndRef} themeColors={t}
        />
      ) : (
        <div className={`app-card ${glassBlurClass} ${cornerStyle} bg-zinc-950/90 p-3 sm:p-5 border border-white/10 shadow-2xl space-y-4`}>
          <nav className="flex border-b border-white/10 pb-1.5 gap-2 overflow-x-auto custom-scrollbar">
            <button type="button" onClick={() => { triggerHaptic("LIGHT"); setDbSubTab("parts"); }} className={`pb-1.5 text-[8px] sm:text-[9px] font-black uppercase tracking-wider border-b-2 cursor-pointer min-h-[36px] px-2 transition-all flex items-center gap-1.5 whitespace-nowrap ${dbSubTab === "parts" ? `${t.border} ${t.text}` : "border-transparent text-zinc-500 hover:text-zinc-300"}`}>
              <span>Multi-Source Parts Finder</span>
            </button>
            <button type="button" onClick={() => { triggerHaptic("LIGHT"); setDbSubTab("specs"); }} className={`pb-1.5 text-[8px] sm:text-[9px] font-black uppercase tracking-wider border-b-2 cursor-pointer min-h-[36px] px-2 transition-all flex items-center gap-1.5 whitespace-nowrap ${dbSubTab === "specs" ? `${t.border} ${t.text}` : "border-transparent text-zinc-500 hover:text-zinc-300"}`}>
              <span>Deep Spec Matrix</span>
            </button>
            <button type="button" onClick={() => { triggerHaptic("LIGHT"); setDbSubTab("tools"); }} className={`pb-1.5 text-[8px] sm:text-[9px] font-black uppercase tracking-wider border-b-2 cursor-pointer min-h-[36px] px-2 transition-all flex items-center gap-1.5 whitespace-nowrap ${dbSubTab === "tools" ? `${t.border} ${t.text}` : "border-transparent text-zinc-500 hover:text-zinc-300"}`}>
              <span>80+ Lab Diagnostic Suites</span>
            </button>
          </nav>

          {dbSubTab === "parts" && (
            <PartsFinderTab 
              universalPartQuery={universalPartQuery} setUniversalPartQuery={setUniversalPartQuery} partsMake={partsMake} setPartsMake={setPartsMake}
              partsModel={partsModel} setPartsModel={setPartsModel} partsTireSize={partsTireSize} setPartsTireSize={setPartsTireSize} partsCategory={partsCategory}
              setPartsCategory={setPartsCategory} partsDifficultyFilter={partsDifficultyFilter} setPartsDifficultyFilter={setPartsDifficultyFilter}
              searchLiveParts={searchLiveParts} exportToCSV={exportToCSV} buildManifest={buildManifest} copyManifest={copyManifest} clearManifest={clearManifest}
              isSearchingParts={isSearchingParts} hasSearchedParts={hasSearchedParts} filteredParts={filteredParts} displayCountParts={displayCountParts}
              setDisplayCountParts={setDisplayCountParts} addToManifest={addToManifest} userRegion={userRegion} setUserRegion={setUserRegion}
              getRegionalDomainSuffix={getRegionalDomainSuffix} themeColors={t}
            />
          )}

          {dbSubTab === "specs" && (
            <SpecEngineTab 
              dbSearch={dbSearch} setDbSearch={setDbSearch} searchLiveSpecs={searchLiveSpecs} handleRandomDiscovery={() => searchLiveSpecs("Top Tier 72V 60V High Performance PEVs")} 
              specCategoryFilter={specCategoryFilter} setSpecCategoryFilter={setSpecCategoryFilter} specSortBy={specSortBy} setSpecSortBy={setSpecSortBy}
              exportToCSV={exportToCSV} isSearchingDb={isSearchingDb} hasSearchedDb={hasSearchedDb} filteredAndSortedSpecs={filteredAndSortedSpecs} displayCountSpecs={displayCountSpecs}
              setDisplayCountSpecs={setDisplayCountSpecs} expandedPevIdx={expandedPevIdx} setExpandedPevIdx={setExpandedPevIdx} activeImageIndices={activeImageIndices}
              setActiveImageIndices={setActiveImageIndices} compareList={compareList} toggleCompare={toggleCompare} setActiveVideoUrl={setActiveVideoUrl}
              setActiveVideoTitle={setActiveVideoTitle} setLightboxState={setLightboxState} 
              specMake={specMake} setSpecMake={setSpecMake}
              specModel={specModel} setSpecModel={setSpecModel}
              specVehicleType={specVehicleType} setSpecVehicleType={setSpecVehicleType}
              themeColors={t}
            />
          )}

          {dbSubTab === "tools" && (
            <EngineeringToolsTab 
              selectedErrorCode={selectedErrorCode} setSelectedErrorCode={setSelectedErrorCode} getErrorCodeDetails={getErrorCodeDetails} factoryPeakVolts={factoryPeakVolts}
              setFactoryPeakVolts={setFactoryPeakVolts} measuredLoadVolts={measuredLoadVolts} setMeasuredLoadVolts={setMeasuredLoadVolts} testLoadAmps={testLoadAmps}
              setTestLoadAmps={setTestLoadAmps} calculateBatteryHealth={calculateBatteryHealth} selectedSymptom={selectedSymptom} setSelectedSymptom={setSelectedSymptom}
              getSymptomTriageGuide={getSymptomTriageGuide} kwhRate={kwhRate} setKwhRate={setKwhRate} chargerAmps={chargerAmps} setChargerAmps={setChargerAmps}
              calcVoltage={calcVoltage} calcAh={calcAh} hillGradePercent={hillGradePercent} setHillGradePercent={setHillGradePercent} targetClimbSpeed={targetClimbSpeed}
              setTargetClimbSpeed={setTargetClimbSpeed} riderWeight={riderWeight} setRiderWeight={setRiderWeight} vehicleWeight={vehicleWeight} setVehicleWeight={setVehicleWeight}
              ambientTemp={ambientTemp} setAmbientTemp={setAmbientTemp} ambientTempF={ambientTempF} setAmbientTempF={setAmbientTempF} brakeFluidType={brakeFluidType}
              setBrakeFluidType={setBrakeFluidType} getBrakeFluidInfo={getBrakeFluidInfo} calcWhPerMile={calcWhPerMile} setCalcWhPerMile={setCalcWhPerMile}
              oldTireSize={oldTireSize} setOldTireSize={setOldTireSize} newTireSize={newTireSize} setNewTireSize={setNewTireSize} currentSpeed={currentSpeed}
              setCurrentSpeed={setCurrentSpeed} stockAmps={stockAmps} setStockAmps={setStockAmps} shuntSolderPercent={shuntSolderPercent} setShuntSolderPercent={setShuntSolderPercent}
              selectedBoltSize={selectedBoltSize} setSelectedBoltSize={setSelectedBoltSize} getDynamicTorqueSpec={getDynamicTorqueSpec} customWireAmps={customWireAmps}
              setCustomWireAmps={setCustomWireAmps} getRecommendedAWG={getRecommendedAWG} budgetItems={budgetItems} setBudgetItems={setBudgetItems} newItemName={newItemName}
              setNewItemName={setNewItemName} newItemCost={newItemCost} setNewItemCost={setNewItemCost} themeColors={t}
            />
          )}
        </div>
      )}

      {/* GLOBAL LIGHTBOX MODAL */}
      {lightboxState && (
        <div className="fixed inset-0 z-[300] bg-black/95 backdrop-blur-2xl flex items-center justify-center p-3 animate-fadeIn" onClick={() => setLightboxState(null)}>
          <div className="relative max-w-5xl max-h-[90vh] w-full flex flex-col items-center" onClick={(e) => e.stopPropagation()}>
            <button type="button" onClick={() => setLightboxState(null)} className="absolute -top-10 right-0 bg-white/10 hover:bg-white/20 text-white p-2 rounded-xl cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center"><X className="w-5 h-5" /></button>
            <img src={livePevResults[lightboxState.pevIdx]?.imageUrls?.[lightboxState.imgIdx] || ""} alt="High-Res Lightbox" className="max-w-full max-h-[82vh] object-contain rounded-2xl border border-white/20 shadow-2xl bg-black" />
            <div className="mt-3 flex items-center justify-between w-full font-mono text-[9px] sm:text-[10px] text-zinc-300 px-3">
              <span>{livePevResults[lightboxState.pevIdx]?.brand} {livePevResults[lightboxState.pevIdx]?.name}</span>
              <span className="text-cyan-400">Angle {lightboxState.imgIdx + 1} of {livePevResults[lightboxState.pevIdx]?.imageUrls?.length || 1}</span>
            </div>
          </div>
        </div>
      )}

      {/* GLOBAL VIDEO PREVIEW MODAL */}
      {activeVideoUrl && (
        <div className="fixed inset-0 z-[300] bg-black/95 backdrop-blur-2xl flex items-center justify-center p-3 animate-fadeIn" onClick={() => setActiveVideoUrl(null)}>
          <div className={`relative max-w-4xl w-full app-card ${glassBlurClass} ${cornerStyle} bg-[#0a0a0f] border border-white/20 p-3 sm:p-5 shadow-2xl space-y-3`} onClick={(e) => e.stopPropagation()}>
            <div className="flex justify-between items-center border-b border-white/10 pb-2">
              <h4 className="text-white font-mono font-black text-[10px] sm:text-xs uppercase flex items-center gap-1.5"><Play className="w-3.5 h-3.5 fill-rose-500 text-rose-500" /> {activeVideoTitle || "PEV Field Test & Review"}</h4>
              <button type="button" onClick={() => setActiveVideoUrl(null)} className="text-zinc-400 hover:text-white cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center"><X className="w-4 h-4" /></button>
            </div>
            <div className="w-full aspect-video bg-black rounded-xl overflow-hidden border border-white/10 flex items-center justify-center">
              {activeVideoUrl.includes("youtube.com") || activeVideoUrl.includes("youtu.be") ? (
                <iframe src={activeVideoUrl.includes("watch?v=") ? activeVideoUrl.replace("watch?v=", "embed/") : activeVideoUrl} title="YouTube video player" className="w-full h-full border-0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen />
              ) : (
                <div className="text-center p-6 space-y-2 font-mono">
                  <Play className="w-10 h-10 text-rose-500 mx-auto animate-pulse" />
                  <p className="text-[10px] text-zinc-300">External media feed loaded via secure web gateway.</p>
                  <a href={activeVideoUrl} target="_blank" rel="noreferrer" className="inline-block bg-rose-600 hover:bg-rose-500 text-white font-black uppercase text-[9px] px-5 py-2.5 rounded-lg transition-all min-h-[36px] mt-2">Open Source Stream <ExternalLink className="w-3 h-3 inline ml-1" /></a>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}