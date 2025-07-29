#!/usr/bin/env python3
"""
Compose Compiler Metrics Analyzer
Analyzes the Compose compiler metrics to identify performance issues
"""

import json
import os
import sys
from pathlib import Path
from typing import Dict, List, Any

class ComposeMetricsAnalyzer:
    def __init__(self, metrics_dir: str):
        self.metrics_dir = Path(metrics_dir)
        if not self.metrics_dir.exists():
            print(f"❌ Metrics directory not found: {metrics_dir}")
            print("🏗️ Run './gradlew assembleDebug' first to generate metrics")
            sys.exit(1)
    
    def analyze_metrics(self):
        """Analyze all Compose compiler metrics"""
        print("🔍 Analyzing Compose Compiler Metrics")
        print("=" * 50)
        
        # Analyze composable metrics
        self._analyze_composables()
        
        # Analyze class stability
        self._analyze_stability()
        
        # Analyze recomposition data
        self._analyze_recomposition()
        
        print("\n✅ Analysis complete!")
    
    def _analyze_composables(self):
        """Analyze composable function metrics"""
        print("\n📊 Composable Function Analysis")
        print("-" * 30)
        
        composable_files = list(self.metrics_dir.glob("*-composables.txt"))
        if not composable_files:
            print("⚠️ No composable metrics found")
            return
        
        for file in composable_files:
            print(f"\n📄 {file.name}")
            try:
                with open(file, 'r') as f:
                    content = f.read()
                    
                # Count different types of composables
                skippable = content.count("skippable")
                restartable = content.count("restartable")
                readonly = content.count("readonly")
                
                print(f"  ✅ Skippable: {skippable}")
                print(f"  🔄 Restartable: {restartable}")
                print(f"  📖 Readonly: {readonly}")
                
                # Look for performance issues
                if "unskippable" in content:
                    unskippable_count = content.count("unskippable")
                    print(f"  ⚠️ Unskippable: {unskippable_count}")
                    print("     Consider adding @Stable/@Immutable annotations")
                
            except Exception as e:
                print(f"  ❌ Error reading {file}: {e}")
    
    def _analyze_stability(self):
        """Analyze class stability metrics"""
        print("\n🏗️ Class Stability Analysis")
        print("-" * 30)
        
        stability_files = list(self.metrics_dir.glob("*-classes.txt"))
        if not stability_files:
            print("⚠️ No stability metrics found")
            return
        
        stable_classes = []
        unstable_classes = []
        
        for file in stability_files:
            print(f"\n📄 {file.name}")
            try:
                with open(file, 'r') as f:
                    lines = f.readlines()
                    
                for line in lines:
                    line = line.strip()
                    if "stable class" in line:
                        stable_classes.append(line)
                    elif "unstable class" in line:
                        unstable_classes.append(line)
                
            except Exception as e:
                print(f"  ❌ Error reading {file}: {e}")
        
        print(f"\n✅ Stable classes: {len(stable_classes)}")
        print(f"⚠️ Unstable classes: {len(unstable_classes)}")
        
        if unstable_classes:
            print("\n🔧 Unstable classes (consider @Stable/@Immutable):")
            for cls in unstable_classes[:10]:  # Show first 10
                print(f"  • {cls}")
            if len(unstable_classes) > 10:
                print(f"  ... and {len(unstable_classes) - 10} more")
    
    def _analyze_recomposition(self):
        """Analyze recomposition behavior"""
        print("\n🔄 Recomposition Analysis")
        print("-" * 30)
        
        # Look for module info files
        module_files = list(self.metrics_dir.glob("*-module.json"))
        if not module_files:
            print("⚠️ No module metrics found")
            return
        
        for file in module_files:
            print(f"\n📄 {file.name}")
            try:
                with open(file, 'r') as f:
                    data = json.load(f)
                    
                # Analyze module data
                if "skippableComposables" in data:
                    skippable = data["skippableComposables"]
                    total = data.get("composables", 0)
                    percentage = (skippable / total * 100) if total > 0 else 0
                    print(f"  📈 Skippable composables: {skippable}/{total} ({percentage:.1f}%)")
                    
                    if percentage < 80:
                        print("  ⚠️ Low skippability - consider optimization")
                    else:
                        print("  ✅ Good skippability rate")
                
                if "restartableComposables" in data:
                    restartable = data["restartableComposables"]
                    print(f"  🔄 Restartable composables: {restartable}")
                
            except Exception as e:
                print(f"  ❌ Error reading {file}: {e}")
    
    def generate_report(self, output_file: str = "compose_performance_report.md"):
        """Generate a comprehensive performance report"""
        print(f"\n📝 Generating detailed report: {output_file}")
        
        report_lines = [
            "# Compose Performance Analysis Report",
            f"*Generated from metrics in: {self.metrics_dir}*",
            "",
            "## Summary",
            "",
            "This report analyzes the Compose compiler metrics to identify performance optimization opportunities.",
            "",
            "## Key Metrics",
            "",
        ]
        
        # Add composable analysis
        composable_files = list(self.metrics_dir.glob("*-composables.txt"))
        if composable_files:
            report_lines.extend([
                "### Composable Functions",
                "",
                "| File | Skippable | Restartable | Readonly | Issues |",
                "|------|-----------|-------------|----------|--------|",
            ])
            
            for file in composable_files:
                try:
                    with open(file, 'r') as f:
                        content = f.read()
                    
                    skippable = content.count("skippable")
                    restartable = content.count("restartable")
                    readonly = content.count("readonly")
                    unskippable = content.count("unskippable")
                    
                    issues = "⚠️ Unskippable functions" if unskippable > 0 else "✅ Good"
                    
                    report_lines.append(
                        f"| {file.name} | {skippable} | {restartable} | {readonly} | {issues} |"
                    )
                    
                except Exception:
                    continue
        
        report_lines.extend([
            "",
            "## Recommendations",
            "",
            "1. **Add @Stable/@Immutable annotations** to data classes used in Compose",
            "2. **Use remember() for expensive calculations** that don't need to recompute on every composition",
            "3. **Use derivedStateOf()** for computed values that depend on other state",
            "4. **Avoid lambda allocations** in frequently recomposing areas",
            "5. **Profile with Layout Inspector** to verify optimizations are working",
            "",
            "## Next Steps",
            "",
            "- Review unskippable composables and add stability annotations",
            "- Optimize frequently recomposing components",
            "- Run this analysis again after optimizations",
            "",
        ])
        
        # Write report
        try:
            with open(output_file, 'w') as f:
                f.write('\n'.join(report_lines))
            print(f"✅ Report saved to: {output_file}")
        except Exception as e:
            print(f"❌ Failed to save report: {e}")

def main():
    if len(sys.argv) < 2:
        print("Usage: python analyze_compose_metrics.py <metrics_directory>")
        print("Example: python analyze_compose_metrics.py app/build/compose_compiler")
        sys.exit(1)
    
    metrics_dir = sys.argv[1]
    analyzer = ComposeMetricsAnalyzer(metrics_dir)
    analyzer.analyze_metrics()
    analyzer.generate_report()

if __name__ == "__main__":
    main()