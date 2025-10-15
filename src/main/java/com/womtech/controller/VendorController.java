package com.womtech.controller;

import com.womtech.entity.*;
import com.womtech.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor")
public class VendorController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private SubcategoryService subcategoryService;
    
    @Autowired
    private BrandService brandService;
    
    @Autowired
    private SpecificationService specificationService;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private CloudinaryService cloudinaryService;
    
    @Autowired
    private UserService userService;
    
    // Helper method to get current user
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        System.out.println("Current username: " + authentication.getName());

        String userId = authentication.getName();
        return userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

    }
    
    // Helper method to check if user owns the product
    private boolean isOwner(Product product, User user) {
        return product.getOwnerUser() != null && 
               product.getOwnerUser().getUserID().equals(user.getUserID());
    }
    
    // ========== DASHBOARD ==========
    @GetMapping("/dashboard")
    public String vendorDashboard(Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);
        List<Product> myProducts = productService.getAllProducts().stream()
                .filter(p -> isOwner(p, currentUser))
                .collect(Collectors.toList());
        
        // Calculate statistics
        long totalProducts = myProducts.size();
        long activeProducts = myProducts.stream().filter(p -> p.getStatus() == 1).count();
        long outOfStockProducts = myProducts.stream().filter(p -> p.getStatus() == 2).count();
        
        // Calculate total value
        BigDecimal totalValue = productService.calculateTotalValueByOwnerId(currentUser.getUserID());
        
       
        
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("activeProducts", activeProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("recentProducts", myProducts.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("currentUser", currentUser);
        
        return "vendor/dashboard";
    }
    
    
    // ========== PRODUCT MANAGEMENT ==========
    @GetMapping("/products")
    public String listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryID,
            @RequestParam(required = false) String subcategoryID,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication,
            Model model) {
        
        User currentUser = getCurrentUser(authentication);
        
        // Get only products owned by current vendor
        List<Product> allProducts = productService.getAllProducts().stream()
                .filter(p -> isOwner(p, currentUser))
                .collect(Collectors.toList());        
        System.out.println(currentUser.getUserID() + " có " + allProducts.size() + " sản phẩm");
        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            allProducts = allProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(searchLower) ||
                                p.getProductID().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }
        
        if (categoryID != null && !categoryID.trim().isEmpty()) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getSubcategory().getCategory().getCategoryID().equals(categoryID))
                    .collect(Collectors.toList());
        }
        
        if (subcategoryID != null && !subcategoryID.trim().isEmpty()) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getSubcategory().getSubcategoryID().equals(subcategoryID))
                    .collect(Collectors.toList());
        }
        
        if (brandId != null && !brandId.trim().isEmpty()) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getBrand().getBrandID().equals(brandId))
                    .collect(Collectors.toList());
        }
        
        if (status != null) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getStatus().equals(status))
                    .collect(Collectors.toList());
        }
        
        // Sort by createAt descending
        allProducts = allProducts.stream()
                .sorted((p1, p2) -> p2.getCreateAt().compareTo(p1.getCreateAt()))
                .collect(Collectors.toList());
        
        // Manual pagination
        int start = Math.min(page * size, allProducts.size());
        int end = Math.min(start + size, allProducts.size());
        List<Product> pagedProducts = allProducts.subList(start, end);
        
        // Create Page object
        Pageable pageable = PageRequest.of(page, size);
        org.springframework.data.domain.PageImpl<Product> pageImpl = 
            new org.springframework.data.domain.PageImpl<>(pagedProducts, pageable, allProducts.size());
        
        model.addAttribute("products", pagedProducts);
        model.addAttribute("page", pageImpl);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("brands", brandService.getActiveBrands());
        
        if (categoryID != null && !categoryID.trim().isEmpty()) {
            model.addAttribute("subcategories", subcategoryService.getSubcategoriesByCategoryId(categoryID));
        }
        
        return "vendor/products";
    }
    
    @GetMapping("/products/new")
    public String newProductForm(Authentication authentication, Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());
        return "vendor/product-form";
    }
    
    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable String id, Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check ownership
        if (!isOwner(product, currentUser)) {
            throw new RuntimeException("You don't have permission to edit this product");
        }
        
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", brandService.getAllBrands());
        return "vendor/product-form";
    }
    
    @PostMapping("/products/save")
    public String saveProduct(
            @ModelAttribute Product product,
            @RequestParam(required = false) MultipartFile thumbnailFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            // If editing, check ownership
            if (product.getProductID() != null && !product.getProductID().isEmpty()) {
                Product existingProduct = productService.getProductById(product.getProductID())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                
                if (!isOwner(existingProduct, currentUser)) {
                    throw new RuntimeException("You don't have permission to edit this product");
                }
                
                // Preserve existing thumbnail if no new file
                if (thumbnailFile == null || thumbnailFile.isEmpty()) {
                    product.setThumbnail(existingProduct.getThumbnail());
                }
            }
            
            // Auto-assign current user as owner
            product.setOwnerUser(currentUser);
            
            // Handle thumbnail upload
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String imageUrl = cloudinaryService.uploadImage(thumbnailFile);
                product.setThumbnail(imageUrl);
            }
            
            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được lưu thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu sản phẩm: " + e.getMessage());
        }
        
        return "redirect:/vendor/products";
    }
    
    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable String id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(authentication);
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            
            // Check ownership
            if (!isOwner(product, currentUser)) {
                throw new RuntimeException("You don't have permission to delete this product");
            }
            
            // Delete thumbnail from Cloudinary if exists
            if (product.getThumbnail() != null && !product.getThumbnail().isEmpty()) {
                try {
                    cloudinaryService.deleteImage(product.getThumbnail());
                } catch (Exception e) {
                    // Log but continue with deletion
                    System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
                }
            }
            
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được xóa thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/vendor/products";
    }
    
    // ========== SPECIFICATION MANAGEMENT ==========
    @GetMapping("/specifications/product/{productId}")
    public String listSpecifications(@PathVariable String productId, Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);
        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check ownership
        if (!isOwner(product, currentUser)) {
            throw new RuntimeException("You don't have permission to view specifications for this product");
        }
        
        List<Specification> specifications = specificationService.getSpecificationsByProduct(product);
        
        model.addAttribute("product", product);
        model.addAttribute("specifications", specifications);
        model.addAttribute("specCount", specifications.size());
        
        return "vendor/specifications-by-product";
    }
    
    @GetMapping("/specifications/new/{productID}")
	public String newSpecificationFormForProduct(@PathVariable String productID, Model model) {
		Product product = productService.getProductById(productID)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

		Specification specification = new Specification();
		specification.setProduct(product);

		model.addAttribute("specification", specification);
		model.addAttribute("product", product);

		return "vendor/specification-form";
	}
    
    @GetMapping("/specifications/edit/{id}")
    public String editSpecificationForm(@PathVariable String id, Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);
        Specification specification = specificationService.getSpecificationByID(id)
                .orElseThrow(() -> new RuntimeException("Specification not found"));
        
        // Check ownership through product
        if (!isOwner(specification.getProduct(), currentUser)) {
            throw new RuntimeException("You don't have permission to edit this specification");
        }
        
        model.addAttribute("specification", specification);
        model.addAttribute("product", specification.getProduct());
        
        return "vendor/specification-form";
    }
    
    @PostMapping("/specifications/save")
	public String saveSpecification(@ModelAttribute Specification specification,
			@RequestParam(required = false) String productID, RedirectAttributes redirectAttributes) {
		try {
			// Nếu có productID từ form, set product
			if (productID != null && !productID.isEmpty()) {
				Product product = productService.getProductById(productID)
						.orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
				specification.setProduct(product);
			}

			specificationService.saveSpecification(specification);
			redirectAttributes.addFlashAttribute("success", "Thông số kỹ thuật đã được lưu thành công!");

			// Redirect về trang specifications của product nếu có
			if (specification.getProduct() != null) {
				return "redirect:/vendor/specifications/product/" + specification.getProduct().getProductID();
			}

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu thông số: " + e.getMessage());
		}

		return "redirect:/vendor/specifications";
	}
    
    @GetMapping("/specifications/delete/{id}")
    public String deleteSpecification(@PathVariable String id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(authentication);
            Specification specification = specificationService.getSpecificationByID(id)
                    .orElseThrow(() -> new RuntimeException("Specification not found"));
            
            String productId = specification.getProduct().getProductID();
            
            // Check ownership
            if (!isOwner(specification.getProduct(), currentUser)) {
                throw new RuntimeException("You don't have permission to delete this specification");
            }
            
            specificationService.deleteSpecification(id);
            redirectAttributes.addFlashAttribute("success", "Thông số kỹ thuật đã được xóa thành công!");
            
            return "redirect:/vendor/specifications/product/" + productId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa thông số: " + e.getMessage());
            return "redirect:/vendor/products";
        }
    }
    
    // ========== API ENDPOINTS ==========
    @GetMapping("/api/subcategories/category/{categoryID}")
    @ResponseBody
    public List<Subcategory> getSubcategoriesByCategory(@PathVariable String categoryID) {
        return subcategoryService.getSubcategoriesByCategoryId(categoryID);
    }
}
