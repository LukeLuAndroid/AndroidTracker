package com.sdk.tracker.injector;

import com.sdk.annotation.InjectRule;
import com.sdk.annotation.InjectType;
import com.sdk.annotation.Modifier;
import com.sdk.annotation.Tracker;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.AnnotationProxyMaker;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Names;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public class InjectorDelegate {

    static Map<InjectType, InjectProcessor> injectProcessorMap = new HashMap<>();
    private static Map<String, Boolean> methodUniqueMaps = new HashMap<>();
    boolean isContructorEnable = false;
    private InjectRule mInjectRule;

    public InjectRule getInjectRule() {
        return mInjectRule;
    }

    public void setInjectRule(InjectRule rule) {
        this.mInjectRule = rule;
    }

    private boolean isRuleEnable(InjectRule injectRule, long flags, String methodName) {
        if (injectRule == null) {
            return true;
        }

        boolean isEnable = false;
        if (injectRule.attrs().length == 0) {
            isEnable = true;
        } else {
            for (Modifier rule : injectRule.attrs()) {
                if ((rule == Modifier.PRIVATE && (flags & Flags.PRIVATE) != 0) ||
                        (rule == Modifier.ABSTRACT && (flags & Flags.ABSTRACT) != 0) ||
                        (rule == Modifier.FINAL && (flags & Flags.FINAL) != 0) ||
                        (rule == Modifier.PROTECTED && (flags & Flags.PROTECTED) != 0) ||
                        (rule == Modifier.PUBLIC && (flags & Flags.PUBLIC) != 0) ||
                        (rule == Modifier.STATIC && (flags & Flags.STATIC) != 0)) {
                    isEnable = true;
                    break;
                }
            }
        }

        if (isEnable) {
            String regex = injectRule.regex();
            if (regex != null && !"".equals(regex)) {
                Pattern pattern = Pattern.compile(regex);
                if (!pattern.matcher(methodName).matches()) {
                    isEnable = false;
                } else {
                    isEnable = true;
                }
            }
        }

        return isEnable;
    }

    public InjectorDelegate(TreeMaker maker, Names names, Trees trees) {
        InjectorFactory factory = new InjectorFactory(maker, names, trees);
        injectProcessorMap.put(InjectType.DEFAULT, factory.getInjectProcessor(InjectType.DEFAULT));
        injectProcessorMap.put(InjectType.REFLECT, factory.getInjectProcessor(InjectType.REFLECT));
    }

    public boolean isEnable(Element element) {
        if (element.getKind() == ElementKind.METHOD) {
            Tracker methodTracker = element.getAnnotation(Tracker.class);
            if (methodTracker == null || !methodTracker.enable()) {
                return false;
            }

            if (methodTracker == null) {
                Element closing = element.getEnclosingElement();
                if (closing != null) {
                    Tracker tracker = closing.getAnnotation(Tracker.class);
                    if (tracker != null) {
                        if (!tracker.enable()) {
                            return false;
                        }
                        isContructorEnable = tracker.construct();
                    }
                }
            }
        } else if (element.getKind() == ElementKind.CLASS) {
            Tracker tracker = element.getAnnotation(Tracker.class);
            if (tracker == null || !tracker.enable()) {
                return false;
            }
            isContructorEnable = tracker.construct();
        }
        return true;
    }

    public void injectClassDecl(Element element, JCTree.JCClassDecl jcClassDecl, Trees _trees) {
        acceptClassDecl(element, jcClassDecl, _trees);
        java.util.List<? extends Element> childElements = element.getEnclosedElements();
        for (Element e : childElements) {
            JCTree ctree = (JCTree) _trees.getTree(e);
            if (ctree != null) {
                if (ctree instanceof JCTree.JCMethodDecl) {
                    injectMethod(e, jcClassDecl, (JCTree.JCMethodDecl) ctree);
                } else if (ctree instanceof JCTree.JCClassDecl) {
                    injectClassDecl(e, (JCTree.JCClassDecl) ctree, _trees);
                    acceptClassDecl(e, (JCTree.JCClassDecl) ctree, _trees);
                }
            }
        }
    }

    /**
     * inject method
     *
     * @param classDecl
     * @param methodDecl
     */
    public void injectMethod(final Element element, final JCTree.JCClassDecl classDecl, JCTree.JCMethodDecl methodDecl) {
        //deprecate the duplicate method
        String fullClassName;
        if (methodDecl.sym != null) {
            fullClassName = ((Symbol.ClassSymbol) methodDecl.sym.owner).flatname.toString();
        } else {
            return;
        }
        final String methodId = fullClassName + "." + methodDecl.getName().toString() + methodDecl.getParameters().size();

        if (methodUniqueMaps.containsKey(methodId)) {
            return;
        }
        methodUniqueMaps.put(methodId, true);

//        System.out.println("methodid=" + methodId);

        methodDecl.accept(new TreeTranslator() {
            @Override
            public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
                super.visitMethodDef(jcMethodDecl);
                if (jcMethodDecl.sym == null) {
                    return;
                }

                handleMethodInjectInfo(element, fullClassName, null, jcMethodDecl, false);
            }
        });
        acceptMethodDecl(element, methodDecl);
    }

    //handle the vardef in classdecl
    public void acceptClassDecl(Element element, JCTree.JCClassDecl classDecl, Trees _trees) {
        classDecl.accept(new TreeTranslator() {
            @Override
            public void visitVarDef(JCTree.JCVariableDecl parentVariableDecl) {
                super.visitVarDef(parentVariableDecl);
                if (parentVariableDecl.sym != null && parentVariableDecl.init != null) {
                    parentVariableDecl.accept(new TreeTranslator() {
                        @Override
                        public void visitNewClass(JCTree.JCNewClass jcNewClass) {
                            super.visitNewClass(jcNewClass);
                            String clsName;
                            if (jcNewClass.clazz.toString().equals(parentVariableDecl.vartype.toString())) {
                                clsName = null;
                            } else {
                                clsName = jcNewClass.clazz.toString();
                            }
                            injectVarDeclMethod(element, jcNewClass, parentVariableDecl.getName().toString(), clsName);
                        }
                    });
                }
            }
        });
    }

    //the second level method
    private void acceptMethodDecl(final Element element, final JCTree.JCMethodDecl methodDecl) {
        methodDecl.accept(new TreeTranslator() {
            @Override
            public void visitNewClass(JCTree.JCNewClass jcNewClass) {
                super.visitNewClass(jcNewClass);
                result = jcNewClass;
                if (jcNewClass != null) {
                    acceptNewClass(element, methodDecl, jcNewClass);
                }
            }

            @Override
            public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                super.visitClassDef(jcClassDecl);
                if ("".equals(jcClassDecl.getSimpleName().toString())) {
                    return;
                }
                jcClassDecl.accept(new TreeTranslator() {
                    @Override
                    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
                        super.visitMethodDef(jcMethodDecl);
                        injectMethodInMethodClass(element, methodDecl, jcClassDecl, jcMethodDecl);
                        acceptMethodDecl(element, jcMethodDecl);
                    }

                    @Override
                    public void visitNewClass(JCTree.JCNewClass jcNewClass) {
                        super.visitNewClass(jcNewClass);
                        if (jcNewClass != null) {
                            acceptNewClass(element, methodDecl, jcNewClass);
                        }
                    }
                });
            }
        });
    }

    private void injectMethodInMethodClass(Element element, JCTree.JCMethodDecl parentMethod, JCTree.JCClassDecl jcClassDecl, JCTree.JCMethodDecl jcMethodDecl) {
        String methodPre = ((Symbol.MethodSymbol) element).owner.flatName().toString() + "." + parentMethod.getName().toString() + "." + jcClassDecl.getSimpleName().toString();
        handleMethodInjectInfo(element, methodPre, null, jcMethodDecl, true);
    }

    private void acceptNewClass(Element element, JCTree.JCMethodDecl parentMethod, JCTree.JCNewClass jcNewClass) {
        String varName = "";
        String methodPre = ((Symbol.MethodSymbol) element).owner.flatName().toString() + "." + parentMethod.getName().toString() + ("".equals(varName) ? "" : ("." + varName));
        jcNewClass.accept(new TreeTranslator() {
            @Override
            public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
                super.visitMethodDef(jcMethodDecl);
                handleMethodInjectInfo(element, methodPre, null, jcMethodDecl, true);
            }
        });
    }

    private void acceptVarDef(Element element, JCTree.JCMethodDecl parentMethod, JCTree.JCVariableDecl jcVariableDecl, JCTree.JCVariableDecl parentVarDecl) {
        jcVariableDecl.accept(new TreeTranslator() {
            @Override
            public void visitNewClass(JCTree.JCNewClass jcNewClass) {
                super.visitNewClass(jcNewClass);
//                result = jcNewClass;
                StringBuilder builder = new StringBuilder();
                if (element instanceof Symbol.ClassSymbol) {
                    builder.append(((Symbol.ClassSymbol) element).flatname.toString());
                } else if (element instanceof Symbol.MethodSymbol) {
                    builder.append(((Symbol.MethodSymbol) element).owner.flatName().toString());
                }

                if (parentMethod != null) {
                    builder.append(".").append(parentMethod.getName().toString());
                } else if (parentVarDecl != null) {
                    builder.append(".").append(parentVarDecl.getName().toString());
                }

                builder.append(".").append(jcVariableDecl.getName().toString());

                String methodPre = builder.toString();

                jcNewClass.accept(new TreeTranslator() {
                    @Override
                    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
                        super.visitMethodDef(jcMethodDecl);
                        handleMethodInjectInfo(element, methodPre, jcVariableDecl.getName().toString(), jcMethodDecl, true);
                    }
                });
            }
        });
    }

    private void handleMethodInjectInfo(Element element, String fullClassName, String varDeclName, JCTree.JCMethodDecl jcMethodDecl, boolean isAnonymous) {
        //if the contructor function enable
        if (!isContructorEnable && "<init>".equals(jcMethodDecl.getName().toString())) {
//            System.out.println("contructor is disable in method methodName=" + jcMethodDecl.getName() + ",className=" + ((Symbol.ClassSymbol) jcMethodDecl.sym.owner).fullname);
            return;
        }

        //the body is null maybe interface
        if (jcMethodDecl.getBody() == null) {
            return;
        }

        String methodId = fullClassName + "." + jcMethodDecl.getName().toString() + jcMethodDecl.getParameters().size();
        System.out.println("methodid.handleMethodInject=" + methodId);

//        if (methodUniqueMaps.containsKey(methodId)) {
//            return;
//        }
//        methodUniqueMaps.put(methodId, true);

        //current method do not has the tracker
        Tracker tracker = getTracker(jcMethodDecl.mods.annotations);
        if (tracker == null) {
            tracker = element.getAnnotation(Tracker.class);
            Element parent = element;
            if (tracker == null) {
                parent = parent.getEnclosingElement();
                while (parent != null) {
                    tracker = parent.getAnnotation(Tracker.class);
                    if (tracker != null) {
                        break;
                    }
                    parent = parent.getEnclosingElement();
                }

                if (tracker == null || (tracker != null && !tracker.enable())) {
                    return;
                }
            }

            InjectRule rule = tracker.injectRule();
            if (parent != null && isPackageElement(parent.getEnclosingElement()) && "".equals(rule.regex()) && rule.attrs().length == 0) {
                rule = mInjectRule;
            }
            if (!isRuleEnable(rule, jcMethodDecl.mods.flags, jcMethodDecl.name.toString())) {
                return;
            }
        } else if (!tracker.enable()) {
            return;
        }

        InjectProcessor injectProcessor = injectProcessorMap.get(tracker.injectType());

        if (tracker != null && injectProcessor != null) {
            injectProcessor.visitorMethod(jcMethodDecl, fullClassName, varDeclName, tracker, isAnonymous);
        }
    }

    private void injectVarDeclMethod(Element element, JCTree.JCNewClass jcNewClass, String varDeclName, String clsName) {
        jcNewClass.accept(new TreeTranslator() {
            @Override
            public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
                super.visitMethodDef(jcMethodDecl);

                StringBuilder builder = new StringBuilder();
                if (element instanceof Symbol.ClassSymbol) {
                    builder.append(((Symbol.ClassSymbol) element).flatName().toString());
                }

                builder.append(".").append(varDeclName);
                if (clsName != null) {
                    builder.append(".").append(clsName);
                }

                String fullClassName = builder.toString();
                handleMethodInjectInfo(element, fullClassName, varDeclName, jcMethodDecl, true);
            }
        });
    }

    private boolean isPackageElement(Element element) {
        if (element != null && element.getKind() == ElementKind.PACKAGE) {
            return true;
        }
        return false;
    }

    private Tracker getTracker(List<JCTree.JCAnnotation> anns) {
        Tracker tracker = null;
        for (JCTree.JCAnnotation annotation : anns) {
            String name = annotation.getAnnotationType().toString();
            if ("Tracker".equals(name)) {
                if (annotation.attribute != null) {
                    tracker = AnnotationProxyMaker.generateAnnotation(annotation.attribute, Tracker.class);
                }
                break;
            }
        }
        return tracker;
    }

    //add import info
    public void addImportInfo(Element element, Element parent) {
        Tracker tracker = element.getAnnotation(Tracker.class);
        if (tracker == null && parent != null) {
            tracker = parent.getAnnotation(Tracker.class);
        }

        if (tracker != null) {
            InjectProcessor injectProcessor = injectProcessorMap.get(tracker.injectType());
            if (tracker != null && injectProcessor != null) {
                injectProcessor.addImportInfo(parent == null ? element : parent);
            }
        }
    }
}
