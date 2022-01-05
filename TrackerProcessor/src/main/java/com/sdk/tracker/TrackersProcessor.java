package com.sdk.tracker;

import com.sdk.annotation.InjectRule;
import com.sdk.annotation.Tracker;
import com.sdk.tracker.injector.InjectorDelegate;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

//@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.sdk.annotation.Tracker", "com.sdk.annotation.InjectRule"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"debug"})
public class TrackersProcessor extends AbstractProcessor {
    private ProcessingEnvironment _context;
    private InjectorDelegate delegate;
    private Trees _trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        _context = processingEnvironment;
        _trees = Trees.instance(processingEnv);

        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();

        TreeMaker _maker = TreeMaker.instance(context);
        Names _names = Names.instance(context);

        delegate = new InjectorDelegate(_maker, _names, _trees);
        System.out.println("init processor");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver() || delegate == null) {
            return false;
        }

        Set<? extends Element> rules = roundEnvironment.getElementsAnnotatedWith(InjectRule.class);
        for (Element e : rules) {
            InjectRule rule = e.getAnnotation(InjectRule.class);
            if (rule.isMainRule()) {
                delegate.setInjectRule(rule);
                break;
            }
        }

        Set<? extends Element> elementsAnnotatedWith = roundEnvironment.getElementsAnnotatedWith(Tracker.class);
        for (Element element : elementsAnnotatedWith) {
            if (element.getKind() == ElementKind.CLASS) {
                if (!delegate.isEnable(element)) {
                    continue;
                }

                JCTree tree = (JCTree) _trees.getTree(element);
                if (tree instanceof JCTree.JCClassDecl) {
                    delegate.injectClassDecl(element, (JCTree.JCClassDecl) tree, _trees);
                }
                delegate.addImportInfo(element, null);
            } else if (element.getKind() == ElementKind.METHOD) {
                if (!delegate.isEnable(element)) {
                    continue;
                }

                JCTree tree = (JCTree) _trees.getTree(element);
                if (tree instanceof JCTree.JCMethodDecl) {
                    delegate.injectMethod(element, null, (JCTree.JCMethodDecl) tree);
                }

                Element closing = element.getEnclosingElement();
                if (closing != null) {
                    delegate.addImportInfo(element, closing);
                }
            }
        }
        return false;
    }

}
