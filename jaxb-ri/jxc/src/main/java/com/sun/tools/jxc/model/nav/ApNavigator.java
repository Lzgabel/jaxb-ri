/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.tools.jxc.model.nav;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import cn.glassfish.jaxb.core.v2.model.nav.Navigator;
import cn.glassfish.jaxb.core.v2.runtime.Location;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;

/**
 * {@link Navigator} implementation for annotation processing.
 * TODO: check the spec on how generics are supposed to be handled
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class ApNavigator implements Navigator<TypeMirror, TypeElement, VariableElement, ExecutableElement> {

    private final ProcessingEnvironment env;

    private final PrimitiveType primitiveByte;

    public ApNavigator(ProcessingEnvironment env) {
        this.env = env;
        this.primitiveByte = env.getTypeUtils().getPrimitiveType(TypeKind.BYTE);
    }

    @Override
    public TypeElement getSuperClass(TypeElement typeElement) {
        if (typeElement.getKind().equals(ElementKind.CLASS)) {
            TypeMirror sup = typeElement.getSuperclass();
            if (!sup.getKind().equals(TypeKind.NONE))
                return (TypeElement) ((DeclaredType) sup).asElement();
            else
                return null;
        }
        return env.getElementUtils().getTypeElement(Object.class.getName());
    }

    @Override
    public TypeMirror getBaseClass(TypeMirror type, TypeElement sup) {
        return baseClassFinder.visit(type, sup);
    }

    @Override
    public String getClassName(TypeElement t) {
        return t.getQualifiedName().toString();
    }

    @Override
    public String getTypeName(TypeMirror typeMirror) {
        return typeMirror.toString();
    }

    @Override
    public String getClassShortName(TypeElement t) {
        return t.getSimpleName().toString();
    }

    @Override
    public Collection<VariableElement> getDeclaredFields(TypeElement typeElement) {
        return ElementFilter.fieldsIn(typeElement.getEnclosedElements());
    }

    @Override
    public VariableElement getDeclaredField(TypeElement clazz, String fieldName) {
        for (VariableElement fd : ElementFilter.fieldsIn(clazz.getEnclosedElements())) {
            if (fd.getSimpleName().toString().equals(fieldName))
                return fd;
        }
        return null;
    }

    @Override
    public Collection<ExecutableElement> getDeclaredMethods(TypeElement typeElement) {
        return ElementFilter.methodsIn(typeElement.getEnclosedElements());
    }

    @Override
    public TypeElement getDeclaringClassForField(VariableElement f) {
        return (TypeElement) f.getEnclosingElement();
    }

    @Override
    public TypeElement getDeclaringClassForMethod(ExecutableElement m) {
        return (TypeElement) m.getEnclosingElement();
    }

    @Override
    public TypeMirror getFieldType(VariableElement f) {
        return f.asType();
    }

    @Override
    public String getFieldName(VariableElement f) {
        return f.getSimpleName().toString();
    }

    @Override
    public String getMethodName(ExecutableElement m) {
        return m.getSimpleName().toString();
    }

    @Override
    public TypeMirror getReturnType(ExecutableElement m) {
        return m.getReturnType();
    }

    @Override
    public TypeMirror[] getMethodParameters(ExecutableElement m) {
        Collection<? extends VariableElement> ps = m.getParameters();
        TypeMirror[] r = new TypeMirror[ps.size()];
        int i=0;
        for (VariableElement p : ps)
            r[i++] = p.asType();
        return r;
    }

    @Override
    public boolean isStaticMethod(ExecutableElement m) {
        return hasModifier(m, Modifier.STATIC);
    }

    @Override
    public boolean isFinalMethod(ExecutableElement m) {
        return hasModifier(m, Modifier.FINAL);
    }

    private boolean hasModifier(Element d, Modifier mod) {
        return d.getModifiers().contains(mod);
    }

    @Override
    public boolean isSubClassOf(TypeMirror sub, TypeMirror sup) {
        if(sup==DUMMY)
        // see ref(). if the sub type is known to Annotation Processing,
        // its base class must be known. Thus if the sup is DUMMY,
        // it cannot possibly be the super type.
            return false;
        return env.getTypeUtils().isSubtype(sub,sup);
    }

    private String getSourceClassName(Class<?> clazz) {
        Class<?> d = clazz.getDeclaringClass();
        if(d==null)
            return clazz.getName();
        else {
            String shortName = clazz.getName().substring(d.getName().length()+1/*for $*/);
            return getSourceClassName(d)+'.'+shortName;
        }
    }

    @Override
    public TypeMirror ref(Class<?> c) {
        if(c.isArray())
            return env.getTypeUtils().getArrayType( ref(c.getComponentType()) );
        if(c.isPrimitive())
            return getPrimitive(c);
        TypeElement t = env.getElementUtils().getTypeElement(getSourceClassName(c));
        // Annotation Processing only operates on a set of classes used in the compilation,
        // and it won't recognize additional classes (even if they are visible from javac)
        // and return null.
        //
        // this is causing a problem where we check if a type is collection.
        // so until the problem is fixed in Annotation Processing, work around the issue
        // by returning a dummy token
        // TODO: check if this is still valid
        if(t==null)
            return DUMMY;
        return env.getTypeUtils().getDeclaredType(t);
    }

    @Override
    public TypeMirror use(TypeElement t) {
        assert t != null;
        return env.getTypeUtils().getDeclaredType(t);
    }

    @Override
    public TypeElement asDecl(TypeMirror m) {
        m = env.getTypeUtils().erasure(m);
        if (m.getKind().equals(TypeKind.DECLARED)) {
            DeclaredType d = (DeclaredType) m;
            return (TypeElement) d.asElement();
        } else
            return null;
    }

    @Override
    public TypeElement asDecl(Class<?> c) {
        return env.getElementUtils().getTypeElement(getSourceClassName(c));
    }

    @Override
    public TypeMirror erasure(TypeMirror t) {
        Types tu = env.getTypeUtils();
        t = tu.erasure(t);
        if (t.getKind().equals(TypeKind.DECLARED)) {
            DeclaredType dt = (DeclaredType)t;
            if (!dt.getTypeArguments().isEmpty())
                return tu.getDeclaredType((TypeElement) dt.asElement());
        }
        if (isPrimitive(t) && !t.getAnnotationMirrors().isEmpty()) {
            return tu.getPrimitiveType(t.getKind());
        }
        return t;
    }

    @Override
    public boolean isAbstract(TypeElement clazz) {
        return hasModifier(clazz,Modifier.ABSTRACT);
    }

    @Override
    public boolean isFinal(TypeElement clazz) {
        return hasModifier(clazz, Modifier.FINAL);
    }

    @Override
    public VariableElement[] getEnumConstants(TypeElement clazz) {
        List<? extends Element> elements = env.getElementUtils().getAllMembers(clazz);
        Collection<VariableElement> constants = new ArrayList<VariableElement>();
        for (Element element : elements) {
            if (element.getKind().equals(ElementKind.ENUM_CONSTANT)) {
                constants.add((VariableElement) element);
            }
        }
        return constants.toArray(new VariableElement[constants.size()]);
    }

    @Override
    public TypeMirror getVoidType() {
        return env.getTypeUtils().getNoType(TypeKind.VOID);
    }

    @Override
    public String getPackageName(TypeElement clazz) {
        return env.getElementUtils().getPackageOf(clazz).getQualifiedName().toString();
    }

    @Override
    public TypeElement loadObjectFactory(TypeElement referencePoint, String packageName) {
        return env.getElementUtils().getTypeElement(packageName + ".ObjectFactory");
    }

    @Override
    public boolean isBridgeMethod(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.VOLATILE);
    }

    @Override
    public boolean isOverriding(ExecutableElement method, TypeElement base) {
        Elements elements = env.getElementUtils();

        while (true) {
            for (ExecutableElement m : ElementFilter.methodsIn(elements.getAllMembers(base))) {
                if (elements.overrides(method, m, base))
                    return true;
            }

            if (base.getSuperclass().getKind().equals(TypeKind.NONE))
                return false;
            base = (TypeElement) env.getTypeUtils().asElement(base.getSuperclass());
        }
    }

    @Override
    public boolean isInterface(TypeElement clazz) {
        return clazz.getKind().isInterface();
    }

    @Override
    public boolean isTransient(VariableElement f) {
        return f.getModifiers().contains(Modifier.TRANSIENT);
    }

    @Override
    public boolean isInnerClass(TypeElement clazz) {
        return clazz.getEnclosingElement() != null && !clazz.getModifiers().contains(Modifier.STATIC);
    }

    @Override
    public boolean isSameType(TypeMirror t1, TypeMirror t2) {
        return env.getTypeUtils().isSameType(t1, t2);
    }

    @Override
    public boolean isArray(TypeMirror type) {
        return type != null && type.getKind().equals(TypeKind.ARRAY);
    }

    @Override
    public boolean isArrayButNotByteArray(TypeMirror t) {
        if(!isArray(t))
            return false;

        ArrayType at = (ArrayType) t;
        TypeMirror ct = at.getComponentType();

        return !ct.equals(primitiveByte);
    }

    @Override
    public TypeMirror getComponentType(TypeMirror t) {
        if (isArray(t)) {
            ArrayType at = (ArrayType) t;
            return at.getComponentType();
        }

        throw new IllegalArgumentException();
    }

    @Override
    public TypeMirror getTypeArgument(TypeMirror typeMirror, int i) {
        if (typeMirror != null && typeMirror.getKind().equals(TypeKind.DECLARED)) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            TypeMirror[] args = declaredType.getTypeArguments().toArray(new TypeMirror[declaredType.getTypeArguments().size()]);
            return args[i];
        } else throw new IllegalArgumentException();
    }

    @Override
    public boolean isParameterizedType(TypeMirror typeMirror) {
        if (typeMirror != null && typeMirror.getKind().equals(TypeKind.DECLARED)) {
            DeclaredType d = (DeclaredType) typeMirror;
            return !d.getTypeArguments().isEmpty();
        }
        return false;
    }

    @Override
    public boolean isPrimitive(TypeMirror t) {
        return t.getKind().isPrimitive();
    }

    private static final Map<Class<?>, TypeKind> primitives = new HashMap<>();

    static {
        primitives.put(Integer.TYPE, TypeKind.INT);
        primitives.put(Byte.TYPE, TypeKind.BYTE);
        primitives.put(Float.TYPE, TypeKind.FLOAT);
        primitives.put(Boolean.TYPE, TypeKind.BOOLEAN);
        primitives.put(Short.TYPE, TypeKind.SHORT);
        primitives.put(Long.TYPE, TypeKind.LONG);
        primitives.put(Double.TYPE, TypeKind.DOUBLE);
        primitives.put(Character.TYPE, TypeKind.CHAR);
    }

    @Override
    public TypeMirror getPrimitive(Class<?> primitiveType) {
        assert primitiveType.isPrimitive();
        if(primitiveType==void.class)
            return getVoidType();
        return env.getTypeUtils().getPrimitiveType(primitives.get(primitiveType));
    }

    /**
     * see {@link #ref(Class)}.
     */
    private static final TypeMirror DUMMY = new TypeMirror() {
        @Override
        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            throw new IllegalStateException();
        }

        @Override
        public TypeKind getKind() {
            throw new IllegalStateException();
        }

