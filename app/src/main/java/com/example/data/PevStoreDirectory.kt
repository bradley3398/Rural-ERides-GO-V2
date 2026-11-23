package com.example.data

import java.net.URI
import java.net.URLEncoder

data class PevStoreEntry(
  val id: String,
  val name: String,
  val category: String, // MULTI, SCOOTER, EBIKE, EUC, ONEWHEEL, EMOTO, ESKATE, PARTS_DIY, MARKETPLACE
  val region: String,
  val specialty: String,
  val homeUrl: String,
  val searchTemplate: String, // Use {q} for encoded search query
  val isShopify: Boolean = false,
) {
  fun buildSearchUrl(query: String): String {
    val clean = query.trim()
    if (clean.isBlank()) return homeUrl
    val enc = URLEncoder.encode(clean, "UTF-8")
    return searchTemplate.replace("{q}", enc)
  }

  fun hostDomain(): String =
    runCatching { URI(homeUrl).host?.lowercase()?.removePrefix("www.").orEmpty() }.getOrDefault("")
}

object PevStoreDirectory {
  val STORE_CATEGORIES =
    listOf("ALL", "MULTI", "SCOOTER", "EBIKE", "EUC", "ONEWHEEL", "EMOTO", "ESKATE", "PARTS_DIY", "MARKETPLACE")

