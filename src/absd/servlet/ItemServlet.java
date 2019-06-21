package absd.servlet;

import javax.annotation.Resource;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(urlPatterns = "/items")
public class ItemServlet extends HttpServlet {

    @Resource(name = "java:comp/env/jdbc/pool")
    private DataSource ds;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("working......");

        JsonReader reader = Json.createReader(req.getReader());
        resp.setContentType("application/json");

        PrintWriter out = resp.getWriter();

        Connection connection = null;

        try {
            JsonObject item = reader.readObject();

            String code = item.getString("code");
            String description = item.getString("description");
            String unitPrice = item.getString("unitPrice");
            String qtyOnHand = item.getString("qtyOnHand");

            System.out.println("code : " + code);
            System.out.println("description : " + description);
            System.out.println("unitprice : " + unitPrice);
            System.out.println("qtyonhand : " + qtyOnHand);


            connection = ds.getConnection();

            PreparedStatement pstm = connection.prepareStatement("INSERT INTO Item VALUES (?,?,?,?)");
            pstm.setObject(1, code);
            pstm.setObject(2, description);
            pstm.setObject(3, unitPrice);
            pstm.setObject(4, qtyOnHand);

            boolean result = pstm.executeUpdate() > 0;

            if (result) {

                out.println("true");
            } else {
                out.println("false");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            out.println("false");
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            out.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("get all  working....");

        try (PrintWriter out = resp.getWriter()) {

            resp.setContentType("application/json");

            try {
                Connection connections = ds.getConnection();

                Statement stm = connections.createStatement();
                ResultSet rst = stm.executeQuery("SELECT * FROM Item");

                JsonArrayBuilder items = Json.createArrayBuilder();

                while (rst.next()) {
                    String codes = rst.getString("code");
                    String description = rst.getString("description");
                    String unitPrice = rst.getString("qtyOnHand");
                    String qtyOnHand = rst.getString("qtyOnHand");

                    JsonObject customer = Json.createObjectBuilder()
                            .add("code", codes)
                            .add("description", description)
                            .add("unitPrice", unitPrice)
                            .add("qtyOnHand", qtyOnHand)
                            .build();
                    items.add(customer);
                }

                out.println(items.build().toString());

                connections.close();
            } catch (Exception ex) {
                resp.sendError(500, ex.getMessage());
                ex.printStackTrace();
            }

        }
    }
}
