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

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by user on 7/22/14.
 */
public class PyCallHierarchyProvider implements HierarchyProvider {
  @Nullable
  @Override
  public PsiElement getTarget(@NotNull DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return null;

    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if (element == null) {
      Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
      if (editor != null) {
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) return null;

        element = TargetElementUtilBase.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED |
                                                                  TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED |
                                                                  TargetElementUtilBase.LOOKUP_ITEM_ACCEPTED);
        if (element instanceof PyFunction) {
          return element;
        }

        element = file.findElementAt(editor.getCaretModel().getOffset());
      }
    }
    return PsiTreeUtil.getParentOfType(element, PyFunction.class, false);
  }

  @NotNull
  @Override
  public HierarchyBrowser createHierarchyBrowser(PsiElement target) {
    return new PyCallHierarchyBrowser((PyFunction)target);
  }

  @Override
  public void browserActivated(@NotNull HierarchyBrowser hierarchyBrowser) {
    ((PyCallHierarchyBrowser)hierarchyBrowser).changeView(CallHierarchyBrowserBase.CALLER_TYPE);
  }
}
