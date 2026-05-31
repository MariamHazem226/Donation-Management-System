package com.givinghands.givinghands.controller;

import com.givinghands.givinghands.dto.CampaignMapper;
import com.givinghands.givinghands.entity.Campaign;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.entity.Volunteer;
import com.givinghands.givinghands.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // GET /api/admin/statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // GET /api/admin/users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // GET /api/admin/campaigns
    @GetMapping("/campaigns")
    public ResponseEntity<List<Campaign>> getAllCampaigns() {
        return ResponseEntity.ok(adminService.getAllCampaigns());
    }

    // GET /api/admin/volunteers
    @GetMapping("/volunteers")
    public ResponseEntity<List<Volunteer>> getAllVolunteers() {
        return ResponseEntity.ok(adminService.getAllVolunteers());
    }

    // GET /api/admin/volunteers/dashboard (used by admin dashboard tables)
    @GetMapping("/volunteers/dashboard")
    public ResponseEntity<java.util.List<com.givinghands.givinghands.dto.AdminVolunteerDTO>> getAllVolunteersForDashboard() {
        return ResponseEntity.ok(adminService.getAllVolunteersForDashboard());
    }



    // GET /api/admin/donations (used by admin dashboard tables)
    @GetMapping("/donations")
    public ResponseEntity<java.util.List<com.givinghands.givinghands.dto.AdminDonationDTO>> getAllDonations() {
        return ResponseEntity.ok(adminService.getAllDonations());
    }




    // DELETE /api/admin/users/{id}
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/admin/campaigns/{id}
    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id) {
        try {
            adminService.deleteCampaign(id);
            return ResponseEntity.ok(Map.of("message", "Campaign deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/admin/volunteers/{id}
    @DeleteMapping("/volunteers/{id}")
    public ResponseEntity<?> deleteVolunteer(@PathVariable Long id) {
        try {
            adminService.deleteVolunteer(id);
            return ResponseEntity.ok(Map.of("message", "Volunteer deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    // PUT /api/admin/campaigns/{id}/approve
    @PutMapping("/campaigns/{id}/approve")
    public ResponseEntity<?> approveCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = adminService.approveCampaign(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Campaign approved",
                    "campaign", CampaignMapper.toDto(campaign)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/admin/campaigns/{id}/reject
    @PutMapping("/campaigns/{id}/reject")
    public ResponseEntity<?> rejectCampaign(@PathVariable Long id) {
        try {
            Campaign campaign = adminService.rejectCampaign(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Campaign rejected",
                    "campaign", CampaignMapper.toDto(campaign)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
