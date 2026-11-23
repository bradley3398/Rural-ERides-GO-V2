"use client";

import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  X, Settings, User, Shield, Cloud, Loader2, CloudUpload, CloudDownload, 
  FileText, LogOut, Trash2, ShieldCheck, Languages, Type as TypeIcon, 
  Activity, Bell, Navigation, EyeOff, HeartPulse, Palette, Layout, 
  PieChart, Layers, Map, Volume2, Radio, VolumeX, Cpu, Eye, MapPin, 
  CloudRain, HardDrive, ShieldAlert, BrainCircuit, Image as ImageIcon, 
  Sun, ShieldOff, Search, FileUp, FileDown, Terminal, RefreshCw, 
  Gauge, Zap, Sliders, Compass, Target, MessageSquare, SlidersHorizontal,
  CheckCircle2, AlertTriangle, Mic, Clock, Stethoscope, AlignLeft, Paintbrush, Contrast, Thermometer, Globe,
  CircleDot, Grid, Mountain, Sparkles
} from "lucide-react";

type ThemePreset = "lime" | "cyan" | "emerald" | "amber" | "rose" | "purple" | "void" | string;

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

// ============================================================================
// 🔥 1. AVIONICS ERROR BOUNDARY (CATCHES CRASHES & SHOWS ERROR CODES)
// ============================================================================
class SettingsErrorBoundary extends React.Component<{ children: React.ReactNode }, { hasError: boolean, error: Error | null }> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("SettingsMatrix Critical Fault:", error, errorInfo);
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="fixed inset-0 z-[9999] bg-black/95 flex items-center justify-center p-4 backdrop-blur-xl">
          <div className="bg-red-950/80 border-2 border-red-500 rounded-3xl p-6 max-w-lg w-full shadow-[0_0_50px_rgba(239,68,68,0.4)]">
            <div className="flex items-center gap-3 text-red-500 font-black text-lg uppercase tracking-widest mb-4">
              <AlertTriangle className="w-8 h-8 animate-pulse" />
              <span>Avionics Subsystem Fault</span>
            </div>
            <p className="text-zinc-300 text-xs mb-4">A critical error occurred while rendering the Settings Matrix. Error trace:</p>
            <div className="bg-black/80 p-4 rounded-xl border border-red-900/50 text-[10px] text-red-300 font-mono overflow-y-auto max-h-48 mb-6 break-words shadow-inner">
              {this.state.error?.toString()}
            </div>
            <button 
              onClick={() => window.location.reload()}
              className="w-full min-h-[48px] bg-red-600 hover:bg-red-500 text-white rounded-xl font-black uppercase text-xs tracking-widest transition-all active:scale-95 shadow-lg cursor-pointer"
            >
              Force Reboot Avionics
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

