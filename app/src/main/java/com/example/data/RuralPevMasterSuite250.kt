package com.example.data

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

data class TorqueSpecItem(
  val id: String,
  val component: String,
  val group: String,
  val torqueNm: Double,
  val threadLock: String,
  val gravelCheckNote: String,
) {
  val torqueFtLbs: Double get() = torqueNm * 0.737562
}

data class BarnFaultTreeItem(
  val id: String,
  val symptom: String,
  val subsystem: String,
  val meterCheck: String,
  val barnFix: String,
  val aiPrompt: String,
)

data class TerrainPsiProfile(
  val id: String,
  val surface: String,
  val category: String,
  val psiMultiplier: Double,
  val maxSafeLeanDeg: Int,
  val rollingCoeff: Double,
  val ridingTip: String,
)

data class RuralHazardPreset(
  val id: String,
  val label: String,
  val group: String,
  val firestoreType: String,
  val description: String,
)

data class ChannelRuralSession(
  val id: String,
  val title: String,
  val group: String,
  val query: String,
  val isMusicMix: Boolean,
)

data class BoardRuralTemplate(
  val id: String,
  val label: String,
  val group: String,
  val category: String,
  val titleTemplate: String,
  val bodyTemplate: String,
)

data class BarnSolarRecipe(
  val id: String,
  val name: String,
  val watts: Int,
  val breakerAmps: Int,
  val note: String,
)

object RuralPevPhysics {
  fun computeDynamicTirePsi(basePsi: Double, totalRiderAndRigLbs: Double, surfaceMultiplier: Double): Double {
    val weightDelta = ((totalRiderAndRigLbs - 220.0) / 35.0) * 1.6
    return ((basePsi + weightDelta) * surfaceMultiplier).coerceIn(14.0, 65.0)
  }

  data class BreakerLoadResult(
    val totalWatts: Int,
    val lineAmps: Double,
    val continuousSafeAmps: Double,
    val isOverloaded: Boolean,
    val advice: String,
  )

  fun computeBarnBreakerLoad(chargerWatts: Int, rigCount: Int, breakerAmps: Int): BreakerLoadResult {
    val totalW = (chargerWatts * rigCount).coerceAtLeast(0)
    val lineA = totalW / 120.0
    val safeA = breakerAmps * 0.80
    val overloaded = lineA > safeA
    val advice =
      when {
        lineA > breakerAmps -> "TRIP RISK! Exceeds ${breakerAmps}A breaker. Stagger chargers or use a separate circuit."
        overloaded -> "Exceeds 80% continuous barn load (${"%.1f".format(safeA)}A). Drop charger amps by 20%."
        else -> "Safe continuous barn load (${"%.0f".format((lineA / breakerAmps) * 100.0)}% of ${breakerAmps}A breaker)."
      }
    return BreakerLoadResult(totalW, lineA, safeA, overloaded, advice)
  }

  fun computeSolarChargeHours(packWh: Double, startPct: Int, targetPct: Int, panelWatts: Int, sunFactor: Double): Double {
    val neededWh = packWh * ((targetPct - startPct).coerceAtLeast(0) / 100.0)
    val effectiveW = (panelWatts * sunFactor * 0.82).coerceAtLeast(10.0)
    return neededWh / effectiveW
  }

  data class HillClimbResult(
    val requiredWatts: Double,
    val batteryAmps: Double,
    val thermalRisk: String,
  )

  fun computeHillClimbPower(totalLbs: Double, speedMph: Double, gradePct: Double, packVoltage: Double, rollingCrr: Double): HillClimbResult {
    val massKg = totalLbs.coerceAtLeast(60.0) * 0.453592
    val speedMs = speedMph.coerceAtLeast(1.0) * 0.44704
    val theta = atan(gradePct / 100.0)
    val gravityForce = massKg * 9.81 * sin(theta)
    val rollingForce = massKg * 9.81 * cos(theta) * rollingCrr
    val aeroForce = 0.5 * 1.225 * 0.55 * 0.65 * speedMs * speedMs
    val wheelWatts = max(0.0, (gravityForce + rollingForce + aeroForce) * speedMs)
    val electricalWatts = wheelWatts / 0.84
    val profile = com.example.core.PevBatteryPhysics.packProfile(packVoltage.coerceAtLeast(24.0), "NMC")
    val rPackOhms = (profile.seriesCells * 0.022 * 1.15) / 5.0
    val estLoadV = (profile.nominalPackV - (electricalWatts / profile.nominalPackV) * rPackOhms).coerceAtLeast(profile.cutoffEmptyV)
    val amps = electricalWatts / estLoadV
    val risk =
      when {
        amps > 65.0 -> "HIGH HEAT — Feather throttle & zig-zag steep grade"
        amps > 40.0 -> "WARM CLIMB — Keep speed > 10 MPH for motor cooling"
        else -> "COOL & EFFICIENT — Sustainable rural climb"
      }
    return HillClimbResult(electricalWatts, amps, risk)
  }

  fun computeCreekFordSafety(depthInches: Double, ipRating: String, axleHeightInches: Double): Pair<Boolean, String> {
    val sealBonus =
      when (ipRating.uppercase()) {
        "IP67", "IP68" -> 1.5
        "IP66" -> 0.5
        "IP65" -> 0.0
        else -> -2.0
      }
    val safeDepth = (axleHeightInches - 2.0 + sealBonus).coerceAtLeast(1.0)
    return if (depthInches <= safeDepth) {
      true to "PASSABLE (${"%.1f".format(depthInches)}\" <= ${"%.1f".format(safeDepth)}\" limit). Keep steady momentum; dry rotors afterward."
    } else {
      false to "DO NOT RIDE (${"%.1f".format(depthInches)}\" exceeds ${"%.1f".format(safeDepth)}\" hub seal margin). Walk across with power OFF."
    }
  }

  fun computeColdWeatherRangeFactor(tempF: Double, chemistry: String): Double {
    val tempC = (tempF - 32.0) * 5.0 / 9.0
    return com.example.core.PevBatteryPhysics.coldCapacityFactor(tempC, chemistry)
  }

  fun computeGasSavingsUsd(distanceKm: Double, whUsed: Double, kwhRateUsd: Double, gasPriceUsd: Double, mpg: Double): Double {
    val miles = distanceKm * 0.621371
    val gasCost = (miles / mpg.coerceAtLeast(8.0)) * gasPriceUsd
    val elecCost = (whUsed / 1000.0) * kwhRateUsd
    return (gasCost - elecCost).coerceAtLeast(0.0)
  }
}

