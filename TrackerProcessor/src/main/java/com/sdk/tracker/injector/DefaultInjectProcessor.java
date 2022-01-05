package com.sdk.tracker.injector;

import com.sdk.annotation.Tracker;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import java.util.ArrayList;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

public class DefaultInjectProcessor implements InjectProcessor {

    protected static final String TRACKER_PACKAGE = "com.sdk.tracker.injector";
    private static final String TRACKER_CLASS = "TrackerDefaultInjector";
    protected static final String TRACKER_IF_NAME = "isEnable";
    protected static final String TRACKER_ANNOTATION_NAME = "Tracker";
    protected static final String TRACKER_ANNOTATION_ENABLE = "enable";
    protected static final String TRACKER_ANNOTATION_GROUP = "group";
    protected static final String TRACKER_ANNOTATION_TAG = "tag";

    private static final String TRACKER_GET_CLASS_NAME = "getMethodClassName";
    private static final String TRACKER_GET_FRONT_NAME = "insertFront";
    private static final String TRACKER_GET_BACK_NAME = "insertBack";

    /**
     * is contructor method enable
     */


    private ProcessingEnvironment _context;
    protected TreeMaker _maker;
    protected Names _names;
    protected Trees _trees;

    public DefaultInjectProcessor(TreeMaker maker, Names names, Trees trees) {
        _maker = maker;
        _names = names;
        _trees = trees;
    }

    @Override
    public void visitorMethod(JCTree.JCMethodDecl jcMethodDecl, String className, String varDeclName, Tracker tracker, boolean isAnonymous) {
        java.util.List<JCTree.JCStatement> stats = new ArrayList<>();

        JCTree.JCStatement superOrThisStatement = getSuperOrThisStatement(jcMethodDecl);
        if (superOrThisStatement != null) {
            stats.add(superOrThisStatement);
        }

        JCTree.JCStatement statement = null;
        if (superOrThisStatement != null && jcMethodDecl.body.stats.size() >= 2) {
            statement = jcMethodDecl.body.stats.get(1);
        } else if (jcMethodDecl.body.stats.size() >= 1) {
            statement = jcMethodDecl.body.stats.get(0);
        }

        if (statement != null && statement.toString().startsWith("if (" + TRACKER_CLASS)) {
            return;
        }

        JCTree.JCStatement ifStatement = getFrontStatement(jcMethodDecl, className, varDeclName, tracker);
        if (!stats.isEmpty()) {
            stats.add(ifStatement);
            getBackStatement(jcMethodDecl, className, varDeclName, tracker, stats);
            stats.remove(2);
        } else {
            stats.add(ifStatement);
            getBackStatement(jcMethodDecl, className, varDeclName, tracker, stats);
        }

        jcMethodDecl.getBody().stats = List.from(stats);
    }


