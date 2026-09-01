# OpenMC Earth operations guide

This guide describes the currently implemented Earth simulation in PluginV2. It is intentionally limited to behavior present in the repository.

## Enablement and configuration

`config.yml` contains the `earth` section.

- `earth.enabled`: defaults to `true`. Set `false` to retain Earth data while skipping Earth commands, listeners, and the scheduled simulation.
- `earth.scale`: real-world metres per Minecraft block. `EarthLocationService.project(Location)` exposes the configured east/north metre projection for map and import adapters; existing persisted chunk-bound gameplay lookup is intentionally unaffected.
- `earth.simulation-period-ticks`: bounded simulation interval; default `1200` ticks.
- `earth.simulation.real-day-seconds` and `earth.simulation.simulated-months-per-real-day`: accelerated virtual calendar settings. Election timing remains real wall-clock time.
- `earth.elections.*-seconds`: candidacy, campaign, voting, and mandate durations.
- `earth.countries.<id>`: persistent country bootstrap metadata: `code`, `name`, `capital`, `currency`, `government`, `government-id`, `active`, `tax-rate`, budget allocation, and child region bounds.

Country configuration synchronizes into existing Earth country records on startup without changing the country ID, creation time, citizenship, or historical statistics. Active configured regions must have valid, non-overlapping inclusive chunk bounds.

## Persistent schema

`EarthSchemaManager` applies additive, versioned migrations and records the result in `earth_schema_version`. Current version: **11**.

The Earth tables cover countries, regions, statistics, citizenship, properties/access/sale offers, businesses/jobs, regional causal conditions, laws/bills, elections/government/parties/cabinet appointments, routes/travel, demographics, diplomacy, and simulation time. Migrations do not drop or rewrite OpenMC city, home, shop, or economy tables.

## Player workflow

1. Explore an active region with `/earth whereami`, `/earth status`, and `/earth conditions`.
2. Select citizenship with `/earth citizenship <country>`; work, volunteer, own/rent property, trade, and travel using the existing Earth commands.
3. Join or found a party with `/earth party`, `/earth party create <country> <name> <platform>`, or `/earth party join <party-id>`.
4. During candidacy and voting phases, nominate and vote through `/earth election nominate` and `/earth election vote`.
5. The election winner becomes the persisted country government leader; their party becomes the ruling party when affiliated.
6. The leader proposes and passes bills with `/earth bill propose` and `/earth bill pass`. Passing a bill activates the corresponding causal law effects.
7. The leader may appoint a same-country citizen with `/earth government appoint <country> <portfolio> <citizen UUID>`; `/earth government ministries` lists the current cabinet. Appointments clear when the government transitions or becomes vacant.
8. A property owner creates a restart-safe public offer with `/earth property sale-offer <property UUID> <price> <minutes>`. A buyer explicitly settles it with `/earth property buy <property UUID>`; `/earth property sale-withdraw` cancels an unaccepted offer. Home and Shop ownership is persisted by the existing authoritative managers before payment; a failed payment compensates that owner change.

## Permission nodes

Base access is `omc.commands.earth`. Specific player nodes include:

- `omc.commands.earth.citizenship`
- `omc.commands.earth.civic`
- `omc.commands.earth.election.nominate`
- `omc.commands.earth.election.vote`
- `omc.commands.earth.party.manage`
- `omc.commands.earth.government.manage`
- `omc.commands.earth.property.manage`
- `omc.commands.earth.property.purchase`
- `omc.commands.earth.property.rent`
- `omc.commands.earth.job.take`
- `omc.commands.earth.job.work`
- `omc.commands.earth.travel`

Government mutation also checks that the player is the installed country leader. The explicit administrator recovery bypass is `omc.admins.commands.earth.government`. Other administrator categories use the same existing `omc.admins.commands.earth.*` namespace.

## Causal simulation and performance

The simulation is scheduled, not per tick. It operates on cached persistent state and sends database work through one serialized asynchronous queue, preserving mutation order without blocking Paper's main thread. It aggregates jobs, commercial ledger activity, laws, fiscal policy, cabinet portfolios, demographics, events, diplomacy, and route modes.

Transport routes affect infrastructure, pollution, and energy. Those regional conditions feed fares, productivity, employment, prosperity, satisfaction, and visible severe-condition effects. Population/NPC data remains statistical; the NPC and map boundaries are provider-neutral adapters.

## Runtime status and limits

The current workspace passes the clean Gradle build/test/shaded-artifact audit. A Paper 1.21.7 test boot reached OpenMC discovery/bootstrap but halted at the unaccepted Mojang EULA; full Paper enable and live database migration are therefore not claimed as verified.

Installed BlueMap/NPC providers, Bedrock/Geyser, and a real geographic data import remain external or deferred validation items. Property sales use parent-owner persistence plus payment compensation; they are not represented as one cross-feature database transaction because the existing economy is memory-first.
