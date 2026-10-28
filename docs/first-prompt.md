/plan Review AGENTS.md, docs/engineering-spec.md, docs/acceptance-tests.md, docs/delivery-plan.md and docs/reference/BusTicketBookingApp_Spec.pdf.
This is a 15-day training project. The PDF is the mandatory baseline; the other docs are approved engineering decisions. Do not silently change them.
First perform a read-only feasibility and consistency audit: validate entity model, overlapping segment locking, multi-leg transaction, route-search time semantics, auth/session/CSRF approach, migrations, and milestone dependencies. Identify any conflicts with the PDF.
Produce a reviewable implementation plan and ADRs, requirement traceability, concrete schema with DDL constraints/indexes, OpenAPI draft, and a test mapping. Separate P0/P1/deferred.
Do not implement application code until I approve the plan. After approval, execute one vertical slice at a time; run actual verification commands and report evidence. Ask only for decisions that block correctness or security.
