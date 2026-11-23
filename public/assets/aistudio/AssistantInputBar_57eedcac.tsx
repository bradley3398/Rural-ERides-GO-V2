"use client";

import React, { useState } from "react";
import { ImagePlus, Camera, Mic, MicOff, ImageIcon, Youtube, Send, X, Loader2, Radio } from "lucide-react";

interface AssistantInputBarProps {
  input: string;
  setInput: (val: string) => void;
  handleSearch: (query: string) => void;
  selectedImage: string | null;
  clearSelectedImage: () => void;
  isUploadingImg: boolean;
  fileInputRef: React.RefObject<HTMLInputElement>;
  cameraInputRef: React.RefObject<HTMLInputElement>;
  handleImageUpload: (e: React.ChangeEvent<HTMLInputElement>) => void;
  toggleListening: (continuous?: boolean) => void;
  isListening: boolean;
  enableImageSearch: boolean;
  setEnableImageSearch: (val: boolean) => void;
  enableYouTubeSearch: boolean;
  setEnableYouTubeSearch: (val: boolean) => void;
  isSearching: boolean;
  deepReasoningMode: boolean;
  fontSizeClass: string;
  themeColors: any;
}

export default function AssistantInputBar({
  input, 
  setInput, 
  handleSearch, 
  selectedImage, 
  clearSelectedImage,
  isUploadingImg, 
  fileInputRef, 
  cameraInputRef, 
  handleImageUpload,
  toggleListening, 
  isListening, 
  enableImageSearch, 
  setEnableImageSearch,
  enableYouTubeSearch, 
  setEnableYouTubeSearch, 
  isSearching, 
  deepReasoningMode,
  fontSizeClass, 
  themeColors: t
}: AssistantInputBarProps) {

  // --- 🔥 NEW: CONTINUOUS HANDS-FREE MODE 🔥 ---
  const [isHandsFree, setIsHandsFree] = useState(false);

  const toggleHandsFree = () => {
    setIsHandsFree(!isHandsFree);
    toggleListening(!isHandsFree);
  };

  return (
    <div className={`mt-3 flex flex-col gap-3 shrink-0 relative z-10 border-t border-white/10 pt-4 font-sans bg-black/20 rounded-xl p-2 transition-all ${isHandsFree ? 'ring-2 ring-rose-500/50 shadow-[0_0_20px_rgba(225,29,72,0.3)]' : ''}`}>
      
      <div className="flex gap-2 items-center justify-between flex-wrap">
        {selectedImage ? (
          <div className="relative inline-block w-fit">
            <img src={selectedImage} alt="Attached Inspection" className={`h-12 w-12 object-cover rounded-xl border-2 ${t.border} shadow-lg`} />
            <button 
              type="button" 
              onClick={clearSelectedImage} 
              className="absolute -top-2 -right-2 bg-rose-600 hover:bg-rose-500 text-white rounded-full p-1.5 shadow-lg cursor-pointer transition-transform active:scale-95 min-h-[30px] min-w-[30px] flex items-center justify-center"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ) : (
          <div className="flex gap-2 overflow-x-auto custom-scrollbar pb-1">
            <input type="file" accept="image/*" ref={fileInputRef} onChange={handleImageUpload} className="hidden" />
            <button 
              type="button" 
              onClick={() => fileInputRef.current?.click()} 
              disabled={isUploadingImg || isHandsFree} 
              className="min-h-[44px] min-w-[44px] sm:min-w-[50px] sm:min-h-[50px] flex items-center justify-center bg-white/5 border border-white/10 text-zinc-300 hover:text-white rounded-xl cursor-pointer shadow-md shrink-0 transition-all active:scale-95 disabled:opacity-50"
              title="Upload Photo"
            >
              {isUploadingImg ? <Loader2 className="w-5 h-5 animate-spin" /> : <ImagePlus className="w-5 h-5" />}
            </button>

            <input type="file" accept="image/*" capture="environment" ref={cameraInputRef} onChange={handleImageUpload} className="hidden" />
            <button 
              type="button" 
              onClick={() => cameraInputRef.current?.click()} 
              disabled={isUploadingImg || isHandsFree} 
              className="min-h-[44px] min-w-[44px] sm:min-w-[50px] sm:min-h-[50px] flex items-center justify-center bg-white/5 border border-white/10 text-zinc-300 hover:text-white rounded-xl cursor-pointer shadow-md shrink-0 transition-all active:scale-95 disabled:opacity-50"
              title="Camera Snap"
            >
              {isUploadingImg ? <Loader2 className="w-5 h-5 animate-spin" /> : <Camera className="w-5 h-5" />}
            </button>

            {/* SINGLE PTT */}
            <button 
              type="button" 
              onClick={() => toggleListening(false)} 
              disabled={isHandsFree}
              className={`min-h-[44px] min-w-[44px] sm:min-w-[50px] sm:min-h-[50px] flex items-center justify-center rounded-xl cursor-pointer shadow-md shrink-0 transition-all active:scale-95 disabled:opacity-30 ${
                (isListening && !isHandsFree) ? "bg-rose-900/60 border-rose-500/50 text-rose-400 animate-pulse" : "bg-white/5 border-white/10 text-zinc-300 hover:text-white"
              }`}
              title="Voice Input"
            >
              {(isListening && !isHandsFree) ? <Mic className="w-5 h-5" /> : <MicOff className="w-5 h-5" />}
            </button>

            {/* CONTINUOUS HANDS-FREE */}
            <button 
              type="button" 
              onClick={toggleHandsFree} 
              className={`min-h-[44px] px-3 sm:px-4 rounded-xl cursor-pointer shadow-md shrink-0 transition-all active:scale-95 flex items-center gap-2 border font-black uppercase tracking-widest text-[9px] ${
                isHandsFree ? "bg-rose-600 border-rose-500 text-white animate-pulse shadow-[0_0_15px_rgba(225,29,72,0.5)]" : "bg-white/5 border-white/10 text-zinc-300 hover:text-white"
              }`}
            >
              <Radio className="w-4 h-4 shrink-0" />
              <span className="hidden sm:inline">{isHandsFree ? "Comms Open" : "Hands-Free"}</span>
            </button>
          </div>
        )}

        <div className="flex items-center gap-2 ml-auto">
          <button 
            type="button" 
            onClick={() => setEnableImageSearch(!enableImageSearch)} 
            className={`min-h-[44px] px-3.5 rounded-xl border text-[10px] font-black uppercase tracking-wider flex items-center gap-2 cursor-pointer shadow-sm transition-all active:scale-95 ${
              enableImageSearch ? `${t.dim} ${t.border}` : 'bg-black/40 border-white/10 text-zinc-500 hover:bg-white/5 hover:text-zinc-300'
            }`}
          >
            <ImageIcon className="w-4 h-4 shrink-0" /> 
            <span className="hidden xs:inline">{enableImageSearch ? "Pics ON" : "Pics OFF"}</span>
          </button>
          <button 
            type="button" 
            onClick={() => setEnableYouTubeSearch(!enableYouTubeSearch)} 
            className={`min-h-[44px] px-3.5 rounded-xl border text-[10px] font-black uppercase tracking-wider flex items-center gap-2 cursor-pointer shadow-sm transition-all active:scale-95 ${
              enableYouTubeSearch ? 'bg-rose-950/40 text-rose-400 border-rose-900/50' : 'bg-black/40 border-white/10 text-zinc-500 hover:bg-white/5 hover:text-zinc-300'
            }`}
          >
            <Youtube className="w-4 h-4 shrink-0" /> 
            <span className="hidden xs:inline">{enableYouTubeSearch ? "YT ON" : "YT OFF"}</span>
          </button>
        </div>
      </div>

      <div className="flex gap-2 items-end">
        {/* CSS WAVEFORM RENDERER DURING HANDS FREE */}
        {isHandsFree ? (
          <div className="flex-1 min-w-0 bg-rose-950/40 border border-rose-500/50 rounded-2xl h-[50px] flex items-center justify-center gap-1.5 shadow-inner">
             <span className="w-1.5 h-4 bg-rose-500 rounded-full animate-[bounce_0.8s_infinite]" />
             <span className="w-1.5 h-6 bg-rose-500 rounded-full animate-[bounce_1.1s_infinite]" />
             <span className="w-1.5 h-3 bg-rose-500 rounded-full animate-[bounce_0.9s_infinite]" />
             <span className="w-1.5 h-7 bg-rose-500 rounded-full animate-[bounce_1.2s_infinite]" />
             <span className="w-1.5 h-4 bg-rose-500 rounded-full animate-[bounce_0.7s_infinite]" />
             <span className="ml-3 text-[10px] font-mono font-black text-rose-400 uppercase tracking-widest">Listening...</span>
          </div>
        ) : (
          <textarea 
            value={input} 
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { 
              if (e.key === "Enter" && !e.shiftKey) { 
                e.preventDefault(); 
                handleSearch(input); 
              } 
            }}
            rows={input.split('\n').length > 1 ? Math.min(input.split('\n').length, 5) : 1}
            className={`flex-1 min-w-0 bg-black/60 backdrop-blur-xl border border-white/10 rounded-2xl py-3.5 px-5 text-white ${fontSizeClass} font-bold outline-none focus:border-white/30 placeholder:text-zinc-500 min-h-[50px] max-h-[160px] custom-scrollbar shadow-inner resize-none leading-relaxed transition-all`}
            placeholder="Ask Copilot or input trail logs..."
          />
        )}
        
        <button 
          type="button"
          onClick={() => handleSearch(input)}
          disabled={(!input.trim() && !selectedImage) || isSearching || isUploadingImg || isHandsFree}
          className={`min-h-[50px] h-[50px] min-w-[50px] shrink-0 rounded-2xl disabled:opacity-20 flex items-center justify-center cursor-pointer shadow-lg transition-all active:scale-95 ${
            deepReasoningMode ? 'bg-amber-500 text-black shadow-[0_0_20px_rgba(245,158,11,0.6)]' : `${t.bg} text-black`
          }`}
        >
          <Send className="w-6 h-6 ml-1" />
        </button>
      </div>
    </div>
  );
}