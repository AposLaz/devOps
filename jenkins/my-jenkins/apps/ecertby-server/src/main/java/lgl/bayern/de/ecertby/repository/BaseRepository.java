package lgl.bayern.de.ecertby.repository;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.StringPath;
import lgl.bayern.de.ecertby.model.BaseEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

@Repository
@Primary
public interface BaseRepository<M extends BaseEntity, Q extends EntityPath<M>> extends JpaRepository<M, String>,
    QuerydslPredicateExecutor<M>, QuerydslBinderCustomizer<Q> {

    @Override
    default void customize(QuerydslBindings bindings, Q root) {
        bindings.bind(String.class)
                .first((StringPath path, String value) -> path.containsIgnoreCase(value));
        // Add here any general binding, required for predicates.
    }
    default M partialSave(Map<String, Object> fields, String id, Class type){
        Optional<M> entity = this.findById(id);
        if(entity.isPresent()){
            fields.forEach((key, value) -> {
                Field field = ReflectionUtils.findField( type , key);
                field.setAccessible(true);
                ReflectionUtils.setField(field, entity.get(), value);
            });
            return save(entity.get());
        }

        return null;
    }

    default boolean activate(Boolean activate, String id, Class type){
        Optional<M> entity = this.findById(id);
        if(entity.isPresent()){
            Field field = ReflectionUtils.findField( type , "active");
            field.setAccessible(true);
            ReflectionUtils.setField(field, entity.get(), activate);
            return save(entity.get()) != null;
        }
        return false;
    }

    default boolean release(String id, Class type){
        Optional<M> entity = this.findById(id);
        if(entity.isPresent()){
            Field field = ReflectionUtils.findField( type , "release");
            field.setAccessible(true);
            ReflectionUtils.setField(field, entity.get(), true);
            return save(entity.get()) != null;
        }
        return false;
    }

    default boolean publish(String id, Class type){
        Optional<M> entity = this.findById(id);
        if(entity.isPresent()){
            Field field = ReflectionUtils.findField( type , "published");
            field.setAccessible(true);
            ReflectionUtils.setField(field, entity.get(), true);
            return save(entity.get()) != null;
        }
        return false;
    }

    default boolean delete(String id, Class type){
        Optional<M> entity = this.findById(id);
        if(entity.isPresent()){
            Field field = ReflectionUtils.findField( type , "deleted");
            field.setAccessible(true);
            ReflectionUtils.setField(field, entity.get(), true);
            return save(entity.get()) != null;
        }
        return false;
    }
}
