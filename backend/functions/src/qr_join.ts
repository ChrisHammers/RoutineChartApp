import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * Generates a join token for a family
 * Called by parents to create QR invite codes
 */
export const generateJoinToken = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Must be signed in to generate join token"
    );
  }

  const { familyId } = data;

  if (!familyId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "familyId is required"
    );
  }

  // Verify user is a parent in this family
  const userDoc = await admin.firestore()
    .collection("families").doc(familyId)
    .collection("users").doc(context.auth.uid)
    .get();

  if (!userDoc.exists || userDoc.data()?.role !== "parent") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only parents can generate join tokens"
    );
  }

  // Generate random token ID
  const tokenId = generateRandomToken();
  const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24 hours

  // Create join token document
  await admin.firestore()
    .collection("families").doc(familyId)
    .collection("joinTokens").doc(tokenId)
    .set({
      createdBy: context.auth.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
      used: false,
    });

  functions.logger.info(`Generated join token ${tokenId} for family ${familyId}`);

  return {
    tokenId,
    familyId,
    expiresAt: expiresAt.toISOString(),
  };
});

/**
 * Joins a family using a join token
 * Called by children who scan QR code
 */
export const joinFamilyWithToken = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Must be signed in to join family"
    );
  }

  const { tokenId, childProfileData } = data;

  if (!tokenId || !childProfileData) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "tokenId and childProfileData are required"
    );
  }

  // Validate child profile data
  if (!childProfileData.displayName || !childProfileData.ageBand || !childProfileData.readingMode) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "displayName, ageBand, and readingMode are required in childProfileData"
    );
  }

  // Find token in all families (using collection group query)
  const tokenQuery = await admin.firestore()
    .collectionGroup("joinTokens")
    .where(admin.firestore.FieldPath.documentId(), "==", tokenId)
    .limit(1)
    .get();

  if (tokenQuery.empty) {
    throw new functions.https.HttpsError(
      "not-found",
      "Invalid join token"
    );
  }

  const tokenDoc = tokenQuery.docs[0];
  const tokenData = tokenDoc.data();
  const familyId = tokenDoc.ref.parent.parent!.id;

  // Validate token
  if (tokenData.used) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Join token has already been used"
    );
  }

  const now = admin.firestore.Timestamp.now();
  if (tokenData.expiresAt < now) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Join token has expired"
    );
  }

  // Create child profile
  const childId = admin.firestore().collection("families").doc().id;
  await admin.firestore()
    .collection("families").doc(familyId)
    .collection("children").doc(childId)
    .set({
      displayName: childProfileData.displayName,
      avatarIcon: childProfileData.avatarIcon || null,
      ageBand: childProfileData.ageBand,
      readingMode: childProfileData.readingMode,
      audioEnabled: childProfileData.audioEnabled !== false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

  // Create user document linking auth to child profile
  await admin.firestore()
    .collection("families").doc(familyId)
    .collection("users").doc(context.auth.uid)
    .set({
      role: "child",
      childId: childId,
      displayName: childProfileData.displayName,
      email: context.auth.token.email || null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

  // Mark token as used
  await tokenDoc.ref.update({
    used: true,
    usedBy: context.auth.uid,
    usedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  functions.logger.info(`User ${context.auth.uid} joined family ${familyId} as child ${childId}`);

  return {
    familyId,
    childId,
    success: true,
  };
});

/**
 * Generates a random token string
 */
function generateRandomToken(): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let token = "";
  for (let i = 0; i < 8; i++) {
    token += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return token;
}

