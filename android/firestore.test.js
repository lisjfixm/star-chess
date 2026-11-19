const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_starchess_1";
const BOB_UID = "bob_starchess_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCreatePayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    label: "Primary Sync",
    schemaVersion: "v7",
    entries: {
      "starchess.lang": "zh-Hant",
      "starchess.starCoins": "250",
      "starchess.v7.schemaVer": "v7",
    },
    starCoins: 250,
    eloPeak: 1240,
    achievementsCount: 4,
    gamesPlayed: 12,
    preferredLanguage: "zh-Hant",
    deviceName: "Pixel 9",
    clientSavedAtMs: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
}

function validGameStatPayload(uid, gameId = "gomoku", now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    gameId,
    wins: 7,
    losses: 3,
    draws: 1,
    playDurationSeconds: 1840,
    createdAt: now,
    updatedAt: now,
  };
}

function validLeaderboardPayload(uid, wins = 24, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "Nova Commander",
    totalWins: wins,
    totalLosses: 9,
    totalDraws: 2,
    totalPlayDurationSeconds: 5400,
    gamesPlayedCount: 8,
    favoriteTheme: "nebula_purple",
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

test("Unauthenticated user cannot read or write cloudSaves, gameStats, or leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cloudSaves").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("cloudSaves").doc("primary").set(validCreatePayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("gameStats").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("gameStats").doc("gomoku").set(validGameStatPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("leaderboard").orderBy("totalWins", "desc").limit(50).get());
  await assertFails(
    unauthDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID))
  );
});

