package cl.ipt.siga.service;

import cl.ipt.siga.entity.CareerEntity;
import cl.ipt.siga.exception.BusinessRuleException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.CareerRepository;
import org.springframework.stereotype.Service;

import java.util.List;


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

    public CareerEntity create(CareerEntity careerEntity){
        if(!careerRepository.existsById(careerEntity.getId())){
            return careerRepository.save(careerEntity);
        }
        else{
            throw new BusinessRuleException("Career already exists!");
        }
    }

    public CareerEntity update(Long id, CareerEntity data){
        CareerEntity careerEntity = getById(id);
        if(careerEntity.getCode() != data.getCode()){
            throw new BusinessRuleException("It is not possible to modify a career code");
        }
        careerEntity.setName(data.getName());
        careerEntity.setDescription(data.getDescription());
        careerEntity.setStatus(data.getStatus());

        return careerRepository.save(careerEntity);
    }


}
