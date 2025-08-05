#!/bin/bash

# Memory and Performance Monitoring Script for Strmr Android TV App
# Usage: ./scripts/memory_monitor.sh [test_duration_minutes] [device_id]

set -e

# Configuration
APP_PACKAGE="com.strmr.ai"
TEST_DURATION=${1:-5}  # Default 5 minutes
DEVICE_ID=${2:-""}     # Default: first available device
RESULTS_DIR="build/memory_reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="$RESULTS_DIR/memory_report_$TIMESTAMP.txt"

# Memory thresholds (in KB)
MAX_HEAP_SIZE=102400      # 100MB
MAX_NATIVE_SIZE=51200     # 50MB  
MAX_TOTAL_PSS=153600      # 150MB

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 Strmr Memory & Performance Monitor${NC}"
echo "========================================"

# Setup
mkdir -p "$RESULTS_DIR"
mkdir -p "build/heap_dumps"

# Device selection
if [ -z "$DEVICE_ID" ]; then
    DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo -e "${RED}❌ No Android devices found${NC}"
        exit 1
    elif [ "$DEVICE_COUNT" -gt 1 ]; then
        echo -e "${YELLOW}⚠️  Multiple devices found. Using first available device.${NC}"
        echo "   Use: $0 $TEST_DURATION <device_id> to specify device"
    fi
    DEVICE_CMD="adb"
else
    DEVICE_CMD="adb -s $DEVICE_ID"
    echo "📱 Using device: $DEVICE_ID"
fi

# Check if app is installed
if ! $DEVICE_CMD shell pm list packages | grep -q "$APP_PACKAGE"; then
    echo -e "${RED}❌ App $APP_PACKAGE not installed on device${NC}"
    echo "   Install with: ./gradlew installDebug"
    echo ""
    echo "MEMORY MONITORING SKIPPED" >> "$REPORT_FILE"
    echo "=========================" >> "$REPORT_FILE"
    echo "Reason: App not installed on device" >> "$REPORT_FILE"
    echo "Install command: ./gradlew installDebug" >> "$REPORT_FILE"
    echo ""
    echo -e "${YELLOW}⚠️  SKIPPING: Memory monitoring (app not installed)${NC}"
    echo -e "${BLUE}💡 To enable device testing:${NC}"
    echo "   1. ./gradlew installDebug"
    echo "   2. Re-run this script"
    exit 0  # Exit gracefully instead of failure
fi

echo "📊 Test Duration: ${TEST_DURATION} minutes"
echo "📁 Results will be saved to: $REPORT_FILE"
echo ""

# Initialize report
cat > "$REPORT_FILE" << EOF
Strmr Memory & Performance Report
Generated: $(date)
Device: $($DEVICE_CMD shell getprop ro.product.model)
Android Version: $($DEVICE_CMD shell getprop ro.build.version.release)
Test Duration: ${TEST_DURATION} minutes
App Package: $APP_PACKAGE

======================================

EOF

