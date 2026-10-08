package com.example.realestate_backend.controller;

import com.example.realestate_backend.entity.Property;
import com.example.realestate_backend.entity.PropertyType;
import com.example.realestate_backend.entity.User;
import com.example.realestate_backend.service.AdminService;
import com.example.realestate_backend.service.CloudinaryService;
import com.example.realestate_backend.service.PropertyService;
import com.example.realestate_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.security.core.Authentication;
import com.example.realestate_backend.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@Controller
public class WebController {
    private final PropertyService propertyService;
    private final AdminService adminService;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    public WebController(PropertyService propertyService,
                         AdminService adminService,
                         UserService userService,
                         CloudinaryService cloudinaryService) {
        this.propertyService = propertyService;
        this.adminService = adminService;
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
    }

    @ModelAttribute
    public void navigation(Authentication authentication, Model model) {
        model.addAttribute("authenticated", authentication != null);
        model.addAttribute("admin", authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @InitBinder("user")
    public void bindRegistration(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "password");
    }

    @InitBinder("newProperty")
    public void bindProperty(WebDataBinder binder) {
        binder.setAllowedFields("title", "description", "price", "type", "location");
    }

    // =========================
    // PUBLIC HOME / SEARCH PAGE
    // =========================

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) PropertyType type,
            @RequestParam(required = false) String keyword,
            Model model) {

        if ((minPrice != null && (!Double.isFinite(minPrice) || minPrice < 0))
                || (maxPrice != null && (!Double.isFinite(maxPrice) || maxPrice < 0))
                || (minPrice != null && maxPrice != null && minPrice > maxPrice)) {
            throw new IllegalArgumentException("Invalid price range");
        }
        model.addAttribute(
                "properties",
                propertyService.searchProperties(
                        location,
                        minPrice,
                        maxPrice,
                        type,
                        keyword
                )
        );

        return "index";
    }

    // =========================
    // PUBLIC PROPERTY DETAILS
    // =========================

    @GetMapping("/properties/{id}")
    public String propertyDetail(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        Property property = propertyService.getPropertyById(id);
        boolean owner = authentication != null && property.getOwner() != null
                && authentication.getName().equals(property.getOwner().getEmail());
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!property.isApproved() && !owner && !admin) {
            throw new ResourceNotFoundException("Property not found");
        }
        model.addAttribute("property", property);

        return "property-detail";
    }

    // =========================
    // REGISTER
    // =========================

    @GetMapping("/register")
    public String showRegisterForm(Model model) {

        model.addAttribute("user", new User());

        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("user") User user,
            BindingResult result) {

        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(user);
        } catch (IllegalArgumentException ex) {
            result.rejectValue("email", "duplicate", "An account with this email already exists.");
            return "register";
        }

        return "redirect:/login?registered";
    }

    // =========================
    // LOGIN
    // =========================

    @GetMapping("/login")
    public String showLoginForm() {

        return "login";
    }

    // =========================
    // CUSTOMER DASHBOARD
    // =========================

    @GetMapping("/dashboard")
    public String customerDashboard(
            Principal principal,
            Model model) {

        User currentUser =
                userService.findByEmail(principal.getName());

        model.addAttribute(
                "properties",
                propertyService.getPropertiesByOwner(
                        currentUser.getId()
                )
        );

        model.addAttribute(
                "newProperty",
                new Property()
        );

        return "dashboard";
    }

    // =========================
    // CREATE PROPERTY
    // =========================

    @PostMapping("/properties/create")
    public String createProperty(
            @Valid @ModelAttribute("newProperty") Property property,
            BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Principal principal,
            Model model) {

        User currentUser =
                userService.findByEmail(principal.getName());

        if (result.hasErrors()) {

            model.addAttribute(
                    "properties",
                    propertyService.getPropertiesByOwner(
                            currentUser.getId()
                    )
            );

            return "dashboard";
        }

        if (imageFile != null && !imageFile.isEmpty()) {

            try {
                property.setImageUrl(cloudinaryService.uploadImage(imageFile));
            } catch (IllegalArgumentException ex) {
                result.reject("image.invalid", "Choose a valid PNG, JPEG, or GIF image up to 10 MB.");
            } catch (IOException ex) {
                result.reject("image.upload", "Image upload failed. Please try again or submit without an image.");
            }
            if (result.hasErrors()) {
                model.addAttribute("properties", propertyService.getPropertiesByOwner(currentUser.getId()));
                return "dashboard";
            }
        }

        propertyService.createProperty(
                property,
                currentUser
        );

        return "redirect:/dashboard?created";
    }
    @GetMapping("/properties/create")
    public String showCreatePropertyForm() {
        return "redirect:/dashboard";
    }

    // =========================
    // ADMIN DASHBOARD
    // =========================

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {

        model.addAttribute(
                "users",
                adminService.getAllUsers()
        );

        model.addAttribute(
                "pendingProperties",
                adminService.getPendingProperties()
        );

        return "admin";
    }

    // =========================
    // APPROVE PROPERTY
    // =========================

    @PostMapping("/admin/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveProperty(
            @PathVariable Long id) {

        adminService.approveProperty(id);

        return "redirect:/admin?approved";
    }

    // =========================
    // DELETE USER
    // =========================

    @PostMapping("/admin/users/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(
            @PathVariable Long id) {

        adminService.deleteUser(id);

        return "redirect:/admin?userDeleted";
    }
}

