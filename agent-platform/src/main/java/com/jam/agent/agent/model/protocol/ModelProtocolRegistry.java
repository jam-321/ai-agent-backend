package com.jam.agent.agent.model.protocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 发现协议适配器并按供应商配置路由模型调用。 */
@Component
public class ModelProtocolRegistry {

    private final Map<String, ModelProtocolAdapter> adapters;

    public ModelProtocolRegistry(List<ModelProtocolAdapter> discovered) {
        Map<String, ModelProtocolAdapter> registered = new LinkedHashMap<>();
        for (ModelProtocolAdapter adapter : discovered) {
            String protocol = ModelProtocol.normalize(adapter.protocolType());
            if (registered.putIfAbsent(protocol, adapter) != null) {
                throw new IllegalStateException("模型协议适配器重复：" + protocol);
            }
        }
        this.adapters = Map.copyOf(registered);
    }

    public boolean supports(String protocolType) {
        return adapters.containsKey(ModelProtocol.normalize(protocolType));
    }

    public ModelProtocolAdapter require(String protocolType) {
        String normalized = ModelProtocol.normalize(protocolType);
        ModelProtocolAdapter adapter = adapters.get(normalized);
        if (adapter == null) {
            throw new IllegalArgumentException("当前后端尚未支持模型协议：" + normalized);
        }
        return adapter;
    }

    /** 管理端不能配置一个当前运行时没有适配器的协议。 */
    public List<String> protocolTypes() {
        return adapters.keySet().stream().sorted().toList();
    }
}
