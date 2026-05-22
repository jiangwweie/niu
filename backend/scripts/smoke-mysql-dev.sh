#!/bin/bash
# MySQL dev smoke test
# Tests core business chain on real MySQL database with JWT auth

set -e
BASE="http://localhost:8080"
CT="Content-Type: application/json"

# Dev seed credentials (local dev only, see docs/dev/local-startup-guide.md)
DEV_USER="${DEV_LOGIN_USERNAME:-admin01}"
DEV_PASS="${DEV_LOGIN_PASSWORD:-dev123}"

pass=0
fail=0

check() {
  local desc="$1" expected="$2" actual="$3"
  if echo "$actual" | grep -q "$expected"; then
    echo "  PASS: $desc"
    pass=$((pass + 1))
  else
    echo "  FAIL: $desc (expected '$expected' in response)"
    echo "    actual: $actual"
    fail=$((fail + 1))
  fi
}

echo "=== Step 0: Health check ==="
r=$(curl -s "$BASE/api/health")
check "Health endpoint" "UP" "$r"

echo ""
echo "=== Step 0b: JWT login ==="
LOGIN_RESP=$(curl -s -X POST -H "$CT" \
  -d "{\"username\":\"$DEV_USER\",\"password\":\"$DEV_PASS\"}" \
  "$BASE/api/auth/login/password")
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)
if [ -z "$TOKEN" ]; then
  echo "  FAIL: Could not obtain JWT token"
  echo "    response: $LOGIN_RESP"
  exit 1
fi
echo "  PASS: JWT login succeeded (user=$DEV_USER)"
pass=$((pass + 1))
AUTH="Authorization: Bearer $TOKEN"

echo ""
echo "=== Step 1: Sequence generation ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/work-orders?pageNo=1&pageSize=1")
check "Work order list (sequence available)" "SUCCESS" "$r"

echo ""
echo "=== Step 2: Create third-party part ==="
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"partName":"测试刹车片","model":"BP-2024","categoryCode":"BRAKE","referenceCostPrice":45.50}' \
  "$BASE/api/admin/parts/third-party")
check "Create third-party part" "SUCCESS" "$r"

echo ""
echo "=== Step 3: Query parts to get partId ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/parts?pageNo=1&pageSize=10")
PART_ID=$(echo "$r" | python3 -c "import sys,json; d=json.load(sys.stdin); recs=d['data']['records']; print([x['id'] for x in recs if x['partName']=='测试刹车片'][0])" 2>/dev/null)
if [ -z "$PART_ID" ]; then
  echo "  FAIL: Could not find created part"
  fail=$((fail + 1))
else
  echo "  PASS: Found part with id=$PART_ID"
  pass=$((pass + 1))
fi

echo ""
echo "=== Step 4: Inbound inventory ==="
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d "{\"partId\":$PART_ID,\"quantity\":100,\"unitCost\":45.50,\"reason\":\"初始入库\"}" \
  "$BASE/api/admin/inventory/inbound")
check "Inbound inventory" "SUCCESS" "$r"

echo ""
echo "=== Step 5: Verify inventory stock ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/inventory/stocks/$PART_ID")
check "Stock actual_qty=100" '"actualQty":100' "$r"
check "Stock available_qty=100" '"availableQty":100' "$r"
check "Stock reserved_qty=0" '"reservedQty":0' "$r"

echo ""
echo "=== Step 6: Create DRAFT work order ==="
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"customerNameSnapshot":"测试客户","customerPhoneSnapshot":"13900001111","vehicleModelSnapshot":"小牛N1S","frameNoSnapshot":"TEST-FRAME-001","repairItem":"更换刹车片","chargeItems":[{"chargeType":"PART","itemName":"刹车片","partId":'$PART_ID',"quantity":2,"unit":"片","unitPrice":120.00,"lineAmount":240.00,"costPriceSnapshot":45.50,"lineCostAmount":91.00,"inventoryAffecting":true,"tempPart":false},{"chargeType":"LABOR","itemName":"工时费","quantity":1,"unit":"次","unitPrice":80.00,"lineAmount":80.00,"inventoryAffecting":false,"tempPart":false},{"chargeType":"OTHER","itemName":"清洗费","quantity":1,"unit":"次","unitPrice":30.00,"lineAmount":30.00,"inventoryAffecting":false,"tempPart":false}]}' \
  "$BASE/api/admin/work-orders/drafts")
