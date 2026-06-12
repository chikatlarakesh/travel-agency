# API Testing Guide - Travel Booking API

## Base URL
```
http://localhost:8080
```

## Quick Access
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **API Docs**: http://localhost:8080/v3/api-docs

---

## 1. Authentication Endpoints

### 1.1 Sign Up (Register New User)

**Endpoint:** `POST /api/v1/auth/sign-up`

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-555-0123",
  "role": "CUSTOMER"
}
```

**Other test users:**
```json
{
  "email": "sarah.agent@travel.com",
  "password": "AgentPass456!",
  "firstName": "Sarah",
  "lastName": "Smith",
  "phone": "+1-555-0124",
  "role": "TRAVEL_AGENT"
}
```

```json
{
  "email": "admin@travel.com",
  "password": "AdminPass789!",
  "firstName": "Admin",
  "lastName": "User",
  "phone": "+1-555-0125",
  "role": "ADMIN"
}
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-up ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"john.doe@example.com\",\"password\":\"SecurePass123!\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"phone\":\"+1-555-0123\",\"role\":\"CUSTOMER\"}"
```

**Expected Response:** `201 CREATED`
```json
{
  "userId": "generated-user-id",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "CUSTOMER",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.2 Sign In (Login)

**Endpoint:** `POST /api/v1/auth/sign-in`

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-in ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"john.doe@example.com\",\"password\":\"SecurePass123!\"}"
```

**Expected Response:** `200 OK`
```json
{
  "userId": "user-id",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "CUSTOMER",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

> **Important:** Save the `token` from the response. You'll need it for booking endpoints!

---

## 2. Tour Endpoints

### 2.1 Search Destinations (Autocomplete)

**Endpoint:** `GET /api/v1/tours/destinations?destination={query}`

**Test Queries:**
- `?destination=par` → Returns Paris
- `?destination=new` → Returns New York, New Zealand
- `?destination=dom` → Returns Dominican Republic
- `?destination=ita` → Returns Italy

**cURL Command:**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/destinations?destination=par"
```

**Expected Response:** `200 OK`
```json
{
  "destinations": [
    "Paris, France",
    "Phuket, Thailand"
  ]
}
```

---

### 2.2 Get Available Tours (Search & Filter)

**Endpoint:** `GET /api/v1/tours/available`

**Query Parameters:**
- `page` (default: 1)
- `pageSize` (default: 6)
- `destination` (default: "Any destination")
- `startDate` (optional, format: YYYY-MM-DD)
- `duration` (optional, e.g., "7 days", "14 days")
- `adults` (default: 1)
- `children` (default: 0)
- `mealPlan` (optional: BB, HB, FB, AI)
- `tourType` (optional: RESORT, CRUISE, HIKE)
- `sortBy` (default: RATING_DESC, options: RATING_ASC, PRICE_DESC, PRICE_ASC, NEWEST, OLDEST)

**Example Requests:**

**1. Get all tours (basic):**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/available"
```

**2. Search by destination:**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/available?destination=Phuket,%20Thailand"
```

**3. Filter by tour type (Resort):**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/available?tourType=RESORT"
```

**4. Filter by meal plan (All Inclusive):**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/available?mealPlan=AI"
```

**5. Filter by guests (2 adults, 1 child):**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/available?adults=2&children=1"
```

**6. Sort by price (lowest first):**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/available?sortBy=PRICE_ASC"
```

**7. Complex filter:**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/available?destination=Phuket,%20Thailand&tourType=RESORT&mealPlan=AI&adults=2&children=1&sortBy=PRICE_ASC&page=1&pageSize=10"
```

**Expected Response:** `200 OK`
```json
{
  "tours": [
    {
      "id": "t-001",
      "name": "Garden Resort & Spa - Phuket Paradise",
      "destination": "Phuket, Thailand",
      "country": "Thailand",
      "image": "https://example.com/tours/garden-resort.jpg",
      "description": "Experience luxury at Garden Resort...",
      "rating": 4.8,
      "reviewCount": 142,
      "tourType": "RESORT",
      "duration": "7 days",
      "availableFrom": "2024-05-01",
      "availableTo": "2024-12-31",
      "minPrice": 1299.00
    }
  ],
  "totalCount": 6,
  "currentPage": 1,
  "pageSize": 6,
  "totalPages": 1
}
```

---

### 2.3 Get Tour Details by ID

**Endpoint:** `GET /api/v1/tours/{id}`

**Test IDs:**
- `t-001` → Garden Resort & Spa - Phuket Paradise (RESORT)
- `t-002` → Dolomites 7-day guided hike (HIKE)
- `t-003` → Caribbean Dream Cruise (CRUISE)
- `t-004` → Tropical Caribe Hotel (RESORT)
- `t-005` → Machu Picchu Trek (HIKE)
- `t-006` → Mediterranean Cruise Experience (CRUISE)

**cURL Command:**
```bash
curl -X GET "http://localhost:8080/api/v1/tours/t-001"
```

**Expected Response:** `200 OK`
```json
{
  "id": "t-001",
  "name": "Garden Resort & Spa - Phuket Paradise",
  "destination": "Phuket, Thailand",
  "country": "Thailand",
  "image": "https://example.com/tours/garden-resort.jpg",
  "description": "Experience luxury at Garden Resort & Spa...",
  "rating": 4.8,
  "reviewCount": 142,
  "tourType": "RESORT",
  "duration": "7 days",
  "availableFrom": "2024-05-01",
  "availableTo": "2024-12-31",
  "hotel": {
    "name": "Garden Resort & Spa",
    "address": "123 Beach Road, Patong, Phuket",
    "stars": 5,
    "amenities": ["Pool", "Spa", "WiFi", "Beach Access", "Restaurant"]
  },
  "instances": [
    {
      "startDate": "2024-12-01",
      "endDate": "2024-12-08",
      "availableSeats": 25,
      "pricingOptions": [
        {
          "mealPlan": "AI",
          "guestCounts": {
            "adults": 2,
            "children": 0
          },
          "price": 2599.00
        }
      ]
    }
  ]
}
```

---

### 2.4 Get Tour Reviews

**Endpoint:** `GET /api/v1/tours/{id}/reviews`

**Query Parameters:**
- `page` (default: 1)
- `pageSize` (default: 4)
- `sortBy` (default: RATING_DESC)

**cURL Commands:**

```bash
# Get reviews for Garden Resort
curl -X GET "http://localhost:8080/api/v1/tours/t-001/reviews"

# Get reviews with pagination
curl -X GET "http://localhost:8080/api/v1/tours/t-001/reviews?page=1&pageSize=2"

# Get reviews sorted by rating (lowest first)
curl -X GET "http://localhost:8080/api/v1/tours/t-001/reviews?sortBy=RATING_ASC"
```

**Expected Response:** `200 OK`
```json
{
  "reviews": [
    {
      "id": "r-001",
      "author": "David",
      "authorImage": "https://example.com/authors/david.jpg",
      "date": "2024-06-06",
      "rating": 5,
      "comment": "Absolutely stunning resort! The pools are beautiful..."
    }
  ],
  "totalCount": 3,
  "currentPage": 1,
  "pageSize": 4,
  "totalPages": 1
}
```

---

## 3. Booking Endpoints (Requires Authentication)

### 3.1 Create Booking

**Endpoint:** `POST /api/v1/bookings`

**Headers:**
```
Authorization: Bearer {your-jwt-token-from-sign-in}
Content-Type: application/json
```

**Request Body:**
```json
{
  "tourId": "t-001",
  "userId": "user-id-from-sign-in",
  "tourInstanceId": "ti-001-dec01",
  "mealPlan": "AI",
  "adults": 2,
  "children": 1,
  "leadTraveler": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+1-555-0123",
    "dateOfBirth": "1985-06-15",
    "passportNumber": "P12345678"
  },
  "additionalTravelers": [
    {
      "firstName": "Jane",
      "lastName": "Doe",
      "email": "jane.doe@example.com",
      "phone": "+1-555-0124",
      "dateOfBirth": "1987-08-20",
      "passportNumber": "P87654321"
    },
    {
      "firstName": "Jimmy",
      "lastName": "Doe",
      "email": "",
      "phone": "",
      "dateOfBirth": "2015-03-10",
      "passportNumber": "P11223344"
    }
  ]
}
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/v1/bookings ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" ^
  -d "{\"tourId\":\"t-001\",\"userId\":\"user-id\",\"tourInstanceId\":\"ti-001-dec01\",\"mealPlan\":\"AI\",\"adults\":2,\"children\":1,\"leadTraveler\":{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"+1-555-0123\",\"dateOfBirth\":\"1985-06-15\",\"passportNumber\":\"P12345678\"},\"additionalTravelers\":[{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane.doe@example.com\",\"phone\":\"+1-555-0124\",\"dateOfBirth\":\"1987-08-20\",\"passportNumber\":\"P87654321\"}]}"
```

**Expected Response:** `201 CREATED`
```json
{
  "bookingId": "generated-booking-id",
  "tourName": "Garden Resort & Spa - Phuket Paradise",
  "startDate": "2024-12-01",
  "endDate": "2024-12-08",
  "totalPrice": 3899.00,
  "status": "CONFIRMED",
  "message": "Your booking has been confirmed!"
}
```

---

### 3.2 Get User Bookings

**Endpoint:** `GET /api/v1/bookings?userId={userId}`

**Headers:**
```
Authorization: Bearer {your-jwt-token}
```

**cURL Command:**
```bash
curl -X GET "http://localhost:8080/api/v1/bookings?userId=your-user-id" ^
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

**Expected Response:** `200 OK`
```json
{
  "bookings": [
    {
      "bookingId": "booking-id",
      "tourId": "t-001",
      "tourName": "Garden Resort & Spa - Phuket Paradise",
      "destination": "Phuket, Thailand",
      "image": "https://example.com/tours/garden-resort.jpg",
      "startDate": "2024-12-01",
      "endDate": "2024-12-08",
      "adults": 2,
      "children": 1,
      "totalPrice": 3899.00,
      "status": "CONFIRMED",
      "bookingDate": "2024-04-29"
    }
  ]
}
```

---

## Testing Flow

### Recommended Testing Order:

1. **Test Authentication:**
   ```bash
   # Step 1: Sign up
   curl -X POST http://localhost:8080/api/v1/auth/sign-up -H "Content-Type: application/json" -d "{\"email\":\"test@example.com\",\"password\":\"Test123!\",\"firstName\":\"Test\",\"lastName\":\"User\",\"phone\":\"+1-555-0001\",\"role\":\"CUSTOMER\"}"
   
   # Step 2: Sign in (save the token!)
   curl -X POST http://localhost:8080/api/v1/auth/sign-in -H "Content-Type: application/json" -d "{\"email\":\"test@example.com\",\"password\":\"Test123!\"}"
   ```

2. **Browse Tours:**
   ```bash
   # Search destinations
   curl -X GET "http://localhost:8080/api/v1/tours/destinations?destination=phu"
   
   # Get all tours
   curl -X GET "http://localhost:8080/api/v1/tours/available"
   
   # Get specific tour details
   curl -X GET "http://localhost:8080/api/v1/tours/t-001"
   
   # Get tour reviews
   curl -X GET "http://localhost:8080/api/v1/tours/t-001/reviews"
   ```

3. **Create Booking:**
   ```bash
   # Use the token from sign-in
   curl -X POST http://localhost:8080/api/v1/bookings -H "Content-Type: application/json" -H "Authorization: Bearer YOUR_TOKEN" -d @booking-request.json
   ```

4. **View Bookings:**
   ```bash
   curl -X GET "http://localhost:8080/api/v1/bookings?userId=YOUR_USER_ID" -H "Authorization: Bearer YOUR_TOKEN"
   ```

---

## Quick Test Data Reference

### Available Tour IDs:
- `t-001` - Garden Resort & Spa, Phuket (RESORT, AI, 5★)
- `t-002` - Dolomites Hike, Italy (HIKE, HB, 4★)
- `t-003` - Caribbean Cruise, Miami (CRUISE, AI, 5★)
- `t-004` - Tropical Caribe Hotel, Dominican Republic (RESORT, FB, 3★)
- `t-005` - Machu Picchu Trek, Peru (HIKE, BB, 4★)
- `t-006` - Mediterranean Cruise, Barcelona (CRUISE, AI, 5★)

### Meal Plans:
- `BB` - Bed & Breakfast
- `HB` - Half Board
- `FB` - Full Board
- `AI` - All Inclusive

### Tour Types:
- `RESORT`
- `CRUISE`
- `HIKE`

### Sort Options:
- `RATING_DESC` (highest rated first)
- `RATING_ASC` (lowest rated first)
- `PRICE_DESC` (most expensive first)
- `PRICE_ASC` (cheapest first)
- `NEWEST`
- `OLDEST`

### User Roles:
- `CUSTOMER`
- `TRAVEL_AGENT`
- `ADMIN`

---

## Using Swagger UI (Easiest Method!)

1. **Start your application**
2. **Open browser:** http://localhost:8080/swagger-ui/index.html
3. **Test endpoints interactively:**
   - Click on any endpoint to expand it
   - Click "Try it out"
   - Fill in the parameters
   - Click "Execute"
   - View the response

**For authenticated endpoints:**
1. First, call `/api/v1/auth/sign-in` to get a token
2. Copy the token value
3. Click the "Authorize" button at the top of Swagger UI
4. Enter: `Bearer YOUR_TOKEN_HERE`
5. Click "Authorize"
6. Now you can test booking endpoints!

---

## Troubleshooting

### Common Issues:

1. **Connection Refused:**
   - Ensure MongoDB is running: `docker-compose up -d`
   - Check application is running on port 8080

2. **401 Unauthorized on bookings:**
   - You need to sign in first and get a JWT token
   - Add the token to the Authorization header: `Bearer {token}`

3. **404 Tour Not Found:**
   - Use valid tour IDs from the list above
   - Tours are pre-seeded in the database

4. **400 Bad Request:**
   - Check your JSON syntax
   - Ensure all required fields are provided
   - Verify date formats (YYYY-MM-DD)

---

Happy Testing! 🚀

