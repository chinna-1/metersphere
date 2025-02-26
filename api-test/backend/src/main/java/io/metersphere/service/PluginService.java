package io.metersphere.service;

import io.metersphere.api.dto.plugin.PluginDTO;
import io.metersphere.api.dto.plugin.PluginRequest;
import io.metersphere.api.dto.plugin.PluginResourceDTO;
import io.metersphere.base.domain.Plugin;
import io.metersphere.base.domain.PluginExample;
import io.metersphere.base.domain.PluginWithBLOBs;
import io.metersphere.base.mapper.PluginMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.BeanUtils;
import io.metersphere.commons.utils.FileUtils;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.plugin.core.ui.PluginResource;
import io.metersphere.commons.utils.CommonUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class PluginService {
    @Resource
    private PluginMapper pluginMapper;

    public String editPlugin(MultipartFile file) {
        String id = UUID.randomUUID().toString();
        String path = FileUtils.create(id, file);
        if (StringUtils.isNotEmpty(path)) {
            List<PluginResourceDTO> resources = this.getMethod(path, file.getOriginalFilename());
            if (CollectionUtils.isNotEmpty(resources)) {
                for (PluginResourceDTO resource : resources) {
                    PluginExample example = new PluginExample();
                    example.createCriteria().andPluginIdEqualTo(resource.getPluginId());
                    List<Plugin> plugins = pluginMapper.selectByExample(example);
                    if (CollectionUtils.isNotEmpty(plugins)) {
                        String delPath = plugins.get(0).getSourcePath();
                        // this.closeJar(delPath);
                        FileUtils.deleteFile(delPath);
                        pluginMapper.deleteByExample(example);
                    }
                    this.create(resource, path, file.getOriginalFilename());
                }
            }
        }
        return null;
    }

    private void create(PluginResourceDTO resource, String path, String name) {
        resource.getUiScripts().forEach(item -> {
            PluginWithBLOBs plugin = new PluginWithBLOBs();
            plugin.setId(UUID.randomUUID().toString());
            plugin.setCreateTime(System.currentTimeMillis());
            plugin.setUpdateTime(System.currentTimeMillis());
            plugin.setName(item.getName());
            plugin.setPluginId(resource.getPluginId());
            plugin.setScriptId(item.getId());
            plugin.setSourcePath(path);
            plugin.setFormOption(item.getFormOption());
            plugin.setFormScript(item.getFormScript());
            plugin.setClazzName(item.getClazzName());
            plugin.setSourceName(name);
            plugin.setJmeterClazz(item.getJmeterClazz());
            plugin.setExecEntry(resource.getEntry());
            plugin.setCreateUserId(SessionUtils.getUserId());
            pluginMapper.insert(plugin);
        });
    }

    private boolean isXpack(Class<?> aClass, Object instance) {
        try {
            Object verify = aClass.getDeclaredMethod("xpack").invoke(instance);
            return (Boolean) verify;
        } catch (Exception e) {
            return false;
        }
    }

    private List<PluginResourceDTO> getMethod(String path, String fileName) {
        List<PluginResourceDTO> resources = new LinkedList<>();
        this.loadJar(path);
        List<Class<?>> classes = CommonUtil.getSubClass(fileName);
        try {
            for (Class<?> aClass : classes) {
                Object instance = aClass.newInstance();
                Object pluginObj = aClass.getDeclaredMethod("init").invoke(instance);
                if (pluginObj != null) {
                    PluginResourceDTO pluginResourceDTO = new PluginResourceDTO();
                    BeanUtils.copyBean(pluginResourceDTO, (PluginResource) pluginObj);
                    pluginResourceDTO.setEntry(aClass.getName());
                    resources.add(pluginResourceDTO);
                }
            }
        } catch (Exception e) {
            LogUtil.error("初始化脚本异常：" + e.getMessage());
            MSException.throwException("调用插件初始化脚本失败");
        }
        return resources;
    }

    private boolean loadJar(String jarPath) {
        try {
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            try {
                File file = new File(jarPath);
                if (!file.exists()) {
                    return false;
                }
                Method method = classLoader.getClass().getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, file.toURI().toURL());
            } catch (NoSuchMethodException e) {
                Method method = classLoader.getClass()
                        .getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
                method.setAccessible(true);
                method.invoke(classLoader, jarPath);
            }
            return true;
        } catch (Exception e) {
            LogUtil.error(e);
        }
        return false;
    }

    public void loadPlugins() {
        try {
            PluginExample example = new PluginExample();
            List<Plugin> plugins = pluginMapper.selectByExample(example);
            if (CollectionUtils.isNotEmpty(plugins)) {
                plugins = plugins.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(()
                        -> new TreeSet<>(Comparator.comparing(Plugin::getPluginId))), ArrayList::new));
                if (CollectionUtils.isNotEmpty(plugins)) {
                    plugins.forEach(item -> {
                        boolean isLoad = this.loadJar(item.getSourcePath());
                        if (!isLoad) {
                            PluginExample pluginExample = new PluginExample();
                            pluginExample.createCriteria().andPluginIdEqualTo(item.getPluginId());
                            pluginMapper.deleteByExample(pluginExample);
                        }
                    });
                }
            }
        } catch (Exception e) {
            LogUtil.error(e);
        }
    }

    public List<PluginDTO> list(String name) {
        List<PluginDTO> lists = new LinkedList<>();
        try {
            PluginExample example = new PluginExample();
            if (StringUtils.isNotBlank(name)) {
                name = "%" + name + "%";
                example.createCriteria().andNameLike(name);
            }
            List<Plugin> plugins = pluginMapper.selectByExample(example);
            Map<String, Boolean> pluginMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(plugins)) {
                // 校验插件是否是企业版
                plugins.forEach(item -> {
                    PluginDTO dto = new PluginDTO();
                    BeanUtils.copyBean(dto, item);
                    if (!pluginMap.containsKey(item.getPluginId())) {
                        try {
                            Class<?> clazz = Class.forName(item.getExecEntry());
                            Object instance = clazz.newInstance();
                            dto.setLicense(this.isXpack(Class.forName(item.getExecEntry()), instance));
                        } catch (Exception e) {
                            LogUtil.error(e.getMessage());
                        }
                    } else {
                        dto.setLicense(pluginMap.get(item.getPluginId()));
                    }
                    lists.add(dto);
                    pluginMap.put(item.getPluginId(), dto.getLicense());
                });
                return lists;
            }
        } catch (Exception e) {
            LogUtil.error(e);
        }
        return lists;
    }

    public Plugin get(String scriptId) {
        PluginExample example = new PluginExample();
        example.createCriteria().andScriptIdEqualTo(scriptId);
        List<PluginWithBLOBs> plugins = pluginMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isNotEmpty(plugins)) {
            return plugins.get(0);
        }
        return null;
    }

    public String delete(String id) {
        //通过pluginId判断是否还有其他脚本，无则清理加载的jar包
        PluginExample example = new PluginExample();
        example.createCriteria().andPluginIdEqualTo(id);
        List<Plugin> list = pluginMapper.selectByExample(example);
        if (CollectionUtils.isNotEmpty(list)) {
            FileUtils.deleteFile(list.get(0).getSourcePath());
            pluginMapper.deleteByExample(example);
        }
        return "success";
    }

    public Object customMethod(PluginRequest request) {
        try {
            Class<?> clazz = Class.forName(request.getEntry());
            Object instance = clazz.newInstance();
            Object pluginObj = clazz.getDeclaredMethod("customMethod", String.class).invoke(instance, request.getRequest());
            return pluginObj;
        } catch (Exception ex) {
            LogUtil.error("加载自定义方法失败：" + ex.getMessage());
        }
        return null;
    }

    public List<Plugin> list() {
        PluginExample example = new PluginExample();
        List<Plugin> plugins = pluginMapper.selectByExample(example);
        return plugins;
    }
}
