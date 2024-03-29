package xyz.chener.zp.zpstoragecalculation;

import com.alibaba.cloud.sentinel.endpoint.SentinelEndpointAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import xyz.chener.zp.zpstoragecalculation.config.feign.loadbalance.LoadbalanceConfig;

import java.util.UUID;

@SpringBootApplication(exclude = {SentinelEndpointAutoConfiguration.class})
@EnableFeignClients
@LoadBalancerClients({
        @LoadBalancerClient(name = "zp-storagecalculation-module",configuration = LoadbalanceConfig.class)
})
@EnableTransactionManagement
@Deprecated(since = "2023-08-29",forRemoval = true)
public class ZpStorageCalculationApplication {
    public static void main(String[] args) {
        System.setProperty("csp.sentinel.log.output.type","console");
        SpringApplication.run(ZpStorageCalculationApplication.class, args);
    }

}
