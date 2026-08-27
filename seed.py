#!/usr/bin/env python3
"""
=============================================================================
VegGo Fresh — Full-Database Seeding Script
=============================================================================
Database: PostgreSQL  (matches application-local.yml)
Purpose : Load stable, never-expiring seed data that covers every realistic
          scenario the app team may need for local/dev testing.

Scenarios covered
-----------------
  AUTH
    • Admin user  (already in Flyway V7 — idempotent)
    • Customer users  (3)
    • Vendor users    (3)
    • Delivery-partner users (3)

  CUSTOMER MODULE
    • customer_profiles for each customer
    • addresses (home + work) per customer
    • delivery_slots  (30 days × 4 slots = 120 rows, always in the future)
    • Carts + cart items pre-loaded with real catalog products
    • Orders in every meaningful status:
        PENDING, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    • order_items on each order
    • ratings  for delivered orders
    • wishlists entries
    • cart_candidate_vendors / order_candidate_vendors

  VENDOR MODULE
    • vendor_shops — KYC states: PENDING, APPROVED, REJECTED
    • vendor documents (PENDING / APPROVED)
    • vendor_operating_hours  (full week for each approved shop)
    • vendor_categories + vendor_products + vendor_inventory_items  (legacy)
    • vendor_listings linked to catalog products
    • vendor_shop_ratings
    • vendor_special_closures

  ADMIN / CATALOG MODULE
    • platform_settings (single-row)
    • catalog_categories  (Vegetables, Fruits, Dairy & Eggs, Grains & Pulses)
    • catalog_subcategories  per category
    • catalog_products  (20 products across subcategories, with discounts)

  DELIVERY MODULE
    • delivery_partners  — KYC states: PENDING, APPROVED, REJECTED
    • delivery documents
    • delivery_assignments — statuses: PENDING, ACCEPTED, PICKED_UP, DELIVERED, REJECTED
    • delivery_assignment_status_history
    • delivery_otps  (PICKUP + DROP, used + unused)
    • delivery_earnings + breakdown
    • delivery_online_sessions
    • delivery_partner_ratings
    • delivery_proof_of_delivery

  PAYMENT MODULE
    • wallets for all users
    • wallet_transactions (CREDIT / DEBIT)
    • payment_orders (CREATED, CAPTURED, FAILED)
    • payment_order_lines
    • payment_webhook_events
    • payout_requests (PENDING, APPROVED, REJECTED)

Usage
-----
  pip install psycopg2-binary   # one-time
  python seed.py [--host HOST] [--port PORT] [--db DB] [--user USER] [--password PASSWORD]

All INSERTs use ON CONFLICT DO NOTHING — safe to re-run as many times as needed.
=============================================================================
"""

import argparse
import sys
import uuid
from datetime import date, datetime, timedelta

try:
    import psycopg2
    from psycopg2.extras import execute_values
except ImportError:
    print("ERROR: psycopg2 not installed.  Run:  pip install psycopg2-binary")
    sys.exit(1)

# ---------------------------------------------------------------------------
# CLI args
# ---------------------------------------------------------------------------
parser = argparse.ArgumentParser(description="VegGo Fresh seed script")
parser.add_argument("--host",     default="localhost")
parser.add_argument("--port",     default=5432, type=int)
parser.add_argument("--db",       default="veggofresh_local")
parser.add_argument("--user",     default="postgres")
parser.add_argument("--password", default="postgres")
parser.add_argument("--keep-data", action="store_true", help="Do not clear existing data before seeding")
parser.add_argument("--clear", action="store_true", help="(Deprecated) Data is now cleared by default")
args = parser.parse_args()

# ---------------------------------------------------------------------------
# Stable UUIDs  (deterministic so reruns are idempotent)
# ---------------------------------------------------------------------------

def uid(name: str) -> str:
    """Generate a deterministic UUID-v5 from a namespace + name."""
    return str(uuid.uuid5(uuid.NAMESPACE_DNS, f"veggofresh.seed.{name}"))


NOW = datetime.utcnow()
TODAY = date.today()

# ── Users ──────────────────────────────────────────────────────────────────
ADMIN_ID          = "e837cfbe-7d6f-474c-8bb3-455b55018b10"   # Flyway V7

CUST1_ID          = uid("customer.1")
CUST2_ID          = uid("customer.2")
CUST3_ID          = uid("customer.3")

VENDOR1_ID        = uid("vendor.1")
VENDOR2_ID        = uid("vendor.2")
VENDOR3_ID        = uid("vendor.3")

DP1_ID            = uid("delivery.partner.1")
DP2_ID            = uid("delivery.partner.2")
DP3_ID            = uid("delivery.partner.3")

# ── Vendor Shops ───────────────────────────────────────────────────────────
SHOP1_ID          = uid("shop.1")   # KYC APPROVED, online
SHOP2_ID          = uid("shop.2")   # KYC APPROVED, offline
SHOP3_ID          = uid("shop.3")   # KYC PENDING

# ── Catalog ────────────────────────────────────────────────────────────────
CAT_VEG_ID        = uid("cat.vegetables")
CAT_FRUIT_ID      = uid("cat.fruits")
CAT_DAIRY_ID      = uid("cat.dairy")
CAT_GRAIN_ID      = uid("cat.grains")

SUBCAT_LEAFY_ID   = uid("subcat.leafy")
SUBCAT_ROOT_ID    = uid("subcat.root")
SUBCAT_CITRUS_ID  = uid("subcat.citrus")
SUBCAT_TROPICAL_ID = uid("subcat.tropical")
SUBCAT_MILK_ID    = uid("subcat.milk")
SUBCAT_EGGS_ID    = uid("subcat.eggs")
SUBCAT_LENTIL_ID  = uid("subcat.lentil")
SUBCAT_RICE_ID    = uid("subcat.rice")

CP1_ID  = uid("cp.spinach")
CP2_ID  = uid("cp.coriander")
CP3_ID  = uid("cp.potato")
CP4_ID  = uid("cp.tomato")
CP5_ID  = uid("cp.orange")
CP6_ID  = uid("cp.lemon")
CP7_ID  = uid("cp.mango")
CP8_ID  = uid("cp.banana")
CP9_ID  = uid("cp.milk.full")
CP10_ID = uid("cp.milk.skim")
CP11_ID = uid("cp.eggs.6")
CP12_ID = uid("cp.eggs.12")
CP13_ID = uid("cp.toor.dal")
CP14_ID = uid("cp.chana.dal")
CP15_ID = uid("cp.basmati.rice")
CP16_ID = uid("cp.brown.rice")
CP17_ID = uid("cp.onion")
CP18_ID = uid("cp.carrot")
CP19_ID = uid("cp.apple")
CP20_ID = uid("cp.cucumber")

ALL_CATALOG_PRODUCTS = [
    CP1_ID, CP2_ID, CP3_ID, CP4_ID, CP5_ID,
    CP6_ID, CP7_ID, CP8_ID, CP9_ID, CP10_ID,
    CP11_ID, CP12_ID, CP13_ID, CP14_ID, CP15_ID,
    CP16_ID, CP17_ID, CP18_ID, CP19_ID, CP20_ID,
]

