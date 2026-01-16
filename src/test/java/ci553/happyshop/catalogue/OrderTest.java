package ci553.happyshop.catalogue;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import ci553.happyshop.orderManagement.OrderState;

class OrderTest {
    @Test
    void constructorStoresMetadataCorrectly() {
        Product p1 = new Product("0001", "40 inch TV", "0001.jpg", 269.00, 95);
        p1.setOrderedQuantity(2);
        ArrayList<Product> trolley = new ArrayList<>();
        trolley.add(p1);
        int orderId = 123;
        String dateTime = "2026-01-15 10:30:00";
        Order order = new Order(orderId, OrderState.Ordered, dateTime, trolley);
        assertEquals(orderId, order.getOrderId());
        assertEquals(OrderState.Ordered, order.getState());
        assertEquals(dateTime, order.getOrderedDateTime());
        assertEquals(1, order.getProductList().size());
        assertEquals("0001", order.getProductList().getFirst().getProductId());
        assertEquals(2, order.getProductList().getFirst().getOrderedQuantity());
    }

    @Test
    void createOrderTest() {
        Product p1 = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 97);
        p1.setOrderedQuantity(1);
        ArrayList<Product> trolley = new ArrayList<>();
        trolley.add(p1);
        Order order = new Order(5, OrderState.Ordered, "2026-01-15 10:30", trolley);
        String details = order.orderDetails();
        assertTrue(details.contains("5"));
        assertTrue(details.contains("DAB Radio"));
        assertTrue(details.contains("2026-01-15"));
        assertTrue(details.contains("Ordered"));
        assertTrue(details.contains("29.99") || details.contains("£"));
    }

}