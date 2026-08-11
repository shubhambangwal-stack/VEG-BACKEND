## Phase 1 — Master Product Catalog (this phase)
- Tables: `catalog_categories`, `catalog_subcategories`, `catalog_products` —
  deliberately NOT named `categories`/`products` to avoid colliding with
  Vendor's pre-pivot `products` table (V72). Vendor's table is untouched.
- Category ↔ Subcategory ↔ Product are real 2-level entities with FKs, not
  flat strings (confirmed direction for Phase 1).
- Product image is a plain `imageUrl` string field — no upload/storage
  plumbing built yet. Revisit if/when a real upload mechanism gets decided.
- **No hard-delete endpoint anywhere** — only `PATCH .../status?active=`.
  Deliberate: once Vendor's rework (separate, later phase) lets vendors
  select these products via `isListed`, a hard delete here would orphan
  those references. Deactivate only, forever, unless this gets explicitly
  revisited.
- Deactivating a Category does **not** cascade to deactivate its
  Subcategories/Products automatically — not enforced in Phase 1. Flag this
  if it becomes a real problem; deferred deliberately to keep this phase
  scoped.
- Vendor product-request submission endpoint is explicitly **not** part of
  this phase — see Phase 3, which is the Admin-review-side-only slice of
  that feature (submission itself needs a new Vendor-module endpoint, out of
  scope for Admin work).

## Class-name collision with Vendor module (fixed)
- Admin's Phase 1 catalog originally used `Category`/`Subcategory`/`Product`
  as entity/repository class names. This collided with Vendor's pre-existing
  pre-pivot `Category`/`Product` (same simple names, different packages) —
  Spring Data JPA registers repository beans by simple class name regardless
  of package, so app startup failed with a bean-name conflict.
- **Fixed by renaming**, not by enabling `allow-bean-definition-overriding`:
  `Category`→`CatalogCategory`, `Subcategory`→`CatalogSubcategory`,
  `Product`→`CatalogProduct`, repositories/services renamed to match
  (`CatalogCategoryService`, `CatalogSubcategoryService`, etc.).
  `AdminProductService`/`Impl` kept their names — already distinctly
  `Admin`-prefixed, no collision risk.
- Table names (`catalog_categories` etc., set in V111) were already correct
  and needed no change — only the Java class layer had the collision.
- **Lesson for future modules** (flagging for Payment/Notification too):
  don't assume different packages are enough to avoid naming collisions in
  this codebase — check simple class names against what Vendor/Delivery/
  Customer already use before naming new entities/repos/services, same
  standard already applied to the known `ProductDto`/`ShopDto` duplication.
