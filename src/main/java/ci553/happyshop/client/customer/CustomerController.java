package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import javafx.fxml.FXML;

import java.io.IOException;
import java.sql.SQLException;

public class CustomerController {
    public CustomerModel cusModel;
    public void addFromSearchResult(Product p, int qty) throws SQLException {
        System.out.println("DEBUG controller addFromSearchResult called qty=" + qty);
        cusModel.addFromSearchResult(p, qty);
    }

    public void doAction(String action) throws SQLException, IOException {
        switch (action) {
            case "Search":
                cusModel.search();
                break;
            case "Add to Trolley":
                cusModel.addToTrolley();
                break;
            case "Cancel":
                cusModel.cancel();
                break;
            case "Check Out":
                cusModel.checkOut();
                break;
            case "OK & Close":
                cusModel.closeReceipt();
                break;
        }
    }

    }


