import React, { useState } from "react";
import { 
  Globe, CircleGauge, ShieldAlert as ShieldAlertIcon, Palette, Sun, EyeOff, 
  SunMoon, Smartphone, Unlock, RotateCw, Crosshair, Scale, Magnet, Activity, 
  Disc, Network, Wifi, BellRing, VolumeX, EyeIcon, Radar, Battery, MapPin,
  Sliders, Volume2, Shield, Flame, Zap, Gauge, RefreshCw, Cpu, Radio,
  Sparkles, CheckCircle2, ChevronRight, SlidersHorizontal, Settings2,
  GaugeCircle, Vibrate, AlertTriangle, Compass, ShieldCheck, Wrench, BatteryCharging, Eye, LayoutGrid, BarChart3,
  Type, Lock, SmartphoneNfc, CompassIcon, Sparkle, Thermometer, ArrowUpDown, Clock, ZapOff
} from "lucide-react";

const safeNum = (val: any, fallback = 0): number => {
  const parsed = Number(val);
  return isNaN(parsed) || !isFinite(parsed) ? fallback : parsed;
};

export interface SettingsMatrixProps {
  showSettings: boolean;
  configTab: "display" | "sensors" | "preferences" | "audio" | "power" | "gps";
  setConfigTab: (tab: any) => void;
  triggerHaptic?: () => void;
  hudStyle: "digital" | "analog" | "minimalist" | "bar" | "cyber" | "aviation" | "orb";
  setHudStyle: (style: any) => void;
  speedSignStyle: "us" | "eu" | "minimal";
  setSpeedSignStyle: (style: any) => void;
  hudFontColor: string;
  setHudFontColor: (color: string) => void;
  theme: string;
  handleThemeChange: (theme: any) => void;
  isDayMode: boolean;
  toggleSunVisibility: () => void;
  isNightVision: boolean;
  setIsNightVision: (val: boolean) => void;
  setIsDayMode: (val: boolean) => void;
  autoNightMode: boolean;
  setAutoNightMode: (val: boolean) => void;
  sensorsUnlocked: boolean;
  requestSensorPermissions: () => void;
  getCalibratedLean: () => number;
  pitchAngle: number;
  gyroOffset: { pitch: number; lean: number };
  hardwareGForce: number;
  ambientLux: number;
  magBaseline: number;
  calibrateEMF: () => void;
  magAmplifier: number;
  setMagAmplifier: (val: number) => void;
  magFluxDelta: number;
  calibrateIMU: () => void;
  brakeSensitivity: number;
  setBrakeSensitivity: (val: number) => void;
  tireFriction: string;
  setTireFriction: (val: string) => void;
  networkType: string;
  networkPing: number;
  useMetric: boolean;
  setUseMetric: (val: boolean) => void;
  audioWarnings: boolean;
  setAudioWarnings: (val: boolean) => void;
  audioThrottleInterval: number;
  setAudioThrottleInterval: (val: number) => void;
  visualGForceAlerts: boolean;
  setVisualGForceAlerts: (val: boolean) => void;
  gpsSmoothing: "Loose" | "Balanced" | "Strict";
  setGpsSmoothing: (val: any) => void;
  telemetryRate: number;
  setTelemetryRate: (val: number) => void;
  speedLimitPolling: "Aggressive" | "Balanced" | "Eco";
  setSpeedLimitPolling: (val: any) => void;
  autoWakeSpeed: number;
  setAutoWakeSpeed: (val: number) => void;
  speedLabel: string;
  batteryChemistry: string;
  setBatteryChemistry: (val: string) => void;
  fallbackInput: string;
  setFallbackInput: (val: string) => void;
  manuallySetFallback: () => void;
  ui: any;
  tx: any;
}

