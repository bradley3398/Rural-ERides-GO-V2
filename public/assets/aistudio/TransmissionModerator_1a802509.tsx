"use client";

import React from "react";
import { Trash2, AlertTriangle, ShieldAlert } from "lucide-react";

export default function TransmissionModerator({ flaggedPosts = [], onPurge }: any) {
  return (
    <div className="bg-black/90 border border-white/10 rounded-3xl p-5 space-y-4 shadow-2xl">
      <div className="flex items-center justify-between">
        <span className="text-xs font-mono font-black uppercase tracking-widest text-rose-500 flex items-center gap-2">
          <ShieldAlert className="w-4 h-4"/> Flagged Post Queue &amp; Instant Purge
        </span>
        <span className="text-[10px] font-mono text-zinc-500 uppercase">{flaggedPosts.length} Flagged Items</span>
      </div>

      <div className="space-y-2 max-h-[350px] overflow-y-auto custom-scrollbar pr-1">
        {flaggedPosts.length === 0 ? (
          <div className="p-8 text-center text-zinc-600 font-mono text-xs uppercase">No flagged transmissions requiring moderation.</div>
        ) : (
          flaggedPosts.map((p: any) => (
            <div key={p.id} className="p-3 bg-rose-950/20 border border-rose-500/30 rounded-xl flex items-center justify-between gap-4">
              <div>
                <span className="text-[10px] font-mono font-bold text-rose-400 uppercase">@{p.username} • {p.category}</span>
                <p className="text-xs text-zinc-200 mt-0.5 line-clamp-1">{p.content}</p>
              </div>
              <button onClick={() => onPurge(p.id)} className="px-3 py-1.5 bg-rose-600 hover:bg-rose-500 text-white rounded-lg text-[10px] font-black uppercase cursor-pointer shrink-0">
                Purge
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}