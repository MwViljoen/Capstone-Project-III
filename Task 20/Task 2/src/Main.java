/*I did not modify the code since the way i wrote the previous delivery code was a bit difficult to just edit
 * and change to using a database
 * I also made a few changes, i am not using object for the restaurant and customers anymore
 * I am working directly from the Database
 *
 * I used Microsoft SQL for this task
 * */

/***
 * This is a very basic presentation of a delivery system using a SQL Database with tables
 * @author  Willem Viljoen
 * @version 1.0
 */

import java.io.FileWriter; // file writer to create invoice
import java.sql.*;// all sql utils was imported
// https://www.javatpoint.com/java-get-current-date for time and date
import java.time.LocalDateTime;// used to get local time for time and date stamp for finalize
import java.time.format.DateTimeFormatter;// also used for time and date
import java.util.ArrayList; // using array list for items and prices
import java.util.Scanner;// scanner for user input

public class Main {

// My goal here was to show a clear flow of how the program runs step by step

    public static void main(String[] args) {
        int userMenuNumber; // menu for operator to select the operation
        // Setting up database connections
        try {// try connection if fails it will show why with catch
            Connection connection = DriverManager.getConnection(// logging in with username and password
                    "jdbc:sqlserver://localhost;database=QuickFoodMS",
                    "MW",
                    "1234"
            );
            // Menu for user to add, edit customer information or add new customers. Make new orders and finalize
            // or quit
            do {
                userMenuNumber = userMenu(); // method returning user option
                switch (userMenuNumber) { // using switch to control operations as per input
                    case 1:
                        // the connection is cast to most methods to utilise interaction with DB
                        newDelivery(connection); // creates a new delivery/order
                        break;
                    case 2:
                        finalizeDelivery(connection);// finalizes delivery/order
                        break;
                    case 3:
                        addCustomerToDB(connection);// creates a new customer
                        break;
                    case 4:
                        editCustomerInDB(connection);// edits a existing customer
                        break;
                    case 5:
                        removeCustomer(connection);// removes existing customer
                        break;
                    default:
                        break;
                }
            } while (userMenuNumber != 0);
            // once user quit the connection will be closed to DB( Database )
            connection.close();

        } catch (SQLException e) {// in case something goes wrong
            e.printStackTrace();
            System.out.println("Something went wrong, contact SP for help");
        }
    }

// Here we finalize the delivery this is the main method to finalize delivery and invoice
// most of these sequences are automated and there is little user interaction

    /**
     * Finalize Order/delivery
     * @param connection Connection to database
     */
    public static void finalizeDelivery(Connection connection) {
        int orderToFinalize = printIncompleteOrders(connection);// printing incomplete orders to select from
        // if all went according to plan it wil execute the remaining steps, else do nothing and go back to menu
        // if is used to determine that the selection of order number is valid
        if (orderToFinalize > 0) {
            // once user selected order the order number is returned and used here to finalize
            addDateToFinalize(connection, orderToFinalize);
            // here we fetch all required data and create the invoice for customer
            createInvoice(connection, orderToFinalize);
        }
    }
/* from here on i will only comment on not so obvious operations and programming*/

