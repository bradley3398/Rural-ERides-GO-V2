"use client";

import React, { useState, useMemo } from "react";
import { 
  Globe, Sparkles, Search, Sliders, Layers, FileText, Loader2, 
  ChevronUp, ChevronDown, ExternalLink, Cpu, AlertTriangle, 
  ChevronLeft, ChevronRight, Maximize2, Zap, ShieldCheck, 
  Scale, Gauge, BatteryCharging, Play, Check, Wrench, Disc,
  Wind, Video, Image, Film, Eye, ShieldAlert, Compass, Box, X, Calculator, Activity
} from "lucide-react";

interface YouTubeVideo {
  title: string;
  url: string;
  type?: "review" | "speed_test" | "teardown" | "range_test";
}

interface PevSpecRecord {
  name: string;
  brand: string;
  category: string;
  price: string;
  topSpeed: string;
  range: string;
  motorPower: string;
  battery: string;
  nominalVoltage?: number;
  weight: string;
  chargingTime: string;
  efficiency: string;
  siteUrl: string;
  brakingSystem: string;
  suspensionType: string;
  tireProfile: string;
  ipRating: string;
  maxLoad: string;
  videoReviewUrl?: string;
  youtubeVideos?: YouTubeVideo[];
  imageUrls: string[];
  dimensionsFolded?: string;
  dimensionsUnfolded?: string;
  controllerAmps?: string;
  frameMaterial?: string;
  fastenerTorque?: string;
}

interface SpecEngineTabProps {
  dbSearch: string;
  setDbSearch: (val: string) => void;
  searchLiveSpecs: (customQuery?: string) => void;
  handleRandomDiscovery: () => void;
  specCategoryFilter: string;
  setSpecCategoryFilter: (cat: string) => void;
  specSortBy: string;
  setSpecSortBy: (sort: string) => void;
  exportToCSV: (type: string) => void;
  isSearchingDb: boolean;
  hasSearchedDb: boolean;
  filteredAndSortedSpecs: PevSpecRecord[];
  displayCountSpecs: number;
  setDisplayCountSpecs: React.Dispatch<React.SetStateAction<number>>;
  expandedPevIdx: number | null;
  setExpandedPevIdx: React.Dispatch<React.SetStateAction<number | null>>;
  activeImageIndices: { [key: number]: number };
  setActiveImageIndices: React.Dispatch<React.SetStateAction<{ [key: number]: number }>>;
  compareList: any[];
  toggleCompare: (pev: any) => void;
  setActiveVideoUrl?: (url: string) => void;
  setActiveVideoTitle?: (title: string) => void;
  setLightboxState: (state: { pevIdx: number; imgIdx: number } | null) => void;
  specMake?: string;
  setSpecMake?: (val: string) => void;
  specModel?: string;
  setSpecModel?: (val: string) => void;
  specVehicleType?: string;
  setSpecVehicleType?: (val: string) => void;
  themeColors: any;
}

