package com.givinghands.givinghands.controller;

import com.givinghands.givinghands.dto.CampaignDTO;
import com.givinghands.givinghands.entity.Campaign;

import com.givinghands.givinghands.service.CampaignService;
import com.givinghands.givinghands.util.FileStorageUtil;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    private final Path uploadsDir = Paths.get("uploads", "campaigns");


    // ─── GET ALL CAMPAIGNS ────────────────────────────────────────────────────
    // GET /api/campaigns
    // GET /api/campaigns?category=education&status=active  (optional filter)
    @GetMapping
    public ResponseEntity<List<CampaignDTO>> getAllCampaigns(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {

        List<CampaignDTO> campaigns;

        if (category != null || status != null) {
            campaigns = campaignService.filterCampaigns(category, status);
        } else {
            campaigns = campaignService.getAllCampaigns();
        }

        return ResponseEntity.ok(campaigns);
    }

    // GET /api/campaigns/active
    @GetMapping("/active")
    public ResponseEntity<List<CampaignDTO>> getActiveCampaigns() {
        return ResponseEntity.ok(campaignService.getCampaignsByStatus("APPROVED"));
    }


    // GET /api/campaigns/pending
    @GetMapping("/pending")
    public ResponseEntity<List<CampaignDTO>> getPendingCampaigns() {
        return ResponseEntity.ok(campaignService.getCampaignsByStatus("PENDING"));
    }

    // GET /api/campaigns/approved
    @GetMapping("/approved")
    public ResponseEntity<List<CampaignDTO>> getApprovedCampaigns() {
        return ResponseEntity.ok(campaignService.getCampaignsByStatus("APPROVED"));
    }




    // ─── GET CAMPAIGN BY ID ───────────────────────────────────────────────────
    // GET /api/campaigns/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CampaignDTO> getCampaignById(@PathVariable Long id) {
        CampaignDTO campaign = campaignService.getCampaignById(id);
        return ResponseEntity.ok(campaign);
    }

    // ─── CREATE CAMPAIGN ──────────────────────────────────────────────────────
    // POST /api/campaigns
    @PostMapping
    public ResponseEntity<?> createCampaign(@RequestBody CampaignDTO campaignDTO) {
        try {
            CampaignDTO created = campaignService.createCampaign(campaignDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ─── UPDATE CAMPAIGN ──────────────────────────────────────────────────────
    // PUT /api/campaigns/{id}
    @PutMapping("/{id}")
    public ResponseEntity<CampaignDTO> updateCampaign(
            @PathVariable Long id,
            @RequestBody CampaignDTO campaignDTO) {
        CampaignDTO updated = campaignService.updateCampaign(id, campaignDTO);
        return ResponseEntity.ok(updated);
    }

    // ─── DELETE CAMPAIGN ──────────────────────────────────────────────────────
    // DELETE /api/campaigns/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/campaigns/{campaignId}/image
    @PostMapping(value = "{campaignId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCampaignImage(
            @PathVariable Long campaignId,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "file is required"));
        }

        // Ensure campaign exists
        CampaignDTO campaign = campaignService.getCampaignById(campaignId);
        if (campaign == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Campaign not found"));
        }

        try {
            String storedFilename = FileStorageUtil.storeMultipartFile(uploadsDir, file.getOriginalFilename(), file);
            String imagePath = "/api/campaigns/" + campaignId + "/image/file/" + storedFilename;

            // Persist imagePath in DB by updating campaign entity through service layer
            // Quick approach: call update with DTO (service doesn't yet support imagePath), so we do repository-like update here.
            // Since we don't have a repository injected in controller, we update by calling getCampaignById and using updateCampaign.
            // Persist imagePath by updating campaign via DTO
            campaign.setImage(imagePath);
            CampaignDTO updateDto = new CampaignDTO();
            updateDto.setTitle(campaign.getTitle());
            updateDto.setDescription(campaign.getDescription());
            updateDto.setCategory(campaign.getCategory());
            updateDto.setGoalAmount(campaign.getGoalAmount());
            updateDto.setCurrentAmount(campaign.getCurrentAmount());
            updateDto.setDeadline(campaign.getDeadline());
            updateDto.setStatus(campaign.getStatus());
            updateDto.setOrganizationId(campaign.getOrganizationId());
            updateDto.setImage(campaign.getImage());
            campaignService.updateCampaign(campaignId, updateDto);


            return ResponseEntity.ok(java.util.Map.of("image", imagePath));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", "Failed to store image"));
        }
    }

    // GET /api/campaigns/{campaignId}/image/file/{filename}
    @GetMapping(value = "{campaignId}/image/file/{filename}")
    public ResponseEntity<byte[]> getCampaignImageFile(
            @PathVariable Long campaignId,
            @PathVariable String filename) {

        // Ensure campaign exists (and is approved is handled by list endpoint; for direct access we just check existence)
        campaignService.getCampaignById(campaignId);

        try {
            Path filePath = uploadsDir.resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] bytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (!StringUtils.hasText(contentType)) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

