package com.givinghands.givinghands.controller;

import com.givinghands.givinghands.dto.DonationDTO;
import com.givinghands.givinghands.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    @Autowired
    private DonationService donationService;


    // POST /api/donations
    @PostMapping
    public ResponseEntity<DonationDTO> submitDonation(@RequestBody DonationDTO donationDTO) {
        return ResponseEntity.ok(donationService.submitDonation(donationDTO));
    }

    // GET /api/donations/user/{id}
    @GetMapping("/user/{id}")
    public ResponseEntity<List<DonationDTO>> getDonationHistory(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.getDonationHistory(id));
    }
}
