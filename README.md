# Tulka Samvær API

Spring Boot backend for **Samvær**, the Tulka members app for family and friends. The Angular
frontend ([TulkaSamvaerWeb](https://github.com/asawegelius/TulkaSamvaerWeb)) is a pure view: every
business rule lives here, and every item tells the frontend what the member may do in `allowedActions`.

Architecture, decisions and requirements live in [TulkaSamvaerDoc](https://github.com/asawegelius/TulkaSamvaerDoc).
Bootstrapped from [template-service](https://github.com/asawegelius/template-service) (persistence
flavor); provenance is in `template-origin.properties`.

## Run it

Java 21.

```bash
./gradlew test                                            # all tests
./gradlew bootRun --args='--spring.profiles.active=dev'   # http://localhost:8080
```

The `dev` profile uses in-memory H2 with the same sample members and groups as the frontend mock
(`mock-api/seed.mjs`), allows CORS from `http://localhost:4200`, and signs you in as Åsa. To act as
someone else, send `X-Stub-Member-Id: <member uuid>`, for example `...0002` for Jonas.
Swagger UI: http://localhost:8080/swagger-ui.html.

**Login is not decided yet (ADR-005).** Until it is, `samvaer.auth.mode=stub` lets the caller pick
who they are with a header. It is only on in the `dev` profile and in tests; without it every API
call answers 401. Never deploy with the stub on.

## Structure (ADR-001)

One deployable app, split into Spring Modulith modules. `ModularitySpec` fails the build when a
module reaches into another module's internals; only types in a module's root package are public.

```text
com.tulka.samvaer
  shared/                 problem-details errors, paging, correlation id, clock and home time zone
  platform/identity/      members, roles, CurrentMember (login stub until ADR-005)   /api/v1/me, /api/v1/members
  platform/groups/        member-made groups                                          /api/v1/me/groups
  platform/visibility/    Visibility value (Only me / groups / all members) and who may see an item
  sections/events/        events, invites, RSVP, bring-list, programme, sleeping, recurring events   /api/v1/events
```

Each module has `api`, `application`, `domain`, `infrastructure`. Tables are prefixed with the
module key and created by Liquibase changelogs in `src/main/resources/db/changelog/<module>/`.
Modules store each other's ids but never join each other's tables.

## API contract

The contract is `api/openapi.yaml` in TulkaSamvaerWeb. A copy is in
`src/test/resources/contract/openapi.yaml`, and the API tests check every response (status,
content type, body schema, formats) against it. `ContractCoverageSpec` checks that every operation
in the contract is implemented and that nothing under `/api` is missing from it. When the contract
changes in the web repo, copy it here and run the tests.

Errors are RFC 9457 problem details with a stable `code` (`common.validation`, `common.not-found`,
`common.forbidden`, `common.stale-version`, `events.item-full`, `auth.not-logged-in`, ...).

## Events rules

- **Visibility.** Only me: the host only. Groups: members of the chosen groups and anyone on the
  guest list. All members: every member. Anything else answers 404, never 403.
- **Invites.** Choosing groups invites their members. Hosts can invite more members or groups.
  A private (Only me) event cannot have guests.
- **RSVP.** Going, Maybe or Can't; answering again replaces the answer. On an open event anyone may
  answer; only invited members count as pending.
- **Who brings what.** Anyone who can see the event adds items. Claiming takes one slot (`slots:
  null` means anyone); a full item answers 409 `events.item-full`. The item row is locked while
  claiming, so two people racing for the last slot get one claim and one 409. Host or the member
  who added an item may remove it.
- **Edits.** Host only, with `version`; a stale version answers 409. Answers and bring-list changes
  never make the host's form stale.
- **Recurring events.** Weekly, monthly or yearly every *n*. Occurrences are created up front (up to
  24, at most two years ahead, never after `until`), each with its own guest list and bring-list,
  at the same local time in Europe/Copenhagen across daylight saving changes.
