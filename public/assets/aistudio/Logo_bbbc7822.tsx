"use client";

import React from "react";

interface LogoProps {
  className?: string;
  size?: number;
  activeTheme?: any;
  cornerStyle?: string;
  glassBlurClass?: string;
}

export default function Logo({ 
  className = "", 
  size = 64,
  activeTheme,
  cornerStyle = "rounded-2xl",
  glassBlurClass = "backdrop-blur-xl"
}: LogoProps) {
  const accentColor = activeTheme?.hex || "#00f0ff";
  const strokeAccent = activeTheme?.text ? "currentColor" : "#39ff14";

  return (
    <div 
      className={`app-card relative inline-flex items-center justify-center select-none overflow-hidden ${cornerStyle} ${glassBlurClass} border border-white/20 shadow-[0_8px_32px_0_rgba(0,0,0,0.36)] transition-all ${className}`}
      style={{ 
        width: size, 
        height: size,
        background: "linear-gradient(135deg, rgba(0,210,255,0.18) 0%, rgba(0,112,243,0.25) 50%, rgba(16,185,129,0.18) 100%), #0a0a0f" 
      }}
    >
      <svg 
        viewBox="0 0 200 200" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        className="w-full h-full p-2.5 drop-shadow-md"
      >
        {/* Rolling Hills Background */}
        <path 
          d="M 20 60 Q 100 40 180 65 L 180 75 Q 100 55 20 70 Z" 
          fill="#1b5e20" 
          opacity="0.85"
        />
        <path 
          d="M 10 70 Q 100 50 190 75 L 190 85 Q 100 60 10 80 Z" 
          fill="#121212" 
        />

        {/* Pine Trees Silhouette */}
        {/* Tree 1 */}
        <polygon points="75,55 81,65 77,65 83,73 72,73 75,55" fill="#121212" />
        <rect x="76.5" y="73" width="1.5" height="4" fill="#121212" />

        {/* Tree 2 (Main Center) */}
        <polygon points="86,43 94,56 89,56 97,68 82,68 86,43" fill="#121212" />
        <rect x="88.5" y="68" width="2" height="6" fill="#121212" />

        {/* Tree 3 */}
        <polygon points="98,50 104,61 100,61 106,70 94,70 98,50" fill="#121212" />
        <rect x="99.5" y="70" width="1.5" height="4" fill="#121212" />

        {/* Electric Scooter / PEV Chassis Silhouette */}
        {/* Wheels */}
        <circle cx="70" cy="142" r="14" stroke="#121212" strokeWidth="6" fill="none" />
        <circle cx="138" cy="142" r="14" stroke="#121212" strokeWidth="6" fill="none" />
        <circle cx="70" cy="142" r="4" fill="#121212" />
        <circle cx="138" cy="142" r="4" fill="#121212" />

        {/* Base Frame & Deck */}
        <path 
          d="M 70 142 L 78 126 L 114 126 L 138 142" 
          stroke="#121212" 
          strokeWidth="6" 
          strokeLinecap="round" 
          strokeLinejoin="round" 
        />
        
        {/* Main Body Hull */}
        <path 
          d="M 78 126 Q 96 112 114 126 C 114 126 122 108 122 96 C 122 92 118 90 114 92 C 104 96 82 96 78 114 Z" 
          fill="#121212" 
        />

        {/* Steering Stem & Handlebars */}
        <path 
          d="M 120 126 L 128 86 L 122 84" 
          stroke="#121212" 
          strokeWidth="6" 
          strokeLinecap="round" 
        />

        {/* Integrated Seat / Rear Subframe */}
        <path 
          d="M 76 114 L 64 114 L 62 106" 
          stroke="#121212" 
          strokeWidth="4" 
          strokeLinecap="round" 
        />

        {/* Dynamic Theme Lightning Bolt */}
        <polygon 
          points="100,103 94,116 99,116 97,127 106,112 101,112 100,103" 
          fill={accentColor} 
          stroke={strokeAccent}
          strokeWidth="1.5"
          strokeLinejoin="round"
          className="drop-shadow-[0_0_8px_currentColor]"
        />
      </svg>
    </div>
  );
}