# ── Legacy vendor products ─────────────────────────────────────────────────
VCAT1_ID  = uid("vcat.vegetables")
VPROD1_ID = uid("vprod.spinach")
VPROD2_ID = uid("vprod.potato")
VINV1_ID  = uid("vinv.spinach")
VINV2_ID  = uid("vinv.potato")

# ── Customer data ──────────────────────────────────────────────────────────
CPROF1_ID  = uid("cprofile.1")
CPROF2_ID  = uid("cprofile.2")
CPROF3_ID  = uid("cprofile.3")

ADDR1_ID   = uid("addr.1.home")
ADDR2_ID   = uid("addr.1.work")
ADDR3_ID   = uid("addr.2.home")
ADDR4_ID   = uid("addr.3.home")

# Delivery slots — generated dynamically below (120 slots)

CART1_ID   = uid("cart.1")
CART2_ID   = uid("cart.2")
CART3_ID   = uid("cart.3")

CARTITEM1_ID = uid("ci.1")
CARTITEM2_ID = uid("ci.2")
CARTITEM3_ID = uid("ci.3")

ORDER1_ID  = uid("order.1")  # PLACED
ORDER2_ID  = uid("order.2")  # CONFIRMED
ORDER3_ID  = uid("order.3")  # PREPARING
ORDER4_ID  = uid("order.4")  # OUT_FOR_DELIVERY
ORDER5_ID  = uid("order.5")  # DELIVERED
ORDER6_ID  = uid("order.6")  # CANCELLED
ORDER7_ID  = uid("order.7")  # DELIVERED (cust2)

OITEM1_ID  = uid("oi.1")
OITEM2_ID  = uid("oi.2")
OITEM3_ID  = uid("oi.3")
OITEM4_ID  = uid("oi.4")
OITEM5_ID  = uid("oi.5")
OITEM6_ID  = uid("oi.6")
OITEM7_ID  = uid("oi.7")
OITEM8_ID  = uid("oi.8")

RATING1_ID = uid("rating.order5")
RATING2_ID = uid("rating.order7")

WISH1_ID   = uid("wish.1")
WISH2_ID   = uid("wish.2")

# ── Vendor listings ────────────────────────────────────────────────────────
VL1_ID = uid("vl.shop1.cp1")
VL2_ID = uid("vl.shop1.cp3")
VL3_ID = uid("vl.shop1.cp9")
VL4_ID = uid("vl.shop2.cp5")
VL5_ID = uid("vl.shop2.cp7")

# ── Vendor docs ────────────────────────────────────────────────────────────
VDOC1_ID = uid("vdoc.1.gst")
VDOC2_ID = uid("vdoc.1.pan")
VDOC3_ID = uid("vdoc.2.gst")
VDOC4_ID = uid("vdoc.3.gst")

# ── Vendor operating hours ─────────────────────────────────────────────────
DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]

# ── Vendor shop ratings ────────────────────────────────────────────────────
VSRATING1_ID = uid("vs.rating.1")
VSRATING2_ID = uid("vs.rating.2")

# ── Vendor special closure ─────────────────────────────────────────────────
VCLOSE1_ID = uid("vclose.1")

# ── Delivery partners ──────────────────────────────────────────────────────
DP_PROFILE1_ID = uid("dp.profile.1")   # user_id IS the fk in delivery_partners
DP_PROFILE2_ID = uid("dp.profile.2")
DP_PROFILE3_ID = uid("dp.profile.3")

DDOC1_ID = uid("ddoc.1.dl")
DDOC2_ID = uid("ddoc.1.rc")
DDOC3_ID = uid("ddoc.2.dl")

DA1_ID   = uid("da.1")   # PENDING
DA2_ID   = uid("da.2")   # ACCEPTED
DA3_ID   = uid("da.3")   # PICKED_UP
DA4_ID   = uid("da.4")   # DELIVERED
DA5_ID   = uid("da.5")   # REJECTED
DA6_ID   = uid("da.6")   # DELIVERED (order7)

DASH1_ID = uid("dash.1")
DASH2_ID = uid("dash.2")
DASH3_ID = uid("dash.3")
DASH4_ID = uid("dash.4")

DOTP1_ID = uid("dotp.1.drop")
DOTP2_ID = uid("dotp.2.pickup")
DOTP3_ID = uid("dotp.2.drop")
DOTP4_ID = uid("dotp.4.drop")
DOTP5_ID = uid("dotp.6.drop")

DE1_ID   = uid("de.1")
DE2_ID   = uid("de.2")

DSESS1_ID = uid("dsess.1")
DSESS2_ID = uid("dsess.2")

DRATING1_ID = uid("drating.1")
DPOD1_ID    = uid("dpod.1")
DPOD2_ID    = uid("dpod.2")

# ── Payment ────────────────────────────────────────────────────────────────
WALLET_ADMIN_ID  = uid("wallet.admin")
WALLET_C1_ID     = uid("wallet.c1")
WALLET_C2_ID     = uid("wallet.c2")
WALLET_C3_ID     = uid("wallet.c3")
WALLET_V1_ID     = uid("wallet.v1")
WALLET_V2_ID     = uid("wallet.v2")
WALLET_DP1_ID    = uid("wallet.dp1")
WALLET_DP2_ID    = uid("wallet.dp2")

WT1_ID = uid("wt.1")
WT2_ID = uid("wt.2")
WT3_ID = uid("wt.3")
WT4_ID = uid("wt.4")

PO1_ID = uid("po.1")   # CAPTURED
PO2_ID = uid("po.2")   # CREATED
PO3_ID = uid("po.3")   # FAILED

POL1_ID = uid("pol.1")
POL2_ID = uid("pol.2")
POL3_ID = uid("pol.3")

PWE1_ID = uid("pwe.1")
PWE2_ID = uid("pwe.2")

PR1_ID = uid("pr.1")   # PENDING payout
PR2_ID = uid("pr.2")   # APPROVED payout

# ── Platform settings ──────────────────────────────────────────────────────
PLATFORM_SETTINGS_ID = uid("platform.settings")

# ---------------------------------------------------------------------------
# Connection helper
# ---------------------------------------------------------------------------
def connect():
    return psycopg2.connect(
        host=args.host, port=args.port,
        dbname=args.db, user=args.user, password=args.password,
    )

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def clear_db(cur):
    print("\n── CLEARING EXISTING DATA ──────────────────────────────────────────")
    cur.execute("""
        TRUNCATE TABLE 
            customer_profiles, addresses, delivery_slots, carts, cart_items, cart_candidate_vendors,
            orders, order_items, order_candidate_vendors, ratings, wishlists, 
            vendor_shops, vendor_documents, vendor_operating_hours, 
            vendor_categories, vendor_products, vendor_inventory_items, 
            vendor_listings, vendor_shop_ratings, vendor_special_closures,
            catalog_categories, catalog_subcategories, catalog_products, platform_settings,
            delivery_partners, delivery_documents, delivery_assignments, 
            delivery_assignment_status_history, delivery_otps, delivery_earnings, 
            delivery_online_sessions, delivery_partner_ratings, delivery_proof_of_delivery,
            wallets, wallet_transactions, payment_orders, payment_order_lines, 
            payment_webhook_events, payout_requests, refresh_tokens, otp_verifications
        CASCADE;
    """)
    # Delete all users except the Flyway-seeded admin
    cur.execute("DELETE FROM users WHERE role != 'ADMIN'")
    log("All non-admin data truncated successfully.")

