package org.comroid.varbind.preprocessor;

import org.comroid.annotations.AnnotationProcessor;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("org.comroid.varbind.annotation.RootBind")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class VarBindPreprocessor extends AnnotationProcessor {
    @Override
    public void onAnnotation(TypeElement annotation, Element annotated) {
    }
}
