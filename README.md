# Adobe Programming Challenge Solution
## This repository contains my solution for Adobe's Programming Challenge, which involves deduplicating a set of JSON records based on specific rules.

## Building the Project:
Prerequisites: Java 8 (or higher) + Maven
- Open terminal in the root directory of the project and run the following command:
- **mvn clean install**
## Running the Program:
- Once project is built, execute the program by providing the JSON file using the following command:
- **mvn exec:java -Dexec.args="path_to_your_file/leads.json"**
- Replace "path_to_your_file/leads.json" with the path to your JSON file containing the records.
- The program will process the file, de-duplicate the records, and output the result.
