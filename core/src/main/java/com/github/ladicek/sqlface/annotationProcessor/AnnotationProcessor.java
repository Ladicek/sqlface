package com.github.ladicek.sqlface.annotationProcessor;

import com.github.ladicek.sqlface.Dialect;
import com.github.ladicek.sqlface.Query;
import com.github.ladicek.sqlface.SQL;
import com.github.ladicek.sqlface.Update;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("com.github.ladicek.sqlface.SQL")
public class AnnotationProcessor extends AbstractProcessor {
    private static final String LIST_FQN = "java.util.List";
    private static final String ROW_MAPPER_FQN = "com.github.ladicek.sqlface.RowMapper";

    private TypeMirror listTypeErased;
    private TypeMirror rowMapperTypeErased;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        listTypeErased = processingEnv.getTypeUtils().erasure(
                processingEnv.getElementUtils().getTypeElement(LIST_FQN).asType());
        rowMapperTypeErased = processingEnv.getTypeUtils().erasure(
                processingEnv.getElementUtils().getTypeElement(ROW_MAPPER_FQN).asType());

        Set<? extends Element> interfaces = roundEnv.getRootElements();
        for (Element iface : interfaces) {
            try {
                GeneratedInterface generatedInterface = scanInterface(iface);
            } catch (CantGenerateException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.message, e.element);
            }
        }
        return false;
    }

    private GeneratedInterface scanInterface(Element iface) throws CantGenerateException {
        SQL sql = iface.getAnnotation(SQL.class);
        Dialect dialect = sql.dialect();

        boolean wasError = false;
        List<GeneratedMethod> generatedMethods = new ArrayList<>();
        for (ExecutableElement method : ElementFilter.methodsIn(Collections.singletonList(iface))) {
            try {
                generatedMethods.add(scanMethod(method));
            } catch (CantGenerateException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.message, e.element);
                wasError = true;
            }
        }

        if (wasError) {
            throw new CantGenerateException("Can't generate implementation, there were errors", iface);
        }

        PackageElement interfacePackage = processingEnv.getElementUtils().getPackageOf(iface);
        return new GeneratedInterface(interfacePackage.getQualifiedName().toString(), iface.getSimpleName().toString(),
                generatedMethods);
    }

    private GeneratedMethod scanMethod(ExecutableElement method) throws CantGenerateException {
        Query queryAnnotation = method.getAnnotation(Query.class);
        Update updateAnnotation = method.getAnnotation(Update.class);

        if (queryAnnotation == null && updateAnnotation == null) {
            throw new CantGenerateException("@Query or @Update required", method);
        }

        if (queryAnnotation != null && updateAnnotation != null) {
            throw new CantGenerateException("Only one of @Query or @Update", method);
        }

        if (queryAnnotation != null) {
            return scanQueryMethod(method);
        } else {
            return scanUpdateMethod(method);
        }
    }

    private QueryMethod scanQueryMethod(ExecutableElement method) throws CantGenerateException {
        Query queryAnnotation = method.getAnnotation(Query.class);
        String sql = queryAnnotation.value();
        int numberOfParameters = numberOfParameters(sql);
        int requiredMethodParameters = numberOfParameters + 1;
        List<? extends VariableElement> methodParameters = method.getParameters();
        if (methodParameters.size() != requiredMethodParameters) {
            if (numberOfParameters > 0) {
                throw new CantGenerateException("@Query method must have " + requiredMethodParameters
                        + " parameters, " + numberOfParameters + " for SQL parameters and the last one for "
                        + "a RowMapper", method);
            } else {
                throw new CantGenerateException("@Query method without SQL parameters must have 1 parameter for "
                        + "a RowMapper", method);
            }
        }

        VariableElement lastParam = methodParameters.get(methodParameters.size() - 1);
        TypeMirror lastParamType = lastParam.asType();
        TypeMirror lastParamTypeErased = processingEnv.getTypeUtils().erasure(lastParamType);
        if (lastParamType.getKind() != TypeKind.DECLARED
                || !processingEnv.getTypeUtils().isSameType(lastParamTypeErased, rowMapperTypeErased)) {
            throw new CantGenerateException("Last parameter of a @Query method must be a RowMapper", lastParam);
        }

        List<? extends TypeMirror> lastParamTypeArguments = ((DeclaredType) lastParamType).getTypeArguments();
        if (lastParamTypeArguments.size() == 0) {
            throw new CantGenerateException("RowMapper must have a type argument (RowMapper<T>)", lastParam);
        } else if (lastParamTypeArguments.size() > 1) {
            throw new CantGenerateException("RowMapper must have exactly 1 type argument (RowMapper<T>)", lastParam);
        }
        TypeMirror rowMapperTypeArgument = lastParamTypeArguments.get(0);

        TypeMirror returnType = method.getReturnType();
        TypeMirror returnTypeErased = processingEnv.getTypeUtils().erasure(returnType);
        if (returnType.getKind() != TypeKind.DECLARED) {
            throw new CantGenerateException("@Query method must return either " + rowMapperTypeArgument
                    + " or List<" + rowMapperTypeArgument + ">", method);
        }

        boolean singleResult = processingEnv.getTypeUtils().isSameType(returnType, rowMapperTypeArgument);
        boolean multiResult = processingEnv.getTypeUtils().isSameType(listTypeErased, returnTypeErased);
        if (!singleResult && !multiResult) {
            throw new CantGenerateException("@Query method must return either " + rowMapperTypeArgument
                    + " or List<" + rowMapperTypeArgument + ">", method);
        }
        if (multiResult) {
            List<? extends TypeMirror> returnTypeTypeArguments = ((DeclaredType) returnType).getTypeArguments();
            if (returnTypeTypeArguments.size() != 1) {
                throw new CantGenerateException("@Query method must return either " + rowMapperTypeArgument
                        + " or List<" + rowMapperTypeArgument + ">", method);
            }
            TypeMirror returnTypeTypeArgument = returnTypeTypeArguments.get(0);
            if (!processingEnv.getTypeUtils().isSameType(returnTypeTypeArgument, rowMapperTypeArgument)) {
                throw new CantGenerateException("@Query method must return either " + rowMapperTypeArgument
                        + " or List<" + rowMapperTypeArgument + ">", method);
            }
        }

        return new QueryMethod(method.getSimpleName().toString(), methodParameters(method), sql, returnType,
                singleResult ? QueryMethod.Kind.SINGLE_RESULT : QueryMethod.Kind.MULTI_RESULT);
    }

    private UpdateMethod scanUpdateMethod(ExecutableElement method) throws CantGenerateException {
        Update updateAnnotation = method.getAnnotation(Update.class);
        String sql = updateAnnotation.value();
        int numberOfParameters = numberOfParameters(sql);
        List<? extends VariableElement> methodParameters = method.getParameters();

        if (methodParameters.size() != numberOfParameters) {
            if (numberOfParameters > 0) {
                throw new CantGenerateException("@Update method must have " + numberOfParameters
                        + " parameters for SQL parameters", method);
            } else {
                throw new CantGenerateException("@Update method without SQL parameters must not have parameters",
                        method);
            }
        }

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            throw new CantGenerateException("@Update method must return void", method);
        }

        return new UpdateMethod(method.getSimpleName().toString(), methodParameters(method), sql);
    }

    private int numberOfParameters(String sql) {
        int result = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?') {
                result++;
            }
        }
        return result;
    }

    private List<MethodParameter> methodParameters(ExecutableElement method) {
        List<MethodParameter> result = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            result.add(new MethodParameter(parameter.asType(), parameter.getSimpleName().toString()));
        }
        return result;
    }
}
