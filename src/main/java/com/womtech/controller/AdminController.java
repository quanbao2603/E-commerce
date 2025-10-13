package com.womtech.controller;

import com.womtech.entity.*;
import com.womtech.service.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
	private SubcategoryService subcategoryService;

	@Autowired
	private BrandService brandService;

	@Autowired
	private SpecificationService specificationService;

	@Autowired
	private InventoryService inventoryService;

	@Autowired
	private ProductService productService;

	// ========== DASHBOARD ==========
	@GetMapping("/dashboard")
	public String adminDashboard(Model model) {
		model.addAttribute("totalCategories", categoryService.getTotalCount());
		model.addAttribute("totalBrands", brandService.getTotalCount());
		model.addAttribute("totalSpecifications", specificationService.getTotalCount());
		model.addAttribute("totalProducts", productService.getTotalCount());
		model.addAttribute("lowStockCount", inventoryService.getLowStockCount());
		model.addAttribute("outOfStockCount", inventoryService.getOutOfStockCount());
		model.addAttribute("lowStockItems", inventoryService.getLowStockItems());
		return "admin/dashboard";
	}

	// ========== CATEGORY MANAGEMENT ==========
	@GetMapping("/categories")
	public String listCategories(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size,
			Model model) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
		Page<Category> categoryPage = categoryService.getAllCategories(pageable);

		model.addAttribute("categories", categoryPage.getContent());
		model.addAttribute("page", categoryPage);
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
			System.out.println(">>> [DEBUG] Dữ liệu nhận được từ form: ID: " + category.getCategoryID() + ", Name: "
					+ category.getName() + ", Status: " + category.getStatus());
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

	// ========== SUBCATEGORY MANAGEMENT ==========
	@GetMapping("/subcategories/category/{categoryID}")
	public String listSubcategories(@PathVariable String categoryID, Model model) {
		Category category = categoryService.getCategoryById(categoryID)
				.orElseThrow(() -> new RuntimeException("Category not found"));
		model.addAttribute("category", category);
		model.addAttribute("subcategories", subcategoryService.getSubcategoriesByCategoryId(categoryID));
		return "admin/subcategories";
	}
	
	@GetMapping("/subcategories/{subcategoryId}/products")
    public String listProductsBySubcategory(@PathVariable String subcategoryId, Model model) {
        Subcategory subcategory = subcategoryService.getSubcategoryById(subcategoryId)
                .orElseThrow(() -> new RuntimeException("Subcategory not found"));
        List<Product> products = productService.getProductsBySubcategory(subcategory);
        
        model.addAttribute("subcategory", subcategory);
        model.addAttribute("category", subcategory.getCategory());
        model.addAttribute("products", products);
        model.addAttribute("productCount", products.size());
        
        return "admin/product-by-subcategory";
    }

	@GetMapping("/subcategories/new/{categoryID}")
	public String newSubcategoryForm(@PathVariable String categoryID, Model model) {
		Category category = categoryService.getCategoryById(categoryID)
				.orElseThrow(() -> new RuntimeException("Category not found"));
		Subcategory subcategory = new Subcategory();
		subcategory.setCategory(category);
		model.addAttribute("subcategory", subcategory);
		model.addAttribute("category", category);
		return "admin/subcategory-form";
	}

	@GetMapping("/subcategories/edit/{id}")
	public String editSubcategoryForm(@PathVariable String id, Model model) {
		Subcategory subcategory = subcategoryService.getSubcategoryById(id)
				.orElseThrow(() -> new RuntimeException("Subcategory not found"));
		model.addAttribute("subcategory", subcategory);
		model.addAttribute("category", subcategory.getCategory());
		return "admin/subcategory-form";
	}

	@PostMapping("/subcategories/save")
	public String saveSubcategory(@ModelAttribute Subcategory subcategory, RedirectAttributes redirectAttributes) {
		try {
			subcategoryService.saveSubcategory(subcategory);
			redirectAttributes.addFlashAttribute("success", "Danh mục con đã được lưu thành công!");
			return "redirect:/admin/subcategories/category/" + subcategory.getCategory().getCategoryID();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu danh mục con: " + e.getMessage());
			return "redirect:/admin/subcategories";
		}
	}

	@GetMapping("/subcategories/delete/{id}")
	public String deleteSubcategory(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			Subcategory subcategory = subcategoryService.getSubcategoryById(id)
					.orElseThrow(() -> new RuntimeException("Subcategory not found"));
			String categoryID = subcategory.getCategory().getCategoryID();
			subcategoryService.deleteSubcategory(id);
			redirectAttributes.addFlashAttribute("success", "Danh mục con đã được xóa thành công!");
			return "redirect:/admin/subcategories/category/" + categoryID;
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa danh mục con: " + e.getMessage());
			return "redirect:/admin/subcategories";
		}
	}

	// ========== BRAND MANAGEMENT ==========
	@GetMapping("/brands")
	public String listBrands(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size,
			Model model) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
		Page<Brand> brandPage = brandService.getAllBrands(pageable);

		model.addAttribute("brands", brandPage.getContent());
		model.addAttribute("page", brandPage);
		return "admin/brands";
	}

	@GetMapping("/brands/new")
	public String newBrandForm(Model model) {
		model.addAttribute("brand", new Brand());
		return "admin/brand-form";
	}

	@GetMapping("/brands/edit/{id}")
	public String editBrandForm(@PathVariable String id, Model model) {
		Brand brand = brandService.getBrandById(id).orElseThrow(() -> new RuntimeException("Brand not found"));
		model.addAttribute("brand", brand);
		return "admin/brand-form";
	}

	@PostMapping("/brands/save")
	public String saveBrand(@ModelAttribute Brand brand, RedirectAttributes redirectAttributes) {
		try {
			brandService.saveBrand(brand);
			redirectAttributes.addFlashAttribute("success", "Thương hiệu đã được lưu thành công!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu thương hiệu: " + e.getMessage());
		}
		return "redirect:/admin/brands";
	}

	@GetMapping("/brands/delete/{id}")
	public String deleteBrand(@PathVariable String id, RedirectAttributes redirectAttributes) {
		System.out.println("Gọi API xóa thương hiệu với ID: " + id);
		try {
			brandService.deleteBrand(id);
			System.out.println("Xóa thành công, thêm thông báo success");
			redirectAttributes.addFlashAttribute("success", "Thương hiệu đã được xóa thành công!");
		} catch (RuntimeException e) {
			System.out.println("Lỗi RuntimeException: " + e.getMessage());
			redirectAttributes.addFlashAttribute("error", e.getMessage());
		} catch (Exception e) {
			System.out.println("Lỗi Exception: " + e.getMessage());
			redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa thương hiệu: " + e.getMessage());
		}
		System.out.println("Chuyển hướng đến /admin/brands");
		return "redirect:/admin/brands";
	}

	// ========== PRODUCT MANAGEMENT ==========
	@GetMapping("/products")
	public String listProducts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			Model model) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
		Page<Product> productPage = productService.getAllProducts(pageable);

		model.addAttribute("products", productPage.getContent());
		model.addAttribute("page", productPage);
		return "admin/products";
	}

	@GetMapping("/products/new")
	public String newProductForm(Model model) {
		model.addAttribute("product", new Product());
		model.addAttribute("categories", categoryService.getAllCategories());
		model.addAttribute("brands", brandService.getAllBrands());
		return "admin/product-form";
	}

	@GetMapping("/products/edit/{id}")
	public String editProductForm(@PathVariable String id, Model model) {
		Product product = productService.getProductById(id)
				.orElseThrow(() -> new RuntimeException("Product not found"));
		model.addAttribute("product", product);
		model.addAttribute("categories", categoryService.getAllCategories());
		model.addAttribute("brands", brandService.getAllBrands());
		return "admin/product-form";
	}

	@PostMapping("/products/save")
	public String saveProduct(@ModelAttribute Product product, RedirectAttributes redirectAttributes) {
		try {
			productService.saveProduct(product);
			redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được lưu thành công!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu sản phẩm: " + e.getMessage());
		}
		return "redirect:/admin/products";
	}

	@GetMapping("/products/delete/{id}")
	public String deleteProduct(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			productService.deleteProduct(id);
			redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được xóa thành công!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa sản phẩm: " + e.getMessage());
		}
		return "redirect:/admin/products";
	}

	// ========== SPECIFICATION MANAGEMENT (UPDATED) ==========

	@GetMapping("/specifications")
	public String listSpecifications(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Model model) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("product.productID").ascending());
		Page<Specification> specificationPage = specificationService.getAllSpecifications(pageable);

		model.addAttribute("specifications", specificationPage.getContent());
		model.addAttribute("page", specificationPage);
		return "admin/specifications";
	}

	@GetMapping("/specifications/product/{productID}")
	public String listSpecificationsByProduct(@PathVariable String productID, Model model) {
		Product product = productService.getProductById(productID)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

		model.addAttribute("product", product);
		model.addAttribute("specifications", product.getSpecifications());

		return "admin/specifications-by-product";
	}

	@GetMapping("/specifications/new/{productID}")
	public String newSpecificationFormForProduct(@PathVariable String productID, Model model) {
		Product product = productService.getProductById(productID)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

		Specification specification = new Specification();
		specification.setProduct(product);

		model.addAttribute("specification", specification);
		model.addAttribute("product", product);

		return "admin/specification-form";
	}

	@GetMapping("/specifications/new")
	public String newSpecificationForm(Model model) {
		model.addAttribute("specification", new Specification());
		model.addAttribute("products", productService.getAllProducts());
		return "admin/specification-form";
	}

	@GetMapping("/specifications/edit/{id}")
	public String editSpecificationForm(@PathVariable String id, Model model) {
		Specification specification = specificationService.getSpecificationByID(id)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy thông số kỹ thuật"));

		model.addAttribute("specification", specification);
		model.addAttribute("product", specification.getProduct());
		model.addAttribute("products", productService.getAllProducts());

		return "admin/specification-form";
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
				return "redirect:/admin/specifications/product/" + specification.getProduct().getProductID();
			}

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu thông số: " + e.getMessage());
		}

		return "redirect:/admin/specifications";
	}

	/**
	 * Xóa thông số kỹ thuật
	 */
	@GetMapping("/specifications/delete/{id}")
	public String deleteSpecification(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			Specification specification = specificationService.getSpecificationByID(id)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy thông số kỹ thuật"));

			String productID = specification.getProduct().getProductID();
			specificationService.deleteSpecification(id);

			redirectAttributes.addFlashAttribute("success", "Thông số kỹ thuật đã được xóa thành công!");

			// Redirect về trang specifications của product
			return "redirect:/admin/specifications/product/" + productID;

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa thông số: " + e.getMessage());
			return "redirect:/admin/specifications";
		}
	}

	// ========== INVENTORY MANAGEMENT ==========

	@GetMapping("/inventory")
	public String listInventory(@RequestParam(value = "filter", defaultValue = "all") String filter,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("updateAt").descending());
		Page<Inventory> inventoryPage;

		switch (filter.toLowerCase()) {
		case "low-stock":
			inventoryPage = inventoryService.getLowStockItems(pageable);
			model.addAttribute("title", "Sản phẩm sắp hết hàng");
			break;
		case "out-of-stock":
			inventoryPage = inventoryService.getOutOfStockItems(pageable);
			model.addAttribute("title", "Sản phẩm đã hết hàng");
			break;
		default:
			inventoryPage = inventoryService.getAllInventory(pageable);
			model.addAttribute("title", "Tất cả sản phẩm tồn kho");
			break;
		}

		model.addAttribute("inventoryItems", inventoryPage.getContent());
		model.addAttribute("page", inventoryPage);
		model.addAttribute("filter", filter);

		// Dữ liệu tổng quan
		model.addAttribute("totalCount", inventoryService.getTotalCount());
		model.addAttribute("lowStockCount", inventoryService.getLowStockCount());
		model.addAttribute("outOfStockCount", inventoryService.getOutOfStockCount());

		return "admin/inventory";

	}

	@GetMapping("/inventory/new")
	public String newInventoryForm(Model model) {
		model.addAttribute("inventory", new Inventory());
		model.addAttribute("products", productService.getAllProducts());
		model.addAttribute("locations", inventoryService.getAllLocations());
		return "admin/inventory-form";
	}

	@GetMapping("/inventory/edit/{id}")
	public String editInventoryForm(@PathVariable String id, Model model) {
		Inventory inventory = inventoryService.getInventoryByID(id)
				.orElseThrow(() -> new RuntimeException("Inventory not found"));
		model.addAttribute("inventory", inventory);
		model.addAttribute("products", productService.getAllProducts());
		model.addAttribute("locations", inventoryService.getAllLocations());
		return "admin/inventory-form";
	}

	@PostMapping("/inventory/save")
	public String saveInventory(@ModelAttribute Inventory inventory, RedirectAttributes redirectAttributes) {
		try {
			inventoryService.saveInventory(inventory);
			redirectAttributes.addFlashAttribute("success", "Tồn kho đã được cập nhật thành công!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật tồn kho: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}

	@PostMapping("/inventory/restock/{id}")
	public String restockInventory(@PathVariable String id, @RequestParam Integer quantity,
			RedirectAttributes redirectAttributes) {
		try {
			inventoryService.restockInventory(id, quantity);
			redirectAttributes.addFlashAttribute("success", "Nhập kho thành công!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi nhập kho: " + e.getMessage());
		}
		return "redirect:/admin/inventory";
	}
}
