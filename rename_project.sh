#!/bin/bash

echo "Starting the renaming process..."

# 1. ফাইলের ভেতরের প্যাকেজ নেম পরিবর্তন (com.tuktak -> com.kinchat)
echo "Replacing package names in files..."
find . -type f \( -name "*.kt" -o -name "*.xml" -o -name "*.kts" -o -name "*.gradle" -o -name "*.pro" \) -exec sed -i 's/com\.tuktak/com\.kinchat/g' {} +

# 2. ফাইলের ভেতরের অ্যাপের নাম পরিবর্তন (TukTak -> KinChat)
echo "Replacing app names in files..."
find . -type f \( -name "*.kt" -o -name "*.xml" -o -name "*.kts" \) -exec sed -i 's/TukTak/KinChat/g' {} +
find . -type f \( -name "*.kt" -o -name "*.xml" -o -name "*.kts" \) -exec sed -i 's/tuktak/kinchat/g' {} +

# 3. অ্যাপ্লিকেশন ক্লাসের নাম পরিবর্তন (TukTakApplication.kt -> KinChatApplication.kt)
echo "Renaming Application class..."
if [ -f "app/src/main/java/com/tuktak/app/TukTakApplication.kt" ]; then
    mv app/src/main/java/com/tuktak/app/TukTakApplication.kt app/src/main/java/com/tuktak/app/KinChatApplication.kt
fi

# 4. ফোল্ডারের নাম পরিবর্তন (com/tuktak -> com/kinchat)
echo "Renaming directories..."
if [ -d "app/src/main/java/com/tuktak" ]; then
    mv app/src/main/java/com/tuktak app/src/main/java/com/kinchat
fi

echo "All done! Successfully renamed TukTak to KinChat."
echo "Please CLEAN and REBUILD your project now."
