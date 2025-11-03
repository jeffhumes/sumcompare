# GUI Date Organization Quick Reference

## Location in GUI

The date organization controls are located in the **Options** section, below the "Create Excel report" checkbox:

```
Options:
  ☐ Dry Run (preview without copying)
  ☐ Keep source directory structure
  ☐ Backup source first
  ☐ Preserve file dates
  ☐ Create Excel report
  ─────────────────────────────────
  ☐ Organize files into date-based folders    ← NEW
  Date Source: [MODIFIED ▼]    Folder Pattern: [YEAR_MONTH ▼]    ← NEW
```

## How to Use

### Step 1: Enable Date Organization

Check the **"Organize files into date-based folders"** checkbox

### Step 2: Choose Date Source (Optional)

Select which timestamp to use for organizing:

- **MODIFIED** (default) - File modification date
- **CREATED** - File creation date
- **ACCESSED** - File last access date

### Step 3: Choose Folder Pattern (Optional)

Select how to structure the date folders:

- **YEAR_MONTH** (default) - `2025-11/`
- **YEAR_MONTH_SLASH** - `2025/11/`
- **YEAR_MONTH_DAY** - `2025-11-03/`
- **YEAR_MONTH_DAY_SLASH** - `2025/11/03/`
- **YEAR_ONLY** - `2025/`
- **YEAR_QUARTER** - `2025-Q4/`

### Step 4: Configure Other Options

Combine with other options as needed:

- Enable "Dry Run" to preview without copying
- Enable "Keep source directory structure" to preserve subdirectories within date folders

### Step 5: Start Comparison

Click **"Start Comparison"** to begin processing

## Example Scenarios

### Organize Photos by Creation Date

1. ☑ Organize files into date-based folders
2. Date Source: **CREATED**
3. Folder Pattern: **YEAR_MONTH_SLASH**
4. Result: `target/2024/08/photo1.jpg`

### Organize Videos by Month

1. ☑ Organize files into date-based folders
2. Date Source: **MODIFIED** (default)
3. Folder Pattern: **YEAR_MONTH** (default)
4. Result: `target/2025-11/video.mp4`

### Organize Documents by Quarter

1. ☑ Organize files into date-based folders
2. Date Source: **MODIFIED**
3. Folder Pattern: **YEAR_QUARTER**
4. Result: `target/2025-Q4/report.pdf`

## UI Behavior

- **Date Source** and **Folder Pattern** dropdowns are **disabled** by default
- They become **enabled** when you check "Organize files into date-based folders"
- They become **disabled** again when you uncheck the checkbox
- Default values are pre-selected (MODIFIED and YEAR_MONTH)

## Log Messages

When date organization is enabled, you'll see messages like:

```
Date-based organization: Organizing by modification date into YYYY-MM folders
Copying [Video]: vacation.mp4 (Size: 156.32 MB | Modified: 2024-08-15 14:23:10)
```

The target path will reflect the date-based folder structure.
