package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.CopilotPersonaOption
import com.example.data.CopilotSkillOption
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalDialog
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.tacticalOutlinedTextFieldColors
import com.example.ui.theme.LocalTacticalChrome

object CopilotProCatalog {

  val personas12: List<CopilotPersonaOption> =
    listOf(
      CopilotPersonaOption("coach", "Tactical Coach", "Crisp telemetry pacing & range strategy", "You are Rural ERides Tactical Coach. Analyze live GPS/battery stats accurately."),
      CopilotPersonaOption("mech", "Senior E-Moto Mechanic", "High-voltage diagnostics & torque precision", "You are a Master High-Voltage PEV & E-Moto Mechanic. Give torque specs and multimeter tests."),
      CopilotPersonaOption("nav", "Trail Navigator", "Backcountry terrain, wind & elevation awareness", "You are a Backcountry PEV Trail Navigator focusing on weather, elevation, and route safety."),
      CopilotPersonaOption("medic", "Emergency Medic", "Trauma & backcountry triage protocol", "You are a Wilderness Trail Medic. Provide calm, urgent stabilization and SOS dispatch steps."),
      CopilotPersonaOption("batt", "Battery Engineer", "Li-ion sag, thermal runaway & BMS expert", "You are a Lithium Battery & BMS Engineer specializing in 16S-32S cells and discharge physics."),
      CopilotPersonaOption("race", "Race Strategist", "Line selection, aero tuck & apex speed", "You are a PEV Track & Race Strategist maximizing corner speed and regen efficiency."),
      CopilotPersonaOption("chaplain", "Trail Chaplain", "Encouragement, calm focus & rider safety", "You are an encouraging Trail Chaplain offering grounded wisdom and rider protection prayer."),
      CopilotPersonaOption("vesc", "VESC Tuner", "FOC motor detection, field weakening & duty limits", "You are a VESC 6/75V/100V Firmware & FOC Tuning Specialist. Give safe phase/battery amp, ERPM, and thermal ramp settings."),
      CopilotPersonaOption("susp", "Suspension Tech", "Spring rates, preload, rebound & damping setup", "You are a PEV & E-Moto Suspension Engineer tuning coil/air shocks, fork oil weight, sag, and steering dampers."),
      CopilotPersonaOption("legal", "PEV Legal Counsel", "Class 1/2/3 e-bike, scooter & trail access rules", "You are a PEV Mobility Policy Specialist explaining US/state e-bike/scooter classifications, speed limits, and trail etiquette clearly."),
      CopilotPersonaOption("euc", "EUC & Onewheel Master", "Self-balancing PWM headroom, carving & pedal angle", "You are an Electric Unicycle and Onewheel Technical Coach specializing in PWM safety margins, power pads, and off-road balance."),
      CopilotPersonaOption("media", "PEV Content Creator", "Ride story scripts, YouTube hooks & viral reels", "You are a PEV Media Producer writing punchy video scripts, titles, captions, and thumbnail ideas for electric ride channels."),
      CopilotPersonaOption("rural_boss", "Country Trail Boss", "Warm backroad hospitality, gravel routes & homestead pacing", "You are the Country Trail Boss for Rural ERides GO. Speak with warm, neighborly rural hospitality that makes the rider feel at home on every dirt road, country lane, or city street. Give sharp PEV range, gravel traction, and trail advice."),
      CopilotPersonaOption("barn_grandpa", "Barn Shop Grandpa", "Old-school barn-bench wrenching & steady practical wisdom", "You are Barn Shop Grandpa, a kindly veteran country mechanic who loves electric rides. Speak with warm rural storytelling, steady encouragement, and rock-solid practical maintenance advice."),
      CopilotPersonaOption("rancher_guide", "Backroad Rancher Guide", "Countryside weather, pasture roads & creek crossings", "You are a Backroad Rancher Guide helping PEV riders read country skies, mud ruts, fence-line windbreaks, and safe rural routes with warm down-home friendliness."),
      CopilotPersonaOption("creek_scout", "Gravel Creek Scout", "Singletrack, pine hollers, ford depth & knobby tire grip", "You are the Gravel Creek Scout. Blend warm country porch camaraderie with tactical backcountry EUC, eMoto, and scooter dirt-trail expertise."),
      CopilotPersonaOption("solar_homestead", "Homestead Solar Tech", "Off-grid LiFePO4 solar banks, barn outlets & inverter math", "You are an Off-Grid Homestead Solar Tech. Help riders charge PEVs cleanly off solar arrays, barn 120V circuits, and portable power stations with friendly country clarity."),
      CopilotPersonaOption("county_sheriff", "County Road Steward", "Friendly rural road visibility, farm-equipment safety & local laws", "You are a friendly County Road Steward. Help riders stay safe around tractors, grain trucks, blind country hills, and local PEV ordinances with neighborly respect."),
      CopilotPersonaOption("ridge_medic", "Appalachian Ridge Medic", "Calm backcountry first-aid & remote county rescue triage", "You are an Appalachian Ridge Trail Medic. Offer steady, reassuring rural care and crisp step-by-step remote emergency triage when far from town."),
      CopilotPersonaOption("holler_convoy", "Midnight Holler Convoy", "CB-radio style night convoy pacing, headlights & tail sweep", "You are the Midnight Holler Convoy Lead. Speak with upbeat CB-radio country camaraderie while coordinating safe night lighting, battery stops, and group pacing."),
      CopilotPersonaOption("appalachian_guide", "Appalachian Ridge Guide", "Steep holler grades, regen thermal management & rocky switchbacks", "You are an Appalachian Ridge PEV Guide. Focus on steep hill-climb watts, motor thermal safety, switchback line choice, and mountain weather."),
      CopilotPersonaOption("midwest_gravel", "Midwest Gravel Scout", "Long county section-line roads, crosswinds & crushed limestone", "You are a Midwest Gravel PEV Scout specializing in long flat county gravel efficiency, crosswind drag reduction, and loose limestone stability."),
      CopilotPersonaOption("ozark_creek", "Ozark Creek Tech", "Low-water crossings, clay mud recovery & waterproof hub sealing", "You are an Ozark Creek PEV Tech focusing on IP67 waterproofing, post-creek bearing maintenance, and slick red-clay riding."),
      CopilotPersonaOption("rocky_climb", "Rocky Mountain Strategist", "High-altitude thin air cooling, long descents & brake fade prevention", "You are a Rocky Mountain PEV Strategist helping riders manage brake fade on 2,000ft descents and pack voltage sag in mountain cold."),
      CopilotPersonaOption("desert_nomad", "Desert Overland Nomad", "High-ambient heat protection, sand washes & solar pack refills", "You are a Desert Overland PEV Nomad specializing in heat-soak prevention, deep sand tire PSI, hydration planning, and portable solar charging."),
      CopilotPersonaOption("piney_night", "Piney Woods Night Rider", "Amber fog optics, wildlife avoidance & solo woods navigation", "You are a Piney Woods Night PEV Rider focusing on helmet/bar lighting angles, dusk deer awareness, and calm woods trail craft."),
      CopilotPersonaOption("farmstead_fleet", "Farmstead Fleet Manager", "Multi-rig barn charging, breaker load balancing & family fleet care", "You are a Farmstead PEV Fleet Manager optimizing multi-rig barn 15A/20A charging schedules, torque inspections, and cost-per-mile ROI."),
      CopilotPersonaOption("backcountry_medic", "Backcountry First Responder", "Remote county GPS extraction, crash stabilization & zero-cell SOS", "You are a Backcountry First Responder providing calm trauma stabilization, remote GPS relay formatting, and zero-cell emergency steps."),
    )

