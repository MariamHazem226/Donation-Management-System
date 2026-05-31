package com.givinghands.givinghands.controller;

import com.givinghands.givinghands.dto.CampaignDTO;
import com.givinghands.givinghands.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@CrossOrigin(origins = "*")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    // ─── GET ALL CAMPAIGNS FOR AN ORGANIZATION ────────────────────────────────
    // GET /api/organizations/{id}/campaigns
    @GetMapping("/{id}/campaigns")
    public ResponseEntity<List<CampaignDTO>> getCampaignsByOrganization(@PathVariable Long id) {
        List<CampaignDTO> campaigns = organizationService.getCampaignsByOrganization(id);
        return ResponseEntity.ok(campaigns);
    }

    // ─── GET VOLUNTEERS FOR ORGANIZATION CAMPAIGNS ────────────────────────────
    // GET /api/organizations/{id}/volunteers
    @GetMapping("/{id}/volunteers")
    public ResponseEntity<List<CampaignDTO>> getVolunteersForOrganizationCampaigns(@PathVariable Long id) {
        List<CampaignDTO> campaigns = organizationService.getVolunteersForOrganizationCampaigns(id);
        return ResponseEntity.ok(campaigns);
    }

    // ─── TRACK CAMPAIGN PROGRESS ──────────────────────────────────────────────
    // GET /api/organizations/campaigns/{campaignId}/progress
    @GetMapping("/campaigns/{campaignId}/progress")
    public ResponseEntity<CampaignDTO> trackCampaignProgress(@PathVariable Long campaignId) {
        CampaignDTO campaign = organizationService.trackCampaignProgress(campaignId);
        return ResponseEntity.ok(campaign);
    }
}