object RuralPevMasterSuite250 {
  // 60 REAL PEV FASTENER & TORQUE SPECS
  val TORQUE_SPECS: List<TorqueSpecItem> =
    listOf(
      TorqueSpecItem("tq01", "Front Hub Motor Axle Nut (M12)", "Axles & Wheels", 45.0, "Blue 243", "Inspect after every 100 mi of gravel"),
      TorqueSpecItem("tq02", "Rear Hub Motor Axle Nut (M14)", "Axles & Wheels", 65.0, "Blue 243", "Verify torque washer seated in dropout"),
      TorqueSpecItem("tq03", "Split-Rim Hub Bolts (M5x6)", "Axles & Wheels", 8.5, "Blue 243", "Tighten in star pattern; never over-torque"),
      TorqueSpecItem("tq04", "Heavy Split-Rim Bolts (M6x6)", "Axles & Wheels", 13.0, "Blue 243", "Check for pinch before seating bead"),
      TorqueSpecItem("tq05", "E-Moto Front Axle Pinch Bolts", "Axles & Wheels", 12.0, "Blue 243", "Pump fork twice before final tighten"),
      TorqueSpecItem("tq06", "E-Moto Rear Axle Nut (Sur-Ron/Talaria)", "Axles & Wheels", 58.0, "None (Locknut)", "Check chain alignment marks both sides"),
      TorqueSpecItem("tq07", "Torque Arm Pinch Bolt (M6)", "Axles & Wheels", 11.0, "Red 271", "Zero play allowed on high-wattage hubs"),
      TorqueSpecItem("tq08", "EUC Motor Cover Screws (M4)", "Axles & Wheels", 4.5, "Blue 242", "Re-seal silicone bead after tire swap"),
      TorqueSpecItem("tq09", "Onewheel Hub Axle Bolts (5/16\")", "Axles & Wheels", 27.0, "Blue 243", "Inspect after curb drops or rock strikes"),
      TorqueSpecItem("tq10", "E-Skate Kingpin Locknut", "Axles & Wheels", 14.0, "Nyloc", "Adjust 1/4 turn for rural washboard stability"),
      TorqueSpecItem("tq11", "Brake Rotor 6-Bolt (M5 Torx T25)", "Brakes", 6.2, "Blue 243", "Star pattern; degrease rotor with IPA"),
      TorqueSpecItem("tq12", "Hydraulic Caliper Mount Bolts (M6)", "Brakes", 9.5, "Blue 243", "Squeeze lever while centering caliper"),
      TorqueSpecItem("tq13", "Brake Caliper Adapter Bracket", "Brakes", 10.5, "Blue 243", "Verify 180mm vs 203mm arrow orientation"),
      TorqueSpecItem("tq14", "Brake Lever Bar Clamp (M5)", "Brakes", 5.0, "None", "Snug enough to rotate on impact, not snap"),
      TorqueSpecItem("tq15", "Hydraulic Banjo Fitting Bolt", "Brakes", 6.0, "None", "Replace copper crush washers if weeping"),
      TorqueSpecItem("tq16", "Bleed Port Screw (Mineral/DOT)", "Brakes", 2.5, "None", "Never strip soft aluminum threads"),
      TorqueSpecItem("tq17", "Brake Pad Retaining Pin", "Brakes", 2.0, "Clip", "Install safety cotter clip every ride"),
      TorqueSpecItem("tq18", "Mechanical Brake Cable Anchor", "Brakes", 6.5, "None", "Check for fray near clamp"),
      TorqueSpecItem("tq19", "Stem Faceplate 4-Bolt Clamp (M5)", "Steering & Stem", 6.0, "Blue 242", "Keep top & bottom gaps perfectly even"),
      TorqueSpecItem("tq20", "Stem Steerer Pinch Bolts (M6)", "Steering & Stem", 11.0, "Blue 243", "Critical after any gravel front-end hit"),
      TorqueSpecItem("tq21", "Headset Top Cap Preload Bolt", "Steering & Stem", 2.5, "None", "Tighten only until fork play disappears"),
      TorqueSpecItem("tq22", "Folding Stem Latch Cam Bolt", "Steering & Stem", 18.0, "Blue 243", "Zero creak when rocking bars fore/aft"),
      TorqueSpecItem("tq23", "Steering Damper Frame Mount", "Steering & Stem", 10.0, "Blue 243", "Check full lock left & right clearance"),
      TorqueSpecItem("tq24", "Triple-Clamp Upper Crown Pinch", "Steering & Stem", 16.0, "Blue 243", "Check after any trail washout drop"),
      TorqueSpecItem("tq25", "Triple-Clamp Lower Crown Pinch", "Steering & Stem", 14.0, "Blue 243", "Over-torquing binds fork stanchions"),
      TorqueSpecItem("tq26", "Handlebar Lock-On Grip Collar", "Steering & Stem", 3.0, "None", "Check before wet mud rides"),
      TorqueSpecItem("tq27", "Throttle Clamp Screw (M4)", "Steering & Stem", 2.2, "None", "Avoid binding plastic return spring"),
      TorqueSpecItem("tq28", "Display / Voltmeter Bar Bracket", "Steering & Stem", 2.0, "None", "Use rubber shim to absorb washboard vibe"),
      TorqueSpecItem("tq29", "Rear Swingarm Pivot Bolt (M10)", "Suspension & Frame", 36.0, "Blue 243", "Grease needle bearings every 500 mi"),
      TorqueSpecItem("tq30", "Front Swingarm Pivot Bolt (M8)", "Suspension & Frame", 24.0, "Blue 243", "Check for lateral play on dual-motor rigs"),
      TorqueSpecItem("tq31", "Coil/Air Shock Eye Bolt (M8)", "Suspension & Frame", 18.0, "Blue 243", "Inspect DU bushings for oval wear"),
      TorqueSpecItem("tq32", "Suspension Linkage Triangle Bolts", "Suspension & Frame", 16.0, "Blue 243", "Clean clay mud out of linkage bearings"),
      TorqueSpecItem("tq33", "Deck Lid Perimeter Screws (M4)", "Suspension & Frame", 3.5, "Blue 242", "Press silicone gasket evenly for IP rating"),
      TorqueSpecItem("tq34", "Kickstand Frame Mount Bolts (M6)", "Suspension & Frame", 11.0, "Blue 243", "High vibration zone on gravel roads"),
      TorqueSpecItem("tq35", "Rear Fender / Mudguard Stays", "Suspension & Frame", 6.5, "Blue 243", "Use rubber washers to stop cracking"),
      TorqueSpecItem("tq36", "Footpeg Bracket Bolts (E-Moto)", "Suspension & Frame", 28.0, "Red 271", "Inspect after standing climbs"),
      TorqueSpecItem("tq37", "EUC Pedal Hanger Chassis Bolts", "Suspension & Frame", 22.0, "Blue 243", "Critical load-bearing fasteners"),
      TorqueSpecItem("tq38", "Rear Cargo Rack Frame Mounts", "Suspension & Frame", 9.0, "Blue 243", "Re-check when hauling barn tools"),
      TorqueSpecItem("tq39", "Seat Post / Saddle Rail Clamp", "Suspension & Frame", 12.0, "None", "Inspect after rough pasture riding"),
      TorqueSpecItem("tq40", "Battery Box Hold-Down Strap Bolts", "Suspension & Frame", 8.0, "Blue 242", "Zero pack rattle protects nickel strips"),
      TorqueSpecItem("tq41", "Rear Chain Sprocket Bolts (M6x4)", "Drivetrain", 14.0, "Blue 243", "Check runout & missing teeth after mud"),
      TorqueSpecItem("tq42", "Jackshaft / Primary Belt Tensioner", "Drivetrain", 12.0, "Blue 243", "Leave 5-8mm belt deflection"),
      TorqueSpecItem("tq43", "Mid-Drive Motor Mounting Bolts", "Drivetrain", 25.0, "Blue 243", "Creaking on hill climb = loose mount"),
      TorqueSpecItem("tq44", "E-Bike Bottom Bracket Lockring", "Drivetrain", 40.0, "Grease", "Verify cadence/torque sensor clearance"),
      TorqueSpecItem("tq45", "Crank Arm Pinch Bolts (M6)", "Drivetrain", 13.0, "Blue 242", "Alternate tightening left/right bolts"),
      TorqueSpecItem("tq46", "Pedal Spindle Threads (9/16\")", "Drivetrain", 35.0, "Anti-Seize", "Left pedal is reverse-threaded"),
      TorqueSpecItem("tq47", "Derailleur Hanger Mounting Bolt", "Drivetrain", 9.0, "Blue 242", "Carry spare hanger on rural trails"),
      TorqueSpecItem("tq48", "Chainring Spider Bolts", "Drivetrain", 10.0, "Blue 243", "Check after steep singletrack climbs"),
      TorqueSpecItem("tq49", "Belt Drive Pulley Grub Screws", "Drivetrain", 4.0, "Blue 243", "Align flat spot on motor shaft"),
      TorqueSpecItem("tq50", "Freewheel / Cassette Lockring", "Drivetrain", 40.0, "Grease", "Inspect pawls if coasting clicks loudly"),
      TorqueSpecItem("tq51", "Controller Phase Terminal Lugs (M5)", "Electrical", 4.2, "Spring Washer", "Loose phase lug melts insulation fast"),
      TorqueSpecItem("tq52", "Main Battery Busbar Bolts (M6)", "Electrical", 6.5, "Lock Washer", "Disconnect main fuse before touching"),
      TorqueSpecItem("tq53", "Controller Heatsink Deck Screws", "Electrical", 4.5, "Thermal Paste", "Fresh thermal pad prevents 70C cutoff"),
      TorqueSpecItem("tq54", "High-Current Fuse Holder Studs", "Electrical", 5.5, "Lock Washer", "Zero discoloration or heat crust"),
      TorqueSpecItem("tq55", "DC-DC 12V Buck Converter Mount", "Electrical", 3.0, "Blue 242", "Keep high & dry inside deck"),
      TorqueSpecItem("tq56", "Headlight Fork Crown Bracket", "Electrical", 7.5, "Blue 243", "Aim cutoff below oncoming truck eyes"),
      TorqueSpecItem("tq57", "Horn / Siren Mounting Bolt", "Electrical", 5.0, "Blue 242", "Face horn downward to drain rainwater"),
      TorqueSpecItem("tq58", "Charge Port Locknut (GX16/XT60)", "Electrical", 3.5, "Silicone", "Verify cap gasket seals out gravel dust"),
      TorqueSpecItem("tq59", "GPS Tracker / AirTag Hidden Mount", "Electrical", 2.5, "Blue 242", "Avoid enclosing inside full metal shield"),
      TorqueSpecItem("tq60", "BMS Balance Sense Bracket Screws", "Electrical", 1.8, "None", "Never pinch thin balance ribbon wires"),
    )

