package net.shubhankarpotnis.diaryApp.cache;

import net.shubhankarpotnis.diaryApp.entity.ConfigDiaryAppEntity;
import net.shubhankarpotnis.diaryApp.repository.ConfigDiaryAppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AppCache {

    @Autowired
    private ConfigDiaryAppRepository configDiaryAppRepository;

    public Map<String, String> APP_CACHE;

    @PostConstruct
    public void init(){
        APP_CACHE = new HashMap<>();
        List<ConfigDiaryAppEntity> all = configDiaryAppRepository.findAll();
        for(ConfigDiaryAppEntity configDiaryAppEntity : all){
            APP_CACHE.put(configDiaryAppEntity.getKey(), configDiaryAppEntity.getValue());
        }
    }
}
