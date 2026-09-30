/*
 * HALLBOOK - Java JDBC backend
 *
 * Flow:
 * Browser -> Node.js -> this Java API -> JDBC -> MySQL
 *
 * Before running:
 * 1. Run database.sql in MySQL.
 * 2. Change DB_PASSWORD below.
 * 3. Add MySQL Connector/J dependency using Maven.
 * 4. Run this class.
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class BookingServer {

    static final String DB_URL =
        "jdbc:mysql://localhost:3306/hallbook?serverTimezone=Asia/Kolkata";
    static final String DB_USER = "root";

    // CHANGE THIS to your MySQL password.
    static final String DB_PASSWORD = "YOUR_MYSQL_PASSWORD";

    static final int PORT = 8080;

    static Connection db() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    static void send(HttpExchange ex, int code, String json) throws IOException {
        byte[] out=json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set(
            "Content-Type","application/json; charset=UTF-8"
        );

        ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().set(
            "Access-Control-Allow-Headers","Content-Type"
        );
        ex.getResponseHeaders().set(
            "Access-Control-Allow-Methods","GET,POST,OPTIONS"
        );

        ex.sendResponseHeaders(code,out.length);

        try(OutputStream os=ex.getResponseBody()){
            os.write(out);
        }
    }

    static String body(HttpExchange ex)throws IOException{
        return new String(
            ex.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );
    }

    /*
      Small JSON helper for this beginner project.
      In production, use Jackson/Gson.
    */
    static String value(String json,String key){
        String k="\"" + key + "\":";
        int p=json.indexOf(k);

        if(p<0) return "";

        p+=k.length();

        while(p<json.length() &&
              Character.isWhitespace(json.charAt(p))) p++;

        if(p<json.length() && json.charAt(p)=='"'){
            int e=p+1;

            while(e<json.length()){
                if(json.charAt(e)=='"' &&
                   json.charAt(e-1)!='\\'){
                    return json.substring(p+1,e)
                        .replace("\\\"","\"")
                        .replace("\\\\","\\");
                }
                e++;
            }
            return "";
        }

        int e=p;

        while(e<json.length() &&
              json.charAt(e)!=',' &&
              json.charAt(e)!='}') e++;

        return json.substring(p,e).trim();
    }

    static String escape(String s){
        if(s==null) return "";
        return s
            .replace("\\","\\\\")
            .replace("\"","\\\"")
            .replace("\n","\\n")
            .replace("\r","\\r");
    }

    static String query(String sql) throws SQLException {
        StringBuilder j=new StringBuilder("[");

        try(Connection c=db();
            PreparedStatement p=c.prepareStatement(sql);
            ResultSet r=p.executeQuery()){

            ResultSetMetaData m=r.getMetaData();
            int cols=m.getColumnCount();
            boolean first=true;

            while(r.next()){
                if(!first) j.append(",");
                first=false;

                j.append("{");

                for(int i=1;i<=cols;i++){
                    if(i>1) j.append(",");

                    String name=m.getColumnLabel(i);
                    Object v=r.getObject(i);

                    j.append("\"").append(escape(name)).append("\":");

                    if(v==null){
                        j.append("null");
                    }else if(v instanceof Number){
                        j.append(v);
                    }else{
                        j.append("\"")
                         .append(escape(v.toString()))
                         .append("\"");
                    }
                }

                j.append("}");
            }
        }

        return j.append("]").toString();
    }

    static void getData(HttpExchange ex,String path,String query)
        throws Exception{

        if(path.equals("/api/health")){
            send(ex,200,
                "{\"status\":\"OK\",\"message\":\"Java JDBC server is running\"}");
            return;
        }

        if(path.equals("/api/halls")){
            send(ex,200,query(
                "SELECT id,name,description,capacity,price " +
                "FROM function_halls WHERE active=TRUE ORDER BY id"
            ));
            return;
        }

        if(path.equals("/api/chairs")){
            send(ex,200,query(
                "SELECT id,name,description,quantity,price " +
                "FROM chair_packages WHERE active=TRUE ORDER BY id"
            ));
            return;
        }

        if(path.equals("/api/decorations")){
            send(ex,200,query(
                "SELECT id,name,description,price " +
                "FROM decorations WHERE active=TRUE ORDER BY id"
            ));
            return;
        }

        if(path.equals("/api/food")){
            String type="VEG";

            if(query!=null && query.contains("type=")){
                type=query.substring(query.indexOf("type=")+5)
                    .toUpperCase();

                if(type.contains("&")){
                    type=type.substring(0,type.indexOf("&"));
                }
            }

            if(!type.equals("VEG") && !type.equals("NON_VEG")){
                type="VEG";
            }

            try(Connection c=db();
                PreparedStatement p=c.prepareStatement(
                    "SELECT id,name,food_type,description,price " +
                    "FROM food_items " +
                    "WHERE active=TRUE AND food_type=? " +
                    "ORDER BY id")){

                p.setString(1,type);

                try(ResultSet r=p.executeQuery()){
                    StringBuilder j=new StringBuilder("[");
                    boolean first=true;

                    while(r.next()){
                        if(!first) j.append(",");
                        first=false;

                        j.append("{")
                         .append("\"id\":").append(r.getInt("id"))
                         .append(",\"name\":\"")
                         .append(escape(r.getString("name")))
                         .append("\"")
                         .append(",\"food_type\":\"")
                         .append(escape(r.getString("food_type")))
                         .append("\"")
                         .append(",\"description\":\"")
                         .append(escape(r.getString("description")))
                         .append("\"")
                         .append(",\"price\":")
                         .append(r.getBigDecimal("price"))
                         .append("}");
                    }

                    send(ex,200,j.append("]").toString());
                }
            }

            return;
        }

        if(path.equals("/api/bookings")){
            send(ex,200,query(
                "SELECT booking_id,customer_name,phone,event_date," +
                "payment_method,total,status,created_at " +
                "FROM bookings ORDER BY id DESC"
            ));
            return;
        }

        send(ex,404,"{\"message\":\"API endpoint not found\"}");
    }

    static String extractArray(String json,String key){
        String k="\"" + key + "\":";
        int p=json.indexOf(k);

        if(p<0) return "";

        p=json.indexOf("[",p);

        if(p<0) return "";

        int depth=0;

        for(int i=p;i<json.length();i++){
            char ch=json.charAt(i);

            if(ch=='['){
                depth++;
            }else if(ch==']'){
                depth--;

                if(depth==0){
                    return json.substring(p+1,i);
                }
            }
        }

        return "";
    }

    static List<String> splitObjects(String a){
        List<String> out=new ArrayList<>();
        int depth=0;
        int start=-1;

        for(int i=0;i<a.length();i++){
            char ch=a.charAt(i);

            if(ch=='{'){
                if(depth==0) start=i;
                depth++;
            }else if(ch=='}'){
                depth--;

                if(depth==0 && start>=0){
                    out.add(a.substring(start,i+1));
                    start=-1;
                }
            }
        }

        return out;
    }

    static void createBooking(HttpExchange ex)throws Exception{

        String json=body(ex);

        String name=value(json,"customerName");
        String phone=value(json,"phone");
        String date=value(json,"eventDate");
        String payment=value(json,"payment");
        String totalText=value(json,"total");

        if(name.isBlank() ||
           phone.isBlank() ||
           date.isBlank() ||
           payment.isBlank()){

            send(ex,400,
                "{\"message\":\"Name, phone, date and payment are required\"}");
            return;
        }

        if(!phone.matches("[0-9]{10}")){
            send(ex,400,
                "{\"message\":\"Phone number must contain 10 digits\"}");
            return;
        }

        if(!payment.equals("UPI") &&
           !payment.equals("CARD") &&
           !payment.equals("CASH")){

            send(ex,400,
                "{\"message\":\"Invalid payment method\"}");
            return;
        }

        BigDecimal total;

        try{
            total=new BigDecimal(totalText);
        }catch(Exception e){
            send(ex,400,"{\"message\":\"Invalid total\"}");
            return;
        }

        if(total.compareTo(BigDecimal.ZERO)<0){
            send(ex,400,"{\"message\":\"Invalid total amount\"}");
            return;
        }

        String bookingId =
            "HB-" +
            UUID.randomUUID()
                .toString()
                .substring(0,8)
                .toUpperCase();

        try(Connection c=db()){

            c.setAutoCommit(false);

            try{

                int bookingDbId;

                String insert =
                    "INSERT INTO bookings(" +
                    "booking_id,customer_name,phone,event_date," +
                    "payment_method,total,status" +
                    ") VALUES(?,?,?,?,?,?,?)";

                try(PreparedStatement p=
                        c.prepareStatement(
                            insert,
                            Statement.RETURN_GENERATED_KEYS)){

                    p.setString(1,bookingId);
                    p.setString(2,name);
                    p.setString(3,phone);
                    p.setDate(4,Date.valueOf(date));
                    p.setString(5,payment);
                    p.setBigDecimal(6,total);
                    p.setString(7,"CONFIRMED");

                    p.executeUpdate();

                    try(ResultSet keys=p.getGeneratedKeys()){
                        if(!keys.next()){
                            throw new SQLException(
                                "Could not create booking ID"
                            );
                        }
                        bookingDbId=keys.getInt(1);
                    }
                }

                String items=extractArray(json,"items");

                for(String obj:splitObjects(items)){

                    String itemName=value(obj,"name");
                    String price=value(obj,"price");
                    String qty=value(obj,"qty");
                    String itemType=value(obj,"itemType");

                    if(itemName.isBlank()) continue;

                    if(itemType.isBlank()){
                        itemType="SERVICE";
                    }

                    try(PreparedStatement p=
                            c.prepareStatement(
                                "INSERT INTO booking_items(" +
                                "booking_id,item_type,item_name," +
                                "unit_price,quantity" +
                                ") VALUES(?,?,?,?,?)")){

                        p.setInt(1,bookingDbId);
                        p.setString(2,itemType);
                        p.setString(3,itemName);
                        p.setBigDecimal(
                            4,
                            new BigDecimal(price)
                        );
                        p.setInt(5,Integer.parseInt(qty));

                        p.executeUpdate();
                    }
                }

                String paymentStatus =
                    payment.equals("CASH") ? "PENDING" : "PAID";

                Timestamp paidAt =
                    payment.equals("CASH")
                    ? null
                    : new Timestamp(System.currentTimeMillis());

                try(PreparedStatement p=
                        c.prepareStatement(
                            "INSERT INTO payments(" +
                            "booking_id,payment_reference,amount," +
                            "method,status,paid_at" +
                            ") VALUES(?,?,?,?,?,?)")){

                    p.setInt(1,bookingDbId);
                    p.setString(2,"DEMO-"+bookingId);
                    p.setBigDecimal(3,total);
                    p.setString(4,payment);
                    p.setString(5,paymentStatus);

                    if(paidAt==null){
                        p.setNull(6,Types.TIMESTAMP);
                    }else{
                        p.setTimestamp(6,paidAt);
                    }

                    p.executeUpdate();
                }

                c.commit();

            }catch(Exception e){
                c.rollback();
                throw e;
            }
        }

        send(ex,200,
            "{\"success\":true," +
            "\"bookingId\":\""+escape(bookingId)+"\"," +
            "\"message\":\"Booking saved successfully using JDBC\"}"
        );
    }

    static void route(HttpExchange ex){

        try{

            String method=ex.getRequestMethod();

            if(method.equalsIgnoreCase("OPTIONS")){
                send(ex,204,"");
                return;
            }

            String path=ex.getRequestURI().getPath();

            if(method.equalsIgnoreCase("GET")){
                getData(
                    ex,
                    path,
                    ex.getRequestURI().getQuery()
                );
                return;
            }

            if(method.equalsIgnoreCase("POST") &&
               path.equals("/api/bookings")){

                createBooking(ex);
                return;
            }

            send(ex,404,
                "{\"message\":\"Route not found\"}");

        }catch(Exception e){

            e.printStackTrace();

            try{
                send(
                    ex,
                    500,
                    "{\"success\":false," +
                    "\"message\":\""+escape(
                        e.getMessage()==null
                        ? "Server error"
                        : e.getMessage()
                    )+"\"}"
                );
            }catch(IOException ignored){}
        }
    }

    public static void main(String[] args)throws Exception{

        Class.forName("com.mysql.cj.jdbc.Driver");

        try(Connection c=db()){
            System.out.println(
                "Connected to MySQL database: hallbook"
            );
        }

        HttpServer server=
            HttpServer.create(
                new InetSocketAddress(PORT),
                0
            );

        server.createContext("/api/",BookingServer::route);

        server.start();

        System.out.println(
            "Java JDBC API running at http://localhost:"+PORT
        );
    }
}
