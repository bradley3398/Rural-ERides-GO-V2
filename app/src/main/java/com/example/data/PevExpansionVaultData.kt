package com.example.data

data class PevErrorCodeEntry(
  val brand: String,
  val code: String,
  val severity: String, // CRITICAL, WARNING, INFO
  val title: String,
  val cause: String,
  val fix: String,
)

data class PevFieldGuide(
  val id: String,
  val category: String, // ELECTRICAL, TIRES, BRAKES, SUSPENSION, BATTERY, WATERPROOF, DRIVETRAIN
  val title: String,
  val difficulty: String,
  val timeEst: String,
  val steps: String,
)

data class PevRuleEntry(
  val region: String,
  val topic: String,
  val summary: String,
)

data class PevConvoySignal(
  val signal: String,
  val gesture: String,
  val meaning: String,
)

data class PevGlossaryItem(
  val term: String,
  val category: String,
  val definition: String,
)

data class BatteryCellSpec(
  val name: String,
  val nominalV: Double,
  val fullV: Double,
  val cutoffV: Double,
  val capacityAh: Double,
  val maxContA: Double,
  val weightGrams: Double,
)

/**
 * 200+ PEV Engineering Vault Dataset:
 * - 60 Controller & BMS Error Codes
 * - 42 Step-by-Step PEV Field Repair & Mod Guides
 * - 32 PEV Legal, Trail & Airline Battery Rulebook Cards
 * - 28 Group Ride Hand Signals, Convoy Protocols & Safety Audits
 * - 40 PEV Technical Glossary & Slang Definitions
 * - 8 Real 18650/21700/LFP Battery Cells for Pack Builder
 */
object PevExpansionVaultData {

  val BATTERY_CELLS: List<BatteryCellSpec> =
    listOf(
      BatteryCellSpec("Molicel P45B (21700)", 3.6, 4.2, 2.8, 4.5, 45.0, 70.0),
      BatteryCellSpec("Samsung 50S (21700)", 3.6, 4.2, 2.8, 5.0, 25.0, 72.0),
      BatteryCellSpec("Molicel P42A (21700)", 3.6, 4.2, 2.8, 4.2, 35.0, 68.0),
      BatteryCellSpec("LG M50LT (21700)", 3.6, 4.2, 2.8, 4.9, 14.4, 69.0),
      BatteryCellSpec("Samsung 40T (21700)", 3.6, 4.2, 2.8, 4.0, 35.0, 67.0),
      BatteryCellSpec("Panasonic NCR18650GA", 3.6, 4.2, 2.8, 3.5, 10.0, 48.0),
      BatteryCellSpec("Samsung 30Q (18650)", 3.6, 4.2, 2.8, 3.0, 15.0, 46.0),
      BatteryCellSpec("LiFePO4 32700 Cell", 3.2, 3.65, 2.5, 6.0, 18.0, 140.0),
    )

