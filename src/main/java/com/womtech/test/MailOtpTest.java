package com.womtech.test;

import com.womtech.util.EmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;


@SpringBootApplication(
	    scanBasePackages = "com.womtech",
	    exclude = {
	        DataSourceAutoConfiguration.class,
	        HibernateJpaAutoConfiguration.class
	    }
	)
public class MailOtpTest implements CommandLineRunner {

    @Autowired
    private EmailUtil emailUtil;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MailOtpTest.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        try {
            String to = "23110181@student.hcmute.edu.vn";
            String username = "quanbao2603";
            String otp = "839264"; 
            int ttl = 10;
            System.out.println("🔄 Đang gửi mail xác thực đến: " + to);
            emailUtil.sendVerifyOtp(to, username, otp, ttl);
            System.out.println("✅ Gửi thành công!");
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi gửi mail: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
