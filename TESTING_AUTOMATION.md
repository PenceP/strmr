# Automated Testing & Performance Monitoring

This document describes the automated testing system for the Strmr Android TV app, including memory leak detection, performance monitoring, and regular health checks.

## 🚀 Quick Start

```bash
# Run all tests (quick suite - ~2 minutes)
./scripts/run_all_tests.sh

# Run full comprehensive tests (~10 minutes)
./scripts/run_all_tests.sh full

# Run only memory tests (~5 minutes)
./scripts/run_all_tests.sh memory-only

# Run with specific device
./scripts/run_all_tests.sh quick emulator-5554
```

## 📋 Available Test Suites

### 1. Unit Tests with Memory Leak Detection

**File**: `app/src/test/java/com/strmr/ai/memory/MemoryLeakTest.kt`

**Tests**:
- DpadFocusManager memory usage with 1000+ focus changes
- ImagePreloader coroutine leak detection
- Large dataset simulation (10,000 items)
- Concurrent operations stress testing
- String concatenation memory efficiency

**Run**:
```bash
./gradlew testDebugUnitTest --tests '*MemoryLeakTest*'
```

### 2. Device Memory Monitoring

**File**: `scripts/memory_monitor.sh`

**Features**:
- Real-time memory usage tracking
- Automated app usage simulation
- Heap dump collection
- Memory threshold alerts
- Performance regression detection

**Usage**:
```bash
# Monitor for 5 minutes
./scripts/memory_monitor.sh 5

# Monitor specific device for 10 minutes
./scripts/memory_monitor.sh 10 emulator-5554
```

**Thresholds**:
- Max Heap: 100MB
- Max Native: 50MB
- Max Total PSS: 150MB

### 3. Performance Testing

**File**: `scripts/performance_test.sh`

**Tests**:
- Build time performance
- Unit test execution speed
- App startup time (cold start)
- Memory usage at startup
- Incremental compilation speed

**Usage**:
```bash
# Quick performance tests
./scripts/performance_test.sh quick

# Full performance suite
./scripts/performance_test.sh full
```

## 📊 Understanding Reports

### Memory Reports
Location: `build/memory_reports/memory_report_TIMESTAMP.txt`

**Key Metrics**:
```
Time,Heap(KB),Native(KB),Total_PSS(KB)
14:30:15,45678,12345,89012
14:30:20,46123,12398,89456
```

**Heap Dumps**: `build/heap_dumps/heap_*.hprof`
- Analyze with Android Studio Memory Profiler
- Look for memory leaks and object retention

### Performance Reports  
Location: `build/performance_reports/performance_report_TIMESTAMP.txt`

**Example Results**:
```
BUILD PERFORMANCE
=================
Clean Build Test
Time: 45.234s
PASSED: 45.234s (limit: 120000ms)

Unit Test Performance  
Time: 12.567s
PASSED: 12.567s (limit: 60000ms)
```

### Comprehensive Reports
Location: `build/test_reports/test_summary_TIMESTAMP.md`

**Contains**:
- All test results summary
- Success/failure rates
- System information
- Links to detailed reports

## 🔧 Integration with CI/CD

### GitHub Actions Example

```yaml
name: Performance & Memory Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v2
        
      - name: Run Comprehensive Tests
        run: ./scripts/run_all_tests.sh quick
        
      - name: Upload Test Reports
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-reports
          path: build/test_reports/
```

### Pre-commit Hook

```bash
#!/bin/sh
# .git/hooks/pre-commit

echo "Running memory leak tests..."
./gradlew testDebugUnitTest --tests '*MemoryLeakTest*'

if [ $? -ne 0 ]; then
    echo "❌ Memory tests failed. Commit blocked."
    exit 1
fi

echo "✅ Memory tests passed."
```

## 🚨 Failure Analysis

### Common Memory Issues

**High Heap Usage**:
```bash
# Check heap dumps
ls -la build/heap_dumps/
# Analyze with Android Studio or MAT
```

**Focus Manager Leak**:
```bash
# Run specific test
./gradlew testDebugUnitTest --tests '*DpadFocusManager*should*not*leak*'
```

**Image Preloader Issues**:
```bash
# Monitor during image preloading
./scripts/memory_monitor.sh 3
# Then check for coroutine leaks
```

### Performance Degradation

**Slow Build Times**:
```bash
# Profile build
./gradlew assembleDebug --profile
# Check build/reports/profile/
```

**High App Startup Time**:
```bash
# Use systrace
adb shell am start -W -n com.strmr.ai/.ui.MainActivity
```

## 📅 Regular Testing Schedule

### Daily (Automated)
- Unit tests with memory leak detection
- Quick performance tests
- Build verification

### Weekly
- Full memory monitoring (10 minutes)
- Performance regression testing
- Heap dump analysis

### Before Release
- Comprehensive test suite
- Device compatibility testing
- Memory profiling on low-end devices

## 🛠️ Customization

### Adjusting Thresholds

Edit `scripts/memory_monitor.sh`:
```bash
# Memory thresholds (in KB)
MAX_HEAP_SIZE=102400      # 100MB
MAX_NATIVE_SIZE=51200     # 50MB  
MAX_TOTAL_PSS=153600      # 150MB
```

Edit `scripts/performance_test.sh`:
```bash
# Performance thresholds
MAX_BUILD_TIME=120         # 2 minutes
MAX_TEST_TIME=60           # 1 minute  
MAX_APP_STARTUP_TIME=3000  # 3 seconds
```

### Adding Custom Tests

1. **Memory Tests**: Add to `MemoryLeakTest.kt`
2. **Performance**: Extend `performance_test.sh`
3. **Device Tests**: Modify `memory_monitor.sh`

### Integration with Monitoring

```bash
# Send results to monitoring system
./scripts/run_all_tests.sh quick | tee /tmp/test_results.log
curl -X POST "https://monitoring.example.com/metrics" \
  -d @/tmp/test_results.log
```

## 🎯 Success Criteria

### Memory Health
- ✅ All memory leak tests pass
- ✅ Heap usage stays under 100MB
- ✅ No memory warnings during 5min monitoring
- ✅ Successful garbage collection

### Performance Health  
- ✅ Unit tests complete in <60 seconds
- ✅ App startup in <3 seconds
- ✅ Build time <2 minutes (clean)
- ✅ No performance regressions

### Overall Health
- ✅ 100% unit test success rate
- ✅ Zero critical failures
- ✅ All thresholds met
- ✅ Reports generated successfully

## 🔗 Related Files

- `/scripts/memory_monitor.sh` - Device memory monitoring
- `/scripts/performance_test.sh` - Performance testing
- `/scripts/run_all_tests.sh` - Comprehensive test runner
- `/app/src/test/java/com/strmr/ai/memory/MemoryLeakTest.kt` - Memory leak unit tests
- `/ROW_IMPROVEMENT_CHECKLIST.md` - Implementation progress
- `/TESTING_GUIDELINES.md` - General testing standards

---

*Generated by Strmr automated testing system*