    /***
     * Creating invoice for customer
     * @param connection Connection to database
     * @param orderToFinalize )rder selected by user to finalize
     */
    public static void  createInvoice(Connection connection, int orderToFinalize){
        ArrayList<String> foodNames = new ArrayList<>();
        //
        try{
            //1st result set
            Statement statement = connection.createStatement();// new statement
            ResultSet results;// declare result set for select
            // selecting the Orders table and the row that will be used for invoice
            results = statement.executeQuery("SELECT * FROM Orders WHERE OrderNr = " + orderToFinalize);// results for init
            // we move cursor to the first line
            results.next();
            // here all info neededd is extracted from the orders row selected to be finalized
            int restaurantNumber = results.getInt("RestaurantNr");
            int qtyItem1 = results.getInt("QtyItem1");
            int qtyItem2 = results.getInt("QtyItem2");
            int qtyItem3 = results.getInt("QtyItem3");
            int qtyItem4 = results.getInt("QtyItem4");
            String customerEmail = results.getString("CustomerEmail");
            String  restaurantContactNr = results.getString("RestaurantContactNr");
            String  customerSpecial = results.getString("CustomerSpecial");
            String  finalized = results.getString("Finalized");

            //finding name of restaurant
            // here we only use one result set
            results = statement.executeQuery("SELECT rName FROM Restaurant_Table WHERE Menu = " + restaurantNumber);
            results.next();// moving cursor to first table
            String restaurantName = results.getString("rName");

            // now we need to get food names and indicate quantity
            // we do so by placing the whole row in a arraylist as [name , price, name , price .........]
            results = statement.executeQuery("SELECT * FROM Menus WHERE Menu = " + restaurantNumber);
            while (results.next()) {
                foodNames.add(results.getString("Name"));
                foodNames.add(Integer.toString(results.getInt("Price")));
            }
// fetching data about customer from DB
            // we select all data of customer for use in invoice
            results = statement.executeQuery("SELECT * FROM Customers_Table WHERE cEmail = '" + customerEmail + "'");
            results.next();
            String cName = results.getString("cName");
            String cNumber = results.getString("cNumber");
            String cAddress = results.getString("cAddress");
            String cCity = results.getString("cCity");
            String cEmail = results.getString("cEmail");

// Select driver from DB With lowest load
            // here we call a method that determines the driver in same location and has the least load
            String dName = SelectDriver(connection, cCity);
            // if the name was not selected that means that the customer can not be served and there is no suitable location
            // nearby
            if(dName == null){
                // we print this out to console instead of a text file
                System.out.println("\"Sorry! Our drivers are too far away from you to be able to\r\n"
                        + "deliver to your location.\"" +
                        "Thus a invoice will not be created for this Delivery");
            }
            else{
// list items for invoice
                // if the name is not null we know a suitable driver was selected we then tally the
                // items and prices to add to invoice
                String items = "";// items is a string generated for use in the invoice
                int total = 0; // total cost
                // the if statement determines wether a item has a quantity if not skipp for that item
                if(qtyItem1 != 0){
                    // here we now use the array list
                    items += qtyItem1 + " x " + foodNames.get(0) + " at R " + foodNames.get(1) + "\n";
                    total += qtyItem1 * Integer.parseInt(foodNames.get(1));// changing string to int
                }
                if(qtyItem2 != 0){
                    items += qtyItem2 + " x " + foodNames.get(2) + " at R " + foodNames.get(3) + "\n";
                    total += qtyItem2 * Integer.parseInt(foodNames.get(3));
                }
                if(qtyItem3 != 0){
                    items += qtyItem3 + " x " + foodNames.get(4) + " at R " + foodNames.get(5) + "\n";
                    total += qtyItem3 * Integer.parseInt(foodNames.get(5));
                }
                if(qtyItem4 != 0){
                    items += qtyItem4 + " x " + foodNames.get(6) + " at R " + foodNames.get(7) + "\n";
                    total += qtyItem4 * Integer.parseInt(foodNames.get(7));
                }
                // now we write the information to invoice
                try{
                    FileWriter write = new FileWriter("invoice.txt");// create file
                    String invoice;

                    invoice = "Order Number: " + orderToFinalize + "\r\n" // construct string for invoice to print
                            + "Customer: " + cName + "\r\n"
                            + "Email: " + cEmail + "\r\n"
                            + "Phone Number: " + cNumber + "\r\n"
                            + "Location: " + cCity + "\r\n\r\n"
                            + "You have ordered the following from " + restaurantName + " in " + cCity + ":\r\n\r\n"
                            + items + "\r\n"
                            + "Special instructions : " + customerSpecial + "\r\n\r\n"
                            + "Total: R " + total + "\r\n\r\n"
                            + "The Driver " + dName + ", Is nearest to the restaurant and" +
                            " will be delivering your \rorder to you at:\r\n\r\n"
                            + cAddress + "\r\n\r\n"
                            + "If you need to contact the restaurant, their number is " + restaurantContactNr + "\r\n\r\n"
                            + "Order Completed On : " + finalized;

                    write.write(invoice);// write data to file
                    write.close();// close write operation
                }catch (Exception e){
                    // try catch is for file writing operations
                    e.printStackTrace();
                    System.out.println("Could not print invoice, something went wrong");// in case something goes wrong
                }
            }

            foodNames.clear();// clearing arraylist
            statement.close();// closing statement
            results.close();// closing result set

        }catch (Exception e){
            // try catch is for SQL operations
            System.out.println("Could not Finalize Order, Something went Wrong");
            e.printStackTrace();
        }
    }
// select driver method, we pass the DB connection and city of customer, select suitable driver and return name
    // we also increment load of driver in db