# Function to get memory stats
get_memory_stats() {
    local timestamp=$1
    local meminfo
    
    # Get detailed memory info
    meminfo=$($DEVICE_CMD shell dumpsys meminfo "$APP_PACKAGE" | head -20)
    
    # Extract key metrics
    local heap_size=$(echo "$meminfo" | grep "TOTAL" | awk '{print $3}' | head -1)
    local native_size=$(echo "$meminfo" | grep "Native Heap" | awk '{print $4}')
    local total_pss=$(echo "$meminfo" | grep "TOTAL PSS:" | awk '{print $3}')
    
    # Clean up values (remove commas)
    heap_size=${heap_size//,/}
    native_size=${native_size//,/}
    total_pss=${total_pss//,/}
    
    # Default to 0 if empty
    heap_size=${heap_size:-0}
    native_size=${native_size:-0}
    total_pss=${total_pss:-0}
    
    echo "$timestamp,$heap_size,$native_size,$total_pss"
}

# Function to check memory thresholds
check_thresholds() {
    local heap=$1
    local native=$2
    local total=$3
    local timestamp=$4
    
    local warnings=0
    
    if [ "$heap" -gt "$MAX_HEAP_SIZE" ]; then
        echo -e "${RED}⚠️  [$timestamp] HEAP WARNING: ${heap}KB > ${MAX_HEAP_SIZE}KB${NC}"
        warnings=$((warnings + 1))
    fi
    
    if [ "$native" -gt "$MAX_NATIVE_SIZE" ]; then
        echo -e "${RED}⚠️  [$timestamp] NATIVE WARNING: ${native}KB > ${MAX_NATIVE_SIZE}KB${NC}"
        warnings=$((warnings + 1))
    fi
    
    if [ "$total" -gt "$MAX_TOTAL_PSS" ]; then
        echo -e "${RED}⚠️  [$timestamp] TOTAL PSS WARNING: ${total}KB > ${MAX_TOTAL_PSS}KB${NC}"
        warnings=$((warnings + 1))
    fi
    
    return $warnings
}

# Function to simulate app usage
simulate_usage() {
    echo "🤖 Simulating app usage..."
    
    # Launch app
    $DEVICE_CMD shell am start -n "$APP_PACKAGE/.ui.MainActivity"
    sleep 3
    
    # Simulate navigation and scrolling
    for i in {1..5}; do
        echo "   Scroll simulation $i/5"
        
        # Simulate D-pad navigation
        $DEVICE_CMD shell input keyevent KEYCODE_DPAD_RIGHT
        sleep 1
        $DEVICE_CMD shell input keyevent KEYCODE_DPAD_DOWN
        sleep 1
        $DEVICE_CMD shell input keyevent KEYCODE_DPAD_LEFT
        sleep 1
        
        # Simulate item selection
        $DEVICE_CMD shell input keyevent KEYCODE_DPAD_CENTER
        sleep 2
        $DEVICE_CMD shell input keyevent KEYCODE_BACK
        sleep 1
    done
}

# Function to force garbage collection
force_gc() {
    echo "🗑️  Forcing garbage collection..."
    $DEVICE_CMD shell am send-trim-memory "$APP_PACKAGE" RUNNING_CRITICAL
    sleep 2
}

# Function to take heap dump
take_heap_dump() {
    local timestamp=$1
    local heap_file="build/heap_dumps/heap_${timestamp}.hprof"
    
    echo "💾 Taking heap dump..."
    $DEVICE_CMD shell am dumpheap "$APP_PACKAGE" "/data/local/tmp/heap_tmp.hprof"
    sleep 3
    $DEVICE_CMD pull "/data/local/tmp/heap_tmp.hprof" "$heap_file" 2>/dev/null || true
    $DEVICE_CMD shell rm "/data/local/tmp/heap_tmp.hprof" 2>/dev/null || true
    
    if [ -f "$heap_file" ]; then
        echo "   Heap dump saved: $heap_file"
    else
        echo "   ⚠️  Failed to save heap dump"
    fi
}

# Main monitoring loop
echo "🚀 Starting memory monitoring..."
echo "Time,Heap(KB),Native(KB),Total_PSS(KB)" >> "$REPORT_FILE"

total_warnings=0
max_heap=0
max_native=0
max_total=0

start_time=$(date +%s)
end_time=$((start_time + TEST_DURATION * 60))

# Take initial heap dump
take_heap_dump "start"

# Initial app launch and usage simulation
simulate_usage

sample_count=0
while [ $(date +%s) -lt $end_time ]; do
    current_time=$(date +"%H:%M:%S")
    sample_count=$((sample_count + 1))
    
    # Get memory stats
    stats=$(get_memory_stats "$current_time")
    heap=$(echo "$stats" | cut -d',' -f2)
    native=$(echo "$stats" | cut -d',' -f3)
    total=$(echo "$stats" | cut -d',' -f4)
    
    # Track maximums
    [ "$heap" -gt "$max_heap" ] && max_heap=$heap
    [ "$native" -gt "$max_native" ] && max_native=$native
    [ "$total" -gt "$max_total" ] && max_total=$total
    
    # Log to file
    echo "$stats" >> "$REPORT_FILE"
    
    # Check thresholds and display current stats
    if check_thresholds "$heap" "$native" "$total" "$current_time"; then
        total_warnings=$((total_warnings + $?))
    fi
    
    echo -e "${GREEN}[$current_time] Heap: ${heap}KB | Native: ${native}KB | Total: ${total}KB${NC}"
    
    # Periodic actions
    if [ $((sample_count % 10)) -eq 0 ]; then
        # Every 10 samples (~50 seconds), simulate more usage
        simulate_usage
    fi
    
    if [ $((sample_count % 20)) -eq 0 ]; then
        # Every 20 samples (~100 seconds), force GC and take heap dump
        force_gc
        take_heap_dump "sample_$sample_count"
    fi
    
    sleep 5
done

# Final heap dump
take_heap_dump "end"

# Generate summary
echo "" >> "$REPORT_FILE"
echo "SUMMARY" >> "$REPORT_FILE"
echo "=======" >> "$REPORT_FILE"
echo "Total Samples: $sample_count" >> "$REPORT_FILE"
echo "Total Warnings: $total_warnings" >> "$REPORT_FILE"
echo "Max Heap Usage: ${max_heap}KB" >> "$REPORT_FILE"
echo "Max Native Usage: ${max_native}KB" >> "$REPORT_FILE"
echo "Max Total PSS: ${max_total}KB" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Heap dump analysis
heap_dumps=$(ls build/heap_dumps/heap_*.hprof 2>/dev/null | wc -l || echo "0")
echo "Heap Dumps Collected: $heap_dumps" >> "$REPORT_FILE"

if [ "$heap_dumps" -gt 0 ]; then
    echo "Heap Dump Locations:" >> "$REPORT_FILE"
    ls -la build/heap_dumps/heap_*.hprof >> "$REPORT_FILE" 2>/dev/null || true
fi

# Final report
echo ""
echo -e "${BLUE}📊 MEMORY TEST COMPLETE${NC}"
echo "========================="
echo -e "📁 Full report: ${YELLOW}$REPORT_FILE${NC}"
echo -e "📈 Total samples: ${GREEN}$sample_count${NC}"
echo -e "⚠️  Total warnings: ${RED}$total_warnings${NC}"
echo -e "🔥 Peak heap: ${YELLOW}${max_heap}KB${NC}"
echo -e "💾 Peak native: ${YELLOW}${max_native}KB${NC}"
echo -e "📊 Peak total PSS: ${YELLOW}${max_total}KB${NC}"

# Pass/Fail determination
if [ "$total_warnings" -eq 0 ] && [ "$max_total" -lt "$MAX_TOTAL_PSS" ]; then
    echo -e "${GREEN}✅ MEMORY TEST PASSED${NC}"
    exit 0
else
    echo -e "${RED}❌ MEMORY TEST FAILED${NC}"
    exit 1
fi