  val MASTER_ERROR_CODES: List<PevErrorCodeEntry> =
    listOf(
      // Dualtron / EY3 / EY4 / Kaabo / VSETT / NAMI
      PevErrorCodeEntry("Dualtron / EY3-EY4", "E00 / E02", "WARNING", "Brake Cutoff Active", "Brake lever magnet or e-brake reed sensor stuck closed.", "Check brake lever return spring and unplug 2-pin brake cutoff plugs one at a time."),
      PevErrorCodeEntry("Dualtron / Minimotors", "E06", "CRITICAL", "Battery Under-Voltage", "Pack dropped below P-setting cutoff or cell group collapsed.", "Measure main pack voltage with multimeter; verify P1 voltage setting matches pack."),
      PevErrorCodeEntry("Dualtron / VSETT / Kaabo", "E07", "CRITICAL", "Motor Hall / Phase Fault", "Disconnected 5-pin Hall sensor JST plug or damaged motor phase bullet.", "Inspect axle wire exit for pinch; test Hall pins (Red 5V, Black GND, Y/G/B toggle 0-5V)."),
      PevErrorCodeEntry("Dualtron / VSETT", "E08", "WARNING", "Throttle Hall Sensor Fault", "Throttle signal outside 0.8V–4.2V resting window.", "Check throttle magnet alignment inside EY3/trigger housing; test green signal wire."),
      PevErrorCodeEntry("Dualtron / Kaabo", "E09", "CRITICAL", "Controller MOSFET Failure", "Shorted MOSFET bank or controller over-current latch.", "Unplug motor phases; if wheel resists spinning when shorted, replace controller."),
      PevErrorCodeEntry("Dualtron / VSETT / NAMI", "E10", "CRITICAL", "Controller-Display RX Comm Drop", "Broken TX/RX wire in folding stem harness.", "Inspect stem fold pinch point; test continuity on green/blue UART lines."),
      PevErrorCodeEntry("Dualtron / VSETT", "E11", "WARNING", "Display TX Fault", "Display cannot transmit commands to controller.", "Reseat waterproof Julet stem connector and clean pins with DeoxIT."),
      PevErrorCodeEntry("NAMI Burn-E / Klima", "E01 / E02", "WARNING", "Front/Rear Controller Overtemp", "Controller exceeded 80°C on long climb or heavy regen.", "Let deck cool 15 min; inspect thermal pad contact between controller and aluminum frame."),
      PevErrorCodeEntry("NAMI", "E12 / E13", "CRITICAL", "BMS / Low Pack Protection", "Voltage sag triggered cutoff under high dual-motor load.", "Lower P-setting power ramp; balance charge pack at 100% for 2 hours."),
      PevErrorCodeEntry("Apollo Phantom / Pro", "E01 / E04", "WARNING", "Throttle / Brake Sensor Fault", "Throttle stuck on startup or brake hall magnet displaced.", "Power cycle without touching levers; recalibrate throttle deadzone in app."),
      PevErrorCodeEntry("Apollo", "E07 / E08", "CRITICAL", "Motor Phase / Controller Short", "Overheated controller or loose motor connector inside deck.", "Open deck, inspect MR60/MT60 motor connectors for browning or loose pins."),
      // Segway Ninebot
      PevErrorCodeEntry("Segway Ninebot", "Error 10", "CRITICAL", "Dashboard-Controller UART", "Broken stem communication cable or wet BLE dashboard.", "Unplug stem Julet cable, dry with isopropyl alcohol, inspect for green corrosion."),
      PevErrorCodeEntry("Segway Ninebot", "Error 14", "WARNING", "Throttle Hall Sensor Out of Range", "Thumb throttle reading <0.6V or >4.3V at boot.", "Flick throttle lever to free spring; replace thumb throttle if magnet detached."),
      PevErrorCodeEntry("Segway Ninebot", "Error 15", "WARNING", "Brake Hall Lever Fault", "Electronic brake lever sensor stuck or unplugged.", "Check handlebar brake lever sensor wire and magnet gap."),
      PevErrorCodeEntry("Segway Ninebot", "Error 18", "CRITICAL", "Motor Hall Sensor Error", "Rear hub motor Hall sensor array unreadable.", "Check 5-pin Hall connector on controller board; test 5V rail."),
      PevErrorCodeEntry("Segway Ninebot", "Error 21", "CRITICAL", "Battery BMS Communication Fail", "BMS UART lost or blown pack fuse.", "Inspect battery data connector; check cell group balance via BMS LED blink."),
      PevErrorCodeEntry("Segway Ninebot", "Error 24", "CRITICAL", "System Voltage Abnormal", "Wrong charger voltage or blown regulator on ESC.", "Verify pack voltage; never plug 48V charger into 36V Ninebot."),
      PevErrorCodeEntry("Segway Ninebot", "Error 39 / 40", "WARNING", "Battery / Controller Overheat", "NTC thermistor >60°C (pack) or >85°C (ESC).", "Stop riding immediately, shade scooter, verify NTC sensor wire isn't pinched."),
      // VESC / Flipsky / Spintend / Floatwheel
      PevErrorCodeEntry("VESC / Flipsky", "FAULT_CODE_OVER_VOLTAGE", "CRITICAL", "Regen Over-Voltage Spike", "Full-pack braking pushed bus voltage over max V cutoff.", "Do not brake hard at 100% SoC; lower battery regen amps by 20%."),
      PevErrorCodeEntry("VESC / Spintend", "FAULT_CODE_UNDER_VOLTAGE", "WARNING", "Battery Under-Voltage Cutoff", "Pack sagged below start/end cutoff threshold.", "Lower battery max current draw or verify healthy cell balance."),
      PevErrorCodeEntry("VESC / Flipsky", "FAULT_CODE_DRV", "CRITICAL", "Gate Driver Fault (DRV8301)", "Phase short, loose bullet, or damaged gate driver IC.", "Check phase-to-phase resistance (should be symmetric); inspect solder joints."),
      PevErrorCodeEntry("VESC", "FAULT_CODE_ABS_OVER_CURRENT", "CRITICAL", "Absolute Over-Current Spike", "Motor inductance saturated or phase current overshoot.", "Run FOC Motor Detection again; lower Motor Current Max by 10–15A."),
      PevErrorCodeEntry("VESC", "FAULT_CODE_OVER_TEMP_FET", "WARNING", "MOSFET Thermal Throttling", "Controller FETs hit 85°C thermal limit.", "Add silicone thermal pad to heatsink deck and reduce slow-speed high-amp climbs."),
      PevErrorCodeEntry("VESC", "FAULT_CODE_OVER_TEMP_MOTOR", "WARNING", "Motor Stator Overheat", "Stator thermistor exceeded configured limit (100–120°C).", "Install Ferrofluid (Statorade) and reduce continuous phase amps."),
      PevErrorCodeEntry("VESC / Floatwheel", "FAULT_CODE_HIGH_OFFSET", "CRITICAL", "Current Shunt Offset Error", "Damaged current sense resistor or moisture on PCB.", "Dry PCB thoroughly with 99% isopropyl; re-flash firmware if clean."),
      // Bafang / Bosch / Shimano / Yamaha eBikes
      PevErrorCodeEntry("Bafang BBS / Ultra", "Error 04 / 05", "WARNING", "Throttle Not Zero / Fault", "Throttle depressed at power-on or stuck magnet.", "Release throttle, cycle power; inspect 3-pin HIGO yellow plug."),
      PevErrorCodeEntry("Bafang", "Error 07 / 08", "CRITICAL", "Over-Voltage / Motor Hall", "Pack >58.8V on 48V unit or Hall sensor failure inside hub.", "Verify nominal battery match; check 9-pin motor plug arrow alignment."),
      PevErrorCodeEntry("Bafang", "Error 12", "CRITICAL", "Current Sensor Abnormal", "Controller internal current sensor failed.", "Unplug battery 10 minutes to discharge caps; replace controller if persistent."),
      PevErrorCodeEntry("Bafang", "Error 21", "WARNING", "Speed Sensor No Signal", "Spoke magnet shifted >10mm away from chainstay pickup.", "Align wheel spoke magnet within 3–5mm of speed sensor mark."),
      PevErrorCodeEntry("Bafang", "Error 25", "WARNING", "Torque Sensor Signal Fault", "Foot resting on pedal during boot or torque ring dirty.", "Keep feet off pedals while powering on; clean bottom bracket plug."),
      PevErrorCodeEntry("Bafang", "Error 30", "CRITICAL", "Display Communication Error", "1T4 main bus cable damaged or green 5-pin plug loose.", "Unplug and firmly seat green Julet connector matching arrows."),
      PevErrorCodeEntry("Bosch eBike", "Code 500", "CRITICAL", "Internal Drive Unit Fault", "Drive unit self-test failed.", "Restart system; if repeating after 2 cycles, update firmware via dealer tool."),
      PevErrorCodeEntry("Bosch eBike", "Code 503", "WARNING", "Speed Sensor Fault", "Rear spoke magnet misaligned or tuning dongle interference.", "Re-align magnet 5–12mm from sensor line on chainstay."),
      PevErrorCodeEntry("Bosch eBike", "Code 530", "CRITICAL", "Battery Pack Error", "Pack communication or dirty terminal pins.", "Clean battery cradle contacts with dry cloth; check lock latch tightness."),
      PevErrorCodeEntry("Shimano STEPS", "W011 / E010", "WARNING", "Speed Sensor / Drive Unit Error", "Wheel speed unreadable or torque sensor calibrated under load.", "Power on with zero pedal pressure; check rear rotor magnet."),
      PevErrorCodeEntry("Shimano STEPS", "E020", "CRITICAL", "Battery-Drive Comm Drop", "Di2 / power wire unplugged inside frame.", "Check battery mount terminals and seat-tube Di2 junction plug."),
      PevErrorCodeEntry("Yamaha PW-X", "Error 71", "WARNING", "Over-Current / Over-Temp", "Sustained steep climb in EXPW mode.", "Downshift rear cassette to increase cadence above 70 RPM."),
      // Lectric / Aventon / Rad Power / Super73
      PevErrorCodeEntry("Lectric / Rad / Aventon", "E06 / E21", "CRITICAL", "Low Voltage / Current Abnormal", "Tripped battery fuse or controller phase fault.", "Check external 30A/40A blade fuse on battery pack; reseat motor cable."),
      PevErrorCodeEntry("Aventon", "E22 / E24", "WARNING", "Throttle / Motor Hall Fault", "Damaged handlebar throttle or rear hub 9-pin plug slightly backed out.", "Firmly push rear hub waterproof connector past the molded line."),
      PevErrorCodeEntry("Aventon / Lectric", "E25", "WARNING", "Brake Cutoff Engaged at Boot", "Brake lever pressed or sensor switch unscrewed.", "Check brake lever cutoff cables near stem."),
      PevErrorCodeEntry("Rad Power", "Error 30", "CRITICAL", "Communication Error", "Pinched wiring harness or damaged display/controller.", "Unplug taillight/headlight/PAS one by one to isolate 5V short."),
      PevErrorCodeEntry("Super73", "E007 / E008", "CRITICAL", "Motor / Throttle Fault", "Water ingress in hub plug or throttle cable.", "Disconnect, blow out moisture, apply dielectric grease to seal skirt."),
      // Sur-Ron / Talaria / E-Ride Pro
      PevErrorCodeEntry("Sur-Ron Light Bee", "Error 105 / 206", "WARNING", "Throttle / Brake / Tilt Cutoff", "Kickstand sensor, tilt sensor, or brake sensor triggered.", "Inspect kickstand magnet & brake sensor plugs; bypass faulty trail sensor safely."),
      PevErrorCodeEntry("Sur-Ron", "Error 207 / 210", "CRITICAL", "Controller Overheat / Over-Current", "Stock X-controller hit thermal limit on mud/hill climb.", "Let controller cool; verify rear wheel spins freely without brake drag."),
      PevErrorCodeEntry("Talaria Sting MX4/MX5", "Error 00300 / 00400", "CRITICAL", "Battery / Encoder Fault", "Motor Hall/Encoder misread or BMS tripped.", "Check motor encoder plug behind bash guard; reboot breaker switch."),
      PevErrorCodeEntry("E-Ride Pro SS", "E004 / E009", "WARNING", "Throttle / Comm Error", "Handlebar harness plug loose after bar swap.", "Verify throttle 3-pin waterproof plug and breaker switch contact."),
      // EUC: LeaperKim, InMotion, Begode, KingSong
      PevErrorCodeEntry("LeaperKim / Veteran", "High PWM Beep (80%+)", "CRITICAL", "Approaching Cutout Limit", "Motor near max back-EMF speed for current battery voltage.", "Ease lean back smoothly immediately; do not brake abruptly."),
      PevErrorCodeEntry("LeaperKim Lynx / Sherman", "Hall / Smart BMS Warn", "CRITICAL", "Cell Imbalance >0.15V", "One cell group drifting low under load.", "Leave on OEM charger 2 hours past green light to complete top-balancing."),
      PevErrorCodeEntry("InMotion V14 / V13", "Overload / Tiltback", "CRITICAL", "Peak Torque / Temp Limit", "Board >80°C or battery sag under steep climb.", "Check cooling fan intake for mud; reduce acceleration torque."),
      PevErrorCodeEntry("Begode / Extreme Bull", "2-Beep / 3-Beep / 5-Beep", "CRITICAL", "80% PWM / Low Batt / Over-Temp", "Audible piezo alarm indicating imminent safety margin breach.", "Slow down immediately; check DarknessBot/EUC World PWM & MOSFET temp."),
      PevErrorCodeEntry("KingSong S22 / 16X", "Tiltback + Voice Alert", "WARNING", "Speed / Power / Temp Threshold", "Controller enforcing safety envelope.", "Respect tiltback; verify slider bearings aren't binding suspension."),
      // Onewheel
      PevErrorCodeEntry("Onewheel GT / Pint X", "Error 15 / 16", "CRITICAL", "Footpad Engaged / Bus Corruption", "Toe/heel pressed at boot (E15) or BMS-controller comm drop (E16).", "Power on with feet off pad; check footpad grip tape bubble (E15)."),
      PevErrorCodeEntry("Onewheel", "Error 23", "CRITICAL", "Incompatible Hardware / Regen", "Downhill regen overcharged 100% battery.", "Ride uphill first or drain to 90% before starting steep descents."),
      // Kelly / Fardriver / Sabvoton DIY Controllers
      PevErrorCodeEntry("Fardriver ND", "Blink 2 / 3 / 5", "CRITICAL", "Hall / Phase / Over-Voltage", "Motor auto-learn not completed or Hall sequence mismatch.", "Run Fardriver app Auto-Learn with wheel off ground; verify Hall 5V."),
      PevErrorCodeEntry("Kelly KLS", "Flash 1,2 / 2,4", "CRITICAL", "Over-Voltage / Hall Sensor", "Regen spike or open Hall wire.", "Check throttle 0–5V range and 5V Hall supply."),
      PevErrorCodeEntry("Sabvoton SVMC", "Hall Test Fail", "WARNING", "Angle Calibration Failed", "Wheel blocked or phase/Hall order swapped.", "Lift wheel cleanly off ground, ensure zero brake drag, re-run Hall test."),
      // General BMS / Charger
      PevErrorCodeEntry("Smart BMS (JK / JBD)", "Cell OVP / UVP Lock", "CRITICAL", "Single Cell >4.25V or <2.75V", "Out-of-balance parallel group triggered FET cutoff.", "Inspect cell deltas in BMS readout; do not force-charge a <2.0V dead cell."),
      PevErrorCodeEntry("Smart BMS", "Low-Temp Charge Cut (<0°C)", "WARNING", "Freezing Lithium Protection", "Charging below 32°F (0°C) causes permanent lithium plating.", "Bring pack indoors to >50°F (10°C) for 2 hours before plugging in."),
      PevErrorCodeEntry("Smart BMS", "Short-Circuit Latch", "CRITICAL", "Output Spark / Short Detected", "Controller capacitor surge or pinched wire tripped SCP.", "Use XT90-S/QS8-S anti-spark connector; reset BMS by briefly connecting charger."),
      PevErrorCodeEntry("Universal Charger", "Red-Green Rapid Blink", "WARNING", "Reverse Polarity or Blown Fuse", "Charge port pin shorted or charger output fuse blown.", "Unplug immediately; inspect GX16/XLR/LP16 charge port pins for bent metal."),
    )

