const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read recipes, pantry, shopping, production_logs, or config", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("recipes").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("pantry").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("shopping_items").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("production_logs").get());
  await assertFails(unauthDb.collection("app_config").doc("distribution").get());
  await assertFails(unauthDb.collection("licenses").doc("KEY_123").get());
});

test("Cross-user isolation: Bob cannot read or write Alice's collections or production_logs", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").set({
      userId: ALICE_UID,
      title: "Tortilla Española",
      category: "Almuerzos",
      servings: 4,
      prepTimeMinutes: 15,
      cookTimeMinutes: 20,
      difficulty: "Fácil",
      ingredients: [{ name: "Huevos", amount: 4, unit: "unidades", category: "Lácteos", manualUnitCost: 0.3 }],
      instructions: ["Batir huevos", "Cocinar en sartén"],
      notes: "",
      imageUrl: "",
      createdAt: past,
      updatedAt: past,
    });
    await context.firestore().collection("users").doc(ALICE_UID).collection("production_logs").doc("log_1").set({
      userId: ALICE_UID,
      recipeId: "rec_1",
      recipeTitle: "Tortilla Española",
      servingsProduced: 8,
      scaleFactor: 2.0,
      totalCost: 5.0,
      suggestedSellingPrice: 7.5,
      currencyCode: "USD",
      deductedIngredientsCount: 3,
      createdAt: past,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recipes").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("production_logs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("production_logs").doc("log_1").delete());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner: can create, partial-update, and read recipe with compressed image data URI", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const compactDataUri = "data:image/jpeg;base64," + "A".repeat(4000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").set({
      userId: ALICE_UID,
      title: "Paella de Pollo",
      category: "Almuerzos",
      servings: 4,
      prepTimeMinutes: 20,
      cookTimeMinutes: 35,
      difficulty: "Media",
      ingredients: [{ name: "Arroz", amount: 400, unit: "g", category: "Granos", manualUnitCost: 0.004 }],
      instructions: ["Dorar pollo", "Añadir arroz y caldo"],
      notes: "Usar arroz bomba",
      imageUrl: compactDataUri,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").update({
      servings: 8,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject modifying createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost @DocumentId `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_ghost").set({
      id: "rec_ghost",
      userId: ALICE_UID,
      title: "Sopa",
      servings: 2,
      prepTimeMinutes: 5,
      cookTimeMinutes: 10,
      ingredients: [],
      instructions: [],
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Owner: can record production_logs and delete account data cleanly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      displayName: "Alice",
      photoUrl: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("production_logs").doc("log_a").set({
      userId: ALICE_UID,
      recipeId: "rec_1",
      recipeTitle: "Pan Casero",
      servingsProduced: 12,
      scaleFactor: 3.0,
      totalCost: 4.5,
      suggestedSellingPrice: 6.75,
      currencyCode: "USD",
      deductedIngredientsCount: 4,
      createdAt: created,
    })
  );

  // Production logs are immutable (update must fail)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("production_logs").doc("log_a").update({
      totalCost: 1.0,
    })
  );

  // Owner can list & delete production log + account profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("production_logs").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("production_logs").doc("log_a").delete());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});

test("Commercial distribution: app_config read-only get & licenses single-claim enforced", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("app_config").doc("distribution").set({
      latestVersionCode: 13,
      latestVersionName: "3.0",
      downloadUrl: "https://example.com/Recetario.apk",
      releaseNotes: "Multi-idioma y conversión de unidades",
      requireLicense: true,
      supportContact: "soporte@example.com",
    });
    await context.firestore().collection("licenses").doc("LIC-2026-TEST").set({
      active: true,
      assignedUid: "",
      assignedEmail: "",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Signed-in user can get distribution doc, cannot list or overwrite it
  await assertSucceeds(aliceDb.collection("app_config").doc("distribution").get());
  await assertFails(aliceDb.collection("app_config").get());
  await assertFails(aliceDb.collection("app_config").doc("distribution").update({ requireLicense: false }));

  // Licenses cannot be listed or created by users
  await assertFails(aliceDb.collection("licenses").get());
  await assertFails(aliceDb.collection("licenses").doc("FAKE-KEY").set({ active: true, assignedUid: ALICE_UID }));

  // Alice can get and claim unused active license
  await assertSucceeds(aliceDb.collection("licenses").doc("LIC-2026-TEST").get());
  await assertSucceeds(
    aliceDb.collection("licenses").doc("LIC-2026-TEST").update({
      assignedUid: ALICE_UID,
      assignedEmail: "alice@gmail.com",
      activatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot steal Alice's claimed license
  await assertFails(
    bobDb.collection("licenses").doc("LIC-2026-TEST").update({
      assignedUid: BOB_UID,
      assignedEmail: "bob@gmail.com",
      activatedAt: new Date(Date.now() - 1000),
    })
  );

  // Even Alice cannot re-claim / reset activatedAt or assignedEmail once claimed
  await assertFails(
    aliceDb.collection("licenses").doc("LIC-2026-TEST").update({
      assignedUid: ALICE_UID,
      assignedEmail: "alice+2@gmail.com",
      activatedAt: new Date(Date.now() - 500),
    })
  );

  // Expired license cannot be claimed
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("licenses").doc("LIC-EXPIRED").set({
      active: true,
      assignedUid: "",
      assignedEmail: "",
      expiresAt: new Date(Date.now() - 60000),
    });
  });
  await assertFails(
    bobDb.collection("licenses").doc("LIC-EXPIRED").update({
      assignedUid: BOB_UID,
      assignedEmail: "bob@gmail.com",
      activatedAt: new Date(Date.now() - 1000),
    })
  );
});
