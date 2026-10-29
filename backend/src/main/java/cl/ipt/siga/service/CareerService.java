package cl.ipt.siga.service;

import cl.ipt.siga.entity.CareerEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.DuplicateResourceException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
public class CareerService {

    private final CareerRepository careerRepository;
    public CareerService(CareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

    public List<CareerEntity> getAll(){
        return careerRepository.findAll();
    }


    public CareerEntity getById(Long id){
        return careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find a career with ID: "+ id));
    }

    public CareerEntity create(CareerEntity careerEntity) {
        if (careerRepository.existsByCode(careerEntity.getCode())) {
            throw new DuplicateResourceException(
                    "A career with code " + careerEntity.getCode() + " already exists");
        }
        careerEntity.setId(null);   // a create must never overwrite an existing career
        return careerRepository.save(careerEntity);
    }

    public CareerEntity update(Long id, CareerEntity data) {
        CareerEntity careerEntity = getById(id);
        // career code cant change
        if (!Objects.equals(careerEntity.getCode(), data.getCode())) {
            throw new BusinessRuleException("The career code cannot be modified");
        }
        careerEntity.setName(data.getName());
        careerEntity.setDescription(data.getDescription());
        careerEntity.setStatus(data.getStatus());
        return careerRepository.save(careerEntity);
    }


}