  val KNOWN_PEV_STORES: List<PevStoreEntry> =
    listOf(
      // --- TOP MAJOR RETAILERS & GLOBAL MARKETPLACES (WITH BRAND LOGOS) ---
      PevStoreEntry("amazon", "Amazon", "MARKETPLACE", "Global", "Fast shipping tires, brakes, helmets, chargers & PEV gear", "https://www.amazon.com", "https://www.amazon.com/s?k={q}"),
      PevStoreEntry("ebay", "eBay", "MARKETPLACE", "Global", "OEM takeoffs, used PEVs, rare rotors, controllers & hubs", "https://www.ebay.com", "https://www.ebay.com/sch/i.html?_nkw={q}"),
      PevStoreEntry("walmart", "Walmart", "MARKETPLACE", "US/Global", "eBikes, eScooters, tubes, slime, helmets, racks & tools", "https://www.walmart.com", "https://www.walmart.com/search?q={q}"),
      PevStoreEntry("aliexpress", "AliExpress", "MARKETPLACE", "Global", "Direct factory controllers, BMS, motors, frames & tires", "https://www.aliexpress.com", "https://www.aliexpress.com/wholesale?SearchText={q}"),
      PevStoreEntry("bestbuy", "Best Buy", "MARKETPLACE", "US", "Segway, NIU, Aventon, Super73, chargers & cameras", "https://www.bestbuy.com", "https://www.bestbuy.com/site/searchpage.jsp?st={q}"),
      PevStoreEntry("target", "Target", "MARKETPLACE", "US", "Commuter eScooters, locks, lights, pumps & safety gear", "https://www.target.com", "https://www.target.com/s?searchTerm={q}"),
      PevStoreEntry("homedepot", "Home Depot", "MARKETPLACE", "US", "Metric fasteners, torque wrenches, Loctite & shop tools", "https://www.homedepot.com", "https://www.homedepot.com/s/{q}"),
      PevStoreEntry("google_shop", "Google Shopping", "MARKETPLACE", "Global", "Compare live prices across every store on the web", "https://shopping.google.com", "https://www.google.com/search?tbm=shop&q={q}"),
      PevStoreEntry("rei", "REI Co-op", "EBIKE", "US", "eBikes, hydraulic brakes, pumps, racks & trail helmets", "https://www.rei.com", "https://www.rei.com/search?q={q}"),
      PevStoreEntry("revzilla", "RevZilla", "EMOTO", "US/Global", "ECE/DOT full-face helmets, armored jackets, gloves & moto tires", "https://www.revzilla.com", "https://www.revzilla.com/search?query={q}"),
      PevStoreEntry("cyclegear", "Cycle Gear", "EMOTO", "US", "Moto armor, dirt bike tires, chain lube & helmet comms", "https://www.cyclegear.com", "https://www.cyclegear.com/search?query={q}"),
      PevStoreEntry("jensonusa", "Jenson USA", "EBIKE", "US", "MTB hydraulic brakes, 203mm rotors, shocks & Maxxis tires", "https://www.jensonusa.com", "https://www.jensonusa.com/search?q={q}"),
      PevStoreEntry("harborfreight", "Harbor Freight", "PARTS_DIY", "US", "Torque wrenches, multimeters, hex kits & tire spoons", "https://www.harborfreight.com", "https://www.harborfreight.com/search?q={q}"),

      // --- MULTI-PEV & MEGA DISTRIBUTORS ---
      PevStoreEntry("voro", "Voro Motors", "MULTI", "US/Global", "Kaabo, EMOVE, InMotion, Sur-Ron & Parts", "https://www.voromotors.com", "https://www.voromotors.com/search?q={q}", isShopify = true),
      PevStoreEntry("fluid", "FluidFreeRide", "SCOOTER", "US", "NAMI, Kaabo, Mantis, Wolf King & Parts", "https://fluidfreeride.com", "https://fluidfreeride.com/search?q={q}", isShopify = true),
      PevStoreEntry("alienrides", "Alien Rides", "MULTI", "US", "Begode, LeaperKim, VSETT, NAMI, Talaria", "https://alienrides.com", "https://alienrides.com/search?q={q}", isShopify = true),
      PevStoreEntry("revrides", "Rev Rides", "MULTI", "US", "VSETT, Segway, Sur-Ron, E-Ride Pro, Parts", "https://revrides.com", "https://revrides.com/search?q={q}", isShopify = true),
      PevStoreEntry("wyrdryds", "WyrdRyds", "SCOOTER", "US", "NAMI, Kaabo, VSETT Scooters, Rotors & Parts", "https://wyrdryds.com", "https://wyrdryds.com/search?q={q}", isShopify = true),
      PevStoreEntry("ewheels", "eWheels", "EUC", "US/Global", "LeaperKim, InMotion, Begode, Kingsong EUCs", "https://www.ewheels.com", "https://www.ewheels.com/?s={q}&post_type=product"),
      PevStoreEntry("euco", "EUCO", "EUC", "US", "InMotion, Kingsong, Begode, EUC Armor & Pads", "https://www.euco.us", "https://www.euco.us/search?q={q}", isShopify = true),
      PevStoreEntry("nextgen", "NextGen Mobility", "EUC", "US", "Begode, Extreme Bull, LeaperKim & Parts", "https://nextgenmobility.org", "https://nextgenmobility.org/search?q={q}", isShopify = true),
      PevStoreEntry("minimotors", "Minimotors USA", "SCOOTER", "US", "Dualtron Flagship Scooters & Genuine Parts", "https://minimotorsusa.com", "https://minimotorsusa.com/search?q={q}", isShopify = true),
      PevStoreEntry("apollo", "Apollo Scooters", "SCOOTER", "US/CA", "Apollo Pro, Phantom, City, Go & OEM Parts", "https://apolloscooters.co", "https://apolloscooters.co/search?q={q}", isShopify = true),
      PevStoreEntry("segway", "Segway Store", "SCOOTER", "Global", "GT2, GT1, Max G2, P100S, ZT3 Pro & Parts", "https://store.segway.com", "https://store.segway.com/catalogsearch/result/?q={q}"),
      PevStoreEntry("kaabo", "Kaabo USA", "SCOOTER", "US", "Wolf King GTR, Wolf Warrior, Mantis King GT", "https://www.kaabousa.com", "https://www.kaabousa.com/search?q={q}", isShopify = true),
      PevStoreEntry("roadrunner", "RoadRunner Scooters", "SCOOTER", "US", "RS5 Max, RX7, D4+ & OEM Parts", "https://roadrunnerscooters.com", "https://roadrunnerscooters.com/search?q={q}", isShopify = true),
      PevStoreEntry("nami", "NAMI Electric", "SCOOTER", "Global", "Burn-E 2 Max, Klima, Blast & OEM Parts", "https://namielectric.com", "https://www.google.com/search?q=site:namielectric.com+{q}"),
      PevStoreEntry("teverun", "Teverun", "SCOOTER", "Global", "Fighter Supreme, Blade GT2, Tetra", "https://teverun.com", "https://www.google.com/search?q=Teverun+{q}"),
      PevStoreEntry("ausom", "Ausom Store", "SCOOTER", "US/EU", "Gallop, Leopard, DT2 Pro Dual-Motor", "https://ausomstore.com", "https://ausomstore.com/search?q={q}", isShopify = true),
      PevStoreEntry("kukirin", "KuKirin / Kugoo", "SCOOTER", "Global", "G2 Master, G3 Pro, G4 Max & Spares", "https://www.kugookirinus.com", "https://www.kugookirinus.com/search?q={q}", isShopify = true),
      PevStoreEntry("niu", "NIU Mobility", "SCOOTER", "Global", "KQi3 Max, KQi Air Carbon & Chargers", "https://shop.niu.com", "https://shop.niu.com/search?q={q}", isShopify = true),
      PevStoreEntry("gotrax", "Gotrax", "SCOOTER", "US", "GX3, GX2, GMax Ultra Commuter Scooters", "https://gotrax.com", "https://gotrax.com/search?q={q}", isShopify = true),
      PevStoreEntry("inmotion", "InMotion World", "EUC", "Global", "V14 Adventure, V13 Challenger, RS Scooter", "https://store.inmotionworld.com", "https://store.inmotionworld.com/search?q={q}", isShopify = true),
      PevStoreEntry("oneride", "OneRide EU", "EUC", "EU/Global", "EUCs, Grizzla Pads, Fast Chargers", "https://oneride.eu", "https://oneride.eu/en/search?s={q}"),

      // --- ONEWHEEL & SELF-BALANCING ---
      PevStoreEntry("onewheel", "Future Motion (Onewheel)", "ONEWHEEL", "US/Global", "GT S-Series, GT, Pint X, Pint S & Rails", "https://onewheel.com", "https://onewheel.com/search?q={q}", isShopify = true),
      PevStoreEntry("floatlife", "The Float Life (TFL)", "ONEWHEEL", "US/Global", "Enduro Tires, WTF Rails, Kush Pads, VESC", "https://thefloatlife.com", "https://thefloatlife.com/search?q={q}", isShopify = true),
      PevStoreEntry("craftride", "Craft&Ride", "ONEWHEEL", "US/Global", "Fenders, Burris/Hoosier Tires, Grip, Footpads", "https://craftandride.com", "https://craftandride.com/search?q={q}", isShopify = true),
      PevStoreEntry("floatwheel", "Floatwheel", "ONEWHEEL", "Global", "ADV2 VESC Self-Balancing Boards & CannonCore Parts", "https://floatwheel.co", "https://www.google.com/search?q=site:floatwheel.co+{q}"),

      // --- E-BIKES & UTILITY / TRIKES ---
      PevStoreEntry("lectric", "Lectric eBikes", "EBIKE", "US", "XP 3.0, XPeak, ONE, XPedition Cargo, XP Trike", "https://lectricebikes.com", "https://lectricebikes.com/search?q={q}", isShopify = true),
      PevStoreEntry("aventon", "Aventon", "EBIKE", "US", "Aventure.2, Ramblas, Level.2, Abound", "https://www.aventon.com", "https://www.aventon.com/search?q={q}", isShopify = true),
      PevStoreEntry("radpower", "Rad Power Bikes", "EBIKE", "US/CA", "RadRover, Radster Trail, RadWagon", "https://www.radpowerbikes.com", "https://www.radpowerbikes.com/search?q={q}", isShopify = true),
      PevStoreEntry("juiced", "Juiced Bikes", "EBIKE", "US", "HyperScorpion, Scrambler, RipCurrent 52V eBikes", "https://www.juicedbikes.com", "https://www.juicedbikes.com/search?q={q}", isShopify = true),
      PevStoreEntry("ride1up", "Ride1Up", "EBIKE", "US", "Revv 1, Prodigy V2, LMTD V2, Rift", "https://ride1up.com", "https://ride1up.com/?s={q}&post_type=product"),
      PevStoreEntry("velotric", "Velotric", "EBIKE", "US", "Nomad 1 Plus, Discover 2, Fold 1", "https://www.velotricbike.com", "https://www.velotricbike.com/search?q={q}", isShopify = true),
      PevStoreEntry("super73", "Super73", "EBIKE", "US/EU", "RX Mojave, R-Adventure, S2, Z-Miami", "https://super73.com", "https://super73.com/search?q={q}", isShopify = true),
      PevStoreEntry("arielrider", "Ariel Rider", "EBIKE", "US", "Grizzly Dual-Motor, X-Class 52V, Kepler", "https://arielrider.com", "https://arielrider.com/search?q={q}", isShopify = true),
      PevStoreEntry("engwe", "Engwe Bikes", "EBIKE", "Global", "M20 Dual Battery, Engine Pro 2.0, X26", "https://engwe-bikes.com", "https://engwe-bikes.com/search?q={q}", isShopify = true),
      PevStoreEntry("fiido", "Fiido", "OTHER", "Global", "Q1S, C11, Titan & Seated Cargo eBikes", "https://www.fiido.com", "https://www.fiido.com/search?q={q}", isShopify = true),
      PevStoreEntry("mooncool", "Mooncool", "OTHER", "US", "TK1, Meetup Electric Trikes & Cargo Parts", "https://www.mooncool.com", "https://www.mooncool.com/search?q={q}", isShopify = true),
      PevStoreEntry("specialized", "Specialized Turbo", "EBIKE", "Global", "Turbo Levo, Vado, Globe Haul ST", "https://www.specialized.com", "https://www.specialized.com/us/en/search?q={q}"),
      PevStoreEntry("worldwidecyclery", "Worldwide Cyclery", "EBIKE", "US", "Magura MDR-P Rotors, Galfer, MTB & eMTB Components", "https://www.worldwidecyclery.com", "https://www.worldwidecyclery.com/search?q={q}", isShopify = true),

      // --- E-MOTOS / SUR-RON / TALARIA ---
      PevStoreEntry("lunacycle", "Luna Cycle", "EMOTO", "US", "Sur-Ron, Talaria MX4/MX5, X2.5, Batteries", "https://lunacycle.com", "https://lunacycle.com/search.php?search_query={q}"),
      PevStoreEntry("gritshift", "GritShift", "EMOTO", "US", "Sur-Ron, Talaria, E-Ride Pro Performance Parts", "https://gritshift.com", "https://gritshift.com/search?q={q}", isShopify = true),
      PevStoreEntry("chargedcycle", "Charged Cycle Works", "EMOTO", "US", "EBMX X-9000, KO Moto, Chi Batteries, Wheels", "https://chargedcycleworks.com", "https://chargedcycleworks.com/search?q={q}", isShopify = true),
      PevStoreEntry("eridepro", "E-Ride Pro", "EMOTO", "US", "Pro SS 2.0/3.0, SR 72V Off-Road eMoto", "https://eridepros.com", "https://eridepros.com/?s={q}&post_type=product"),
      PevStoreEntry("warp9", "Warp 9 Racing", "EMOTO", "US", "Sur-Ron / Talaria CNC Sprockets, Chains & Pegs", "https://www.warp9racing.com", "https://www.warp9racing.com/?s={q}&post_type=product"),
      PevStoreEntry("rockymountain", "Rocky Mountain ATV/MC", "EMOTO", "US", "Dunlop MX33, Shinko 241 Moto Tires & Chains", "https://www.rockymountainatvmc.com", "https://www.rockymountainatvmc.com/search?term={q}"),

      // --- E-SKATE ---
      PevStoreEntry("backfire", "Backfire Boards", "ESKATE", "Global", "Zealot X, Ranger X5, Hammer Sledge", "https://www.backfireboards.com", "https://www.backfireboards.com/search?q={q}", isShopify = true),
      PevStoreEntry("meepo", "Meepo Board", "ESKATE", "Global", "Hurricane Ultra, Voyager, V5, Hub Motors", "https://www.meepoboard.com", "https://www.meepoboard.com/search?q={q}", isShopify = true),
      PevStoreEntry("exway", "Exway", "ESKATE", "Global", "Atlas Pro 4WD, Flex Pro, Wave", "https://www.exwayboard.com", "https://www.exwayboard.com/search?q={q}", isShopify = true),
      PevStoreEntry("eboardstuff", "eBoardStuff", "ESKATE", "US", "HTD 5M Kevlar Drive Belts, HyperDrive & Pulleys", "https://www.eboardstuff.com", "https://www.eboardstuff.com/search?q={q}", isShopify = true),

      // --- DIY BATTERIES, CONTROLLERS, TIRES & PARTS ---
      PevStoreEntry("chibattery", "ChiBatterySystems", "PARTS_DIY", "US", "Custom 60V/72V/84V Molicel Packs & Chargers", "https://chibatterysystems.com", "https://chibatterysystems.com/search?q={q}", isShopify = true),
      PevStoreEntry("grin", "Grin Technologies", "PARTS_DIY", "CA/Global", "Cycle Satiator, Phaserunner, Torque Arms", "https://ebikes.ca", "https://ebikes.ca/catalogsearch/result/?q={q}"),
      PevStoreEntry("flipsky", "Flipsky VESC", "PARTS_DIY", "Global", "75100/75200 Pro VESC, Remotes, Motors", "https://flipsky.net", "https://flipsky.net/search?q={q}", isShopify = true),
      PevStoreEntry("spintend", "Spintend Ubox", "PARTS_DIY", "Global", "Ubox 80V/100V Dual VESC Controllers", "https://spintend.com", "https://spintend.com/search?q={q}", isShopify = true),
      PevStoreEntry("tannus", "Tannus America", "PARTS_DIY", "US", "Tannus Armour Fat Tire & Tubeless Inserts", "https://tannusamerica.com", "https://tannusamerica.com/search?q={q}", isShopify = true),
      PevStoreEntry("outbound", "Outbound Lighting", "PARTS_DIY", "US", "Detour & Trail Evo Cutoff Headlights", "https://www.outboundlighting.com", "https://www.outboundlighting.com/search?q={q}", isShopify = true),
      PevStoreEntry("pmt", "PMT Tyres", "PARTS_DIY", "IT/Global", "Italian Radial Stradale/Junior Scooter Tires", "https://www.pmt-tyres.it", "https://www.google.com/search?q=PMT+tyres+{q}"),
    )

