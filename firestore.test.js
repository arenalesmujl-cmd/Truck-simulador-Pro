const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_driver_1";
const BOB_UID = "bob_driver_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    driverName: "Capitan Ruta",
    money: 25000,
    xp: 0,
    level: 1,
    selectedTruckId: "titan_680",
    ownedTruckIds: ["titan_680"],
    truckPaintHex: "#FF6D00",
    engineLevel: 1,
    transmissionLevel: 1,
    suspensionLevel: 1,
    tankLevel: 1,
    unlockedRouteIds: ["route_valley"],
    totalDistanceKm: 0.0,
    completedMissionsCount: 0,
    graphicsQuality: "HIGH",
    gearboxMode: "AUTO",
    steeringMode: "WHEEL",
    soundEnabled: true,
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or write career profile or deliveries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("deliveries").get());
});

test("Owner can create career profile, run action-based partial updates, and log delivery", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Action: garage purchase & upgrade
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      money: 18000,
      ownedTruckIds: ["titan_680", "euro_fh16"],
      selectedTruckId: "euro_fh16",
      truckPaintHex: "#1E88E5",
      engineLevel: 2,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Action: career progress update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      money: 22500,
      xp: 350,
      level: 2,
      totalDistanceKm: 18.4,
      completedMissionsCount: 1,
      unlockedRouteIds: ["route_valley", "route_coast"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Action: settings update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      graphicsQuality: "MEDIUM",
      gearboxMode: "MANUAL",
      steeringMode: "BUTTONS",
      soundEnabled: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Create immutable delivery record (NO @DocumentId id field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("deliveries").doc("deliv_1").set({
      userId: ALICE_UID,
      missionId: "m_valley_steel",
      cargoName: "Vigas de Acero",
      originName: "Puerto Bahía",
      destinationName: "Ciudad Central",
      truckId: "euro_fh16",
      distanceKm: 18.4,
      rewardMoney: 4500,
      damagePercent: 2,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // List own deliveries succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("deliveries").get()
  );
});

test("Rejects ghost fields, immutable createdAt changes, future timestamps, and invalid truck IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      money: 999999,
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Mutating createdAt rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      money: 26000,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Invalid truck ID rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedTruckId: "hacked_truck",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delivery updates rejected (immutable)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("deliveries").doc("d1").set({
      userId: ALICE_UID,
      missionId: "m1",
      cargoName: "Madera",
      originName: "Bosque Norte",
      destinationName: "Valle Sur",
      truckId: "titan_680",
      distanceKm: 12.0,
      rewardMoney: 2200,
      damagePercent: 0,
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("deliveries").doc("d1").update({
      rewardMoney: 99999,
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice profile or deliveries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, new Date(Date.now() - 5000)))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ money: 0, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("deliveries").get());
});
