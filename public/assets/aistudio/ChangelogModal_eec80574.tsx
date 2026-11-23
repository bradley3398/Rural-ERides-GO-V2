"use client";

import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  X, Sparkles, ShieldCheck, CheckCircle2, Radio, Phone, 
  Layers, Sliders, MessageSquare, MapPin, Zap, Compass, 
  Volume2, Cpu, Video, Image, Trophy, AlertTriangle, FileCode,
  Share2, Shield, Flame, Search, Check, Smartphone, Terminal,
  Activity, Wrench, DollarSign, CloudSun, Database, Mic
} from "lucide-react";

export default function ChangelogModal({
  showChangelogModal,
  dismissChangelogModal,
  activeTheme,
  cornerStyle,
  reducedMotion,
  currentVersion
}: any) {
  const [activeCategory, setActiveCategory] = useState<
    "all" | "core_avionics" | "viewport" | "comms" | "settings" | "feed" | "composer" | "profile" | "radar" | "gamification"
  >("all");
  const [searchQuery, setSearchQuery] = useState("");

  if (!showChangelogModal) return null;

  const CHANGELOG_ITEMS = [
    // --- 1. CORE COCKPIT, AVIONICS & TELEMETRY SUITE ---
    { id: 1, cat: "core_avionics", title: "Universal TSX Compartmentalization", desc: "Deconstructed legacy dashboard into isolated modules: SettingsMatrix, PEVProfileSetup, AICopilotWidget, PredictiveMaintenance, WeatherMatrix, YoutubeDeck, MissionLogs, FinancialImpact, and ChangelogModal to eliminate memory bottlenecks[cite: 13]." },
    { id: 2, cat: "core_avionics", title: "9-DOF AHRS IMU Calibration Matrix & Zeroing", desc: "Sensor fusion engine combining gyroscope integration, gravity anchoring, kinematic centripetal decoupling, and a handlebar level calibration zeroing offset[cite: 13]." },
    { id: 3, cat: "core_avionics", title: "Policy-Compliant Hybrid YouTube Audio Deck", desc: "Visible iframe player container with volume sliders, mute toggles, track looping, 0.75x–1.5x speed adjustment, ±10s skip controls, and 18 curated genre presets[cite: 13]." },
    { id: 4, cat: "core_avionics", title: "Tactical AI Copilot & Voice HUD Suite", desc: "Neural advisory states, virtual 10S–24S cell balancing monitors calculating voltage sag, KERS regenerative bonus range counters, and TTS voice engine[cite: 13]." },
    { id: 5, cat: "core_avionics", title: "Predictive Maintenance Vault & Financial Ledger", desc: "Mechanical wear tracking for tires, brake pads, chains, spoke tension, suspension forks, hydraulic flushes, IRS standard mileage deduction ($0.76/mi), and gas savings[cite: 13]." },
    { id: 6, cat: "core_avionics", title: "Live Synoptic Weather & Aero Matrix", desc: "12-sensor atmospheric grid monitoring UV, visibility, barometric pressure, dew point, headwind/tailwind vectors, crosswind shear, and air density (ρ)[cite: 13]." },
    { id: 7, cat: "core_avionics", title: "Mission Telemetry Archives & Database Vault", desc: "Real-time search, sorting pipelines (speed, distance, efficiency), JSON database backup export/restore utilities, and GPX track exports[cite: 13]." },

    // --- 2. VIEWPORT ARCHITECTURE & MOBILE CLAMPING ---
    { id: 8, cat: "viewport", title: "Top Cockpit Status Bar Clearance", desc: "Clamped modal overlays with fixed top-16 padding to prevent the top avionics HUD from covering headers and close buttons[cite: 7, 8]." },
    { id: 9, cat: "viewport", title: "Bottom Navigation Dock Decoupling", desc: "Enforced strict bottom-20 and bottom-24 padding to prevent action buttons from colliding with Cockpit, Radar, Diagnostics, and Board tabs[cite: 7, 8]." },
    { id: 10, cat: "viewport", title: "Flexbox Flow Control & Pinned Footers", desc: "Converted action drawers to flex-shrink-0 anchors so Save, Reset, and Send buttons never cover scrollable options[cite: 7, 8]." },
    { id: 11, cat: "viewport", title: "Mobile Touch Target Optimization", desc: "Expanded all button hitboxes to a minimum of 44x44px across modals, menus, and trays to meet mobile native guidelines." },
    { id: 12, cat: "viewport", title: "Tactile Mobile Drag Handles", desc: "Added interactive drag pill indicators across bottom sheet wrappers for natural touch interaction on mobile screens." },

    // --- 3. COMMS & WEBRTC ENGINE ---
    { id: 13, cat: "comms", title: "P2P WebRTC Audio Call Pipeline", desc: "Native browser RTCPeerConnection with STUN candidate routing for zero-latency peer-to-peer audio comms[cite: 11]." },
    { id: 14, cat: "comms", title: "Full-Duplex WebRTC Video Streams", desc: "Live video calling with automatic remote track binding and local stream monitoring[cite: 11]." },
    { id: 15, cat: "comms", title: "Environment/User Camera Toggling", desc: "Front-and-rear camera flipping during live video sessions without breaking peer connection state[cite: 11]." },
    { id: 16, cat: "comms", title: "Native Capacitor Call Notifications", desc: "Background local notification channels with Accept and Decline actions for incoming hails[cite: 11]." },
    { id: 17, cat: "comms", title: "Web Audio Synthesizer Ringers", desc: "Dual-tone 440Hz/480Hz procedural oscillators for call ringing with zero static audio asset dependencies[cite: 11]." },
    { id: 18, cat: "comms", title: "Canvas Base64 Image Compression", desc: "In-memory HTML5 Canvas image resizer compressing photo uploads directly in comms before Firestore persistence." },
    { id: 19, cat: "comms", title: "MediaRecorder Voice Note Capture", desc: "Microphone audio recording into WebM blobs with live recording duration counters." },
    { id: 20, cat: "comms", title: "Inline Voice Note Audio Player", desc: "Native audio controls directly inside chat bubbles for sent and received voice notes." },
    { id: 21, cat: "comms", title: "Gemini 3.1 Flash AI Comms Translator", desc: "Incoming message translation directly via Gemini API with per-message translation caching[cite: 5, 11]." },
    { id: 22, cat: "comms", title: "SHA-256 Web Crypto Session Hashes", desc: "Deterministic SHA-256 session hashes generated from participant callsigns via crypto.subtle." },
    { id: 23, cat: "comms", title: "Persistent Blocked Operator Vault", desc: "User blocking tied directly to persistent browser storage, hiding conversations and blocking calls[cite: 11]." },
    { id: 24, cat: "comms", title: "High-Accuracy GPS Comms Pinning", desc: "Direct location drops to device GPS with timeout fallbacks and coordinate formatting[cite: 11]." },
    { id: 25, cat: "comms", title: "Dynamic Backdrops & Topography", desc: "Real-time background switching between Dark, Matrix grid, and Topographic radar maps[cite: 11]." },

    // --- 4. SETTINGS MATRIX & PROCEDURAL AUDIO ---
    { id: 26, cat: "settings", title: "100% Social-Only Architecture", desc: "Removed mechanical PEV diagnostic placeholders in favor of community, curation, and comms toggles[cite: 7]." },
    { id: 27, cat: "settings", title: "10 Procedural Audio Waveforms", desc: "10 native procedural audio tones (Tactical Dual, Cyber Arpeggio, Neon Glider, Sonar Pulse, 8-Bit Arcade, Emergency Siren, Voltage Spike, Trail Echo, Digital Chirp, Sub-Bass Drop)[cite: 1, 7]." },
    { id: 28, cat: "settings", title: "Real-Time Audio Audition Previews", desc: "Play/stop controls on all tone cards with automatic gain ramping and timeout cleanup[cite: 1, 7]." },
    { id: 29, cat: "settings", title: "Audio Ducking & Haptic Preferences", desc: "Settings for media volume ducking during incoming alerts and mechanical haptic motor toggles[cite: 1, 7]." },
    { id: 30, cat: "settings", title: "Full Social Link Manager", desc: "Direct profile parameters for YouTube, Instagram, Strava, TikTok, and Discord channels[cite: 7]." },
    { id: 31, cat: "settings", title: "250+ Persistent LocalStorage Keys", desc: "Every toggle, range slider, and dropdown wired directly to persistent storage with window broadcast dispatchers[cite: 1, 7]." },
    { id: 32, cat: "settings", title: "One-Tap Avionics Matrix Factory Reset", desc: "Confirmation-gated cache wipe returning all social settings to default[cite: 1, 7]." },

    // --- 5. FEED & BROADCAST TRANSMISSION ENGINE ---
    { id: 33, cat: "feed", title: "Live Inline Post Editing", desc: "Broadcast authors can edit content in-place with instant Firestore updates and [Edited] status stamps[cite: 5]." },
    { id: 34, cat: "feed", title: "Gemini AI Transmission Summarizer", desc: "Gemini 3.1 Flash Lite integration generating punchy one-sentence operational summaries[cite: 5]." },
    { id: 35, cat: "feed", title: "Universal Feed Translation", desc: "Feed-level translation converting foreign posts to English/Spanish via Google Gemini[cite: 5]." },
    { id: 36, cat: "feed", title: "Double-Tap to Volt Gesture", desc: "Double-tap gesture listeners on post media containers triggering animated haptic volting[cite: 5]." },
    { id: 37, cat: "feed", title: "Super Volt 100-Volt Milestone Bursts", desc: "Milestone particle celebrations triggering on transmissions reaching 100+ Volts[cite: 5]." },
    { id: 38, cat: "feed", title: "Classified Marketplace Offer Comms Bridge", desc: "Marketplace buy offers linked directly into secure Direct Messages with pre-populated offer amounts[cite: 5]." },
    { id: 39, cat: "feed", title: "Interactive Multi-Option Poll Suite", desc: "Dynamic poll voting with arrayUnion locking, vote counters, and animated percentage bars[cite: 5]." },
    { id: 40, cat: "feed", title: "GPX Trail Elevation Contour Display", desc: "Interactive SVG route contours, elevation gains, and mileage stats on trail report cards[cite: 5]." },
    { id: 41, cat: "feed", title: "Live Reaction Breakdown Drawer", desc: "Inspection drawer displaying real reaction counts across fire, volt, heart, and battery types[cite: 5]." },
    { id: 42, cat: "feed", title: "Universal Post Repost Pipeline", desc: "One-tap reposting that clones transmissions into the Universal Feed with attributed pilot handles[cite: 5]." },

    // --- 6. POST COMPOSER PIPELINE ---
    { id: 43, cat: "composer", title: "Zero-Alert Rule Enforcement", desc: "Eliminated browser alert() popups across GIF searching, GPX parsing, and draft saving[cite: 7]." },
    { id: 44, cat: "composer", title: "Visual Badge & Sticker Selector Tray", desc: "Interactive sticker grid embedding custom badges into transmissions without alerts." },
    { id: 45, cat: "composer", title: "Real GPX / KML / GeoJSON File Importer", desc: "File picker parsing satellite coordinate files with badge chips." },
    { id: 46, cat: "composer", title: "Multi-Track Background Audio Selector", desc: "Curated ambient, synthwave, and bassline riding tracks attached to post metadata[cite: 7]." },
    { id: 47, cat: "composer", title: "Scheduled Post Release Dispatcher", desc: "Datetime-local pickers to stage broadcast publication for group ride sync[cite: 7]." },
    { id: 48, cat: "composer", title: "Crew Co-Author & Collab Tagger", desc: "Collaborative authoring fields attaching multi-rider callsigns to feed cards[cite: 7]." },
    { id: 49, cat: "composer", title: "AI Polish Voice & Tone Switcher", desc: "Gemini prompt engineering to polish rider drafts into technical or trail-ready tones[cite: 7]." },

    // --- 7. USER PROFILES & SOCIAL SYNC ---
    { id: 50, cat: "profile", title: "Direct Profile Social Link Synchronization", desc: "Bound external profile icons directly to URLs saved in settings, showing verified links on public cards[cite: 10]." },
    { id: 51, cat: "profile", title: "Computed Pilot Trust Score Algorithm", desc: "Derived trust scores from community interactions, volts, and verified posts[cite: 10]." },
    { id: 52, cat: "profile", title: "SOS Trail Rescue Log Counter", desc: "Automated scanning computing total SOS beacons resolved by each operator[cite: 10]." },
    { id: 53, cat: "profile", title: "Live Firestore Volt Tip Jar", desc: "Instant Volt transfers creating system notifications for recipients[cite: 10]." },
    { id: 54, cat: "profile", title: "Sector Rivalry Challenge Beacon", desc: "Rivalry challenge triggers broadcasting sector alerts to rival pilots[cite: 10]." },

    // --- 8. RADAR & LEADERBOARD MECHANICS ---
    { id: 55, cat: "radar", title: "Trigonometric Haversine Distance Calculator", desc: "Calculated exact nautical mile/kilometer radii from live GPS coordinates to surrounding operators[cite: 6]." },
    { id: 56, cat: "radar", title: "360° True Bearing Canvas Scanner", desc: "400x400 canvas sweep beam plotting live color-coded blips for hazards, SOS, and group rides[cite: 6]." },
    { id: 57, cat: "gamification", title: "Mathematical Rider XP & Streak Badges", desc: "Derived level badges, daily flame streaks, and prestige stars from database telemetry[cite: 8]." },
    { id: 58, cat: "gamification", title: "Sector Standings Record Aggregator", desc: "Deterministic mathematical aggregator identifying top speeds, MVP volts, and master mechanic builds[cite: 9]." }
  ];

  const filteredItems = CHANGELOG_ITEMS.filter((item) => {
    const matchesCat = activeCategory === "all" || item.cat === activeCategory;
    const matchesSearch = item.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          item.desc.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

  return (
    <div className="fixed inset-0 z-[999999] bg-black/90 backdrop-blur-2xl flex flex-col justify-between pt-16 pb-20 sm:p-6 sm:justify-center sm:items-center">
      <motion.div 
        initial={{ scale: reducedMotion ? 1 : 0.95, opacity: 0 }} 
        animate={{ scale: 1, opacity: 1 }} 
        exit={{ scale: reducedMotion ? 1 : 0.95, opacity: 0 }} 
        className={`app-card bg-zinc-950 border border-white/15 w-full max-w-2xl h-full max-h-[86vh] flex flex-col overflow-hidden shadow-2xl relative rounded-3xl`}
      >
        {/* HEADER */}
        <div className="flex items-center justify-between p-4 sm:p-5 border-b border-white/10 shrink-0 bg-white/5">
          <div className="flex items-center gap-3 min-w-0 pr-2">
            <div className={`p-2.5 rounded-2xl ${activeTheme.bg} text-black font-black shadow-md shrink-0`}>
              <Sparkles className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <h2 className="text-xs sm:text-sm font-black text-white uppercase tracking-widest truncate">
                SYSTEM CHANGE LOG • COMPLETE SUITE
              </h2>
              <span className={`text-[9px] sm:text-[10px] ${activeTheme.text} font-mono uppercase tracking-wider block mt-0.5 font-bold truncate`}>
                v{currentVersion} • Core Avionics + Social Architecture Verified
              </span>
            </div>
          </div>
          <button 
            type="button" 
            onClick={dismissChangelogModal} 
            className="min-h-[44px] min-w-[44px] text-zinc-400 hover:text-white p-2.5 bg-black/50 rounded-full border border-white/10 cursor-pointer active:scale-95 transition-all flex items-center justify-center shrink-0"
          >
            <X className="w-4 h-4"/>
          </button>
        </div>

        {/* SEARCH BAR */}
        <div className="px-4 sm:px-5 py-3 border-b border-white/10 bg-black/40 flex items-center gap-2.5 shrink-0">
          <Search className="w-4 h-4 text-zinc-500 shrink-0"/>
          <input 
            type="text"
            placeholder="Search all 58 features, avionics, WebRTC comms, radar..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-transparent text-xs font-bold text-white outline-none placeholder:text-zinc-600 min-h-[32px]"
          />
          {searchQuery && (
            <button onClick={() => setSearchQuery("")} className="text-xs text-zinc-500 hover:text-white shrink-0 px-2 py-1">Clear</button>
          )}
        </div>

        {/* CATEGORY FILTER TABS */}
        <div className="flex border-b border-white/10 bg-black/60 px-4 py-2.5 gap-2 overflow-x-auto custom-scrollbar shrink-0">
          {[
            { id: "all", label: `All (${CHANGELOG_ITEMS.length})` },
            { id: "core_avionics", label: "Core Avionics" },
            { id: "viewport", label: "Viewport Clamping" },
            { id: "comms", label: "WebRTC Comms" },
            { id: "settings", label: "Settings Matrix" },
            { id: "feed", label: "Feed Engine" },
            { id: "composer", label: "Composer" },
            { id: "profile", label: "Profiles" },
            { id: "radar", label: "Radar Sweep" },
            { id: "gamification", label: "XP & Standings" }
          ].map(tab => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveCategory(tab.id as any)}
              className={`min-h-[36px] px-3.5 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-wider transition-all cursor-pointer flex items-center shrink-0 whitespace-nowrap shadow-sm ${
                activeCategory === tab.id ? `${activeTheme.bg} text-black font-black shadow-md` : "text-zinc-400 hover:text-white bg-white/5"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* CHANGELOG SCROLLER */}
        <div className="flex-1 overflow-y-auto custom-scrollbar p-4 sm:p-6 space-y-3">
          <div className="bg-black/40 border border-white/10 rounded-2xl p-4 shadow-inner">
            <h3 className={`text-[10px] font-black uppercase tracking-widest ${activeTheme.text} mb-4 flex items-center gap-1.5`}>
              <ShieldCheck className="w-4 h-4"/> Verified Deployments: {filteredItems.length} Active System Nodes
            </h3>

            <ul className="text-[11px] text-zinc-300 space-y-4 font-medium list-none">
              {filteredItems.map((item) => (
                <li key={item.id} className="flex gap-3 items-start">
                  <span className={`px-2 py-0.5 rounded-md font-mono text-[9px] font-black uppercase tracking-wider shrink-0 mt-0.5 ${activeTheme.bg} text-black`}>
                    #{item.id}
                  </span>
                  <div className="min-w-0 flex-1">
                    <strong className="text-white text-xs block mb-0.5 tracking-wide">
                      {item.title}
                    </strong>
                    <p className="text-zinc-400 leading-relaxed font-normal text-[11px]">
                      {item.desc}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* FOOTER ACTION BUTTON */}
        <div className="shrink-0 p-4 border-t border-white/10 bg-zinc-950/95 flex">
          <button 
            type="button" 
            onClick={dismissChangelogModal} 
            className={`w-full min-h-[48px] ${activeTheme.bg} text-black font-black uppercase tracking-widest rounded-2xl text-xs shadow-xl cursor-pointer transition-all active:scale-95 flex items-center justify-center gap-2`}
          >
            <CheckCircle2 className="w-4 h-4"/> Acknowledge &amp; Launch Cockpit Platform
          </button>
        </div>
      </motion.div>
    </div>
  );
}