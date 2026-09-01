# Earth simulation progress

Canonical checkpoint for the OpenMC Earth migration. Updated: 2026-08-31.

## Current delivery state

| Area | Progress | Evidence |
| --- | ---: | --- |
| Discovery and reuse audit | 100% | Existing city/claim, mayor/law, economy, shop, home, command and ORMLite systems mapped. |
| Earth domain foundation | 74% | Persistent countries now include stable codes, government IDs, active state, and timestamps alongside aggregates, regions, city links, citizenship and regional state; all use ordered, durable, additive migrations through schema version 11. |
| Geography and territory | 42% | Region identifiers, validated non-overlapping chunk bounds, persisted safe arrival waypoints, configurable multi-country bootstrap metadata, and an OpenMC-owned country/region/city location service are active; polygon/BlueMap support remains. |
| Cities and properties | 67% | New OpenMC homes create residential properties; completed shops create commercial properties, both inherit a claiming city when present. Persistent access modes, lease offers, tenants, expiry, seller-created/buyer-accepted sales, and authoritative owner transfers are active without replacing existing protections. |
| Citizenship and society | 68% | Citizenship, occupations, civic reputation, elections, and persistent regional NPC demographics are active. |
| Government and law | 92% | Country policy, attributed laws, persistent bills, candidate/vote records, player-founded political parties/memberships, a leader-appointed durable cabinet, deterministic election closure, durable government identity/leader/ruling-party/mandate state, elected-leader authorization for bill/law/budget actions, persisted wall-clock candidacy/campaign/voting/mandate phases, and condition-aware public-budget impact previews affect the simulation. |
| Economy, jobs and businesses | 55% | Player jobs persist, pay work actions through EconomyManager, validate retail employers, scale productivity with civic/local conditions, support restart-safe property rents, and require retail employers to fund payroll. |
| Simulation and events | 83% | Events, public policy, laws, elected mandate, housing capacity, prosperity, employment, pollution, crime, health, education, infrastructure, energy, food security, inflation, debt, and approval participate in one persistent causal loop with scheduled player-facing consequences and a restart-safe accelerated calendar. |
| Transport, geopolitics and integrations | 70% | Paid routes, safe waypoints, destination-region work, persistent bilateral relations with trade effects, route-mode causal infrastructure/pollution/energy impacts, condition-based transport costs, and OpenMC-owned map/NPC adapter boundaries are active; actual BlueMap/NPC providers remain optional and uninstalled. |
| Verification and operational audit | 98% | Focused Earth suites now include reconnection persistence, permissions, legacy-schema migration, government transition, leader authority, parties, bills, cabinet effects, and repeated-cycle stability. The current full 348-test suite has zero failures/errors; clean compilation and shaded packaging pass. |
| Overall | 96% | Player/NPC society, policy, law, cabinet appointments, timed elections, accelerated simulation time, travel, events, international trade climate, local conditions, persistent property leases, business-funded payroll, visible regional consequences, optional integration boundaries, country aggregates, a dedicated Earth location API, explicit LuckPerms-compatible permission nodes, upgraded country metadata, and elected-government authority now jointly shape the Earth simulation with a green release-candidate build. |

## Reuse decisions

- `CityManager` remains the authority for player-created cities and chunk claims; Earth stores only a city-to-region link.
- `EconomyManager` remains the authority for player balances; Earth simulation maintains public regional metrics and treasury separately.
- The existing mayor and law subfeatures remain municipal governance. Earth country policy is additive, not a replacement.
- ORMLite tables are created by a normal `Feature implements HasDatabase`; commands use the established Lamp command registry.

## Risks and blockers

- This workspace copy has no `.git` directory, so the requested target branch cannot be verified and no commit can be created here.
- Final external validation is still required for production Paper/database migration boot, geographic dataset import, installed map/NPC providers, and Bedrock/Geyser compatibility. Home/shop sales now use authoritative owner persistence plus compensating payment rollback; they remain non-transactional across the memory-first economy.

## 30% architecture audit

- The core loop is live and additive: OpenMC owns balances/claims; Earth owns regional accounting, policy and outcomes.
- The commerce integration is event-driven at `Shop.buy`, while simulation remains bounded and periodic.
- Persistent tables are isolated by `earth_` prefix; no existing City, Shop, Home, or Economy table was altered.
- Focused model tests verify geographic bounds, bounded simulation metrics, ledger draining, and public-budget effects.

