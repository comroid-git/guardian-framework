package org.comroid.varbind.preprocessor;

import org.comroid.annotations.AnnotationProcessor;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes("org.comroid.varbind.annotation.RootBind")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class VarBindPreprocessor extends AnnotationProcessor {
}
