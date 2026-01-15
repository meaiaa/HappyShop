package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.Comparator;
import java.util.List;

/**
 * TODO
 * You can either directly modify the CustomerModel class to implement the required tasks,
 * or create a subclass of CustomerModel and override specific methods where appropriate.
 */
public class CustomerModel {
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation



    private Product theProduct = null; // product found from search
    private ArrayList<Product> trolley = new ArrayList<>(); // a list of products in trolley
    ArrayList<Product> searchResult = new ArrayList<>();
    private boolean looksLikeProductId(String s) {
        return s !=null && s.matches("//d+");
    }

    // Four UI elements to be passed to CustomerView for display updates.
    private String imageName = "imageHolder.jpg";                // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaTrolley = "";                                // Text area content showing current trolley items (Trolley Page)
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)

    //SELECT productID, Product Name, description, image, unitPrice,inStock quantity
    void search() throws SQLException {
        String keyword = cusView.tfName.getText().trim();

        //empty
        if (keyword.isEmpty()) {
            theProduct = null;
            searchResult.clear();
            displayLaSearchResult = "Please type a Product ID or Name.";
            updateView();
            return;
        }

        //ID priority
        if (looksLikeProductId(keyword)) {
            searchResult.clear();
            theProduct = databaseRW.searchByProductId(keyword); //search database

            if (theProduct != null && theProduct.getStockQuantity() > 0) {
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();

                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", keyword, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;
                System.out.println(displayLaSearchResult);

            } else {
                theProduct = null;
                displayLaSearchResult = "No Product was found with ID " + keyword;
                System.out.println("No Product was found with ID " + keyword);
            }

            updateView();
            return;
        }
    theProduct =null;
    searchResult = databaseRW.searchProduct(keyword);
    if(searchResult ==null||searchResult.isEmpty()) {
        displayLaSearchResult = "NO product was found" + keyword;
        System.out.println("DEBUG name search results = " + (searchResult == null ? "null" : 0));
    }
    else {
        displayLaSearchResult = searchResult.size() + " products found";
    }
    updateView();
    }

    void addToTrolley(){

        if(theProduct!= null){
            System.out.println("Test addToTrolley entered. theProduct=" + (theProduct == null ? "null" : theProduct.getProductId())
                    + " qty=" + (theProduct == null ? "null" : theProduct.getOrderedQuantity()));

            // trolley.add(theProduct) — Product is appended to the end of the trolley.
            // To keep the trolley organized, add code here or call a method that:
            //TODO
            // 1. Merges items with the same product ID (combining their quantities).
            // 2. Sorts the products in the trolley by product ID.
            //trolley.add(theProduct);
            makeOrganisedTrolley();
            displayTaTrolley = ProductListFormatter.buildString(trolley); //build a String for trolley so that we can show it
        }
        else{
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");
        }
        displayTaReceipt=""; // Clear receipt to switch back to trolleyPage (receipt shows only when not empty)
        updateView();
    }

    void makeOrganisedTrolley(){
        for(Product p: trolley){
            if(p.getProductId().equals(theProduct.getProductId())){
                p.setOrderedQuantity(p.getOrderedQuantity()+ theProduct.getOrderedQuantity());
                return;
            }
        }
        Product pNew= new Product(theProduct.getProductId(), theProduct.getProductDescription(), theProduct.getProductImageName(), theProduct.getUnitPrice(), theProduct.getStockQuantity());
        pNew.setOrderedQuantity(theProduct.getOrderedQuantity());
        trolley.add(pNew);
        sortTrolleyByProductId();

    } //increases quantity when same product is added more than once

    void sortTrolleyByProductId(){
        trolley.sort(Comparator.comparing(Product::getProductId));
    };

    public RemoveProductNotifier removeProductNotifier;{}

    void checkOut() throws IOException, SQLException {
        if(!trolley.isEmpty()){
            // Group the products in the trolley by productId to optimize stock checking
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
            ArrayList<Product> groupedTrolley= groupProductsById(trolley);
            ArrayList<Product> insufficientProducts= databaseRW.purchaseStocks(groupedTrolley);

            if(insufficientProducts.isEmpty()){ // If stock is sufficient for all products
                //get OrderHub and tell it to make a new Order
                OrderHub orderHub =OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley);
                trolley.clear();
                displayTaTrolley ="";
                displayTaReceipt = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                System.out.println(displayTaReceipt);
            }
            else{ // Some products have insufficient stock — build an error message to inform the customer
                StringBuilder errorMsg = new StringBuilder();
                for(Product p : insufficientProducts){
                    errorMsg.append("\u2022 "+ p.getProductId()).append(", ")
                            .append(p.getProductDescription()).append(" (Only ")
                            .append(p.getStockQuantity()).append(" available, ")
                            .append(p.getOrderedQuantity()).append(" requested)\n");
                }
                theProduct=null;

                //TODO
                // Add the following logic here:
                // 1. Remove products with insufficient stock from the trolley.
                for (Product insufficient : insufficientProducts) {
                    String badId = insufficient.getProductId();
                    trolley.removeIf(p -> p.getProductId().equals(badId));
                    sortTrolleyByProductId();
                    displayTaTrolley = ProductListFormatter.buildString(trolley);
                    if (removeProductNotifier != null){
                        removeProductNotifier.showRemovalMsg(errorMsg.toString());
                        displayTaTrolley = "Items removed due to insufficient stock";
                    }
                    if (removeProductNotifier != null){
                        removeProductNotifier.closeNotifierWindow();
                    }
                }
                // 2. Trigger a message window to notify the customer about the insufficient stock, rather than directly changing displayLaSearchResult.
                //You can use the provided RemoveProductNotifier class and its showRemovalMsg method for this purpose.
                //remember close the message window where appropriate (using method closeNotifierWindow() of RemoveProductNotifier class)
                displayLaSearchResult = "Checkout failed due to insufficient stock for the following products:\n" + errorMsg.toString();
                System.out.println("stock is not enough");
            }
        }
        else{
            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
        }
        updateView();
    }

    /**
     * Groups products by their productId to optimize database queries and updates.
     * By grouping products, we can check the stock for a given `productId` once, rather than repeatedly
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();
        for (Product p : proList) {
            String id = p.getProductId();
            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Make a shallow copy to avoid modifying the original
                grouped.put(id,new Product(p.getProductId(),p.getProductDescription(),
                        p.getProductImageName(),p.getUnitPrice(),p.getStockQuantity()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    void cancel(){
        trolley.clear();
        displayTaTrolley="";
        if (removeProductNotifier != null){
            removeProductNotifier.closeNotifierWindow();
        }
        updateView();
    }
    void closeReceipt(){
        displayTaReceipt="";
    }

    void updateView() {
        if(theProduct != null){
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder +imageName; //relative file path, eg images/0001.jpg
            // Get the full absolute path to the image
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString(); //get the image full Uri then convert to String
            System.out.println("Image absolute path: " + imageFullPath); // Debugging to ensure path is correct
        }
        else{
            imageName = "imageHolder.jpg";
        }
        cusView.update(imageName, displayLaSearchResult, displayTaTrolley,displayTaReceipt);
        System.out.println("DEBUG pushing results to view" + (searchResult == null ? "" : searchResult));
        cusView.updateSearchResult(searchResult);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object

    //for test only
    public ArrayList<Product> getTrolley() {
        return trolley;
    }
    public void setTheProduct(Product theProduct) {
        this.theProduct = theProduct;
    }
    void addFromSearchResult(Product p, int qty) {
        System.out.println("Test model addFromSearchResult product=" + p.getProductId() + " qty=" + qty);
        if(qty <= 0) return;
        Product chosen = new Product(p.getProductId(), p.getProductDescription(), p.getProductImageName(), p.getUnitPrice(), p.getStockQuantity());
        chosen.setOrderedQuantity(qty);
        System.out.println("Test model addFromSearchResult product=" + p.getProductId() + " qty=" + qty);
        theProduct = chosen;
        addToTrolley();
    }
}