// ============================================================================
// 🔥 2. MAIN SETTINGS COMPONENT
// ============================================================================
function SettingsModalCore(props: any) {
  const {
    isSettingsOpen, setIsSettingsOpen, settingsTab, setSettingsTab,
    user, isOfflineMode, cloudSyncStatus, handleCloudBackup, handleCloudRestore, exportLocalJsonBackup, isProcessingAuth,
    handleSignOut, wipeLocalData, pilotRankBadge, setPilotRankBadge, autoCloudSyncOnFinish, setAutoCloudSyncOnFinish,
    locale, changeLanguage, uiScale, updateUiScale, fontFamily, setFontFamily, reducedMotion, setReducedMotion,
    dynamicIsland, setDynamicIsland, swipeNav, setSwipeNav, autoHideHeader, setAutoHideHeader,
    hapticDuration, setHapticDuration, dataFontWeight, setDataFontWeight, tabIconAnimation, setTabIconAnimation,
    cardOpacityLevel, setCardOpacityLevel, minimalBanners, setMinimalBanners, dashLayout, setDashLayout,
    dataVizStyle, setDataVizStyle, dashDataDensity, setDashDataDensity, mapDefaultZoom, setMapDefaultZoom,
    hapticFeedback, toggleHapticFeedback, hapticIntensity, setHapticIntensity, globalVolume, updateGlobalVolume,
    soundPack, setSoundPack, autoMuteOnVideo, setAutoMuteOnVideo, dataRetentionPolicy, setDataRetentionPolicy,
    highPrecisionBgGps, setHighPrecisionBgGps, autoErrorLogging, setAutoErrorLogging, maxFps, setMaxFps,
    distanceUnitOverride, setDistanceUnitOverride, tempUnitOverride, setTempUnitOverride, decimalPrecision, setDecimalPrecision,
    privacyZone, setPrivacyZone, routeSnapping, setRouteSnapping, autoWeatherSync, setAutoWeatherSync,
    weatherUpdateFrequency, setWeatherUpdateFrequency, strictHardwareGps, setStrictHardwareGps, telemetryRate, setTelemetryRate,
    gpsAccuracyLimit, setGpsAccuracyLimit, gpsDriftTol, setGpsDriftTol, autoStartTracking, setAutoStartTracking,
    shareLeaderboard, setShareLeaderboard, cloudSyncInterval, setCloudSyncInterval, offlineSyncTimeout, setOfflineSyncTimeout,
    bgNetThrottle, setBgNetThrottle, backgroundGps, setBackgroundGps, powerSaveThreshold, setPowerSaveThreshold,
    maxCacheLimit, setMaxCacheLimit, offlinePrefetch, setOfflinePrefetch, biometricAppLock, setBiometricAppLock,
    emergencyContactName, setEmergencyContactName, sosContactNumber, setSosContactNumber, sosCancelWindow, setSosCancelWindow,
    crashSensitivity, setCrashSensitivity, batteryProfile, setBatteryProfile, voiceHotwordActive, setVoiceHotwordActive,
    smartBatteryAi, setSmartBatteryAi, autoErrorScrape, setAutoErrorScrape, localEncryption, setLocalEncryption,
    imuPollingRate, setImuPollingRate, offlineTileProvider, setOfflineTileProvider, wakeLockEnabled, toggleWakeLock,
    autoSleep, toggleAutoSleep, hardwareAcceleration, setHardwareAcceleration, diagnosticAutoScan, setDiagnosticAutoScan,
    autoUpdateChecks, setAutoUpdateChecks, debugMode, setDebugMode, brandTheme, changeTheme, nightModeSchedule, setNightModeSchedule,
    cornerStyle, setCornerStyle, panelBorderWidth, setPanelBorderWidth, cardElevation, setCardElevation,
    transitionSpeedProfile, setTransitionSpeedProfile, headerStyleVariant, setHeaderStyleVariant, borderGlowIntensity, setBorderGlowIntensity,
    bgTexture, setBgTexture, glowRadius, setGlowRadius, mapLineColor, setMapLineColor, buttonStyling, setButtonStyling, glassmorphism, setGlassmorphism,
    activeTheme, glassBlurClass, triggerHaptic
  } = props;

  const [settingsSearchQuery, setSettingsSearchQuery] = useState("");
  
  // --- 14 ORIGINAL LIVE AVIONICS STATES ---
  const [beepPitch, setBeepPitch] = useState(() => localStorage.getItem("rt_beep_pitch") || "650");
  const [speedOffset, setSpeedOffset] = useState(() => localStorage.getItem("rt_speed_offset") || "5");
  const [radarRadius, setRadarRadius] = useState(() => localStorage.getItem("radar_scan_radius") || "50");
  const [ghostSpeed, setGhostSpeed] = useState(() => localStorage.getItem("rt_ghost_speed") || "1.0");
  const [copilotPersona, setCopilotPersona] = useState(() => localStorage.getItem("copilot_persona") || "Tactical Coach");
  const [pressureUnit, setPressureUnit] = useState(() => localStorage.getItem("rt_pressure_unit") || "inHg");
  const [lockOrient, setLockOrient] = useState(() => localStorage.getItem("rt_lock_orient") === "true");
  const [csvDelimiter, setCsvDelimiter] = useState(() => localStorage.getItem("rt_csv_delim") || ",");
  const [dmChime, setDmChime] = useState(() => localStorage.getItem("rt_dm_chime") !== "false");
  const [mapScalePos, setMapScalePos] = useState(() => localStorage.getItem("rt_map_scale_pos") || "Bottom Left");
  const [elevationGradient, setElevationGradient] = useState(() => localStorage.getItem("rt_elevation_gradient") !== "false");
  const [boardLimit, setBoardLimit] = useState(() => localStorage.getItem("rt_board_limit") || "50");
  const [slopeSmooth, setSlopeSmooth] = useState(() => localStorage.getItem("rt_slope_smooth") || "Balanced");
  const [sosTemplate, setSosTemplate] = useState(() => localStorage.getItem("rt_sos_template") || "EMERGENCY: Pilot requires immediate assistance at GPS coordinates.");

  // --- NEW LIVE CONFIGURATION STATES ---
  const [medicalProfile, setMedicalProfile] = useState(() => localStorage.getItem("rt_medical_profile") || "");
  const [publicProfile, setPublicProfile] = useState(() => localStorage.getItem("rt_public_profile") !== "false");
  const [colorblindMode, setColorblindMode] = useState(() => localStorage.getItem("rt_colorblind") || "none");
  const [timeFormat, setTimeFormat] = useState(() => localStorage.getItem("rural_time_format") || "12h");
  const [hudOrientation, setHudOrientation] = useState(() => localStorage.getItem("rural_hud_orient") || "auto");
  const [dialSweep, setDialSweep] = useState(() => localStorage.getItem("rt_dial_sweep") || "240");
  const [trafficLayer, setTrafficLayer] = useState(() => localStorage.getItem("rt_traffic_layer") === "true");
  const [voltDisplayMode, setVoltDisplayMode] = useState(() => localStorage.getItem("rt_volt_disp_mode") || "Percentage");
  const [regenAlgorithm, setRegenAlgorithm] = useState(() => localStorage.getItem("rt_regen_algo") || "Linear");
  const [motorTempWarn, setMotorTempWarn] = useState(() => localStorage.getItem("rt_motor_temp_warn") || "85");
  const [schoolZoneStrict, setSchoolZoneStrict] = useState(() => localStorage.getItem("rt_school_zone") !== "false");
  const [hudBlinkMax, setHudBlinkMax] = useState(() => localStorage.getItem("rt_hud_blink") !== "false");
  const [autoClearLogs, setAutoClearLogs] = useState(() => localStorage.getItem("rt_clear_logs") || "never");
  const [quickReplyTemplates, setQuickReplyTemplates] = useState(() => localStorage.getItem("rt_quick_replies") || "On my way!, Need a charge, Trail blocked");
  const [sagCompensation, setSagCompensation] = useState(() => localStorage.getItem("rt_sag_comp") || "AI Dynamic");
  const [navVoice, setNavVoice] = useState(() => localStorage.getItem("rt_nav_voice") || "AI Female");
  const [autoDimmer, setAutoDimmer] = useState(() => (typeof window !== 'undefined' ? localStorage.getItem("rt_auto_dimmer") === "true" : false));
  const [fatigueAlert, setFatigueAlert] = useState(() => (typeof window !== 'undefined' ? localStorage.getItem("rt_fatigue_alert") || "None" : "None"));

  // --- THE MASTER LIVE SYNC ENGINE ---
  const updateSetting = (key: string, value: any, setterMethod?: Function) => {
    if (triggerHaptic) triggerHaptic();
    else if (typeof navigator !== "undefined" && "vibrate" in navigator) navigator.vibrate(15);
    
    if (setterMethod) setterMethod(value);
    
    if (typeof window !== "undefined") {
      localStorage.setItem(key, String(value));
      window.dispatchEvent(new Event("settings-sync"));
      window.dispatchEvent(new Event("storage"));
    }
  };

  const handleUnitOverrideChange = (val: string) => {
    updateSetting("rt_dist_unit", val, setDistanceUnitOverride);
    const isMetric = val === "km" ? "true" : val === "mi" ? "false" : "false";
    localStorage.setItem("rt_use_metric", isMetric);
    localStorage.setItem("radar_use_metric", isMetric);
    window.dispatchEvent(new Event("settings-sync"));
  };

  const handleJsonFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const parsed = JSON.parse(event.target?.result as string);
        if (parsed && typeof parsed === 'object') {
          Object.keys(parsed).forEach(k => { localStorage.setItem(k, parsed[k]); });
          alert("Local vault successfully restored! Rebooting avionics...");
          window.location.reload();
        } else { throw new Error("Invalid vault format."); }
      } catch (err: any) { alert(`Import rejected: ${err.message}`); }
    };
    reader.readAsText(file);
  };

  // 25 Live Font Options
  const fontOptions = [
    { id: "font-sans", name: "Modern Sans (Inter)", style: "font-sans", css: "ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif" },
    { id: "font-mono", name: "Cyberpunk Mono (JetBrains)", style: "font-mono", css: "'JetBrains Mono', monospace" },
    { id: "font-serif", name: "Classic Editorial Serif", style: "font-serif", css: "ui-serif, Georgia, Cambria, serif" },
    { id: "orbitron", name: "Sci-Fi HUD (Orbitron)", style: "'Orbitron', sans-serif", css: "'Orbitron', sans-serif" },
    { id: "rajdhani", name: "Avionics Bold (Rajdhani)", style: "'Rajdhani', sans-serif", css: "'Rajdhani', sans-serif" },
    { id: "sharetech", name: "Terminal Tech (Share Tech)", style: "'Share Tech Mono', monospace", css: "'Share Tech Mono', monospace" },
    { id: "spacegrotesk", name: "Space Grotesk Modern", style: "'Space Grotesk', sans-serif", css: "'Space Grotesk', sans-serif" },
    { id: "audiowide", name: "Futuristic Display (Audiowide)", style: "'Audiowide', cursive", css: "'Audiowide', cursive" },
    { id: "ibmplex", name: "Industrial Clean (IBM Plex)", style: "'IBM Plex Sans', sans-serif", css: "'IBM Plex Sans', sans-serif" },
    { id: "courier", name: "Military Courier", style: "'Courier New', monospace", css: "'Courier New', monospace" },
    { id: "georgia", name: "Elite Georgia Serif", style: "'Georgia', serif", css: "'Georgia', serif" },
    { id: "verdana", name: "High-Legibility Verdana", style: "'Verdana', sans-serif", css: "'Verdana', sans-serif" },
    { id: "trebuchet", name: "Clean Trebuchet Sans", style: "'Trebuchet MS', sans-serif", css: "'Trebuchet MS', sans-serif" },
    { id: "palatino", name: "Palatino Classic", style: "'Palatino Linotype', serif", css: "'Palatino Linotype', serif" },
    { id: "lucide", name: "Console Lucida Mono", style: "'Lucida Console', monospace", css: "'Lucida Console', monospace" },
    { id: "roboto", name: "Google Roboto", style: "'Roboto', sans-serif", css: "'Roboto', sans-serif" },
    { id: "montserrat", name: "Montserrat", style: "'Montserrat', sans-serif", css: "'Montserrat', sans-serif" },
    { id: "lato", name: "Lato", style: "'Lato', sans-serif", css: "'Lato', sans-serif" },
    { id: "poppins", name: "Poppins", style: "'Poppins', sans-serif", css: "'Poppins', sans-serif" },
    { id: "oswald", name: "Oswald Display", style: "'Oswald', sans-serif", css: "'Oswald', sans-serif" },
    { id: "raleway", name: "Raleway Elegant", style: "'Raleway', sans-serif", css: "'Raleway', sans-serif" },
    { id: "playfair", name: "Playfair Display", style: "'Playfair Display', serif", css: "'Playfair Display', serif" },
    { id: "firacode", name: "Fira Code Terminal", style: "'Fira Code', monospace", css: "'Fira Code', monospace" },
    { id: "sourcecodepro", name: "Source Code Pro", style: "'Source Code Pro', monospace", css: "'Source Code Pro', monospace" },
    { id: "vt323", name: "VT323 Retro Terminal", style: "'VT323', monospace", css: "'VT323', monospace" }
  ];

  const applyFont = (fontCss: string) => {
    updateSetting("rt_font_family", fontCss, setFontFamily);
    if (typeof document !== "undefined") {
      if (!document.getElementById("avionics-web-fonts")) {
        const link = document.createElement("link");
        link.id = "avionics-web-fonts";
        link.rel = "stylesheet";
        link.href = "https://fonts.googleapis.com/css2?family=Audiowide&family=Fira+Code:wght@400;700&family=IBM+Plex+Sans:wght@400;700&family=JetBrains+Mono:wght@400;700&family=Lato:wght@400;700&family=Montserrat:wght@400;700&family=Open+Sans:wght@400;700&family=Orbitron:wght@400;700;900&family=Oswald:wght@400;700&family=Playfair+Display:wght@400;700&family=Poppins:wght@400;700&family=Rajdhani:wght@500;700&family=Raleway:wght@400;700&family=Roboto:wght@400;700&family=Share+Tech+Mono&family=Source+Code+Pro:wght@400;700&family=Space+Grotesk:wght@400;700&family=VT323&display=swap";
        document.head.appendChild(link);
      }
      document.documentElement.style.fontFamily = fontCss;
    }
  };

  const matchesSearch = (searchTermsString: string) => {
    if (!settingsSearchQuery.trim()) return true;
    const query = settingsSearchQuery.toLowerCase();
    return searchTermsString.toLowerCase().includes(query);
  };

  // =========================================================================
  // 🔥 REUSABLE UI COMPONENTS (FIX FOR "NOT DEFINED" ERRORS)
  // =========================================================================
  const safeHaptic = () => {
    if (triggerHaptic) triggerHaptic();
    else if (typeof navigator !== "undefined" && "vibrate" in navigator) navigator.vibrate(15);
  };

  const ControlWrapper = ({ label, desc, icon: Icon, children, badge }: any) => (
    <div className="flex items-center justify-between p-3.5 sm:p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-xl">
      <div className="flex-1 min-w-0 pr-2">
        <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-0.5">
          {Icon ? <Icon className="w-4 h-4 text-zinc-400 shrink-0" /> : <div className="w-4 h-4 shrink-0" />}
          <span className="truncate">{label}</span>
          {badge && <span className={`text-[8px] px-1.5 py-0.5 bg-white/10 ${activeTheme?.text || 'text-white'} rounded font-mono uppercase border border-white/10`}>{badge}</span>}
        </div>
        {desc && <div className="text-[10px] text-zinc-500 font-mono leading-tight mt-1">{desc}</div>}
      </div>
      <div className="shrink-0 flex items-center">{children}</div>
    </div>
  );

  const Toggle = ({ active, onToggle, label }: { active: boolean; onToggle: () => void; label?: string }) => (
    <button
      type="button"
      role="switch"
      aria-checked={active}
      aria-label={label || "Toggle"}
      onClick={onToggle}
      className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-white/20 cursor-pointer ${
        active ? `${activeTheme?.bg || 'bg-white'} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"
      }`}
    >
      <motion.span
        layout
        transition={{ type: "spring", stiffness: 500, damping: 30 }}
        className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md ${active ? "translate-x-6" : "translate-x-1"}`}
      />
    </button>
  );

  const Select = ({ value, onChange, options, className = "" }: any) => (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className={`min-h-[38px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner ${className}`}
    >
      {options.map((opt: any) => {
        const val = typeof opt === 'object' ? opt.value : opt;
        const lbl = typeof opt === 'object' ? opt.label : opt;
        return (
          <option key={val} value={val} className="bg-zinc-900 text-white py-2">
            {lbl}
          </option>
        );
      })}
    </select>
  );

  const SectionTitle = ({ icon: Icon, title, color = "text-zinc-400" }: any) => (
    <div className={`text-[10px] font-black uppercase tracking-widest flex items-center gap-2 mb-2 pl-1 ${color}`}>
      {Icon && <Icon className="w-4 h-4" />}
      <span>{title}</span>
    </div>
  );

  if (!isSettingsOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-[100] flex items-center justify-center p-3 sm:p-4">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setIsSettingsOpen(false)} className={`absolute inset-0 bg-black/80 backdrop-blur-2xl`}></motion.div>
        
        <motion.div 
          initial={{ opacity: 0, scale: reducedMotion ? 1 : 0.95, y: reducedMotion ? 0 : 20 }} 
          animate={{ opacity: 1, scale: 1, y: 0 }} 
          exit={{ opacity: 0, scale: reducedMotion ? 1 : 0.95, y: reducedMotion ? 0 : 20 }} 
          className={`bg-black/95 backdrop-blur-3xl border border-white/20 w-full max-w-2xl ${glassBlurClass} ${cornerStyle} p-4 sm:p-7 shadow-[0_0_90px_rgba(0,0,0,0.98)] relative z-10 flex flex-col max-h-[92vh]`}
        >
          
          {/* HEADER & GLOBAL SETTINGS SEARCH BAR */}
          <div className="flex flex-col gap-3.5 border-b border-white/10 pb-4 mb-4 shrink-0">
            <div className="flex justify-between items-center">
              <h2 className={`text-sm sm:text-lg font-black uppercase tracking-widest flex items-center gap-2 text-white`}>
                <Settings className={`w-5 h-5 ${activeTheme?.text || 'text-white'}`} /> Master Avionics Control Matrix
              </h2>
              <button type="button" onClick={() => { if(triggerHaptic) triggerHaptic(); setIsSettingsOpen(false); }} className="min-h-[44px] min-w-[44px] flex items-center justify-center p-2.5 bg-white/10 border border-white/15 rounded-full hover:bg-white/20 text-zinc-300 hover:text-white transition-colors cursor-pointer active:scale-95">
                <X className="w-4 h-4" />
              </button>
            </div>
            
            <div className="relative">
              <Search className="absolute left-3.5 top-3.5 w-4 h-4 text-zinc-400" />
              <input 
                type="text" 
                placeholder="Search live system configurations, 25 live fonts, & active controls..." 
                value={settingsSearchQuery} 
                onChange={(e) => setSettingsSearchQuery(e.target.value)} 
                className="w-full min-h-[46px] bg-black/70 border border-white/15 rounded-2xl py-3 pl-10 pr-4 text-xs font-bold text-white outline-none focus:border-cyan-400 shadow-inner" 
              />
            </div>
          </div>

          {/* TAB SWITCHER */}
          <div className="flex flex-wrap border border-white/10 bg-black/60 shrink-0 mb-4 rounded-2xl shadow-inner overflow-hidden p-1 gap-1">
            <button type="button" onClick={() => { if(triggerHaptic) triggerHaptic(); setSettingsTab("profile"); }} className={`flex-1 min-h-[44px] py-2.5 px-2 text-[10px] font-black uppercase tracking-widest transition-all text-center rounded-xl cursor-pointer ${settingsTab === "profile" ? `${activeTheme?.bg || 'bg-white'} text-black font-black shadow-md` : "text-zinc-400 hover:text-white hover:bg-white/5"}`}>Account</button>
            <button type="button" onClick={() => { if(triggerHaptic) triggerHaptic(); setSettingsTab("preferences"); }} className={`flex-1 min-h-[44px] py-2.5 px-2 text-[10px] font-black uppercase tracking-widest transition-all text-center rounded-xl cursor-pointer ${settingsTab === "preferences" ? `${activeTheme?.bg || 'bg-white'} text-black font-black shadow-md` : "text-zinc-400 hover:text-white hover:bg-white/5"}`}>UI Prefs</button>
            <button type="button" onClick={() => { if(triggerHaptic) triggerHaptic(); setSettingsTab("app_settings"); }} className={`flex-1 min-h-[44px] py-2.5 px-2 text-[10px] font-black uppercase tracking-widest transition-all text-center rounded-xl cursor-pointer ${settingsTab === "app_settings" ? `${activeTheme?.bg || 'bg-white'} text-black font-black shadow-md` : "text-zinc-400 hover:text-white hover:bg-white/5"}`}>App Config</button>
            <button type="button" onClick={() => { if(triggerHaptic) triggerHaptic(); setSettingsTab("appearance"); }} className={`flex-1 min-h-[44px] py-2.5 px-2 text-[10px] font-black uppercase tracking-widest transition-all text-center rounded-xl cursor-pointer ${settingsTab === "appearance" ? `${activeTheme?.bg || 'bg-white'} text-black font-black shadow-md` : "text-zinc-400 hover:text-white hover:bg-white/5"}`}>Appearance</button>
          </div>

          <div className="overflow-y-auto space-y-6 pr-2 custom-scrollbar relative">
            <AnimatePresence mode="wait">
            
            {/* ========================================================================= */}
            {/* 1. PROFILE & VAULT TAB */}
            {/* ========================================================================= */}
            {(settingsTab === "profile" || settingsSearchQuery) && (
              <motion.div key="profile" initial={{ opacity: 0, y: reducedMotion ? 0 : 5 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }} className="space-y-6">
                
                {matchesSearch("Identity Network User Token Email Offline Public") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] text-zinc-400 font-black uppercase tracking-widest flex items-center gap-1.5 pl-1"><User className="w-3.5 h-3.5"/> Network Identity</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl p-4 flex flex-col sm:flex-row items-center justify-between shadow-inner gap-4">
                      <div className="flex items-center gap-3.5 w-full">
                        <div className={`w-14 h-14 shrink-0 rounded-2xl ${activeTheme?.bg || 'bg-white'} flex items-center justify-center text-black font-black text-2xl shadow-xl`}>
                          {user?.email?.charAt(0).toUpperCase() || "P"}
                        </div>
                        <div className="truncate w-full">
                          <div className="text-sm font-bold text-white mb-0.5 truncate">{user?.email || "Unknown Pilot"}</div>
                          <div className={`text-[9px] ${isOfflineMode ? 'text-amber-500' : (activeTheme?.text || 'text-white')} font-mono uppercase tracking-widest flex items-center gap-1.5`}>
                            <Shield className="w-3.5 h-3.5" /> {isOfflineMode ? 'Offline Cache Active' : 'Secure Token Active'}
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/10 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><User className="w-4 h-4 text-zinc-400 shrink-0"/> Public Profile Visibility</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Allow other riders to see your profile on the radar.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_public_profile", !publicProfile, setPublicProfile)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${publicProfile ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <span className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${publicProfile ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("Medical Profile Health Blood Allergies") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] text-rose-400 font-black uppercase tracking-widest flex items-center gap-1.5 pl-1"><Stethoscope className="w-3.5 h-3.5"/> Rider Medical &amp; Safety Profile</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl p-4 shadow-inner">
                      <div className="flex flex-col gap-2">
                        <label className="text-[10px] font-bold text-white flex items-center gap-1.5"><Stethoscope className="w-4 h-4 text-zinc-400 shrink-0"/> Emergency Medical Profile (Optional)</label>
                        <span className="text-[9px] text-zinc-400 font-mono">List blood type, allergies, or conditions. Displayed on SOS dispatch.</span>
                        <input 
                          type="text" 
                          value={medicalProfile} 
                          onChange={(e) => updateSetting("rt_medical_profile", e.target.value, setMedicalProfile)} 
                          placeholder="e.g., O-Negative, No Allergies"
                          className="w-full min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-xs font-bold outline-none transition-all shadow-inner mt-1" 
                        />
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("Quick Replies Chat Messaging Templates") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] text-cyan-400 font-black uppercase tracking-widest flex items-center gap-1.5 pl-1"><MessageSquare className="w-3.5 h-3.5"/> Communication Templates</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl p-4 shadow-inner">
                      <div className="flex flex-col gap-2">
                        <label className="text-[10px] font-bold text-white flex items-center gap-1.5"><MessageSquare className="w-4 h-4 text-zinc-400 shrink-0"/> Quick Reply Presets</label>
                        <span className="text-[9px] text-zinc-400 font-mono">Comma separated list of quick replies for radar chat.</span>
                        <input 
                          type="text" 
                          value={quickReplyTemplates} 
                          onChange={(e) => updateSetting("rt_quick_replies", e.target.value, setQuickReplyTemplates)} 
                          placeholder="On my way!, Need a charge..."
                          className="w-full min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-xs font-bold outline-none transition-all shadow-inner mt-1" 
                        />
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("Cloud Local JSON Vault Backup Restore Export Import Firebase") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] text-zinc-400 font-black uppercase tracking-widest flex items-center gap-1.5 pl-1"><Cloud className="w-3.5 h-3.5 text-cyan-400"/> Cloud &amp; Local JSON Vault Backup</h3>
                    {cloudSyncStatus ? (
                      <div className="bg-cyan-950/40 border border-cyan-900/50 text-cyan-300 text-[10px] font-mono p-4 rounded-xl flex items-center justify-center gap-3 w-full shadow-inner">
                        <Loader2 className={`w-5 h-5 ${reducedMotion ? "" : "animate-spin"}`} /> {cloudSyncStatus}
                      </div>
                    ) : (
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <button type="button" onClick={handleCloudBackup} disabled={isProcessingAuth} className="min-h-[48px] bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/30 text-cyan-400 p-4 rounded-xl text-left transition-all active:scale-95 flex items-center gap-3 font-bold text-xs cursor-pointer shadow-md disabled:opacity-50">
                          <CloudUpload className="w-5 h-5 shrink-0 text-cyan-400" /> Backup to Cloud
                        </button>
                        <button type="button" onClick={handleCloudRestore} disabled={isProcessingAuth} className="min-h-[48px] bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 p-4 rounded-xl text-left transition-all active:scale-95 flex items-center gap-3 font-bold text-xs cursor-pointer shadow-md disabled:opacity-50">
                          <CloudDownload className="w-5 h-5 shrink-0 text-emerald-400" /> Restore from Cloud
                        </button>
                        
                        <button type="button" onClick={exportLocalJsonBackup} className="min-h-[48px] bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/30 text-amber-400 p-4 rounded-xl text-left transition-all active:scale-95 flex items-center gap-3 font-bold text-xs cursor-pointer shadow-md">
                          <FileDown className="w-5 h-5 shrink-0 text-amber-400" /> Export JSON Vault
                        </button>
                        
                        <label className="min-h-[48px] bg-purple-500/10 hover:bg-purple-500/20 border border-purple-500/30 text-purple-400 p-4 rounded-xl transition-all active:scale-95 flex items-center gap-3 font-bold text-xs cursor-pointer shadow-md">
                          <FileUp className="w-5 h-5 shrink-0 text-purple-400" /> Import JSON Vault
                          <input type="file" accept=".json" onChange={handleJsonFileUpload} className="hidden" />
                        </label>
                      </div>
                    )}
                  </div>
                )}

                {matchesSearch("Access Control Disconnect Pilot Link Purge Cache Reset Wipe") && (
                  <div className="space-y-3 pt-2">
                    <h3 className="text-[10px] text-zinc-400 font-black uppercase tracking-widest flex items-center gap-1.5 pl-1"><FileText className="w-3.5 h-3.5 text-rose-400"/> Access Control</h3>
                    <div className="flex flex-col gap-2.5">
                      <button type="button" onClick={handleSignOut} className="w-full min-h-[48px] bg-rose-950/20 hover:bg-rose-900/30 border border-rose-900/40 text-rose-400 hover:text-rose-300 p-4 rounded-xl text-left transition-all active:scale-95 flex items-center justify-between group shadow-md cursor-pointer">
                        <div>
                          <div className="text-xs font-bold mb-0.5 flex items-center gap-2">Disconnect Pilot Link</div>
                          <div className="text-[10px] text-rose-400/80 font-mono">Sign out of active network link securely.</div>
                        </div>
                        <LogOut className="w-5 h-5 opacity-50 group-hover:opacity-100 transition-opacity" />
                      </button>
                      <button type="button" onClick={wipeLocalData} className="w-full min-h-[48px] bg-black/40 hover:bg-rose-950/30 border border-white/10 hover:border-rose-900/40 text-zinc-400 hover:text-rose-300 p-4 rounded-xl text-left transition-all active:scale-95 flex items-center justify-between group shadow-md cursor-pointer">
                        <div>
                          <div className="text-xs font-bold mb-0.5 flex items-center gap-2">Purge Local Cache &amp; Reset Matrix</div>
                          <div className="text-[10px] text-zinc-500 font-mono">Deletes saved rides, logs, and resets all configurations.</div>
                        </div>
                        <Trash2 className="w-5 h-5 opacity-50 group-hover:opacity-100 transition-opacity" />
                      </button>
                    </div>
                  </div>
                )}

                {matchesSearch("Pilot Rank Verified Cloud Automation Badge") && (
                  <div className="space-y-3 pt-2">
                    <h3 className="text-[10px] text-zinc-400 font-black uppercase tracking-widest flex items-center gap-1.5 pl-1"><ShieldCheck className="w-3.5 h-3.5 text-emerald-400"/> Pilot Rank &amp; Cloud Automation</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/10 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><ShieldCheck className="w-4 h-4 text-zinc-400 shrink-0"/> Verified Pilot Rank Badge</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Display classification badge on profile feeds.</div>
                        </div>
                        <select value={pilotRankBadge} onChange={(e) => updateSetting("rt_pilot_rank", e.target.value, setPilotRankBadge)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="Rookie Pilot" className="bg-zinc-900 text-white">Rookie Pilot</option>
                          <option value="Trail Scout" className="bg-zinc-900 text-white">Trail Scout</option>
                          <option value="Elite Scout" className="bg-zinc-900 text-white">Elite Scout</option>
                          <option value="Master Commander" className="bg-zinc-900 text-white">Master Commander</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><CloudUpload className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Cloud Backup on Ride Finish</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Automatically backup stats to Firebase post-run.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_cloud_sync", !autoCloudSyncOnFinish, setAutoCloudSyncOnFinish)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoCloudSyncOnFinish ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoCloudSyncOnFinish ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                    </div>
                  </div>
                )}

              </motion.div>
            )}

            {/* ========================================================================= */}
            {/* 2. UI PREFERENCES TAB */}
            {/* ========================================================================= */}
            {(settingsTab === "preferences" || settingsSearchQuery) && (
              <motion.div key="preferences" initial={{ opacity: 0, y: reducedMotion ? 0 : 5 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }} className="space-y-6">
                
                {matchesSearch("Language UI Scale Font Reduced Motion Typography Global Interface Settings Colorblind Time Format") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-zinc-400 pl-1"><Settings className="w-4 h-4"/> Global Interface Settings</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/10 shadow-inner">
                      
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Languages className="w-4 h-4 text-zinc-400 shrink-0"/> System Language Matrix</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Active translation dictionary.</div>
                        </div>
                        <select value={locale} onChange={(e) => { updateSetting("rt_locale", e.target.value, changeLanguage); updateSetting("rural_locale", e.target.value); }} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="en" className="bg-zinc-900 text-white">English (US)</option>
                          <option value="es" className="bg-zinc-900 text-white">Español</option>
                          <option value="de" className="bg-zinc-900 text-white">Deutsch</option>
                          <option value="fr" className="bg-zinc-900 text-white">Français</option>
                          <option value="zh" className="bg-zinc-900 text-white">中文 (Chinese)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><TypeIcon className="w-4 h-4 text-zinc-400 shrink-0"/> Global UI Scale</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Adjusts master text rendering size.</div>
                        </div>
                        <div className="flex bg-black/60 border border-white/10 rounded-xl p-1.5 shrink-0 gap-1 shadow-inner">
                          {["compact", "normal", "large"].map((s) => (
                            <button
                              key={s} type="button" onClick={() => updateSetting("rural_ui_scale", s, updateUiScale)}
                              className={`px-3 py-1.5 rounded-lg text-[10px] font-black uppercase transition-all shadow-sm ${uiScale === s ? `${activeTheme?.bg || 'bg-white'} text-black` : "text-zinc-400 hover:text-white"}`}
                            >
                              {s === "compact" ? "SM" : s === "normal" ? "MD" : "LG"}
                            </button>
                          ))}
                        </div>
                      </div>
                      
                      {/* 🔥 25 LIVE FONT FAMILIES */}
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><TypeIcon className="w-4 h-4 text-cyan-400 shrink-0"/> System Font Family (25 Presets)</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Instant live-rendered application typography.</div>
                        </div>
                        <select 
                          value={fontFamily} 
                          onChange={(e) => applyFont(e.target.value)} 
                          className="min-h-[44px] bg-black/70 border border-cyan-500/40 hover:border-cyan-400 text-cyan-300 rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[170px]"
                          style={{ fontFamily: fontOptions.find(f => f.id === fontFamily || f.style === fontFamily)?.style || 'inherit' }}
                        >
                           {fontOptions.map(font => (
                             <option key={font.id} value={font.css} style={{ fontFamily: font.style }} className="bg-black text-white py-1">
                               {font.name}
                             </option>
                           ))}
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Clock className="w-4 h-4 text-zinc-400 shrink-0"/> System Time Format</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">12-hour or 24-hour military time.</div>
                        </div>
                        <select value={timeFormat} onChange={(e) => updateSetting("rural_time_format", e.target.value, setTimeFormat)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="12h" className="bg-zinc-900 text-white">12-Hour (AM/PM)</option>
                          <option value="24h" className="bg-zinc-900 text-white">24-Hour (Military)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Eye className="w-4 h-4 text-zinc-400 shrink-0"/> Colorblind Assist Mode</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Adjust UI palette for color vision deficiencies.</div>
                        </div>
                        <select value={colorblindMode} onChange={(e) => updateSetting("rt_colorblind", e.target.value, setColorblindMode)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="none" className="bg-zinc-900 text-white">None</option>
                          <option value="protanopia" className="bg-zinc-900 text-white">Protanopia (Red-Blind)</option>
                          <option value="deuteranopia" className="bg-zinc-900 text-white">Deuteranopia (Green-Blind)</option>
                          <option value="tritanopia" className="bg-zinc-900 text-white">Tritanopia (Blue-Blind)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Reduced Motion</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Strips animations for performance.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_reduced_motion", !reducedMotion, setReducedMotion)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${reducedMotion ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${reducedMotion ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                    </div>
                  </div>
                )}

                {matchesSearch("Dynamic Island Swipe Nav Auto-Hide Header Haptic Duration Data Font Enhanced Interface Controls Orientation Dimmer") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-amber-400 pl-1"><Layers className="w-4 h-4"/> Enhanced Interface Controls</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/10 shadow-inner">
                      
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Bell className="w-4 h-4 text-zinc-400 shrink-0"/> Dynamic Island Alerts</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Use pill-shaped top popups for alerts.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_dyn_island", !dynamicIsland, setDynamicIsland)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${dynamicIsland ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${dynamicIsland ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Navigation className="w-4 h-4 text-zinc-400 shrink-0"/> Swipe Navigation</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Enable left/right swiping between tabs.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_swipe_nav", !swipeNav, setSwipeNav)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${swipeNav ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${swipeNav ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><EyeOff className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Hide Top Header</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Hide header when scrolling down feed.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_hide_header", !autoHideHeader, setAutoHideHeader)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoHideHeader ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoHideHeader ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Sun className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Dimmer in Tunnels/Night</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Dim brightness when entering dark zones.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_dimmer", !autoDimmer, setAutoDimmer)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoDimmer ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoDimmer ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layout className="w-4 h-4 text-zinc-400 shrink-0"/> HUD Orientation Lock</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Force UI to portrait or landscape.</div>
                        </div>
                        <select value={hudOrientation} onChange={(e) => updateSetting("rural_hud_orient", e.target.value, setHudOrientation)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                           <option value="auto" className="bg-zinc-900 text-white">Auto-Rotate</option>
                           <option value="portrait" className="bg-zinc-900 text-white">Force Portrait</option>
                           <option value="landscape" className="bg-zinc-900 text-white">Force Landscape</option>
                           <option value="reverse portrait" className="bg-zinc-900 text-white">Reverse Portrait</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><HeartPulse className="w-4 h-4 text-zinc-400 shrink-0"/> Haptic Pulse Duration</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Length of physical vibration.</div>
                        </div>
                        <select value={hapticDuration} onChange={(e) => updateSetting("rt_haptic_dur", e.target.value, setHapticDuration)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                           <option value="short" className="bg-zinc-900 text-white">Short</option>
                           <option value="medium" className="bg-zinc-900 text-white">Medium</option>
                           <option value="long" className="bg-zinc-900 text-white">Long</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><TypeIcon className="w-4 h-4 text-zinc-400 shrink-0"/> Data Font Weight</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Thickness of telemetry numbers.</div>
                        </div>
                        <select value={dataFontWeight} onChange={(e) => updateSetting("rt_data_font_wt", e.target.value, setDataFontWeight)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                           <option value="normal" className="bg-zinc-900 text-white">Standard</option>
                           <option value="bold" className="bg-zinc-900 text-white">Bold</option>
                           <option value="black" className="bg-zinc-900 text-white">Ultra Black</option>
                        </select>
                      </div>

                    </div>
                  </div>
                )}

                {matchesSearch("Tab Animation Card Opacity Minimal Banners Customization") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-fuchsia-400 pl-1"><Palette className="w-4 h-4"/> Advanced UI Customization</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/10 shadow-inner">
                      
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Tab Bar Icon Animation</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Animate icons when switching tabs.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_tab_anim", !tabIconAnimation, setTabIconAnimation)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${tabIconAnimation ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${tabIconAnimation ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layers className="w-4 h-4 text-zinc-400 shrink-0"/> Dashboard Card Opacity</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Transparency level for modular cards.</div>
                        </div>
                        <select value={cardOpacityLevel} onChange={(e) => updateSetting("rt_card_opacity", e.target.value, setCardOpacityLevel)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="60" className="bg-zinc-900 text-white">60% (Transparent)</option>
                          <option value="80" className="bg-zinc-900 text-white">80% (Balanced)</option>
                          <option value="100" className="bg-zinc-900 text-white">100% (Solid)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Bell className="w-4 h-4 text-zinc-400 shrink-0"/> Minimal Notification Banners</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Condense toast alerts heavily.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_minimal_banners", !minimalBanners, setMinimalBanners)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${minimalBanners ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${minimalBanners ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                    </div>
                  </div>
                )}

                {matchesSearch("Layout Dash Densities Visualization Zoom Map Pie Chart Sweep Traffic") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-cyan-400 pl-1"><Layout className="w-4 h-4"/> Layout &amp; Dash Densities</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/10 shadow-inner">
                      
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layout className="w-4 h-4 text-zinc-400 shrink-0"/> Dashboard Layout Style</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Arrangement of main components.</div>
                        </div>
                        <select value={dashLayout} onChange={(e) => updateSetting("rt_dash_layout", e.target.value, setDashLayout)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="modular" className="bg-zinc-900 text-white">Modular Cards</option>
                          <option value="compact" className="bg-zinc-900 text-white">Compact Grid</option>
                          <option value="expanded" className="bg-zinc-900 text-white">Expanded List</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><PieChart className="w-4 h-4 text-zinc-400 shrink-0"/> Data Visualization Style</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">How telemetry metrics are rendered.</div>
                        </div>
                        <select value={dataVizStyle} onChange={(e) => updateSetting("rt_data_viz", e.target.value, setDataVizStyle)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="gauges" className="bg-zinc-900 text-white">Arc Gauges</option>
                          <option value="raw" className="bg-zinc-900 text-white">Raw Numbers</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Gauge className="w-4 h-4 text-zinc-400 shrink-0"/> Speedometer Dial Sweep</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Arc angle for analog gauges.</div>
                        </div>
                        <select value={dialSweep} onChange={(e) => updateSetting("rt_dial_sweep", e.target.value, setDialSweep)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="180" className="bg-zinc-900 text-white">180° Half</option>
                          <option value="240" className="bg-zinc-900 text-white">240° Optimal</option>
                          <option value="270" className="bg-zinc-900 text-white">270° Wide</option>
                          <option value="360" className="bg-zinc-900 text-white">360° Full</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layers className="w-4 h-4 text-zinc-400 shrink-0"/> Dash Data Density</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Padding and spacing in widgets.</div>
                        </div>
                        <select value={dashDataDensity} onChange={(e) => updateSetting("rt_dash_density", e.target.value, setDashDataDensity)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="spacious" className="bg-zinc-900 text-white">Spacious (Default)</option>
                          <option value="compact" className="bg-zinc-900 text-white">Compact (More Data)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Map className="w-4 h-4 text-zinc-400 shrink-0"/> Map Default Zoom Level</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Camera elevation upon boot.</div>
                        </div>
                        <select value={mapDefaultZoom} onChange={(e) => updateSetting("rt_map_default_zoom", e.target.value, setMapDefaultZoom)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="12" className="bg-zinc-900 text-white">Level 12 (Macro)</option>
                          <option value="14" className="bg-zinc-900 text-white">Level 14 (Standard)</option>
                          <option value="16" className="bg-zinc-900 text-white">Level 16 (Tactical)</option>
                          <option value="18" className="bg-zinc-900 text-white">Level 18 (Street)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Live Map Traffic Layer</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Overlay live road congestion.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_traffic_layer", !trafficLayer, setTrafficLayer)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${trafficLayer ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${trafficLayer ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                    </div>
                  </div>
                )}

                {matchesSearch("Haptic Audio Volume Gain Sound Pack Mute Video Pulse Navigation Voice") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-rose-400 pl-1"><Volume2 className="w-4 h-4"/> Haptics &amp; Acoustic Soundstage</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/10 shadow-inner">
                      
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><HeartPulse className="w-4 h-4 text-zinc-400 shrink-0"/> Haptic Feedback Master</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Physical vibration on touch.</div>
                        </div>
                        <button type="button" onClick={() => { if(toggleHapticFeedback) toggleHapticFeedback(); updateSetting("rural_haptic", !hapticFeedback); }} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${hapticFeedback ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${hapticFeedback ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><HeartPulse className="w-4 h-4 text-zinc-400 shrink-0"/> Haptic Force Intensity</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Vibration motor pulse multiplier.</div>
                        </div>
                        <select value={hapticIntensity} onChange={(e) => updateSetting("rt_haptic_intensity", e.target.value, setHapticIntensity)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                           <option value="light" className="bg-zinc-900 text-white">Light</option>
                           <option value="medium" className="bg-zinc-900 text-white">Medium</option>
                           <option value="heavy" className="bg-zinc-900 text-white">Heavy</option>
                           <option value="extreme" className="bg-zinc-900 text-white">Extreme</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Volume2 className="w-4 h-4 text-zinc-400 shrink-0"/> Global Audio Gain Master</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Set {globalVolume}% volume cap.</div>
                        </div>
                        <div className="w-28 sm:w-36 shrink-0">
                          <input type="range" min="0" max="100" step="5" value={globalVolume} onChange={(e) => { const v = Number(e.target.value); if(updateGlobalVolume) updateGlobalVolume(v); updateSetting("rural_global_vol", v); }} className={`w-full min-h-[36px] ${activeTheme?.text || "text-white"} accent-current cursor-pointer`} />
                        </div>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Radio className="w-4 h-4 text-zinc-400 shrink-0"/> Notification Sound Pack</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">System alert tone style.</div>
                        </div>
                        <select value={soundPack} onChange={(e) => updateSetting("rt_sound_pack", e.target.value, setSoundPack)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                           <option value="tactical" className="bg-zinc-900 text-white">Tactical Beeps</option>
                           <option value="soft" className="bg-zinc-900 text-white">Soft Chimes</option>
                           <option value="scifi" className="bg-zinc-900 text-white">Sci-Fi Synths</option>
                           <option value="silent" className="bg-zinc-900 text-white">Silent / Muted</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Mic className="w-4 h-4 text-zinc-400 shrink-0"/> Navigation Voice Type</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Turn-by-turn guidance voice.</div>
                        </div>
                        <select value={navVoice} onChange={(e) => updateSetting("rt_nav_voice", e.target.value, setNavVoice)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                           <option value="AI Female" className="bg-zinc-900 text-white">AI Female</option>
                           <option value="AI Male" className="bg-zinc-900 text-white">AI Male</option>
                           <option value="Robotic" className="bg-zinc-900 text-white">Robotic Synth</option>
                           <option value="Disabled" className="bg-zinc-900 text-white">Disabled</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><VolumeX className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Mute on Video Playback</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Duck comms when viewing clips.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_mute_video", !autoMuteOnVideo, setAutoMuteOnVideo)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoMuteOnVideo ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoMuteOnVideo ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                    </div>
                  </div>
                )}
              </motion.div>
            )}

            {/* ========================================================================= */}
            {/* 3. APP CONFIG TAB (ALL 20+ AVIONICS SETTINGS) */}
            {/* ========================================================================= */}
            {(settingsTab === "app_settings" || settingsSearchQuery) && (
              <motion.div key="app_settings" initial={{ opacity: 0, y: reducedMotion ? 0 : 5 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }} className="space-y-6">
                
                {/* 🔥 20 LIVE AVIONICS FEATURES */}
                {matchesSearch("beep speed offset radar radius ghost copilot persona pressure unit lock orient csv chime map scale elevation gradient board limit slope sos template battery display mode regen motor temp sag school zone blink 20 live avionics features") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase text-cyan-400 tracking-widest mb-2 flex items-center gap-1.5 pl-1"><SlidersHorizontal className="w-4 h-4"/> 20 Live Avionics System Features</h3>
                    <div className="bg-black/50 border border-cyan-500/30 rounded-2xl divide-y divide-white/10 shadow-inner">
                      
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Volume2 className="w-4 h-4 text-zinc-400 shrink-0"/> 1. Speed Limit Beep Frequency</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Audio warning tone pitch.</div>
                        </div>
                        <select value={beepPitch} onChange={(e) => updateSetting("rt_beep_pitch", e.target.value, setBeepPitch)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="450" className="bg-zinc-900 text-white">450 Hz (Deep)</option>
                          <option value="650" className="bg-zinc-900 text-white">650 Hz (Standard)</option>
                          <option value="950" className="bg-zinc-900 text-white">950 Hz (Sharp)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Gauge className="w-4 h-4 text-zinc-400 shrink-0"/> 2. Speed Warning Offset Threshold</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Buffer over limit before alarm triggers.</div>
                        </div>
                        <select value={speedOffset} onChange={(e) => updateSetting("rt_speed_offset", e.target.value, setSpeedOffset)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="2" className="bg-zinc-900 text-white">+2 Over Limit</option>
                          <option value="5" className="bg-zinc-900 text-white">+5 Over Limit</option>
                          <option value="10" className="bg-zinc-900 text-white">+10 Over Limit</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Radio className="w-4 h-4 text-zinc-400 shrink-0"/> 3. Radar Scan Radius Range</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Proximity radar scan distance.</div>
                        </div>
                        <select value={radarRadius} onChange={(e) => updateSetting("radar_scan_radius", e.target.value, setRadarRadius)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="25" className="bg-zinc-900 text-white">25 Miles</option>
                          <option value="50" className="bg-zinc-900 text-white">50 Miles</option>
                          <option value="100" className="bg-zinc-900 text-white">100 Miles</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> 4. Ghost Racer Multiplier</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Pace ghost benchmark speed.</div>
                        </div>
                        <select value={ghostSpeed} onChange={(e) => updateSetting("rt_ghost_speed", e.target.value, setGhostSpeed)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="0.75" className="bg-zinc-900 text-white">0.75x Cruise</option>
                          <option value="1.0" className="bg-zinc-900 text-white">1.0x Parity</option>
                          <option value="1.25" className="bg-zinc-900 text-white">1.25x Push</option>
                          <option value="1.5" className="bg-zinc-900 text-white">1.5x Beast</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><BrainCircuit className="w-4 h-4 text-zinc-400 shrink-0"/> 5. AI Co-Pilot Personality Tone</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Voice briefing persona style.</div>
                        </div>
                        <select value={copilotPersona} onChange={(e) => updateSetting("copilot_persona", e.target.value, setCopilotPersona)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[180px]">
                          <option value="Tactical Coach" className="bg-zinc-900 text-white">Tactical Coach</option>
                          <option value="Mechanic Expert" className="bg-zinc-900 text-white">Mechanic Expert</option>
                          <option value="Friendly Companion" className="bg-zinc-900 text-white">Friendly Companion</option>
                          <option value="Sarcastic Spotter" className="bg-zinc-900 text-white">Sarcastic Spotter</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><CloudRain className="w-4 h-4 text-zinc-400 shrink-0"/> 6. Barometric Pressure Units</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Atmospheric readout format.</div>
                        </div>
                        <select value={pressureUnit} onChange={(e) => updateSetting("rt_pressure_unit", e.target.value, setPressureUnit)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="inHg" className="bg-zinc-900 text-white">Inches Hg</option>
                          <option value="hPa" className="bg-zinc-900 text-white">Hectopascals</option>
                          <option value="mmHg" className="bg-zinc-900 text-white">Millimeters Hg</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Layout className="w-4 h-4 text-zinc-400 shrink-0"/> 7. Force Portrait Lock in Tracking</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Lock screen rotation during GPS rides.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_lock_orient", !lockOrient, setLockOrient)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${lockOrient ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${lockOrient ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><FileText className="w-4 h-4 text-zinc-400 shrink-0"/> 8. Telemetry CSV Export Delimiter</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Separator character for ride log spreadsheets.</div>
                        </div>
                        <select value={csvDelimiter} onChange={(e) => updateSetting("rt_csv_delim", e.target.value, setCsvDelimiter)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="," className="bg-zinc-900 text-white">Comma (,)</option>
                          <option value=";" className="bg-zinc-900 text-white">Semicolon (;)</option>
                          <option value="\t" className="bg-zinc-900 text-white">Tab (\t)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><MessageSquare className="w-4 h-4 text-zinc-400 shrink-0"/> 9. Direct Message Chime Alerts</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Audio chime on incoming secure comms.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_dm_chime", !dmChime, setDmChime)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${dmChime ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${dmChime ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Map className="w-4 h-4 text-zinc-400 shrink-0"/> 10. Map Scale Bar Position</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Leaflet distance scale anchor.</div>
                        </div>
                        <select value={mapScalePos} onChange={(e) => updateSetting("rt_map_scale_pos", e.target.value, setMapScalePos)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="Bottom Left" className="bg-zinc-900 text-white">Bottom Left</option>
                          <option value="Bottom Right" className="bg-zinc-900 text-white">Bottom Right</option>
                          <option value="Top Left" className="bg-zinc-900 text-white">Top Left</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Layers className="w-4 h-4 text-zinc-400 shrink-0"/> 11. Elevation Profile Graph Shading</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Gradient fill beneath ride altitude graphs.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_elevation_gradient", !elevationGradient, setElevationGradient)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${elevationGradient ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${elevationGradient ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Layout className="w-4 h-4 text-zinc-400 shrink-0"/> 12. Community Board Post Load Limit</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Max items rendered in feed view.</div>
                        </div>
                        <select value={boardLimit} onChange={(e) => updateSetting("rt_board_limit", e.target.value, setBoardLimit)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="25" className="bg-zinc-900 text-white">25 Posts</option>
                          <option value="50" className="bg-zinc-900 text-white">50 Posts</option>
                          <option value="100" className="bg-zinc-900 text-white">100 Posts</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> 13. Incline Slope Sensitivity Smoothing</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Grade calculation damping factor.</div>
                        </div>
                        <select value={slopeSmooth} onChange={(e) => updateSetting("rt_slope_smooth", e.target.value, setSlopeSmooth)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="Aggressive" className="bg-zinc-900 text-white">Aggressive</option>
                          <option value="Balanced" className="bg-zinc-900 text-white">Balanced</option>
                          <option value="Damped" className="bg-zinc-900 text-white">Damped</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><ShieldAlert className="w-4 h-4 text-zinc-400 shrink-0"/> 14. Custom SOS Emergency SMS Template</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Custom text payload dispatched during panic mode.</div>
                        </div>
                        <input type="text" value={sosTemplate} onChange={(e) => updateSetting("rt_sos_template", e.target.value, setSosTemplate)} className="w-full max-w-[200px] min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 rounded-xl px-3 py-1.5 text-[10px] font-bold text-white outline-none focus:border-cyan-400 shadow-inner transition-all" />
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> 15. Battery Display Mode</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">HUD readout format.</div>
                        </div>
                        <select value={voltDisplayMode} onChange={(e) => updateSetting("rt_volt_disp_mode", e.target.value, setVoltDisplayMode)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="Percentage" className="bg-zinc-900 text-white">Percentage (%)</option>
                          <option value="Voltage" className="bg-zinc-900 text-white">Actual Voltage (V)</option>
                          <option value="Bars" className="bg-zinc-900 text-white">Simple Bars</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Zap className="w-4 h-4 text-zinc-400 shrink-0"/> 16. Regen Braking Algorithm</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">KERS recovery modeling.</div>
                        </div>
                        <select value={regenAlgorithm} onChange={(e) => updateSetting("rt_regen_algo", e.target.value, setRegenAlgorithm)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="Linear" className="bg-zinc-900 text-white">Linear</option>
                          <option value="Progressive" className="bg-zinc-900 text-white">Progressive</option>
                          <option value="Aggressive" className="bg-zinc-900 text-white">Aggressive</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Thermometer className="w-4 h-4 text-zinc-400 shrink-0"/> 17. Motor Over-Temp Threshold</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Thermal warning limit (°C).</div>
                        </div>
                        <select value={motorTempWarn} onChange={(e) => updateSetting("rt_motor_temp_warn", e.target.value, setMotorTempWarn)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="75" className="bg-zinc-900 text-white">75°C (Conservative)</option>
                          <option value="85" className="bg-zinc-900 text-white">85°C (Standard)</option>
                          <option value="95" className="bg-zinc-900 text-white">95°C (High)</option>
                          <option value="105" className="bg-zinc-900 text-white">105°C (Extreme)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> 18. Voltage Sag Compensation</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Load drop smoothing algorithm.</div>
                        </div>
                        <select value={sagCompensation} onChange={(e) => updateSetting("rt_sag_comp", e.target.value, setSagCompensation)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner">
                          <option value="AI Dynamic" className="bg-zinc-900 text-white">AI Dynamic</option>
                          <option value="Linear" className="bg-zinc-900 text-white">Linear</option>
                          <option value="Raw" className="bg-zinc-900 text-white">Raw (No Comp)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><ShieldAlert className="w-4 h-4 text-zinc-400 shrink-0"/> 19. School Zone Strict Compliance</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Aggressive warnings in school zones.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_school_zone", !schoolZoneStrict, setSchoolZoneStrict)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${schoolZoneStrict ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${schoolZoneStrict ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5"><Bell className="w-4 h-4 text-zinc-400 shrink-0"/> 20. HUD Blink on Top Speed</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Flash display at V-MAX.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_hud_blink", !hudBlinkMax, setHudBlinkMax)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${hudBlinkMax ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${hudBlinkMax ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                    </div>
                  </div>
                )}

                {matchesSearch("telemetry storage retention gps error logging high precision background purge") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-emerald-400 pl-1"><Cpu className="w-4 h-4"/> System Telemetry &amp; Storage</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><HardDrive className="w-4 h-4 text-zinc-400 shrink-0"/> Telemetry Data Retention Policy</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">How long historical rides are saved in local cache.</div>
                        </div>
                        <select value={dataRetentionPolicy} onChange={(e) => updateSetting("rt_data_retention", e.target.value, setDataRetentionPolicy)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="30days" className="bg-zinc-900 text-white">30 Days</option>
                          <option value="1year" className="bg-zinc-900 text-white">1 Year</option>
                          <option value="forever" className="bg-zinc-900 text-white">Forever</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Trash2 className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Purge Old Logs</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Automatically delete stale cache to save memory.</div>
                        </div>
                        <select value={autoClearLogs} onChange={(e) => updateSetting("rt_clear_logs", e.target.value, setAutoClearLogs)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="never" className="bg-zinc-900 text-white">Never</option>
                          <option value=">30 Days" className="bg-zinc-900 text-white">&gt; 30 Days</option>
                          <option value=">90 Days" className="bg-zinc-900 text-white">&gt; 90 Days</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Target className="w-4 h-4 text-zinc-400 shrink-0"/> High-Precision Background GPS</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Request maximum satellite accuracy when app is minimized.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_high_prec_gps", !highPrecisionBgGps, setHighPrecisionBgGps)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${highPrecisionBgGps ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${highPrecisionBgGps ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><AlertTriangle className="w-4 h-4 text-zinc-400 shrink-0"/> Automatic Error Log Diagnostics</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Bundle and log runtime exceptions for review.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_error_log", !autoErrorLogging, setAutoErrorLogging)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoErrorLogging ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoErrorLogging ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("fps unit override distance temperature metric imperial decimal precision") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-cyan-400 pl-1"><Eye className="w-4 h-4"/> Display &amp; HUD Physics</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Max Rendering FPS Limit</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Visual frame rate cap to limit GPU.</div>
                        </div>
                        <select value={maxFps} onChange={(e) => updateSetting("rt_max_fps", e.target.value, setMaxFps)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="30" className="bg-zinc-900 text-white">30 FPS</option>
                          <option value="60" className="bg-zinc-900 text-white">60 FPS</option>
                          <option value="120" className="bg-zinc-900 text-white">120 FPS</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Navigation className="w-4 h-4 text-zinc-400 shrink-0"/> Distance Unit Override</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Force standard output (Imperial/Metric).</div>
                        </div>
                        <select value={distanceUnitOverride} onChange={(e) => handleUnitOverrideChange(e.target.value)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="auto" className="bg-zinc-900 text-white">Sync Locale</option>
                          <option value="mi" className="bg-zinc-900 text-white">Miles (Mi)</option>
                          <option value="km" className="bg-zinc-900 text-white">Kilometers (Km)</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Sun className="w-4 h-4 text-zinc-400 shrink-0"/> Temperature Override</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Force thermal units.</div>
                        </div>
                        <select value={tempUnitOverride} onChange={(e) => updateSetting("rt_temp_unit", e.target.value, setTempUnitOverride)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="auto" className="bg-zinc-900 text-white">Sync Locale</option>
                          <option value="f" className="bg-zinc-900 text-white">Fahrenheit (°F)</option>
                          <option value="c" className="bg-zinc-900 text-white">Celsius (°C)</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Decimal Precision Output</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Rounding limits for speed/distance.</div>
                        </div>
                        <select value={decimalPrecision} onChange={(e) => updateSetting("rt_dec_prec", e.target.value, setDecimalPrecision)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="0" className="bg-zinc-900 text-white">Whole (0)</option>
                          <option value="1" className="bg-zinc-900 text-white">Tenths (0.1)</option>
                          <option value="2" className="bg-zinc-900 text-white">Hundredths (0.01)</option>
                        </select>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("privacy route snapping location zone radius hide map") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-purple-400 pl-1"><MapPin className="w-4 h-4"/> Location Privacy &amp; Routing</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><EyeOff className="w-4 h-4 text-zinc-400 shrink-0"/> Privacy Zone Radius</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Hide start/end points of your rides.</div>
                        </div>
                        <select value={privacyZone} onChange={(e) => updateSetting("rt_privacy_zone", e.target.value, setPrivacyZone)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="0" className="bg-zinc-900 text-white">Disabled</option>
                          <option value="0.25" className="bg-zinc-900 text-white">0.25 Mi</option>
                          <option value="0.5" className="bg-zinc-900 text-white">0.5 Mi</option>
                          <option value="1" className="bg-zinc-900 text-white">1.0 Mi</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Map className="w-4 h-4 text-zinc-400 shrink-0"/> Map Route Snapping</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Align raw GPS tracks to known roads/trails.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_route_snapping", !routeSnapping, setRouteSnapping)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${routeSnapping ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${routeSnapping ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("weather environment atmospheric sync frequency") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-sky-400 pl-1"><CloudRain className="w-4 h-4"/> Environmental &amp; Weather</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><CloudRain className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Sync Weather Telemetry</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Fetch live atmospheric data via GPS.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_weather", !autoWeatherSync, setAutoWeatherSync)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoWeatherSync ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoWeatherSync ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Cloud className="w-4 h-4 text-zinc-400 shrink-0"/> Weather Update Frequency</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Conserve battery by throttling pulls.</div>
                        </div>
                        <select value={weatherUpdateFrequency} onChange={(e) => updateSetting("rt_weather_freq", e.target.value, setWeatherUpdateFrequency)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="15m" className="bg-zinc-900 text-white">15 Mins</option>
                          <option value="30m" className="bg-zinc-900 text-white">30 Mins</option>
                          <option value="1h" className="bg-zinc-900 text-white">1 Hour</option>
                        </select>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("tracking gps drift accuracy leaderboard polling strict start") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-rose-400 pl-1"><Target className="w-4 h-4"/> Telemetry &amp; Tracking Physics</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Shield className="w-4 h-4 text-zinc-400 shrink-0"/> Strict Hardware GPS Lock</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Reject IP/Tower fallback signals (Prevents Eufaula bug).</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_strict_hardware_gps", !strictHardwareGps, setStrictHardwareGps)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${strictHardwareGps ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${strictHardwareGps ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Telemetry UI Polling Rate</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">CPU tick speed for UI updates.</div>
                        </div>
                        <select value={telemetryRate} onChange={(e) => updateSetting("rt_telemetry_rate", Number(e.target.value), setTelemetryRate)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="500" className="bg-zinc-900 text-white">Fast (0.5s)</option>
                          <option value="1000" className="bg-zinc-900 text-white">Normal (1.0s)</option>
                          <option value="2000" className="bg-zinc-900 text-white">Slow (2.0s)</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Target className="w-4 h-4 text-zinc-400 shrink-0"/> Minimum GPS Accuracy Filter</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Reject bad satellite pings over limit.</div>
                        </div>
                        <select value={gpsAccuracyLimit} onChange={(e) => updateSetting("rt_gps_acc_limit", Number(e.target.value), setGpsAccuracyLimit)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="10" className="bg-zinc-900 text-white">10 Meters</option>
                          <option value="20" className="bg-zinc-900 text-white">20 Meters</option>
                          <option value="50" className="bg-zinc-900 text-white">50 Meters</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Navigation className="w-4 h-4 text-zinc-400 shrink-0"/> GPS Drift Tolerance</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Movement distance validation filter.</div>
                        </div>
                        <select value={gpsDriftTol} onChange={(e) => updateSetting("rt_gps_drift", e.target.value, setGpsDriftTol)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="2" className="bg-zinc-900 text-white">Strict (2m)</option>
                          <option value="5" className="bg-zinc-900 text-white">Balanced (5m)</option>
                          <option value="15" className="bg-zinc-900 text-white">Loose (15m)</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Start Telemetry Tracking</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Launch telemetry instantly on boot.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_start_track", !autoStartTracking, setAutoStartTracking)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoStartTracking ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoStartTracking ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Globe className="w-4 h-4 text-zinc-400 shrink-0"/> Share Anonymous Stats to Global Leaderboard</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Opt-in to global rider stats.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_share_leaderboard", !shareLeaderboard, setShareLeaderboard)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${shareLeaderboard ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${shareLeaderboard ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("network cloud sync throttle background timeout") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-blue-400 pl-1"><Radio className="w-4 h-4"/> Network & Cloud Routing</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Cloud className="w-4 h-4 text-zinc-400 shrink-0"/> Cloud Sync Interval</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Background backup timing.</div>
                        </div>
                        <select value={cloudSyncInterval} onChange={(e) => updateSetting("rural_cloud_sync", e.target.value, setCloudSyncInterval)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="instant" className="bg-zinc-900 text-white">Instant</option>
                          <option value="hourly" className="bg-zinc-900 text-white">Hourly</option>
                          <option value="manual" className="bg-zinc-900 text-white">Manual Only</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Clock className="w-4 h-4 text-zinc-400 shrink-0"/> Offline Sync Timeout Limit</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Wait time before defaulting to local cache.</div>
                        </div>
                        <select value={offlineSyncTimeout} onChange={(e) => updateSetting("rt_offline_timeout", e.target.value, setOfflineSyncTimeout)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="10s" className="bg-zinc-900 text-white">10 Secs</option>
                          <option value="30s" className="bg-zinc-900 text-white">30 Secs</option>
                          <option value="60s" className="bg-zinc-900 text-white">60 Secs</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Background Network Throttling</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Limit bandwidth usage when minimized.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_bg_throttle", !bgNetThrottle, setBgNetThrottle)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${bgNetThrottle ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${bgNetThrottle ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><MapPin className="w-4 h-4 text-zinc-400 shrink-0"/> Background Location Services</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Run tracking when app is minimized.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rural_bg_gps", !backgroundGps, setBackgroundGps)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${backgroundGps ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${backgroundGps ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("resource storage power save cache prefetch aggressive max") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-amber-500 pl-1"><HardDrive className="w-4 h-4"/> Resource Management & Storage</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Zap className="w-4 h-4 text-zinc-400 shrink-0"/> Aggressive Power Save</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Halt non-essential tracking when battery &lt; 20%.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_power_threshold", !powerSaveThreshold, setPowerSaveThreshold)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${powerSaveThreshold ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${powerSaveThreshold ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><HardDrive className="w-4 h-4 text-zinc-400 shrink-0"/> Max Storage Cache Limit</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Cap local database size.</div>
                        </div>
                        <select value={maxCacheLimit} onChange={(e) => updateSetting("rt_max_cache", e.target.value, setMaxCacheLimit)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="50" className="bg-zinc-900 text-white">50 MB</option>
                          <option value="100" className="bg-zinc-900 text-white">100 MB</option>
                          <option value="500" className="bg-zinc-900 text-white">500 MB</option>
                          <option value="unlimited" className="bg-zinc-900 text-white">Unlimited</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Map className="w-4 h-4 text-zinc-400 shrink-0"/> Offline Map Pre-fetching</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Download local tiles over Wi-Fi automatically.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_offline_prefetch", !offlinePrefetch, setOfflinePrefetch)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${offlinePrefetch ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${offlinePrefetch ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("safety sos battery crash biometric lock emergency chemistry voice hotword fatigue") && (
                  <div className="space-y-3">
                    <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-1.5 mb-2 text-rose-500 pl-1"><ShieldAlert className="w-4 h-4"/> Safety, SOS & Battery Modeling</h3>
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Shield className="w-4 h-4 text-zinc-400 shrink-0"/> Native Biometric App Lock</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Require FaceID/Fingerprint to open app.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_biometric_lock", !biometricAppLock, setBiometricAppLock)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${biometricAppLock ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${biometricAppLock ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><ShieldAlert className="w-4 h-4 text-zinc-400 shrink-0"/> SOS Dispatch Config</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Contact Name & Number.</div>
                        </div>
                        <div className="flex flex-col gap-1.5 w-[140px]">
                          <input 
                            type="text" 
                            value={emergencyContactName} 
                            onChange={(e) => updateSetting("rt_sos_name", e.target.value, setEmergencyContactName)} 
                            placeholder="Contact Name" 
                            className="w-full min-h-[36px] bg-black/60 border border-white/20 hover:border-white/40 rounded-lg px-2.5 py-1 text-[10px] font-bold text-white outline-none focus:border-rose-400 shadow-inner transition-all" 
                          />
                          <input 
                            type="tel" 
                            value={sosContactNumber} 
                            onChange={(e) => updateSetting("rural_sos_number", e.target.value, setSosContactNumber)} 
                            placeholder="Phone Number" 
                            className="w-full min-h-[36px] bg-black/60 border border-white/20 hover:border-white/40 rounded-lg px-2.5 py-1 text-[10px] font-bold text-white outline-none focus:border-rose-400 font-mono shadow-inner transition-all" 
                          />
                        </div>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Clock className="w-4 h-4 text-zinc-400 shrink-0"/> SOS Cancel Window</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Time before auto-dispatch.</div>
                        </div>
                        <select value={sosCancelWindow} onChange={(e) => updateSetting("rt_sos_window", e.target.value, setSosCancelWindow)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="5" className="bg-zinc-900 text-white">5s</option>
                          <option value="10" className="bg-zinc-900 text-white">10s</option>
                          <option value="15" className="bg-zinc-900 text-white">15s</option>
                          <option value="30" className="bg-zinc-900 text-white">30s</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Crash Detection Sensitivity</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">G-Force limits for SOS Trigger.</div>
                        </div>
                        <select value={crashSensitivity} onChange={(e) => updateSetting("rt_crash_sens", e.target.value, setCrashSensitivity)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="high" className="bg-zinc-900 text-white">High (Minor Spills)</option>
                          <option value="normal" className="bg-zinc-900 text-white">Normal</option>
                          <option value="low" className="bg-zinc-900 text-white">Low (Severe Impact)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Zap className="w-4 h-4 text-zinc-400 shrink-0"/> Battery Chemistry Profile</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Adjusts voltage sag curve predictions.</div>
                        </div>
                        <select value={batteryProfile} onChange={(e) => updateSetting("rt_batt_profile", e.target.value, setBatteryProfile)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="Li-Ion" className="bg-zinc-900 text-white">Li-Ion (Standard)</option>
                          <option value="LiFePO4" className="bg-zinc-900 text-white">LiFePO4 (Flat Curve)</option>
                          <option value="Solid State" className="bg-zinc-900 text-white">Solid State (High Drain)</option>
                        </select>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Mic className="w-4 h-4 text-zinc-400 shrink-0"/> Hands-Free Voice Hotword</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Background listening for Co-Pilot.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rural_voice_hotword", !voiceHotwordActive, setVoiceHotwordActive)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${voiceHotwordActive ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${voiceHotwordActive ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>

                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Rider Fatigue Monitor Alerts</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Alerts after extended riding times.</div>
                        </div>
                        <select value={fatigueAlert} onChange={(e) => updateSetting("rt_fatigue_alert", e.target.value, setFatigueAlert)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="None" className="bg-zinc-900 text-white">Disabled</option>
                          <option value="1 Hour" className="bg-zinc-900 text-white">Every 1 Hour</option>
                          <option value="2 Hours" className="bg-zinc-900 text-white">Every 2 Hours</option>
                        </select>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("advanced logic ai scrape encrypt imu gyro tile provider") && (
                  <div className="space-y-3">
                    <SectionTitle icon={BrainCircuit} title="Advanced System Logic" color="text-purple-400" />
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><BrainCircuit className="w-4 h-4 text-zinc-400 shrink-0"/> Smart Battery AI</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">AI dynamically manages GPS polling to save battery.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_smart_batt_ai", !smartBatteryAi, setSmartBatteryAi)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${smartBatteryAi ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${smartBatteryAi ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Search className="w-4 h-4 text-zinc-400 shrink-0"/> Automated Error Code Scraping</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Auto-queries web manuals if fault is detected.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_err_scrape", !autoErrorScrape, setAutoErrorScrape)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoErrorScrape ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoErrorScrape ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><ShieldCheck className="w-4 h-4 text-zinc-400 shrink-0"/> Local Cache Encryption</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Encrypt offline logs and telemetry data.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_local_encrypt", !localEncryption, setLocalEncryption)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${localEncryption ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${localEncryption ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> IMU Gyro Polling Rate</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Frequency of internal accelerometer checks.</div>
                        </div>
                        <select value={imuPollingRate} onChange={(e) => updateSetting("rt_imu_poll", e.target.value, setImuPollingRate)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="20hz" className="bg-zinc-900 text-white">20 Hz (Eco)</option>
                          <option value="50hz" className="bg-zinc-900 text-white">50 Hz (Standard)</option>
                          <option value="100hz" className="bg-zinc-900 text-white">100 Hz (Aggressive)</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Map className="w-4 h-4 text-zinc-400 shrink-0"/> Offline Map Tile Provider</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Source imagery for map cache.</div>
                        </div>
                        <select value={offlineTileProvider} onChange={(e) => updateSetting("rt_off_tile", e.target.value, setOfflineTileProvider)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="carto_dark" className="bg-zinc-900 text-white">CartoDB Dark</option>
                          <option value="osm" className="bg-zinc-900 text-white">OpenStreet</option>
                          <option value="satellite" className="bg-zinc-900 text-white">Satellite High-Res</option>
                        </select>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("developer diagnostics wake lock hardware accel auto-sleep debug render boot sequence") && (
                  <div className="space-y-3">
                    <SectionTitle icon={Terminal} title="Advanced / Developer Diagnostics" color="text-cyan-500" />
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-amber-400 flex items-center gap-1.5 mb-1"><Sun className="w-4 h-4 text-zinc-400 shrink-0"/> Hardware Screen Wake-Lock</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Native device screen timeout override.</div>
                        </div>
                        <button type="button" onClick={() => { if(toggleWakeLock) toggleWakeLock(); updateSetting("rural_wakelock", !wakeLockEnabled); }} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${wakeLockEnabled ? "bg-amber-500 shadow-[0_0_12px_rgba(245,158,11,0.4)]" : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${wakeLockEnabled ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Clock className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Sleep GPS Throttle</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Throttle polling to save battery when idle.</div>
                        </div>
                        <button type="button" onClick={() => { if(toggleAutoSleep) toggleAutoSleep(); updateSetting("rural_autosleep", !autoSleep); }} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoSleep ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoSleep ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Cpu className="w-4 h-4 text-zinc-400 shrink-0"/> Hardware Render Acceleration</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Use WebGL/GPU for heavy components.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_hw_accel", !hardwareAcceleration, setHardwareAcceleration)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${hardwareAcceleration ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${hardwareAcceleration ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><RefreshCw className="w-4 h-4 text-zinc-400 shrink-0"/> Diagnostic Auto-Scan</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Scan hardware components on app boot.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_diag_autoscan", !diagnosticAutoScan, setDiagnosticAutoScan)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${diagnosticAutoScan ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${diagnosticAutoScan ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><CloudDownload className="w-4 h-4 text-zinc-400 shrink-0"/> Auto-Update Repository Checks</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Ping GitHub API for latest releases.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_auto_update", !autoUpdateChecks, setAutoUpdateChecks)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${autoUpdateChecks ? `${activeTheme?.bg || "bg-white"} shadow-[0_0_12px_rgba(255,255,255,0.2)]` : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${autoUpdateChecks ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-amber-400 flex items-center gap-1.5 mb-1"><Terminal className="w-4 h-4 text-zinc-400 shrink-0"/> Developer Debug Mode</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Expose unhandled exceptions &amp; raw logs.</div>
                        </div>
                        <button type="button" onClick={() => updateSetting("rt_debug_mode", !debugMode, setDebugMode)} className={`relative inline-flex h-7 w-12 items-center rounded-full transition-all duration-300 cursor-pointer ${debugMode ? "bg-amber-500 shadow-[0_0_12px_rgba(245,158,11,0.4)]" : "bg-black/80 border border-white/20"}`}>
                          <motion.span layout transition={{ type: "spring", stiffness: 500, damping: 30 }} className={`inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform duration-300 ${debugMode ? "translate-x-6" : "translate-x-1"}`} />
                        </button>
                      </div>
                    </div>
                  </div>
                )}
                
              </motion.div>
            )}

            {/* ========================================================================= */}
            {/* 4. APPEARANCE TAB */}
            {/* ========================================================================= */}
            {(settingsTab === "appearance" || settingsSearchQuery) && (
              <motion.div key="appearance" initial={{ opacity: 0, y: reducedMotion ? 0 : 5 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }} className="space-y-6">
                
                {matchesSearch("theme color texture dark night primary base") && (
                  <>
                    <div className="space-y-3">
                      <SectionTitle icon={Palette} title="Primary Base Themes" />
                      <div className="grid grid-cols-3 sm:grid-cols-4 gap-2.5">
                        {[
                          { id: "lime", label: "Neon Lime", color: "bg-[#39ff14]" },
                          { id: "cyan", label: "Cyber Cyan", color: "bg-cyan-500" },
                          { id: "emerald", label: "Emerald City", color: "bg-emerald-500" },
                          { id: "amber", label: "Warning Amber", color: "bg-amber-500" },
                          { id: "rose", label: "Danger Rose", color: "bg-rose-500" },
                          { id: "purple", label: "Synth Purple", color: "bg-purple-500" },
                          { id: "void", label: "Void Black", color: "bg-zinc-800 border border-zinc-600" }
                        ].map(themeItem => (
                          <button 
                            key={themeItem.id} type="button" 
                            onClick={() => { if(changeTheme) changeTheme(themeItem.id); updateSetting("rural_theme", themeItem.id); updateSetting("rt_theme", themeItem.id); window.dispatchEvent(new Event('theme-sync')); }} 
                            className={`flex flex-col items-center justify-center gap-2 p-3 rounded-2xl border transition-all text-left shadow-inner ${
                              brandTheme === themeItem.id ? `bg-white/10 border-white/30 scale-[1.02] shadow-[0_0_15px_${activeTheme?.hex || '#ffffff'}]` : "bg-black/40 border-white/5 hover:border-white/20 hover:bg-white/5"
                            }`}
                          >
                            <div className={`w-5 h-5 rounded-full ${themeItem.color} shrink-0 shadow-sm`} />
                            <span className={`text-[9px] font-bold uppercase truncate ${brandTheme === themeItem.id ? "text-white" : "text-zinc-400"}`}>{themeItem.label}</span>
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="space-y-3">
                      <SectionTitle icon={Sun} title="Auto Night-Mode Schedule" />
                      <div className="grid grid-cols-3 gap-2">
                         {["system", "always_dark", "time_based"].map((mode) => (
                           <button 
                             key={mode} type="button" 
                             onClick={() => updateSetting("rt_night_mode_schedule", mode, setNightModeSchedule)} 
                             className={`min-h-[40px] p-2 text-[9px] font-black uppercase rounded-xl transition-all cursor-pointer shadow-inner ${nightModeSchedule === mode ? `${activeTheme?.bg || "bg-white"} text-black border-transparent shadow-md` : "bg-black/50 text-zinc-400 border border-white/10 hover:text-white hover:bg-white/10"}`}
                           >
                             {mode.replace('_', ' ')}
                           </button>
                         ))}
                      </div>
                    </div>
                  </>
                )}

                {matchesSearch("corner style border panel width elevation raised flat") && (
                  <div className="space-y-3">
                    <SectionTitle icon={Layout} title="Component Border Styling" />
                    <div className="grid grid-cols-3 gap-2">
                       {[ { id: "rounded-none", name: "Sharp Box" }, { id: "rounded-xl", name: "Soft Curve" }, { id: "rounded-3xl", name: "Pill Edge" } ].map((c) => (
                         <button 
                           key={c.id} type="button" 
                           onClick={() => updateSetting("rt_corner_style", c.id, setCornerStyle)} 
                           className={`min-h-[44px] p-2 text-[9px] font-black uppercase ${c.id} transition-all cursor-pointer shadow-inner ${cornerStyle === c.id ? `${activeTheme?.bg || "bg-white"} text-black border-transparent shadow-md` : "bg-black/50 text-zinc-400 border border-white/10 hover:text-white hover:bg-white/10"}`}
                         >
                           {c.name}
                         </button>
                       ))}
                    </div>
                  </div>
                )}

                {matchesSearch("appearance structural speed transition floating border header") && (
                  <div className="space-y-3">
                    <SectionTitle icon={Layers} title="Advanced Structural Appearance" color="text-amber-400" />
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layout className="w-4 h-4 text-zinc-400 shrink-0"/> Panel Border Width</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Stroke thickness for cards & modals.</div>
                        </div>
                        <select value={panelBorderWidth} onChange={(e) => updateSetting("rt_panel_border_width", e.target.value, setPanelBorderWidth)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="thin" className="bg-zinc-900 text-white">Thin (1px)</option>
                          <option value="normal" className="bg-zinc-900 text-white">Normal (2px)</option>
                          <option value="thick" className="bg-zinc-900 text-white">Thick (3px)</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layers className="w-4 h-4 text-zinc-400 shrink-0"/> Card Elevation Style</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Shadow depth and elevation lighting.</div>
                        </div>
                        <select value={cardElevation} onChange={(e) => updateSetting("rt_card_elevation", e.target.value, setCardElevation)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="flat" className="bg-zinc-900 text-white">Flat Minimal</option>
                          <option value="raised" className="bg-zinc-900 text-white">Raised Shadow</option>
                          <option value="floating" className="bg-zinc-900 text-white">Floating 3D</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Activity className="w-4 h-4 text-zinc-400 shrink-0"/> Transition Speed Profile</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Animation timing curve preset.</div>
                        </div>
                        <select value={transitionSpeedProfile} onChange={(e) => updateSetting("rt_transition_speed", e.target.value, setTransitionSpeedProfile)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="instant" className="bg-zinc-900 text-white">Instant Snappy</option>
                          <option value="smooth" className="bg-zinc-900 text-white">Smooth Fluid</option>
                          <option value="cinematic" className="bg-zinc-900 text-white">Cinematic Slow</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layout className="w-4 h-4 text-zinc-400 shrink-0"/> Header Style Variant</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Top navigation bar visual layout.</div>
                        </div>
                        <select value={headerStyleVariant} onChange={(e) => updateSetting("rt_header_style", e.target.value, setHeaderStyleVariant)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="solid" className="bg-zinc-900 text-white">Solid Opaque</option>
                          <option value="translucent" className="bg-zinc-900 text-white">Translucent Glass</option>
                          <option value="floating" className="bg-zinc-900 text-white">Floating Island</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Sun className="w-4 h-4 text-zinc-400 shrink-0"/> Border Glow Intensity</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Ambient aura strength around active elements.</div>
                        </div>
                        <select value={borderGlowIntensity} onChange={(e) => updateSetting("rt_border_glow", e.target.value, setBorderGlowIntensity)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                          <option value="none" className="bg-zinc-900 text-white">Disabled</option>
                          <option value="subtle" className="bg-zinc-900 text-white">Subtle Aura</option>
                          <option value="intense" className="bg-zinc-900 text-white">Intense Glow</option>
                        </select>
                      </div>
                    </div>
                  </div>
                )}

                {matchesSearch("texture background image overlay grid asphalt forest") && (
                  <div className="space-y-3">
                    <SectionTitle icon={ImageIcon} title="Background Texture Overlays (18 Standout Options)" />
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2.5">
                      {BACKGROUND_OPTIONS.map((tex) => {
                        const IconComp = tex.icon;
                        const isSelected = bgTexture === tex.id;
                        return (
                          <button
                            key={tex.id} type="button" 
                            onClick={() => updateSetting("rt_bg_texture", tex.id, setBgTexture)}
                            className={`min-h-[64px] px-2.5 py-2 border text-[9px] font-black uppercase tracking-wider rounded-2xl transition-all shadow-inner cursor-pointer flex flex-col items-center justify-center gap-1.5 ${
                              isSelected 
                                ? `${activeTheme?.bg || "bg-white"} text-black border-transparent shadow-md scale-[1.02]` 
                                : "bg-black/50 text-zinc-400 border-white/10 hover:text-white hover:bg-white/10"
                            }`}
                            title={tex.description}
                          >
                            <IconComp className="w-4 h-4 shrink-0" />
                            <span className="truncate w-full text-center">{tex.label}</span>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}

                {matchesSearch("visual effects glow radius line color button styling glassmorphism blur") && (
                  <div className="space-y-3">
                    <SectionTitle icon={ShieldOff} title="Advanced Visual Effects" color="text-fuchsia-400" />
                    <div className="bg-black/50 border border-white/10 rounded-2xl divide-y divide-white/5 shadow-inner">
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-t-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Sun className="w-4 h-4 text-zinc-400 shrink-0"/> Accent Glow Radius</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Intensity of drop-shadows globally.</div>
                        </div>
                        <select value={glowRadius} onChange={(e) => updateSetting("rt_glow_rad", e.target.value, setGlowRadius)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                           <option value="none" className="bg-zinc-900 text-white">None</option>
                           <option value="small" className="bg-zinc-900 text-white">Small</option>
                           <option value="medium" className="bg-zinc-900 text-white">Medium</option>
                           <option value="massive" className="bg-zinc-900 text-white">Massive</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Map className="w-4 h-4 text-zinc-400 shrink-0"/> Custom Map Line Color</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Routing line path visualizer.</div>
                        </div>
                        <select value={mapLineColor} onChange={(e) => updateSetting("rt_map_line", e.target.value, setMapLineColor)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                           <option value="theme" className="bg-zinc-900 text-white">Sync w/ Theme</option>
                           <option value="pink" className="bg-zinc-900 text-white">Neon Pink</option>
                           <option value="green" className="bg-zinc-900 text-white">Acid Green</option>
                           <option value="blue" className="bg-zinc-900 text-white">Electric Blue</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layout className="w-4 h-4 text-zinc-400 shrink-0"/> UI Button Styling</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Depth logic for interactables.</div>
                        </div>
                        <select value={buttonStyling} onChange={(e) => updateSetting("rt_btn_style", e.target.value, setButtonStyling)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                           <option value="flat" className="bg-zinc-900 text-white">Modern Flat</option>
                           <option value="neumorphic" className="bg-zinc-900 text-white">Neumorphic</option>
                           <option value="outlined" className="bg-zinc-900 text-white">Hollow / Outlined</option>
                        </select>
                      </div>
                      <div className="flex items-center justify-between p-4 gap-4 hover:bg-white/[0.02] transition-colors rounded-b-2xl">
                        <div>
                          <div className="text-xs font-bold text-white flex items-center gap-1.5 mb-1"><Layers className="w-4 h-4 text-zinc-400 shrink-0"/> Glassmorphism Intensity</div>
                          <div className="text-[9px] text-zinc-400 font-mono mt-1">Control backdrop blur levels.</div>
                        </div>
                        <select value={glassmorphism} onChange={(e) => updateSetting("rt_glassmorphism", e.target.value, setGlassmorphism)} className="min-h-[44px] bg-black/60 border border-white/20 hover:border-white/40 text-white rounded-xl px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider focus:outline-none transition-all cursor-pointer shadow-inner max-w-[140px]">
                           <option value="high" className="bg-zinc-900 text-white">High Blur</option>
                           <option value="medium" className="bg-zinc-900 text-white">Medium Blur</option>
                           <option value="low" className="bg-zinc-900 text-white">Low Blur</option>
                           <option value="none" className="bg-zinc-900 text-white">Disabled</option>
                        </select>
                      </div>
                    </div>
                  </div>
                )}

              </motion.div>
            )}
            </AnimatePresence>
          </div>

          {/* FOOTER */}
          <div className="pt-4 border-t border-white/10 mt-auto flex items-center justify-between shrink-0 text-[10px] text-zinc-500 font-mono">
            <span className="flex items-center gap-1.5 font-bold">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> MATRIX SYNCED
            </span>
            <button 
              type="button" 
              onClick={() => { safeHaptic(); setIsSettingsOpen(false); }} 
              className={`min-h-[44px] px-6 rounded-xl text-black font-black uppercase text-xs tracking-wider transition-all active:scale-95 ${activeTheme?.bg || "bg-white"} hover:brightness-110 shadow-[0_0_15px_rgba(255,255,255,0.2)] cursor-pointer`}
            >
              Apply &amp; Close
            </button>
          </div>

        </motion.div>
      </div>
    </AnimatePresence>
  );
}

// ============================================================================
// 🔥 3. EXPORT WRAPPED IN ERROR BOUNDARY TO PREVENT FATAL APP CRASHES
// ============================================================================
export default function SettingsModal(props: any) {
  return (
    <SettingsErrorBoundary>
      <SettingsModalCore {...props} />
    </SettingsErrorBoundary>
  );
}