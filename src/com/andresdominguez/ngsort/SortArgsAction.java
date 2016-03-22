package com.andresdominguez.ngsort;

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

    final Document document = editor.getDocument();

    final CommentAndParamList commentAndParamList = findCommentAndParamList(psiFile);
    if (commentAndParamList == null) {
      return;
    }

//    new NgSorter(document, commentAndParamList);

    final String fileText = document.getText();
    final List<JSDocTag> paramsInComments = NgSorter.findParamsInComments(commentAndParamList.comment);
    final List<JSDocTag> sortedParams = NgSorter.getSortedCommentParams(paramsInComments);

    Collections.reverse(paramsInComments);

    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        // Replace from bottom to top. Start with the function args.
        NgSorter.sortFunctionArgs(commentAndParamList.parameterList, document);

        // Replace @param tags in reverse order.
        for (int i = 0; i < paramsInComments.size(); i++) {
          JSDocTag jsDocTag = sortedParams.get(i);
          String substring = NgSorter.getParamText(fileText, jsDocTag);

          TextRange range = NgSorter.getTextRange(paramsInComments.get(i));

          document.replaceString(range.getStartOffset(), range.getEndOffset(), substring);
        }
      }
    }, "ng sort", null);
  }

  @Nullable
  private CommentAndParamList findCommentAndParamList(PsiFile psiFile) {
    int index = psiFile.getText().indexOf("@ngInject");
    if (index == -1) {
      return null;
    }

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
}
