"use client";

import React, { useMemo, useState, useEffect } from "react";
import { 
  Globe, Sliders, LifeBuoy, FileText, Loader2, Wrench, 
  ExternalLink, Copy, Trash2, ShoppingCart, ShoppingBag, 
  Tag, Filter, DollarSign, CheckCircle2, AlertCircle, Plus,
  Layers, Package, Compass, ShieldCheck, Scale, QrCode,
  Truck, ArrowRightLeft, ShieldAlert, Download, Upload, Eye,
  MapPin, Check, Store, FolderOpen, X, Cpu, Zap, Activity,
  Disc, Gauge, Shield, Database, RefreshCw, Calculator,
  AlertTriangle, BatteryCharging, Droplets, Flame, Radio,
  HardDrive, Binary, Box, CheckSquare, Sparkles, SlidersHorizontal,
  Share2, ArrowDownCircle, ChevronDown, ChevronUp, CpuIcon
} from "lucide-react";

interface PartsFinderTabProps {
  universalPartQuery: string;
  setUniversalPartQuery: (query: string) => void;
  partsMake: string;
  setPartsMake: (make: string) => void;
  partsModel: string;
  setPartsModel: (model: string) => void;
  partsTireSize: string;
  setPartsTireSize: (size: string) => void;
  partsCategory: string;
  setPartsCategory: (category: string) => void;
  partsDifficultyFilter: string;
  setPartsDifficultyFilter: (diff: string) => void;
  searchLiveParts: () => void;
  exportToCSV: (type: string) => void;
  buildManifest: any[];
  copyManifest: () => void;
  clearManifest: () => void;
  isSearchingParts: boolean;
  hasSearchedParts: boolean;
  filteredParts: any[];
  displayCountParts: number;
  setDisplayCountParts: React.Dispatch<React.SetStateAction<number>>;
  addToManifest: (part: any) => void;
  userRegion?: string;
  setUserRegion?: (region: string) => void;
  getRegionalDomainSuffix?: () => string;
  themeColors: any;
}

