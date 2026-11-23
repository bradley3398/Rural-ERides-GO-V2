import React, { useState, useRef, useEffect, useMemo } from "react";
import { 
  Wrench, UploadCloud, XCircle, Camera, CheckCircle, 
  AlertTriangle, Trash2, History, Mic, MicOff, Volume2, Square, 
  ImagePlus, ListChecks, Copy, Loader2, Sparkles, ShieldAlert,
  ChevronDown, ChevronUp, Search, Download, ZoomIn, 
  Cpu, Zap, Gauge, Activity, FileText, Check, RefreshCw,
  Layers, ArrowRight, MessageSquare, PlusCircle, Save, Filter, Database,
  Flame, Crosshair, Eye, Ruler, Scan, ShieldCheck, Tag, GitCompare
} from "lucide-react";

interface ChecklistItem {
  text: string;
  completed: boolean;
}

interface Message {
  id: string;
  role: "user" | "ai" | "system";
  text: string;
  timestamp?: string;
  image?: string | null;
  comparisonImages?: string[];
  checklist?: ChecklistItem[];
}

interface DiagnosticPreset {
  id: string;
  cat: "POWERTRAIN" | "BATTERY & BMS" | "CONTROLLER & ELEC" | "CHASSIS & BRAKES" | "SAFETY & PRE-RIDE" | "FIRMWARE & TUNING";
  title: string;
  query: string;
}

interface AnomalyBox {
  label: string;
  ymin: number;
  xmin: number;
  ymax: number;
  xmax: number;
  color: string;
  severity?: "CRITICAL" | "WARNING" | "ADVISORY";
  wearPct?: number;
  componentType?: string;
}

