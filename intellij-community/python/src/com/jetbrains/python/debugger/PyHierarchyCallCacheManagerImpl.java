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
package com.jetbrains.python.debugger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.messages.impl.Message;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * HIERARCHY_CALLERS_DATA file attribute contains for every function in file information about its callers and lines
 * where this function is called by its callers
 *
 * HIERARCHY_CALLEES_DATA file attribute contains for every function in file information about its callees and lines
 * where this function calls its callees
 *
 */
public class PyHierarchyCallCacheManagerImpl extends PyHierarchyCallCacheManager {
  protected static final Logger LOG = Logger.getInstance(PyHierarchyCallCacheManagerImpl.class.getName());

  public static final FileAttribute HIERARCHY_CALLERS_DATA = new FileAttribute("callers.hierarchy.attribute", 1, true);
  public static final FileAttribute HIERARCHY_CALLEES_DATA = new FileAttribute("callees.hierarchy.attribute", 1, true);

  private final Project myProject;

  public PyHierarchyCallCacheManagerImpl(Project project) {
    myProject = project;
  }

  private final LoadingCache<VirtualFile, String> myHierarchyCallersCache = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build(
      new CacheLoader<VirtualFile, String>() {
        @Override
        public String load(VirtualFile key) throws Exception {
          return readCallerAttributeFromFile(key);
        }
      }
    );

  private final LoadingCache<VirtualFile, String> myHierarchyCalleesCache = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build(
      new CacheLoader<VirtualFile, String>() {
        @Override
        public String load(VirtualFile key) throws Exception {
          return readCalleeAttributeFromFile(key);
        }
      }
    );

  private static String readCallerAttributeFromFile(VirtualFile key) {
    return readAttributeFromFile(key, HIERARCHY_CALLERS_DATA);
  }

  private static String readCalleeAttributeFromFile(VirtualFile key) {
    return readAttributeFromFile(key, HIERARCHY_CALLEES_DATA);
  }

  private static String readAttributeFromFile(VirtualFile key, FileAttribute fileAttribute) {
    byte[] data;
    try {
      data = fileAttribute.readAttributeBytes(key);
    }
    catch (Exception e) {
      data = null;
    }

    String content;
    if (data != null && data.length > 0) {
      content = new String(data);
    }
    else {
      content = null;
    }

    return content;
  }

  @Override
  public void recordHierarchyCallInfo(@NotNull PyHierarchyCallInfo callInfo) {
    GlobalSearchScope scope = ProjectScope.getProjectScope(myProject);

    VirtualFile calleeFile = getCalleeFile(callInfo);
    if (calleeFile != null && scope.contains(calleeFile)) {
      recordHierarchyCallerInfo(calleeFile, callInfo);
    }

    VirtualFile callerFile = getCallerFile(callInfo);
    if (callerFile != null && scope.contains(callerFile)) {
      //recordHierarchyCalleeInfo(callerFile, callInfo);
    }
  }

  @Override
  public Object[] findFunctionCallers(@NotNull PyFunction function) {
    VirtualFile calleeFile = getFile(function);
    if (calleeFile != null) {
      return readCallersForFunction(calleeFile, function.getName());
    }

    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public Object[] findFunctionCallees(@NotNull PyFunction function) {
    VirtualFile callerFile = getFile(function);
    if (callerFile != null) {
      return readCalleesForFunction(callerFile, function.getName());
    }

    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  public static VirtualFile getFile(@NotNull PyFunction function) {
    PsiFile file = function.getContainingFile();

    return file != null ? file.getOriginalFile().getVirtualFile() : null;
  }

  @Override
  public PyHierarchyCalleeData[] findFunctionCallees(@NotNull PyFunction function) {
    return new PyHierarchyCalleeData[0];
  }

  @Override
  public void clearCache() {
    final Ref<Boolean> deleted = Ref.create(false);
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      @Override
      public void run() {
        ProjectFileIndex.SERVICE.getInstance(myProject).iterateContent(new ContentIterator() {
          @Override
          public boolean processFile(VirtualFile fileOrDir) {
            if (readCallerAttributeFromFile(fileOrDir) != null || readCalleeAttributeFromFile(fileOrDir) != null) {
              writeCallerAttribute(fileOrDir, "");
              writeCalleeAttribute(fileOrDir, "");
              deleted.set(true);
            }
            if (ProgressManager.getInstance().getProgressIndicator().isCanceled()) {
              return false;
            }
            return true;
          }
        });
      }
    }, "Cleaning the cache of dynamically collected call hierarchy data", true, myProject);

    String message;
    if (deleted.get()) {
      message = "Collected call hierarchy data was deleted";
    }
    else {
      message = "Nothing to delete";
    }
    Messages.showInfoMessage(myProject, message, "Delete cache");
  }

