"use client";

import React from "react";
import { Users, CheckCircle2, Ban } from "lucide-react";

export default function PilotAuthority({ profiles = {}, onToggleVerify, onToggleBan }: any) {
  return (
    <div className="bg-black/90 border border-white/10 rounded-3xl p-5 space-y-4 shadow-2xl">
      <div className="flex items-center justify-between">
        <span className="text-xs font-mono font-black uppercase tracking-widest text-cyan-400 flex items-center gap-2">
          <Users className="w-4 h-4"/> Pilot Authority &amp; Access Control
        </span>
        <span className="text-[10px] font-mono text-zinc-500 uppercase">{Object.keys(profiles).length} Operators</span>
      </div>

      <div className="space-y-2 max-h-[350px] overflow-y-auto custom-scrollbar pr-1">
        {Object.keys(profiles).map(callsign => {
          const prof = profiles[callsign];
          return (
            <div key={callsign} className="p-3 bg-white/5 border border-white/5 rounded-xl flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-8 h-8 rounded-full bg-black border border-white/10 flex items-center justify-center text-xs font-bold text-white shrink-0">
                  {prof.pfpUrl ? <img src={prof.pfpUrl} alt="" className="w-full h-full object-cover"/> : callsign.charAt(0).toUpperCase()}
                </div>
                <div className="min-w-0">
                  <span className="text-xs font-bold text-white truncate block flex items-center gap-1">
                    {callsign}
                    {prof.isVerifiedRider && <CheckCircle2 className="w-3.5 h-3.5 text-blue-400 fill-blue-400 shrink-0"/>}
                  </span>
                  <span className="text-[9px] font-mono text-zinc-500 uppercase">{prof.userFleet || "Universal PEV"}</span>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button onClick={() => onToggleVerify(callsign, !!prof.isVerifiedRider)} className={`px-2.5 py-1 rounded-lg text-[9px] font-black uppercase cursor-pointer ${prof.isVerifiedRider ? "bg-blue-600 text-white" : "bg-white/10 text-zinc-400 hover:text-white"}`}>
                  {prof.isVerified ? "Verified" : "Verify"}
                </button>
                <button onClick={() => onToggleBan(callsign, !!prof.isGrounded)} className={`px-2.5 py-1 rounded-lg text-[9px] font-black uppercase cursor-pointer ${prof.isGrounded ? "bg-emerald-600 text-black font-bold" : "bg-rose-600/40 text-rose-300 hover:bg-rose-600 hover:text-white"}`}>
                  {prof.isGrounded ? "Pardon" : "Ban"}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}