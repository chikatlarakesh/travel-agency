# Quick Start - Testing Your API

## ✅ Prerequisites Check
- [x] MongoDB running on port 27017
- [x] Application running on port 8080

## 🚀 3 Easy Ways to Test

### Method 1: Swagger UI (Easiest - No Installation Required)

1. **Open your browser**
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

2. **Test endpoints interactively:**
   - Expand any endpoint
   - Click "Try it out"
   - Fill in parameters
   - Click "Execute"
   - See results instantly!

3. **For authenticated endpoints (bookings):**
   - First call `/api/v1/auth/sign-up` or `/api/v1/auth/sign-in`
   - Copy the `token` from response
   - Click "Authorize" button (top right)
   - Enter: `Bearer YOUR_TOKEN_HERE`
   - Now test booking endpoints!

---

### Method 2: PowerShell Script (Automated Testing)

**Run all tests at once:**

```powershell
cd C:\Users\KaviyaS\Desktop\PBE-Backend\api-handler
.\test-api.ps1
```

This will:
- ✓ Create a test user
- ✓ Sign in
- ✓ Search destinations
- ✓ Get all tours
- ✓ Filter tours
- ✓ Get tour details
- ✓ Get reviews
- ✓ Create a booking
- ✓ Get user bookings

---

### Method 3: Postman (Professional Testing)

**Import the collection:**

1. Open Postman
2. Click "Import"
3. Select `postman-collection.json`
4. Collection will be imported with all endpoints ready!

**Testing workflow:**
1. Run "Sign Up" or "Sign In" (saves token automatically)
2. Test any tour endpoint
3. Test booking endpoints (token auto-included)

---

## 📋 Quick Test Commands (cURL)

### 1️⃣ Sign Up
```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-up -H "Content-Type: application/json" -d @test-data/signup-request.json
```

### 2️⃣ Sign In
```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-in -H "Content-Type: application/json" -d @test-data/signin-request.json
```

### 3️⃣ Search Destinations
```bash
curl "http://localhost:8080/api/v1/tours/destinations?destination=phu"
```

### 4️⃣ Get All Tours
```bash
curl "http://localhost:8080/api/v1/tours/available"
```

### 5️⃣ Get Tour Details
```bash
curl "http://localhost:8080/api/v1/tours/t-001"
```

### 6️⃣ Get Reviews
```bash
curl "http://localhost:8080/api/v1/tours/t-001/reviews"
```

### 7️⃣ Create Booking (replace YOUR_TOKEN)
```bash
curl -X POST http://localhost:8080/api/v1/bookings -H "Content-Type: application/json" -H "Authorization: Bearer YOUR_TOKEN" -d @test-data/create-booking-request.json
```

### 8️⃣ Get Bookings (replace YOUR_TOKEN and USER_ID)
```bash
curl "http://localhost:8080/api/v1/bookings?userId=USER_ID" -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🎯 Available Test Data

### Tour IDs:
- `t-001` - Garden Resort & Spa, Phuket (RESORT, All Inclusive, 5★)
- `t-002` - Dolomites 7-day Hike, Italy (HIKE, Half Board, 4★)
- `t-003` - Caribbean Dream Cruise (CRUISE, All Inclusive, 5★)
- `t-004` - Tropical Caribe Hotel, Dominican Republic (RESORT, Full Board, 3★)
- `t-005` - Machu Picchu Trek, Peru (HIKE, Bed & Breakfast, 4★)
- `t-006` - Mediterranean Cruise, Barcelona (CRUISE, All Inclusive, 5★)

### Tour Instance IDs (for bookings):
- `ti-001-dec01` - Garden Resort, Dec 1-8
- `ti-002-sep15` - Dolomites Hike, Sep 15-22
- `ti-003-jul15` - Caribbean Cruise, Jul 15-25
- `ti-004-aug10` - Tropical Caribe, Aug 10-17
- `ti-005-oct05` - Machu Picchu, Oct 5-12
- `ti-006-jun20` - Mediterranean Cruise, Jun 20-30

### Filter Options:
- **Meal Plans:** BB, HB, FB, AI
- **Tour Types:** RESORT, CRUISE, HIKE
- **Sort By:** RATING_DESC, RATING_ASC, PRICE_DESC, PRICE_ASC, NEWEST, OLDEST

---

## 📚 Full Documentation

See **API_TESTING_GUIDE.md** for:
- Complete endpoint documentation
- All query parameters
- Request/response examples
- Error handling
- Troubleshooting tips

---

## 🐛 Troubleshooting

**Application won't start?**
```bash
# Check if MongoDB is running
docker ps

# Start MongoDB
docker-compose up -d
```

**Can't access endpoints?**
- Ensure app is running: http://localhost:8080
- Check console for errors

**401 Unauthorized on bookings?**
1. Sign in first to get token
2. Add header: `Authorization: Bearer YOUR_TOKEN`

**404 Tour not found?**
- Use valid tour IDs: t-001, t-002, t-003, t-004, t-005, t-006

---

## 🎉 Happy Testing!

**Recommended Flow:**
1. Start with Swagger UI to understand endpoints
2. Use PowerShell script for automated testing
3. Import Postman collection for advanced testing

**Need Help?**
- Check **API_TESTING_GUIDE.md** for detailed examples
- View Swagger UI for interactive API documentation

