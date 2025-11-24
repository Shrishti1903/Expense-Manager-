# Expense-Manager-
# project-for-software-engineering


Software Requirements Specification (SRS)

Users should be able to add and manage expenses

System should store expense details in memory (or file, if implemented)

Application should calculate total expenses dynamically

Interface must be intuitive and easy to use



Features:

Add new expenses with name, category, amount, date, and notes

Import expenses from CSV file

View all expenses in a structured table format

Automatic calculation of total expenses

Category-based expense summary (optional if implemented)

Editable and user-friendly Java Swing interface

Input validation & error handling



Design

Model–View–Controller (MVC) style architecture

Model: Expense.java

View: ExpenseManagerGUI.java

Controller: ExpenseManager.java & TableModel

Test Cases:
Test Case	                 Input	                    Expected Output
Add valid expense         Enter data & save	          Appears in table
Add empty fields        	Blank input	Show            validation alert
Calculate total	            Table filled	            Correct sum shown


![WhatsApp Image 2025-11-24 at 22 17 13_47ee0ea2](https://github.com/user-attachments/assets/1b797311-9377-43b8-917a-1c8ed3ca0fb5)

![WhatsApp Image 2025-11-24 at 22 17 13_f585afe8](https://github.com/user-attachments/assets/b3664790-f70d-4a01-801b-c15984f355a3)

![WhatsApp Image 2025-11-24 at 22 17 13_96dc19fc](https://github.com/user-attachments/assets/c22df221-c7ec-49a3-83c1-221790f8709d)

![WhatsApp Image 2025-11-24 at 22 17 14_de6d62c9](https://github.com/user-attachments/assets/e8de3231-57db-41ad-9f05-9a29332293b9)


Author

Shrishti Pathak
2301010052
K.R Mangalam University