  val FIELD_REPAIR_GUIDES: List<PevFieldGuide> =
    listOf(
      PevFieldGuide("g01", "TIRES", "Tubeless Scooter Tire Bead Seating Without Air Compressor", "Moderate", "25 min", "1. Remove valve core with core tool.\n2. Lubricate both tire beads with soapy water.\n3. Wrap a ratchet strap tightly around the center tread circumference.\n4. Pump rapidly with a high-volume floor pump until both beads POP onto the rim.\n5. Release strap, reinstall core, inject 2–3 oz Kevlar sealant, inflate to target PSI."),
      PevFieldGuide("g02", "TIRES", "High-Speed PEV Sealant Dosing (Armor-Dilloz / FlatOut)", "Easy", "10 min", "1. Rotate valve stem to 4 or 8 o'clock.\n2. Remove valve core and deflate completely.\n3. Dose: 10–11\" Scooter = 2.5–3 oz | 16–20\" EUC = 4 oz | 20x4 Fat eBike = 6–8 oz | Onewheel = 4 oz.\n4. Reinstall core, spin wheel 20 times, ride 2 miles to coat inner carcass."),
      PevFieldGuide("g03", "TIRES", "Split-Rim Scooter Tube Swap (Zero Pinch Flats)", "Easy", "20 min", "1. Deflate tube completely BEFORE removing rim bolts (safety critical!).\n2. Remove 6 split-rim bolts evenly.\n3. Coat new tube lightly in baby powder.\n4. Pre-inflate tube slightly (2 PSI) so it holds round shape.\n5. Check zero tube rubber is pinched between halves; torque bolts in star pattern to 8 Nm with Blue 243 Loctite."),
      PevFieldGuide("g04", "TIRES", "Roadside Tubeless Bacon Strip Plug Fix", "Easy", "5 min", "1. Locate puncture and pull nail/glass out.\n2. Ream hole cleanly with rasp tool.\n3. Thread sticky plug half-way through insertion fork.\n4. Push firmly into hole leaving 6mm tail outside, yank fork straight out.\n5. Inflate with CO2/mini-pump and trim excess tail to 3mm."),
      PevFieldGuide("g05", "TIRES", "Static Wheel Balancing for 45+ MPH Stability", "Moderate", "20 min", "1. Lift wheel off ground with brake caliper removed.\n2. Let wheel settle naturally — heavy spot drops to bottom.\n3. Stick 5g–10g adhesive motorcycle wheel weights directly opposite (at 12 o'clock).\n4. Repeat until wheel stops randomly at any position."),
      PevFieldGuide("g06", "BRAKES", "Hydraulic Brake Lever Bleed (Mineral Oil: Shimano/Nutt/Logan/Zoom)", "Moderate", "25 min", "1. Level handlebar brake reservoir horizontally.\n2. Remove wheel & pads; insert plastic bleed block between pistons.\n3. Thread bleed cup onto lever port with 15ml mineral oil.\n4. Push oil slowly from bottom caliper syringe up to lever cup until bubbles stop.\n5. Flick lever 10x to release trapped micro-bubbles; close bleed nipple and wipe rotor with 99% isopropyl."),
      PevFieldGuide("g07", "BRAKES", "Silent Brake Caliper Centering & Rub Elimination", "Easy", "10 min", "1. Loosen the two 5mm caliper mount bolts 1 full turn.\n2. Squeeze brake lever firmly and hold.\n3. Tighten caliper bolts alternately in small increments to 8–9 Nm.\n4. Release lever, spin wheel, and eyeball daylight gap on both sides of rotor."),
      PevFieldGuide("g08", "BRAKES", "Bedding-In New Brake Pads & Rotors Properly", "Easy", "10 min", "1. Find flat quiet street.\n2. Perform 15 controlled decelerations from 15 MPH down to 3 MPH (never come to a dead stop while clamped!).\n3. Perform 5 harder stops from 25 MPH to 5 MPH to transfer even pad material onto rotor.\n4. Allow 5 min cool-down before hard riding."),
      PevFieldGuide("g09", "BRAKES", "Fixing Warped Disc Rotor (Trueing On-Bike)", "Easy", "15 min", "1. Spin wheel slowly and listen/watch where rotor rubs pad.\n2. Mark high spot with dry-erase marker.\n3. Use clean rotor truing fork (never bare oily fingers) to gently flex spot opposite the rub.\n4. Verify runout <0.2mm; clean rotor with 99% IPA."),
      PevFieldGuide("g10", "BRAKES", "Contaminated Screaming Brake Pad Rescue", "Easy", "15 min", "1. Remove pads and inspect for glossy oil glaze.\n2. Sand pad surface lightly on flat 220-grit sandpaper until matte.\n3. Soak pads briefly in 99% isopropyl alcohol and wipe rotor thoroughly.\n4. Re-bed pads; replace pads if hydraulic oil soaked deep into resin."),
      PevFieldGuide("g11", "BATTERY", "Lithium Pack Top-Balancing Routine (Cell Drift Cure)", "Easy", "2 hrs", "1. Ride battery down to ~30%.\n2. Plug in genuine OEM charger until LED turns green (100%).\n3. Leave plugged in an EXTRA 2–3 hours (passive BMS bleeds high cells down to match lowest).\n4. Do this once every 10–15 cycles; never leave unattended overnight."),
      PevFieldGuide("g12", "BATTERY", "Long-Term Winter Storage Protocol (3.80V/Cell)", "Easy", "15 min", "1. Discharge or charge pack to 50%–60% SoC (~3.80V per cell: 48V=49.4V, 52V=53.2V, 60V=60.8V, 72V=76.0V).\n2. Store indoors at 55–72°F (13–22°C) away from exits.\n3. Check voltage every 45 days; top back to 55% if below 40%."),
      PevFieldGuide("g13", "BATTERY", "Anti-Spark XT90-S / QS8-S Connector Upgrade", "Advanced", "40 min", "1. Verify pack power switch OFF and wear safety glasses.\n2. Cut ONE battery wire at a time (never cut + and - together!).\n3. Tin 8–10AWG silicone wire with 80W+ iron and rosin flux.\n4. Slide adhesive heat-shrink onto wire first, solder cleanly into cup, shrink tight.\n5. Confirm polarity twice with multimeter before mating!"),
      PevFieldGuide("g14", "BATTERY", "Cold Weather Pack Insulation & Charging Safety", "Easy", "15 min", "1. NEVER charge Li-Ion below 32°F (0°C) — permanent dendrite fire hazard.\n2. Wrap removable eBike/eMoto packs in 5mm neoprene thermal sleeve for winter rides.\n3. Warm battery indoors 2 hours before plugging in after cold rides."),
      PevFieldGuide("g15", "BATTERY", "Multimeter Battery Sag & Dead Group Diagnosis", "Moderate", "15 min", "1. Measure resting open-circuit voltage at discharge port.\n2. Compare against healthy 100% full voltage (13S=54.6V, 14S=58.8V, 16S=67.2V, 20S=84.0V).\n3. If pack stops charging exactly 4.2V short of full, one series group is dead/unbalanced — stop using pack and inspect BMS."),
      PevFieldGuide("g16", "ELECTRICAL", "5-Pin Motor Hall Sensor Live Multimeter Test", "Moderate", "20 min", "1. Power PEV ON with drive wheel off ground.\n2. Set multimeter to 20V DC; Black probe to Hall GND (Black).\n3. Red probe to Red wire -> must read ~4.5V–5.2V.\n4. Probe Yellow, Green, Blue wires individually while slowly hand-turning wheel.\n5. Healthy Hall sensors cleanly toggle 0.1V <-> 4.8V as magnets pass."),
      PevFieldGuide("g17", "ELECTRICAL", "Controller Blown MOSFET Resistance Check", "Moderate", "15 min", "1. Disconnect battery COMPLETELY and press power button to drain capacitors.\n2. Set multimeter to Diode/Resistance mode.\n3. Measure Red(+) to Yellow, Green, Blue motor phases; then Black(-) to Y, G, B.\n4. All three phases must read nearly identical (~8–15 kOhm or 0.45V diode). Near 0 Ohm = shorted MOSFET!"),
      PevFieldGuide("g18", "ELECTRICAL", "Throttle 0.8V–4.2V Signal Verification", "Easy", "10 min", "1. Back-probe 3-pin throttle connector with system ON.\n2. Verify Red-to-Black = ~5.0V.\n3. Measure Green/White signal wire to Black: resting should be ~0.8V, full throttle ~4.1–4.3V smoothly without dropouts."),
      PevFieldGuide("g19", "ELECTRICAL", "Preventing Stem Harness Fatigue & Pinch Breaks", "Easy", "15 min", "1. Inspect cable entry grommets where harness exits folding stem.\n2. Wrap abrasion zones in spiral cable armor or self-fusing silicone tape.\n3. Leave 2cm slack loop at full lock left/right turns so wires never pull tight."),
      PevFieldGuide("g20", "ELECTRICAL", "Motor Stator Ferrofluid (Statorade) Cooling Mod", "Advanced", "45 min", "1. Open hub motor side cover cleanly without nicking Hall wires.\n2. Inject 5–10ml genuine Ferrofluid onto magnet ring (not bearings!).\n3. Re-seal cover with Permatex Black RTV silicone.\n4. Drops peak hub motor temps by 20–30°C on steep hills!"),
      PevFieldGuide("g21", "WATERPROOF", "Deck & Cable Grommet IPX6 Silicone Sealing", "Moderate", "45 min", "1. Open deck lid and inspect factory foam gasket.\n2. Clean edges with isopropyl alcohol.\n3. Apply neat bead of Neutral-Cure Silicone (NOT acetic acid bath caulk which corrodes copper!).\n4. Seal cable entry holes around grommets; cure 24 hours."),
      PevFieldGuide("g22", "WATERPROOF", "Dielectric Grease Connector Protection Protocol", "Easy", "15 min", "1. Unplug exposed wheel/brake/light Julet plugs.\n2. Apply tiny dab of silicone dielectric grease to outer rubber O-ring skirt (prevents moisture wicking).\n3. Seat plugs firmly until color arrows touch."),
      PevFieldGuide("g23", "WATERPROOF", "Post-Rain Drying & Bearing Rust Prevention", "Easy", "10 min", "1. Never blast PEV axles or display with a pressure washer.\n2. Towel dry deck, stem, and brake rotors immediately.\n3. Store upright so drainage weep holes empty; apply marine grease film outside wheel bearing seals."),
      PevFieldGuide("g24", "SUSPENSION", "Eliminating High-Speed Scooter Stem Wobble", "Moderate", "25 min", "1. Check headset top-cap preload bolt — tighten until zero rocking when holding front brake.\n2. Check folding latch pin play and shim with TPU washer if needed.\n3. Balance front tire and verify front/rear PSI match.\n4. Install 24-click CNC hydraulic steering damper set to 8–12 clicks."),
      PevFieldGuide("g25", "SUSPENSION", "Setting Coil Shock Sag & Rebound Damping", "Easy", "15 min", "1. Measure shock eye-to-eye unloaded.\n2. Stand on PEV in normal riding gear; measure compressed length.\n3. Adjust preload collar for 20–25% sag (street) or 25–30% (off-road).\n4. Set rebound dial so suspension returns quickly without pogo-bouncing."),
      PevFieldGuide("g26", "SUSPENSION", "EUC Suspension Stanchion Cleaning & Lubrication", "Easy", "10 min", "1. Wipe mud/grit off stanchion tubes after every dirt ride.\n2. Apply 2 drops of PTFE / Slickoleum fork lube to wiper seals.\n3. Compress suspension 5 times and wipe away excess dirt ring."),
      PevFieldGuide("g27", "SUSPENSION", "Hydraulic Fork Oil Change & Seal Refresh", "Advanced", "60 min", "1. Release air pressure / remove top cap & spring.\n2. Drain old dark fork oil completely.\n3. Refill with exact factory weight oil (usually 5WT–7.5WT) to spec ml.\n4. Pump damper rod 10x to bleed air before closing."),
      PevFieldGuide("g28", "DRIVETRAIN", "eMoto 420 Chain Tension & Alignment Tuning", "Easy", "15 min", "1. Measure chain slack at midpoint between sprockets (target 15–25mm with rider weight).\n2. Match rear axle adjuster hash marks identically on left & right swingarm.\n3. Clean with kerosene/chain cleaner and lube with dry/wax off-road chain lube."),
      PevFieldGuide("g29", "DRIVETRAIN", "eSkate HTD 5M Belt Tensioning (Zero Slip)", "Easy", "10 min", "1. Loosen motor mount bolts slightly.\n2. Tension belt so pressing center deflects ~4–5mm (too tight kills motor bearings; too loose skips under hard braking).\n3. Torque motor bolts with Blue Loctite 243."),
      PevFieldGuide("g30", "DRIVETRAIN", "Bafang Geared Hub Nylon Planetary Gear Grease", "Advanced", "50 min", "1. Open hub motor sideplate using security Torx/Allen.\n2. Inspect 36T nylon planetary gears for chipped teeth.\n3. Clean old grease; apply Mobilgrease 28 or plastic-safe synthetic PTFE grease (never petroleum grease that swells nylon!)."),
      PevFieldGuide("g31", "DRIVETRAIN", "Spoke Tensioning & Wheel Dish Truing (eBike/eMoto)", "Moderate", "30 min", "1. Squeeze adjacent pairs of spokes around wheel — all should feel firm and uniform.\n2. Pluck spokes like guitar strings: dull thud = loose spoke.\n3. Tighten loose nipples 1/4 turn at a time; check rim lateral runout <1mm."),
      PevFieldGuide("g32", "DRIVETRAIN", "Pressing Sealed SKF 2RSH Wheel Bearings", "Advanced", "45 min", "1. Freeze new SKF bearings in ziplock bag for 1 hour (shrinks outer race slightly).\n2. Tap out old bearings evenly using brass drift.\n3. Press new bearings strictly by the OUTER race using socket matching outer diameter — never hammer inner race!"),
      PevFieldGuide("g33", "ELECTRICAL", "VESC FOC Motor & Throttle Wizard Calibration", "Moderate", "20 min", "1. Connect via VESC Tool with drive wheel lifted.\n2. Set exact battery series count (e.g. 20S) & cell cutoff.\n3. Run FOC Motor Wizard (auto-detects R, L, Flux Linkage, Hall table).\n4. Calibrate ADC/PPM throttle min/max with 15% deadband & smooth positive expo."),
      PevFieldGuide("g34", "ELECTRICAL", "Dual-Motor Regen Braking Safe Tuning", "Easy", "10 min", "1. Keep front motor regen lower than rear (e.g., 30% front / 60% rear) to prevent front-wheel lockup on gravel.\n2. Avoid heavy regen above 95% battery charge to prevent BMS OVP cutoff."),
      PevFieldGuide("g35", "ELECTRICAL", "Headlight StVZO Cutoff Aiming for Rural Night Riding", "Easy", "5 min", "1. Park 15 ft (5m) from a flat wall on level ground.\n2. Aim top horizontal beam cutoff 2 inches BELOW mounting height.\n3. Illuminates potholes 150 ft ahead without blinding oncoming cars or cyclists."),
      PevFieldGuide("g36", "TIRES", "Onewheel Tire Change Without Rim Scratches", "Moderate", "45 min", "1. Unplug motor & footpad connectors carefully.\n2. Break bead using 2x4 wood blocks near hub edge (never pry on aluminum lip!).\n3. Walk tire off/on through the drop-center rim valley with nylon levers.\n4. Seat bead safely inside a 5-gallon bucket up to 50 PSI max, then drop to 15–19 PSI."),
      PevFieldGuide("g37", "BATTERY", "Reading Cell Delta & Spotting Weak Parallel Groups", "Easy", "10 min", "1. Healthy pack delta at 100%: <0.020V (20mV).\n2. Warning delta: 0.050V–0.100V -> run 3-hour top balance.\n3. Critical delta: >0.150V under load -> inspect nickel strip welds & weak cell."),
      PevFieldGuide("g38", "BRAKES", "Adding HIGO / Magnetic E-Brake Cutoff Sensors", "Moderate", "20 min", "1. Mount small neodymium magnet on moving brake lever.\n2. Position reed sensor 2mm away on lever perch.\n3. Test motor immediately cuts throttle the instant lever pulls 3mm."),
      PevFieldGuide("g39", "SUSPENSION", "Blue 243 vs Red 271 Threadlocker Rules for PEVs", "Easy", "5 min", "1. Use BLUE 243 on brake rotors, caliper bolts, stem clamps, shock mounts, axle nuts (removable with hand tools).\n2. NEVER use Red 271 on aluminum threads or rotor bolts (requires 500°F torch!).\n3. Degrease threads with IPA before applying 1 drop."),
      PevFieldGuide("g40", "ELECTRICAL", "Installing GPS AirTag / Hidden Tracker Inside Deck", "Easy", "15 min", "1. Place tracker inside non-metallic plastic battery/headlight fairing (carbon fiber & aluminum deck walls block RF signals!).\n2. Foam-pad tracker so vibration doesn't dislodge coin battery.\n3. Record IMEI/serial in your PEV Passport."),
      PevFieldGuide("g41", "WATERPROOF", "EUC Motherboard & Mosfet Thermal Paste Refresh", "Advanced", "45 min", "1. Remove shell and unplug battery packs (discharge caps by pressing power button).\n2. Clean dry chalky factory paste from heatsink & MOSFET insulators.\n3. Apply thin non-conductive thermal paste (Arctic MX-6) and torque clamp bar evenly."),
      PevFieldGuide("g42", "TIRES", "Reading Tire Sidewall Speed, Load & Direction Arrows", "Easy", "5 min", "1. Locate ROTATION arrow on sidewall — tread V-grooves must point forward at top of tire to channel water outward.\n2. Never exceed molded MAX PSI cold rating.\n3. Check tread wear bars monthly."),
    )