export default function MechanicChat({
  scanHistory = [],
  clearAllHistory,
  loadPreviousSession,
  activeSessionId,
  galleryInputRef,
  cameraInputRef,
  handleFileChange,
  previewUrl,
  clearImage,
  analyzeImage: _analyzeImageOld,
  isAnalyzing = false,
  setIsAnalyzing,
  messages = [],
  setMessages,
  errorMsg,
  setErrorMsg,
  question = "",
  setQuestion,
  toggleListening,
  isListening = false,
  currentlyReadingId,
  handleTTS,
  copyToClipboard,
  copiedId,
  chatEndRef,
  themeColors,
  getGeminiApiKey,
  callsign,
  userFleet,
  unitSystem,
  aiDetailLevel,
  aiModelVersion,
  autoReadAloud,
  autoClearPhoto,
  triggerHaptic
}: any) {
  const t = themeColors || {
    text: "text-amber-400",
    bg: "bg-amber-400",
    dim: "bg-amber-400/10",
    borderSubtle: "border-amber-400/30"
  };

  const [isPresetDropdownOpen, setIsPresetDropdownOpen] = useState(false);
  const [presetSearch, setPresetSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState<string>("ALL");
  const [historySearch, setHistorySearch] = useState("");
  const [isZoomed, setIsZoomed] = useState(false);
  const [ttsSpeed, setTtsSpeed] = useState<number>(1.0);
  const [showWiringModal, setShowWiringModal] = useState(false);
  const [showErrorCodeModal, setShowErrorCodeModal] = useState(false);
  const [logFilter, setLogFilter] = useState<string>("ALL");

  // --- MULTI-ANGLE & AR ENHANCED STATES ---
  const [secondaryPreviewUrl, setSecondaryPreviewUrl] = useState<string | null>(null);
  const [detectedAnomalies, setDetectedAnomalies] = useState<AnomalyBox[]>([]);
  const [wiringPinoutMap, setWiringPinoutMap] = useState<Record<string, string> | null>(null);
  const [showThermalOverlay, setShowThermalOverlay] = useState<boolean>(false);
  const [showArReticles, setShowArReticles] = useState<boolean>(true);
  const [selectedAnomaly, setSelectedAnomaly] = useState<AnomalyBox | null>(null);
  const [inspectionScope, setInspectionScope] = useState<"ALL" | "WIRING" | "BRAKES" | "TIRES" | "BOLTS">("ALL");
  const [severityFilter, setSeverityFilter] = useState<"ALL" | "CRITICAL" | "WARNING">("ALL");

  const dropdownRef = useRef<HTMLDivElement>(null);
  const secondaryGalleryInputRef = useRef<HTMLInputElement>(null);

  const diagnosticCategories = ["ALL", "POWERTRAIN", "BATTERY & BMS", "CONTROLLER & ELEC", "CHASSIS & BRAKES", "FIRMWARE & TUNING", "SAFETY & PRE-RIDE"];

  const quickDiagnosticsList: DiagnosticPreset[] = [
    { id: "p1", cat: "POWERTRAIN", title: "Motor Cogging & Stutter Under Load", query: "Powertrain Diagnostic: Why is my brushless hub motor cogging, hesitating, or stuttering under initial throttle load?" },
    { id: "p2", cat: "POWERTRAIN", title: "5V Hall Sensor Multimeter Test", query: "Powertrain Diagnostic: Step-by-step procedure to probe 5V Hall effect sensors (VCC, GND, Signal A/B/C) with a digital multimeter." },
    { id: "p3", cat: "POWERTRAIN", title: "Motor Thermal Throttling & Cutoff", query: "Powertrain Diagnostic: Hub motor reaching critical thermal threshold and cutting power during sustained incline ascent." },
    { id: "p4", cat: "POWERTRAIN", title: "Planetary Gear Noise & Clutch Slip", query: "Powertrain Diagnostic: Grinding gear mesh and slipping clutch inside planetary gear hub reduction assembly." },
    { id: "p5", cat: "POWERTRAIN", title: "Reverse Phase Rotation Mapping", query: "Powertrain Diagnostic: Motor spinning backwards after controller replacement. Step-by-step 3-phase and 5-hall swap combinations." },
    { id: "p6", cat: "POWERTRAIN", title: "Hub Motor Bearing Replacement Guide", query: "Powertrain Diagnostic: Step-by-step procedure for removing the stator and pressing in new 6000-2RS or 6200-2RS bearings." },
    { id: "p7", cat: "POWERTRAIN", title: "Stator Rust & Water Ingress Recovery", query: "Powertrain Diagnostic: Hub motor was submerged. Give the protocol for baking the stator, rust removal, and re-varnishing with Glyptal." },
    { id: "p8", cat: "POWERTRAIN", title: "Sur-Ron Primary Belt Tensioning", query: "Powertrain Diagnostic: Proper sonic tension or deflection measurement protocol for a Sur-Ron/Talaria primary reduction belt." },
    { id: "p9", cat: "POWERTRAIN", title: "Bafang Mid-Drive Greasing Protocol", query: "Powertrain Diagnostic: Step-by-step disassembly and application of Mobil 28 or Mobilgrease 28 to the Bafang BBSHD/BBS02 nylon secondary gear." },
    { id: "b1", cat: "BATTERY & BMS", title: "Extreme Voltage Sag Under Draw", query: "Battery Diagnostic: Troubleshooting excessive voltage sag (>6V drop) and high internal series resistance (IR) under load." },
    { id: "b2", cat: "BATTERY & BMS", title: "BMS Instant Overcurrent Trip", query: "Battery Diagnostic: BMS latches off and kills total vehicle power when opening throttle from a dead stop." },
    { id: "b3", cat: "BATTERY & BMS", title: "Cell Group Voltage Imbalance", query: "Battery Diagnostic: Diagnosing and rebalancing unequal P-group voltages across 13S, 14S, 16S, or 20S lithium-ion packs." },
    { id: "b4", cat: "BATTERY & BMS", title: "Charger Stays Green / Won't Charge", query: "Battery Diagnostic: Charger LED remains green and refuses to deliver charging current. Blown charge fuse or BMS gate fault." },
    { id: "b5", cat: "BATTERY & BMS", title: "Cold Weather Low-Temp Preservation", query: "Battery Diagnostic: Winter storage state-of-charge (SoC) management and cold-temperature charging safety limits." },
    { id: "b6", cat: "BATTERY & BMS", title: "BMS Passive Bleed Balancing", query: "Battery Diagnostic: How long does it take for a standard Smart BMS to passively balance a 0.15V delta at 100% charge?" },
    { id: "b7", cat: "BATTERY & BMS", title: "Daly Smart BMS Bluetooth Reset", query: "Battery Diagnostic: Protocol for hardware resetting a Daly or ANT Smart BMS that is locked out of charging and Bluetooth connection." },
    { id: "b8", cat: "BATTERY & BMS", title: "XT90 Anti-Spark Connector Soldering", query: "Battery Diagnostic: Safe protocol for soldering XT90-S anti-spark connectors to live 72V pack discharge leads without shorting." },
    { id: "b9", cat: "BATTERY & BMS", title: "Molicel P42A vs Samsung 50E Specs", query: "Battery Diagnostic: Compare the internal resistance, max discharge, and capacity trade-offs between Molicel P42A and Samsung 50E/50S cells." },
    { id: "c1", cat: "CONTROLLER & ELEC", title: "Error 30H / UART Comm Dropout", query: "Electronics Diagnostic: Resolving Error 30H / UART communication failure between display unit and motor controller." },
    { id: "c2", cat: "CONTROLLER & ELEC", title: "Complete Power Loss / Dead Console", query: "Electronics Diagnostic: Display won't power on. Checking ignition circuit, lock wire, and 12V/5V step-down buck rails." },
    { id: "c3", cat: "CONTROLLER & ELEC", title: "Throttle Signal Rail Testing", query: "Electronics Diagnostic: Probing 3-wire Hall throttle (5V Red, GND Black, 0.8V-4.2V Signal) with digital multimeter." },
    { id: "c4", cat: "CONTROLLER & ELEC", title: "Controller MOSFET Short-Circuit Test", query: "Electronics Diagnostic: Performing diode continuity check across battery input leads and motor phase lines for shorted FETs." },
    { id: "c5", cat: "CONTROLLER & ELEC", title: "Brake Cut-Off Sensor Switch Stuck", query: "Electronics Diagnostic: Magnetic reed e-brake sensor or hydraulic inline pressure switch stuck open, cutting motor feed." },
    { id: "c6", cat: "CONTROLLER & ELEC", title: "VESC Auto-Detection Failure (Flux)", query: "Electronics Diagnostic: VESC motor auto-detection failed during FOC setup. How to manually measure flux linkage and resistance." },
    { id: "c7", cat: "CONTROLLER & ELEC", title: "FarDriver Auto-Learn Protocol", query: "Electronics Diagnostic: Step-by-step Bluetooth protocol for initiating FarDriver controller auto-learn sequence for Hall Angle." },
    { id: "c8", cat: "CONTROLLER & ELEC", title: "Dualtron EY3 Display P-Settings", query: "Electronics Diagnostic: Full mapping of Dualtron EY3 P-settings (P0-P9, PA-PD) for ABS, start mode, and battery calibration." },
    { id: "c9", cat: "CONTROLLER & ELEC", title: "Bafang BBSHD Error 21 Triage", query: "Electronics Diagnostic: Resolving Bafang Error 21 (Speed Sensor Failure). Magnet alignment and reed switch continuity tests." },
    { id: "c10", cat: "CONTROLLER & ELEC", title: "Controller Thermal Pasting Guide", query: "Electronics Diagnostic: Proper protocol for removing old thermal pads and applying high-conductivity thermal paste to controller MOSFETs." },
    { id: "f1", cat: "FIRMWARE & TUNING", title: "VESC Field Weakening Tuning", query: "Firmware Tuning: Step by step guide to configuring Field Weakening in the VESC tool. Recommended max D-Axis current parameters." },
    { id: "f2", cat: "FIRMWARE & TUNING", title: "Segway GT2 / Max SHFW Flashing", query: "Firmware Tuning: Protocol for flashing custom ScooterHacking Firmware (SHFW) onto Segway Ninebot controllers using ST-Link." },
    { id: "f3", cat: "FIRMWARE & TUNING", title: "FarDriver ND72680 Soft Start Tuning", query: "Firmware Tuning: How to smooth out the initial throttle curve (Soft Start) and adjust phase current ramps in the FarDriver app." },
    { id: "k1", cat: "CHASSIS & BRAKES", title: "Hydraulic Bleed & Mushy Lever Feel", query: "Chassis Diagnostic: Inspecting hydraulic brake lines for air pockets, mineral oil contamination, and complete bleed steps." },
    { id: "k2", cat: "CHASSIS & BRAKES", title: "Rotor Glaze & Squealing", query: "Chassis Diagnostic: Cleaning contaminated disc brake rotors with 99% Isopropyl alcohol and re-bedding glazed pads." },
    { id: "k3", cat: "CHASSIS & BRAKES", title: "Caliper Light-Gap Centering", query: "Chassis Diagnostic: Eliminating brake pad rotor rub using caliper alignment and correct bracket torque specs." },
    { id: "k4", cat: "CHASSIS & BRAKES", title: "Headset Slop & Steering Play", query: "Chassis Diagnostic: Differentiating stanchion bushing play from loose headset compression ring star nut preload." },
    { id: "k5", cat: "CHASSIS & BRAKES", title: "Spoke Tension Balancing & Trueing", query: "Chassis Diagnostic: Balancing drive-side spoke tension (110-130 kgf) and lateral/radial trueing on hub motors." },
    { id: "k6", cat: "CHASSIS & BRAKES", title: "Magura MT5/MT7 Vacuum Bleeding", query: "Chassis Diagnostic: Specific gravity vacuum bleed protocol using royal blood for Magura MT series calipers." },
    { id: "k7", cat: "CHASSIS & BRAKES", title: "Pneumatic Tire Bead Seating", query: "Chassis Diagnostic: Tubeless pneumatic tire bead will not sit flush on the rim. Solutions involving soapy water and compressor bursts." },
    { id: "k8", cat: "CHASSIS & BRAKES", title: "Nami Burn-E Stem Wobble Fix", query: "Chassis Diagnostic: Protocol for tightening the Nami Burn-E folding mechanism and securing the heavy-duty stem latch." },
    { id: "k9", cat: "CHASSIS & BRAKES", title: "Suspension Sag Configuration", query: "Chassis Diagnostic: How to properly measure and adjust pre-load rings on coil shocks to hit 25-30% optimal suspension sag." },
    { id: "s1", cat: "SAFETY & PRE-RIDE", title: "10-Point Pre-Ride Hardware Protocol", query: "Safety Protocol: Generate a comprehensive 10-point Pre-Ride High-Power EV/E-Bike Safety Checklist covering tire pressure, axle nut torque, brake bite, and harness strain relief." },
    { id: "s2", cat: "SAFETY & PRE-RIDE", title: "Axle Torque Spec Verification", query: "Safety Protocol: Standard fastener torque specifications for front/rear motor axle nuts, suspension pivots, and caliper adapters." },
    { id: "s3", cat: "SAFETY & PRE-RIDE", title: "Emergency Trailside Breakout Protocol", query: "Safety Protocol: Step-by-step triage guide for emergency roadside failure: phase separation, dead battery bypass, and roll-out check." },
    { id: "s4", cat: "SAFETY & PRE-RIDE", title: "Rain Ingress & Corrosion Triage", query: "Safety Protocol: Water ingress triage: drying protocol, dielectric grease sealing, and controller conformal coating check." },
    { id: "s5", cat: "SAFETY & PRE-RIDE", title: "Lithium Fire Suppression Plan", query: "Safety Protocol: Step-by-step emergency response for a thermal runaway event. Class D extinguishers vs water deluge." }
  ];

  const errorCodeDb = [
    { code: "E01 / 01H", meaning: "Over-Current Protection", fix: "Inspect motor phase wires for short circuits or blown controller MOSFETs." },
    { code: "E02 / 02H", meaning: "Brake Cut-off Switch Latched", fix: "Verify brake lever return springs; test magnetic cutoff reed switch continuity." },
    { code: "E06 / 06H", meaning: "Battery Low-Voltage Cutoff (LVC)", fix: "Charge pack. Measure resting voltage; verify individual series P-group balance." },
    { code: "E07 / 07H", meaning: "Motor Phase Line Failure", fix: "Check motor 9-pin waterproof harness or 3 high-current bullet connectors for heat damage." },
    { code: "E08 / 08H", meaning: "Throttle Hall Sensor Error", fix: "Inspect throttle return spring; test green signal line for voltage outside 0.8V–4.2V range." },
    { code: "E09 / 09H", meaning: "Controller Gate Driver Error", fix: "MOSFET gate drive failure or internal short. Test controller logic rails with DMM." },
    { code: "E10 / 30H", meaning: "Display/Controller UART Link Lost", fix: "Inspect 5-pin harness between display and controller for pinched or severed data leads." },
    { code: "Bafang E04", meaning: "Throttle Not Reset at Startup", fix: "Throttle engaged while powering on. Release throttle and reboot display." },
    { code: "Bafang E07", meaning: "High Voltage Protection", fix: "Battery voltage exceeds controller maximum rating. Check battery chemistry match." },
    { code: "Bafang E08", meaning: "Motor Hall Sensor Fault", fix: "Motor core hall sensor failure. Requires mid-drive teardown to replace 41F sensors." },
    { code: "Bafang E12", meaning: "Current Sensor Abnormal", fix: "Controller phase current measurement failure. Usually requires controller replacement." },
    { code: "Bafang E21", meaning: "Speed Sensor Failure", fix: "Align wheel magnet to chainstay speed sensor. Ensure gap is under 3mm and LED blinks on pass." },
    { code: "Bafang E30", meaning: "Communications Failure", fix: "Check main wiring harness for pinches. Re-seat green 5-pin display connector." },
    { code: "Segway E10", meaning: "Dashboard / BLE Communication", fix: "Communication between dashboard and ESC failed. Check stem cable." },
    { code: "Segway E14", meaning: "Throttle Hall Error", fix: "Throttle trigger magnet loose or hall sensor wire severed. Replace throttle unit." },
    { code: "Segway E15", meaning: "Brake Hall Error", fix: "Electronic brake lever switch stuck. Flick lever or replace hall assembly." },
    { code: "Segway E18", meaning: "Motor Hall Sensor", fix: "Motor phase/hall detection failed. Check motor harness connector." },
    { code: "Segway E21", meaning: "BMS Communication Error", fix: "ESC cannot talk to BMS. Check 4-pin data cable entering the battery pack." },
    { code: "Segway E39", meaning: "Battery Temperature Abnormal", fix: "Battery pack exceeded safe thermal limits. Let cool down." },
    { code: "FAULT_DRV", meaning: "Gate Driver Error", fix: "The DRV chip detected an anomaly. Usually caused by a shorted MOSFET or blown DRV8301/8323 chip." },
    { code: "FAULT_OVER_VOLTAGE", meaning: "Voltage Exceeded Limit", fix: "Regen braking spiked voltage above max setting. Lower regen current or raise max voltage limit." },
    { code: "FAULT_UNDER_VOLTAGE", meaning: "Battery Sag Drop", fix: "Voltage dropped below battery cutoff threshold under load." },
    { code: "FAULT_OVER_TEMP_FET", meaning: "MOSFET Overheating", fix: "Controller exceeded thermal limits. Lower continuous motor current or improve heatsink cooling." },
    { code: "Dualtron E1", meaning: "Motor Hall Fault", fix: "Motor hall sensor cable damaged. Requires motor teardown to replace sensors." },
    { code: "Dualtron E2", meaning: "System Overcurrent", fix: "Shorted phase wires or blown controller FETs. Check phase-to-ground resistance." },
    { code: "Kaabo Err 01", meaning: "Display Communication", fix: "TFT display cannot reach the base controllers. Check main stem harness plug." },
    { code: "Nami Err 06", meaning: "Throttle Fault", fix: "Nami thumb throttle stuck or shorted. Replace throttle assembly." },
    { code: "Sur-Ron 0x40", meaning: "Controller Over-Temp", fix: "Controller exceeded 95C. Allow to cool. Check airflow and fins for mud." },
    { code: "Sur-Ron 0x43", meaning: "Motor Over-Temp", fix: "Motor thermistor detecting > 110C. Avoid steep prolonged hill climbs." },
    { code: "Sur-Ron 0x11", meaning: "Throttle Start Error", fix: "Throttle was not at zero position when ignition was engaged." }
  ];

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsPresetDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const filteredPresets = useMemo(() => {
    return quickDiagnosticsList.filter(p => {
      const matchesCategory = activeCategory === "ALL" || p.cat === activeCategory;
      const matchesSearch = p.title.toLowerCase().includes(presetSearch.toLowerCase()) || 
                            p.query.toLowerCase().includes(presetSearch.toLowerCase());
      return matchesCategory && matchesSearch;
    });
  }, [activeCategory, presetSearch]);

  const filteredHistory = useMemo(() => {
    return (scanHistory || []).filter((item: any) => {
      const searchMatch = (item.title || "").toLowerCase().includes(historySearch.toLowerCase()) || (item.timestamp || "").toLowerCase().includes(historySearch.toLowerCase());
      if (logFilter === "ALL") return searchMatch;
      return searchMatch && (item.title || "").toUpperCase().includes(logFilter);
    });
  }, [scanHistory, historySearch, logFilter]);

  const parseDynamicChecklist = (text: string): Array<{ text: string; completed: boolean }> => {
    const lines = text.split("\n"); 
    const checklist: Array<{ text: string; completed: boolean }> = [];
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

  const convertUrlToBase64 = async (url: string): Promise<string> => {
    if (url.startsWith("data:")) {
      return url.split(",")[1];
    }
    const response = await fetch(url);
    const blob = await response.blob();
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onloadend = () => {
        const res = reader.result as string;
        resolve(res.split(",")[1]);
      };
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  };

  // --- WORKSHOP CHIEF PERSONA COMPUTER VISION ANALYZER ---
  const analyzeImage = async (overridePrompt?: string) => {
    const activeQuery = (overridePrompt || question).trim();
    if (!activeQuery && !previewUrl) {
      if (setErrorMsg) setErrorMsg("Please submit an inquiry, upload a photo, or choose a diagnostic preset.");
      return;
    }
    
    if (triggerHaptic) triggerHaptic("HEAVY"); 
    if (setIsAnalyzing) setIsAnalyzing(true); 
    if (setErrorMsg) setErrorMsg(null); 
    setQuestion("");
    
    setDetectedAnomalies([]);
    setWiringPinoutMap(null);
    setSelectedAnomaly(null);

    const timestampStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userMsg: Message = { 
      id: `USER-${Date.now()}`, 
      role: "user", 
      text: activeQuery || "Optical Hardware Inspection Request", 
      timestamp: timestampStr, 
      image: previewUrl 
    };
    
    const updatedMessages = [...messages, userMsg]; 
    setMessages(updatedMessages);

    try {
      const envKey = typeof import.meta !== "undefined" && (import.meta as any).env ? (import.meta as any).env.VITE_GEMINI_API_KEY : "";
      const geminiKey = envKey || (typeof getGeminiApiKey === "function" ? getGeminiApiKey() : "");
      
      if (!geminiKey) throw new Error("Missing Gemini API Key. Please verify VITE_GEMINI_API_KEY.");

      const targetModel = aiModelVersion || "gemini-3.1-flash-lite";

      const systemInstruction = `You are a seasoned, veteran workshop chief and PEV master mechanic with decades of hands-on experience in high-power micro-mobility, e-bikes, and PEV powertrains. Talk straight, respect the hardware, and speak like an experienced workshop chief assisting operator ${callsign || 'Rider'}.
Active Machine Fleet: ${userFleet || 'High-Power Electric PEV'}. Measurement Units: ${unitSystem || 'Imperial'}. Detail Level: ${aiDetailLevel || 'Standard'}.
ACTIVE OPTICAL SCAN FILTER: ${inspectionScope}.

CRITICAL COMPUTER VISION & AR INSTRUCTIONS:
1. Examine the visual image with extreme mechanical scrutiny. Detect scorched wires, blown controller MOSFETs, rotor scoring/glazing, worn tire tread, loose axle nuts, or cracked folding linkages.
2. Deliver verified technical diagnostics with torque specs (Nm) and multimeter test voltages in a grounded workshop mechanic tone.
3. ALWAYS INCLUDE AN ACTIONABLE STEP-BY-STEP CHECKLIST:
- [ ] Step 1: Specific test or fix
4. AR BOUNDING BOX ANOMALIES: If you detect physical components or damage, output normalized bounding box coordinates (0.0 to 1.0) inside a trailing JSON code block:
\`\`\`json
{
  "anomalies": [
    {
      "label": "Scored Brake Rotor",
      "ymin": 0.20,
      "xmin": 0.35,
      "ymax": 0.60,
      "xmax": 0.75,
      "color": "#f43f5e",
      "severity": "CRITICAL",
      "wearPct": 85,
      "componentType": "Brake Rotor"
    }
  ]
}
\`\`\`
5. WIRING AUTO-MAPPER: If inspecting cables or controller leads, output a wiring table in the same JSON block:
\`\`\`json
{
  "wiringMap": {
    "Yellow Lead": "Phase A",
    "Red Wire": "+5V Hall VCC",
    "Black Wire": "GND"
  }
}
\`\`\``;

      const currentParts: any[] = [{ text: activeQuery || `Optical Hardware Inspection Sweep [SCOPE: ${inspectionScope}]` }];

      if (previewUrl) {
        const base64Data = await convertUrlToBase64(previewUrl);
        if (base64Data) {
          currentParts.push({ inlineData: { mimeType: "image/jpeg", data: base64Data } });
        }
      }

      if (secondaryPreviewUrl) {
        const secondaryBase64 = await convertUrlToBase64(secondaryPreviewUrl);
        if (secondaryBase64) {
          currentParts.push({ inlineData: { mimeType: "image/jpeg", data: secondaryBase64 } });
        }
      }

      const contentsPayload = [{ role: "user", parts: currentParts }];

      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${targetModel}:generateContent?key=${geminiKey}`, {
        method: "POST", 
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ 
          system_instruction: { parts: [{ text: systemInstruction }] }, 
          contents: contentsPayload, 
          generationConfig: { temperature: 0.2 } 
        })
      });

      const data = await response.json();
      if (!response.ok) throw new Error(data.error?.message || `API Status Error ${response.status}`);
      
      let rawAiText = data?.candidates?.[0]?.content?.parts?.[0]?.text || "Diagnostic analysis complete.";

      // Extract JSON Anomaly & Wiring Data
      const jsonMatch = rawAiText.match(/```json\n([\s\S]*?)\n```/g);
      if (jsonMatch) {
        jsonMatch.forEach((block: string) => {
          try {
            const parsed = JSON.parse(block.replace(/```json|```/g, "").trim());
            if (parsed.anomalies && Array.isArray(parsed.anomalies)) {
              setDetectedAnomalies(parsed.anomalies);
              if (parsed.anomalies.length > 0) setSelectedAnomaly(parsed.anomalies[0]);
            }
            if (parsed.wiringMap) {
              setWiringPinoutMap(parsed.wiringMap);
            }
            rawAiText = rawAiText.replace(block, "").trim(); 
          } catch (e) {}
        });
      }

      const aiMsg: Message = { 
        id: `AI-${Date.now()}`, 
        role: "ai", 
        text: rawAiText, 
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }), 
        checklist: parseDynamicChecklist(rawAiText) 
      };
      
      setMessages([...updatedMessages, aiMsg]);
      
      if (detectedAnomalies.length > 0 && triggerHaptic) triggerHaptic("EXTREME");
      if (autoReadAloud && handleTTS) handleTTS(rawAiText, aiMsg.id);
      
    } catch (err: any) { 
      const errText = `[VISION FAULT]: ${err.message || String(err)}`;
      if (setErrorMsg) setErrorMsg(errText); 
      setMessages([...updatedMessages, { id: `ERR-${Date.now()}`, role: "system", text: errText }]);
    } finally { 
      if (setIsAnalyzing) setIsAnalyzing(false); 
      if (autoClearPhoto && detectedAnomalies.length === 0 && clearImage) {
        clearImage();
        setSecondaryPreviewUrl(null);
      }
    }
  };

  const handleSelectPreset = (presetQuery: string) => {
    setIsPresetDropdownOpen(false);
    setQuestion(""); 
    if (clearImage) clearImage();
    setSecondaryPreviewUrl(null);
    analyzeImage(presetQuery);
  };

  const exportLogsToJson = () => {
    if (scanHistory.length === 0) return;
    const dateStr = new Date().toISOString().slice(0, 10);
    const blob = new Blob([JSON.stringify(scanHistory, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `WorkshopChief_Backup_${dateStr}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const filteredAnomalies = useMemo(() => {
    if (severityFilter === "ALL") return detectedAnomalies;
    return detectedAnomalies.filter(a => a.severity === severityFilter);
  }, [detectedAnomalies, severityFilter]);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 font-sans relative">
      
      {/* ================= LEFT COLUMN: OPTICAL INPUTS & GARAGE LOG ================= */}
      <div className="lg:col-span-1 space-y-3 sm:space-y-4">
        
        {/* OPTICAL RECOGNITION PANEL */}
        <div className="bg-[#0a0a0f] p-3 sm:p-4 rounded-3xl border border-white/10 shadow-xl space-y-3 backdrop-blur-xl">
          <div className="flex items-center justify-between border-b border-white/10 pb-2">
            <h3 className="text-zinc-400 font-black text-[9px] uppercase tracking-widest flex items-center gap-1.5">
              <Camera className={`w-3.5 h-3.5 ${t.text}`} /> Optical AR Viewport
            </h3>
            <div className="flex items-center gap-1">
              {detectedAnomalies.length > 0 && (
                <button
                  type="button"
                  onClick={() => setShowThermalOverlay(!showThermalOverlay)}
                  className={`text-[8px] font-mono px-2 py-1 rounded-lg border uppercase cursor-pointer transition-all ${
                    showThermalOverlay 
                      ? "bg-rose-600 text-white border-rose-500 shadow-md animate-pulse" 
                      : "bg-white/5 text-zinc-400 border-white/10 hover:text-white"
                  }`}
                  title="Toggle Simulated FLIR Heatmap"
                >
                  <Flame className="w-2.5 h-2.5 inline mr-1" /> FLIR
                </button>
              )}
              {previewUrl && (
                <button 
                  type="button"
                  onClick={() => setIsZoomed(true)}
                  className="text-zinc-400 hover:text-white text-[8px] flex items-center gap-1 font-mono uppercase cursor-pointer min-h-[30px] justify-center px-1"
                >
                  <ZoomIn className="w-3 h-3" />
                </button>
              )}
            </div>
          </div>

          {/* AI OPTICAL INSPECTION MODE CHIPS */}
          <div className="flex gap-1 overflow-x-auto custom-scrollbar pb-1">
            {[
              { id: "ALL", label: "General" },
              { id: "WIRING", label: "Harness" },
              { id: "BRAKES", label: "Brakes" },
              { id: "TIRES", label: "Tires" },
              { id: "BOLTS", label: "Torque" }
            ].map(m => (
              <button
                key={m.id}
                type="button"
                onClick={() => {
                  setInspectionScope(m.id as any);
                  if (triggerHaptic) triggerHaptic("LIGHT");
                }}
                className={`px-2 py-1 rounded-md text-[7px] font-mono font-bold uppercase transition-all whitespace-nowrap cursor-pointer ${
                  inspectionScope === m.id ? "bg-white text-black font-black" : "bg-white/5 text-zinc-400 hover:text-white"
                }`}
              >
                {m.label}
              </button>
            ))}
          </div>

          {/* ANOMALY SEVERITY FILTER CHIPS */}
          {detectedAnomalies.length > 0 && (
            <div className="flex gap-1 items-center bg-black/60 p-1.5 rounded-xl border border-white/5 text-[7px] font-mono">
              <span className="text-zinc-500 font-bold uppercase mr-1">Severity:</span>
              {(["ALL", "CRITICAL", "WARNING"] as const).map(sev => (
                <button
                  key={sev}
                  type="button"
                  onClick={() => setSeverityFilter(sev)}
                  className={`px-2 py-0.5 rounded uppercase font-bold cursor-pointer transition-all ${
                    severityFilter === sev ? "bg-rose-600 text-white" : "text-zinc-400 hover:text-white"
                  }`}
                >
                  {sev}
                </button>
              ))}
            </div>
          )}
          
          <input type="file" accept="image/*" ref={galleryInputRef} onChange={handleFileChange} className="hidden" />
          <input type="file" accept="image/*" capture="environment" ref={cameraInputRef} onChange={handleFileChange} className="hidden" />
          <input type="file" accept="image/*" ref={secondaryGalleryInputRef} onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) setSecondaryPreviewUrl(URL.createObjectURL(f));
          }} className="hidden" />

          {/* MEDIA SELECTION & DUAL-ANGLE VIEWFINDER */}
          {!previewUrl ? (
            <div className="grid grid-cols-2 gap-2">
              <button 
                type="button"
                onClick={() => galleryInputRef.current?.click()}
                className="border border-white/10 hover:border-white/20 rounded-xl p-3 min-h-[48px] text-center bg-black/40 transition-all flex flex-col items-center justify-center gap-1 group cursor-pointer shadow-inner active:scale-95"
              >
                <ImagePlus className="w-4 h-4 text-zinc-400 group-hover:text-white transition-colors" />
                <span className="text-[7px] font-black uppercase tracking-widest text-zinc-300">Upload Photo</span>
              </button>
              <button 
                type="button"
                onClick={() => cameraInputRef.current?.click()}
                className="border border-white/10 hover:border-white/20 rounded-xl p-3 min-h-[48px] text-center bg-black/40 transition-all flex flex-col items-center justify-center gap-1 group cursor-pointer shadow-inner active:scale-95"
              >
                <Camera className="w-4 h-4 text-zinc-400 group-hover:text-white transition-colors" />
                <span className="text-[7px] font-black uppercase tracking-widest text-zinc-300">Open Camera</span>
              </button>
            </div>
          ) : (
            <div className={`relative rounded-xl overflow-hidden border ${filteredAnomalies.length > 0 ? 'border-rose-500 shadow-[0_0_20px_rgba(244,63,94,0.3)]' : t.borderSubtle} bg-zinc-950 shadow-inner group space-y-1`}>
              
              <div className="grid grid-cols-2 gap-1 bg-black p-1">
                {/* Primary Photo */}
                <div className="relative w-full h-32 bg-black flex items-center justify-center overflow-hidden rounded-lg">
                  <img src={previewUrl} alt="Primary Target" onClick={() => setIsZoomed(true)} className="w-full h-full object-contain cursor-zoom-in" />
                  <span className="absolute bottom-1 left-1 bg-black/80 text-[6px] font-mono text-zinc-400 px-1 rounded">PRIMARY ANGLE</span>
                </div>

                {/* Secondary Multi-Angle Comparison Photo */}
                <div className="relative w-full h-32 bg-black flex items-center justify-center overflow-hidden rounded-lg border border-white/10">
                  {secondaryPreviewUrl ? (
                    <>
                      <img src={secondaryPreviewUrl} alt="Secondary Angle" className="w-full h-full object-contain" />
                      <button type="button" onClick={() => setSecondaryPreviewUrl(null)} className="absolute top-1 right-1 bg-black/80 text-rose-400 p-1 rounded-md"><X className="w-3 h-3"/></button>
                      <span className="absolute bottom-1 left-1 bg-black/80 text-[6px] font-mono text-cyan-400 px-1 rounded">COMPARE ANGLE</span>
                    </>
                  ) : (
                    <button type="button" onClick={() => secondaryGalleryInputRef.current?.click()} className="flex flex-col items-center justify-center text-zinc-500 hover:text-white cursor-pointer w-full h-full p-2 text-center">
                      <GitCompare className="w-4 h-4 mb-1 text-cyan-400" />
                      <span className="text-[7px] font-bold uppercase">+ Add Compare Angle</span>
                    </button>
                  )}
                </div>
              </div>

              {/* THERMAL SIMULATION HEATMAP GRADIENT */}
              {showThermalOverlay && filteredAnomalies.length > 0 && (
                <div className="absolute inset-0 pointer-events-none mix-blend-screen opacity-75 bg-gradient-to-tr from-blue-900/40 via-amber-500/30 to-rose-600/50 animate-pulse" />
              )}

              {/* AR BOUNDING BOXES & RETICLES */}
              {filteredAnomalies.map((box, i) => (
                <div 
                  key={i} 
                  onClick={() => setSelectedAnomaly(box)}
                  className="absolute border-2 transition-all duration-300 cursor-pointer pointer-events-auto z-20"
                  style={{
                    top: `${box.ymin * 100}%`,
                    left: `${box.xmin * 100}%`,
                    height: `${(box.ymax - box.ymin) * 100}%`,
                    width: `${(box.xmax - box.xmin) * 100}%`,
                    borderColor: box.color || "#f43f5e",
                    boxShadow: `0 0 14px ${box.color || "#f43f5e"}90, inset 0 0 8px ${box.color || "#f43f5e"}40`
                  }}
                >
                  {showArReticles && (
                    <>
                      <div className="absolute -top-1 -left-1 w-2 h-2 border-t-2 border-l-2 border-white" />
                      <div className="absolute -top-1 -right-1 w-2 h-2 border-t-2 border-r-2 border-white" />
                      <div className="absolute -bottom-1 -left-1 w-2 h-2 border-b-2 border-l-2 border-white" />
                      <div className="absolute -bottom-1 -right-1 w-2 h-2 border-b-2 border-r-2 border-white" />
                    </>
                  )}
                  <div className="absolute -top-5 left-0 flex items-center gap-1 whitespace-nowrap z-30">
                    <span className="bg-black/90 px-1.5 py-0.5 text-[6px] font-mono font-black uppercase text-white border border-white/20 rounded shadow-md">
                      {box.label}
                    </span>
                    {box.wearPct !== undefined && (
                      <span className="bg-rose-950/90 text-rose-300 px-1 py-0.5 text-[6px] font-mono font-black rounded border border-rose-600/40">
                        {box.wearPct}%
                      </span>
                    )}
                  </div>
                </div>
              ))}

              {/* Close Button */}
              <button 
                type="button" 
                onClick={() => { clearImage(); setSecondaryPreviewUrl(null); }} 
                className="absolute top-2 right-2 bg-black/80 hover:bg-rose-950 text-zinc-300 hover:text-rose-400 p-1.5 rounded-xl transition-all cursor-pointer shadow-lg min-h-[36px] min-w-[36px] flex items-center justify-center z-30"
              >
                <XCircle className="w-4 h-4" />
              </button>
              
              <div className={`absolute bottom-2 left-2 bg-black/80 px-2 py-1 rounded-lg text-[7px] ${filteredAnomalies.length > 0 ? 'text-rose-400' : t.text} font-mono font-black flex items-center gap-1 shadow-md z-30`}>
                {filteredAnomalies.length > 0 ? (
                  <><ShieldAlert className="w-2.5 h-2.5 animate-pulse" /> {filteredAnomalies.length} FAULT(S) LOCKED</>
                ) : (
                  <><CheckCircle className="w-2.5 h-2.5" /> FRAME LOCKED</>
                )}
              </div>
            </div>
          )}

          {/* INTERACTIVE FAULT INSPECTOR CARD */}
          {selectedAnomaly && (
            <div className="bg-black/70 border border-rose-500/40 p-3 rounded-2xl space-y-1.5 animate-fadeIn font-mono text-[9px] shadow-inner">
              <div className="flex justify-between items-center border-b border-white/10 pb-1">
                <span className="font-black text-rose-400 uppercase flex items-center gap-1">
                  <ShieldAlert className="w-3 h-3 text-rose-500" /> {selectedAnomaly.label}
                </span>
                <span className="text-[7px] bg-rose-950 px-1.5 py-0.5 rounded text-rose-300 font-bold">
                  {selectedAnomaly.severity || "CRITICAL"}
                </span>
              </div>
              <div className="flex justify-between text-zinc-400 text-[8px]">
                <span>Component: <strong className="text-white">{selectedAnomaly.componentType || "Structural Hardware"}</strong></span>
                {selectedAnomaly.wearPct !== undefined && <span>Wear Load: <strong className="text-amber-400">{selectedAnomaly.wearPct}%</strong></span>}
              </div>
              <button
                type="button"
                onClick={() => {
                  analyzeImage(`Provide an exact multimeter rail test and torque fix protocol for: ${selectedAnomaly.label}`);
                }}
                className="w-full mt-1 bg-white/10 hover:bg-white/20 text-white rounded-lg py-1.5 text-[8px] font-black uppercase tracking-wider cursor-pointer transition-all"
              >
                Run Component Fix Protocol →
              </button>
            </div>
          )}

          {/* DYNAMIC WIRING MAPPER OUTPUT */}
          {wiringPinoutMap && (
            <div className="bg-black/60 border border-amber-500/30 p-3 rounded-xl mt-2 animate-fadeIn shadow-inner">
              <h4 className="text-[8px] font-black text-amber-400 uppercase tracking-widest mb-2 flex items-center gap-1.5"><Layers className="w-3 h-3"/> Auto-Mapped Pinout</h4>
              <div className="grid grid-cols-2 gap-2 text-[9px] font-mono">
                {Object.entries(wiringPinoutMap).map(([color, purpose], idx) => (
                  <div key={idx} className="flex flex-col justify-center bg-black/80 p-2 rounded-lg border border-white/5">
                    <span className="text-zinc-500 uppercase text-[7px]">{color}</span>
                    <span className="text-white font-bold leading-tight">{purpose}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* QUICK HARDWARE UTILITY TOOLBAR */}
          <div className="grid grid-cols-2 gap-2 pt-1 border-t border-white/5">
            <button 
              type="button"
              onClick={() => setShowErrorCodeModal(true)}
              className="px-2 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-[8px] font-mono font-bold uppercase tracking-wider text-zinc-300 flex items-center justify-center gap-1.5 transition-all cursor-pointer min-h-[36px]"
            >
              <Activity className="w-3 h-3 text-cyan-400" /> Error Codes
            </button>
            <button 
              type="button"
              onClick={() => setShowWiringModal(true)}
              className="px-2 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-[8px] font-mono font-bold uppercase tracking-wider text-zinc-300 flex items-center justify-center gap-1.5 transition-all cursor-pointer min-h-[36px]"
            >
              <Cpu className="w-3 h-3 text-emerald-400" /> Pinout Guide
            </button>
          </div>
        </div>

        {/* GARAGE LOG LEDGER WITH SEARCH */}
        <div className="bg-[#0a0a0f] p-3 sm:p-4 rounded-3xl border border-white/10 shadow-xl flex flex-col h-[360px] backdrop-blur-xl">
          <div className="flex items-center justify-between border-b border-white/10 pb-2 mb-2">
            <h3 className="text-zinc-400 font-black text-[9px] uppercase tracking-widest flex items-center gap-1.5">
              <History className="w-3.5 h-3.5" /> Garage Log Ledger
            </h3>
            <div className="flex items-center gap-1">
              {scanHistory.length > 0 && (
                <>
                  <button 
                    type="button" 
                    onClick={exportLogsToJson}
                    title="Export Logs as JSON Database" 
                    className="text-zinc-400 hover:text-white p-1 rounded-md hover:bg-white/5 transition-all cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center"
                  >
                    <Database className="w-3.5 h-3.5" />
                  </button>
                  <button 
                    type="button" 
                    onClick={clearAllHistory} 
                    title="Wipe Ledger"
                    className="text-zinc-500 hover:text-rose-400 p-1 rounded-md hover:bg-white/5 transition-all cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </>
              )}
            </div>
          </div>

          {scanHistory.length > 2 && (
            <div className="space-y-2 mb-2">
              <div className="relative">
                <Search className="w-3 h-3 absolute left-2.5 top-3 text-zinc-500" />
                <input
                  type="text"
                  value={historySearch}
                  onChange={(e) => setHistorySearch(e.target.value)}
                  placeholder="Search history records..."
                  className="w-full bg-black/40 border border-white/10 rounded-xl pl-8 pr-2 py-2 text-[9px] text-zinc-200 placeholder-zinc-600 focus:outline-none focus:border-white/20 font-mono min-h-[36px]"
                />
              </div>
              <div className="flex gap-1 overflow-x-auto custom-scrollbar pb-1">
                {["ALL", "POWERTRAIN", "BATTERY", "BRAKES"].map(f => (
                  <button 
                    key={f} 
                    type="button" 
                    onClick={() => setLogFilter(f)} 
                    className={`px-2 py-1 rounded-md text-[7px] font-bold uppercase transition-all min-h-[28px] cursor-pointer ${logFilter === f ? "bg-white/20 text-white" : "bg-white/5 text-zinc-400"}`}
                  >
                    {f}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="flex-1 overflow-y-auto pr-1 space-y-1.5 custom-scrollbar">
            {filteredHistory.length === 0 ? (
              <div className="text-center py-10 text-zinc-600">
                <Wrench className="w-5 h-5 mx-auto mb-2 opacity-20" />
                <p className="text-[8px] uppercase font-black tracking-widest">
                  {historySearch ? "No Matching Records" : "No Logs Recorded"}
                </p>
              </div>
            ) : (
              filteredHistory.map((item: any) => (
                <div 
                  key={item.id} 
                  onClick={() => loadPreviousSession(item)}
                  className={`p-2.5 min-h-[44px] bg-black/50 border rounded-2xl cursor-pointer transition-all shadow-inner flex flex-col justify-center ${activeSessionId === item.id ? `${t.borderSubtle}${t.dim}` : "border-white/5 hover:border-white/10 text-zinc-400"}`}
                >
                  <div className="flex justify-between items-center mb-0.5">
                    <span className="text-[8px] font-mono font-bold text-zinc-500">{item.timestamp}</span>
                    {activeSessionId === item.id && <span className={`w-1.5 h-1.5 rounded-full ${t.bg} animate-pulse`} />}
                  </div>
                  <p className="text-[9px] font-black text-zinc-200 truncate tracking-wide uppercase">{item.title}</p>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* ================= RIGHT COLUMN: MAIN CHAT & AUTO-SEARCH SUITE ================= */}
      <div className="lg:col-span-3 flex flex-col bg-[#0a0a0f] border border-white/10 rounded-3xl shadow-xl h-[660px] overflow-hidden backdrop-blur-xl relative">
        
        {/* HEADER BAR WITH AUTO-SEARCH DROPDOWN */}
        <div className="bg-black/80 px-3 sm:px-4 py-2.5 border-b border-white/10 flex flex-wrap items-center justify-between gap-2 shrink-0 z-30">
          
          <div className="relative" ref={dropdownRef}>
            <button
              type="button"
              onClick={() => setIsPresetDropdownOpen(!isPresetDropdownOpen)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-[9px] font-mono font-black uppercase tracking-wider transition-all border shadow-lg cursor-pointer min-h-[36px] ${
                isPresetDropdownOpen 
                  ? `${t.bg} text-black border-transparent` 
                  : "bg-white/5 hover:bg-white/10 border-white/15 text-zinc-200"
              }`}
            >
              <Sparkles className={`w-3.5 h-3.5 ${isPresetDropdownOpen ? "text-black" : "text-amber-400"}`} />
              <span>Workshop Protocols ({quickDiagnosticsList.length})</span>
              {isPresetDropdownOpen ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
            </button>

            {isPresetDropdownOpen && (
              <div className="absolute top-full left-0 mt-2 w-[300px] sm:w-[480px] bg-[#0f1117] border border-white/20 rounded-3xl shadow-2xl p-3 z-50 backdrop-blur-2xl animate-in fade-in slide-in-from-top-2 duration-150">
                <div className="relative mb-2">
                  <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-zinc-400" />
                  <input
                    type="text"
                    value={presetSearch}
                    onChange={(e) => setPresetSearch(e.target.value)}
                    placeholder="Search workshop protocols..."
                    className="w-full bg-black/60 border border-white/15 rounded-xl pl-9 pr-3 py-2 text-xs text-zinc-100 placeholder-zinc-500 focus:outline-none focus:border-amber-400/50 font-mono min-h-[36px]"
                    autoFocus
                  />
                </div>

                <div className="flex gap-1 overflow-x-auto pb-1.5 mb-1.5 custom-scrollbar">
                  {diagnosticCategories.map(cat => (
                    <button
                      key={cat}
                      type="button"
                      onClick={() => setActiveCategory(cat)}
                      className={`px-2 py-1 rounded-md text-[7px] font-mono font-bold uppercase shrink-0 transition-all cursor-pointer min-h-[28px] ${
                        activeCategory === cat ? `${t.bg} text-black` : "bg-white/5 text-zinc-400 hover:text-white"
                      }`}
                    >
                      {cat}
                    </button>
                  ))}
                </div>

                <div className="max-h-[280px] overflow-y-auto space-y-1 pr-1 custom-scrollbar">
                  {filteredPresets.length === 0 ? (
                    <div className="py-8 text-center text-zinc-500 text-[10px] font-mono">
                      No matching diagnostic protocols found.
                    </div>
                  ) : (
                    filteredPresets.map((preset) => (
                      <button
                        key={preset.id}
                        type="button"
                        onClick={() => handleSelectPreset(preset.query)}
                        className="w-full text-left p-2.5 rounded-xl bg-black/40 hover:bg-white/10 border border-white/5 hover:border-white/20 transition-all group flex items-start justify-between gap-1.5 cursor-pointer min-h-[44px]"
                      >
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="text-[6px] font-mono uppercase px-1 py-0.5 rounded bg-white/10 text-zinc-400">
                              {preset.cat}
                            </span>
                            <span className="text-[9px] font-bold text-zinc-200 group-hover:text-amber-400 transition-colors line-clamp-1">
                              {preset.title}
                            </span>
                          </div>
                          <p className="text-[8px] text-zinc-500 line-clamp-1 mt-0.5 font-sans">
                            {preset.query}
                          </p>
                        </div>
                        <Zap className="w-3.5 h-3.5 text-zinc-600 group-hover:text-amber-400 shrink-0 mt-0.5 transition-colors" />
                      </button>
                    ))
                  )}
                </div>

                <div className="mt-2 pt-2 border-t border-white/10 flex items-center justify-between text-[8px] text-zinc-500 font-mono">
                  <span>⚡ Instant trigger without stale preview crossover</span>
                  <button 
                    type="button"
                    onClick={() => handleSelectPreset("Generate a 10-Point Pre-Ride PEV Safety & Torque Checklist.")}
                    className="text-amber-400 hover:underline flex items-center gap-1 font-bold cursor-pointer min-h-[28px]"
                  >
                    <ShieldAlert className="w-2.5 h-2.5" /> 10-Point Check
                  </button>
                </div>
              </div>
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => handleSelectPreset("Generate a 10-point comprehensive pre-ride mechanical and electrical safety checklist.")}
              className="hidden sm:flex items-center gap-1 bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white px-2.5 py-1.5 rounded-xl text-[9px] font-mono font-bold cursor-pointer transition-all min-h-[36px]"
            >
              <ListChecks className="w-3 h-3 text-cyan-400" /> Pre-Ride Inspection
            </button>

            <div className="flex items-center bg-black/50 border border-white/10 rounded-xl px-2 py-1 text-[8px] font-mono text-zinc-400 gap-1 min-h-[36px]">
              <span>TTS:</span>
              {[1.0, 1.25, 1.5].map((speed) => (
                <button
                  key={speed}
                  type="button"
                  onClick={() => setTtsSpeed(speed)}
                  className={`px-1 py-0.5 rounded cursor-pointer min-h-[28px] ${ttsSpeed === speed ? "bg-white/20 text-white font-bold" : "hover:text-zinc-200"}`}
                >
                  {speed}x
                </button>
              ))}
            </div>

            {messages.length > 0 && (
              <button
                type="button"
                onClick={() => setMessages([])}
                className="p-1.5 rounded-xl bg-white/5 hover:bg-rose-950/40 text-zinc-400 hover:text-rose-400 border border-white/10 transition-all cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center"
                title="Clear current active canvas"
              >
                <RefreshCw className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>

        {/* CHAT LOGS CANVAS */}
        <div className="flex-1 overflow-y-auto p-3 sm:p-4 space-y-4 custom-scrollbar">
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center opacity-50 space-y-3">
              <div className="p-3 rounded-2xl bg-white/5 border border-white/10 shadow-2xl">
                <Wrench className={`w-8 h-8 ${t.text} animate-pulse`} />
              </div>
              <div className="text-center space-y-1">
                <p className="text-[10px] sm:text-[11px] text-zinc-200 font-black uppercase tracking-widest font-mono">
                  MASTER WORKSHOP CHIEF READY
                </p>
                <p className="text-[8px] text-zinc-500 font-mono max-w-sm px-4">
                  Select a workshop protocol above, upload hardware photos, or describe the fault.
                </p>
              </div>

              <div className="flex flex-wrap gap-1.5 justify-center max-w-md mt-2 px-2">
                {[
                  "Hall Sensor Multimeter Test",
                  "Why is motor stuttering under load?",
                  "Battery Voltage Sag Diagnostics",
                  "Hydraulic Brake Bleeding Guide"
                ].map((chip, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSelectPreset(chip)}
                    className="px-2.5 py-1.5 rounded-xl bg-black/40 border border-white/10 text-zinc-400 hover:text-white hover:border-white/30 text-[8px] font-mono transition-all cursor-pointer min-h-[36px]"
                  >
                    + {chip}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((m: Message) => {
              const completedCount = m.checklist ? m.checklist.filter(c => c.completed).length : 0;
              const totalChecklist = m.checklist ? m.checklist.length : 0;
              const progressPercent = totalChecklist > 0 ? Math.round((completedCount / totalChecklist) * 100) : 0;

              return (
                <div key={m.id} className={`flex flex-col ${m.role === "user" || m.role === "system" ? "items-end" : "items-start"} animate-in fade-in duration-200`}>
                  
                  <div className="flex items-center gap-1.5 mb-1 px-1">
                    <span className="text-[7px] font-mono font-black uppercase text-zinc-500 tracking-wider">
                      {m.role === "user" ? "OPERATOR" : m.role === "system" ? "SYSTEM FAULT" : "WORKSHOP CHIEF"}
                    </span>
                    {m.timestamp && <span className="text-[7px] font-mono text-zinc-600">• {m.timestamp}</span>}
                  </div>

                  <div className={`p-3.5 sm:p-4 rounded-3xl text-[10px] sm:text-xs whitespace-pre-wrap leading-relaxed max-w-[95%] sm:max-w-[85%] shadow-lg ${
                    m.role === "user" 
                      ? `${t.dim} text-zinc-100 rounded-br-sm border${t.borderSubtle}` 
                      : m.role === "system"
                        ? "bg-rose-950/40 text-rose-300 rounded-br-sm border border-rose-900/50"
                        : "bg-[#181a20]/95 backdrop-blur-md border border-white/10 text-zinc-100 rounded-bl-sm font-medium shadow-[0_4px_25px_rgba(0,0,0,0.6)]"
                  }`}>
                    {m.image && (
                      <div className="relative mb-3 group cursor-pointer" onClick={() => setIsZoomed(true)}>
                        <img src={m.image} alt="" className="max-w-full h-32 object-cover rounded-2xl border border-white/10" />
                        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity rounded-2xl flex items-center justify-center text-white text-[9px] font-mono">
                          Click to Magnify Target
                        </div>
                      </div>
                    )}

                    <div className="relative z-10 space-y-1.5">
                      {m.text.split("\n").map((line: string, lineIdx: number) => (
                        <p key={lineIdx} className="min-h-[0.8rem] leading-relaxed">{line}</p>
                      ))}
                    </div>

                    {/* INTERACTIVE CHECKLIST WITH PROGRESS BAR */}
                    {m.checklist && m.checklist.length > 0 && (
                      <div className="mt-3 pt-3 border-t border-white/10 space-y-2 bg-black/30 p-3 rounded-2xl">
                        <div className="flex justify-between items-center font-mono">
                          <span className="text-[8px] sm:text-[9px] font-black text-cyan-400 uppercase tracking-widest flex items-center gap-1.5">
                            <ListChecks className="w-3.5 h-3.5" /> Workshop Action Plan:
                          </span>
                          <span className="text-[8px] sm:text-[9px] text-zinc-400 font-bold">{completedCount} / {totalChecklist} Completed ({progressPercent}%)</span>
                        </div>
                        
                        <div className="w-full bg-zinc-800 h-1.5 rounded-full overflow-hidden">
                          <div className="bg-cyan-400 h-full transition-all duration-300" style={{ width: `${progressPercent}%` }}></div>
                        </div>

                        <div className="space-y-1.5 pt-1">
                          {m.checklist.map((item: ChecklistItem, cIdx: number) => (
                            <label key={cIdx} className="flex items-center gap-2.5 min-h-[36px] text-[10px] text-zinc-200 cursor-pointer hover:text-white transition-colors">
                              <input 
                                type="checkbox" 
                                checked={item.completed} 
                                onChange={() => {
                                  setMessages((prev: any) => prev.map((msg: any) => {
                                    if (msg.id !== m.id || !msg.checklist) return msg;
                                    const updated = [...msg.checklist];
                                    updated[cIdx] = { ...updated[cIdx], completed: !updated[cIdx].completed };
                                    return { ...msg, checklist: updated };
                                  }));
                                }} 
                                className="w-3.5 h-3.5 accent-cyan-400 rounded cursor-pointer shrink-0" 
                              />
                              <span className={item.completed ? "line-through text-zinc-500 font-normal" : "font-bold"}>{item.text}</span>
                            </label>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  {m.role === "ai" && (
                    <div className="flex flex-wrap items-center gap-1.5 mt-2">
                      <button 
                        type="button" 
                        onClick={() => handleTTS(m.text, m.id, ttsSpeed)} 
                        className={`text-[8px] min-h-[36px] px-2.5 font-mono font-bold uppercase flex items-center gap-1.5 cursor-pointer bg-black/60 border border-white/10 rounded-xl transition-all ${currentlyReadingId === m.id ? "text-rose-400 border-rose-500/50" : `${t.text} hover:bg-white/10`}`}
                      >
                        {currentlyReadingId === m.id ? <Square className="w-2.5 h-2.5" /> : <Volume2 className="w-2.5 h-2.5" />}
                        {currentlyReadingId === m.id ? "Halt Voice" : `Voice Read (${ttsSpeed}x)`}
                      </button>
                      <button 
                        type="button" 
                        onClick={() => copyToClipboard(m.text, m.id)} 
                        className="text-[8px] min-h-[36px] px-2.5 font-mono font-bold uppercase flex items-center gap-1.5 text-zinc-400 hover:text-white cursor-pointer bg-black/60 border border-white/10 rounded-xl hover:bg-white/10 transition-all"
                      >
                        {copiedId === m.id ? <Check className="w-2.5 h-2.5 text-emerald-400" /> : <Copy className="w-2.5 h-2.5" />}
                        {copiedId === m.id ? "Copied" : "Copy"}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setQuestion(`Explain more detail regarding: "${m.text.slice(0, 40)}..."`);
                        }}
                        className="text-[8px] min-h-[36px] px-2.5 font-mono text-zinc-500 hover:text-zinc-300 bg-black/40 border border-white/5 rounded-xl transition-all cursor-pointer"
                      >
                        + Follow-up
                      </button>
                    </div>
                  )}
                </div>
              );
            })
          )}
          
          {isAnalyzing && (
            <div className="flex justify-start animate-in fade-in">
              <div className="bg-[#181a20]/90 border border-white/10 rounded-xl p-3 flex items-center gap-2.5 shadow-xl">
                <Loader2 className={`w-3.5 h-3.5 ${t.text} animate-spin`} />
                <span className="text-[10px] text-zinc-300 font-mono font-bold">Workshop chief examining telemetry &amp; components...</span>
              </div>
            </div>
          )}
          
          {errorMsg && (
            <div className="bg-rose-950/40 border border-rose-900/50 text-rose-300 text-[10px] rounded-xl p-3 flex gap-2 items-start shadow-xl">
              <AlertTriangle className="w-3.5 h-3.5 text-rose-400 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}
          
          <div ref={chatEndRef} />
        </div>

        {/* INPUT BAR */}
        <div className="p-2 sm:p-3 bg-black/80 border-t border-white/10 flex gap-1.5 items-end backdrop-blur-xl z-20">
          {previewUrl && (
            <button 
              type="button" 
              onClick={() => { clearImage(); setSecondaryPreviewUrl(null); }} 
              title="Clear Image"
              className="p-2 min-h-[44px] min-w-[44px] rounded-xl border flex items-center justify-center cursor-pointer transition-all bg-rose-500/20 border-rose-500/50 text-rose-400 hover:bg-rose-500/30"
            >
              <XCircle className="w-4 h-4" />
            </button>
          )}
          <button 
            type="button" 
            onClick={toggleListening} 
            title={isListening ? "Listening active" : "Activate microphone"}
            className={`p-2 min-h-[44px] min-w-[44px] rounded-xl border flex items-center justify-center cursor-pointer transition-all ${
              isListening 
                ? "bg-rose-500/25 border-rose-500 text-rose-400 animate-pulse" 
                : "bg-white/5 border-white/10 text-zinc-300 hover:text-white hover:bg-white/10"
            }`}
          >
            {isListening ? <Mic className="w-4 h-4" /> : <MicOff className="w-4 h-4" />}
          </button>
          
          <div className="flex-1 relative">
            <textarea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={(e) => { 
                if (e.key === "Enter" && !e.shiftKey) { 
                  e.preventDefault(); 
                  analyzeImage(); 
                } 
              }}
              rows={1}
              placeholder="Describe mechanical fault to Master Mechanic..."
              className="w-full bg-black/50 border border-white/10 text-[11px] text-white rounded-xl py-3 pl-3 pr-8 outline-none font-bold min-h-[44px] max-h-[120px] resize-none leading-relaxed shadow-inner focus:border-amber-400/50 transition-colors"
            />
            {question && (
              <button
                type="button"
                onClick={() => setQuestion("")}
                className="absolute right-2 top-2.5 text-zinc-500 hover:text-white cursor-pointer min-h-[24px] min-w-[24px] flex items-center justify-center"
              >
                <XCircle className="w-3 h-3" />
              </button>
            )}
          </div>
          
          <button 
            type="button" 
            onClick={() => analyzeImage()} 
            disabled={isAnalyzing || (!question.trim() && !previewUrl)} 
            className={`p-2 h-[44px] w-12 shrink-0 rounded-xl font-black uppercase text-xs flex items-center justify-center border ${t.bg} text-black cursor-pointer shadow-lg disabled:opacity-30 active:scale-95 transition-all`}
          >
            {isAnalyzing ? <Loader2 className="w-4 h-4 animate-spin" /> : <UploadCloud className="w-4 h-4" />}
          </button>
        </div>

      </div>

      {/* ERROR CODES MODAL */}
      {showErrorCodeModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center p-3 z-50 animate-in fade-in">
          <div className="bg-[#0e1017] border border-white/20 rounded-3xl p-5 max-w-lg w-full shadow-2xl space-y-3">
            <div className="flex items-center justify-between border-b border-white/10 pb-2">
              <h3 className="text-white font-mono font-black text-xs uppercase flex items-center gap-1.5">
                <Activity className="w-3.5 h-3.5 text-cyan-400" /> Controller Error Codes
              </h3>
              <button type="button" onClick={() => setShowErrorCodeModal(false)} className="text-zinc-400 hover:text-white cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center">
                <XCircle className="w-4 h-4" />
              </button>
            </div>
            
            <div className="space-y-2 max-h-[300px] overflow-y-auto custom-scrollbar pr-1">
              {errorCodeDb.map((err, i) => (
                <div key={i} className="p-2.5 bg-black/50 border border-white/10 rounded-xl space-y-1">
                  <div className="flex justify-between items-center">
                    <span className="font-mono font-bold text-amber-400 text-[10px]">{err.code}</span>
                    <span className="text-[7px] font-mono uppercase bg-white/10 px-1.5 py-0.5 rounded text-zinc-300">
                      {err.meaning}
                    </span>
                  </div>
                  <p className="text-[8px] text-zinc-400">{err.fix}</p>
                </div>
              ))}
            </div>

            <button 
              type="button"
              onClick={() => setShowErrorCodeModal(false)} 
              className="w-full py-2 bg-white/10 hover:bg-white/20 text-white rounded-xl text-[9px] font-mono font-bold uppercase transition-all cursor-pointer min-h-[44px]"
            >
              Close Database
            </button>
          </div>
        </div>
      )}

      {/* WIRING PINOUT MODAL */}
      {showWiringModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center p-3 z-50 animate-in fade-in">
          <div className="bg-[#0e1017] border border-white/20 rounded-3xl p-5 max-w-lg w-full shadow-2xl space-y-3">
            <div className="flex items-center justify-between border-b border-white/10 pb-2">
              <h3 className="text-white font-mono font-black text-xs uppercase flex items-center gap-1.5">
                <Cpu className="w-3.5 h-3.5 text-emerald-400" /> PEV Pinout Guide
              </h3>
              <button type="button" onClick={() => setShowWiringModal(false)} className="text-zinc-400 hover:text-white cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center">
                <XCircle className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2.5 text-[9px] text-zinc-300 max-h-[320px] overflow-y-auto custom-scrollbar">
              <div className="p-2.5 bg-black/50 border border-white/10 rounded-xl relative">
                <h4 className="font-mono font-bold text-amber-400 text-[10px] mb-1">3-Wire Throttle Lead:</h4>
                <p className="text-[8px] font-mono text-zinc-400">• Red = +5V Regulated Rail</p>
                <p className="text-[8px] font-mono text-zinc-400">• Black = Ground (GND)</p>
                <p className="text-[8px] font-mono text-zinc-400">• Green / White = Hall Signal (0.8V Idle → 4.2V Full WOT)</p>
                <button type="button" onClick={() => copyToClipboard("3-Wire Throttle: Red=+5V, Black=GND, Green=Signal(0.8-4.2V)")} className="absolute top-2 right-2 text-zinc-500 hover:text-white"><Copy className="w-3 h-3" /></button>
              </div>

              <div className="p-2.5 bg-black/50 border border-white/10 rounded-xl relative">
                <h4 className="font-mono font-bold text-emerald-400 text-[10px] mb-1">5-Wire Hall Sensor Harness:</h4>
                <p className="text-[8px] font-mono text-zinc-400">• Red/Black = +5V &amp; GND logic supply</p>
                <p className="text-[8px] font-mono text-zinc-400">• Yellow, Green, Blue = 120° Phase Sensor Channels A, B, C</p>
                <button type="button" onClick={() => copyToClipboard("5-Wire Hall: Red=+5V, Black=GND, Y/G/B=Phases")} className="absolute top-2 right-2 text-zinc-500 hover:text-white"><Copy className="w-3 h-3" /></button>
              </div>
            </div>

            <button 
              type="button"
              onClick={() => setShowWiringModal(false)} 
              className="w-full py-2 bg-white/10 hover:bg-white/20 text-white rounded-xl text-[9px] font-mono font-bold uppercase transition-all cursor-pointer min-h-[44px]"
            >
              Done
            </button>
          </div>
        </div>
      )}

      {/* OPTICAL ZOOM MAGNIFIER */}
      {isZoomed && previewUrl && (
        <div className="fixed inset-0 bg-black/95 backdrop-blur-xl flex items-center justify-center p-3 z-50" onClick={() => setIsZoomed(false)}>
          <div className="relative max-w-4xl max-h-[90vh] w-full flex flex-col items-center">
            <button 
              type="button"
              onClick={() => setIsZoomed(false)}
              className="absolute -top-10 right-0 text-white bg-white/10 p-1.5 rounded-xl hover:bg-white/20 cursor-pointer min-h-[44px] min-w-[44px] flex items-center justify-center"
            >
              <XCircle className="w-5 h-5" />
            </button>
            <img src={previewUrl} alt="Inspection Magnified" className="max-w-full max-h-[85vh] object-contain rounded-2xl border border-white/20 shadow-2xl" />
            <span className="text-zinc-400 text-[9px] font-mono mt-2">Click anywhere to close</span>
          </div>
        </div>
      )}

    </div>
  );
}