export default function SpecEngineTab({
  dbSearch,
  setDbSearch,
  searchLiveSpecs,
  handleRandomDiscovery,
  specCategoryFilter,
  setSpecCategoryFilter,
  specSortBy,
  setSpecSortBy,
  exportToCSV,
  isSearchingDb,
  hasSearchedDb,
  filteredAndSortedSpecs,
  displayCountSpecs,
  setDisplayCountSpecs,
  expandedPevIdx,
  setExpandedPevIdx,
  activeImageIndices,
  setActiveImageIndices,
  compareList,
  toggleCompare,
  setActiveVideoUrl,
  setActiveVideoTitle,
  setLightboxState,
  specMake = "all",
  setSpecMake = () => {},
  specModel = "",
  setSpecModel = () => {},
  specVehicleType = "all",
  setSpecVehicleType = () => {},
  themeColors
}: SpecEngineTabProps) {
  const t = themeColors;

  const [showCompareMatrixModal, setShowCompareMatrixModal] = useState<boolean>(false);
  const [showRangeSimulatorModal, setShowRangeSimulatorModal] = useState<boolean>(false);
  const [simPayloadLbs, setSimPayloadLbs] = useState<number>(180);
  const [simCruisingMph, setSimCruisingMph] = useState<number>(25);

  const handleAdvancedSpecSearch = () => {
    let composedQuery = dbSearch;
    if (specMake !== "all" || specModel.trim() || specVehicleType !== "all") {
      const makeStr = specMake !== "all" ? specMake : "";
      const modelStr = specModel.trim();
      const typeStr = specVehicleType !== "all" ? specVehicleType : "";
      composedQuery = `${makeStr} ${modelStr} ${typeStr} official specifications PEV technical data`.trim();
      setDbSearch(composedQuery);
    }
    searchLiveSpecs(composedQuery);
  };

  const calculateStoppingDistanceFeet = (topSpeedStr: string) => {
    const mph = parseFloat(topSpeedStr) || 20;
    const feet = (mph * mph) / (29.9 * 0.7);
    return feet.toFixed(1);
  };

  const calculatePowerToWeight = (powerStr: string, weightStr: string) => {
    const watts = parseFloat(String(powerStr || "1000").replace(/[^0-9.]/g, "")) || 1000;
    const lbs = parseFloat(String(weightStr || "65").replace(/[^0-9.]/g, "")) || 65;
    return (watts / lbs).toFixed(1);
  };

  // Fleet Analytics Summary
  const fleetAnalytics = useMemo(() => {
    if (!filteredAndSortedSpecs || filteredAndSortedSpecs.length === 0) return { avgSpeed: 0, avgPower: 0, maxTopSpeed: "0 MPH" };
    let totalSpeed = 0;
    let totalPower = 0;
    let fastestPev = filteredAndSortedSpecs[0];

    filteredAndSortedSpecs.forEach(p => {
      const speedNum = parseFloat(p.topSpeed) || 25;
      const powerNum = parseFloat(String(p.motorPower || "1000").replace(/[^0-9.]/g, "")) || 1000;
      totalSpeed += speedNum;
      totalPower += powerNum;
      if (speedNum > (parseFloat(fastestPev.topSpeed) || 25)) fastestPev = p;
    });

    return {
      avgSpeed: Math.round(totalSpeed / filteredAndSortedSpecs.length),
      avgPower: Math.round(totalPower / filteredAndSortedSpecs.length),
      fastestModel: `${fastestPev.brand} ${fastestPev.name} (${fastestPev.topSpeed})`
    };
  }, [filteredAndSortedSpecs]);

  return (
    <div className="space-y-6 font-sans">
      
      {/* COMPARISON MATRIX MODAL */}
      {showCompareMatrixModal && (
        <div className="fixed inset-0 z-[250] bg-black/85 backdrop-blur-xl flex items-center justify-center p-3 animate-fadeIn">
          <div className="bg-[#0a0a0f] border border-white/20 p-5 rounded-3xl max-w-4xl w-full space-y-4 font-mono shadow-2xl max-h-[90vh] overflow-y-auto custom-scrollbar">
            <div className="flex justify-between items-center border-b border-white/10 pb-3">
              <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2`}>
                <Layers className="w-4 h-4" /> Side-by-Side PEV Comparison Matrix ({compareList.length} Models)
              </h4>
              <button type="button" onClick={() => setShowCompareMatrixModal(false)} className="text-zinc-400 hover:text-white min-h-[36px] min-w-[36px] flex items-center justify-center cursor-pointer"><X className="w-5 h-5" /></button>
            </div>

            {compareList.length === 0 ? (
              <div className="py-12 text-center text-zinc-500 text-xs uppercase">No vehicles selected for comparison. Click &quot;+ Compare&quot; on any specification card.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-[10px] text-left border-collapse">
                  <thead>
                    <tr className="border-b border-white/10 text-zinc-400 uppercase">
                      <th className="p-2">Metric</th>
                      {compareList.map((c, i) => (
                        <th key={i} className="p-2 text-white font-black">{c.brand} {c.name}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    <tr>
                      <td className="p-2 text-zinc-500 font-bold">MSRP Price</td>
                      {compareList.map((c, i) => <td key={i} className={`p-2 ${t.text} font-black`}>{c.price}</td>)}
                    </tr>
                    <tr>
                      <td className="p-2 text-zinc-500 font-bold">Top Speed</td>
                      {compareList.map((c, i) => <td key={i} className="p-2 text-white font-bold">{c.topSpeed}</td>)}
                    </tr>
                    <tr>
                      <td className="p-2 text-zinc-500 font-bold">Max Range</td>
                      {compareList.map((c, i) => <td key={i} className="p-2 text-cyan-400 font-bold">{c.range}</td>)}
                    </tr>
                    <tr>
                      <td className="p-2 text-zinc-500 font-bold">Motor Power</td>
                      {compareList.map((c, i) => <td key={i} className="p-2 text-white">{c.motorPower}</td>)}
                    </tr>
                    <tr>
                      <td className="p-2 text-zinc-500 font-bold">Battery Pack</td>
                      {compareList.map((c, i) => <td key={i} className="p-2 text-amber-400">{c.battery}</td>)}
                    </tr>
                    <tr>
                      <td className="p-2 text-zinc-500 font-bold">Curb Weight</td>
                      {compareList.map((c, i) => <td key={i} className="p-2 text-white">{c.weight}</td>)}
                    </tr>
                    <tr>
                      <td className="p-2 text-zinc-500 font-bold">Braking System</td>
                      {compareList.map((c, i) => <td key={i} className="p-2 text-zinc-300">{c.brakingSystem}</td>)}
                    </tr>
                  </tbody>
                </table>
              </div>
            )}

            <button type="button" onClick={() => setShowCompareMatrixModal(false)} className="w-full bg-white/10 text-white min-h-[40px] rounded-xl text-[9px] font-black uppercase cursor-pointer active:scale-95">Close Matrix</button>
          </div>
        </div>
      )}

      {/* RANGE & EFFICIENCY SIMULATOR MODAL */}
      {showRangeSimulatorModal && (
        <div className="fixed inset-0 z-[250] bg-black/85 backdrop-blur-xl flex items-center justify-center p-3 animate-fadeIn">
          <div className="bg-[#0a0a0f] border border-white/20 p-5 rounded-3xl max-w-md w-full space-y-4 font-mono shadow-2xl">
            <div className="flex justify-between items-center border-b border-white/10 pb-3">
              <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2`}>
                <Calculator className="w-4 h-4" /> Real-World Range Calculator
              </h4>
              <button type="button" onClick={() => setShowRangeSimulatorModal(false)} className="text-zinc-400 hover:text-white min-h-[36px] min-w-[36px] flex items-center justify-center cursor-pointer"><X className="w-5 h-5" /></button>
            </div>

            <div className="space-y-3 text-[10px]">
              <div className="space-y-1 bg-black/40 p-3 rounded-xl border border-white/5">
                <div className="flex justify-between"><label className="text-zinc-400">Rider Payload:</label><span className="text-white font-bold">{simPayloadLbs} lbs</span></div>
                <input type="range" min="100" max="320" step="10" value={simPayloadLbs} onChange={(e) => setSimPayloadLbs(Number(e.target.value))} className="w-full accent-cyan-400 cursor-pointer" />
              </div>

              <div className="space-y-1 bg-black/40 p-3 rounded-xl border border-white/5">
                <div className="flex justify-between"><label className="text-zinc-400">Cruising Speed:</label><span className="text-white font-bold">{simCruisingMph} MPH</span></div>
                <input type="range" min="10" max="60" step="5" value={simCruisingMph} onChange={(e) => setSimCruisingMph(Number(e.target.value))} className="w-full accent-amber-400 cursor-pointer" />
              </div>

              <div className="p-3 bg-black/80 rounded-xl border border-white/10 space-y-1.5 text-center">
                <span className="text-zinc-500 text-[8px] uppercase block">Estimated Real-World Range</span>
                <span className="text-emerald-400 font-black text-base">~{Math.max(10, Math.round(45 * (200 / simPayloadLbs) * (25 / simCruisingMph)))} Miles</span>
                <span className="text-zinc-400 text-[8px] block">Factor: Aero Drag &amp; Rolling Resistance</span>
              </div>
            </div>

            <button type="button" onClick={() => setShowRangeSimulatorModal(false)} className="w-full bg-white/10 text-white min-h-[40px] rounded-xl text-[9px] font-black uppercase cursor-pointer active:scale-95">Close Calculator</button>
          </div>
        </div>
      )}

      {/* SEARCH COMMAND BAR */}
      <div className="bg-black/50 border border-white/10 p-5 rounded-3xl space-y-4 backdrop-blur-xl shadow-2xl">
        <div className="space-y-2">
          <div className="flex justify-between items-center flex-wrap gap-2">
            <label className={`text-[10px] font-black uppercase tracking-widest ${t.text} font-mono flex items-center gap-1.5`}>
              <Globe className="w-3.5 h-3.5 animate-pulse" /> Verified Vehicle Engineering Datasheet Matrix
            </label>
            <button
              type="button"
              onClick={() => setShowRangeSimulatorModal(true)}
              className="bg-white/5 hover:bg-white/10 border border-white/10 text-cyan-300 px-3 min-h-[32px] rounded-lg text-[8px] font-mono font-bold uppercase flex items-center gap-1 cursor-pointer transition-all active:scale-95"
            >
              <Calculator className="w-3 h-3 text-cyan-400" /> Range Simulator
            </button>
          </div>

          <div className="flex items-center gap-2 bg-black/80 border border-white/10 rounded-2xl px-3 py-1.5 min-h-[50px] shadow-inner focus-within:border-white/30 transition-all">
            <Search className={`w-4 h-4 ${t.text} shrink-0`} />
            <input 
              type="text"
              value={dbSearch}
              onChange={(e) => setDbSearch(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleAdvancedSpecSearch()}
              placeholder="Query vehicle model (e.g. Dualtron Storm, Aostirmotor S07, Segway GT2, Sur-Ron Light Bee...)"
              className="bg-transparent border-none text-xs text-white outline-none w-full font-bold px-2 py-2 min-h-[40px] placeholder:text-zinc-600"
            />
            <button 
              type="button"
              onClick={handleAdvancedSpecSearch}
              disabled={isSearchingDb}
              className={`${t.bg} text-black px-6 min-h-[44px] rounded-xl font-black uppercase tracking-widest text-[10px] cursor-pointer shadow-md disabled:opacity-40 transition-transform active:scale-95 shrink-0 flex items-center gap-1.5`}
            >
              {isSearchingDb ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Wrench className="w-3.5 h-3.5" />}
              <span>Scan Specs</span>
            </button>
          </div>
        </div>

        {/* FLEET ANALYTICS BAR */}
        {filteredAndSortedSpecs.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 bg-black/70 p-3 rounded-2xl border border-white/5 font-mono text-[9px]">
            <div><span className="text-zinc-500 uppercase block">Fleet Avg Speed</span><span className="text-white font-black text-xs">{fleetAnalytics.avgSpeed} MPH</span></div>
            <div><span className="text-zinc-500 uppercase block">Fleet Avg Power</span><span className="text-cyan-400 font-black text-xs">{fleetAnalytics.avgPower} Watts</span></div>
            <div><span className="text-zinc-500 uppercase block">Fastest Node</span><span className="text-emerald-400 font-black text-xs truncate block">{fleetAnalytics.fastestModel}</span></div>
          </div>
        )}

        {/* 65+ GLOBAL PEV BRAND REGISTRY */}
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 pt-2 border-t border-white/10 font-mono">
          <div className="space-y-1">
            <label className="text-[8px] uppercase text-zinc-500 font-bold">Brand Make (65+ Registry)</label>
            <select 
              value={specMake} 
              onChange={(e) => setSpecMake(e.target.value)}
              className="w-full bg-black/60 border border-white/10 text-white text-[10px] font-bold rounded-xl px-2.5 py-2 outline-none cursor-pointer min-h-[44px]"
            >
              <option value="all">All Makes &amp; Brands (65+ Global)</option>
              <option value="dualtron">Dualtron / Minimotors</option>
              <option value="nami">Nami Electric</option>
              <option value="kaabo">Kaabo Wolf Warrior</option>
              <option value="segway">Segway Ninebot</option>
              <option value="vsett">Vsett</option>
              <option value="apollo">Apollo</option>
              <option value="inmotion">Inmotion</option>
              <option value="begode">Begode / GotWay</option>
              <option value="kingsong">KingSong</option>
              <option value="leaperkim">LeaperKim (Veteran)</option>
              <option value="surron">Sur-Ron</option>
              <option value="talaria">Talaria</option>
              <option value="eridepro">E-Ride Pro</option>
              <option value="rawrr">Rawrr Mantis</option>
              <option value="79bike">79Bike Falcon</option>
              <option value="aostirmotor">Aostirmotor</option>
              <option value="aventon">Aventon</option>
              <option value="radpower">Rad Power Bikes</option>
              <option value="lectric">Lectric eBikes</option>
              <option value="juiced">Juiced Bikes</option>
              <option value="super73">Super73</option>
              <option value="himiway">Himiway</option>
              <option value="heybike">Heybike</option>
              <option value="isinwheel">isinwheel</option>
              <option value="engwe">Engwe</option>
              <option value="zero">Zero Scooters</option>
              <option value="hiboy">Hiboy</option>
              <option value="niu">NIU</option>
              <option value="evolve">Evolve Skateboards</option>
              <option value="backfire">Backfire</option>
              <option value="meepo">Meepo</option>
              <option value="exway">Exway</option>
              <option value="propella">Propella</option>
              <option value="ride1up">Ride1Up</option>
              <option value="blix">Blix</option>
              <option value="specialized">Specialized Turbo</option>
              <option value="trek">Trek E-Bikes</option>
              <option value="gazelle">Gazelle</option>
              <option value="stromer">Stromer</option>
              <option value="haibike">Haibike</option>
              <option value="riese">Riese &amp; Müller</option>
              <option value="yuba">Yuba Cargo</option>
              <option value="tern">Tern Bicycles</option>
              <option value="nanrobot">Nanrobot</option>
              <option value="kukirin">KuKirin / Kugoo</option>
              <option value="yume">Yume Scooters</option>
              <option value="roadrunner">RoadRunner Scooters</option>
              <option value="emove">Emove (Voromotors)</option>
              <option value="weped">Weped</option>
              <option value="bronco">Bronco Motors</option>
              <option value="currus">Currus</option>
              <option value="fiido">Fiido</option>
              <option value="cyrusher">Cyrusher</option>
              <option value="quietkat">QuietKat</option>
              <option value="biktrix">Biktrix</option>
              <option value="arielrider">Ariel Rider</option>
              <option value="wallke">Wallke</option>
              <option value="eunorau">Eunorau</option>
              <option value="gotrax">GOTRAX</option>
              <option value="varla">Varla</option>
              <option value="teverun">Teverun</option>
              <option value="okai">Okai</option>
              <option value="bird">Bird</option>
              <option value="turboant">TurboAnt</option>
              <option value="unagi">Unagi</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[8px] uppercase text-zinc-500 font-bold">Model Designation</label>
            <input 
              type="text"
              value={specModel}
              onChange={(e) => setSpecModel(e.target.value)}
              placeholder="e.g. S07, GT2, Storm, Light Bee..."
              className="w-full bg-black/60 border border-white/10 text-white text-[10px] font-bold rounded-xl px-2.5 py-2 outline-none cursor-text min-h-[44px] placeholder:text-zinc-600"
            />
          </div>

          <div className="space-y-1">
            <label className="text-[8px] uppercase text-zinc-500 font-bold">Topology Category</label>
            <select 
              value={specVehicleType} 
              onChange={(e) => setSpecVehicleType(e.target.value)}
              className="w-full bg-black/60 border border-white/10 text-white text-[10px] font-bold rounded-xl px-2.5 py-2 outline-none cursor-pointer min-h-[44px]"
            >
              <option value="all">All Vehicle Types</option>
              <option value="e-scooter">High-Speed E-Scooter</option>
              <option value="e-bike">Fat-Tire E-Bike / Cargo</option>
              <option value="euc">Electric Unicycle (EUC)</option>
              <option value="dirt_bike">Electric Dirt Bike (Mid-Drive)</option>
              <option value="e-skateboard">Electric Skateboard</option>
              <option value="e-trike">Utility E-Trike</option>
            </select>
          </div>

          <div className="space-y-1 flex flex-col justify-end">
            <button 
              type="button" 
              onClick={handleRandomDiscovery}
              disabled={isSearchingDb}
              className={`w-full bg-white/5 hover:bg-white/10 border border-white/10 ${t.text} px-3 min-h-[44px] rounded-xl text-[9px] font-black uppercase tracking-widest flex items-center justify-center gap-1.5 cursor-pointer transition-all active:scale-95`}
            >
              <Sparkles className={`w-3 h-3 ${t.text}`} /> Random Discovery
            </button>
          </div>
        </div>
      </div>

      {/* FILTER & MATRIX CONTROLS */}
      {filteredAndSortedSpecs.length > 0 && (
        <div className="flex flex-col sm:flex-row justify-between items-center gap-3 bg-black/60 p-3.5 rounded-2xl border border-white/10 shadow-inner backdrop-blur-md font-mono">
          <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
            <div className="flex items-center gap-2 text-xs">
              <Sliders className={`w-3.5 h-3.5 ${t.text}`} />
              <select 
                value={specCategoryFilter} 
                onChange={(e) => setSpecCategoryFilter(e.target.value)} 
                className="bg-black border border-white/10 text-white text-[10px] font-bold rounded-xl px-3 py-2 outline-none cursor-pointer min-h-[40px]"
              >
                <option value="all">All Vehicle Topology</option>
                <option value="e-bike">E-Bike / Cargo</option>
                <option value="e-scooter">E-Scooter / Commuter</option>
                <option value="euc">EUC (Electric Unicycle)</option>
                <option value="hyperscooter">Hyperscooter (60V+)</option>
                <option value="dirt_bike">Electric Dirt Bike</option>
                <option value="e-trike">E-Trike / Utility</option>
              </select>
            </div>

            <div className="flex items-center gap-2 text-xs">
              <Layers className={`w-3.5 h-3.5 ${t.text}`} />
              <select 
                value={specSortBy} 
                onChange={(e) => setSpecSortBy(e.target.value)} 
                className="bg-black border border-white/10 text-white text-[10px] font-bold rounded-xl px-3 py-2 outline-none cursor-pointer min-h-[40px]"
              >
                <option value="default">Default Node Match</option>
                <option value="speed_desc">Top Speed (V-Max Desc)</option>
                <option value="range_desc">Max Range (High to Low)</option>
                <option value="price_asc">Price (Budget First)</option>
                <option value="power_desc">Motor Power (Watts Desc)</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            {compareList.length > 0 && (
              <button 
                type="button"
                onClick={() => setShowCompareMatrixModal(true)}
                className="text-[9px] font-black uppercase text-amber-400 bg-amber-500/20 hover:bg-amber-500/30 border border-amber-500/40 px-3 py-2 rounded-xl cursor-pointer transition-all animate-pulse"
              >
                Compare Matrix ({compareList.length})
              </button>
            )}
            <button 
              type="button" 
              onClick={() => exportToCSV("specs")} 
              className="px-4 min-h-[40px] rounded-xl text-[9px] font-black uppercase tracking-widest border bg-white/5 hover:bg-white/10 border-white/10 text-zinc-300 hover:text-white cursor-pointer shrink-0 transition-all flex items-center gap-1.5 active:scale-95"
            >
              <FileText className="w-3.5 h-3.5" /> CSV Export
            </button>
          </div>
        </div>
      )}

      {/* SEARCHING SPINNER */}
      {isSearchingDb && (
        <div className="text-center py-20 flex flex-col items-center justify-center space-y-4 bg-black/40 border border-white/5 rounded-3xl backdrop-blur-md">
          <Loader2 className={`w-10 h-10 ${t.text} animate-spin`} />
          <div className={`${t.text} font-mono text-xs uppercase tracking-widest animate-pulse`}>
            Querying technical specifications, teardowns, YouTube feeds &amp; 20 image angles...
          </div>
        </div>
      )}

      {/* RESULTS LISTING */}
      {!isSearchingDb && filteredAndSortedSpecs.length === 0 ? (
        <div className="text-center py-20 border border-dashed border-white/10 rounded-3xl text-zinc-500 font-mono text-[10px] uppercase tracking-widest flex flex-col items-center gap-3 bg-black/30 backdrop-blur-sm">
          <Globe className="w-8 h-8 opacity-20" />
          {hasSearchedDb ? (
            <span>0 Compatible Technical Datasheets Parsed. Try broadening search criteria.</span>
          ) : (
            <span>Search any model above to extract 100% verified technical specs, YouTube reviews &amp; 20 photo angles.</span>
          )}
        </div>
      ) : !isSearchingDb && (
        <div className="flex flex-col gap-5">
          {filteredAndSortedSpecs.slice(0, displayCountSpecs).map((pev: PevSpecRecord, idx: number) => {
            const isExpanded = expandedPevIdx === idx;
            const currentImgIdx = activeImageIndices[idx] || 0;
            const totalImages = pev.imageUrls?.length || 1;
            const isComparing = compareList.some((p: any) => p.name === pev.name && p.brand === pev.brand);

            const pwrToWeight = calculatePowerToWeight(pev.motorPower, pev.weight);
            const stoppingFeet = calculateStoppingDistanceFeet(pev.topSpeed);

            const youtubeList: YouTubeVideo[] = pev.youtubeVideos && pev.youtubeVideos.length > 0 
              ? pev.youtubeVideos 
              : pev.videoReviewUrl 
                ? [{ title: `${pev.brand} ${pev.name} Review & Speed Run`, url: pev.videoReviewUrl, type: "review" }]
                : [
                    { title: `${pev.brand} ${pev.name} Range & Speed Test`, url: `https://www.youtube.com/results?search_query=${encodeURIComponent(pev.brand + " " + pev.name + " top speed review")}`, type: "speed_test" },
                    { title: `${pev.brand} ${pev.name} Teardown & Maintenance Guide`, url: `https://www.youtube.com/results?search_query=${encodeURIComponent(pev.brand + " " + pev.name + " teardown repair")}`, type: "teardown" }
                  ];

            return (
              <div 
                key={idx} 
                className="bg-black/60 border border-white/10 rounded-3xl p-5 flex flex-col shadow-2xl backdrop-blur-xl hover:border-white/20 transition-all"
              >
                <div className="flex flex-col lg:flex-row gap-5">
                  
                  {/* IMAGE CAROUSEL */}
                  <div className="w-full lg:w-64 flex flex-col gap-2 shrink-0">
                    <div className="w-full h-56 rounded-2xl overflow-hidden bg-white border border-white/10 relative flex items-center justify-center shadow-inner group">
                      <img 
                        src={pev.imageUrls?.[currentImgIdx] || "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?auto=format&fit=crop&w=400&q=80"} 
                        alt="" 
                        className="w-full h-full object-contain p-2 mix-blend-multiply" 
                      />
                      
                      {totalImages > 1 && (
                        <>
                          <button 
                            type="button" 
                            onClick={(e) => { 
                              e.stopPropagation(); 
                              setActiveImageIndices((prev: any) => ({ ...prev, [idx]: currentImgIdx === 0 ? totalImages - 1 : currentImgIdx - 1 })); 
                            }} 
                            className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/80 hover:bg-black p-2 rounded-xl text-white cursor-pointer transition-transform active:scale-95 z-10"
                          >
                            <ChevronLeft className="w-4 h-4" />
                          </button>
                          <button 
                            type="button" 
                            onClick={(e) => { 
                              e.stopPropagation(); 
                              setActiveImageIndices((prev: any) => ({ ...prev, [idx]: currentImgIdx === totalImages - 1 ? 0 : currentImgIdx + 1 })); 
                            }} 
                            className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/80 hover:bg-black p-2 rounded-xl text-white cursor-pointer transition-transform active:scale-95 z-10"
                          >
                            <ChevronRight className="w-4 h-4" />
                          </button>
                        </>
                      )}

                      <span className="absolute top-2 left-2 bg-black/80 text-zinc-300 font-mono text-[9px] font-black px-2 py-0.5 rounded-lg border border-white/10 flex items-center gap-1 z-10">
                        <Image className="w-2.5 h-2.5 text-cyan-400" /> {currentImgIdx + 1} / {totalImages}
                      </span>

                      <button 
                        type="button" 
                        onClick={() => setLightboxState({ pevIdx: idx, imgIdx: currentImgIdx })} 
                        className="absolute bottom-2 right-2 bg-black/80 hover:bg-black p-2 rounded-xl text-white cursor-pointer transition-transform active:scale-95 z-10"
                        title="View Full High-Res Lightbox"
                      >
                        <Maximize2 className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    {totalImages > 1 && (
                      <div className="flex gap-1.5 overflow-x-auto pb-1 custom-scrollbar">
                        {pev.imageUrls.map((imgUrl, thumbIdx) => (
                          <button
                            key={thumbIdx}
                            type="button"
                            onClick={() => setActiveImageIndices((prev: any) => ({ ...prev, [idx]: thumbIdx }))}
                            className={`w-10 h-10 rounded-lg overflow-hidden bg-white border shrink-0 transition-all cursor-pointer ${
                              currentImgIdx === thumbIdx ? "border-amber-400 ring-1 ring-amber-400" : "border-white/10 opacity-60 hover:opacity-100"
                            }`}
                          >
                            <img src={imgUrl} alt="" className="w-full h-full object-cover mix-blend-multiply" />
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* SUMMARY METRICS */}
                  <div className="flex-1 flex flex-col justify-between space-y-3 font-mono">
                    <div>
                      <div className="flex justify-between items-start gap-3 flex-wrap">
                        <div>
                          <div className="text-[10px] text-zinc-400 font-bold uppercase tracking-widest flex items-center gap-1.5">
                            <span>{pev.brand || "PEV Topology"}</span>
                            <span className="text-[8px] bg-white/10 text-zinc-300 px-1.5 py-0.2 rounded font-black">
                              {parseFloat(pev.topSpeed) > 28 ? "Hyper Performance Class" : "Standard Commuter Class"}
                            </span>
                          </div>
                          <h4 className="text-white font-black text-lg uppercase leading-tight font-sans tracking-tight">{pev.name}</h4>
                        </div>
                        <div className="flex gap-2 flex-wrap">
                          <button 
                            type="button" 
                            onClick={() => toggleCompare(pev)} 
                            className={`text-[9px] font-black uppercase px-3.5 min-h-[38px] rounded-xl border cursor-pointer transition-all active:scale-95 ${
                              isComparing 
                                ? "bg-amber-500 text-black border-amber-400 font-black" 
                                : "bg-white/5 text-zinc-300 border-white/10 hover:bg-white/10 hover:text-white"
                            }`}
                          >
                            {isComparing ? "✓ In Matrix" : "+ Compare"}
                          </button>
                        </div>
                      </div>
                      
                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mt-3.5 text-[10px] bg-black/50 p-3 rounded-2xl border border-white/5">
                        <div className="flex flex-col"><span className="text-zinc-500 text-[8px] uppercase">MSRP:</span><span className={`${t.text} font-black text-xs`}>{pev.price || "$--"}</span></div>
                        <div className="flex flex-col"><span className="text-zinc-500 text-[8px] uppercase">TOP SPEED:</span><span className="text-white font-black text-xs">{pev.topSpeed || "N/A"}</span></div>
                        <div className="flex flex-col"><span className="text-zinc-500 text-[8px] uppercase">MAX RANGE:</span><span className="text-white font-black text-xs">{pev.range || "N/A"}</span></div>
                        <div className="flex flex-col"><span className="text-zinc-500 text-[8px] uppercase">PWR/WEIGHT:</span><span className="text-cyan-400 font-black text-xs">{pwrToWeight} W/lb</span></div>
                      </div>
                    </div>

                    {/* YOUTUBE CONTENT HUB */}
                    <div className="space-y-1.5 pt-1">
                      <span className="text-[8px] font-mono uppercase text-zinc-500 font-black flex items-center gap-1">
                        <Video className="w-3 h-3 text-red-500" /> YouTube Content &amp; Field Tests:
                      </span>
                      <div className="flex flex-wrap gap-2">
                        {youtubeList.map((yt, ytIdx) => (
                          <button
                            key={ytIdx}
                            type="button"
                            onClick={() => {
                              if (setActiveVideoUrl) setActiveVideoUrl(yt.url);
                              if (setActiveVideoTitle) setActiveVideoTitle(yt.title);
                            }}
                            className="bg-red-950/40 hover:bg-red-900/50 text-red-300 border border-red-500/30 text-[9px] font-black uppercase px-3 py-1.5 rounded-xl flex items-center gap-1.5 cursor-pointer transition-all active:scale-95"
                          >
                            <Play className="w-2.5 h-2.5 fill-red-400 text-red-400" />
                            <span className="line-clamp-1 max-w-[200px]">{yt.title}</span>
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="pt-2 flex gap-2">
                      <button 
                        type="button" 
                        onClick={() => setExpandedPevIdx(isExpanded ? null : idx)} 
                        className={`flex-1 bg-white/5 hover:bg-white/10 border border-white/10 ${t.text} font-black text-[10px] uppercase min-h-[44px] rounded-2xl cursor-pointer flex items-center justify-center gap-2 transition-all active:scale-[0.99]`}
                      >
                        {isExpanded ? (
                          <>Collapse Technical Sheet <ChevronUp className="w-4 h-4" /></>
                        ) : (
                          <>Expand 100% Verified Engineering Matrix <ChevronDown className="w-4 h-4" /></>
                        )}
                      </button>
                      <a 
                        href={pev.siteUrl || "#"} 
                        target="_blank" 
                        rel="noreferrer" 
                        className="bg-white/5 hover:bg-white/10 text-zinc-300 px-4 min-h-[44px] rounded-2xl text-[10px] font-black uppercase flex items-center gap-1.5 border border-white/10 cursor-pointer"
                      >
                        OEM Site <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                  </div>
                </div>

                {isExpanded && (
                  <div className="mt-4 pt-4 border-t border-white/10 bg-black/60 p-5 rounded-2xl space-y-4 font-mono">
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3.5 text-[11px]">
                      <div className="space-y-1">
                        <p className="text-zinc-500 text-[9px] uppercase font-bold flex items-center gap-1">
                          <Disc className="w-3 h-3 text-rose-400" /> Braking System &amp; Stop Dist:
                        </p>
                        <p className="text-zinc-200 font-bold bg-black/80 p-2.5 rounded-xl border border-white/5">
                          {pev.brakingSystem || "Dual Hydraulic Disc / EBS"} <span className="text-rose-400">({stoppingFeet} ft Stop)</span>
                        </p>
                      </div>

                      <div className="space-y-1">
                        <p className="text-zinc-500 text-[9px] uppercase font-bold flex items-center gap-1">
                          <BatteryCharging className="w-3 h-3 text-amber-400" /> Battery &amp; Pack Topology:
                        </p>
                        <p className="text-zinc-200 font-bold bg-black/80 p-2.5 rounded-xl border border-white/5">
                          {pev.battery || "52V 20Ah (1040 Wh)"} {pev.nominalVoltage ? `(${pev.nominalVoltage}V Nom)` : ""}
                        </p>
                      </div>

                      <div className="space-y-1">
                        <p className="text-zinc-500 text-[9px] uppercase font-bold flex items-center gap-1">
                          <Gauge className="w-3 h-3 text-cyan-400" /> Tire Profile &amp; Dimensions:
                        </p>
                        <p className="text-zinc-200 font-bold bg-black/80 p-2.5 rounded-xl border border-white/5">
                          {pev.tireProfile || "10x3.0 Pneumatic"} (Clearance: ~5.5")
                        </p>
                      </div>

                      <div className="space-y-1">
                        <p className="text-zinc-500 text-[9px] uppercase font-bold flex items-center gap-1">
                          <Zap className="w-3 h-3 text-amber-400" /> Motor &amp; Controller Amperage:
                        </p>
                        <p className="text-zinc-200 font-bold bg-black/80 p-2.5 rounded-xl border border-white/5">
                          {pev.motorPower || "1200W Nominal"} {pev.controllerAmps ? `| ESC: ${pev.controllerAmps}` : "| Dual Sine-Wave"}
                        </p>
                      </div>

                      <div className="space-y-1">
                        <p className="text-zinc-500 text-[9px] uppercase font-bold flex items-center gap-1">
                          <Scale className="w-3 h-3 text-emerald-400" /> Curb Weight &amp; Max Load:
                        </p>
                        <p className="text-zinc-200 font-bold bg-black/80 p-2.5 rounded-xl border border-white/5">
                          {pev.weight || "65 lbs"} (Max Load: {pev.maxLoad || "330 lbs"})
                        </p>
                      </div>

                      <div className="space-y-1">
                        <p className="text-zinc-500 text-[9px] uppercase font-bold flex items-center gap-1">
                          <Wind className="w-3 h-3 text-blue-400" /> Ingress &amp; Chassis Material:
                        </p>
                        <p className="text-zinc-200 font-bold bg-black/80 p-2.5 rounded-xl border border-white/5">
                          {pev.ipRating || "IPX5"} | {pev.frameMaterial || "Aviation 6061-T6 Aluminum"}
                        </p>
                      </div>

                      {pev.fastenerTorque && (
                        <div className="space-y-1 col-span-1 sm:grid-cols-2 md:col-span-3">
                          <p className="text-zinc-500 text-[9px] uppercase font-bold flex items-center gap-1">
                            <Wrench className="w-3 h-3 text-violet-400" /> Critical Fastener Torque Specs:
                          </p>
                          <p className="text-zinc-200 font-bold bg-black/80 p-2.5 rounded-xl border border-white/5">
                            {pev.fastenerTorque}
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })}

          {filteredAndSortedSpecs.length > displayCountSpecs && (
            <div className="text-center pt-2 font-mono">
              <button
                type="button"
                onClick={() => setDisplayCountSpecs((prev) => prev + 12)}
                className="bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white px-6 min-h-[44px] rounded-2xl text-[10px] font-black uppercase font-mono tracking-widest cursor-pointer transition-all active:scale-95"
              >
                Load Next Technical Datasheets (+12 Vehicles)
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}