  val LEGAL_AND_TRAIL_RULES: List<PevRuleEntry> =
    listOf(
      PevRuleEntry("US Federal / 42+ States", "Class 1 eBike", "Pedal-assist only (no throttle), motor assistance cuts off at 20 MPH, max 750W nominal. Allowed on most paved bike paths & greenways."),
      PevRuleEntry("US Federal / 42+ States", "Class 2 eBike", "Throttle-capable + pedal-assist, motor assistance cuts off at 20 MPH, max 750W. Widely permitted on bike lanes and multi-use paths."),
      PevRuleEntry("US Federal / 42+ States", "Class 3 eBike", "Pedal-assist up to 28 MPH (speedometer required), max 750W. Street & bike-lane legal; restricted on some shared pedestrian trails; helmet often mandatory."),
      PevRuleEntry("US Multi-State", "eScooter Street Laws", "Most states cap street eScooters at 15–20 MPH on bike lanes/roads with 25–35 MPH speed limits; sidewalks generally prohibited in downtown cores."),
      PevRuleEntry("US Multi-State", "EUC & Onewheel (EPAMD)", "Often classified under Electric Personal Assistive Mobility Devices (EPAMD) or electric skateboards; stay below 15–20 MPH near pedestrians and yield always."),
      PevRuleEntry("US Off-Road / OHV", "eMoto (Sur-Ron / Talaria / Stark)", "Vehicles >750W or >28 MPH without operable pedals are legally off-highway motorcycles (OHV). Ride OHV parks/private land or register with moped/moto plate & DOT kit."),
      PevRuleEntry("US Federal Trails (BLM / USFS)", "Natural Surface eMTB Rules", "National Forest & BLM non-motorized singletrack classifies eBikes as motorized unless explicitly designated Class 1 eMTB open — always check local trailhead sign!"),
      PevRuleEntry("US National Parks (NPS)", "National Park eBike Rule", "Class 1, 2, and 3 eBikes allowed where traditional bicycles are permitted; throttle-only riding without pedaling is prohibited on non-motorized park paths."),
      PevRuleEntry("Airline / FAA / TSA", "100Wh Carry-On Rule", "Li-Ion batteries <=100Wh allowed in cabin carry-on (never checked luggage!). Protects terminals with tape or case."),
      PevRuleEntry("Airline / FAA / TSA", "101Wh–160Wh Airline Approval", "Up to TWO spare Li-Ion batteries between 101Wh and 160Wh permitted in carry-on ONLY with prior airline approval. Packs >160Wh are strictly banned on passenger aircraft!"),
      PevRuleEntry("Shipping / Ground", "Hazmat Class 9 Ground Shipping", "PEV batteries >300Wh ship via FedEx/UPS Ground Dangerous Goods (UN3480/UN3481) with Class 9 lithium hazard label & <30% SoC."),
      PevRuleEntry("EU / European Union", "EN 15194 EPAC Standard", "250W continuous rated motor, pedal-assist cutoff at 25 km/h (15.5 MPH), walk-assist up to 6 km/h. Higher power requires S-Pedelec L1e-B plate & insurance."),
      PevRuleEntry("UK", "EAPC & Private eScooter Law", "eBikes capped at 250W & 15.5 MPH with pedals. Privately owned eScooters & EUCs currently restricted to private land unless government trial rental."),
      PevRuleEntry("Canada", "Federal / Provincial eBike Rules", "500W max motor, 32 km/h (20 MPH) top assisted speed, working pedals required; helmets mandatory in most provinces."),
      PevRuleEntry("Australia", "250W EN15194 & PMD Rules", "QLD/ACT/WA permit PMD scooters & EUCs at 12 km/h footpath / 25 km/h bikeway with mandatory helmet & bell."),
      PevRuleEntry("Safety Standard", "UL 2849 (eBike Electrical)", "Certifies entire eBike drivetrain, battery, and charger together against fire/thermal runaway. Required in NYC & many apartment leases."),
      PevRuleEntry("Safety Standard", "UL 2272 (Scooter / EUC / Hoverboard)", "Certifies personal e-mobility electrical & battery safety system. Essential proof for landlords and campus transit."),
      PevRuleEntry("Safety Standard", "UL 2271 (Battery Pack Only)", "Tests individual lithium pack cells, BMS fault protection, vibration, crush, and thermal propagation."),
      PevRuleEntry("Helmet Certification", "CPSC / NTA 8776 (Speed Pedelec)", "Standard CPSC bike helmets rated up to ~15 MPH; NTA 8776 eBike helmets add temple/occipital coverage rated up to 28 MPH."),
      PevRuleEntry("Helmet Certification", "ASTM F1952 Downhill Full-Face", "Lightweight chin-bar protection ideal for 20–35 MPH scooters, EUCs, Onewheels & eSkates."),
      PevRuleEntry("Helmet Certification", "ECE 22.06 / DOT Motorcycle", "Mandatory safety tier for 40+ MPH hyper-scooters, fast EUCs, and eMotos. Look for ECE 22.06 rotational impact testing."),
      PevRuleEntry("Night Lighting Law", "Front White + Rear Red Mandate", "All 50 states require a steady white headlight visible 500 ft ahead and red taillight/reflector visible 300–600 ft behind after dusk."),
      PevRuleEntry("Rural Road Protocol", "Riding With Traffic + Shoulder Visibility", "Always ride WITH traffic on the right edge (never salmoning against cars). Wear high-vis retroreflective vest on 45+ MPH county roads."),
      PevRuleEntry("Trail Yield Triangle", "Right-of-Way Hierarchy", "PEVs yield to ALL pedestrians AND horses! Slow to walking speed, announce calmly ('Rider passing left'), and stop completely for spooked horses."),
      PevRuleEntry("School Zones", "Strict 15–20 MPH Enforcement", "Observe active flashing school zone limits regardless of bike lane presence; expect unpredictable crossings."),
      PevRuleEntry("Audio / Headphones", "Single-Ear or Bone-Conduction Only", "Many states prohibit wearing both earbuds while riding on public roads so you can hear sirens, horns, and approaching trucks."),
      PevRuleEntry("Insurance & Theft", "PEV Specialty Insurance", "Standard auto policies exclude PEVs; homeowners policies often cap motorized vehicles at $1,000. Use dedicated eBike/PEV coverage (Velosurance, Oyster, Sundaes)."),
      PevRuleEntry("Apartment Charging", "Fire Code Safe Storage", "Never block bedroom/apartment exit doors with charging PEVs. Charge on non-flammable tile/concrete or inside a Bat-Safe vault with smoke detector overhead."),
      PevRuleEntry("Rail-Trail Etiquette", "15 MPH Pass Cap & Bell Tap", "Ring bell or call 'On your left!' 50 ft before overtaking walkers/dogs; pass at <=10 MPH differential."),
      PevRuleEntry("Agricultural / Rural", "Farm Machinery & Loose Dogs", "Give wide tractors full lane clearance (blind spots are huge). For chasing farm dogs, slow slightly then accelerate smoothly away without swerving into traffic."),
      PevRuleEntry("Weather & Lightning", "30/30 Storm Rule", "If thunder follows lightning in <30 seconds, seek enclosed shelter immediately — PEVs offer zero Faraday protection."),
      PevRuleEntry("DUI / BUI Laws", "Impaired PEV Operation", "In most jurisdictions, operating a motorized scooter, eBike, or EUC while intoxicated carries full DUI penalties."),
    )