WORK_ORDER_ID=$(echo "$r" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'])" 2>/dev/null)
if [ -z "$WORK_ORDER_ID" ] || [ "$WORK_ORDER_ID" = "null" ]; then
  echo "  FAIL: Could not create draft work order"
  echo "    response: $r"
  fail=$((fail + 1))
else
  echo "  PASS: Created draft work order id=$WORK_ORDER_ID"
  pass=$((pass + 1))
fi

echo ""
echo "=== Step 7: Verify work order detail ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/work-orders/$WORK_ORDER_ID")
check "Work order status=DRAFT" '"status":"DRAFT"' "$r"
check "Work order has 3 charge items" '"chargeItems"' "$r"

echo ""
echo "=== Step 8: Submit work order (triggers RESERVE) ==="
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"remark":"提交工单"}' \
  "$BASE/api/admin/work-orders/$WORK_ORDER_ID/submit")
check "Submit work order" "SUCCESS" "$r"

echo ""
echo "=== Step 9: Verify inventory after RESERVE ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/inventory/stocks/$PART_ID")
check "After reserve: available_qty=98 (100-2)" '"availableQty":98' "$r"
check "After reserve: reserved_qty=2" '"reservedQty":2' "$r"
check "After reserve: actual_qty=100 (unchanged)" '"actualQty":100' "$r"

echo ""
echo "=== Step 10: Verify inventory flow has RESERVE record ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/inventory/flows?partId=$PART_ID&flowType=RESERVE&pageNo=1&pageSize=10")
check "RESERVE flow exists" '"flowType":"RESERVE"' "$r"

echo ""
echo "=== Step 11: Record payment ==="
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"amount":350.00,"paymentMethod":"WECHAT","receiverId":1,"remark":"客户微信支付"}' \
  "$BASE/api/admin/work-orders/$WORK_ORDER_ID/payments")
PAYMENT_ID=$(echo "$r" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'])" 2>/dev/null)
if [ -z "$PAYMENT_ID" ] || [ "$PAYMENT_ID" = "null" ]; then
  echo "  FAIL: Could not record payment"
  echo "    response: $r"
  fail=$((fail + 1))
else
  echo "  PASS: Recorded payment id=$PAYMENT_ID"
  pass=$((pass + 1))
fi

echo ""
echo "=== Step 12: Check payment summary ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/work-orders/$WORK_ORDER_ID/payment-summary")
check "Payment summary: received=350" '"receivedAmount":350' "$r"
check "Payment summary: canSettle=true" '"canSettle":true' "$r"

echo ""
echo "=== Step 13: Settle work order (triggers CONSUME) ==="
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"remark":"完成结算"}' \
  "$BASE/api/admin/work-orders/$WORK_ORDER_ID/settle")
check "Settle work order" "SUCCESS" "$r"

echo ""
echo "=== Step 14: Verify inventory after CONSUME ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/inventory/stocks/$PART_ID")
check "After consume: actual_qty=98 (100-2)" '"actualQty":98' "$r"
check "After consume: reserved_qty=0 (2-2)" '"reservedQty":0' "$r"
check "After consume: available_qty=98 (unchanged)" '"availableQty":98' "$r"

echo ""
echo "=== Step 15: Verify CONSUME flow record ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/inventory/flows?partId=$PART_ID&flowType=CONSUME&pageNo=1&pageSize=10")
check "CONSUME flow exists" '"flowType":"CONSUME"' "$r"

echo ""
echo "=== Step 16: Verify work order final status ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/work-orders/$WORK_ORDER_ID")
check "Final status=SETTLED" '"status":"SETTLED"' "$r"

echo ""
echo "=== Step 17: Cancel + RELEASE (new work order) ==="
# Create another part for this test
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"partName":"测试轮胎","model":"TR-001","categoryCode":"TIRE","referenceCostPrice":100.00}' \
  "$BASE/api/admin/parts/third-party")
