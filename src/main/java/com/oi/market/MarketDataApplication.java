package com.oi.market;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MarketDataApplication {

    public static void main(String[] args) {
        // Load .env file BEFORE Spring initialization
        loadEnvironmentVariables();
        SpringApplication.run(MarketDataApplication.class, args);
    }

    /**
     * Load environment variables from .env file
     * This must happen before Spring tries to resolve property placeholders
     */
    private static void loadEnvironmentVariables() {
        try {
            // Try multiple locations for .env file
            String[] searchDirs = {
                ".",           // Current directory (when running from JAR)
                "..",          // Parent directory
                "../..",       // Grandparent directory
            };

            Dotenv dotenv = null;
            boolean found = false;
            
            for (String dir : searchDirs) {
                try {
                    Dotenv temp = Dotenv.configure()
                            .directory(dir)
                            .filename(".env")
                            .load();
                    
                    // Check if it actually found the file (has UPSTOX entries)
                    if (temp.get("UPSTOX_CLIENT_ID") != null) {
                        dotenv = temp;
                        System.out.println("✓ Loaded .env from: " + dir);
                        found = true;
                        break;
                    }
                } catch (Exception ignored) {
                    // Try next directory
                }
            }

            if (found && dotenv != null) {
                // Set environment variables as system properties
                dotenv.entries().forEach(entry -> {
                    System.setProperty(entry.getKey(), entry.getValue());
                    // Log variables (mask sensitive values)
                    if (entry.getKey().contains("SECRET") || entry.getKey().contains("PASSWORD")) {
                        System.out.println("  ✓ " + entry.getKey() + "=***");
                    } else if (entry.getKey().startsWith("UPSTOX")) {
                        System.out.println("  ✓ " + entry.getKey() + "=" + entry.getValue());
                    }
                });
                System.out.println("✓ Successfully loaded environment variables");
            } else {
                System.out.println("⚠ No .env file found, using system environment variables");
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to load environment: " + e.getMessage());
        }
    }
}

