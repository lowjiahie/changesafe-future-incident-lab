---
description: Analyze a proposed code change for downstream failure risks using the ChangeSafe Future Incident Lab workflow.
---

Activate the `changesafe` skill to begin the 12-phase ChangeSafe workflow.

Provide a change description, diff reference, or requirement. The skill will guide impact analysis,
risk hypotheses, safety contracts, executable regression checks, and evidence-pack delivery before
any application code is modified.

> **Note:** Verify that the ChangeSafe mode and this command appear in the Bob IDE mode picker and
> command palette after saving. If they do not appear, the configuration files may not have been
> discovered by the installed Bob version -- fall back to activating the skill manually by asking
> Bob to "activate the changesafe skill".
