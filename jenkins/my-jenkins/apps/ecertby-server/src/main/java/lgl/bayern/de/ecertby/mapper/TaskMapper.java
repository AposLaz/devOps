package lgl.bayern.de.ecertby.mapper;

import jakarta.annotation.Resource;
import lgl.bayern.de.ecertby.dto.OptionDTO;
import lgl.bayern.de.ecertby.dto.TaskDTO;
import lgl.bayern.de.ecertby.model.CatalogValue;
import lgl.bayern.de.ecertby.model.Task;
import lgl.bayern.de.ecertby.model.QTask;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class TaskMapper extends BaseMapper<TaskDTO, Task, QTask> {
    @Resource(name = "messages")
    private Map<String, String> messages;

    @Mapping(source="product.id", target = "id")
    @Mapping(source="product.data", target = "name")
    public abstract OptionDTO catalogValueToOptionDTO(CatalogValue product);

    @AfterMapping
    public void checkReason(@MappingTarget TaskDTO taskDTO) {
        if (taskDTO.getReason() != null) {
            taskDTO.setInfo(taskDTO.getInfo() + " \n" + messages.get("rejection_reason") + ": " + taskDTO.getReason());
        }
    }
}
