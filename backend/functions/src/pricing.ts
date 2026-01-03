import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * Validates routine creation against plan tier limits
 * Enforces free tier limit of 3 routines
 */
export const validateRoutineCreation = functions.firestore
  .document("families/{familyId}/routines/{routineId}")
  .onCreate(async (snap, context) => {
    const familyId = context.params.familyId;
    const routineId = context.params.routineId;

    // Get family plan tier
    const familyDoc = await admin.firestore()
      .collection("families")
      .doc(familyId)
      .get();

    if (!familyDoc.exists) {
      functions.logger.error(`Family ${familyId} not found`);
      return;
    }

    const planTier = familyDoc.data()?.planTier || "free";

    // Paid tier has unlimited routines
    if (planTier === "paid") {
      functions.logger.info(`Family ${familyId} is on paid tier, allowing routine creation`);
      return;
    }

    // Free tier: check routine count (excluding soft-deleted)
    const routinesSnapshot = await admin.firestore()
      .collection("families").doc(familyId)
      .collection("routines")
      .where("deletedAt", "==", null)
      .get();

    const activeRoutineCount = routinesSnapshot.size;

    // If over limit, soft-delete the newly created routine
    if (activeRoutineCount > 3) {
      functions.logger.warn(
        `Family ${familyId} exceeded free tier limit (${activeRoutineCount} routines), ` +
        `soft-deleting routine ${routineId}`
      );

      await snap.ref.update({
        deletedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Note: The client should check limits before creating,
      // but this is a server-side safety check
    } else {
      functions.logger.info(
        `Family ${familyId} has ${activeRoutineCount} routines (within free tier limit)`
      );
    }
  });

