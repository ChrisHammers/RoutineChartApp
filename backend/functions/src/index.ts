import * as admin from "firebase-admin";

// Initialize Firebase Admin SDK
admin.initializeApp();

// Export all cloud functions
export { generateJoinToken, joinFamilyWithToken } from "./qr_join";
export { validateRoutineCreation } from "./pricing";

