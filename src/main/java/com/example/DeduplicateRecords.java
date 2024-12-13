package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DeduplicateRecords {
    public static void main(String[] args) throws IOException {
        // Read and parse JSON
        ObjectMapper mapper = new ObjectMapper();
        // Jackson doesn't support JAVA 8 Date/Time Type by default, so you need to provide this and register it to our mapper.
        mapper.registerModule(new JavaTimeModule());  // Register the module
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Optional: to write dates as ISO-8601 strings instead of as integer arrays.


        LeadsData leadsData = mapper.readValue(new File("leads.json"), LeadsData.class);

        // Deduplicate records
        List<Lead> deduplicatedLeads = deduplicate(leadsData.getLeads());

        // Write deduplicated leads to a new file
        LeadsData result = new LeadsData(deduplicatedLeads);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("deduplicated_leads.json"), result);


    }

    private static List<Lead> deduplicate(List<Lead> leads) {
        // Sort by entryDate (newest first) and awthen by order in the list
        leads.sort((a, b) -> {
            int dateComparison = b.getEntryDate().compareTo(a.getEntryDate());
            return dateComparison != 0 ? dateComparison : 1; // Tie-breaker: keep last in list
        });

        // Maps for tracking unique IDs and Emails
        Map<String, Lead> idMap = new HashMap<>();
        Map<String, Lead> emailMap = new HashMap<>();
        List<Lead> deduplicated = new ArrayList<>();
        List<String> log = new ArrayList<>();

        for (Lead lead : leads) {
            Lead conflictingId = idMap.get(lead.getId());
            Lead conflictingEmail = emailMap.get(lead.getEmail());

            if (conflictingId == null && conflictingEmail == null) {
                // No conflict, add to results
                idMap.put(lead.getId(), lead);
                emailMap.put(lead.getEmail(), lead);
                deduplicated.add(lead);
            } else {
                // Log conflict resolution
                Lead chosen = resolveConflict(lead, conflictingId != null ? conflictingId : conflictingEmail, log);
            }
        }

        // Print log to console
        log.forEach(System.out::println);

        // Print logs to file
        String logFilePath = "conflict_logs.txt";
        try {
            logConflictsToFile(log, logFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return deduplicated;
    }

    private static Lead resolveConflict(Lead newLead, Lead existingLead, List<String> log) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("Conflict detected:\n");

        // Log the original (existing) and the new record
        logEntry.append("New Record: ").append(newLead).append("\n");
        logEntry.append("Existing Record: ").append(existingLead).append("\n");

        // Compare field by field and log changes
        logEntry.append("Potential Field Changes:\n");
        logChanges(newLead, existingLead, logEntry);  // Field-level changes are logged here

        // You can log the preferred record here (newLead, based on your logic)
        logEntry.append("Chosen Record: ").append(existingLead).append("\n");

        // Add log entry to the log list
        log.add(logEntry.toString());  // Adds the full log entry (including field-level changes)

        return existingLead; // Always choose the new record based on sorting
    }

    // Helper method to log field-level changes
    private static void logChanges(Lead newLead, Lead existingLead, StringBuilder logEntry) {
        // Compare fields and log changes
        if (!newLead.getId().equals(existingLead.getId())) {
            logEntry.append("ID change: ").append(existingLead.getId()).append(" -> ").append(newLead.getId()).append("\n");
        }
        if (!newLead.getEmail().equals(existingLead.getEmail())) {
            logEntry.append("Email change: ").append(existingLead.getEmail()).append(" -> ").append(newLead.getEmail()).append("\n");
        }
        if (!newLead.getFirstName().equals(existingLead.getFirstName())) {
            logEntry.append("First Name change: ").append(existingLead.getFirstName()).append(" -> ").append(newLead.getFirstName()).append("\n");
        }
        if (!newLead.getLastName().equals(existingLead.getLastName())) {
            logEntry.append("Last Name change: ").append(existingLead.getLastName()).append(" -> ").append(newLead.getLastName()).append("\n");
        }
        if (!newLead.getAddress().equals(existingLead.getAddress())) {
            logEntry.append("Address change: ").append(existingLead.getAddress()).append(" -> ").append(newLead.getAddress()).append("\n");
        }
        if (!newLead.getEntryDate().equals(existingLead.getEntryDate())) {
            logEntry.append("Entry Date change: ").append(existingLead.getEntryDate()).append(" -> ").append(newLead.getEntryDate()).append("\n");
        }
    }

    private static void logConflictsToFile(List<String> log, String logFilePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath))) {
            for (String logEntry : log) {
                writer.write(logEntry);
                writer.newLine();
            }
        }
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
