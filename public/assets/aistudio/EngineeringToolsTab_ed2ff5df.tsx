"use client";

import React, { useState, useEffect, useCallback } from "react";
import { 
  ShieldAlert, Activity, LifeBuoy, Zap, Cpu, Flame, Wrench, 
  Navigation, Droplets, Wind, BatteryCharging, Gauge, ShoppingBag,
  TrendingUp, Leaf, CloudRain, ThermometerSun, AlertTriangle, Scale,
  Crosshair, Disc, GitMerge, Combine, Layers, Radio, Calculator,
  Search, Loader2, ExternalLink, CheckCircle2, RefreshCw, Sliders,
  Binary, Compass, ShieldCheck, Box, Anchor, Timer, Mountain, Shield,
  MapPin, Database, BookmarkCheck, FileText, CheckSquare, HeartPulse,
  SlidersHorizontal, ZapOff, Cable, Magnet, ZapIcon, Lightbulb
} from "lucide-react";
import { getGeminiApiKey, getTavilyApiKey } from "../../services/CoPilotService";

export default function EngineeringToolsTab({
  selectedErrorCode, setSelectedErrorCode, getErrorCodeDetails,
  factoryPeakVolts = 54.6, setFactoryPeakVolts, measuredLoadVolts = 48.0, setMeasuredLoadVolts,
  testLoadAmps = 20.0, setTestLoadAmps, calculateBatteryHealth, selectedSymptom,
  setSelectedSymptom, getSymptomTriageGuide, kwhRate = 0.14, setKwhRate,
  chargerAmps = 3, setChargerAmps, calcVoltage = 52, calcAh = 20, hillGradePercent = 15,
  setHillGradePercent, targetClimbSpeed = 15, setTargetClimbSpeed, riderWeight = 180,
  setRiderWeight, vehicleWeight = 65, setVehicleWeight, ambientTempF = 45, setAmbientTempF,
  brakeFluidType = "Mineral Oil", setBrakeFluidType, getBrakeFluidInfo, calcWhPerMile = 25,
  setCalcWhPerMile, oldTireSize = 10, setOldTireSize, newTireSize = 11, setNewTireSize,
  currentSpeed = 30, setCurrentSpeed, stockAmps = 25, setStockAmps, shuntSolderPercent = 20,
  setShuntSolderPercent, selectedBoltSize = "M6", setSelectedBoltSize, getDynamicTorqueSpec,
  customWireAmps = 30, setCustomWireAmps, getRecommendedAWG, budgetItems = [], setBudgetItems,
  newItemName = "", setNewItemName, newItemCost = "", setNewItemCost, themeColors
}: any) {
  const t = themeColors || {
    text: "text-[#39ff14]",
    bg: "bg-[#39ff14]",
    border: "border-[#39ff14]",
    dim: "bg-[#39ff14]/10 text-[#39ff14] border-[#39ff14]/50"
  };

  const [localMetric, setLocalMetric] = useState<boolean>(false);

  useEffect(() => {
    const handleSettingsSync = () => {
      const isMetric = localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km";
      setLocalMetric(isMetric);
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
  }, []);

  const distLabel = localMetric ? "KM" : "MI";
  const speedLabel = localMetric ? "KM/H" : "MPH";

  // --- LIVE WEB ERROR CODE DIAGNOSTIC STATES ---
  const [errorCodeInput, setErrorCodeInput] = useState<string>(selectedErrorCode || "E06");
  const [controllerDisplayBrand, setControllerDisplayBrand] = useState<string>("Universal QS-S4 / TF-100 / LT01");
  const [liveDiagnosticResult, setLiveDiagnosticResult] = useState<{
    fault: string;
    component: string;
    symptoms: string;
    voltageCheck: string;
    fix: string;
    sourceUrl?: string;
  } | null>(null);
  const [isSearchingError, setIsSearchingError] = useState<boolean>(false);
  const [errorSearchMsg, setErrorSearchMsg] = useState<string | null>(null);

  // --- ORIGINAL TELEMETRY & LAB STATES ---
  const [lifetimeMileage, setLifetimeMileage] = useState<number>(1250);
  const [ambientTemp, setAmbientTemp] = useState<number>(45);
  const [headwindMph, setHeadwindMph] = useState<number>(15);
  const [gasPrice, setGasPrice] = useState<number>(3.45);
  const [carMpg, setCarMpg] = useState<number>(22);
  const [regenForce, setRegenForce] = useState<number>(3);
  const [regenDescentGrade, setRegenDescentGrade] = useState<number>(10);
  const [cargoManifestWeightLbs, setCargoManifestWeightLbs] = useState<number>(15);

  const [cellSeriesCount, setCellSeriesCount] = useState<number>(20); 
  const [cellParallelCount, setCellParallelCount] = useState<number>(5); 
  const [cellCapacityMah, setCellCapacityMah] = useState<number>(5000); 

  const [chainringTeeth, setChainringTeeth] = useState<number>(48);
  const [rearCogTeeth, setRearCogTeeth] = useState<number>(16);
  const [wheelDiamInches, setWheelDiamInches] = useState<number>(20);
  const [selectedChargerCurrent, setSelectedChargerCurrent] = useState<number>(chargerAmps || 4);
  const [targetSocPercent, setTargetSocPercent] = useState<number>(80);
  const [throttleVcc, setThrottleVcc] = useState<number>(5.0);
  const [throttleMinV, setThrottleMinV] = useState<number>(0.85);
  const [throttleMaxV, setThrottleMaxV] = useState<number>(4.15);
  const [discRotorSizeMm, setDiscRotorSizeMm] = useState<number>(180);
  const [stopDecelMph, setStopDecelMph] = useState<number>(currentSpeed || 32);
  const [fieldWeakeningAmps, setFieldWeakeningAmps] = useState<number>(25);
  const [baseRpm, setBaseRpm] = useState<number>(650);
  const [riderPosture, setRiderPosture] = useState<"upright" | "tuck" | "aero_crouch">("upright");
  const [continuousDrawAmps, setContinuousDrawAmps] = useState<number>(60);
  const [fuseBlowMultiplier, setFuseBlowMultiplier] = useState<number>(1.5);
  const [tireWidthInches, setTireWidthInches] = useState<number>(3.0);
  const [ridingSurface, setRidingSurface] = useState<"pavement" | "gravel" | "loose_dirt">("pavement");
  const [motorRatedWatts, setMotorRatedWatts] = useState<number>(1500);
  const [burstWattsLoad, setBurstWattsLoad] = useState<number>(3500);
  const [spokeCrossPattern, setSpokeCrossPattern] = useState<number>(2);
  const [rimErdMm, setRimErdMm] = useState<number>(406);
  const [hubFlangeDiamMm, setHubFlangeDiamMm] = useState<number>(145);

  const [climbInclinePercent, setClimbInclinePercent] = useState<number>(15);
  const [climbDurationMins, setClimbDurationMins] = useState<number>(10);
  const [stockShuntMilliohms, setStockShuntMilliohms] = useState<number>(0.50);
  const [solderAddPercent, setSolderAddPercent] = useState<number>(25);

  // --- NEW ADVANCED ENGINEERING STATES ---
  const [activeEngineeringTab, setActiveEngineeringTab] = useState<"sizing" | "powertrain" | "battery" | "chassis" | "performance" | "elec_adv" | "mech_adv" | "pack_builder" | "vesc_foc">("sizing");
  const [calcBearingCode, setCalcBearingCode] = useState<string>("6000-2RS");
  const [calcBrakePadCompound, setCalcBrakePadCompound] = useState<string>("semi_metallic");
  const [calcWireAmps, setCalcWireAmps] = useState<number>(45);
  const [calcChainPitch, setCalcChainPitch] = useState<string>("428");
  const [calcFastChargePort, setCalcFastChargePort] = useState<string>("LP20-3");
  const [calcRotorWearMm, setCalcRotorWearMm] = useState<number>(1.8);
  const [calcHydraulicFluid, setCalcHydraulicFluid] = useState<string>("mineral_oil");
  const [calcSpokeGauge, setCalcSpokeGauge] = useState<string>("12G");
  const [calcControllerFETs, setCalcControllerFETs] = useState<string>("18-FET");
  const [calcCoilSpringLbs, setCalcCoilSpringLbs] = useState<number>(650);
  const [calcRiderWeightLbs, setCalcRiderWeightLbs] = useState<number>(180);
  const [calcTireCompound, setCalcTireCompound] = useState<string>("pneumatic_tubeless");
  const [calcHallSensorAngle, setCalcHallSensorAngle] = useState<string>("120");
  const [calcSystemVoltage, setCalcSystemVoltage] = useState<number>(72);
  const [calcMotorKv, setCalcMotorKv] = useState<number>(11);
  const [calcChargeAmps, setCalcChargeAmps] = useState<number>(5);
  const [calcControllerAmps, setCalcControllerAmps] = useState<number>(50);

  // ADVANCED ELEC & MECH STATES
  const [motorPolePairs, setMotorPolePairs] = useState<number>(15);
  const [packIRMilliohms, setPackIRMilliohms] = useState<number>(45);
  const [phaseWireLengthMeters, setPhaseWireLengthMeters] = useState<number>(1.5);
  const [capRippleCurrent, setCapRippleCurrent] = useState<number>(15);
  const [beltToothPitchMm, setBeltToothPitchMm] = useState<number>(5);
  const [beltPulleyTeeth, setBeltPulleyTeeth] = useState<number>(72);
  const [stemRakeAngle, setStemRakeAngle] = useState<number>(24);
  const [regenBrakeAmps, setRegenBrakeAmps] = useState<number>(25);
  const [simSystemVoltage, setSimSystemVoltage] = useState<number>(72);
  const [simMotorKv, setSimMotorKv] = useState<number>(11);

  const [wireStrands, setWireStrands] = useState<number>(5);
  const [wireGaugeAwg, setWireGaugeAwg] = useState<number>(20);
  const [turnsPerSlot, setTurnsPerSlot] = useState<number>(4);

  const [leverPistonMm, setLeverPistonMm] = useState<number>(10);
  const [caliperPistonMm, setCaliperPistonMm] = useState<number>(22);
  const [caliperPistonCount, setCaliperPistonCount] = useState<number>(2);

  const [ledLumens, setLedLumens] = useState<number>(3000);
  const [ledEfficacy, setLedEfficacy] = useState<number>(120);

  const [wheelTravelMm, setWheelTravelMm] = useState<number>(160);
  const [shockStrokeMm, setShockStrokeMm] = useState<number>(55);

  // PACK BUILDER & VESC STATES
  const [calcNickelThickness, setCalcNickelThickness] = useState<string>("0.15");
  const [calcNickelWidth, setCalcNickelWidth] = useState<number>(8);
  const [calcNickelLayers, setCalcNickelLayers] = useState<number>(1);
  const [calcSpotWeldMs, setCalcSpotWeldMs] = useState<number>(15);

  const [calcVescInductance, setCalcVescInductance] = useState<number>(15);
  const [calcVescResistance, setCalcVescResistance] = useState<number>(25);

  // --- SAFE REACTIVE CALCULATIONS ---
  const packTotalVoltage = (cellSeriesCount * 3.6).toFixed(1);
  const packPeakVoltage = (cellSeriesCount * 4.2).toFixed(1);
  const packTotalAh = ((cellParallelCount * cellCapacityMah) / 1000).toFixed(1);
  const packTotalWh = (parseFloat(packTotalVoltage) * parseFloat(packTotalAh)).toFixed(0);

  const gearRatio = (chainringTeeth / (rearCogTeeth || 1)).toFixed(2);
  const gearInches = ((chainringTeeth / (rearCogTeeth || 1)) * wheelDiamInches).toFixed(1);
  const metersOfDevelopment = ((parseFloat(gearInches) * 0.0254 * Math.PI)).toFixed(2);

  const currentPackAh = parseFloat(packTotalAh) || calcAh || 20;
  const chargeCRate = (selectedChargerCurrent / (currentPackAh || 1)).toFixed(2);
  const timeToTargetHours = (((parseFloat(packTotalAh) * (targetSocPercent / 100)) / (selectedChargerCurrent || 1))).toFixed(1);

  const totalMassKg = ((riderWeight + vehicleWeight) * 0.453592);
  const velocityMps = (stopDecelMph * 0.44704);
  const kineticJoules = Math.round(0.5 * totalMassKg * (velocityMps * velocityMps));
  const rotorTempRiseEstC = Math.round(kineticJoules / (discRotorSizeMm * 1.8));

  const cdaFactor = riderPosture === "upright" ? 0.42 : riderPosture === "tuck" ? 0.30 : 0.22;
  const aeroWattsRequired = Math.round(0.5 * 1.225 * cdaFactor * Math.pow(currentSpeed * 0.44704, 3));

  const totalWeightLbs = (riderWeight + vehicleWeight);
  const basePsi = Math.round((totalWeightLbs * 0.15) / (tireWidthInches * 0.8));
  const optimizedPsi = ridingSurface === "pavement" ? basePsi + 4 : ridingSurface === "gravel" ? basePsi - 3 : basePsi - 8;

  const estClimbPowerDraw = Math.round((totalWeightLbs * 0.453592 * 9.81 * (climbInclinePercent / 100) * (targetClimbSpeed * 0.44704)) + aeroWattsRequired + 300);
  const estWindingTempRiseC = Math.round((estClimbPowerDraw / (motorRatedWatts || 1)) * (climbDurationMins * 1.8));

  const newShuntMilliohms = (stockShuntMilliohms * (1 - (solderAddPercent / 100))).toFixed(3);
  const newCurrentLimitAmps = Math.round((stockAmps || 25) * (stockShuntMilliohms / parseFloat(newShuntMilliohms)));

  // --- NEW ADVANCED CALCULATIONS ---
  const erpmLimit = Math.round(simSystemVoltage * simMotorKv * motorPolePairs);
  const irHeatLossWatts = Math.round(Math.pow(continuousDrawAmps, 2) * (packIRMilliohms / 1000));
  const phaseVoltDrop = (continuousDrawAmps * phaseWireLengthMeters * 0.005).toFixed(2); 
  const beltTensionHz = Math.round((beltPulleyTeeth * beltToothPitchMm * baseRpm) / 6000);
  const regenStopWatts = Math.round(simSystemVoltage * regenBrakeAmps);
  const traceAmpacity = Math.round(continuousDrawAmps * 1.25); 

  const leverArea = Math.PI * Math.pow(leverPistonMm / 2, 2);
  const caliperArea = Math.PI * Math.pow(caliperPistonMm / 2, 2) * caliperPistonCount;
  const hydraulicRatio = (caliperArea / leverArea).toFixed(1);

  const getAwgDiameter = (awg: number) => 0.127 * Math.pow(92, (36 - awg) / 39);
  const wireArea = Math.PI * Math.pow(getAwgDiameter(wireGaugeAwg) / 2, 2);
  const slotAreaTotalMm = (wireArea * wireStrands * turnsPerSlot).toFixed(2);
  
  const leverageRatio = (wheelTravelMm / shockStrokeMm).toFixed(2);
  const ledPowerDrawWatts = Math.round(ledLumens / ledEfficacy);

  // VESC & Pack Builder Calculations
  const fluxLinkageMWb = (60000 / (Math.sqrt(3) * 2 * Math.PI * (simMotorKv || 11) * (motorPolePairs || 15))).toFixed(2);
  const observerGain = (1000 / (calcVescInductance || 15)).toFixed(2);
  const nickelArea = (parseFloat(calcNickelThickness) * calcNickelWidth).toFixed(2);
  const nickelAmpacity = Math.round(parseFloat(nickelArea) * 25 * calcNickelLayers); // Burst ampacity rule of thumb

  const queryLiveErrorCode = async (codeToQuery?: string) => {
    const activeCode = (codeToQuery || errorCodeInput).trim();
    if (!activeCode) return;
    setIsSearchingError(true); setErrorSearchMsg(null); setLiveDiagnosticResult(null);

    try {
      const tavilyKey = getTavilyApiKey();
      const geminiKey = getGeminiApiKey();

      const query = `${controllerDisplayBrand} error code ${activeCode} electric scooter ebike PEV troubleshooting fault code fix wiring diagram manual`;
      const tavilyResponse = await fetch("https://api.tavily.com/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ api_key: tavilyKey, query, search_depth: "advanced", include_raw_content: false, max_results: 15 })
      });

      const tavilyData = await tavilyResponse.json();
      if (!tavilyResponse.ok) throw new Error("Tavily diagnostic routing node dropped connection.");

      const webContext = JSON.stringify(tavilyData.results || []);

      const prompt = `You are a certified master PEV diagnostics engineer. Extract verified repair and diagnostic telemetry for Error Code "${activeCode}" on system "${controllerDisplayBrand}". Context: ${webContext}
      Return ONLY a clean JSON object:
      {
        "fault": "Exact verified technical fault title",
        "component": "Specific harness, sensor, MOSFET, or data wire",
        "symptoms": "Observable vehicle symptoms",
        "voltageCheck": "Multimeter rail test instructions",
        "fix": "Step-by-step mechanical and electrical repair instructions",
        "sourceUrl": "Real documentation URL from search results if available"
      }`;

      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${geminiKey}`;
      const res = await fetch(geminiUrl, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contents: [{ role: "user", parts: [{ text: prompt }] }], generationConfig: { responseMimeType: "application/json" } })
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.error?.message || "AI diagnostic engine timeout.");

      setLiveDiagnosticResult(JSON.parse(data.candidates[0].content.parts[0].text));
      if (setSelectedErrorCode) setSelectedErrorCode(activeCode);
    } catch (err: any) {
      setErrorSearchMsg(`Live Diagnostic Lookup Failed: ${err.message || String(err)}`);
    } finally {
      setIsSearchingError(false);
    }
  };

  return (
    <div className="space-y-4 sm:space-y-6 font-sans">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-5">
        
        {/* BAY 1: LIVE ERROR CODE DIAGNOSTICS & TELEMETRY */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl col-span-1 md:col-span-2 backdrop-blur-xl">
          <div className="flex justify-between items-center flex-wrap gap-2">
            <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
              <ShieldAlert className="w-4 h-4 text-rose-500" /> Live Web Error Code (E-Code) Diagnostic Vault
            </h4>
            <span className="text-[9px] font-mono font-bold bg-rose-950/60 border border-rose-500/30 text-rose-300 px-2 py-1 rounded-md hidden sm:inline-block">
              LIVE MULTI-SYSTEM WEB ENGINE
            </span>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 font-mono text-[9px] sm:text-[10px]">
            <div className="space-y-3">
              <div className="space-y-1">
                <label className="uppercase text-zinc-500 font-bold">Display / Controller Ecosystem</label>
                <select 
                  value={controllerDisplayBrand} 
                  onChange={(e) => setControllerDisplayBrand(e.target.value)}
                  className="w-full bg-black/80 border border-white/10 rounded-xl px-2.5 py-2 text-white font-bold outline-none cursor-pointer min-h-[36px]"
                >
                  <option value="Universal QS-S4 / TF-100 / LT01">Universal QS-S4 / TF-100</option>
                  <option value="Dualtron EY3 / EY4 Minimotors">Dualtron Minimotors EY3 / EY4</option>
                  <option value="Segway Ninebot Max / GT Series">Segway Ninebot Max / GT</option>
                  <option value="Bafang BBSHD / M600 / DPC18 / UART / CAN">Bafang Mid-Drive UART &amp; CAN</option>
                  <option value="Sur-Ron Light Bee / Talaria Sting OEM">Sur-Ron / Talaria OEM</option>
                  <option value="KT-LCD3 / Kunteng FOC Controller">Kunteng KT-LCD3 / KT-LED</option>
                  <option value="Shimano STEPS / Bosch Performance Line">Shimano STEPS / Bosch</option>
                  <option value="VESC / Spintend / Makerbase FOC">VESC Open Source FOC</option>
                  <option value="Nami Burn-E / Viper Display">Nami Burn-E / Kaabo TFT</option>
                  <option value="Aostirmotor / Aventon / Rad Power OEM">Aostirmotor / Rad Power</option>
                  <option value="Vsett Display / NFC Throttle">Vsett NFC Display</option>
                </select>
              </div>

              <div className="space-y-1">
                <label className="uppercase text-zinc-500 font-bold">Error Code Input (Any System Syntax)</label>
                <div className="flex gap-2">
                  <input 
                    type="text"
                    value={errorCodeInput}
                    onChange={(e) => setErrorCodeInput(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && queryLiveErrorCode()}
                    placeholder="e.g. E06, 30H"
                    className="flex-1 bg-black/80 border border-white/10 rounded-xl px-3 py-1 text-white font-bold outline-none min-h-[36px] uppercase"
                  />
                  <button 
                    type="button"
                    onClick={() => queryLiveErrorCode()}
                    disabled={isSearchingError || !errorCodeInput.trim()}
                    className={`${t.bg} text-black px-4 min-h-[36px] rounded-xl font-black uppercase cursor-pointer shadow-md disabled:opacity-40 active:scale-95 flex items-center gap-1`}
                  >
                    {isSearchingError ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Search className="w-3.5 h-3.5" />} <span>Query</span>
                  </button>
                </div>
              </div>

              <div className="space-y-1.5 pt-1">
                <span className="uppercase text-zinc-500 font-black block">Quick Multi-Brand Errors:</span>
                <div className="flex flex-wrap gap-1.5 max-h-[100px] overflow-y-auto custom-scrollbar pr-1 p-1 bg-black/40 rounded-xl border border-white/5">
                  {["E01", "E02", "E03", "E04", "E05", "E06", "E07", "E08", "E09", "E10", "Error 21", "Error 30H", "FAULT_DRV"].map(c => (
                    <button
                      key={c}
                      type="button"
                      onClick={() => {
                        setErrorCodeInput(c);
                        queryLiveErrorCode(c);
                      }}
                      className="bg-white/5 hover:bg-white/15 border border-white/10 text-zinc-300 px-2 py-1.5 rounded-lg font-bold cursor-pointer transition-all active:scale-95 min-h-[32px]"
                    >
                      {c}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="lg:col-span-2 bg-zinc-950 border border-white/10 p-4 rounded-2xl space-y-2.5 shadow-inner flex flex-col justify-between">
              {isSearchingError ? (
                <div className="py-12 text-center flex flex-col items-center justify-center space-y-2">
                  <Loader2 className={`w-8 h-8 ${t.text} animate-spin`} />
                  <span className="text-zinc-400 font-mono animate-pulse">Parsing live technical manuals &amp; UART datasheets...</span>
                </div>
              ) : liveDiagnosticResult ? (
                <div className="space-y-2.5">
                  <div className="flex justify-between border-b border-white/10 pb-2 flex-wrap gap-1">
                    <span className="text-zinc-500 font-bold uppercase">Verified Fault:</span>
                    <span className="text-rose-400 font-black">{liveDiagnosticResult.fault}</span>
                  </div>
                  
                  <div className="flex justify-between border-b border-white/10 pb-2 flex-wrap gap-1">
                    <span className="text-zinc-500 font-bold uppercase">Target Component / Wire:</span>
                    <span className="text-cyan-400 font-black">{liveDiagnosticResult.component}</span>
                  </div>

                  {liveDiagnosticResult.symptoms && (
                    <div className="bg-black/60 p-2.5 rounded-xl border border-white/5 space-y-0.5">
                      <span className="text-zinc-500 uppercase font-black block">Observable Symptoms:</span>
                      <p className="text-zinc-300 font-medium leading-relaxed">{liveDiagnosticResult.symptoms}</p>
                    </div>
                  )}

                  {liveDiagnosticResult.voltageCheck && (
                    <div className="bg-amber-950/30 p-2.5 rounded-xl border border-amber-500/20 space-y-0.5">
                      <span className="text-amber-400 uppercase font-black block">Multimeter Diagnostic Rail Test:</span>
                      <p className="text-amber-200 font-medium leading-relaxed">{liveDiagnosticResult.voltageCheck}</p>
                    </div>
                  )}

                  <div className="bg-black/80 p-3 rounded-xl border border-white/5 space-y-1">
                    <span className="text-emerald-400 uppercase font-black block">Step-By-Step Repair Protocol:</span>
                    <p className="text-zinc-200 font-bold leading-relaxed">{liveDiagnosticResult.fix}</p>
                  </div>

                  {liveDiagnosticResult.sourceUrl && (
                    <div className="pt-1 flex justify-end">
                      <a href={liveDiagnosticResult.sourceUrl} target="_blank" rel="noreferrer" className="text-cyan-400 hover:text-cyan-300 font-bold flex items-center gap-1 uppercase">
                        Official Source Documentation <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                  )}
                </div>
              ) : errorSearchMsg ? (
                <div className="py-8 text-center text-rose-400 space-y-2">
                  <AlertTriangle className="w-6 h-6 mx-auto text-rose-400" />
                  <p className="font-bold">{errorSearchMsg}</p>
                </div>
              ) : (
                <div className="py-12 text-center text-zinc-500 space-y-2">
                  <ShieldAlert className="w-8 h-8 mx-auto opacity-30 text-zinc-400" />
                  <p className="uppercase font-black tracking-wider">Select a quick error code preset or type any custom code to pull live factory diagnostic logs.</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* BAY 2: HILL CLIMB THERMAL OVERLOAD & GRADE POWER SIMULATOR */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl backdrop-blur-xl border-l-4 border-l-rose-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Mountain className="w-4 h-4 text-rose-500" /> Hill Climb Thermal Overload &amp; Grade Simulator
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Incline Grade (%):</label><span className="text-white font-bold">{climbInclinePercent}%</span></div>
              <input type="range" min="5" max="35" step="1" value={climbInclinePercent} onChange={(e) => setClimbInclinePercent(Number(e.target.value))} className="w-full accent-rose-500 cursor-pointer" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Climb Duration (Mins):</label><span className="text-white font-bold">{climbDurationMins} Mins</span></div>
              <input type="range" min="2" max="30" step="1" value={climbDurationMins} onChange={(e) => setClimbDurationMins(Number(e.target.value))} className="w-full accent-amber-400 cursor-pointer" />
            </div>
          </div>
          <div className="bg-black/80 border border-white/10 p-3 rounded-2xl grid grid-cols-2 gap-2 text-center font-mono">
            <div><span className="text-zinc-500 text-[8px] uppercase block">Sustained Power Draw</span><span className="text-rose-400 font-black text-sm">{estClimbPowerDraw.toLocaleString()} Watts</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Est. Winding Temp Spike</span><span className={`font-black text-sm ${estWindingTempRiseC > 75 ? 'text-rose-500' : 'text-emerald-400'}`}>+{estWindingTempRiseC}&deg;C</span></div>
          </div>
        </div>

        {/* BAY 3: CONTROLLER SHUNT RESISTOR SOLDER MOD CALCULATOR */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl backdrop-blur-xl border-l-4 border-l-emerald-400">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Zap className="w-4 h-4 text-emerald-400" /> Controller Shunt Resistor Solder Mod &amp; Amp Boost
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Stock Shunt (m&Omega;):</label><span className="text-white font-bold">{stockShuntMilliohms} m&Omega;</span></div>
              <input type="range" min="0.2" max="1.0" step="0.05" value={stockShuntMilliohms} onChange={(e) => setStockShuntMilliohms(Number(e.target.value))} className="w-full accent-emerald-400 cursor-pointer" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Solder Stacking Layer:</label><span className="text-white font-bold">{solderAddPercent}% Layer</span></div>
              <input type="range" min="5" max="50" step="5" value={solderAddPercent} onChange={(e) => setSolderAddPercent(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
            </div>
          </div>
          <div className="bg-black/80 border border-white/10 p-3 rounded-2xl grid grid-cols-2 gap-2 text-center font-mono">
            <div><span className="text-zinc-500 text-[8px] uppercase block">New Shunt Resistance</span><span className="text-cyan-400 font-black text-sm">{newShuntMilliohms} m&Omega;</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Boosted Peak Amperage</span><span className="text-emerald-400 font-black text-sm">{newCurrentLimitAmps} Amps</span></div>
          </div>
        </div>

        {/* BAY 4: CUSTOM SERIES/PARALLEL BATTERY PACK BUILDER */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl col-span-1 md:col-span-2 backdrop-blur-xl border-l-4 border-l-emerald-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Calculator className="w-4 h-4 text-emerald-400" /> Custom Series / Parallel Battery Architecture Builder
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-4 rounded-2xl border border-white/5">
              <label className="text-zinc-400 uppercase font-bold block">Series Group (S): {cellSeriesCount}S</label>
              <input type="range" min="10" max="32" step="1" value={cellSeriesCount} onChange={(e) => setCellSeriesCount(Number(e.target.value))} className="w-full accent-emerald-500 cursor-pointer" />
              <span className="text-[8px] text-zinc-500 block">Nominal V = S &times; 3.6V</span>
            </div>
            <div className="space-y-2 bg-black/40 p-4 rounded-2xl border border-white/5">
              <label className="text-zinc-400 uppercase font-bold block">Parallel Group (P): {cellParallelCount}P</label>
              <input type="range" min="2" max="16" step="1" value={cellParallelCount} onChange={(e) => setCellParallelCount(Number(e.target.value))} className="w-full accent-emerald-500 cursor-pointer" />
              <span className="text-[8px] text-zinc-500 block">Total capacity scale</span>
            </div>
            <div className="space-y-2 bg-black/40 p-4 rounded-2xl border border-white/5">
              <label className="text-zinc-400 uppercase font-bold block">Cell mAh: {cellCapacityMah} mAh</label>
              <input type="range" min="2500" max="6000" step="100" value={cellCapacityMah} onChange={(e) => setCellCapacityMah(Number(e.target.value))} className="w-full accent-cyan-500 cursor-pointer" />
              <span className="text-[8px] text-zinc-500 block">18650 / 21700 cell rating</span>
            </div>
            <div className="bg-emerald-950/40 border border-emerald-500/30 p-4 rounded-2xl flex flex-col justify-center text-center shadow-inner">
              <span className="text-zinc-400 text-[8px] uppercase">Pack Summary ({cellSeriesCount * cellParallelCount} Cells)</span>
              <span className="text-emerald-400 font-black text-base mt-1">{packTotalVoltage}V Nom ({packPeakVoltage}V Max)</span>
              <span className="text-cyan-400 font-bold text-xs">{packTotalAh} Ah | {packTotalWh} Wh</span>
            </div>
          </div>
        </div>

        {/* BAY 5: GEAR RATIO & FINAL DRIVE CADENCE LAB */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl backdrop-blur-xl border-l-4 border-l-cyan-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <GitMerge className="w-4 h-4 text-cyan-400" /> Gear Ratio &amp; Final Drive Cadence Lab
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 font-mono text-[9px]">
            <div className="space-y-1.5 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Chainring (T):</label>
              <input type="number" value={chainringTeeth} onChange={(e) => setChainringTeeth(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold" />
            </div>
            <div className="space-y-1.5 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Rear Cog (T):</label>
              <input type="number" value={rearCogTeeth} onChange={(e) => setRearCogTeeth(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold" />
            </div>
            <div className="space-y-1.5 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Wheel Diam ({localMetric ? 'cm' : 'in'}):</label>
              <input type="number" value={wheelDiamInches} onChange={(e) => setWheelDiamInches(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold" />
            </div>
          </div>
          <div className="bg-black/80 border border-white/10 p-3 rounded-2xl grid grid-cols-3 gap-2 text-center font-mono">
            <div><span className="text-zinc-500 text-[8px] uppercase block">Drive Ratio</span><span className="text-white font-black text-sm">{gearRatio}:1</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Gear Inches</span><span className="text-cyan-400 font-black text-sm">{gearInches}"</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Development</span><span className="text-emerald-400 font-black text-sm">{metersOfDevelopment} m</span></div>
          </div>
        </div>

        {/* BAY 6: CHARGING C-RATE & TIME-TO-80% PRESERVATION LAB */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl backdrop-blur-xl border-l-4 border-l-amber-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <BatteryCharging className="w-4 h-4 text-amber-400" /> Charge C-Rate &amp; 80% Longevity Lab
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Charger Output:</label><span className="text-white font-bold">{selectedChargerCurrent}A</span></div>
              <input type="range" min="1" max="25" step="0.5" value={selectedChargerCurrent} onChange={(e) => setSelectedChargerCurrent(Number(e.target.value))} className="w-full accent-amber-500 cursor-pointer" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Target SoC Buffer:</label><span className="text-white font-bold">{targetSocPercent}%</span></div>
              <input type="range" min="50" max="100" step="5" value={targetSocPercent} onChange={(e) => setTargetSocPercent(Number(e.target.value))} className="w-full accent-emerald-500 cursor-pointer" />
            </div>
          </div>
          <div className="bg-black/80 border border-white/10 p-3 rounded-2xl grid grid-cols-2 gap-2 text-center font-mono">
            <div><span className="text-zinc-500 text-[8px] uppercase block">Charging C-Rate</span><span className={`font-black text-sm ${parseFloat(chargeCRate) > 0.5 ? 'text-rose-400' : 'text-emerald-400'}`}>{chargeCRate}C</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Est. Time to {targetSocPercent}%</span><span className="text-amber-400 font-black text-sm">{timeToTargetHours} Hours</span></div>
          </div>
        </div>

        {/* BAY 7: KINETIC ENERGY & BRAKE ROTOR HEAT DISSIPATION LAB */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl backdrop-blur-xl border-l-4 border-l-rose-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Disc className="w-4 h-4 text-rose-500" /> Kinetic Energy &amp; Rotor Thermal Dissipation
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Braking Velocity:</label><span className="text-white font-bold">{stopDecelMph} {speedLabel}</span></div>
              <input type="range" min="10" max="75" step="1" value={stopDecelMph} onChange={(e) => setStopDecelMph(Number(e.target.value))} className="w-full accent-rose-500 cursor-pointer" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400 block">Rotor Diameter:</label>
              <select value={discRotorSizeMm} onChange={(e) => setDiscRotorSizeMm(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold cursor-pointer">
                <option value={140}>140 mm Rotor</option>
                <option value={160}>160 mm Rotor</option>
                <option value={180}>180 mm Rotor</option>
                <option value={203}>203 mm Heavy Downhill</option>
                <option value={220}>220 mm Moto Grade</option>
              </select>
            </div>
          </div>
          <div className="bg-black/80 border border-white/10 p-3 rounded-2xl grid grid-cols-2 gap-2 text-center font-mono">
            <div><span className="text-zinc-500 text-[8px] uppercase block">Kinetic Energy Dissipated</span><span className="text-rose-400 font-black text-sm">{kineticJoules.toLocaleString()} Joules</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Est. Thermal Spike</span><span className="text-amber-400 font-black text-sm">+{rotorTempRiseEstC}&deg;C per Stop</span></div>
          </div>
        </div>

        {/* BAY 8: FIELD WEAKENING / FLUX EXTENSION SPEED LAB */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl backdrop-blur-xl border-l-4 border-l-violet-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Zap className="w-4 h-4 text-violet-400" /> Field Weakening &amp; D-Axis Flux Extension
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">D-Axis FW Current:</label><span className="text-white font-bold">{fieldWeakeningAmps}A</span></div>
              <input type="range" min="0" max="80" step="5" value={fieldWeakeningAmps} onChange={(e) => setFieldWeakeningAmps(Number(e.target.value))} className="w-full accent-violet-500 cursor-pointer" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Base Top RPM:</label><span className="text-white font-bold">{baseRpm} RPM</span></div>
              <input type="range" min="300" max="1500" step="50" value={baseRpm} onChange={(e) => setBaseRpm(Number(e.target.value))} className="w-full accent-violet-500 cursor-pointer" />
            </div>
          </div>
          <div className="bg-black/80 border border-white/10 p-3 rounded-2xl grid grid-cols-2 gap-2 text-center font-mono">
            <div><span className="text-zinc-500 text-[8px] uppercase block">Theoretical Top RPM</span><span className="text-violet-400 font-black text-sm">{Math.round(baseRpm * (1 + (fieldWeakeningAmps * 0.008)))} RPM</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Top Speed Boost</span><span className="text-emerald-400 font-black text-sm">+{Math.round((fieldWeakeningAmps * 0.35))}% {speedLabel}</span></div>
          </div>
        </div>

        {/* BAY 9: TIRE CONTACT PATCH & SURFACE PSI OPTIMIZER */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl backdrop-blur-xl border-l-4 border-l-blue-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Gauge className="w-4 h-4 text-blue-400" /> Terrain Contact Patch &amp; PSI Optimizer
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Tire Width:</label><span className="text-white font-bold">{tireWidthInches}"</span></div>
              <input type="range" min="1.5" max="5.0" step="0.25" value={tireWidthInches} onChange={(e) => setTireWidthInches(Number(e.target.value))} className="w-full accent-blue-400 cursor-pointer" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400 block">Primary Terrain:</label>
              <select value={ridingSurface} onChange={(e) => setRidingSurface(e.target.value as any)} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold cursor-pointer">
                <option value="pavement">Smooth Asphalt / High Speed</option>
                <option value="gravel">Hardpacked Gravel &amp; Fire Roads</option>
                <option value="loose_dirt">Loose Sand / Snow / Rocky Trail</option>
              </select>
            </div>
          </div>
          <div className="bg-black/80 border border-white/10 p-3 rounded-2xl grid grid-cols-2 gap-2 text-center font-mono">
            <div><span className="text-zinc-500 text-[8px] uppercase block">Recommended PSI</span><span className="text-blue-400 font-black text-sm">{optimizedPsi} PSI</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Recommended BAR</span><span className="text-cyan-400 font-black text-sm">{(optimizedPsi * 0.0689476).toFixed(2)} BAR</span></div>
          </div>
        </div>

        {/* BAY 10: DC MAIN FUSE & CIRCUIT BREAKER SIZING */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl backdrop-blur-xl border-l-4 border-l-rose-400">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <ShieldCheck className="w-4 h-4 text-rose-400" /> DC Main Fuse &amp; Circuit Breaker Sizing
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Max Continuous Draw:</label><span className="text-white font-bold">{continuousDrawAmps}A</span></div>
              <input type="range" min="10" max="250" step="10" value={continuousDrawAmps} onChange={(e) => setContinuousDrawAmps(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Safety Headroom Multiplier:</label><span className="text-white font-bold">{fuseBlowMultiplier}x</span></div>
              <input type="range" min="1.25" max="2.0" step="0.05" value={fuseBlowMultiplier} onChange={(e) => setFuseBlowMultiplier(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
            </div>
          </div>
          <div className="bg-black/80 border border-white/10 p-3 rounded-2xl grid grid-cols-2 gap-2 text-center font-mono">
            <div><span className="text-zinc-500 text-[8px] uppercase block">Rec. ANL / MIDI Fuse</span><span className="text-rose-400 font-black text-sm">{Math.round(continuousDrawAmps * fuseBlowMultiplier)}A Fuse</span></div>
            <div><span className="text-zinc-500 text-[8px] uppercase block">Recommended Breaker</span><span className="text-white font-black text-sm">{Math.round(continuousDrawAmps * 1.25)}A DC Breaker</span></div>
          </div>
        </div>

        {/* BAY 11: AERODYNAMIC CdA & POWER CONSUMPTION LAB */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl col-span-1 md:col-span-2 backdrop-blur-xl border-l-4 border-l-cyan-400">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Wind className="w-4 h-4 text-cyan-400" /> Aerodynamic CdA &amp; True High-Speed Wattage Draw
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400 block">Rider Body Posture:</label>
              <select value={riderPosture} onChange={(e) => setRiderPosture(e.target.value as any)} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold cursor-pointer">
                <option value="upright">Upright Commuter (CdA ~0.42)</option>
                <option value="tuck">Semi-Tuck / Crouched (CdA ~0.30)</option>
                <option value="aero_crouch">Full Race Aero Tuck (CdA ~0.22)</option>
              </select>
            </div>
            <div className="bg-black/80 border border-white/10 p-3 rounded-2xl flex flex-col justify-center text-center">
              <span className="text-zinc-500 text-[8px] uppercase">Aero Power Loss (@ {currentSpeed} {speedLabel})</span>
              <span className="text-cyan-400 font-black text-base">{aeroWattsRequired} Watts</span>
            </div>
            <div className="bg-black/80 border border-white/10 p-3 rounded-2xl flex flex-col justify-center text-center">
              <span className="text-zinc-500 text-[8px] uppercase">% Total Pack Energy Lost to Air</span>
              <span className="text-rose-400 font-black text-base">{Math.min(85, Math.round((aeroWattsRequired / (stockAmps * (factoryPeakVolts * 0.85) || 1)) * 100))}%</span>
            </div>
          </div>
        </div>

        {/* BAY 12: MOTOR THERMAL DUTY CYCLE & HEAT DISSIPATION */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl col-span-1 md:col-span-2 backdrop-blur-xl border-l-4 border-l-rose-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Flame className="w-4 h-4 text-rose-500" /> Stator Thermal Dissipation &amp; Burst Duty Cycle
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Rated Continuous:</label><span className="text-white font-bold">{motorRatedWatts}W</span></div>
              <input type="range" min="250" max="8000" step="250" value={motorRatedWatts} onChange={(e) => setMotorRatedWatts(Number(e.target.value))} className="w-full accent-emerald-500 cursor-pointer" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between"><label className="text-zinc-400">Peak Burst Load:</label><span className="text-white font-bold">{burstWattsLoad}W</span></div>
              <input type="range" min="500" max="15000" step="500" value={burstWattsLoad} onChange={(e) => setBurstWattsLoad(Number(e.target.value))} className="w-full accent-rose-500 cursor-pointer" />
            </div>
            <div className="bg-black/80 border border-white/10 p-3 rounded-2xl flex flex-col justify-center text-center">
              <span className="text-zinc-500 text-[8px] uppercase">Safe Max Burst Time</span>
              <span className="text-amber-400 font-black text-base">{Math.max(5, Math.round(180 / (burstWattsLoad / (motorRatedWatts || 1))))} Seconds</span>
            </div>
            <div className="bg-black/80 border border-white/10 p-3 rounded-2xl flex flex-col justify-center text-center">
              <span className="text-zinc-500 text-[8px] uppercase">Cool Down Interval</span>
              <span className="text-cyan-400 font-black text-base">{Math.round((burstWattsLoad / (motorRatedWatts || 1)) * 45)} Seconds</span>
            </div>
          </div>
        </div>

        {/* BAY 13: SPOKE TENSION & HUB FLANGE GEOMETRY */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl col-span-1 md:col-span-2 backdrop-blur-xl border-l-4 border-l-emerald-400">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Compass className="w-4 h-4 text-emerald-400" /> Hub Motor Wheel Spoke Geometry &amp; Tension Lab
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 font-mono text-[9px]">
            <div className="space-y-1 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Rim ERD (mm):</label>
              <input type="number" value={rimErdMm} onChange={(e) => setRimErdMm(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold" />
            </div>
            <div className="space-y-1 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Motor Flange (mm):</label>
              <input type="number" value={hubFlangeDiamMm} onChange={(e) => setHubFlangeDiamMm(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold" />
            </div>
            <div className="space-y-1 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Cross Pattern:</label>
              <select value={spokeCrossPattern} onChange={(e) => setSpokeCrossPattern(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold cursor-pointer">
                <option value={1}>1-Cross (Small Wheels)</option>
                <option value={2}>2-Cross (Standard PEV)</option>
                <option value={3}>3-Cross (Heavy Cargo)</option>
              </select>
            </div>
            <div className="bg-black/80 border border-white/10 p-3 rounded-2xl flex flex-col justify-center text-center">
              <span className="text-zinc-500 text-[8px] uppercase">Rec. Drive-Side Tension</span>
              <span className="text-emerald-400 font-black text-sm">115 - 130 kgf (12G/13G)</span>
            </div>
          </div>
        </div>

        {/* BAY 14: THROTTLE VOLTAGE DIVIDER ADC CALIBRATOR */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl col-span-1 md:col-span-2 backdrop-blur-xl border-l-4 border-l-violet-400">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <Sliders className="w-4 h-4 text-violet-400" /> Throttle Hall Sensor Voltage Divider &amp; ADC Calibrator
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 font-mono text-[9px]">
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Supply Rail VCC (V):</label>
              <input type="number" step="0.1" value={throttleVcc} onChange={(e) => setThrottleVcc(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Deadband Start (V):</label>
              <input type="number" step="0.05" value={throttleMinV} onChange={(e) => setThrottleMinV(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold" />
            </div>
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <label className="text-zinc-400">Full Throttle WOT (V):</label>
              <input type="number" step="0.05" value={throttleMaxV} onChange={(e) => setThrottleMaxV(Number(e.target.value))} className="w-full bg-black border border-white/10 rounded p-2 text-white font-bold" />
            </div>
          </div>
        </div>

        {/* BAY 15: 50+ ADVANCED COMPONENT SIZING, HARDWARE CALIBRATION & PERFORMANCE MATRIX */}
        <div className="bg-black/60 border border-white/10 rounded-3xl p-4 sm:p-5 space-y-4 shadow-2xl col-span-1 md:col-span-2 backdrop-blur-xl border-l-4 border-l-cyan-500">
          <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2 font-mono`}>
            <SlidersHorizontal className="w-4 h-4" /> 50+ Component Sizing &amp; Hardware Tolerances Matrix
          </h4>
          
          <div className="bg-black/90 border border-white/10 rounded-2xl p-4 space-y-4 font-mono text-xs">
            <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-2 border-b border-white/10">
              {(["sizing", "powertrain", "battery", "chassis", "performance", "elec_adv", "mech_adv", "pack_builder", "vesc_foc"] as const).map(tabKey => (
                <button
                  key={tabKey}
                  type="button"
                  onClick={() => setActiveEngineeringTab(tabKey)}
                  className={`px-3 py-1.5 rounded-lg text-[9px] font-bold uppercase tracking-wider transition-all min-h-[36px] cursor-pointer whitespace-nowrap ${activeEngineeringTab === tabKey ? "bg-cyan-500 text-black font-black shadow-md" : "bg-white/5 text-zinc-400 hover:bg-white/10 hover:text-white"}`}
                >
                  {tabKey.replace("_", " ")}
                </button>
              ))}
            </div>

            {activeEngineeringTab === "sizing" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px]">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Axle Bearings</span>
                  <select value={calcBearingCode} onChange={(e) => setCalcBearingCode(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="6000-2RS">6000-2RS (10x26x8)</option>
                    <option value="6001-2RS">6001-2RS (12x28x8)</option>
                    <option value="6002-2RS">6002-2RS (15x32x9)</option>
                    <option value="6802-2RS">6802-2RS (15x24x5)</option>
                  </select>
                  <span className="text-[8px] text-emerald-400 block font-bold">~4.5 kN Load</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Brake Friction</span>
                  <select value={calcBrakePadCompound} onChange={(e) => setCalcBrakePadCompound(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="semi_metallic">Semi-Metallic</option>
                    <option value="sintered">Sintered (650°C Limit)</option>
                    <option value="ceramic">Ceramic (Low Dust)</option>
                    <option value="organic">Organic (Soft Bite)</option>
                  </select>
                  <span className="text-[8px] text-cyan-400 block font-bold">Max Fade: {calcBrakePadCompound === "sintered" ? "650°C" : "400°C"}</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Phase Wire AWG</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="10" max="150" step="5" value={calcWireAmps} onChange={(e) => setCalcWireAmps(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{calcWireAmps}A</span>
                  </div>
                  <span className="text-[8px] text-amber-400 block font-bold">Rec: {calcWireAmps > 80 ? "8 AWG (QS8)" : calcWireAmps > 40 ? "10 AWG (XT90)" : "12 AWG (XT60)"}</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Disc Rotor Gauge</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="1.2" max="2.3" step="0.1" value={calcRotorWearMm} onChange={(e) => setCalcRotorWearMm(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
                    <span className="text-white font-bold w-10 text-right">{calcRotorWearMm}mm</span>
                  </div>
                  <span className={`text-[8px] font-bold block ${calcRotorWearMm < 1.5 ? "text-rose-500" : "text-emerald-400"}`}>
                    {calcRotorWearMm < 1.5 ? "Replace (<1.5mm)" : "Serviceable (1.8mm)"}
                  </span>
                </div>
              </div>
            )}

            {activeEngineeringTab === "powertrain" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px]">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Chain Pitch</span>
                  <select value={calcChainPitch} onChange={(e) => setCalcChainPitch(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="219H">219H Kart (7.77mm)</option>
                    <option value="420">420 O-Ring (Sur-Ron)</option>
                    <option value="428">428 Heavy Moto</option>
                  </select>
                  <span className="text-[8px] text-cyan-400 block font-bold">Standard Moto Pitch</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Controller FETs</span>
                  <select value={calcControllerFETs} onChange={(e) => setCalcControllerFETs(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="6-FET">6-FET (Max 25A Peak)</option>
                    <option value="12-FET">12-FET (Max 50A Peak)</option>
                    <option value="18-FET">18-FET (Max 80A Peak)</option>
                    <option value="24-FET">24-FET (120A+ Extreme)</option>
                  </select>
                  <span className="text-[8px] text-emerald-400 block font-bold">MOSFET: IRFB4110 / TO-247</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Spoke Wire Gauge</span>
                  <select value={calcSpokeGauge} onChange={(e) => setCalcSpokeGauge(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="10G">10G Heavy Moto (3.2mm)</option>
                    <option value="12G">12G Heavy Hub (2.6mm)</option>
                    <option value="13G">13G Standard PEV (2.3mm)</option>
                  </select>
                  <span className="text-[8px] text-cyan-400 block font-bold">Tension: 110-130 kgf</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Hall Sensor Angle</span>
                  <select value={calcHallSensorAngle} onChange={(e) => setCalcHallSensorAngle(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="120">120° Standard Inversion</option>
                    <option value="60">60° Legacy Stator</option>
                  </select>
                  <span className="text-[8px] text-amber-400 block font-bold">Pull-Up Rail: 5V Regulated</span>
                </div>
              </div>
            )}

            {activeEngineeringTab === "battery" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px]">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Series Groups (S)</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="10" max="24" step="1" value={cellSeriesCount} onChange={(e) => setCellSeriesCount(Number(e.target.value))} className="w-full accent-emerald-400 cursor-pointer" />
                    <span className="text-white font-bold w-6 text-right">{cellSeriesCount}S</span>
                  </div>
                  <span className="text-[8px] text-emerald-400 block font-bold">Nom: {(cellSeriesCount * 3.6).toFixed(1)}V | Peak: {(cellSeriesCount * 4.2).toFixed(1)}V</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Parallel Count (P)</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="2" max="12" step="1" value={cellParallelCount} onChange={(e) => setCellParallelCount(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
                    <span className="text-white font-bold w-6 text-right">{cellParallelCount}P</span>
                  </div>
                  <span className="text-[8px] text-cyan-400 block font-bold">Total Cells: {cellSeriesCount * cellParallelCount} Units</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Max Pack Discharge</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="10" max="45" step="5" value={cellCapacityMah} onChange={(e) => setCellCapacityMah(Number(e.target.value))} className="w-full accent-amber-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{cellCapacityMah}</span>
                  </div>
                  <span className="text-[8px] text-amber-400 block font-bold">Continuous Draw: {cellParallelCount * 15} Amps</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Charge Port Sizing</span>
                  <select value={calcFastChargePort} onChange={(e) => setCalcFastChargePort(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="GX16-3">GX16-3 Pin (Max 5A)</option>
                    <option value="LP20-3">LP20-3 (Max 15A)</option>
                    <option value="XLR-3">Neutrik XLR (Max 10A)</option>
                    <option value="Anderson">Anderson SB50 (Max 35A)</option>
                  </select>
                  <span className="text-[8px] text-emerald-400 block font-bold">Max Power: ~{calcFastChargePort === "LP20-3" ? "1200W" : "400W"}</span>
                </div>
              </div>
            )}

            {activeEngineeringTab === "chassis" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px]">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Rider Weight</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="100" max="320" step="10" value={riderWeight} onChange={(e) => setRiderWeight(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{riderWeight}lbs</span>
                  </div>
                  <span className="text-[8px] text-cyan-400 block font-bold">Rec Coil: {Math.round(riderWeight * 3.5)} lbs/in</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Rear Coil Weight</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="300" max="1500" step="50" value={calcCoilSpringLbs} onChange={(e) => setCalcCoilSpringLbs(Number(e.target.value))} className="w-full accent-emerald-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{calcCoilSpringLbs}lbs</span>
                  </div>
                  <span className="text-[8px] text-emerald-400 block font-bold">Sag Ratio: ~25-30% Stroke</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Hydraulic Fluid Type</span>
                  <select value={brakeFluidType} onChange={(e) => setBrakeFluidType(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="Mineral Oil">Low-Vis Mineral Oil</option>
                    <option value="DOT 4">DOT 4 Glycol Fluid</option>
                    <option value="DOT 5.1">DOT 5.1 High Temp</option>
                  </select>
                  <span className="text-[8px] text-cyan-400 block font-bold">Seal-Safe Non-Corrosive Base</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block">Thermal Interface</span>
                  <select value={calcTireCompound} onChange={(e) => setCalcTireCompound(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="pneumatic_tubeless">Tubeless Pneumatic</option>
                    <option value="pneumatic_tube">Tube Type (Standard)</option>
                    <option value="solid">Solid / Honeycomb</option>
                  </select>
                  <span className="text-[8px] text-amber-400 block font-bold">Optimal Grip &amp; Comfort</span>
                </div>
              </div>
            )}

            {activeEngineeringTab === "performance" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px]">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><TrendingUp className="w-3 h-3" /> Max Range Estimator</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="15" max="60" step="1" value={calcWhPerMile} onChange={(e) => setCalcWhPerMile(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{calcWhPerMile} Wh/mi</span>
                  </div>
                  <span className="text-[8px] text-cyan-400 block font-bold">Est. Range: {Math.round((calcSystemVoltage * calcAh) / calcWhPerMile)} Miles</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Zap className="w-3 h-3" /> System Peak Wattage</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="20" max="150" step="5" value={calcControllerAmps} onChange={(e) => setCalcControllerAmps(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
                    <span className="text-white font-bold w-10 text-right">{calcControllerAmps}A</span>
                  </div>
                  <span className="text-[8px] text-rose-400 block font-bold">Peak Output: {calcSystemVoltage * calcControllerAmps}W</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Activity className="w-3 h-3" /> Top Speed Theoretical</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="6" max="25" step="1" value={calcMotorKv} onChange={(e) => setCalcMotorKv(Number(e.target.value))} className="w-full accent-emerald-400 cursor-pointer" />
                    <span className="text-white font-bold w-10 text-right">{calcMotorKv} kV</span>
                  </div>
                  <span className="text-[8px] text-emerald-400 block font-bold">Max Unloaded: {Math.round((calcSystemVoltage * calcMotorKv) * 0.06)} MPH</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Timer className="w-3 h-3" /> Pack Charge Time</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="2" max="20" step="1" value={calcChargeAmps} onChange={(e) => setCalcChargeAmps(Number(e.target.value))} className="w-full accent-amber-400 cursor-pointer" />
                    <span className="text-white font-bold w-10 text-right">{calcChargeAmps}A</span>
                  </div>
                  <span className="text-[8px] text-amber-400 block font-bold">0-100%: {(calcAh / calcChargeAmps).toFixed(1)} Hours Total</span>
                </div>
              </div>
            )}

            {activeEngineeringTab === "elec_adv" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px] animate-fadeIn">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Cpu className="w-3 h-3 text-purple-400"/> ERPM Pole Limit</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="5" max="30" step="1" value={motorPolePairs} onChange={(e) => setMotorPolePairs(Number(e.target.value))} className="w-full accent-purple-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{motorPolePairs}P</span>
                  </div>
                  <span className="text-[8px] text-purple-400 block font-bold">eRPM: {erpmLimit.toLocaleString()} (Max 100k)</span>
                </div>
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Flame className="w-3 h-3 text-rose-400"/> Pack IR Heat Loss</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="15" max="150" step="5" value={packIRMilliohms} onChange={(e) => setPackIRMilliohms(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{packIRMilliohms} mΩ</span>
                  </div>
                  <span className="text-[8px] text-rose-400 block font-bold">Thermal Loss: {irHeatLossWatts}W @ {continuousDrawAmps}A</span>
                </div>
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Cable className="w-3 h-3 text-blue-400"/> Phase Wire Drop</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="0.5" max="3.0" step="0.1" value={phaseWireLengthMeters} onChange={(e) => setPhaseWireLengthMeters(Number(e.target.value))} className="w-full accent-blue-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{phaseWireLengthMeters}m</span>
                  </div>
                  <span className="text-[8px] text-blue-400 block font-bold">Loss: {phaseVoltDrop}V @ {calcWireAmps}A</span>
                </div>
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Activity className="w-3 h-3 text-emerald-400"/> Cap Ripple (I_rms)</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="5" max="40" step="1" value={capRippleCurrent} onChange={(e) => setCapRippleCurrent(Number(e.target.value))} className="w-full accent-emerald-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{capRippleCurrent}A</span>
                  </div>
                  <span className="text-[8px] text-emerald-400 block font-bold">Min Caps: {Math.ceil(capRippleCurrent / 2.5)}x 1000uF</span>
                </div>
              </div>
            )}

            {activeEngineeringTab === "mech_adv" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px] animate-fadeIn">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Disc className="w-3 h-3 text-amber-400"/> Belt Tension Hz</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="3" max="8" step="1" value={beltToothPitchMm} onChange={(e) => setBeltToothPitchMm(Number(e.target.value))} className="w-full accent-amber-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{beltToothPitchMm}mm</span>
                  </div>
                  <span className="text-[8px] text-amber-400 block font-bold">Est Freq: {beltTensionHz} Hz @ {baseRpm}rpm</span>
                </div>
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Compass className="w-3 h-3 text-cyan-400"/> Steerer Rake Angle</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="15" max="35" step="1" value={stemRakeAngle} onChange={(e) => setStemRakeAngle(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{stemRakeAngle}°</span>
                  </div>
                  <span className="text-[8px] text-cyan-400 block font-bold">Trail: {stemRakeAngle < 22 ? 'Twitchy' : 'Stable'}</span>
                </div>
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><ZapOff className="w-3 h-3 text-rose-400"/> E-Brake Regen Max</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="5" max="50" step="5" value={regenBrakeAmps} onChange={(e) => setRegenBrakeAmps(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{regenBrakeAmps}A</span>
                  </div>
                  <span className="text-[8px] text-rose-400 block font-bold">Decel Power: {regenStopWatts}W Return</span>
                </div>
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Layers className="w-3 h-3 text-emerald-400"/> PCB Trace Ampacity</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="10" max="150" step="5" value={continuousDrawAmps} onChange={(e) => setContinuousDrawAmps(Number(e.target.value))} className="w-full accent-emerald-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{continuousDrawAmps}A</span>
                  </div>
                  <span className="text-[8px] text-emerald-400 block font-bold">Rec Width: {traceAmpacity}mm (2oz Cu)</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Disc className="w-3 h-3 text-rose-400"/> Hyd Leverage Ratio</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="15" max="25" step="1" value={caliperPistonMm} onChange={(e) => setCaliperPistonMm(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{caliperPistonMm}mm</span>
                  </div>
                  <span className="text-[8px] text-rose-400 block font-bold">Ratio: {hydraulicRatio}:1</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Magnet className="w-3 h-3 text-amber-500"/> Stator Copper Fill</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="2" max="10" step="1" value={turnsPerSlot} onChange={(e) => setTurnsPerSlot(Number(e.target.value))} className="w-full accent-amber-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{turnsPerSlot}T</span>
                  </div>
                  <span className="text-[8px] text-amber-500 block font-bold">Area: {slotAreaTotalMm} mm²</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Activity className="w-3 h-3 text-blue-400"/> Suspension Leverage</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="100" max="250" step="10" value={wheelTravelMm} onChange={(e) => setWheelTravelMm(Number(e.target.value))} className="w-full accent-blue-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{wheelTravelMm}mm</span>
                  </div>
                  <span className="text-[8px] text-blue-400 block font-bold">Ratio: {leverageRatio}:1</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Lightbulb className="w-3 h-3 text-yellow-400"/> LED Wattage Conv</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="1000" max="10000" step="500" value={ledLumens} onChange={(e) => setLedLumens(Number(e.target.value))} className="w-full accent-yellow-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{ledLumens} lm</span>
                  </div>
                  <span className="text-[8px] text-yellow-400 block font-bold">Est Draw: {ledPowerDrawWatts}W</span>
                </div>
              </div>
            )}

            {/* BATTERY PACK BUILDER & SPOT WELD AMPACITY LAB */}
            {activeEngineeringTab === "pack_builder" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px] animate-fadeIn">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Layers className="w-3 h-3 text-amber-500"/> Pure Nickel Thickness</span>
                  <select value={calcNickelThickness} onChange={(e) => setCalcNickelThickness(e.target.value)} className="w-full bg-zinc-900 border border-white/10 text-white rounded-lg p-2 font-bold min-h-[36px] cursor-pointer outline-none">
                    <option value="0.10">0.10mm (Light Duty)</option>
                    <option value="0.15">0.15mm (Standard PEV)</option>
                    <option value="0.20">0.20mm (Heavy Duty / Sur-Ron)</option>
                    <option value="0.30">0.30mm (Copper-Sandwich)</option>
                  </select>
                  <span className="text-[8px] text-amber-500 block font-bold">Cross-Section Base: {calcNickelThickness}mm</span>
                </div>
                
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Scale className="w-3 h-3 text-cyan-400"/> Nickel Strip Width</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="5" max="15" step="1" value={calcNickelWidth} onChange={(e) => setCalcNickelWidth(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{calcNickelWidth} mm</span>
                  </div>
                  <span className="text-[8px] text-cyan-400 block font-bold">Area: {nickelArea} mm² per layer</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Combine className="w-3 h-3 text-emerald-400"/> Stacked Layers</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="1" max="4" step="1" value={calcNickelLayers} onChange={(e) => setCalcNickelLayers(Number(e.target.value))} className="w-full accent-emerald-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{calcNickelLayers}x</span>
                  </div>
                  <span className="text-[8px] text-emerald-400 block font-bold">Max Safe Burst: {nickelAmpacity} Amps</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Zap className="w-3 h-3 text-rose-400"/> Spot Weld Pulse (kWeld)</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="5" max="50" step="5" value={calcSpotWeldMs} onChange={(e) => setCalcSpotWeldMs(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{calcSpotWeldMs} Joules</span>
                  </div>
                  <span className="text-[8px] text-rose-400 block font-bold">Tungsten Tip Temp: &gt;1000°C</span>
                </div>
              </div>
            )}

            {/* VESC FOC MOTOR TUNING LAB */}
            {activeEngineeringTab === "vesc_foc" && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-[9px] animate-fadeIn">
                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Cpu className="w-3 h-3 text-purple-400"/> Motor Pole Pairs</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="5" max="30" step="1" value={motorPolePairs} onChange={(e) => setMotorPolePairs(Number(e.target.value))} className="w-full accent-purple-400 cursor-pointer" />
                    <span className="text-white font-bold w-8 text-right">{motorPolePairs}P</span>
                  </div>
                  <span className="text-[8px] text-purple-400 block font-bold">Standard Hub: 15P | Mid: 4-5P</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Activity className="w-3 h-3 text-cyan-400"/> Motor Kv (RPM/V)</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="5" max="150" step="1" value={calcMotorKv} onChange={(e) => setCalcMotorKv(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{calcMotorKv} kV</span>
                  </div>
                  <span className="text-[8px] text-cyan-400 block font-bold">Flux Linkage: {fluxLinkageMWb} mWb</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Magnet className="w-3 h-3 text-amber-500"/> Motor Inductance (L)</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="5" max="100" step="1" value={calcVescInductance} onChange={(e) => setCalcVescInductance(Number(e.target.value))} className="w-full accent-amber-500 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{calcVescInductance} µH</span>
                  </div>
                  <span className="text-[8px] text-amber-500 block font-bold">Observer Gain: {observerGain} (Est.)</span>
                </div>

                <div className="bg-black/50 p-3 rounded-xl border border-white/5 space-y-2">
                  <span className="text-zinc-400 uppercase font-bold block flex items-center gap-1"><Flame className="w-3 h-3 text-rose-400"/> Motor Resistance (R)</span>
                  <div className="flex items-center gap-1.5 h-[36px]">
                    <input type="range" min="10" max="200" step="5" value={calcVescResistance} onChange={(e) => setCalcVescResistance(Number(e.target.value))} className="w-full accent-rose-400 cursor-pointer" />
                    <span className="text-white font-bold w-12 text-right">{calcVspecResistance || calcVescResistance} mΩ</span>
                  </div>
                  <span className="text-[8px] text-rose-400 block font-bold">Temp Compensation: 0.39%/°C</span>
                </div>
              </div>
            )}

          </div>
        </div>

      </div>
    </div>
  );
}