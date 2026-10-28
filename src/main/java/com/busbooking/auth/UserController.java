package com.busbooking.auth;

import com.busbooking.auth.dto.UpdateProfileRequest;
import com.busbooking.auth.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getCurrentProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = authService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
}