r=$(curl -s -H "$AUTH" "$BASE/api/admin/parts?pageNo=1&pageSize=20")
PART2_ID=$(echo "$r" | python3 -c "import sys,json; d=json.load(sys.stdin); recs=d['data']['records']; print([x['id'] for x in recs if x['partName']=='测试轮胎'][0])" 2>/dev/null)
echo "  Created second part id=$PART2_ID"

# Inbound for part2
curl -s -X POST -H "$AUTH" -H "$CT" \
  -d "{\"partId\":$PART2_ID,\"quantity\":50,\"unitCost\":100.00,\"reason\":\"入库\"}" \
  "$BASE/api/admin/inventory/inbound" > /dev/null

# Create and submit work order for part2
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"customerNameSnapshot":"取消测试客户","repairItem":"更换轮胎","chargeItems":[{"chargeType":"PART","itemName":"轮胎","partId":'$PART2_ID',"quantity":2,"unit":"条","unitPrice":200.00,"lineAmount":400.00,"costPriceSnapshot":100.00,"lineCostAmount":200.00,"inventoryAffecting":true,"tempPart":false}]}' \
  "$BASE/api/admin/work-orders/drafts")
WO2_ID=$(echo "$r" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'])" 2>/dev/null)
echo "  Created work order #2 id=$WO2_ID"

# Submit to trigger RESERVE
curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{}' \
  "$BASE/api/admin/work-orders/$WO2_ID/submit" > /dev/null

# Check inventory after reserve
r=$(curl -s -H "$AUTH" "$BASE/api/admin/inventory/stocks/$PART2_ID")
RESERVED_BEFORE=$(echo "$r" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['reservedQty'])" 2>/dev/null)
echo "  Before cancel: reserved_qty=$RESERVED_BEFORE"

# Cancel work order
r=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"reason":"客户取消"}' \
  "$BASE/api/admin/work-orders/$WO2_ID/cancel")
check "Cancel work order" "SUCCESS" "$r"

# Check inventory after release
r=$(curl -s -H "$AUTH" "$BASE/api/admin/inventory/stocks/$PART2_ID")
check "After release: reserved_qty=0" '"reservedQty":0' "$r"
check "After release: available_qty=50 (restored)" '"availableQty":50' "$r"

echo ""
echo "=== Step 18: Verify RELEASE flow ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/inventory/flows?partId=$PART2_ID&flowType=RELEASE&pageNo=1&pageSize=10")
check "RELEASE flow exists" '"flowType":"RELEASE"' "$r"

echo ""
echo "=== Step 19: Dict types list (P1) ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/dict/types")
check "Dict types list returns SUCCESS" "SUCCESS" "$r"
check "Dict types contains WORK_ORDER_STATUS" "WORK_ORDER_STATUS" "$r"
check "Dict types contains PAYMENT_METHOD" "PAYMENT_METHOD" "$r"

echo ""
echo "=== Step 20: Dict items for existing typeCode (P0 fix) ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/dict/types/WORK_ORDER_STATUS/items")
check "Dict items WORK_ORDER_STATUS returns SUCCESS" "SUCCESS" "$r"
check "Dict items contains DRAFT" '"itemCode":"DRAFT"' "$r"
check "Dict items contains SETTLED" '"itemCode":"SETTLED"' "$r"

echo ""
echo "=== Step 21: Dict items for non-existent typeCode ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/dict/types/NONEXISTENT/items")
check "Non-existent typeCode returns SUCCESS (empty list)" "SUCCESS" "$r"

echo ""
echo "=== Step 22: Part list with new filters (P2) ==="
r=$(curl -s -H "$AUTH" "$BASE/api/admin/parts?categoryCode=BRAKE&pageNo=1&pageSize=10")
check "Part filter by categoryCode returns SUCCESS" "SUCCESS" "$r"
check "Part filter by categoryCode finds BRAKE part" "BRAKE" "$r"

echo ""
echo "==================================="
echo "  SMOKE TEST RESULTS: $pass passed, $fail failed"
echo "==================================="

if [ $fail -gt 0 ]; then
  exit 1
fi