  // 45 INTERACTIVE BARN-SHOP FAULT TREES
  val FAULT_TREES: List<BarnFaultTreeItem> =
    listOf(
      BarnFaultTreeItem("ft01", "Motor stutters / jerks from standstill", "Motor & Hall", "Measure 5V between Red/Black Hall plug; spin wheel slowly — Yellow/Green/Blue should toggle 0V <-> 5V.", "Re-seat waterproof Julet Hall plug, inspect pinched axle wire, or run sensorless VESC fallback.", "My PEV motor stutters and jerks on takeoff. Walk me through testing the 3 Hall sensors with a multimeter and fixing it in my barn shop."),
      BarnFaultTreeItem("ft02", "Wheel hard to spin even with power OFF", "Controller", "Disconnect 3 thick phase wires from controller. If wheel spins freely now, controller MOSFETs are shorted.", "Replace shorted controller and inspect motor phase wires for rubbed insulation.", "My PEV wheel resists spinning even when turned off. Explain the shorted-MOSFET phase wire test and replacement checklist."),
      BarnFaultTreeItem("ft03", "Instant full power shutoff on hard hill climb", "Battery & BMS", "Check resting pack voltage vs peak voltage under load. If sag drops below 3.0V/cell, BMS LVC tripped.", "Inspect parallel cell group imbalance, high-IR aging pack, or reduce battery amps limit 15%.", "My PEV shuts off completely halfway up steep rural hills. Help me diagnose BMS low-voltage cutoff vs cell sag."),
      BarnFaultTreeItem("ft04", "Charger stays green & refuses to charge pack", "Charging", "Measure charger output with multimeter (e.g., 54.6V for 48V, 67.2V for 60V, 84.0V for 72V) and charge port fuse.", "Replace blown inline charge fuse, check charge-port solder joints, or wake BMS after cold barn storage.", "My PEV charger light stays green and won't charge the pack. Give me the exact step-by-step multimeter voltage and fuse diagnostic."),
      BarnFaultTreeItem("ft05", "Throttle unresponsive (display ON, no error)", "Throttle", "Test throttle Hall signal wire: should read ~0.8V at rest and rise smoothly to ~4.2V at full twist/thumb.", "Check brake-lever cutoff icon on dash (stuck reed switch), swap throttle plug, or re-seat magnet.", "My PEV display turns on normally with no error code, but the throttle does nothing. Walk me through brake cutoff and Hall throttle tests."),
      BarnFaultTreeItem("ft06", "High-speed handlebar wobble above 28 MPH", "Chassis", "Lift wheel and check headset play, tire out-of-round bead seating, and swingarm pivot slack.", "Preload headset bearings, true tire bead with soapy water, balance wheel, and add steering damper.", "My PEV develops a front-end speed wobble on rural roads above 28 MPH. Give me the exact mechanical inspection and damping fix."),
      BarnFaultTreeItem("ft07", "Spongy hydraulic brake lever pulls to bar", "Brakes", "Inspect caliper pistons for fluid weep and check rotor thickness (>= 1.5mm).", "Perform gravity + syringe bleed with matching Mineral or DOT fluid and clean glazed pads.", "My hydraulic brakes feel spongy and pull almost to the grip. Walk me through a clean barn-bench brake bleed."),
      BarnFaultTreeItem("ft08", "Loud metallic screech when braking after creek", "Brakes", "Inspect rotor surface for embedded grit/clay and oil contamination.", "Scrub rotor with 91% isopropyl + Scotch-Brite, sand pad face lightly on flat glass, and re-bed pads.", "My disc brakes screech loudly after riding muddy gravel and shallow water. How do I decontaminate and re-bed them?"),
      BarnFaultTreeItem("ft09", "Pack stops charging at 88-93% (won't hit 100%)", "Battery & BMS", "Compare final voltage to full charge target (e.g. 54.6V for 13S, 67.2V for 16S, 84.0V for 20S).", "Leave charger connected 2-4 hours after green to let passive BMS balance high group; check cell drift.", "My PEV battery stops charging around 90% and won't reach full 100% voltage. Explain cell group imbalance and safe balancing."),
      BarnFaultTreeItem("ft10", "Grinding or clicking noise from hub axle", "Motor & Hall", "Remove wheel and spin inner bearing races by finger; feel for gritty notches.", "Press out rusted 6001/6202 bearings, pack marine waterproof grease, install sealed 2RS bearings.", "I hear a grinding/clicking sound from my hub motor on rural trails. How do I confirm bad wheel bearings and swap them?"),
      BarnFaultTreeItem("ft11", "Tubeless tire loses 10 PSI overnight", "Tires", "Spray soapy water on valve stem base, bead edge, and tread pinholes.", "Tighten Presta/Schrader valve core, inject 60ml latex/fiber sealant, or seat bead at 55 PSI briefly.", "My tubeless PEV tire leaks down overnight after gravel riding. Walk me through finding and sealing slow rim/valve leaks."),
      BarnFaultTreeItem("ft12", "E-brake regen feels harsh or cuts in late", "Controller", "Check P-settings / VESC regen current limits and lever magnetic sensor gap.", "Dial electronic brake strength to 2/5 and set brake-lever magnet 1-2mm from Hall sensor.", "My regenerative braking grabs way too hard on loose gravel. How should I tune regen current and e-brake settings safely?"),
      BarnFaultTreeItem("ft13", "Front motor drops out in dual-motor mode", "Controller", "Swap dual/single handlebar switch plug or test continuity on ECO/DUAL switch.", "Clean mud from handlebar switch, inspect front controller XT60 power tap, check front thermal cutoff.", "My dual-motor scooter randomly drops the front motor mid-ride. Walk me through switch, connector, and thermal diagnostics."),
      BarnFaultTreeItem("ft14", "Voltage reads 3V lower on dash than multimeter", "Electrical", "Measure battery XT90 directly vs display P-setting voltage calibration.", "Correct voltage calibration offset in display/VESC and check for high-resistance corroded main connector.", "My cockpit voltage display reads lower than my multimeter at the battery pack. How do I calibrate it and check connector drop?"),
      BarnFaultTreeItem("ft15", "Deck creaks loudly on every bump", "Chassis", "Press deck corners while stationary and isolate folding stem vs swingarm bushings.", "Clean dry grit from folding block, apply PTFE dry lube, and torque swingarm pivot bolts to spec.", "My PEV deck and stem creak constantly on washboard gravel. Give me a step-by-step silence & bolt-check checklist."),
      BarnFaultTreeItem("ft16", "Chain jumps teeth under heavy trail torque", "Drivetrain", "Measure 12 chain links (should be 12.0\"; >12.125\" is stretched) and check rear sprocket hook wear.", "Replace stretched 420/219 chain and worn sprocket together; align rear axle tension blocks.", "My e-moto chain slips and jumps teeth on steep dirt climbs. How do I check chain stretch, tension, and sprocket wear?"),
      BarnFaultTreeItem("ft17", "12V headlight & horn dead while motor runs", "Electrical", "Measure 12V buck converter output (Red/Black) with key ON; inspect 12V blade fuse.", "Replace inline 10A fuse or swap blown 60V/72V-to-12V step-down buck module.", "My PEV motor runs fine, but the headlight, taillight, and horn are all dead. Help me test the 12V DC-DC buck converter."),
      BarnFaultTreeItem("ft18", "Controller overheating after 10 min hill climb", "Thermal", "Feel deck underside temperature; check if controller sits on air gap instead of aluminum deck.", "Install 2mm silicone thermal pad + paste between controller case and deck; drop phase amps 10%.", "My PEV controller hits thermal rollback on long rural hills. How do I improve deck heat sinking and tune phase amps?"),
      BarnFaultTreeItem("ft19", "Erratic speedometer / odometer freezes", "Motor & Hall", "Check motor speed-sensor wire (white wire in Hall plug) or P-setting magnet pole count.", "Verify magnet pole pairs (usually 30 for hub motors) and clean Hall connector pins.", "My speedometer jumps erratically or drops to 0 while moving. How do I check the speed wire and pole-pair setting?"),
      BarnFaultTreeItem("ft20", "Charger trips 15A barn GFCI outlet", "Charging", "Sum total load on barn circuit; measure inrush surge when plugging in 8A-10A charger.", "Plug charger into wall BEFORE connecting to PEV, reduce charger to 5A, or move off shared freezer circuit.", "My fast PEV charger keeps tripping our 15A barn breaker/GFCI. Explain safe circuit load math and anti-spark plug sequence."),
      BarnFaultTreeItem("ft21", "Spark pop when plugging charger into PEV", "Charging", "Inspect charge port pins for pitting and confirm charger is powered ON first.", "Power charger from wall first so capacitors pre-charge, or wire XT60-S anti-spark connector.", "My charging port sparks loudly when I plug in. Is that dangerous and how do I stop charger port arcing?"),
      BarnFaultTreeItem("ft22", "Fork bottoms out hard on rural potholes", "Suspension", "Check air fork PSI or coil preload collar and measure static sag (target 20-25%).", "Increase air pressure 10 PSI / add heavier coil spring and dial compression damping 3 clicks firmer.", "My front suspension bottoms out harshly on rural road potholes. Help me set static sag, spring rate, and rebound."),
      BarnFaultTreeItem("ft23", "Pinch flats on gravel every other week", "Tires", "Inspect punctured tube: two parallel 'snakebite' slits confirm too-low PSI.", "Bump PSI +5 for gravel, add Mr. Tuffy tire liner or FlatOut sealant, or convert to tubeless.", "I keep getting pinch flats on county gravel roads. What PSI, tire liners, and sealant setup prevents snakebite flats?"),
      BarnFaultTreeItem("ft24", "EUC tilts back early with plenty of battery", "EUC", "Check app PWM % and cell group minimum voltage; cold weather spikes sag.", "Warm pack indoors before winter ride, lower max cruise speed in cold, verify cell balance.", "My EUC gives early tiltback even at 60% battery in chilly rural weather. Explain PWM margin and cold sag limits."),
      BarnFaultTreeItem("ft25", "Onewheel surges unexpectedly uphill", "Onewheel", "Check tire PSI, rider stance forward bias, and battery temp.", "Keep hips centered over axle, carve S-turns up steep grades, and respect pushback speed.", "My Onewheel surges on rural hills. Explain duty-cycle physics and safe uphill carving technique."),
      BarnFaultTreeItem("ft26", "Water droplets inside display after rain", "Waterproofing", "Power OFF immediately; do not charge. Check screen perimeter seam.", "Dry 24h in warm airflow with silica gel, then bead clear neutral-cure silicone around bezel.", "Moisture got inside my PEV display after a rural rain shower. Give me the safe dry-out and waterproofing protocol."),
      BarnFaultTreeItem("ft27", "Motor phase plug smells burnt / melted", "Electrical", "Inspect yellow/green/blue bullet plugs for dark oxidation or loose fit.", "Cut off melted bullets and solder heavy MR60 / MT60 or bolted ring-terminal phase block.", "One of my motor phase wire bullet plugs melted inside the deck. How do I safely upgrade to MT60 or a bolted terminal block?"),
      BarnFaultTreeItem("ft28", "Rear tire wobbles side-to-side visually", "Tires", "Spin wheel and watch rim edge vs tire sidewall witness line.", "Deflate to 5 PSI, massage bead with dish soap solution, inflate to 50 PSI until witness line seats evenly.", "My new tire looks wobbly side-to-side when spinning. Walk me through seating a stubborn tire bead evenly."),
      BarnFaultTreeItem("ft29", "Key ignition switch cuts power on bumps", "Electrical", "Wiggle key barrel while display is ON; test continuity across switch leads.", "Flush ignition contacts with DeoxIT contact cleaner or replace worn barrel switch.", "My PEV shuts off momentarily when hitting gravel bumps near the key switch. How do I test and bypass/replace it?"),
      BarnFaultTreeItem("ft30", "Battery won't wake after winter storage", "Battery & BMS", "Measure pack voltage at discharge lead. If below 2.5V/cell, DO NOT fast charge!", "Check pack safety first; inspect BMS low-temp lock; never blindly jump-start deeply over-discharged cells.", "My PEV sat in an unheated barn all winter and won't turn on or charge. Walk me through safe voltage inspection."),
      BarnFaultTreeItem("ft31", "Regen cuts out when pack is at 98-100%", "Battery & BMS", "Expected BMS over-voltage protection when pack is near full charge.", "Use mechanical brakes for first 2 miles from hilltop home or charge daily to 85-90%.", "Why does my electronic regen brake stop working at the top of my hill right after a full charge?"),
      BarnFaultTreeItem("ft32", "Belt drive squeals on wet mud trails", "Drivetrain", "Check belt tension and inspect pulley grooves for packed grit.", "Wash pulleys with clean water + soft brush, never apply oil/grease to timing belts, check alignment.", "My Sur-Ron/Talaria primary belt squeals after riding wet dirt. How do I clean and tension a PEV drive belt properly?"),
      BarnFaultTreeItem("ft33", "Hydraulic rotor rubs rhythmically (zing-zing)", "Brakes", "Spin wheel looking through caliper pad gap to spot high bend on rotor.", "Gently true rotor with clean truing fork/adjustable wrench in tiny bends; recenter caliper.", "My disc brake rotor makes a rhythmic scraping sound every wheel rotation. How do I true it straight in the shop?"),
      BarnFaultTreeItem("ft34", "Throttle sticks open briefly in cold/dust", "Throttle", "Inspect grip gap rubbing throttle barrel and dust inside twist housing.", "Slide grip 2mm outward from throttle, blow out dust with dry air, lube plastic slide with dry PTFE.", "My twist throttle feels sticky returning to zero after dusty gravel rides. How do I clean and make it 100% safe?"),
      BarnFaultTreeItem("ft35", "Kickstand drags on rough trail ruts", "Chassis", "Check kickstand return spring tension and mounting bolt angle.", "Replace weak return spring, shim bracket, or add safety rubber strap for rocky singletrack.", "My kickstand keeps bouncing down on bumpy rural trails. What's the best reinforcement or spring upgrade?"),
      BarnFaultTreeItem("ft36", "Headlight flickers over washboard gravel", "Electrical", "Trace headlight wire through stem fold pinch point for broken copper strands.", "Splice fresh silicone 18AWG wire with heat-shrink sleeve and add spiral wrap at fold hinge.", "My headlight flickers when I hit rough gravel. Where do stem wiring harnesses usually break and how do I re-wire it?"),
      BarnFaultTreeItem("ft37", "BMS Bluetooth app won't connect", "Battery & BMS", "Verify pack is awake (plug charger 5 sec) and check Bluetooth UART plug on BMS.", "Wake BMS with brief charge pulse, reseat 4-pin BT dongle inside deck, toggle Android Bluetooth.", "My smart BMS Bluetooth module stopped connecting to my phone. How do I wake or reset the BMS BT module?"),
      BarnFaultTreeItem("ft38", "Range dropped 30% suddenly this week", "Battery & BMS", "Check tire PSI first (soft tires eat 20%+ Wh/mi), brake drag, and headwind before blaming cells.", "Pump tires to gravel spec, spin both wheels freely to rule out brake drag, then log 100%-to-20% Wh.", "My PEV range dropped noticeably this week. Give me a systematic checklist to separate tire/brake drag from battery wear."),
      BarnFaultTreeItem("ft39", "Motor makes loud whining hum under load", "Motor & Hall", "Check square-wave vs sine-wave controller mode and loose stator magnets/cover screws.", "Torque motor cover bolts, verify sine-wave FOC tuning in VESC, and check hub axle nut tightness.", "My hub motor started making a louder resonant whine under acceleration. What mechanical and controller checks should I do?"),
      BarnFaultTreeItem("ft40", "Rear shock loses air overnight", "Suspension", "Spray soapy water on Schrader valve core and main seal dust wiper.", "Tighten valve core with valve tool; if wiper bubbles, service air can O-rings with suspension grease.", "My air rear shock won't hold PSI overnight. Walk me through valve-core vs air-can seal testing and servicing."),
      BarnFaultTreeItem("ft41", "Horn button stays stuck / honks nonstop", "Electrical", "Unplug handlebar switch cluster to isolate stuck microswitch from pinched wire.", "Spray electronic cleaner into switch housing or replace waterproof switch pod.", "My horn button stuck on after a muddy ride. How do I diagnose switch vs wiring short?"),
      BarnFaultTreeItem("ft42", "Fender cracks repeatedly on country gravel", "Chassis", "Check unsupported fender overhang vibrating at resonant frequency.", "Add aluminum flat-bar reinforcement brace under fender with rubber isolator grommets.", "My rear mudguard keeps cracking from gravel vibration. Show me how to brace it permanently in a home shop."),
      BarnFaultTreeItem("ft43", "Wheel spoke pings or feels loose", "Axles & Wheels", "Pluck spokes around wheel like guitar strings — dull thud means loose spoke.", "Tighten loose nipples 1/4 turn evenly while checking lateral/radial rim true.", "A couple spokes on my e-bike/e-moto feel loose after trail riding. How do I tension and true the wheel safely?"),
      BarnFaultTreeItem("ft44", "Dual battery parallel switch runs hot", "Electrical", "Verify both packs are identical voltage (within 0.1V) and check module amp rating.", "Never parallel mismatched voltages; upgrade to 80A+ ideal-diode dual battery combiner with 10AWG wire.", "My dual-battery parallel combiner gets warm during long rural rides. Check my wiring gauge and balancer sizing."),
      BarnFaultTreeItem("ft45", "Display shows E07 / E08 / E09 motor/throttle fault", "Controller", "E07 = Motor/Hall, E08 = Throttle, E09 = Controller MOSFET. Inspect exact matching harness.", "Unplug & dry connectors with contact cleaner, test 5V rail on controller, then test individual sensor.", "My PEV dash is throwing an E07/E08/E09 error code. Break down the exact bench troubleshooting tree for each code."),
    )

