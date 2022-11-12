package com.example.namegenderservlet;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.google.gson.Gson;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.*;

@WebServlet(name = "getGender", urlPatterns = {"/getGender","/delete"})
public class GetGenderServlet extends HttpServlet {
    public void init() {
    }
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        String url = request.getRequestURI();
        if(url.endsWith("delete") || url.endsWith("delete/")){
            clear();
            response.setStatus(200);
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write("<h4>Successfully cleared!</h4>");
            return;
        }
        String name = request.getParameter("name");
        if(name == null){
            List<Record> docs = getHistory();
            String resp = "<h4>Search History:</h4>\n" +
                    "<table border=\"1\">\n" +
                    "    <tr>\n" +
                    "        <th>Name</th>\n" +
                    "        <th>Gender</th>\n" +
                    "        <th>Probability</th>\n" +
                    "        <th>Number of Searches</th>\n" +
                    "    </tr>";
            for(Record r : docs){
                resp += "<tr>";
                resp += "<td>"+r.name+"</td>";
                resp += "<td>"+r.gender+"</td>";
                resp += "<td>"+r.prob+"</td>";
                resp += "<td>"+r.cnt+"</td>";
                resp += "</tr>";
            }
            resp += "</table>";
            response.setStatus(200);
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write(resp);
        }
        else{
            Document doc = search(name);
            String res = doc.toJson();
            response.setStatus(200);
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write(res);
        }
    }
    public void clear(){
        String uri = "mongodb+srv://jiezhu:jiezhu123@cluster0.ft6jpin.mongodb.net/?retryWrites=true&w=majority";

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("name_gender");
            MongoCollection<Document> collection = database.getCollection("name_gender");
            Bson query = empty();
            try {
                DeleteResult result = collection.deleteMany(query);
                System.out.println("Deleted document count: " + result.getDeletedCount());
            } catch (MongoException me) {
                System.err.println("Unable to delete due to an error: " + me);
            }
        }
    }

    public void destroy() {
        clear();
    }
    public Document search(String name) {
        String uri = "mongodb+srv://jiezhu:jiezhu123@cluster0.ft6jpin.mongodb.net/?retryWrites=true&w=majority";
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("name_gender");
        MongoCollection<Document> collection = database.getCollection("name_gender");
        try (mongoClient) {
            Document doc = fetch(name);
            InsertOneResult result = collection.insertOne(doc);
            System.out.println("Success! Inserted document id: " + result.getInsertedId());
            return doc;
        }
        catch (Exception e) {
        }
        return null;
    }
    public List<Record> getHistory(){
        String uri = "mongodb+srv://jiezhu:jiezhu123@cluster0.ft6jpin.mongodb.net/?retryWrites=true&w=majority";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase database = mongoClient.getDatabase("name_gender");
            MongoCollection<Document> collection = database.getCollection("name_gender");
            Bson query = empty();
            MongoCursor<Document> cursor = collection.find(query).iterator();
            Map<String, Record> map = new HashMap<>();

            try {
                while(cursor.hasNext()) {
                    Document doc = cursor.next();
                    String name = doc.getString("name").toLowerCase();
                    if(map.containsKey(name)){
                        Record rec = map.get(name);
                        rec.cnt++;
                    }
                    else{
                        String gender = doc.getString("gender");
                        double prob = doc.getDouble("probability");
                        map.put(name, new Record(name, gender, prob, 1));
                    }
                }
                List<Record> res = new ArrayList<>();
                for(Map.Entry<String, Record> e : map.entrySet()){
                    res.add(e.getValue());
                }
                Collections.sort(res,(o1,o2)->(o2.cnt-o1.cnt));
                return res;
            } finally {
                cursor.close();
            }
        }
//        return null;
    }
    private Document fetch(String name) {
        String urlString = "https://api.genderize.io?name="+name;
        String response = "";
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String str;
            while ((str = in.readLine()) != null) {
                response += str;
            }
            in.close();
        } catch (IOException e) {
            System.out.println("Eeek, an exception");
        }
        Document doc = Document.parse(response);
        return new Document(doc);
    }
}