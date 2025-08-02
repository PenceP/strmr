# Push Changes Log

This file tracks all changes pushed to GitHub with commit hashes and descriptions.

## Navigation Fixes Branch

### 2025-01-08

- **b60592b** - Fix settings page focus states - implement 3-level focus system
    - Navigation bar (level 1): Settings icon red when focused, white otherwise
    - Left settings panel (level 2): Trakt item blue when focused, grey otherwise
    - Right settings content (level 3): Content items blue when focused, dark otherwise
    - Fixed focus transition from nav bar to left panel (grey → blue)

- **a58228c** - feat(settings): Implement individual row focus and create push log
  - Remove focus logic from ModernSettingsCard - serves only as visual container
  - Add self-contained focus logic to SettingsToggleRow with blue background when focused
  - Add self-contained focus logic to SettingsRadioGroup options with blue background when focused
  - Fix right panel settings submenu focus behavior for proper 3-level navigation

---

*Format: `commit_hash` - Brief description*