    /**
     * is tracker enable
     * because when you visit class some method may disable
     * in the tracker
     *
     * @param jcMethodDecl
     * @return
     */
    protected String[] getTrackerGroupByAnnotation(JCTree.JCMethodDecl jcMethodDecl) {
        String[] group_tags = {"", ""};
        JCTree.JCModifiers modifiers = jcMethodDecl.getModifiers();
        if (modifiers != null) {
            List<JCTree.JCAnnotation> annotations = modifiers.annotations;
            for (JCTree.JCAnnotation annotation : annotations) {
                JCTree.JCIdent annotatedType = (JCTree.JCIdent) annotation.getAnnotationType();
                List<JCTree.JCExpression> args = annotation.args;
                if (annotatedType != null) {
                    String name = annotatedType.name.toString();
                    if (TRACKER_ANNOTATION_NAME.equals(name)) {
                        for (JCTree.JCExpression arg : args) {
                            JCTree.JCAssign assign = (JCTree.JCAssign) arg;
                            if (assign != null) {
                                JCTree.JCIdent lhs = (JCTree.JCIdent) assign.lhs;
                                JCTree.JCLiteral rhs = (JCTree.JCLiteral) assign.rhs;
                                if (TRACKER_ANNOTATION_ENABLE.equals(lhs.name.toString()) && "0".equals(String.valueOf(rhs.value))) {
                                    group_tags[0] = null;
                                } else if (TRACKER_ANNOTATION_GROUP.equals(lhs.name.toString())) {
                                    group_tags[0] = String.valueOf(rhs.value);
                                } else if (TRACKER_ANNOTATION_TAG.equals(lhs.name.toString())) {
                                    group_tags[1] = String.valueOf(rhs.value);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        return group_tags;
    }

    /**
     * get the super or this method
     * because we can't insert method before it
     *
     * @param jcMethodDecl
     * @return
     */
    protected JCTree.JCStatement getSuperOrThisStatement(JCTree.JCMethodDecl jcMethodDecl) {
        if (jcMethodDecl.getBody().stats.nonEmpty() && "<init>".equals(jcMethodDecl.getName().toString())) {
            JCTree.JCStatement statement = jcMethodDecl.getBody().stats.get(0);
            if (statement != null && statement instanceof JCTree.JCExpressionStatement) {
                JCTree.JCExpressionStatement expressionStatement = (JCTree.JCExpressionStatement) statement;
                if (expressionStatement.expr instanceof JCTree.JCMethodInvocation) {
                    JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) expressionStatement.expr;
                    String name = "";
                    if (methodInvocation.meth instanceof JCTree.JCIdent) {
                        name = ((JCTree.JCIdent) methodInvocation.meth).name.toString();
                    } else if (methodInvocation.meth instanceof JCTree.JCFieldAccess) {
                        JCTree.JCFieldAccess methFieldAccess = (JCTree.JCFieldAccess) methodInvocation.meth;
                        if (methFieldAccess.selected.toString().startsWith("super.")) {
                            name = "super";
                        }
                    }
                    if ("super".equals(name) || "this".equals(name)) {
                        return statement;
                    }
                }
            }
        }
        return null;
    }

    /**
     * add block before method
     *
     * @param jcMethodDecl
     * @param tracker
     * @return
     */
    protected JCTree.JCStatement getFrontStatement(JCTree.JCMethodDecl jcMethodDecl, String className, String varDeclName, Tracker tracker) {
        JCTree.JCFieldAccess ifSelect = _maker.Select(_maker.Ident(_names.fromString(TRACKER_CLASS)), _names.fromString(TRACKER_IF_NAME));
        java.util.List<JCTree.JCExpression> args = new ArrayList();
        java.util.List<JCTree.JCExpression> params = new ArrayList<>();
        java.util.List<JCTree.JCExpression> paramNames = new ArrayList<>();

        List<JCTree.JCVariableDecl> methodParams = jcMethodDecl.getParameters();
        for (JCTree.JCVariableDecl decl : methodParams) {
            params.add(_maker.Ident(decl.name));
            paramNames.add(_maker.Literal(TypeTag.CLASS, decl.name.toString()));
        }

        java.util.List<JCTree.JCExpression> groups = new ArrayList<>();
        for (String group : tracker.group()) {
            groups.add(_maker.Literal(TypeTag.CLASS, group));
        }


        args.add(_maker.Literal(TypeTag.CLASS, className));
        args.add(_maker.Literal(TypeTag.CLASS, jcMethodDecl.name.toString()));
        args.add(_maker.NewArray(_maker.Ident(_names.fromString("String")), List.nil(), List.from(paramNames)));
        args.add(_maker.NewArray(_maker.Ident(_names.fromString("String")), List.nil(), List.from(groups)));

        JCTree.JCMethodInvocation ifMethod = _maker.Apply(List.nil(), ifSelect, List.from(args));

        args.add(_maker.Literal(TypeTag.CLASS, tracker.tag()));
        args.addAll(params);
        addCurrentObject(jcMethodDecl, varDeclName, args);

        //make the front method
        JCTree.JCFieldAccess getFrontSelect = _maker.Select(_maker.Ident(_names.fromString(TRACKER_CLASS)), _names.fromString(TRACKER_GET_FRONT_NAME));
        JCTree.JCMethodInvocation getFrontMethod = _maker.Apply(List.nil(), getFrontSelect, List.from(args));

//        JCTree.JCExpression jcReturnExpression = null;
//        JCTree jcClassType = null;
//        if (jcMethodDecl.getReturnType() == null) {
//            jcClassType = null;
//        } else if (jcMethodDecl.getReturnType() instanceof JCTree.JCIdent) {
//            jcClassType = jcMethodDecl.getReturnType();
//        } else if (jcMethodDecl.getReturnType() instanceof JCTree.JCPrimitiveTypeTree) {
//            JCTree.JCPrimitiveTypeTree typeTree = (JCTree.JCPrimitiveTypeTree) jcMethodDecl.getReturnType();
//            if (typeTree.typetag == TypeTag.VOID) {
//                jcClassType = null;
//            } else {
//                jcClassType = jcMethodDecl.getReturnType();
//            }
//        }

        JCTree.JCBlock block = _maker.Block(0, List.of(_maker.Exec(getFrontMethod)));

        JCTree.JCIf jcIf = _maker.If(ifMethod, block, null);

        return jcIf;
    }


    /**
     * add block after method
     *
     * @param jcMethodDecl
     * @param tracker
     * @param stats
     */
    protected void getBackStatement(JCTree.JCMethodDecl jcMethodDecl, String className, String varDeclName, Tracker tracker, java.util.List<JCTree.JCStatement> stats) {
        JCTree.JCFieldAccess ifSelect = _maker.Select(_maker.Ident(_names.fromString(TRACKER_CLASS)), _names.fromString(TRACKER_IF_NAME));
        java.util.List<JCTree.JCExpression> args = new ArrayList();
        java.util.List<JCTree.JCExpression> params = new ArrayList<>();
        java.util.List<JCTree.JCExpression> paramNames = new ArrayList<>();

        List<JCTree.JCVariableDecl> methodParams = jcMethodDecl.getParameters();
        for (JCTree.JCVariableDecl decl : methodParams) {
            params.add(_maker.Ident(decl.name));
            paramNames.add(_maker.Literal(TypeTag.CLASS, decl.name.toString()));
        }

        java.util.List<JCTree.JCExpression> groups = new ArrayList<>();
        for (String group : tracker.group()) {
            groups.add(_maker.Literal(TypeTag.CLASS, group));
        }

        args.add(_maker.Literal(TypeTag.CLASS, className));
        args.add(_maker.Literal(TypeTag.CLASS, jcMethodDecl.name.toString()));
        args.add(_maker.NewArray(_maker.Ident(_names.fromString("String")), List.nil(), List.from(paramNames)));
        args.add(_maker.NewArray(_maker.Ident(_names.fromString("String")), List.nil(), List.from(groups)));

        JCTree.JCMethodInvocation ifMethod = _maker.Apply(List.nil(), ifSelect, List.from(args));

        args.add(_maker.Literal(TypeTag.CLASS, tracker.tag()));

        boolean hasReturn = false;
        if (jcMethodDecl.getReturnType() != null) {
            hasReturn = true;
            if (jcMethodDecl.getReturnType() instanceof JCTree.JCPrimitiveTypeTree) {
                JCTree.JCPrimitiveTypeTree typeTree = (JCTree.JCPrimitiveTypeTree) jcMethodDecl.getReturnType();
                if (typeTree.typetag == TypeTag.VOID) {
                    hasReturn = false;
                }
            }
        }

        // if there is no return then the result is null
        if (!hasReturn) {
            args.add(_maker.Literal(TypeTag.BOT, null));
        }

        args.addAll(params);
        addCurrentObject(jcMethodDecl, varDeclName, args);

        JCTree.JCFieldAccess getBackSelect = _maker.Select(_maker.Ident(_names.fromString(TRACKER_CLASS)), _names.fromString(TRACKER_GET_BACK_NAME));
        JCTree.JCMethodInvocation getBackMethod = _maker.Apply(List.nil(), getBackSelect, List.from(args));

        JCTree.JCIf jcIf = _maker.If(ifMethod, _maker.Exec(getBackMethod), null);

//        String result = "object" + System.currentTimeMillis() + "call";
        if (hasReturn) {
//            args.add(_maker.Ident(_names.fromString(result)));
            handleReturnStatement(jcMethodDecl.body, jcIf, (JCTree.JCExpression) jcMethodDecl.getReturnType(), "");
            stats.addAll(jcMethodDecl.getBody().getStatements());
        } else {
            stats.addAll(jcMethodDecl.getBody().getStatements());
            stats.add(jcIf);
        }
    }

    private void handleBlockReturn(JCTree.JCBlock block, JCTree.JCIf jcIf, JCTree.JCExpression returnType, String result) {
        for (JCTree.JCStatement statement : block.stats) {
            if (statement instanceof JCTree.JCReturn) {
                setReturnStatement(block, (JCTree.JCReturn) statement, false, jcIf, returnType, result);
            } else {
                handleReturnStatement(statement, jcIf, returnType, result);
            }
        }
    }

    private void handleSwitchReturn(JCTree.JCSwitch jcSwitch, JCTree.JCIf jcIf, JCTree.JCExpression returnType, String result) {
        List<JCTree.JCCase> jcCases = jcSwitch.cases;
        for (JCTree.JCCase cases : jcCases) {
            for (JCTree.JCStatement statement : cases.stats) {
                if (statement instanceof JCTree.JCBlock) {
                    handleBlockReturn((JCTree.JCBlock) statement, jcIf, returnType, result);
                } else if (statement instanceof JCTree.JCSwitch) {
                    handleSwitchReturn((JCTree.JCSwitch) statement, jcIf, returnType, result);
                } else if (statement instanceof JCTree.JCIf) {
                    handleJcIfStatement((JCTree.JCIf) statement, jcIf, returnType, result);
                } else if (statement instanceof JCTree.JCReturn) {
                    setReturnStatement(cases, (JCTree.JCReturn) statement, false, jcIf, returnType, result);
                }
            }
        }
    }

    private void handleReturnStatement(JCTree.JCStatement statement, JCTree.JCIf jcIf, JCTree.JCExpression returnType, String result) {
        if (statement instanceof JCTree.JCBlock) {
            handleBlockReturn((JCTree.JCBlock) statement, jcIf, returnType, result);
        } else if (statement instanceof JCTree.JCIf) {
            handleJcIfStatement((JCTree.JCIf) statement, jcIf, returnType, result);
        } else if (statement instanceof JCTree.JCSwitch) {
            handleSwitchReturn((JCTree.JCSwitch) statement, jcIf, returnType, result);
        } else if (statement instanceof JCTree.JCWhileLoop) {
            handleWhileStatement((JCTree.JCWhileLoop) statement, jcIf, returnType, result);
        } else if (statement instanceof JCTree.JCForLoop) {
            handleForStatement((JCTree.JCForLoop) statement, jcIf, returnType, result);
        } else if (statement instanceof JCTree.JCDoWhileLoop) {
            handleDoWhileStatement((JCTree.JCDoWhileLoop) statement, jcIf, returnType, result);
        }
    }

    private void handleForStatement(JCTree.JCForLoop statement, JCTree.JCIf jcIf, JCTree.JCExpression returnType, String result) {
        handleReturnStatement(statement.body, jcIf, returnType, result);
    }

    private void handleWhileStatement(JCTree.JCWhileLoop statement, JCTree.JCIf jcIf, JCTree.JCExpression returnType, String result) {
        handleReturnStatement(statement.body, jcIf, returnType, result);
    }

    private void handleDoWhileStatement(JCTree.JCDoWhileLoop statement, JCTree.JCIf jcIf, JCTree.JCExpression returnType, String result) {
        handleReturnStatement(statement.body, jcIf, returnType, result);
    }

    private void handleJcIfStatement(JCTree.JCIf jcIf, JCTree.JCIf ifs, JCTree.JCExpression returnType, String result) {
        JCTree.JCStatement thenpart = jcIf.thenpart;
        if (thenpart instanceof JCTree.JCReturn) {
            setReturnStatement(jcIf, (JCTree.JCReturn) thenpart, true, ifs, returnType, result);
        } else {
            handleReturnStatement(thenpart, ifs, returnType, result);
        }

        JCTree.JCStatement elsepart = jcIf.elsepart;
        if (elsepart instanceof JCTree.JCReturn) {
            setReturnStatement(jcIf, (JCTree.JCReturn) elsepart, false, ifs, returnType, result);
        } else {
            handleReturnStatement(elsepart, ifs, returnType, result);
        }
    }

    //change the return statement to a JCVariableDecl
    private void setReturnStatement(JCTree.JCStatement parentStatement, JCTree.JCReturn lastStatement, boolean isThenPart, JCTree.JCIf resourceIf, JCTree.JCExpression returnType, String result) {
        if (parentStatement != null) {
            String fResult = "obj" + System.currentTimeMillis();
            JCTree.JCVariableDecl resultInit = _maker.VarDef(_maker.Modifiers(Flags.FINAL), _names.fromString(fResult), _maker.Ident(_names.fromString("Object")), lastStatement.expr);

            JCTree.JCMethodInvocation method = (JCTree.JCMethodInvocation) ((JCTree.JCExpressionStatement) resourceIf.thenpart).expr;
            java.util.List<JCTree.JCExpression> args = new ArrayList<>();
            args.addAll(method.args);
            args.add(6, _maker.Ident(_names.fromString(fResult)));

            JCTree.JCFieldAccess getBackSelect = _maker.Select(_maker.Ident(_names.fromString(TRACKER_CLASS)), _names.fromString(TRACKER_GET_BACK_NAME));
            JCTree.JCMethodInvocation getBackMethod = _maker.Apply(List.nil(), getBackSelect, List.from(args));
            JCTree.JCIf resultJcIf = _maker.If(resourceIf.cond, _maker.Exec(getBackMethod), null);

            JCTree.JCReturn returnStatement = _maker.Return(_maker.TypeCast(returnType, _maker.Ident(_names.fromString(fResult))));
            JCTree.JCBlock rBlock = _maker.Block(0, List.of(resultInit, resultJcIf, returnStatement));

            if (parentStatement instanceof JCTree.JCBlock) {
                JCTree.JCBlock block = (JCTree.JCBlock) parentStatement;
                java.util.List<JCTree.JCStatement> stats = new ArrayList<>();
                stats.addAll(block.stats);
                int index = stats.indexOf(lastStatement);
                stats.remove(lastStatement);
                stats.add(index, rBlock);
                block.stats = List.from(stats);
            } else if (parentStatement instanceof JCTree.JCIf) {
                if (isThenPart) {
                    ((JCTree.JCIf) parentStatement).thenpart = rBlock;
                } else {
                    ((JCTree.JCIf) parentStatement).elsepart = rBlock;
                }
            } else if (parentStatement instanceof JCTree.JCCase) {
                JCTree.JCCase jccase = (JCTree.JCCase) parentStatement;
                java.util.List<JCTree.JCStatement> stats = new ArrayList<>();
                stats.addAll(jccase.stats);
                int index = stats.indexOf(lastStatement);
                stats.remove(lastStatement);
                stats.add(index, rBlock);
                jccase.stats = List.from(stats);
            }
        }
    }

    /**
     * add the current object for the first arg
     *
     * @param jcMethodDecl
     * @param stats
     */
    protected void addCurrentObject(JCTree.JCMethodDecl jcMethodDecl, String varDeclName, java.util.List<JCTree.JCExpression> stats) {
        JCTree.JCModifiers modifiers = jcMethodDecl.getModifiers();
        //handle the static method
        if (varDeclName != null) {
            stats.add(0, _maker.Ident(_names.fromString(varDeclName)));
        } else {
            if (modifiers != null && modifiers.getFlags().contains(Modifier.STATIC)) {
                stats.add(0, _maker.Literal(TypeTag.BOT, null));
            } else {
                stats.add(0, _maker.Ident(_names.fromString("this")));
            }
        }
    }

    /**
     * add the import to the class
     *
     * @param element
     */
    @Override
    public void addImportInfo(Element element) {
        TreePath treePath = _trees.getPath(element);
        Tree leaf = treePath.getLeaf();
        if (treePath.getCompilationUnit() instanceof JCTree.JCCompilationUnit && leaf instanceof JCTree) {
            JCTree.JCCompilationUnit jccu = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();

            for (JCTree jcTree : jccu.getImports()) {
                if (jcTree != null && jcTree instanceof JCTree.JCImport) {
                    JCTree.JCImport jcImport = (JCTree.JCImport) jcTree;
                    if (jcImport.qualid != null && jcImport.qualid instanceof JCTree.JCFieldAccess) {
                        JCTree.JCFieldAccess jcFieldAccess = (JCTree.JCFieldAccess) jcImport.qualid;
                        try {
                            if (TRACKER_PACKAGE.equals(jcFieldAccess.selected.toString()) && TRACKER_CLASS.equals(jcFieldAccess.name.toString())) {
                                return;
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            java.util.List<JCTree> trees = new ArrayList<>();
            trees.addAll(jccu.defs);
            JCTree.JCIdent ident = _maker.Ident(_names.fromString(TRACKER_PACKAGE));
            JCTree.JCImport jcImport = _maker.Import(_maker.Select(
                    ident, _names.fromString(TRACKER_CLASS)), false);
            if (!trees.contains(jcImport)) {
                trees.add(0, jcImport);
            }
            jccu.defs = List.from(trees);
        }
    }
}
