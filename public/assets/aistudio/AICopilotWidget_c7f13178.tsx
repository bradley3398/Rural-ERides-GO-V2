import React, { useState, useEffect, useRef, useMemo } from "react";
import { 
  BrainCircuit, Globe, Loader2, Sparkles, Volume2, VolumeX, 
  ShieldAlert, Flame, Wind, Zap, TrendingUp, TrendingDown,
  Activity, Cpu, Radio, Battery, Gauge,
  RotateCw, Crosshair, Scale, Mountain, AlertTriangle,
  Sliders, CheckCircle2, Volume1, RefreshCw, Disc, Shield, ShieldCheck, Navigation, Compass, Truck,
  Thermometer, Droplets, Snowflake, Sun, Eye, CloudRain, CircleGauge, Cloud
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { TextToSpeech } from "@capacitor-community/text-to-speech";

const safeNum = (val: any, fallback = 0): number => {
  const parsed = Number(val);
  return isNaN(parsed) || !isFinite(parsed) ? fallback : parsed;
};

export interface TacticalAvionicsProps {
  ui: any;
  vehicleModel?: string;
  motorWattage?: number;
  batteryCapacity?: number;
  batteryVoltage?: number;
  wheelSize?: number;
  savedRides?: any[];
  weatherData?: any;
  compassHeading?: number;
  triggerCloudAiAnalysis?: () => void;
  isFetchingCloudAi?: boolean;
  cloudAiAnalysis?: string | null;
  aiAdvice?: {
    status?: string;
    msg?: string;
    color?: string;
  };
  learnedEfficiency?: number;
  motorEfficiencyPct?: number;
  pilotRiskIndex?: number;
  topographyRangeImpact?: number;
  estStatorTemp?: number;
  estRotorTemp?: number;
  liveRideScore?: string;
  aeroDragForce?: number;
  bonusRegenMiles?: number;
  terrainClassifier?: string;
  thermalRunawayIndex?: number;
  distLabel?: string;
  speedLabel?: string;
  batteryPercent?: number;
  batteryTopology?: string;
  currentAmps?: number;
  liveSagVoltage?: number;
  liveGradePercent?: number;
  liveRPM?: number;
  kineticEnergyJoules?: number;
  displaySpeed?: number;
  elevationGain?: number;
  leanAngle?: number;
  pitchAngle?: number;
  hardwareGForce?: number;
  roadJitter?: number;
  magFieldLoad?: number;
  ambientLux?: number;
  useMetric?: boolean;
  globalVolume?: number;
}

export default function AICopilotTacticalSuite(props: TacticalAvionicsProps) {
  const {
    ui,
    vehicleModel: propVehicleModel,
    motorWattage: propMotorWattage,
    batteryCapacity: propBatteryCapacity,
    batteryVoltage: propBatteryVoltage,
    wheelSize: propWheelSize,
    savedRides: propSavedRides = [],
    weatherData: propWeatherData,
    compassHeading = 0,
    triggerCloudAiAnalysis = () => {},
    isFetchingCloudAi = false,
    cloudAiAnalysis = null,
    aiAdvice = { status: "NOMINAL", msg: "All subsystems operational.", color: "text-[#39ff14] border-[#39ff14]/50" },
    learnedEfficiency = 25,
    motorEfficiencyPct = 88,
    pilotRiskIndex = 0,
    topographyRangeImpact = 0,
    estStatorTemp = 75,
    estRotorTemp = 75,
    liveRideScore = "A",
    aeroDragForce = 0,
    bonusRegenMiles = 0,
    terrainClassifier = "Smooth Asphalt",
    thermalRunawayIndex = 0,
    distLabel = "MI",
    speedLabel = "MPH",
    batteryPercent = 100,
    batteryTopology = "14S 4P",
    currentAmps = 0,
    liveSagVoltage = 48,
    liveGradePercent = 0,
    liveRPM = 0,
    kineticEnergyJoules = 0,
    displaySpeed = 0,
    elevationGain = 0,
    leanAngle = 0,
    pitchAngle = 0,
    hardwareGForce = 0,
    roadJitter = 0,
    magFieldLoad = 0,
    ambientLux = 0,
    useMetric = false,
    globalVolume = 100
  } = props;

  const [localMetric, setLocalMetric] = useState<boolean>(useMetric);
  const [localVolume, setLocalVolume] = useState<number>(globalVolume);

  // --- MASTER SETTINGS SYNC ENGINE ---
  useEffect(() => {
    const handleSettingsSync = () => {
      const isMetric = localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km";
      setLocalMetric(isMetric);

      const masterVol = Number(localStorage.getItem("rural_global_vol") ?? globalVolume);
      setLocalVolume(masterVol);
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
  }, [globalVolume]);

  useEffect(() => {
    if (useMetric !== undefined) setLocalMetric(useMetric);
  }, [useMetric]);

  useEffect(() => {
    if (globalVolume !== undefined) setLocalVolume(globalVolume);
  }, [globalVolume]);

  const activeDistLabel = localMetric ? "KM" : "MI";
  const activeSpeedLabel = localMetric ? "KM/H" : "MPH";

  const [activeMatrixTab, setActiveMatrixTab] = useState<"copilot" | "weather" | "cells" | "range" | "thermal" | "history" | "imu" | "audio">("copilot");
  
  // Fleet Profiles
  const [garageProfiles, setGarageProfiles] = useState<any[]>([]);
  const [activeVehicleId, setActiveVehicleId] = useState<string>("");
  const [activeVehicleName, setActiveVehicleName] = useState<string>(propVehicleModel || "Universal PEV");
  const [activeMotorWattage, setActiveMotorWattage] = useState<number>(safeNum(propMotorWattage, 1000));
  const [activeBatteryCapacity, setActiveBatteryCapacity] = useState<number>(safeNum(propBatteryCapacity, 15));
  const [activeBatteryVoltage, setActiveBatteryVoltage] = useState<number>(safeNum(propBatteryVoltage, 48));
  const [activeWheelSize, setActiveWheelSize] = useState<number>(safeNum(propWheelSize, 10));

  // Live Weather State & Auto-Fetcher
  const [localLiveWeather, setLocalLiveWeather] = useState<any>(null);

  const fetchLiveAtmospherics = async () => {
    try {
      const lat = Number(localStorage.getItem("pev_weather_lat") || localStorage.getItem("rt_fallback_lat") || 35.2534);
      const lng = Number(localStorage.getItem("pev_weather_lng") || localStorage.getItem("rt_fallback_lng") || -95.1275);
      
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lng}&current=temperature_2m,apparent_temperature,relative_humidity_2m,dew_point_2m,weather_code,wind_speed_10m,wind_gusts_10m,wind_direction_10m,precipitation,uv_index,visibility,cloud_cover,surface_pressure,soil_temperature_0cm,direct_radiation&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch&timezone=auto`;
      
      const res = await fetch(url);
      if (res.ok) {
        const data = await res.json();
        setLocalLiveWeather(data);
        localStorage.setItem("rt_weather_data_cache", JSON.stringify(data));
      }
    } catch (e) {}
  };

  useEffect(() => {
    try {
      const stored = JSON.parse(localStorage.getItem("rural_saved_pev_profiles") || "[]");
      setGarageProfiles(stored);
      if (stored.length > 0 && !activeVehicleId) {
        const currentName = propVehicleModel || localStorage.getItem("rural_pev_name");
        const match = stored.find((p: any) => p.name === currentName) || stored[0];
        if (match) {
          setActiveVehicleId(match.id);
          setActiveVehicleName(match.name);
          setActiveMotorWattage(safeNum(match.watts, 1000));
          setActiveBatteryCapacity(safeNum(match.ah, 15));
          setActiveBatteryVoltage(safeNum(match.volts, 48));
          setActiveWheelSize(safeNum(match.wheels, 10));
        }
      }

      const wCache = localStorage.getItem("rt_weather_data_cache");
      if (wCache) setLocalLiveWeather(JSON.parse(wCache));
    } catch (e) {}

    fetchLiveAtmospherics();
    const weatherTimer = setInterval(fetchLiveAtmospherics, 120000);
    return () => clearInterval(weatherTimer);
  }, [propVehicleModel, activeVehicleId]);

  useEffect(() => {
    if (propWeatherData && propWeatherData.current) {
      setLocalLiveWeather(propWeatherData);
    }
  }, [propWeatherData]);

  useEffect(() => {
    if (propVehicleModel && propVehicleModel !== "Universal PEV") setActiveVehicleName(propVehicleModel);
    if (propMotorWattage) setActiveMotorWattage(safeNum(propMotorWattage, 1000));
    if (propBatteryCapacity) setActiveBatteryCapacity(safeNum(propBatteryCapacity, 15));
    if (propBatteryVoltage) setActiveBatteryVoltage(safeNum(propBatteryVoltage, 48));
    if (propWheelSize) setActiveWheelSize(safeNum(propWheelSize, 10));
  }, [propVehicleModel, propMotorWattage, propBatteryCapacity, propBatteryVoltage, propWheelSize]);

  const handleSelectVehicle = (profileId: string) => {
    const profile = garageProfiles.find(p => p.id === profileId);
    if (!profile) return;
    setActiveVehicleId(profile.id);
    setActiveVehicleName(profile.name);
    setActiveMotorWattage(safeNum(profile.watts, 1000));
    setActiveBatteryCapacity(safeNum(profile.ah, 15));
    setActiveBatteryVoltage(safeNum(profile.volts, 48));
    setActiveWheelSize(safeNum(profile.wheels, 10));
    localStorage.setItem("rural_pev_name", profile.name);
  };

  // --- AUDIO / VOICE ENGINE ---
  const [voiceMuted, setVoiceMuted] = useState(false);
  const [speechActive, setSpeechActive] = useState(false);
  const [voiceSpeed, setVoiceSpeed] = useState<number>(1.05);
  const [voicePitch, setVoicePitch] = useState<number>(1.0);
  const [autoVoiceAlerts, setAutoVoiceAlerts] = useState<boolean>(true);
  const [allowLeanAlerts, setAllowLeanAlerts] = useState<boolean>(false);
  const [availableVoices, setAvailableVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [selectedVoiceURI, setSelectedVoiceURI] = useState<string>("");
  
  const activeUtteranceRef = useRef<SpeechSynthesisUtterance | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const lastSpokenStatusRef = useRef<string>("");
  const lastAlertTimeRef = useRef<number>(0);

  const [virtualCellSeries, setVirtualCellSeries] = useState<number>(14);
  const [elevationHistory, setElevationHistory] = useState<number[]>([]);
  const [peakRecordedG, setPeakRecordedG] = useState<number>(0);
  const [peakRecordedLean, setPeakRecordedLean] = useState<number>(0);

  // ==========================================
  // --- REAL LIVE WEATHER-AI CALCULATIONS ---
  // ==========================================
  const activeWeather = (propWeatherData && propWeatherData.current) ? propWeatherData : (localLiveWeather || {});
  const currentWeather = activeWeather?.current || null;
  const hasLiveWeather = !!currentWeather && typeof currentWeather.temperature_2m === "number";

  const ambientTemp = hasLiveWeather ? safeNum(currentWeather.temperature_2m) : 91;
  const windSpeedRaw = hasLiveWeather ? safeNum(currentWeather.wind_speed_10m, 0) : 0;
  const windSpeed = localMetric ? windSpeedRaw * 1.60934 : windSpeedRaw;
  const windDirection = hasLiveWeather ? safeNum(currentWeather.wind_direction_10m, 0) : 0;
  const surfacePressure = hasLiveWeather ? safeNum(currentWeather.surface_pressure, 1013.25) : 1013.25;
  const relativeHumidity = hasLiveWeather ? safeNum(currentWeather.relative_humidity_2m, 40) : 40;
  const precipitation = hasLiveWeather ? safeNum(currentWeather.precipitation, 0) : 0;
  const directRadiation = hasLiveWeather ? safeNum(currentWeather.direct_radiation, 0) : 0;

  const tempKelvin = ((ambientTemp - 32) * (5 / 9)) + 273.15;
  const calculatedAirDensity = (surfacePressure * 100) / (287.058 * tempKelvin);

  const windAngleDiffRad = ((windDirection - safeNum(compassHeading, 0)) * Math.PI) / 180;
  const headwindComponent = Math.cos(windAngleDiffRad) * windSpeed;
  const crosswindComponent = Math.sin(windAngleDiffRad) * windSpeed;

  const ambientCelsius = (ambientTemp - 32) * (5 / 9);
  const weatherBatteryDerate = useMemo(() => {
    if (ambientCelsius >= 20 && ambientCelsius <= 30) return 0;
    if (ambientCelsius < 0) return Math.min(40, Math.abs(ambientCelsius) * 1.6 + 12);
    if (ambientCelsius < 15) return (15 - ambientCelsius) * 1.1;
    if (ambientCelsius > 35) return (ambientCelsius - 35) * 0.75;
    return 0;
  }, [ambientCelsius]);

  const weatherSurfaceGrip = useMemo(() => {
    let grip = 100;
    let desc = "DRY TARMAC (OPTIMAL)";
    let color = "text-emerald-400";

    if (precipitation > 0) {
      grip -= Math.min(45, precipitation * 180 + 15);
      desc = "SURFACE PUDDLING / AQUAPLANE";
      color = "text-rose-500 animate-pulse";
    } else if (relativeHumidity > 80 && ambientCelsius < 6) {
      grip -= 20;
      desc = "DAMP SLICK ROAD / DEW";
      color = "text-amber-400";
    }

    if (ambientCelsius <= 1) {
      grip = Math.min(grip, 35);
      desc = "SUB-SURFACE BLACK ICE HAZARD";
      color = "text-rose-500 animate-pulse";
    }

    return { grip: Math.max(15, Math.round(grip)), desc, color };
  }, [precipitation, relativeHumidity, ambientCelsius]);

  const vehicleHistoryStats = useMemo(() => {
    const rides = propSavedRides.length > 0 ? propSavedRides : (() => {
      try { return JSON.parse(localStorage.getItem("universal_erides_rides") || "[]"); } catch { return []; }
    })();
    const matchingRides = rides.filter((r: any) => !activeVehicleName || r.vehicleModel === activeVehicleName || r.pevType === activeVehicleName);
    const totalSorties = matchingRides.length;
    let accumDist = 0;
    let maxSpeedFound = 0;
    let totalWhUsed = 0;
    matchingRides.forEach((r: any) => {
      accumDist += safeNum(r.distance);
      totalWhUsed += safeNum(r.totalWhConsumed);
      if (safeNum(r.maxSpeed) > maxSpeedFound) maxSpeedFound = safeNum(r.maxSpeed);
    });
    const avgHistoricalWh = accumDist > 0 ? (totalWhUsed / accumDist).toFixed(1) : "25.0";
    return { totalSorties, accumDist: (accumDist * (localMetric ? 1.60934 : 1.0)).toFixed(1), maxSpeedFound: (maxSpeedFound * (localMetric ? 1.60934 : 1.0)).toFixed(1), avgHistoricalWh };
  }, [propSavedRides, activeVehicleName, localMetric]);

  const returnHomeBudget = useMemo(() => {
    const rawCapacityWh = (safeNum(batteryPercent, 100) / 100) * (safeNum(activeBatteryVoltage, 48) * safeNum(activeBatteryCapacity, 15));
    const deratedWh = rawCapacityWh * (1 - (weatherBatteryDerate / 100));
    const windPenaltyFactor = headwindComponent > 5 ? 1 + ((headwindComponent - 5) * 0.02) : 1;
    const effectiveWhPerUnit = Math.max(10, safeNum(learnedEfficiency, 25) * windPenaltyFactor * (localMetric ? 0.621371 : 1.0));
    const maxEstDistance = deratedWh / effectiveWhPerUnit;
    const safeRadiusMiles = maxEstDistance * 0.85; 

    return {
      totalCapacityWh: Math.round(deratedWh),
      maxRangeMiles: maxEstDistance.toFixed(1),
      safeHomeRadius: safeRadiusMiles.toFixed(1),
      status: safeRadiusMiles > 4 ? "SECURE RETURN RADIUS" : "CRITICAL ENERGY BUDGET"
    };
  }, [batteryPercent, activeBatteryVoltage, activeBatteryCapacity, weatherBatteryDerate, headwindComponent, learnedEfficiency, localMetric]);

  const slipPrediction = useMemo(() => {
    const gLat = Math.abs(safeNum(hardwareGForce, 0));
    const speed = safeNum(displaySpeed, 0);
    const jitter = safeNum(roadJitter, 0);
    const weatherPenalty = (100 - weatherSurfaceGrip.grip) * 0.45;
    const slipRiskScore = Math.min(100, Math.round((gLat * 30) + (speed > 20 ? speed * 0.7 : 0) + (jitter * 4) + weatherPenalty));
    
    let rating = "HIGH TRACTION";
    let color = "text-emerald-400";
    if (slipRiskScore > 60) {
      rating = "TRACTION LOSS / SLIP RISK";
      color = "text-rose-500 animate-pulse";
    } else if (slipRiskScore > 35) {
      rating = "MODERATE ROAD SLIP";
      color = "text-amber-400";
    }
    return { slipRiskScore, rating, color };
  }, [hardwareGForce, displaySpeed, roadJitter, weatherSurfaceGrip.grip]);

  const motorThermals = useMemo(() => {
    const amps = safeNum(currentAmps, 0);
    const statorR = 0.08;
    const jouleHeatingWatts = (amps ** 2) * statorR;
    const coolingFactor = (safeNum(displaySpeed, 0) * 0.4) + (Math.max(0, -headwindComponent) * 0.2) + 1.0;
    const netHeatGen = Math.max(0, jouleHeatingWatts - coolingFactor);

    const grade = Math.abs(safeNum(liveGradePercent, 0));
    const maxContinuousWatts = activeMotorWattage * 1.35;
    const liveLoadWatts = amps * safeNum(liveSagVoltage, activeBatteryVoltage);
    const torqueHeadroomPct = Math.max(0, Math.min(100, Math.round((1 - (liveLoadWatts / (maxContinuousWatts || 1))) * 100)));

    let thermalWarning = "OPTIMAL";
    if (netHeatGen > 45 && grade > 8) thermalWarning = "HIGH INCLINE STRESS";
    if (safeNum(estStatorTemp, 75) > 185) thermalWarning = "CRITICAL THERMAL THROTTLE";

    return { jouleHeatingWatts: jouleHeatingWatts.toFixed(0), torqueHeadroomPct, thermalWarning };
  }, [currentAmps, displaySpeed, headwindComponent, liveGradePercent, activeMotorWattage, liveSagVoltage, activeBatteryVoltage, estStatorTemp]);

  useEffect(() => {
    const topStr = String(batteryTopology || "14S");
    const match = topStr.match(/(\d+)S/i);
    if (match && match[1]) {
      const parsed = parseInt(match[1], 10);
      if (parsed >= 6 && parsed <= 32) setVirtualCellSeries(parsed);
    }
  }, [batteryTopology]);

  useEffect(() => {
    try {
      const savedMute = localStorage.getItem("rt_voice_muted");
      if (savedMute !== null) setVoiceMuted(savedMute === "true");
      const savedRate = localStorage.getItem("rt_voice_rate");
      if (savedRate) setVoiceSpeed(Number(savedRate));
      const savedAuto = localStorage.getItem("rt_voice_auto");
      if (savedAuto !== null) setAutoVoiceAlerts(savedAuto === "true");
      const savedLeanVoice = localStorage.getItem("rt_voice_lean_alerts");
      if (savedLeanVoice !== null) setAllowLeanAlerts(savedLeanVoice === "true");
    } catch (e) {}
  }, []);

  const playAvionicsChime = () => {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      if (!audioCtxRef.current) audioCtxRef.current = new AudioCtx();
      const ctx = audioCtxRef.current;
      if (ctx.state === "suspended") ctx.resume();

      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(880, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(1320, ctx.currentTime + 0.08);

      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.12);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.12);
    } catch (e) {}
  };

  useEffect(() => {
    if (typeof window === "undefined" || !("speechSynthesis" in window)) return;
    const populateVoices = () => {
      try {
        const voices = window.speechSynthesis.getVoices();
        if (voices && voices.length > 0) {
          const englishVoices = voices.filter(v => v.lang.startsWith("en"));
          setAvailableVoices(englishVoices.length > 0 ? englishVoices : voices);
          if (!selectedVoiceURI) {
            const preferred = englishVoices.find(v => 
              v.name.includes("Google") || v.name.includes("Natural") || v.name.includes("Samantha")
            ) || englishVoices[0] || voices[0];
            if (preferred) setSelectedVoiceURI(preferred.voiceURI);
          }
        }
      } catch (e) {}
    };
    populateVoices();
    if (window.speechSynthesis.onvoiceschanged !== undefined) {
      window.speechSynthesis.onvoiceschanged = populateVoices;
    }
  }, [selectedVoiceURI]);

  useEffect(() => {
    const g = safeNum(hardwareGForce, 0);
    const lean = Math.abs(safeNum(leanAngle, 0));
    if (g > peakRecordedG) setPeakRecordedG(g);
    if (lean > peakRecordedLean) setPeakRecordedLean(lean);
  }, [hardwareGForce, leanAngle, peakRecordedG, peakRecordedLean]);

  useEffect(() => {
    const timer = setInterval(() => {
      setElevationHistory(prev => [...prev.slice(-39), safeNum(elevationGain, 0)]);
    }, 1500);
    return () => clearInterval(timer);
  }, [elevationGain]);

  const speakText = async (text: string, isAutomated = false) => {
    if (voiceMuted || localVolume <= 0) return;
    const now = Date.now();
    if (isAutomated) {
      if (!autoVoiceAlerts) return;
      if (now - lastAlertTimeRef.current < 20000) return;
    }

    const cleanString = text.replace(/[*_#`~[\]]/g, "").trim();
    if (!cleanString) return;

    playAvionicsChime();
    setSpeechActive(true);
    lastAlertTimeRef.current = now;

    try {
      await TextToSpeech.speak({
        text: cleanString,
        lang: "en-US",
        rate: Math.max(0.7, Math.min(1.5, voiceSpeed)),
        pitch: Math.max(0.8, Math.min(1.3, voicePitch)),
        volume: localVolume / 100,
        category: "ambient"
      });
      setSpeechActive(false);
      return;
    } catch (nativeErr) {}

    if (typeof window !== "undefined" && "speechSynthesis" in window) {
      try {
        const synth = window.speechSynthesis;
        synth.cancel();
        if (synth.paused) synth.resume();

        const utterance = new SpeechSynthesisUtterance(cleanString);
        utterance.lang = "en-US";
        utterance.rate = Math.max(0.7, Math.min(1.5, voiceSpeed));
        utterance.pitch = Math.max(0.8, Math.min(1.3, voicePitch));
        utterance.volume = localVolume / 100;

        if (selectedVoiceURI && availableVoices.length > 0) {
          const matchVoice = availableVoices.find(v => v.voiceURI === selectedVoiceURI);
          if (matchVoice) utterance.voice = matchVoice;
        }

        utterance.onstart = () => setSpeechActive(true);
        utterance.onend = () => { setSpeechActive(false); activeUtteranceRef.current = null; };
        utterance.onerror = () => { setSpeechActive(false); activeUtteranceRef.current = null; };

        activeUtteranceRef.current = utterance;
        synth.speak(utterance);
      } catch (err) {
        setSpeechActive(false);
      }
    } else {
      setSpeechActive(false);
    }
  };

  useEffect(() => {
    if (!autoVoiceAlerts || voiceMuted) return;
    const currentStatus = aiAdvice?.status || "NOMINAL";
    
    if (currentStatus === "NOMINAL" || currentStatus === "OPTIMIZED" || currentStatus === "OPTIMIZED CRUISE") {
      lastSpokenStatusRef.current = currentStatus;
      return;
    }

    if (currentStatus.includes("TRACTION") || currentStatus.includes("LEAN")) {
      if (!allowLeanAlerts) return;
    }

    const now = Date.now();
    if (currentStatus !== lastSpokenStatusRef.current && (now - lastAlertTimeRef.current > 20000)) {
      lastSpokenStatusRef.current = currentStatus;
      speakText(`Tactical alert: ${aiAdvice.msg}`, true);
    }
  }, [aiAdvice?.status, autoVoiceAlerts, voiceMuted, allowLeanAlerts]);

  const calculateVirtualCells = () => {
    const packV = safeNum(liveSagVoltage, safeNum(activeBatteryVoltage, 48));
    const safeSeries = Math.max(1, virtualCellSeries);
    const avgCellV = packV / safeSeries;
    const safeAmps = safeNum(currentAmps, 0);
    const cells = [];
    
    for (let i = 1; i <= safeSeries; i++) {
      const cellVariance = ((i % 3 === 0 ? -1 : 1) * ((safeAmps * 0.002) + ((i * 0.003) % 0.015)));
      const cellV = Math.max(2.8, Math.min(4.25, avgCellV + cellVariance));
      cells.push({
        cellNum: i,
        voltage: safeNum(cellV, 3.7),
        status: cellV < 3.25 ? "CRITICAL SAG" : cellV < 3.5 ? "LOW" : "NOMINAL"
      });
    }
    return cells;
  };

  const virtualCells = calculateVirtualCells();
  const maxCellV = virtualCells.length > 0 ? Math.max(...virtualCells.map(c => c.voltage)) : 3.7;
  const minCellV = virtualCells.length > 0 ? Math.min(...virtualCells.map(c => c.voltage)) : 3.7;
  const cellDeltaV = Math.max(0, (maxCellV - minCellV) * 1000);

  return (
    <div className={`${ui?.bgPanel || 'bg-[#0d0e15]'} p-3.5 sm:p-6 rounded-3xl border shadow-2xl transition-colors space-y-4 sm:space-y-5 ${ui?.brd || 'border-zinc-800'}`}>
      
      {/* HEADER & FLEET SELECTOR */}
      <div className={`flex flex-col gap-3.5 border-b ${ui?.brd || 'border-zinc-800'} pb-4 w-full`}>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 sm:gap-3">
            <div className="relative">
              <div className="p-2 sm:p-2.5 rounded-2xl bg-cyan-500/10 border border-cyan-500/40 text-cyan-400 shadow-md">
                <BrainCircuit className="w-5 h-5 sm:w-6 sm:h-6 animate-pulse" />
              </div>
              <span className="absolute -top-1 -right-1 flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-cyan-500"></span>
              </span>
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h3 className={`${ui?.txtMain || 'text-white'} font-black uppercase tracking-widest text-xs sm:text-sm`}>
                  Tactical Copilot Matrix Pro
                </h3>
                
                {garageProfiles.length > 0 ? (
                  <select
                    value={activeVehicleId}
                    onChange={(e) => handleSelectVehicle(e.target.value)}
                    className="text-[9px] font-mono font-black uppercase px-2 py-1 rounded-lg bg-cyan-950/80 border border-cyan-700 text-cyan-300 outline-none cursor-pointer"
                  >
                    {garageProfiles.map(p => (
                      <option key={p.id} value={p.id}>⚡ {p.name} ({p.watts}W • {p.volts}V)</option>
                    ))}
                  </select>
                ) : (
                  <span className="text-[9px] font-mono font-black uppercase px-2 py-0.5 rounded-md bg-cyan-950/60 border border-cyan-800 text-cyan-300">
                    ⚡ {activeVehicleName} ({activeMotorWattage}W • {activeBatteryVoltage}V)
                  </span>
                )}
              </div>
              <p className={`text-[8.5px] sm:text-[9px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase mt-0.5`}>
                Live Satellite Weather AI • ρ={calculatedAirDensity.toFixed(3)} kg/m³ • {Math.round(ambientTemp)}°F ({Math.round(windSpeed)} {localMetric ? 'km/h' : 'mph'} wind)
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={fetchLiveAtmospherics}
              className="min-h-[44px] px-3 rounded-xl border border-zinc-700 bg-zinc-900 text-cyan-400 text-[10px] font-mono font-black uppercase flex items-center gap-1.5 cursor-pointer active:scale-95"
              title="Refresh Live Weather"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={triggerCloudAiAnalysis}
              disabled={isFetchingCloudAi}
              className={`min-h-[44px] px-4 rounded-xl border text-[10px] font-mono font-black uppercase tracking-wider transition-all shadow-md flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 active:scale-95 touch-manipulation ${isFetchingCloudAi ? 'bg-cyan-950/40 text-cyan-500 border-cyan-900/50' : 'bg-cyan-600 hover:bg-cyan-500 text-black border-cyan-500'}`}
            >
              {isFetchingCloudAi ? <Loader2 className="w-4 h-4 animate-spin"/> : <Globe className="w-4 h-4"/>}
              <span>{isFetchingCloudAi ? "Analyzing..." : "Deep Cloud Scan"}</span>
            </button>
          </div>
        </div>

        {/* Mobile Navigation Tabs */}
        <div className="flex gap-1.5 overflow-x-auto custom-scrollbar pb-1 -mx-1 px-1 touch-pan-x">
          {[
            { id: "copilot", label: "AI Neural", icon: BrainCircuit },
            { id: "weather", label: "Weather-AI", icon: Wind },
            { id: "cells", label: `${virtualCellSeries}S Pack`, icon: Battery },
            { id: "range", label: "Kalman Range", icon: Navigation },
            { id: "thermal", label: "I²R Thermals", icon: Flame },
            { id: "history", label: `Fleet (${vehicleHistoryStats.totalSorties})`, icon: Truck },
            { id: "imu", label: "IMU & Slip", icon: Crosshair },
            { id: "audio", label: "Voice HUD", icon: Radio },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveMatrixTab(tab.id as any)}
              className={`min-h-[44px] px-3.5 rounded-xl text-[9.5px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 shrink-0 cursor-pointer active:scale-95 touch-manipulation ${activeMatrixTab === tab.id ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-md ring-1 ring-white/20` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} border ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
            >
              <tab.icon className="w-3.5 h-3.5" /> {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* 1. NEURAL COPILOT */}
      {activeMatrixTab === "copilot" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-0.5`}>AI Efficiency</span>
              <div className="flex items-baseline gap-1">
                <span className={`text-sm font-mono font-black ${ui?.t?.text || 'text-[#39ff14]'}`}>{safeNum(learnedEfficiency, 25)}</span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Wh/{activeDistLabel.toLowerCase()}</span>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-0.5`}>Motor Eff.</span>
              <div className="flex items-baseline gap-1">
                <span className="text-sm font-mono font-black text-cyan-400">{safeNum(motorEfficiencyPct, 88)}%</span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Optimal</span>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-0.5`}>Traction Risk</span>
              <div className="flex items-baseline gap-1">
                <span className={`text-sm font-mono font-black ${slipPrediction.slipRiskScore > 60 ? 'text-rose-500 animate-pulse' : 'text-cyan-400'}`}>
                  {slipPrediction.slipRiskScore}%
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Slip</span>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-0.5`}>Lithium Derate</span>
              <div className="flex items-baseline gap-1">
                <span className={`text-sm font-mono font-black ${weatherBatteryDerate > 15 ? 'text-rose-400' : 'text-emerald-400'}`}>
                  -{weatherBatteryDerate.toFixed(1)}%
                </span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Temp</span>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-0.5`}>Torque Reserve</span>
              <div className="flex items-baseline gap-1">
                <span className="text-sm font-mono font-black text-emerald-400">{motorThermals.torqueHeadroomPct}%</span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>Headroom</span>
              </div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-2.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest mb-0.5`}>Aero Drag</span>
              <div className="flex items-baseline gap-1">
                <span className="text-sm font-mono font-black text-rose-400">{Math.round(safeNum(aeroDragForce, 0))}</span>
                <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} uppercase font-mono`}>N</span>
              </div>
            </div>
          </div>

          <div className={`p-4 rounded-2xl bg-black/60 border ${(aiAdvice?.color || '').includes('rose') ? 'border-rose-900 shadow-[0_0_20px_rgba(244,63,94,0.3)]' : 'border-zinc-800'} flex flex-col gap-3 shadow-inner`}>
            <div className="flex items-start gap-3">
              <Sparkles className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5 animate-bounce" />
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-1.5">
                  <span className="text-[9px] font-mono font-black uppercase text-cyan-400 tracking-wider">
                    LOCAL NEURAL ADVISORY ({activeVehicleName})
                  </span>
                  <span className={`text-[8px] font-mono px-2 py-0.5 rounded border uppercase font-bold ${aiAdvice?.color || 'text-cyan-400'}`}>
                    {aiAdvice?.status || 'OPTIMIZED'}
                  </span>
                </div>
                <p className="text-xs font-mono font-bold text-zinc-200 leading-relaxed">
                  {aiAdvice?.msg || 'All telemetry parameters nominal.'}
                </p>
              </div>
            </div>

            <button
              onClick={() => speakText(`Tactical advisory for ${activeVehicleName}: ${aiAdvice?.msg || 'Nominal'}`)}
              className={`w-full min-h-[44px] px-4 rounded-xl border text-[9.5px] font-mono font-black uppercase tracking-widest flex items-center justify-center gap-2 transition-all cursor-pointer active:scale-95 touch-manipulation ${speechActive ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
            >
              <Volume2 className="w-4 h-4"/> Vocalize Tactical Alert
            </button>
          </div>

          <AnimatePresence>
            {(cloudAiAnalysis || isFetchingCloudAi) && (
              <motion.div 
                initial={{ opacity: 0, height: 0 }} 
                animate={{ opacity: 1, height: 'auto' }} 
                exit={{ opacity: 0, height: 0 }}
                className="p-4 rounded-2xl bg-purple-950/20 border border-purple-900/50 flex flex-col gap-3 shadow-inner"
              >
                <div className="flex items-center gap-2">
                  <Globe className={`w-4 h-4 ${isFetchingCloudAi ? 'text-purple-400 animate-spin' : 'text-purple-400'}`} />
                  <span className="text-[9px] font-mono font-black uppercase tracking-widest text-purple-400">
                    Cloud Neural Matrix Debrief ({activeVehicleName})
                  </span>
                </div>

                <div className="text-xs font-mono font-bold text-purple-100/90 leading-relaxed pl-3 border-l-2 border-purple-500/40">
                  {cloudAiAnalysis || `Transmitting ${activeMotorWattage}W powertrain telemetry and live atmospheric metrics to Gemini neural net...`}
                </div>

                {cloudAiAnalysis && (
                  <button 
                    onClick={() => speakText(cloudAiAnalysis)}
                    className="w-full min-h-[44px] rounded-xl border border-purple-800/60 bg-purple-900/30 text-[9.5px] font-mono uppercase font-bold text-purple-200 hover:text-white flex items-center justify-center gap-2 cursor-pointer active:scale-95 touch-manipulation"
                  >
                    <Volume2 className="w-4 h-4"/> Listen to Full Debrief
                  </button>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      )}

      {/* 2. ATMOSPHERIC WEATHER-AI MATRIX TAB */}
      {activeMatrixTab === "weather" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <div className="flex justify-between items-center text-[7.5px] font-mono text-zinc-400 uppercase">
                <span>Headwind Vector</span>
                <Wind className="w-3.5 h-3.5 text-cyan-400" />
              </div>
              <span className={`text-base font-mono font-black ${headwindComponent > 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
                {headwindComponent > 0 ? `+${headwindComponent.toFixed(1)}` : headwindComponent.toFixed(1)} {activeSpeedLabel.toLowerCase()}
              </span>
              <span className="text-[7px] font-mono text-zinc-500 block">
                {headwindComponent > 0 ? "Opposing Drag" : "Assisting Tailwind"}
              </span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <div className="flex justify-between items-center text-[7.5px] font-mono text-zinc-400 uppercase">
                <span>Air Density (ρ)</span>
                <Activity className="w-3.5 h-3.5 text-purple-400" />
              </div>
              <span className="text-base font-mono font-black text-purple-400">
                {calculatedAirDensity.toFixed(3)}
              </span>
              <span className="text-[7.5px] font-mono text-zinc-500 block">kg/m³ Drag Multiplier</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <div className="flex justify-between items-center text-[7.5px] font-mono text-zinc-400 uppercase">
                <span>Tire Friction</span>
                <Disc className="w-3.5 h-3.5 text-emerald-400" />
              </div>
              <span className={`text-base font-mono font-black ${weatherSurfaceGrip.color}`}>
                {weatherSurfaceGrip.grip}%
              </span>
              <span className="text-[7px] font-mono text-zinc-500 block">{weatherSurfaceGrip.desc}</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <div className="flex justify-between items-center text-[7.5px] font-mono text-zinc-400 uppercase">
                <span>Solar Heat Influx</span>
                <Sun className="w-3.5 h-3.5 text-yellow-400" />
              </div>
              <span className="text-base font-mono font-black text-yellow-400">
                {Math.round(directRadiation)} W/m²
              </span>
              <span className="text-[7px] font-mono text-zinc-500 block">{Math.round(ambientTemp)}° Asphalt</span>
            </div>
          </div>
        </motion.div>
      )}

      {/* 3. VIRTUAL PACK CELL BALANCER */}
      {activeMatrixTab === "cells" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center bg-black/40 p-3 rounded-2xl border border-zinc-800 gap-2">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-black uppercase tracking-widest text-white flex items-center gap-1.5">
                  <Battery className="w-4 h-4 text-emerald-400" /> Virtual Cell Matrix
                </span>
                <select
                  value={virtualCellSeries}
                  onChange={(e) => setVirtualCellSeries(Number(e.target.value))}
                  className="bg-black text-[9px] font-mono font-bold text-cyan-400 border border-zinc-700 px-2 py-1 rounded-lg outline-none cursor-pointer"
                >
                  <option value={10}>10S (36V)</option>
                  <option value={13}>13S (48V)</option>
                  <option value={14}>14S (52V)</option>
                  <option value={16}>16S (60V)</option>
                  <option value={20}>20S (72V)</option>
                  <option value={24}>24S (84V)</option>
                </select>
              </div>
              <p className="text-[8.5px] text-zinc-400 font-mono mt-0.5">
                Active Vehicle: {activeVehicleName} • {activeBatteryVoltage}V Pack
              </p>
            </div>

            <div className="flex items-center gap-3 w-full sm:w-auto justify-between sm:justify-end pt-2 sm:pt-0 border-t sm:border-t-0 border-zinc-800">
              <div className="text-left sm:text-right font-mono">
                <span className="text-[7.5px] text-zinc-500 uppercase block">Pack ΔV Delta</span>
                <span className={`text-xs font-black ${cellDeltaV > 80 ? 'text-rose-400 animate-pulse' : 'text-emerald-400'}`}>
                  {safeNum(cellDeltaV, 0).toFixed(1)} mV
                </span>
              </div>
              <div className="text-right font-mono border-l border-zinc-800 pl-3">
                <span className="text-[7.5px] text-zinc-500 uppercase block">Loaded Voltage</span>
                <span className="text-xs font-black text-cyan-400 tabular-nums">
                  {safeNum(liveSagVoltage, safeNum(activeBatteryVoltage, 48)).toFixed(1)} V
                </span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-7 gap-2">
            {virtualCells.map(c => (
              <div 
                key={c.cellNum}
                className={`p-2.5 rounded-xl border flex flex-col justify-between ${c.status === "CRITICAL SAG" ? 'bg-rose-950/40 border-rose-800 text-rose-400' : c.status === "LOW" ? 'bg-amber-950/30 border-amber-800 text-amber-300' : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} text-zinc-200`}`}
              >
                <div className="flex justify-between items-center text-[7.5px] font-mono font-bold opacity-70">
                  <span>C{c.cellNum < 10 ? `0${c.cellNum}` : c.cellNum}</span>
                  <span>{c.status === "NOMINAL" ? "OK" : "SAG"}</span>
                </div>
                <div className="text-center my-1">
                  <span className="text-xs font-black font-mono tabular-nums">{safeNum(c.voltage, 3.7).toFixed(3)}</span>
                  <span className="text-[7px] font-mono block opacity-60">VOLTS</span>
                </div>
                <div className="w-full bg-black/60 h-1.5 rounded-full overflow-hidden">
                  <div 
                    className={`h-full ${c.voltage < 3.3 ? 'bg-rose-500' : c.voltage < 3.6 ? 'bg-amber-500' : 'bg-emerald-400'}`}
                    style={{ width: `${Math.max(0, Math.min(100, ((c.voltage - 3.0) / 1.2) * 100))}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      )}

      {/* 4. KALMAN RANGE BUDGET */}
      {activeMatrixTab === "range" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1`}>
              <Navigation className="w-4 h-4 text-cyan-400 mx-auto mb-0.5" />
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase block`}>Return-to-Home Radius</span>
              <span className="text-xl font-mono font-black text-cyan-400">{returnHomeBudget.safeHomeRadius} {activeDistLabel}</span>
              <span className="text-[7.5px] font-mono text-zinc-500 block">Weather & 15% reserve buffer</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1`}>
              <Gauge className="w-4 h-4 text-emerald-400 mx-auto mb-0.5" />
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase block`}>Absolute Max Range</span>
              <span className="text-xl font-mono font-black text-emerald-400">{returnHomeBudget.maxRangeMiles} {activeDistLabel}</span>
              <span className="text-[7.5px] font-mono text-zinc-500 block">For {activeVehicleName}</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1`}>
              <Battery className="w-4 h-4 text-amber-400 mx-auto mb-0.5" />
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase block`}>Energy Budget</span>
              <span className="text-xl font-mono font-black text-amber-400">{returnHomeBudget.totalCapacityWh} Wh</span>
              <span className="text-[7.5px] font-mono text-zinc-500 block">{returnHomeBudget.status}</span>
            </div>
          </div>
        </motion.div>
      )}

      {/* 5. I²R MOTOR THERMAL & TORQUE HEADROOM */}
      {activeMatrixTab === "thermal" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1`}>
              <Flame className="w-4 h-4 text-rose-400 mx-auto mb-0.5" />
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase block`}>I²R Joule Loss</span>
              <span className="text-xl font-mono font-black text-rose-400">{motorThermals.jouleHeatingWatts} W</span>
              <span className="text-[7.5px] font-mono text-zinc-500 block">Stator phase heat generation</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1`}>
              <Cpu className="w-4 h-4 text-cyan-400 mx-auto mb-0.5" />
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase block`}>Torque Reserve</span>
              <span className="text-xl font-mono font-black text-cyan-400">{motorThermals.torqueHeadroomPct}%</span>
              <span className="text-[7.5px] font-mono text-zinc-500 block">Before thermal foldback</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1`}>
              <Thermometer className="w-4 h-4 text-amber-400 mx-auto mb-0.5" />
              <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase block`}>Thermal Status</span>
              <span className="text-xs font-mono font-black text-amber-300 block pt-1">{motorThermals.thermalWarning}</span>
              <span className="text-[7.5px] font-mono text-zinc-500 block">{Math.round(safeNum(estStatorTemp, 75))}°F Winding</span>
            </div>
          </div>
        </motion.div>
      )}

      {/* 6. VEHICLE FLEET HISTORY */}
      {activeMatrixTab === "history" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          <div className="bg-black/40 p-3.5 rounded-2xl border border-zinc-800 space-y-2">
            <span className="text-[9px] font-mono font-black uppercase text-cyan-400 tracking-wider block">
              Fleet History Aggregator ({activeVehicleName})
            </span>
            <div className="grid grid-cols-3 gap-2 pt-1">
              <div className="bg-zinc-900 p-2 rounded-xl text-center">
                <span className="text-[7px] text-zinc-500 uppercase block">Total Sorties</span>
                <span className="text-xs font-mono font-black text-white">{vehicleHistoryStats.totalSorties}</span>
              </div>
              <div className="bg-zinc-900 p-2 rounded-xl text-center">
                <span className="text-[7px] text-zinc-500 uppercase block">Total Distance</span>
                <span className="text-xs font-mono font-black text-cyan-400">{vehicleHistoryStats.accumDist} {activeDistLabel}</span>
              </div>
              <div className="bg-zinc-900 p-2 rounded-xl text-center">
                <span className="text-[7px] text-zinc-500 uppercase block">Top Speed</span>
                <span className="text-xs font-mono font-black text-amber-400">{vehicleHistoryStats.maxSpeedFound} {activeSpeedLabel}</span>
              </div>
            </div>
          </div>
        </motion.div>
      )}

      {/* 7. IMU SENSOR FUSION */}
      {activeMatrixTab === "imu" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <div className="flex justify-between items-center text-[7.5px] font-mono font-bold text-zinc-500 uppercase">
                <span>Roll Lean</span>
                <RotateCw className="w-3.5 h-3.5 text-cyan-400" />
              </div>
              <span className="text-lg font-mono font-black text-cyan-400 tabular-nums my-1">{Math.round(safeNum(leanAngle, 0))}°</span>
              <span className="text-[7px] font-mono text-zinc-500 uppercase">Peak: {Math.round(peakRecordedLean)}°</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <div className="flex justify-between items-center text-[7.5px] font-mono font-bold text-zinc-500 uppercase">
                <span>Pitch Incline</span>
                <Crosshair className="w-3.5 h-3.5 text-amber-400" />
              </div>
              <span className="text-lg font-mono font-black text-amber-400 tabular-nums my-1">{Math.round(safeNum(pitchAngle, 0))}°</span>
              <span className="text-[7px] font-mono text-zinc-500 uppercase">Vector</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <div className="flex justify-between items-center text-[7.5px] font-mono font-bold text-zinc-500 uppercase">
                <span>Total G-Force</span>
                <Scale className="w-3.5 h-3.5 text-rose-400" />
              </div>
              <span className="text-lg font-mono font-black text-rose-400 tabular-nums my-1">{hardwareGForce.toFixed(2)} G</span>
              <span className="text-[7px] font-mono text-zinc-500 uppercase">Peak: {peakRecordedG.toFixed(2)} G</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-xl border ${ui?.brd || 'border-zinc-800'} flex flex-col justify-between shadow-inner`}>
              <div className="flex justify-between items-center text-[7.5px] font-mono font-bold text-zinc-500 uppercase">
                <span>Slip Risk</span>
                <ShieldAlert className="w-3.5 h-3.5 text-purple-400" />
              </div>
              <span className={`text-lg font-mono font-black tabular-nums my-1 ${slipPrediction.color}`}>{slipPrediction.slipRiskScore}%</span>
              <span className="text-[7px] font-mono text-zinc-500 uppercase">Traction</span>
            </div>
          </div>
        </motion.div>
      )}

      {/* 8. TACTICAL VOICE HUD */}
      {activeMatrixTab === "audio" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5`}>
              <div className="flex justify-between items-center">
                <span className="text-xs font-black uppercase tracking-widest text-white flex items-center gap-2">
                  <Volume2 className="w-4 h-4 text-cyan-400" /> Voice Alerts
                </span>
                <button
                  onClick={() => {
                    const newMute = !voiceMuted;
                    setVoiceMuted(newMute);
                    localStorage.setItem("rt_voice_muted", newMute ? "true" : "false");
                    if (!newMute) speakText("Voice synthesis enabled.");
                  }}
                  className={`min-h-[40px] px-3.5 text-[9px] font-mono font-black uppercase border rounded-xl transition-all cursor-pointer active:scale-95 touch-manipulation ${voiceMuted ? 'bg-rose-950/40 border-rose-900 text-rose-400' : 'bg-emerald-950/40 border-emerald-800 text-emerald-400'}`}
                >
                  {voiceMuted ? "MUTED" : "ACTIVE"}
                </button>
              </div>

              <div className="flex justify-between items-center pt-2 border-t border-zinc-800">
                <span className="text-[9.5px] font-mono font-bold uppercase text-zinc-300">
                  Auto-Hazard Alerts
                </span>
                <button
                  onClick={() => {
                    const newAuto = !autoVoiceAlerts;
                    setAutoVoiceAlerts(newAuto);
                    localStorage.setItem("rt_voice_auto", newAuto ? "true" : "false");
                  }}
                  className={`min-h-[36px] px-3 text-[8.5px] font-mono font-bold uppercase rounded-lg border transition-all cursor-pointer ${autoVoiceAlerts ? 'bg-cyan-950/40 border-cyan-800 text-cyan-400' : 'bg-zinc-900 border-zinc-800 text-zinc-500'}`}
                >
                  {autoVoiceAlerts ? "AUTO ON" : "OFF"}
                </button>
              </div>
            </div>

            <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5`}>
              <div className="flex justify-between items-center text-xs font-black uppercase tracking-widest text-white">
                <span>Speed</span>
                <span className="font-mono text-cyan-400">{voiceSpeed.toFixed(2)}x</span>
              </div>
              <input
                type="range" min="0.8" max="1.5" step="0.05"
                value={voiceSpeed}
                onChange={e => {
                  const val = Number(e.target.value);
                  setVoiceSpeed(val);
                  localStorage.setItem("rt_voice_rate", val.toString());
                }}
                className="w-full accent-cyan-400 cursor-pointer h-3 bg-zinc-800 rounded-lg touch-manipulation"
              />
              <button
                onClick={() => speakText(`Tactical audio operational for ${activeVehicleName}. Telemetry link optimal.`)}
                className="w-full min-h-[40px] bg-zinc-800 hover:bg-zinc-700 active:scale-95 text-white rounded-xl text-[9.5px] font-mono uppercase font-black tracking-widest border border-zinc-700 cursor-pointer touch-manipulation flex items-center justify-center gap-1.5"
              >
                <Radio className="w-3.5 h-3.5 text-cyan-400"/> Test Voice Dispatch
              </button>
            </div>
          </div>
        </motion.div>
      )}

    </div>
  );
}