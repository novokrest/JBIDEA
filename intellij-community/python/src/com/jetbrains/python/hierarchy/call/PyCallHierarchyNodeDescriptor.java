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
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Created by user on 7/23/14.
 */
public class PyCallHierarchyNodeDescriptor extends HierarchyNodeDescriptor {
  protected PyCallHierarchyNodeDescriptor(@NotNull Project project, NodeDescriptor parentDescriptor, @NotNull PsiElement element, boolean isBase) {
    super(project, parentDescriptor, element, isBase);
  }

  public final PyFunction getPyFunction() {
    return (PyFunction)myElement;
  }

  public final PyFunction getEnclosingElement(){
    return myElement == null ? null : getEnclosingElement(myElement);
  }

  public static PyFunction getEnclosingElement(final PsiElement element){
    return PsiTreeUtil.getNonStrictParentOfType(element, PyFunction.class);
  }

  @Override
  public boolean isValid() {
    return myElement != null && myElement.isValid();
  }
}