  val skillCategories12: List<String> =
    listOf(
      "All",
      "Rural Country",
      "Safety",
      "Battery",
      "Thermal",
      "Workshop",
      "Legal",
      "Radar",
      "Tuning",
      "Finance",
      "Trail & Off-Road",
      "EUC & Onewheel",
      "Mods & Builds",
      "Social & Media",
    )

  val skills100: List<CopilotSkillOption> =
    listOf(
      // Safety (8)
      CopilotSkillOption("s1", "Pre-Ride Safety Audit", "Safety", "Run a complete 6-point pre-ride PEV check using my live battery and weather."),
      CopilotSkillOption("s2", "Wet Trail Braking", "Safety", "Give regenerative and hydraulic brake modulation guidance for wet gravel."),
      CopilotSkillOption("s3", "Emergency SOS Protocol", "Safety", "Provide immediate backcountry crash triage steps and SOS readiness."),
      CopilotSkillOption("s4", "Night Visibility Check", "Safety", "Recommend headlight cutoff and reflective gear visibility spacing."),
      CopilotSkillOption("s5", "High-Speed Wobble Cure", "Safety", "Diagnose steering damper, tire balance, and weight distribution fixes for speed wobbles."),
      CopilotSkillOption("s6", "PPE Armor Grading", "Safety", "Compare ECE 22.06 full-face helmets, D3O chest protectors, and wrist guards for 45+ MPH rides."),
      CopilotSkillOption("s7", "Rural Traffic Lane Strategy", "Safety", "Advise safe shoulder vs lane positioning on 45-55 MPH rural two-lane roads."),
      CopilotSkillOption("s8", "Dog & Wildlife Encounter Plan", "Safety", "Best evasive speed, horn, and line choices when encountering loose dogs or deer on rural roads."),
      // Battery (9)
      CopilotSkillOption("b1", "Remaining Range Calc", "Battery", "Estimate remaining real-world range from my current battery percentage and speed."),
      CopilotSkillOption("b2", "Voltage Sag Diagnostic", "Battery", "Explain if my voltage drop under throttle indicates cell imbalance or aged pack."),
      CopilotSkillOption("b3", "Winter Battery Storage", "Battery", "Give exact 50% storage voltage and cold-charging lithium safety rules."),
      CopilotSkillOption("b4", "Charger Amp Selection", "Battery", "Recommend safe fast-charge amperage without overheating 21700 cells."),
      CopilotSkillOption("b5", "Cell Balance Protocol", "Battery", "Explain how top-balancing at 100% works for smart BMS packs and when to do it."),
      CopilotSkillOption("b6", "NMC vs LFP Cutoff", "Battery", "Give exact LVC low-voltage cutoff settings for my pack voltage and chemistry."),
      CopilotSkillOption("b7", "21700 Cell Comparison", "Battery", "Compare Molicel P45B, Samsung 50S, and LG M50LT for high-discharge PEV packs."),
      CopilotSkillOption("b8", "Dual-Port Charge Safety", "Battery", "Explain safe dual-charger amp limits, diode checks, and charge port fusing."),
      CopilotSkillOption("b9", "Cycle Life Preservation", "Battery", "Give an 80% daily charge vs 100% range trip schedule to double battery lifespan."),
      // Thermal (8)
      CopilotSkillOption("t1", "Controller Heat Check", "Thermal", "Evaluate safe controller MOSFET and motor stator cutoff temperatures."),
      CopilotSkillOption("t2", "Statorade Cooling Guide", "Thermal", "Assess ferrofluid stator cooling gains for steep rural hill climbs."),
      CopilotSkillOption("t3", "Summer Asphalt Derate", "Thermal", "Estimate motor and ESC heat soak on 95F steep climbs."),
      CopilotSkillOption("t4", "Brake Fade Recovery", "Thermal", "Advise cooling stops and sintered pad upgrades after mountain descents."),
      CopilotSkillOption("t5", "Thermal Pad Swap", "Thermal", "Guide replacing dry MOSFET thermal pads on high-wattage ESC heatsinks."),
      CopilotSkillOption("t6", "Thermal Runaway Signs", "Thermal", "List immediate sensory signs of lithium pack venting and emergency isolation steps."),
      CopilotSkillOption("t7", "HubSink Fin Installation", "Thermal", "Explain how external motor hub cooling fins reduce continuous winding temps."),
      CopilotSkillOption("t8", "Sub-Zero Ride Warmup", "Thermal", "How to safely pre-warm a lithium pack and limit amps below 32F / 0C."),
      // Workshop (9)
      CopilotSkillOption("w1", "Hydraulic Brake Bleed", "Workshop", "Walk me through mineral oil vs DOT brake bleed steps cleanly."),
      CopilotSkillOption("w2", "Flat Tire Field Plug", "Workshop", "Guide quick roadside tubeless tire plug and CO2 inflation repair."),
      CopilotSkillOption("w3", "Bearing Press Inspection", "Workshop", "How to test hub motor 6003/6004 bearings for water ingress or play."),
      CopilotSkillOption("w4", "Locknut Loctite Checklist", "Workshop", "List every critical fastener needing Blue 243 threadlocker after assembly."),
      CopilotSkillOption("w5", "Hall Sensor Ohm Test", "Workshop", "Step-by-step multimeter voltage test for 5V motor Hall sensors."),
      CopilotSkillOption("w6", "IP66 Waterproof Sealing", "Workshop", "Best neutral-cure silicone and cable gland sealing spots for deck waterproofing."),
      CopilotSkillOption("w7", "Rotor Truing & Rub Fix", "Workshop", "Step-by-step disc brake caliper centering and bent rotor truing guide."),
      CopilotSkillOption("w8", "Headset Play Tightening", "Workshop", "Fix steering stem knock and preload headset bearings without binding."),
      CopilotSkillOption("w9", "QS8 / XT90 Solder Guide", "Workshop", "Proper high-current 8AWG/10AWG connector soldering and heat-shrink insulation."),
      // Legal (8)
      CopilotSkillOption("l1", "PEV Class Law Brief", "Legal", "Summarize US Class 1/2/3 e-bike and scooter trail wattage/speed regulations."),
      CopilotSkillOption("l2", "School Zone Speed Rule", "Legal", "Check typical 15-20 MPH active school zone speed governor requirements."),
      CopilotSkillOption("l3", "Sidewalk vs Bike Lane", "Legal", "Clarify etiquette and legal lane positioning when commuting through towns."),
      CopilotSkillOption("l4", "Trail Wattage Compliance", "Legal", "Explain how 750W nominal vs peak output is treated on multi-use trails."),
      CopilotSkillOption("l5", "Lighting & Bell Mandate", "Legal", "List required night lumen front/rear lights and audible warning gear."),
      CopilotSkillOption("l6", "Incident Documentation", "Legal", "What telemetry and photo evidence to capture after a road incident."),
      CopilotSkillOption("l7", "UL 2272 / UL 2849 Standards", "Legal", "Explain UL battery & charger certifications required by landlords and insurers."),
      CopilotSkillOption("l8", "PEV Theft Recovery Kit", "Legal", "Best hidden AirTag/GPS tracker placement, serial logging, and police report checklist."),
      // Radar (8)
      CopilotSkillOption("r1", "Group Convoy Spacing", "Radar", "Recommend safe staggered convoy spacing and hand signals for 4 riders."),
      CopilotSkillOption("r2", "Doppler Storm Strategy", "Radar", "Plan detour and waterproofing steps when a rain cell hits within 30 minutes."),
      CopilotSkillOption("r3", "Geo-Fence Camp Setup", "Radar", "Recommend perimeter radius and SOS check-in cadence for backcountry loops."),
      CopilotSkillOption("r4", "Sweep & Lead Roles", "Radar", "Assign lead scout and tail sweep duties for mixed-battery group rides."),
      CopilotSkillOption("r5", "Charging Waypoint Plan", "Radar", "Calculate stop duration using fast charger amps to add 40% range."),
      CopilotSkillOption("r6", "Radio PTT Brevity", "Radar", "Concise 5-word walkie-talkie callouts for potholes, cars back, and turns."),
      CopilotSkillOption("r7", "Headwind Range Buffer", "Radar", "Calculate Wh/mi penalty for riding into a 15-25 MPH headwind on open rural roads."),
      CopilotSkillOption("r8", "Elevation Gain Wh Math", "Radar", "Estimate extra watt-hours needed for a 1,500 ft climb and regen recovery downhill."),
      // Tuning (9)
      CopilotSkillOption("n1", "VESC Duty Cycle Limit", "Tuning", "Explain safe 85% duty cycle tiltback margins to prevent cutouts."),
      CopilotSkillOption("n2", "Phase vs Battery Amps", "Tuning", "Tune phase amp boost vs battery current limits for torque without sag."),
      CopilotSkillOption("n3", "Field Weakening Tradeoff", "Tuning", "Explain high-RPM field weakening heat and Wh/km efficiency impact."),
      CopilotSkillOption("n4", "Regen Curve Calibration", "Tuning", "Balance progressive regen braking strength for wet vs dry grip."),
      CopilotSkillOption("n5", "Tire Pressure Tuning", "Tuning", "Calculate optimal cold PSI for mixed rural asphalt and gravel trails."),
      CopilotSkillOption("n6", "Suspension Sag Setup", "Tuning", "Set 20-25% static rider sag and rebound damping on coil/air shocks."),
      CopilotSkillOption("n7", "Throttle Exponential Curve", "Tuning", "Design a smooth deadband and expo throttle curve for jerky dual-motor scooters."),
      CopilotSkillOption("n8", "Dual-Motor Torque Split", "Tuning", "Optimize front vs rear traction split (e.g. 40/60) to eliminate front wheel spin."),
      CopilotSkillOption("n9", "Steering Damper Dial-In", "Tuning", "Set hydraulic damper clicks for low-speed agility vs 50 MPH highway stability."),
      // Finance (7)
      CopilotSkillOption("f1", "Mission Fuel ROI", "Finance", "Compare my PEV electricity cost against a 22 MPG car over 1,000 miles."),
      CopilotSkillOption("f2", "Tire Cost Per Mile", "Finance", "Compare PMT radial longevity vs budget nylon tires per 1,000 miles."),
      CopilotSkillOption("f3", "Battery Replacement Plan", "Finance", "Estimate cost-per-cycle depreciation on a 72V 40Ah pack."),
      CopilotSkillOption("f4", "Courier Commuter Payback", "Finance", "Calculate break-even months replacing a 30-mile daily car commute."),
      CopilotSkillOption("f5", "Solar Off-Grid Charging", "Finance", "Size a portable LiFePO4 power station + solar panel to recharge my PEV."),
      CopilotSkillOption("f6", "Resale Value Audit", "Finance", "How documented maintenance logs boost secondary market PEV resale value."),
      CopilotSkillOption("f7", "Used PEV Inspection Script", "Finance", "Give a 10-point buyer checklist for inspecting a used high-voltage PEV before paying."),
      // Trail & Off-Road (9) — NEW
      CopilotSkillOption("tr1", "Loose Gravel Cornering", "Trail & Off-Road", "Body weight, counter-steering, and rear-brake technique for loose rural gravel turns."),
      CopilotSkillOption("tr2", "Mud & Rut Line Choice", "Trail & Off-Road", "How to cross muddy farm tracks without bogging down hub motors or slipping."),
      CopilotSkillOption("tr3", "Steep Climb Traction", "Trail & Off-Road", "Standing posture and throttle feathering for steep loose-dirt climbs on dual-motor PEVs."),
      CopilotSkillOption("tr4", "Water Crossing Safety", "Trail & Off-Road", "Maximum safe water depth vs axle/bearing seals and post-splash drying routine."),
      CopilotSkillOption("tr5", "Knobby vs Hybrid Tires", "Trail & Off-Road", "Compare off-road knobby vs all-terrain hybrid tires for 60% asphalt / 40% gravel riding."),
      CopilotSkillOption("tr6", "Backcountry Tool Roll", "Trail & Off-Road", "Build a lightweight under-2lb rural trail emergency tool kit for scooters and e-motos."),
      CopilotSkillOption("tr7", "Sand & Soft Shoulder Recovery", "Trail & Off-Road", "Exact reaction steps if your front wheel drops onto a soft gravel or sand shoulder at 30 MPH."),
      CopilotSkillOption("tr8", "Night Trail Light Aiming", "Trail & Off-Road", "Combine helmet spot beam + bar flood beam for pitch-black rural trail visibility."),
      CopilotSkillOption("tr9", "Chain vs Belt Drive Mud Care", "Trail & Off-Road", "Post-trail cleaning and lubrication for Sur-Ron/Talaria chains and primary belts."),
      // EUC & Onewheel (9) — NEW
      CopilotSkillOption("eu1", "PWM Alarm Ladder Setup", "EUC & Onewheel", "Configure 75% / 80% / 85% PWM beeps and tiltback safely for high-speed EUCs."),
      CopilotSkillOption("eu2", "Seated EUC Riding Posture", "EUC & Onewheel", "Step-by-step drills for smooth seated transitions, braking, and wobble control on an EUC."),
      CopilotSkillOption("eu3", "Power Pad Placement", "EUC & Onewheel", "Position jump pads and brake pads for torque leverage without calf fatigue."),
      CopilotSkillOption("eu4", "EUC Pedal Dip Calibration", "EUC & Onewheel", "Tune soft/medium/hard pedal mode and forward angle for climbs vs long cruises."),
      CopilotSkillOption("eu5", "Onewheel Nosedive Prevention", "EUC & Onewheel", "Explain pushback, duty-cycle surge, and uphill acceleration limits to avoid nosedives."),
      CopilotSkillOption("eu6", "EUC Suspension Linkage Care", "EUC & Onewheel", "Inspect slider bushings, stanchions, and air shock PSI on suspension EUCs."),
      CopilotSkillOption("eu7", "High-Wind Crosswind Stance", "EUC & Onewheel", "Body lean and carving tactics for 25+ MPH gusting crosswinds on an EUC."),
      CopilotSkillOption("eu8", "EUC Fast-Charge Etiquette", "EUC & Onewheel", "Use J1772 EV adapter cables safely with high-amp EUC chargers without tripping breakers."),
      CopilotSkillOption("eu9", "Tire Swap Bead Alignment", "EUC & Onewheel", "Centering stubborn tubeless EUC tires using soapy water and progressive PSI seating."),
      // Mods & Builds (9) — NEW
      CopilotSkillOption("mb1", "60V to 72V Upgrade Audit", "Mods & Builds", "Verify controller MOSFET voltage rating, BMS, charger, and DC-DC buck converter before upgrading to 72V."),
      CopilotSkillOption("mb2", "4-Piston Hydraulic Brake Swap", "Mods & Builds", "Plan caliper adapter brackets, 160mm/180mm/203mm rotors, and e-brake cutoff sensors."),
      CopilotSkillOption("mb3", "12V Auxiliary Lighting Bus", "Mods & Builds", "Wire an isolated 12V 10A step-down buck converter with blade fuse for headlights and horn."),
      CopilotSkillOption("mb4", "Custom Carbon Deck & Grip", "Mods & Builds", "Choose coarse silicon-carbide grip tape, footrest angles, and silicone deck gaskets."),
      CopilotSkillOption("mb5", "External Range Extender Pack", "Mods & Builds", "Safe parallel battery pack matching (within 0.1V) and ideal diode balancer wiring."),
      CopilotSkillOption("mb6", "Motor Phase Wire Upgrade", "Mods & Builds", "When to upgrade phase wires and replace bullet plugs with MR60/MT60 or terminal blocks."),
      CopilotSkillOption("mb7", "Twist vs Thumb Throttle Mod", "Mods & Builds", "Compare Hall sensor half-twist, thumb, and index trigger throttles with 5V pinout check."),
      CopilotSkillOption("mb8", "Handlebar Width & Rise Fit", "Mods & Builds", "Ergonomic riser bar, stem length, and ergonomic lock-on grips to cure wrist numbness."),
      CopilotSkillOption("mb9", "Custom Build BOM Planner", "Mods & Builds", "Draft a balanced $1,500-$3,000 PEV upgrade roadmap prioritized by safety, brakes, tires, then power."),
      // Social & Media (7) — NEW
      CopilotSkillOption("sm1", "YouTube Ride Video Script", "Social & Media", "Write a 60-second hook + chapter outline for my latest Rural ERides GO trail video."),
      CopilotSkillOption("sm2", "Instagram & TikTok Caption", "Social & Media", "Draft 3 high-engagement captions + PEV hashtags using my live telemetry stats."),
      CopilotSkillOption("sm3", "Action Cam Mount Setup", "Social & Media", "Best chin-mount, chest-mount, and 360 selfie-stick angles with wind-noise reduction."),
      CopilotSkillOption("sm4", "Group Ride Flyer Copy", "Social & Media", "Write a clear, hype group ride announcement with pace class, charger stop, and safety rules."),
      CopilotSkillOption("sm5", "PEV Review Scorecard", "Social & Media", "Generate a structured 10-category PEV review rubric (range, sag, brakes, suspension, IP rating)."),
      CopilotSkillOption("sm6", "Marketplace Listing Optimizer", "Social & Media", "Rewrite a PEV or part for-sale listing to highlight battery health, upgrades, and fair value."),
      CopilotSkillOption("sm7", "Stolen PEV BOLO Alert Writer", "Social & Media", "Format an urgent, shareable Stolen PEV BOLO post for Facebook, X, Reddit, and local groups."),
      // Rural Country (8) — NEW
      CopilotSkillOption("rc1", "Gravel & Creek Ford Prep", "Rural Country", "Give me a neighborly rural checklist for crossing shallow creeks and washboard gravel safely on my PEV."),
      CopilotSkillOption("rc2", "Barn 120V & Solar Charging", "Rural Country", "Calculate safe breaker amps and portable solar panel sizing for charging my PEV in a rural barn or campsite."),
      CopilotSkillOption("rc3", "Farm Equipment & Livestock Etiquette", "Rural Country", "How to safely pass tractors, hay trailers, horses, and loose farm dogs on narrow county roads."),
      CopilotSkillOption("rc4", "Barn-Bench Winterization", "Rural Country", "Step-by-step cold-barn storage voltage, tire pressure, and moisture protection plan for winter months."),
      CopilotSkillOption("rc5", "Country Dusk & Fog Lighting", "Rural Country", "Best amber fog beam, headlight cutoff, and taillight visibility tips for unlit rural two-lane roads."),
      CopilotSkillOption("rc6", "Mud-Packed Hub Cleanup", "Rural Country", "Safe post-ride cleaning routine for clay mud around hub seals, brake calipers, and folding hinges without pressure-washer damage."),
      CopilotSkillOption("rc7", "Backroad Emergency Kit", "Rural Country", "Pack a compact saddlebag kit for riding 15+ miles from town on rural backroads with spotty cell service."),
      CopilotSkillOption("rc8", "Country Porch Ride Story", "Rural Country", "Turn today's live PEV telemetry and weather into a warm, down-home rural trail journal entry I can share."),
      CopilotSkillOption("rc9", "Crushed Limestone Tire PSI", "Rural Country", "Calculate optimal front and rear tire PSI for fresh crushed county limestone based on my rig and weight."),
      CopilotSkillOption("rc10", "Washboard Gravel Stance", "Rural Country", "Teach me exact knee-bend, grip pressure, and speed rhythm to glide smoothly over washboard gravel roads."),
      CopilotSkillOption("rc11", "One-Lane Bridge Protocol", "Rural Country", "Best visibility, yield checks, and wooden plank safety rules for narrow rural covered and steel bridges."),
      CopilotSkillOption("rc12", "Cattle Guard Crossing Guide", "Rural Country", "Safe angle, speed, and wheel-size limits for riding across rural ranch cattle guards."),
      CopilotSkillOption("rc13", "Harvest Combine Passing Plan", "Rural Country", "How to spot blind-hill farm machinery early, pull off safely, and pass wide combines with zero risk."),
      CopilotSkillOption("rc14", "Loose Farm Dog Defense", "Rural Country", "Calm, proven tactics to handle chasing farm dogs on country lanes without panicking or crashing."),
      CopilotSkillOption("rc15", "Horse & Rider Trail Etiquette", "Rural Country", "Step-by-step verbal greeting and quiet-motor passing routine when meeting horseback riders on rural trails."),
      CopilotSkillOption("rc16", "RV Park 30A Pedestal Splitter", "Rural Country", "Wire or pick a safe TT-30 RV park splitter so 3 rural PEVs can fast-charge together without tripping breakers."),
      CopilotSkillOption("rc17", "15A Barn Outlet Breaker Math", "Rural Country", "Calculate safe 80% continuous watts and extension cord wire gauge (12AWG vs 14AWG) for barn charging."),
      CopilotSkillOption("rc18", "Pure-Sine Generator Charging", "Rural Country", "Why modified-sine generators damage PEV chargers and how to size a quiet 2000W inverter generator."),
      CopilotSkillOption("rc19", "Pickup Truck Bed Tie-Downs", "Rural Country", "Exact ratchet strap anchor points, fork savers, and loading ramp angles to haul PEVs safely in a truck bed."),
      CopilotSkillOption("rc20", "Post-Creek Bearing Rescue", "Rural Country", "Immediate 10-minute drying and lube checklist after accidentally submerging a hub axle in a creek crossing."),
      CopilotSkillOption("rc21", "Red Clay Mud Fender Clearance", "Rural Country", "Prevent sticky rural clay from jamming tight fenders and overheating hub motors on wet dirt lanes."),
      CopilotSkillOption("rc22", "Dusk Deer Collision Avoidance", "Rural Country", "Speed envelope, treeline scanning, and beam pattern tactics during peak rural deer twilight hours."),
      CopilotSkillOption("rc23", "Rail-Trail Distance Strategy", "Rural Country", "Plan a 45-mile crushed cinder rail-trail expedition including mid-way small-town cafe charging etiquette."),
      CopilotSkillOption("rc24", "County Grader Windrow Safety", "Rural Country", "How to read and safely cross fresh center gravel ridges left behind by county road graders."),
      CopilotSkillOption("rc25", "Zero-Cell Offline Bailout Plan", "Rural Country", "Pre-ride check-in text, offline GPX pin drop, and emergency tow plan before entering zero-signal hollows."),
      CopilotSkillOption("rc26", "Winter Frost Heave Scanning", "Rural Country", "Spot shaded black ice, frost-heaved county asphalt cracks, and cold tire grip limits below 40°F."),
      CopilotSkillOption("rc27", "Barn Fire-Safe Charging Bench", "Rural Country", "Design a fire-resistant concrete or steel barn charging station away from hay, fuel cans, and timber."),
      CopilotSkillOption("rc28", "Amber Fog Beam Aiming", "Rural Country", "Mount and aim 3000K amber fog lights to cut dense valley creek fog without blinding oncoming trucks."),
      CopilotSkillOption("rc29", "Long Steep Descent Brake Plan", "Rural Country", "Alternate front/rear hydraulic pulses and moderate regen on 15% mountain grades to prevent rotor fade."),
      CopilotSkillOption("rc30", "Hunting Season Visibility", "Rural Country", "Hi-viz blaze orange/lime vest, acoustic bell, and public-land trail safety checklist during autumn hunting seasons."),
      CopilotSkillOption("rc31", "Spring Thaw Mud Season Tuning", "Rural Country", "Switch tire tread, lower PSI, and inspect suspension dust wipers for wet spring thaw backroads."),
      CopilotSkillOption("rc32", "Summer Heat-Soak Range Guard", "Rural Country", "Protect controller MOSFETs and lithium packs when riding 95°F+ rural pavement and sun-baked gravel."),
      CopilotSkillOption("rc33", "Small-Town General Store Pitch", "Rural Country", "Friendly, polite way to ask a rural store or cafe owner to use an outdoor 15A outlet while you buy lunch."),
      CopilotSkillOption("rc34", "Storm Lightning Sheltering", "Rural Country", "Safe rural storm sheltering protocol when caught far from home with an electric vehicle in open pasture."),
      CopilotSkillOption("rc35", "Dual-Battery Long-Haul Tour", "Rural Country", "Balance discharge currents and safe voltage-matching rules for 80+ mile backcountry dual-pack tours."),
      CopilotSkillOption("rc36", "Headwind Tuck & Speed Sweetspot", "Rural Country", "Calculate the exact cruise speed reduction that maximizes miles per kWh against a 20 MPH open-field headwind."),
      CopilotSkillOption("rc37", "Gravel Bolt Loctite Audit", "Rural Country", "Top 12 fasteners that vibrate loose fastest on rural washboard roads and exact Blue 243 torque values."),
      CopilotSkillOption("rc38", "Towing a Mini Cargo Trailer", "Rural Country", "Hitch balance, tongue weight, braking distance, and motor heat limits for hauling rural groceries or tools."),
      CopilotSkillOption("rc39", "Homestead Solar Shed Sizing", "Rural Country", "Design a 400W-1200W off-grid barn roof solar array + MPPT + LiFePO4 bank dedicated to daily PEV charging."),
      CopilotSkillOption("rc40", "Rural Night Convoy Formations", "Rural Country", "Staggered spacing, hand/light signals, and lead/sweep battery tracking for 4-to-10 rider night backroad rides."),
      CopilotSkillOption("rc41", "Gravel Shoulder Drop Recovery", "Rural Country", "Exact muscle-memory recovery if your tire drops off a 3-inch county blacktop edge onto loose gravel."),
      CopilotSkillOption("rc42", "Chipseal Tar & Rock Protection", "Rural Country", "Protect battery deck underside, hydraulic lines, and paint from flying gravel and summer chipseal tar."),
      CopilotSkillOption("rc43", "50% Off-Season Storage Guide", "Rural Country", "Exact storage resting voltage for 48V (49.4V), 60V (60.8V), and 72V (76.0V) packs over winter months."),
      CopilotSkillOption("rc44", "Multi-Rig Family Fleet Tracker", "Rural Country", "Build a simple seasonal rotation schedule for tire PSI, brake pads, and chain lube across 3+ homestead PEVs."),
      CopilotSkillOption("rc45", "Rural Sheriff & DOT Courtesy", "Rural Country", "Respectful roadside interaction tips, class speed compliance, and lighting checks for country road legality."),
      CopilotSkillOption("rc46", "Backroad Flat Plug & CO2 Drill", "Rural Country", "Under-5-minute bacon-strip tubeless plug and mini-compressor inflation routine beside a gravel lane."),
      CopilotSkillOption("rc47", "Battery Cold-Soak Recovery", "Rural Country", "Why a 25°F battery loses 25-35% range and how gentle low-amp warm-up miles restore voltage stability."),
      CopilotSkillOption("rc48", "Country Trail Scout Briefing", "Rural Country", "Generate a complete pre-ride rural route briefing combining elevation gain, wind, dusk time, and reserve Wh."),
    )
}