  /** Extra PEV Specs appended to PevCatalogData for expanded built-in coverage (65+ models total). */
  val EXTRA_PEV_SPECS: List<PevSpecRecord> =
    listOf(
      PevSpecRecord(
        name = "Wolf King GTR", brand = "Kaabo", category = "SCOOTER", price = "$3,995",
        topSpeed = "65 mph", range = "112 mi", motorPower = "2x 2000W Nominal (13,440W Peak)",
        battery = "2520Wh (72V 35Ah Removable LG/Molicel)", nominalVoltage = 72, weight = "139 lb",
        chargingTime = "7 hrs", efficiency = "36 Wh/mi", brakingSystem = "Hydraulic 4-Piston + ABS + Regen",
        suspensionType = "Motorcycle Inverted Fork + Rear Adjustable", tireProfile = "12\" Hybrid Tubeless",
        ipRating = "IPX5", maxLoad = "330 lb", siteUrl = "https://www.kaabousa.com/products/kaabo-wolf-king-gtr",
      ),
      PevSpecRecord(
        name = "Fighter Supreme 7260R", brand = "Teverun", category = "SCOOTER", price = "$4,399",
        topSpeed = "75 mph", range = "124 mi", motorPower = "2x 2500W (15,000W Peak)",
        battery = "4320Wh (72V 60Ah SK Blade)", nominalVoltage = 72, weight = "128 lb",
        chargingTime = "8 hrs", efficiency = "38 Wh/mi", brakingSystem = "4-Piston Hydraulic 160mm + E-ABS",
        suspensionType = "KKE Hydraulic Adjustable + Dual Damper", tireProfile = "13\" Tubeless Street",
        ipRating = "IPX5", maxLoad = "330 lb", siteUrl = "https://teverun.com/fighter-supreme-7260r/",
      ),
      PevSpecRecord(
        name = "RX7 Hyper", brand = "RoadRunner", category = "SCOOTER", price = "$3,799",
        topSpeed = "70 mph", range = "70 mi", motorPower = "2x 1800W (72V Sinewave)",
        battery = "2304Wh (72V 32Ah Samsung 50E)", nominalVoltage = 72, weight = "102 lb",
        chargingTime = "6 hrs", efficiency = "35 Wh/mi", brakingSystem = "Magura MT5e 4-Piston Hydraulic",
        suspensionType = "KKE Adjustable Hydraulic + Steering Damper", tireProfile = "11\" PMT Stradale Radial",
        ipRating = "IPX5", maxLoad = "330 lb", siteUrl = "https://roadrunnerscooters.com/products/roadrunner-rx7",
      ),
      PevSpecRecord(
        name = "RS Midnight", brand = "InMotion", category = "SCOOTER", price = "$3,999",
        topSpeed = "68 mph", range = "100 mi", motorPower = "2x 2000W (8400W Peak)",
        battery = "2880Wh (72V 40Ah 21700)", nominalVoltage = 72, weight = "128 lb",
        chargingTime = "6.5 hrs", efficiency = "34 Wh/mi", brakingSystem = "Dual Hydraulic 160mm + Regen",
        suspensionType = "4-Height Adjustable C-Type Hydraulic", tireProfile = "11x3.5\" Tubeless Street",
        ipRating = "IPX6 / IPX7 Pack", maxLoad = "330 lb", siteUrl = "https://store.inmotionworld.com/products/inmotion-rs",
      ),
      PevSpecRecord(
        name = "ZT3 Pro", brand = "Segway", category = "SCOOTER", price = "$949",
        topSpeed = "25 mph", range = "43 mi", motorPower = "650W (1600W Peak) Rear Hub",
        battery = "597Wh (48V 12.75Ah)", nominalVoltage = 48, weight = "65 lb",
        chargingTime = "4 hrs", efficiency = "18 Wh/mi", brakingSystem = "Dual Mechanical Disc + E-Brake",
        suspensionType = "Dual Telescopic Front + Rear Spring", tireProfile = "11\" Off-Road Tubeless",
        ipRating = "IPX5", maxLoad = "265 lb", siteUrl = "https://store.segway.com/segway-zt3-pro",
      ),
      PevSpecRecord(
        name = "G2 Master Dual", brand = "KuKirin", category = "SCOOTER", price = "$899",
        topSpeed = "37 mph", range = "43 mi", motorPower = "2x 1000W Dual Brushless",
        battery = "1081Wh (52V 20.8Ah)", nominalVoltage = 52, weight = "72 lb",
        chargingTime = "9 hrs", efficiency = "26 Wh/mi", brakingSystem = "Dual Disc + E-ABS",
        suspensionType = "Hydraulic Spring Fork + Rear", tireProfile = "10\" Off-Road Pneumatic",
        ipRating = "IP54", maxLoad = "265 lb", siteUrl = "https://www.kugookirinus.com/products/kukirin-g2-master",
      ),
      PevSpecRecord(
        name = "Sherman L", brand = "LeaperKim", category = "EUC", price = "$4,199",
        topSpeed = "56 mph", range = "130 mi", motorPower = "3200W Nominal (8000W Peak)",
        battery = "4000Wh (151.2V Samsung 50S)", nominalVoltage = 133, weight = "101 lb",
        chargingTime = "4.5 hrs (20A)", efficiency = "31 Wh/mi", brakingSystem = "High-Torque Regenerative",
        suspensionType = "90mm Fastace Progressive Hydraulic", tireProfile = "20\" Knobby / Street",
        ipRating = "IPX6", maxLoad = "300 lb", siteUrl = "https://www.ewheels.com/product/veteran-sherman-l/",
      ),
      PevSpecRecord(
        name = "Lynx 151V", brand = "LeaperKim", category = "EUC", price = "$3,999",
        topSpeed = "56 mph", range = "95 mi", motorPower = "3200W Nominal (8000W Peak)",
        battery = "2700Wh (151.2V Samsung 50S)", nominalVoltage = 133, weight = "88 lb",
        chargingTime = "3.5 hrs", efficiency = "29 Wh/mi", brakingSystem = "Regen + Smart BMS",
        suspensionType = "90mm Fastace Dual Hydraulic", tireProfile = "20\" Kenda / Street",
        ipRating = "IPX6", maxLoad = "300 lb", siteUrl = "https://www.ewheels.com/product/veteran-lynx-2700wh-50s/",
      ),
      PevSpecRecord(
        name = "V14 Adventure 50S", brand = "InMotion", category = "EUC", price = "$2,999",
        topSpeed = "43 mph", range = "75 mi", motorPower = "4000W Nominal (9000W Peak)",
        battery = "2400Wh (134V Samsung 50S)", nominalVoltage = 115, weight = "86 lb",
        chargingTime = "2.5 hrs (16A)", efficiency = "32 Wh/mi", brakingSystem = "High-Torque Regen",
        suspensionType = "85mm Progressive Coil Linkage", tireProfile = "16x3\" Off-Road Knobby",
        ipRating = "IPX6 / IPX7 Packs", maxLoad = "308 lb", siteUrl = "https://www.ewheels.com/product/inmotion-v14/",
      ),
      PevSpecRecord(
        name = "ET Max 168V", brand = "Begode", category = "EUC", price = "$3,850",
        topSpeed = "62 mph", range = "90 mi", motorPower = "4500W C40 Tile Motor",
        battery = "3000Wh (168V Samsung 50S)", nominalVoltage = 148, weight = "108 lb",
        chargingTime = "3.5 hrs", efficiency = "35 Wh/mi", brakingSystem = "High-Voltage Regen",
        suspensionType = "130mm DS200 Hydraulic", tireProfile = "20\" Motorcycle Street",
        ipRating = "IPX5", maxLoad = "300 lb", siteUrl = "https://alienrides.com/products/begode-et-max",
      ),
      PevSpecRecord(
        name = "Pro SS 3.0 72V", brand = "E-Ride Pro", category = "EMOTO", price = "$4,999",
        topSpeed = "62 mph", range = "64 mi", motorPower = "15.8 kW Peak Mid-Drive",
        battery = "3600Wh (72V 50Ah Swappable)", nominalVoltage = 72, weight = "167 lb",
        chargingTime = "3.5 hrs", efficiency = "55 Wh/mi", brakingSystem = "DOT4 Moto Hydraulic 220mm + Thumb Regen",
        suspensionType = "FastAce 2.0 Moto Fork + Rear Linkage", tireProfile = "19\" Front / 18\" Rear Moto Knobby",
        ipRating = "IP65", maxLoad = "302 lb", siteUrl = "https://eridepros.com/product/pro-ss-3-0/",
      ),
      PevSpecRecord(
        name = "Sting MX5 Pro", brand = "Talaria", category = "EMOTO", price = "$4,700",
        topSpeed = "59 mph", range = "62 mi", motorPower = "13.4 kW Peak Gearbox Mid-Drive",
        battery = "2880Wh (72V 40Ah Samsung 50S)", nominalVoltage = 72, weight = "158 lb",
        chargingTime = "3.0 hrs", efficiency = "48 Wh/mi", brakingSystem = "Moto Hydraulic 220mm 3mm Rotors + 4-Level Regen",
        suspensionType = "Upgraded Talaria Factory Moto Fork + Rear", tireProfile = "19\" Moto Knobby",
        ipRating = "IP65", maxLoad = "300 lb", siteUrl = "https://lunacycle.com/talaria-sting-pro-mx5/",
      ),
      PevSpecRecord(
        name = "Ultra Bee 74V", brand = "Sur-Ron", category = "EMOTO", price = "$6,499",
        topSpeed = "56 mph", range = "87 mi", motorPower = "12.5 kW Peak (440 Nm Wheel Torque)",
        battery = "4070Wh (74V 55Ah)", nominalVoltage = 74, weight = "187 lb",
        chargingTime = "4.0 hrs", efficiency = "52 Wh/mi", brakingSystem = "Full Moto Hydraulic + SRTC Traction Control",
        suspensionType = "240mm Inverted Moto Fork + TR Intersect", tireProfile = "19\" Full Moto Tires",
        ipRating = "IP67 Pack", maxLoad = "300 lb", siteUrl = "https://www.sur-ronusa.com/ultra-bee",
      ),
      PevSpecRecord(
        name = "Stark VARG EX", brand = "Stark Future", category = "EMOTO", price = "$12,900",
        topSpeed = "85 mph", range = "80 mi", motorPower = "80 HP (60 kW Peak) Carbon-Sleeve",
        battery = "7200Wh (360V Magnesium Pack)", nominalVoltage = 360, weight = "264 lb",
        chargingTime = "2.0 hrs", efficiency = "90 Wh/mi", brakingSystem = "Brembo Moto Hydraulic + Adjustable Regen",
        suspensionType = "KYB 300mm Enduro Suspension", tireProfile = "21\" Front / 18\" Rear Enduro",
        ipRating = "IP69K", maxLoad = "330 lb", siteUrl = "https://starkfuture.com/products/stark-varg-ex",
      ),
      PevSpecRecord(
        name = "GT S-Series Rally", brand = "Onewheel", category = "ONEWHEEL", price = "$3,500",
        topSpeed = "25 mph", range = "25 mi", motorPower = "S-Series 113V High-Torque Hub",
        battery = "525Wh (113V Molicel P45B)", nominalVoltage = 113, weight = "34 lb",
        chargingTime = "1.5 hrs (Hypercharger)", efficiency = "22 Wh/mi", brakingSystem = "Self-Balancing Regen",
        suspensionType = "6\" Billet Hub + Recurve Rails", tireProfile = "11.5x6.5-6 Performance Treaded",
        ipRating = "IPX5", maxLoad = "275 lb", siteUrl = "https://onewheel.com/products/onewheel-gt-s-series-rally-edition",
      ),
      PevSpecRecord(
        name = "ADV2 VESC", brand = "Floatwheel", category = "ONEWHEEL", price = "$2,199",
        topSpeed = "28 mph", range = "32 mi", motorPower = "CannonCore 20S VESC Hub",
        battery = "740Wh (84V 20S2P Samsung 50S)", nominalVoltage = 72, weight = "36 lb",
        chargingTime = "1.8 hrs", efficiency = "23 Wh/mi", brakingSystem = "Open-Source VESC Regen",
        suspensionType = "6\" High-Sidewall Cushion", tireProfile = "11.5x6.5-6 Enduro Compatible",
        ipRating = "IPX5", maxLoad = "275 lb", siteUrl = "https://floatwheel.co",
      ),
      PevSpecRecord(
        name = "Grizzly Dual-Motor 52V", brand = "Ariel Rider", category = "EBIKE", price = "$3,299",
        topSpeed = "36 mph", range = "75 mi", motorPower = "2x 1000W Nominal (3600W Peak AWD)",
        battery = "1820Wh (Dual 52V 20Ah + 15Ah)", nominalVoltage = 52, weight = "108 lb",
        chargingTime = "6 hrs", efficiency = "24 Wh/mi", brakingSystem = "Tektro Hydraulic 180mm",
        suspensionType = "Full Dual-Crown Fork + Rear Coil", tireProfile = "20x4.0\" Fat All-Terrain",
        ipRating = "IPX4", maxLoad = "300 lb", siteUrl = "https://arielrider.com/products/grizzly",
      ),
      PevSpecRecord(
        name = "Revv 1 DRT", brand = "Ride1Up", category = "EBIKE", price = "$2,495",
        topSpeed = "35+ mph", range = "60 mi", motorPower = "1000W Geared Hub (95 Nm)",
        battery = "1040Wh (52V 20Ah)", nominalVoltage = 52, weight = "93 lb",
        chargingTime = "6 hrs", efficiency = "21 Wh/mi", brakingSystem = "4-Piston Hydraulic 203mm",
        suspensionType = "Dual-Crown Moto Fork + Rear Air Shock", tireProfile = "20x4.0\" Off-Road Knobby",
        ipRating = "IP65", maxLoad = "350 lb", siteUrl = "https://ride1up.com/product/revv-1/",
      ),
      PevSpecRecord(
        name = "Ramblas eMTB", brand = "Aventon", category = "EBIKE", price = "$2,699",
        topSpeed = "20 mph", range = "80 mi", motorPower = "A100 Mid-Drive (100 Nm / 750W Peak)",
        battery = "708Wh (36V 19.6Ah LG)", nominalVoltage = 36, weight = "54 lb",
        chargingTime = "5 hrs", efficiency = "11 Wh/mi", brakingSystem = "SRAM DB8 4-Piston Hydraulic",
        suspensionType = "RockShox 35 Silver 130mm Air Fork", tireProfile = "29x2.4\" Maxxis Rekon Tubeless",
        ipRating = "IP67 Motor", maxLoad = "300 lb", siteUrl = "https://www.aventon.com/products/ramblas-electric-mountain-bike",
      ),
      PevSpecRecord(
        name = "Atlas Pro 4WD", brand = "Exway", category = "ESKATE", price = "$2,399",
        topSpeed = "35 mph", range = "35 mi", motorPower = "4x 1764W (7056W Peak AWD)",
        battery = "701Wh (51.8V 12S4P Samsung 40T)", nominalVoltage = 44, weight = "33 lb",
        chargingTime = "2.5 hrs", efficiency = "22 Wh/mi", brakingSystem = "4WD Regenerative Braking",
        suspensionType = "Carbon Deck + Pneumatic 160mm Tires", tireProfile = "160x50mm All-Terrain Pneumatic",
        ipRating = "IP55", maxLoad = "330 lb", siteUrl = "https://www.exwayboard.com/products/atlas-pro",
      ),
      PevSpecRecord(
        name = "Hurricane Ultra X", brand = "Meepo", category = "ESKATE", price = "$1,799",
        topSpeed = "35 mph", range = "31 mi", motorPower = "2x 3500W Gear/Belt Drive",
        battery = "725Wh (12S4P Molicel P42A)", nominalVoltage = 44, weight = "35 lb",
        chargingTime = "3.5 hrs", efficiency = "24 Wh/mi", brakingSystem = "FOC Regen",
        suspensionType = "Forged DKP/TKP Trucks + 175mm Tires", tireProfile = "175mm Pneumatic AT",
        ipRating = "IP54", maxLoad = "330 lb", siteUrl = "https://www.meepoboard.com/products/meepo-hurricane",
      ),
    )

