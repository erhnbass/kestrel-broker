package com.erhan.kestrel_broker.controller;

import com.erhan.kestrel_broker.domain.entity.Asset;
import com.erhan.kestrel_broker.dto.response.AssetResponse;
import com.erhan.kestrel_broker.repository.AssetRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
@SecurityRequirement(name = "basicAuth")
public class AssetController {

    private final AssetRepository assetRepository;

    @GetMapping
    public List<AssetResponse> listAssets(@RequestParam(required = false) String customerId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String finalCustomerId = isAdmin ? customerId : username;

        if (finalCustomerId != null) {
            return assetRepository.findByCustomerId(finalCustomerId)
                    .stream()
                    .map(AssetResponse::from)
                    .toList();
        }

        return assetRepository.findAll()
                .stream()
                .map(AssetResponse::from)
                .toList();
    }
}
