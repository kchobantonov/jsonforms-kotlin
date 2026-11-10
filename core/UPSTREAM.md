# JSON Forms core provenance

The initial Kotlin port was based on the JSON Forms core API at **3.8.0-alpha.0**
([upstream tag and commit](https://github.com/eclipsesource/jsonforms/tree/754d9ddd77b86d1bc78ac31d4a21da61f82bc9ff/packages/core)).
The original Kotlin commit did not record an upstream SHA, so this is the closest
verifiable tagged baseline rather than a claim of exact source identity.

The review target is **3.9.0-alpha.2**
([upstream commit](https://github.com/eclipsesource/jsonforms/tree/2c5fae62a8aace19970ac037d122fa6428cd0436/packages/core)).
The latest stable release at review time is 3.8.0. The proposed 3.9.0-beta.2
tag was not present in the checked upstream repository.

## Core changes reviewed

The upstream diff between those commits changes 16 files under `packages/core/src`.
The Kotlin port now handles nullable object UI schema generation, required-property
error paths, canonical array indexes, and reusable ID allocation. Tests cover these
cases. Readonly state, rules, and root schema generation were already represented in
the Kotlin implementation.

Upstream's new `setData` helper protects JavaScript objects from prototype pollution
and preserves sparse-array behavior. Kotlin uses immutable `JsonObject`/`JsonArray`,
which do not have JavaScript prototypes or sparse indexes; `JsonPath` rejects
ambiguous array indexes. AJV state replacement and TypeScript translator typing
are specific to upstream's JavaScript reducer and type system. The Kotlin validator
and translation APIs are separate implementations.
