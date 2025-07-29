# Compose Performance Analysis Report
*Generated from metrics in: app/build/compose_compiler*

## Summary

This report analyzes the Compose compiler metrics to identify performance optimization opportunities.

## Key Metrics

### Composable Functions

| File | Skippable | Restartable | Readonly | Issues |
|------|-----------|-------------|----------|--------|
| app_debug-composables.txt | 82 | 119 | 0 | ✅ Good |

## Recommendations

1. **Add @Stable/@Immutable annotations** to data classes used in Compose
2. **Use remember() for expensive calculations** that don't need to recompute on every composition
3. **Use derivedStateOf()** for computed values that depend on other state
4. **Avoid lambda allocations** in frequently recomposing areas
5. **Profile with Layout Inspector** to verify optimizations are working

## Next Steps

- Review unskippable composables and add stability annotations
- Optimize frequently recomposing components
- Run this analysis again after optimizations
