#!/usr/bin/env python3
"""
VegGoFresh — Seed Data Reference Document Generator
Generates a styled HTML (and optionally PDF via WeasyPrint) documenting
every seeded entity so the app team can use the data confidently.
"""

import uuid, os, sys

# ── Deterministic UUID helper (must match seed.py) ──────────────────────────
def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_DNS, f"veggofresh.seed.{name}"))

# ── All IDs (mirrors seed.py) ────────────────────────────────────────────────
ADMIN_ID   = "e837cfbe-7d6f-474c-8bb3-455b55018b10"
CUST1_ID   = uid("customer.1")
CUST2_ID   = uid("customer.2")
CUST3_ID   = uid("customer.3")
VENDOR1_ID = uid("vendor.1")
VENDOR2_ID = uid("vendor.2")
VENDOR3_ID = uid("vendor.3")
DP1_ID     = uid("delivery.partner.1")
DP2_ID     = uid("delivery.partner.2")
DP3_ID     = uid("delivery.partner.3")
SHOP1_ID   = uid("shop.1")
SHOP2_ID   = uid("shop.2")
SHOP3_ID   = uid("shop.3")
ORDER1_ID  = uid("order.1")
ORDER2_ID  = uid("order.2")
ORDER3_ID  = uid("order.3")
ORDER4_ID  = uid("order.4")
ORDER5_ID  = uid("order.5")
ORDER6_ID  = uid("order.6")
ORDER7_ID  = uid("order.7")
DA1_ID     = uid("da.1")
DA2_ID     = uid("da.2")
DA3_ID     = uid("da.3")
DA4_ID     = uid("da.4")
DA5_ID     = uid("da.5")
DA6_ID     = uid("da.6")
PO1_ID     = uid("po.1")
PO2_ID     = uid("po.2")
PO3_ID     = uid("po.3")
WALLET_ADMIN_ID = uid("wallet.admin")
WALLET_C1_ID    = uid("wallet.c1")
WALLET_C2_ID    = uid("wallet.c2")
WALLET_V1_ID    = uid("wallet.v1")
WALLET_V2_ID    = uid("wallet.v2")
WALLET_DP1_ID   = uid("wallet.dp1")
WALLET_DP2_ID   = uid("wallet.dp2")
PR1_ID     = uid("pr.1")
PR2_ID     = uid("pr.2")

PASSWORD = "Password@123"

