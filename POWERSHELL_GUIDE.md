# 📖 POWERSHELL COMMANDS GUIDE FOR WINDOWS

## ⚠️ Problem

PowerShell on Windows doesn't have Unix commands like `head`, `tail`, `grep`, `ls`, etc.

---

## ✅ Solution: Use PowerShell Equivalents

### **Listing Files**

```powershell
# Unix: ls
# PowerShell:
Get-ChildItem
Get-ChildItem -Path "app/src/main/res"

# With filtering:
Get-ChildItem "*.png"
Get-ChildItem "app/src/main/res/drawable/ic_launcher*.png"
```

### **Counting Files**

```powershell
# Unix: ls | wc -l
# PowerShell:
Get-ChildItem | Measure-Object
(Get-ChildItem).Count

# Example:
Get-ChildItem app/src/main/res/mipmap-*/ic_launcher*.png | Measure-Object
```

### **Viewing First N Lines**

```powershell
# Unix: head -5
# PowerShell:
Get-Content filename | Select-Object -First 5
Get-Content filename -TotalCount 5

# Example:
Get-Content build.log | Select-Object -First 10
```

### **Viewing Last N Lines**

```powershell
# Unix: tail -5
# PowerShell:
Get-Content filename | Select-Object -Last 5
Get-Content filename -Tail 5

# Example:
Get-Content build.log -Tail 20
```

### **Searching Text (grep equivalent)**

```powershell
# Unix: grep "pattern" file
# PowerShell:
Select-String "pattern" filename
Get-Content filename | Select-String "pattern"

# Example:
Select-String "BUILD" build.log
Get-Content build.log | Where-Object { $_ -match "SUCCESS" }
```

### **Piping Multiple Operations**

```powershell
# Unix: cat file | grep "ERROR" | head -5
# PowerShell:
Get-Content filename | Select-String "ERROR" | Select-Object -First 5

# Example:
Get-Content build.log | Where-Object { $_ -match "BUILD" } | Select-Object -First 3
```

---

## 🔧 Common CameraStream Commands (Corrected)

### **Check Resource Files**

```powershell
# View all drawable icons
Get-ChildItem app/src/main/res/drawable/ic_launcher*.png

# View all launcher icons (all densities)
Get-ChildItem app/src/main/res/mipmap-*/ic_launcher.png

# Count all launcher icons
(Get-ChildItem app/src/main/res/mipmap-*/ic_launcher*.png).Count
```

### **Check Build Status**

```powershell
# View build output
Get-Content app/build/outputs/apk/debug/output.json

# Find build errors
Get-Content build.log | Select-String "ERROR"

# Find success message
Get-Content build.log | Select-String "BUILD SUCCESSFUL"
```

### **Git Commands**

```powershell
# Check status
git status

# View last 5 commits
git log --oneline -5

# View detailed commit
git show HEAD

# Check branch
git branch -a
```

---

## 📋 Complete Example: Verify Project

```powershell
# Navigate to project
cd D:\PATH\apps\CameraStream

# Check all drawable icons
Write-Host "=== Drawable Icons ===" 
Get-ChildItem app/src/main/res/drawable/ic_launcher*.png

# Check all launcher icons
Write-Host "`n=== All Launcher Icons ===" 
Get-ChildItem app/src/main/res/mipmap-*/ic_launcher*.png | Measure-Object

# Check Git status
Write-Host "`n=== Git Status ===" 
git status --short

# Show last 3 commits
Write-Host "`n=== Last Commits ===" 
git log --oneline -3

# Check if APK exists
Write-Host "`n=== APK Status ===" 
if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
    Write-Host "✅ APK FOUND"
} else {
    Write-Host "❌ APK NOT FOUND"
}
```

---

## 🔄 Build and Check (Correct PowerShell)

```powershell
# Compile
.\gradlew.bat build -x test

# Check result
Get-Content build.log | Select-String "BUILD"

# List output files
Get-ChildItem app/build/outputs/apk/debug/
```

---

## ✨ Tips for PowerShell

### **Enable Command History**
```powershell
# Previous command
Get-History
Get-History -Id 5
```

### **Create Aliases**
```powershell
# Create alias for Get-ChildItem
New-Alias -Name ls -Value Get-ChildItem -Force

# Create alias for Select-String grep
New-Alias -Name grep -Value Select-String -Force

# Now you can use: ls, grep like Unix
Get-ChildItem file.txt | grep "pattern"
```

### **Add to Profile (persistent)**
```powershell
# Edit profile
notepad $PROFILE

# Add these lines:
New-Alias -Name ls -Value Get-ChildItem -Force
New-Alias -Name grep -Value Select-String -Force
New-Alias -Name head -Value Get-Content -Force
```

---

## 📚 PowerShell Help

```powershell
# Get help for any command
Get-Help Get-ChildItem
Get-Help Select-String -Examples
Get-Help Select-Object

# Online help
Get-Help Get-ChildItem -Online
```

---

## 🎯 Summary

| Task | Unix | PowerShell |
|------|------|-----------|
| List files | `ls` | `Get-ChildItem` |
| Show first 5 lines | `head -5` | `Get-Content file \| Select-Object -First 5` |
| Show last 5 lines | `tail -5` | `Get-Content file \| Select-Object -Last 5` |
| Search text | `grep "pattern"` | `Select-String "pattern"` |
| Count lines | `wc -l` | `Measure-Object` |
| View file | `cat` | `Get-Content` |
| Pipe | `\|` | `\|` (same!) |

---

## ✅ Now You Can Use PowerShell Correctly!

**Always remember:**
- ✅ Use `Get-ChildItem` instead of `ls`
- ✅ Use `Select-Object -First 5` instead of `head -5`
- ✅ Use `Select-Object -Last 5` instead of `tail -5`
- ✅ Use `Select-String "pattern"` instead of `grep "pattern"`

---

**Happy coding with PowerShell!** 🚀