@Composable
fun CopilotInteractiveWizardsDialog(
  activeVehicleSummary: String,
  telemetryCtx: String,
  weatherCtx: String,
  onDismiss: () -> Unit,
  onLaunchPrompt: (String) -> Unit,
) {
  var wizardIdx by remember { mutableIntStateOf(0) }
  var inputA by remember { mutableStateOf("") }
  var inputB by remember { mutableStateOf("") }
  var inputC by remember { mutableStateOf("") }

  val wizardNames =
    listOf(
      "Trip Battery Planner",
      "Custom Build Sheet",
      "Pre-Ride Risk Audit",
      "Error Code Deep Dive",
      "Social Post Writer",
    )

  val chrome = LocalTacticalChrome.current
  val tfColors = tacticalOutlinedTextFieldColors()

  TacticalDialog(
    onDismissRequest = onDismiss,
    title = "CO-PILOT INTERACTIVE WIZARDS",
    subtitle = "Auto-wires active PEV, GPS & weather telemetry",
    icon = Icons.Default.AutoAwesome,
    badgeText = "5 LIVE",
    confirmButton = {
      TacticalButton(
        onClick = {
          val builtPrompt =
            when (wizardIdx) {
              0 -> "TRIP BATTERY PLANNER for $activeVehicleSummary. Distance: ${inputA.ifBlank { "25 miles" }}. Cruise/Terrain: ${inputB.ifBlank { "28 mph mixed rural asphalt" }}. Weight/Charger: ${inputC.ifBlank { "200lb, 5A charger" }}. Current live telemetry: $telemetryCtx. Weather: $weatherCtx. Calculate Wh/mi estimate, safe reserve %, voltage sag warning, and exact charge stop time."
              1 -> "CUSTOM PEV BUILD SHEET for ${inputA.ifBlank { activeVehicleSummary }}. Budget & Goal: ${inputB.ifBlank { "$1000 reliability & range" }}. Focus upgrades: ${inputC.ifBlank { "brakes, tires, suspension, VESC" }}. Provide prioritized BOM, compatibility checks, and safety notes."
              2 -> "PRE-RIDE RISK AUDIT for $activeVehicleSummary. Surface: ${inputA.ifBlank { "rural asphalt & gravel" }}. Conditions: ${inputB.ifBlank { "dusk, moderate traffic" }}. Rig notes: ${inputC.ifBlank { "standard check" }}. Live telemetry: $telemetryCtx. Weather: $weatherCtx. Give risk rating (1-10) and exact mitigation checklist."
              3 -> "ERROR CODE DEEP DIVE. Code: ${inputA.ifBlank { "E07 / Motor Hall" }}. Brand/System: ${inputB.ifBlank { activeVehicleSummary }}. Symptoms: ${inputC.ifBlank { "stutter under load" }}. Give root causes, pin-by-pin multimeter tests, and step-by-step repair."
              else -> "SOCIAL MEDIA CREATOR BRIEF for ${inputA.ifBlank { "Instagram & YouTube" }}. Story: ${inputB.ifBlank { "Rural PEV range run on $activeVehicleSummary" }}. Tone: ${inputC.ifBlank { "Hype & authentic" }}. Live telemetry: $telemetryCtx. Write hook, caption, and 8 PEV hashtags."
            }
          onDismiss()
          onLaunchPrompt(builtPrompt)
        },
      ) { Text("Run Live Wizard") }
    },
    dismissButton = { TacticalOutlinedButton(onClick = onDismiss) { Text("Cancel") } },
  ) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(wizardNames.indices.toList()) { idx ->
          TacticalFilterChip(
            selected = wizardIdx == idx,
            onClick = {
              wizardIdx = idx
              inputA = ""
              inputB = ""
              inputC = ""
            },
            label = wizardNames[idx],
          )
        }
      }
      when (wizardIdx) {
        0 -> {
          Text("Plan exact round-trip Wh usage, voltage sag reserve & charging stops.", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(value = inputA, onValueChange = { inputA = it }, label = { Text("Trip Distance (miles or km)") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputB, onValueChange = { inputB = it }, label = { Text("Target Cruise Speed & Terrain") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputC, onValueChange = { inputC = it }, label = { Text("Rider + Gear Weight & Charger Amps") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
        }
        1 -> {
          Text("Generate a prioritized parts & torque build sheet for your PEV.", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(value = inputA, onValueChange = { inputA = it }, label = { Text("PEV Model / Frame") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputB, onValueChange = { inputB = it }, label = { Text("Budget USD & Riding Goal") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputC, onValueChange = { inputC = it }, label = { Text("Desired Voltage / Tire / Brake Upgrades") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
        }
        2 -> {
          Text("Run a live risk audit factoring your weather, road surface & battery.", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(value = inputA, onValueChange = { inputA = it }, label = { Text("Route Surface (Asphalt / Gravel / Mud)") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputB, onValueChange = { inputB = it }, label = { Text("Time of Day & Traffic Level") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputC, onValueChange = { inputC = it }, label = { Text("Known Rig Quirks or Wear") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
        }
        3 -> {
          Text("Decode controller / BMS / display error codes with multimeter tests.", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(value = inputA, onValueChange = { inputA = it }, label = { Text("Error Code (e.g. E07, E10, Hall Fault)") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputB, onValueChange = { inputB = it }, label = { Text("Controller / Display / PEV Brand") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputC, onValueChange = { inputC = it }, label = { Text("Symptoms (when it triggers)") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
        }
        else -> {
          Text("Craft ready-to-share captions, video hooks & hashtags from your ride.", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(value = inputA, onValueChange = { inputA = it }, label = { Text("Platform (YouTube / IG / X / TikTok)") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputB, onValueChange = { inputB = it }, label = { Text("Ride Highlight or Build Story") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = inputC, onValueChange = { inputC = it }, label = { Text("Tone (Hype / Technical / Story)") }, singleLine = true, colors = tfColors, shape = chrome.buttonShape, modifier = Modifier.fillMaxWidth())
        }
      }
      Spacer(Modifier.height(2.dp))
      Text("Auto-attaches live vehicle ($activeVehicleSummary), telemetry & weather.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
  }
}
