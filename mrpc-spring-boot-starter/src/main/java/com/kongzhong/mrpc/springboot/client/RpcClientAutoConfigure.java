package com.kongzhong.mrpc.springboot.client;

import com.kongzhong.mrpc.Const;
import com.kongzhong.mrpc.client.Referers;
import com.kongzhong.mrpc.springboot.config.AdminProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * RPC客户端自动配置
 *
 * @author biezhi
 *         2017/5/13
 */
@Slf4j
@Conditional(ClientEnvironmentCondition.class)
@EnableConfigurationProperties({AdminProperties.class})
public class RpcClientAutoConfigure {

    @Bean
    @ConditionalOnBean(value = Referers.class)
    public BootRpcClient bootRpcClient() {
        System.out.println(Const.CLIENT_BANNER);
        return new BootRpcClient();
    }

}
