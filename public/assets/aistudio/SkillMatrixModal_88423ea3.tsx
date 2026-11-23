"use client";

import React, { useState } from "react";
import { X, Sparkles } from "lucide-react";
import { COPILOT_SKILLS } from "../data/copilotSkills";

interface SkillMatrixModalProps {
  show: boolean;
  onClose: () => void;
  onSelectSkill: (prompt: string) => void;
  themeColors: any;
}

export default function SkillMatrixModal({
  show,
  onClose,
  onSelectSkill,
  themeColors: t
}: SkillMatrixModalProps) {
  const [selectedCategory, setSelectedCategory] = useState<string>("Navigation");

  if (!show) return null;

  const categories = Array.from(new Set(COPILOT_SKILLS.map((s) => s.cat)));
  const filteredSkills = COPILOT_SKILLS.filter((s) => s.cat === selectedCategory);

  return (
    <div className="fixed inset-0 z-[999999] bg-black/80 backdrop-blur-xl flex items-center justify-center p-3 sm:p-4 font-sans">
      <div className="app-card bg-black/70 backdrop-blur-3xl border border-white/10 w-full max-w-5xl rounded-3xl p-5 sm:p-6 shadow-2xl flex flex-col max-h-[85vh] overflow-hidden">
        
        {/* HEADER */}
        <div className="flex justify-between items-center border-b border-white/10 pb-4 mb-4 shrink-0">
          <h3 className={`text-xs sm:text-sm font-black uppercase tracking-widest flex items-center gap-2 ${t.text}`}>
            <Sparkles className="w-4 h-4" /> Universal AI Skill Matrix (100+ Skills)
          </h3>
          <button 
            type="button"
            onClick={onClose}
            className="bg-white/5 border border-white/10 text-zinc-400 hover:text-white p-2.5 rounded-xl cursor-pointer transition-all active:scale-95 min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* BODY */}
        <div className="flex flex-col md:flex-row gap-4 h-full overflow-hidden">
          
          {/* CATEGORY NAV */}
          <div className="w-full md:w-56 shrink-0 flex md:flex-col gap-2 overflow-x-auto md:overflow-y-auto custom-scrollbar pb-2 md:pb-0 md:pr-1">
            {categories.map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => setSelectedCategory(cat)}
                className={`min-h-[44px] px-4 py-2.5 rounded-xl text-[9px] sm:text-[10px] font-black uppercase tracking-widest flex items-center justify-center md:justify-start transition-all active:scale-95 cursor-pointer whitespace-nowrap border ${
                  selectedCategory === cat 
                    ? `${t.bg} text-black border-transparent shadow-lg font-black` 
                    : 'bg-black/40 border-white/10 text-zinc-400 hover:text-white hover:bg-white/10'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* SKILL LIST */}
          <div className="flex-1 overflow-y-auto custom-scrollbar pr-1 grid grid-cols-1 sm:grid-cols-2 gap-2.5 h-full pb-4">
            {filteredSkills.map((skill, sIdx) => (
              <button
                key={sIdx}
                type="button"
                onClick={() => {
                  onSelectSkill(skill.prompt);
                  onClose();
                }}
                className="flex flex-col items-start text-left p-3.5 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/10 transition-all duration-300 active:scale-95 cursor-pointer shadow-inner min-h-[44px]"
              >
                <span className={`text-[10px] font-black uppercase tracking-widest flex items-center gap-2 mb-1.5 ${t.text}`}>
                  <skill.icon className="w-4 h-4 shrink-0" /> {skill.label}
                </span>
                <span className="text-[10px] font-medium text-zinc-400 leading-relaxed line-clamp-2">
                  {skill.prompt}
                </span>
              </button>
            ))}
          </div>

        </div>

      </div>
    </div>
  );
}