test("Authenticated owner can create, partial-update, list, and delete cloudSaves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("cloudSaves").doc("primary");

  await assertSucceeds(ref.set(validCreatePayload(ALICE_UID)));

  await assertSucceeds(
    ref.update({
      label: "Primary Sync (Updated)",
      starCoins: 420,
      eloPeak: 1310,
      gamesPlayed: 15,
      entries: {
        "starchess.lang": "en",
        "starchess.starCoins": "420",
      },
      preferredLanguage: "en",
      clientSavedAtMs: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cloudSaves").get());
  await assertSucceeds(ref.delete());
});

test("Authenticated owner can create, partial-update, list, and delete gameStats", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const statsCol = aliceDb.collection("users").doc(ALICE_UID).collection("gameStats");
  const gomokuRef = statsCol.doc("gomoku");

  await assertSucceeds(gomokuRef.set(validGameStatPayload(ALICE_UID, "gomoku")));
  await assertSucceeds(
    gomokuRef.update({
      wins: 9,
      losses: 4,
      draws: 1,
      playDurationSeconds: 2450,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(statsCol.get());
  await assertSucceeds(gomokuRef.delete());
});

test("Global leaderboard allows bounded authenticated reads and owner-only writes, rejects PII and tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceRef = aliceDb.collection("leaderboard").doc(ALICE_UID);
  const bobRef = bobDb.collection("leaderboard").doc(BOB_UID);

  // Owners can publish and update their own leaderboard rank
  await assertSucceeds(aliceRef.set(validLeaderboardPayload(ALICE_UID, 24)));
  await assertSucceeds(bobRef.set(validLeaderboardPayload(BOB_UID, 42)));

  await assertSucceeds(
    aliceRef.update({
      displayName: "Astral Alice",
      totalWins: 30,
      totalLosses: 10,
      totalDraws: 2,
      totalPlayDurationSeconds: 6100,
      gamesPlayedCount: 9,
      favoriteTheme: "solar_gold",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bounded query matching LeaderboardRepository succeeds for any signed-in player
  await assertSucceeds(
    aliceDb.collection("leaderboard").orderBy("totalWins", "desc").limit(50).get()
  );
  await assertSucceeds(
    bobDb.collection("leaderboard").orderBy("totalWins", "desc").limit(20).get()
  );

  // Unbounded or oversized list queries fail
  await assertFails(aliceDb.collection("leaderboard").orderBy("totalWins", "desc").get());
  await assertFails(aliceDb.collection("leaderboard").orderBy("totalWins", "desc").limit(100).get());

  // Cross-user write/update/delete is strictly forbidden
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      totalWins: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("leaderboard").doc(ALICE_UID).delete());

  // Reject PII email inside displayName, ghost fields, invalid theme, and > 26 gamesPlayedCount
  const emailPii = validLeaderboardPayload(ALICE_UID, 30);
  emailPii.displayName = "alice@example.com";
  await assertFails(aliceDb.collection("leaderboard").doc(ALICE_UID).update({
    displayName: "alice@example.com",
    updatedAt: new Date(Date.now() - 400),
  }));

  await assertFails(
    aliceRef.update({
      gamesPlayedCount: 27,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertFails(
    aliceRef.update({
      favoriteTheme: "invalid_palette",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertFails(
    aliceRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertFails(
    aliceRef.update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Rejects invalid gameId, negative counters, ghost fields, and @DocumentId id on gameStats", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const statsCol = aliceDb.collection("users").doc(ALICE_UID).collection("gameStats");

  await assertFails(statsCol.doc("unknown_game").set(validGameStatPayload(ALICE_UID, "unknown_game")));

  const badWins = validGameStatPayload(ALICE_UID, "chess");
  badWins.wins = -1;
  await assertFails(statsCol.doc("chess").set(badWins));

  const withId = validGameStatPayload(ALICE_UID, "chess");
  withId.id = "chess";
  await assertFails(statsCol.doc("chess").set(withId));

  const withGhost = validGameStatPayload(ALICE_UID, "chess");
  withGhost.isAdmin = true;
  await assertFails(statsCol.doc("chess").set(withGhost));

  const mismatchedGameId = validGameStatPayload(ALICE_UID, "go");
  await assertFails(statsCol.doc("chess").set(mismatchedGameId));
});

test("Rejects @DocumentId id injection, ghost fields, future timestamps, and invalid enums on cloudSaves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const savesCol = aliceDb.collection("users").doc(ALICE_UID).collection("cloudSaves");

  const withId = validCreatePayload(ALICE_UID);
  withId.id = "ckpt_bad_id";
  await assertFails(savesCol.doc("ckpt_bad_id").set(withId));

  const withGhost = validCreatePayload(ALICE_UID);
  withGhost.isAdmin = true;
  await assertFails(savesCol.doc("ckpt_ghost").set(withGhost));

  const futureTs = new Date(Date.now() + 60000);
  await assertFails(savesCol.doc("ckpt_future").set(validCreatePayload(ALICE_UID, futureTs)));

  const badLang = validCreatePayload(ALICE_UID);
  badLang.preferredLanguage = "fr";
  await assertFails(savesCol.doc("ckpt_lang").set(badLang));
});

test("Rejects immutable createdAt, userId, or gameId mutation on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const saveRef = aliceDb.collection("users").doc(ALICE_UID).collection("cloudSaves").doc("primary");
  await assertSucceeds(saveRef.set(validCreatePayload(ALICE_UID)));

  await assertFails(
    saveRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertFails(
    saveRef.update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  const statRef = aliceDb.collection("users").doc(ALICE_UID).collection("gameStats").doc("go");
  await assertSucceeds(statRef.set(validGameStatPayload(ALICE_UID, "go")));

  await assertFails(
    statRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertFails(
    statRef.update({
      gameId: "chess",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user isolation blocks Bob from reading, writing, or deleting Alice saves and stats", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("cloudSaves")
      .doc("primary")
      .set(validCreatePayload(ALICE_UID));
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("gameStats")
      .doc("gomoku")
      .set(validGameStatPayload(ALICE_UID, "gomoku"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceSaveRef = bobDb.collection("users").doc(ALICE_UID).collection("cloudSaves").doc("primary");
  const aliceStatRef = bobDb.collection("users").doc(ALICE_UID).collection("gameStats").doc("gomoku");

  await assertFails(aliceSaveRef.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cloudSaves").get());
  await assertFails(aliceSaveRef.update({ starCoins: 9999, updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceSaveRef.delete());

  await assertFails(aliceStatRef.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("gameStats").get());
  await assertFails(aliceStatRef.update({ wins: 999, updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceStatRef.delete());
});
