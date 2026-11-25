# DocuCut — Documentary & Investigative Video Studio (Android)

CapCut-style mobile documentary video editor built with Kotlin, Jetpack Compose, Material 3, Room, and Jetpack Media3 (`Transformer`, `ExoPlayer`, `Effect`).

## GitHub Actions দিয়ে স্বয়ংক্রিয়ভাবে APK বিল্ড করার নিয়ম

এই প্রজেক্টে `.github/workflows/android-build.yml` যুক্ত করা আছে, তাই কোড GitHub-এ পুশ করলেই GitHub নিজে থেকে Android APK তৈরি করে দেবে:

1. **GitHub-এ পুশ করুন:** AI Studio-এর Settings মেনু থেকে **Push to GitHub** ব্যবহার করুন অথবা প্রজেক্ট ZIP এক্সপোর্ট করে আপনার GitHub রিপোজিটরিতে আপলোড করুন।
2. **Actions ট্যাব খুলুন:** আপনার GitHub রিপোজিটরির **Actions** ট্যাবে গেলে `Build DocuCut Android APK` ওয়ার্কফ্লোটি চলতে দেখবেন (বা `Run workflow` বাটনে ক্লিক করে ম্যানুয়ালি চালাতে পারবেন)।
3. **APK ডাউনলোড করুন:** বিল্ড শেষ হলে ওই রান-এর নিচে **Artifacts → `DocuCut-Debug-APK`** থেকে সরাসরি `.apk` ফাইল ডাউনলোড করে আপনার অ্যান্ড্রয়েড ফোনে ইনস্টল করতে পারবেন।

### প্লে-স্টোর / রিলিজ (Signed APK & AAB) বিল্ড করতে চাইলে:
আপনার GitHub রিপোজিটরির **Settings → Secrets and variables → Actions** সেকশনে নিচের ৩টি Secret যোগ করুন (কী-অ্যালিয়াস `upload`):
- `KEYSTORE_BASE64` (আপনার `.jks` ফাইলের base64 টেক্সট)
- `STORE_PASSWORD`
- `KEY_PASSWORD`

এগুলো সেট করা থাকলে GitHub Actions স্বয়ংক্রিয়ভাবে `DocuCut-Release-Build` (`.apk` এবং `.aab`) তৈরি করে দেবে।