## Next checkpoint

At the 75% audit, complete the requirement-by-requirement delivery audit, harden any uncovered persistence edge cases, and decide whether optional map integration is in scope. Focused tests pass with `gradlew test --tests "fr.openmc.core.features.earth.models.EarthRegionStateTest"`. A full `gradlew test` compiled all source and test source but completed with 16 unrelated failures: MockBukkit classpath/bootstrap errors and economy-format assertions.

## 75% operational audit

- `/earth diagnose` gives operators a compact live count of configured countries, regions, properties, businesses, jobs, citizen records, events, routes, waypoints, diplomacy records, and active travel passes.
- Each bounded simulation cycle removes expired travel-pass rows from memory and persistent storage, preventing stale pass accumulation.
- `gradlew compileJava --no-daemon` and the focused Earth regression suite both passed after this hardening batch.

## 73% causal-model checkpoint

- `earth_regional_indicators` is an additive persistent table for inflation, crime, health, education, pollution, infrastructure, energy, food security, public debt, and government approval; existing Earth and OpenMC tables are untouched.
- Fiscal allocations, public-safety and green-infrastructure laws, economic activity, and persistent crisis events affect those indicators. The resulting conditions feed back into prosperity, employment, satisfaction, and player-work productivity.
- `/earth conditions` exposes the local conditions affected by the simulation, and the event catalogue now includes recession, pollution, strike, epidemic, food, energy, and migration crises.
- The focused model suite passed after the new causal feedback coverage was added.

## 75% governance checkpoint

- `earth_election_cycles` is an additive persistent election clock. Candidacy, campaign, voting, and mandate phases use real timestamps and advance after downtime; a weekly default mandate is configurable in `earth.elections`.
- Candidacy and voting are enforced only in their appropriate persisted phase. Closing a voting phase installs the existing deterministic winner before the mandate begins.
- The regional-condition productivity multiplier now changes real job output recorded in the regional commercial ledger, while fixed player wages avoid a new money-printing path.
- `gradlew test --tests "fr.openmc.core.features.earth.models.EarthRegionStateTest" --no-daemon` passed after these changes.

## 77% property-lifecycle checkpoint

- `earth_property_access` is an additive table for access intent, lease offers, tenants and real timestamp expiry. The property location registry is unchanged.
- `/earth property`, `info`, `access`, `lease-offer`, and `rent` expose the safe lifecycle. Rent debits the tenant before crediting the owner and keeps claims/home protections authoritative.
- This earlier limitation has been superseded: existing homes and shops now transfer through their parent authorities after a seller-created, buyer-accepted offer. Settlement uses compensation rather than a cross-feature database transaction.
- Focused model coverage verifies private/public access and lease expiry. The focused Earth suite passed after this checkpoint.

## 78% policy-preview checkpoint

- `/earth policy preview <country> <social> <infrastructure> <investment>` estimates next-cycle health, infrastructure, employment, pollution, approval, and treasury tradeoffs before a budget is saved.
- Estimates incorporate current country-level pollution and crime conditions and are expressly not guarantees; actual events, trade climate, and future player activity still influence outcomes.
- The focused Earth suite passed after the preview and allocation-normalization coverage was added.

## 80% world-consequence checkpoint

- Severe persistent pollution applies short, scheduled smog effects (nausea/slowness) to players in the affected region; severe food insecurity applies weakness. Effects are evaluated only during the bounded simulation cycle on the Paper main thread.
- Poor infrastructure and pollution raise paid route fares through a bounded multiplier, making public investment visibly relevant to travel.
- These effects are OpenMC-owned and do not depend on BlueMap, Citizens, or other optional plugins. The focused Earth suite passed after the transport-bound regression coverage was added.

## 82% simulation-time checkpoint

- `earth_simulation_clock` persists an accelerated virtual calendar independently from the real-time election clock. It advances using configurable `earth.simulation.real-day-seconds` and `simulated-months-per-real-day` values.
- The calendar deliberately resets its uptime baseline on server start, so an offline server does not silently simulate months of unobserved economic change.
- `/earth sim status` shows the virtual calendar, and permission-protected `/earth sim tick` provides a bounded operational recovery action. Focused clock coverage and the Earth suite passed.

