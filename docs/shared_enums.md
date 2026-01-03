# Shared Enums and Constants

This document defines the canonical enum values that MUST be used consistently across iOS, Android, and Backend implementations.

## Core Enums

### Role

User roles within a family.

**Values:**
- `parent` - Can create/edit routines, manage family, view all analytics
- `child` - Can only complete/undo steps, view own analytics

**Implementation:**
- Swift: `enum Role: String { case parent, child }`
- Kotlin: `enum class Role { PARENT, CHILD }` (serialize as lowercase)
- Firestore: Store as string `"parent"` or `"child"`

---

### PlanTier

Subscription tier for the family.

**Values:**
- `free` - Maximum 3 routines
- `paid` - Unlimited routines

**Implementation:**
- Swift: `enum PlanTier: String { case free, paid }`
- Kotlin: `enum class PlanTier { FREE, PAID }` (serialize as lowercase)
- Firestore: Store as string `"free"` or `"paid"`

---

### AgeBand

Age range for child profiles, used to adapt UI presentation.

**Values:**
- `2_4` - Toddler (2-4 years) - purely visual interface
- `5_7` - Preschool (5-7 years) - icons + minimal text
- `8_10` - Elementary (8-10 years) - full text labels
- `11_plus` - Teen (11+ years) - advanced features

**Implementation:**
- Swift: `enum AgeBand: String { case age_2_4 = "2_4", age_5_7 = "5_7", age_8_10 = "8_10", age_11_plus = "11_plus" }`
- Kotlin: `enum class AgeBand { AGE_2_4, AGE_5_7, AGE_8_10, AGE_11_PLUS }` (serialize as "2_4", "5_7", "8_10", "11_plus")
- Firestore: Store as string `"2_4"`, `"5_7"`, `"8_10"`, or `"11_plus"`

---

### ReadingMode

Reading level preference for child profiles.

**Values:**
- `visual` - Icons only, no text labels
- `light_text` - Icons + short labels
- `full_text` - Full text descriptions

**Implementation:**
- Swift: `enum ReadingMode: String { case visual, light_text = "light_text", full_text = "full_text" }`
- Kotlin: `enum class ReadingMode { VISUAL, LIGHT_TEXT, FULL_TEXT }` (serialize as "visual", "light_text", "full_text")
- Firestore: Store as string `"visual"`, `"light_text"`, or `"full_text"`

---

### CompletionRule

Rule for determining when a routine is complete.

**Values:**
- `all_steps_required` - All steps must be completed (V1 only supports this rule)

**Implementation:**
- Swift: `enum CompletionRule: String { case all_steps_required = "all_steps_required" }`
- Kotlin: `enum class CompletionRule { ALL_STEPS_REQUIRED }` (serialize as "all_steps_required")
- Firestore: Store as string `"all_steps_required"`

**Note:** V2 may add additional rules like `any_step`, `minimum_count`, etc.

---

### EventType

Type of completion event.

**Values:**
- `complete` - Step was marked as complete
- `undo` - Step was marked as incomplete (by tapping a completed step again)

**Implementation:**
- Swift: `enum EventType: String { case complete, undo }`
- Kotlin: `enum class EventType { COMPLETE, UNDO }` (serialize as lowercase)
- Firestore: Store as string `"complete"` or `"undo"`

---

## Constants

### Week Start Day

**Values:**
- `0` = Sunday
- `1` = Monday
- `2` = Tuesday
- ... etc

**Default:** `0` (Sunday)

---

### Date Formats

**Local Day Key:**
- Format: `YYYY-MM-DD`
- Example: `2026-01-03`
- Timezone: Family timezone (IANA format like "America/Los_Angeles")
- Use: Event grouping, analytics, streak calculation

**Timestamp Format:**
- ISO 8601 with timezone
- Example: `2026-01-03T14:30:00-08:00`

---

## ULID Format

**Structure:**
- 26 characters, Base32 encoded
- 10 bytes timestamp (millisecond precision)
- 16 bytes randomness
- Example: `01ARZ3NDEKTSV4RRFFQ69G5FAV`

**Properties:**
- Lexicographically sortable by time
- Collision-free in distributed systems
- Used for: `eventId` in CompletionEvent

**Libraries:**
- Swift: Use `swift-ulid` or custom implementation
- Kotlin: Use `com.github.azam:ulidj`

---

## Device ID

**Format:**
- UUID v4 or platform-specific identifier
- Generated once per app installation
- Stored in local persistent storage
- Used for: Event attribution and sync conflict resolution

**Implementation:**
- iOS: Store in UserDefaults or Keychain
- Android: Store in SharedPreferences or Room

---

## Free Tier Limits

**Routine Limit:**
- Free: 3 active routines maximum
- Paid: Unlimited routines

**Note:** Cloud sync is NOT paywalled - available for all tiers.

---

## Version History

- **V1.0** (2026-01-03): Initial specification for Phase 0