  /** Extra PEV Parts with direct product links appended to Parts Finder. */
  val EXTRA_PEV_PARTS: List<PevPartItem> =
    listOf(
      PevPartItem("PMT Stradale 100/55-6.5 Radial", "Tires & Tubes", "100/55-6.5 (11\" Tubeless)", "SCOOTER • NAMI, Dualtron, Wolf King, RS", "$79.00", "PMT Italian Radial", "Voro Motors", "https://www.voromotors.com/search?q=PMT+Stradale+11+inch", "Moderate", "PERFORMANCE_UPGRADE", 2.5),
      PevPartItem("TFL Enduro Soft Compound Tire", "Tires & Tubes", "11.5x6.5-6 / 6.5\" Hub", "ONEWHEEL • GT, GT-S, XR, Pint X", "$115.00", "The Float Life", "The Float Life", "https://thefloatlife.com/search?q=Enduro+Tire", "Moderate", "PERFORMANCE_UPGRADE", 2.8),
      PevPartItem("Shinko 241 Dual-Sport 19\" Tire", "Tires & Tubes", "2.75-19 Tube-Type", "EMOTO • Sur-Ron, Talaria, E-Ride Pro", "$58.00", "Shinko", "GritShift", "https://gritshift.com/search?q=Shinko+241", "Moderate", "PERFORMANCE_UPGRADE", 5.2),
      PevPartItem("Maxxis Minion DHF 26x4.0 Fat Tire", "Tires & Tubes", "26x4.0\" 120 TPI EXO", "EBIKE • Aventon, Rad Power, Fat Trail", "$89.00", "Maxxis", "Amazon", "https://www.amazon.com/s?k=Maxxis+Minion+26x4.0+fat+bike+tire", "Easy", "PERFORMANCE_UPGRADE", 3.3),
      PevPartItem("Armor-Dilloz Red High-Speed Sealant", "Tires & Tubes", "8 oz Kevlar-Fiber Bottle", "ALL • Scooters, EUCs, Onewheels, eBikes", "$32.00", "Armor-Dilloz", "The Float Life", "https://thefloatlife.com/search?q=Armor-Dilloz", "Easy", "MAINTENANCE", 0.6),
      PevPartItem("Magura MT5e 4-Piston eBike/Scooter Brake", "Brakes & Rotors", "4-Piston w/ HIGO Cutoff", "ALL • SCOOTER • EBIKE • EMOTO", "$155.00", "Magura", "Voro Motors", "https://www.voromotors.com/search?q=Magura+MT5e", "Moderate", "PERFORMANCE_UPGRADE", 0.9),
      PevPartItem("Galfer 203mm Floating Wave Rotor", "Brakes & Rotors", "203mm OD x 2.0mm 6-Bolt", "ALL • EBIKE • EMOTO • Heavy Scooters", "$64.00", "Galfer", "Amazon", "https://www.amazon.com/s?k=Galfer+203mm+ebike+rotor", "Easy", "PERFORMANCE_UPGRADE", 0.55),
      PevPartItem("Copper-Sintered 4-Piston Brake Pads", "Brakes & Rotors", "39.5x26.5mm Sintered", "ALL • Magura, Shimano, Nutt, Logan, Zoom", "$19.00", "Galfer / TruckerCo", "Amazon", "https://www.amazon.com/s?k=sintered+4+piston+ebike+brake+pads", "Easy", "MAINTENANCE", 0.2),
      PevPartItem("EBMX X-9000 V2 High-Power Controller", "Controllers & BMS", "Up to 35kW 60V-90V FOC", "EMOTO • Sur-Ron, Talaria, E-Ride Pro", "$1,150.00", "EBMX", "Charged Cycle Works", "https://chargedcycleworks.com/search?q=EBMX+X-9000", "Advanced", "PERFORMANCE_UPGRADE", 3.8),
      PevPartItem("Flipsky 75100 Pro V2 Aluminum VESC", "Controllers & BMS", "14S-20S 84V 100A FOC", "ALL • SCOOTER • EBIKE • ESKATE • DIY", "$119.00", "Flipsky", "Flipsky VESC", "https://flipsky.net/search?q=75100+Pro", "Advanced", "PERFORMANCE_UPGRADE", 1.2),
      PevPartItem("Spintend Ubox 100V 200A Dual VESC", "Controllers & BMS", "20S-24S 100V Dual Motor", "SCOOTER • ESKATE • Custom Dual Builds", "$349.00", "Spintend", "Spintend Ubox", "https://spintend.com/search?q=Ubox+100V", "Advanced", "PERFORMANCE_UPGRADE", 2.1),
      PevPartItem("JK Smart Active Balancer BMS 200A", "Controllers & BMS", "8S-24S 2A Active Balance", "ALL • Custom 48V-84V Packs", "$98.00", "JK-BMS", "AliExpress PEV", "https://www.aliexpress.com/wholesale?SearchText=JK+Smart+BMS+200A+2A+active+balance", "Advanced", "PERFORMANCE_UPGRADE", 1.1),
      PevPartItem("ChiBattery Gladiator 72V 51Ah Molicel Pack", "Batteries & Chargers", "72V 51Ah P45B High-Drain", "EMOTO • Sur-Ron, Talaria, E-Ride Pro", "$2,299.00", "ChiBatterySystems", "ChiBatterySystems", "https://chibatterysystems.com/search?q=72V+Gladiator", "Moderate", "PERFORMANCE_UPGRADE", 26.0),
      PevPartItem("Roger Charger SC 16A Variable Fast Charger", "Batteries & Chargers", "67V-168V 1A-16A Digital", "EUC • SCOOTER • High-Voltage PEVs", "$299.00", "Roger Charger", "eWheels", "https://www.ewheels.com/?s=rapid+charger", "Easy", "PERFORMANCE_UPGRADE", 3.4),
      PevPartItem("QS8-S Heavy Duty 180A Anti-Spark Plug", "Batteries & Chargers", "8mm Gold Bullet 6-8AWG", "EMOTO • SCOOTER • 72V+ High-Amp Packs", "$18.00", "QS8 Genuine", "Amazon", "https://www.amazon.com/s?k=QS8-S+anti+spark+connector", "Moderate", "PERFORMANCE_UPGRADE", 0.25),
      PevPartItem("Hydraulic Steering Damper Stabilizer Kit", "Suspension", "24-Click CNC Oil Damper", "SCOOTER • NAMI, Wolf King, Dualtron, RS", "$119.00", "SONKEN / EREXX", "Voro Motors", "https://www.voromotors.com/search?q=steering+damper", "Moderate", "PERFORMANCE_UPGRADE", 1.3),
      PevPartItem("SKF 6001-2RSH / 6203-2RSH Sealed Bearings", "Bearings & Hardware", "Dual-Lip Waterproof 2RSH", "ALL • Hub Motors & Axle Assemblies", "$16.00", "SKF", "Amazon", "https://www.amazon.com/s?k=SKF+6001+2RSH+bearing", "Advanced", "MAINTENANCE", 0.3),
      PevPartItem("Grizzla Flow Power Pads (EUC)", "Safety & Armor", "Dual-Density TPU Ergonomic", "EUC • Lynx, Sherman, V14, Extreme, S22", "$189.00", "Grizzla", "EUCO", "https://www.euco.us/search?q=Grizzla+pads", "Easy", "PERFORMANCE_UPGRADE", 1.5),
      PevPartItem("TSG Pass Pro Carbon Full-Face Helmet", "Safety & Armor", "ASTM F1952 / EN1078 Carbon", "ALL • EUC • SCOOTER • ESKATE • ONEWHEEL", "$329.00", "TSG", "eWheels", "https://www.ewheels.com/?s=helmet", "Easy", "MAINTENANCE", 2.2),
      PevPartItem("Outbound Lighting Detour / Trail Evo 2200lm", "Cockpit & Lights", "Cutoff StVZO Beam USB-C", "ALL • EBIKE • SCOOTER • EMOTO", "$195.00", "Outbound Lighting", "Amazon", "https://www.amazon.com/s?k=Outbound+Lighting+Detour+headlight", "Easy", "PERFORMANCE_UPGRADE", 0.5),
      PevPartItem("Garmin Varia RTL515 Rear Radar Tail Light", "Cockpit & Lights", "140m Car Radar + Strobe", "ALL • EBIKE • SCOOTER • EUC • Rural Roads", "$199.00", "Garmin", "Amazon", "https://www.amazon.com/s?k=Garmin+Varia+RTL515", "Easy", "PERFORMANCE_UPGRADE", 0.2),
      PevPartItem("Kryptonite New York Fahgettaboudit U-Lock", "Safety & Armor", "18mm Hardened MAX Steel", "ALL • High-Value PEV Theft Deterrent", "$129.00", "Kryptonite", "Amazon", "https://www.amazon.com/s?k=Kryptonite+Fahgettaboudit", "Easy", "MAINTENANCE", 4.5),
      PevPartItem("Bat-Safe LiPo / PEV Fire Containment Box", "Batteries & Chargers", "Flame-Arrestor Vault", "ALL • Safe Indoor PEV Battery Charging", "$89.00", "Bat-Safe", "Amazon", "https://www.amazon.com/s?k=Bat-Safe+charging+box", "Easy", "MAINTENANCE", 4.2),
    )