## 83% business-payroll checkpoint

- Retail job work now first debits the registered business owner using the existing EconomyManager, then credits the employee. A business with insufficient funds cannot emit another paid retail work unit.
- Public occupations retain their existing public-economy wage path. Regional production and tax accounting continue to use the same bounded work result.
- The focused Earth regression suite passed after the payroll constraint was added.

## 84% integration-boundary checkpoint

- `EarthMapAdapter` receives region bounds plus prosperity, pollution, approval and infrastructure overlays after bounded simulation cycles; no BlueMap API is imported by core gameplay.
- `EarthNpcAdapter` is reconciled only for online players inside active regions, receiving statistical demographics and conditions. The default implementation is a guarded no-op, so no physical NPCs are created unless a provider is installed and registered.
- Adapter failures are isolated and logged instead of breaking the simulation. The focused Earth suite passed after the boundary was added.

## 85% release-audit finding: country aggregates

- The audit identified that country IDs and metadata existed, but country-level treasury, population, citizen count and economic-state records were missing. `earth_country_statistics` now persists these aggregates and refreshes them from bounded regional state each simulation cycle.
- `/earth country <id>` exposes capital, aggregate treasury, population, citizenship, and economic state for operations and player discovery.
- This is an additive table; existing country identifiers, city links and player data remain untouched. Focused aggregate-bound coverage and the Earth suite passed.

## 85% initial verification audit (resolved at 90%)

- An initial `gradlew test --no-daemon` run compiled all source and test code but found 16 failures. All were outside `features.earth`: 14 plugin/economy/MOTD tests exposed a missing MockBukkit/bootstrap class, and two economy-format expectations were locale-dependent.
- The Earth-focused suite was green. The broader baseline was subsequently repaired at the 86% and 90% checkpoints; the 90% rerun passed all 334 then-current tests.
- No production data was changed to suppress the initial failures.

## 86% test-bootstrap repair

- CraftEngine remains `compileOnly` for production. Its core and Bukkit artifacts are available only to the test runtime, allowing MockBukkit to load the plugin without reintroducing an active CraftEngine runtime dependency.
- `OpenMCContent#createItem` now degrades to no content when CraftEngine's server manager is not initialized (as in MockBukkit), preserving the OpenMC integration boundary and avoiding a plugin-load crash.
- `OMCPluginTest`, `EconomyManagerTest`, `EconomyFormattingTest`, and `MotdUtilsTest` pass together after the repair. A full-suite rerun is still required for certification.

## 90% release-candidate audit

- The former 16-test baseline has been resolved without adding CraftEngine as a production runtime dependency: CraftEngine API artifacts are test-only, and the OpenMC adapter degrades safely when its optional server manager is absent.
- Compact economy formatting is now locale-stable. This removed the order-dependent comma-versus-dot failures from the full suite.
- Verification completed: the 90% `gradlew test --no-daemon` run passed all 334 then-current tests; `gradlew clean shadowJar --no-daemon` passed and produced `builds/OpenMC.jar` (7,053,677 bytes).
- Release-candidate limits remain explicit: production Paper/database migration boot, real geographic dataset import, installed BlueMap/Citizens provider behavior, and Bedrock/Geyser compatibility have not been exercised in this workspace. Property settlement is compensated across the existing memory-first economy rather than transactionally atomic.

## 91% location-service checkpoint

- `EarthLocationService` provides OpenMC-owned `getCountry(Location)`, `getRegion(Location)`, ID lookups, and linked-city lookup APIs. It contains the configured chunk projection and existing city-link semantics in one boundary.
- `/earth status` and `/earth whereami` now use the service instead of embedding chunk lookup logic.

## 92% persistence verification checkpoint

- `EarthPersistenceTest` uses a clean H2 connection source and a second, fresh source to confirm that the persisted virtual calendar, election phase, country aggregate, and property lease/access records can be read after reconnecting.
- This validates ORM mappings and durable state records. It does not substitute for a production Paper server restart against the deployment database, which remains a final external validation item.
- The final workspace rerun of `gradlew test --no-daemon` passed all 335 then-current tests with zero failures and zero errors.

## 93% command-permission checkpoint

