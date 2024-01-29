package nu.ndw.nls.routingmapmatcher.network.annotations.mappers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.AnnotatedPropertyDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper retrieves {@link EncodedValue} annotations from a class and returns the description of fields as
 * {@link EncodedValuesByTypeDto} objects. Internally uses the {@link AnnotationMapper} to obtain the field meta
 * information, this mapper already enforces that fields must have getter methods to obtain the values.
 *
 * For non-directional fields, the return type is obtained from the getter method. It is allowed for non-generic typed
 * getter methods to return a different type than the field.
 *
 * When a directional {@link DirectionalDto } field is used, the type is obtained from the generic type argument of the
 * field and not from the method. This is the only possible way to get the generic type argument, because java type
 * erasure does not store any generic type information of return methods. Because of this restriction, your getter must
 * return the exact same type as the field.
 *
 * {@link EncodedValuesByTypeDto} uses a {@link Function } that accepts a link and returns the value. Primitive values
 * will be boxes, because functions are using generic types and generic types cannot be primitives.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncodedValuesMapper {

    private static final String DIRECTIONAL_EXCEPTION_MSG =
            "Failed invoke getter method %s to obtain Link DirectionalDto %s value";

    private final AnnotationMapper annotationMapper;
    private final DirectionalFieldGenericTypeArgumentMapper directionalFieldGenericTypeArgumentMapper;

    /**
     *
     * @param returnType
     * @param valueSupplierFunction
     * @param <T>
     * @param <R>
     */
    private record TypeAndValueSupplierFunction<T, R>(Class<R> returnType, Function<T, R> valueSupplierFunction) {

    }

    private record TypeAndValueSupplierFunctionDirectional<T, R>(Class<R> returnType,
                                                                 Function<T, R> valueSupplierForward,
                                                                 Function<T, R> valueSupplierReverse) {
    }

    public <T extends Link> EncodedValuesByTypeDto<T> map(Class<T> annotatedClass) {

        Map<String, AnnotatedPropertyDto<T, EncodedValue>> fieldAnnotatedPropertyDtoMap = annotationMapper
                .resolveAnnotation(EncodedValue.class, annotatedClass);

        EncodedValuesByTypeDto<T> result = new EncodedValuesByTypeDto<>();

        fieldAnnotatedPropertyDtoMap.values()
                .stream()
                .map(propertyDto -> mapDecideDirectional(annotatedClass, propertyDto))
                .forEach(encodedValueDto -> result.add(encodedValueDto.valueType(), encodedValueDto));

        return result;
    }

    private <T extends Link, R> EncodedValueDto<T, R> mapDecideDirectional(Class<T> annotatedClass,
            AnnotatedPropertyDto<T, EncodedValue> dto) {
        if (DirectionalDto.class.equals(dto.getterMethod().getReturnType())) {
            return mapDirectional(annotatedClass, dto);
        } else {
            return mapNonDirectional(dto);
        }
    }

    private <T extends Link, R> EncodedValueDto<T, R> mapDirectional(Class<T> annotatedClass,
            AnnotatedPropertyDto<T, EncodedValue> dto) {

        TypeAndValueSupplierFunctionDirectional<T,R> valueGetterFunctionDirectional =
                createValueGetterFunctionDirectional(annotatedClass, dto);

        return EncodedValueDto.<T, R>builder().key(dto.annotationValue().key()).bits(dto.annotationValue().bits())
                .valueType(valueGetterFunctionDirectional.returnType())
                .valueSupplier(valueGetterFunctionDirectional.valueSupplierForward())
                .valueReverseSupplier(valueGetterFunctionDirectional.valueSupplierReverse()).build();
    }


    private <T extends Link, R> EncodedValueDto<T, R> mapNonDirectional(AnnotatedPropertyDto<T, EncodedValue> dto) {

        TypeAndValueSupplierFunction<T, R> valueGetterFunction = createValueGetterFunction(dto.getterMethod());

        return EncodedValueDto.<T, R>builder()
                .key(dto.annotationValue().key())
                .bits(dto.annotationValue().bits())
                .valueType(valueGetterFunction.returnType())
                .valueSupplier(valueGetterFunction.valueSupplierFunction()).build();
    }

    /**
     *
     * @param annotatedClass
     * @param dto
     * @return
     * @param <T> Link extending class
     * @param <R> Value
     */
    @SuppressWarnings("unchecked")
    private <T extends Link, R> TypeAndValueSupplierFunctionDirectional<T, R> createValueGetterFunctionDirectional(
            Class<T> annotatedClass, AnnotatedPropertyDto<T, EncodedValue> dto) {

        Class<?> directionalGenericType = directionalFieldGenericTypeArgumentMapper.map(annotatedClass,
                dto.propertyName());

        Method getterMethod = dto.getterMethod();
        return new TypeAndValueSupplierFunctionDirectional<>(
                (Class<R>) directionalGenericType,
                link -> getForward(getterMethod, link),
                link -> getReverse(getterMethod, link)
        );

    }

    private <T extends Link, R> R getForward(Method getterMethod, T link) {
        Optional<DirectionalDto<R>> directionalDtoOptional = getDirectionalDto(getterMethod, link, "forward");
        return directionalDtoOptional.map(DirectionalDto::forward).orElse(null);
    }

    private <T extends Link, R> R getReverse(Method getterMethod, T link) {
        Optional<DirectionalDto<R>> directionalDtoOptional = getDirectionalDto(getterMethod, link, "reverse");
        return directionalDtoOptional.map(DirectionalDto::reverse).orElse(null);
    }

    private <T extends Link, R> Optional<DirectionalDto<R>> getDirectionalDto(Method getterMethod, T link,
            String direction) {
        try {
            return Optional.ofNullable((DirectionalDto<R>) getterMethod.invoke(link));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(DIRECTIONAL_EXCEPTION_MSG.formatted(getterMethod.getName(), direction), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Link, R> TypeAndValueSupplierFunction<T, R> createValueGetterFunction(
            Method getterMethod) {
        return new TypeAndValueSupplierFunction<>((Class<R>) boxPrimitive(getterMethod.getReturnType()), link -> {
            try {
                return (R) getterMethod.invoke(link);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(
                        "Failed invoke getter method " + getterMethod.getName() + " to obtain Link " + "value", e);
            }
        });
    }

    /**
     * We need to box primitive class types, because we use a {@link Function } to return the value and they always
     * require you to box a version.
     *
     * @param aClazz
     * @return
     */
    private static Class<?> boxPrimitive(Class<?> aClazz) {
        if (aClazz.isPrimitive()) {
            return ClassUtils.primitiveToWrapper(aClazz);
        }

        return aClazz;
    }
}
