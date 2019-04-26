package com.utzcoz.robolectric.junitparams;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.internal.SandboxTestRunner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import junitparams.internal.ParameterisedTestClassRunner;
import junitparams.internal.ParameterisedTestMethodRunner;
import junitparams.internal.TestMethod;

public class RobolectricJUnitParamsTestRunner extends RobolectricTestRunner {
    private Set<FrameworkMethod> mParameterisedMethods = new HashSet<>();
    private Map<FrameworkMethod, ParameterisedTestMethodRunnerWrapper>
            mParameterisedTestMethodRunners = new HashMap<>();

    public RobolectricJUnitParamsTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        ParameterisedTestClassRunner parameterisedTestClassRunner =
                new ParameterisedTestClassRunner(getTestClass());
        mParameterisedMethods.addAll(
                parameterisedTestClassRunner
                        .computeFrameworkMethods()
                        .stream()
                        .filter(
                                method ->
                                        TestMethodWrapper
                                                .isTestMethodParameterised(method, getTestClass())
                        ).collect(Collectors.toSet())
        );
        mParameterisedMethods.forEach(
                method -> {
                    if (!mParameterisedTestMethodRunners.containsKey(method)) {
                        mParameterisedTestMethodRunners.put(
                                method,
                                new ParameterisedTestMethodRunnerWrapper(
                                        new TestMethod(method, getTestClass())
                                )
                        );
                    }
                }
        );
    }

    @Override
    protected void validatePublicVoidNoArgMethods(Class<? extends Annotation> annotation,
                                                  boolean isStatic,
                                                  List<Throwable> errors) {
        if (shouldValidatePublicVoidNoArgMethods(annotation, isStatic)) {
            super.validatePublicVoidNoArgMethods(annotation, isStatic, errors);
        }
    }

    @Override
    protected Description describeChild(FrameworkMethod method) {
        if (!TestMethodWrapper.isTestMethodParameterised(method, getTestClass())) {
            return super.describeChild(method);
        }
        Description description = null;
        for (FrameworkMethod parameterisedMethod : mParameterisedMethods) {
            if (parameterisedMethod.getName().equals(method.getName())) {
                ParameterisedTestMethodRunnerWrapper wrapper =
                        mParameterisedTestMethodRunners.get(parameterisedMethod);
                assert wrapper != null;
                description = wrapper.getCurrentTestDescription();
                break;
            }
        }
        return description == null ? super.describeChild(method) : description;
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> originTestMethods = new ArrayList<>(super.computeTestMethods());
        List<TestMethodWrapper> methodWrappers =
                originTestMethods
                        .stream()
                        .filter(
                                method ->
                                        TestMethodWrapper
                                                .isTestMethodParameterised(
                                                        method,
                                                        getTestClass()
                                                )
                        ).map(method -> new TestMethodWrapper(method, getTestClass()))
                        .collect(Collectors.toList());
        List<FrameworkMethod> parameterisedMethods = new ArrayList<>();
        for (TestMethodWrapper wrapper : methodWrappers) {
            for (int index = 0; index < wrapper.parametersSets().length; index++) {
                parameterisedMethods.add(wrapper.frameworkMethod());
            }
        }
        originTestMethods.removeAll(parameterisedMethods);
        originTestMethods.addAll(parameterisedMethods);
        return originTestMethods;
    }

    @Override
    protected SandboxTestRunner.HelperTestRunner getHelperTestRunner(Class bootstrappedTestClass) {
        try {
            return new HelperTestRunnerWrapper(bootstrappedTestClass);
        } catch (InitializationError initializationError) {
            throw new RuntimeException(
                    "Failed to create HelperTestRunnerWrapper", initializationError
            );
        }
    }

    private class HelperTestRunnerWrapper extends RobolectricTestRunner.HelperTestRunner {

        HelperTestRunnerWrapper(Class bootstrappedTestClass) throws InitializationError {
            super(bootstrappedTestClass);
        }

        @Override
        protected Statement methodInvoker(FrameworkMethod method, Object test) {
            FrameworkMethod wrapper = method;
            for (FrameworkMethod parameterisedMethod : mParameterisedMethods) {
                if (!parameterisedMethod.getName().equals(method.getName())) {
                    continue;
                }
                ParameterisedTestMethodRunnerWrapper methodRunnerWrapper =
                        mParameterisedTestMethodRunners.get(parameterisedMethod);
                Method[] declaredMethods = getTestClass().getJavaClass().getDeclaredMethods();
                for (Method declaredMethod : declaredMethods) {
                    if (declaredMethod.getName().equals(method.getMethod().getName())
                            && declaredMethod.getParameterCount() > 0) {
                        wrapper = new FrameworkMethod(declaredMethod);
                        break;
                    }
                }
                assert methodRunnerWrapper != null;
                InvokeParameterisedMethodWrapper invokeParameterisedMethodWrapper =
                        new InvokeParameterisedMethodWrapper(
                                wrapper,
                                getTestClass(),
                                methodRunnerWrapper.getCurrentParameter(),
                                methodRunnerWrapper.getCurrentParameterIndex()
                        );
                wrapper = new FrameworkMethodWrapper(
                        wrapper.getMethod(),
                        invokeParameterisedMethodWrapper.getParameters()
                );
                break;
            }
            return super.methodInvoker(wrapper, test);
        }

        @Override
        protected void validatePublicVoidNoArgMethods(Class<? extends Annotation> annotation,
                                                      boolean isStatic,
                                                      List<Throwable> errors) {
            if (shouldValidatePublicVoidNoArgMethods(annotation, isStatic)) {
                super.validatePublicVoidNoArgMethods(annotation, isStatic, errors);
            }
        }
    }

    private static boolean shouldValidatePublicVoidNoArgMethods(Class<? extends Annotation> annotation,
                                                                boolean isStatic) {
        return (annotation == BeforeClass.class && isStatic)
                || (annotation == AfterClass.class && isStatic)
                || (annotation == Before.class && !isStatic)
                || (annotation == After.class && !isStatic);
    }

    private static class ParameterisedTestMethodRunnerWrapper extends ParameterisedTestMethodRunner {
        private List<Description> mDescriptions = new ArrayList<>();
        private Object[] mParameters;
        private int mParameterIndex;
        private int mDescriptionIndex;

        public ParameterisedTestMethodRunnerWrapper(TestMethod testMethod) {
            super(testMethod);
            mParameters = testMethod.parametersSets();
            mDescriptions.addAll(TestMethodWrapper.getAllDescriptions(testMethod));
        }

        Description getCurrentTestDescription() {
            mDescriptionIndex = (mDescriptionIndex + 1) % mDescriptions.size();
            return mDescriptions.get(mDescriptionIndex);
        }

        Object getCurrentParameter() {
            mParameterIndex = (mParameterIndex + 1) % mParameters.length;
            return mParameters[mParameterIndex];
        }

        int getCurrentParameterIndex() {
            return mParameterIndex;
        }
    }

    private static class TestMethodWrapper extends TestMethod {

        TestMethodWrapper(FrameworkMethod method, TestClass testClass) {
            super(method, testClass);
        }

        static boolean isTestMethodParameterised(FrameworkMethod method, TestClass testClass) {
            TestMethodWrapper testMethodWrapper = new TestMethodWrapper(method, testClass);
            try {
                Method isParameterisedMethod =
                        TestMethod.class.getDeclaredMethod("isParameterised");
                isParameterisedMethod.setAccessible(true);
                return (boolean) isParameterisedMethod.invoke(testMethodWrapper);
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                throw new RuntimeException("Failed to run isTestMethodParameterised", e);
            }
        }

        static List<Description> getAllDescriptions(TestMethod testMethod) {
            try {
                Method descriptionMethod = TestMethod.class.getDeclaredMethod("description");
                descriptionMethod.setAccessible(true);
                Description description = (Description) descriptionMethod.invoke(testMethod);
                return description.getChildren();
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                throw new RuntimeException("Failed to get all descriptions", e);
            }
        }
    }

    private static class InvokeParameterisedMethodWrapper {
        private Object[] mInvokeParameterisedMethodParameters;

        InvokeParameterisedMethodWrapper(FrameworkMethod method,
                                         Object testClass,
                                         Object parameters,
                                         int parameterSetIndex) {
            try {
                Class<?> invokeParameterisedMethodClass =
                        Class.forName("junitparams.internal.InvokeParameterisedMethod");
                Constructor<?> invokeParameterisedMethodConstrocutor =
                        invokeParameterisedMethodClass.getDeclaredConstructor(
                                FrameworkMethod.class,
                                Object.class,
                                Object.class,
                                int.class
                        );
                invokeParameterisedMethodConstrocutor.setAccessible(true);
                Object invokeParametrisedMethodInstance =
                        invokeParameterisedMethodConstrocutor.newInstance(
                                method, testClass, parameters, parameterSetIndex
                        );
                Field parameterisedField =
                        invokeParameterisedMethodClass.getDeclaredField("params");
                parameterisedField.setAccessible(true);
                mInvokeParameterisedMethodParameters =
                        (Object[]) parameterisedField.get(invokeParametrisedMethodInstance);
            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | IllegalAccessException
                    | InstantiationException
                    | InvocationTargetException
                    | NoSuchFieldException e) {
                throw new RuntimeException(
                        "Failed to initialize InvokeParameterisedMethodWrapper", e
                );
            }
        }

        Object[] getParameters() {
            return mInvokeParameterisedMethodParameters;
        }
    }

    private static class FrameworkMethodWrapper extends FrameworkMethod {
        private Object[] mParameters;

        FrameworkMethodWrapper(Method method, Object[] parameters) {
            super(method);
            mParameters = parameters;
        }

        @Override
        public Object invokeExplosively(Object target, Object... params) throws Throwable {
            return super.invokeExplosively(target, mParameters);
        }
    }
}
