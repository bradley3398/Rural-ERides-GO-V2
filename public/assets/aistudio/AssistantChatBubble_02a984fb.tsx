"use client";

import React from "react";
import { UserCircle, BrainCircuit, Youtube, ImageIcon, Maximize2, Volume2, Square, Copy, Check, Gauge, CloudRain, AlertOctagon } from "lucide-react";

interface AssistantChatBubbleProps {
  m: any;
  i: number;
  callsign: string;
  persona: string;
  t: any;
  fontSizeClass: string;
  performanceMode: boolean;
  currentlyReadingIndex: number | null;
  handleTTS: (text: string, index: number) => void;
  copyToClipboard: (text: string, index: number) => void;
  copiedIndex: number | null;
  setLightboxImg: (url: string) => void;
  maxImageCount: number;
  liveStats?: any; 
  weatherStats?: string;
}

export default function AssistantChatBubble({ 
  m, 
  i, 
  callsign, 
  persona, 
  t, 
  fontSizeClass, 
  performanceMode, 
  currentlyReadingIndex, 
  handleTTS, 
  copyToClipboard, 
  copiedIndex, 
  setLightboxImg, 
  maxImageCount,
  liveStats,
  weatherStats 
}: AssistantChatBubbleProps) {

  // --- 🔥 NEW: AGENTIC WIDGET PARSER 🔥 ---
  const extractWidgets = (text: string) => {
    const widgets: string[] = [];
    let cleanText = text;
    
    if (cleanText.includes("[WIDGET: TELEMETRY]")) {
      widgets.push("TELEMETRY");
      cleanText = cleanText.replace(/\[WIDGET:\s*TELEMETRY\]/gi, "");
    }
    if (cleanText.includes("[WIDGET: WEATHER]")) {
      widgets.push("WEATHER");
      cleanText = cleanText.replace(/\[WIDGET:\s*WEATHER\]/gi, "");
    }
    if (cleanText.includes("[WIDGET: SOS]")) {
      widgets.push("SOS");
      cleanText = cleanText.replace(/\[WIDGET:\s*SOS\]/gi, "");
    }
    
    return { cleanText: cleanText.trim(), widgets };
  };

  const { cleanText, widgets } = extractWidgets(m.text || "");

  return (
    <div className={`flex flex-col w-full font-sans mb-4 ${m.role === 'user' ? 'items-end' : 'items-start'}`}>
      
      <div className={`flex items-center gap-2 mb-2 ${m.role === 'user' ? 'flex-row-reverse mr-1' : 'ml-1'}`}>
        <div className={`w-6 h-6 rounded-full flex items-center justify-center border ${m.role === 'user' ? `${t.border} bg-white/5` : `${t.bg} border-transparent shadow-lg`}`}>
          {m.role === 'user' ? <UserCircle className={`w-4 h-4 ${t.text}`} /> : <BrainCircuit className="w-4 h-4 text-black" />} </div> <span className="text-[9px] font-mono font-black text-zinc-500 tracking-widest uppercase"> [{m.timestamp \vert{}\vert{} "Log"} // {m.role === 'user' ? (callsign ? callsign : 'PILOT') : `SYSTEM.${persona?.toUpperCase() || 'AI'}`}]
        </span>
      </div>

      <div className={`p-4 sm:p-5 rounded-2xl ${fontSizeClass} whitespace-pre-wrap leading-relaxed max-w-[95\%] sm:max-w-[90\%] relative overflow-hidden shadow-xl${
        m.role === 'user' 
          ? `${t.bg} text-black font-bold rounded-tr-sm` 
          : `bg-black/60 backdrop-blur-3xl border ${t.subtle || 'border-white/10'} text-zinc-100 rounded-tl-sm font-medium shadow-[0_8px_30px_rgba(0,0,0,0.6)]`
      }`}>
        
        {m.role === 'ai' && !performanceMode && (
          <div className="absolute top-0 left-0 w-full h-1/2 bg-gradient-to-b from-white/10 to-transparent pointer-events-none" />
        )}

        <div className="relative z-10 space-y-3">
          {(cleanText.split('\n')).map((line: string, lineIdx: number) => {
            const isBullet = line.trim().startsWith('* ') || line.trim().startsWith('- ');
            const clnLine = line.replace(/^[\*\-]\s/, '');
            
            const formattedLine = clnLine.split(/(\*\*.*?\*\*)/g).map((part: string, pIdx: number) => {
              if (part.startsWith('**') && part.endsWith('**')) {
                return <strong key={pIdx} className="font-black text-white tracking-wide">{part.slice(2, -2)}</strong>;
              }
              return <span key={pIdx}>{part}</span>;
            });

            if (isBullet) {
              return (
                <div key={lineIdx} className="flex items-start gap-3 pl-2 mt-1">
                  <div className={`w-2 h-2 rounded-full mt-1.5 shrink-0 shadow-lg ${m.role === 'user' ? 'bg-black/50' : `${t.bg} shadow-[0_0_8px_currentColor]`}`} /> <div className="flex-1 leading-relaxed">{formattedLine}</div> </div> ); } return <p key={lineIdx} className="min-h-[1rem] leading-relaxed">{formattedLine}</p>; })} </div>  {/* 🔥 NEW: RENDER AGENTIC WIDGETS 🔥 */} {widgets.length > 0 && ( <div className="mt-4 pt-3 border-t border-white/10 relative z-10 grid grid-cols-1 sm:grid-cols-2 gap-3"> {widgets.includes("TELEMETRY") && ( <div className="bg-black/60 border border-cyan-500/30 p-3 rounded-xl shadow-inner flex items-center gap-3"> <div className="p-2 bg-cyan-500/20 rounded-lg text-cyan-400"><Gauge className="w-5 h-5"/></div> <div> <span className="text-[9px] font-black uppercase text-cyan-400 tracking-widest block mb-0.5">Live Velocity</span> <span className="text-sm font-mono font-bold text-white">{liveStats?.speed?.toFixed(1) \vert{}\vert{} '0.0'} MPH</span> </div> </div> )} {widgets.includes("WEATHER") && ( <div className="bg-black/60 border border-amber-500/30 p-3 rounded-xl shadow-inner flex items-center gap-3"> <div className="p-2 bg-amber-500/20 rounded-lg text-amber-400"><CloudRain className="w-5 h-5"/></div> <div className="min-w-0"> <span className="text-[9px] font-black uppercase text-amber-400 tracking-widest block mb-0.5">Atmospherics</span> <span className="text-xs font-mono font-bold text-white block truncate">{weatherStats \vert{}\vert{} 'Scanning...'}</span> </div> </div> )} {widgets.includes("SOS") && ( <button className="col-span-1 sm:col-span-2 bg-rose-600 hover:bg-rose-500 border border-rose-400 p-3 rounded-xl shadow-[0_0_20px_rgba(225,29,72,0.4)] flex items-center justify-center gap-3 transition-colors cursor-pointer active:scale-95"> <AlertOctagon className="w-5 h-5 text-white animate-pulse"/> <span className="text-xs font-black uppercase text-white tracking-widest">Transmit Mayday Broadcast</span> </button> )} </div> )}  {m.youtubeVideos?.length > 0 && ( <div className="mt-5 pt-4 border-t border-white/10 relative z-10"> <span className="text-[10px] font-mono font-black text-zinc-400 uppercase tracking-widest block mb-3 flex items-center gap-1.5"> <Youtube className="w-4 h-4 text-rose-500" /> Integrated Research &amp; Media Feed: </span> <div className="grid grid-cols-1 md:grid-cols-2 gap-4"> {m.youtubeVideos.map((yt: any, ytIdx: number) => ( <div key={ytIdx} className="rounded-xl border border-white/10 bg-black/60 overflow-hidden flex flex-col shadow-inner transition-transform hover:scale-[1.02]"> <div className="relative w-full aspect-video bg-zinc-950"> {yt.videoId ? ( <iframe src={`https://www.youtube.com/embed/${yt.videoId}`} 
                        title={yt.title} 
                        className="w-full h-full border-0" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                        allowFullScreen 
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-zinc-600 text-[10px] uppercase font-mono">Video Unavailable</div>
                    )}
                  </div>
                  <div className="p-3 flex flex-col justify-between flex-1">
                    <span className="text-[11px] font-bold text-zinc-200 line-clamp-2">{yt.title}</span>
                    <a href={yt.url} target="_blank" rel="noopener noreferrer" className="text-[9px] font-mono font-bold text-zinc-400 hover:text-rose-400 truncate mt-2 block transition-colors min-h-[24px] flex items-center">Open on YouTube ↗</a>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {m.webImages?.length > 0 && (
          <div className="mt-5 pt-4 border-t border-white/10 relative z-10">
            <span className="text-[10px] font-mono font-black text-zinc-400 uppercase tracking-widest block mb-3 flex items-center gap-1.5">
              <ImageIcon className="w-4 h-4 text-cyan-400" /> Discovered Route &amp; Terrain Photos (Up to {maxImageCount} Results):
            </span>
            <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-5 gap-3 max-h-[350px] overflow-y-auto custom-scrollbar pr-2">
              {m.webImages.map((imgUrl: string, imgIdx: number) => (
                <div 
                  key={imgIdx} 
                  onClick={() => setLightboxImg(imgUrl)} 
                  className="block relative group overflow-hidden rounded-xl border border-white/10 bg-black/50 cursor-pointer aspect-square shadow-sm min-h-[44px]"
                >
                  <img src={imgUrl} alt="" className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300 mix-blend-lighten" />
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-sm">
                    <Maximize2 className="w-6 h-6 text-white drop-shadow-lg" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
      
      <div className={`flex gap-3 mt-2 px-1 ${m.role === 'user' ? 'justify-end' : 'justify-start'} w-full items-center flex-wrap`}>
         {m.role === 'ai' && (
           <>
              <button 
                type="button"
                onClick={() => handleTTS(cleanText || '', i)} 
                className={`min-h-[44px] min-w-[44px] px-4 text-[10px] font-black uppercase flex items-center justify-center gap-2 cursor-pointer border border-white/5 rounded-xl bg-black/40 backdrop-blur-md shadow-inner transition-all active:scale-95 ${
                  currentlyReadingIndex === i ? 'text-amber-400 animate-pulse border-amber-500/30' : `${t.text} hover:text-white hover:bg-black/60`
                }`}
              >
                {currentlyReadingIndex === i ? <Square className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
                <span className="hidden sm:inline">{currentlyReadingIndex === i ? "Halt Feed" : "Read Aloud"}</span>
              </button>

              <button 
                type="button"
                onClick={() => copyToClipboard(cleanText || '', i)} 
                className="min-h-[44px] min-w-[44px] px-4 text-[10px] font-black uppercase flex items-center justify-center gap-2 cursor-pointer border border-white/5 rounded-xl bg-black/40 backdrop-blur-md shadow-inner text-zinc-400 hover:text-white hover:bg-black/60 transition-all active:scale-95"
              >
                {copiedIndex === i ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                <span className="hidden sm:inline">{copiedIndex === i ? "Synced" : "Copy Log"}</span>
              </button>

              <span className="text-[9px] font-mono font-black text-zinc-500 tracking-widest uppercase ml-auto">
                TOKENS: {(cleanText || '').split(/\s+/).length}
              </span>
           </>
         )}
      </div>
    </div>
  );
}