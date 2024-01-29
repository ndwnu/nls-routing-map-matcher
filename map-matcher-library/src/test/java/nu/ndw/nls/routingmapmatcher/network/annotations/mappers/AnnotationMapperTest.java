package nu.ndw.nls.routingmapmatcher.network.annotations.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import lombok.Getter;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.AnnotatedPropertyDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.AnnotatedSource;
import org.junit.jupiter.api.Test;

class AnnotationMapperTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface CustomAnnotation {

        String value();
    }

    @Getter
    public static class ParentDto {

        @CustomAnnotation(value = "parentField")
        private int parentProperty;

        private int parentMethodAnnotatedProperty;

        @CustomAnnotation(value = "parentMethodAnnotatedProperty")
        public int getParentMethodAnnotatedProperty() {
            return parentMethodAnnotatedProperty;
        }
    }

    @Getter
    public static class ChildDto extends ParentDto {

        @CustomAnnotation(value = "childField")
        private int childProperty;

        private int childMethodAnnotatedProperty;

        @CustomAnnotation(value = "childMethodAnnotatedProperty")
        public int getChildMethodAnnotatedProperty() {
            return childMethodAnnotatedProperty;
        }
    }

    @Test
    @SneakyThrows
    void map_ok_supportsInheritanceWithFieldAndMethodAnnotations() {

        Map<String, AnnotatedPropertyDto<ChildDto, CustomAnnotation>> result = annotationMapper.resolveAnnotation(
                CustomAnnotation.class, ChildDto.class);

        assertTrue(result.containsKey("parentProperty"));
        AnnotatedPropertyDto<ChildDto, CustomAnnotation> parentAnnotated = result.get("parentProperty");
        assertNotNull(parentAnnotated.annotationValue());
        assertEquals("parentField", parentAnnotated.annotationValue().value());
        assertEquals(ParentDto.class.getMethod("getParentProperty"), parentAnnotated.getterMethod());
        assertEquals("parentProperty", parentAnnotated.propertyName());
        assertEquals(AnnotatedSource.FIELD, parentAnnotated.annotatedSource());

        assertTrue(result.containsKey("parentMethodAnnotatedProperty"));
        AnnotatedPropertyDto<ChildDto, CustomAnnotation> parentMethodAnnotated = result.get(
                "parentMethodAnnotatedProperty");
        assertNotNull(parentMethodAnnotated.annotationValue());
        assertEquals("parentMethodAnnotatedProperty", parentMethodAnnotated.annotationValue().value());
        assertEquals(ParentDto.class.getMethod("getParentMethodAnnotatedProperty"),
                parentMethodAnnotated.getterMethod());
        assertEquals("parentMethodAnnotatedProperty", parentMethodAnnotated.propertyName());
        assertEquals(AnnotatedSource.GETTER_METHOD, parentMethodAnnotated.annotatedSource());

        assertTrue(result.containsKey("childProperty"));
        AnnotatedPropertyDto<ChildDto, CustomAnnotation> childAnnotation = result.get("childProperty");
        assertNotNull(childAnnotation.annotationValue());
        assertEquals("childField", childAnnotation.annotationValue().value());
        assertEquals(ChildDto.class.getMethod("getChildProperty"), childAnnotation.getterMethod());
        assertEquals("childProperty", childAnnotation.propertyName());
        assertEquals(AnnotatedSource.FIELD, childAnnotation.annotatedSource());

        assertTrue(result.containsKey("childMethodAnnotatedProperty"));
        AnnotatedPropertyDto<ChildDto, CustomAnnotation> childMethodAnnotated = result.get(
                "childMethodAnnotatedProperty");
        assertNotNull(childMethodAnnotated.annotationValue());
        assertEquals("childMethodAnnotatedProperty", childMethodAnnotated.annotationValue().value());
        assertEquals(ChildDto.class.getMethod("getChildMethodAnnotatedProperty"), childMethodAnnotated.getterMethod());
        assertEquals("childMethodAnnotatedProperty", childMethodAnnotated.propertyName());
        assertEquals(AnnotatedSource.GETTER_METHOD, childMethodAnnotated.annotatedSource());
    }


    @Getter
    public static class ParentOverridenDto {

        private int overridenProperty;

        @CustomAnnotation(value = "parentOverridenProperty")
        public int getOverridenProperty() {
            return overridenProperty;
        }
    }

    public static class ChildOverridenDto extends ParentOverridenDto {

        @CustomAnnotation(value = "childOverridenProperty")
        public int getOverridenProperty() {
            return super.getOverridenProperty();
        }
    }

    private final AnnotationMapper annotationMapper = new AnnotationMapper();

    @Test
    @SneakyThrows
    void map_ok_supportsOverridenMethodAnnotations() {
        Map<String, AnnotatedPropertyDto<ChildOverridenDto, CustomAnnotation>> result = annotationMapper.resolveAnnotation(
                CustomAnnotation.class, ChildOverridenDto.class);

        assertTrue(result.containsKey("overridenProperty"));
        AnnotatedPropertyDto<ChildOverridenDto, CustomAnnotation> overridenProperty = result.get("overridenProperty");
        assertNotNull(overridenProperty);
        assertEquals("childOverridenProperty", overridenProperty.annotationValue().value());
        assertEquals(ChildOverridenDto.class.getMethod("getOverridenProperty"), overridenProperty.getterMethod());
        assertEquals("overridenProperty", overridenProperty.propertyName());
        assertEquals(AnnotatedSource.GETTER_METHOD, overridenProperty.annotatedSource());
    }

    public static class MethodAnnotationHasPriorityDto {

        @CustomAnnotation("fieldAnnotation")
        private double annotatedField;

        @CustomAnnotation("getterMethodAnnotation")
        public double getAnnotatedField() {
            return annotatedField;
        }
    }

    @Test
    @SneakyThrows
    void map_ok_annotationsOnMethodsOverrideFieldAnnotations() {
        Map<String, AnnotatedPropertyDto<MethodAnnotationHasPriorityDto, CustomAnnotation>> map =
                annotationMapper.resolveAnnotation(CustomAnnotation.class, MethodAnnotationHasPriorityDto.class);

        assertNotNull(map);
        assertTrue(map.containsKey("annotatedField"));
        AnnotatedPropertyDto<MethodAnnotationHasPriorityDto, CustomAnnotation> annotatedPropertyDto = map.get(
                "annotatedField");
        assertEquals(MethodAnnotationHasPriorityDto.class.getMethod("getAnnotatedField"),
                annotatedPropertyDto.getterMethod());
        assertEquals(AnnotatedSource.GETTER_METHOD, annotatedPropertyDto.annotatedSource());
        assertEquals("getterMethodAnnotation", annotatedPropertyDto.annotationValue().value());
        assertEquals("annotatedField", annotatedPropertyDto.propertyName());
    }


}