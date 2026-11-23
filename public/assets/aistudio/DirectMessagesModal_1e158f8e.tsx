"use client";

import React, { useState, useEffect, useRef } from "react";
import { 
  X, Send, Search, Mail, Shield, Check, CheckCheck, User, 
  Sparkles, Image as ImageIcon, Smile, Phone, PhoneOff, PhoneIncoming, 
  Video, VideoOff, Mic, MicOff, Maximize2, Minimize2, 
  SwitchCamera, Radio, Volume2, ShieldCheck, AlertCircle,
  Paperclip, MapPin, PlusCircle, MoreVertical, Ban, BellOff, 
  MessageSquare, Clock, MonitorUp, Wifi, Activity, Pin, Lock, Globe, Play, Square, ShieldAlert
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  collection, addDoc, query, onSnapshot, doc, updateDoc, 
  serverTimestamp, setDoc, getDoc, deleteDoc, where 
} from "firebase/firestore";
import { db } from "../../services/firebase";

import { Capacitor } from '@capacitor/core';
import { LocalNotifications, ActionPerformed } from '@capacitor/local-notifications';
import { getGeminiApiKey } from "../../services/CoPilotService";

const RTC_CONFIG: RTCConfiguration = { 
  iceServers: [{ urls: ["stun:stun.l.google.com:19302"] }], 
  iceCandidatePoolSize: 10 
};

const compressImage = (file: File, maxWidth = 800, maxHeight = 800, quality = 0.7): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (event) => {
      const img = new Image();
      img.src = event.target?.result as string;
      img.onload = () => {
        let width = img.width;
        let height = img.height;
        if (width > height) {
          if (width > maxWidth) {
            height = Math.round((height * maxWidth) / width);
            width = maxWidth;
          }
        } else {
          if (height > maxHeight) {
            width = Math.round((height * maxHeight) / height);
            height = maxHeight;
          }
        }
        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          reject(new Error("Canvas context failed"));
          return;
        }
        ctx.drawImage(img, 0, 0, width, height);
        resolve(canvas.toDataURL("image/jpeg", quality));
      };
      img.onerror = reject;
    };
    reader.onerror = reject;
  });
};