    /***
     * Selecting suitable driver
     * @param connection Connection to database
     * @param cCity Customers city/location
     * @return Returns name of selected driver
     */
    public static String SelectDriver(Connection connection, String cCity){
        boolean status = false;
        int load, placeholder = 100;
        String dName = "";

        try{// try catch for
            Statement statement5 = connection.createStatement();// new statement
            ResultSet results5;// declare result set for select
            results5 = statement5.executeQuery("SELECT * FROM Drivers_Table WHERE City = '" + cCity +"'");// results for init
            // filter through drivers with location close by and their load
            while (results5.next()){
                load = results5.getInt("Que");
                if(load < placeholder){
                    placeholder = load;
                    dName = results5.getString("dName");
                    status = true;
                }
            }
            // if name was found we increment the load of that driver
            if(status){
                placeholder += 1;
               statement5.executeUpdate("UPDATE Drivers_Table SET Que = " + placeholder + " WHERE dName = '" + dName + "'" );
            }

        }catch (Exception e){
            // in case something goes wrong
            e.printStackTrace();
            System.out.println("Could not select driver");
        }
        // if name was found we return name else we return null
        if(status){
            return dName;
        }
        else{
            return null;
        }
    }
// method to finalize order, this method writes the date to the order that is finalized

    /***
     * Adding date and time when order is finalized
     * @param connection Connection to database
     * @param orderToFinalize Selected order to finalize
     */
    public static void addDateToFinalize(Connection connection, int orderToFinalize) {
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            PreparedStatement pstmt = connection.prepareStatement("UPDATE Orders SET Finalized = ? WHERE OrderNr = ?");
            pstmt.setString(1, dtf.format(now));
            pstmt.setInt(2, orderToFinalize);
            pstmt.executeUpdate();
            pstmt.close();
            // indication that operation was successfull
            System.out.println("\nOrder Number \"" + orderToFinalize + "\" Was Successfully Finalized");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
// method mainly fo printing incomplete orders

    /***
     * Prints un-finalized orders and selects order to finalize
     * @param connection Connection to database
     * @return Returning order selected by user to finalize
     */
    public static int printIncompleteOrders(Connection connection) {
        boolean state, state2 = false;
        int orderNumber, restaurantNumber, qtyItem1, qtyItem2, qtyItem3, qtyItem4;
        int nextOperation = 0;
        String customerEmail, customerSpecial, userInput;
        String restaurantName;
        ArrayList<String> foodNames = new ArrayList<>();
// try catch for restult set and statement. SQL related queries
        try {
            //1st result set
            Statement statement = connection.createStatement();// new statement
            ResultSet results;// declare result set for select
            // creating a second result set
            // for each extra result set we need a new statement
            // this is not optimal but was needed here not to lose data
            Statement statement1 = connection.createStatement();// new statement
            ResultSet results1;// declare result set for select
            // I will explain the select only once and not again
            // selecting all columns from table with name Orders where condtion for column Finalized is null
            results = statement.executeQuery("SELECT * FROM Orders WHERE Finalized IS NULL");// results for init
            // this returns all rows with column Finalized is equal to null
            // as soon as Finalize is not null it means the order was finalized
            // all orders have 2 stages: stage 1 is creating order in db, stage 2 is finalizing order with
            // dat and time stamp
            // the while extracts row data from the current row and prints the order with most information
            while (results.next()) {
                state2 = true;
                // getting info from order row in DB
                orderNumber = results.getInt("OrderNr");
                restaurantNumber = results.getInt("RestaurantNr");
                qtyItem1 = results.getInt("QtyItem1");
                qtyItem2 = results.getInt("QtyItem2");
                qtyItem3 = results.getInt("QtyItem3");
                qtyItem4 = results.getInt("QtyItem4");
                customerEmail = results.getString("CustomerEmail");
                customerSpecial = results.getString("CustomerSpecial");

                //finding name of restaurant
                results1 = statement1.executeQuery("SELECT rName FROM Restaurant_Table WHERE Menu = " + restaurantNumber);
                results1.next();// setting cursor in first row
                restaurantName = results1.getString("rName"); // retrieving name of restaurant

                // now we need to get food names and indicate quantity
                results1 = statement1.executeQuery("SELECT * FROM Menus WHERE Menu = " + restaurantNumber);
                // we are storing the food name and price in the array list eg. [name, price, name, price, .........]
                // this will be used later
                while (results1.next()) {
                    foodNames.add(results1.getString("Name"));
                    foodNames.add(Integer.toString(results1.getInt("Price")));
                }
                // here we print out the current rows information
                // this information is all UNFINALIZED orders ONLY
                // this process is repeated for all rows that are not finalized
                System.out.print("Order Nr = " + orderNumber + " , " + restaurantName + " , ");
                System.out.print(qtyItem1 + " X " + foodNames.get(0) + " , " + qtyItem2 + " X " + foodNames.get(2) + " , ");
                System.out.print(qtyItem3 + " X " + foodNames.get(4) + " , " + qtyItem4 + " X " + foodNames.get(6) + " , ");
                System.out.print("Customer Email = " + customerEmail + " , ");
                System.out.print(customerSpecial + "\n");
                foodNames.clear();// we clear array list for next row
            }
            System.out.println("\n");
            // here the user select the order to finalize and then we check if the order is valid
            // the if statement is used as defensive programming if there are no orders to finalize
            if (state2) {
                // do while main purpose is if the user entered in something incorrect
                // if the details are correct the do while is skipped if the user entered a order that does not exist
                // the user has an opportunity to enter the correct order
                // it is the users responsability to enter the correct order number from the list
                // if we want to add extra precaution we can always let the user enter the email as conformation
                // this is however not programmed in here
                do {
                    state = false;
                    nextOperation = userIntInput("The above list of orders are NOT finalized,\nEnter order number to" +
                            "Finalize the order: ", 10000, 1);
                    results1 = statement1.executeQuery("SELECT OrderNr FROM Orders WHERE OrderNr = " + nextOperation);

                    if (results1.next()) {
                        break;
                    } else {
                        System.out.println("Order number not found, double check your entry");
                        userInput = userStringInput("Would you like to try again Y/N?").toLowerCase();
                        if (userInput.equals("y")) {
                            state = true;
                        } else {
                            nextOperation = 0;// we return 0 if the selection is canceled
                            System.out.println("---------------------Finalize aborted.---------------------");
                        }
                    }
                } while (state);
                // closing all result set and statments
                statement.close();
                results.close();
                statement1.close();
                results1.close();
            } else {
                nextOperation = -1;// we return -1 if there are no orders to finalize
                System.out.println("ALL Orders are Finalized, there are no orders to Finalize");
                // here we only close the main result set and statement
                // if we try to close the result1 here we will get exception since the result set was not initialized
                statement.close();
                results.close();
            }
//  in case something goes wrong
        } catch (Exception e) {
            System.out.println("Could not Finalize Order, Something went Wrong");
            e.printStackTrace();
        }

        return nextOperation; // returns the order number to finalize
    }

    // Method for adding a new order to the Database

    /***
     * Adds new order/delivery to database
     * @param connection Connection to database
     */
    public static void newDelivery(Connection connection) {
        int restaurantNr;
        String customerEmail, special, contactNrRestaurant;
        ArrayList<Integer> meals;

        //select customer from database. by using email of customer, the email is the primary key in the DB
        // thus one of a kind like a ID
        customerEmail = fetchCustomerEmail(connection);
        if (customerEmail == null) {
            return;
        }
        // we first get the restaurant the client is ordering from
        restaurantNr = selectRestaurant(connection);// the restaurant is selected by id
        // now we need to select the menu from the type of restaurant selected
        // then create a order list with the meals selected
        meals = selectMeal(connection, restaurantNr);
        // Contact Number of restaurant
        contactNrRestaurant = userStringInput("Enter Contact Number of Restaurant:");
        //Special instruction from customer
        special = userStringInput("Special instruction from Customer: ");
        // here we want to write information to table in DB
        writeOrderToDB(restaurantNr, customerEmail, meals, contactNrRestaurant, special, connection);
    }
// method used to write information on order to DB

    /***
     * Writing order/delivery to Database
     * @param restaurantNr Restaurant selected to order from
     * @param customerEmail Customer email of order/delivery
     * @param meals List of meals ordered
     * @param contactNrRestaurant Restaurant's Contact Number where meals were ordered
     * @param special Customer special instruction
     * @param connection Connection to database
     */
    public static void writeOrderToDB(Integer restaurantNr,
                                      String customerEmail,
                                      ArrayList<Integer> meals,
                                      String contactNrRestaurant,
                                      String special,
                                      Connection connection) {

        try {
            ResultSet results;// declare result set for select
            // using prepared statement to write calues to table
            // this autogenerates the new primary key on the orders table and this key is also the order Nr
            PreparedStatement pstmt = connection.prepareStatement("INSERT INTO Orders (RestaurantNr," +
                    " CustomerEmail," +
                    " QtyItem1," +
                    " QtyItem2," +
                    " QtyItem3," +
                    " QtyItem4," +
                    " RestaurantContactNr," +
                    " CustomerSpecial) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            // then we return that key we generate from database and use this key later

            pstmt.setInt(1, restaurantNr);
            pstmt.setString(2, customerEmail);
            pstmt.setInt(3, meals.get(0));
            pstmt.setInt(4, meals.get(1));
            pstmt.setInt(5, meals.get(2));
            pstmt.setInt(6, meals.get(3));
            pstmt.setString(7, contactNrRestaurant);
            pstmt.setString(8, special);
            pstmt.executeUpdate();// executes the changes to table
            results = pstmt.getGeneratedKeys();// here we select that returned key from DB

            results.next(); // move cursor to the one and only row to get the key
            String key = results.getString(1);

            pstmt.close();// closes the preparedStatement
            results.close();
            // giving order number back to user to indicate order was created
            System.out.println("Order added to database, Order number is " + key);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not add order to Database");
        }
    }
// this method is for user to select the meal information and is stored in an arraylist
    // passed in the restaurant the user selected, this is how we kow which menu in the database to use

    /***
     * Selection of meals
     * @param connection Connection to database
     * @param restaurantNr Restaurant the meals are being selected from
     * @return Returns meal list
     */
    public static ArrayList<Integer> selectMeal(Connection connection, Integer restaurantNr) {
        boolean state;
        int selection, quantity, temp, repeat;
        // creating new arraylist and initializing it
        ArrayList<Integer> meals = new ArrayList<>();
        meals.add(0, 0);
        meals.add(1, 0);
        meals.add(2, 0);
        meals.add(3, 0);

        try {
            Statement statement = connection.createStatement();// new statement

            do {
                // here we print out the meal information to select from, it is a method called
                //we pass in which restaurant was selected and also the statement from this method
                printAllFromTableMenues(statement, restaurantNr);
                // in our menu we have only 4 items so we know we have to select a number from 1 - 4 and is passed as limits
                // tells the user if a invalid entry was made
                // user input is a pre written method used almost everywhere
                selection = userIntInput("Select Meal from List Above: ", 4, 1);
                // here the user can enter the quantity of that selected meal
                quantity = userIntInput("Enter quantity of meal: ", 50, 1);
                // here we increment the meal quantity for in case the user suddenly wants to add one more of the same
                // meal this just adds the new quantity to the old one
                // this program only caters for the sum and not for when he wants to remove a certain amount
                temp = meals.get(selection - 1);
                temp += quantity;
                meals.set(selection - 1, temp);
                // if the user would like to add another meal they can do so by entering the value 1 and the loop will
                //  repeat as long as the user needs to add meals
                // if he is done the user will enter 0
                repeat = userIntInput("Enter \"1\" if you want to add another item else enter \"0\" : ", 1, 0);

                if (repeat == 1) {
                    state = true;
                } else {
                    state = false;
                }

            } while (state);

            statement.close();

            return meals;// we then return the arraylist with info

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Something went wrong...");
            return null;
        }
    }
// here we ask for the customer email and also make sure the customer does not already exist on the database

    /***
     * Fetches email from Database to use in new Order/delivery
     * @param connection Connection to database
     * @return Returns customer email to use in order
     */
    public static String fetchCustomerEmail(Connection connection) {
        String customerEmail, userInput;
        boolean state = false;

        try {
            Statement statement = connection.createStatement();// new statement
            ResultSet results;// declare result set for select

            do {
                // enter email of new customer
                customerEmail = userStringInput("\nEnter email of the Customer for delivery: ");
                // we then select a customer from that email entered
                results = statement.executeQuery("SELECT * FROM Customers_Table WHERE cEmail = '" + customerEmail + "'");
                // if the customer email is found the results.next will return true and we can break out of testing
                // we thus look whether the email is on the database or not
                // if not the user will cancel the order and create the customer first
                if (results.next()) {
                    break;
                } else {
                    System.out.println("Customer Not Found on Database, double check spelling of Email");
                    userInput = userStringInput("Would you like to search again Y/N?").toLowerCase();
                    if (userInput.equals("y")) {
                        state = true;
                    } else {
                        customerEmail = null;
                        System.out.println("---------------------Selection aborted. Order Canceled---------------------");
                    }
                }
            } while (state);

            statement.close();
            results.close();
            return customerEmail;// we then return customer email for use in new delivery

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Something went wrong...");
            return null;
        }
    }
// here we select the restaurant from the database. simple

    /***
     * Selects Restaurant to order from
     * @param connection Connection to database
     * @return Returns restaurant to order from
     */
    public static int selectRestaurant(Connection connection) {
        int restaurant;

        try {
            Statement statement = connection.createStatement();// new statement

            System.out.print("\n");
            printAllFromTableRestaurant(statement);// calling method to print all restaurants from database
            // we only have four so we give limits 1 - 4 for user input
            restaurant = userIntInput("Select restaurant by number above: ", 4, 1);
            System.out.print("\n");

            statement.close();

            return restaurant;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Something went wrong, could not select restaurant");
            return 0;
        }
    }
// method used to select type of operation that will follow, and returns the number

    /***
     * User selects operations from here
     * @return Returns the desired operation
     */
    public static int userMenu() {
        Scanner input = new Scanner(System.in);
        System.out.print("\n");
        System.out.print("""
                1 - New Delivery
                2 - Finalize Order
                3 - New Customer
                4 - Edit Customer
                5 - Remove Customer
                0 - Quit
                Choose operation by entering the corresponding Number: """);
        // using try catch to assure correct input is entered
        // if a string or char is entered we will use try catch to know it is incorrect
        // else we use a simple if else for range of numbers
        try {
            int number = Integer.parseInt(input.next()); // casting string to int
            if (number >= 0 && number <= 5) {// defensive programming for user errors
                return number;
            } else {
                System.out.println("Number is out of range, enter a number from the list.");
                return userMenu();
            }
        } catch (Exception e) { // if a string is entered we know it is not a number from the casting
            System.out.println("This is not a number!");
            return userMenu();
        }
    }

    // used to return string input from user

    /**
     * Prompts user the message and returns user input
     * @param message User String prompt
     * @return Returns user String input
     */
    public static String userStringInput(String message) {
        Scanner input = new Scanner(System.in);
        System.out.print(message);
        return input.nextLine();
    }

    // used to return int input from user with defensive programming
    //caters for wrong number entered as well as if strings are entered

    /**
     * Promts user for a Integer value
     * @param message User Prompt
     * @param max Maximum integer user can enter
     * @param min Minimum integer user can enter
     * @return Returns input
     */
    public static int userIntInput(String message, Integer max, Integer min) {
        Scanner input = new Scanner(System.in);
        try {
            System.out.print(message);
            int number = Integer.parseInt(input.next());
            if (number >= min && number <= max) {
                return number;
            } else {
                System.out.println("Number is out of range, enter a number from from " + min + " to " + max);
                return userIntInput(message, max, min);
            }
        } catch (Exception e) {
            System.out.println("This is not a number!");
            return userIntInput(message, max, min);
        }
    }

    // adding new customer to Database, we will first check if the email, which is the unique id of a customer
    // does not already exist to avoid problems

    /***
     * Adds customer to Database if not yet on database
     * @param connection Connection to database
     */
    public static void addCustomerToDB(Connection connection) {
        String email;
        int userInput;// state 0 is abort, state 1 is to re enter and state 2 is noting changed

        try {
            Statement statement = connection.createStatement();// new statement
            ResultSet results;// declare result set for select
            results = statement.executeQuery("SELECT cEmail FROM Customers_Table");// results for init

            do {
                userInput = 2;
                email = userStringInput("Enter Customer Email: ");
                while (results.next()) {
                    if (results.getString("cEmail").equals(email)) {
                        userInput = userIntInput("-----------The email already exists in Database-----------\n" +
                                "--if " + email + " is correct enter \"0\" to abort and select customer from database--\n" +
                                "------if this email is incorrect enter \"1\" to re enter email.------", 1, 0);
                    }
                }
            } while (userInput == 1);

            if (userInput != 0) {

                PreparedStatement pstmt = connection.prepareStatement("INSERT INTO Customers_Table VALUES (?, ?, ?, ?,?)");
                pstmt.setString(1, userStringInput("Enter Customer Name: "));
                pstmt.setString(2, userStringInput("Enter Customer Number: "));
                pstmt.setString(3, userStringInput("Enter Customer Address: "));
                pstmt.setString(4, userStringInput("Enter Customer City: "));
                pstmt.setString(5, email);
                pstmt.executeUpdate();// executes the changes to table
                pstmt.close();// closes the preparedStatement
            }

            results.close();
            statement.close();

        } catch (Exception e) {
            System.out.println("-------------------Could not add Customer, Something went wrong.------------------");
        }
    }
// here we can fix or edit all columns/info of the customer if something needs to change

    /***
     * Edit a customer that exists on Database
     * @param connection Connection to database
     * @return Return used as recursion for repeatability
     */
    public static Object editCustomerInDB(Connection connection) {
        String eMail, userInput;

        try {
            Statement statement = connection.createStatement();// new statement
            ResultSet results;// declare result set for select

            System.out.println("\n");
            printAllFromTableCustomers(statement);
            // Customer to be updated is selected by email because it is unique and used as primary key in DB
            eMail = userStringInput("\nEnter email of the Customer to update information of that Customer from list: ");
            // selecting row from DB using input form user
            results = statement.executeQuery("SELECT * FROM Customers_Table WHERE cEmail = '" + eMail + "'");// results for init
            // if email is found the if will execute otherwise else will execute
            // if updates all info of selected customer
            // while else tells user the customer was not found and gives user option to retry using recursion
            if (results.next()) {
                // using preparestatements to update selected toy by variables entered by user above
                PreparedStatement pstmt = connection.prepareStatement("UPDATE Customers_Table SET cName = ?," +
                        " cNumber = ?," +
                        " cAddress = ?," +
                        " cCity = ?, " +
                        " cEmail = ? " +
                        "WHERE cEmail = ? ");
                pstmt.setString(1, userStringInput("Update Customer Name: "));
                pstmt.setString(2, userStringInput("Update Customer Number: "));
                pstmt.setString(3, userStringInput("Update Customer Address: "));
                pstmt.setString(4, userStringInput("Update Customer City: "));
                pstmt.setString(5, userStringInput("Update Customer Email: "));
                pstmt.setString(6, eMail);
                pstmt.executeUpdate();
                pstmt.close();
                results.close();
                statement.close();

                System.out.println("---------------------------Customer Updated---------------------------");
                return null;
            } else {
                System.out.println("Customer Not Found on Database, double check spelling of Email");
                userInput = userStringInput("Would you like to search again Y/N? : ").toLowerCase();
                if (userInput.equals("y")) {
                    return editCustomerInDB(connection);
                } else {
                    System.out.println("---------------------------Search aborted.---------------------------");
                    return null;
                }
            }
        // email of customer is unique thus if the same customers email is added for 2 different names of customer
            // this is not allowed and this will give this error and is catered for it
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("The email already exists on the database, the email must be unique\n");
            userInput = userStringInput("Enter \"Y\" to retry or \"N\" to go to Main Menu").toLowerCase();
            if (userInput.equals("y")) {
                return editCustomerInDB(connection);
            } else {
                System.out.println("-----------------------Update Customer operation aborted-----------------------");
                return null;
            }

        }
    }
// here we simply remove a customer

    /***
     * Removes an existing customer
     * @param connection Connection to database
     * @return Return used as recursion for repeatability
     */
    public static Object removeCustomer(Connection connection) {
        String remove, retry;

        try {
            Statement statement = connection.createStatement();// new statement
            ResultSet results;

            System.out.println("\n");
            printAllFromTableCustomers(statement);
            remove = userStringInput("\nEnter email of the Customer to remove that Customer: ");
            results = statement.executeQuery("SELECT * FROM Customers_Table WHERE cEmail = '" + remove + "'");
            // basic if else to cater for if user entered a email that does not exist
            if (results.next()) {
                statement.executeUpdate("DELETE FROM Customers_Table WHERE cEmail = '" + remove + "'");
                System.out.println("Customer with email " + remove + " has been Removed");
                return null;
            } else {
                System.out.println("Could not Remove Customer from Database, " +
                        "the email you entered does not exist\n");
                retry = userStringInput("if you would like to retry, enter \"Y\" else enter \"N\"").toLowerCase();
                if (retry.equals("y")) {
                    return removeCustomer(connection);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
// here we print selected rows for customer

    /***
     * Prints all in Selected Table
     * @param statement Statement from main
     * @throws SQLException exception if something goes wrong
     */
    public static void printAllFromTableCustomers(Statement statement) throws SQLException {
        ResultSet results = statement.executeQuery("SELECT * FROM Customers_Table");
        while (results.next()) {
            System.out.println(
                    results.getString("cName") + ", "
                            + results.getString("cNumber") + ", "
                            + results.getString("cAddress") + ", "
                            + results.getString("cCity") + ", "
                            + results.getString("cEmail")
            );
        }
    }
    // here we print selected rows for restaurants

    /***
     * Prints all in Selected Table
     * @param statement Statement from main
     * @throws SQLException exception if something goes wrong
     */
    public static void printAllFromTableRestaurant(Statement statement) throws SQLException {
        ResultSet results = statement.executeQuery("SELECT * FROM Restaurant_Table");
        while (results.next()) {
            System.out.println(
                    results.getInt("Menu") + ", "
                            + results.getString("rName")
            );
        }
    }
    // here we print selected rows for Food from Menus

    /***
     * Prints all in Selected Table
     * @param statement Statement from main
     * @param Menu Prints all Selected meals from relating menu
     * @throws SQLException exception if something goes wrong
     */
    public static void printAllFromTableMenues(Statement statement, Integer Menu) throws SQLException {
        ResultSet results = statement.executeQuery("SELECT * FROM Menus WHERE Menu = " + Menu);
        while (results.next()) {
            System.out.println(
                    results.getInt("Number") + ", "
                            + results.getString("Name") + ", R"
                            + results.getInt("Price")
            );
        }
    }
}