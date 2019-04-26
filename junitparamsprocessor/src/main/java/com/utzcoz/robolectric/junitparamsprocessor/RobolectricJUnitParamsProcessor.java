package com.utzcoz.robolectric.junitparamsprocessor;

import com.google.auto.service.AutoService;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import junitparams.Parameters;

@AutoService(Processor.class)
@SupportedAnnotationTypes("junitparams.Parameters")
public class RobolectricJUnitParamsProcessor extends AbstractProcessor {
    private Trees mTrees;
    private TreeMaker mTreeMaker;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mTreeMaker = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());
        mTrees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) mTrees.getTree(element);
            classDecl.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl tree) {
                    for (JCTree jcTree : tree.defs) {
                        if (!jcTree.getKind().equals(Tree.Kind.METHOD)) {
                            continue;
                        }
                        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) jcTree;
                        Parameters annotation = methodDecl.sym.getAnnotation(Parameters.class);
                        if (annotation == null) {
                            continue;
                        }
                        tree.defs = tree.defs.prepend(addEmptyMethod(methodDecl));
                    }
                    super.visitClassDef(tree);
                }
            });
        }
        return true;
    }

    private JCTree.JCMethodDecl addEmptyMethod(JCTree.JCMethodDecl jcMethodDecl) {
        JCTree.JCModifiers modifiers = mTreeMaker.Modifiers(Flags.PUBLIC);
        JCTree.JCExpression returnType = mTreeMaker.TypeIdent(TypeTag.VOID);
        List<JCTree.JCVariableDecl> parameters = List.nil();
        List<JCTree.JCTypeParameter> generics = List.nil();
        List<JCTree.JCExpression> exceptions = List.nil();
        Name name = jcMethodDecl.getName();
        JCTree.JCBlock body = mTreeMaker.Block(0, List.<JCTree.JCStatement>nil());
        return mTreeMaker.MethodDef(
                modifiers, name, returnType, generics, parameters, exceptions, body, null
        );
    }
}
