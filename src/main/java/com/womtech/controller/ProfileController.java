package com.womtech.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.womtech.entity.Address;
import com.womtech.entity.Role;
import com.womtech.entity.User;
import com.womtech.service.AddressService;
import com.womtech.service.RoleService;
import com.womtech.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/profile")
public class ProfileController {
	@Autowired
	UserService userService;
	@Autowired
	RoleService roleService;
	@Autowired
	AddressService addressService;

//  @GetMapping("")
//  public String showProfilePage(Model model) {
//	  User user = userService.findByEmail("abc@mail.com").orElse(null);
//	  
//	  List<Address> addresses = user.getAddresses();
//	  Address defaultAddress = addressService.findByUserAndIsDefaultTrue(user).orElse(null);
//	  
//	  model.addAttribute("addresses", addresses);
//	  model.addAttribute("address", defaultAddress);
//      model.addAttribute("user", user);
//      return "user/profile";
//  }
  
    
    @GetMapping("")
    public String showProfilePage(Model model) {
        // Tạo user mẫu để test UI
        UserMock user = new UserMock();
        user.setFullName("Nguyễn Văn A");
        user.setEmail("nguyenvana@example.com");
        user.setPhone("0987654321");
        user.setCity("Hồ Chí Minh");
        user.setDistrict("Quận 1");
        user.setWard("Phường Bến Nghé");
        user.setAddress("123 Lê Lợi");
        user.setCreatedAt(LocalDateTime.of(2023, 5, 20, 10, 0));
        user.setAdmin(false);

        // Gửi dữ liệu tới view
        model.addAttribute("user", user);
        return "user/profile";
    }

    // Lớp UserMock tạm để test, chưa cần entity thật
    static class UserMock {
        private String fullName;
        private String email;
        private String phone;
        private String city;
        private String district;
        private String ward;
        private String address;
        private boolean admin;
        private LocalDateTime createdAt;

        // Getter & Setter
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public boolean isAdmin() { return admin; }
        public void setAdmin(boolean admin) { this.admin = admin; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        // Dùng trong giao diện
        public String getFullAddress() {
            return address + ", " + ward + ", " + district + ", " + city;
        }
    }
}
