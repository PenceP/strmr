#!/bin/bash

# Performance Testing Script for Strmr Android TV App
# Tests compilation time, test execution speed, and app startup performance
# Usage: ./scripts/performance_test.sh [full|quick] [device_id]

set -e

# Configuration
TEST_MODE=${1:-"quick"}    # quick or full
DEVICE_ID=${2:-""}         # Optional device ID
RESULTS_DIR="build/performance_reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$RESULTS_DIR/performance_report_$TIMESTAMP.txt"

# Performance thresholds
MAX_BUILD_TIME=120         # 2 minutes for full build
MAX_TEST_TIME=60           # 1 minute for unit tests
MAX_APP_STARTUP_TIME=3000  # 3 seconds app startup

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}⚡ Strmr Performance Test Suite${NC}"
echo "=================================="
echo "Mode: $TEST_MODE"
echo "Timestamp: $TIMESTAMP"
echo ""

# Setup
mkdir -p "$RESULTS_DIR"

# Initialize report
cat > "$REPORT_FILE" << EOF
Strmr Performance Test Report
Generated: $(date)
Mode: $TEST_MODE
Host: $(hostname)
Java Version: $(java -version 2>&1 | head -1)

======================================

EOF

# Function to measure execution time
measure_time() {
    local start_time=$(date +%s%3N)
    "$@"
    local end_time=$(date +%s%3N)
    echo $((end_time - start_time))
}

# Function to run performance test
run_perf_test() {
    local test_name="$1"
    local command="$2"
    local max_time="$3"
    
    echo -e "${YELLOW}🧪 Running: $test_name${NC}"
    
    local execution_time
    execution_time=$(measure_time bash -c "$command")
    
    local seconds=$((execution_time / 1000))
    local milliseconds=$((execution_time % 1000))
    
    echo "Time: ${seconds}.${milliseconds}s" >> "$REPORT_FILE"
    
    if [ "$execution_time" -le "$max_time" ]; then
        echo -e "${GREEN}✅ PASSED: ${seconds}.${milliseconds}s (limit: ${max_time}ms)${NC}"
        echo "PASSED: ${seconds}.${milliseconds}s (limit: ${max_time}ms)" >> "$REPORT_FILE"
        return 0
    else
        echo -e "${RED}❌ FAILED: ${seconds}.${milliseconds}s (limit: ${max_time}ms)${NC}"
        echo "FAILED: ${seconds}.${milliseconds}s (limit: ${max_time}ms)" >> "$REPORT_FILE"
        return 1
    fi
}

echo "BUILD PERFORMANCE" >> "$REPORT_FILE"
echo "=================" >> "$REPORT_FILE"

# Clean build performance test
if [ "$TEST_MODE" = "full" ]; then
    echo -e "${BLUE}🧹 Clean Build Test${NC}"
    echo "Clean Build Test" >> "$REPORT_FILE"
    ./gradlew clean > /dev/null 2>&1
    
    if ! run_perf_test "Full Clean Build" "./gradlew assembleDebug" $((MAX_BUILD_TIME * 1000)); then
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "" >> "$REPORT_FILE"
fi

# Incremental build test
echo -e "${BLUE}🔄 Incremental Build Test${NC}"
echo "Incremental Build Test" >> "$REPORT_FILE"
if ! run_perf_test "Incremental Build" "./gradlew assembleDebug" 30000; then
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo "" >> "$REPORT_FILE"

echo "TEST PERFORMANCE" >> "$REPORT_FILE"
echo "================" >> "$REPORT_FILE"

# Unit test performance
echo -e "${BLUE}🧪 Unit Test Performance${NC}"
echo "Unit Test Performance" >> "$REPORT_FILE"
if ! run_perf_test "Unit Tests" "./gradlew testDebugUnitTest" $((MAX_TEST_TIME * 1000)); then
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Get test count and success rate
TEST_REPORT="app/build/reports/tests/testDebugUnitTest/index.html"
if [ -f "$TEST_REPORT" ]; then
    TEST_COUNT=$(grep -o '<div class="counter">[0-9]*</div>' "$TEST_REPORT" | head -1 | grep -o '[0-9]*')
    FAILURE_COUNT=$(grep -o '<div class="counter">[0-9]*</div>' "$TEST_REPORT" | sed -n '2p' | grep -o '[0-9]*')
    SUCCESS_RATE=$(grep -o '<div class="percent">[0-9]*%</div>' "$TEST_REPORT" | grep -o '[0-9]*')
    
    echo "Tests: $TEST_COUNT, Failures: $FAILURE_COUNT, Success Rate: ${SUCCESS_RATE}%" >> "$REPORT_FILE"
    echo -e "   📊 Tests: ${GREEN}$TEST_COUNT${NC}, Failures: ${RED}$FAILURE_COUNT${NC}, Success Rate: ${GREEN}${SUCCESS_RATE}%${NC}"
fi
echo "" >> "$REPORT_FILE"

# Memory-specific unit tests
echo -e "${BLUE}🧠 Memory Leak Tests${NC}"
echo "Memory Leak Tests" >> "$REPORT_FILE"
if ! run_perf_test "Memory Tests" "./gradlew testDebugUnitTest --tests '*MemoryLeakTest'" 15000; then
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo "" >> "$REPORT_FILE"

