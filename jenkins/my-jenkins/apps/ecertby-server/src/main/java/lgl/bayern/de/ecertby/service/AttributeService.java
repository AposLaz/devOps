package lgl.bayern.de.ecertby.service;

import com.eurodyn.qlack.common.exception.QCouldNotSaveException;
import lgl.bayern.de.ecertby.dto.AttributeDTO;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.exception.EcertBYErrorException;
import lgl.bayern.de.ecertby.exception.EcertBYGeneralException;
import lgl.bayern.de.ecertby.mapper.AttributeMapper;
import lgl.bayern.de.ecertby.mapper.UserDetailMapper;
import lgl.bayern.de.ecertby.model.Attribute;
import lgl.bayern.de.ecertby.model.AttributeRadioOption;
import lgl.bayern.de.ecertby.model.QAttribute;
import lgl.bayern.de.ecertby.model.util.AuditAction;
import lgl.bayern.de.ecertby.model.util.ObjectType;
import lgl.bayern.de.ecertby.repository.AttributeRepository;
import lgl.bayern.de.ecertby.validator.AttributeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static lgl.bayern.de.ecertby.config.AppConstants.LOG_PREFIX;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class AttributeService extends BaseService<AttributeDTO, Attribute, QAttribute> {
    AttributeMapper attributeMapper = Mappers.getMapper(AttributeMapper.class);
    UserDetailMapper userMapperInstance = Mappers.getMapper(UserDetailMapper.class);

    private final ObjectLockService objectLockService;
    private final AuditService auditService;
    private final SecurityService securityService;
    private final AttributeRepository attributeRepository;
    private final AttributeValidator attributeValidator;

    public AttributeDTO saveAttribute(AttributeDTO attributeDTO) {
        objectLockService.checkAndThrowIfLocked(attributeDTO.getId(), ObjectType.NOTIFICATION);
        attributeValidator.validateAttribute(attributeDTO);

        AttributeDTO oldAttributeDTO = null;
        if (attributeDTO.getId() != null) {
            oldAttributeDTO = findById(attributeDTO.getId());
        }

        Attribute attributeEntity = attributeMapper.map(attributeDTO);
        for (AttributeRadioOption radioOption : attributeEntity.getRadioOptionList()) {
            if (radioOption.getAttribute() == null) {
                radioOption.setAttribute(attributeEntity);
            }
        }

        AttributeDTO savedAttributeDTO = attributeMapper.map(attributeRepository.save(attributeEntity));
        if (attributeDTO.getId() == null) {
            log.info(LOG_PREFIX + "New attribute with id {} successfully created by user with id : {}.", savedAttributeDTO.getId(), securityService.getLoggedInUserDetailId());
            auditService.saveAttributeAudit(AuditAction.CREATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), savedAttributeDTO);
        } else {
            log.info(LOG_PREFIX + "Attribute with id {} successfully updated by user with id : {}.", savedAttributeDTO.getId(), securityService.getLoggedInUserDetailId());
            auditService.saveAttributeAudit(AuditAction.UPDATE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()),
                    attributeDTO.getName(), oldAttributeDTO, savedAttributeDTO);
        }
        return savedAttributeDTO;
    }

    public void deleteAttribute(String id) {
        AttributeDTO attributeDTO = findById(id);
        log.info(LOG_PREFIX + "Deleting attribute...");
        objectLockService.checkAndThrowIfLocked(id, ObjectType.ATTRIBUTE);

        List<EcertBYErrorException> errors = new ArrayList<>();
        attributeValidator.validateDelete(id, errors);
        if (!errors.isEmpty()) {
            throw new QCouldNotSaveException("Errors for deleting attribute.", new EcertBYGeneralException(errors));
        }
        AttributeDTO deletedAttributeDTO = deleteById(id);

        // LOG DELETION
        auditService.saveAttributeAudit(AuditAction.DELETE, userMapperInstance.map(securityService.getLoggedInUserDetailDTO()), attributeDTO);
        log.info(LOG_PREFIX + "Attribute with id {} successfully deleted by user with id : {}",
                deletedAttributeDTO.getId(),
                securityService.getLoggedInUserDetailId());
    }

    public List<OptionDTO> getAttributeRadioOptionList() {
        return null;
    }

    public List<OptionDTO> getAllAttributeOptionDTO() {
        List<Attribute> attributeList = attributeRepository.findAll();
        List<OptionDTO> optionDTOList = attributeList.stream().map(o ->
                attributeMapper.attributeToOptionDTO(o)).collect(Collectors.toList());
        return optionDTOList;
    }
}