export default function DirectMessagesModal({
  showInbox, setShowInbox, activeDMUser, setActiveDMUser, username, pfpUrl = "",
  userProfiles = {}, newChatTarget, setNewChatTarget, inboxSearch, setInboxSearch,
  chatList = [], dms = [], themeColors: t, getTimeMs, glassBlurClass = "backdrop-blur-2xl", cornerStyle = "rounded-3xl", onStartCall
}: any) {
  const [messageText, setMessageText] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [showAttachmentMenu, setShowAttachmentMenu] = useState(false);
  const chatScrollRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [isTyping, setIsTyping] = useState(false);
  const [destructTimer, setDestructTimer] = useState<"off"|"1h"|"24h">("off");
  const [chatBg, setChatBg] = useState<"dark"|"matrix"|"topo">("dark");
  const [showThreadSettings, setShowThreadSettings] = useState(false);
  const [isTranslating, setIsTranslating] = useState(false);
  const [translations, setTranslations] = useState<Record<string, string>>({});
  const [isMuted, setIsMuted] = useState(false);
  const [toastAlert, setToastAlert] = useState<string | null>(null);

  const [blockedUsers, setBlockedUsers] = useState<string[]>(() => {
    try {
      return JSON.parse(localStorage.getItem("soc_blocked_users") || "[]");
    } catch {
      return [];
    }
  });

  const [e2eHash, setE2eHash] = useState("0xSECURE");
  const [showFingerprintModal, setShowFingerprintModal] = useState(false);

  const [isRecordingVoice, setIsRecordingVoice] = useState(false);
  const [recordingDuration, setRecordingDuration] = useState(0);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const recordingTimerRef = useRef<any>(null);

  // --- WEBRTC CALL & NETWORK MONITORING STATES ---
  const [callState, setCallState] = useState<"idle" | "outgoing_ringing" | "incoming_ringing" | "connected">("idle");
  const [callType, setCallType] = useState<"audio" | "video">("video");
  const [activeCallId, setActiveCallId] = useState<string | null>(null);
  const [incomingCallData, setIncomingCallData] = useState<any>(null);
  const [isMicMuted, setIsMicMuted] = useState(false);
  const [isVideoDisabled, setIsVideoDisabled] = useState(false);
  const [isSpeakerOn, setIsSpeakerOn] = useState(true);
  const [isScreenSharing, setIsScreenSharing] = useState(false);
  const [isLowDataMode, setIsLowDataMode] = useState(false);
  const [showInCallChat, setShowInCallChat] = useState(false);
  const [callDuration, setCallDuration] = useState(0);
  const [callError, setCallError] = useState<string | null>(null);
  const [cameraFacingMode, setCameraFacingMode] = useState<"user"|"environment">("user");
  const [connectionQuality, setConnectionQuality] = useState<"Optimal P2P" | "Stable Stream" | "High Latency">("Optimal P2P");

  const peerConnectionRef = useRef<RTCPeerConnection | null>(null);
  const localStreamRef = useRef<MediaStream | null>(null);
  const remoteStreamRef = useRef<MediaStream | null>(null);
  const localVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteAudioRef = useRef<HTMLAudioElement | null>(null);
  const timerIntervalRef = useRef<any>(null);
  const statsIntervalRef = useRef<any>(null);

  const showToast = (msg: string) => {
    setToastAlert(msg);
    setTimeout(() => setToastAlert(null), 3000);
  };

  useEffect(() => { 
    if (activeDMUser && chatScrollRef.current) {
      chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight; 
    }
  }, [activeDMUser, dms]);

  useEffect(() => {
    if (username && activeDMUser) {
      const pair = [username, activeDMUser].sort().join("::");
      crypto.subtle.digest("SHA-256", new TextEncoder().encode(pair)).then(buf => {
        const hashArray = Array.from(new Uint8Array(buf));
        const hex = hashArray.slice(0, 4).map(b => b.toString(16).padStart(2, '0')).join('').toUpperCase();
        setE2eHash(`0x${hex}`);
      }).catch(() => setE2eHash("0xSECURE"));
    }
  }, [username, activeDMUser]);

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;
    LocalNotifications.createChannel({ 
      id: 'incoming_calls', 
      name: 'Incoming Calls', 
      importance: 5, 
      visibility: 1, 
      sound: 'default', 
      vibration: true 
    }).catch(() => {});
    
    LocalNotifications.registerActionTypes({ 
      types: [{ 
        id: 'CALL_ACTION', 
        actions: [
          { id: 'accept', title: 'Accept', foreground: true }, 
          { id: 'reject', title: 'Decline', destructive: true, foreground: false }
        ] 
      }] 
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;
    const listener = LocalNotifications.addListener('localNotificationActionPerformed', (action: ActionPerformed) => {
      if (action.notification.extra?.callId) {
        if (action.actionId === 'accept' || action.actionId === 'tap') { 
          if (incomingCallData) acceptCall(); 
        } else if (action.actionId === 'reject') { 
          handleHangup(true); 
        }
      }
    });
    return () => { listener.then(sub => sub.remove()); };
  }, [incomingCallData]);

  // 🔥 LIVE WEBRTC CONNECTION STATS POLLER 🔥
  useEffect(() => {
    if (callState === "connected" && peerConnectionRef.current) {
      statsIntervalRef.current = setInterval(async () => {
        try {
          const stats = await peerConnectionRef.current?.getStats();
          if (stats) {
            stats.forEach(report => {
              if (report.type === 'inbound-rtp' && report.kind === 'video') {
                const packetsLost = report.packetsLost || 0;
                if (packetsLost > 15) setConnectionQuality("High Latency");
                else if (packetsLost > 5) setConnectionQuality("Stable Stream");
                else setConnectionQuality("Optimal P2P");
              }
            });
          }
        } catch (e) {}
      }, 3000);
    } else {
      if (statsIntervalRef.current) clearInterval(statsIntervalRef.current);
    }
    return () => { if (statsIntervalRef.current) clearInterval(statsIntervalRef.current); };
  }, [callState]);

  useEffect(() => {
    let audioCtx: AudioContext;
    let interval: NodeJS.Timeout;
    if (callState === "incoming_ringing" || callState === "outgoing_ringing") {
      try {
        audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
        const playRing = () => {
          if (audioCtx.state === 'suspended') audioCtx.resume();
          const osc1 = audioCtx.createOscillator(); 
          const osc2 = audioCtx.createOscillator(); 
          const gainNode = audioCtx.createGain();
          osc1.type = 'sine'; osc1.frequency.setValueAtTime(440, audioCtx.currentTime);
          osc2.type = 'sine'; osc2.frequency.setValueAtTime(480, audioCtx.currentTime);
          gainNode.gain.setValueAtTime(0, audioCtx.currentTime); gainNode.gain.linearRampToValueAtTime(0.3, audioCtx.currentTime + 0.1);
          gainNode.gain.setValueAtTime(0.3, audioCtx.currentTime + 2.0); gainNode.gain.linearRampToValueAtTime(0, audioCtx.currentTime + 2.1);
          osc1.connect(gainNode); osc2.connect(gainNode); gainNode.connect(audioCtx.destination);
          osc1.start(audioCtx.currentTime); osc2.start(audioCtx.currentTime);
          osc1.stop(audioCtx.currentTime + 2.1); osc2.stop(audioCtx.currentTime + 2.1);
        };
        playRing(); interval = setInterval(playRing, 4000); 
      } catch (e) {}
    }
    return () => { if (interval) clearInterval(interval); if (audioCtx && audioCtx.state !== 'closed') audioCtx.close(); };
  }, [callState]);

  useEffect(() => {
    if (callState === "connected") {
      setCallDuration(0); 
      timerIntervalRef.current = setInterval(() => setCallDuration((prev) => prev + 1), 1000);
    } else { 
      if (timerIntervalRef.current) clearInterval(timerIntervalRef.current); 
    }
    return () => { if (timerIntervalRef.current) clearInterval(timerIntervalRef.current); };
  }, [callState]);

  useEffect(() => { 
    if (localVideoRef.current && localStreamRef.current) localVideoRef.current.srcObject = localStreamRef.current; 
  }, [localStreamRef.current, callState]);

  useEffect(() => {
    if (remoteVideoRef.current && remoteStreamRef.current) { 
      remoteVideoRef.current.srcObject = remoteStreamRef.current; 
      remoteVideoRef.current.volume = isSpeakerOn ? 1.0 : 0.4; 
    }
    if (remoteAudioRef.current && remoteStreamRef.current) { 
      remoteAudioRef.current.srcObject = remoteStreamRef.current; 
      remoteAudioRef.current.volume = isSpeakerOn ? 1.0 : 0.4; 
    }
  }, [remoteStreamRef.current, callState, isSpeakerOn]);

  const toggleSpeaker = () => { setIsSpeakerOn(prev => !prev); };

  const startCall = async (type: "audio" | "video") => {
    if (!activeDMUser || activeDMUser === username) return alert("Select another rider to initiate secure comms.");
    setCallError(null); setCallType(type); setCallState("outgoing_ringing");
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ 
        audio: true, 
        video: type === "video" ? { width: { ideal: 1280 }, height: { ideal: 720 }, facingMode: cameraFacingMode } : false 
      });
      localStreamRef.current = stream; 
      if (localVideoRef.current) localVideoRef.current.srcObject = stream;
      
      const pc = new RTCPeerConnection(RTC_CONFIG); 
      peerConnectionRef.current = pc;
      stream.getTracks().forEach((track) => pc.addTrack(track, stream));

      pc.ontrack = (event) => {
        if (event.streams && event.streams[0]) {
          remoteStreamRef.current = event.streams[0];
          if (remoteVideoRef.current) remoteVideoRef.current.srcObject = event.streams[0];
          if (remoteAudioRef.current) remoteAudioRef.current.srcObject = event.streams[0];
        }
      };

      const callDocRef = doc(collection(db, "call_sessions")); 
      const callId = callDocRef.id; setActiveCallId(callId);
      const offerCandidatesCol = collection(db, `call_sessions/${callId}/callerCandidates`); 
      const answerCandidatesCol = collection(db, `call_sessions/${callId}/calleeCandidates`);

      pc.onicecandidate = (event) => { if (event.candidate) addDoc(offerCandidatesCol, event.candidate.toJSON()); };

      const offerDescription = await pc.createOffer(); 
      await pc.setLocalDescription(offerDescription);
      
      await setDoc(callDocRef, { 
        caller: username, 
        callerPfp: pfpUrl || null, 
        recipient: activeDMUser, 
        type: type, 
        status: "offering", 
        offer: { type: offerDescription.type, sdp: offerDescription.sdp }, 
        createdAt: serverTimestamp() 
      });

      onSnapshot(callDocRef, (snapshot) => {
        const data = snapshot.data();
        if (data?.status === "answered" && data.answer && !pc.currentRemoteDescription) { 
          pc.setRemoteDescription(new RTCSessionDescription(data.answer)); 
          setCallState("connected"); 
        }
        if (data?.status === "ended" || data?.status === "rejected") { handleHangup(false); }
      });

      onSnapshot(answerCandidatesCol, (snapshot) => { 
        snapshot.docChanges().forEach((change) => { 
          if (change.type === "added") { pc.addIceCandidate(new RTCIceCandidate(change.doc.data())).catch(() => {}); } 
        }); 
      });
    } catch (err: any) { 
      setCallError(err.message || "Failed to access camera or microphone hardware."); 
      handleHangup(true); 
    }
  };

  const acceptCall = async () => {
    if (!incomingCallData) return;
    setCallError(null); setCallState("connected"); setActiveCallId(incomingCallData.id);

    if (Capacitor.isNativePlatform()) { LocalNotifications.cancel({ notifications: [{ id: 8888 }] }).catch(() => {}); }

    try {
      const type = incomingCallData.type || "video";
      const stream = await navigator.mediaDevices.getUserMedia({ 
        audio: true, 
        video: type === "video" ? { width: { ideal: 1280 }, height: { ideal: 720 }, facingMode: "user" } : false 
      });
      localStreamRef.current = stream; 
      if (localVideoRef.current) localVideoRef.current.srcObject = stream;

      const pc = new RTCPeerConnection(RTC_CONFIG); 
      peerConnectionRef.current = pc;
      stream.getTracks().forEach((track) => pc.addTrack(track, stream));

      pc.ontrack = (event) => {
        if (event.streams && event.streams[0]) {
          remoteStreamRef.current = event.streams[0];
          if (remoteVideoRef.current) remoteVideoRef.current.srcObject = event.streams[0];
          if (remoteAudioRef.current) remoteAudioRef.current.srcObject = event.streams[0];
        }
      };

      const callDocRef = doc(db, "call_sessions", incomingCallData.id);
      const offerCandidatesCol = collection(db, `call_sessions/${incomingCallData.id}/callerCandidates`); 
      const answerCandidatesCol = collection(db, `call_sessions/${incomingCallData.id}/calleeCandidates`);

      pc.onicecandidate = (event) => { if (event.candidate) addDoc(answerCandidatesCol, event.candidate.toJSON()); };

      await pc.setRemoteDescription(new RTCIceCandidate(incomingCallData.offer));
      const answerDescription = await pc.createAnswer(); 
      await pc.setLocalDescription(answerDescription);

      await updateDoc(callDocRef, { answer: { type: answerDescription.type, sdp: answerDescription.sdp }, status: "answered" });

      onSnapshot(offerCandidatesCol, (snapshot) => { 
        snapshot.docChanges().forEach((change) => { 
          if (change.type === "added") { pc.addIceCandidate(new RTCIceCandidate(change.doc.data())).catch(() => {}); } 
        }); 
      });
    } catch (err: any) { 
      setCallError(err.message || "Could not connect to media stream."); 
      handleHangup(true); 
    }
  };

  const handleHangup = async (notifyRemote = true) => {
    if (Capacitor.isNativePlatform()) { LocalNotifications.cancel({ notifications: [{ id: 8888 }] }).catch(() => {}); }
    if (localStreamRef.current) { localStreamRef.current.getTracks().forEach((track) => track.stop()); localStreamRef.current = null; }
    if (remoteStreamRef.current) { remoteStreamRef.current.getTracks().forEach((track) => track.stop()); remoteStreamRef.current = null; }
    if (peerConnectionRef.current) { peerConnectionRef.current.close(); peerConnectionRef.current = null; }

    const targetCallId = activeCallId || incomingCallData?.id;
    if (notifyRemote && targetCallId) { 
      try { await updateDoc(doc(db, "call_sessions", targetCallId), { status: "ended" }); } catch (e) {} 
    }

    setCallState("idle"); setActiveCallId(null); setIncomingCallData(null); 
    setIsMicMuted(false); setIsVideoDisabled(false); setCallDuration(0); setIsScreenSharing(false); setShowInCallChat(false);
  };

  const toggleMute = () => { 
    if (localStreamRef.current) { 
      const audioTrack = localStreamRef.current.getAudioTracks()[0]; 
      if (audioTrack) { audioTrack.enabled = !audioTrack.enabled; setIsMicMuted(!audioTrack.enabled); } 
    } 
  };

  const toggleVideo = () => { 
    if (localStreamRef.current) { 
      const videoTrack = localStreamRef.current.getVideoTracks()[0]; 
      if (videoTrack) { videoTrack.enabled = !videoTrack.enabled; setIsVideoDisabled(!videoTrack.enabled); } 
    } 
  };
  
  const toggleCameraFacingMode = () => { setCameraFacingMode(prev => prev === "user" ? "environment" : "user"); };

  const toggleScreenShare = async () => {
    if (isScreenSharing) {
      setIsScreenSharing(false);
    } else {
      try {
        const screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true });
        if (peerConnectionRef.current) {
          const videoSender = peerConnectionRef.current.getSenders().find(s => s.track?.kind === "video");
          if (videoSender) videoSender.replaceTrack(screenStream.getVideoTracks()[0]);
        }
        setIsScreenSharing(true);
      } catch(e) { alert("Screen share access denied."); }
    }
  };

  const formatTimer = (seconds: number) => { 
    const mins = Math.floor(seconds / 60); const secs = seconds % 60; 
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`; 
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0] && activeDMUser) {
      const file = e.target.files[0];
      setShowAttachmentMenu(false); setIsSending(true);
      try {
        const base64Image = await compressImage(file, 800, 800, 0.7);
        await addDoc(collection(db, "direct_messages"), {
          participants: [username, activeDMUser].sort(),
          sender: username, senderPfp: pfpUrl || null,
          text: "📸 Image", imageUrl: base64Image, readBy: [username],
          timestamp: serverTimestamp(), timer: destructTimer !== "off" ? destructTimer : null
        });
        if (!isMuted) {
          await addDoc(collection(db, "user_notifications"), {
            recipient: activeDMUser, sender: username, type: 'DM',
            content: `sent an image transmission.`, read: false, timestamp: serverTimestamp()
          });
        }
      } catch (err) { alert("Failed to upload image."); } finally { setIsSending(false); }
    }
  };

  const startVoiceRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder; audioChunksRef.current = [];
      mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) audioChunksRef.current.push(e.data); };
      mediaRecorder.onstop = () => {
        const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        const reader = new FileReader(); reader.readAsDataURL(audioBlob);
        reader.onloadend = () => { handleSendVoiceNote(reader.result as string, recordingDuration); };
        stream.getTracks().forEach(t => t.stop());
      };
      mediaRecorder.start(); setIsRecordingVoice(true); setRecordingDuration(0);
      recordingTimerRef.current = setInterval(() => setRecordingDuration(d => d + 1), 1000);
    } catch (e) { alert("Microphone access denied."); }
  };

  const stopVoiceRecording = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== "inactive") mediaRecorderRef.current.stop();
    if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
    setIsRecordingVoice(false);
  };

  const handleSendVoiceNote = async (base64Audio: string, duration: number) => {
    if (!activeDMUser || isSending) return;
    setIsSending(true);
    try {
      await addDoc(collection(db, "direct_messages"), {
        participants: [username, activeDMUser].sort(),
        sender: username, senderPfp: pfpUrl || null,
        text: "🎤 Voice Note", audioUrl: base64Audio, audioDuration: duration,
        readBy: [username], timestamp: serverTimestamp(), timer: destructTimer !== "off" ? destructTimer : null
      });
      if (!isMuted) {
        await addDoc(collection(db, "user_notifications"), {
          recipient: activeDMUser, sender: username, type: 'DM',
          content: `sent a voice memo (${formatTimer(duration)}).`, read: false, timestamp: serverTimestamp()
        });
      }
    } catch (e) {} finally { setIsSending(false); }
  };

  // 🔥 NEW FEATURE: STEALTH SOS PING DIRECT IN DM 🔥
  const handleStealthSosPing = async () => {
    if (!activeDMUser) return;
    setIsSending(true);
    try {
      await addDoc(collection(db, "direct_messages"), {
        participants: [username, activeDMUser].sort(),
        sender: username, senderPfp: pfpUrl || null,
        text: "🚨 STEALTH SOS DISTRESS PING: Immediate assistance requested!",
        readBy: [username], timestamp: serverTimestamp()
      });
      showToast("Stealth SOS Ping transmitted securely!");
    } catch (e) {
      showToast("Failed to transmit SOS ping.");
    } finally {
      setIsSending(false);
    }
  };

  const translateMessage = async (msgId: string, text: string) => {
    if (translations[msgId]) return;
    try {
      const geminiKey = getGeminiApiKey(); if (!geminiKey) return;
      const prompt = `Translate this chat message to English (or Spanish if in English). Return ONLY translated text: "${text}"`;
      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${geminiKey}`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] })
      });
      const data = await res.json();
      const translated = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (translated) setTranslations(prev => ({ ...prev, [msgId]: translated.trim() }));
    } catch (e) {}
  };

  const handleDropLocation = async () => {
    setShowAttachmentMenu(false); setShowThreadSettings(false); setIsSending(true);
    try {
      if (!navigator.geolocation) throw new Error("Geolocation offline");
      const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 15000 });
      });
      handleSendDM(null, `📍 Live Coordinates: [${pos.coords.latitude.toFixed(5)}, ${pos.coords.longitude.toFixed(5)}]`);
    } catch (err: any) {
      handleSendDM(null, `📍 GPS Lock Error: ${err.message || 'Signal Lost'}`);
    } finally { setIsSending(false); }
  };

  const handleBlockUser = () => {
    setShowThreadSettings(false);
    if (activeDMUser) {
      const updated = [...blockedUsers, activeDMUser];
      setBlockedUsers(updated);
      localStorage.setItem("soc_blocked_users", JSON.stringify(updated));
      setActiveDMUser(null);
    }
  };

  if (!showInbox && callState === "idle") return null;

  const activeThreadMessages = activeDMUser ? dms.filter((m: any) => m.participants && m.participants.includes(activeDMUser) && m.participants.includes(username)) : [];
  const recipientProfile = activeDMUser ? userProfiles[activeDMUser] || {} : {};
  const recipientPfp = recipientProfile.pfpUrl; 
  const isRecipientOnline = recipientProfile.lastActive && (Date.now() - getTimeMs(recipientProfile.lastActive)) < 900000;

  const handleSendDM = async (e: React.FormEvent | null, textOverride?: string) => {
    if (e) e.preventDefault();
    const textToSend = textOverride || messageText;
    if (!textToSend.trim() || !activeDMUser || isSending) return;

    setIsSending(true);
    try {
      await addDoc(collection(db, "direct_messages"), {
        participants: [username, activeDMUser].sort(),
        sender: username, senderPfp: pfpUrl || null, text: textToSend.trim(),
        readBy: [username], timestamp: serverTimestamp(), timer: destructTimer !== "off" ? destructTimer : null
      });
      if (!isMuted) {
        await addDoc(collection(db, "user_notifications"), {
          recipient: activeDMUser, sender: username, type: 'DM', 
          content: `sent you a direct message.`, read: false, timestamp: serverTimestamp()
        });
      }
      setMessageText(""); setShowEmojiPicker(false); setShowAttachmentMenu(false); setIsTyping(false);
    } catch (err) {} finally { setIsSending(false); }
  };

  const handleStartNewChat = (e: React.FormEvent) => {
    e.preventDefault();
    const target = newChatTarget.trim();
    if (!target || target.toLowerCase() === username.toLowerCase() || blockedUsers.includes(target)) return;
    setActiveDMUser(target); setNewChatTarget("");
  };

  const QUICK_EMOJIS = ["👍", "🔥", "⚡", "❤️", "🚀", "😂", "🔋", "🛠️"];
  const QUICK_REPLIES = ["On my way! 🚀", "Send location 📍", "Battery low 🔋", "Regroup here 🛑"];

  const getBgStyle = () => {
    if (chatBg === "matrix") return { backgroundImage: "url('https://www.transparenttextures.com/patterns/cubes.png')" };
    if (chatBg === "topo") return { backgroundImage: "url('https://www.transparenttextures.com/patterns/stardust.png')" };
    return {};
  };

  return (
    <>
      {/* LIVE TOAST NOTIFICATION */}
      <AnimatePresence>
        {toastAlert && (
          <motion.div initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 20 }} className="fixed bottom-24 left-1/2 -translate-x-1/2 z-[99999999] bg-zinc-900 border border-emerald-500 text-emerald-300 px-4 py-2 rounded-xl shadow-2xl text-xs font-black uppercase tracking-wider flex items-center gap-2">
            <CheckCheck className="w-4 h-4 text-emerald-400" /> {toastAlert}
          </motion.div>
        )}
      </AnimatePresence>

      {/* ========================================================================= */}
      {/* 🔥 FULLSCREEN WEBRTC AUDIO & VIDEO CALL MODAL OVERLAY 🔥 */}
      {/* ========================================================================= */}
      {callState !== "idle" && (
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} className="fixed inset-0 z-[9999999] bg-black flex flex-col justify-between p-3 sm:p-5 pb-24 sm:pb-5 select-none overflow-hidden">
          <audio ref={remoteAudioRef} autoPlay playsInline />

          <div className="flex items-center justify-between z-30 bg-black/60 backdrop-blur-xl border border-white/10 px-4 py-2.5 rounded-xl shadow-xl">
            <div className="flex items-center gap-2.5">
              <div className={`p-1.5 rounded-lg ${callType === "video" ? "bg-cyan-500 text-black" : "bg-emerald-500 text-black"} font-black`}>
                {callType === "video" ? <Video className="w-4 h-4" /> : <Phone className="w-4 h-4" />}
              </div>
              <div>
                <h4 className="text-xs font-black uppercase text-white tracking-wider flex items-center gap-1.5">
                  {callState === "incoming_ringing" ? `Link from: ${incomingCallData?.caller}` : callState === "outgoing_ringing" ? `Calling ${activeDMUser}...` : `Live Comms: ${activeDMUser || incomingCallData?.caller}`}
                </h4>
                <div className="flex items-center gap-2 mt-0.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                  <span className="text-[9px] font-mono font-bold text-emerald-300 uppercase tracking-wider">{callState === "connected" ? `Active ${formatTimer(callDuration)} (P2P)` : "Transmitting Signals..."}</span>
                  {callState === "connected" && <span className="text-[9px] font-mono bg-cyan-950/80 border border-cyan-500/40 text-cyan-300 px-2 py-0.5 rounded font-bold">📶 {connectionQuality}</span>}
                </div>
              </div>
            </div>
          </div>

          {callError && (
            <div className="z-30 bg-rose-950/80 border border-rose-500 p-2.5 rounded-xl flex items-center gap-2 text-rose-200 text-xs font-mono my-2">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
              <span>{callError}</span>
            </div>
          )}

          <div className="flex-1 relative w-full h-full my-3 rounded-2xl overflow-hidden border border-white/10 bg-zinc-950 flex items-center justify-center shadow-xl">
            {callType === "video" && (
              <video ref={remoteVideoRef} autoPlay playsInline className={`w-full h-full object-cover ${callState === "connected" ? "block" : "hidden"} ${isLowDataMode ? "filter blur-sm brightness-75" : ""}`} />
            )}

            {(callType === "audio" || callState !== "connected" || !remoteStreamRef.current) && (
              <div className="flex flex-col items-center justify-center gap-3 text-center z-10">
                <div className={`relative w-24 h-24 rounded-full bg-black border-4 ${t.border} flex items-center justify-center overflow-hidden shadow-lg`}>
                  {recipientPfp ? <img src={recipientPfp} alt="" className="w-full h-full object-cover" /> : <span className="text-3xl font-black text-white uppercase">{(activeDMUser || incomingCallData?.caller || "R").charAt(0)}</span>}
                  {callState === "outgoing_ringing" && <div className="absolute inset-0 border-4 border-emerald-400 rounded-full animate-ping pointer-events-none opacity-40" />}
                </div>
                <div>
                  <h3 className="text-lg font-black uppercase text-white tracking-wider">{activeDMUser || incomingCallData?.caller}</h3>
                  <p className="text-[10px] font-mono text-zinc-400 uppercase mt-0.5 tracking-wider">{callState === "outgoing_ringing" ? "Ringing Remote Avionics..." : callState === "incoming_ringing" ? "Incoming Transmission..." : "Encrypted Voice Stream Active"}</p>
                </div>
              </div>
            )}

            {callType === "video" && (
              <div className="absolute top-3 right-3 w-28 h-36 rounded-xl overflow-hidden border-2 border-white/30 shadow-xl bg-black z-20">
                <video ref={localVideoRef} autoPlay playsInline muted className={`w-full h-full object-cover ${cameraFacingMode === "user" && !isScreenSharing ? "mirror" : ""}`} />
                {isVideoDisabled && <div className="absolute inset-0 bg-zinc-900 flex flex-col items-center justify-center text-zinc-400 text-[9px] font-mono uppercase font-bold"><VideoOff className="w-5 h-5 mb-0.5 text-zinc-500" /> Camera Off</div>}
                <span className="absolute bottom-1 left-1 bg-black/75 text-white text-[8px] font-mono px-1.5 py-0.5 rounded font-bold">{isScreenSharing ? "Screen Share" : "You"}</span>
                <button onClick={toggleCameraFacingMode} className="absolute top-1 right-1 bg-black/70 p-1.5 rounded-full text-white hover:text-cyan-400"><SwitchCamera className="w-3.5 h-3.5"/></button>
              </div>
            )}
          </div>

          <div className="flex items-center justify-center gap-2.5 z-30 bg-black/80 backdrop-blur-2xl border border-white/10 p-3 rounded-2xl shadow-xl flex-wrap shrink-0">
            {callState === "incoming_ringing" ? (
              <div className="flex items-center gap-3">
                <button onClick={acceptCall} className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-500 text-black px-5 py-2.5 rounded-xl font-black uppercase text-[10px] tracking-wider active:scale-95 shadow-md cursor-pointer"><PhoneIncoming className="w-4 h-4 animate-bounce" /> Accept Link</button>
                <button onClick={() => handleHangup(true)} className="flex items-center gap-1.5 bg-rose-600 hover:bg-rose-500 text-white px-5 py-2.5 rounded-xl font-black uppercase text-[10px] tracking-wider active:scale-95 shadow-md cursor-pointer"><PhoneOff className="w-4 h-4" /> Decline</button>
              </div>
            ) : (
              <div className="flex items-center gap-2 flex-wrap justify-center">
                <button onClick={toggleMute} className={`p-2.5 rounded-xl border transition-all active:scale-90 cursor-pointer ${isMicMuted ? "bg-rose-900/60 border-rose-500 text-rose-300" : "bg-white/10 hover:bg-white/20 border-white/10 text-white"}`}>{isMicMuted ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}</button>
                <button onClick={toggleSpeaker} className={`p-2.5 rounded-xl border transition-all active:scale-90 cursor-pointer ${isSpeakerOn ? "bg-emerald-900/60 border-emerald-500 text-emerald-300" : "bg-white/10 hover:bg-white/20 border-white/10 text-white"}`}><Volume2 className="w-4 h-4" /></button>
                {callType === "video" && (
                  <>
                    <button onClick={toggleVideo} className={`p-2.5 rounded-xl border transition-all active:scale-90 cursor-pointer ${isVideoDisabled ? "bg-rose-900/60 border-rose-500 text-rose-300" : "bg-white/10 hover:bg-white/20 border-white/10 text-white"}`}>{isVideoDisabled ? <VideoOff className="w-4 h-4" /> : <Video className="w-4 h-4" />}</button>
                    <button onClick={toggleScreenShare} className={`p-2.5 rounded-xl border transition-all active:scale-90 cursor-pointer ${isScreenSharing ? "bg-purple-900/60 border-purple-500 text-purple-300" : "bg-white/10 hover:bg-white/20 border-white/10 text-white"}`}>{<MonitorUp className="w-4 h-4" />}</button>
                    <button onClick={() => setIsLowDataMode(!isLowDataMode)} className={`p-2.5 rounded-xl border transition-all active:scale-90 cursor-pointer ${isLowDataMode ? "bg-amber-900/60 border-amber-500 text-amber-300" : "bg-white/10 hover:bg-white/20 border-white/10 text-white"}`}><Wifi className="w-4 h-4" /></button>
                  </>
                )}
                <button onClick={() => handleHangup(true)} className="flex items-center gap-1.5 bg-rose-600 hover:bg-rose-500 text-white px-4 py-2.5 rounded-xl font-black uppercase text-[10px] tracking-wider active:scale-95 shadow-md cursor-pointer"><PhoneOff className="w-4 h-4" /> End Link</button>
              </div>
            )}
          </div>
        </motion.div>
      )}

      {/* ========================================================================= */}
      {/* 🔥 FINGERPRINT MODAL FOR E2E CRYPTO VERIFICATION 🔥 */}
      {/* ========================================================================= */}
      {showFingerprintModal && (
        <div className="fixed inset-0 z-[999999999] bg-black/9org flex items-center justify-center p-4 backdrop-blur-xl">
          <div className="bg-zinc-950 border border-white/20 p-6 rounded-3xl max-w-sm w-full space-y-4 shadow-2xl text-center">
             <div className="w-12 h-12 bg-emerald-500/20 border border-emerald-500/50 rounded-2xl flex items-center justify-center mx-auto text-emerald-400">
               <ShieldCheck className="w-6 h-6"/>
             </div>
             <div>
               <h4 className="text-sm font-black uppercase text-white tracking-wider">Cryptographic P2P Fingerprint</h4>
               <p className="text-[10px] font-mono text-zinc-400 mt-1">Verified 256-bit SHA-2 secure session hash between @{username} and @{activeDMUser}.</p>
             </div>
             <div className="bg-black border border-white/10 p-3 rounded-xl font-mono text-xs text-emerald-400 select-all font-bold">
               {e2eHash}::AUTH::SECURE
             </div>
             <button onClick={() => setShowFingerprintModal(false)} className={`w-full py-3 ${t.bg} text-black font-black uppercase text-xs rounded-xl shadow-lg cursor-pointer active:scale-95`}>Close Fingerprint</button>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 🔥 MAIN INBOX CONVERSATION WINDOW 🔥 */}
      {/* ========================================================================= */}
      {showInbox && callState === "idle" && (
        <div className="fixed inset-0 z-[999999] bg-black/90 backdrop-blur-2xl flex flex-col justify-between pt-16 pb-20 sm:p-6 sm:justify-center sm:items-center">
          <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }} transition={{ type: "spring", damping: 25, stiffness: 200 }} className={`app-card ${glassBlurClass} ${cornerStyle} bg-zinc-950 border border-white/15 w-full max-w-lg h-full max-h-[85vh] flex flex-col overflow-hidden shadow-2xl relative rounded-3xl`} style={getBgStyle()}>
            <input type="file" accept="image/*" ref={fileInputRef} onChange={handleFileChange} className="hidden" />

            <div className="flex justify-between items-center px-4 py-3 border-b border-white/10 shrink-0 bg-white/5">
              <h3 className={`text-xs font-black uppercase tracking-widest ${t.text} flex items-center gap-2`}>
                <Mail className="w-4 h-4" /> Comms Hub &amp; Direct Messaging
              </h3>
              <button type="button" onClick={() => { setShowInbox(false); setActiveDMUser(null); }} className="text-zinc-500 hover:text-white p-2 bg-black/50 rounded-full border border-white/10 cursor-pointer active:scale-95 transition-all">
                <X className="w-4 h-4" />
              </button>
            </div>

            {activeDMUser ? (
              <div className="flex-1 flex flex-col min-h-0 overflow-hidden relative">
                <div className="px-4 py-2.5 bg-black/40 border-b border-white/10 flex items-center justify-between shrink-0 shadow-md z-10">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className={`relative w-9 h-9 rounded-full bg-black border ${t.border} flex items-center justify-center font-black ${t.text} text-xs shadow-md overflow-hidden shrink-0`}>
                      {recipientPfp ? <img src={recipientPfp} alt={activeDMUser} className="w-full h-full object-cover" /> : activeDMUser.charAt(0).toUpperCase()}
                      {isRecipientOnline && <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-emerald-500 rounded-full border-2 border-black" />}
                    </div>
                    <div className="min-w-0">
                      <span className="text-xs sm:text-sm font-black uppercase text-white tracking-wide block truncate">{activeDMUser}</span>
                      <span onClick={() => setShowFingerprintModal(true)} className="text-[9px] font-mono text-emerald-400 uppercase tracking-widest block flex items-center gap-1 cursor-pointer hover:underline" title="Tap to view cryptographic fingerprint">
                        {isMuted ? <BellOff className="w-2.5 h-2.5 text-zinc-500"/> : <Lock className="w-2.5 h-2.5"/>}
                        {isRecipientOnline ? 'Active Now' : `E2E ${e2eHash}`}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    {/* 🔥 NEW FEATURE: STEALTH SOS DISTRESS PING BUTTON 🔥 */}
                    <button type="button" onClick={handleStealthSosPing} className="p-2 bg-rose-950/60 hover:bg-rose-900 border border-rose-500/40 text-rose-300 rounded-full cursor-pointer transition-all active:scale-95" title="Transmit Stealth SOS Ping"><ShieldAlert className="w-3.5 h-3.5"/></button>
                    <button type="button" onClick={() => startCall("audio")} className="p-2 bg-white/5 hover:bg-emerald-950/40 border border-white/10 text-zinc-300 hover:text-emerald-400 rounded-full cursor-pointer transition-all active:scale-95"><Phone className="w-3.5 h-3.5"/></button>
                    <button type="button" onClick={() => startCall("video")} className="p-2 bg-white/5 hover:bg-cyan-950/40 border border-white/10 text-zinc-300 hover:text-cyan-400 rounded-full cursor-pointer transition-all active:scale-95"><Video className="w-3.5 h-3.5"/></button>
                    <button type="button" onClick={() => setShowThreadSettings(!showThreadSettings)} className="p-2 bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white rounded-full cursor-pointer transition-all active:scale-95"><MoreVertical className="w-3.5 h-3.5"/></button>
                    <button type="button" onClick={() => setActiveDMUser(null)} className="px-3 py-1.5 bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 rounded-xl text-[9px] font-black uppercase tracking-wider cursor-pointer active:scale-95 ml-1">Back</button>
                  </div>

                  <AnimatePresence>
                    {showThreadSettings && (
                      <motion.div initial={{ opacity: 0, scale: 0.9, y: 10 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.9, y: 10 }} className="absolute right-4 top-14 w-52 bg-zinc-950 border border-white/15 rounded-2xl shadow-xl z-50 overflow-hidden py-1.5 flex flex-col text-xs">
                         <button onClick={() => { setShowThreadSettings(false); setShowFingerprintModal(true); }} className="px-4 py-2.5 text-left font-bold text-zinc-300 hover:bg-white/10 flex items-center gap-2.5"><ShieldCheck className="w-3.5 h-3.5 text-emerald-400"/> Verify Key Fingerprint</button>
                         <button onClick={() => { setShowThreadSettings(false); setIsTranslating(!isTranslating); }} className="px-4 py-2.5 text-left font-bold text-zinc-300 hover:bg-white/10 flex items-center gap-2.5"><Globe className="w-3.5 h-3.5 text-sky-400"/> {isTranslating ? 'Stop Translating' : 'AI Translate'}</button>
                         <button onClick={() => { setShowThreadSettings(false); setChatBg(chatBg === "dark" ? "matrix" : chatBg === "matrix" ? "topo" : "dark"); }} className="px-4 py-2.5 text-left font-bold text-zinc-300 hover:bg-white/10 flex items-center gap-2.5"><ImageIcon className="w-3.5 h-3.5 text-emerald-400"/> Theme Backdrop</button>
                         <button onClick={handleDropLocation} className="px-4 py-2.5 text-left font-bold text-zinc-300 hover:bg-white/10 flex items-center gap-2.5"><Pin className="w-3.5 h-3.5 text-amber-400"/> Drop Live GPS</button>
                         <button onClick={() => { setShowThreadSettings(false); setIsMuted(!isMuted); }} className="px-4 py-2.5 text-left font-bold text-rose-400 hover:bg-white/10 flex items-center gap-2.5 border-t border-white/5"><BellOff className="w-3.5 h-3.5 text-rose-500"/> {isMuted ? 'Unmute Alerts' : 'Mute Thread'}</button>
                         <button onClick={handleBlockUser} className="px-4 py-2.5 text-left font-bold text-rose-400 hover:bg-rose-950/40 flex items-center gap-2.5"><Ban className="w-3.5 h-3.5 text-rose-600"/> Block Operator</button>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>

                <div ref={chatScrollRef} className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar bg-black/20 text-xs sm:text-sm">
                  {destructTimer !== "off" && <div className="sticky top-0 z-20 flex justify-center"><span className="bg-amber-500 text-black px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-wider shadow flex items-center gap-1"><Clock className="w-3 h-3"/> Timer: {destructTimer}</span></div>}

                  {activeThreadMessages.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-zinc-600 font-mono text-[10px] uppercase font-bold tracking-widest text-center space-y-3">
                      <Shield className="w-8 h-8 text-zinc-700 mx-auto" />
                      <p>End-to-End Encrypted Channel Active ({e2eHash}).<br/>Send a transmission or start live calls above.</p>
                    </div>
                  ) : (
                    activeThreadMessages.map((msg: any) => {
                      const isSelf = msg.sender === username;
                      const senderPic = isSelf ? pfpUrl : (userProfiles[msg.sender]?.pfpUrl || msg.senderPfp);
                      
                      if (isTranslating && !isSelf && !translations[msg.id] && msg.text && msg.text !== "📸 Image" && msg.text !== "🎤 Voice Note") {
                        translateMessage(msg.id, msg.text);
                      }

                      return (
                        <div key={msg.id} className={`flex items-end gap-2 ${isSelf ? 'justify-end' : 'justify-start'}`}>
                          {!isSelf && (
                            <div className="w-7 h-7 rounded-full bg-zinc-800 border border-white/10 flex items-center justify-center text-[10px] font-bold text-white overflow-hidden shrink-0 mb-1">
                              {senderPic ? <img src={senderPic} alt="" className="w-full h-full object-cover"/> : msg.sender?.charAt(0).toUpperCase()}
                            </div>
                          )}
                          <div className={`flex flex-col ${isSelf ? 'items-end' : 'items-start'}`}>
                            <div className={`max-w-[85%] p-3.5 rounded-2xl text-[13px] font-sans leading-relaxed shadow-md whitespace-pre-wrap ${
                              isSelf ? `${t.bg} text-black font-bold rounded-tr-sm` : "bg-white/10 border border-white/10 text-zinc-100 rounded-tl-sm font-medium"
                            }`}>
                              {msg.imageUrl && <img src={msg.imageUrl} alt="Attached Media" className="rounded-xl max-h-52 w-full object-cover my-1 border border-white/20" />}
                              {msg.audioUrl && (
                                <div className="flex flex-col gap-1 py-1">
                                  <span className="text-[10px] font-mono uppercase font-bold flex items-center gap-1 opacity-80"><Mic className="w-3 h-3" /> Voice Memo ({formatTimer(msg.audioDuration || 0)})</span>
                                  <audio src={msg.audioUrl} controls className="h-8 max-w-[220px] rounded-lg" />
                                </div>
                              )}
                              {msg.text && msg.text !== "📸 Image" && msg.text !== "🎤 Voice Note" && (
                                <p>{isTranslating && !isSelf && translations[msg.id] ? `[AI Translate]: ${translations[msg.id]}` : msg.text}</p>
                              )}
                            </div>
                            <span className="text-[9px] font-mono text-zinc-500 mt-1 px-1 flex items-center gap-1">
                              {new Date(getTimeMs(msg.timestamp)).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              {isSelf && <CheckCheck className="w-3 h-3 text-emerald-400 ml-1" />}
                            </span>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>

                {activeThreadMessages.length > 0 && !messageText && !isRecordingVoice && (
                  <div className="flex gap-1.5 overflow-x-auto px-3 py-1.5 bg-black/80 backdrop-blur-xl custom-scrollbar border-t border-white/5 shrink-0">
                    {QUICK_REPLIES.map(qr => (
                      <button key={qr} onClick={() => { if (qr === "Send location 📍") handleDropLocation(); else handleSendDM(null, qr); }} className="min-h-[28px] px-2.5 bg-white/10 hover:bg-white/20 border border-white/10 rounded-full text-[9px] font-bold text-white shrink-0 whitespace-nowrap cursor-pointer active:scale-95 shadow-sm">{qr}</button>
                    ))}
                  </div>
                )}

                <AnimatePresence>
                  {showAttachmentMenu && (
                    <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="bg-zinc-950 border-t border-white/10 p-3 grid grid-cols-4 gap-2 overflow-hidden shadow-inner text-xs shrink-0">
                      <button onClick={() => { fileInputRef.current?.click(); }} className="flex flex-col items-center justify-center p-2.5 bg-white/5 hover:bg-white/10 rounded-xl gap-1.5 cursor-pointer transition-colors"><ImageIcon className="w-4 h-4 text-cyan-400"/><span className="text-[8px] font-black uppercase text-white tracking-wider">Photo</span></button>
                      <button onClick={handleDropLocation} className="flex flex-col items-center justify-center p-2.5 bg-white/5 hover:bg-white/10 rounded-xl gap-1.5 cursor-pointer transition-colors"><MapPin className="w-4 h-4 text-emerald-400"/><span className="text-[8px] font-black uppercase text-white tracking-wider">Location</span></button>
                      <button onClick={() => setDestructTimer(destructTimer === "off" ? "1h" : destructTimer === "1h" ? "24h" : "off")} className={`flex flex-col items-center justify-center p-2.5 rounded-xl gap-1.5 cursor-pointer transition-colors ${destructTimer !== "off" ? "bg-amber-500/20 border border-amber-500/40" : "bg-white/5 hover:bg-white/10"}`}><Clock className={`w-4 h-4 ${destructTimer !== "off" ? "text-amber-400" : "text-amber-500"}`}/><span className="text-[8px] font-black uppercase text-white tracking-wider">{destructTimer !== "off" ? destructTimer : "Timer"}</span></button>
                      <button onClick={() => { setShowAttachmentMenu(false); startVoiceRecording(); }} className="flex flex-col items-center justify-center p-2.5 bg-white/5 hover:bg-white/10 rounded-xl gap-1.5 cursor-pointer transition-colors"><Mic className="w-4 h-4 text-rose-400"/><span className="text-[8px] font-black uppercase text-white tracking-wider">Record</span></button>
                    </motion.div>
                  )}
                </AnimatePresence>

                {showEmojiPicker && (
                  <div className="p-2 bg-black/90 border-t border-white/10 flex items-center gap-2 overflow-x-auto custom-scrollbar shrink-0">
                    {QUICK_EMOJIS.map(emoji => (
                      <button key={emoji} type="button" onClick={() => setMessageText(prev => prev + emoji)} className="text-lg p-1.5 hover:bg-white/10 rounded-xl transition-transform active:scale-95 cursor-pointer shrink-0">{emoji}</button>
                    ))}
                  </div>
                )}

                <form onSubmit={handleSendDM} className="p-3 bg-zinc-950 border-t border-white/15 flex items-center gap-2 shrink-0 z-30">
                  {isRecordingVoice ? (
                    <div className="flex-1 flex items-center gap-2 px-1">
                      <div className="flex-1 flex items-center gap-1 justify-center py-1 h-10 bg-rose-950/40 border border-rose-500/30 rounded-full px-3 overflow-hidden shadow-inner">
                        <span className="w-1.5 h-3.5 bg-rose-500 rounded-full animate-pulse" />
                        <span className="ml-2 text-[11px] font-mono text-rose-400 font-bold uppercase tracking-wider">Recording ({formatTimer(recordingDuration)})...</span>
                      </div>
                      <button type="button" onClick={() => { if (recordingTimerRef.current) clearInterval(recordingTimerRef.current); setIsRecordingVoice(false); }} className="w-8 h-8 rounded-full bg-zinc-800 text-white flex items-center justify-center shadow"><X className="w-3.5 h-3.5"/></button>
                      <button type="button" onClick={stopVoiceRecording} className="w-8 h-8 rounded-full bg-emerald-500 text-black flex items-center justify-center shadow"><Send className="w-3.5 h-3.5 ml-0.5"/></button>
                    </div>
                  ) : (
                    <>
                      <button type="button" onClick={() => { setShowAttachmentMenu(!showAttachmentMenu); setShowEmojiPicker(false); }} className="p-2 text-zinc-400 hover:text-white rounded-full hover:bg-white/5 cursor-pointer transition-colors shrink-0"><PlusCircle className="w-5 h-5"/></button>
                      <button type="button" onClick={() => { setShowEmojiPicker(!showEmojiPicker); setShowAttachmentMenu(false); }} className="p-2 text-zinc-400 hover:text-amber-400 rounded-full hover:bg-white/5 cursor-pointer transition-colors shrink-0"><Smile className="w-5 h-5"/></button>
                      <input type="text" value={messageText} onChange={(e) => { setMessageText(e.target.value); setIsTyping(e.target.value.length > 0); }} onBlur={() => setIsTyping(false)} placeholder={`Message ${activeDMUser}...`} className="flex-1 min-h-[40px] bg-white/5 border border-white/10 rounded-full px-4 text-xs font-bold text-white outline-none focus:border-white/30 shadow-inner" />
                      <button type="submit" disabled={!messageText.trim() || isSending} className={`w-10 h-10 rounded-full transition-all active:scale-95 cursor-pointer disabled:opacity-40 ${t.bg} text-black shadow-md flex items-center justify-center shrink-0`}><Send className="w-4 h-4 ml-0.5" /></button>
                    </>
                  )}
                </form>
              </div>
            ) : (
              <div className="flex-1 flex flex-col min-h-0 overflow-hidden p-4 space-y-4">
                <form onSubmit={handleStartNewChat} className="flex gap-2 shrink-0">
                  <input type="text" value={newChatTarget} onChange={(e) => setNewChatTarget(e.target.value)} placeholder="Start chat with rider callsign..." className="flex-1 min-h-[42px] bg-black/50 border border-white/10 rounded-full px-4 text-xs font-bold text-white outline-none focus:border-white/30 shadow-inner" />
                  <button type="submit" disabled={!newChatTarget.trim()} className={`min-h-[42px] px-5 rounded-full font-black uppercase text-[10px] tracking-wider transition-all active:scale-95 cursor-pointer disabled:opacity-40 ${t.bg} text-black shadow-md shrink-0`}>Message</button>
                </form>

                <div className="relative shrink-0">
                  <Search className="absolute left-4 top-3 w-4 h-4 text-zinc-500" />
                  <input type="text" value={inboxSearch} onChange={(e) => setInboxSearch(e.target.value)} placeholder="Search secure conversations..." className="w-full min-h-[42px] bg-white/5 border border-white/10 rounded-full pl-10 pr-4 text-xs font-bold text-white outline-none shadow-inner" />
                </div>

                <div className="flex-1 overflow-y-auto space-y-1.5 custom-scrollbar pr-1 pt-1 pb-4">
                  {chatList.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-zinc-600 font-mono text-[10px] uppercase font-bold tracking-widest text-center">
                      No active direct messages.<br/>Type a rider callsign above to initiate link.
                    </div>
                  ) : (
                    chatList.map(({ user, lastMessage }: any) => {
                      if (blockedUsers.includes(user)) return null;

                      const uProfile = userProfiles[user] || {};
                      const uPfp = uProfile.pfpUrl;
                      const isOnline = uProfile.lastActive && (Date.now() - getTimeMs(uProfile.lastActive)) < 900000;
                      return (
                        <div key={user} onClick={() => setActiveDMUser(user)} className="p-3.5 rounded-2xl hover:bg-white/5 border border-transparent hover:border-white/10 transition-all cursor-pointer flex items-center justify-between active:scale-98">
                          <div className="flex items-center gap-3.5 min-w-0">
                            <div className={`relative w-10 h-10 rounded-full bg-black border ${t.borderSubtle} flex items-center justify-center font-black ${t.text} text-xs shrink-0 shadow-md overflow-hidden`}>
                              {uPfp ? <img src={uPfp} alt={user} className="w-full h-full object-cover"/> : user.charAt(0).toUpperCase()}
                              {isOnline && <span className="absolute bottom-0 right-0.5 w-2.5 h-2.5 bg-emerald-500 border-2 border-black rounded-full" />}
                            </div>
                            <div className="min-w-0">
                              <span className="text-xs sm:text-sm font-black text-white truncate block">{user}</span>
                              <p className="text-[11px] text-zinc-400 font-medium truncate mt-0.5">{lastMessage.sender === username ? "You: " : ""}{lastMessage.text}</p>
                            </div>
                          </div>
                          <span className="text-[9px] font-mono text-zinc-500 shrink-0 ml-2">
                            {new Date(getTimeMs(lastMessage.timestamp)).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            )}
          </motion.div>
        </div>
      )}
    </>
  );
}