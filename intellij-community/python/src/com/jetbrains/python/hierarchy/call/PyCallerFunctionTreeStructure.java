/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.hierarchy.call;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.HashSet;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.search.PySuperMethodsSearch;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by user on 7/23/14.
 */
public class PyCallerFunctionTreeStructure extends HierarchyTreeStructure {
  private final String myScopeType;

  public PyCallerFunctionTreeStructure(Project project, PyFunction function, String currentScopeType) {
    super(project, new PyCallHierarchyNodeDescriptor(project, null, function, true, false));
    myScopeType = currentScopeType;
  }

  @NotNull
  @Override
  protected Object[] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
    final PyFunction function = ((PyCallHierarchyNodeDescriptor)descriptor).getEnclosingElement();
    HierarchyNodeDescriptor nodeDescriptor = getBaseDescriptor();
    if (function == null || nodeDescriptor == null) {
      return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }
    final SearchScope searchScope = getSearchScope(myScopeType, function.getContainingClass());
    final Set<PyFunction> functionsToFind = new HashSet<PyFunction>();
    final Collection<PsiElement> superMethods = PySuperMethodsSearch.search(function, true).findAll();
    //functionsToFind.add(function);
    for (PsiElement superMethod : superMethods) {
      if (superMethod instanceof PyFunction) {
        functionsToFind.add((PyFunction)superMethod);
      }
    }

    List<PyCallHierarchyNodeDescriptor> descriptors = new ArrayList<PyCallHierarchyNodeDescriptor>();
    for (PyFunction pyFunction: functionsToFind) {
      descriptors.add(new PyCallHierarchyNodeDescriptor(myProject, null, pyFunction, false, false));
    }

    return ArrayUtil.toObjectArray(descriptors);
  }

  @Override
  public boolean isAlwaysShowPlus() {
    return true;
  }
}