//        @Override
        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            throw new IllegalStateException();
        }

//        @Override
        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            throw new IllegalStateException();
        }

//        @Override
        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            throw new IllegalStateException();
        }
    };

    @Override
    public Location getClassLocation(TypeElement typeElement) {
        Trees trees = Trees.instance(env);
        return getLocation(typeElement.getQualifiedName().toString(), trees.getPath(typeElement));
    }

    @Override
    public Location getFieldLocation(VariableElement variableElement) {
        return getLocation(variableElement);
    }

    @Override
    public Location getMethodLocation(ExecutableElement executableElement) {
        return getLocation(executableElement);
    }

    @Override
    public boolean hasDefaultConstructor(TypeElement t) {
        if (t == null || !t.getKind().equals(ElementKind.CLASS))
            return false;

        for (ExecutableElement init : ElementFilter.constructorsIn(env.getElementUtils().getAllMembers(t))) {
            if (init.getParameters().isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public boolean isStaticField(VariableElement f) {
        return hasModifier(f,Modifier.STATIC);
    }

    @Override
    public boolean isPublicMethod(ExecutableElement m) {
        return hasModifier(m,Modifier.PUBLIC);
    }

    @Override
    public boolean isPublicField(VariableElement f) {
        return hasModifier(f,Modifier.PUBLIC);
    }

    @Override
    public boolean isEnum(TypeElement t) {
        return t != null && t.getKind().equals(ElementKind.ENUM);
    }

    private Location getLocation(Element element) {
        Trees trees = Trees.instance(env);
        return getLocation(
                ((TypeElement) element.getEnclosingElement()).getQualifiedName() + "." + element.getSimpleName(),
                trees.getPath(element)
        );
    }

    private Location getLocation(final String name, final TreePath treePath) {
        return new Location() {
            public String toString() {
                if (treePath == null)
                    return name + " (Unknown Source)";
                // just like stack trace, we just print the file name and
                // not the whole path. The idea is that the package name should
                // provide enough clue on which directory it lives.
                CompilationUnitTree compilationUnit = treePath.getCompilationUnit();
                Trees trees = Trees.instance(env);
                long startPosition = trees.getSourcePositions().getStartPosition(compilationUnit, treePath.getLeaf());
                return name + "(" +
                        compilationUnit.getSourceFile().getName() + ":" + compilationUnit.getLineMap().getLineNumber(startPosition) +
                        ")";
            }
        };
    }

    /**
     * Implements {@link #getBaseClass}.
     */
    private final SimpleTypeVisitor8<TypeMirror, TypeElement> baseClassFinder = new SimpleTypeVisitor8<TypeMirror, TypeElement>() {
        @Override
        public TypeMirror visitDeclared(DeclaredType t, TypeElement sup) {
            if (t.asElement().equals(sup))
                return t;

            for (TypeMirror i : env.getTypeUtils().directSupertypes(t)) {
                TypeMirror r = visitDeclared((DeclaredType) i, sup);
                if (r != null)
                    return r;
            }

            // otherwise recursively apply super class and base types
            TypeMirror superclass = ((TypeElement) t.asElement()).getSuperclass();
            if (!superclass.getKind().equals(TypeKind.NONE)) {
                TypeMirror r = visitDeclared((DeclaredType) superclass, sup);
                if (r != null)
                    return r;
            }
            return null;
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable t, TypeElement typeElement) {
            // we are checking if T (declared as T extends A&B&C) is assignable to sup.
            // so apply bounds recursively.
            for (TypeMirror typeMirror : ((TypeParameterElement) t.asElement()).getBounds()) {
                TypeMirror m = visit(typeMirror, typeElement);
                if (m != null)
                    return m;
            }
            return null;
        }

        @Override
        public TypeMirror visitArray(ArrayType t, TypeElement typeElement) {
            // we are checking if t=T[] is assignable to sup.
            // the only case this is allowed is sup=Object,
            // and Object isn't parameterized.
            return null;
        }

        @Override
        public TypeMirror visitWildcard(WildcardType t, TypeElement typeElement) {
            // we are checking if T (= ? extends A&B&C) is assignable to sup.
            // so apply bounds recursively.
            return visit(t.getExtendsBound(), typeElement);
        }

        @Override
        protected TypeMirror defaultAction(TypeMirror e, TypeElement typeElement) {
            return e;
        }
    };
}

