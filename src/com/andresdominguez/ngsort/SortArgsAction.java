package com.andresdominguez.ngsort;

import com.google.common.collect.Lists;
import com.intellij.lang.javascript.psi.JSParameterList;
import com.intellij.lang.javascript.psi.JSProperty;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTag;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SortArgsAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    final CommentAndParamList commentAndParamList = findCommentAndParamList(psiFile);
    if (commentAndParamList == null) {
      return;
    }

    final NgSorter ngSorter = new NgSorter(editor.getDocument(), commentAndParamList);

    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        ngSorter.sort();
      }
    }, "ng sort", null);
  }

  @Nullable
  private CommentAndParamList findCommentAndParamList(PsiFile psiFile) {
    List<Integer> indices = findNgInjectIndices(psiFile);
    if (indices.size() == 0) {
      return null;
    }
    int index = indices.get(0);

    PsiElement ngInjectElement = psiFile.findElementAt(index);
    PsiComment comment = PsiTreeUtil.getParentOfType(ngInjectElement, PsiComment.class);
    JSProperty jsProperty = PsiTreeUtil.getParentOfType(ngInjectElement, JSProperty.class);

    Collection<JSParameterList> parameterLists = PsiTreeUtil.findChildrenOfType(jsProperty, JSParameterList.class);
    if (parameterLists.size() != 1) {
      return null;
    }
    JSParameterList parameterList = parameterLists.iterator().next();

    return new CommentAndParamList(parameterList, comment);
  }

  List<Integer> findNgInjectIndices(PsiFile psiFile) {
    String text = psiFile.getText();
    ArrayList<Integer> indices = Lists.newArrayList();

    int index = text.indexOf("@ngInject");
    while (index != -1) {
      indices.add(index);
      index = text.indexOf("@ngInject", index + 1);
    }

    return indices;
  }
}