  // 30 RURAL TERRAIN & SURFACE TIRE PSI PROFILES
  val TERRAIN_PROFILES: List<TerrainPsiProfile> =
    listOf(
      TerrainPsiProfile("tp01", "Smooth County Asphalt", "Pavement", 1.05, 42, 0.012, "Max range & crisp turn-in; watch tar snakes in summer heat."),
      TerrainPsiProfile("tp02", "Chipseal Backroad", "Pavement", 0.96, 36, 0.016, "Drop 2-3 PSI to tame high-frequency handlebar buzz."),
      TerrainPsiProfile("tp03", "Frost-Heaved Rural Road", "Pavement", 0.92, 32, 0.018, "Keep knees bent; watch sudden transverse cracks in shade."),
      TerrainPsiProfile("tp04", "Wet Rainy Tarmac", "Pavement", 0.94, 24, 0.015, "Avoid painted white lines and oil streaks at intersections."),
      TerrainPsiProfile("tp05", "Fresh Crushed Limestone", "Gravel", 0.84, 18, 0.028, "Keep weight neutral; avoid sudden front brake grabs."),
      TerrainPsiProfile("tp06", "Washboard Gravel", "Gravel", 0.80, 20, 0.032, "Light grip on bars; ride smooth crown or packed tire track."),
      TerrainPsiProfile("tp07", "Hardpack County Gravel", "Gravel", 0.88, 26, 0.022, "Fast rural cruising; scan ahead for loose marble corners."),
      TerrainPsiProfile("tp08", "Deep Pea Gravel", "Gravel", 0.76, 14, 0.042, "Weight slightly rearward; keep steady gentle throttle."),
      TerrainPsiProfile("tp09", "Rail-Trail Crushed Cinder", "Gravel", 0.90, 28, 0.020, "Ideal efficiency surface; yield kindly to horses & hikers."),
      TerrainPsiProfile("tp10", "Gravel Shoulder Drop-Off", "Gravel", 0.85, 15, 0.030, "Never yank bars back up onto asphalt — slow down first."),
      TerrainPsiProfile("tp11", "Dry Hardpack Dirt Trail", "Dirt & Trail", 0.86, 28, 0.024, "Great bite for hybrid & knobby tires; watch hidden roots."),
      TerrainPsiProfile("tp12", "Red Clay Mud (Slick)", "Dirt & Trail", 0.72, 12, 0.052, "Treat like ice — upright chassis, zero front brake, slow roll."),
      TerrainPsiProfile("tp13", "Deep Tractor Ruts", "Dirt & Trail", 0.78, 16, 0.038, "Commit to high center ridge or bottom track; never cross shallow."),
      TerrainPsiProfile("tp14", "Pine Needle Singletrack", "Dirt & Trail", 0.82, 20, 0.029, "Slippery when damp; look 30 feet ahead through turns."),
      TerrainPsiProfile("tp15", "Wet Oak Leaves", "Dirt & Trail", 0.85, 16, 0.026, "Zero sudden lean; hidden sticks & rocks lurk underneath."),
      TerrainPsiProfile("tp16", "Pasture Grass (Dry)", "Dirt & Trail", 0.82, 22, 0.034, "Watch gopher holes and hidden barbed-wire remnants."),
      TerrainPsiProfile("tp17", "Dewy Morning Grass", "Dirt & Trail", 0.76, 14, 0.036, "Surprising low friction — brake 2x earlier than dry dirt."),
      TerrainPsiProfile("tp18", "Sand Wash / Riverbed", "Sand & Water", 0.68, 14, 0.058, "Shift weight back, keep front light, steady momentum."),
      TerrainPsiProfile("tp19", "Shallow Creek Cobbles", "Sand & Water", 0.80, 10, 0.040, "Mossy river rock is ultra-slick; straight line, power steady."),
      TerrainPsiProfile("tp20", "Low-Water Concrete Ford", "Sand & Water", 0.84, 10, 0.024, "Algae film is slipperier than ice — stay dead straight."),
      TerrainPsiProfile("tp21", "Mud Puddle (Unknown Depth)", "Sand & Water", 0.78, 14, 0.044, "Skirt edge or probe depth; hidden holes drown hub seals."),
      TerrainPsiProfile("tp22", "Rocky Appalachian Climb", "Mountain", 0.76, 18, 0.045, "Stand over deck/pegs; use low gear/eco torque curve."),
      TerrainPsiProfile("tp23", "Loose Shale Switchback", "Mountain", 0.74, 15, 0.048, "Rear brake modulates drift; keep inside foot ready."),
      TerrainPsiProfile("tp24", "Forest Service Fire Road", "Mountain", 0.86, 24, 0.025, "Watch oncoming trucks on blind crests; stay strictly right."),
      TerrainPsiProfile("tp25", "Wooden Covered Bridge Deck", "Rural Special", 0.92, 14, 0.017, "Wet wood planks & gaps trap narrow tires — ride straight."),
      TerrainPsiProfile("tp26", "Steel Grate Rural Bridge", "Rural Special", 0.90, 12, 0.020, "Relax grip; let wheel track naturally without fighting bars."),
      TerrainPsiProfile("tp27", "Railroad Track Crossing", "Rural Special", 0.90, 15, 0.018, "Always cross steel rails as close to 90 degrees as possible."),
      TerrainPsiProfile("tp28", "Cattle Guard Grid", "Rural Special", 0.88, 10, 0.022, "Cross dead straight at steady moderate speed or walk."),
      TerrainPsiProfile("tp29", "Packed Winter Snow", "Winter", 0.70, 12, 0.050, "Soft regen only; pre-warm battery indoors before roll-out."),
      TerrainPsiProfile("tp30", "Black Ice / Shaded Creek Bridge", "Winter", 0.75, 5, 0.025, "Do not brake or steer suddenly; coast straight across."),
    )

