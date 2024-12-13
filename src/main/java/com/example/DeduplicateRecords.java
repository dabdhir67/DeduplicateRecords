package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DeduplicateRecords {

    private static final Logger logger = LogManager.getLogger(DeduplicateRecords.class);

    public static void main(String[] args) throws IOException {
        //provide input file in command line check
        if (args.length != 1) {
            System.out.println("Please provide input file.");
            System.exit(1); // Exit with a non-zero status to indicate an error
        }

        String inputFile = args[0];  // First argument is the input file name

        // Read and parse JSON
        ObjectMapper mapper = new ObjectMapper();
        // Jackson doesn't support JAVA 8 Date/Time Type by default, so you need to provide this and register it to our mapper.
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


        try {

            LeadsData leadsData = mapper.readValue(new File(inputFile), LeadsData.class);

            List<Lead> deduplicatedLeads = deduplicate(leadsData.getLeads());

            LeadsData result = new LeadsData(deduplicatedLeads);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("deduplicated_leads.json"), result);

            System.out.println("Deduplication completed successfully. Output saved to: " + "deduplicated_leads.json");
            System.out.println("Logs of changes are available in the console and saved to: " + "app.logs");

        } catch (IOException e) {
            System.out.println("Error processing the files: " + e.getMessage());
        }

    }

    private static List<Lead> deduplicate(List<Lead> leads) {
        // Sort by entryDate desc order(newest first)
        leads.sort((a, b) -> {
            int dateComparison = b.getEntryDate().compareTo(a.getEntryDate());
            return dateComparison != 0 ? dateComparison : 1; // Tie-breaker: keep last in list
        });

        // Maps for tracking unique IDs and Emails
        Map<String, Lead> idMap = new HashMap<>();
        Map<String, Lead> emailMap = new HashMap<>();
        List<Lead> deduplicated = new ArrayList<>();

        for (Lead lead : leads) {
            Lead conflictingId = idMap.get(lead.getId());
            Lead conflictingEmail = emailMap.get(lead.getEmail());

            if (conflictingId == null && conflictingEmail == null) {
                // No conflict, add to results
                idMap.put(lead.getId(), lead);
                emailMap.put(lead.getEmail(), lead);
                deduplicated.add(lead);
            } else {

                if (conflictingId != null && conflictingId.getEntryDate().equals(lead.getEntryDate())) {
                    // Same entryDate, prefer the last occurrence (current one)
                    deduplicated.remove(conflictingId);
                    deduplicated.add(lead);
                } else if (conflictingEmail != null && conflictingEmail.getEntryDate().equals(lead.getEntryDate())) {
                    // Same entryDate, prefer the last occurrence (current one)
                    deduplicated.remove(conflictingEmail);
                    deduplicated.add(lead);
                }

                // Log conflict resolution
                resolveConflict(lead, conflictingId != null ? conflictingId : conflictingEmail);
            }
        }

        return deduplicated;
    }

    private static void resolveConflict(Lead newLead, Lead existingLead) {
        logger.warn("Conflict detected between records:");
        logger.warn("New Record: {}", newLead);
        logger.warn("Existing Record: {}", existingLead);

        logger.warn("Potential Field Changes:");
        if (!Objects.equals(newLead.getId(), existingLead.getId())) {
            logger.warn("ID Change: {} -> {}", existingLead.getId(), newLead.getId());
        }
        if (!Objects.equals(newLead.getEmail(), existingLead.getEmail())) {
            logger.warn("Email Change: {} -> {}", existingLead.getEmail(), newLead.getEmail());
        }
        if (!Objects.equals(newLead.getFirstName(), existingLead.getFirstName())) {
            logger.warn("First Name Change: {} -> {}", existingLead.getFirstName(), newLead.getFirstName());
        }
        if (!Objects.equals(newLead.getLastName(), existingLead.getLastName())) {
            logger.warn("Last Name Change: {} -> {}", existingLead.getLastName(), newLead.getLastName());
        }
        if (!Objects.equals(newLead.getAddress(), existingLead.getAddress())) {
            logger.warn("Address Change: {} -> {}", existingLead.getAddress(), newLead.getAddress());
        }
        if (!Objects.equals(newLead.getEntryDate(), existingLead.getEntryDate())) {
            logger.warn("Entry Date Change: {} -> {}", existingLead.getEntryDate(), newLead.getEntryDate());
        }
        logger.info("Chosen Record: {}", existingLead);
        logger.warn("\n--- Next Record ---\n");
    }

}

// Models
class LeadsData {
    @JsonProperty("leads")
    private List<Lead> leads;

    public LeadsData() {}

    public LeadsData(List<Lead> leads) {
        this.leads = leads;
    }

    public List<Lead> getLeads() {
        return leads;
    }

    public void setLeads(List<Lead> leads) {
        this.leads = leads;
    }
}

@JsonPropertyOrder({ "_id", "email", "firstName", "lastName", "address", "entryDate" })
class Lead {
    @JsonProperty("_id")
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String address;
    private LocalDateTime entryDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = LocalDateTime.parse(entryDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public String toString() {
        return String.format("{_id: %s, email: %s, entryDate: %s}", id, email, entryDate);
    }


}



