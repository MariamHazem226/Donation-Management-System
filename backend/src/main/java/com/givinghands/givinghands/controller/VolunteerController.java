package com.givinghands.givinghands.controller;

import com.givinghands.givinghands.dto.VolunteerDTO;
import com.givinghands.givinghands.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    @Autowired
    private VolunteerService volunteerService;


    // POST /api/volunteers/apply
    @PostMapping("/apply")
    public ResponseEntity<?> applyVolunteer(@RequestBody VolunteerDTO volunteerDTO) {
        try {
            if (volunteerDTO.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User id is required"));
            }
            if (volunteerDTO.getCampaignId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Campaign id is required"));
            }
            if (volunteerDTO.getWhyJoin() == null || volunteerDTO.getWhyJoin().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please provide a reason for volunteering"));
            }
            return ResponseEntity.ok(volunteerService.applyVolunteer(volunteerDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/volunteers?campaignId=1
    @GetMapping
    public ResponseEntity<List<VolunteerDTO>> getVolunteersByCampaign(@RequestParam Long campaignId) {
        return ResponseEntity.ok(volunteerService.getVolunteersByCampaign(campaignId));
    }

    // GET /api/volunteers/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VolunteerDTO>> getVolunteersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(volunteerService.getVolunteersByUser(userId));
    }


    // PUT /api/volunteers/{id}/approve
    @PutMapping("/{id}/approve")
    public ResponseEntity<VolunteerDTO> approveVolunteer(@PathVariable Long id) {
        return ResponseEntity.ok(volunteerService.approveVolunteer(id));
    }

    // PUT /api/volunteers/{id}/reject
    @PutMapping("/{id}/reject")
    public ResponseEntity<VolunteerDTO> rejectVolunteer(@PathVariable Long id) {
        return ResponseEntity.ok(volunteerService.rejectVolunteer(id));
    }

    // GET /api/volunteers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<VolunteerDTO> getVolunteerById(@PathVariable Long id) {
        return ResponseEntity.ok(volunteerService.getVolunteerById(id));
    }

    // DELETE /api/volunteers/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVolunteer(@PathVariable Long id) {
        volunteerService.deleteVolunteer(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Volunteer deleted successfully"));
    }
}
