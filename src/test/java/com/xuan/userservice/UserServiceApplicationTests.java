package com.xuan.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=test-only-user-role-control-secret-key-32-bytes",
        "jwt.expiration=7200"
})
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
