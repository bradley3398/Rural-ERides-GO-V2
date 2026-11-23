import React, { useState, useMemo } from "react";
import { 
  Thermometer, MapPin, Search, Sun, Eye, Activity, 
  Waves, Droplets, CloudRain, Sunrise, Sunset, CalendarDays, 
  Snowflake, Zap, Wind, Compass, ShieldAlert, 
  Sparkles, RefreshCw, Gauge, Umbrella, 
  Flame, Battery, Clock, AlertTriangle, Layers, Navigation,
  Disc, Cloud
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

const safeNum = (val: any, fallback = 0): number => {
  const parsed = Number(val);
  return isNaN(parsed) || !isFinite(parsed) ? fallback : parsed;
};

export interface WeatherMatrixProps {
  weatherData?: any;
  weatherInput?: string;
  setWeatherInput?: (val: string) => void;
  handleLocationSearch?: () => void;
  isFetchingWeather?: boolean;
  savedLocationName?: string;
  rideSafetyRating?: { text: string; color: string };
  useMetric?: boolean;
  formatTimeFromIso?: (iso: string) => string;
  getWeatherIcon?: (code: number) => React.ReactNode;
  getCardinalDirection?: (angle: number | null | undefined) => string;
  ui?: any;
  tx?: any;
  localeCode?: string;
  compassHeading?: number;
  currentSpeed?: number;
}

export default function WeatherMatrix(props: WeatherMatrixProps) {
  const {
    weatherData = {},
    weatherInput = "",
    setWeatherInput = () => {},
    handleLocationSearch = () => {},
    isFetchingWeather = false,
    savedLocationName = "Local GPS Lock",
    rideSafetyRating = { text: "OPTIMAL: Clear Skies", color: "text-[#39ff14]" },
    useMetric = false,
    formatTimeFromIso,
    getWeatherIcon,
    getCardinalDirection,
    ui,
    tx,
    localeCode = "en",
    compassHeading = 0,
    currentSpeed = 0
  } = props;

  const [activeTab, setActiveTab] = useState<"overview" | "hourly" | "aerodynamics" | "thermal" | "traction" | "forecast">("overview");

  const safeFormatTime = (iso: string): string => {
    if (typeof formatTimeFromIso === "function") {
      try { return formatTimeFromIso(iso); } catch (e) {}
    }
    try {
      return new Date(iso).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
    } catch (e) {
      return iso || "--";
    }
  };

  const safeGetWeatherIcon = (code: number): React.ReactNode => {
    if (typeof getWeatherIcon === "function") {
      try { return getWeatherIcon(code); } catch (e) {}
    }
    if (code <= 1) return <Sun className="text-yellow-400 w-5 h-5" />;
    if (code <= 3) return <Cloud className="text-zinc-400 w-5 h-5" />;
    if (code <= 67) return <CloudRain className="text-cyan-400 w-5 h-5" />;
    if (code <= 82) return <Snowflake className="text-cyan-200 w-5 h-5" />;
    return <Zap className="text-amber-400 w-5 h-5" />;
  };

  const safeCardinal = (angle: number | null | undefined): string => {
    if (typeof getCardinalDirection === "function") {
      try { return getCardinalDirection(angle); } catch (e) {}
    }
    if (angle === null || angle === undefined || isNaN(angle)) return "N/A";
    const val = Math.floor((angle / 22.5) + 0.5);
    const arr = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"];
    return arr[(val % 16 + 16) % 16];
  };

  const current = weatherData?.current || {};
  const daily = weatherData?.daily || {};
  const hourly = weatherData?.hourly || {};

  const currentTemp = safeNum(current.temperature_2m, 72);
  const windSpeed = safeNum(current.wind_speed_10m, 0);
  const windGusts = safeNum(current.wind_gusts_10m, windSpeed);
  const windDir = safeNum(current.wind_direction_10m, 0);
  const pressureHpa = safeNum(current.surface_pressure, 1013.25);
  const relativeHumidity = safeNum(current.relative_humidity_2m, 40);
  const uvIndex = safeNum(current.uv_index, 0);
  const precipitation = safeNum(current.precipitation, 0);
  const visibilityMeters = safeNum(current.visibility, 10000);
  const directRadiation = safeNum(current.direct_radiation, 0);

  // 1. Aerodynamic Air Density Calculation (ρ in kg/m³)
  const tempKelvin = (useMetric ? currentTemp : (currentTemp - 32) * (5 / 9)) + 273.15;
  const airDensity = (pressureHpa * 100) / (287.058 * tempKelvin);

  // 2. Relative Wind Vectors
  const angleDiffRad = ((windDir - compassHeading) * Math.PI) / 180;
  const headwindComponent = Math.cos(angleDiffRad) * windSpeed;
  const crosswindComponent = Math.sin(angleDiffRad) * windSpeed;

  // 3. Dynamic Apparent Wind Chill
  const effectiveWindSpeed = windSpeed + safeNum(currentSpeed, 0);
  const apparentWindChill = useMemo(() => {
    const tempF = useMetric ? (currentTemp * 1.8) + 32 : currentTemp;
    const effectiveMph = useMetric ? effectiveWindSpeed * 0.621371 : effectiveWindSpeed;
    if (tempF <= 50 && effectiveMph > 3) {
      const chillF = 35.74 + (0.6215 * tempF) - (35.75 * Math.pow(effectiveMph, 0.16)) + (0.4275 * tempF * Math.pow(effectiveMph, 0.16));
      return useMetric ? Math.round((chillF - 32) * (5 / 9)) : Math.round(chillF);
    }
    return Math.round(currentTemp);
  }, [currentTemp, effectiveWindSpeed, useMetric]);

  // 4. Lithium Battery Capacity Derating Model
  const ambientCelsius = useMetric ? currentTemp : (currentTemp - 32) * (5 / 9);
  const batteryCapacityDerate = useMemo(() => {
    if (ambientCelsius >= 20 && ambientCelsius <= 30) return 0;
    if (ambientCelsius < 0) return Math.min(45, Math.abs(ambientCelsius) * 1.8 + 15);
    if (ambientCelsius < 15) return (15 - ambientCelsius) * 1.2;
    if (ambientCelsius > 35) return (ambientCelsius - 35) * 0.8;
    return 0;
  }, [ambientCelsius]);

  // 5. Tire Traction & Surface Grip Index
  const surfaceTraction = useMemo(() => {
    let gripScore = 100;
    let surfaceStatus = "OPTIMAL DRY TARMAC";
    let statusColor = "text-emerald-400";

    if (precipitation > 0) {
      gripScore -= Math.min(50, precipitation * (useMetric ? 8 : 200));
      surfaceStatus = "WET / HYDROPLANE RISK";
      statusColor = "text-rose-500 animate-pulse";
    } else if (relativeHumidity > 85 && currentTemp < (useMetric ? 5 : 41)) {
      gripScore -= 25;
      surfaceStatus = "DAMP / DEW SLICKNESS";
      statusColor = "text-amber-400";
    }

    if (currentTemp <= (useMetric ? 1 : 34)) {
      gripScore = Math.min(gripScore, 35);
      surfaceStatus = "BLACK ICE / FREEZE HAZARD";
      statusColor = "text-rose-500 animate-pulse";
    }

    return { gripScore: Math.max(15, Math.round(gripScore)), surfaceStatus, statusColor };
  }, [precipitation, relativeHumidity, currentTemp, useMetric]);

  // 6. Tire PSI Temperature Compensation
  const psiCompensation = useMemo(() => {
    const tempF = useMetric ? (currentTemp * 1.8) + 32 : currentTemp;
    const variance = (tempF - 70) / 10;
    return {
      variance: variance.toFixed(1),
      status: variance < -2 ? "PRESSURE LOSS WARNING" : variance > 2 ? "OVER-INFLATION RISK" : "NOMINAL PRESSURE",
      color: variance < -2 ? "text-amber-400" : variance > 2 ? "text-rose-400" : "text-emerald-400"
    };
  }, [currentTemp, useMetric]);

  // 7. Robust 12-Hour Timeline Aggregator
  const next12Hours = useMemo(() => {
    if (!hourly?.time || !Array.isArray(hourly.time) || hourly.time.length === 0) return [];
    
    // Find closest hour index matching current time
    const nowMs = Date.now();
    let startIdx = hourly.time.findIndex((t: string) => {
      const entryTime = new Date(t).getTime();
      return !isNaN(entryTime) && entryTime >= nowMs - (1000 * 60 * 45); // Within 45m of current hour
    });

    if (startIdx < 0) startIdx = 0;

    return hourly.time.slice(startIdx, startIdx + 12).map((timeStr: string, i: number) => {
      const idx = startIdx + i;
      return {
        time: safeFormatTime(timeStr),
        temp: Math.round(safeNum(hourly.temperature_2m?.[idx], currentTemp)),
        precipProb: safeNum(hourly.precipitation_probability?.[idx], 0),
        wind: Math.round(safeNum(hourly.wind_speed_10m?.[idx], windSpeed)),
        code: safeNum(hourly.weather_code?.[idx], 0)
      };
    });
  }, [hourly, currentTemp, windSpeed]);

  const windUnit = useMetric ? "km/h" : "mph";
  const tempUnit = "°";
  const isDanger = rideSafetyRating?.color ? rideSafetyRating.color.includes('rose') : false;

  return (
    <div className={`${ui?.bgPanel || 'bg-[#0d0e15]'} p-3.5 sm:p-6 rounded-3xl shadow-2xl relative overflow-hidden flex flex-col border transition-colors ${ui?.brd || 'border-zinc-800'} space-y-4`}>
      
      {/* HEADER & LOCATION SEARCH BAR */}
      <div className={`flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b ${ui?.brd || 'border-zinc-800'} pb-3.5 relative z-10 shrink-0`}>
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-2xl bg-[#39ff14]/10 border border-[#39ff14]/30 text-[#39ff14] shadow-md">
            <Thermometer className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <h3 className={`${ui?.txtMain || 'text-white'} font-black uppercase tracking-widest text-xs sm:text-sm flex items-center gap-2`}>
              {tx ? tx('weather') : "Atmospheric & Microclimate Matrix"}
            </h3>
            <span className={`text-[8.5px] ${ui?.txtMuted || 'text-zinc-400'} flex items-center gap-1.5 font-mono uppercase mt-0.5`}>
              <MapPin className="w-3 h-3 text-cyan-400 shrink-0"/> {savedLocationName}
            </span>
          </div>
        </div>

        {/* City Search Bar */}
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <div className={`flex flex-1 sm:flex-none ${ui?.bgBase || 'bg-[#06060a]'} rounded-xl border overflow-hidden transition-colors ${ui?.brd || 'border-zinc-800'} focus-within:border-cyan-500 shadow-inner w-full sm:w-56`}>
            <input 
              value={weatherInput} 
              onChange={(e) => setWeatherInput(e.target.value)} 
              onKeyDown={(e) => e.key === "Enter" && handleLocationSearch()}
              placeholder="City / Coordinates..."
              className={`min-h-[44px] bg-transparent text-xs font-mono font-bold ${ui?.txtMain || 'text-white'} px-3 outline-none w-full`}
            />
            <button 
              onClick={handleLocationSearch} 
              disabled={isFetchingWeather}
              className={`min-h-[44px] min-w-[44px] ${ui?.bgList || 'bg-[#121318]'} transition-colors flex items-center justify-center cursor-pointer text-cyan-400 active:scale-95 touch-manipulation`}
            >
              {isFetchingWeather ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Search className="w-3.5 h-3.5" />}
            </button>
          </div>
        </div>
      </div>

      {/* HORIZONTAL NAVIGATION TABS */}
      <div className="flex gap-1.5 overflow-x-auto custom-scrollbar pb-1 -mx-1 px-1 touch-pan-x">
        {[
          { id: "overview", label: "Overview", icon: CloudRain },
          { id: "hourly", label: `12-Hr Timeline (${next12Hours.length})`, icon: Clock },
          { id: "aerodynamics", label: "Aero Vectors", icon: Wind },
          { id: "traction", label: "Tire Grip", icon: Disc },
          { id: "thermal", label: "Battery & Heat", icon: Battery },
          { id: "forecast", label: "10-Day Outlook", icon: CalendarDays },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`min-h-[44px] px-3.5 rounded-xl text-[9.5px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 shrink-0 cursor-pointer active:scale-95 touch-manipulation ${activeTab === tab.id ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-md ring-1 ring-white/20` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} border ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
          >
            <tab.icon className="w-3.5 h-3.5" /> {tab.label}
          </button>
        ))}
      </div>

      {isFetchingWeather ? (
        <div className={`flex-1 flex flex-col items-center justify-center py-16 text-xs font-mono font-bold uppercase tracking-widest animate-pulse ${ui?.t?.text || 'text-[#39ff14]'} gap-3`}>
          <RefreshCw className="w-7 h-7 animate-spin text-cyan-400" />
          <span>Synchronizing Weather Satellite Radar...</span>
        </div>
      ) : weatherData && weatherData.current ? (
        <div className="relative z-10 flex-1 flex flex-col justify-between space-y-3.5">
          
          {/* SAFETY VECTOR & LIVE CONDITION BANNER */}
          <div className={`p-3 rounded-2xl bg-black/60 border ${isDanger ? 'border-rose-900 shadow-[0_0_20px_rgba(244,63,94,0.3)]' : 'border-zinc-800'} flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 shadow-inner`}>
            <div className="flex items-center gap-2">
              <ShieldAlert className={`w-4 h-4 ${rideSafetyRating?.color || 'text-[#39ff14]'}`} />
              <span className="text-[9.5px] font-mono font-black uppercase tracking-widest text-zinc-300">
                Atmospheric Ride Flight Safety
              </span>
            </div>
            <span className={`text-[9.5px] font-mono font-black uppercase tracking-wider px-2.5 py-0.5 rounded-lg border ${isDanger ? 'bg-rose-950/40 border-rose-800 text-rose-400' : 'bg-[#39ff14]/10 border-[#39ff14]/40 text-[#39ff14]'}`}>
              {rideSafetyRating?.text || "OPTIMAL"}
            </span>
          </div>

          {/* --- TAB 1: CORE OVERVIEW --- */}
          {activeTab === "overview" && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
              <div className="flex flex-wrap items-center gap-6 p-4 rounded-2xl bg-black/40 border border-zinc-800">
                <div className={`${ui?.txtMain || 'text-white'} text-5xl sm:text-6xl font-black font-mono tracking-tighter tabular-nums drop-shadow-md`}>
                  {Math.round(currentTemp)}{tempUnit}
                </div>
                
                <div className="border-l border-zinc-800 pl-4 sm:pl-6 space-y-1 font-mono">
                  <div className={`text-xs ${ui?.txtMuted || 'text-zinc-400'} font-bold uppercase tracking-wider`}>
                    Peak: <span className="text-white">{Math.round(safeNum(daily?.temperature_2m_max?.[0], currentTemp))}°</span> • Low: <span className="text-white">{Math.round(safeNum(daily?.temperature_2m_min?.[0], currentTemp))}°</span>
                  </div>
                  <div className={`text-xs font-bold uppercase tracking-wider ${ui?.t?.text || 'text-[#39ff14]'}`}>
                    Wind: {Math.round(windSpeed)} {windUnit} ({safeCardinal(windDir)})
                  </div>
                  <div className="text-xs uppercase tracking-wider text-rose-400 font-bold flex items-center gap-1">
                    Gusts: {Math.round(windGusts)} {windUnit}
                    {windGusts > (useMetric ? 32 : 20) && <AlertTriangle className="w-3 h-3 text-rose-500 animate-pulse" />}
                  </div>
                </div>
              </div>

              {/* Sensor Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2">
                <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 flex items-center gap-2.5 shadow-inner`}>
                  <Sun className="w-4 h-4 text-amber-400 shrink-0" />
                  <div>
                    <div className={`text-[7.5px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Solar UV Index</div>
                    <div className={`text-xs font-mono font-bold ${ui?.txtMain || 'text-white'}`}>{uvIndex.toFixed(1)} / 11</div>
                  </div>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 flex items-center gap-2.5 shadow-inner`}>
                  <Eye className="w-4 h-4 text-cyan-400 shrink-0" />
                  <div>
                    <div className={`text-[7.5px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Visibility Range</div>
                    <div className={`text-xs font-mono font-bold ${ui?.txtMain || 'text-white'}`}>
                      {useMetric ? `${(visibilityMeters / 1000).toFixed(1)} km` : `${(visibilityMeters / 1609.34).toFixed(1)} mi`}
                    </div>
                  </div>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 flex items-center gap-2.5 shadow-inner`}>
                  <Activity className="w-4 h-4 text-purple-400 shrink-0" />
                  <div>
                    <div className={`text-[7.5px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Baro Pressure</div>
                    <div className={`text-xs font-mono font-bold ${ui?.txtMain || 'text-white'}`}>{pressureHpa.toFixed(0)} hPa</div>
                  </div>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 flex items-center gap-2.5 shadow-inner`}>
                  <Thermometer className="w-4 h-4 text-rose-400 shrink-0" />
                  <div>
                    <div className={`text-[7.5px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Apparent Feels</div>
                    <div className={`text-xs font-mono font-bold ${ui?.txtMain || 'text-white'}`}>{Math.round(safeNum(current.apparent_temperature, currentTemp))}°</div>
                  </div>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 flex items-center gap-2.5 shadow-inner`}>
                  <Droplets className="w-4 h-4 text-cyan-400 shrink-0" />
                  <div>
                    <div className={`text-[7.5px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Relative Humidity</div>
                    <div className={`text-xs font-mono font-bold ${ui?.txtMain || 'text-white'}`}>{relativeHumidity}%</div>
                  </div>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 flex items-center gap-2.5 shadow-inner`}>
                  <CloudRain className="w-4 h-4 text-emerald-400 shrink-0" />
                  <div>
                    <div className={`text-[7.5px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Precipitation</div>
                    <div className={`text-xs font-mono font-bold ${ui?.txtMain || 'text-white'}`}>{precipitation} {useMetric ? 'mm' : 'in'}</div>
                  </div>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 flex items-center gap-2.5 shadow-inner`}>
                  <Sunrise className="w-4 h-4 text-amber-400 shrink-0" />
                  <div>
                    <div className={`text-[7.5px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Dawn Sunrise</div>
                    <div className={`text-xs font-mono font-bold ${ui?.txtMain || 'text-white'}`}>
                      {daily?.sunrise?.[0] ? safeFormatTime(daily.sunrise[0]) : "06:00 AM"}
                    </div>
                  </div>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} rounded-xl p-3 flex items-center gap-2.5 shadow-inner`}>
                  <Sunset className="w-4 h-4 text-orange-400 shrink-0" />
                  <div>
                    <div className={`text-[7.5px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'}`}>Dusk Sunset</div>
                    <div className={`text-xs font-mono font-bold ${ui?.txtMain || 'text-white'}`}>
                      {daily?.sunset?.[0] ? safeFormatTime(daily.sunset[0]) : "08:30 PM"}
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {/* --- TAB 2: 12-HOUR TIMELINE MATRIX --- */}
          {activeTab === "hourly" && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
              {next12Hours.length === 0 ? (
                <div className="text-center py-8 text-xs font-mono text-zinc-500 uppercase">
                  Hourly trajectory data synchronizing...
                </div>
              ) : (
                <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-2">
                  {next12Hours.map((hour, idx) => (
                    <div key={idx} className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-2.5 rounded-xl text-center flex flex-col items-center justify-between min-w-[76px] h-32 shrink-0 shadow-inner`}>
                      <span className={`text-[8.5px] font-mono font-black ${ui?.txtMuted || 'text-zinc-400'} uppercase`}>
                        {hour.time}
                      </span>
                      <div className="my-1 scale-95">
                        {safeGetWeatherIcon(hour.code)}
                      </div>
                      <span className="text-xs font-mono font-black text-white">{hour.temp}°</span>
                      <div className="w-full space-y-0.5">
                        <div className="text-[7.5px] font-mono text-cyan-400">{hour.wind} {windUnit}</div>
                        {hour.precipProb > 0 && (
                          <div className="text-[7.5px] font-mono text-blue-400 font-bold">{hour.precipProb}% rain</div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </motion.div>
          )}

          {/* --- TAB 3: AERODYNAMICS & WIND VECTORS --- */}
          {activeTab === "aerodynamics" && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1 shadow-inner`}>
                  <div className="flex justify-between items-center text-[8px] font-mono text-zinc-400 uppercase">
                    <span>Effective Headwind</span>
                    <Wind className={`w-3.5 h-3.5 ${headwindComponent > 0 ? 'text-rose-400' : 'text-emerald-400'}`} />
                  </div>
                  <div className={`text-xl font-mono font-black ${headwindComponent > 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
                    {headwindComponent > 0 ? `+${headwindComponent.toFixed(1)}` : headwindComponent.toFixed(1)} {windUnit}
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 uppercase block">
                    {headwindComponent > 0 ? "Opposing Drag Vector (+Drain)" : "Assisting Tailwind Vector (-Drain)"}
                  </span>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1 shadow-inner`}>
                  <div className="flex justify-between items-center text-[8px] font-mono text-zinc-400 uppercase">
                    <span>Crosswind Instability</span>
                    <Compass className="w-3.5 h-3.5 text-cyan-400" />
                  </div>
                  <div className="text-xl font-mono font-black text-cyan-400">
                    {Math.abs(crosswindComponent).toFixed(1)} {windUnit}
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 uppercase block">
                    Lateral shear force
                  </span>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1 shadow-inner`}>
                  <div className="flex justify-between items-center text-[8px] font-mono text-zinc-400 uppercase">
                    <span>Aero Wind Chill</span>
                    <Thermometer className="w-3.5 h-3.5 text-purple-400" />
                  </div>
                  <div className="text-xl font-mono font-black text-purple-400">
                    {apparentWindChill}°
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 uppercase block">
                    At current ground speed
                  </span>
                </div>
              </div>

              <div className="bg-black/40 p-3 rounded-xl border border-zinc-800 flex items-center justify-between">
                <div>
                  <span className="text-[9.5px] font-black uppercase text-white font-mono block">Dynamic Air Density (ρ)</span>
                  <span className="text-[8px] text-zinc-400 font-mono">Calculated aerodynamic resistance coefficient</span>
                </div>
                <span className="text-xs font-mono font-black text-cyan-400">{airDensity.toFixed(3)} kg/m³</span>
              </div>
            </motion.div>
          )}

          {/* --- TAB 4: TIRE GRIP & HYDROPLANE RISK --- */}
          {activeTab === "traction" && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1 shadow-inner`}>
                  <Disc className="w-4 h-4 text-cyan-400 mx-auto mb-0.5" />
                  <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>Surface Grip Index</span>
                  <div className={`text-2xl font-mono font-black ${surfaceTraction.gripScore < 50 ? 'text-rose-400' : 'text-emerald-400'}`}>
                    {surfaceTraction.gripScore}%
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 block">Tire-to-tarmac friction</span>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1 shadow-inner`}>
                  <ShieldAlert className="w-4 h-4 text-amber-400 mx-auto mb-0.5" />
                  <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>Traction Status</span>
                  <div className={`text-xs font-mono font-black pt-1 ${surfaceTraction.statusColor}`}>
                    {surfaceTraction.surfaceStatus}
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 block">Precipitation & dew analysis</span>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1 shadow-inner`}>
                  <Gauge className="w-4 h-4 text-blue-400 mx-auto mb-0.5" />
                  <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>Tire PSI Variance</span>
                  <div className={`text-xs font-mono font-black pt-1 ${psiCompensation.color}`}>
                    {Number(psiCompensation.variance) > 0 ? `+${psiCompensation.variance}` : psiCompensation.variance} PSI
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 block">{psiCompensation.status}</span>
                </div>
              </div>
            </motion.div>
          )}

          {/* --- TAB 5: BATTERY & THERMAL RANGE IMPACT --- */}
          {activeTab === "thermal" && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1 shadow-inner text-center`}>
                  <Battery className="w-4 h-4 text-emerald-400 mx-auto mb-0.5" />
                  <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>Thermal Range Penalty</span>
                  <div className={`text-xl font-mono font-black ${batteryCapacityDerate > 20 ? 'text-rose-400' : batteryCapacityDerate > 5 ? 'text-amber-400' : 'text-emerald-400'}`}>
                    -{batteryCapacityDerate.toFixed(1)}%
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 block">Ambient chemical loss</span>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1 shadow-inner text-center`}>
                  <Flame className="w-4 h-4 text-amber-400 mx-auto mb-0.5" />
                  <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>Asphalt Heat Index</span>
                  <div className="text-xl font-mono font-black text-amber-400">
                    {Math.round(safeNum(current.soil_temperature_0cm, currentTemp))}°
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 block">Road surface thermal transfer</span>
                </div>

                <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-xl border ${ui?.brd || 'border-zinc-800'} space-y-1 shadow-inner text-center`}>
                  <Sun className="w-4 h-4 text-yellow-400 mx-auto mb-0.5" />
                  <span className={`text-[7.5px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block`}>Solar Heat Influx</span>
                  <div className="text-xl font-mono font-black text-yellow-400">
                    {Math.round(directRadiation)} W/m²
                  </div>
                  <span className="text-[7.5px] font-mono text-zinc-500 block">Photovoltaic absorption</span>
                </div>
              </div>
            </motion.div>
          )}

          {/* --- TAB 6: 10-DAY SYNOPTIC FORECAST --- */}
          {activeTab === "forecast" && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
              <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-2">
                {Array.isArray(daily?.time) && daily.time.map((day: string, idx: number) => (
                  <div key={day} className={`${ui?.bgList || 'bg-[#121318]'} border ${ui?.brd || 'border-zinc-800'} p-2.5 rounded-xl text-center flex flex-col items-center justify-between h-28 min-w-[76px] shrink-0 shadow-inner`}>
                    <span className={`text-[8.5px] font-mono font-black ${ui?.txtMuted || 'text-zinc-400'} uppercase`}>
                      {new Date(day).toLocaleDateString(localeCode, { weekday: 'short' })}
                    </span>
                    <div className="my-0.5 scale-95">
                      {safeGetWeatherIcon(daily?.weather_code?.[idx])}
                    </div>
                    <div className="space-y-0.5 font-mono">
                      <span className={`text-xs font-black ${ui?.txtMain || 'text-white'}`}>
                        {Math.round(safeNum(daily?.temperature_2m_max?.[idx], 0))}°
                      </span>
                      <span className={`text-[8.5px] font-bold ${ui?.txtMuted || 'text-zinc-400'} ml-1`}>
                        {Math.round(safeNum(daily?.temperature_2m_min?.[idx], 0))}°
                      </span>
                    </div>
                    <span className="text-[7.5px] font-mono text-cyan-400 font-bold">
                      {Math.round(safeNum(daily?.wind_speed_10m_max?.[idx], 0))} {windUnit}
                    </span>
                  </div>
                ))}
              </div>
            </motion.div>
          )}

        </div>
      ) : (
        <div className={`flex-1 flex flex-col items-center justify-center py-12 ${ui?.txtMuted || 'text-zinc-400'} font-mono text-xs uppercase font-bold tracking-widest gap-2`}>
          <AlertTriangle className="w-5 h-5 text-amber-500 animate-bounce" />
          <span>Atmospheric Base Station Offline</span>
          <button
            onClick={handleLocationSearch}
            className="mt-1 px-3 py-1.5 rounded-xl bg-zinc-900 border border-zinc-700 text-cyan-400 text-[9.5px] uppercase font-black cursor-pointer active:scale-95"
          >
            Retry Connection
          </button>
        </div>
      )}
    </div>
  );
}