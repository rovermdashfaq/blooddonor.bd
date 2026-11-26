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
const ADMIN_EMAIL = "mdashfaqurrahmantanim@gmail.com";

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated requests are denied across collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("donorDirectory").where("approvalStatus", "==", "approved").get());
  await assertFails(unauthDb.collection("bloodRequests").where("status", "==", "active").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("User profile isolation and privilege escalation protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      fullName: "Alice Rahman",
      phoneNumber: "+8801711111111",
      userType: "donor",
      donorEnabled: true,
      seekerEnabled: false,
      bloodGroup: "O+",
      district: "Dhaka",
      gender: "female",
      approvalStatus: "pending",
      role: "user",
      totalDonations: 0,
      totalUnitsDonated: 0,
      lastDonationApprovalStatus: "none",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's PII profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Alice cannot self-promote role or self-approve donor status
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "main_admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      approvalStatus: "approved",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid partial update by Alice succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Regular O+ donor in Dhaka",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_1", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    spoofDb.collection("notices").doc("notice_1").set({
      title: "Fake Notice",
      message: "Spoof attempt",
      priority: "urgent",
      active: true,
      createdBy: "spoof_1",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Verified Super Admin can approve donors, issue certificates, and post notices", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("donorDirectory").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "Alice Rahman",
      bloodGroup: "O+",
      district: "Dhaka",
      gender: "female",
      approvalStatus: "pending",
      role: "user",
      totalDonations: 0,
      available: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const adminDb = testEnv.authenticatedContext("admin_1", { email: ADMIN_EMAIL, email_verified: true }).firestore();

  await assertSucceeds(
    adminDb.collection("donorDirectory").doc(ALICE_UID).update({
      approvalStatus: "approved",
      donorId: "BD20260001",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    adminDb.collection("certificates").doc("CERT_001").set({
      certificateId: "CERT_001",
      userId: ALICE_UID,
      userName: "Alice Rahman",
      donorId: "BD20260001",
      bloodGroup: "O+",
      district: "Dhaka",
      issuedBy: "admin_1",
      status: "valid",
      issuedDate: "2026-11-25",
      createdAt: new Date(Date.now() - 500),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertSucceeds(bobDb.collection("donorDirectory").where("approvalStatus", "==", "approved").get());
  await assertFails(bobDb.collection("donorDirectory").get());
  await assertSucceeds(bobDb.collection("certificates").doc("CERT_001").get());
});

test("Blood requests terminal state lock & query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("bloodRequests").doc("req_1").set({
      requesterUid: ALICE_UID,
      requesterName: "Alice",
      patientName: "Karim Uddin",
      bloodGroup: "B+",
      unitsNeeded: 2,
      hospitalName: "Dhaka Medical College",
      district: "Dhaka",
      contactNumber: "+8801712345678",
      requiredBy: "2026-12-01",
      urgency: "emergency",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("bloodRequests").where("status", "==", "active").get());
  await assertFails(bobDb.collection("bloodRequests").get());
  await assertFails(bobDb.collection("bloodRequests").doc("req_1").update({ status: "fulfilled", updatedAt: new Date(Date.now() - 500) }));

  // Owner fulfills request
  await assertSucceeds(
    aliceDb.collection("bloodRequests").doc("req_1").update({
      status: "fulfilled",
      fulfilledAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: owner cannot reactivate after fulfilled
  await assertFails(
    aliceDb.collection("bloodRequests").doc("req_1").update({
      status: "active",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Private DM isolation and subcollection access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_789").firestore();
  const now = new Date(Date.now() - 1000);
  const convId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("privateMessages").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      lastMessage: "Hello, can you donate?",
      lastMessageTimestamp: now,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("privateMessages").doc(convId).collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      receiverId: BOB_UID,
      participantIds: [ALICE_UID, BOB_UID],
      message: "Hello, can you donate?",
      readStatus: false,
      createdAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("privateMessages").where("participantIds", "array-contains", BOB_UID).get());
  await assertSucceeds(bobDb.collection("privateMessages").doc(convId).collection("messages").where("participantIds", "array-contains", BOB_UID).get());
  await assertFails(eveDb.collection("privateMessages").doc(convId).get());
  await assertFails(eveDb.collection("privateMessages").doc(convId).collection("messages").doc("msg_1").get());
});

test("Null-resource existence checks work safely for owner/verifier without leaking peers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const eveDb = testEnv.authenticatedContext("eve_789").firestore();

  // Owner checking non-existent donorDirectory entry before creation succeeds; outsider fails
  await assertSucceeds(aliceDb.collection("donorDirectory").doc(ALICE_UID).get());
  await assertFails(eveDb.collection("donorDirectory").doc(ALICE_UID).get());

  // Participant checking non-existent DM conversation doc before creation succeeds; outsider fails
  await assertSucceeds(aliceDb.collection("privateMessages").doc(`${ALICE_UID}_${BOB_UID}`).get());
  await assertFails(eveDb.collection("privateMessages").doc(`${ALICE_UID}_${BOB_UID}`).get());

  // Certificate verification lookup of non-existent ID succeeds (returns !exists)
  await assertSucceeds(aliceDb.collection("certificates").doc("CERT_MISSING").get());
});

