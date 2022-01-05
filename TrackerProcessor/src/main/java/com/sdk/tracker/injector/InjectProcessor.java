package com.sdk.tracker.injector;

import com.sdk.annotation.Tracker;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;

public interface InjectProcessor {
//    boolean isEnable(Element element);
//
//    void injectMethod(Element element,JCTree.JCMethodDecl jcMethodDecl);
//
//    void injectClassDecl(Element element, JCTree.JCClassDecl jcClassDecl);

    void addImportInfo(Element element);

    void visitorMethod(JCTree.JCMethodDecl jcMethodDecl, String className, String objectName, Tracker tracker, boolean isAnonymous);
}
