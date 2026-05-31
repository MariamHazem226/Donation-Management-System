package com.givinghands.givinghands.config;

import java.time.LocalDate;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.Donation;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.entity.Volunteer;
import com.givinghands.givinghands.repository.CampaignRepository;
import com.givinghands.givinghands.repository.DonationRepository;
import com.givinghands.givinghands.repository.UserRepository;
import com.givinghands.givinghands.repository.VolunteerRepository;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final VolunteerRepository volunteerRepository;
    private final DonationRepository donationRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SchemaHealthChecker schemaHealthChecker;

    public DataSeeder(UserRepository userRepository,
                      CampaignRepository campaignRepository,
                      VolunteerRepository volunteerRepository,
                      DonationRepository donationRepository,
                      BCryptPasswordEncoder passwordEncoder,
                      SchemaHealthChecker schemaHealthChecker) {
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.volunteerRepository = volunteerRepository;
        this.donationRepository = donationRepository;
        this.passwordEncoder = passwordEncoder;
        this.schemaHealthChecker = schemaHealthChecker;
    }

    @Override
    public void run(String... args) {
        SchemaHealthChecker.HealthReport schema = schemaHealthChecker.check();
        if (!schema.readyForSeeding()) {
            log.error("[seeder] ABORTED — database schema is not valid for seeding");
            schemaHealthChecker.logReport(schema);
            return;
        }

        log.info("[seeder] START — schema valid, running idempotent demo data seed");
        SeedingStats stats = new SeedingStats();

        try {
            seedDemoData(stats);
            log.info("[seeder] COMPLETE — users: {} inserted, {} verified | campaigns: {} inserted, {} verified | "
                            + "donations: {} inserted, {} skipped | volunteers: {} inserted, {} skipped",
                    stats.usersInserted, stats.usersVerified,
                    stats.campaignsInserted, stats.campaignsVerified,
                    stats.donationsInserted, stats.donationsSkipped,
                    stats.volunteersInserted, stats.volunteersSkipped);
            log.info("[seeder] TOTALS in database — users: {}, campaigns: {}, donations: {}, volunteers: {}",
                    userRepository.count(), campaignRepository.count(),
                    donationRepository.count(), volunteerRepository.count());
        } catch (Exception e) {
            log.error("[seeder] FAILED — seeding aborted: {}", e.getMessage(), e);
        }
    }

    private void seedDemoData(SeedingStats stats) {
        ensureUser("admin@givinghands.com", "Sarah Admin", "ADMIN", "admin123", stats);
        ensureUser("mariamhazem226@gmail.com", "Mariam Admin", "ADMIN", "mariam123", stats);
        User org1 = ensureUser("hope@org.com", "Hope Foundation", "ORGANIZATION", "org123", stats);
        User org2 = ensureUser("green@org.com", "Green Earth Org", "ORGANIZATION", "org123", stats);
        User user1 = ensureUser("sara@gmail.com", "Sara Ahmed", "USER", "user123", stats);
        User user2 = ensureUser("mohamed@gmail.com", "Mohamed Ali", "USER", "user123", stats);
        User user3 = ensureUser("nour@gmail.com", "Nour Hassan", "USER", "user123", stats);

        Campaign c1 = ensureCampaign("Help Gaza Children", stats, c -> {
            c.setDescription("Providing food, medicine, and education to children affected by conflict in Gaza.");
            c.setCategory("Emergency");
            c.setGoalAmount(50000.0);
            c.setCurrentAmount(32000.0);
            c.setDeadline(LocalDate.of(2026, 12, 31));
            c.setStatus("APPROVED");
            c.setAllowVolunteers(true);
            c.setCreator(org1);
            c.setOrganizationId(org1.getId());
        });

        Campaign c2 = ensureCampaign("Clean Water for Villages", stats, c -> {
            c.setDescription("Building water wells in rural areas to provide clean drinking water.");
            c.setCategory("Health");
            c.setGoalAmount(20000.0);
            c.setCurrentAmount(8500.0);
            c.setDeadline(LocalDate.of(2026, 10, 15));
            c.setStatus("APPROVED");
            c.setAllowVolunteers(true);
            c.setCreator(org1);
            c.setOrganizationId(org1.getId());
        });

        ensureCampaign("Education for All", stats, c -> {
            c.setDescription("Providing school supplies and scholarships for underprivileged children.");
            c.setCategory("Education");
            c.setGoalAmount(15000.0);
            c.setCurrentAmount(0.0);
            c.setDeadline(LocalDate.of(2026, 9, 1));
            c.setStatus("PENDING");
            c.setAllowVolunteers(false);
            c.setCreator(org2);
            c.setOrganizationId(org2.getId());
        });

        Campaign c4 = ensureCampaign("Plant 10,000 Trees", stats, c -> {
            c.setDescription("Reforestation project to fight climate change and restore natural habitats.");
            c.setCategory("Environment");
            c.setGoalAmount(30000.0);
            c.setCurrentAmount(12000.0);
            c.setDeadline(LocalDate.of(2026, 11, 30));
            c.setStatus("APPROVED");
            c.setAllowVolunteers(true);
            c.setCreator(org2);
            c.setOrganizationId(org2.getId());
        });

        Campaign c5 = ensureCampaign("Medical Aid for Refugees", stats, c -> {
            c.setDescription("Deploying mobile clinics and medical supplies to refugee camps.");
            c.setCategory("Health");
            c.setGoalAmount(40000.0);
            c.setCurrentAmount(25000.0);
            c.setDeadline(LocalDate.of(2026, 8, 31));
            c.setStatus("APPROVED");
            c.setAllowVolunteers(true);
            c.setCreator(org1);
            c.setOrganizationId(org1.getId());
        });

        ensureDonation(user1.getId(), c1.getId(), 500.0, LocalDate.now().minusDays(5), "COMPLETED", stats);
        ensureDonation(user1.getId(), c4.getId(), 250.0, LocalDate.now().minusDays(2), "COMPLETED", stats);
        ensureDonation(user2.getId(), c2.getId(), 1000.0, LocalDate.now().minusDays(10), "COMPLETED", stats);
        ensureDonation(user2.getId(), c5.getId(), 750.0, LocalDate.now().minusDays(1), "COMPLETED", stats);
        ensureDonation(user3.getId(), c1.getId(), 200.0, LocalDate.now(), "COMPLETED", stats);

        ensureVolunteer(user1, c1, "APPROVED", LocalDate.now().minusDays(7),
                "I want to help children in need", "Teaching, First Aid", "Weekends", "2 years NGO work", stats);
        ensureVolunteer(user2, c2, "PENDING", LocalDate.now().minusDays(3),
                "Clean water is a basic right", "Engineering, Construction", "Flexible", "Civil engineer with 5 years experience", stats);
        ensureVolunteer(user3, c4, "APPROVED", LocalDate.now().minusDays(1),
                "Environmental activist", "Gardening, Team coordination", "Weekdays", "3 years environmental volunteer", stats);
    }

    private User ensureUser(String email, String name, String role, String plainPassword, SeedingStats stats) {
        String normalizedEmail = email.trim().toLowerCase();
        boolean isNew = userRepository.findByEmail(normalizedEmail).isEmpty();

        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User created = new User();
            created.setEmail(normalizedEmail);
            created.setName(name);
            created.setRole(role);
            created.setPassword(passwordEncoder.encode(plainPassword));
            return userRepository.save(created);
        });

        if (isNew) {
            stats.usersInserted++;
            log.info("[seeder] User INSERTED: {}", normalizedEmail);
            return user;
        }

        boolean changed = false;
        if (name != null && !name.isBlank() && (user.getName() == null || user.getName().isBlank())) {
            user.setName(name);
            changed = true;
        }
        if (role != null && (user.getRole() == null || !role.equalsIgnoreCase(user.getRole().trim()))) {
            user.setRole(role);
            changed = true;
        }
        user.setPassword(passwordEncoder.encode(plainPassword));
        changed = true;
        if (changed) {
            userRepository.save(user);
        }
        stats.usersVerified++;
        return user;
    }

    private Campaign ensureCampaign(String title, SeedingStats stats, Consumer<Campaign> builder) {
        boolean isNew = campaignRepository.findByTitleIgnoreCase(title).isEmpty();
        Campaign campaign = campaignRepository.findByTitleIgnoreCase(title)
                .orElseGet(() -> {
                    Campaign c = new Campaign();
                    c.setTitle(title);
                    builder.accept(c);
                    return campaignRepository.save(c);
                });

        if (isNew) {
            stats.campaignsInserted++;
            log.info("[seeder] Campaign INSERTED: {}", title);
        } else {
            stats.campaignsVerified++;
        }
        return campaign;
    }

    private void ensureDonation(Long userId, Long campaignId, double amount, LocalDate date, String status,
                                SeedingStats stats) {
        if (donationRepository.existsByUserIdAndCampaignIdAndAmount(userId, campaignId, amount)) {
            stats.donationsSkipped++;
            return;
        }
        Donation donation = new Donation();
        donation.setUserId(userId);
        donation.setCampaignId(campaignId);
        donation.setAmount(amount);
        donation.setDate(date);
        donation.setStatus(status);
        donationRepository.save(donation);
        stats.donationsInserted++;
        log.info("[seeder] Donation INSERTED: user {} -> campaign {} (${})", userId, campaignId, amount);
    }

    private void ensureVolunteer(User user, Campaign campaign, String status, LocalDate appliedDate,
                                 String whyJoin, String skills, String availability, String experience,
                                 SeedingStats stats) {
        if (volunteerRepository.existsByUser_IdAndCampaign_Id(user.getId(), campaign.getId())) {
            stats.volunteersSkipped++;
            return;
        }
        Volunteer volunteer = new Volunteer();
        volunteer.setUser(user);
        volunteer.setCampaign(campaign);
        volunteer.setStatus(status);
        volunteer.setAppliedDate(appliedDate);
        volunteer.setWhyJoin(whyJoin);
        volunteer.setSkills(skills);
        volunteer.setAvailability(availability);
        volunteer.setExperience(experience);
        volunteerRepository.save(volunteer);
        stats.volunteersInserted++;
        log.info("[seeder] Volunteer INSERTED: {} -> {}", user.getEmail(), campaign.getTitle());
    }

    private static final class SeedingStats {
        int usersInserted;
        int usersVerified;
        int campaignsInserted;
        int campaignsVerified;
        int donationsInserted;
        int donationsSkipped;
        int volunteersInserted;
        int volunteersSkipped;
    }
}