  /** Returns true when a URL is just a store/brand homepage without a product path or search query. */
  fun isHomepageUrl(url: String): Boolean {
    val clean = url.trim()
    if (!clean.startsWith("http://", true) && !clean.startsWith("https://", true)) return true
    val parsed = runCatching { URI(clean) }.getOrNull() ?: return false
    val path = parsed.path.orEmpty().trimEnd('/').lowercase()
    val hasQuery = !parsed.query.isNullOrBlank()
    return !hasQuery && (path.isBlank() || path in setOf("/en", "/us", "/us/en", "/en-us", "/pages/home"))
  }

  /** Returns true when a URL is a Google search page rather than an actual merchant destination. */
  fun isGenericGoogleSearchUrl(url: String): Boolean {
    val clean = url.trim()
    val parsed = runCatching { URI(clean) }.getOrNull() ?: return false
    val host = parsed.host?.lowercase().orEmpty()
    val path = parsed.path?.lowercase().orEmpty()
    return (host == "google.com" || host.endsWith(".google.com")) && path.startsWith("/search")
  }

  /** Returns true when a URL is a deterministic native store search endpoint (excluding Google SERPs). */
  private fun isReliableNativeSearchUrl(url: String): Boolean {
    val u = url.trim().lowercase()
    if (!u.startsWith("http://") && !u.startsWith("https://")) return false
    if (isGenericGoogleSearchUrl(u)) return false
    return u.contains("/search") ||
      u.contains("?q=") ||
      u.contains("&q=") ||
      u.contains("?s=") ||
      u.contains("?k=") ||
      u.contains("?_nkw=") ||
      u.contains("searchtext=") ||
      u.contains("search_query=") ||
      u.contains("searchterm=") ||
      u.contains("catalogsearch")
  }

