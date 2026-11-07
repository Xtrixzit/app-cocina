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

test("Unauthenticated user: cannot read recipes, pantry, or shopping", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("recipes").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("pantry").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("shopping_items").get());
});

test("Cross-user isolation: Bob cannot read Alice's recipes", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").set({
      userId: ALICE_UID,
      title: "Tortilla Española",
      category: "Almuerzo",
      servings: 4,
      prepTimeMinutes: 15,
      cookTimeMinutes: 20,
      difficulty: "Fácil",
      ingredients: [{ name: "Huevos", amount: 4, unit: "unidades", category: "Lácteos" }],
      instructions: ["Batir huevos", "Cocinar en sartén"],
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("recipes").get());
});

test("Owner: can create and read their own recipe", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").set({
      userId: ALICE_UID,
      title: "Paella de Pollo",
      category: "Almuerzo",
      servings: 4,
      prepTimeMinutes: 20,
      cookTimeMinutes: 35,
      difficulty: "Media",
      ingredients: [{ name: "Arroz", amount: 400, unit: "g", category: "Granos" }],
      instructions: ["Dorar pollo", "Añadir arroz y caldo"],
      createdAt: new Date(),
    })
  );

  const doc = await aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_1").get();
  if (!doc.exists) throw new Error("Document should exist");
});

test("Validation: rejects recipe with invalid servings or missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Missing required title
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_bad").set({
      userId: ALICE_UID,
      servings: 4,
      prepTimeMinutes: 10,
      cookTimeMinutes: 10,
      ingredients: [],
      instructions: [],
      createdAt: new Date(),
    })
  );

  // Servings 0 is invalid
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("recipes").doc("rec_bad2").set({
      userId: ALICE_UID,
      title: "Sopa",
      servings: 0,
      prepTimeMinutes: 10,
      cookTimeMinutes: 10,
      ingredients: [],
      instructions: [],
      createdAt: new Date(),
    })
  );
});

test("Owner: can manage pantry items and shopping items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  
  // Create pantry item
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pantry").doc("item_1").set({
      userId: ALICE_UID,
      name: "Aceite de Oliva",
      category: "Despensa",
      quantity: 500,
      unit: "ml",
      minQuantity: 200,
      createdAt: new Date(),
    })
  );

  // Create shopping item
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("shopping_items").doc("shop_1").set({
      userId: ALICE_UID,
      name: "Cebollas",
      category: "Verduras",
      quantity: 3,
      unit: "unidades",
      isPurchased: false,
      recipeSource: "Tortilla Española",
      createdAt: new Date(),
    })
  );

  // Bob cannot touch Alice's pantry or shopping items
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("pantry").doc("item_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("shopping_items").doc("shop_1").delete());
});
