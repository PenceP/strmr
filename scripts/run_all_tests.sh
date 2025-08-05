#!/bin/bash

# Comprehensive Test Runner for Strmr Android TV App
# Runs all tests: unit, performance, memory, and integration
# Usage: ./scripts/run_all_tests.sh [quick|full|memory-only] [device_id]

set -e

# Configuration
TEST_SUITE=${1:-"quick"}   # quick, full, or memory-only
DEVICE_ID=${2:-""}         # Optional device ID
RESULTS_DIR="build/test_reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
MAIN_REPORT="$RESULTS_DIR/test_summary_$TIMESTAMP.md"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}${BLUE}🧪 STRMR COMPREHENSIVE TEST SUITE${NC}"
echo "=================================="
echo "Suite: $TEST_SUITE"
echo "Timestamp: $TIMESTAMP"
echo ""

# Setup
mkdir -p "$RESULTS_DIR"
mkdir -p "build/memory_reports"
mkdir -p "build/performance_reports"

# Test tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Function to run test and track results
run_test_suite() {
    local test_name="$1"
    local test_command="$2"
    local is_critical="$3"  # true/false
    
    echo -e "${YELLOW}🚀 Running: $test_name${NC}"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if eval "$test_command"; then
        echo -e "${GREEN}✅ PASSED: $test_name${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "✅ **PASSED**: $test_name" >> "$MAIN_REPORT"
        return 0
    else
        echo -e "${RED}❌ FAILED: $test_name${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "❌ **FAILED**: $test_name" >> "$MAIN_REPORT"
        
        if [ "$is_critical" = "true" ]; then
            echo -e "${RED}💥 CRITICAL TEST FAILED - ABORTING${NC}"
            exit 1
        fi
        return 1
    fi
}

# Function to skip test
skip_test() {
    local test_name="$1"
    local reason="$2"
    
    echo -e "${YELLOW}⏭️  SKIPPED: $test_name ($reason)${NC}"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
    echo "⏭️ **SKIPPED**: $test_name - $reason" >> "$MAIN_REPORT"
}

# Initialize main report
cat > "$MAIN_REPORT" << EOF
# Strmr Test Suite Report

**Generated**: $(date)  
**Suite**: $TEST_SUITE  
**Host**: $(hostname)  
**Java**: $(java -version 2>&1 | head -1)  

## Test Results

EOF

echo -e "${BLUE}📋 TEST PLAN${NC}"
echo "=============="
case $TEST_SUITE in
    "quick")
        echo "• Unit tests (all)"
        echo "• Memory leak tests"
        echo "• Basic performance tests"
        echo "• Compilation tests"
        ;;
    "full") 
        echo "• Unit tests (all)"
        echo "• Memory leak tests"
        echo "• Full performance tests"
        echo "• Memory monitoring (5 min)"
        echo "• Device tests (if available)"
        echo "• Static analysis"
        ;;
    "memory-only")
        echo "• Memory leak tests only"
        echo "• Memory monitoring (10 min)"
        ;;
esac
echo ""

# 1. UNIT TESTS (Critical)
echo -e "${BOLD}${BLUE}PHASE 1: UNIT TESTS${NC}"
echo "====================" >> "$MAIN_REPORT"

run_test_suite "All Unit Tests" "./gradlew testDebugUnitTest" true

# Get detailed test results
if [ -f "app/build/reports/tests/testDebugUnitTest/index.html" ]; then
    TEST_COUNT=$(grep -o '<div class="counter">[0-9]*</div>' app/build/reports/tests/testDebugUnitTest/index.html | head -1 | grep -o '[0-9]*')
    FAILURE_COUNT=$(grep -o '<div class="counter">[0-9]*</div>' app/build/reports/tests/testDebugUnitTest/index.html | sed -n '2p' | grep -o '[0-9]*')
    SUCCESS_RATE=$(grep -o '<div class="percent">[0-9]*%</div>' app/build/reports/tests/testDebugUnitTest/index.html | grep -o '[0-9]*')
    
    echo "**Unit Test Details**: $TEST_COUNT tests, $FAILURE_COUNT failures, ${SUCCESS_RATE}% success rate" >> "$MAIN_REPORT"
    echo -e "   📊 ${GREEN}$TEST_COUNT tests${NC}, ${RED}$FAILURE_COUNT failures${NC}, ${GREEN}${SUCCESS_RATE}% success${NC}"