  // 30 OFF-GRID BARN & SOLAR CHARGING RECIPES
  val BARN_SOLAR_RECIPES: List<BarnSolarRecipe> =
    listOf(
      BarnSolarRecipe("bs01", "Standard 48V 2A Overnight Barn Trickle", 110, 15, "Gentle on cells; safe on any rural extension cord (12AWG)."),
      BarnSolarRecipe("bs02", "48V 5A Fast Barn Turnaround", 275, 15, "Recharges 1000Wh pack in ~3.5h with zero breaker stress."),
      BarnSolarRecipe("bs03", "60V 5A Dual-Motor Shop Charge", 340, 15, "Ideal daily charge rate; ~2.8A wall draw."),
      BarnSolarRecipe("bs04", "72V 5A Standard Garage Charger", 420, 15, "Draws ~3.5A at 120V; safe to run 3 rigs on one 15A circuit."),
      BarnSolarRecipe("bs05", "72V 10A Rapid Convoy Pit-Stop", 840, 15, "Draws ~7.0A; max 1 charger per 15A outlet with barn lights."),
      BarnSolarRecipe("bs06", "72V 15A Hyper-Charge (20A Shop Circuit)", 1260, 20, "Draws 10.5A; requires dedicated 20A barn receptacle."),
      BarnSolarRecipe("bs07", "84V/100V EUC 8A High-Speed Charge", 800, 15, "Use 12AWG heavy outdoor cord only; inspect plug warmth."),
      BarnSolarRecipe("bs08", "134V Master/Lynx EUC 10A Fast Fill", 1340, 20, "Needs 20A shop circuit or 240V split-phase adapter."),
      BarnSolarRecipe("bs09", "Dual-Charger Split (2x 60V 5A)", 680, 15, "5.7A total; safe continuous load on 15A barn branch."),
      BarnSolarRecipe("bs10", "3-Rider Rural Convoy 15A Limit", 1260, 15, "Max 420W x 3 = 10.5A (<12A 80% rule). Safe!"),
      BarnSolarRecipe("bs11", "4-Rider Group Stop on 20A Barn Outlet", 1680, 20, "14.0A total; keep within 16A continuous rating."),
      BarnSolarRecipe("bs12", "RV Park 30A TT-30 Pedestal (3x Fast)", 2520, 30, "TT-30 to triple 15A splitter powers 3 rapid chargers easily."),
      BarnSolarRecipe("bs13", "RV Park 50A NEMA 14-50 Convoy Hub", 4800, 50, "Dual 120V legs support 6+ fast chargers simultaneously."),
      BarnSolarRecipe("bs14", "J1772 Public EV Station Adapter (240V)", 1600, 20, "Requires 100-240V auto-switching PEV charger! Verify label."),
      BarnSolarRecipe("bs15", "200W Portable Folding Solar + 1kWh Station", 160, 15, "Adds ~800Wh/day in clear rural sun — great camp top-off."),
      BarnSolarRecipe("bs16", "400W Barn Roof Solar Off-Grid Rig", 320, 15, "Refills a 1.5kWh scooter pack cleanly in one sunny afternoon."),
      BarnSolarRecipe("bs17", "800W Homestead Solar Shed Array", 650, 15, "Charges two 60V rigs off-grid without touching utility power."),
      BarnSolarRecipe("bs18", "1200W Solar + 2kWh LiFePO4 Bank", 980, 20, "Full rural off-grid PEV fleet charging day or night."),
      BarnSolarRecipe("bs19", "2000W Pure-Sine Inverter Generator", 1500, 15, "Pure sine ONLY — modified-sine generators fry PEV chargers."),
      BarnSolarRecipe("bs20", "Truck Bed 400W AC Outlet Top-Off", 320, 15, "Keep charger <= 320W to avoid tripping pickup bed inverter."),
      BarnSolarRecipe("bs21", "80% Longevity Daily Barn Charge", 300, 15, "Doubles lithium cycle life (1000+ cycles) vs 100% daily."),
      BarnSolarRecipe("bs22", "50% Winter Barn Storage Prep", 150, 15, "Store at 3.80V/cell (49.4V for 48V, 60.8V for 60V, 76.0V for 72V)."),
      BarnSolarRecipe("bs23", "Monthly 100% Cell-Balance Cycle", 220, 15, "Leave plugged 2h past green once/month to equalize groups."),
      BarnSolarRecipe("bs24", "Cold Barn Pre-Warm Protocol (<38°F)", 0, 15, "NEVER charge lithium below 32°F; bring pack indoors 2h first."),
      BarnSolarRecipe("bs25", "Post-Ride Thermal Cooldown Wait", 0, 15, "Wait 25-35 min after steep hills so cell core drops <40°C."),
      BarnSolarRecipe("bs26", "12AWG vs 16AWG Extension Cord Rule", 900, 15, "50ft+ barn cords MUST be 12AWG to prevent voltage drop heat."),
      BarnSolarRecipe("bs27", "Fire-Safe Metal Barn Charging Pad", 500, 15, "Charge on concrete/metal bench away from hay, fuel, & wood."),
      BarnSolarRecipe("bs28", "Smart Timer Shutoff at 85% SoC", 400, 15, "Set timer = (Wh Needed / Charger Watts) for hands-free 85%."),
      BarnSolarRecipe("bs29", "Dual-Pack Simultaneous Barn Charge", 700, 15, "Keep chargers spaced 6\" apart for cooling fan airflow."),
      BarnSolarRecipe("bs30", "Rural Storm Surge Protector Rule", 500, 15, "Unplug chargers during country lightning storms to save BMS."),
    )

