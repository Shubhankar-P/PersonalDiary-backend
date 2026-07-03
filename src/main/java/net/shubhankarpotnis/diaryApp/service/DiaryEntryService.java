package net.shubhankarpotnis.diaryApp.service;

import lombok.extern.slf4j.Slf4j;
import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.DiaryEntryRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DiaryEntryService {

    @Autowired
    private DiaryEntryRepository diaryEntryRepository;

    @Autowired
    private UserService userService;

    // TODO: re-enable after Postgres migration (JPA transactions work fine on
    // single-node Postgres — this only broke on standalone MongoDB, which
    // requires a replica set for multi-document transactions)
    // @Transactional
    public void saveEntry(DiaryEntry diaryEntry, String userName){
          try {
              User user = userService.findByUserName(userName);
              diaryEntry.setDate(LocalDateTime.now());
              DiaryEntry saved = diaryEntryRepository.save(diaryEntry);
              user.getDiaryEntries().add(saved);
              userService.saveUser(user);
          } catch (Exception e){
              log.error("Exception ", e);
          }
    }

    public void saveEntry(DiaryEntry diaryEntry){
        diaryEntryRepository.save(diaryEntry);
    }

//    public List<DiaryEntry> getAllDiaryEntries(){
//        return diaryEntryRepository.findAll();
//    }

    public Optional<DiaryEntry> findById(ObjectId id){
        return diaryEntryRepository.findById(id);
    }

    // TODO: re-enable after Postgres migration (see note above)
  //  @Transactional
    public boolean deleteById(ObjectId id, String userName){
        boolean removed = false;
        try{
            User user = userService.findByUserName(userName);
             removed = user.getDiaryEntries().removeIf(x -> x.getId().equals(id));
            if(removed) {
                userService.saveUser(user);
                diaryEntryRepository.deleteById(id);
            }
        }
        catch(Exception e){
            throw new RuntimeException("An error occurred while deleting the entry.", e);
        }
        return removed;
    }

}

// controller ---> service ---> repository