def run(cur, sql: str, params=None):
    cur.execute(sql, params)

def run_many(cur, sql: str, rows):
    if rows:
        execute_values(cur, sql, rows)

def ts(delta_seconds: int = 0) -> datetime:
    return NOW + timedelta(seconds=delta_seconds)

def log(msg: str):
    print(f"  ✓  {msg}")

# ---------------------------------------------------------------------------
# Seeding functions  (one per module for clarity)
# ---------------------------------------------------------------------------

def seed_users(cur):
    """AUTH MODULE — users table (admin already seeded by Flyway V7)."""

    # bcrypt hash of "Password@123" (cost 12)
    BCR = "$2a$12$0xtehnDIDPxxlEDcdsvi7uznca3VImlRmHHi7NtD7K0l9hMlT0hqW"

    rows = [
        # (id, phone, email, password, role, is_verified)
        (CUST1_ID,  "+919876543201", "customer1@veggofresh.dev", BCR, "CUSTOMER", True),
        (CUST2_ID,  "+919876543202", "customer2@veggofresh.dev", BCR, "CUSTOMER", True),
        (CUST3_ID,  "+919876543203", "customer3@veggofresh.dev", BCR, "CUSTOMER", False),
        (VENDOR1_ID, "+918765432101", "vendor1@veggofresh.dev",   BCR, "VENDOR",   True),
        (VENDOR2_ID, "+918765432102", "vendor2@veggofresh.dev",   BCR, "VENDOR",   True),
        (VENDOR3_ID, "+918765432103", "vendor3@veggofresh.dev",   BCR, "VENDOR",   False),
        (DP1_ID,    "+917654321001", "dp1@veggofresh.dev",        BCR, "DELIVERY", True),
        (DP2_ID,    "+917654321002", "dp2@veggofresh.dev",        BCR, "DELIVERY", True),
        (DP3_ID,    "+917654321003", "dp3@veggofresh.dev",        BCR, "DELIVERY", False),
    ]
    sql = """
        INSERT INTO users (id, created_at, updated_at, deleted_at, version,
                           phone, email, password, role, is_verified, is_blocked)
        VALUES %s
        ON CONFLICT DO NOTHING
    """
    run_many(cur, sql, [
        (r[0], NOW, NOW, None, 0, r[1], r[2], r[3], r[4], r[5], False)
        for r in rows
    ])
    log("users (9 seed users)")


