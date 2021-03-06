package com.kongzhong.mrpc.server;

import com.kongzhong.mrpc.Const;
import com.kongzhong.mrpc.annotation.RpcService;
import com.kongzhong.mrpc.enums.RegistryEnum;
import com.kongzhong.mrpc.exception.SystemException;
import com.kongzhong.mrpc.interceptor.RpcServerInterceptor;
import com.kongzhong.mrpc.model.RegistryBean;
import com.kongzhong.mrpc.model.ServiceBean;
import com.kongzhong.mrpc.registry.DefaultRegistry;
import com.kongzhong.mrpc.registry.ServiceRegistry;
import com.kongzhong.mrpc.spring.utils.AopTargetUtils;
import com.kongzhong.mrpc.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * RPC服务端Spring实现
 *
 * @author biezhi
 * 2017/4/24
 */
@Slf4j
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RpcSpringServer extends SimpleRpcServer implements ApplicationContextAware, InitializingBean {

    /**
     * ① 设置上下文
     *
     * @param ctx Spring应用上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        System.out.println(Const.SERVER_BANNER);

        // 注册中心
        Map<String, RegistryBean> registryBeanMap = ctx.getBeansOfType(RegistryBean.class);
        if (null != registryBeanMap) {
            registryBeanMap.values().forEach(registryBean -> SERVICE_REGISTRY_MAP.put(registryBean.getName(), parseRegistry(registryBean)));
        }

        if (StringUtils.isNotEmpty(this.interceptors)) {
            String[] inters = this.interceptors.split(",");
            for (String interceptorName : inters) {
                RpcServerInterceptor rpcServerInteceptor = (RpcServerInterceptor) ctx.getBean(interceptorName);
                rpcMapping.addInterceptor(rpcServerInteceptor);
            }
        }

        Map<String, Object> rpcServiceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
        if (null != rpcServiceBeanMap && !rpcServiceBeanMap.isEmpty()) {
            rpcServiceBeanMap.forEach((beanName, target) -> {
                Object realBean = null;
                try {
                    realBean = AopTargetUtils.getTarget(target);
                } catch (Exception e) {
                    log.error("Get bean target error", e);
                }
                rpcMapping.addServiceBean(realBean, beanName);
            });
        }

        // 服务
        Map<String, ServiceBean> serviceBeanMap = ctx.getBeansOfType(ServiceBean.class);
        if (serviceBeanMap != null && !serviceBeanMap.isEmpty()) {
            serviceBeanMap.values().forEach(serviceBean -> {
                if (null == serviceBean.getBean()) {
                    Object bean = ctx.getBean(serviceBean.getBeanName());
                    if (null == bean) {
                        throw new SystemException(String.format("Not found bean [%s]", serviceBean.getBeanName()));
                    }
                    serviceBean.setBean(bean);
                    rpcMapping.addServiceBean(serviceBean);
                }
            });
        }
    }

    /**
     * ② 后置操作
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> super.close()));
        this.startServer();
    }

    private ServiceRegistry parseRegistry(RegistryBean registryBean) {
        String type = registryBean.getType();
        if (RegistryEnum.DEFAULT.getName().equals(type)) {
            return new DefaultRegistry();
        }
        if (RegistryEnum.ZOOKEEPER.getName().equals(type)) {
            String zkAddress = registryBean.getAddress();
            log.info("RPC server connect zookeeper address: {}", zkAddress);
            return super.getZookeeperServiceRegistry(zkAddress);
        }
        return null;
    }

}