const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_pilot_123";
const BOB_UID = "bob_pilot_456";
const ADMIN_UID = "admin_pilot_001";
const ADMIN_EMAIL = "bcbradley12@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = (offsetMs = 5000) => new Date(Date.now() - offsetMs);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are completely blocked across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("board_posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("radar_riders").where("visibility", "==", "PUBLIC").get());
  await assertFails(
    unauthDb.collection("board_posts").doc("post_1").set({
      userId: ALICE_UID,
      authorCallsign: "ALICE",
      authorBadge: "Scout",
      category: "UNIVERSAL",
      title: "Unauth",
      body: "No auth",
      imageUrl: "",
      pollQuestion: "",
      pollOptions: [],
      priceUsd: 0,
      locationLabel: "",
      telemetrySummary: "",
      helpNeeded: false,
      speedTrap: false,
      stolenAlert: false,
      likesCount: 0,
      commentsCount: 0,
      flaggedCount: 0,
      visibility: "PUBLIC",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Owner-only /users/{uid} and subcollections isolate PII and reject cross-user access & future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rider",
      callsign: "VOLT_ALICE",
      email: "alice@example.com",
      activeVehicleId: "veh_1",
      unitSystem: "IMPERIAL",
      emergencyContactName: "Mom",
      sosContactNumber: "555-0100",
      medicalProfile: "O-Pos",
      acceptedLegal: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      callsign: "BAD_TIME",
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  // Cross-user PII read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ callsign: "HIJACK" }));

  // Owner CAN change username/callsign and update rich profile info (birthdate, bio, hometown, privacy)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      callsign: "OZARK_ALICE",
      displayName: "Alice Trailblazer",
      birthdate: "1994-05-14",
      bio: "Rural EUC & off-road e-scooter explorer.",
      hometown: "Springfield, MO",
      ridingStyle: "Trail & Gravel",
      profileVisibility: "PUBLIC",
      allowDirectCalls: true,
      updatedAt: pastDate(1000),
    })
  );

  // Legacy/partial document without callsign can be healed once by owner
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
    });
  });
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).update({
      userId: BOB_UID,
      displayName: "Bob Rider",
      callsign: "VOLT_BOB",
      email: "bob@example.com",
      activeVehicleId: "",
      unitSystem: "IMPERIAL",
      emergencyContactName: "",
      sosContactNumber: "",
      medicalProfile: "",
      acceptedLegal: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Subcollections: vehicle + ride + prayer
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_1").set({
      userId: ALICE_UID,
      name: "Inmotion V14",
      category: "EUC",
      batteryWh: 2400,
      voltage: 134.4,
      targetPsi: 35.0,
      odometerKm: 420.5,
      serviceIntervalKm: 500.0,
      tireSize: "16x3",
      photoUri: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("prayers").doc("p_1").set({
      userId: ALICE_UID,
      title: "Morning Trail Safety",
      body: "Watch over the crew today.",
      category: "Safety",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("vehicles").get()
  );
});

test("PublicPilotProfile blocks self-promotion (verified/banned/trustScore) & enforces verified email for Admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_1", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const ts = pastDate(10000);

  // Alice cannot create with verified=true
  await assertFails(
    aliceDb.collection("user_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      callsign: "ALICE",
      rankBadge: "Rookie Pilot",
      xpPoints: 0,
      totalKm: 0,
      ridesCount: 0,
      verified: true,
      banned: false,
      trustScore: 100,
      vehicleSummary: "EUC",
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid creation
  await assertSucceeds(
    aliceDb.collection("user_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      callsign: "ALICE",
      rankBadge: "Rookie Pilot",
      xpPoints: 150,
      totalKm: 42.5,
      ridesCount: 3,
      verified: false,
      banned: false,
      trustScore: 100,
      vehicleSummary: "Inmotion V14",
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Spoofed unverified admin email blocked
  await assertFails(
    spoofAdminDb.collection("user_profiles").doc(ALICE_UID).update({
      verified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Real verified admin succeeds
  await assertSucceeds(
    realAdminDb.collection("user_profiles").doc(ALICE_UID).update({
      verified: true,
      trustScore: 100,
      updatedAt: pastDate(1000),
    })
  );

  // Filtered query succeeds, unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("user_profiles").where("visibility", "==", "PUBLIC").get()
  );
  await assertFails(aliceDb.collection("user_profiles").get());
});

test("Community Board, Comments Parent Gate, Likes, and Admin Broadcasts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("board_posts").doc("post_1").set({
      userId: ALICE_UID,
      authorCallsign: "ALICE",
      authorBadge: "Trail Scout",
      category: "UNIVERSAL",
      title: "Trail open!",
      body: "Dry gravel on sector 4.",
      imageUrl: "",
      pollQuestion: "",
      pollOptions: [],
      priceUsd: 0.0,
      locationLabel: "Sector 4",
      telemetrySummary: "32km @ 28km/h",
      helpNeeded: false,
      speedTrap: false,
      stolenAlert: false,
      likesCount: 0,
      commentsCount: 0,
      flaggedCount: 0,
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob can comment with parent post existing
  await assertSucceeds(
    bobDb.collection("board_posts").doc("post_1").collection("comments").doc("c_1").set({
      userId: BOB_UID,
      authorCallsign: "BOB",
      body: "On my way!",
      visibility: "PUBLIC",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Orphan comment fails
  await assertFails(
    bobDb.collection("board_posts").doc("missing_post").collection("comments").doc("c_2").set({
      userId: BOB_UID,
      authorCallsign: "BOB",
      body: "Orphan",
      visibility: "PUBLIC",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Bob cannot delete Alice's post
  await assertFails(bobDb.collection("board_posts").doc("post_1").delete());

  // Non-admin cannot issue broadcast; real admin can
  await assertFails(
    aliceDb.collection("broadcasts").doc("b_1").set({
      userId: ALICE_UID,
      category: "SYSTEM ALERT",
      message: "Fake",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    realAdminDb.collection("broadcasts").doc("b_1").set({
      userId: ADMIN_UID,
      category: "SYSTEM ALERT",
      message: "Bridge closed ahead",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Direct Messages & Call Sessions enforce strict participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_999").firestore();
  const ts = pastDate(10000);
  const threadId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("direct_messages").doc(threadId).set({
      participantUids: [ALICE_UID, BOB_UID],
      participantCallsigns: ["ALICE", "BOB"],
      lastMessage: "Meet at trailhead",
      selfDestructSec: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("direct_messages").doc(threadId).collection("messages").doc("m_1").set({
      participantUids: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderCallsign: "BOB",
      text: "Copy that",
      imageUrl: "",
      voiceBase64: "",
      selfDestructSec: 0,
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Eve blocked from thread, direct message get, unfiltered list, and querying Alice's UID
  await assertFails(eveDb.collection("direct_messages").doc(threadId).get());
  await assertFails(eveDb.collection("direct_messages").doc(threadId).collection("messages").doc("m_1").get());
  await assertFails(eveDb.collection("direct_messages").doc(threadId).collection("messages").get());
  await assertFails(
    eveDb.collection("direct_messages").doc(threadId).collection("messages").where("participantUids", "array-contains", ALICE_UID).get()
  );

  // Call session terminal ENDED state lock
  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_1").set({
      callerUid: ALICE_UID,
      callerCallsign: "ALICE",
      calleeUid: BOB_UID,
      calleeCallsign: "BOB",
      callType: "AUDIO",
      status: "RINGING",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("call_sessions").doc("call_1").update({
      status: "ENDED",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("call_sessions").doc("call_1").update({
      status: "ACTIVE",
      updatedAt: pastDate(100),
    })
  );
});

test("Radar Beacons & Walkie-Talkie channels enforce ownership & active filters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("radar_riders").doc(ALICE_UID).set({
      userId: ALICE_UID,
      callsign: "ALICE",
      vehicleName: "V14",
      lat: 35.25,
      lng: -95.12,
      speedKmh: 38.5,
      headingDeg: 180.0,
      batteryPct: 86,
      statusTag: "CRUISING",
      visibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    bobDb.collection("radar_riders").doc(ALICE_UID).update({
      speedKmh: 99.0,
      updatedAt: pastDate(500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("channels").doc("CH-1").collection("presence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      callsign: "ALICE",
      channelId: "CH-1",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("channels").doc("CH-1").collection("presence").where("active", "==", true).get()
  );
});

test("WebRTC Call Signals & Owner Cloud Backups enforce participant and owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_999").firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_sig_1").set({
      callerUid: ALICE_UID,
      callerCallsign: "ALICE",
      calleeUid: BOB_UID,
      calleeCallsign: "BOB",
      callType: "VIDEO",
      status: "RINGING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice sends OFFER; Bob sends ANSWER; Eve is blocked
  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_sig_1").collection("signals").doc("sig_1").set({
      senderId: ALICE_UID,
      signalType: "OFFER",
      payload: "{\"type\":\"offer\",\"sdp\":\"v=0...\"}",
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );
  await assertSucceeds(
    bobDb.collection("call_sessions").doc("call_sig_1").collection("signals").get()
  );
  await assertSucceeds(
    bobDb.collection("call_sessions").doc("call_sig_1").collection("signals").doc("sig_2").set({
      senderId: BOB_UID,
      signalType: "ANSWER",
      payload: "{\"type\":\"answer\",\"sdp\":\"v=0...\"}",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  await assertFails(
    eveDb.collection("call_sessions").doc("call_sig_1").collection("signals").get()
  );
  await assertFails(
    eveDb.collection("call_sessions").doc("call_sig_1").collection("signals").doc("sig_3").set({
      senderId: "eve_999",
      signalType: "ICE",
      payload: "{}",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );
  await assertFails(
    aliceDb.collection("call_sessions").doc("call_sig_1").collection("signals").doc("sig_bad").set({
      senderId: ALICE_UID,
      signalType: "HACK",
      payload: "{}",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Cloud Backup owner isolation & validation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("backups").doc("latest").set({
      userId: ALICE_UID,
      provider: "FIRESTORE",
      summary: "2 PEVs, 4 maintenance items",
      payloadJson: "{\"version\":2,\"settings\":{}}",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("backups").doc("latest").update({
      provider: "FIRESTORE",
      summary: "Updated snapshot",
      payloadJson: "{\"version\":2,\"settings\":{\"dark\":true}}",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("backups").doc("latest").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("backups").doc("latest").set({
      userId: ALICE_UID,
      provider: "FIRESTORE",
      summary: "Spoof",
      payloadJson: "{}",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("CallSession fixed-slot /streams/{streamId} enforces participant isolation, owner slot IDs, and payload limits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_999").firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_stream_1").set({
      callerUid: ALICE_UID,
      callerCallsign: "ALICE",
      calleeUid: BOB_UID,
      calleeCallsign: "BOB",
      callType: "VIDEO",
      status: "RINGING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob can attach a listener/get to Alice's video slot even before frame #1 exists
  await assertSucceeds(
    bobDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc(`v_${ALICE_UID}`).get()
  );

  // Alice writes & overwrites her own video and audio stream slots
  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc(`v_${ALICE_UID}`).set({
      senderId: ALICE_UID,
      kind: "VIDEO",
      seq: 1,
      active: true,
      payload: "/9j/4AAQSkZJRgABAQAAAQABAAD",
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc(`v_${ALICE_UID}`).set({
      senderId: ALICE_UID,
      kind: "VIDEO",
      seq: 2,
      active: true,
      payload: "/9j/4AAQSkZJRgABAQAAAQABAAE",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc(`a_${BOB_UID}`).set({
      senderId: BOB_UID,
      kind: "AUDIO",
      seq: 1,
      active: true,
      payload: "f39/f39/f38=",
      updatedAt: pastDate(1000),
    })
  );

  // Bob reads Alice's live video slot
  await assertSucceeds(
    bobDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc(`v_${ALICE_UID}`).get()
  );

  // Bob CANNOT overwrite Alice's stream slot
  await assertFails(
    bobDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc(`v_${ALICE_UID}`).set({
      senderId: BOB_UID,
      kind: "VIDEO",
      seq: 99,
      active: true,
      payload: "hijack",
      updatedAt: pastDate(500),
    })
  );

  // Eve (non-participant) cannot read or write streams
  await assertFails(
    eveDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc(`v_${ALICE_UID}`).get()
  );
  await assertFails(
    eveDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc("v_eve_999").set({
      senderId: "eve_999",
      kind: "VIDEO",
      seq: 1,
      active: true,
      payload: "intruder",
      updatedAt: pastDate(500),
    })
  );

  // Oversized payload (> 95,000 bytes) rejected
  await assertFails(
    aliceDb.collection("call_sessions").doc("call_stream_1").collection("streams").doc(`v_${ALICE_UID}`).set({
      senderId: ALICE_UID,
      kind: "VIDEO",
      seq: 3,
      active: true,
      payload: "A".repeat(96000),
      updatedAt: pastDate(500),
    })
  );
});