def seed_customer_module(cur):
    """CUSTOMER MODULE — profiles, addresses, delivery_slots, carts, orders, etc."""

    # ── customer_profiles ──────────────────────────────────────────────────
    profiles = [
        (CPROF1_ID, CUST1_ID, "Rohan Sharma",  None),
        (CPROF2_ID, CUST2_ID, "Priya Nair",    None),
        (CPROF3_ID, CUST3_ID, "Arjun Mehta",   None),
    ]
    run_many(cur, """
        INSERT INTO customer_profiles (id, created_at, updated_at, deleted_at, version,
                                       user_id, full_name, avatar_url)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(p[0], NOW, NOW, None, 0, p[1], p[2], p[3]) for p in profiles])
    log("customer_profiles")

    # ── addresses ─────────────────────────────────────────────────────────
    addresses = [
        (ADDR1_ID, CUST1_ID, "12 MG Road",     None,           "Bengaluru", "Karnataka", "560001", 12.9716, 77.5946, True,  "Home"),
        (ADDR2_ID, CUST1_ID, "Infosys Campus",  "Block A",      "Bengaluru", "Karnataka", "560100", 12.9352, 77.6245, False, "Work"),
        (ADDR3_ID, CUST2_ID, "45 FC Road",      "Apt 3B",       "Pune",      "Maharashtra","411004", 18.5204, 73.8567, True,  "Home"),
        (ADDR4_ID, CUST3_ID, "8 Park Street",   None,           "Mumbai",    "Maharashtra","400001", 18.9334, 72.8299, True,  "Home"),
    ]
    run_many(cur, """
        INSERT INTO addresses (id, created_at, updated_at, deleted_at, version,
                               user_id, address_line1, address_line2,
                               city, state, postal_code,
                               latitude, longitude, is_default, label)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(a[0], NOW, NOW, None, 0, a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10])
          for a in addresses])
    log("addresses")

    # ── delivery_slots — 30 days, 4 slots per day, never expiring in data ──
    SLOT_TIMES = [
        ("09:00", "11:00", "09:00 - 11:00"),
        ("11:00", "13:00", "11:00 - 13:00"),
        ("14:00", "16:00", "14:00 - 16:00"),
        ("17:00", "19:00", "17:00 - 19:00"),
    ]
    slot_rows = []
    for day_offset in range(0, 30):
        slot_date = TODAY + timedelta(days=day_offset)
        for slot_idx, (st, et, label) in enumerate(SLOT_TIMES):
            slot_id = uid(f"slot.{day_offset}.{slot_idx}")
            slot_rows.append((slot_id, NOW, NOW, None, 0, slot_date, st, et, label, True))
    run_many(cur, """
        INSERT INTO delivery_slots (id, created_at, updated_at, deleted_at, version,
                                    slot_date, start_time, end_time, label, is_available)
        VALUES %s ON CONFLICT DO NOTHING
    """, slot_rows)
    log(f"delivery_slots ({len(slot_rows)} rows — 30 days × 4 slots)")

    # ── carts ──────────────────────────────────────────────────────────────
    run_many(cur, """
        INSERT INTO carts (id, created_at, updated_at, deleted_at, version,
                           user_id, promo_code, promo_discount)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (CART1_ID, NOW, NOW, None, 0, CUST1_ID, "FRESH10", 10.00),
        (CART2_ID, NOW, NOW, None, 0, CUST2_ID, None, None),
        (CART3_ID, NOW, NOW, None, 0, CUST3_ID, None, None),
    ])
    log("carts")

    # ── cart_items ─────────────────────────────────────────────────────────
    run_many(cur, """
        INSERT INTO cart_items (id, created_at, updated_at, deleted_at, version,
                                cart_id, product_id, quantity)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (CARTITEM1_ID, NOW, NOW, None, 0, CART1_ID, CP1_ID, 2),
        (CARTITEM2_ID, NOW, NOW, None, 0, CART1_ID, CP3_ID, 5),
        (CARTITEM3_ID, NOW, NOW, None, 0, CART2_ID, CP9_ID, 1),
    ])
    log("cart_items")

    # ── cart_candidate_vendors ─────────────────────────────────────────────
    run_many(cur, """
        INSERT INTO cart_candidate_vendors (cart_id, vendor_id)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (CART1_ID, SHOP1_ID),
        (CART2_ID, SHOP2_ID),
    ])
    log("cart_candidate_vendors")

    # ── orders ─────────────────────────────────────────────────────────────
    DELIVERY_ADDR = "12 MG Road, Bengaluru, Karnataka 560001"
    orders = [
        # (id, user_id, order_number, status, total_amount, delivery_fee, tax,
        #  delivery_addr, lat, lon, source_cart_id, accepted_shop_id,
        #  confirmed_at, preparing_at, out_at, delivered_at, cancelled_at)
        (ORDER1_ID, CUST1_ID, "VGF-0001", "PLACED",           155.00, 20.00, 5.00, DELIVERY_ADDR, 12.9716, 77.5946, CART1_ID, None,    None, None, None, None, None),
        (ORDER2_ID, CUST1_ID, "VGF-0002", "CONFIRMED",         85.00, 15.00, 3.50, DELIVERY_ADDR, 12.9716, 77.5946, None,     SHOP1_ID, ts(-3600), None, None, None, None),
        (ORDER3_ID, CUST1_ID, "VGF-0003", "PREPARING",        200.00, 20.00, 8.00, DELIVERY_ADDR, 12.9716, 77.5946, None,     SHOP1_ID, ts(-7200), ts(-3600), None, None, None),
        (ORDER4_ID, CUST2_ID, "VGF-0004", "OUT_FOR_DELIVERY",  99.00, 15.00, 4.00, "45 FC Road, Pune 411004", 18.5204, 73.8567, None, SHOP1_ID, ts(-10800), ts(-7200), ts(-3600), None, None),
        (ORDER5_ID, CUST1_ID, "VGF-0005", "DELIVERED",        320.00, 20.00, 12.00, DELIVERY_ADDR, 12.9716, 77.5946, None, SHOP1_ID, ts(-86400), ts(-82800), ts(-79200), ts(-75600), None),
        (ORDER6_ID, CUST2_ID, "VGF-0006", "CANCELLED",         60.00, 15.00, 2.50, "45 FC Road, Pune 411004", 18.5204, 73.8567, None, None, None, None, None, None, ts(-43200)),
        (ORDER7_ID, CUST2_ID, "VGF-0007", "DELIVERED",        180.00, 20.00, 7.00, "45 FC Road, Pune 411004", 18.5204, 73.8567, None, SHOP2_ID, ts(-172800), ts(-169200), ts(-165600), ts(-162000), None),
    ]
    run_many(cur, """
        INSERT INTO orders (id, created_at, updated_at, deleted_at, version,
                            user_id, order_number, status, total_amount,
                            delivery_fee, estimated_tax,
                            delivery_address, latitude, longitude,
                            source_cart_id, accepted_shop_id,
                            confirmed_at, preparing_at, out_for_delivery_at,
                            delivered_at, cancelled_at)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(o[0], NOW, NOW, None, 0, o[1], o[2], o[3], o[4], o[5], o[6],
           o[7], o[8], o[9], o[10], o[11], o[12], o[13], o[14], o[15], o[16])
          for o in orders])
    log("orders (7 — one per status scenario)")

    # ── order_candidate_vendors ────────────────────────────────────────────
    run_many(cur, """
        INSERT INTO order_candidate_vendors (order_id, vendor_id)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (ORDER1_ID, SHOP1_ID),
        (ORDER1_ID, SHOP2_ID),
        (ORDER2_ID, SHOP1_ID),
    ])
    log("order_candidate_vendors")

    # ── order_items ────────────────────────────────────────────────────────
    run_many(cur, """
        INSERT INTO order_items (id, created_at, updated_at, deleted_at, version,
                                 order_id, product_id, quantity, price, unit)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (OITEM1_ID, NOW, NOW, None, 0, ORDER1_ID, CP1_ID,  2,  40.00, "250g"),
        (OITEM2_ID, NOW, NOW, None, 0, ORDER1_ID, CP3_ID,  5,  25.00, "1kg"),
        (OITEM3_ID, NOW, NOW, None, 0, ORDER2_ID, CP9_ID,  1,  85.00, "1L"),
        (OITEM4_ID, NOW, NOW, None, 0, ORDER3_ID, CP15_ID, 2, 100.00, "1kg"),
        (OITEM5_ID, NOW, NOW, None, 0, ORDER4_ID, CP5_ID,  4,  24.75, "500g"),
        (OITEM6_ID, NOW, NOW, None, 0, ORDER5_ID, CP7_ID,  3,  90.00, "1kg"),
        (OITEM7_ID, NOW, NOW, None, 0, ORDER6_ID, CP11_ID, 2,  30.00, "6pcs"),
        (OITEM8_ID, NOW, NOW, None, 0, ORDER7_ID, CP13_ID, 2,  90.00, "500g"),
    ])
    log("order_items")

    # ── ratings (one per DELIVERED order) ─────────────────────────────────
    run_many(cur, """
        INSERT INTO ratings (id, created_at, updated_at, deleted_at, version,
                             order_id, rating_value, comment)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (RATING1_ID, NOW, NOW, None, 0, ORDER5_ID, 5, "Fresh vegetables, super fast delivery!"),
        (RATING2_ID, NOW, NOW, None, 0, ORDER7_ID, 4, "Good quality, packaging could be better."),
    ])
    log("ratings")

    # ── wishlists ──────────────────────────────────────────────────────────
    run_many(cur, """
        INSERT INTO wishlists (id, created_at, updated_at, deleted_at, version,
                               user_id, product_id)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (WISH1_ID, NOW, NOW, None, 0, CUST1_ID, CP7_ID),
        (WISH2_ID, NOW, NOW, None, 0, CUST2_ID, CP15_ID),
    ])
    log("wishlists")


