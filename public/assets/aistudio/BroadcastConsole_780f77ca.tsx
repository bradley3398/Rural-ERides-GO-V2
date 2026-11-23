"use client";

import React, { useState } from "react";
import { Send, AlertTriangle } from "lucide-react";

export default function BroadcastConsole({ onSendBroadcast }: any) {
  const [msg, setMsg] = useState("");
  const [cat, setCat] = useState("🚨 SYSTEM ALERT");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!msg.trim() || loading) return;
    setLoading(true);
    await onSendBroadcast(msg, cat);
    setMsg("");
    setLoading(false);
  };

  return (
    <div className="bg-black/90 border border-white/10 rounded-3xl p-5 space-y-4 shadow-2xl">
      <div className="flex items-center justify-between">
        <span className="text-xs font-mono font-black uppercase tracking-widest text-amber-400 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4"/> Emergency Broadcast Console
        </span>
      </div>

      <form onSubmit={handleSubmit} className="space-y-3">
        <select value={cat} onChange={e => setCat(e.target.value)} className="w-full bg-zinc-900 border border-white/10 rounded-xl px-3 py-2 text-xs font-bold text-white outline-none">
          <option value="🚨 SYSTEM ALERT">🚨 SYSTEM ALERT</option>
          <option value="⚠️ TRAIL CLOSURE">⚠️ TRAIL CLOSURE</option>
          <option value="⚡ VOLT BOUNTY">⚡ VOLT BOUNTY</option>
        </select>
        <textarea rows={3} value={msg} onChange={e => setMsg(e.target.value)} placeholder="Type network broadcast payload..." className="w-full bg-zinc-950 border border-white/10 rounded-xl p-3 text-xs text-white outline-none font-sans" />
        <button type="submit" disabled={!msg.trim() || loading} className="w-full py-2.5 bg-[#39ff14] hover:bg-[#32e012] text-black rounded-xl text-xs font-black uppercase tracking-widest transition-all cursor-pointer active:scale-98 shadow-md flex items-center justify-center gap-2">
          <Send className="w-3.5 h-3.5"/> {loading ? "Broadcasting..." : "Broadcast Alert"}
        </button>
      </form>
    </div>
  );
}