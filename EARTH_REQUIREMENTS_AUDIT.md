# Earth simulation requirements audit

Audit date: 2026-08-31. This document records repository evidence; it does not claim external runtime checks that have not occurred.

| Requirement area | Repository evidence | Status |
| --- | --- | --- |
| Earth hierarchy and lookup | Persistent country/region/city-link records, active-bound validation, `EarthLocationService`, configurable metre projection | Implemented; geographic dataset import not performed |
| Country metadata | Stable IDs/codes, capital, currency, government ID/type, active state, timestamps, aggregates | Implemented and migration-tested |
| City compatibility | Existing `CityManager` remains authoritative; Earth stores city-to-region links only | Implemented by additive integration |
| Citizenship and society | Persistent citizenship, civic status, occupation, demographics | Implemented |
| Property | Home/shop registration, access modes, rent/lease lifecycle, seller-created/buyer-accepted sale offers, authoritative Home/Shop owner transfer, existing protections remain authoritative | Implemented with compensation; no cross-feature database transaction because the existing economy is memory-first |
| Jobs/business/economy | Existing balances, shops, business payroll, regional ledger/tax and causal conditions | Implemented within existing OpenMC authority boundaries |
| Government/elections | Persistent cycles, candidates/votes, office, parties, government leader/ruling party, cabinet appointments, mandate/vacancy handling | Implemented and focused-tested |
| Bills/laws/budgets | Persisted bill proposal/pass lifecycle, causal law effects, leader authorization, budget preview | Implemented; emergency direct law activation is admin-only |
| Simulation/time/events | Scheduled virtual clock, public finance, demographics, economics, causal indicators, events, visible condition effects | Implemented and model-tested |
| Transport/geopolitics | Routes/travel/waypoints, mode-specific causal network effects, bilateral diplomacy/trade | Implemented as aggregate systems |
| NPC/map integration | OpenMC-owned `EarthNpcAdapter`/`EarthMapAdapter` no-op boundaries | Adapter boundary implemented; no provider installation tested |
| Schema safety | `EarthSchemaManager` versions 1-11, additive migrations, legacy H2 migration/reconnection coverage | Implemented and tested |
| Permissions | Explicit LuckPerms-compatible player/government/admin nodes | Implemented and reflection-tested |
| Performance/threading | Bounded scheduled simulation, route aggregation, cached maps, serialized async persistence, main-thread world effects, 2,000-cycle bounded-model regression | Implemented by source inspection and focused stability/persistence verification; no Paper load profile run |
| CraftEngine migration | Production remains compile-only; test-only API support and Paper bootstrap discovery observed | Build/test verified; full enabled runtime remains pending |

## Verified commands and artifacts

- The latest `gradlew clean test shadowJar --no-daemon` completed successfully after all current Earth changes, with 350 tests passing and zero failures/errors. It produced `builds/OpenMC.jar` (7,088,761 bytes).
- A fresh local Paper 1.21.7 download/boot reached Java 21 startup and OpenMC plugin discovery/bootstrap.
- Current later focused tests also cover vacancy handling, bill enforcement, and governance persistence.

## Not verified or not implemented

1. Full Paper enable, Earth database boot, command execution, player join, restart, and live simulation require accepting the generated Mojang EULA before the local server can run.
2. A real Earth geographic dataset has not been imported; configuration provides the safe country/region import boundary and default sample territory only.
3. BlueMap, Citizens/FancyNpcs provider behavior, Java-player interaction, Bedrock/Geyser interaction, and stress/load profiling have not been exercised.
4. Property settlement is compensated rather than transactionally atomic across Home/Shop and the memory-first economy; an unrecoverable parent rollback failure would require administrator recovery.
5. Physical infrastructure rendering, wars/international organizations, local government tiers, and constitution-specific rules are future extensions, not represented as complete gameplay systems. Country-level portfolio appointments are implemented.

## Completion conclusion

The integrated Earth simulation core is buildable and substantially verified in the workspace, but Definition of Done is **not yet proven** because the runtime EULA gate and the explicit external/provider/geographic validation items above remain outstanding.