  // 30 RURAL HAZARD & TRAIL DISPATCH PRESETS
  val RURAL_HAZARD_PRESETS: List<RuralHazardPreset> =
    listOf(
      RuralHazardPreset("hz01", "Slow Tractor / Combine", "Farm & Road", "CLOSURE", "Wide slow-moving farm machinery occupying rural lane ahead."),
      RuralHazardPreset("hz02", "Hay Wagon Blind Turn", "Farm & Road", "CLOSURE", "Wide hay trailer rounding blind county curve — slow & hold right."),
      RuralHazardPreset("hz03", "Fresh Oil & Chip Road", "Farm & Road", "GRAVEL", "Loose fresh tar & chipseal gravel — slippery corners."),
      RuralHazardPreset("hz04", "Unlit One-Lane Bridge", "Farm & Road", "CLOSURE", "Narrow single-lane country bridge — yield & activate headlight."),
      RuralHazardPreset("hz05", "Log Truck / Timber Hauler", "Farm & Road", "CLOSURE", "Heavy logging truck traffic on county gravel road."),
      RuralHazardPreset("hz06", "Soft Muddy Road Shoulder", "Farm & Road", "GRAVEL", "Steep soft shoulder drop-off along narrow blacktop."),
      RuralHazardPreset("hz07", "County Grader Working", "Farm & Road", "GRAVEL", "Road grader active — fresh deep windrow ridge in center."),
      RuralHazardPreset("hz08", "Railroad Crossing Rough", "Farm & Road", "POTHOLE", "Rough recessed rail tracks — cross at 90 degrees."),
      RuralHazardPreset("hz09", "County Patrol Speed Trap", "Farm & Road", "POLICE", "Local sheriff / county patrol monitoring road speed ahead."),
      RuralHazardPreset("hz10", "School Bus Rural Stop", "Farm & Road", "CLOSURE", "Active rural school bus stop on blind hill crest."),
      RuralHazardPreset("hz11", "Washout / Culvert Collapse", "Water & Surface", "POTHOLE", "Deep rain washout across gravel trail — slow immediately."),
      RuralHazardPreset("hz12", "High Creek Low-Water Ford", "Water & Surface", "FLOOD", "Water flowing over low-water crossing — verify depth before crossing."),
      RuralHazardPreset("hz13", "Deep Clay Mud Bog", "Water & Surface", "FLOOD", "Slick deep farm-track mud bog — walk or detour."),
      RuralHazardPreset("hz14", "Washboard Gravel Ruts", "Water & Surface", "GRAVEL", "Severe chatter washboard bumps — lower speed & loosen grip."),
      RuralHazardPreset("hz15", "Downed Oak / Storm Limb", "Water & Surface", "CLOSURE", "Fallen tree blocking trail/backroad after windstorm."),
      RuralHazardPreset("hz16", "Standing Flash Flood Water", "Water & Surface", "FLOOD", "Flooded dip across rural lane — depth unknown."),
      RuralHazardPreset("hz17", "Deep Pothole Cluster", "Water & Surface", "POTHOLE", "Sharp rim-bending potholes in shaded tarmac section."),
      RuralHazardPreset("hz18", "Loose Pea Gravel Drift", "Water & Surface", "GRAVEL", "Deep loose gravel washed across paved corner apex."),
      RuralHazardPreset("hz19", "Wet Mossy Bridge Planks", "Water & Surface", "FLOOD", "Slick wooden bridge deck — stay straight, no front brake."),
      RuralHazardPreset("hz20", "Black Ice Shaded Hollow", "Water & Surface", "FLOOD", "Freezing shaded valley road — extreme slip hazard."),
      RuralHazardPreset("hz21", "Loose Farm Dog Chasing", "Wildlife & Dusk", "DOG", "Unleashed farm dog patrolling driveway — slow calmly & pass wide."),
      RuralHazardPreset("hz22", "Dusk Deer Crossing Herd", "Wildlife & Dusk", "DOG", "Active deer crossing at treeline — drop speed at dawn/dusk."),
      RuralHazardPreset("hz23", "Horseback Riders Ahead", "Wildlife & Dusk", "DOG", "Equestrians on trail — slow to walk & speak calmly so horses don't spook."),
      RuralHazardPreset("hz24", "Cattle / Livestock on Road", "Wildlife & Dusk", "DOG", "Free-range cows or escaped livestock on rural road."),
      RuralHazardPreset("hz25", "Wild Turkey Flock", "Wildlife & Dusk", "DOG", "Turkeys crossing gravel road — slow gently."),
      RuralHazardPreset("hz26", "Dense Valley Fog Bank", "Wildlife & Dusk", "CLOSURE", "Low visibility fog in creek hollow — amber light & strobe ON."),
      RuralHazardPreset("hz27", "Sun Glare Blind Crest", "Wildlife & Dusk", "CLOSURE", "Direct low sun blinding oncoming drivers on hilltop."),
      RuralHazardPreset("hz28", "Hunting Season Trailhead", "Wildlife & Dusk", "CLOSURE", "Wear hi-viz orange/lime vest near woods trails."),
      RuralHazardPreset("hz29", "Zero-Cell Dead Zone Starts", "Wildlife & Dusk", "CLOSURE", "Entering rural cellular dead zone — offline bailout ready."),
      RuralHazardPreset("hz30", "ATV / Side-by-Side Traffic", "Wildlife & Dusk", "CLOSURE", "Fast UTV/ATV traffic on shared rural forest trail."),
    )

