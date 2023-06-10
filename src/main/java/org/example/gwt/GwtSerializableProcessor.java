package org.example.gwt;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("org.example.gwt.GwtSerializable")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class GwtSerializableProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Types typeUtils = processingEnv.getTypeUtils();
            Elements elementUtils = processingEnv.getElementUtils();
            Messager messager = processingEnv.getMessager();
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            TypeMirror serializableTypeMirror = elementUtils.getTypeElement(Serializable.class.getName()).asType();
            for (Element annotatedElement : annotatedElements) {
                validateSerializable(annotatedElement, serializableTypeMirror, typeUtils, messager);
                validateDefaultConstructor(annotatedElement, messager);
                validateNonFinalFields(messager, annotatedElement);
            }
        }
        return true;
    }

    private static void validateSerializable(Element annotatedElement, TypeMirror serializableTypeMirror, Types typeUtils, Messager messager) {
        boolean isSerializable = typeUtils.isAssignable(annotatedElement.asType(), serializableTypeMirror);
        if (!isSerializable) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Gwt serializable class must be serializable", annotatedElement);
        }
    }

    private static void validateDefaultConstructor(Element annotatedElement, Messager messager) {
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(annotatedElement.getEnclosedElements());
        // No constructors in class => there is a default constructor
        if (constructors.stream().noneMatch(constructor -> constructor.getParameters().isEmpty())) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Gwt serializable class must have a default constructor", annotatedElement);
        }
    }

    private static void validateNonFinalFields(Messager messager, Element annotatedElement) {
        List<? extends Element> fields = getNonStaticFields(annotatedElement);
        for (Element field : fields) {
            Set<Modifier> modifiers = field.getModifiers();
            if (modifiers.contains(Modifier.FINAL)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Gwt serializable class fields cannot be final", field);
            }
        }

//        List<? extends Element> superClasses = getAllSuperClasses(typeUtils, annotatedElement);
//        for (Element superClass : superClasses) {
//            List<? extends Element> superFields = getNonStaticFields(superClass);
//            for (Element field : superFields) {
//                Set<Modifier> modifiers = field.getModifiers();
//                if (modifiers.contains(Modifier.FINAL)) {
//                    messager.printMessage(Diagnostic.Kind.ERROR,
//                            "Gwt serializable super class fields cannot be final. Found in " + superClass.getSimpleName(), annotatedElement);
//                }
//            }
//        }
    }

    private static List<? extends Element> getNonStaticFields(Element annotatedElement) {
        return ElementFilter.fieldsIn(annotatedElement.getEnclosedElements()).stream()
                .filter(element -> !element.getModifiers().contains(Modifier.STATIC))
                .toList();
    }

//    private static List<? extends Element> getAllSuperClasses(Types typeUtils, Element annotatedElement) {
//        List<Element> superClasses = new ArrayList<>();
//        List<? extends TypeMirror> superTypes = typeUtils.directSupertypes(annotatedElement.asType());
//        while (!superTypes.isEmpty()) {
//            List<TypeMirror> nextSuperTypes = new ArrayList<>();
//            for (TypeMirror superType : superTypes) {
//                if (!(superType instanceof DeclaredType)) {
//                    continue;
//                }
//                Element superElement = ((DeclaredType) superType).asElement();
//                if (!superElement.getKind().isClass()) {
//                    continue;
//                }
//                nextSuperTypes.addAll(typeUtils.directSupertypes(superType));
//                superClasses.add(superElement);
//            }
//            superTypes = nextSuperTypes;
//        }
//        return superClasses;
//    }
}
