import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class StationeryShopGUI 
{
    private JFrame frame;
    private JTextField customerNameField;
    private JTextField productIdField;
    private JTextField quantityField;
    private JTextArea orderSummaryArea;
    private JLabel totalAmountLabel;
    private List<OrderItem> orderItems;
    private int totalAmount;

    private static final String DB_URL = "jdbc:mysql://sql12.freesqldatabase.com:3306/sql12751589";
    private static final String DB_USERNAME = "sql12751589";
    private static final String DB_PASSWORD = "E7vpYsAfzn";

    public StationeryShopGUI() 
    {
        frame = new JFrame("Stationery Shop");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Customer Name: "));
        customerNameField = new JTextField(20);
        topPanel.add(customerNameField);
        JLabel errorLabel = new JLabel();
        errorLabel.setForeground(Color.RED);
        errorLabel.setVisible(false);
        topPanel.add(errorLabel);
        frame.add(topPanel, BorderLayout.NORTH);

        customerNameField.getDocument().addDocumentListener(new DocumentListener() 
        {
            private void validateCustomerName() 
            {
                String customerName = customerNameField.getText().trim();
                if (!customerName.isEmpty() && !customerName.matches("^[a-zA-Z\\s]+$")) 
                {
                    customerNameField.setBackground(Color.PINK);
                    errorLabel.setText("Customer name can only contain letters and spaces.");
                    errorLabel.setVisible(true);
                } else 
                {
                    customerNameField.setBackground(Color.WHITE);
                    errorLabel.setVisible(false);
                }
            }

            public void insertUpdate(DocumentEvent e) 
            { 
                validateCustomerName(); 
            }
            public void removeUpdate(DocumentEvent e) 
            { 
                validateCustomerName(); 
            }
            public void changedUpdate(DocumentEvent e) 
            { 
                validateCustomerName(); 
            }
        });

        JPanel centerPanel = new JPanel(new GridLayout(1, 2));

        JTextArea productListArea = new JTextArea("ID\tName\tPrice (₹)\n" +
                "1\tPen\t10\n" +
                "2\tNotebook\t50\n" +
                "3\tEraser\t5\n" +
                "4\tMarker\t15\n" +
                "5\tFolder\t20\n" +
                "6\tPencil\t5\n" +
                "7\tHighlighter\t20\n" +
                "8\tStapler\t55\n" +
                "9\tGlue\t25\n" +
                "10\tScissors\t60\n");
        productListArea.setEditable(false);
        centerPanel.add(new JScrollPane(productListArea));

        JPanel orderSummaryPanel = new JPanel(new BorderLayout());
        orderSummaryArea = new JTextArea();
        orderSummaryArea.setEditable(false);
        orderSummaryPanel.add(new JScrollPane(orderSummaryArea), BorderLayout.CENTER);
        totalAmountLabel = new JLabel("Total Amount: ₹0");
        orderSummaryPanel.add(totalAmountLabel, BorderLayout.SOUTH);
        centerPanel.add(orderSummaryPanel);

        frame.add(centerPanel, BorderLayout.CENTER);

        
        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(new JLabel("Product ID: "));
        productIdField = new JTextField(5);
        bottomPanel.add(productIdField);
        bottomPanel.add(new JLabel("Quantity: "));
        quantityField = new JTextField(5);
        bottomPanel.add(quantityField);

        JButton addOrderButton = new JButton("Add to Order");
        JButton removeItemButton = new JButton("Remove Item");
        JButton reduceQuantityButton = new JButton("Reduce Quantity");
        JButton submitButton = new JButton("Submit Order");

        bottomPanel.add(addOrderButton);
        bottomPanel.add(removeItemButton);
        bottomPanel.add(reduceQuantityButton);
        bottomPanel.add(submitButton);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        orderItems = new ArrayList<>();

        addOrderButton.addActionListener(e -> handleAddOrder());
        removeItemButton.addActionListener(e -> handleRemoveItem());
        reduceQuantityButton.addActionListener(e -> handleReduceQuantity());
        submitButton.addActionListener(e -> handleSubmitOrder());

        frame.setVisible(true);
    }

    private void handleAddOrder() 
    {
        try 
        {
            int productId = Integer.parseInt(productIdField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());

            if (productId < 1 || productId > 10 || quantity <= 0) 
            {
                showError("Invalid Product ID or Quantity.");
                return;
            }

            String productName = getProductById(productId);
            int productPrice = getPriceById(productId);
            orderItems.add(new OrderItem(productName, productPrice, quantity));
            totalAmount += productPrice * quantity;

            updateOrderSummary();
        } 
        catch (NumberFormatException e) 
        {
                showError("Product ID and Quantity must be numbers.");
        }
    }

    private void handleRemoveItem() 
    {
        if (orderItems.isEmpty()) 
        {
            showError("No items to remove.");
            return;
        }

        String[] itemNames = orderItems.stream().map(OrderItem::toString).toArray(String[]::new);
        String selectedItem = (String) JOptionPane.showInputDialog(frame, "Select an item to remove:", "Remove Item",
                JOptionPane.PLAIN_MESSAGE, null, itemNames, itemNames[0]);

        if (selectedItem != null) 
        {
            orderItems.removeIf(item -> item.toString().equals(selectedItem));
            updateOrderSummary();
        }
    }

    private void handleReduceQuantity() 
    {
        if (orderItems.isEmpty()) {
            showError("No items to reduce quantity.");
            return;
        }

        String[] itemNames = orderItems.stream().map(OrderItem::toString).toArray(String[]::new);
        String selectedItem = (String) JOptionPane.showInputDialog(frame, "Select an item to reduce quantity:", "Reduce Quantity",
                JOptionPane.PLAIN_MESSAGE, null, itemNames, itemNames[0]);

        if (selectedItem != null) 
        {
            for (OrderItem item : orderItems) 
            {
                if (item.toString().equals(selectedItem)) 
                {
                    String newQuantityText = JOptionPane.showInputDialog(frame, "Enter new quantity:");
                    try 
                    {
                        int newQuantity = Integer.parseInt(newQuantityText);
                        if (newQuantity <= 0 || newQuantity >= item.getQuantity()) 
                        {
                            showError("Invalid quantity. Must be positive and less than current quantity.");
                            return;
                        }
                        int difference = item.getQuantity() - newQuantity;
                        item.reduceQuantity(difference);
                        totalAmount -= difference * item.getPrice();

                        if (item.getQuantity() == 0) 
                        {
                            orderItems.remove(item);
                        }
                        updateOrderSummary();
                        break;
                    } 
                    catch (NumberFormatException e) 
                        {
                        showError("Quantity must be a number.");
                    }
                }
            }
        }
    }

    private void handleSubmitOrder() 
    {
        String customerName = customerNameField.getText().trim();
        if (customerName.isEmpty() || !customerName.matches("^[a-zA-Z\\s]+$")) 
        {
            showError("Invalid customer name.");
            return;
        }
        if (orderItems.isEmpty()) 
        {
            showError("No items in the order.");
            return;
        }

        
        int customerId = saveCustomer(customerName);
        if (customerId != -1) 
        {
            saveOrder(customerId, orderItems);
            JOptionPane.showMessageDialog(frame, "Order submitted successfully for " + customerName + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
            orderItems.clear();
            updateOrderSummary();
        } 
        else 
        {
            showError("Failed to save order. Please try again.");
        }
    }

    private int saveCustomer(String customerName) 
    {
        int customerId = -1;
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) 
        {
            String insertCustomerSQL = "INSERT INTO customer (name) VALUES (?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertCustomerSQL, Statement.RETURN_GENERATED_KEYS)) 
            {
                preparedStatement.setString(1, customerName);
                preparedStatement.executeUpdate();

                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) 
                {
                    if (generatedKeys.next()) 
                    {
                        customerId = generatedKeys.getInt(1);
                    }
                }
            }
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
        }
        return customerId;
    }

    private void saveOrder(int customerId, List<OrderItem> orderItems) 
    {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) 
        {
            String insertOrderSQL = "INSERT INTO orders (customer_id, total_amount) VALUES (?, ?)";
            int orderId = -1;
            try (PreparedStatement orderStatement = connection.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS)) 
            {
                orderStatement.setInt(1, customerId);
                orderStatement.setInt(2, totalAmount);
                orderStatement.executeUpdate();

                try (ResultSet generatedKeys = orderStatement.getGeneratedKeys()) 
                {
                    if (generatedKeys.next()) 
                    {
                        orderId = generatedKeys.getInt(1);
                    }
                }
            }

            String insertOrderItemSQL = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
            try (PreparedStatement itemStatement = connection.prepareStatement(insertOrderItemSQL)) 
            {
                for (OrderItem item : orderItems) 
                {
                    itemStatement.setInt(1, orderId);
                    itemStatement.setInt(2, getProductIdByName(item.getName()));
                    itemStatement.setInt(3, item.getQuantity());
                    itemStatement.setInt(4, item.getPrice());
                    itemStatement.addBatch();
                }
                itemStatement.executeBatch();
            }
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
        }
    }

    private int getProductIdByName(String productName) 
    {
        switch (productName) 
        {
            case "Pen": return 1;
            case "Notebook": return 2;
            case "Eraser": return 3;
            case "Marker": return 4;
            case "Folder": return 5;
            case "Pencil": return 6;
            case "Highlighter": return 7;
            case "Stapler": return 8;
            case "Glue": return 9;
            case "Scissors": return 10;
            default: return -1;
        }
    }

    private String getProductById(int productId) 
    {
        switch (productId) 
        {
            case 1: return "Pen";
            case 2: return "Notebook";
            case 3: return "Eraser";
            case 4: return "Marker";
            case 5: return "Folder";
            case 6: return "Pencil";
            case 7: return "Highlighter";
            case 8: return "Stapler";
            case 9: return "Glue";
            case 10: return "Scissors";
            default: return "Unknown";
        }
    }

    private int getPriceById(int productId) 
    {
        switch (productId) 
        {
            case 1: return 10;
            case 2: return 50;
            case 3: return 5;
            case 4: return 15;
            case 5: return 20;
            case 6: return 5;
            case 7: return 20;
            case 8: return 55;
            case 9: return 25;
            case 10: return 60;
            default: return 0;
        }
    }

    private void updateOrderSummary() 
    {
        StringBuilder summary = new StringBuilder("Product\tQuantity\tPrice\n");
        for (OrderItem item : orderItems) 
        {
            summary.append(item).append("\n");
        }
        orderSummaryArea.setText(summary.toString());
        totalAmountLabel.setText("Total Amount: ₹" + totalAmount);
    }

    private void showError(String message) 
    {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(StationeryShopGUI::new);
    }
}

class OrderItem 
{
    private final String name;
    private final int price;
    private int quantity;

    public OrderItem(String name, int price, int quantity) 
    {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getName() 
    {
        return name;
    }

    public int getPrice() 
    {
        return price;
    }

    public int getQuantity() 
    {
        return quantity;
    }

    public void reduceQuantity(int amount) 
    {
        this.quantity -= amount;
    }

  
    public String toString() {
        return name + "\t" + quantity + "\t₹" + (price * quantity);
    }
}
