package com.example.data

data class PevLiveOpsModule(
  val id: String,
  val category: String, // GPS_NAV, VESC_TUNING, BATTERY_BMS, SUSPENSION_TIRES, TRAIL_SAFETY, GARAGE_MODS
  val title: String,
  val platform: String,
  val keyMetric: String,
  val protocol: String,
  val storeSearchQuery: String,
)

/**
 * 205 structured live PEV engineering, GPS navigation, VESC tuning, battery/BMS,
 * suspension/tire, trail safety, and garage mod modules integrated into the PEV Pro Suite.
 */
object PevLiveExpansion200 {

  val CATEGORIES =
    listOf(
      "ALL",
      "GPS_NAV",
      "VESC_TUNING",
      "BATTERY_BMS",
      "SUSPENSION_TIRES",
      "TRAIL_SAFETY",
      "GARAGE_MODS",
    )

  val MODULES: List<PevLiveOpsModule> by lazy {
    val list = mutableListOf<PevLiveOpsModule>()

    // 1. GPS NAVIGATION & ROUTE PROTOCOLS (35)
    val navItems =
      listOf(
        Triple("Turn-by-Turn Bike Lane Priority", "All PEVs", "Avoid highways & >45 mph arterials via cycling profile; verify shoulder width at rural intersections."),
        Triple("Live Charger Hop-Planning", "Long-Range", "Space J1772-to-NEMA 5-15 adapter stops every 60% of nominal range to retain a 40% headwind reserve."),
        Triple("Elevation Gain Wh/km Penalty", "Mountain", "Budget +14 Wh per 100m vertical climb per 100kg total rider+PEV mass on paved grades."),
        Triple("Headwind Aerodynamic Derate", "Scooter / EUC", "A 15 mph headwind increases drag power ~45% at 25 mph; tuck posture or drop cruise speed 5 mph."),
        Triple("Off-Route Auto-Reroute Buffer", "GPS Nav", "Trigger clean reroute when >45m off polyline to prevent GPS multipath jitter under dense tree canopy."),
        Triple("Gravel & Washout Trail Routing", "eMoto / Fat eBike", "Prefer compacted rail-trails over unmaintained logging roads after >10mm rainfall in 24h."),
        Triple("Voice Turn Chime Lead Distance", "Helmet Audio", "Play chime 120m before turn at <20 mph and 220m before turn at 30+ mph for safe braking."),
        Triple("Rural Cell Dead-Zone Prep", "Backcountry", "Pre-load route waypoints & offline tiles before leaving town; rely on GPS-only breadcrumb lock."),
        Triple("J1772 EVSE Adapter Protocol", "Fast Charge", "Use a 240V-to-C13/NEMA J1772 adapter ONLY with dual-voltage 100-240V chargers; verify input rating first."),
        Triple("Return-to-Base Point of No Return", "Range Ops", "Turn back at 55% SOC on outbound flat runs or 65% SOC if return leg climbs >150m elevation."),
        Triple("Night Route Streetlight Corridor", "Urban Night", "Favor collector streets with continuous LED lamps and painted bike buffers over dark unlit shortcuts."),
        Triple("Bridge Deck Expansion Joint Alert", "10\"-11\" Scooters", "Cross steel grates and longitudinal expansion joints at >30 deg angle to prevent wheel tramlining."),
        Triple("Railroad Crossing Perpendicular Rule", "All PEVs", "Approach wet steel tracks at 90 degrees, knees bent, zero throttle/brake input across rails."),
        Triple("Roundabout Lane Positioning", "Road PEVs", "Take the center of the lane through single-lane roundabouts to avoid right-hook blind spots."),
        Triple("Multi-Use Path Speed Etiquette", "Greenways", "Cap speed at 12-15 mph near pedestrians, ring bell 30m out, and pass on left with 1.5m clearance."),
        Triple("Convoy Waypoint Sync Check", "Group Rides", "Share destination pin & regroup points before launch; never split across red lights without a sweep."),
        Triple("Charging Cafe Courtesy Protocol", "Touring", "Ask staff before plugging in >600W chargers; keep cables flat along walls with Velcro wraps."),
        Triple("Cold-Snap Range Derate Map", "Winter", "Expect -20% range at 5°C and -35% at -5°C due to Li-Ion internal resistance rise."),
        Triple("Steep Descent Regen Margin", "Hill Routes", "Never start a >200m descent at 100% SOC; charge to 85-90% to avoid BMS over-voltage regen cutoff."),
        Triple("Crosswind Gust Corridor Warning", "Open Plains", "Open farmland bridges amplify side gusts on tall EUCs and scooters; drop 20% speed and widen stance."),
        Triple("GPS Bearing vs Compass Heading", "AR Wayfinder", "At <2 mph rely on magnetometer heading; above 4 mph switch to GPS course-over-ground for stability."),
        Triple("Emergency Evacuation Bailout Pin", "Remote Trails", "Pin the nearest paved county road intersection every 8 km for pickup/tow truck accessibility."),
        Triple("School Zone & Park Speed Geofence", "Legal", "Observe 15 mph posted trail/school limits automatically via legal compliance HUD warnings."),
        Triple("Construction Gravel Detour Check", "Commuting", "Watch for milled asphalt grooves that grab narrow 2.5\" scooter tires; shift weight slightly rearward."),
        Triple("Trailhead Parking & Lock Anchor", "Security", "Lock through solid frame triangle (not removable fork/wheel) with 16mm+ angle-grinder resistant U-lock."),
        Triple("Sun-Glare Dusk Routing", "Westbound", "Riding directly into low sunset blinds oncoming drivers; turn on high-vis daytime strobe + vest."),
        Triple("Live Weather Radar Cell Avoidance", "Storm Ops", "Check precipitation radar 30 min ahead; seek shelter before lightning is within 10 miles (30-sec thunder)."),
        Triple("Urban Tunnel GPS Loss Recovery", "City", "Hold steady cruise through tunnels; GPS lock re-acquires within 3-5 sec after portal exit."),
        Triple("Single-Track Erosion & Mud Protocol", "MTB Trails", "Avoid clay singletrack within 24h of heavy rain to prevent tire ruts and trail closure."),
        Triple("RV Park 30A/50A Pedestal Charge", "Overland", "Carry compact TT-30P to 5-15R adapter for emergency campground fast charging on rural tours."),
        Triple("Dual-Charger Breaker Split Rule", "Group Charge", "A standard 15A US outlet maxes at 1,440W continuous; split >2 fast chargers across separate breakers."),
        Triple("Scenic Overlook Elevation Check", "Touring", "Log barometric & GPS peak altitude to calibrate your AI ride-record Wh/km predictor."),
        Triple("Ferry & Transit PEV Boarding Rules", "Multimodal", "Verify UL-2272 / UL-2849 certification sticker before boarding public rail or commuter ferries."),
        Triple("Farm Equipment Rural Blind Crest", "Country Roads", "Slow below 20 mph over blind rural hill crests where slow tractors or loose gravel may sit."),
        Triple("Post-Ride GPX Archive & Sync", "Telemetry", "Save ride track with start/end battery % so AI continuously sharpens your remaining-charge forecast."),
      )
    navItems.forEachIndexed { idx, (t, p, proto) ->
      list += PevLiveOpsModule("nav_$idx", "GPS_NAV", t, p, "NAV-${idx + 1}", proto, "$t PEV GPS accessory")
    }

    // 2. VESC & CONTROLLER TUNING RECIPES (35)
    val vescItems =
      listOf(
        Triple("FOC Motor Detection Baseline", "VESC 6.x", "Run FOC detection with wheel off ground, cool motor (<30°C), and verify R/L/Lambda values are symmetric."),
        Triple("85% Duty Cycle Headroom Rule", "EUC / VESC", "Set Duty Cycle Current Limit Start at 82-85% so soft tiltback/power taper prevents abrupt cutouts."),
        Triple("Motor Phase vs Battery Amp Ratio", "FOC Tuning", "Safe rule of thumb: Phase Amps = 1.6x to 2.2x Battery Amps for crisp torque without MOSFET spikes."),
        Triple("Field Weakening Safe Ceiling", "High-Speed", "Keep Field Weakening max current <= 25% of Motor Max Amps and start duty >= 70% to avoid stator bake."),
        Triple("Regen Brake Max Amp Limit", "Battery Safe", "Cap Battery Regen Amps strictly below pack max charge rate (typically 0.5C–1.0C) to protect cells."),
        Triple("Absolute Max Current Headroom", "DRV Safety", "Set ABS Max Current 1.5x higher than Motor Max Current (never above controller hardware spec)."),
        Triple("Throttle Exponential Curve (+25%)", "Scooter / eMoto", "Use +20% to +35% positive throttle expo for smooth low-speed control on high-kW dual-motor rigs."),
        Triple("Throttle Deadband Calibration", "Hall Lever", "Measure min/max Hall voltages (typically 0.85V–4.15V) and add 5-8% deadband on both ends."),
        Triple("Smart Reverse Speed Cap", "Cargo / Trike", "Limit smart reverse duty to 12% and max 4 mph for safe garage backing."),
        Triple("MOSFET Thermal Rollback Curve", "Thermal", "Start FET temp rollback at 75°C and full cutoff at 85°C to prevent thermal runaway solder melt."),
        Triple("Motor Stator NTC Thermal Guard", "Hub / Mid", "Configure 10k NTC sensor rollback starting at 110°C and hard cutoff at 130°C (magnet Curie guard)."),
        Triple("Low-Voltage Cutoff Soft Start", "Li-Ion Life", "Start LVC soft taper at 3.25V/cell and hard cutoff at 3.00V/cell (e.g. 65V start / 60V end on 20S)."),
        Triple("Hall Sensor Interpolation ERPM", "Sensored FOC", "Set Sensorless Transition ERPM around 2500–4000 ERPM for silent startup and high-RPM efficiency."),
        Triple("Zero-Vector / HFI Silent Start", "Sensorless", "Enable HFI Start on sensorless hubs if Hall harness fails mid-ride as an emergency limp-home mode."),
        Triple("Dual-Controller CAN Bus Sync", "AWD Scooters", "Terminate CAN-H/CAN-L twisted pair cleanly; verify identical firmware version on Master & Slave."),
        Triple("Front/Rear Traction Torque Split", "AWD Tuning", "Dial Front motor to 75-85% of Rear phase amps on loose gravel to stop front wheel washout."),
        Triple("MTPA Torque Efficiency Mode", "IPM Motors", "Enable MTPA (Maximum Torque Per Amp) on interior permanent magnet mid-drives for +6% efficiency."),
        Triple("Zero-Throttle Coast vs Drag Brake", "Commute", "Zero coast maximizes range; 5-10A light auto-regen gives one-pedal urban control."),
        Triple("Phase Wire Inductance Check", "Diagnostics", "Asymmetric phase resistance (>10% variance) signals shorted winding enamel or loose bullet connector."),
        Triple("Anti-Spark Switch Pre-Charge", "72V-100V", "Always use QS8-S or smart MOSFET anti-spark switch so capacitor inrush never welds contacts."),
        Triple("Switching Frequency 20kHz-30kHz", "FOC Noise", "25 kHz eliminates audible whine while keeping MOSFET switching heat low on 72V+ controllers."),
        Triple("Speed-Based PID Ramp Filter", "Cruise", "Smooth throttle ramp time (0.15s–0.25s) reduces belt shear and axle-nut spin on high-torque launches."),
        Triple("Wheel Diameter & Pole Pair Math", "Telemetry", "Count magnets/2 = pole pairs; measure loaded tire circumference in mm for 0.5% speed accuracy."),
        Triple("Over-Voltage Bus Clamp Margin", "100V VESC", "Maintain >=15% voltage margin between 100% charged pack (e.g. 84V) and FET rating (100V)."),
        Triple("Current Filter Constant Tuning", "Stability", "Default current filter 0.1 works for most hubs; raise to 0.15 if noisy Hall readings cause cogging."),
        Triple("Stall Protection Timeout", "Hill Climb", "Enable 1.5s stall timeout to cut phase current if wheel locks in deep mud or jammed rock."),
        Triple("Brake Hall Cutoff Priority", "Safety", "Ensure mechanical brake cutoff switch overrides throttle 100% of the time before riding."),
        Triple("Bluetooth Telemetry Module Baud", "Logging", "Pair BLE UART module at 115200 baud for real-time phase amps, duty %, and FET temp capture."),
        Triple("Firmware Backup Before Flash", "VESC Tool", "Export XML Motor + App configs before any firmware update so proven settings are never lost."),
        Triple("External Shunt Calibration", "Wattmeter", "Verify idle current reads 0.0A and compare against clamp meter at 20A load for accurate Wh/km."),
        Triple("Sinewave vs Squarewave Upgrade", "Scooters", "Swapping squarewave to FOC sinewave drops motor noise 12dB and boosts range 8-12%."),
        Triple("Cruise Control Tap-Cancel Check", "Touring", "Verify either brake lever tap or throttle blip immediately disengages cruise control."),
        Triple("Regen Variable Thumb Lever", "Precision", "Dedicated Hall regen thumb lever gives linear hydraulic-free braking down long paved descents."),
        Triple("Motor Statorade Ferrofluid Dose", "Thermal Mod", "Inject 5-8 mL Statorade into direct-drive hub stator gap to cut winding temperatures by 20-30°C."),
        Triple("Controller Thermal Paste Reflow", "Reliability", "Replace factory dry pads with 12W/mK thermal compound & torque heatsink screws evenly."),
      )
    vescItems.forEachIndexed { idx, (t, p, proto) ->
      list += PevLiveOpsModule("vesc_$idx", "VESC_TUNING", t, p, "FOC-${idx + 1}", proto, "$t VESC controller")
    }

    // 3. BATTERY PACK & BMS ENGINEERING (35)
    val battItems =
      listOf(
        Triple("80% Daily Charge Longevity Rule", "All Li-Ion", "Stopping daily charge at 80-85% (4.05V/cell) doubles cycle life from ~500 to 1,200+ cycles."),
        Triple("Monthly 100% Cell Balance Cycle", "Smart BMS", "Leave pack plugged in 1-2 hours past green LED once a month so passive/active BMS equalizes cells."),
        Triple("Winter Storage 50% Voltage Spec", "Storage", "Store packs for >2 weeks at 3.70-3.80V/cell (~50% SOC) in a dry 10-20°C room—never at 100% or 0%."),
        Triple("Never Charge Below 0°C (32°F)", "Cold Safety", "Charging frozen Li-Ion causes irreversible lithium plating & internal short risk. Warm pack indoors first."),
        Triple("Voltage Sag Health Diagnostic", "Pack Test", "Healthy 72V pack sags 2.5-4.5V under full throttle; >7V sag indicates aging cells or high-R weld."),
        Triple("Cell Delta Threshold (<30mV)", "Smart BMS", "Healthy balanced pack stays within 10-25mV delta; >80mV delta under load indicates a weak P-group."),
        Triple("Molicel P45B / P42A High-Drain", "Performance", "45A continuous per cell, superior cold-weather performance, lowest voltage sag for 72V+ eMotos/EUCs."),
        Triple("Samsung 50S vs 50E Selection", "Range vs Power", "50S handles 25A+ bursts for high-power rigs; 50E (10A) excels in large parallel touring packs."),
        Triple("LiFePO4 3.2V Pack Profile", "Cargo / Utility", "3,000+ cycles, ultra-safe thermal stability, flat discharge curve; full charge = 3.65V/cell."),
        Triple("Active Balancer 2A Upgrade (JK)", "Custom Packs", "Active inductive balancing transfers energy from high to low cells 20x faster than 50mA passive resistors."),
        Triple("Charge Port Fuse Sizing", "Fire Safety", "Install automotive/ceramic fuse rated at 1.5x max charger amps directly behind GX16/XT30 charge port."),
        Triple("Parallel Dual-Battery Diode/Switch", "Range Extender", "NEVER connect two packs in parallel unless voltages match within 0.2V—or use an ideal diode combiner."),
        Triple("Smart Charger 0.2C-0.5C Sweet Spot", "Fast Charging", "0.3C (e.g. 6A on 20Ah pack) balances fast pit stops with minimal heat stress."),
        Triple("Series Cell Count Quick Chart", "Reference", "36V=10S(42V) • 48V=13S(54.6V) • 52V=14S(58.8V) • 60V=16S(67.2V) • 72V=20S(84V) • 84V=24S(100.8V)."),
        Triple("Copper-Nickel Sandwich Busbars", "Pack Building", "Use 0.15mm copper + 0.1mm nickel strips for >120A continuous packs to eliminate strip heating."),
        Triple("Fishpaper & Kapton Chafe Guard", "Insulation", "Double-layer barley fishpaper on every positive terminal and pack edge to prevent vibration rub-through."),
        Triple("Water-Ingress Pack Quarantine", "Emergency", "If a pack is submerged, unplug immediately, store outdoors in dry sand/fireproof bin, and inspect BMS."),
        Triple("Charger Output Voltage Calibration", "Multimeter", "Verify charger open-circuit voltage matches exact series max (e.g. 67.2V ±0.2V for 16S) before plugging in."),
        Triple("Connector Amp Rating Table", "Wiring", "XT30=30A • XT60=60A • XT90-S=90A • Anderson SB50=50-120A • QS8-S=180A+ continuous."),
        Triple("Wire Gauge (AWG) Ampacity Guide", "Harness", "12 AWG=40A • 10 AWG=60A • 8 AWG=100A • 6 AWG=150A (silicone 200°C fine-strand copper)."),
        Triple("Bat-Safe Fire Containment Charging", "Indoor Safety", "Charge removable packs inside vented flame-arrestor steel box away from exits & combustibles."),
        Triple("Regen Energy Recovery Reality", "Efficiency", "Urban stop-and-go regen recovers ~5-12% energy; steep mountain descents recover up to 18-25%."),
        Triple("BMS Sleep Mode Wake-Up", "Troubleshooting", "Some smart BMS units enter deep sleep after 30 days; plug charger for 10 sec or hold power button 5s."),
        Triple("Airline TSA 100Wh/160Wh Limit", "Travel", "FAA/TSA caps carry-on Li-Ion at 100Wh (up to 160Wh with airline approval); full PEV packs must ship ground HazMat."),
        Triple("Internal Resistance (IR) Baseline", "Diagnostics", "New 21700 power cells measure 12-18 mΩ; pack needs rebuild when cell IR doubles (>40 mΩ)."),
        Triple("Temperature Probe Placement", "BMS", "Place 2+ NTC thermistors in the center core of the pack, not just outer shrink wrap, for true hot-spot reading."),
        Triple("Swappable Pack Latch Inspection", "eMoto / eBike", "Check slide-rail pins for arcing pits every 500 km and apply tiny dab of dielectric grease to housing seals."),
        Triple("Smart Plug Charge Timer", "Automation", "Use Wi-Fi energy-monitoring smart plug with auto-off timer so packs never float unattended overnight."),
        Triple("C-Rate Math Made Simple", "Formula", "Max Amps = Ah × C-Rate. Example: 30Ah pack @ 3C discharge = 90A continuous safe draw."),
        Triple("Pack Energy Wh Formula", "Formula", "Wh = Nominal Voltage × Ah. Example: 72V × 35Ah = 2,520 Wh total stored energy."),
        Triple("UL-2271 / UL-2849 Pack Standards", "Compliance", "Look for certified UL-2271 battery packs & UL-2849 e-bike systems for building/insurance compliance."),
        Triple("Balancer Lead Pinout Check", "BMS Repair", "Always verify B-, B1, B2... incremental voltage steps with a multimeter BEFORE plugging JST harness into BMS."),
        Triple("Potted vs Shrink-Wrapped Packs", "Off-Road", "Silicone-potted hard-case packs resist trail shock & mud far better than blue PVC wrap alone."),
        Triple("DC-DC 12V Buck Converter Isolation", "Lighting", "Power high-wattage headlights via fused 12V buck converter off main pack rather than controller 5V rail."),
        Triple("End-of-Life Pack Recycling", "Environment", "Tape terminals with electrical tape and drop worn packs at Call2Recycle or local hazmat facility."),
      )
    battItems.forEachIndexed { idx, (t, p, proto) ->
      list += PevLiveOpsModule("batt_$idx", "BATTERY_BMS", t, p, "BMS-${idx + 1}", proto, "$t PEV battery BMS")
    }

    // 4. SUSPENSION, TIRES & CHASSIS SETUPS (35)
    val suspItems =
      listOf(
        Triple("Static Sag 20%-25% Setup", "Coil / Air", "Sit in full riding gear; suspension should compress 20-25% of total stroke. Adjust preload or air PSI."),
        Triple("Rebound Damping Goldilocks Test", "Hydraulic Shocks", "Compress hard and release: shock should return briskly once with zero pogo bounce or sluggish pack-down."),
        Triple("PMT Italian Radial Tire Upgrade", "10\"-11\" Scooters", "True radial carcass adds 30% cornering lean grip and cuts harsh buzz vs stiff nylon stock tires."),
        Triple("Cold Tire PSI Weight Formula", "All PEVs", "Check cold PSI weekly! Under-inflation causes pinch flats & -15% range; over-inflation kills wet traction."),
        Triple("Tubeless Bead Seating Trick", "Tubeless", "Remove valve core, lubricate bead with soapy water, wrap ratchet strap around tread, blast with compressor."),
        Triple("Kevlar Tire Sealant Dosage", "Flat Guard", "4 oz per 10-11\" scooter tire, 6 oz per 20x4\" fat tire or 16-20\" EUC tire. Rotate slowly after fill."),
        Triple("High-Speed Stem Wobble Elimination", "Hyper Scooters", "Check headset bearing preload, tire balance weights, rear-biased stance, and add 24-click steering damper."),
        Triple("Wheel Dynamic Balancing Weights", "45+ mph PEVs", "Stick-on 5g motorcycle wheel weights opposite the heavy valve-stem spot eliminate 50 mph handlebar buzz."),
        Triple("Hydraulic Brake Mineral vs DOT4", "Brakes", "NEVER mix fluids! Shimano/Tektro/Magura/Zoom/Nutt = Mineral Oil; SRAM/Hope/Moto = DOT 4/5.1."),
        Triple("Rotor Trueing & 2.0mm Thickness", "Brakes", "Replace rotors worn below 1.5mm (1.8mm on 2.3mm eMoto rotors); bend minor rub gently with clean fork tool."),
        Triple("Brake Pad Bed-In Protocol", "New Pads", "Make 15 firm decelerations from 20 mph down to 5 mph (never full stop) to transfer even pad compound onto rotor."),
        Triple("Sintered vs Ceramic vs Organic Pads", "Pad Choice", "Sintered metallic = best wet/mud/high-heat stopping; Ceramic = quiet; Organic = soft initial bite, fast wear."),
        Triple("Blue Loctite 242 Critical Fasteners", "Chassis", "Degrease & apply medium Blue 242 to rotor bolts (6 Nm), caliper mounts (8 Nm), folding latch, and kickstand."),
        Triple("Axle Nut Anti-Rotation Torque Arm", "Hub Motors", "Always verify torque washers sit flush in dropout slots; torque M12/M14 axle nuts 45-65 Nm."),
        Triple("Spring Rate Calculator (lbs/in)", "Coil Shocks", "150 lb rider: 650-750 lb/in | 185 lb rider: 850-1000 lb/in | 220+ lb rider: 1200-1500 lb/in (scooter short shock)."),
        Triple("Split-Rim Bolt Cross-Torque", "Scooter Hubs", "Deflate tube 100% BEFORE unbolting split-rim halves! Reassemble in star pattern at 10-12 Nm with blue Loctite."),
        Triple("EUC Pedal Dip & Tilt Calibration", "EUC", "Calibrate gyro level on a verified flat spirit-level surface; add +1.5° forward bias for steep climbs if desired."),
        Triple("EUC Power Pad Ergonomic Lock-In", "EUC Control", "Position front jump block snug above ankle bone and rear brake pad behind calf for emergency stopping."),
        Triple("Onewheel Tire PSI Rule of Thumb", "Onewheel", "Start at 10% of rider weight in lbs (e.g. 180 lb = 18 PSI); -2 PSI for rough roots, +2 PSI for max paved range."),
        Triple("Sur-Ron / Talaria Belt Tension", "eMoto Primary", "Primary belt deflection should be 6-8mm under firm thumb press; too tight destroys jackshaft bearings."),
        Triple("420 O-Ring Chain Lube & Slack", "eMoto Final", "Keep 25-30mm chain slack on stand; clean with kerosene/brush and apply dry wax lube every 200 km."),
        Triple("Fork Stanchion Wiper Seal Care", "Suspension", "Wipe stanchions clean after dusty rides and apply tiny drop of silicone fork oil to preserve low-friction seals."),
        Triple("Dual-Crown Fork Stem Alignment", "eMoto / Fat", "Loosen lower crown bolts, pump fork 5 times to align parallel tubes, then torque evenly top to bottom."),
        Triple("Headset Bearing Repack (Marine)", "Waterproofing", "Pack upper/lower headset bearings with waterproof PTFE marine grease every 1,000 km to stop rust creaks."),
        Triple("Spoke Tension Ping Check", "eBike / eMoto", "Pluck spokes around rim every 300 km; any dull thud signals a loose spoke—quarter-turn true before rim warps."),
        Triple("Tire Tread Direction Arrow Check", "Installation", "Verify rotation arrow matches forward wheel spin so water channels evacuate outward instead of hydroplaning."),
        Triple("Folding Stem Collar Shimming", "Scooters", "Zero-play folding stem prevents fatigue cracks; adjust tension hex nut or install UHMW anti-creak shim."),
        Triple("Rear Kickplate Stance Weighting", "Scooter Braking", "Drop hips low and push heels back against rear footrest during hard braking to stop nose-dive endo."),
        Triple("Studded Winter Tire Conversion", "Ice Riding", "Run carbide-studded 20x4\" or 10\" tires at lower PSI on packed snow/ice; avoid bare dry tarmac to keep studs."),
        Triple("Steering Damper Bracket Clearance", "Safety", "Sweep handlebars full lock left and right after installing damper to confirm zero cable pinch or bind."),
        Triple("SKF 2RSH Hub Bearing Upgrade", "Reliability", "Swap cheap stock hub bearings for SKF 6001/6203-2RSH dual-lip waterproof seals to triple wet mileage."),
        Triple("Air Shock Negative Chamber Equalize", "MTB Forks", "Pump 50 PSI, cycle fork 10 times to equalize positive/negative chambers, then finish inflating to target PSI."),
        Triple("Brake Hose Routing & Abrasion Wrap", "Cockpit", "Wrap hydraulic hoses in spiral guard where they rub frame headtube to prevent pinhole leaks."),
        Triple("Rim Tape & Spoke Nipple Shield", "Tube Life", "Add 2 layers of Gorilla or tubeless rim tape over spoke heads inside fat eBike rims to stop mystery flats."),
        Triple("Tire Sidewall Dry-Rot Inspection", "Safety", "Replace tires showing micro-cracks, exposed cords, or flat squared center tread regardless of age."),
      )
    suspItems.forEachIndexed { idx, (t, p, proto) ->
      list += PevLiveOpsModule("susp_$idx", "SUSPENSION_TIRES", t, p, "CHASSIS-${idx + 1}", proto, "$t PEV tire suspension")
    }

    // 5. TRAIL WEATHER, NIGHT & EMERGENCY OPS (35)
    val safetyItems =
      listOf(
        Triple("Pre-Ride ABCDE 30-Second Check", "Every Ride", "Air (PSI), Brakes (lever firm), Controls (throttle snap-back), Drive/Nuts (axle tight), Energy (SOC %)."),
        Triple("Full-Face Helmet Certification Guide", "Gear", "Below 20 mph: ASTM F1952 DH MTB. Above 28 mph: ECE 22.06 / DOT moto helmet with MIPS rotational liner."),
        Triple("Wrist Guard & Palm Slider Physics", "EUC / Onewheel", "Low-friction palm sliders let hands slide on asphalt instead of catching and fracturing the scaphoid/radius."),
        Triple("StVZO Cutoff Headlight Aiming", "Night Ops", "Aim top horizontal beam cutoff 2° below level so road hazards glow brightly without blinding oncoming cars."),
        Triple("Dual-Zone Helmet + Deck Lighting", "Visibility", "Deck light shows potholes via shadows; helmet light illuminates corners where your eyes look."),
        Triple("Rear Radar + Brake-Sense Tail Light", "Rural Roads", "Mount high-visibility 100+ lumen rear flasher at seat/backpack height—low fender lights hide behind hills."),
        Triple("Wet Asphalt & Painted Line Hazard", "Rain Ops", "White crosswalk paint, wet leaves, and manhole covers have ~1/4 the grip of dry tarmac; ride upright across them."),
        Triple("Puddle Depth Motor Axle Limit", "Water Safety", "Never ride through standing water deeper than bottom rim edge; submerged hubs suck water in as they cool."),
        Triple("Post-Rain Dry-Out Routine", "IPX Maintenance", "Never flip a wet scooter upside down (drains water into display/controller); towel dry upright with fan."),
        Triple("Fall-Detection SOS Auto-Check", "Safety", "Keep fall-detection armed on solo rural rides so a hard impact triggers 15-sec countdown & GPS SMS alert."),
        Triple("Compact Trailside Repair Kit", "Carry Pack", "Pack 3-6mm hex keys, 18mm axle wrench, CO2/mini electric pump, bacon strips, zip ties, and electrical tape."),
        Triple("Emergency Tubeless Plug Procedure", "Flat Fix", "Locate nail, ream hole, thread tacky bacon strip halfway in with fork tool, pull swiftly, reinflate to PSI."),
        Triple("Overheated Brake Fade Recovery", "Mountain", "If brake lever goes spongy on steep descent, pull over 5 min to cool rotors; alternate front/rear braking."),
        Triple("Loose Dog Encounter Protocol", "Rural Roads", "Do not panic-swerve into traffic; either smoothly accelerate past cleanly or slow/dismount putting PEV between you."),
        Triple("Deer & Wildlife Dusk Scanning", "Country Night", "Scan roadside ditches for eye-shine at dusk/dawn; deer travel in groups—if one crosses, expect two more."),
        Triple("Heatstroke & Rider Hydration Math", "Summer", "Drink 500 mL water/electrolytes per hour in >30°C armored rides; ventilated mesh CE-2 jacket beats bare skin."),
        Triple("Wind-Chill Layering Formula", "Cold Rides", "At 30 mph, 45°F air feels like 32°F! Wear windproof membrane outer shell + heated or neoprene bar mitts."),
        Triple("Fog & Mist Visor Anti-Fog Prep", "Weather", "Use Pinlock insert or tiny drop of dish-soap buff on inside of visor so helmet stays crystal clear in fog."),
        Triple("Driver Eye-Contact Intersection Rule", "Urban", "Assume cars turning left do not see you—cover both brake levers and watch their front wheel rotation."),
        Triple("Door-Zone 4-Foot Buffer", "City", "Ride at least 1.2 meters (4 ft) left of parked cars so a sudden swinging door cannot clothesline you."),
        Triple("Emergency One-Handed Signal Caution", "Scooters", "Short-wheelbase scooters destabilize one-handed; use helmet/bar turn signals or leg-out signals instead."),
        Triple("Thermal Runaway Warning Signs", "Fire Safety", "Hissing sound, sweet chemical smell, swelling pack, or rapid smoke = evacuate immediately outdoors & call 911."),
        Triple("Class D / Water Battery Fire Fact", "Fire Ops", "Standard ABC extinguishers cannot stop Li-Ion thermal runaway; only massive water cooling or containment works."),
        Triple("Medical ICE Tag on Helmet", "Emergency", "Place waterproof ICE (In Case of Emergency) sticker on helmet side with blood type, allergies, and contact."),
        Triple("High-Vis Retroreflective Ankle Bands", "Night", "Biomotion reflective straps on ankles & elbows catch headlights 3x farther away than static dark jackets."),
        Triple("Cross-Traffic Gravel Driveway Watch", "Rural", "Watch for loose pea-gravel spilled onto paved corners from country driveways—brake BEFORE entering turn."),
        Triple("Group Ride Staggered Formation", "Convoy", "Ride staggered left/right with 2-second following gap; never overlap wheels with the rider ahead."),
        Triple("Sweep Rider Radio Protocol", "Walkie-Talkie", "Keep Walkie-Talkie PTT channel active; sweep immediately calls 'Hold Up' if any rider flats or drops back."),
        Triple("Lightning 30/30 Safety Rule", "Storms", "Wait 30 full minutes after the last thunderclap before resuming exposed ridgeline or open-field riding."),
        Triple("Sun-Glare High-Vis HUD Switch", "Daylight", "Toggle Sun High-Vis mode in Cockpit quick-bar for crisp black-on-white telemetry readability at noon."),
        Triple("Night Vision Red Cockpit Mode", "Night", "Enable Night Red mode to preserve scotopic dark adaptation on pitch-black rural trails."),
        Triple("Emergency Tow / Walk-Assist Mode", "Breakdown", "Use 3 mph walk-assist or push with motor powered off (spinning hub smoothly) to avoid back-EMF drag."),
        Triple("First-Aid Trail Trauma Micro-Kit", "Backcountry", "Carry sterile gauze, antiseptic wipes, cohesive wrap, and emergency foil blanket in compact frame bag."),
        Triple("Noise & Wildlife Respect Zones", "Trails", "Slow near horses on shared rural trails, speak calmly so the horse recognizes you as human, pass wide."),
        Triple("Post-Crash Frame & Fork Audit", "Inspection", "After any spill >15 mph, inspect stem weld, fork crown, rotor straightness, and battery case before riding."),
      )
    safetyItems.forEachIndexed { idx, (t, p, proto) ->
      list += PevLiveOpsModule("safe_$idx", "TRAIL_SAFETY", t, p, "SAFE-${idx + 1}", proto, "$t PEV safety gear")
    }

    // 6. GARAGE MAINTENANCE & UPGRADE BLUEPRINTS (30)
    val garageItems =
      listOf(
        Triple("Silicone Deck & Cable Waterproofing", "Scooters", "Seal deck perimeter & cable grommets with neutral-cure RTV silicone (avoid acetic acid near copper wires)."),
        Triple("Conformal Coating PCB Protection", "Controllers", "Brush silicone conformal coating over display/controller PCBs (mask connectors & buttons) for IPX6 resilience."),
        Triple("Hydraulic Brake Bleed Syringe Flow", "Maintenance", "Attach funnel at lever, push bubble-free fluid upward from caliper syringe, flick lever 10x to purge air."),
        Triple("Caliper Piston Sticky-Seal Reset", "Brakes", "Extend pistons 2mm, clean edges with isopropyl + Q-tip, lubricate with matching brake fluid, press flush."),
        Triple("Split-Rim Inner Tube Pinch-Free Install", "Tires", "Lightly inflate tube until round, dust with talc/baby powder, seat inside tire, check no pinch before bolting."),
        Triple("Motor Phase Bullet-to-MR60 Upgrade", "Electrical", "Replace melted 3.5mm bullets with soldered MR60/MT60 3-pin connectors & dual-wall adhesive heatshrink."),
        Triple("Multimeter Continuity & Rail Check", "Diagnostics", "Test 5V Hall rail (Red-Black = 4.8-5.2V) and rotate wheel slowly; Blue/Green/Yellow should toggle 0V <-> 5V."),
        Triple("Throttle Swap Pinout Verification", "Electrical", "Always verify Red (+5V), Black (GND), Green/White (Signal) with meter—some Chinese looms swap colors!"),
        Triple("12V High-Output Motorcycle Headlight", "Mod", "Wire fused 10A 12V step-down buck converter with handlebar toggle switch for real 2,500+ lumen trail lighting."),
        Triple("GPS Tracker Stealth Frame Hide", "Anti-Theft", "Conceal LTE/AirTag tracker inside plastic/non-shielded neck fairing (carbon/thick aluminum blocks RF signal)."),
        Triple("Carbon Fiber Bar & Stem Torque Guard", "Cockpit", "Always use carbon assembly grit paste and torque wrench (4-5 Nm max) to prevent crushing carbon bars."),
        Triple("Thumb-to-Twist Throttle Conversion", "Ergonomics", "Half-twist Hall throttle reduces thumb fatigue on 30+ mile tours while retaining full grip over rough bumps."),
        Triple("Hydraulic Horn 115dB Wiring", "Safety Mod", "Run 12V car/moto snail horn through relay off buck converter—draws 3-4A, never wire directly to 5V logic."),
        Triple("Deck Grip Tape Custom Replace", "Scooters", "Warm old grip with hair dryer, peel cleanly, wipe deck with 91% isopropyl, roll fresh silicone/36-grit grip."),
        Triple("CNC Kickstand Heavy-Duty Upgrade", "Heavy PEVs", "Upgrade stamped kickstand on 90+ lb scooters to billet CNC adjustable stand with red/blue Loctite bolts."),
        Triple("Brake Cutoff HIGO 2-Pin Magnet Mod", "Upgrades", "When upgrading to Shimano/SRAM MTB brakes, glue neodymium magnet + reed sensor on lever for instant e-cutoff."),
        Triple("Battery Deck Foam Vibration Isolation", "Pack Care", "Line battery bay floor & walls with 5mm closed-cell EVA foam so pack cannot rattle during jumps."),
        Triple("Axle Nut Stripped Thread Recovery", "Repair", "Use M12x1.25 / M14x1.5 split thread-chaser die (never standard coarse die) to restore fine-pitch hub threads."),
        Triple("USB-C PD 65W Cockpit Phone Charger", "Touring Mod", "Tap main battery via waterproof 24-90V to USB-C PD buck module so navigation phone stays at 100% all day."),
        Triple("Mid-Drive Nylon Gear Grease Service", "Bafang / eBike", "Repack Bafang reduction gears every 2,500 km with Mobilgrease 28 (synthetic aviation clay grease, nylon-safe)."),
        Triple("Fender Rattle & Crack Reinforcement", "Scooters", "Add rubber washer grommets under fender bolts and aluminum brace strip to stop plastic fatigue snaps."),
        Triple("Display Odometer & P-Setting Backup", "Reference", "Photo every P-setting (P0-P20: wheel size, magnets, voltage, start mode, regen) before resetting controller."),
        Triple("Torque Wrench Click Calibration", "Shop Tools", "Always dial spring torque wrench back to lowest setting after use to preserve internal spring accuracy."),
        Triple("Wheel Alignment Laser / String Check", "eMoto / eBike", "Measure rear axle blocks equally on both swingarm sides so chain/belt tracks dead straight with zero squeal."),
        Triple("Dielectric Grease Connector Armor", "Weatherproof", "Apply thin film of dielectric silicone grease on exterior lighting/display plug seals to block salt corrosion."),
        Triple("Rim Dent Straightening Inspection", "Off-Road", "Minor aluminum bead dings can be gently dressed with crescent wrench + rag; deep cracks require rim swap."),
        Triple("Suspension Linkage Pivot Bearing Lube", "Full-Susp", "Disassemble rear shock linkage yearly, clean mud grit, grease bushings, and re-torque to spec."),
        Triple("Custom Splash Guard Mud Flap", "All-Weather", "Cut flexible 2mm rubber mud flap extension for front fender to keep water spray off lower headset & deck."),
        Triple("500km / 1500km Service Interval Log", "Fleet Care", "Log every tire swap, brake bleed, and bearing service in Garage Service Tracker to maximize resale value."),
        Triple("Pre-Sale Detailing & SOH Certificate", "Resale", "Include full-charge voltage photo, cell delta screenshot, and ride log history for top used PEV value."),
      )
    garageItems.forEachIndexed { idx, (t, p, proto) ->
      list += PevLiveOpsModule("gar_$idx", "GARAGE_MODS", t, p, "MOD-${idx + 1}", proto, "$t PEV tool kit")
    }

    list
  }
}
