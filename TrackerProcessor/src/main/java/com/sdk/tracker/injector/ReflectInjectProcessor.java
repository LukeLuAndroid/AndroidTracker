package com.sdk.tracker.injector;

import com.sdk.annotation.Tracker;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import java.util.ArrayList;

import javax.lang.model.element.Element;

public class ReflectInjectProcessor extends DefaultInjectProcessor {

    private static final String TRACKER_CLASS = "TrackerReflectInjector";
    private static final String TRACKER_INVOKE_NAME = "invoke";


    public ReflectInjectProcessor(TreeMaker maker, Names names, Trees trees) {
        super(maker, names, trees);
    }

    @Override
    public void visitorMethod(JCTree.JCMethodDecl jcMethodDecl, String className, String varDeclName, Tracker tracker, boolean isAnonymous) {
        if (jcMethodDecl.sym == null) {
            return;
        }

        java.util.List<JCTree.JCStatement> stats = new ArrayList<>();

        JCTree.JCStatement superOrThisStatement = getSuperOrThisStatement(jcMethodDecl);
        if (superOrThisStatement != null) {
            stats.add(superOrThisStatement);
        }

        JCTree.JCStatement ifStatement = getFrontStatement(jcMethodDecl, className, varDeclName, tracker);
        if (!stats.isEmpty()) {
            stats.add(ifStatement);
            stats.addAll(jcMethodDecl.getBody().getStatements());
            stats.remove(2);
        } else {
            stats.add(ifStatement);
            stats.addAll(jcMethodDecl.getBody().getStatements());
        }
        jcMethodDecl.getBody().stats = List.from(stats);
    }

    /**
     * get the super or this method
     * because we can't insert method before it
     *
     * @param jcMethodDecl
     * @return
     */
    protected JCTree.JCStatement getSuperOrThisStatement(JCTree.JCMethodDecl jcMethodDecl) {
        return super.getSuperOrThisStatement(jcMethodDecl);
    }

    /**
     * get the statement to insert to first line in the method
     *
     * @param jcMethodDecl
     * @return
     */

    /**
     * get the statement to insert to first line in the method
     *
     * @param jcMethodDecl
     * @return
     */
    protected JCTree.JCStatement getFrontStatement(JCTree.JCMethodDecl jcMethodDecl, String className, String varDeclName, Tracker tracker) {
        JCTree.JCFieldAccess ifSelect = _maker.Select(_maker.Ident(_names.fromString(TRACKER_CLASS)), _names.fromString(TRACKER_IF_NAME));
        java.util.List<JCTree.JCExpression> args = new ArrayList();
        java.util.List<JCTree.JCExpression> classNames = new ArrayList<>();
        java.util.List<JCTree.JCExpression> params = new ArrayList<>();
        java.util.List<JCTree.JCExpression> paramNames = new ArrayList<>();

        List<JCTree.JCVariableDecl> methodParams = jcMethodDecl.getParameters();
        for (JCTree.JCVariableDecl decl : methodParams) {
            params.add(_maker.Ident(decl.name));
            classNames.add(_maker.Literal(TypeTag.CLASS, decl.sym.type.toString()));
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

        args.remove(args.size() - 1);
        args.add(2, _maker.NewArray(_maker.Ident(_names.fromString("String")), List.nil(), List.from(classNames)));
//        args.add(_maker.Literal(TypeTag.CLASS, tracker.tag()));
        args.addAll(params);
        addCurrentObject(jcMethodDecl, varDeclName, args);

        JCTree.JCFieldAccess actionSelect = _maker.Select(_maker.Ident(_names.fromString(TRACKER_CLASS)), _names.fromString(TRACKER_INVOKE_NAME));
        JCTree.JCMethodInvocation actionMethod = _maker.Apply(List.nil(), actionSelect, List.from(args));


        JCTree.JCExpression jcReturnExpression = null;
        JCTree jcClassType = null;
        if (jcMethodDecl.getReturnType() == null) {
            jcClassType = null;
        } else if (jcMethodDecl.getReturnType() instanceof JCTree.JCIdent) {
            jcClassType = jcMethodDecl.getReturnType();
        } else if (jcMethodDecl.getReturnType() instanceof JCTree.JCPrimitiveTypeTree) {
            JCTree.JCPrimitiveTypeTree typeTree = (JCTree.JCPrimitiveTypeTree) jcMethodDecl.getReturnType();
            if (typeTree.typetag == TypeTag.VOID) {
                jcClassType = null;
            } else {
                jcClassType = jcMethodDecl.getReturnType();
            }
        }

        JCTree.JCBlock block;

        if (jcClassType == null) {
            JCTree.JCReturn returnStatement = _maker.Return(jcReturnExpression);
            block = _maker.Block(0, List.of(_maker.Exec(actionMethod), returnStatement));
        } else {
            JCTree.JCReturn returnStatement = _maker.Return(_maker.TypeCast(jcClassType, actionMethod));
            block = _maker.Block(0, List.of(returnStatement));
        }

        JCTree.JCIf jcIf = _maker.If(ifMethod, block, null);

        return jcIf;
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
