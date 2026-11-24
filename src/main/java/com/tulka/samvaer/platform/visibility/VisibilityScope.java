package com.tulka.samvaer.platform.visibility;

/**
 * ONLY_ME: a private note. GROUPS: only members of the chosen groups (plus anyone the item names
 * directly, such as an invited guest). ALL_MEMBERS: every Samvær member ("Public"), never
 * anyone outside Samvær.
 */
public enum VisibilityScope {
    ONLY_ME,
    GROUPS,
    ALL_MEMBERS
}
