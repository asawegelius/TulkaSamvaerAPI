/**
 * Cross-cutting building blocks every module may use: problem-details errors, paging,
 * correlation ids, time and OpenAPI configuration. Contains no business rules.
 */
@ApplicationModule(displayName = "Shared", type = ApplicationModule.Type.OPEN)
package com.tulka.samvaer.shared;

import org.springframework.modulith.ApplicationModule;