  /** Finds a known store whose domain matches the supplied URL. */
  fun findStoreByUrl(url: String): PevStoreEntry? {
    val host = runCatching { URI(url.trim()).host?.lowercase()?.removePrefix("www.").orEmpty() }.getOrDefault("")
    if (host.isBlank()) return null
    return KNOWN_PEV_STORES.firstOrNull { entry ->
      val entryHost = entry.hostDomain()
      entryHost.isNotBlank() && (host == entryHost || host.endsWith(".$entryHost"))
    }
  }

  /** Match a single PEV brand or vendor platform name to an official store in KNOWN_PEV_STORES. */
  fun findBestBrandStore(brandOrPlatform: String, category: String = ""): PevStoreEntry? {
    val key = brandOrPlatform.trim().lowercase()
    if (key.isBlank()) return null

    // Explicit alias table for top PEV makers & marketplaces
    val aliasId =
      when {
        key.contains("amazon") -> "amazon"
        key.contains("ebay") -> "ebay"
        key.contains("walmart") -> "walmart"
        key.contains("aliexpress") -> "aliexpress"
        key.contains("best buy") || key.contains("bestbuy") -> "bestbuy"
        key.contains("target") -> "target"
        key.contains("home depot") -> "homedepot"
        key.contains("google") -> "google_shop"
        key.contains("rei") -> "rei"
        key.contains("revzilla") -> "revzilla"
        key.contains("cycle gear") -> "cyclegear"
        key.contains("jenson") -> "jensonusa"
        key.contains("worldwide cyclery") || key.contains("worldwidecyclery") -> "worldwidecyclery"
        key.contains("harbor freight") -> "harborfreight"
        key.contains("wyrdryds") || key.contains("wyrd ryds") -> "wyrdryds"
        key.contains("voro") || key.contains("emove") -> "voro"
        key.contains("fluid") -> "fluid"
        key.contains("alien") -> "alienrides"
        key.contains("rev rides") || key.contains("revrides") || key.contains("vsett") -> "revrides"
        key.contains("ewheels") || key.contains("leaperkim") || key.contains("veteran") || key.contains("kingsong") -> "ewheels"
        key.contains("euco") || key.contains("grizzla") -> "euco"
        key.contains("nextgen") -> "nextgen"
        key.contains("begode") || key.contains("gotway") || key.contains("extreme bull") -> "alienrides"
        key.contains("dualtron") || key.contains("minimotors") -> "minimotors"
        key.contains("apollo") -> "apollo"
        key.contains("segway") || key.contains("ninebot") -> "segway"
        key.contains("kaabo") -> "kaabo"
        key.contains("roadrunner") -> "roadrunner"
        key.contains("nami") -> "nami"
        key.contains("teverun") -> "teverun"
        key.contains("ausom") -> "ausom"
        key.contains("kukirin") || key.contains("kugoo") -> "kukirin"
        key.contains("niu") -> "niu"
        key.contains("gotrax") -> "gotrax"
        key.contains("inmotion") -> "inmotion"
        key.contains("onewheel") || key.contains("future motion") -> "onewheel"
        key.contains("float life") || key.contains("tfl") -> "floatlife"
        key.contains("craft&ride") || key.contains("craft and ride") -> "craftride"
        key.contains("floatwheel") -> "floatwheel"
        key.contains("lectric") -> "lectric"
        key.contains("aventon") -> "aventon"
        key.contains("rad power") || key.contains("radpower") -> "radpower"
        key.contains("juiced") -> "juiced"
        key.contains("ride1up") -> "ride1up"
        key.contains("velotric") -> "velotric"
        key.contains("super73") -> "super73"
        key.contains("ariel") -> "arielrider"
        key.contains("engwe") -> "engwe"
        key.contains("fiido") -> "fiido"
        key.contains("mooncool") -> "mooncool"
        key.contains("specialized") -> "specialized"
        key.contains("sur-ron") || key.contains("surron") || key.contains("talaria") || key.contains("luna") -> "lunacycle"
        key.contains("gritshift") -> "gritshift"
        key.contains("charged cycle") || key.contains("ebmx") -> "chargedcycle"
        key.contains("e-ride") || key.contains("eride") -> "eridepro"
        key.contains("warp 9") || key.contains("warp9") -> "warp9"
        key.contains("rocky mountain") || key.contains("rockymountain") -> "rockymountain"
        key.contains("backfire") -> "backfire"
        key.contains("meepo") -> "meepo"
        key.contains("exway") -> "exway"
        key.contains("eboardstuff") -> "eboardstuff"
        key.contains("chibattery") || key.contains("chi battery") -> "chibattery"
        key.contains("grin") || key.contains("ebikes.ca") -> "grin"
        key.contains("flipsky") -> "flipsky"
        key.contains("spintend") -> "spintend"
        key.contains("tannus") -> "tannus"
        key.contains("outbound") -> "outbound"
        key.contains("pmt") -> "pmt"
        else -> null
      }
    if (aliasId != null) {
      KNOWN_PEV_STORES.firstOrNull { it.id == aliasId }?.let { return it }
    }
    return KNOWN_PEV_STORES.firstOrNull { store ->
      key.contains(store.name.lowercase()) ||
        store.name.lowercase().contains(key) ||
        key.contains(store.id.lowercase())
    }
  }