- `EarthPermissions` centralizes explicit LuckPerms-compatible nodes for player Earth access, citizenship, civic activity, candidacy, voting, property management/renting, jobs, travel, and each administrator mutation category.
- Earth commands now use `omc.commands.earth` for ordinary access and the existing `omc.admins.commands.*` wildcard namespace for administrative actions; no Earth action relies on the generic `op` node.
- Reflection coverage verifies representative command annotations and the focused permissions test passed.

## 94% configuration-control checkpoint

- `earth.enabled` now defaults to `true` and safely disables Earth command/listener/scheduled-simulation registration when set to `false`; existing persistent data is retained and no tables are dropped.
- The Earth feature gate and explicit-permission coverage compiled and passed in the focused Gradle verification.

## 95% schema-migration checkpoint

- `earth_schema_version` records the applied Earth migration level. `EarthSchemaManager` applies five ordered, additive migrations for the entire Earth table set and rejects a database version newer than the running plugin, avoiding unsafe downgrade writes.
- Repeating the migration preserves existing Earth country data; the H2 migration regression test passed. No migration drops or rewrites existing OpenMC city, home, shop, or economy tables.

## 96% country-metadata checkpoint

- Countries now persist a stable code, government ID, active state, and created/updated timestamps. Configuration can provide `code`, `government-id`, and `active` for every imported country while retaining backward-compatible defaults.
- Schema migration 6 adds those country columns to a pre-existing version-5 Earth database without data loss. The H2 regression test constructs a legacy country table, migrates it, and verifies the original row remains readable.
- The complete workspace rerun of `gradlew test --no-daemon` now passes all 338 tests with zero failures and zero errors.

## 97% government-transition checkpoint

- `earth_governments` now persists the country government ID/type, elected leader, mandate expiry, and transition timestamp. Country bootstrap creates its initial government record without replacing existing municipal systems.
- Closing a persisted election installs its winner into the country government for the configured mandate duration. `/earth government` now reports government leadership and its existing budget policy.
- The focused government transition regression test passed. Schema migration 7 adds this additive Earth-only table.

## 98% elected-government authority checkpoint

- Winning a country election now grants real governance authority: the installed leader can save budget policy and enact or repeal country laws. Administrators retain an explicit `omc.admins.commands.earth.government` bypass for recovery and moderation.
- Policy preview is available to ordinary Earth users, while mutation stays behind the government-management node plus leader/admin authorization. The focused command-permission regression test passed.

## 99% clean release-artifact audit (workspace scope)

- `gradlew clean test shadowJar --no-daemon` completed successfully from a clean workspace in 4m39s. The run compiled source and tests, passed all 339 tests with zero failures/errors, and produced `builds/OpenMC.jar` (7,064,861 bytes).
- Existing compiler deprecation/unchecked notices and Shadow Kotlin metadata duplicate notices remain warnings only; no build task failed.
- This validates the repository artifact. It does not replace the still-required external production Paper/database migration boot, real geographic dataset import, installed BlueMap/NPC provider checks, Java/Bedrock join tests, or stress profiling.

## Runtime Paper boot attempt (not completed)

- `gradlew runServer --no-daemon` downloaded and verified Paper 1.21.7 build 32, started under Java 21, and discovered OpenMC as its sole Paper plugin. OpenMC bootstrap began generating CraftEngine content before server startup reached the EULA gate.
- The server then stopped because `run/eula.txt` does not contain Mojang EULA acceptance. No EULA was accepted or changed on the user’s behalf, so full Paper plugin enable, Earth database startup, command checks, and join/runtime validation remain untested.

## Active-country behavior checkpoint

- Country `active` state now governs behavior rather than only display metadata: inactive countries are excluded from Earth location lookup, citizenship selection, elections, diplomacy mutation, country policy/law actions, and scheduled regional simulation. Their persisted statistics remain intact and are marked inactive for safe reactivation.
- The focused active-country regression test passed.

## Geographic-boundary validation checkpoint

- Active configured regions must now have ordered chunk bounds and cannot overlap any other active Earth region. This makes the location service deterministic at region borders instead of depending on map iteration order.
- Inactive configured countries no longer bootstrap new regional territory. Inclusive overlap, disjoint bounds, and invalid-bound behavior are covered by the focused Earth regression suite.

## Persistent country configuration synchronization checkpoint