  val CONVOY_SIGNALS_AND_PROTOCOLS: List<PevConvoySignal> =
    listOf(
      PevConvoySignal("Left Turn", "Left arm extended straight out horizontally", "Turning left or changing left lane."),
      PevConvoySignal("Right Turn", "Right arm straight out OR left arm bent 90° up", "Turning right ahead."),
      PevConvoySignal("Slowing / Stopping", "Left arm extended downward, palm facing rear", "Braking or coming to full stop — leave extra space!"),
      PevConvoySignal("Road Hazard (Left/Right)", "Point down & diagonally at pothole/glass/branch", "Obstacle on indicated side — echo signal back through convoy."),
      PevConvoySignal("Loose Gravel / Sand / Wet", "Hand low, fingers wiggling downward", "Low traction ahead — stay upright, no hard lean or front brake!"),
      PevConvoySignal("Railroad Tracks / Bump", "Pat air up-and-down with flat palm", "Unweight knees and cross tracks at 90° angle."),
      PevConvoySignal("Single File", "Single index finger raised vertically above helmet", "Narrow road or traffic — collapse staggered pack into single file."),
      PevConvoySignal("Double / Staggered Pack", "Two fingers raised (V-sign)", "Wide clear road — resume staggered formation with 2-second gaps."),
      PevConvoySignal("Car Back!", "Shout 'CAR BACK!' + wave forward from behind", "Vehicle overtaking convoy from rear — hold straight line on right."),
      PevConvoySignal("Car Up!", "Shout 'CAR UP!' + point ahead", "Oncoming vehicle on narrow rural lane — tuck right immediately."),
      PevConvoySignal("Runner / Walker / Dog", "Shout 'WALKER UP!' / 'DOG!' + slow hand", "Drop speed below 12 MPH and give 5 ft wide berth."),
      PevConvoySignal("Pull Over / Pit Stop", "Point to right shoulder repeatedly", "Convoy pulling off for charge, tire check, or regroup."),
      PevConvoySignal("Low Battery / Range Alert", "Tap helmet or mimic plugging in cable", "Rider below 25% pack — route to nearest AC outlet or slow cruise."),
      PevConvoySignal("Mechanical / Flat Tire", "Raise fist + pull cleanly off roadway", "Tail-Gunner stops immediately with stranded rider while Lead regroups."),
      PevConvoySignal("Lead Pilot Role", "Front of convoy", "Sets conservative pace, scans road 200 ft ahead, calls turns early."),
      PevConvoySignal("Tail-Gunner (Sweep) Role", "Rear of convoy", "No rider left behind! Carries spare tubes, plug kit, tools, first-aid, and Walkie-Talkie."),
      PevConvoySignal("Corking Protocol", "Safe group intersection crossing", "Wait for green lights; never block aggressive motorists — split into small pods if light turns red."),
      PevConvoySignal("2-Second Stagger Rule", "Left-Right-Left spacing", "Never ride directly behind another wheel's rear tire; stagger so you always have an escape lane."),
      PevConvoySignal("Night Convoy Lights", "Steady rear red (avoid blinding strobe in pack)", "Use steady rear lights inside pack so riders behind don't lose depth perception; Sweep runs bright strobe."),
      PevConvoySignal("Speed Differential Rule", "Mix of EUCs, Scooters & eBikes", "Group pace matches the slowest comfortable rig; fast rigs loop back rather than forcing beginners over PWM limits."),
      PevConvoySignal("Pre-Ride Voltage Roll Call", "Check pack % & target route miles", "Confirm every rider's worst-case Wh/mi covers route + 25% reserve margin."),
      PevConvoySignal("Charge Etiquette", "Share AC circuits (15A = 1800W max)", "Stagger fast chargers! Two 10A 100V chargers = 2000W -> trips 15A café breaker!"),
      PevConvoySignal("Li-Ion Smoke / Hiss Emergency", "Thermal Runaway Response", "Drop PEV immediately, move 30+ ft upwind (toxic HF gas!), call 911. Never use small ABC extinguisher to hover over venting cells."),
      PevConvoySignal("Crash Scene Triage", "Protect, Assess, Alert", "Block traffic with flashing lights, do NOT remove full-face helmet if neck pain suspected, trigger SOS GPS pin."),
      PevConvoySignal("Walkie-Talkie Comms Check", "Channel + Callsign verification", "Confirm PTT audio before rollout; keep transmissions under 5 seconds ('Lead to Sweep: sharp gravel right in 100 yards')."),
      PevConvoySignal("Passing Faster Rider", "Always pass on LEFT", "Never undertake on the right shoulder; announce clearly before passing."),
      PevConvoySignal("Wet Metal Bridge Deck", "Zero lean, zero throttle input", "Steel grates, painted crosswalks & wet leaves turn to ice — coast straight across."),
      PevConvoySignal("Wind Gust Stance", "Lower center of gravity", "Bend knees, widen foot stance, keep relaxed grip on handlebars so wind gusts don't induce tank-slapper wobble."),
    )

