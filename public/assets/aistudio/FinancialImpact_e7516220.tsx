"use client";

import React, { useState, useEffect, useMemo } from "react";
import { 
  DollarSign, Fuel, Car, Wrench, Leaf, Droplets, Zap, 
  TrendingUp, Calculator, ShieldCheck, TreePine, 
  Plus, Minus, Coins, PiggyBank, Sparkles, Briefcase, 
  Calendar, HeartPulse, ParkingSquare, Copy, Check
} from "lucide-react";
import { motion } from "framer-motion";

const safeNum = (val: any, fallback = 0): number => {
  const parsed = Number(val);
  return isNaN(parsed) || !isFinite(parsed) ? fallback : parsed;
};

export interface FinancialImpactProps {
  totalMiles: number;
  ui: any;
  triggerHaptic?: () => void;
  useMetric?: boolean;
}

export default function FinancialImpact({ totalMiles = 0, ui, triggerHaptic = () => {}, useMetric = false }: FinancialImpactProps) {
  const [localMetric, setLocalMetric] = useState<boolean>(useMetric);

  // --- MASTER SETTINGS SYNC ENGINE ---
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

  useEffect(() => {
    if (useMetric !== undefined) setLocalMetric(useMetric);
  }, [useMetric]);

  // Configurable Parameters
  const [localGasPrice, setLocalGasPrice] = useState<number>(3.50);
  const [referenceMpg, setReferenceMpg] = useState<number>(25);
  const [electricityRateKwh, setElectricityRateKwh] = useState<number>(0.13); // $/kWh
  const [pevEfficiencyWhMi, setPevEfficiencyWhMi] = useState<number>(25); // Wh/mi
  const [initialPevCost, setInitialPevCost] = useState<number>(899);
  const [dailyCommuteMiles, setDailyCommuteMiles] = useState<number>(12);
  const [parkingCostPerDay, setParkingCostPerDay] = useState<number>(12);
  const [isBusinessUse, setIsBusinessUse] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<"summary" | "roi" | "projections" | "maintenance" | "eco">("summary");
  const [copiedSummary, setCopiedSummary] = useState<boolean>(false);

  // Environmental & Financial Constants (Adjusted for Metric if active)
  const CO2_SAVED_PER_MILE_LBS = localMetric ? 0.55 : 0.89;
  const IRS_BUSINESS_RATE = 0.67;
  const LBS_CO2_PER_TREE_YEAR = 48;
  const CALORIES_BURNED_PER_MILE_PEV = 35;
  const distLabel = localMetric ? "KM" : "MI";

  // Load Saved Preferences
  useEffect(() => {
    try {
      const savedGas = localStorage.getItem("pev_local_gas_price");
      if (savedGas) setLocalGasPrice(Number(savedGas));
      const savedMpg = localStorage.getItem("pev_reference_mpg");
      if (savedMpg) setReferenceMpg(Number(savedMpg));
      const savedKwh = localStorage.getItem("pev_electric_rate");
      if (savedKwh) setElectricityRateKwh(Number(savedKwh));
      const savedWhMi = localStorage.getItem("pev_efficiency_wh_mi");
      if (savedWhMi) setPevEfficiencyWhMi(Number(savedWhMi));
      const savedCost = localStorage.getItem("pev_initial_hardware_cost");
      if (savedCost) setInitialPevCost(Number(savedCost));
      const savedDaily = localStorage.getItem("pev_daily_commute");
      if (savedDaily) setDailyCommuteMiles(Number(savedDaily));
      const savedPark = localStorage.getItem("pev_parking_cost");
      if (savedPark) setParkingCostPerDay(Number(savedPark));
      const savedBiz = localStorage.getItem("pev_business_use");
      if (savedBiz) setIsBusinessUse(savedBiz === "true");
    } catch (e) {}
  }, []);

  // Persist Updates
  useEffect(() => {
    try {
      localStorage.setItem("pev_local_gas_price", localGasPrice.toString());
      localStorage.setItem("pev_reference_mpg", referenceMpg.toString());
      localStorage.setItem("pev_electric_rate", electricityRateKwh.toString());
      localStorage.setItem("pev_efficiency_wh_mi", pevEfficiencyWhMi.toString());
      localStorage.setItem("pev_initial_hardware_cost", initialPevCost.toString());
      localStorage.setItem("pev_daily_commute", dailyCommuteMiles.toString());
      localStorage.setItem("pev_parking_cost", parkingCostPerDay.toString());
      localStorage.setItem("pev_business_use", isBusinessUse.toString());
    } catch (e) {}
  }, [localGasPrice, referenceMpg, electricityRateKwh, pevEfficiencyWhMi, initialPevCost, dailyCommuteMiles, parkingCostPerDay, isBusinessUse]);

  const adjust = (setter: (val: number) => void, current: number, delta: number, min = 0.01, max = 99999) => {
    triggerHaptic();
    const nextVal = Math.max(min, Math.min(max, Math.round((current + delta) * 100) / 100));
    setter(nextVal);
  };

  const metrics = useMemo(() => {
    const rawMiles = safeNum(totalMiles, 0);
    const miles = localMetric ? rawMiles * 0.621371 : rawMiles; 
    const gasPrice = safeNum(localGasPrice, 3.50);
    const mpg = Math.max(1, safeNum(referenceMpg, 25));
    const kwhRate = safeNum(electricityRateKwh, 0.13);
    const whPerMile = safeNum(pevEfficiencyWhMi, 25);
    const pevCost = safeNum(initialPevCost, 899);
    const dailyCommute = safeNum(dailyCommuteMiles, 12);
    const parkingDaily = safeNum(parkingCostPerDay, 12);

    const gasGallonsSaved = miles / mpg;
    const gasMoneySaved = gasGallonsSaved * gasPrice;
    
    const totalKwhConsumed = (miles * whPerMile) / 1000;
    const totalElectricityCost = totalKwhConsumed * kwhRate;
    const costPerElectricMile = (whPerMile / 1000) * kwhRate;

    const estimatedWorkDaysRidden = Math.max(1, Math.round(miles / Math.max(1, dailyCommute)));
    const parkingMoneySaved = estimatedWorkDaysRidden * parkingDaily;
    const businessTaxDeduction = isBusinessUse ? miles * IRS_BUSINESS_RATE : 0;
    
    const oilChangesAvoidedCount = Math.floor(miles / 5000);
    const oilChangeDollarsSaved = oilChangesAvoidedCount * 75;
    const brakePadWearSaved = (miles / 30000) * 250;
    
    const totalNetSavings = (gasMoneySaved + oilChangeDollarsSaved + brakePadWearSaved + parkingMoneySaved + businessTaxDeduction) - totalElectricityCost;

    const savingsPerMile = (gasPrice / mpg) + (parkingDaily / Math.max(1, dailyCommute)) + 0.08 - costPerElectricMile;
    const breakEvenMiles = pevCost > 0 ? pevCost / Math.max(0.1, savingsPerMile) : 0;
    const breakEvenProgressPct = Math.min(100, Math.max(0, (miles / Math.max(1, breakEvenMiles)) * 100));
    const netProfitAfterHardwareCost = totalNetSavings - pevCost;

    const annualProjectedMiles = dailyCommute * 250;
    const annualNetSavings = annualProjectedMiles * savingsPerMile;

    const totalCo2SavedLbs = miles * CO2_SAVED_PER_MILE_LBS;
    const equivalentTreesPlanted = totalCo2SavedLbs / LBS_CO2_PER_TREE_YEAR;
    const barrelsOfCrudeSaved = gasGallonsSaved / 42;
    const caloriesBurned = miles * CALORIES_BURNED_PER_MILE_PEV;

    return {
      miles,
      gasGallonsSaved,
      gasMoneySaved,
      totalKwhConsumed,
      totalElectricityCost,
      costPerElectricMile,
      parkingMoneySaved,
      businessTaxDeduction,
      oilChangesAvoidedCount,
      oilChangeDollarsSaved,
      brakePadWearSaved,
      totalNetSavings,
      breakEvenMiles,
      breakEvenProgressPct,
      netProfitAfterHardwareCost,
      annualProjectedMiles,
      annualNetSavings,
      totalCo2SavedLbs,
      equivalentTreesPlanted,
      barrelsOfCrudeSaved,
      caloriesBurned
    };
  }, [totalMiles, localGasPrice, referenceMpg, electricityRateKwh, pevEfficiencyWhMi, initialPevCost, dailyCommuteMiles, parkingCostPerDay, isBusinessUse, localMetric]);

  const copySummaryReport = () => {
    triggerHaptic();
    const text = `RURAL ERIDES GO - FINANCIAL IMPACT REPORT\n` +
      `Total Mileage: ${metrics.miles.toFixed(1)} ${distLabel}\n` +
      `Net Savings: $${metrics.totalNetSavings.toFixed(2)}\n` +
      `Gas Unburned: $${metrics.gasMoneySaved.toFixed(2)} (${metrics.gasGallonsSaved.toFixed(1)} gal)\n` +
      `Parking Saved: $${metrics.parkingMoneySaved.toFixed(2)}\n` +
      `CO2 Reduced: ${metrics.totalCo2SavedLbs.toFixed(1)} lbs\n` +
      `Calories Burned: ${metrics.caloriesBurned.toLocaleString()} kcal`;
    navigator.clipboard.writeText(text);
    setCopiedSummary(true);
    setTimeout(() => setCopiedSummary(false), 2500);
  };

  return (
    <div className={`${ui?.bgPanel || 'bg-[#0d0e15]'} p-3.5 sm:p-5 rounded-3xl border shadow-xl transition-colors space-y-3.5 sm:space-y-4 ${ui?.brd || 'border-zinc-800'}`}>
      
      {/* HEADER BAR */}
      <div className={`flex flex-col sm:flex-row justify-between items-start sm:items-center border-b ${ui?.brd || 'border-zinc-800'} pb-3 gap-2.5 w-full`}>
        <div className="flex items-center gap-2.5">
          <div className="bg-emerald-500/20 border border-emerald-500/50 p-2 rounded-xl text-emerald-400 shadow-sm shrink-0">
            <DollarSign className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <h3 className={`${ui?.txtMain || 'text-white'} font-black uppercase tracking-widest text-xs sm:text-sm flex items-center gap-2`}>
              Financial Impact &amp; ROI Ledger Pro
            </h3>
            <p className={`text-[8.5px] ${ui?.txtMuted || 'text-zinc-400'} font-mono uppercase mt-0.5`}>
              IRS Standard Write-Off • Hardware Payback • Projections ({distLabel})
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 font-mono w-full sm:w-auto justify-between sm:justify-end">
          <span className="text-[9px] uppercase px-2.5 py-1 rounded-lg border border-emerald-900/60 bg-emerald-950/60 text-emerald-400 font-bold">
            NET SAVED: ${metrics.totalNetSavings.toFixed(2)}
          </span>
          <button
            onClick={copySummaryReport}
            className="min-h-[38px] px-3 bg-zinc-900 hover:bg-zinc-800 border border-zinc-700 text-zinc-200 rounded-xl text-[9px] font-mono font-bold flex items-center gap-1.5 cursor-pointer active:scale-95 touch-manipulation"
            title="Copy Report to Clipboard"
          >
            {copiedSummary ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
            {copiedSummary ? "Copied" : "Share"}
          </button>
        </div>
      </div>

      {/* HORIZONTAL NAVIGATION TABS (COMPACT MOBILE BUTTONS) */}
      <div className="flex gap-1.5 overflow-x-auto custom-scrollbar pb-1 -mx-1 px-1 touch-pan-x">
        {[
          { id: "summary", label: "Summary", icon: PiggyBank },
          { id: "roi", label: "ROI & Payback", icon: TrendingUp },
          { id: "projections", label: "Projections", icon: Calendar },
          { id: "maintenance", label: "Maintenance", icon: Wrench },
          { id: "eco", label: "Eco & Health", icon: Leaf },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => { triggerHaptic(); setActiveTab(tab.id as any); }}
            className={`min-h-[40px] px-3 rounded-xl text-[9px] sm:text-[10px] font-mono font-black uppercase tracking-wider transition-all flex items-center justify-center gap-1.5 shrink-0 cursor-pointer active:scale-95 touch-manipulation ${activeTab === tab.id ? `${ui?.t?.bg || 'bg-[#39ff14]'} text-black font-black shadow-md ring-1 ring-white/20` : `${ui?.bgList || 'bg-[#121318]'} ${ui?.brd || 'border-zinc-800'} border ${ui?.txtMuted || 'text-zinc-400'} hover:${ui?.txtMain || 'text-white'}`}`}
          >
            <tab.icon className="w-3.5 h-3.5 shrink-0" /> {tab.label}
          </button>
        ))}
      </div>

      {/* --- TAB 1: SAVINGS SUMMARY & CORE PARAMS --- */}
      {activeTab === "summary" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2.5">
            
            {/* Local Gas Price */}
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2`}>
              <div className="flex justify-between items-center">
                <label className={`text-[8px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'} flex items-center gap-1`}>
                  <Fuel className="w-3 h-3 text-amber-400" /> Fuel Price ($/Gal)
                </label>
                <span className="text-xs font-mono font-black text-amber-400">${localGasPrice.toFixed(2)}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjust(setLocalGasPrice, localGasPrice, -0.10, 1.00, 10.00)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input 
                  type="number" step="0.05" value={localGasPrice} 
                  onChange={e => setLocalGasPrice(Number(e.target.value))} 
                  className={`flex-1 min-h-[40px] ${ui?.bgInput || 'bg-black'} text-center text-xs font-mono font-black text-white rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none`}
                />
                <button onClick={() => adjust(setLocalGasPrice, localGasPrice, 0.10, 1.00, 10.00)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Reference Car MPG */}
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2`}>
              <div className="flex justify-between items-center">
                <label className={`text-[8px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'} flex items-center gap-1`}>
                  <Car className="w-3 h-3 text-cyan-400" /> Car MPG
                </label>
                <span className="text-xs font-mono font-black text-cyan-400">{referenceMpg} MPG</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjust(setReferenceMpg, referenceMpg, -1, 5, 100)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input 
                  type="number" step="1" value={referenceMpg} 
                  onChange={e => setReferenceMpg(Number(e.target.value))} 
                  className={`flex-1 min-h-[40px] ${ui?.bgInput || 'bg-black'} text-center text-xs font-mono font-black text-white rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none`}
                />
                <button onClick={() => adjust(setReferenceMpg, referenceMpg, 1, 5, 100)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Home Electricity Cost */}
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2`}>
              <div className="flex justify-between items-center">
                <label className={`text-[8px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'} flex items-center gap-1`}>
                  <Zap className="w-3 h-3 text-yellow-400" /> Grid Power ($/kWh)
                </label>
                <span className="text-xs font-mono font-black text-yellow-400">${electricityRateKwh.toFixed(2)}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjust(setElectricityRateKwh, electricityRateKwh, -0.01, 0.01, 1.00)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input 
                  type="number" step="0.01" value={electricityRateKwh} 
                  onChange={e => setElectricityRateKwh(Number(e.target.value))} 
                  className={`flex-1 min-h-[40px] ${ui?.bgInput || 'bg-black'} text-center text-xs font-mono font-black text-white rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none`} 
                />
                <button onClick={() => adjust(setElectricityRateKwh, electricityRateKwh, 0.01, 0.01, 1.00)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Daily Parking Cost Saved */}
            <div className={`${ui?.bgCard || 'bg-black/60'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2`}>
              <div className="flex justify-between items-center">
                <label className={`text-[8px] font-black uppercase tracking-widest ${ui?.txtMuted || 'text-zinc-400'} flex items-center gap-1`}>
                  <ParkingSquare className="w-3 h-3 text-purple-400" /> Parking ($/Day)
                </label>
                <span className="text-xs font-mono font-black text-purple-400">${parkingCostPerDay.toFixed(2)}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <button onClick={() => adjust(setParkingCostPerDay, parkingCostPerDay, -1, 0, 100)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <input 
                  type="number" step="1" value={parkingCostPerDay} 
                  onChange={e => setParkingCostPerDay(Number(e.target.value))} 
                  className={`flex-1 min-h-[40px] ${ui?.bgInput || 'bg-black'} text-center text-xs font-mono font-black text-white rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none`} 
                />
                <button onClick={() => adjust(setParkingCostPerDay, parkingCostPerDay, 1, 0, 100)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

          </div>

          {/* Primary Savings Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center flex flex-col justify-between shadow-inner`}>
              <Fuel className="w-4 h-4 text-amber-400 mx-auto mb-1" />
              <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block mb-0.5`}>Gas Unburned</span>
              <span className="text-sm font-black font-mono text-amber-400 tabular-nums">${metrics.gasMoneySaved.toFixed(2)}</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center flex flex-col justify-between shadow-inner`}>
              <ParkingSquare className="w-4 h-4 text-purple-400 mx-auto mb-1" />
              <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block mb-0.5`}>Parking Saved</span>
              <span className="text-sm font-black font-mono text-purple-400 tabular-nums">${metrics.parkingMoneySaved.toFixed(2)}</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center flex flex-col justify-between shadow-inner`}>
              <Wrench className="w-4 h-4 text-rose-400 mx-auto mb-1" />
              <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block mb-0.5`}>Wear Avoided</span>
              <span className="text-sm font-black font-mono text-rose-400 tabular-nums">${(metrics.oilChangeDollarsSaved + metrics.brakePadWearSaved).toFixed(2)}</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center flex flex-col justify-between shadow-inner`}>
              <Zap className="w-4 h-4 text-yellow-400 mx-auto mb-1" />
              <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block mb-0.5`}>Charging Cost</span>
              <span className="text-sm font-black font-mono text-yellow-400 tabular-nums">${metrics.totalElectricityCost.toFixed(2)}</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center flex flex-col justify-between shadow-inner`}>
              <Coins className="w-4 h-4 text-cyan-400 mx-auto mb-1" />
              <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block mb-0.5`}>Cost / E-{distLabel}</span>
              <span className="text-sm font-black font-mono text-cyan-400 tabular-nums">${metrics.costPerElectricMile.toFixed(3)}/{distLabel.toLowerCase()}</span>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center flex flex-col justify-between shadow-inner`}>
              <Droplets className="w-4 h-4 text-blue-400 mx-auto mb-1" />
              <span className={`text-[8px] ${ui?.txtMuted || 'text-zinc-400'} font-black uppercase tracking-widest block mb-0.5`}>Gas Gallons</span>
              <span className="text-sm font-black font-mono text-blue-400 tabular-nums">{metrics.gasGallonsSaved.toFixed(1)} Gal</span>
            </div>
          </div>
        </motion.div>
      )}

      {/* --- TAB 2: ROI & HARDWARE PAYBACK BREAK-EVEN --- */}
      {activeTab === "roi" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          
          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 sm:p-4 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5`}>
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
              <div>
                <span className="text-xs font-black uppercase tracking-widest text-white flex items-center gap-1.5">
                  <Calculator className="w-3.5 h-3.5 text-cyan-400" /> Initial Hardware Acquisition Cost
                </span>
                <p className="text-[8.5px] text-zinc-400 font-mono mt-0.5">
                  Set your PEV purchase price to track vehicle payback break-even distance
                </p>
              </div>
              <span className="text-sm font-mono font-black text-cyan-400">${initialPevCost}</span>
            </div>

            <div className="flex items-center gap-1.5">
              <button onClick={() => adjust(setInitialPevCost, initialPevCost, -50, 50, 10000)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                <Minus className="w-4 h-4" />
              </button>
              <input 
                type="number" step="25" value={initialPevCost} 
                onChange={e => setInitialPevCost(Number(e.target.value))} 
                className={`flex-1 min-h-[40px] ${ui?.bgInput || 'bg-black'} text-center text-xs font-mono font-black text-white rounded-xl border ${ui?.brd || 'border-zinc-800'} outline-none`} 
              />
              <button onClick={() => adjust(setInitialPevCost, initialPevCost, 50, 50, 10000)} className="min-h-[40px] min-w-[40px] bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl flex items-center justify-center font-bold text-sm active:scale-95 touch-manipulation cursor-pointer border border-zinc-700">
                <Plus className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Payback Progress Bar */}
          <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 sm:p-4 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-2.5`}>
            <div className="flex justify-between items-center text-xs font-mono font-bold">
              <span className="text-zinc-300">Break-Even Mileage Target:</span>
              <span className="text-cyan-400">{Math.round(metrics.breakEvenMiles)} {distLabel}</span>
            </div>
            <div className="w-full bg-zinc-900 h-3.5 rounded-full overflow-hidden flex">
              <div 
                className={`h-full transition-all duration-500 ${metrics.breakEvenProgressPct >= 100 ? 'bg-emerald-500' : 'bg-cyan-500'}`} 
                style={{ width: `${metrics.breakEvenProgressPct}%` }}
              ></div>
            </div>
            <div className="flex justify-between items-center text-[9px] font-mono text-zinc-400 pt-0.5">
              <span>Ridden: {metrics.miles.toFixed(1)} {distLabel.toLowerCase()} ({metrics.breakEvenProgressPct.toFixed(1)}%)</span>
              <span className={metrics.netProfitAfterHardwareCost >= 0 ? "text-emerald-400 font-bold" : "text-amber-400 font-bold"}>
                {metrics.netProfitAfterHardwareCost >= 0 
                  ? `+$${metrics.netProfitAfterHardwareCost.toFixed(2)} Net Profit` 
                  : `-$${Math.abs(metrics.netProfitAfterHardwareCost).toFixed(2)} to Break-Even`}
              </span>
            </div>
          </div>
        </motion.div>
      )}

      {/* --- TAB 3: 1-5 YEAR PROJECTIONS & BUSINESS WRITE-OFFS --- */}
      {activeTab === "projections" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3.5">
          
          <div className={`${ui?.bgCard || 'bg-black/60'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2.5`}>
            <div className="flex items-center gap-2.5">
              <div className="bg-emerald-500/20 p-2 rounded-xl text-emerald-400 shrink-0">
                <Briefcase className="w-4 h-4" />
              </div>
              <div>
                <span className="text-xs font-black text-white uppercase block">IRS Business Mileage Deduction ($0.67/{distLabel.toLowerCase()})</span>
                <span className="text-[8.5px] text-zinc-400 font-mono">Enable for business or work-site commuting write-offs</span>
              </div>
            </div>
            <button
              onClick={() => { triggerHaptic(); setIsBusinessUse(!isBusinessUse); }}
              className={`min-h-[40px] px-3.5 rounded-xl text-[10px] font-mono font-bold uppercase transition-all cursor-pointer touch-manipulation ${isBusinessUse ? 'bg-emerald-500 text-black font-black shadow-md' : 'bg-zinc-800 text-zinc-400 hover:text-white'}`}
            >
              {isBusinessUse ? `Enabled ($0.67/${distLabel.toLowerCase()})` : "Disabled"}
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            {[
              { years: 1, label: "Year 1 Projection" },
              { years: 3, label: "3-Year Horizon" },
              { years: 5, label: "5-Year Long Term" },
            ].map((proj) => {
              const projSavings = (metrics.annualNetSavings * proj.years) + (isBusinessUse ? metrics.miles * IRS_BUSINESS_RATE * proj.years : 0);
              return (
                <div key={proj.years} className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1.5 text-center`}>
                  <Calendar className="w-4 h-4 text-cyan-400 mx-auto" />
                  <span className="text-[8.5px] font-black uppercase tracking-widest text-zinc-400 block">{proj.label}</span>
                  <span className="text-base sm:text-lg font-mono font-black text-emerald-400">${projSavings.toFixed(2)}</span>
                  <span className="text-[8px] font-mono text-zinc-500 block">Based on {(metrics.annualProjectedMiles).toLocaleString()} {distLabel.toLowerCase()}/yr</span>
                </div>
              );
            })}
          </div>
        </motion.div>
      )}

      {/* --- TAB 4: AUTO MAINTENANCE & WEAR AVOIDANCE --- */}
      {activeTab === "maintenance" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <div className="flex justify-between items-center text-zinc-400 text-[8.5px] font-mono uppercase">
                <span>Oil Changes Avoided</span>
                <Wrench className="w-3.5 h-3.5 text-amber-400" />
              </div>
              <div className="text-base sm:text-lg font-mono font-black text-white">{metrics.oilChangesAvoidedCount} Intervals</div>
              <div className="text-[9px] font-mono text-emerald-400 font-bold">+${metrics.oilChangeDollarsSaved.toFixed(2)} Saved</div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <div className="flex justify-between items-center text-zinc-400 text-[8.5px] font-mono uppercase">
                <span>Brake System Savings</span>
                <ShieldCheck className="w-3.5 h-3.5 text-rose-400" />
              </div>
              <div className="text-base sm:text-lg font-mono font-black text-white">${metrics.brakePadWearSaved.toFixed(2)}</div>
              <div className="text-[9px] font-mono text-zinc-400">Avoided auto rotor/pad wear</div>
            </div>

            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} space-y-1`}>
              <div className="flex justify-between items-center text-zinc-400 text-[8.5px] font-mono uppercase">
                <span>Vehicle Depreciation</span>
                <TrendingUp className="w-3.5 h-3.5 text-purple-400" />
              </div>
              <div className="text-base sm:text-lg font-mono font-black text-white">${(metrics.miles * 0.22).toFixed(2)}</div>
              <div className="text-[9px] font-mono text-zinc-400">Retained auto residual value</div>
            </div>
          </div>
        </motion.div>
      )}

      {/* --- TAB 5: ECO & HEALTH EQUIVALENCE --- */}
      {activeTab === "eco" && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-3">
          
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-2.5">
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1`}>
              <Leaf className="w-4 h-4 text-emerald-400 mx-auto mb-1" />
              <span className="text-[8px] uppercase text-zinc-400 font-bold block">CO2 Reduced</span>
              <span className="text-base font-mono font-black text-emerald-400">{metrics.totalCo2SavedLbs.toFixed(1)} lbs</span>
            </div>
            <div className={`${ui?.bgList || 'bg-[#121318]'} p-3.5 rounded-2xl border ${ui?.brd || 'border-zinc-800'} text-center space-y-1`}>
              <HeartPulse className="w-4 h-4 text-rose-400 mx-auto mb-1" />
              <span className="text-[8px] uppercase text-zinc-400 font-bold block">Calories Burned</span>
              <span className="text-base font-mono font-black text-rose-400">{metrics.caloriesBurned.toLocaleString()} kcal</span>
            </div>
          </div>
        </motion.div>
      )}

    </div>
  );
}