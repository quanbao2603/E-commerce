package com.womtech.controller;

import com.womtech.entity.*;
import com.womtech.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SpecificationService specificationService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductService productService;

    // ========== DASHBOARD ==========
    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("totalCategories", categoryService.getTotalCount());
//        model.addAttribute("totalBrands", brandService.getTotalCount());
//        model.addAttribute("totalSpecifications", specificationService.getTotalCount());
//        model.addAttribute("totalProducts", productService.getTotalCount());
//        model.addAttribute("lowStockCount", inventoryService.getLowStockCount());
//        model.addAttribute("outOfStockCount", inventoryService.getOutOfStockCount());
//        model.addAttribute("lowStockItems", inventoryService.getLowStockItems());
        return "admin/dashboard";
    }

    // ========== CATEGORY MANAGEMENT ==========
    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String newCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-form";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable String id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        model.addAttribute("category", category);
        return "admin/category-form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category, RedirectAttributes redirectAttributes) {
        try {
        	System.out.println(">>> [DEBUG] Gọi hàm saveCategory()");
            System.out.println(">>> [DEBUG] Dữ liệu nhận được từ form: ID: " + category.getCategoryID() + ", Name: " + category.getName() + ", Status: " + category.getStatus());
            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("success", "Danh mục đã được lưu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu danh mục: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Danh mục đã được xóa thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi khi xóa danh mục: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }


//    // ========== BRAND MANAGEMENT ==========
//    @GetMapping("/brands")
//    public String listBrands(Model model) {
//        model.addAttribute("brands", brandService.getAllBrands());
//        return "admin/brands";
//    }
//
//    @GetMapping("/brands/new")
//    public String newBrandForm(Model model) {
//        model.addAttribute("brand", new Brand());
//        return "admin/brand-form";
//    }
//
//    @GetMapping("/brands/edit/{id}")
//    public String editBrandForm(@PathVariable String id, Model model) {
//        Brand brand = brandService.getBrandById(id)
//                .orElseThrow(() -> new RuntimeException("Brand not found"));
//        model.addAttribute("brand", brand);
//        return "admin/brand-form";
//    }
//
//    @PostMapping("/brands/save")
//    public String saveBrand(@ModelAttribute Brand brand, RedirectAttributes redirectAttributes) {
//        try {
//            brandService.saveBrand(brand);
//            redirectAttributes.addFlashAttribute("success", "Thương hiệu đã được lưu thành công!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu thương hiệu: " + e.getMessage());
//        }
//        return "redirect:/admin/brands";
//    }
//
//    @GetMapping("/brands/delete/{id}")
//    public String deleteBrand(@PathVariable String id, RedirectAttributes redirectAttributes) {
//        try {
//            brandService.deleteBrand(id);
//            redirectAttributes.addFlashAttribute("success", "Thương hiệu đã được xóa thành công!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa thương hiệu: " + e.getMessage());
//        }
//        return "redirect:/admin/brands";
//    }
//
//    // ========== SPECIFICATION MANAGEMENT ==========
//    @GetMapping("/specifications")
//    public String listSpecifications(Model model) {
//        model.addAttribute("specifications", specificationService.getAllSpecifications());
//        return "admin/specifications";
//    }
//
//    @GetMapping("/specifications/new")
//    public String newSpecificationForm(Model model) {
//        model.addAttribute("specification", new Specification());
//        model.addAttribute("products", productService.getAllProducts());
//        return "admin/specification-form";
//    }
//
//    @GetMapping("/specifications/edit/{id}")
//    public String editSpecificationForm(@PathVariable String id, Model model) {
//        Specification specification = specificationService.getSpecificationById(id)
//                .orElseThrow(() -> new RuntimeException("Specification not found"));
//        model.addAttribute("specification", specification);
//        model.addAttribute("products", productService.getAllProducts());
//        return "admin/specification-form";
//    }
//
//    @PostMapping("/specifications/save")
//    public String saveSpecification(@ModelAttribute Specification specification, RedirectAttributes redirectAttributes) {
//        try {
//            specificationService.saveSpecification(specification);
//            redirectAttributes.addFlashAttribute("success", "Thông số kỹ thuật đã được lưu thành công!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu thông số: " + e.getMessage());
//        }
//        return "redirect:/admin/specifications";
//    }
//
//    @GetMapping("/specifications/delete/{id}")
//    public String deleteSpecification(@PathVariable String id, RedirectAttributes redirectAttributes) {
//        try {
//            specificationService.deleteSpecification(id);
//            redirectAttributes.addFlashAttribute("success", "Thông số kỹ thuật đã được xóa thành công!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa thông số: " + e.getMessage());
//        }
//        return "redirect:/admin/specifications";
//    }
//
//    // ========== INVENTORY MANAGEMENT ==========
//    @GetMapping("/inventory")
//    public String listInventory(Model model) {
//        model.addAttribute("inventoryItems", inventoryService.getAllInventory());
//        model.addAttribute("lowStockItems", inventoryService.getLowStockItems());
//        model.addAttribute("outOfStockItems", inventoryService.getOutOfStockItems());
//        return "admin/inventory";
//    }
//
//    @GetMapping("/inventory/new")
//    public String newInventoryForm(Model model) {
//        model.addAttribute("inventory", new Inventory());
//        model.addAttribute("products", productService.getAllProducts());
//        model.addAttribute("locations", inventoryService.getAllLocations());
//        return "admin/inventory-form";
//    }
//
//    @GetMapping("/inventory/edit/{id}")
//    public String editInventoryForm(@PathVariable String id, Model model) {
//        Inventory inventory = inventoryService.getInventoryById(id)
//                .orElseThrow(() -> new RuntimeException("Inventory not found"));
//        model.addAttribute("inventory", inventory);
//        model.addAttribute("products", productService.getAllProducts());
//        model.addAttribute("locations", inventoryService.getAllLocations());
//        return "admin/inventory-form";
//    }
//
//    @PostMapping("/inventory/save")
//    public String saveInventory(@ModelAttribute Inventory inventory, RedirectAttributes redirectAttributes) {
//        try {
//            inventoryService.saveInventory(inventory);
//            redirectAttributes.addFlashAttribute("success", "Tồn kho đã được cập nhật thành công!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật tồn kho: " + e.getMessage());
//        }
//        return "redirect:/admin/inventory";
//    }
//
//    @PostMapping("/inventory/restock/{id}")
//    public String restockInventory(@PathVariable String id, @RequestParam Integer quantity, RedirectAttributes redirectAttributes) {
//        try {
//            inventoryService.restockInventory(id, quantity);
//            redirectAttributes.addFlashAttribute("success", "Nhập kho thành công!");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Lỗi khi nhập kho: " + e.getMessage());
//        }
//        return "redirect:/admin/inventory";
//    }
}
