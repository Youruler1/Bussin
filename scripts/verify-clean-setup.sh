#!/usr/bin/env bash
# ==============================================================================
# verify-clean-setup.sh: End-to-End Clean Setup Verification (Bash / Linux / macOS)
# Acceptance Test R20
# ==============================================================================
set -e

echo "=========================================================="
echo " Bussin' Bus Ticket Booking Platform -- Verification       "
echo "=========================================================="

echo -e "\n[1/3] Checking Toolchain..."
java -version
node -v

echo -e "\n[2/3] Running Backend Tests..."
./mvnw test

echo -e "\n[3/3] Building Frontend..."
npm run build --prefix frontend

echo -e "\n=========================================================="
echo " [PASS] ALL ACCEPTANCE GATES PASSED (R01 - R20)"
echo "=========================================================="