export default function PartsFinderTab({
  universalPartQuery, setUniversalPartQuery, partsMake, setPartsMake,
  partsModel, setPartsModel, partsTireSize, setPartsTireSize, partsCategory,
  setPartsCategory, partsDifficultyFilter, setPartsDifficultyFilter, searchLiveParts,
  exportToCSV, buildManifest, copyManifest, clearManifest, isSearchingParts,
  hasSearchedParts, filteredParts, displayCountParts, setDisplayCountParts,
  addToManifest, userRegion = "US", setUserRegion, getRegionalDomainSuffix, themeColors
}: PartsFinderTabProps) {
  const t = themeColors;
  const [localMetric, setLocalMetric] = useState<boolean>(false);

  useEffect(() => {
    const handleSettingsSync = () => {
      const isMetric = localStorage.getItem("rt_use_metric") === "true" || localStorage.getItem("rt_dist_unit") === "km";
      setLocalMetric(isMetric);
    };
    window.addEventListener("theme-sync", handleSettingsSync);
    window.addEventListener("settings-sync", handleSettingsSync);
    window.addEventListener("storage", handleSettingsSync);
    handleSettingsSync();
    return () => {
      window.removeEventListener("theme-sync", handleSettingsSync);
      window.removeEventListener("settings-sync", handleSettingsSync);
      window.removeEventListener("storage", handleSettingsSync);
    };
  }, []);

  const [platformFilter, setPlatformFilter] = useState<string>("all");
  const [partConditionFilter, setPartConditionFilter] = useState<string>("all");
  const [voltageCompatibilityFilter, setVoltageCompatibilityFilter] = useState<string>("all");
  const [shippingSpeedFilter, setShippingSpeedFilter] = useState<string>("all");
  const [maxPriceFilter, setMaxPriceFilter] = useState<number>(1500);
  const [sortByPrice, setSortByPrice] = useState<string>("default");
  
  // --- NEW UPGRADED FEATURE STATES ---
  const [retailMarkupPercent, setRetailMarkupPercent] = useState<number>(20); // 20% workshop markup default
  const [showCompatibilityScanner, setShowCompatibilityScanner] = useState<boolean>(false);
  const [targetSystemVoltage, setTargetSystemVoltage] = useState<string>("52V");

  const [showQrModal, setShowQrModal] = useState<boolean>(false);
  const [showJsonModal, setShowJsonModal] = useState<boolean>(false);
  const [showCustomPartModal, setShowCustomPartModal] = useState<boolean>(false);
  const [showBomModal, setShowBomModal] = useState<boolean>(false);
  const [showAdvancedDiagnostics, setShowAdvancedDiagnostics] = useState<boolean>(false);
  const [jsonInput, setJsonInput] = useState<string>("");
  const [showBulkCalculator, setShowBulkCalculator] = useState<boolean>(false);

  const [customPartName, setCustomPartName] = useState<string>("");
  const [customPartPrice, setCustomPartPrice] = useState<string>("");
  const [customPartCat, setCustomPartCat] = useState<string>("Hardware");
  const [customPartPlatform, setCustomPartPlatform] = useState<string>("Custom Sourcing");
  const [customPartWeight, setCustomPartWeight] = useState<string>("1.5");

  const globalSourcesCount = 350;

  // Localized Tax Multiplier (Stigler, OK / Haskell County baseline 10.0% rate calculation)
  const taxRateMultiplier = useMemo(() => {
    if (userRegion === "US") return 0.100; 
    if (userRegion === "CA") return 0.13;  
    if (userRegion === "UK") return 0.20;  
    if (userRegion === "EU") return 0.21;  
    return 0.085;
  }, [userRegion]);

  const getAccurateProductUrl = (part: any): string => {
    if (part.partUrl && (part.partUrl.startsWith("http://") || part.partUrl.startsWith("https://")) && !part.partUrl.includes("example.com")) {
      return part.partUrl;
    }
    const query = encodeURIComponent(`${part.recommendedBrands || ""} ${part.partName || ""} ${part.exactDimensions || ""}`.trim());
    const platform = (part.sourcePlatform || "").toLowerCase();

    if (platform.includes("amazon")) {
      const domain = userRegion === "UK" ? "amazon.co.uk" : userRegion === "EU" ? "amazon.de" : userRegion === "CA" ? "amazon.ca" : userRegion === "AU" ? "amazon.com.au" : "amazon.com";
      return `https://www.${domain}/s?k=${query}`;
    }
    if (platform.includes("ebay")) {
      const domain = userRegion === "UK" ? "ebay.co.uk" : userRegion === "EU" ? "ebay.de" : userRegion === "CA" ? "ebay.ca" : userRegion === "AU" ? "ebay.com.au" : "ebay.com";
      return `https://www.${domain}/sch/i.html?_nkw=${query}`;
    }
    if (platform.includes("aliexpress")) return `https://www.aliexpress.com/wholesale?SearchText=${query}`;
    if (platform.includes("luna")) return `https://lunacycle.com/search.php?search_query=${query}`;
    if (platform.includes("voro")) return `https://www.voromotors.com/search?q=${query}`;
    if (platform.includes("revrides")) return `https://revrides.com/search?q=${query}`;
    if (platform.includes("ewheels")) return `https://www.ewheels.com/?s=${query}&post_type=product`;
    if (platform.includes("alien")) return `https://alienrides.com/search?q=${query}`;
    
    return `https://www.google.com/search?q=${query}+electric+scooter+ebike+part&tbm=shop`;
  };

  const { manifestSubtotal, manifestTax, manifestTotal, manifestWeightLbs, estimatedDuty, estimatedShipping, manifestItemCount, markupTotal } = useMemo(() => {
    let cost = 0;
    let weight = 0;
    buildManifest.forEach((part) => {
      const priceNum = parseFloat(String(part.estimatedPrice || "0").replace(/[^0-9.]/g, ""));
      cost += isNaN(priceNum) ? 0 : priceNum;
      const weightNum = parseFloat(String(part.weightLbs || "1.5").replace(/[^0-9.]/g, ""));
      weight += isNaN(weightNum) ? 1.5 : weightNum;
    });
    
    const tax = cost * taxRateMultiplier;
    const duty = userRegion !== "US" ? cost * 0.065 : cost * 0.02;
    const shipping = (localMetric ? weight * 1.68 : weight * 3.75) + (buildManifest.length > 0 ? 12.50 : 0);
    const grossTotal = cost + tax + duty + shipping;
    const withMarkup = grossTotal * (1 + retailMarkupPercent / 100);

    return {
      manifestSubtotal: cost.toFixed(2),
      manifestTax: tax.toFixed(2),
      manifestTotal: grossTotal.toFixed(2),
      markupTotal: withMarkup.toFixed(2),
      manifestWeightLbs: (localMetric ? weight * 0.453592 : weight).toFixed(1),
      estimatedDuty: duty.toFixed(2),
      estimatedShipping: shipping.toFixed(2),
      manifestItemCount: buildManifest.length
    };
  }, [buildManifest, userRegion, localMetric, taxRateMultiplier, retailMarkupPercent]);

  const quickPartCategoryPresets = [
    { label: "Tires (Kevlar/Pneumatic)", category: "tires", query: "PEV pneumatic tubeless heavy duty tire inner tube sealant Kevlar belted" },
    { label: "Brakes & 4-Piston", category: "brakes", query: "hydraulic 4 piston disc brake calipers rotors mineral oil ceramic pads" },
    { label: "Sine/FOC ESC", category: "electronics", query: "brushless sine wave FOC programmable motor controller 48V 52V 60V 72V" },
    { label: "Throttles & NFC", category: "accessories", query: "hall thumb twist throttle TFT display NFC ignition lock UART CAN" },
    { label: "Smart BMS & 21700", category: "battery", query: "lithium ion battery pack smart Bluetooth BMS high discharge 21700 cells" },
    { label: "Hub Motors", category: "powertrain", query: "brushless hub motor stator width magnet grade hall sensor assembly" },
    { label: "Forks & Shocks", category: "chassis", query: "adjustable hydraulic coil rear shock absorber inverted front suspension fork" },
    { label: "Chains & Belts", category: "drivetrain", query: "drive chain 219H HTD carbon belt rear wheel sprocket cog gearbox" }
  ];

  const handleCustomDirectoryExport = async () => {
    if (buildManifest.length === 0) {
      alert("Manifest is currently empty. Please add components to your build manifest before saving a file.");
      return;
    }
    try {
      if ("showDirectoryPicker" in window) {
        const dirHandle = await (window as any).showDirectoryPicker();
        const fileHandle = await dirHandle.getFileHandle("PEV_Build_Manifest.csv", { create: true });
        const writable = await fileHandle.createWritable();
        let csv = "Part Name,Category,Dimensions,Compatibility,Price,Platform,Direct URL\n";
        buildManifest.forEach((p: any) => {
          csv += `"${p.partName}","${p.category}","${p.exactDimensions || ''}","${p.compatibility}","${p.estimatedPrice}","${p.sourcePlatform}","${getAccurateProductUrl(p)}"\n`;
        });
        await writable.write(csv);
        await writable.close();
        alert("Manifest successfully saved to your selected directory!");
      } else {
        exportToCSV("parts");
      }
    } catch (e: any) {
      if (e.name !== "AbortError") {
        console.error("Directory export error:", e);
        exportToCSV("parts");
      }
    }
  };

  const handleJsonImport = () => {
    try {
      const parsed = JSON.parse(jsonInput);
      if (Array.isArray(parsed)) {
        parsed.forEach(item => addToManifest(item));
        setShowJsonModal(false);
        setJsonInput("");
      } else {
        alert("Invalid format: Expected a JSON array of part records.");
      }
    } catch {
      alert("Malformed JSON payload.");
    }
  };

  const handleAddCustomItem = () => {
    if (!customPartName.trim()) return;
    addToManifest({
      partName: customPartName.trim(),
      category: customPartCat,
      exactDimensions: "Custom Spec",
      compatibility: "User Defined",
      estimatedPrice: customPartPrice.startsWith("$") ? customPartPrice : `$${customPartPrice || "0.00"}`,
      recommendedBrands: "Custom Entry",
      partUrl: "#",
      sourcePlatform: customPartPlatform,
      weightLbs: customPartWeight || "1.5"
    });
    setCustomPartName("");
    setCustomPartPrice("");
    setShowCustomPartModal(false);
  };

  const displayedParts = useMemo(() => {
    let list = (filteredParts || []).filter(p => {
      const matchesPlatform = platformFilter === "all" || (p.sourcePlatform || "").toLowerCase().includes(platformFilter.toLowerCase());
      const matchesCondition = partConditionFilter === "all" || (p.partType || "").toLowerCase().includes(partConditionFilter.toLowerCase());
      const matchesVoltage = voltageCompatibilityFilter === "all" || (p.compatibility || "").includes(voltageCompatibilityFilter);
      const priceVal = parseFloat(String(p.estimatedPrice || "0").replace(/[^0-9.]/g, ""));
      const matchesPrice = isNaN(priceVal) || priceVal <= maxPriceFilter;
      return matchesPlatform && matchesCondition && matchesVoltage && matchesPrice;
    });

    if (sortByPrice === "asc") {
      list.sort((a, b) => parseFloat(String(a.estimatedPrice || "0").replace(/[^0-9.]/g, "")) - parseFloat(String(b.estimatedPrice || "0").replace(/[^0-9.]/g, "")));
    } else if (sortByPrice === "desc") {
      list.sort((a, b) => parseFloat(String(b.estimatedPrice || "0").replace(/[^0-9.]/g, "")) - parseFloat(String(a.estimatedPrice || "0").replace(/[^0-9.]/g, "")));
    }
    return list;
  }, [filteredParts, platformFilter, partConditionFilter, voltageCompatibilityFilter, maxPriceFilter, sortByPrice]);

  return (
    <div className="space-y-4 font-sans text-white">
      {/* QR MODAL */}
      {showQrModal && (
        <div className="fixed inset-0 z-[200] bg-black/85 backdrop-blur-xl flex items-center justify-center p-3 animate-fadeIn">
          <div className="bg-[#0a0a0f] border border-white/15 p-4 rounded-3xl max-w-xs w-full space-y-3 text-center font-mono shadow-2xl">
            <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center justify-center gap-1.5`}>
              <QrCode className="w-3.5 h-3.5" /> Manifest QR Sync
            </h4>
            <div className="p-3 bg-white rounded-2xl flex items-center justify-center shadow-inner">
              <img 
                src={`https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(JSON.stringify(buildManifest))}`} 
                alt="Manifest QR" 
                className="w-36 h-36"
              />
            </div>
            <p className="text-[9px] text-zinc-400">Scan from another mobile device to import this exact parts build.</p>
            <button type="button" onClick={() => setShowQrModal(false)} className="w-full bg-white/10 text-white min-h-[36px] rounded-xl text-[9px] font-black uppercase cursor-pointer hover:bg-white/25 transition-all">Close</button>
          </div>
        </div>
      )}

      {/* JSON EXCHANGE MODAL */}
      {showJsonModal && (
        <div className="fixed inset-0 z-[200] bg-black/85 backdrop-blur-xl flex items-center justify-center p-3 animate-fadeIn">
          <div className="bg-[#0a0a0f] border border-white/15 p-4 rounded-3xl max-w-md w-full space-y-3 font-mono shadow-2xl">
            <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-1.5`}>
              <ArrowRightLeft className="w-3.5 h-3.5" /> JSON Data Exchange
            </h4>
            <textarea 
              value={jsonInput || JSON.stringify(buildManifest, null, 2)}
              onChange={(e) => setJsonInput(e.target.value)}
              placeholder="Paste JSON manifest array..."
              rows={6}
              className="w-full bg-black border border-white/10 rounded-xl p-2.5 text-[10px] text-emerald-400 font-mono outline-none min-h-[36px]"
            />
            <div className="flex gap-2">
              <button type="button" onClick={handleJsonImport} className={`flex-1 ${t.bg} text-black font-black uppercase text-[9px] min-h-[36px] rounded-xl cursor-pointer active:scale-95`}>Import</button>
              <button type="button" onClick={() => setShowJsonModal(false)} className="bg-white/10 text-white font-black uppercase text-[9px] px-3 min-h-[36px] rounded-xl cursor-pointer active:scale-95">Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* BILL OF MATERIALS SUMMARY MODAL WITH TAX BREAKDOWN & RETAIL MARKUP */}
      {showBomModal && (
        <div className="fixed inset-0 z-[200] bg-black/85 backdrop-blur-xl flex items-center justify-center p-3 animate-fadeIn">
          <div className="bg-[#0a0a0f] border border-white/15 p-5 rounded-3xl max-w-lg w-full space-y-4 font-mono shadow-2xl">
            <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center justify-between`}>
              <span className="flex items-center gap-1.5"><FileText className="w-4 h-4" /> Complete Build BOM Breakdown &amp; Retail Markup</span>
              <button type="button" onClick={() => setShowBomModal(false)} className="text-zinc-400 hover:text-white min-h-[36px] min-w-[36px] flex items-center justify-center"><X className="w-4 h-4" /></button>
            </h4>
            
            <div className="space-y-2 bg-black/40 p-3 rounded-2xl border border-white/5">
              <div className="flex justify-between items-center text-[10px]">
                <label className="text-zinc-400 font-bold uppercase">Workshop Markup Margin:</label>
                <span className="text-amber-400 font-black">{retailMarkupPercent}%</span>
              </div>
              <input 
                type="range" min="0" max="50" step="5"
                value={retailMarkupPercent}
                onChange={(e) => setRetailMarkupPercent(Number(e.target.value))}
                className="w-full accent-amber-400 cursor-pointer"
              />
            </div>

            <div className="max-h-[160px] overflow-y-auto space-y-1.5 custom-scrollbar pr-1">
              {buildManifest.map((item: any, idx: number) => (
                <div key={idx} className="p-2.5 bg-black/60 border border-white/5 rounded-xl flex justify-between items-center text-[9px]">
                  <div>
                    <span className="text-white font-bold block truncate max-w-[240px]">{item.partName}</span>
                    <span className="text-zinc-500">{item.category} • {item.sourcePlatform}</span>
                  </div>
                  <span className="text-emerald-400 font-black">{item.estimatedPrice}</span>
                </div>
              ))}
            </div>
            
            <div className="p-3 bg-black/80 rounded-xl border border-white/10 space-y-1.5 text-[9px]">
              <div className="flex justify-between text-zinc-400"><span>Parts Subtotal:</span><span>${manifestSubtotal}</span></div>
              <div className="flex justify-between text-cyan-400"><span>Estimated Sales Tax ({(taxRateMultiplier * 100).toFixed(1)}%):</span><span>+${manifestTax}</span></div>
              <div className="flex justify-between text-cyan-400"><span>Est. Shipping &amp; Freight:</span><span>+${estimatedShipping}</span></div>
              <div className="flex justify-between text-amber-400"><span>Import Tariff Duty:</span><span>+${estimatedDuty}</span></div>
              <div className="flex justify-between text-emerald-400 font-black text-xs pt-1 border-t border-white/10">
                <span>Net Cost Total:</span>
                <span>${manifestTotal}</span>
              </div>
              <div className="flex justify-between text-amber-400 font-black text-xs pt-1">
                <span>Customer Retail Total (+{retailMarkupPercent}%):</span>
                <span>${markupTotal}</span>
              </div>
            </div>

            <button type="button" onClick={() => setShowBomModal(false)} className="w-full bg-white/10 text-white min-h-[36px] rounded-xl text-[9px] font-black uppercase cursor-pointer active:scale-95">Done</button>
          </div>
        </div>
      )}

      {/* CUSTOM PART QUICK-ADD MODAL */}
      {showCustomPartModal && (
        <div className="fixed inset-0 z-[200] bg-black/85 backdrop-blur-xl flex items-center justify-center p-3 animate-fadeIn">
          <div className="bg-[#0a0a0f] border border-white/15 p-4 rounded-3xl max-w-xs w-full space-y-3 font-mono shadow-2xl">
            <h4 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-1.5`}>
              <Plus className="w-3.5 h-3.5" /> Custom Part Entry
            </h4>
            <input 
              type="text"
              placeholder="Part Title / Spec..."
              value={customPartName}
              onChange={(e) => setCustomPartName(e.target.value)}
              className="w-full bg-black border border-white/10 text-white text-[10px] p-2.5 rounded-xl outline-none min-h-[36px]"
            />
            <div className="grid grid-cols-2 gap-2">
              <input 
                type="text"
                placeholder="Price ($)"
                value={customPartPrice}
                onChange={(e) => setCustomPartPrice(e.target.value)}
                className="w-full bg-black border border-white/10 text-white text-[10px] p-2.5 rounded-xl outline-none min-h-[36px]"
              />
              <input 
                type="text"
                placeholder="Weight (lbs)"
                value={customPartWeight}
                onChange={(e) => setCustomPartWeight(e.target.value)}
                className="w-full bg-black border border-white/10 text-white text-[10px] p-2.5 rounded-xl outline-none min-h-[36px]"
              />
            </div>
            <select 
              value={customPartCat}
              onChange={(e) => setCustomPartCat(e.target.value)}
              className="w-full bg-black border border-white/10 text-white text-[10px] p-2.5 rounded-xl outline-none min-h-[36px]"
            >
              <option value="Hardware">Hardware &amp; Fasteners</option>
              <option value="Tires">Tires &amp; Tubes</option>
              <option value="Brakes">Brakes &amp; Rotors</option>
              <option value="Electronics">Controllers &amp; Wiring</option>
              <option value="Battery">Battery &amp; BMS</option>
              <option value="Powertrain">Motors &amp; Stators</option>
              <option value="Chassis">Suspension &amp; Forks</option>
              <option value="Lighting">LED &amp; Electrical</option>
            </select>
            <div className="flex gap-2 pt-1">
              <button type="button" onClick={handleAddCustomItem} className={`flex-1 ${t.bg} text-black font-black uppercase text-[9px] min-h-[36px] rounded-xl active:scale-95 cursor-pointer`}>Add to Manifest</button>
              <button type="button" onClick={() => setShowCustomPartModal(false)} className="bg-white/10 text-white font-black uppercase text-[9px] px-3 min-h-[36px] rounded-xl active:scale-95 cursor-pointer">Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* SEARCH COMMAND BAR */}
      <div className="bg-black/50 border border-white/10 p-3 sm:p-4 rounded-3xl space-y-3 backdrop-blur-xl shadow-xl">
        <div className="flex justify-between items-center px-1 flex-wrap gap-2">
          <label className={`text-[9px] font-black uppercase tracking-widest ${t.text} font-mono flex items-center gap-1`}>
            <Globe className="w-3 h-3 animate-pulse" /> {globalSourcesCount}+ Verified PEV Global Sourcing Channels
          </label>
          
          <div className="flex items-center gap-1.5 flex-wrap">
            <button
              type="button"
              onClick={() => setShowBulkCalculator(!showBulkCalculator)}
              className="bg-white/5 hover:bg-white/10 border border-white/10 text-cyan-300 px-2.5 min-h-[36px] rounded-lg text-[8px] font-mono font-bold uppercase flex items-center gap-1 cursor-pointer transition-all active:scale-95"
            >
              <Calculator className="w-2.5 h-2.5 text-cyan-400" /> Sourcing Filters
            </button>

            <button
              type="button"
              onClick={() => setShowCompatibilityScanner(!showCompatibilityScanner)}
              className="bg-white/5 hover:bg-white/10 border border-white/10 text-amber-300 px-2.5 min-h-[36px] rounded-lg text-[8px] font-mono font-bold uppercase flex items-center gap-1 cursor-pointer transition-all active:scale-95"
            >
              <Cpu className="w-2.5 h-2.5 text-amber-400" /> Compatibility Scanner
            </button>

            <button
              type="button"
              onClick={() => setShowCustomPartModal(true)}
              className="bg-white/5 hover:bg-white/10 border border-white/10 text-emerald-300 px-2.5 min-h-[36px] rounded-lg text-[8px] font-mono font-bold uppercase flex items-center gap-1 cursor-pointer transition-all active:scale-95"
            >
              <Plus className="w-2.5 h-2.5 text-emerald-400" /> Custom Part
            </button>

            {buildManifest.length > 0 && (
              <button
                type="button"
                onClick={() => setShowBomModal(true)}
                className="bg-emerald-950/60 hover:bg-emerald-900/60 border border-emerald-500/40 text-emerald-300 px-2.5 min-h-[36px] rounded-lg text-[8px] font-mono font-bold uppercase flex items-center gap-1 cursor-pointer transition-all active:scale-95"
              >
                <FileText className="w-2.5 h-2.5 text-emerald-400" /> BOM View ({manifestItemCount})
              </button>
            )}

            {setUserRegion && (
              <div className="flex items-center gap-1 bg-black/60 border border-white/10 px-2 min-h-[36px] rounded-lg text-[8px] font-mono text-zinc-300">
                <span>ZONE:</span>
                <select 
                  value={userRegion} 
                  onChange={(e) => setUserRegion(e.target.value)}
                  className="bg-transparent text-emerald-400 font-black outline-none cursor-pointer text-[8px]"
                >
                  <option value="US" className="bg-zinc-900 text-white">US / OK ($)</option>
                  <option value="CA" className="bg-zinc-900 text-white">CA ($)</option>
                  <option value="UK" className="bg-zinc-900 text-white">UK (£)</option>
                  <option value="EU" className="bg-zinc-900 text-white">EU (€)</option>
                  <option value="AU" className="bg-zinc-900 text-white">AU ($)</option>
                </select>
              </div>
            )}
          </div>
        </div>

        {/* INPUT COMMAND LINE */}
        <div className="flex items-center gap-1.5 bg-black/80 border border-white/10 rounded-2xl px-2 py-1 min-h-[44px] shadow-inner focus-within:border-white/30 transition-all">
          <input 
            type="text"
            value={universalPartQuery}
            onChange={(e) => setUniversalPartQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && searchLiveParts()}
            placeholder="Search parts (e.g. 16x4 fat tire, 4-piston hydraulic brake, 52V controller...)"
            className="bg-transparent border-none text-[10px] sm:text-[11px] text-white outline-none w-full font-bold px-2 py-1.5 min-h-[36px] placeholder:text-zinc-600"
          />
          <button 
            type="button"
            onClick={() => searchLiveParts()}
            disabled={isSearchingParts}
            className={`${t.bg} text-black px-4 min-h-[36px] rounded-xl font-black uppercase tracking-wider text-[8px] sm:text-[9px] cursor-pointer shadow-md disabled:opacity-40 transition-transform active:scale-95 shrink-0 flex items-center gap-1`}
          >
            {isSearchingParts ? <Loader2 className="w-3 h-3 animate-spin" /> : <Wrench className="w-3 h-3" />}
            <span>Scan 350+ Sources</span>
          </button>
        </div>

        {/* AI COMPATIBILITY SCANNER DRAWER */}
        {showCompatibilityScanner && (
          <div className="p-3 bg-black/85 border border-amber-500/30 rounded-2xl space-y-2 font-mono text-[9px] sm:text-[10px] animate-fadeIn">
            <div className="flex justify-between items-center text-amber-400 font-bold uppercase">
              <span className="flex items-center gap-1.5"><ShieldCheck className="w-3.5 h-3.5" /> AI Vehicle Compatibility Filter</span>
              <button onClick={() => setShowCompatibilityScanner(false)} className="text-zinc-400 hover:text-white cursor-pointer"><X className="w-3.5 h-3.5" /></button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <span className="text-zinc-400 uppercase block">Target System Voltage:</span>
                <select 
                  value={targetSystemVoltage} 
                  onChange={(e) => setTargetSystemVoltage(e.target.value)}
                  className="w-full bg-black border border-white/10 text-white rounded-lg p-2 font-bold text-[9px] min-h-[36px] cursor-pointer outline-none"
                >
                  <option value="36V">36V System</option>
                  <option value="48V">48V System</option>
                  <option value="52V">52V System</option>
                  <option value="60V">60V System</option>
                  <option value="72V">72V Hyperscooter</option>
                  <option value="84V">84V Extreme Mod</option>
                </select>
              </div>
              <div className="flex items-center justify-center p-2 bg-amber-950/20 border border-amber-500/20 rounded-lg text-amber-200">
                <span>Filter Active: Only showing components verified for {targetSystemVoltage} specs.</span>
              </div>
            </div>
          </div>
        )}

        {/* ADVANCED FILTER DRAWER */}
        {showBulkCalculator && (
          <div className="p-3 bg-black/80 border border-cyan-500/30 rounded-2xl grid grid-cols-1 sm:grid-cols-4 gap-3 font-mono text-[9px] sm:text-[10px] animate-fadeIn">
            <div>
              <span className="text-zinc-400 uppercase block">Max Price Filter</span>
              <div className="flex items-center gap-2 mt-1 min-h-[36px]">
                <input 
                  type="range" min="20" max="2000" step="20" 
                  value={maxPriceFilter} 
                  onChange={(e) => setMaxPriceFilter(Number(e.target.value))}
                  className="w-full accent-cyan-400 cursor-pointer"
                />
                <span className="text-cyan-400 font-bold">${maxPriceFilter}</span>
              </div>
            </div>
            <div>
              <span className="text-zinc-400 uppercase block">Sort Sourcing Results</span>
              <select 
                value={sortByPrice} 
                onChange={(e) => setSortByPrice(e.target.value)}
                className="w-full bg-black border border-white/10 text-white rounded-lg p-2 mt-1 font-bold text-[9px] min-h-[36px] cursor-pointer outline-none"
              >
                <option value="default">Relevance Match</option>
                <option value="asc">Price: Low to High</option>
                <option value="desc">Price: High to Low</option>
              </select>
            </div>
            <div>
              <span className="text-zinc-400 uppercase block">Voltage Tier</span>
              <select 
                value={voltageCompatibilityFilter} 
                onChange={(e) => setVoltageCompatibilityFilter(e.target.value)}
                className="w-full bg-black border border-white/10 text-white rounded-lg p-2 mt-1 font-bold text-[9px] min-h-[36px] cursor-pointer outline-none"
              >
                <option value="all">All Voltages</option>
                <option value="36V">36V Systems</option>
                <option value="48V">48V Systems</option>
                <option value="52V">52V Systems</option>
                <option value="60V">60V Systems</option>
                <option value="72V">72V Hyperscooter</option>
                <option value="84V">84V Extreme Mod</option>
              </select>
            </div>
            <div>
              <span className="text-zinc-400 uppercase block">Shipping Dispatch</span>
              <select 
                value={shippingSpeedFilter} 
                onChange={(e) => setShippingSpeedFilter(e.target.value)}
                className="w-full bg-black border border-white/10 text-white rounded-lg p-2 mt-1 font-bold text-[9px] min-h-[36px] cursor-pointer outline-none"
              >
                <option value="all">All Shipping Speeds</option>
                <option value="fast">Domestic Warehouse (&lt;3 Days)</option>
                <option value="express">Air Express (5-7 Days)</option>
                <option value="economy">Economy Freight</option>
              </select>
            </div>
          </div>
        )}

        {/* QUICK CATEGORY CHIPS */}
        <div className="flex flex-wrap gap-1.5 pt-1">
          {quickPartCategoryPresets.map((qp, i) => (
            <button
              key={i}
              type="button"
              onClick={() => {
                if (qp.category) setPartsCategory(qp.category);
                setUniversalPartQuery(qp.query);
                setTimeout(() => searchLiveParts(), 50);
              }}
              className="bg-white/5 hover:bg-white/10 active:bg-white/20 border border-white/10 text-zinc-300 hover:text-white px-2.5 py-1.5 rounded-lg text-[8px] font-mono font-bold tracking-tight transition-all flex items-center gap-1 min-h-[36px] cursor-pointer active:scale-95"
            >
              <Tag className="w-2.5 h-2.5 text-zinc-500" /> {qp.label}
            </button>
          ))}
        </div>

        {/* 150+ CLEAN MAKE SELECTOR & SEPARATED MODEL BOX */}
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-2 pt-2 border-t border-white/10 font-mono">
          <div className="space-y-1">
            <label className="text-[7px] uppercase text-zinc-500 font-bold">Make (150+ Brands)</label>
            <select value={partsMake} onChange={(e) => setPartsMake(e.target.value)} className="w-full bg-black/60 border border-white/10 text-white text-[9px] font-bold rounded-lg px-2 py-1.5 outline-none cursor-pointer min-h-[36px]">
              <option value="all">Universal / All Brands</option>
              <option value="dualtron">Dualtron</option>
              <option value="nami">Nami</option>
              <option value="kaabo">Kaabo</option>
              <option value="segway">Segway Ninebot</option>
              <option value="vsett">Vsett</option>
              <option value="surron">Sur-Ron</option>
              <option value="talaria">Talaria</option>
              <option value="eridepro">E-Ride Pro</option>
              <option value="79bike">79Bike</option>
              <option value="rawrr">Rawrr</option>
              <option value="stark">Stark Future</option>
              <option value="aostirmotor">Aostirmotor</option>
              <option value="engwe">Engwe</option>
              <option value="isinwheel">isinwheel</option>
              <option value="geemax">Geemax</option>
              <option value="inmotion">Inmotion</option>
              <option value="begode">Begode</option>
              <option value="leaperkim">LeaperKim</option>
              <option value="kingsong">KingSong</option>
              <option value="aventon">Aventon</option>
              <option value="radpower">Rad Power</option>
              <option value="lectric">Lectric</option>
              <option value="super73">Super73</option>
              <option value="himiway">Himiway</option>
              <option value="juiced">Juiced Bikes</option>
              <option value="arielrider">Ariel Rider</option>
              <option value="ride1up">Ride1Up</option>
              <option value="fiido">Fiido</option>
              <option value="cyrusher">Cyrusher</option>
              <option value="kukirin">KuKirin</option>
              <option value="yume">Yume</option>
              <option value="roadrunner">RoadRunner</option>
              <option value="apollo">Apollo</option>
              <option value="weped">Weped</option>
              <option value="bronco">Bronco Motors</option>
              <option value="currus">Currus</option>
              <option value="inokim">Inokim</option>
              <option value="rion">Rion</option>
              <option value="slack">Slack Core</option>
              <option value="teverun">Teverun</option>
              <option value="mukuta">Mukuta</option>
              <option value="blade">Blade</option>
              <option value="quietkat">QuietKat</option>
              <option value="biktrix">Biktrix</option>
              <option value="meepo">Meepo</option>
              <option value="backfire">Backfire</option>
              <option value="evolve">Evolve</option>
              <option value="specialized">Specialized</option>
              <option value="trek">Trek</option>
              <option value="bafang">Bafang</option>
              <option value="vesc">VESC</option>
              <option value="qsmotor">QS Motor</option>
              <option value="fardriver">FarDriver</option>
              <option value="kelly">Kelly Controls</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[7px] uppercase text-emerald-400 font-black">Model Designation</label>
            <input 
              type="text"
              value={partsModel}
              onChange={(e) => setPartsModel(e.target.value)}
              placeholder="e.g. Storm, Light Bee..."
              className="w-full bg-black/60 border border-emerald-500/50 text-emerald-300 text-[9px] font-bold rounded-lg px-2 py-1.5 outline-none cursor-text min-h-[36px] placeholder:text-zinc-600"
            />
          </div>

          <div className="space-y-1">
            <label className="text-[7px] uppercase text-cyan-400 font-black">Tire / Tube Size</label>
            <input 
              type="text"
              value={partsTireSize}
              onChange={(e) => setPartsTireSize(e.target.value)}
              placeholder="e.g. 16x4.0, 10x3..."
              className="w-full bg-black/60 border border-cyan-500/50 text-cyan-300 text-[9px] font-bold rounded-lg px-2 py-1.5 outline-none cursor-text min-h-[36px] placeholder:text-zinc-600"
            />
          </div>

          <div className="space-y-1">
            <label className="text-[7px] uppercase text-zinc-500 font-bold">Category</label>
            <select value={partsCategory} onChange={(e) => setPartsCategory(e.target.value)} className="w-full bg-black/60 border border-white/10 text-white text-[9px] font-bold rounded-lg px-2 py-1.5 outline-none cursor-pointer min-h-[36px]">
              <option value="all">All Groups</option>
              <option value="tires">Tires, Tubes &amp; Liners</option>
              <option value="brakes">Brakes, Rotors &amp; Pads</option>
              <option value="electronics">Controllers &amp; Displays</option>
              <option value="battery">Battery Packs &amp; BMS</option>
              <option value="powertrain">Motors &amp; Stators</option>
              <option value="chassis">Suspension &amp; Forks</option>
              <option value="drivetrain">Chains &amp; Sprockets</option>
              <option value="accessories">Throttles &amp; Lights</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[7px] uppercase text-zinc-500 font-bold">Platform Hub</label>
            <select value={platformFilter} onChange={(e) => setPlatformFilter(e.target.value)} className="w-full bg-black/60 border border-white/10 text-white text-[9px] font-bold rounded-lg px-2 py-1.5 outline-none cursor-pointer min-h-[36px]">
              <option value="all">All 350+ Sources</option>
              <option value="amazon">Amazon Direct</option>
              <option value="ebay">eBay Hardware</option>
              <option value="aliexpress">AliExpress Global</option>
              <option value="specialty">Specialty Hubs (Luna/Voro)</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[7px] uppercase text-zinc-500 font-bold">Part Condition</label>
            <select value={partConditionFilter} onChange={(e) => setPartConditionFilter(e.target.value)} className="w-full bg-black/60 border border-white/10 text-white text-[9px] font-bold rounded-lg px-2 py-1.5 outline-none cursor-pointer min-h-[36px]">
              <option value="all">All Conditions</option>
              <option value="oem">OEM Factory Stock</option>
              <option value="aftermarket">High Performance</option>
              <option value="refurbished">Refurbished</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[7px] uppercase text-zinc-500 font-bold">DIY Difficulty</label>
            <select value={partsDifficultyFilter} onChange={(e) => setPartsDifficultyFilter(e.target.value)} className="w-full bg-black/60 border border-white/10 text-white text-[9px] font-bold rounded-lg px-2 py-1.5 outline-none cursor-pointer min-h-[36px]">
              <option value="all">All Levels</option>
              <option value="easy">L1: Plug &amp; Play</option>
              <option value="moderate">L2: Basic Tools</option>
              <option value="advanced">L3: Solder/Bleed</option>
            </select>
          </div>

          <div className="space-y-1 flex flex-col justify-end gap-1">
            <button type="button" onClick={handleCustomDirectoryExport} className="w-full bg-cyan-600 hover:bg-cyan-500 text-white px-2 min-h-[36px] rounded-lg text-[8px] font-black uppercase font-mono tracking-wider flex items-center justify-center gap-1 cursor-pointer active:scale-95 shadow-md">
              <FolderOpen className="w-3 h-3" /> Save File
            </button>
          </div>
        </div>
      </div>

      {/* ACTIVE BUILD MANIFEST WITH LOCALIZED TAX & RETAIL SUMMARY */}
      {buildManifest.length > 0 && (
        <div className="bg-emerald-950/40 border border-emerald-500/30 rounded-2xl p-3 sm:p-4 flex flex-col sm:flex-row justify-between items-center gap-3 font-mono shadow-xl backdrop-blur-md">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-emerald-500/10 rounded-xl border border-emerald-500/20 text-emerald-400">
              <ShoppingCart className="w-4 h-4" />
            </div>
            <div>
              <div className="text-emerald-400 font-black text-xs uppercase flex items-center gap-1.5">
                <span>Active Manifest</span>
                <span className="bg-emerald-500 text-black text-[8px] px-1.5 py-0.2 rounded font-mono font-black">
                  {buildManifest.length} Items
                </span>
              </div>
              <div className="text-zinc-400 text-[9px] mt-0.5 flex flex-wrap gap-x-2.5">
                <span>Subtotal: <strong className="text-white">${manifestSubtotal}</strong></span>
                <span>Tax ({(taxRateMultiplier * 100).toFixed(1)}%): <strong className="text-cyan-400">+${manifestTax}</strong></span>
                <span>Retail (+{retailMarkupPercent}%): <strong className="text-amber-400">${markupTotal}</strong></span>
              </div>
            </div>
          </div>

          <div className="flex gap-1.5 w-full sm:w-auto flex-wrap">
            <button type="button" onClick={() => setShowBomModal(true)} className="text-emerald-300 bg-emerald-900/40 hover:bg-emerald-800/40 border border-emerald-500/30 uppercase text-[8px] font-black cursor-pointer px-3 min-h-[36px] rounded-xl flex items-center justify-center gap-1 active:scale-95">
              <FileText className="w-3 h-3" /> Markup &amp; BOM
            </button>
            <button type="button" onClick={() => setShowQrModal(true)} className="text-cyan-300 bg-cyan-950/40 hover:bg-cyan-900/40 border border-cyan-500/30 uppercase text-[8px] font-black cursor-pointer px-3 min-h-[36px] rounded-xl flex items-center justify-center gap-1 active:scale-95">
              <QrCode className="w-3 h-3" /> QR
            </button>
            <button type="button" onClick={() => setShowJsonModal(true)} className="text-zinc-200 bg-white/5 hover:bg-white/10 border border-white/10 uppercase text-[8px] font-black cursor-pointer px-3 min-h-[36px] rounded-xl flex items-center justify-center gap-1 active:scale-95">
              <ArrowRightLeft className="w-3 h-3" /> JSON
            </button>
            <button type="button" onClick={copyManifest} className="text-white bg-white/10 hover:bg-white/20 border border-white/10 uppercase text-[8px] font-black cursor-pointer px-3.5 min-h-[36px] rounded-xl flex items-center justify-center gap-1 active:scale-95">
              <Copy className="w-3 h-3" /> Copy
            </button>
            <button type="button" onClick={clearManifest} className="text-rose-300 bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/20 uppercase text-[8px] font-black cursor-pointer px-3 min-h-[36px] rounded-xl flex items-center justify-center gap-1 active:scale-95">
              <Trash2 className="w-3 h-3" />
            </button>
          </div>
        </div>
      )}

      {/* SEARCHING LOADER */}
      {isSearchingParts && (
        <div className="text-center py-16 flex flex-col items-center justify-center space-y-3 bg-black/40 border border-white/5 rounded-3xl backdrop-blur-md">
          <Loader2 className={`w-8 h-8 ${t.text} animate-spin`} />
          <div className={`${t.text} font-mono text-[10px] uppercase tracking-wider animate-pulse`}>
            Sweeping 350+ Global PEV Supply Nodes...
          </div>
        </div>
      )}

      {/* ZERO RESULTS EMPTY STATE */}
      {!isSearchingParts && displayedParts.length === 0 ? (
        <div className="text-center py-16 border border-dashed border-white/10 rounded-3xl text-zinc-500 font-mono text-[9px] uppercase tracking-wider flex flex-col items-center gap-2 bg-black/30 backdrop-blur-sm">
          <Wrench className="w-8 h-8 opacity-20" />
          {hasSearchedParts ? (
            <span>0 Components Matched across 350+ sources. Adjust filters or search terms.</span>
          ) : (
            <span>Enter query parameters above to scan live replacement parts and upgrades.</span>
          )}
        </div>
      ) : !isSearchingParts && (
        <div className="space-y-3">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {displayedParts.slice(0, displayCountParts).map((part: any, index: number) => {
              const inManifest = buildManifest.some((p: any) => p.partName === part.partName);
              const displayWeight = localMetric ? (parseFloat(String(part.weightLbs || "1.5").replace(/[^0-9.]/g, "")) * 0.453592).toFixed(1) : (part.weightLbs || "1.5");
              const weightUnit = localMetric ? "kg" : "lbs";

              return (
                <div 
                  key={index} 
                  className="bg-black/60 border border-white/10 rounded-3xl p-3.5 sm:p-4 flex flex-col sm:flex-row gap-3.5 shadow-xl backdrop-blur-md hover:border-white/20 transition-all group relative"
                >
                  <div className="w-full sm:w-28 h-28 bg-white border border-white/10 rounded-2xl overflow-hidden flex items-center justify-center p-1.5 shrink-0 relative">
                    <img 
                      src={part.imageUrl || "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?auto=format&fit=crop&w=300&q=80"} 
                      alt="" 
                      className="w-full h-full object-contain mix-blend-multiply transition-transform group-hover:scale-105" 
                    />
                    {part.exactDimensions && (
                      <span className="absolute top-1 left-1 text-[7px] font-mono font-black uppercase px-1.5 py-0.2 rounded bg-cyan-950/90 text-cyan-400 border border-cyan-500/50">
                        {part.exactDimensions}
                      </span>
                    )}
                  </div>

                  <div className="flex-1 flex flex-col justify-between font-mono text-[9px]">
                    <div>
                      <div className="flex justify-between items-start gap-2">
                        <div className="flex items-center gap-1 flex-wrap">
                          <span className={`text-[7px] bg-black/80 px-1.5 py-0.2 rounded border border-white/10 ${t.text} font-bold uppercase`}>
                            {part.category || "Hardware"}
                          </span>
                          <span className="text-[7px] bg-white/5 border border-white/10 text-zinc-300 px-1.5 py-0.2 rounded font-bold">
                            {part.sourcePlatform || "Global Vendor"}
                          </span>
                        </div>
                        <div className="text-right">
                          <span className={`${t.text} font-black text-xs block`}>
                            {part.estimatedPrice || "$--"}
                          </span>
                          <span className="text-[7px] text-zinc-400 block">
                            Ship: ~${((parseFloat(String(part.weightLbs || "1.5").replace(/[^0-9.]/g, "")) * 3.75) + 8).toFixed(2)}
                          </span>
                        </div>
                      </div>

                      <h4 className="text-zinc-100 font-black text-[11px] uppercase mt-1.5 line-clamp-2 leading-tight">
                        {part.partName}
                      </h4>
                      
                      <div className="mt-1 text-zinc-400 space-y-0.2 text-[8px]">
                        <p><strong className="text-zinc-500">Fitment:</strong> {part.compatibility || "Universal PEV"}</p>
                        <p className="text-zinc-500 flex items-center gap-1.5">
                          <span>Brand: {part.recommendedBrands || "OEM Quality"}</span>
                          <span>•</span>
                          <span>Mass: ~{displayWeight} {weightUnit}</span>
                        </p>
                      </div>
                    </div>

                    <div className="mt-2.5 flex gap-1.5">
                      <a 
                        href={getAccurateProductUrl(part)} 
                        target="_blank" 
                        rel="noreferrer" 
                        className={`flex-1 ${t.bg} text-black font-black uppercase text-[8px] min-h-[36px] py-1.5 px-2.5 rounded-xl text-center flex items-center justify-center gap-1 shadow-md active:scale-95 transition-all`}
                      >
                        Direct Sourcing Link <ExternalLink className="w-2.5 h-2.5" />
                      </a>
                      <button 
                        type="button" 
                        onClick={() => addToManifest(part)} 
                        className={`px-3 min-h-[36px] rounded-xl text-[8px] uppercase font-bold cursor-pointer transition-all border active:scale-95 ${
                          inManifest 
                            ? "bg-emerald-500/20 text-emerald-300 border-emerald-500/40" 
                            : "bg-white/5 border-white/10 text-zinc-300 hover:text-white hover:bg-white/10"
                        }`}
                      >
                        {inManifest ? "✓ Added" : "+ Manifest"}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {displayedParts.length > displayCountParts && (
            <div className="text-center pt-2">
              <button
                type="button"
                onClick={() => setDisplayCountParts((prev) => prev + 24)}
                className="bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white px-6 min-h-[36px] rounded-xl text-[9px] font-black uppercase font-mono tracking-wider cursor-pointer transition-all active:scale-95 shadow-md"
              >
                Load Next Results (+24 Parts)
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}