"use client";

import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  X, Settings, Lock, Shield, Zap, Sparkles, Volume2, Clock, 
  Image, User, UserCheck, ShieldAlert, Key, Eye, EyeOff, Radio, 
  Battery, Gauge, Bell, MapPin, Compass, Wifi, Sliders, 
  HelpCircle, MessageCircle, Heart, Share2, Layers, CheckCircle2, 
  ChevronDown, Trash2, RotateCcw, Phone, PhoneCall, Music, 
  Play, Square, Video, Award, Target, Trophy, Flame, UserPlus, 
  Filter, Search, MessageSquare, AlertTriangle, Globe, Mic2, Server, Activity
} from "lucide-react";

export default function BoardSettingsModal({
  isSettingsOpen, setIsSettingsOpen, username, pfpUrl, userFleet, setUserFleet,
  userBio, setUserBio, defaultPostCategory, setDefaultPostCategory,
  dataSaverMode, setDataSaverMode, privacyMode, setPrivacyMode,
  pfpInputRef, handlePfpUpload, triggerHaptic, themeColors, CATEGORIES,
  glassBlurClass = "backdrop-blur-2xl",
  cornerStyle = "rounded-3xl"
}: any) {
  if (!isSettingsOpen) return null;
  const t = themeColors;

  const [activeSection, setActiveSection] = useState<
    "profile" | "feed_curation" | "reels_media" | "comms" | "map_social" | "gamification" | "ringtones" | "privacy" | "hardware"
  >("profile");
  
  const [searchFilter, setSearchFilter] = useState("");
  const [isPlayingAudio, setIsPlayingAudio] = useState<string | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);

  // --- 🔥 NEW FEATURE: LIVE SENSOR READOUTS FOR HARDWARE CALIBRATION 🔥 ---
  const [liveAccel, setLiveAccel] = useState({ x: 0, y: 0, z: 0 });
  const [isCalibrating, setIsCalibrating] = useState(false);

  useEffect(() => {
    if (activeSection === "hardware" && isCalibrating) {
      const handleMotion = (event: DeviceMotionEvent) => {
        if (event.accelerationIncludingGravity) {
          setLiveAccel({
            x: event.accelerationIncludingGravity.x || 0,
            y: event.accelerationIncludingGravity.y || 0,
            z: event.accelerationIncludingGravity.z || 0
          });
        }
      };
      window.addEventListener('devicemotion', handleMotion);
      return () => window.removeEventListener('devicemotion', handleMotion);
    }
  }, [activeSection, isCalibrating]);

  // --- 🔥 NEW FEATURE: TARGETED CACHE CLEARING 🔥 ---
  const clearSpecificCache = (type: "maps" | "media" | "posts") => {
    if (typeof triggerHaptic === "function") triggerHaptic("HEAVY");
    if (window.confirm(`Are you sure you want to clear the local ${type} cache?`)) {
      if (type === "maps") localStorage.removeItem("rt_offline_map_cache");
      if (type === "media") localStorage.removeItem("soc_media_cache");
      if (type === "posts") localStorage.removeItem("soc_post_bookmarks");
      alert(`Local ${type} cache successfully purged.`);
    }
  };

  const playRingtonePreview = (toneId: string) => {
    if (typeof triggerHaptic === "function") triggerHaptic("MEDIUM");
    if (isPlayingAudio === toneId) {
      if (audioCtxRef.current) {
        audioCtxRef.current.close();
        audioCtxRef.current = null;
      }
      setIsPlayingAudio(null);
      return;
    }

    try {
      if (audioCtxRef.current) audioCtxRef.current.close();
      const ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
      audioCtxRef.current = ctx;
      setIsPlayingAudio(toneId);

      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      gain.connect(ctx.destination);
      osc.connect(gain);

      const now = ctx.currentTime;
      const vol = (Number(localStorage.getItem("soc_ringtone_vol") ?? 80)) / 100 * 0.25;

      switch (toneId) {
        case "tactical_dual":
          osc.type = "sine";
          osc.frequency.setValueAtTime(440, now);
          osc.frequency.setValueAtTime(480, now + 0.2);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.8);
          osc.start(now); osc.stop(now + 0.8);
          break;
        case "cyber_arpeggio":
          osc.type = "sawtooth";
          osc.frequency.setValueAtTime(520, now);
          osc.frequency.setValueAtTime(650, now + 0.15);
          osc.frequency.setValueAtTime(780, now + 0.3);
          osc.frequency.setValueAtTime(1040, now + 0.45);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.9);
          osc.start(now); osc.stop(now + 0.9);
          break;
        case "neon_glider":
          osc.type = "triangle";
          osc.frequency.setValueAtTime(300, now);
          osc.frequency.linearRampToValueAtTime(800, now + 0.4);
          osc.frequency.linearRampToValueAtTime(500, now + 0.7);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.8);
          osc.start(now); osc.stop(now + 0.8);
          break;
        case "sonar_pulse":
          osc.type = "sine";
          osc.frequency.setValueAtTime(1200, now);
          osc.frequency.exponentialRampToValueAtTime(300, now + 0.6);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.7);
          osc.start(now); osc.stop(now + 0.7);
          break;
        case "arcade_8bit":
          osc.type = "square";
          osc.frequency.setValueAtTime(220, now);
          osc.frequency.setValueAtTime(440, now + 0.1);
          osc.frequency.setValueAtTime(880, now + 0.2);
          gain.gain.setValueAtTime(vol * 0.7, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.5);
          osc.start(now); osc.stop(now + 0.5);
          break;
        case "emergency_siren":
          osc.type = "sawtooth";
          osc.frequency.setValueAtTime(800, now);
          osc.frequency.linearRampToValueAtTime(600, now + 0.3);
          osc.frequency.linearRampToValueAtTime(800, now + 0.6);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.9);
          osc.start(now); osc.stop(now + 0.9);
          break;
        case "voltage_spike":
          osc.type = "square";
          osc.frequency.setValueAtTime(100, now);
          osc.frequency.exponentialRampToValueAtTime(2000, now + 0.2);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.3);
          osc.start(now); osc.stop(now + 0.3);
          break;
        case "trail_echo":
          osc.type = "triangle";
          osc.frequency.setValueAtTime(600, now);
          osc.frequency.linearRampToValueAtTime(300, now + 0.4);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.8);
          osc.start(now); osc.stop(now + 0.8);
          break;
        case "digital_chirp":
          osc.type = "sine";
          osc.frequency.setValueAtTime(1500, now);
          osc.frequency.setValueAtTime(2000, now + 0.1);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.2);
          osc.start(now); osc.stop(now + 0.2);
          break;
        case "bass_drop":
          osc.type = "sine";
          osc.frequency.setValueAtTime(150, now);
          osc.frequency.exponentialRampToValueAtTime(20, now + 0.8);
          gain.gain.setValueAtTime(vol * 1.5, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.9);
          osc.start(now); osc.stop(now + 0.9);
          break;
        default:
          osc.type = "sine";
          osc.frequency.setValueAtTime(500, now);
          gain.gain.setValueAtTime(vol, now);
          gain.gain.exponentialRampToValueAtTime(0.001, now + 0.4);
          osc.start(now); osc.stop(now + 0.4);
          break;
      }
      setTimeout(() => setIsPlayingAudio(null), 1000);
    } catch (e) {
      setIsPlayingAudio(null);
    }
  };

  const [settingsMatrix, setSettingsMatrix] = useState<Record<string, any>>(() => {
    const defaults: Record<string, any> = {
      soc_profile_public: true,
      soc_show_social_links: true,
      soc_auto_sync_strava: false,
      soc_display_badge_flair: true,
      soc_link_youtube: "",
      soc_link_instagram: "",
      soc_link_strava: "",
      soc_link_tiktok: "",
      soc_link_discord: "",

      soc_default_feed: "Universal Feed",
      soc_sort_by_default: "newest",
      soc_compact_cards: false,
      soc_speed_trap_alerts: true,
      soc_high_contrast: false,
      soc_auto_translate: false,
      soc_slang_auto_highlight: true,
      soc_show_tag_suggestions: true,
      soc_hide_downvoted: false,
      soc_infinite_scroll: true,
      soc_show_post_views: true,
      soc_hide_marketplace_sold: true,
      soc_feed_filter_verified_only: false,
      soc_auto_expand_comments: false,
      soc_highlight_crew_posts: true,
      soc_sensitive_content_blur: true,

      soc_story_reels: true,
      soc_autoplay_reels: true,
      soc_loop_reels: true,
      soc_muted_reels_start: false,
      soc_allow_remix: true,
      soc_allow_duets: true,
      soc_show_speed_overlay: true,
      soc_show_elev_overlay: true,
      soc_hdr_reel_playback: true,
      soc_double_tap_volt: true,
      soc_blur_sensitive: false,
      soc_auto_embed_yt: true,
      soc_high_fps_reels: true,
      soc_watermark_reels: true,
      soc_crew_stories_first: true,
      soc_auto_save_drafts: true,
      soc_data_saver: false,
      soc_download_on_wifi_only: true,
      soc_save_original_media: false,
      soc_captions_auto_generate: true,

      soc_read_receipts: true,
      soc_typing_indicators: true,
      soc_voice_dms: true,
      soc_voice_transcription: true,
      soc_auto_decline: false,
      soc_reject_non_friends: false,
      soc_chat_bubble_style: "neon",
      soc_media_auto_expand: true,
      soc_quick_reaction_emojis: true,
      soc_direct_encryption_badge: true,
      soc_haptic_on_send: true,
      soc_p2p_mesh_priority: true,
      soc_offline_dm_queue: true,
      soc_auto_translate_dms: false,
      soc_dm_request_filter: true,
      soc_show_active_status: true,
      soc_dm_notification_preview: true,
      soc_sound_effects_in_chat: true,

      soc_show_meetup_pins: true,
      soc_show_photo_pins: true,
      soc_show_outlet_pins: true,
      soc_show_hazard_pins: true,
      soc_show_confession_pins: true,
      soc_pin_clustering: true,
      soc_pin_expiry_window: "24h",
      soc_auto_radius_filter: "50",
      soc_stealth_pin_drop: false,
      soc_pin_elevation_meters: true,
      soc_auto_group_rally_alert: true,
      soc_map_style: "carto_dark",
      soc_live_crew_tracking: true,
      soc_share_live_location: false,
      soc_map_traffic_layer: false,
      soc_notify_nearby_crew: true,
      soc_compass_heading_lock: true,

      soc_xp_progress_badge: true,
      soc_level_up_confetti: true,
      soc_neon_reaction_bursts: true,
      soc_trophy_export_watermark: true,
      soc_bounty_alerts: true,
      soc_streak_fire_badge: true,
      soc_leaderboard_anonymity: false,
      soc_volt_multiplier_alerts: true,
      soc_show_rider_rank_flair: true,
      soc_community_quest_notifications: true,
      soc_hide_xp_from_public: false,
      soc_auto_claim_bounties: true,
      soc_show_top_speed_medal: true,
      soc_show_trail_scout_medal: true,
      soc_daily_login_bonus: true,
      soc_rivalry_challenge_pings: true,

      soc_call_ringtone: "tactical_dual",
      soc_dm_chime: "neon_glider",
      soc_bounty_siren: "emergency_siren",
      soc_volt_sound: "arcade_8bit",
      soc_startup_chime: true,
      soc_ringtone_vol: "85",
      soc_vibrate_calls: true,
      soc_vibrate_dms: true,
      soc_speaker_default: true,
      soc_audio_ducking: true,
      soc_play_reaction_sounds: true,

      rt_privacy_mode: false,
      soc_post_privacy: "Public",
      soc_block_anonymous_comments: true,
      soc_require_approval_tags: true,
      soc_incognito_story_views: false,
      soc_disable_screenshots: false,
      soc_ai_mechanic: true,
      soc_ai_tone: "encouraging",
      soc_offline_mode: false,
      soc_analytics_opt_out: true,
      soc_hide_online_timestamp: false,
      soc_two_factor_auth: false
    };

    if (typeof window !== "undefined") {
      Object.keys(defaults).forEach((key) => {
        const stored = localStorage.getItem(key);
        if (stored !== null) {
          defaults[key] = stored === "true" ? true : stored === "false" ? false : stored;
        }
      });
    }
    return defaults;
  });

  const broadcastSync = (key: string, value: string) => {
    try {
      localStorage.setItem(key, value);
      window.dispatchEvent(new Event("settings-sync"));
      window.dispatchEvent(new Event("theme-sync"));
    } catch (e) {}
  };

  const updateSetting = (key: string, value: any) => {
    if (typeof triggerHaptic === "function") triggerHaptic("LIGHT");
    setSettingsMatrix((prev) => ({ ...prev, [key]: value }));
    broadcastSync(key, value.toString());
  };

  const RINGTONE_OPTIONS = [
    { id: "tactical_dual", name: "Tactical Dual-Tone", desc: "Avionics cockpit alert" },
    { id: "cyber_arpeggio", name: "Cyber Arpeggio", desc: "Synthwave frequency climb" },
    { id: "neon_glider", name: "Neon Glider", desc: "Smooth ambient pulse" },
    { id: "sonar_pulse", name: "Sonar Sub-Ping", desc: "Acoustic submarine rangefinder" },
    { id: "arcade_8bit", name: "8-Bit Arcade Jump", desc: "Retro telemetry bleep" },
    { id: "emergency_siren", name: "Emergency Siren", desc: "SOS urgent beacon alarm" },
    { id: "voltage_spike", name: "Voltage Spike", desc: "Fast electronic sweep" },
    { id: "trail_echo", name: "Trail Echo", desc: "Muffled radar bounce" },
    { id: "digital_chirp", name: "Digital Chirp", desc: "Classic high-pitch notify" },
    { id: "bass_drop", name: "Sub-Bass Drop", desc: "Heavy mechanical descent" }
  ];

  const CONFIG_ARRAYS: Record<string, any[]> = {
    feed_curation: [
      { key: "soc_compact_cards", title: "High-Density Post Cards", desc: "Reduce card padding for fast mobile timeline scanning" },
      { key: "soc_speed_trap_alerts", title: "Speed Trap Warnings", desc: "Flash high-priority warnings when speed traps are reported" },
      { key: "soc_high_contrast", title: "Outdoor High Contrast", desc: "Thicken borders for high-glare sunlight readability" },
      { key: "soc_auto_translate", title: "Auto-Translate Feed", desc: "Translate foreign language posts to system default" },
      { key: "soc_slang_auto_highlight", title: "PEV Slang Highlighting", desc: "Underline community jargon like VESC and BMS for quick lookup" },
      { key: "soc_show_tag_suggestions", title: "Pilot Tag Suggestions", desc: "Auto-suggest nearby riders when typing '@' in composer" },
      { key: "soc_hide_downvoted", title: "Hide Low-Rated Content", desc: "Suppress posts with overwhelmingly negative community feedback" },
      { key: "soc_infinite_scroll", title: "Infinite Feed Scroll", desc: "Continuously load historical posts instead of pagination" },
      { key: "soc_show_post_views", title: "Show Post View Counts", desc: "Display the number of times a transmission has been seen" },
      { key: "soc_hide_marketplace_sold", title: "Hide Sold Market Items", desc: "Automatically filter out items marked as sold from marketplace" },
      { key: "soc_feed_filter_verified_only", title: "Verified Pilots Priority", desc: "Display posts from verified and pro-tier riders at the top of feed" },
      { key: "soc_auto_expand_comments", title: "Auto-Expand Comment Previews", desc: "Open recent replies directly underneath transmissions without tapping" },
      { key: "soc_highlight_crew_posts", title: "Highlight Crew Broadcasts", desc: "Apply vibrant glowing borders around posts made by accepted crew" },
      { key: "soc_sensitive_content_blur", title: "Filter Graphic Media", desc: "Conceal rough wipeout and crash clips behind an interactive blur shield" }
    ],
    reels_media: [
      { key: "soc_story_reels", title: "Story Reels Tray", desc: "Display active pilot video and photo reels above feed" },
      { key: "soc_autoplay_reels", title: "Autoplay Reel Clips", desc: "Start video reels automatically while scrolling" },
      { key: "soc_loop_reels", title: "Infinite Reel Looping", desc: "Loop video clips continuously until dismissed" },
      { key: "soc_muted_reels_start", title: "Mute Reels on Launch", desc: "Always start story reel audio in muted state" },
      { key: "soc_allow_remix", title: "Allow Reel Remixes", desc: "Allow community pilots to duet or remix your clips" },
      { key: "soc_allow_duets", title: "Enable Split-Screen Duets", desc: "Allow others to record side-by-side video reactions" },
      { key: "soc_show_speed_overlay", title: "Overlay Speed Telemetry", desc: "Burn live GPS speed watermark onto exported reels" },
      { key: "soc_show_elev_overlay", title: "Overlay Elevation Contour", desc: "Display climb altitude telemetry on video replays" },
      { key: "soc_double_tap_volt", title: "Double-Tap to Volt", desc: "Quick-like post images by double-tapping photo container" },
      { key: "soc_blur_sensitive", title: "Blur Crash & Hazard Media", desc: "Add warning shield over flagged crash or trail danger photos" },
      { key: "soc_auto_embed_yt", title: "Embed YouTube Players", desc: "Render playable video boxes for pasted YouTube links" },
      { key: "soc_high_fps_reels", title: "60FPS Video Uploads", desc: "Preserve high framerate for action camera footage" },
      { key: "soc_watermark_reels", title: "Pilot Callsign Watermark", desc: "Attach verified handle to all shared clips" },
      { key: "soc_crew_stories_first", title: "Prioritize Crew Stories", desc: "Always display accepted friend reels before public posts" },
      { key: "soc_auto_save_drafts", title: "Auto-Save Media Drafts", desc: "Keep local copies of unposted images" },
      { key: "soc_data_saver", title: "Data Saver Mode", desc: "Compress and suppress high-res image downloads on cellular networks" },
      { key: "soc_download_on_wifi_only", title: "Pre-fetch on Wi-Fi Only", desc: "Save data by holding heavy video downloads until Wi-Fi connects" },
      { key: "soc_save_original_media", title: "Save Sent Media to Device", desc: "Preserve original uploaded videos and pictures to phone gallery" },
      { key: "soc_captions_auto_generate", title: "Auto-Generate Reel Captions", desc: "Transcribe spoken audio in action camera clips into animated text subtitles" }
    ],
    comms: [
      { key: "soc_read_receipts", title: "Read Receipts in DMs", desc: "Send double checkmarks when messages are opened" },
      { key: "soc_typing_indicators", title: "Live Typing Beacon", desc: "Broadcast typing status while crafting direct messages" },
      { key: "soc_voice_dms", title: "Voice Notes in Chat", desc: "Enable microphone audio note recording in conversations" },
      { key: "soc_voice_transcription", title: "AI Voice Note Transcribe", desc: "Auto-convert received voice notes to text summaries" },
      { key: "soc_auto_decline", title: "Auto-Decline Hails", desc: "Reject all incoming voice & video hails automatically" },
      { key: "soc_reject_non_friends", title: "Restrict Hails to Crew", desc: "Only allow calls from riders in your friends list" },
      { key: "soc_quick_reaction_emojis", title: "Quick Emoji Reactions", desc: "Display instant sticker ribbon inside chat threads" },
      { key: "soc_direct_encryption_badge", title: "Show Encryption Badge", desc: "Display P2P end-to-end security tag on chat headers" },
      { key: "soc_haptic_on_send", title: "Haptic Send Feedback", desc: "Vibrate on message transmission confirmation" },
      { key: "soc_offline_dm_queue", title: "Offline Message Queue", desc: "Store unsent messages locally until cell service returns" },
      { key: "soc_auto_translate_dms", title: "Live Chat Translation", desc: "Auto-translate foreign language incoming messages" },
      { key: "soc_dm_request_filter", title: "Message Request Filter", desc: "Send unknown riders to a separate request folder" },
      { key: "soc_show_active_status", title: "Broadcast Active Status", desc: "Show green dot when you are currently online" },
      { key: "soc_p2p_mesh_priority", title: "P2P Mesh Network Routing", desc: "Attempt peer-to-peer comms bypass before cell towers" },
      { key: "soc_dm_notification_preview", title: "Lockscreen Message Previews", desc: "Display sender message text inside push notifications" },
      { key: "soc_sound_effects_in_chat", title: "Interactive Chat FX Audio", desc: "Play subtle acoustic clicks upon message bubble dispatch" }
    ],
    map_social: [
      { key: "soc_show_meetup_pins", title: "Show Meetup Spot Pins", desc: "Display community ride staging points on map" },
      { key: "soc_show_photo_pins", title: "Show Photo Drop Pins", desc: "Display scenic overlook and photography landmarks" },
      { key: "soc_show_outlet_pins", title: "Show Charging Outlets", desc: "Display public 120V charging points verified by riders" },
      { key: "soc_show_hazard_pins", title: "Show Hazard Alerts", desc: "Display potholes, mud hazards, and washed-out trails" },
      { key: "soc_show_confession_pins", title: "Show Trail Confession Pins", desc: "Display humorous rider wipeouts and blooper coordinates" },
      { key: "soc_pin_clustering", title: "Map Pin Clustering", desc: "Group nearby pins into single numbers when zoomed out" },
      { key: "soc_pin_expiry_window", title: "24-Hour Pin Expiration", desc: "Automatically remove temporary staging points from map layer after one day" },
      { key: "soc_auto_group_rally_alert", title: "Group Ride Rally Pings", desc: "Alert when a crew member drops a live pit stop pin" },
      { key: "soc_stealth_pin_drop", title: "Anonymous Pin Drops", desc: "Hide creator callsign when broadcasting map pins" },
      { key: "soc_pin_elevation_meters", title: "Show Pin Elevation", desc: "Display altitude data on dropped coordinates" },
      { key: "soc_live_crew_tracking", title: "Live Crew Map Tracking", desc: "Show real-time movements of accepted friends on radar" },
      { key: "soc_share_live_location", title: "Broadcast Live Location", desc: "Allow friends to see your position on the map" },
      { key: "soc_map_traffic_layer", title: "Live Traffic Layer", desc: "Overlay road congestion data on the street map" },
      { key: "soc_notify_nearby_crew", title: "Proximity Crew Radar Alerts", desc: "Trigger banner notice whenever a squad pilot enters a 5-mile radius" },
      { key: "soc_compass_heading_lock", title: "Map Orientation Follows Heading", desc: "Rotate community leaflet viewport toward direction of travel" }
    ],
    gamification: [
      { key: "soc_xp_progress_badge", title: "Rider XP & Level Badge", desc: "Display Level progress bar prominently in feed header" },
      { key: "soc_neon_reaction_bursts", title: "Neon Particle Bursts", desc: "Explode neon particles on screen when Volting posts" },
      { key: "soc_level_up_confetti", title: "Level-Up Confetti", desc: "Trigger visual celebrations on rank milestones" },
      { key: "soc_bounty_alerts", title: "Weekly Bounty Banners", desc: "Show active photo and ride bounty challenges" },
      { key: "soc_trophy_export_watermark", title: "Trophy Watermarks", desc: "Add verified telemetry stamp to speed flex cards" },
      { key: "soc_streak_fire_badge", title: "Daily Streak Badges", desc: "Display flame streak counter next to pilot callsign" },
      { key: "soc_volt_multiplier_alerts", title: "Volt Multiplier Pops", desc: "Display +2x animation when boosting active crew members" },
      { key: "soc_leaderboard_anonymity", title: "Anonymous Leaderboards", desc: "Hide your callsign from global speed sector rankings" },
      { key: "soc_show_rider_rank_flair", title: "Display Rank Flair", desc: "Show community titles on posts and profiles" },
      { key: "soc_community_quest_notifications", title: "Quest Notifications", desc: "Ping me when community mile goals are reached" },
      { key: "soc_hide_xp_from_public", title: "Hide XP From Public", desc: "Keep your total Volts and Level private from other riders" },
      { key: "soc_auto_claim_bounties", title: "Auto-Claim Bounties", desc: "Automatically deposit XP when a hashtag criteria is met" },
      { key: "soc_show_top_speed_medal", title: "Top Speed Profile Medal", desc: "Display a gold badge on your profile for holding a sector record" },
      { key: "soc_show_trail_scout_medal", title: "Trail Pioneer Medal", desc: "Display scout crest for logging 10+ verified trail reports" },
      { key: "soc_daily_login_bonus", title: "Daily Check-In Bounty Claims", desc: "Automatically register daily participation XP upon first platform launch" },
      { key: "soc_rivalry_challenge_pings", title: "Sector Rivalry Alerts", desc: "Notify immediately when an operator attempts to beat your logged leaderboard times" }
    ],
    privacy: [
      { key: "rt_privacy_mode", title: "Stealth / Incognito Mode", desc: "Conceal GPS coordinates and active typing beacons from public posts" },
      { key: "soc_post_privacy", title: "Default Broadcast Visibility", desc: "Restrict all newly composed transmissions exclusively to accepted crew" },
      { key: "soc_block_anonymous_comments", title: "Block Anonymous Replies", desc: "Only allow verified riders to comment on your broadcasts" },
      { key: "soc_require_approval_tags", title: "Require Tag Approval", desc: "Review before allowing someone to tag you in a Group Ride" },
      { key: "soc_incognito_story_views", title: "Incognito Reel Views", desc: "Watch other pilots' story reels without showing up in their viewer list" },
      { key: "soc_disable_screenshots", title: "Block App Screenshots", desc: "Trigger warning if someone screenshots your DMs (Mobile Native)" },
      { key: "soc_analytics_opt_out", title: "Opt-Out of App Analytics", desc: "Do not send crash reports or UI telemetry to the developer" },
      { key: "soc_hide_online_timestamp", title: "Conceal Last Active Timestamp", desc: "Prevent other pilots from inspecting the exact time of your last session" },
      { key: "soc_two_factor_auth", title: "Biometric Login Verification", desc: "Require fingerprint or face ID to open sensitive comms vault" }
    ]
  };

  const renderToggleList = (items: any[]) => (
    <div className="space-y-3 pb-8">
      {items.filter(item => item.title.toLowerCase().includes(searchFilter.toLowerCase()) || item.desc.toLowerCase().includes(searchFilter.toLowerCase())).map((item, idx) => (
        <div key={item.key} className="flex items-center justify-between p-4 bg-white/5 border border-white/10 rounded-2xl gap-3">
          <div className="min-w-0 pr-2">
            <span className="text-xs text-white font-black uppercase tracking-widest block truncate">{idx + 1}. {item.title}</span>
            <p className="text-[10px] text-zinc-400 font-medium leading-relaxed mt-0.5">{item.desc}</p>
          </div>
          <button 
            type="button" 
            onClick={() => updateSetting(item.key, !settingsMatrix[item.key])}
            className={`relative inline-flex min-h-[32px] min-w-[56px] items-center rounded-full transition-colors shrink-0 cursor-pointer ${settingsMatrix[item.key] ? t.bg : "bg-zinc-800"}`}
          >
            <span className={`inline-block h-6 w-6 transform rounded-full bg-white transition shadow-sm ${settingsMatrix[item.key] ? "translate-x-7 bg-black" : "translate-x-1"}`} />
          </button>
        </div>
      ))}
    </div>
  );

  return (
    <div className="fixed inset-0 z-[999999] bg-black/90 backdrop-blur-2xl flex flex-col justify-between pt-16 pb-20 sm:p-6 sm:justify-center sm:items-center">
      <motion.div 
        initial={{ scale: 0.95, opacity: 0 }} 
        animate={{ scale: 1, opacity: 1 }} 
        exit={{ scale: 0.95, opacity: 0 }} 
        transition={{ type: "spring", damping: 25, stiffness: 200 }}
        className={`app-card ${glassBlurClass} ${cornerStyle} bg-zinc-950 border border-white/15 w-full max-w-2xl h-full max-h-[85vh] flex flex-col overflow-hidden shadow-2xl relative rounded-3xl`}
      >
        {/* HEADER */}
        <div className="flex justify-between items-center p-4 sm:p-5 border-b border-white/10 shrink-0 bg-white/5">
          <div className="flex items-center gap-3 min-w-0 pr-2">
            <div className={`p-2.5 rounded-2xl ${t.bg} text-black font-black shadow-md shrink-0`}>
              <Settings className="w-5 h-5"/>
            </div>
            <div className="min-w-0">
              <h3 className="text-xs sm:text-sm font-black uppercase tracking-widest text-white truncate flex items-center gap-2">
                Social Community Settings
              </h3>
              <span className="text-[9px] sm:text-[10px] font-mono text-emerald-400 uppercase tracking-wider block mt-0.5 truncate">
                ● 100% Social Media &amp; Curation Matrix Live
              </span>
            </div>
          </div>
          <button 
            type="button" 
            onClick={() => setIsSettingsOpen(false)} 
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
            placeholder="Search social settings, ringtones, curation..."
            value={searchFilter}
            onChange={(e) => setSearchFilter(e.target.value)}
            className="w-full bg-transparent text-xs font-bold text-white outline-none placeholder:text-zinc-600 min-h-[32px]"
          />
          {searchFilter && (
            <button onClick={() => setSearchFilter("")} className="text-xs text-zinc-500 hover:text-white shrink-0 px-2 py-1">Clear</button>
          )}
        </div>

        {/* TABS NAVIGATION */}
        <div className="flex border-b border-white/10 bg-black/60 px-4 py-2.5 gap-2 overflow-x-auto custom-scrollbar shrink-0">
          {[
            { id: "profile", label: "Profile & Bio", icon: User },
            { id: "hardware", label: "Sensors & Cache", icon: Activity }, // 🔥 NEW HARDWARE TAB
            { id: "feed_curation", label: "Feed Curation", icon: Sliders },
            { id: "reels_media", label: "Reels & Stories", icon: Video },
            { id: "comms", label: "Crew DMs & Calls", icon: MessageCircle },
            { id: "map_social", label: "Social Map", icon: MapPin },
            { id: "gamification", label: "Bounties & XP", icon: Trophy },
            { id: "ringtones", label: "Ringtones & Chimes", icon: Music },
            { id: "privacy", label: "Privacy & Sec", icon: Shield }
          ].map(tab => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => {
                  if (typeof triggerHaptic === "function") triggerHaptic("LIGHT");
                  setActiveSection(tab.id as any);
                }}
                className={`min-h-[38px] px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-wider transition-all cursor-pointer flex items-center gap-1.5 shrink-0 whitespace-nowrap shadow-sm ${
                  activeSection === tab.id ? `${t.bg} text-black font-black shadow-md` : "text-zinc-400 hover:text-white bg-white/5"
                }`}
              >
                <Icon className="w-3.5 h-3.5"/> {tab.label}
              </button>
            );
          })}
        </div>

        {/* SETTINGS SCROLLER */}
        <div className="flex-1 overflow-y-auto custom-scrollbar p-4 sm:p-6 space-y-6">

          {/* TAB: PROFILE & BIO */}
          {activeSection === "profile" && (
            <div className="space-y-5 pb-6">
              <div className="flex items-center gap-4 bg-white/5 border border-white/10 p-4 rounded-2xl">
                <div className={`w-16 h-16 rounded-full border-2 ${t.border} bg-zinc-900 flex items-center justify-center text-2xl font-black text-white overflow-hidden shadow-xl shrink-0`}>
                  {pfpUrl ? <img src={pfpUrl} alt={username} className="w-full h-full object-cover"/> : username.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <label className="text-[10px] text-zinc-400 font-black uppercase tracking-widest block mb-1.5">Avatar Uplink</label>
                  <input type="file" accept="image/*" ref={pfpInputRef} onChange={handlePfpUpload} className="hidden" />
                  <button type="button" onClick={() => { if (typeof triggerHaptic === 'function') triggerHaptic("LIGHT"); pfpInputRef.current?.click(); }} className="min-h-[44px] px-4 py-2.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-xl text-[10px] font-black uppercase text-white transition-colors cursor-pointer shadow-inner active:scale-95">Change Profile Picture</button>
                </div>
              </div>

              <div>
                <label className="text-[10px] text-zinc-400 font-black uppercase tracking-widest block mb-1.5 flex justify-between">
                  <span>Pilot Callsign</span>
                  <span className="text-rose-500 flex items-center gap-1"><Lock className="w-2.5 h-2.5"/> Locked ID</span>
                </label>
                <input type="text" value={username} disabled={true} className="w-full min-h-[44px] bg-black/80 border border-white/10 rounded-xl px-4 py-3 text-xs font-bold text-zinc-500 shadow-inner cursor-not-allowed" />
              </div>

              <div>
                <label className="text-[10px] text-zinc-400 font-black uppercase tracking-widest block mb-1.5">Fleet Signature / Vehicle Tag</label>
                <input type="text" value={userFleet} onChange={(e) => { setUserFleet(e.target.value); broadcastSync("rural_erides_fleet", e.target.value); }} placeholder="e.g. Dual Motor E-Scooter, Electric Unicycle..." className="w-full min-h-[44px] bg-black/60 border border-white/10 rounded-xl px-4 py-3 text-xs font-bold text-white focus:outline-none focus:border-white/40 transition-colors shadow-inner" />
              </div>

              <div>
                <label className="text-[10px] text-zinc-400 font-black uppercase tracking-widest block mb-1.5">Default Sector Location</label>
                <input type="text" value={settingsMatrix.soc_default_loc || "Global Grid"} onChange={(e) => updateSetting("soc_default_loc", e.target.value)} placeholder="e.g. Downtown Metro" className="w-full min-h-[44px] bg-black/60 border border-white/10 rounded-xl px-4 py-3 text-xs font-bold text-white focus:outline-none focus:border-white/40 transition-colors shadow-inner" />
              </div>

              <div>
                <label className="text-[10px] text-zinc-400 font-black uppercase tracking-widest block mb-1.5">Pilot Bio / Riding Philosophy</label>
                <textarea value={userBio} onChange={(e) => { setUserBio(e.target.value); broadcastSync("rural_erides_bio", e.target.value); }} placeholder="Tell the crew about your setup..." rows={3} className="w-full bg-black/60 border border-white/10 rounded-xl px-4 py-3 text-xs font-bold text-white focus:outline-none focus:border-white/40 transition-colors shadow-inner resize-none leading-relaxed" />
              </div>

              <div className="pt-2 border-t border-white/10 space-y-3">
                <span className="text-[11px] font-black uppercase tracking-widest text-zinc-300 block">External Social Media Uplinks</span>
                
                <div>
                  <label className="text-[9px] text-zinc-400 font-bold uppercase tracking-wider block mb-1">YouTube Channel URL</label>
                  <input type="text" value={settingsMatrix.soc_link_youtube || ""} onChange={(e) => updateSetting("soc_link_youtube", e.target.value)} placeholder="https://youtube.com/@channel" className="w-full min-h-[40px] bg-black/60 border border-white/10 rounded-xl px-4 py-2 text-xs font-bold text-white focus:outline-none focus:border-white/40 transition-colors" />
                </div>

                <div>
                  <label className="text-[9px] text-zinc-400 font-bold uppercase tracking-wider block mb-1">Instagram Profile URL</label>
                  <input type="text" value={settingsMatrix.soc_link_instagram || ""} onChange={(e) => updateSetting("soc_link_instagram", e.target.value)} placeholder="https://instagram.com/handle" className="w-full min-h-[40px] bg-black/60 border border-white/10 rounded-xl px-4 py-2 text-xs font-bold text-white focus:outline-none focus:border-white/40 transition-colors" />
                </div>

                <div>
                  <label className="text-[9px] text-zinc-400 font-bold uppercase tracking-wider block mb-1">Strava Profile URL</label>
                  <input type="text" value={settingsMatrix.soc_link_strava || ""} onChange={(e) => updateSetting("soc_link_strava", e.target.value)} placeholder="https://strava.com/athletes/id" className="w-full min-h-[40px] bg-black/60 border border-white/10 rounded-xl px-4 py-2 text-xs font-bold text-white focus:outline-none focus:border-white/40 transition-colors" />
                </div>

                <div>
                  <label className="text-[9px] text-zinc-400 font-bold uppercase tracking-wider block mb-1">TikTok Profile URL</label>
                  <input type="text" value={settingsMatrix.soc_link_tiktok || ""} onChange={(e) => updateSetting("soc_link_tiktok", e.target.value)} placeholder="https://tiktok.com/@handle" className="w-full min-h-[40px] bg-black/60 border border-white/10 rounded-xl px-4 py-2 text-xs font-bold text-white focus:outline-none focus:border-white/40 transition-colors" />
                </div>

                <div>
                  <label className="text-[9px] text-zinc-400 font-bold uppercase tracking-wider block mb-1">Discord Community Handle</label>
                  <input type="text" value={settingsMatrix.soc_link_discord || ""} onChange={(e) => updateSetting("soc_link_discord", e.target.value)} placeholder="username#0000" className="w-full min-h-[40px] bg-black/60 border border-white/10 rounded-xl px-4 py-2 text-xs font-bold text-white focus:outline-none focus:border-white/40 transition-colors" />
                </div>
              </div>
            </div>
          )}

          {/* --- 🔥 NEW TAB: HARDWARE SENSORS & CACHE SWEEPING 🔥 --- */}
          {activeSection === "hardware" && (
            <div className="space-y-5 pb-6">
              
              <div className="bg-cyan-950/20 border border-cyan-500/30 p-4 rounded-2xl shadow-inner space-y-3">
                <span className="text-xs text-cyan-300 font-black uppercase tracking-widest flex items-center gap-2 mb-1">
                  <Activity className="w-4 h-4"/> Hardware Sensor Calibration Array
                </span>
                <p className="text-[10px] text-zinc-400 font-medium leading-relaxed">
                  Temporarily activate your device's internal gyroscope and accelerometer to verify native sensor health before relying on telemetry during a high-speed run.
                </p>
                <div className="bg-black/60 border border-white/10 p-4 rounded-xl font-mono text-xs text-cyan-400 grid grid-cols-3 gap-2 text-center shadow-inner">
                   <div><span className="block text-[9px] text-zinc-500 uppercase mb-1">X-Axis</span>{liveAccel.x.toFixed(2)}</div>
                   <div><span className="block text-[9px] text-zinc-500 uppercase mb-1">Y-Axis</span>{liveAccel.y.toFixed(2)}</div>
                   <div><span className="block text-[9px] text-zinc-500 uppercase mb-1">Z-Axis</span>{liveAccel.z.toFixed(2)}</div>
                </div>
                <button 
                  onClick={() => setIsCalibrating(!isCalibrating)}
                  className={`w-full py-3 rounded-xl font-black uppercase tracking-widest text-[10px] transition-all active:scale-95 shadow-lg ${isCalibrating ? 'bg-rose-600 text-white hover:bg-rose-500' : 'bg-cyan-600 text-black hover:bg-cyan-500'}`}
                >
                  {isCalibrating ? "Halt Sensor Polling" : "Initiate Live Sensor Readout"}
                </button>
              </div>

              <div className="space-y-3 pt-2">
                 <span className="text-[11px] font-black uppercase tracking-widest text-rose-400 block flex items-center gap-1.5"><Trash2 className="w-3.5 h-3.5"/> Targeted Local Cache Sweeps</span>
                 <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div className="bg-white/5 border border-white/10 p-4 rounded-2xl shadow-inner flex flex-col justify-between">
                       <span className="text-xs font-black text-white uppercase tracking-wider block mb-1">Offline Maps</span>
                       <span className="text-[9px] text-zinc-400 font-medium block mb-3">Clear cached Leaflet map tiles and routing data.</span>
                       <button onClick={() => clearSpecificCache("maps")} className="w-full py-2 bg-rose-950/60 hover:bg-rose-900 border border-rose-500/40 text-rose-400 rounded-xl font-bold uppercase text-[9px] tracking-wider transition-colors active:scale-95">Purge Maps Cache</button>
                    </div>
                    <div className="bg-white/5 border border-white/10 p-4 rounded-2xl shadow-inner flex flex-col justify-between">
                       <span className="text-xs font-black text-white uppercase tracking-wider block mb-1">Saved Media</span>
                       <span className="text-[9px] text-zinc-400 font-medium block mb-3">Clear compressed images and story reel remnants.</span>
                       <button onClick={() => clearSpecificCache("media")} className="w-full py-2 bg-rose-950/60 hover:bg-rose-900 border border-rose-500/40 text-rose-400 rounded-xl font-bold uppercase text-[9px] tracking-wider transition-colors active:scale-95">Purge Media Cache</button>
                    </div>
                    <div className="bg-white/5 border border-white/10 p-4 rounded-2xl shadow-inner flex flex-col justify-between sm:col-span-2">
                       <span className="text-xs font-black text-white uppercase tracking-wider block mb-1">Bookmarked Posts & Prayers</span>
                       <span className="text-[9px] text-zinc-400 font-medium block mb-3">Clear all saved vault transmissions and the local AI prayer journal.</span>
                       <button onClick={() => clearSpecificCache("posts")} className="w-full py-2 bg-rose-950/60 hover:bg-rose-900 border border-rose-500/40 text-rose-400 rounded-xl font-bold uppercase text-[9px] tracking-wider transition-colors active:scale-95">Purge Vault Cache</button>
                    </div>
                 </div>
              </div>
            </div>
          )}

          {/* TAB: RINGTONES & AUDIO CHIMES */}
          {activeSection === "ringtones" && (
            <div className="space-y-5 pb-6">
              <div className="bg-purple-950/20 border border-purple-500/30 p-4 rounded-2xl">
                <span className="text-xs text-purple-300 font-black uppercase tracking-widest flex items-center gap-2 mb-1">
                  <Volume2 className="w-4 h-4"/> Procedural Native Web Audio Synthesizer
                </span>
                <p className="text-[10px] text-zinc-400 font-medium">
                  Ringtones and alerts are rendered in real-time by the device sound card—zero network lag, 100% offline compatibility.
                </p>
              </div>

              <div className="space-y-3">
                <label className="text-[10px] text-zinc-400 font-black uppercase tracking-widest block">
                  Select Incoming Call Ringtone
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {RINGTONE_OPTIONS.filter(tone => tone.name.toLowerCase().includes(searchFilter.toLowerCase())).map(tone => (
                    <div 
                      key={tone.id}
                      onClick={() => updateSetting("soc_call_ringtone", tone.id)}
                      className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                        settingsMatrix.soc_call_ringtone === tone.id ? `${t.bgSubtle} ${t.borderSubtle}` : "bg-white/5 border-white/10 hover:border-white/20"
                      }`}
                    >
                      <div className="min-w-0 pr-2">
                        <span className="text-xs font-black text-white block truncate">{tone.name}</span>
                        <span className="text-[9px] text-zinc-500 font-medium block truncate mt-0.5">{tone.desc}</span>
                      </div>
                      <button
                        type="button"
                        onClick={(e) => { e.stopPropagation(); playRingtonePreview(tone.id); }}
                        className={`min-h-[40px] min-w-[40px] p-2.5 rounded-xl transition-transform active:scale-90 shrink-0 flex items-center justify-center cursor-pointer ${
                          isPlayingAudio === tone.id ? "bg-rose-600 text-white shadow-lg" : "bg-white/10 hover:bg-white/20 text-zinc-200"
                        }`}
                        title="Audition Tone"
                      >
                        {isPlayingAudio === tone.id ? <Square className="w-3.5 h-3.5"/> : <Play className="w-3.5 h-3.5"/>}
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              <div className="space-y-3 pt-2 border-t border-white/10">
                <div>
                  <div className="flex justify-between items-center mb-1.5">
                    <span className="text-xs text-white font-black uppercase tracking-widest">Ringtone &amp; Alert Volume</span>
                    <span className={`text-xs font-mono font-black ${t.text}`}>{settingsMatrix.soc_ringtone_vol}%</span>
                  </div>
                  <input 
                    type="range" 
                    min="0" 
                    max="100" 
                    value={settingsMatrix.soc_ringtone_vol} 
                    onChange={(e) => updateSetting("soc_ringtone_vol", e.target.value)}
                    className="w-full h-3 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-emerald-500"
                  />
                </div>

                <div className="flex items-center justify-between p-4 bg-white/5 border border-white/10 rounded-2xl gap-3">
                  <div>
                    <span className="text-xs text-white font-black uppercase tracking-widest block">Vibrate On Incoming Calls</span>
                    <p className="text-[10px] text-zinc-400 font-medium mt-0.5">Trigger mechanical haptic motors on incoming calls</p>
                  </div>
                  <button 
                    type="button" 
                    onClick={() => updateSetting("soc_vibrate_calls", !settingsMatrix.soc_vibrate_calls)}
                    className={`relative inline-flex min-h-[32px] min-w-[56px] items-center rounded-full transition-colors cursor-pointer ${settingsMatrix.soc_vibrate_calls ? t.bg : "bg-zinc-800"}`}
                  >
                    <span className={`inline-block h-6 w-6 transform rounded-full bg-white transition shadow-sm ${settingsMatrix.soc_vibrate_calls ? "translate-x-7 bg-black" : "translate-x-1"}`} />
                  </button>
                </div>

                <div className="flex items-center justify-between p-4 bg-white/5 border border-white/10 rounded-2xl gap-3">
                  <div>
                    <span className="text-xs text-white font-black uppercase tracking-widest block">Audio Ducking During Alerts</span>
                    <p className="text-[10px] text-zinc-400 font-medium mt-0.5">Temporarily reduce media volume while emergency chimes are active</p>
                  </div>
                  <button 
                    type="button" 
                    onClick={() => updateSetting("soc_audio_ducking", !settingsMatrix.soc_audio_ducking)}
                    className={`relative inline-flex min-h-[32px] min-w-[56px] items-center rounded-full transition-colors cursor-pointer ${settingsMatrix.soc_audio_ducking ? t.bg : "bg-zinc-800"}`}
                  >
                    <span className={`inline-block h-6 w-6 transform rounded-full bg-white transition shadow-sm ${settingsMatrix.soc_audio_ducking ? "translate-x-7 bg-black" : "translate-x-1"}`} />
                  </button>
                </div>
              </div>
            </div>
          )}

          {activeSection === "feed_curation" && renderToggleList(CONFIG_ARRAYS.feed_curation)}
          {activeSection === "reels_media" && renderToggleList(CONFIG_ARRAYS.reels_media)}
          {activeSection === "comms" && renderToggleList(CONFIG_ARRAYS.comms)}
          {activeSection === "map_social" && renderToggleList(CONFIG_ARRAYS.map_social)}
          {activeSection === "gamification" && renderToggleList(CONFIG_ARRAYS.gamification)}
          {activeSection === "privacy" && renderToggleList(CONFIG_ARRAYS.privacy)}

        </div>

        {/* FOOTER CONTROLS */}
        <div className="shrink-0 p-4 border-t border-white/10 bg-zinc-950/95 flex gap-3">
          <button 
            type="button" 
            onClick={() => {
              if (typeof triggerHaptic === "function") triggerHaptic("HEAVY");
              if (window.confirm("Reset all social media matrix preferences back to defaults?")) {
                localStorage.clear();
                window.location.reload();
              }
            }}
            className="px-4 min-h-[48px] bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-zinc-400 hover:text-white font-black uppercase tracking-wider text-xs transition-colors cursor-pointer active:scale-95 flex items-center justify-center gap-1.5 shrink-0"
          >
            <RotateCcw className="w-4 h-4" /> Reset
          </button>
          <button 
            type="button" 
            onClick={() => { 
              setIsSettingsOpen(false); 
              if (typeof triggerHaptic === 'function') triggerHaptic("HEAVY"); 
            }} 
            className={`flex-1 min-h-[48px] ${t.bg} text-black rounded-2xl font-black uppercase tracking-widest text-xs shadow-xl cursor-pointer transition-all active:scale-95 flex items-center justify-center gap-2`}
          >
            <CheckCircle2 className="w-4 h-4"/> Save Matrix Profile
          </button>
        </div>
      </motion.div>
    </div>
  );
}