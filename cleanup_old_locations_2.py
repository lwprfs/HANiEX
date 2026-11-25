#!/usr/bin/env python3
"""Deletes the 8 stale old-location files left after the first SitesHub move.
Run from the project root AFTER unzipping changed_files.zip:
    python3 cleanup_old_locations_2.py [--dry]
A file is deleted only if its new copy under SitesHub/Hanime1/ exists.
"""
import os, sys
DRY = "--dry" in sys.argv
P = "app/src/main/java/com/yenaly/han1meviewer/"
OLD = ['app/src/main/java/com/yenaly/han1meviewer/ui/screen/home/WatchHistoryScreen.kt', 'app/src/main/java/com/yenaly/han1meviewer/ui/screen/home/creatorcenter/CreatorCenterUtils.kt', 'app/src/main/java/com/yenaly/han1meviewer/ui/screen/home/dailycheckin/DailyCheckInCards.kt', 'app/src/main/java/com/yenaly/han1meviewer/ui/screen/home/download/DownloadSort.kt', 'app/src/main/java/com/yenaly/han1meviewer/ui/screen/home/videogrid/VideoGridUtils.kt', 'app/src/main/java/com/yenaly/han1meviewer/ui/screen/search/AdvancedSearchChip.kt', 'app/src/main/java/com/yenaly/han1meviewer/ui/screen/video/VideoIntroductionScreen.kt', 'app/src/main/java/com/yenaly/han1meviewer/ui/screen/video/VideoTabsContent.kt']
if not os.path.isdir(P): sys.exit("Run from the project root (folder containing app/).")
n = 0
for old in OLD:
    if not os.path.exists(old): continue
    new = P + "SitesHub/Hanime1/" + old[len(P):]
    if not os.path.exists(new):
        print("SKIP (unzip changed_files.zip first):", old); continue
    print(("would delete " if DRY else "delete ") + old)
    if not DRY: os.remove(old)
    n += 1
if not DRY:
    for d, _, _ in os.walk("app/src/main/java", topdown=False):
        if not os.listdir(d): os.rmdir(d)
print(n, "file(s) processed.")
