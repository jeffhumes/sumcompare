# SumCompare GUI - Date Organization Controls

## UI Layout

The date organization controls appear in the **Options** section of the main window:

```
┌─────────────────────────────────────────────────────────────────┐
│  SumCompare - Intelligent File Deduplication                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Source Directory:                                               │
│  ┌──────────────────────────────────────┐ ┌─────────┐          │
│  │ Select source directory...            │ │ Browse  │          │
│  └──────────────────────────────────────┘ └─────────┘          │
│  ☐ Check for duplicates in source only                          │
│                                                                  │
│  Target Directory:                                               │
│  ┌──────────────────────────────────────┐ ┌─────────┐          │
│  │ Select target directory...            │ │ Browse  │          │
│  └──────────────────────────────────────┘ └─────────┘          │
│                                                                  │
│  Checksum Algorithm:                                             │
│  ┌─────────────┐  Threads: ┌────┐                              │
│  │ XXHASH64  ▼ │           │ 8  │                              │
│  └─────────────┘           └────┘                              │
│                                                                  │
│  Options:                                                        │
│  ☑ Dry Run (preview without copying)                            │
│  ☐ Keep source directory structure                              │
│  ☐ Backup source first                                          │
│  ☐ Preserve file dates                                          │
│  ☐ Create Excel report                                          │
│  ──────────────────────────────────────────────────            │
│  ☐ Organize files into date-based folders      ← NEW!          │
│  Date Source: ┌──────────┐  Folder Pattern: ┌─────────────┐   │
│               │ MODIFIED ▼│                  │ YEAR_MONTH ▼│   │
│               └──────────┘                  └─────────────┘   │
│  ──────────────────────────────────────────────────            │
│                                                                  │
│  ┌──────────────┐ ┌────────┐              ┌──────┐ ┌──────┐  │
│  │ Start Compare │ │ Cancel │              │ View  │ │ Help │  │
│  └──────────────┘ └────────┘              │ Log  │ └──────┘  │
│                                             └──────┘            │
│                                                                  │
│  Progress:                                                       │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░                            │
│  Processing files...                                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Control Details

### Checkbox: "Organize files into date-based folders"

- **Default**: Unchecked (disabled)
- **Effect**: When checked, enables the Date Source and Folder Pattern dropdowns
- **When unchecked**: Date Source and Folder Pattern dropdowns are disabled (grayed out)

### ComboBox: "Date Source"

- **Location**: Below the date folders checkbox, left side
- **Options**:
  - MODIFIED (default)
  - CREATED
  - ACCESSED
- **State**: Disabled until date folders checkbox is checked
- **Width**: 150 pixels

### ComboBox: "Folder Pattern"

- **Location**: Same row as Date Source, right side
- **Options**:
  - YEAR_MONTH (default)
  - YEAR_MONTH_SLASH
  - YEAR_MONTH_DAY
  - YEAR_MONTH_DAY_SLASH
  - YEAR_ONLY
  - YEAR_QUARTER
- **State**: Disabled until date folders checkbox is checked
- **Width**: 200 pixels

## Visual States

### State 1: Date Organization Disabled (Default)

```
☐ Organize files into date-based folders
Date Source: [MODIFIED ▼] (grayed out)  Folder Pattern: [YEAR_MONTH ▼] (grayed out)
```

### State 2: Date Organization Enabled

```
☑ Organize files into date-based folders
Date Source: [MODIFIED ▼] (active)      Folder Pattern: [YEAR_MONTH ▼] (active)
```

### State 3: Custom Configuration

```
☑ Organize files into date-based folders
Date Source: [CREATED ▼] (active)       Folder Pattern: [YEAR_MONTH_DAY ▼] (active)
```

## How to Verify Controls Are Visible

1. **Run the GUI**: `./run-gui.sh` or `mvn javafx:run`
2. **Scroll down** to the Options section
3. **Look for** the separator line and checkbox below "Create Excel report"
4. You should see:
   - A horizontal separator line
   - Checkbox: "Organize files into date-based folders"
   - Two dropdowns on the same row (initially grayed out)

## Troubleshooting

If you don't see the controls:

1. **Verify FXML is updated**:

   ```bash
   grep -A 5 "dateFoldersCheckBox" src/main/resources/fxml/sumcompare.fxml
   ```

   Should show the checkbox and comboboxes

2. **Rebuild the application**:

   ```bash
   mvn clean package -DskipTests
   ```

3. **Restart the GUI**:

   ```bash
   ./run-gui.sh
   ```

4. **Check the log** for any FXML loading errors

## Testing the Feature

1. Check ☑ "Organize files into date-based folders"
2. Verify dropdowns become enabled (no longer grayed out)
3. Select "CREATED" from Date Source
4. Select "YEAR_MONTH_DAY" from Folder Pattern
5. Enable "Dry Run" to test safely
6. Select source and target directories
7. Click "Start Comparison"
8. Check log output for: "Date-based organization: Organizing by creation date into YYYY-MM-DD folders"