  val PEV_GLOSSARY: List<PevGlossaryItem> =
    listOf(
      PevGlossaryItem("Wh (Watt-Hours)", "Battery", "True energy tank size of a PEV battery (Nominal Volts × Amp-Hours). More reliable than Ah alone when comparing 48V vs 72V rigs."),
      PevGlossaryItem("Wh/mi (Efficiency)", "Telemetry", "Energy burned per mile. Typical: eBike 12–20 Wh/mi, Commuter Scooter 18–26, Hyper-Scooter/EUC 30–45, eMoto 50–75."),
      PevGlossaryItem("Voltage Sag", "Battery", "Temporary voltage drop under heavy throttle due to cell internal resistance (IR). High-drain cells (Molicel P45B, Samsung 50S) sag much less."),
      PevGlossaryItem("C-Rate", "Battery", "Charge/discharge speed relative to capacity. 1C on a 20Ah pack = 20A (1 hour); 0.2C = 4A (gentle 5-hour charge)."),
      PevGlossaryItem("Series / Parallel (e.g. 20S4P)", "Battery", "S = cells in series (multiplies voltage: 20×3.6V=72V nominal). P = cells in parallel (multiplies capacity Ah and max amps)."),
      PevGlossaryItem("BMS (Battery Management System)", "Battery", "Safety circuit monitoring cell voltage, temperature, and over-current, plus balancing parallel groups."),
      PevGlossaryItem("Active vs Passive Balancing", "Battery", "Passive bleeds high cells as heat (slow, 30–100mA). Active transfers energy from high to low cells (fast, 1–2A like JK-BMS)."),
      PevGlossaryItem("SOH (State of Health)", "Battery", "Remaining pack capacity vs brand new (100%). Typically reaches 80% after 500–800 NMC cycles or 2000+ LFP cycles."),
      PevGlossaryItem("NMC vs LFP (LiFePO4)", "Battery", "NMC is lighter & energy-dense (4.2V max). LFP is heavier but lasts 3–4x longer and is far more thermally stable (3.65V max)."),
      PevGlossaryItem("FOC (Field-Oriented Control)", "Controller", "Sinewave motor commutation delivering silent, ultra-smooth torque and higher efficiency than noisy square-wave controllers."),
      PevGlossaryItem("VESC", "Controller", "Open-source high-power FOC motor controller ecosystem (Flipsky, Spintend, Trampa, Floatwheel) with full custom tuning."),
      PevGlossaryItem("PWM (Pulse Width Modulation)", "EUC / Safety", "Duty cycle % of battery voltage sent to motor. On self-balancing EUCs/Onewheels, exceeding ~85–90% PWM leaves no headroom -> cutout!"),
      PevGlossaryItem("Cutout / Nosedive", "Safety", "When rider demands more torque/speed than motor & battery can supply at current voltage, causing self-balancing wheel to drop nose."),
      PevGlossaryItem("Tiltback / Pushback", "Safety", "Intentional nose-up lift by EUC or Onewheel warning you to slow down before hitting cutout."),
      PevGlossaryItem("Field Weakening", "Controller", "Advanced FOC technique injecting d-axis current to push top speed 15–25% past motor back-EMF limit at the cost of extra heat & Wh/mi."),
      PevGlossaryItem("Back-EMF", "Motor", "Voltage generated by spinning motor opposing battery voltage — fundamentally limits top speed until battery V rises or field weakening engages."),
      PevGlossaryItem("KV Rating", "Motor", "Unloaded motor RPM per applied Volt. Lower KV = higher torque/climbing; Higher KV = higher top speed."),
      PevGlossaryItem("Phase Amps vs Battery Amps", "Controller", "Phase amps govern low-speed torque & motor heat; Battery amps govern top-end power & battery sag. Phase amps can safely exceed battery amps (e.g. 120A phase / 50A battery)."),
      PevGlossaryItem("Regen Braking", "Controller", "Turns motor into generator during braking, slowing wheel smoothly while recovering 3–10% energy back into pack."),
      PevGlossaryItem("Hall Sensors", "Motor", "Three tiny magnetic switches inside stator telling controller exact rotor position for smooth 0-MPH launch."),
      PevGlossaryItem("Sensorless / HFI", "Controller", "Running brushless motor without Hall wires using back-EMF or High-Frequency Injection (HFI) for silent starts."),
      PevGlossaryItem("MOSFET", "Controller", "High-speed power transistor switching battery voltage to motor phases. Weakest point during overheating or shorted phase wires."),
      PevGlossaryItem("Statorade (Ferrofluid)", "Motor", "Magnetic oil bridging air gap between stator & rotor magnets to dump heat through motor casing."),
      PevGlossaryItem("IP Rating (IP54 / IPX6 / IP67)", "Hardware", "First digit = dust (5–6), second = water (4=splashes, 5=jets, 6=heavy jets, 7=1m immersion). Always protect bearings & charge ports."),
      PevGlossaryItem("Death Wobble / Speed Wobble", "Chassis", "Self-amplifying front steering oscillation caused by loose headset, unbalanced tire, high speed, or stiff death-grip arms."),
      PevGlossaryItem("Steering Damper", "Chassis", "Hydraulic oil cylinder mounted between frame & stem absorbing sudden steering oscillations at 35+ MPH."),
      PevGlossaryItem("Split Rim", "Tires", "Two-piece bolted wheel hub letting scooter riders swap tubes/tires easily without fighting stiff tire beads."),
      PevGlossaryItem("PMT Radial Tire", "Tires", "Italian motorsport-grade radial rubber scooter tires famous for sticky cornering grip and zero flat-spotting."),
      PevGlossaryItem("Tannus Armour", "Tires", "Closed-cell foam tire liner shielding eBike tubes against thorns, glass, and rim pinch flats."),
      PevGlossaryItem("Sintered vs Organic Pads", "Brakes", "Organic/resin pads are quiet & grab cold. Sintered metallic pads handle extreme 50+ MPH heat & wet mud without fading."),
      PevGlossaryItem("DOT4 vs Mineral Oil", "Brakes", "NEVER mix! Shimano/Tektro/Magura/Nutt/Logan use Mineral Oil. SRAM/Moto/Sur-Ron Ultra Bee use corrosive DOT4/5.1."),
      PevGlossaryItem("Torque Arm", "Drivetrain", "Steel reinforcement bracket preventing high-power hub motor axles from spinning inside aluminum bike dropouts and shredding cables."),
      PevGlossaryItem("Ghosting (Onewheel)", "Safety", "Board continuing to accelerate after rider jumps off due to stuck/delaminated footpad pressure sensor."),
      PevGlossaryItem("Power Pads (EUC)", "EUC", "Sculpted foam/TPU shin & calf blocks bolted to EUC shell giving leverage for hard acceleration, jumps, and emergency braking."),
      PevGlossaryItem("Smart Charger (Satiator / Roger)", "Charging", "Programmable charger letting you cap charge at 80% for 2–3x battery lifespan or throttle amps down on weak outlets."),
      PevGlossaryItem("Anti-Spark (XT90-S / QS8-S)", "Electrical", "Built-in pre-charge resistor inside connector tip that gently charges controller capacitors so high-voltage plugs don't arc-weld."),
      PevGlossaryItem("Internal Resistance (IR)", "Battery", "Measured in milliohms (mΩ). Lower IR = cooler running, less sag, and higher peak power. Rises as cells age."),
      PevGlossaryItem("Range Anxiety / RTB Margin", "Telemetry", "Keep at least 25% pack reserve when turning back to base — cold wind & hills punish the last 20% voltage curve."),
      PevGlossaryItem("PAS (Pedal Assist System)", "eBike", "Cadence sensor (simple magnet disc on/off) vs Torque sensor (measures exact leg pressure for natural bicycle feel)."),
      PevGlossaryItem("StVZO Cutoff Beam", "Lighting", "German road-legal headlight optic projecting a razor-sharp flat horizon so 2000+ lumens light the road without blinding oncoming drivers."),
    )
}