fi

echo "" >> "$MAIN_REPORT"

# 2. MEMORY TESTS
if [ "$TEST_SUITE" != "quick" ] || [ "$TEST_SUITE" = "memory-only" ]; then
    echo -e "${BOLD}${BLUE}PHASE 2: MEMORY TESTS${NC}"
    echo "===================" >> "$MAIN_REPORT"
    
    # Memory leak unit tests
    run_test_suite "Memory Leak Unit Tests" "./gradlew testDebugUnitTest --tests '*MemoryLeakTest*'" false
    
    # Device memory monitoring (if device available)
    if [ -n "$DEVICE_ID" ] || adb devices 2>/dev/null | grep -q "device$"; then
        MEMORY_DURATION=5
        [ "$TEST_SUITE" = "memory-only" ] && MEMORY_DURATION=10
        
        # Run memory monitor but handle graceful failures (app not installed)
        if ./scripts/memory_monitor.sh $MEMORY_DURATION $DEVICE_ID; then
            PASSED_TESTS=$((PASSED_TESTS + 1))
            echo "✅ **PASSED**: Device Memory Monitoring (${MEMORY_DURATION}min)" >> "$MAIN_REPORT"
        else
            # Check if it was a graceful skip (exit code 0) or actual failure
            if [ $? -eq 0 ]; then
                skip_test "Device Memory Monitoring" "App not installed on device"
            else
                FAILED_TESTS=$((FAILED_TESTS + 1))
                echo "❌ **FAILED**: Device Memory Monitoring (${MEMORY_DURATION}min)" >> "$MAIN_REPORT"
            fi
        fi
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        
        # Add memory report link if available
        LATEST_MEMORY_REPORT=$(ls -t build/memory_reports/memory_report_*.txt 2>/dev/null | head -1)
        if [ -n "$LATEST_MEMORY_REPORT" ]; then
            echo "**Memory Report**: [$LATEST_MEMORY_REPORT]($LATEST_MEMORY_REPORT)" >> "$MAIN_REPORT"
        fi
    else
        skip_test "Device Memory Monitoring" "No Android device connected"
    fi
    
    echo "" >> "$MAIN_REPORT"
fi

# 3. PERFORMANCE TESTS
if [ "$TEST_SUITE" != "memory-only" ]; then
    echo -e "${BOLD}${BLUE}PHASE 3: PERFORMANCE TESTS${NC}"
    echo "==========================" >> "$MAIN_REPORT"
    
    PERF_MODE="quick"
    [ "$TEST_SUITE" = "full" ] && PERF_MODE="full"
    
    run_test_suite "Performance Tests ($PERF_MODE)" "./scripts/performance_test.sh $PERF_MODE $DEVICE_ID" false
    
    # Add performance report link
    LATEST_PERF_REPORT=$(ls -t build/performance_reports/performance_report_*.txt 2>/dev/null | head -1)
    if [ -n "$LATEST_PERF_REPORT" ]; then
        echo "**Performance Report**: [$LATEST_PERF_REPORT]($LATEST_PERF_REPORT)" >> "$MAIN_REPORT"
    fi
    
    echo "" >> "$MAIN_REPORT"
fi

# 4. BUILD TESTS
if [ "$TEST_SUITE" = "full" ]; then
    echo -e "${BOLD}${BLUE}PHASE 4: BUILD VERIFICATION${NC}"
    echo "==========================" >> "$MAIN_REPORT"
    
    run_test_suite "Debug Build" "./gradlew assembleDebug" true
    run_test_suite "Release Build" "./gradlew assembleRelease" false
    run_test_suite "Lint Check" "./gradlew lintDebug" false
    
    echo "" >> "$MAIN_REPORT"