# Device tests (if device available)
if [ -n "$DEVICE_ID" ] || adb devices | grep -q "device$"; then
    echo "DEVICE PERFORMANCE" >> "$REPORT_FILE"
    echo "==================" >> "$REPORT_FILE"
    
    DEVICE_CMD="adb"
    [ -n "$DEVICE_ID" ] && DEVICE_CMD="adb -s $DEVICE_ID"
    
    # App startup time test
    echo -e "${BLUE}🚀 App Startup Test${NC}"
    echo "App Startup Test" >> "$REPORT_FILE"
    
    # Force stop app first
    $DEVICE_CMD shell am force-stop com.strmr.ai 2>/dev/null || true
    sleep 1
    
    # Measure cold startup time
    startup_start=$(date +%s%3N)
    $DEVICE_CMD shell am start -W -n com.strmr.ai/.ui.MainActivity | grep "TotalTime" | cut -d':' -f2 | tr -d ' ' > /tmp/startup_time.txt
    startup_time=$(cat /tmp/startup_time.txt)
    
    if [ -n "$startup_time" ] && [ "$startup_time" -le "$MAX_APP_STARTUP_TIME" ]; then
        echo -e "${GREEN}✅ App Startup: ${startup_time}ms (limit: ${MAX_APP_STARTUP_TIME}ms)${NC}"
        echo "PASSED: ${startup_time}ms (limit: ${MAX_APP_STARTUP_TIME}ms)" >> "$REPORT_FILE"
    else
        echo -e "${RED}❌ App Startup: ${startup_time}ms (limit: ${MAX_APP_STARTUP_TIME}ms)${NC}"
        echo "FAILED: ${startup_time}ms (limit: ${MAX_APP_STARTUP_TIME}ms)" >> "$REPORT_FILE"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    # Memory usage test
    echo -e "${BLUE}💾 Memory Usage Check${NC}"
    echo "Memory Usage Check" >> "$REPORT_FILE"
    
    sleep 3  # Let app fully load
    MEMORY_INFO=$($DEVICE_CMD shell dumpsys meminfo com.strmr.ai | grep "TOTAL PSS:" | awk '{print $3}' | head -1)
    MEMORY_KB=${MEMORY_INFO//,/}
    
    if [ -n "$MEMORY_KB" ]; then
        MEMORY_MB=$((MEMORY_KB / 1024))
        echo "Current Memory Usage: ${MEMORY_MB}MB (${MEMORY_KB}KB)" >> "$REPORT_FILE"
        echo -e "   📊 Memory Usage: ${GREEN}${MEMORY_MB}MB${NC}"
        
        # Check if memory usage is reasonable (less than 200MB)
        if [ "$MEMORY_KB" -lt 204800 ]; then
            echo "Memory usage: PASSED" >> "$REPORT_FILE"
        else
            echo "Memory usage: WARNING - High memory usage" >> "$REPORT_FILE"
        fi
    fi
    echo "" >> "$REPORT_FILE"
fi

# Gradle build cache performance
echo "BUILD CACHE PERFORMANCE" >> "$REPORT_FILE"
echo "======================" >> "$REPORT_FILE"

echo -e "${BLUE}⚡ Build Cache Test${NC}"
echo "Build Cache Performance" >> "$REPORT_FILE"

# Test incremental compilation
touch app/src/main/java/com/strmr/ai/ui/MainActivity.kt
if ! run_perf_test "Incremental Compilation" "./gradlew compileDebugKotlin" 10000; then
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo "" >> "$REPORT_FILE"

# Code analysis performance
if [ "$TEST_MODE" = "full" ]; then
    echo -e "${BLUE}🔍 Static Analysis${NC}"
    echo "Static Analysis Performance" >> "$REPORT_FILE"
    
    if ! run_perf_test "Lint Check" "./gradlew lintDebug" 45000; then
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "" >> "$REPORT_FILE"
fi

# Performance summary
echo "PERFORMANCE SUMMARY" >> "$REPORT_FILE"
echo "==================" >> "$REPORT_FILE"
echo "Failed Tests: ${FAILED_TESTS:-0}" >> "$REPORT_FILE"
echo "Total Duration: $((($(date +%s) - $(date -d "$TIMESTAMP" +%s 2>/dev/null || echo $(date +%s))) / 60)) minutes" >> "$REPORT_FILE" 2>/dev/null || echo "Total Duration: N/A" >> "$REPORT_FILE"

# Final results
echo ""
echo -e "${BLUE}📊 PERFORMANCE TEST COMPLETE${NC}"
echo "=============================="
echo -e "📁 Full report: ${YELLOW}$REPORT_FILE${NC}"
echo -e "❌ Failed tests: ${RED}${FAILED_TESTS:-0}${NC}"

if [ "${FAILED_TESTS:-0}" -eq 0 ]; then
    echo -e "${GREEN}✅ ALL PERFORMANCE TESTS PASSED${NC}"
    exit 0
else
    echo -e "${RED}❌ PERFORMANCE TESTS FAILED${NC}"
    echo "Check the report for details: $REPORT_FILE"
    exit 1
fi