# ── HTML ─────────────────────────────────────────────────────────────────────
HTML = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>VegGoFresh — Seed Data Reference</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

  *, *::before, *::after {{ box-sizing: border-box; margin: 0; padding: 0; }}

  :root {{
    --green:   #16a34a;
    --green-l: #dcfce7;
    --green-d: #14532d;
    --amber:   #d97706;
    --amber-l: #fef3c7;
    --blue:    #2563eb;
    --blue-l:  #dbeafe;
    --red:     #dc2626;
    --red-l:   #fee2e2;
    --purple:  #7c3aed;
    --purple-l:#ede9fe;
    --gray-50: #f9fafb;
    --gray-100:#f3f4f6;
    --gray-200:#e5e7eb;
    --gray-400:#9ca3af;
    --gray-600:#4b5563;
    --gray-700:#374151;
    --gray-900:#111827;
    --radius:  8px;
  }}

  body {{
    font-family: 'Inter', system-ui, sans-serif;
    font-size: 13px;
    color: var(--gray-900);
    background: #fff;
    line-height: 1.5;
  }}

  /* ── Cover ── */
  .cover {{
    background: linear-gradient(135deg, #14532d 0%, #166534 40%, #15803d 100%);
    color: #fff;
    padding: 60px 48px 50px;
    position: relative;
    overflow: hidden;
  }}
  .cover::after {{
    content: '';
    position: absolute;
    right: -60px; top: -60px;
    width: 320px; height: 320px;
    border-radius: 50%;
    background: rgba(255,255,255,.06);
  }}
  .cover-logo {{ font-size: 13px; letter-spacing: .15em; text-transform: uppercase; opacity: .7; margin-bottom: 24px; }}
  .cover h1 {{ font-size: 36px; font-weight: 700; line-height: 1.15; margin-bottom: 10px; }}
  .cover p  {{ font-size: 15px; opacity: .8; max-width: 520px; }}
  .cover-meta {{ margin-top: 32px; display: flex; gap: 32px; flex-wrap: wrap; }}
  .cover-meta span {{ font-size: 12px; opacity: .65; }}
  .cover-meta strong {{ display: block; font-size: 14px; font-weight: 600; opacity: 1; margin-top: 2px; }}

  /* ── Layout ── */
  .content {{ padding: 40px 48px; max-width: 1080px; margin: 0 auto; }}

  /* ── Section headers ── */
  .section {{ margin-top: 44px; }}
  .section-header {{
    display: flex; align-items: center; gap: 12px;
    border-bottom: 2px solid var(--gray-200);
    padding-bottom: 10px; margin-bottom: 20px;
  }}
  .section-icon {{
    width: 32px; height: 32px; border-radius: 8px;
    display: flex; align-items: center; justify-content: center;
    font-size: 16px; flex-shrink: 0;
  }}
  .section-header h2 {{ font-size: 18px; font-weight: 700; color: var(--gray-900); }}
  .section-header .badge {{
    margin-left: auto; font-size: 11px; font-weight: 600;
    padding: 3px 10px; border-radius: 20px; background: var(--gray-100); color: var(--gray-600);
  }}

  /* ── Sub-section ── */
  .sub {{ margin-top: 24px; }}
  .sub h3 {{ font-size: 13px; font-weight: 600; color: var(--gray-600); text-transform: uppercase;
             letter-spacing: .08em; margin-bottom: 12px; }}

  /* ── Tables ── */
  table {{ width: 100%; border-collapse: collapse; font-size: 12.5px; }}
  thead tr {{ background: var(--gray-50); }}
  th {{ text-align: left; padding: 9px 12px; font-weight: 600; font-size: 11.5px;
        text-transform: uppercase; letter-spacing: .07em; color: var(--gray-600);
        border-bottom: 1px solid var(--gray-200); white-space: nowrap; }}
  td {{ padding: 9px 12px; border-bottom: 1px solid var(--gray-100); vertical-align: top; }}
  tr:last-child td {{ border-bottom: none; }}
  tr:hover td {{ background: var(--gray-50); }}
  .table-wrap {{ border: 1px solid var(--gray-200); border-radius: var(--radius); overflow: hidden; }}

  /* ── Monospace UUIDs ── */
  code {{ font-family: 'JetBrains Mono', monospace; font-size: 11px;
          background: var(--gray-100); padding: 2px 5px; border-radius: 4px; word-break: break-all; }}
  .uuid {{ font-family: 'JetBrains Mono', monospace; font-size: 10.5px; color: var(--gray-600); }}

  /* ── Status badges ── */
  .pill {{
    display: inline-block; padding: 2px 8px; border-radius: 20px;
    font-size: 11px; font-weight: 600; white-space: nowrap;
  }}
  .pill-green  {{ background: var(--green-l);  color: var(--green-d); }}
  .pill-amber  {{ background: var(--amber-l);  color: #92400e; }}
  .pill-red    {{ background: var(--red-l);    color: #991b1b; }}
  .pill-blue   {{ background: var(--blue-l);   color: #1e40af; }}
  .pill-purple {{ background: var(--purple-l); color: #5b21b6; }}
  .pill-gray   {{ background: var(--gray-100); color: var(--gray-600); }}

  /* ── Password box ── */
  .pw-box {{
    display: inline-flex; align-items: center; gap: 10px;
    background: var(--gray-900); color: #d1fae5;
    font-family: 'JetBrains Mono', monospace; font-size: 15px; font-weight: 500;
    padding: 10px 20px; border-radius: 8px; margin: 8px 0 20px;
    letter-spacing: .05em;
  }}
  .pw-box .label {{ font-size: 10px; color: #86efac; font-family: 'Inter', sans-serif;
                    text-transform: uppercase; letter-spacing: .1em; margin-right: 4px; }}

  /* ── Info callout ── */
  .callout {{
    border-left: 4px solid var(--green); background: var(--green-l);
    padding: 12px 16px; border-radius: 0 8px 8px 0;
    font-size: 12.5px; color: var(--green-d); margin: 16px 0;
  }}
  .callout strong {{ display: block; margin-bottom: 4px; font-size: 13px; }}

  /* ── Two-col grid ── */
  .grid2 {{ display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }}

  /* ── Cards ── */
  .card {{
    border: 1px solid var(--gray-200); border-radius: var(--radius);
    padding: 16px 18px;
  }}
  .card .card-title {{ font-weight: 600; margin-bottom: 10px; font-size: 13px; }}
  .card dl {{ display: grid; grid-template-columns: auto 1fr; gap: 4px 16px; }}
  .card dt {{ color: var(--gray-400); font-size: 11.5px; padding-top: 2px; white-space: nowrap; }}
  .card dd {{ font-size: 12px; word-break: break-all; }}

  /* ── Page break hints for print ── */
  @media print {{
    .section {{ page-break-inside: avoid; }}
    .cover {{ page-break-after: always; }}
    body {{ font-size: 11px; }}
    .content {{ padding: 24px 32px; }}
  }}

  /* ── TOC ── */
  .toc {{ background: var(--gray-50); border: 1px solid var(--gray-200); border-radius: var(--radius); padding: 20px 24px; margin-top: 32px; }}
  .toc h3 {{ font-size: 12px; text-transform: uppercase; letter-spacing: .1em; color: var(--gray-400); margin-bottom: 12px; }}
  .toc ol {{ padding-left: 20px; }}
  .toc li {{ margin-bottom: 6px; font-size: 13px; font-weight: 500; color: var(--blue); }}
  .toc li span {{ font-weight: 400; color: var(--gray-600); font-size: 12px; }}

  .divider {{ border: none; border-top: 1px solid var(--gray-200); margin: 32px 0; }}
  .icon-green  {{ background: var(--green-l);  }}
  .icon-amber  {{ background: var(--amber-l);  }}
  .icon-blue   {{ background: var(--blue-l);   }}
  .icon-purple {{ background: var(--purple-l); }}
  .icon-red    {{ background: var(--red-l);    }}
</style>
</head>
<body>

<!-- ════════════════════════════════════════════════════
     COVER
════════════════════════════════════════════════════ -->
<div class="cover">
  <div class="cover-logo">🥦 VegGoFresh Platform</div>
  <h1>Seed Data<br>Reference Guide</h1>
  <p>Complete reference of all entities seeded into the local / dev database. Use this document to understand which users, orders, shops, deliveries and payments are available for testing.</p>
  <div class="cover-meta">
    <div><span>Environment</span><strong>Local / Dev</strong></div>
    <div><span>Database</span><strong>veggofresh_local (PostgreSQL)</strong></div>
    <div><span>Script</span><strong>seed.py — ON CONFLICT DO NOTHING</strong></div>
    <div><span>Re-runnable</span><strong>Yes — idempotent</strong></div>
  </div>
</div>

<!-- ════════════════════════════════════════════════════
     CONTENT
════════════════════════════════════════════════════ -->
<div class="content">

  <!-- TOC -->
  <div class="toc">
    <h3>Contents</h3>
    <ol>
      <li>Auth &amp; Users <span>— 10 users across 4 roles</span></li>
      <li>Customers <span>— profiles, addresses, carts, orders</span></li>
      <li>Vendors &amp; Shops <span>— 3 shops, documents, listings</span></li>
      <li>Catalog <span>— 4 categories, 8 subcategories, 20 products</span></li>
      <li>Delivery Module <span>— partners, assignments, OTPs, earnings</span></li>
      <li>Payment Module <span>— wallets, payment orders, payouts</span></li>
      <li>Quick-start Cheat Sheet</li>
    </ol>
  </div>

  <!-- ══════════════════════════════════════
       1. AUTH & USERS
  ══════════════════════════════════════ -->
  <div class="section" id="auth">
    <div class="section-header">
      <div class="section-icon icon-green">🔐</div>
      <h2>1 · Auth &amp; Users</h2>
      <span class="badge">10 users</span>
    </div>

    <div class="callout">
      <strong>Universal Password</strong>
      The bcrypt hash stored in the DB corresponds to: &nbsp;
      <span class="pw-box"><span class="label">password</span>Password@123</span>
      <br>Every user (admin, customers, vendors, delivery partners) shares this password.
    </div>

    <div class="sub">
      <h3>All Users</h3>
      <div class="table-wrap">
        <table>
          <thead><tr>
            <th>Role</th><th>Email</th><th>Phone</th><th>Verified</th><th>Status</th><th>User ID</th>
          </tr></thead>
          <tbody>
            <tr>
              <td><span class="pill pill-purple">ADMIN</span></td>
              <td><strong>admin@veg.go</strong></td>
              <td>+910000000000</td>
              <td><span class="pill pill-green">✓ Yes</span></td>
              <td><span class="pill pill-green">Active</span></td>
              <td class="uuid">{ADMIN_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-blue">CUSTOMER</span></td>
              <td><strong>customer1@veggofresh.dev</strong></td>
              <td>+919876543201</td>
              <td><span class="pill pill-green">✓ Yes</span></td>
              <td><span class="pill pill-green">Active</span></td>
              <td class="uuid">{CUST1_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-blue">CUSTOMER</span></td>
              <td><strong>customer2@veggofresh.dev</strong></td>
              <td>+919876543202</td>
              <td><span class="pill pill-green">✓ Yes</span></td>
              <td><span class="pill pill-green">Active</span></td>
              <td class="uuid">{CUST2_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-blue">CUSTOMER</span></td>
              <td><strong>customer3@veggofresh.dev</strong></td>
              <td>+919876543203</td>
              <td><span class="pill pill-amber">✗ No</span></td>
              <td><span class="pill pill-gray">Unverified</span></td>
              <td class="uuid">{CUST3_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-green">VENDOR</span></td>
              <td><strong>vendor1@veggofresh.dev</strong></td>
              <td>+918765432101</td>
              <td><span class="pill pill-green">✓ Yes</span></td>
              <td><span class="pill pill-green">Active</span></td>
              <td class="uuid">{VENDOR1_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-green">VENDOR</span></td>
              <td><strong>vendor2@veggofresh.dev</strong></td>
              <td>+918765432102</td>
              <td><span class="pill pill-green">✓ Yes</span></td>
              <td><span class="pill pill-green">Active</span></td>
              <td class="uuid">{VENDOR2_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-green">VENDOR</span></td>
              <td><strong>vendor3@veggofresh.dev</strong></td>
              <td>+918765432103</td>
              <td><span class="pill pill-amber">✗ No</span></td>
              <td><span class="pill pill-amber">KYC Pending</span></td>
              <td class="uuid">{VENDOR3_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-amber">DELIVERY</span></td>
              <td><strong>dp1@veggofresh.dev</strong></td>
              <td>+917654321001</td>
              <td><span class="pill pill-green">✓ Yes</span></td>
              <td><span class="pill pill-green">Online</span></td>
              <td class="uuid">{DP1_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-amber">DELIVERY</span></td>
              <td><strong>dp2@veggofresh.dev</strong></td>
              <td>+917654321002</td>
              <td><span class="pill pill-green">✓ Yes</span></td>
              <td><span class="pill pill-gray">Offline</span></td>
              <td class="uuid">{DP2_ID}</td>
            </tr>
            <tr>
              <td><span class="pill pill-amber">DELIVERY</span></td>
              <td><strong>dp3@veggofresh.dev</strong></td>
              <td>+917654321003</td>
              <td><span class="pill pill-amber">✗ No</span></td>
              <td><span class="pill pill-amber">KYC Pending</span></td>
              <td class="uuid">{DP3_ID}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <hr class="divider"/>

  <!-- ══════════════════════════════════════
       2. CUSTOMERS
  ══════════════════════════════════════ -->
  <div class="section" id="customers">
    <div class="section-header">
      <div class="section-icon icon-blue">🛒</div>
      <h2>2 · Customers</h2>
      <span class="badge">3 customers · 7 orders</span>
    </div>

    <div class="sub">
      <h3>Orders — One Per Status Scenario</h3>
      <div class="table-wrap">
        <table>
          <thead><tr>
            <th>Order #</th><th>Customer</th><th>Status</th><th>Total</th><th>Shop</th><th>Order ID</th>
          </tr></thead>
          <tbody>
            <tr>
              <td><strong>VGF-0001</strong></td><td>customer1</td>
              <td><span class="pill pill-gray">PENDING</span></td>
              <td>₹155.00</td><td>—</td>
              <td class="uuid">{ORDER1_ID}</td>
            </tr>
            <tr>
              <td><strong>VGF-0002</strong></td><td>customer1</td>
              <td><span class="pill pill-blue">CONFIRMED</span></td>
              <td>₹85.00</td><td>Green Basket Organics</td>
              <td class="uuid">{ORDER2_ID}</td>
            </tr>
            <tr>
              <td><strong>VGF-0003</strong></td><td>customer1</td>
              <td><span class="pill pill-amber">PREPARING</span></td>
              <td>₹200.00</td><td>Green Basket Organics</td>
              <td class="uuid">{ORDER3_ID}</td>
            </tr>
            <tr>
              <td><strong>VGF-0004</strong></td><td>customer2</td>
              <td><span class="pill pill-purple">OUT_FOR_DELIVERY</span></td>
              <td>₹99.00</td><td>Green Basket Organics</td>
              <td class="uuid">{ORDER4_ID}</td>
            </tr>
            <tr>
              <td><strong>VGF-0005</strong></td><td>customer1</td>
              <td><span class="pill pill-green">DELIVERED</span></td>
              <td>₹320.00</td><td>Green Basket Organics</td>
              <td class="uuid">{ORDER5_ID}</td>
            </tr>
            <tr>
              <td><strong>VGF-0006</strong></td><td>customer2</td>
              <td><span class="pill pill-red">CANCELLED</span></td>
              <td>₹60.00</td><td>—</td>
              <td class="uuid">{ORDER6_ID}</td>
            </tr>
            <tr>
              <td><strong>VGF-0007</strong></td><td>customer2</td>
              <td><span class="pill pill-green">DELIVERED</span></td>
              <td>₹180.00</td><td>Farm To Table</td>
              <td class="uuid">{ORDER7_ID}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="sub">
      <h3>Wallets (Customer)</h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>User</th><th>Balance</th><th>Wallet ID</th></tr></thead>
          <tbody>
            <tr><td>customer1</td><td>₹250.00</td><td class="uuid">{WALLET_C1_ID}</td></tr>
            <tr><td>customer2</td><td>₹100.00</td><td class="uuid">{WALLET_C2_ID}</td></tr>
            <tr><td>customer3</td><td>₹0.00</td><td class="uuid">{uid("wallet.c3")}</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <hr class="divider"/>

  <!-- ══════════════════════════════════════
       3. VENDORS & SHOPS
  ══════════════════════════════════════ -->
  <div class="section" id="vendors">
    <div class="section-header">
      <div class="section-icon icon-green">🏪</div>
      <h2>3 · Vendors &amp; Shops</h2>
      <span class="badge">3 shops</span>
    </div>

    <div class="table-wrap">
      <table>
        <thead><tr>
          <th>Shop</th><th>Owner</th><th>City</th><th>KYC</th><th>Online</th><th>Delivery Range</th><th>Shop ID</th>
        </tr></thead>
        <tbody>
          <tr>
            <td><strong>Green Basket Organics</strong><br><small>Organic Produce</small></td>
            <td>vendor1<br><small>Anita Rao</small></td>
            <td>Bengaluru</td>
            <td><span class="pill pill-green">APPROVED</span></td>
            <td><span class="pill pill-green">🟢 Online</span></td>
            <td>8 km</td>
            <td class="uuid">{SHOP1_ID}</td>
          </tr>
          <tr>
            <td><strong>Farm To Table</strong><br><small>Vegetable Wholesaler</small></td>
            <td>vendor2<br><small>Raju Patil</small></td>
            <td>Pune</td>
            <td><span class="pill pill-green">APPROVED</span></td>
            <td><span class="pill pill-gray">🔴 Offline</span></td>
            <td>10 km</td>
            <td class="uuid">{SHOP2_ID}</td>
          </tr>
          <tr>
            <td><strong>Fresh Picks</strong><br><small>Grocery</small></td>
            <td>vendor3<br><small>Meera Shah</small></td>
            <td>Mumbai</td>
            <td><span class="pill pill-amber">PENDING</span></td>
            <td><span class="pill pill-gray">🔴 Offline</span></td>
            <td>—</td>
            <td class="uuid">{SHOP3_ID}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="sub">
      <h3>Vendor Wallets</h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>User</th><th>Balance</th><th>Wallet ID</th></tr></thead>
          <tbody>
            <tr><td>vendor1 (Green Basket)</td><td>₹1,200.00</td><td class="uuid">{WALLET_V1_ID}</td></tr>
            <tr><td>vendor2 (Farm To Table)</td><td>₹800.00</td><td class="uuid">{WALLET_V2_ID}</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <hr class="divider"/>

  <!-- ══════════════════════════════════════
       4. CATALOG
  ══════════════════════════════════════ -->
  <div class="section" id="catalog">
    <div class="section-header">
      <div class="section-icon icon-amber">📦</div>
      <h2>4 · Catalog</h2>
      <span class="badge">4 categories · 8 subcategories · 20 products</span>
    </div>

    <div class="table-wrap">
      <table>
        <thead><tr><th>#</th><th>Product</th><th>Category</th><th>Price</th><th>Original</th><th>Unit</th></tr></thead>
        <tbody>
          <tr><td>CP1</td><td>Palak (Spinach)</td><td>Vegetables › Leafy Greens</td><td>₹40</td><td>₹50</td><td>250g</td></tr>
          <tr><td>CP2</td><td>Coriander</td><td>Vegetables › Leafy Greens</td><td>₹15</td><td>—</td><td>100g</td></tr>
          <tr><td>CP3</td><td>Potato</td><td>Vegetables › Root</td><td>₹25</td><td>₹30</td><td>1kg</td></tr>
          <tr><td>CP4</td><td>Tomato</td><td>Vegetables › Root</td><td>₹30</td><td>—</td><td>500g</td></tr>
          <tr><td>CP5</td><td>Orange</td><td>Fruits › Citrus</td><td>₹80</td><td>₹100</td><td>500g</td></tr>
          <tr><td>CP6</td><td>Lemon</td><td>Fruits › Citrus</td><td>₹20</td><td>—</td><td>200g</td></tr>
          <tr><td>CP7</td><td>Alphonso Mango</td><td>Fruits › Tropical</td><td>₹150</td><td>₹180</td><td>1kg</td></tr>
          <tr><td>CP8</td><td>Banana</td><td>Fruits › Tropical</td><td>₹35</td><td>—</td><td>6pcs</td></tr>
          <tr><td>CP9</td><td>Full Cream Milk</td><td>Dairy &amp; Eggs › Milk</td><td>₹85</td><td>—</td><td>1L</td></tr>
          <tr><td>CP10</td><td>Skimmed Milk</td><td>Dairy &amp; Eggs › Milk</td><td>₹75</td><td>—</td><td>1L</td></tr>
          <tr><td>CP11</td><td>Eggs (Pack of 6)</td><td>Dairy &amp; Eggs › Eggs</td><td>₹60</td><td>₹70</td><td>6pcs</td></tr>
          <tr><td>CP12</td><td>Eggs (Pack of 12)</td><td>Dairy &amp; Eggs › Eggs</td><td>₹110</td><td>₹130</td><td>12pcs</td></tr>
          <tr><td>CP13</td><td>Toor Dal</td><td>Grains &amp; Pulses › Lentils</td><td>₹90</td><td>₹110</td><td>500g</td></tr>
          <tr><td>CP14</td><td>Chana Dal</td><td>Grains &amp; Pulses › Lentils</td><td>₹85</td><td>—</td><td>500g</td></tr>
          <tr><td>CP15</td><td>Basmati Rice</td><td>Grains &amp; Pulses › Rice</td><td>₹120</td><td>₹140</td><td>1kg</td></tr>
          <tr><td>CP16</td><td>Brown Rice</td><td>Grains &amp; Pulses › Rice</td><td>₹95</td><td>—</td><td>1kg</td></tr>
          <tr><td>CP17</td><td>Onion</td><td>Vegetables › Root</td><td>₹20</td><td>—</td><td>500g</td></tr>
          <tr><td>CP18</td><td>Carrot</td><td>Vegetables › Root</td><td>₹35</td><td>₹40</td><td>500g</td></tr>
          <tr><td>CP19</td><td>Apple (Shimla)</td><td>Fruits › Tropical</td><td>₹130</td><td>₹150</td><td>500g</td></tr>
          <tr><td>CP20</td><td>Cucumber</td><td>Vegetables › Root</td><td>₹25</td><td>—</td><td>500g</td></tr>
        </tbody>
      </table>
    </div>
    <p style="margin-top:10px;font-size:12px;color:var(--gray-400)">
      <strong>Vendor Listings:</strong> Shop1 lists CP1, CP3, CP9 &nbsp;|&nbsp; Shop2 lists CP5 (active) and CP7 (not listed).
    </p>
  </div>

  <hr class="divider"/>

  <!-- ══════════════════════════════════════
       5. DELIVERY MODULE
  ══════════════════════════════════════ -->
  <div class="section" id="delivery">
    <div class="section-header">
      <div class="section-icon icon-amber">🛵</div>
      <h2>5 · Delivery Module</h2>
      <span class="badge">3 partners · 6 assignments</span>
    </div>

    <div class="sub">
      <h3>Delivery Partners</h3>
      <div class="table-wrap">
        <table>
          <thead><tr>
            <th>Partner</th><th>Name</th><th>KYC</th><th>Vehicle</th><th>City</th><th>Online</th><th>User ID</th>
          </tr></thead>
          <tbody>
            <tr>
              <td><strong>dp1</strong></td><td>Kiran Kumar</td>
              <td><span class="pill pill-green">APPROVED</span></td>
              <td>BIKE — Honda Activa<br><small>KA-01-AA-1234</small></td>
              <td>Bengaluru</td>
              <td><span class="pill pill-green">🟢 Online</span></td>
              <td class="uuid">{DP1_ID}</td>
            </tr>
            <tr>
              <td><strong>dp2</strong></td><td>Lakshmi Devi</td>
              <td><span class="pill pill-green">APPROVED</span></td>
              <td>BICYCLE — Hero Sprint<br><small>MH-12-AB-5678</small></td>
              <td>Pune</td>
              <td><span class="pill pill-gray">🔴 Offline</span></td>
              <td class="uuid">{DP2_ID}</td>
            </tr>
            <tr>
              <td><strong>dp3</strong></td><td>—</td>
              <td><span class="pill pill-amber">PENDING</span></td>
              <td>BIKE (incomplete)</td>
              <td>Mumbai</td>
              <td><span class="pill pill-gray">🔴 Offline</span></td>
              <td class="uuid">{DP3_ID}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="sub">
      <h3>Delivery Assignments</h3>
      <div class="table-wrap">
        <table>
          <thead><tr>
            <th>ID</th><th>Order</th><th>Partner</th><th>Status</th><th>Assignment ID</th>
          </tr></thead>
          <tbody>
            <tr>
              <td><strong>DA1</strong></td><td>VGF-0001</td><td>— (unassigned)</td>
              <td><span class="pill pill-gray">PENDING</span></td>
              <td class="uuid">{DA1_ID}</td>
            </tr>
            <tr>
              <td><strong>DA2</strong></td><td>VGF-0002</td><td>dp1</td>
              <td><span class="pill pill-blue">ACCEPTED</span></td>
              <td class="uuid">{DA2_ID}</td>
            </tr>
            <tr>
              <td><strong>DA3</strong></td><td>VGF-0003</td><td>dp1</td>
              <td><span class="pill pill-purple">PICKED_UP</span></td>
              <td class="uuid">{DA3_ID}</td>
            </tr>
            <tr>
              <td><strong>DA4</strong></td><td>VGF-0005</td><td>dp1</td>
              <td><span class="pill pill-green">DELIVERED</span></td>
              <td class="uuid">{DA4_ID}</td>
            </tr>
            <tr>
              <td><strong>DA5</strong></td><td>VGF-0006</td><td>dp2</td>
              <td><span class="pill pill-red">REJECTED</span></td>
              <td class="uuid">{DA5_ID}</td>
            </tr>
            <tr>
              <td><strong>DA6</strong></td><td>VGF-0007</td><td>dp2</td>
              <td><span class="pill pill-green">DELIVERED</span></td>
              <td class="uuid">{DA6_ID}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="sub">
      <h3>Active OTPs (DROP type)</h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Assignment</th><th>OTP Code</th><th>Type</th><th>Verified</th></tr></thead>
          <tbody>
            <tr><td>DA1 (VGF-0001)</td><td><strong><code>4821</code></strong></td><td>DROP</td><td><span class="pill pill-amber">Pending</span></td></tr>
            <tr><td>DA3 (VGF-0003)</td><td><strong><code>9283</code></strong></td><td>DROP</td><td><span class="pill pill-amber">Pending</span></td></tr>
            <tr><td>DA4 (VGF-0005)</td><td><strong><code>1029</code></strong></td><td>DROP</td><td><span class="pill pill-green">Verified</span></td></tr>
            <tr><td>DA6 (VGF-0007)</td><td><strong><code>5517</code></strong></td><td>DROP</td><td><span class="pill pill-green">Verified</span></td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="sub">
      <h3>Delivery Earnings</h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Partner</th><th>Order</th><th>Total</th><th>Base Pay</th><th>Distance</th><th>Peak Bonus</th></tr></thead>
          <tbody>
            <tr><td>dp1</td><td>VGF-0005</td><td>₹60.00</td><td>₹30.00</td><td>₹25.00</td><td>₹5.00</td></tr>
            <tr><td>dp2</td><td>VGF-0007</td><td>₹55.00</td><td>₹30.00</td><td>₹20.00</td><td>₹5.00</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="sub">
      <h3>Delivery Partner Wallets</h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Partner</th><th>Balance</th><th>Wallet ID</th></tr></thead>
          <tbody>
            <tr><td>dp1 (Kiran Kumar)</td><td>₹350.00</td><td class="uuid">{WALLET_DP1_ID}</td></tr>
            <tr><td>dp2 (Lakshmi Devi)</td><td>₹220.00</td><td class="uuid">{WALLET_DP2_ID}</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <hr class="divider"/>

  <!-- ══════════════════════════════════════
       6. PAYMENT MODULE
  ══════════════════════════════════════ -->
  <div class="section" id="payment">
    <div class="section-header">
      <div class="section-icon icon-purple">💳</div>
      <h2>6 · Payment Module</h2>
      <span class="badge">3 payment orders · 2 payouts</span>
    </div>

    <div class="sub">
      <h3>Razorpay Payment Orders</h3>
      <div class="table-wrap">
        <table>
          <thead><tr>
            <th>ID</th><th>User</th><th>Razorpay Order ID</th><th>Amount</th><th>Status</th><th>Payment ID</th>
          </tr></thead>
          <tbody>
            <tr>
              <td><strong>PO1</strong></td><td>customer1</td>
              <td><code>order_FakeRzp0001</code></td>
              <td>₹320.00</td>
              <td><span class="pill pill-green">CAPTURED</span></td>
              <td class="uuid">{PO1_ID}</td>
            </tr>
            <tr>
              <td><strong>PO2</strong></td><td>customer2</td>
              <td><code>order_FakeRzp0002</code></td>
              <td>₹99.00</td>
              <td><span class="pill pill-amber">CREATED</span></td>
              <td class="uuid">{PO2_ID}</td>
            </tr>
            <tr>
              <td><strong>PO3</strong></td><td>customer1</td>
              <td><code>order_FakeRzp0003</code></td>
              <td>₹85.00</td>
              <td><span class="pill pill-red">FAILED</span></td>
              <td class="uuid">{PO3_ID}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="sub">
      <h3>Payout Requests</h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>ID</th><th>User</th><th>Amount</th><th>Status</th><th>Razorpay Payout ID</th><th>Payout Request ID</th></tr></thead>
          <tbody>
            <tr>
              <td><strong>PR1</strong></td><td>vendor1</td><td>₹500.00</td>
              <td><span class="pill pill-amber">PENDING</span></td>
              <td>—</td>
              <td class="uuid">{PR1_ID}</td>
            </tr>
            <tr>
              <td><strong>PR2</strong></td><td>dp1</td><td>₹300.00</td>
              <td><span class="pill pill-green">APPROVED</span></td>
              <td><code>pout_FakeRzp0001</code></td>
              <td class="uuid">{PR2_ID}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="sub">
      <h3>Webhook Events</h3>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Event ID</th><th>Type</th><th>Status</th></tr></thead>
          <tbody>
            <tr><td><code>evt_FakeRzp0001</code></td><td>payment.captured</td><td><span class="pill pill-green">Processed</span></td></tr>
            <tr><td><code>evt_FakeRzp0002</code></td><td>payment.failed</td><td><span class="pill pill-green">Processed</span></td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <hr class="divider"/>

  <!-- ══════════════════════════════════════
       7. QUICK-START CHEAT SHEET
  ══════════════════════════════════════ -->
  <div class="section" id="cheatsheet">
    <div class="section-header">
      <div class="section-icon icon-green">⚡</div>
      <h2>7 · Quick-Start Cheat Sheet</h2>
    </div>

    <div class="grid2">

      <div class="card">
        <div class="card-title">🛒 Happy Path — Customer</div>
        <dl>
          <dt>Login as</dt><dd><strong>customer1@veggofresh.dev</strong></dd>
          <dt>Password</dt><dd><strong>Password@123</strong></dd>
          <dt>Has orders</dt><dd>VGF-0001 to VGF-0005 (all statuses)</dd>
          <dt>Wallet balance</dt><dd>₹250.00</dd>
          <dt>Cart</dt><dd>CART1 with Spinach + Potato items</dd>
          <dt>Ratings given</dt><dd>5★ on VGF-0005, 4★ on VGF-0007</dd>
        </dl>
      </div>

      <div class="card">
        <div class="card-title">🏪 Happy Path — Vendor</div>
        <dl>
          <dt>Login as</dt><dd><strong>vendor1@veggofresh.dev</strong></dd>
          <dt>Password</dt><dd><strong>Password@123</strong></dd>
          <dt>Shop</dt><dd>Green Basket Organics (APPROVED, Online)</dd>
          <dt>Listings</dt><dd>CP1 Spinach, CP3 Potato, CP9 Milk</dd>
          <dt>Wallet</dt><dd>₹1,200.00</dd>
          <dt>Payout</dt><dd>PR1 — ₹500 PENDING</dd>
        </dl>
      </div>

      <div class="card">
        <div class="card-title">🛵 Happy Path — Delivery Partner</div>
        <dl>
          <dt>Login as</dt><dd><strong>dp1@veggofresh.dev</strong></dd>
          <dt>Password</dt><dd><strong>Password@123</strong></dd>
          <dt>Status</dt><dd>APPROVED KYC, currently Online</dd>
          <dt>Active assignment</dt><dd>DA3 — PICKED_UP (VGF-0003)</dd>
          <dt>Active OTP</dt><dd><strong>9283</strong> (DROP for DA3)</dd>
          <dt>Earnings</dt><dd>₹60.00 from VGF-0005</dd>
          <dt>Wallet</dt><dd>₹350.00</dd>
        </dl>
      </div>

      <div class="card">
        <div class="card-title">🛡️ Happy Path — Admin</div>
        <dl>
          <dt>Login as</dt><dd><strong>admin@veg.go</strong></dd>
          <dt>Password</dt><dd><strong>Password@123</strong></dd>
          <dt>Phone</dt><dd>+910000000000</dd>
          <dt>Wallet</dt><dd>₹5,000.00</dd>
          <dt>Pending KYC shops</dt><dd>Fresh Picks (vendor3)</dd>
          <dt>Pending payout</dt><dd>PR1 — vendor1 ₹500</dd>
        </dl>
      </div>

      <div class="card">
        <div class="card-title">🔴 Edge Cases to Test</div>
        <dl>
          <dt>Unverified customer</dt><dd>customer3@veggofresh.dev</dd>
          <dt>KYC pending vendor</dt><dd>vendor3@veggofresh.dev (Fresh Picks)</dd>
          <dt>KYC pending DP</dt><dd>dp3@veggofresh.dev</dd>
          <dt>Cancelled order</dt><dd>VGF-0006 (customer2)</dd>
          <dt>Failed payment</dt><dd>PO3 — order_FakeRzp0003</dd>
          <dt>Rejected assignment</dt><dd>DA5 (dp2 rejected VGF-0006)</dd>
        </dl>
      </div>

      <div class="card">
        <div class="card-title">📋 Platform Settings</div>
        <dl>
          <dt>Delivery radius</dt><dd>12.0 km</dd>
          <dt>Commission</dt><dd>10%</dd>
          <dt>Vendor accept timeout</dt><dd>300 seconds (5 min)</dd>
          <dt>Delivery accept timeout</dt><dd>60 seconds</dd>
          <dt>Max broadcast rounds</dt><dd>5</dd>
          <dt>Max elapsed</dt><dd>30 minutes</dd>
        </dl>
      </div>

    </div><!-- /grid2 -->

    <div class="callout" style="margin-top:24px">
      <strong>Re-seeding is safe</strong>
      All INSERTs use <code>ON CONFLICT DO NOTHING</code>. Run <code>python seed.py</code> at any time to restore
      deleted rows without duplicating existing data. To fully reset, run without <code>--keep-data</code> (default clears all non-admin data first).
    </div>

  </div><!-- /section cheatsheet -->

</div><!-- /content -->
</body>
</html>"""

# ── Write HTML ────────────────────────────────────────────────────────────────
out_dir = os.path.dirname(os.path.abspath(__file__))
html_path = os.path.join(out_dir, "VegGoFresh_SeedData_Reference.html")

with open(html_path, "w", encoding="utf-8") as f:
    f.write(HTML)

print(f"✅  HTML written → {html_path}")

# ── Try WeasyPrint → PDF ──────────────────────────────────────────────────────
pdf_path = html_path.replace(".html", ".pdf")
try:
    from weasyprint import HTML as WP
    WP(filename=html_path).write_pdf(pdf_path)
    print(f"✅  PDF written  → {pdf_path}")
except ImportError:
    print("ℹ️   WeasyPrint not installed — PDF not generated.")
    print(f"    Open {html_path} in Chrome/Edge and use File → Print → Save as PDF.")
except Exception as e:
    print(f"⚠️   WeasyPrint error: {e}")
    print(f"    Open {html_path} in Chrome/Edge and use File → Print → Save as PDF.")
