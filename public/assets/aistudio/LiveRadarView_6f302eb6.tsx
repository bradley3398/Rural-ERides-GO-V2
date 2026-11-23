"use client";

import React, { useState, useEffect, useRef, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  Radar, Compass, MapPin, ShieldAlert, Zap, Users, 
  AlertTriangle, Navigation, Radio, Maximize2, Minimize2, 
  Layers, Volume2, User, Phone, Mail, ExternalLink, X
} from "lucide-react";
import { locationService } from "../../services/LocationService";

interface LiveRadarViewProps {
  posts: any[];
  userProfiles: Record<string, any>;
  currentUser: string;
  useMetric: boolean;
  themeColors: any;
  onOpenProfile: (username: string) => void;
  onOpenDM: (username: string) => void;
  glassBlurClass?: string;
  cornerStyle?: string;
}

// Haversine Distance Calculation
function getDistance(lat1: number, lon1: number, lat2: number, lon2: number, useMetric: boolean): number {
  const R = useMetric ? 6371 : 3958.8; // km vs miles
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a = 
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * 
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

// Bearing Calculation (Degrees 0-360)
function getBearing(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const y = Math.sin(dLon) * Math.cos((lat2 * Math.PI) / 180);
  const x = 
    Math.cos((lat1 * Math.PI) / 180) * Math.sin((lat2 * Math.PI) / 180) -
    Math.sin((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.cos(dLon);
  let brng = (Math.atan2(y, x) * 180) / Math.PI;
  return (brng + 360) % 360;
}

export default function LiveRadarView({
  posts = [],
  userProfiles = {},
  currentUser,
  useMetric,
  themeColors: t,
  onOpenProfile,
  onOpenDM,
  glassBlurClass = "backdrop-blur-2xl",
  cornerStyle = "rounded-3xl"
}: LiveRadarViewProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [radarRange, setRadarRange] = useState<number>(25); // Range in mi or km
  const [selectedTarget, setSelectedTarget] = useState<any | null>(null);
  const [activeLayers, setActiveLayers] = useState({
    pilots: true,
    traps: true,
    sos: true,
    rides: true,
    posts: true
  });

  const currentLoc = locationService.getCurrentUpdate();
  const isGpsLocked = !!currentLoc && typeof currentLoc.lat === "number" && currentLoc.lat !== 0;
  const userLat = currentLoc?.lat || 0;
  const userLng = currentLoc?.lng || 0;
  const distUnit = useMetric ? "km" : "mi";

  // --- 1. COMPILE ALL LIVE GEOLOCATED TARGETS ---
  const radarTargets = useMemo(() => {
    if (!isGpsLocked) return [];
    const targets: any[] = [];

    // Map all active geolocated posts
    posts.forEach((post) => {
      if (post.location && typeof post.location.lat === "number") {
        const dist = getDistance(userLat, userLng, post.location.lat, post.location.lng, useMetric);
        const bearing = getBearing(userLat, userLng, post.location.lat, post.location.lng);

        if (dist <= radarRange) {
          targets.push({
            id: post.id,
            type: post.isSpeedTrap 
              ? "trap" 
              : post.isHelpNeeded 
              ? "sos" 
              : post.category === "Group Ride" 
              ? "ride" 
              : "post",
            title: post.username,
            content: post.content,
            distance: dist,
            bearing: bearing,
            lat: post.location.lat,
            lng: post.location.lng,
            pfpUrl: post.pfpUrl || userProfiles[post.username]?.pfpUrl,
            telemetry: post.telemetry,
            timestamp: post.timestamp
          });
        }
      }
    });

    return targets;
  }, [posts, userProfiles, isGpsLocked, userLat, userLng, radarRange, useMetric]);

  // --- 2. CANVAS RADAR SCANNER SWEEP ENGINE ---
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animationFrameId: number;
    let angle = 0;

    const render = () => {
      const width = canvas.width;
      const height = canvas.height;
      const centerX = width / 2;
      const centerY = height / 2;
      const radius = Math.min(centerX, centerY) - 20;

      ctx.clearRect(0, 0, width, height);

      // Background Grid Circles
      ctx.strokeStyle = "rgba(255, 255, 255, 0.1)";
      ctx.lineWidth = 1;
      for (let i = 1; i <= 4; i++) {
        ctx.beginPath();
        ctx.arc(centerX, centerY, (radius / 4) * i, 0, Math.PI * 2);
        ctx.stroke();
      }

      // Crosshairs
      ctx.beginPath();
      ctx.moveTo(centerX - radius, centerY);
      ctx.lineTo(centerX + radius, centerY);
      ctx.moveTo(centerX, centerY - radius);
      ctx.lineTo(centerX, centerY + radius);
      ctx.stroke();

      // Rotating Sweep Beam
      const sweepX = centerX + radius * Math.cos(angle);
      const sweepY = centerY + radius * Math.sin(angle);

      const gradient = ctx.createRadialGradient(centerX, centerY, 0, centerX, centerY, radius);
      gradient.addColorStop(0, "rgba(57, 255, 20, 0.2)");
      gradient.addColorStop(1, "rgba(57, 255, 20, 0)");

      ctx.save();
      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.arc(centerX, centerY, radius, angle - 0.4, angle);
      ctx.closePath();
      ctx.fillStyle = gradient;
      ctx.fill();
      ctx.restore();

      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.lineTo(sweepX, sweepY);
      ctx.strokeStyle = "#39ff14";
      ctx.lineWidth = 2;
      ctx.stroke();

      // Plot Target Blips
      radarTargets.forEach((target) => {
        // Filter out disabled layers
        if (target.type === "trap" && !activeLayers.traps) return;
        if (target.type === "sos" && !activeLayers.sos) return;
        if (target.type === "ride" && !activeLayers.rides) return;
        if (target.type === "post" && !activeLayers.posts) return;

        const distanceRatio = target.distance / radarRange;
        const targetRadius = distanceRatio * radius;
        const rad = ((target.bearing - 90) * Math.PI) / 180;

        const blipX = centerX + targetRadius * Math.cos(rad);
        const blipY = centerY + targetRadius * Math.sin(rad);

        // Blip Glow & Color
        ctx.beginPath();
        ctx.arc(blipX, blipY, target.type === "sos" ? 7 : 5, 0, Math.PI * 2);
        
        if (target.type === "sos") {
          ctx.fillStyle = "#ef4444";
          ctx.shadowColor = "#ef4444";
          ctx.shadowBlur = 10;
        } else if (target.type === "trap") {
          ctx.fillStyle = "#f59e0b";
          ctx.shadowColor = "#f59e0b";
          ctx.shadowBlur = 8;
        } else if (target.type === "ride") {
          ctx.fillStyle = "#a855f7";
          ctx.shadowColor = "#a855f7";
          ctx.shadowBlur = 8;
        } else {
          ctx.fillStyle = "#06b6d4";
          ctx.shadowColor = "#06b6d4";
          ctx.shadowBlur = 6;
        }

        ctx.fill();
        ctx.shadowBlur = 0; // Reset
      });

      // User Self Pin (Center)
      ctx.beginPath();
      ctx.arc(centerX, centerY, 6, 0, Math.PI * 2);
      ctx.fillStyle = "#ffffff";
      ctx.fill();
      ctx.strokeStyle = "#000000";
      ctx.lineWidth = 2;
      ctx.stroke();

      angle += 0.03;
      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [radarTargets, activeLayers, radarRange]);

  // Handle Canvas Tap/Click to Select Nearest Target
  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const width = canvas.width;
    const height = canvas.height;
    const centerX = width / 2;
    const centerY = height / 2;
    const radius = Math.min(centerX, centerY) - 20;

    let closest: any = null;
    let minDistance = 20; // Hitbox radius

    radarTargets.forEach((target) => {
      const distanceRatio = target.distance / radarRange;
      const targetRadius = distanceRatio * radius;
      const rad = ((target.bearing - 90) * Math.PI) / 180;
      const blipX = centerX + targetRadius * Math.cos(rad);
      const blipY = centerY + targetRadius * Math.sin(rad);

      const dist = Math.hypot(blipX - x, blipY - y);
      if (dist < minDistance) {
        minDistance = dist;
        closest = target;
      }
    });

    setSelectedTarget(closest);
  };

  return (
    <div className={`app-card ${glassBlurClass} ${cornerStyle} bg-black/80 border border-white/10 p-5 shadow-2xl relative overflow-hidden space-y-5 font-sans`}>
      {/* RADAR HEADER */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-white/10 pb-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-[#39ff14]/10 border border-[#39ff14]/30 text-[#39ff14]">
            <Radar className="w-5 h-5 animate-spin" style={{ animationDuration: "6s" }} />
          </div>
          <div>
            <h3 className="text-sm font-black uppercase tracking-widest text-white flex items-center gap-2">
              Live Sector Radar Sweep
            </h3>
            <span className="text-[10px] font-mono text-zinc-400 uppercase tracking-wider block">
              Real-Time Tactical GPS Proximity Grid
            </span>
          </div>
        </div>

        {/* RANGE CONTROLS */}
        <div className="flex items-center gap-2">
          {[5, 15, 25, 50].map((rng) => (
            <button
              key={rng}
              type="button"
              onClick={() => setRadarRange(rng)}
              className={`px-3 py-1.5 rounded-xl text-[10px] font-mono font-black uppercase transition-all cursor-pointer ${
                radarRange === rng ? `${t.bg} text-black shadow-md` : "bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-400"
              }`}
            >
              {rng} {distUnit}
            </button>
          ))}
        </div>
      </div>

      {/* LAYER FILTER TOGGLES */}
      <div className="flex items-center gap-2 overflow-x-auto custom-scrollbar pb-1 text-xs">
        <button
          type="button"
          onClick={() => setActiveLayers({ ...activeLayers, sos: !activeLayers.sos })}
          className={`px-3 py-1.5 rounded-full border text-[10px] font-mono font-black uppercase flex items-center gap-1.5 transition-all cursor-pointer ${
            activeLayers.sos ? "bg-red-500/20 border-red-500 text-red-300" : "bg-black/40 border-white/10 text-zinc-600"
          }`}
        >
          <AlertTriangle className="w-3 h-3" /> SOS Beacons
        </button>
        <button
          type="button"
          onClick={() => setActiveLayers({ ...activeLayers, traps: !activeLayers.traps })}
          className={`px-3 py-1.5 rounded-full border text-[10px] font-mono font-black uppercase flex items-center gap-1.5 transition-all cursor-pointer ${
            activeLayers.traps ? "bg-amber-500/20 border-amber-500 text-amber-300" : "bg-black/40 border-white/10 text-zinc-600"
          }`}
        >
          <ShieldAlert className="w-3 h-3" /> Speed Traps
        </button>
        <button
          type="button"
          onClick={() => setActiveLayers({ ...activeLayers, rides: !activeLayers.rides })}
          className={`px-3 py-1.5 rounded-full border text-[10px] font-mono font-black uppercase flex items-center gap-1.5 transition-all cursor-pointer ${
            activeLayers.rides ? "bg-purple-500/20 border-purple-500 text-purple-300" : "bg-black/40 border-white/10 text-zinc-600"
          }`}
        >
          <Users className="w-3 h-3" /> Group Rides
        </button>
        <button
          type="button"
          onClick={() => setActiveLayers({ ...activeLayers, posts: !activeLayers.posts })}
          className={`px-3 py-1.5 rounded-full border text-[10px] font-mono font-black uppercase flex items-center gap-1.5 transition-all cursor-pointer ${
            activeLayers.posts ? "bg-cyan-500/20 border-cyan-500 text-cyan-300" : "bg-black/40 border-white/10 text-zinc-600"
          }`}
        >
          <Radio className="w-3 h-3" /> Broadcasts
        </button>
      </div>

      {/* RADAR CANVAS CONTAINER */}
      <div className="relative w-full aspect-square max-w-[420px] mx-auto rounded-3xl overflow-hidden border border-white/10 bg-black/90 flex items-center justify-center shadow-inner">
        <canvas
          ref={canvasRef}
          width={400}
          height={400}
          onClick={handleCanvasClick}
          className="w-full h-full cursor-crosshair"
        />

        {/* Compass Cardinal Points */}
        <span className="absolute top-3 text-[10px] font-mono font-black text-zinc-500">N</span>
        <span className="absolute bottom-3 text-[10px] font-mono font-black text-zinc-500">S</span>
        <span className="absolute right-3 text-[10px] font-mono font-black text-zinc-500">E</span>
        <span className="absolute left-3 text-[10px] font-mono font-black text-zinc-500">W</span>

        {!isGpsLocked && (
          <div className="absolute inset-0 bg-black/80 backdrop-blur-md flex flex-col items-center justify-center p-6 text-center space-y-2">
            <Compass className="w-10 h-10 text-amber-400 animate-pulse" />
            <h4 className="text-sm font-black uppercase text-white">Acquiring GPS Fix</h4>
            <p className="text-xs text-zinc-400">Positioning sensor locked. Connect device GPS to plot live sector telemetry.</p>
          </div>
        )}
      </div>

      {/* SELECTED TARGET HUD DRAWER */}
      <AnimatePresence>
        {selectedTarget && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            className="p-4 rounded-2xl bg-white/5 border border-white/10 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-xl"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-black border border-white/20 flex items-center justify-center overflow-hidden shrink-0">
                {selectedTarget.pfpUrl ? (
                  <img src={selectedTarget.pfpUrl} alt="" className="w-full h-full object-cover" />
                ) : (
                  <User className="w-5 h-5 text-zinc-500" />
                )}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-black uppercase text-white">{selectedTarget.title}</span>
                  <span className="text-[9px] font-mono px-2 py-0.5 rounded-md bg-white/10 text-zinc-300 uppercase">
                    {selectedTarget.type}
                  </span>
                </div>
                <p className="text-[11px] text-zinc-300 font-medium truncate max-w-xs mt-0.5">
                  {selectedTarget.content}
                </p>
                <span className="text-[9px] font-mono text-emerald-400 font-bold">
                  {selectedTarget.distance.toFixed(1)} {distUnit} away • Bearing {Math.round(selectedTarget.bearing)}°
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <button
                type="button"
                onClick={() => onOpenProfile(selectedTarget.title)}
                className="flex-1 sm:flex-initial px-3 py-2 bg-white/10 hover:bg-white/20 rounded-xl text-[10px] font-black uppercase text-white transition-colors cursor-pointer"
              >
                Profile
              </button>
              <button
                type="button"
                onClick={() => onOpenDM(selectedTarget.title)}
                className={`flex-1 sm:flex-initial px-3 py-2 rounded-xl text-[10px] font-black uppercase transition-colors cursor-pointer ${t.bg} text-black font-black`}
              >
                Comms
              </button>
              <button
                type="button"
                onClick={() => setSelectedTarget(null)}
                className="p-2 bg-black/40 text-zinc-500 hover:text-white rounded-xl transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}