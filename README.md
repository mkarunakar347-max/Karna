# HallBook - End-to-End Function Hall Booking Application

This is a beginner-friendly student project using:

- HTML
- CSS
- JavaScript
- Node.js
- Java
- JDBC
- MySQL
- Maven

## Architecture

Browser
  |
  v
Node.js :3000
  |
  v
Java JDBC API :8080
  |
  v
MySQL :3306

The browser calls `/api/...` on Node. Node forwards the API request to Java.
This avoids the common browser "Failed to fetch" / CORS problem.

## Important: Why the old screen showed "Failed to fetch"

The old frontend directly called:

http://localhost:8080/api/bookings

If the Java server was not running, the browser could not connect.

This version calls:

/api/bookings

and Node proxies that request to Java.

## 1. Install software

Install:

1. JDK 17 or newer
2. MySQL Server
3. Node.js
4. Maven

## 2. Create the database

Open MySQL Workbench.

Open `database.sql`.

Run the complete file.

It creates:

- hallbook database
- users
- function_halls
- chair_packages
- decorations
- food_items
- bookings
- booking_items
- payments

It also inserts:

- 8 halls
- 8 chair packages
- 8 decoration packages
- 24 vegetarian food items
- 24 non-vegetarian food items

## 3. Set MySQL password

Open:

BookingServer.java

Find:

static final String DB_PASSWORD = "YOUR_MYSQL_PASSWORD";

Replace it with your MySQL root password.

Example:

static final String DB_PASSWORD = "root123";

Do not put spaces around the password.

## 4. Build the Java JDBC backend

Open terminal in this project folder.

Run:

mvn clean package

Maven downloads MySQL Connector/J automatically.

## 5. Start Java JDBC server

After `mvn clean package`, run:

    mvn exec:java

Maven automatically puts MySQL Connector/J on the classpath.

You should see:

Connected to MySQL database: hallbook
Java JDBC API running at http://localhost:8080

## 6. Start Node.js frontend

Open another terminal in the same folder.

Run:

node server.js

You should see:

HallBook Node server running:
http://localhost:3000

## 7. Open the website

Open:

http://localhost:3000

Do NOT open `index.html` directly with file:// when testing the complete application.

## 8. Test booking

1. Open Halls.
2. Add a hall.
3. Open Chairs.
4. Add chairs.
5. Open Decoration.
6. Add decoration.
7. Open Food.
8. Add vegetarian/non-vegetarian food.
9. Go to checkout.
10. Enter name.
11. Enter 10 digit phone.
12. Select date.
13. Select UPI/Card/Cash.
14. Tap Confirm Booking.

If Java + MySQL are running:

- booking is inserted into `bookings`
- all cart items are inserted into `booking_items`
- payment is inserted into `payments`
- a booking ID is returned
- success animation appears

## Demo fallback

If Java is NOT running, the frontend intentionally shows a successful demo booking animation.

It stores the demo booking in browser localStorage.

Important:

Demo mode does NOT write to MySQL.

To save permanently to MySQL, start Java and MySQL.

## Check the backend

Open:

http://localhost:3000/api/health

Expected:

{
  "status":"OK",
  "message":"Java JDBC server is running"
}

## JDBC concepts used

BookingServer.java demonstrates:

- DriverManager
- Connection
- PreparedStatement
- ResultSet
- SQLException
- executeQuery()
- executeUpdate()
- transactions
- commit()
- rollback()
- generated keys
- MySQL JDBC driver

## Payment

The payment options are a student/demo flow:

- UPI
- Card
- Cash on booking

No real money is charged.

A real Razorpay/Stripe/PayPal integration requires merchant credentials and should be added separately.

## Project flow for viva

Customer selects products
  -> cart
  -> booking form
  -> Node API proxy
  -> Java Servlet-like HTTP API
  -> JDBC
  -> MySQL
  -> booking + items + payment
  -> success response
  -> success animation

## Important

This is a learning/demo application, not a production payment system.
Passwords, authentication and payment gateway security should be strengthened before real deployment.