export default function SettingsMatrix(props: SettingsMatrixProps) {
  const {
    showSettings, configTab = "display", setConfigTab, triggerHaptic = () => {}, hudStyle = "digital", setHudStyle,
    speedSignStyle = "us", setSpeedSignStyle, hudFontColor = "#39ff14", setHudFontColor, theme = "rural",
    handleThemeChange, isDayMode = false, toggleSunVisibility, isNightVision = false, setIsNightVision,
    setIsDayMode, autoNightMode = false, setAutoNightMode, sensorsUnlocked = false, requestSensorPermissions,
    getCalibratedLean = () => 0, pitchAngle = 0, gyroOffset = { pitch: 0, lean: 0 }, hardwareGForce = 0, ambientLux = 0, magBaseline = 0,
    calibrateEMF, magAmplifier = 15, setMagAmplifier, magFluxDelta = 0, calibrateIMU,
    brakeSensitivity = 3.5, setBrakeSensitivity, tireFriction = "Street", setTireFriction, networkType = "Offline",
    networkPing = 0, useMetric = false, setUseMetric, audioWarnings = true, setAudioWarnings,
    audioThrottleInterval = 3000, setAudioThrottleInterval, visualGForceAlerts = true, setVisualGForceAlerts,
    gpsSmoothing = "Balanced", setGpsSmoothing, telemetryRate = 1000, setTelemetryRate, speedLimitPolling = "Aggressive",
    setSpeedLimitPolling, autoWakeSpeed = 10, setAutoWakeSpeed, speedLabel = "MPH", batteryChemistry = "Li-Ion",
    setBatteryChemistry, fallbackInput = "", setFallbackInput, manuallySetFallback, ui, tx
  } = props;

  // --- STANDARD DASHBOARD CONTROL STATES ---
  const [hudOpacity, setHudOpacity] = useState<number>(95);
  const [dashboardLayoutDensity, setDashboardLayoutDensity] = useState<"Compact" | "Standard" | "Expanded">("Standard");
  const [showTelemetryGraphs, setShowTelemetryGraphs] = useState<boolean>(true);
  const [speedLimitOverlay, setSpeedLimitOverlay] = useState<boolean>(true);
  const [batteryReadoutMode, setBatteryReadoutMode] = useState<"Voltage" | "Percentage" | "Bars" | "WattHours">("Voltage");
  const [speedWarningOffset, setSpeedWarningOffset] = useState<number>(5);
  const [keepScreenAwake, setKeepScreenAwake] = useState<boolean>(true);
  const [gForceSensitivity, setGForceSensitivity] = useState<string>("Standard");
  const [customFontWeight, setCustomFontWeight] = useState<string>("Black");

  // --- ADDITIONAL DASHBOARD SETTINGS ---
  const [showMiniCompass, setShowMiniCompass] = useState<boolean>(true);
  const [speedAlertVibration, setSpeedAlertVibration] = useState<boolean>(true);
  const [hudBlinkOnMaxSpeed, setHudBlinkOnMaxSpeed] = useState<boolean>(true);
  const [maxSpeedClamp, setMaxSpeedClamp] = useState<number>(45);
  const [pitchRollInversion, setPitchRollInversion] = useState<boolean>(false);
  const [elevationUnit, setElevationUnit] = useState<"Feet" | "Meters">("Feet");
  const [speedometerDamping, setSpeedometerDamping] = useState<string>("Smooth");
  const [tripOdometerAutoReset, setTripOdometerAutoReset] = useState<string>("24 Hours");
  const [hudBackgroundStyle, setHudBackgroundStyle] = useState<string>("Carbon Weave");
  const [accelerationUnit, setAccelerationUnit] = useState<string>("G-Force");
  const [temperatureUnit, setTemperatureUnit] = useState<string>("Fahrenheit (°F)");
  const [quickLaunchShortcut, setQuickLaunchShortcut] = useState<string>("AI Copilot");
  const [autoRecordLock, setAutoRecordLock] = useState<boolean>(true);
  const [hudGlowIntensity, setHudGlowIntensity] = useState<string>("Intense");

  if (!showSettings) return null;

  return (
    <div className={`p-3.5 sm:p-6 rounded-3xl border shadow-2xl space-y-4 ${ui?.bgPanel || 'bg-[#0d0e15]'} ${ui?.brd || 'border-zinc-800'}`}>
      
      {/* HEADER BAR */}
      <div className={`flex flex-col sm:flex-row items-start sm:items-center justify-between border-b ${ui?.brd || 'border-zinc-800'} pb-3 gap-2.5`}>
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-2xl bg-[#39ff14]/10 border border-[#39ff14]/30 text-[#39ff14] shadow-md shrink-0">
            <Settings2 className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <h3 className={`${ui?.txtMain || 'text-white'} font-black uppercase tracking-widest text-xs sm:text-sm flex items-center gap-2`}>
              Master Dashboard Control Matrix Pro
            </h3>
            <p className={`text-[8px] sm:text-[8.5px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase mt-0.5`}>
              HUD Layouts, Telemetry Polling, Visual Density &amp; Avionics
            </p>
          </div>
        </div>
      </div>
      
      {/* NAVIGATION TABS (Mobile Optimized 44px+ Touch Targets) */}
      <div className="flex gap-1.5 overflow-x-auto custom-scrollbar pb-1 -mx-1 px-1 touch-pan-x">
        {[
          { id: "display", label: "Display & HUD", icon: CircleGauge },
          { id: "sensors", label: "IMU & Lean", icon: Smartphone },
          { id: "gps", label: "GPS & Polling", icon: Radar },
          { id: "audio", label: "Audio & Alerts", icon: BellRing },
          { id: "preferences", label: "Dashboard Prefs", icon: SlidersHorizontal },
        ].map((tab) => (
          <button 
            key={tab.id}
            type="button" 
            onClick={() => { triggerHaptic?.(); setConfigTab(tab.id); }} 
            className={`min-h-[44px] px-3.5 rounded-xl text-[9.5px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 shrink-0 cursor-pointer active:scale-95 touch-manipulation ${configTab === tab.id ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-md ring-1 ring-white/20` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} border ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
          >
            <tab.icon className="w-3.5 h-3.5 shrink-0" /> {tab.label}
          </button>
        ))}
      </div>

      {/* --- TAB 1: DISPLAY & HUD STYLING --- */}
      {configTab === "display" && (
        <div className="space-y-3.5">
          
          {/* Speedometer Layout Selector */}
          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5`}>
            <div className="flex flex-col space-y-0.5">
              <span className={`text-[9.5px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <CircleGauge className="w-3.5 h-3.5"/> Speed HUD Layout Style
              </span>
              <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>
                Choose your real-time cockpit velocity render layout
              </span>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-1.5">
              {(["digital", "analog", "minimalist", "bar", "cyber", "aviation", "orb"] as const).map(style => (
                <button 
                  key={style} 
                  onClick={() => { triggerHaptic?.(); setHudStyle(style); }}
                  className={`min-h-[44px] px-2.5 py-2 rounded-xl text-[9.5px] font-mono font-black uppercase tracking-wider border transition-all cursor-pointer active:scale-95 touch-manipulation flex items-center justify-center ${hudStyle === style ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black border-transparent shadow-md` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.txtMuted || 'text-zinc-400'} ${ui?.brd || 'border-zinc-800'}`}`}
                >
                  {style}
                </button>
              ))}
            </div>
          </div>

          {/* NEW OPTION: Speed Digits Color Picker & Presets */}
          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5`}>
            <div className="flex justify-between items-center">
              <span className={`text-[9.5px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Type className="w-3.5 h-3.5 text-cyan-400"/> Speed Digits Color Customizer
              </span>
              <div className="flex items-center gap-2">
                <span className="text-xs font-mono font-bold" style={{ color: hudFontColor || "#39ff14" }}>
                  {hudFontColor || "#39ff14"}
                </span>
                <input 
                  type="color" 
                  value={hudFontColor || "#39ff14"} 
                  onChange={(e) => { triggerHaptic?.(); setHudFontColor(e.target.value); }}
                  className="w-8 h-8 rounded-lg bg-transparent cursor-pointer border border-zinc-700 p-0"
                  title="Pick custom speed font color"
                />
              </div>
            </div>
            <div className="flex gap-1.5 overflow-x-auto custom-scrollbar pb-1">
              {[
                { name: 'Neon Green', hex: '#39ff14' },
                { name: 'Electric Cyan', hex: '#06b6d4' },
                { name: 'Thermal Amber', hex: '#f59e0b' },
                { name: 'Laser Rose', hex: '#f43f5e' },
                { name: 'Pure White', hex: '#ffffff' },
                { name: 'Ultra Violet', hex: '#a855f7' }
              ].map(colorPreset => (
                <button
                  key={colorPreset.hex}
                  onClick={() => { triggerHaptic?.(); setHudFontColor(colorPreset.hex); }}
                  className={`min-h-[38px] px-3 rounded-xl text-[9px] font-mono font-bold uppercase border transition-all cursor-pointer flex items-center gap-1.5 shrink-0 ${hudFontColor === colorPreset.hex ? 'border-white bg-white/10 text-white font-black' : 'border-zinc-800 bg-black/40 text-zinc-400'}`}
                >
                  <span className="w-3 h-3 rounded-full shrink-0" style={{ backgroundColor: colorPreset.hex }} />
                  {colorPreset.name}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Type className="w-3.5 h-3.5 text-cyan-400"/> HUD Font Weight
              </span>
              <div className="grid grid-cols-3 gap-1.5">
                {(["Thin", "Bold", "Black"] as const).map(wt => (
                  <button
                    key={wt}
                    onClick={() => { triggerHaptic?.(); setCustomFontWeight(wt); }}
                    className={`min-h-[40px] rounded-xl text-[9px] font-mono font-black uppercase border transition-all cursor-pointer active:scale-95 ${customFontWeight === wt ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.txtMuted || 'text-zinc-400'} ${ui?.brd || 'border-zinc-800'}`}`}
                  >
                    {wt}
                  </button>
                ))}
              </div>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Battery className="w-3.5 h-3.5 text-emerald-400"/> Battery Readout Mode
              </span>
              <select 
                value={batteryReadoutMode} 
                onChange={(e) => { triggerHaptic?.(); setBatteryReadoutMode(e.target.value as any); }}
                className={`w-full min-h-[40px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="Voltage">Voltage (e.g. 52.4V)</option>
                <option value="Percentage">Percentage (e.g. 85%)</option>
                <option value="Bars">Signal Bars (5-Bar Matrix)</option>
                <option value="WattHours">Remaining Watt-Hours (Wh)</option>
              </select>
            </div>
          </div>

          {/* HUD Opacity Slider */}
          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2`}>
            <div className="flex justify-between items-center">
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Eye className="w-3.5 h-3.5"/> Dashboard HUD Overlay Opacity
              </span>
              <span className="text-xs font-mono font-black text-white">{hudOpacity}%</span>
            </div>
            <input 
              type="range" min="40" max="100" step="5" value={hudOpacity} 
              onChange={(e) => setHudOpacity(Number(e.target.value))} 
              className="w-full accent-[#39ff14] cursor-pointer min-h-[40px]"
            />
          </div>

          {/* Interface Theme Palette */}
          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex flex-col sm:flex-row justify-between sm:items-center gap-2.5`}>
            <div className="flex flex-col space-y-0.5">
              <span className={`text-[9.5px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Palette className="w-3.5 h-3.5"/> Dashboard Accent Theme
              </span>
              <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>
                Global neon traces and avionics borders
              </span>
            </div>
            <div className="flex gap-1.5 overflow-x-auto custom-scrollbar pb-1">
              {[
                { id: 'lime', color: 'bg-[#39ff14]' },
                { id: 'cyan', color: 'bg-cyan-500' },
                { id: 'emerald', color: 'bg-emerald-500' },
                { id: 'amber', color: 'bg-amber-500' },
                { id: 'rose', color: 'bg-rose-500' },
                { id: 'purple', color: 'bg-purple-500' },
                { id: 'void', color: 'bg-zinc-700' }
              ].map(item => (
                <button 
                  key={item.id}
                  onClick={() => { triggerHaptic?.(); handleThemeChange(item.id); }} 
                  className={`min-w-[40px] min-h-[40px] shrink-0 flex items-center justify-center rounded-xl ${item.color} border-2 border-black cursor-pointer active:scale-95 touch-manipulation transition-all ${theme === item.id ? 'ring-2 ring-white ring-offset-1 ring-offset-black scale-105' : 'opacity-80 hover:opacity-100'}`}
                />
              ))}
            </div>
          </div>

          {/* Visibility Mode Toggles */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1`}>
                  <Sun className="w-3.5 h-3.5 text-amber-400"/> Sun Mode
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Anti-glare contrast</span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); toggleSunVisibility(); }} 
                className={`min-h-[40px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${isDayMode ? "bg-amber-500" : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${isDayMode ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1`}>
                  <EyeOff className="w-3.5 h-3.5 text-rose-500"/> Night Vision
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Monochrome red beam</span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); setIsNightVision(!isNightVision); if(!isNightVision) setIsDayMode(false); setAutoNightMode(false); }} 
                className={`min-h-[40px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${isNightVision ? "bg-rose-600" : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${isNightVision ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1`}>
                  <SunMoon className="w-3.5 h-3.5 text-cyan-400"/> Auto-Lux
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Photocell auto-switch</span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); setAutoNightMode(!autoNightMode); }} 
                className={`min-h-[40px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${autoNightMode ? `${ui?.t?.bg || 'bg-[#39ff14]'}` : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${autoNightMode ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- TAB 2: SENSORS & IMU CALIBRATION --- */}
      {configTab === "sensors" && (
        <div className="space-y-3.5">
          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5`}>
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Smartphone className={`w-4 h-4 ${ui?.t?.text || 'text-[#39ff14]'}`} />
                <span className={`text-xs font-black uppercase tracking-widest ${ui?.txtMain || 'text-white'}`}>
                  Active Hardware Sensor Diagnostics
                </span>
              </div>
              {!sensorsUnlocked && (
                <button 
                  onClick={() => { triggerHaptic?.(); requestSensorPermissions(); }} 
                  className={`min-h-[40px] px-3.5 text-[9px] font-mono font-black uppercase border rounded-xl ${ui?.t?.bg || 'bg-[#39ff14]'} text-black shadow-md flex items-center gap-1.5 cursor-pointer active:scale-95 touch-manipulation`}
                >
                  <Unlock className="w-3 h-3"/> Unlock Web Sensors
                </button>
              )}
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col gap-0.5 shadow-inner`}>
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-mono font-black uppercase tracking-widest flex items-center gap-1`}>
                  <RotateCw className="w-3 h-3 text-cyan-400"/> Gyro Lean (X)
                </span>
                <span className={`${ui?.txtMain || 'text-white'} text-sm font-mono font-bold tabular-nums`}>{getCalibratedLean()}°</span>
              </div>

              <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col gap-0.5 shadow-inner`}>
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-mono font-black uppercase tracking-widest flex items-center gap-1`}>
                  <Crosshair className="w-3 h-3 text-emerald-400"/> Pitch (Y)
                </span>
                <span className={`${ui?.txtMain || 'text-white'} text-sm font-mono font-bold tabular-nums`}>{Math.round(pitchAngle - gyroOffset.pitch)}°</span>
              </div>

              <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col gap-0.5 shadow-inner`}>
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-mono font-black uppercase tracking-widest flex items-center gap-1`}>
                  <Scale className="w-3 h-3 text-rose-400"/> Inertial G
                </span>
                <span className={`${ui?.txtMain || 'text-white'} text-sm font-mono font-bold tabular-nums`}>{hardwareGForce.toFixed(2)} G</span>
              </div>

              <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col gap-0.5 shadow-inner`}>
                <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-mono font-black uppercase tracking-widest flex items-center gap-1`}>
                  <Sun className="w-3 h-3 text-amber-400"/> Ambient Lux
                </span>
                <span className={`${ui?.txtMain || 'text-white'} text-sm font-mono font-bold tabular-nums`}>{Math.round(ambientLux)} LUX</span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <ArrowUpDown className="w-3.5 h-3.5 text-cyan-400"/> Invert Pitch/Roll Axis
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Reverse artificial horizon</span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); setPitchRollInversion(!pitchRollInversion); }} 
                className={`min-h-[40px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${pitchRollInversion ? `${ui?.t?.bg || 'bg-[#39ff14]'}` : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${pitchRollInversion ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Gauge className="w-3.5 h-3.5 text-emerald-400"/> Acceleration Unit
              </span>
              <select 
                value={accelerationUnit} 
                onChange={(e) => { triggerHaptic?.(); setAccelerationUnit(e.target.value); }}
                className={`w-full min-h-[40px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="G-Force">G-Force (e.g. 0.4g)</option>
                <option value="m/s²">Meters per Second Squared (m/s²)</option>
                <option value="Percent Grade">Percent Incline Grade (%)</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5`}>
              <div className="flex justify-between items-center">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <Magnet className="w-3.5 h-3.5"/> EMF Baseline & Sensitivity
                </span>
                <span className="text-[8.5px] font-mono text-cyan-400">{Math.round(magBaseline)} µT Zeroed</span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); calibrateEMF(); }} 
                className={`w-full min-h-[44px] ${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl text-[9.5px] font-mono font-black uppercase tracking-widest ${ui?.t?.text || 'text-[#39ff14]'} hover:opacity-80 active:scale-95 touch-manipulation cursor-pointer shadow-inner`}
              >
                Zero Ambient EMF Baseline
              </button>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5 flex flex-col justify-between`}>
              <div>
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <RotateCw className="w-3.5 h-3.5 text-cyan-400"/> IMU Handlebar Calibration
                </span>
                <p className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase mt-0.5`}>
                  Level your vehicle on flat ground and tap to zero gyro pitch
                </p>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); calibrateIMU(); }} 
                className="w-full min-h-[44px] bg-cyan-600 hover:bg-cyan-500 text-black text-[9.5px] font-mono font-black uppercase tracking-wider rounded-xl flex items-center justify-center gap-1.5 cursor-pointer active:scale-95 touch-manipulation shadow-md"
              >
                <Crosshair className="w-3.5 h-3.5" /> Zero Handlebar Level
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- TAB 3: GPS & POLLING RATES --- */}
      {configTab === "gps" && (
        <div className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Activity className="w-3.5 h-3.5"/> GPS Path Smoothing
              </span>
              <select 
                value={gpsSmoothing} 
                onChange={(e) => { triggerHaptic?.(); setGpsSmoothing(e.target.value as any); }} 
                className={`w-full min-h-[44px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="Loose">Loose (Instant Velocity Response)</option>
                <option value="Balanced">Balanced (Standard Urban Filter)</option>
                <option value="Strict">Strict (Heavy Anti-Jitter Smoothing)</option>
              </select>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Activity className="w-3.5 h-3.5"/> Telemetry Refresh Interval
              </span>
              <select 
                value={telemetryRate} 
                onChange={(e) => { triggerHaptic?.(); setTelemetryRate(Number(e.target.value)); }} 
                className={`w-full min-h-[44px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="500">Ultra-Fast (0.5s Refresh)</option>
                <option value="1000">Nominal (1.0s Refresh)</option>
                <option value="2000">Battery Saver (2.0s Refresh)</option>
              </select>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Radar className="w-3.5 h-3.5"/> Speed Limit Polling Matrix
              </span>
              <select 
                value={speedLimitPolling} 
                onChange={(e) => { triggerHaptic?.(); setSpeedLimitPolling(e.target.value as any); }} 
                className={`w-full min-h-[44px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="Aggressive">Aggressive (~50ft / Turn Detection)</option>
                <option value="Balanced">Balanced (~250ft Refresh)</option>
                <option value="Eco">Eco (~800ft Cellular Conservation)</option>
              </select>
            </div>
          </div>

          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
            <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
              <Compass className="w-3.5 h-3.5 text-yellow-400"/> Elevation &amp; Altitude Unit
            </span>
            <div className="grid grid-cols-2 gap-1.5">
              {(["Feet", "Meters"] as const).map(unit => (
                <button
                  key={unit}
                  onClick={() => { triggerHaptic?.(); setElevationUnit(unit); }}
                  className={`min-h-[40px] rounded-xl text-[9px] font-mono font-black uppercase border transition-all cursor-pointer active:scale-95 ${elevationUnit === unit ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.txtMuted || 'text-zinc-400'} ${ui?.brd || 'border-zinc-800'}`}`}
                >
                  {unit}
                </button>
              ))}
            </div>
          </div>

          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2`}>
            <div className="flex items-center justify-between">
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <MapPin className="w-3.5 h-3.5"/> Base Station GPS Fallback Coordinates
              </span>
              <span className="text-[7.5px] font-mono text-zinc-500 uppercase">Emergency Weather Lock</span>
            </div>
            <div className="flex flex-col sm:flex-row gap-2">
              <input 
                type="text" 
                value={fallbackInput} 
                onChange={(e) => setFallbackInput(e.target.value)} 
                placeholder="e.g. Stigler, OK or London, UK" 
                className={`flex-1 min-h-[44px] ${ui?.bgInput || 'bg-black'} rounded-xl px-3 text-xs font-mono font-bold text-white outline-none border ${ui?.brd || 'border-zinc-800'} focus:border-cyan-500`}
              />
              <button 
                onClick={() => { triggerHaptic?.(); manuallySetFallback(); }} 
                className={`min-h-[44px] px-5 rounded-xl text-[9.5px] font-mono font-black uppercase tracking-widest transition-colors ${ui?.t?.bg || 'bg-[#39ff14]'} text-black cursor-pointer active:scale-95 touch-manipulation`}
              >
                Lock Base Zone
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- TAB 4: AUDIO, VOICE & HAZARD ALERTS --- */}
      {configTab === "audio" && (
        <div className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <BellRing className="w-3.5 h-3.5 text-cyan-400"/> Audio Speed Alarm
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>
                  Chimes when exceeding limits
                </span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); setAudioWarnings(!audioWarnings); }} 
                className={`min-h-[44px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${audioWarnings ? `${ui?.t?.bg || 'bg-[#39ff14]'}` : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${audioWarnings ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <Vibrate className="w-3.5 h-3.5 text-emerald-400"/> Speed Alert Vibration
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Haptic pulse on overspeed</span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); setSpeedAlertVibration(!speedAlertVibration); }} 
                className={`min-h-[44px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${speedAlertVibration ? `${ui?.t?.bg || 'bg-[#39ff14]'}` : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${speedAlertVibration ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>
          </div>

          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
            <div className="flex flex-col space-y-0.5 pr-2">
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Zap className="w-3.5 h-3.5 text-yellow-400"/> HUD Blink on Max Speed
              </span>
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Flashing indicator at top velocity</span>
            </div>
            <button 
              onClick={() => { triggerHaptic?.(); setHudBlinkOnMaxSpeed(!hudBlinkOnMaxSpeed); }} 
              className={`min-h-[44px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${hudBlinkOnMaxSpeed ? `${ui?.t?.bg || 'bg-[#39ff14]'}` : "bg-zinc-800"}`}
            >
              <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${hudBlinkOnMaxSpeed ? "translate-x-6" : "translate-x-0.5"}`}/>
            </button>
          </div>

          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2`}>
            <div className="flex justify-between items-center">
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <VolumeX className="w-3.5 h-3.5"/> Warning Beep Throttle Interval
              </span>
              <span className="text-xs font-mono text-cyan-400 font-bold">{audioThrottleInterval / 1000}s Delay</span>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[1000, 3000, 5000, 10000].map(ms => (
                <button
                  key={ms}
                  onClick={() => { triggerHaptic?.(); setAudioThrottleInterval(ms); }}
                  className={`min-h-[44px] rounded-xl text-[9.5px] font-mono font-black uppercase border transition-all cursor-pointer active:scale-95 touch-manipulation ${audioThrottleInterval === ms ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.txtMuted || 'text-zinc-400'} ${ui?.brd || 'border-zinc-800'}`}`}
                >
                  {ms / 1000}s Delay
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* --- TAB 5: DASHBOARD CONTROL PREFERENCES --- */}
      {configTab === "preferences" && (
        <div className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <Scale className="w-3.5 h-3.5"/> Metric Velocity Units (KM/H)
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>
                  Toggles between Imperial (MPH) and Metric (KM/H)
                </span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); setUseMetric(!useMetric); }} 
                className={`min-h-[44px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${useMetric ? `${ui?.t?.bg || 'bg-[#39ff14]'}` : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${useMetric ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <Sparkles className="w-3.5 h-3.5 text-yellow-400"/> Keep Screen Awake
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Prevent display sleep mode</span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); setKeepScreenAwake(!keepScreenAwake); }} 
                className={`min-h-[40px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${keepScreenAwake ? `${ui?.t?.bg || 'bg-[#39ff14]'}` : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${keepScreenAwake ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Clock className="w-3.5 h-3.5 text-amber-400"/> Trip Odometer Reset
              </span>
              <select 
                value={tripOdometerAutoReset} 
                onChange={(e) => { triggerHaptic?.(); setTripOdometerAutoReset(e.target.value); }}
                className={`w-full min-h-[40px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="Never">Manual Reset Only</option>
                <option value="4 Hours">After 4 Hours Idle</option>
                <option value="12 Hours">After 12 Hours Idle</option>
                <option value="24 Hours">Daily Midnight Reset</option>
              </select>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <Thermometer className="w-3.5 h-3.5 text-rose-400"/> Temperature Unit
              </span>
              <select 
                value={temperatureUnit} 
                onChange={(e) => { triggerHaptic?.(); setTemperatureUnit(e.target.value); }}
                className={`w-full min-h-[40px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="Fahrenheit (°F)">Fahrenheit (°F)</option>
                <option value="Celsius (°C)">Celsius (°C)</option>
              </select>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5`}>
              <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                <ZapOff className="w-3.5 h-3.5 text-purple-400"/> Quick Action Tile
              </span>
              <select 
                value={quickLaunchShortcut} 
                onChange={(e) => { triggerHaptic?.(); setQuickLaunchShortcut(e.target.value); }}
                className={`w-full min-h-[40px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="AI Copilot">AI Copilot Mechanic</option>
                <option value="Parts Finder">Multi-Source Parts</option>
                <option value="Spec Matrix">Deep Spec Matrix</option>
                <option value="Financial ROI">Financial ROI Ledger</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <LayoutGrid className="w-3.5 h-3.5 text-cyan-400"/> Layout Density
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Dashboard widget spacing</span>
              </div>
              <select 
                value={dashboardLayoutDensity} 
                onChange={(e) => { triggerHaptic?.(); setDashboardLayoutDensity(e.target.value as any); }}
                className={`min-h-[40px] ${ui?.bgInput || 'bg-black'} text-[9.5px] font-mono font-black uppercase px-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none cursor-pointer`}
              >
                <option value="Compact">Compact</option>
                <option value="Standard">Standard</option>
                <option value="Expanded">Expanded</option>
              </select>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
              <div className="flex flex-col space-y-0.5 pr-2">
                <span className={`text-[9px] ${ui?.t?.text || 'text-[#39ff14]'} font-black uppercase tracking-widest flex items-center gap-1.5`}>
                  <BarChart3 className="w-3.5 h-3.5 text-emerald-400"/> Telemetry Graphs
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Live real-time sparklines</span>
              </div>
              <button 
                onClick={() => { triggerHaptic?.(); setShowTelemetryGraphs(!showTelemetryGraphs); }} 
                className={`min-h-[40px] min-w-[56px] rounded-xl transition-colors cursor-pointer active:scale-95 touch-manipulation flex items-center px-1 ${showTelemetryGraphs ? `${ui?.t?.bg || 'bg-[#39ff14]'}` : "bg-zinc-800"}`}
              >
                <span className={`inline-block h-5 w-5 transform rounded-lg bg-white transition-transform ${showTelemetryGraphs ? "translate-x-6" : "translate-x-0.5"}`}/>
              </button>
            </div>
          </div>

          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex justify-between items-center`}>
            <div className="flex items-center gap-2">
              <Network className="w-4 h-4 text-cyan-400 shrink-0" />
              <div>
                <span className="text-[9.5px] font-black uppercase tracking-widest text-white block">
                  Subsystem Network Diagnostics
                </span>
                <span className="text-[8px] text-zinc-400 font-mono">
                  Telemetry uplink ping & connectivity lock
                </span>
              </div>
            </div>
            <div className="flex items-center gap-2.5 font-mono">
              <span className="text-xs font-bold text-white">{networkType.toUpperCase()}</span>
              <span className={`text-xs font-black px-2 py-0.5 rounded-lg border ${networkPing > 100 ? 'bg-amber-950/40 border-amber-800 text-amber-400' : 'bg-emerald-950/40 border-emerald-800 text-emerald-400'}`}>
                {networkPing}ms
              </span>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}