def seed_catalog(cur):
    """ADMIN MODULE — catalog_categories, catalog_subcategories, catalog_products."""

    # categories
    cats = [
        (CAT_VEG_ID,   "Vegetables",     "Fresh farm vegetables",           1),
        (CAT_FRUIT_ID, "Fruits",          "Seasonal and exotic fruits",       2),
        (CAT_DAIRY_ID, "Dairy & Eggs",   "Milk, paneer, curd, eggs",        3),
        (CAT_GRAIN_ID, "Grains & Pulses","Rice, lentils and whole grains",  4),
    ]
    run_many(cur, """
        INSERT INTO catalog_categories (id, created_at, updated_at, deleted_at, version,
                                        name, description, image_url, display_order, is_active)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(c[0], NOW, NOW, None, 0, c[1], c[2], None, c[3], True) for c in cats])
    log("catalog_categories")

    # subcategories
    subcats = [
        (SUBCAT_LEAFY_ID,    CAT_VEG_ID,   "Leafy Greens",    1),
        (SUBCAT_ROOT_ID,     CAT_VEG_ID,   "Root Vegetables", 2),
        (SUBCAT_CITRUS_ID,   CAT_FRUIT_ID, "Citrus Fruits",   1),
        (SUBCAT_TROPICAL_ID, CAT_FRUIT_ID, "Tropical Fruits", 2),
        (SUBCAT_MILK_ID,     CAT_DAIRY_ID, "Milk",            1),
        (SUBCAT_EGGS_ID,     CAT_DAIRY_ID, "Eggs",            2),
        (SUBCAT_LENTIL_ID,   CAT_GRAIN_ID, "Lentils & Dal",   1),
        (SUBCAT_RICE_ID,     CAT_GRAIN_ID, "Rice",            2),
    ]
    run_many(cur, """
        INSERT INTO catalog_subcategories (id, created_at, updated_at, deleted_at, version,
                                           category_id, name, display_order, is_active)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(s[0], NOW, NOW, None, 0, s[1], s[2], s[3], True) for s in subcats])
    log("catalog_subcategories")

    # catalog_products  (name, description, cat, subcat, price, original_price, unit)
    products = [
        (CP1_ID,  "Palak (Spinach)",     "Farm-fresh spinach",        CAT_VEG_ID,   SUBCAT_LEAFY_ID,    40.00, 50.00,  "250g"),
        (CP2_ID,  "Coriander",           "Fresh coriander leaves",    CAT_VEG_ID,   SUBCAT_LEAFY_ID,    15.00, None,   "100g"),
        (CP3_ID,  "Potato",              "Washed baby potatoes",      CAT_VEG_ID,   SUBCAT_ROOT_ID,     25.00, 30.00,  "1kg"),
        (CP4_ID,  "Tomato",              "Ripe red tomatoes",         CAT_VEG_ID,   SUBCAT_ROOT_ID,     30.00, None,   "500g"),
        (CP5_ID,  "Orange",              "Nagpur seedless oranges",   CAT_FRUIT_ID, SUBCAT_CITRUS_ID,   80.00, 100.00, "500g"),
        (CP6_ID,  "Lemon",               "Juicy limes",               CAT_FRUIT_ID, SUBCAT_CITRUS_ID,   20.00, None,   "200g"),
        (CP7_ID,  "Alphonso Mango",      "Premium Ratnagiri mangoes", CAT_FRUIT_ID, SUBCAT_TROPICAL_ID, 150.00,180.00, "1kg"),
        (CP8_ID,  "Banana",              "Robusta bananas",           CAT_FRUIT_ID, SUBCAT_TROPICAL_ID, 35.00, None,   "6pcs"),
        (CP9_ID,  "Full Cream Milk",     "Pasteurised full-fat milk", CAT_DAIRY_ID, SUBCAT_MILK_ID,     85.00, None,   "1L"),
        (CP10_ID, "Skimmed Milk",        "Low-fat toned milk",        CAT_DAIRY_ID, SUBCAT_MILK_ID,     75.00, None,   "1L"),
        (CP11_ID, "Eggs (Pack of 6)",    "Free-range white eggs",     CAT_DAIRY_ID, SUBCAT_EGGS_ID,     60.00, 70.00,  "6pcs"),
        (CP12_ID, "Eggs (Pack of 12)",   "Free-range white eggs",     CAT_DAIRY_ID, SUBCAT_EGGS_ID,     110.00,130.00, "12pcs"),
        (CP13_ID, "Toor Dal",            "Split pigeon peas",         CAT_GRAIN_ID, SUBCAT_LENTIL_ID,   90.00, 110.00, "500g"),
        (CP14_ID, "Chana Dal",           "Split chickpeas",           CAT_GRAIN_ID, SUBCAT_LENTIL_ID,   85.00, None,   "500g"),
        (CP15_ID, "Basmati Rice",        "Long-grain aged basmati",   CAT_GRAIN_ID, SUBCAT_RICE_ID,     120.00,140.00, "1kg"),
        (CP16_ID, "Brown Rice",          "Whole grain brown rice",    CAT_GRAIN_ID, SUBCAT_RICE_ID,     95.00, None,   "1kg"),
        (CP17_ID, "Onion",               "Red onions",                CAT_VEG_ID,   SUBCAT_ROOT_ID,     20.00, None,   "500g"),
        (CP18_ID, "Carrot",              "Fresh tender carrots",      CAT_VEG_ID,   SUBCAT_ROOT_ID,     35.00, 40.00,  "500g"),
        (CP19_ID, "Apple (Shimla)",      "Red & green shimla apples", CAT_FRUIT_ID, SUBCAT_TROPICAL_ID, 130.00,150.00, "500g"),
        (CP20_ID, "Cucumber",            "Crisp salad cucumbers",     CAT_VEG_ID,   SUBCAT_ROOT_ID,     25.00, None,   "500g"),
    ]
    run_many(cur, """
        INSERT INTO catalog_products (id, created_at, updated_at, deleted_at, version,
                                      name, description, category_id, subcategory_id,
                                      price, original_price, image_url, unit, is_active)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(p[0], NOW, NOW, None, 0, p[1], p[2], p[3], p[4], p[5], p[6], None, p[7], True)
          for p in products])
    log("catalog_products (20 items)")

    # platform_settings
    run(cur, """
        INSERT INTO platform_settings (id, delivery_radius_km, platform_commission_percent,
                                       vendor_accept_timeout_seconds,
                                       delivery_accept_timeout_seconds,
                                       rebroadcast_max_rounds, rebroadcast_max_elapsed_minutes,
                                       created_at, updated_at, deleted_at)
        VALUES (%s, 12.0, 10.00, 300, 60, 5, 30, %s, %s, NULL)
        ON CONFLICT DO NOTHING
    """, (PLATFORM_SETTINGS_ID, NOW, NOW))
    log("platform_settings")


def seed_vendor_module(cur):
    """VENDOR MODULE — shops, documents, operating hours, listings, ratings."""

    # vendor_shops
    shops = [
        # (id, owner_user_id, name, address, lat, lon, kyc_status, is_online,
        #  full_name, email, business_phone, business_type,
        #  has_basic_info, street, city, state, zip, has_loc, delivery_range_km,
        #  payment_configured)
        (SHOP1_ID, VENDOR1_ID, "Green Basket Organics", "12 Gandhi Nagar, Bengaluru", 12.9712, 77.5943, "APPROVED", True,
         "Anita Rao",   "anita@greenbask.in", "+918011223344", "Organic Produce",
         True, "12 Gandhi Nagar", "Bengaluru", "Karnataka", "560029", True, 8.0, True),
        (SHOP2_ID, VENDOR2_ID, "Farm To Table",          "56 Shivaji Road, Pune",      18.5199, 73.8560, "APPROVED", False,
         "Raju Patil",  "raju@farmtotable.in", "+919922334455", "Vegetable Wholesaler",
         True, "56 Shivaji Road", "Pune", "Maharashtra", "411005", True, 10.0, True),
        (SHOP3_ID, VENDOR3_ID, "Fresh Picks",            "8 Park Ave, Mumbai",         18.9330, 72.8295, "PENDING",  False,
         "Meera Shah",  "meera@freshpicks.in", "+917733445566", "Grocery",
         True, "8 Park Ave", "Mumbai", "Maharashtra", "400001", False, None, False),
    ]
    run_many(cur, """
        INSERT INTO vendor_shops (id, created_at, updated_at, deleted_at, version,
                                  owner_user_id, name, address, latitude, longitude,
                                  kyc_status, is_online,
                                  full_name, email, business_phone, business_type,
                                  has_basic_info, street_address, city, state, zip_code,
                                  has_business_location, delivery_range_km,
                                  payment_settings_configured,
                                  application_submitted_at, kyc_rejection_reason)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(s[0], NOW, NOW, None, 0, s[1], s[2], s[3], s[4], s[5], s[6], s[7],
           s[8], s[9], s[10], s[11], s[12], s[13], s[14], s[15], s[16], s[17], s[18], s[19],
           ts(-86400), None)
          for s in shops])
    log("vendor_shops")

    # vendor_documents
    vdocs = [
        (VDOC1_ID, SHOP1_ID, "GST_CERTIFICATE", "APPROVED"),
        (VDOC2_ID, SHOP1_ID, "PAN_CARD",         "APPROVED"),
        (VDOC3_ID, SHOP2_ID, "GST_CERTIFICATE", "APPROVED"),
        (VDOC4_ID, SHOP3_ID, "GST_CERTIFICATE", "PENDING"),
    ]
    run_many(cur, """
        INSERT INTO vendor_documents (id, created_at, updated_at, deleted_at, version,
                                      shop_id, document_type, status, file_url)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(d[0], NOW, NOW, None, 0, d[1], d[2], d[3],
           f"https://cdn.veggofresh.dev/docs/{d[0]}.pdf") for d in vdocs])
    log("vendor_documents")

    # vendor_operating_hours (Mon-Sun for shop1 and shop2)
    oh_rows = []
    for shop_id in [SHOP1_ID, SHOP2_ID]:
        for day in DAYS:
            oh_id = uid(f"oh.{shop_id}.{day}")
            is_open = day not in ["SUNDAY"]
            oh_rows.append((oh_id, NOW, NOW, None, 0, shop_id, day, is_open,
                             "08:00" if is_open else None,
                             "21:00" if is_open else None))
    run_many(cur, """
        INSERT INTO vendor_operating_hours (id, created_at, updated_at, deleted_at, version,
                                            shop_id, day_of_week, is_open, open_time, close_time)
        VALUES %s ON CONFLICT DO NOTHING
    """, oh_rows)
    log(f"vendor_operating_hours ({len(oh_rows)} rows)")

    # vendor_special_closures
    # Table schema: id, created_at, updated_at, deleted_at, version, shop_id, name, start_date, end_date
    run(cur, """
        INSERT INTO vendor_special_closures (id, created_at, updated_at, deleted_at, version,
                                             shop_id, name, start_date, end_date)
        VALUES (%s, %s, %s, NULL, 0, %s, %s, %s, %s)
        ON CONFLICT DO NOTHING
    """, (VCLOSE1_ID, NOW, NOW, SHOP1_ID, "National Holiday",
          TODAY + timedelta(days=7), TODAY + timedelta(days=7)))
    log("vendor_special_closures")

    # legacy vendor_categories + vendor_products + vendor_inventory_items
    run(cur, """
        INSERT INTO vendor_categories (id, created_at, updated_at, deleted_at, version,
                                       name, description, is_active, icon_url)
        VALUES (%s, %s, %s, NULL, 0, %s, %s, TRUE, NULL) ON CONFLICT DO NOTHING
    """, (VCAT1_ID, NOW, NOW, "Vegetables", "Assorted fresh vegetables"))

    run_many(cur, """
        INSERT INTO vendor_products (id, created_at, updated_at, deleted_at, version,
                                     shop_id, category_id, name, description, price,
                                     image_url, is_active,
                                     unit, is_best_seller, discount_percent, badge,
                                     why_its_great, storage_tips)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (VPROD1_ID, NOW, NOW, None, 0, SHOP1_ID, VCAT1_ID, "Spinach Bunch",  "Fresh spinach", 40.00, None, True, "250g", True,  20, "Organic", "Straight from the farm", "Store in fridge"),
        (VPROD2_ID, NOW, NOW, None, 0, SHOP1_ID, VCAT1_ID, "Baby Potatoes",  "Washed potatoes",25.00,None, True, "1kg",  False, None,None, None, None),
    ])

    run_many(cur, """
        INSERT INTO vendor_inventory_items (id, created_at, updated_at, deleted_at, version,
                                            product_id, stock_quantity, low_stock_threshold)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (VINV1_ID, NOW, NOW, None, 0, VPROD1_ID, 150, 20),
        (VINV2_ID, NOW, NOW, None, 0, VPROD2_ID, 300, 50),
    ])
    log("legacy vendor_products + vendor_inventory_items")

    # vendor_listings (bridge to catalog)
    listings = [
        (VL1_ID, SHOP1_ID, CP1_ID,  True),
        (VL2_ID, SHOP1_ID, CP3_ID,  True),
        (VL3_ID, SHOP1_ID, CP9_ID,  True),
        (VL4_ID, SHOP2_ID, CP5_ID,  True),
        (VL5_ID, SHOP2_ID, CP7_ID,  False),  # listed but not active
    ]
    run_many(cur, """
        INSERT INTO vendor_listings (id, shop_id, catalog_product_id, is_listed,
                                     created_at, updated_at, deleted_at)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(l[0], l[1], l[2], l[3], NOW, NOW, None) for l in listings])
    log("vendor_listings")

    # vendor_shop_ratings
    run_many(cur, """
        INSERT INTO vendor_shop_ratings (id, created_at, updated_at, deleted_at, version,
                                         order_id, shop_id, customer_user_id,
                                         rating_value, comment)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (VSRATING1_ID, NOW, NOW, None, 0, ORDER5_ID, SHOP1_ID, CUST1_ID, 5, "Excellent quality, highly recommend!"),
        (VSRATING2_ID, NOW, NOW, None, 0, ORDER7_ID, SHOP2_ID, CUST2_ID, 4, "Good variety, slightly pricey."),
    ])
    log("vendor_shop_ratings")


def seed_delivery_module(cur):
    """DELIVERY MODULE — partners, assignments, OTPs, earnings, ratings, POD."""

    # delivery_partners
    dp_rows = [
        # (id=user_id, kyc_status, is_online, lat, lon, vehicle_type,
        #  city, license, plate, model, year, bank_name, account_holder,
        #  account_number, ifsc, agreed_terms, has_basic, step, rejection)
        (DP1_ID, "APPROVED", True,  12.9700, 77.5900, "BIKE",
         "Bengaluru", "KA0120230001", "KA-01-AA-1234", "Honda Activa", 2022,
         "SBI", "Kiran Kumar", "000011112222", "SBIN0001234",
         True, True, 3, None),
        (DP2_ID, "APPROVED", False, 18.5180, 73.8540, "BICYCLE",
         "Pune", "MH1220230002", "MH-12-AB-5678", "Hero Sprint", 2021,
         "HDFC", "Lakshmi Devi", "000033334444", "HDFC0002345",
         True, True, 3, None),
        (DP3_ID, "PENDING",  False, None,   None,   "BIKE",
         "Mumbai", None, None, None, None,
         None, None, None, None,
         False, True, 1, None),
    ]
    run_many(cur, """
        INSERT INTO delivery_partners (id, created_at, updated_at, deleted_at, version,
                                       user_id, kyc_status, is_online,
                                       current_latitude, current_longitude, vehicle_type,
                                       city_of_operation, license_number, plate_number,
                                       vehicle_model, manufacture_year,
                                       bank_name, account_holder_name, account_number,
                                       ifsc_code, agreed_to_payout_terms,
                                       has_basic_info, verification_step, rejection_reason)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(r[0], NOW, NOW, None, 0, r[0], r[1], r[2], r[3], r[4], r[5],
           r[6], r[7], r[8], r[9], r[10], r[11], r[12], r[13], r[14],
           r[15], r[16], r[17], r[18]) for r in dp_rows])
    log("delivery_partners")

    # delivery_documents
    ddocs = [
        (DDOC1_ID, DP1_ID, "DRIVING_LICENSE", "APPROVED", "2030-01-01"),
        (DDOC2_ID, DP1_ID, "RC_BOOK",          "APPROVED", "2027-06-30"),
        (DDOC3_ID, DP2_ID, "DRIVING_LICENSE", "APPROVED", "2028-03-15"),
    ]
    run_many(cur, """
        INSERT INTO delivery_documents (id, created_at, updated_at, deleted_at, version,
                                        delivery_partner_user_id, document_type,
                                        status, file_url, expiry_date)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(d[0], NOW, NOW, None, 0, d[1], d[2], d[3],
           f"https://cdn.veggofresh.dev/docs/{d[0]}.pdf", d[4]) for d in ddocs])
    log("delivery_documents")

    # delivery_assignments
    # (id, order_id, partner_user_id, status, pickup_lat, pickup_lon, drop_lat, drop_lon,
    #  assigned_at, expires_at)
    PICKUP_LAT, PICKUP_LON = 12.9712, 77.5943
    DROP_LAT,   DROP_LON   = 12.9716, 77.5946
    DAS = [
        (DA1_ID, ORDER1_ID, None,   "PENDING",          PICKUP_LAT, PICKUP_LON, DROP_LAT, DROP_LON, None,           ts(300)),
        (DA2_ID, ORDER2_ID, DP1_ID, "ACCEPTED",         PICKUP_LAT, PICKUP_LON, DROP_LAT, DROP_LON, ts(-3600),      ts(-3300)),
        (DA3_ID, ORDER3_ID, DP1_ID, "PICKED_UP",        PICKUP_LAT, PICKUP_LON, DROP_LAT, DROP_LON, ts(-7200),      ts(-6900)),
        (DA4_ID, ORDER5_ID, DP1_ID, "DELIVERED",        PICKUP_LAT, PICKUP_LON, DROP_LAT, DROP_LON, ts(-86400),     ts(-86100)),
        (DA5_ID, ORDER6_ID, DP2_ID, "REJECTED",         18.5199,    73.8560,    18.5204,  73.8567,   ts(-43200),     ts(-42900)),
        (DA6_ID, ORDER7_ID, DP2_ID, "DELIVERED",        18.5199,    73.8560,    18.5204,  73.8567,   ts(-172800),    ts(-172500)),
    ]
    run_many(cur, """
        INSERT INTO delivery_assignments (id, created_at, updated_at, deleted_at, version,
                                          order_id, delivery_partner_user_id, status,
                                          pickup_latitude, pickup_longitude,
                                          drop_latitude, drop_longitude,
                                          assigned_at, expires_at)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(d[0], NOW, NOW, None, 0, d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9])
          for d in DAS])
    log("delivery_assignments")

    # delivery_assignment_status_history
    # Table schema: id, created_at, updated_at, deleted_at, version, assignment_id, status
    history = [
        (DASH1_ID, DA2_ID, "ASSIGNED"),
        (DASH2_ID, DA2_ID, "ACCEPTED"),
        (DASH3_ID, DA4_ID, "ASSIGNED"),
        (DASH4_ID, DA4_ID, "DELIVERED"),
    ]
    run_many(cur, """
        INSERT INTO delivery_assignment_status_history
                    (id, created_at, updated_at, deleted_at, version,
                     assignment_id, status)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(h[0], NOW, NOW, None, 0, h[1], h[2]) for h in history])
    log("delivery_assignment_status_history")

    # delivery_otps
    # Table schema: id, created_at, updated_at, deleted_at, version,
    #               assignment_id, otp_code, expires_at, verified, attempts, type
    # NOTE: uk_delivery_otps_assignment is a non-partial UNIQUE on (assignment_id) — only
    # one row per assignment. We seed only the DROP OTP for DA3 (the PICKUP was consumed).
    otps = [
        # (id, assignment_id, otp_code, type, verified, expires_at, deleted_at)
        (DOTP1_ID, DA1_ID, "4821", "DROP",  False, ts(300),     None),
        (DOTP3_ID, DA3_ID, "9283", "DROP",  False, ts(-7100),   None),
        (DOTP4_ID, DA4_ID, "1029", "DROP",  True,  ts(-75700),  None),
        (DOTP5_ID, DA6_ID, "5517", "DROP",  True,  ts(-162100), None),
    ]
    run_many(cur, """
        INSERT INTO delivery_otps (id, created_at, updated_at, deleted_at, version,
                                   assignment_id, otp_code, expires_at, verified, attempts, type)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(o[0], NOW, NOW, o[6], 0, o[1], o[2], o[5], o[4], 0, o[3]) for o in otps])
    log("delivery_otps")

    # delivery_earnings
    run_many(cur, """
        INSERT INTO delivery_earnings (id, created_at, updated_at, deleted_at, version,
                                       delivery_partner_user_id, order_id, amount,
                                       base_pay, distance_fare, peak_bonus, tip)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (DE1_ID, NOW, NOW, None, 0, DP1_ID, ORDER5_ID, 60.00, 30.00, 25.00, 5.00, 0.00),
        (DE2_ID, NOW, NOW, None, 0, DP2_ID, ORDER7_ID, 55.00, 30.00, 20.00, 5.00, 0.00),
    ])
    log("delivery_earnings")

    # delivery_online_sessions
    run_many(cur, """
        INSERT INTO delivery_online_sessions (id, created_at, updated_at, deleted_at, version,
                                              delivery_partner_user_id, started_at, ended_at)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (DSESS1_ID, NOW, NOW, None, 0, DP1_ID, ts(-28800), ts(-14400)),
        (DSESS2_ID, NOW, NOW, None, 0, DP2_ID, ts(-57600), ts(-43200)),
    ])
    log("delivery_online_sessions")

    # delivery_partner_ratings
    run_many(cur, """
        INSERT INTO delivery_partner_ratings (id, created_at, updated_at, deleted_at, version,
                                              assignment_id, delivery_partner_user_id,
                                              customer_user_id, rating_value, comment)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (DRATING1_ID, NOW, NOW, None, 0, DA4_ID, DP1_ID, CUST1_ID, 5, "Very polite and on time!"),
    ])
    log("delivery_partner_ratings")

    # delivery_proof_of_delivery
    run_many(cur, """
        INSERT INTO delivery_proof_of_delivery (id, created_at, updated_at, deleted_at, version,
                                                assignment_id, photo_url,
                                                delivered_to_customer_directly,
                                                left_at_front_door, packaging_intact,
                                                address_verified_manually, notes)
        VALUES %s ON CONFLICT DO NOTHING
    """, [
        (DPOD1_ID, NOW, NOW, None, 0, DA4_ID,
         "https://cdn.veggofresh.dev/pod/pod-da4.jpg", True, False, True, True,
         "Handed to customer at door."),
        (DPOD2_ID, NOW, NOW, None, 0, DA6_ID,
         "https://cdn.veggofresh.dev/pod/pod-da6.jpg", True, False, True, True,
         "Delivered successfully."),
    ])
    log("delivery_proof_of_delivery")


def seed_payment_module(cur):
    """PAYMENT MODULE — wallets, transactions, payment_orders, order_lines, webhooks, payouts."""

    # wallets (all roles get a wallet)
    wallet_users = [
        (WALLET_ADMIN_ID, ADMIN_ID,  5000.00),
        (WALLET_C1_ID,   CUST1_ID,   250.00),
        (WALLET_C2_ID,   CUST2_ID,   100.00),
        (WALLET_C3_ID,   CUST3_ID,     0.00),
        (WALLET_V1_ID,   VENDOR1_ID, 1200.00),
        (WALLET_V2_ID,   VENDOR2_ID,  800.00),
        (WALLET_DP1_ID,  DP1_ID,      350.00),
        (WALLET_DP2_ID,  DP2_ID,      220.00),
    ]
    run_many(cur, """
        INSERT INTO wallets (id, user_id, balance, created_at, updated_at, deleted_at)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(w[0], w[1], w[2], NOW, NOW, None) for w in wallet_users])
    log("wallets")

    # wallet_transactions
    wt_rows = [
        (WT1_ID, CUST1_ID,  "CREDIT", "ORDER_REFUND",  50.00, 250.00, ORDER6_ID, "Refund for cancelled order VGF-0006"),
        (WT2_ID, VENDOR1_ID,"CREDIT", "SALE_SETTLEMENT",900.00,1200.00,ORDER5_ID,"Settlement for order VGF-0005"),
        (WT3_ID, DP1_ID,    "CREDIT", "DELIVERY_FEE",  60.00, 350.00, ORDER5_ID, "Delivery earnings for order VGF-0005"),
        (WT4_ID, CUST1_ID,  "DEBIT",  "WALLET_TOPUP",  200.00, 200.00, None,     "Wallet top-up via Razorpay"),
    ]
    run_many(cur, """
        INSERT INTO wallet_transactions (id, user_id, type, reason, amount,
                                         balance_after, reference_id, description,
                                         created_at, updated_at, deleted_at)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], NOW, NOW, None)
          for w in wt_rows])
    log("wallet_transactions")

    # payment_orders
    payment_orders = [
        (PO1_ID, CUST1_ID, "order_FakeRzp0001", "pay_FakeRzp0001", "INR", 320.00, 320.00, "CAPTURED", ts(-86400), ts(-86000), False),
        (PO2_ID, CUST2_ID, "order_FakeRzp0002", None,               "INR",  99.00, None,   "CREATED",  None,      None,       False),
        (PO3_ID, CUST1_ID, "order_FakeRzp0003", "pay_FakeRzp0003", "INR",  85.00, None,   "FAILED",   ts(-7200), None,       False),
    ]
    run_many(cur, """
        INSERT INTO payment_orders (id, user_id, razorpay_order_id, razorpay_payment_id,
                                    currency, total_amount, captured_amount, status,
                                    authorized_at, captured_at,
                                    created_at, updated_at, deleted_at, version, is_topup)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], NOW, NOW, None, 0, p[10])
          for p in payment_orders])
    log("payment_orders")

    # payment_order_lines
    pol_rows = [
        (POL1_ID, PO1_ID, ORDER5_ID, 320.00, "ACCEPTED", ts(-86000)),
        (POL2_ID, PO2_ID, ORDER4_ID,  99.00, "PENDING",  None),
        (POL3_ID, PO3_ID, ORDER2_ID,  85.00, "VOIDED",   ts(-7100)),
    ]
    run_many(cur, """
        INSERT INTO payment_order_lines (id, payment_order_id, order_id, amount,
                                         status, resolved_at,
                                         created_at, updated_at, deleted_at, version)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(p[0], p[1], p[2], p[3], p[4], p[5], NOW, NOW, None, 0) for p in pol_rows])
    log("payment_order_lines")

    # payment_webhook_events
    # Table schema: id, razorpay_event_id, event_type, payload, processed_at,
    #               created_at, updated_at, deleted_at, version
    pwe_rows = [
        (PWE1_ID, "evt_FakeRzp0001", "payment.captured",
         '{"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_FakeRzp0001","order_id":"order_FakeRzp0001"}}}}',
         ts(-86000)),
        (PWE2_ID, "evt_FakeRzp0002", "payment.failed",
         '{"event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_FakeRzp0003","order_id":"order_FakeRzp0003"}}}}',
         ts(-7100)),
    ]
    run_many(cur, """
        INSERT INTO payment_webhook_events (id, razorpay_event_id, event_type, payload,
                                            processed_at,
                                            created_at, updated_at, deleted_at, version)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(p[0], p[1], p[2], p[3], p[4], NOW, NOW, None, 0) for p in pwe_rows])
    log("payment_webhook_events")

    # payout_requests
    pr_rows = [
        (PR1_ID, VENDOR1_ID, 500.00, "PENDING",  None, None, None),
        (PR2_ID, DP1_ID,     300.00, "APPROVED", "pout_FakeRzp0001", "Processed manually", ts(-3600)),
    ]
    run_many(cur, """
        INSERT INTO payout_requests (id, user_id, amount, status,
                                     razorpay_payout_id, admin_notes, processed_at,
                                     created_at, updated_at, deleted_at)
        VALUES %s ON CONFLICT DO NOTHING
    """, [(p[0], p[1], p[2], p[3], p[4], p[5], p[6], NOW, NOW, None) for p in pr_rows])
    log("payout_requests")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    print("=" * 65)
    print("  VegGo Fresh — Database Seed Script")
    print(f"  Target: {args.user}@{args.host}:{args.port}/{args.db}")
    print("=" * 65)

    try:
        conn = connect()
    except Exception as e:
        print(f"\nERROR: Could not connect to database.\n  {e}")
        print("\nCheck your connection details or pass them via CLI args:")
        print("  python seed.py --host HOST --port PORT --db DB --user USER --password PASSWORD")
        sys.exit(1)

    conn.autocommit = False
    cur = conn.cursor()

    if not args.keep_data:
        try:
            clear_db(cur)
            conn.commit()
        except Exception as e:
            conn.rollback()
            print(f"\n❌  ERROR during clearing — {e}")
            sys.exit(1)

    modules = [
        ("AUTH MODULE",     seed_users),
        ("CATALOG MODULE",  seed_catalog),
        ("CUSTOMER MODULE", seed_customer_module),
        ("VENDOR MODULE",   seed_vendor_module),
        ("DELIVERY MODULE", seed_delivery_module),
        ("PAYMENT MODULE",  seed_payment_module),
    ]

    try:
        for label, fn in modules:
            print(f"\n── {label} {'─' * (50 - len(label))}")
            fn(cur)

        conn.commit()
        print("\n" + "=" * 65)
        print("  ✅  Seeding completed successfully!")
        print("  ℹ   All INSERTs used ON CONFLICT DO NOTHING — safe to re-run.")
        print("=" * 65)

    except Exception as e:
        conn.rollback()
        print(f"\n❌  ERROR during seeding — transaction rolled back.\n  {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