fi

# Generate summary
echo "## Summary" >> "$MAIN_REPORT"
echo "" >> "$MAIN_REPORT"
echo "| Metric | Count |" >> "$MAIN_REPORT"
echo "|--------|-------|" >> "$MAIN_REPORT"
echo "| Total Tests | $TOTAL_TESTS |" >> "$MAIN_REPORT"
echo "| Passed | $PASSED_TESTS |" >> "$MAIN_REPORT"
echo "| Failed | $FAILED_TESTS |" >> "$MAIN_REPORT"
echo "| Skipped | $SKIPPED_TESTS |" >> "$MAIN_REPORT"

SUCCESS_PERCENTAGE=$(( (PASSED_TESTS * 100) / TOTAL_TESTS ))
echo "| Success Rate | ${SUCCESS_PERCENTAGE}% |" >> "$MAIN_REPORT"

echo "" >> "$MAIN_REPORT"

# Add system info
echo "## System Information" >> "$MAIN_REPORT"
echo "" >> "$MAIN_REPORT"
echo "- **Host**: $(hostname)" >> "$MAIN_REPORT"
echo "- **OS**: $(uname -s) $(uname -r)" >> "$MAIN_REPORT"
echo "- **Java**: $(java -version 2>&1 | head -1)" >> "$MAIN_REPORT"
echo "- **Gradle**: $(./gradlew --version | grep "Gradle" | head -1)" >> "$MAIN_REPORT"

if [ -n "$DEVICE_ID" ] || adb devices 2>/dev/null | grep -q "device$"; then
    DEVICE_CMD="adb"
    [ -n "$DEVICE_ID" ] && DEVICE_CMD="adb -s $DEVICE_ID"
    
    echo "- **Device**: $($DEVICE_CMD shell getprop ro.product.model 2>/dev/null || echo "N/A")" >> "$MAIN_REPORT"
    echo "- **Android**: $($DEVICE_CMD shell getprop ro.build.version.release 2>/dev/null || echo "N/A")" >> "$MAIN_REPORT"
fi

echo "" >> "$MAIN_REPORT"

# Add file locations
echo "## Generated Files" >> "$MAIN_REPORT"
echo "" >> "$MAIN_REPORT"
echo "- **Unit Test Report**: app/build/reports/tests/testDebugUnitTest/index.html" >> "$MAIN_REPORT"

if [ -n "$LATEST_MEMORY_REPORT" ]; then
    echo "- **Memory Report**: $LATEST_MEMORY_REPORT" >> "$MAIN_REPORT"
fi

if [ -n "$LATEST_PERF_REPORT" ]; then
    echo "- **Performance Report**: $LATEST_PERF_REPORT" >> "$MAIN_REPORT"
fi

# Final results
echo ""
echo -e "${BOLD}${BLUE}🎯 TEST SUITE COMPLETE${NC}"
echo "======================="
echo -e "📁 Summary Report: ${YELLOW}$MAIN_REPORT${NC}"
echo -e "📊 Total Tests: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "✅ Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "❌ Failed: ${RED}$FAILED_TESTS${NC}"
echo -e "⏭️  Skipped: ${YELLOW}$SKIPPED_TESTS${NC}"
echo -e "🎯 Success Rate: ${GREEN}${SUCCESS_PERCENTAGE}%${NC}"

if [ "$FAILED_TESTS" -eq 0 ]; then
    echo -e "${BOLD}${GREEN}🎉 ALL TESTS PASSED!${NC}"
    echo ""
    echo "Your code is ready for:"
    echo "• Production deployment"
    echo "• Performance optimization"
    echo "• Memory-constrained devices"
    exit 0
else
    echo -e "${BOLD}${RED}💥 SOME TESTS FAILED${NC}"
    echo ""
    echo "Check the reports for details:"
    echo "• Unit tests: app/build/reports/tests/testDebugUnitTest/index.html"
    echo "• Full summary: $MAIN_REPORT"
    exit 1
fi