  // 35 CURATED RURAL PEV CHANNEL SESSIONS
  val CHANNEL_SESSIONS: List<ChannelRuralSession> =
    listOf(
      ChannelRuralSession("ch01", "Gravel Cruise Country Acoustic", "Riding Radio", "country acoustic road trip riding playlist", true),
      ChannelRuralSession("ch02", "Dusk Porch Americana Mix", "Riding Radio", "americana folk sunset cruising mix", true),
      ChannelRuralSession("ch03", "Barn Wrenching Classic Rock", "Riding Radio", "classic rock garage workshop playlist", true),
      ChannelRuralSession("ch04", "Hill-Climb High-Voltage EDM", "Riding Radio", "high energy electronic cycling trail mix", true),
      ChannelRuralSession("ch05", "Night Trail Synthwave Drive", "Riding Radio", "synthwave night drive cyberpunk mix", true),
      ChannelRuralSession("ch06", "Appalachian Bluegrass Trail", "Riding Radio", "instrumental bluegrass mountain trail music", true),
      ChannelRuralSession("ch07", "Ozark Backroad Southern Rock", "Riding Radio", "southern rock highway cruise playlist", true),
      ChannelRuralSession("ch08", "Dawn Coffee Chill Lo-Fi", "Riding Radio", "chill lofi morning ride beats", true),
      ChannelRuralSession("ch09", "Desert Overland Ambient", "Riding Radio", "ambient desert road trip instrumental", true),
      ChannelRuralSession("ch10", "Hydraulic Brake Bleed Lab", "Barn Repair", "electric scooter ebike hydraulic brake bleed tutorial", false),
      ChannelRuralSession("ch11", "Tubeless PEV Tire Bead Seat", "Barn Repair", "electric scooter tubeless tire change bead seating guide", false),
      ChannelRuralSession("ch12", "Hub Motor Bearing Replacement", "Barn Repair", "electric scooter hub motor bearing replacement tutorial", false),
      ChannelRuralSession("ch13", "Hall Sensor Multimeter Test", "Barn Repair", "ebike scooter hall sensor multimeter test diagnosis", false),
      ChannelRuralSession("ch14", "BMS Cell Balancing & Repair", "Barn Repair", "lithium battery pack bms cell balance test tutorial", false),
      ChannelRuralSession("ch15", "Waterproofing Deck & Seals", "Barn Repair", "electric scooter waterproofing silicone ip67 guide", false),
      ChannelRuralSession("ch16", "Sur-Ron Chain & Belt Maintenance", "Barn Repair", "sur ron talaria chain belt tension lube tutorial", false),
      ChannelRuralSession("ch17", "Folding Stem Wobble Fix", "Barn Repair", "electric scooter stem wobble creak repair guide", false),
      ChannelRuralSession("ch18", "Rotor Truing & Pad Bedding", "Barn Repair", "disc brake rotor truing squeak fix tutorial", false),
      ChannelRuralSession("ch19", "VESC Motor & FOC Tuning", "VESC & Tech", "vesc foc motor detection tuning pev scooter euc", false),
      ChannelRuralSession("ch20", "72V Controller & Phase Upgrade", "VESC & Tech", "72v controller upgrade mt60 phase wire electric scooter", false),
      ChannelRuralSession("ch21", "Smart BMS App Setup Guide", "VESC & Tech", "ant jk smart bms settings lithium ion configuration", false),
      ChannelRuralSession("ch22", "Off-Grid Solar PEV Charging", "VESC & Tech", "solar panel portable power station charge ebike scooter", false),
      ChannelRuralSession("ch23", "Steering Damper Installation", "VESC & Tech", "electric scooter steering damper installation setup", false),
      ChannelRuralSession("ch24", "12V Buck Converter Headlight Mod", "VESC & Tech", "electric scooter 12v buck converter motorcycle headlight wiring", false),
      ChannelRuralSession("ch25", "Battery Voltage Sag Explained", "VESC & Tech", "lithium battery voltage sag explained ebike scooter", false),
      ChannelRuralSession("ch26", "Rural Gravel PEV Adventure", "Trail Rides", "electric scooter off road gravel trail adventure ride", false),
      ChannelRuralSession("ch27", "EUC Backcountry Trail Carving", "Trail Rides", "electric unicycle euc off road mountain trail ride", false),
      ChannelRuralSession("ch28", "Sur-Ron / Talaria Woods Singletrack", "Trail Rides", "sur ron talaria woods trail singletrack pov", false),
      ChannelRuralSession("ch29", "Long-Range 100-Mile PEV Tour", "Trail Rides", "long range electric scooter ebike 100 mile tour", false),
      ChannelRuralSession("ch30", "Onewheel Dirt Trail Flow", "Trail Rides", "onewheel gt trail riding off road tips", false),
      ChannelRuralSession("ch31", "Night Trail High-Lumen Ride", "Trail Rides", "night trail electric bike scooter headlight test", false),
      ChannelRuralSession("ch32", "High-Speed Scooter Stability Tips", "Trail Rides", "high speed electric scooter riding stance braking tips", false),
      ChannelRuralSession("ch33", "Rural E-Bike Commuter Builds", "Trail Rides", "rural off road electric bike commuter build review", false),
      ChannelRuralSession("ch34", "PEV Protective Gear & Helmets", "Trail Rides", "full face helmet armor gear guide electric scooter euc", false),
      ChannelRuralSession("ch35", "Winter Cold-Weather PEV Care", "Trail Rides", "winter cold weather ebike scooter battery riding tips", false),
    )

