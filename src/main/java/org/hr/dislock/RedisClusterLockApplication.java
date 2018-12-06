package org.hr.dislock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisClusterLockApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisClusterLockApplication.class, args);
	}
}