- On startup, configured country metadata (code, display/economic/government fields, and especially `active`) now updates the existing Earth country record instead of being ignored after its first database creation. IDs, creation timestamps, citizenships, statistics, and other historical records remain untouched.
- Government identity/type follows configured country metadata without clearing the installed elected leader or its mandate transition. Focused country synchronization coverage passed.

## Political-party election checkpoint

- `earth_political_parties` and `earth_party_memberships` persist player-founded, country-scoped parties and affiliations. Citizens can create or join a party; nomination records the candidate’s active same-country party affiliation.
- Election closure transfers that affiliation into the country government’s persisted `ruling_party_id`, so political identity survives restarts with the leader and mandate. `/earth party`, `party create`, and `party join` expose the player workflow; election output identifies affiliated candidates.
- Schema migration 8 adds the party tables and election/government affiliation columns. Focused model, schema, and command-permission verification passed.

## Bill-to-law causal governance checkpoint

- `earth_bills` persists country law proposals with author, law category, proposed/resolved timestamps, and proposed/passed/rejected lifecycle. Elected leaders (or the explicit administrator bypass) can propose and pass bills; passing activates the existing law’s causal simulation effects.
- `/earth bill`, `bill propose`, and `bill pass` expose the workflow. Duplicate active-law/open-bill proposals are rejected to keep law state coherent. Schema migration 9 adds the Earth-only bill table.
- Focused bill lifecycle, schema, and command-permission verification passed.

## Clean governance release-artifact audit

- After political-party, active-country, geography validation, and bill work, `gradlew clean test shadowJar --no-daemon` again completed successfully in 3m59s. It passed all 344 tests with zero failures/errors and produced `builds/OpenMC.jar` (7,075,703 bytes).
- The same existing compiler/Shadow warnings remain non-fatal. Runtime Paper enable is still constrained by the unaccepted EULA and has not been claimed as passing.

## Transport causal-network checkpoint

- Each bounded simulation cycle aggregates route modes once per endpoint region. Roads, rail, air, and water routes now change regional infrastructure, pollution, and energy; those conditions feed existing fare, productivity, employment, prosperity, and satisfaction effects.
- Rail receives an additional clean-infrastructure benefit while air carries the largest pollution/energy burden. Focused transport causal-effects regression coverage passed.

## Operations documentation checkpoint

- `EARTH_OPERATIONS.md` now documents the implemented Earth configuration, schema versioning, player/government workflow, permission nodes, causal/performance model, and the exact boundary between verified workspace behavior and still-unverified external runtime gates.

## Vacant-election authority checkpoint

- If a persisted voting phase closes with no valid votes, the prior country office is removed and the government clears leader, ruling party, and mandate expiry. This prevents a former leader from retaining policy, bill, or law authority indefinitely after an uncontested failure.
- The focused vacancy lifecycle regression passed.

## Bill-enforced law activation checkpoint

- Elected leaders now activate national laws only through the persisted bill proposal/pass workflow. `/earth law enact` is retained solely as the explicit `omc.admins.commands.earth.government` emergency recovery path, preventing routine governance from bypassing the bill audit trail.
- Focused permission and Earth model verification passed.

## Governance restart-persistence checkpoint

- The H2 reconnection regression now proves that a political party, membership, ruling-party government leader, and passed bill remain readable after a fresh connection source. This extends restart evidence beyond the original clock/election/property/country records.
- Focused governance persistence verification passed.

## Requirements audit checkpoint

- `EARTH_REQUIREMENTS_AUDIT.md` maps the project specification to concrete repository evidence and explicitly separates implemented systems from external validation and unfinished scope. Its completion conclusion remains intentionally non-final until the Paper EULA/runtime and listed external gates are resolved.

## Latest clean verification checkpoint

- `gradlew clean test shadowJar --no-daemon` completed successfully after every current Earth change in 3m54s. The clean run passed all 346 tests with zero failures/errors and produced `builds/OpenMC.jar` (7,076,939 bytes).
- The output contained only the known non-fatal Java deprecation, Gradle deprecation, and Shadow Kotlin-module duplicate warnings. The Paper runtime remains deliberately unclaimed: `run/eula.txt` is still `eula=false` and has not been changed without the user's authorization.

## Cabinet and ministry checkpoint