  /** Split multi-vendor fields (e.g. "eWheels / Voro", "Amazon / Jenson") into all matching stores in order. */
  fun findAllMatchingStores(vendorField: String, category: String = ""): List<PevStoreEntry> {
    val clean = vendorField.trim()
    if (clean.isBlank()) return emptyList()
    val found = linkedMapOf<String, PevStoreEntry>()
    val parts = clean.split(Regex("""[/,|]""")).map { it.trim() }.filter { it.isNotBlank() }
    parts.forEach { segment ->
      val match = findBestBrandStore(segment, category)
      if (match != null && match.id != "google_shop") {
        found[match.id] = match
      }
    }
    if (found.isEmpty()) {
      findBestBrandStore(clean, category)?.takeIf { it.id != "google_shop" }?.let { found[it.id] = it }
    }
    return found.values.toList()
  }

  /** Build a clean, concise product search query for a PEV Spec without redundant brand names or slash clutter. */
  fun buildCleanSpecQuery(spec: PevSpecRecord): String {
    val cleanBrand = spec.brand.split("/").firstOrNull()?.trim().orEmpty()
    val cleanName =
      spec.name
        .split("/")
        .firstOrNull()
        .orEmpty()
        .replace(Regex("""\([^)]*\)"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    val parts =
      if (cleanBrand.isNotBlank() && cleanName.contains(cleanBrand, ignoreCase = true)) {
        listOf(cleanName)
      } else {
        listOf(cleanBrand, cleanName)
      }
    return parts.filter { it.isNotBlank() }.joinToString(" ").trim()
  }

  /** Build a clean, concise product search query for a PEV Part without noise tags or multi-model slash bloat. */
  fun buildCleanPartQuery(part: PevPartItem): String {
    val withoutNoiseParens =
      part.partName
        .replace(Regex("""\((?:Scooter|EUC|Onewheel|eSkate|Sur-Ron|CNC Billet)[^)]*\)""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\bw/\s*""", RegexOption.IGNORE_CASE), " ")
    val primarySegment =
      withoutNoiseParens
        .split(" / ")
        .firstOrNull()
        .orEmpty()
        .replace("/", " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    return primarySegment.ifBlank { part.partName.trim() }.take(80)
  }

  private fun PevStoreEntry.hasNativeStoreSearch(): Boolean =
    !isGenericGoogleSearchUrl(searchTemplate) && !searchTemplate.contains("google.com/search", true)

  /**
   * Build an accurate, working destination link for a PEV Spec card.
   * Never returns bare homepages or non-shopping Google SERPs; prefers verified deep product URLs
   * or direct brand/authorized dealer store search URLs.
   */
  fun directLinkForSpec(spec: PevSpecRecord): String {
    val rawSite = spec.siteUrl.trim()
    if (rawSite.startsWith("verified::")) {
      val verified = rawSite.removePrefix("verified::").trim()
      if (verified.startsWith("http") && !isHomepageUrl(verified) && !isGenericGoogleSearchUrl(verified)) {
        return verified
      }
    }
    if (rawSite.startsWith("http") && !isHomepageUrl(rawSite) && !isGenericGoogleSearchUrl(rawSite)) {
      return rawSite
    }

    val modelQuery = buildCleanSpecQuery(spec)
    val brandStore = findBestBrandStore(spec.brand, spec.category)
    if (brandStore != null && brandStore.hasNativeStoreSearch() && modelQuery.isNotBlank()) {
      return brandStore.buildSearchUrl(modelQuery)
    }

    val categoryDealer =
      KNOWN_PEV_STORES.firstOrNull { store ->
        store.hasNativeStoreSearch() &&
          store.id != "google_shop" &&
          (store.category.equals(spec.category, true) || store.category == "MULTI")
      }
    if (categoryDealer != null && modelQuery.isNotBlank()) {
      return categoryDealer.buildSearchUrl(modelQuery)
    }

    val fullQuery =
      listOf(modelQuery, spec.category.takeIf { it.isNotBlank() && it != "OTHER" })
        .filterNotNull()
        .joinToString(" ")
        .trim()
    val enc = URLEncoder.encode(fullQuery.ifBlank { "electric PEV" }, "UTF-8")
    return "https://www.google.com/search?tbm=shop&q=$enc"
  }

  /**
   * Build an accurate, working destination buy/search link for a PEV Part item.
   * Rejects fake/generic `google.com/search?q=` links and bare homepages so users always land
   * on an exact verified product URL or the actual retailer store search for that part.
   */
  fun directLinkForPart(part: PevPartItem): String {
    val rawUrl = part.partUrl.trim()
    if (rawUrl.startsWith("verified::")) {
      val verified = rawUrl.removePrefix("verified::").trim()
      if (verified.startsWith("http") && !isHomepageUrl(verified) && !isGenericGoogleSearchUrl(verified)) {
        return verified
      }
    }
    if (isReliableNativeSearchUrl(rawUrl) || (rawUrl.startsWith("http") && !isHomepageUrl(rawUrl) && !isGenericGoogleSearchUrl(rawUrl))) {
      return rawUrl
    }

    val cleanPartQuery = buildCleanPartQuery(part)
    val matchedStores =
      (findAllMatchingStores(part.sourcePlatform) + findAllMatchingStores(part.recommendedBrands))
        .filter { it.hasNativeStoreSearch() }
    val primaryStore = matchedStores.firstOrNull()
    if (primaryStore != null && cleanPartQuery.isNotBlank()) {
      return primaryStore.buildSearchUrl(cleanPartQuery)
    }

    val fallbackDealer =
      when {
        part.compatibility.contains("ONEWHEEL", true) -> "floatlife"
        part.compatibility.contains("EUC", true) -> "ewheels"
        part.compatibility.contains("EMOTO", true) || part.compatibility.contains("SURRON", true) -> "gritshift"
        part.compatibility.contains("ESKATE", true) -> "eboardstuff"
        part.compatibility.contains("EBIKE", true) -> "jensonusa"
        else -> "voro"
      }.let { id -> KNOWN_PEV_STORES.firstOrNull { it.id == id } }
    if (fallbackDealer != null && cleanPartQuery.isNotBlank()) {
      return fallbackDealer.buildSearchUrl(cleanPartQuery)
    }

    val enc = URLEncoder.encode("${cleanPartQuery.ifBlank { part.partName }} PEV", "UTF-8")
    return "https://www.google.com/search?tbm=shop&q=$enc"
  }

  /**
   * Build prioritized candidate store URLs (deep product page + official/authorized specialty PEV stores)
   * to verify live in-stock availability and resolve exact direct product destination URLs.
   * Never seeds homepages or Google SERP links as merchant candidates.
   */
  fun candidateStoresForSpec(spec: PevSpecRecord): List<Pair<String, Pair<String, String>>> {
    val result = linkedMapOf<String, Pair<String, String>>()
    val query = buildCleanSpecQuery(spec)
    val rawSite = spec.siteUrl.trim().removePrefix("verified::").trim()

    val brandStore = findBestBrandStore(spec.brand, spec.category)
    if (rawSite.startsWith("http") && !isHomepageUrl(rawSite) && !isGenericGoogleSearchUrl(rawSite)) {
      val urlStore = findStoreByUrl(rawSite) ?: brandStore
      val id = urlStore?.id ?: "official_${spec.brand.lowercase().replace(Regex("[^a-z0-9]"), "")}"
      val name = urlStore?.name ?: "${spec.brand} Official"
      result[id] = name to rawSite
    }
    if (brandStore != null && brandStore.hasNativeStoreSearch() && query.isNotBlank() && brandStore.id !in result) {
      result[brandStore.id] = brandStore.name to brandStore.buildSearchUrl(query)
    }

    val preferredIds =
      when (spec.category.uppercase()) {
        "SCOOTER" -> listOf("voro", "fluid", "revrides", "alienrides", "wyrdryds", "minimotors", "roadrunner", "kaabo")
        "EUC" -> listOf("ewheels", "alienrides", "euco", "nextgen", "inmotion", "voro")
        "ONEWHEEL" -> listOf("onewheel", "floatlife", "craftride", "ebay")
        "EMOTO", "SURRON" -> listOf("lunacycle", "gritshift", "chargedcycle", "revrides", "eridepro", "voro")
        "ESKATE" -> listOf("backfire", "meepo", "exway", "eboardstuff", "ebay")
        "EBIKE", "OTHER" -> listOf("lectric", "aventon", "radpower", "juiced", "ride1up", "fiido", "mooncool", "rei", "jensonusa", "bestbuy", "amazon")
        else -> listOf("voro", "revrides", "alienrides", "ewheels", "amazon", "ebay")
      }

    preferredIds.forEach { id ->
      val s = KNOWN_PEV_STORES.firstOrNull { it.id == id }
      if (s != null && s.id !in result && s.hasNativeStoreSearch() && query.isNotBlank()) {
        result[s.id] = s.name to s.buildSearchUrl(query)
      }
    }

    KNOWN_PEV_STORES
      .filter { it.id !in result && it.id != "google_shop" && it.hasNativeStoreSearch() && (it.category.equals(spec.category, true) || it.category == "MULTI") }
      .forEach { s ->
        if (query.isNotBlank()) result[s.id] = s.name to s.buildSearchUrl(query)
      }

    if ("ebay" !in result && query.isNotBlank()) {
      KNOWN_PEV_STORES.firstOrNull { it.id == "ebay" }?.let { s ->
        result[s.id] = s.name to s.buildSearchUrl(query)
      }
    }
    return result.entries.take(8).map { it.key to it.value }
  }

  /**
   * Build prioritized candidate store URLs for a PEV part so only verified in-stock stores appear
   * and multi-vendor platforms ("eWheels / Voro", "RevRides / AlienRides") check every named dealer.
   */
  fun candidateStoresForPart(part: PevPartItem): List<Pair<String, Pair<String, String>>> {
    val result = linkedMapOf<String, Pair<String, String>>()
    val query = buildCleanPartQuery(part)
    val rawUrl = part.partUrl.trim().removePrefix("verified::").trim()

    val matchedVendors =
      (findAllMatchingStores(part.sourcePlatform) + findAllMatchingStores(part.recommendedBrands))
        .distinctBy { it.id }

    if (rawUrl.startsWith("http") && !isHomepageUrl(rawUrl) && !isGenericGoogleSearchUrl(rawUrl)) {
      val urlStore = findStoreByUrl(rawUrl) ?: matchedVendors.firstOrNull()
      val id = urlStore?.id ?: part.sourcePlatform.lowercase().replace(Regex("[^a-z0-9]"), "_").ifBlank { "vendor" }
      val name = urlStore?.name ?: part.sourcePlatform.ifBlank { "PEV Vendor" }
      result[id] = name to rawUrl
    }

    matchedVendors.forEach { store ->
      if (store.id !in result && store.hasNativeStoreSearch() && query.isNotBlank()) {
        result[store.id] = store.name to store.buildSearchUrl(query)
      }
    }

    val categoryBoostIds =
      when {
        part.category.contains("Brakes", true) -> listOf("jensonusa", "worldwidecyclery", "voro", "wyrdryds", "gritshift")
        part.category.contains("Controllers", true) -> listOf("flipsky", "spintend", "chargedcycle", "voro", "grin")
        part.category.contains("Batteries", true) -> listOf("chibattery", "ewheels", "grin", "voro", "alienrides")
        part.category.contains("Safety", true) -> listOf("revzilla", "cyclegear", "ewheels", "euco", "amazon")
        part.category.contains("Tires", true) -> listOf("voro", "floatlife", "ewheels", "tannus", "rockymountain", "jensonusa")
        else -> emptyList()
      }

    val compatIds =
      when {
        part.compatibility.contains("ONEWHEEL", true) -> listOf("floatlife", "craftride", "onewheel", "voro", "ebay")
        part.compatibility.contains("EUC", true) -> listOf("ewheels", "euco", "alienrides", "nextgen", "ebay")
        part.compatibility.contains("EMOTO", true) || part.compatibility.contains("SURRON", true) ->
          listOf("gritshift", "lunacycle", "chargedcycle", "warp9", "rockymountain", "revrides", "ebay")
        part.compatibility.contains("ESKATE", true) -> listOf("eboardstuff", "backfire", "meepo", "exway", "flipsky", "ebay")
        part.compatibility.contains("EBIKE", true) -> listOf("jensonusa", "worldwidecyclery", "rei", "tannus", "voro", "amazon", "ebay")
        else -> listOf("voro", "revrides", "fluid", "wyrdryds", "ewheels", "amazon", "ebay")
      }

    (categoryBoostIds + compatIds).distinct().forEach { id ->
      val store = KNOWN_PEV_STORES.firstOrNull { it.id == id }
      if (store != null && store.id !in result && store.hasNativeStoreSearch() && query.isNotBlank()) {
        result[store.id] = store.name to store.buildSearchUrl(query)
      }
    }
    return result.entries.take(8).map { it.key to it.value }
  }
}
