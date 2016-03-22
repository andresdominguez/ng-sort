package com.andresdominguez.ngsort;

import com.google.common.collect.Lists;
import com.intellij.lang.javascript.psi.JSParameterList;
import com.intellij.lang.javascript.psi.JSProperty;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SortArgsAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    final List<CommentAndParamList> list = findAllCommentsAndParams(psiFile);
    if (list.size() == 0) {
      return;
    }

    final Document document = editor.getDocument();

    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        for (CommentAndParamList commentAndParamList : list) {
          new NgSorter(document, commentAndParamList).sort();
        }
      }
    }, "ng sort", null);
  }

  @Nullable
  private CommentAndParamList findCommentAndParamList(PsiElement ngInjectElement) {
    PsiComment comment = PsiTreeUtil.getParentOfType(ngInjectElement, PsiComment.class);
    JSProperty jsProperty = PsiTreeUtil.getParentOfType(ngInjectElement, JSProperty.class);

    Collection<JSParameterList> parameterLists = PsiTreeUtil.findChildrenOfType(jsProperty, JSParameterList.class);
    if (parameterLists.size() != 1) {
      return null;
    }
    JSParameterList parameterList = parameterLists.iterator().next();

    return new CommentAndParamList(parameterList, comment);
  }

  @NotNull
  private List<CommentAndParamList> findAllCommentsAndParams(PsiFile psiFile) {
    List<CommentAndParamList> list = Lists.newArrayList();
    for (Integer index : findNgInjectIndices(psiFile)) {
      CommentAndParamList commentAndParamList = findCommentAndParamList(psiFile.findElementAt(index));
      if (commentAndParamList != null) {
        list.add(commentAndParamList);
      }
    }

    Collections.reverse(list);
    return list;
  }

  List<Integer> findNgInjectIndices(PsiFile psiFile) {
    String text = psiFile.getText();
    List<Integer> indices = Lists.newArrayList();

    int index = text.indexOf("@ngInject");
    while (index != -1) {
      indices.add(index);
      index = text.indexOf("@ngInject", index + 1);
    }

    return indices;
  }
}