- Schema migration 10 adds `earth_government_appointments`, keyed by country and portfolio. An elected leader can appoint a same-country citizen through `/earth government appoint`; `/earth government ministries` makes the installed cabinet visible. A new government transition or a vacant election removes the former cabinet.
- Installed portfolios have bounded causal effects: finance improves inflation/debt pressure, health, education, infrastructure, environment, and public-safety portfolios improve their corresponding regional conditions, and labour/foreign-affairs portfolios provide small public-condition effects. Appointments complement—not replace—law and budget effects.
- Focused schema, causal-model, and fresh-H2-connection persistence coverage passed. A clean `gradlew clean test shadowJar --no-daemon` completed in 3m53s with 347 tests, zero failures/errors, and `builds/OpenMC.jar` (7,082,852 bytes).

## Serialized persistence checkpoint

- Earth ORM operations now enter one asynchronous FIFO drain rather than independent scheduler tasks. This preserves the order of rapid updates—for example an appointment replacement, election transition, or property-state change—while keeping database work off Paper's main thread.
- The drain closes the race between its final queue poll and flag release, so a write that arrives during that interval schedules a new drain instead of being stranded. Focused persistence/schema verification passed, followed by a clean `gradlew clean test shadowJar --no-daemon` run in 4m01s: 347 tests passed with zero failures/errors and `builds/OpenMC.jar` was produced (7,083,211 bytes).

## Repeated-cycle stability checkpoint

- A deterministic 2,000-cycle model regression now exercises public budgets, laws, causal indicators, cabinet portfolios, transport effects, and societal feedback together. It verifies that treasury, prosperity, employment, satisfaction, inflation, and debt remain within their defined bounds after repeated updates.
- The focused Earth causal-model suite passed. A clean `gradlew clean test shadowJar --no-daemon` then completed in 4m41s with 348 tests, zero failures/errors, and `builds/OpenMC.jar` (7,083,211 bytes). This remains a numerical-stability check, not a claim of Paper server load/stress validation.

## Property sale and transfer checkpoint

- Schema migration 11 adds restart-safe `earth_property_sale_offers`. Sellers create or withdraw a bounded offer; buyers must explicitly accept it. Expired offers are purged from memory and persistence during bounded simulation cycles.
- Existing Home and Shop ownership changes go through their parent managers and are persisted before payment. Buyer balance is preflighted; a payment failure attempts to restore the parent owner. The Earth overlay then updates ownership/access and removes the accepted offer. This is compensated settlement, not a claim of one cross-feature database transaction.
- `/earth property sale-offer`, `sale-withdraw`, and `buy` complete the player workflow. Focused schema, reconnection persistence, model, and permission coverage passed. A clean `gradlew clean test shadowJar --no-daemon` completed in 4m32s with 349 tests, zero failures/errors, and `builds/OpenMC.jar` (7,087,336 bytes).

## Configurable Earth projection checkpoint

- `earth.scale` is now operational through `EarthLocationService.project(Location)`, yielding east/north Earth metres for map and geographic-import adapters without hardcoding 1:1000 in gameplay. Existing persisted chunk-bound lookup remains unchanged when scale is configured.
- Focused location-service projection coverage passed for default, custom, and invalid-scale clamping. A clean `gradlew clean test shadowJar --no-daemon` completed in 4m30s with 350 tests, zero failures/errors, and `builds/OpenMC.jar` (7,088,761 bytes). This establishes an import/map boundary; it does not claim that a real-world geographic dataset has been loaded.

## 70% architecture audit

- The domain is now a persistent social/economic loop: geography, property, jobs, commerce, public finance, laws, elections, demographics, events, travel, and trade climate use isolated Earth tables.
- Existing OpenMC systems still own claims, balances, homes, shops, and municipal mayor mechanics.
- NPC residents are aggregate records, and periodic work is bounded by configured regions.
- Remaining work is operational hardening, diagnostics, optional map integration, and a requirement-by-requirement final audit.

## 50% architecture audit

- Player actions are authoritative inputs: buying, working, volunteering, nominating, and voting each write persistent Earth state.
- Outcomes are deterministic and bounded: commerce/work create regional accounting; treasury, budgets, laws, and an elected mandate update public metrics on scheduled cycles.
- Existing OpenMC ownership remains intact: City claims, Home storage/teleport, Shop stock/turnover, and Economy balances are never replaced by Earth tables.
- The active gaps are deliberate: no route graph, dynamic regional events, NPC population, or international relations have been implemented yet.