  private void recordHierarchyCallerInfo(VirtualFile calleeFile, PyHierarchyCallInfo callInfo) {
    String data = readAttribute(myHierarchyCallersCache, calleeFile);

    String[] lines;
    if (data != null) {
      lines = data.split("\n");
    }
    else {
      lines = ArrayUtil.EMPTY_STRING_ARRAY;
    }

    String name = callInfo.getCalleeName();
    String caller = callInfo.getCallerName();
    boolean found = false;
    int i = 0;
    for (String calls: lines) {
      String[] parts = calls.split("\t");
      if (parts.length > 1 && parts[0].equals(name) && parts[1].equals(caller)) {
        found = true;
        lines[i] = hierarchyCallerDataToString(stringToHierarchyCallerData(calleeFile.getCanonicalPath(), lines[i])
                                                 .addAllCallerLines(callInfo.toPyHierarchyCallerData()));
      }
      i++;
    }
    if (!found) {
      String[] lines2 = new String[lines.length + 1];
      System.arraycopy(lines, 0, lines2, 0, lines.length);

      lines2[lines2.length - 1] = hierarchyCallerDataToString(callInfo.toPyHierarchyCallerData());
      lines = lines2;
    }
    String attrString = StringUtil.join(lines, "\n");
    writeCallerAttribute(calleeFile, attrString);
  }

  private static String hierarchyCallDataToString() {
    return null;
  }

  private static PyHierarchyCallerData stringToHierarchyCallerData(String calleePath, String callerData) {
    String[] parts = callerData.split("\t");
    if (parts.length > 2) {
      String calleeName = parts[0];
      String callerPath = parts[1];
      String callerName = parts[2];
      PyHierarchyCallerData data = new PyHierarchyCallerData(callerPath, calleePath, callerName, calleeName);
      for (int i = 3; i < parts.length; i++) {
        data.addCallerLine(Integer.parseInt(parts[i]));
      }

      return data;
    }
    return null;
  }

  private static String hierarchyCallerDataToString(PyHierarchyCallerData data) {
    return data.getCalleeName()
           + "\t" + data.getCallerFile()
           + "\t" + data.getCallerName()
           + "\t" + StringUtil.join(data.getCallerLines(), "\t");
  }

  private static PyHierarchyCalleeData stringToPyHierarchyCalleeData(String callerPath, String calleeData) {
    String[] parts = calleeData.split("\t");
    if (parts.length > 2) {
      String callerName = parts[0];
      String calleePath = parts[1];
      String calleeName = parts[2];
      PyHierarchyCalleeData data = new PyHierarchyCalleeData(callerPath, calleePath, callerName, calleeName);
      for (int i = 3; i < parts.length; i++) {
        data.addCalleeLine(Integer.parseInt(parts[i]));
      }

      return data;
    }

    return null;
  }

  private String readAttribute(LoadingCache<VirtualFile, String> cache, VirtualFile file) {
    try {
      String attrContent = cache.get(file);
      if (!StringUtil.isEmpty(attrContent)) {
        return attrContent;
      }
    }
    catch (ExecutionException e) {
    }

    return null;
  }

  private void writeCallerAttribute(VirtualFile file, String attrString) {
    String cachedValue = myHierarchyCallersCache.asMap().get(file);
    if (!attrString.equals(cachedValue)) {
      writeAttributeToFile(HIERARCHY_CALLERS_DATA, file,attrString);
    }
  }

  private void writeCalleeAttribute(VirtualFile file, String attrString) {
    String cachedValue = myHierarchyCalleesCache.asMap().get(file);
    if (!attrString.equals(cachedValue)) {
      writeAttributeToFile(HIERARCHY_CALLEES_DATA, file, attrString);
    }
  }

  private static void writeAttributeToFile(FileAttribute fileAttribute, VirtualFile file, String attrString) {
    try {
      fileAttribute.writeAttributeBytes(file, attrString.getBytes());
    }
    catch (IOException e) {
      LOG.warn("Can't write attribute " + file.getCanonicalPath() + " " + attrString);
    }
  }

  private static VirtualFile getCallerFile(PyHierarchyCallInfo callInfo) {
    return LocalFileSystem.getInstance().findFileByPath(callInfo.getCallerFile());
  }

  private static VirtualFile getCalleeFile(PyHierarchyCallInfo callInfo) {
    return LocalFileSystem.getInstance().findFileByPath(callInfo.getCalleeFile());
  }
}
