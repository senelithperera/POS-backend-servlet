package absd.servlet;

import javax.annotation.Resource;
import javax.json.Json;
import javax.json.JsonArray;
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

@WebServlet(urlPatterns = "/orders")
public class OrderServlet extends HttpServlet {

    @Resource(name = "java:comp/env/jdbc/pool")
    private DataSource ds;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("working");
        Connection connection = null;
        resp.setContentType("application/json");

        PrintWriter out = resp.getWriter();

        try {

            JsonReader reader = Json.createReader(req.getReader());
            JsonObject JSON = reader.readObject();

            JsonObject order = JSON.getJsonObject("order");

            String orderID = order.getString("orderID");

            JsonArray orderDetails = JSON.getJsonArray("orderDetails");

            System.out.println("sdfs " + orderDetails.toString());
            connection = ds.getConnection();
            connection.setAutoCommit(false);

            String sql = "INSERT INTO Orders VALUES (?,?,?)";
            PreparedStatement pstm = connection.prepareStatement(sql);
            pstm.setString(2, order.getString("date"));
            pstm.setString(3, order.getString("customerID"));
            pstm.setString(1, orderID);

            int affectedRows = pstm.executeUpdate();

            if (affectedRows == 0) {
                connection.rollback();
                out.println("false");
                return;
            } else {
                out.println("true");
                System.out.println("Order OK");
            }

            sql = "INSERT INTO  ItemDetail VALUES (?,?,?,?)";
            pstm = connection.prepareStatement(sql);

            for (int i = 0; i < orderDetails.size(); i++) {

                System.out.println(i);
                JsonObject orderDetail = orderDetails.get(i).asJsonObject();

                pstm.setObject(1, orderID);
                pstm.setObject(2, orderDetail.getString("code"));
                pstm.setObject(3, orderDetail.getInt("qty"));
                pstm.setObject(4, orderDetail.getInt("subPrice"));

                affectedRows = pstm.executeUpdate();

                if (affectedRows == 0) {
                    connection.rollback();
                    return;
                }

                Statement stm = connection.createStatement();
                ResultSet rst = stm.executeQuery("SELECT * FROM Item WHERE code='" + orderDetail.getString("code") + "'");

                int qtyOnHand = 0;

                if (rst.next()) {
                    qtyOnHand = rst.getInt("qtyOnHand");
                }

                PreparedStatement pstm2 = connection.prepareStatement("UPDATE Item SET qtyOnHand=? WHERE code=?");

                pstm2.setObject(1, qtyOnHand - orderDetail.getInt("qty"));
                pstm2.setObject(2, orderDetail.getString("code"));

                affectedRows = pstm2.executeUpdate();

                if (affectedRows == 0) {
                    connection.rollback();
                    out.println("flase");
                    return;
                } else {
                    out.println("true");
                }
            }

            connection.commit();

            System.out.println("Order Success");

        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException ex1) {
                System.out.println(ex1);
            }
            System.out.println(ex);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println(ex);
            }
        }
    }
}

