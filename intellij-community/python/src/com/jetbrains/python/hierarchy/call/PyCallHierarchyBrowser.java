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

import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import com.jetbrains.python.hierarchy.PyHierarchyUtils;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by user on 7/23/14.
 */
public class PyCallHierarchyBrowser extends CallHierarchyBrowserBase {
  private static final Logger LOG = Logger.getInstance("#com.jetbrains.python.hierarchy.call.PyCallHierarchyBrowser");

  public PyCallHierarchyBrowser(@NotNull Project project, @NotNull PsiElement method) {
    super(project, method);
  }

  public PyCallHierarchyBrowser(PyFunction function) {
    super(function.getProject(), function);
  }

  @Nullable
  @Override
  protected PsiElement getElementFromDescriptor(@NotNull HierarchyNodeDescriptor descriptor) {
    if (descriptor instanceof PyCallHierarchyNodeDescriptor) {
      PyCallHierarchyNodeDescriptor nodeDescriptor = (PyCallHierarchyNodeDescriptor)descriptor;
      return nodeDescriptor.getEnclosingElement();
    }
    return null;
  }

  @Override
  protected void createTrees(@NotNull Map<String, JTree> trees) {

  }

  @Override
  protected boolean isApplicableElement(@NotNull PsiElement element) {
    return element instanceof PyFunction;
  }

  @Nullable
  @Override
  protected HierarchyTreeStructure createHierarchyTreeStructure(@NotNull String type, @NotNull PsiElement psiElement) {
    if (CALLER_TYPE.equals(type)) {
      return new PyCallerFunctionTreeStructure(myProject, (PyFunction)psiElement, getCurrentScopeType());
    }
    else if (CALLEE_TYPE.equals(type)) {
      return new PyCalleeFunctionTreeStructure(myProject, (PyFunction)psiElement, getCurrentScopeType());
    }
    else {
      return null;
    }
  }

  @Nullable
  @Override
  protected Comparator<NodeDescriptor> getComparator() {
    return PyHierarchyUtils.getComparator(myProject);
  }

  public static final class BaseOnThisMethodAction extends CallHierarchyBrowserBase.BaseOnThisMethodAction {}
}