  // 35 RURAL PORCH BOARD COMMUNITY TEMPLATES
  val BOARD_TEMPLATES: List<BoardRuralTemplate> =
    listOf(
      BoardRuralTemplate("bt01", "County Gravel Convoy", "Convoy", "GROUP_RIDES", "Saturday Gravel Convoy — Meet at County Trailhead", "Pace: 18-24 MPH • Distance: 25 mi round trip • Charger stop halfway (15A/30A). Bring spare tube, sealant & headlight!"),
      BoardRuralTemplate("bt02", "Sunset Backroad Cruise", "Convoy", "GROUP_RIDES", "Golden Hour Rural Loop — All PEV Classes Welcome", "Easy 15-mile scenic blacktop & rail-trail cruise before dusk. Hi-viz vest & tail strobe recommended."),
      BoardRuralTemplate("bt03", "Hill-Climb Challenge Run", "Convoy", "GROUP_RIDES", "Ridge Climb Torque Run — 60V/72V+ Rigs", "Testing climb efficiency and motor temps up the ridge road. Bring 5A+ charger for summit recharge."),
      BoardRuralTemplate("bt04", "Sunday Coffee & Volts", "Convoy", "EVENTS", "Porch Coffee & Volts Meetup — Bring Your Rig", "Casual Sunday morning meet at the general store. Compare tire setups, check PSI, and swap barn mods."),
      BoardRuralTemplate("bt05", "Beginner PEV Coaching", "Convoy", "GROUP_RIDES", "New Rider Gravel & Braking Clinic", "Friendly zero-pressure session covering loose gravel turns, progressive braking, and battery voltage basics."),
      BoardRuralTemplate("bt06", "Night Trail Beam Test", "Convoy", "EVENTS", "Dusk-to-Night Beam & Fog Light Ride", "Testing handlebar flood + helmet spot beam aiming on unlit backroads."),
      BoardRuralTemplate("bt07", "Rail-Trail 40-Mile Tour", "Convoy", "GROUP_RIDES", "40-Mile Crushed Cinder Rail-Trail Expedition", "Planned RV park 30A pedestal top-off at mile 20. Pack rain shell & water."),
      BoardRuralTemplate("bt08", "Rig For Sale (Spec Card)", "Swap Meet", "MARKETPLACE", "For Sale: [Model / Voltage / Ah] — Clean Barn-Kept Rig", "Pack Voltage / Health: 100% balanced • Odometer: ___ mi • Upgrades: hydraulic brakes, PMT/knobby tires • Includes charger & spare parts."),
      BoardRuralTemplate("bt09", "Spare Tires & Tubes Swap", "Swap Meet", "MARKETPLACE", "Barn Swap: Brand-New Off-Road PEV Tires / Tubes", "Size: ___ (e.g. 11\" 90/65-6.5) • Condition: New/Like-New • Open to cash or trade for brake pads / 60V charger."),
      BoardRuralTemplate("bt10", "Fast Charger Available", "Swap Meet", "MARKETPLACE", "For Sale: Variable 5A-10A Smart Charger (48V/60V/72V)", "Connector: GX16 / XT60 • Adjustable 80%/90%/100% cutoff. Tested on multimeter."),
      BoardRuralTemplate("bt11", "Protective Gear & Helmet", "Swap Meet", "MARKETPLACE", "Swap Meet: Full-Face MIPS Helmet & Armored Jacket", "Size: ___ • Never crashed • Clean liner. Ready for local pickup."),
      BoardRuralTemplate("bt12", "Wanted: Used PEV Parts", "Swap Meet", "MARKETPLACE", "WTB: Controller / Shock / Brake Rotor for Barn Project", "Looking for clean local parts before ordering online. Let me know what's sitting on your workbench!"),
      BoardRuralTemplate("bt13", "Barn Tool Loan Offer", "Swap Meet", "CREW", "Barn Bench Open: Torque Wrench, Bleed Kit & Multimeter", "Happy to help local rural riders bleed brakes, seat tubeless tires, or check pack balance this weekend."),
      BoardRuralTemplate("bt14", "Controller & VESC Parts", "Swap Meet", "MARKETPLACE", "Spare VESC / Buck Converter / XT90 Harness Parts", "Extra project parts ready for a local builder."),
      BoardRuralTemplate("bt15", "Creek Crossing Depth Report", "Road Pulse", "ALERTS", "Creek Ford Water Report — [Creek / Road Name]", "Water depth right now is approx ___ inches after last night's rain. Surface traction: slick/clear."),
      BoardRuralTemplate("bt16", "Fresh Gravel / Grader Alert", "Road Pulse", "ALERTS", "Road Pulse: Fresh Loose Limestone on [County Rd]", "County grader just laid deep loose rock — drop tire PSI 4 lbs and take corners wide."),
      BoardRuralTemplate("bt17", "Downed Tree / Storm Debris", "Road Pulse", "ALERTS", "Trail Blocked: Fallen Timber on [Trail Name]", "Large oak limb down across trail near mile marker ___. Passable on foot only right now."),
      BoardRuralTemplate("bt18", "Farm Harvest Equipment", "Road Pulse", "ALERTS", "Harvest Traffic Active on [County Highway]", "Combines and grain trucks running heavy today — stay visible and give wide room."),
      BoardRuralTemplate("bt19", "Trail Clean-Up Workday", "Road Pulse", "EVENTS", "Rural Trail Clean-Up & Branch Clearing Crew", "Bringing hand saws, trash bags, and cold water to keep our local rail-trail spotless for everyone."),
      BoardRuralTemplate("bt20", "Wildlife / Deer Warning", "Road Pulse", "ALERTS", "Heavy Dusk Deer Activity Near [Hollow / Bridge]", "Spotted 6 deer crossing right at twilight. Dial back speed and run bright beams."),
      BoardRuralTemplate("bt21", "Mud & Traction Conditions", "Road Pulse", "RIDE_REPORTS", "Trail Surface Report: Dry / Tacky / Muddy", "Just rode 12 miles out — upper ridge is dry hardpack, creek bottom still has slick clay."),
      BoardRuralTemplate("bt22", "RV Park 30A/50A Pedestal", "Chargers", "CHARGERS", "Verified Rural Charger Stop: [Park / Store Name]", "30A TT-30 & standard 15A GFCI outlets available. Friendly owner — grab a snack inside to say thanks!"),
      BoardRuralTemplate("bt23", "General Store Porch Outlet", "Chargers", "CHARGERS", "PEV-Friendly Country Store Outlet Verified", "Outdoor 15A receptacle on covered porch. Great spot for a 45-min coffee & top-off."),
      BoardRuralTemplate("bt24", "Public Pavilion 20A Plug", "Chargers", "CHARGERS", "County Park Picnic Pavilion 20A Outlets Working", "Tested live at 121V today — dry covered roof over outlets."),
      BoardRuralTemplate("bt25", "J1772 EV Station Verified", "Chargers", "CHARGERS", "J1772 Station Tested with 240V PEV Adapter", "Charged smoothly at 10A without fault. Easy off-road parking beside stall."),
      BoardRuralTemplate("bt26", "Off-Grid Solar Pit Stop", "Chargers", "CHARGERS", "Mobile Solar Charging Station at Trailhead", "Bringing my 1.2kWh LiFePO4 + 400W solar bank to the trailhead for group ride top-offs."),
      BoardRuralTemplate("bt27", "Homestead Rider Pit-Stop", "Chargers", "CHARGERS", "Emergency Barn 15A/20A Outlet Available for Locals", "DM if you're running low near our county road — happy to let fellow riders top off safely."),
      BoardRuralTemplate("bt28", "Charger Outage Notice", "Chargers", "CHARGERS", "Heads Up: [Location] Outdoor Outlet Currently Tripped", "Breaker is off at the park shelter today — plan your reserve battery accordingly."),
      BoardRuralTemplate("bt29", "Garage Build Spotlight", "Builds", "BUILDS", "Barn Build Spotlight: [Rig Name & Voltage]", "Battery: ___V ___Ah • Motor: ___W • Brakes: 4-piston hydraulic • Best rural mod so far: ___."),
      BoardRuralTemplate("bt30", "1,000-Mile Rural Review", "Builds", "REVIEWS", "1,000-Mile Country Gravel Long-Term Review", "Real-world range, voltage sag on hills, bearing durability, and every bolt that needed Loctite."),
      BoardRuralTemplate("bt31", "Winter Barn Storage Check", "Builds", "GARAGE", "Winter Storage Voltage & Seal Check Complete", "Balanced all rigs to 50% storage voltage, sprayed silicone on seals, and pumped tires off concrete."),
      BoardRuralTemplate("bt32", "Custom 12V Lighting Rig", "Builds", "BUILDS", "Wired Dedicated 12V Buck + Amber Fog Lights", "Zero drain imbalance on main pack — crisp cutoff beam for pitch-black county roads."),
      BoardRuralTemplate("bt33", "Emergency Stolen Rig BOLO", "Builds", "STOLEN", "STOLEN PEV BOLO — [Model / Color / Distinguishing Marks]", "Last seen: [Location & Time] • Serial / Motor Stamp: ___ • Distinguishing mods: ___. Please DM or call local sheriff."),
      BoardRuralTemplate("bt34", "Roadside Breakdown SOS", "Builds", "SOS", "Need a Barn Assist — Flat / Cutoff Near [Road]", "Location shared • Rig: ___ • Tool needed: ___ (18mm socket / tube / truck lift). Appreciate any nearby riders!"),
      BoardRuralTemplate("bt35", "Video Request for Channel", "Builds", "VIDEO_REQUEST", "Channel Video Request: [Topic / Rig Test]", "Would love to see a real rural backroad test or barn repair walkthrough covering this!"),
    )
}
