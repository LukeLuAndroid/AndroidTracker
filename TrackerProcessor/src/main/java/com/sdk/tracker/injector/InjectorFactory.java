package com.sdk.tracker.injector;

import com.sdk.annotation.InjectType;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;

public class InjectorFactory {
    private TreeMaker _maker;
    private Names _names;
    private Trees _trees;

    public InjectorFactory(TreeMaker maker, Names names, Trees trees) {
        _maker = maker;
        _names = names;
        _trees = trees;
    }

    public InjectProcessor getInjectProcessor(InjectType type) {
        switch (type) {
            case DEFAULT:
                return new DefaultInjectProcessor(_maker, _names, _trees);
            case REFLECT:
                return new ReflectInjectProcessor(_maker, _names, _trees);
            default:
                break;
        }
        return null;
    }
}
