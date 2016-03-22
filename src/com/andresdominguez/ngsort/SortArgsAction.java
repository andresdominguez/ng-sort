package com.andresdominguez.ngsort;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.intellij.lang.javascript.JSDocTokenTypes;
import com.intellij.lang.javascript.psi.JSParameter;
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
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
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

    final String fileText = document.getText();
    final List<JSDocTag> paramsInComments = findParamsInComments(commentAndParamList.comment);
    final List<JSDocTag> sortedParams = getSortedCommentParams(paramsInComments);

    Collections.reverse(paramsInComments);

    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        // Replace from bottom to top. Start with the function args.
        sortFunctionArgs(commentAndParamList.parameterList, document);

        // Replace @param tags in reverse order.
        for (int i = 0; i < paramsInComments.size(); i++) {
          JSDocTag jsDocTag = sortedParams.get(i);
          String substring = getParamText(fileText, jsDocTag);

          TextRange range = getTextRange(paramsInComments.get(i));

          document.replaceString(range.getStartOffset(), range.getEndOffset(), substring);
        }
      }
    }, "ng sort", null);
  }

  @NotNull
  private String getParamText(String fileText, JSDocTag jsDocTag) {
    TextRange sortedParam = getTextRange(jsDocTag);
    return fileText.substring(sortedParam.getStartOffset(), sortedParam.getEndOffset());
  }

  private TextRange getTextRange(JSDocTag jsDocTag) {
    TextRange tr = jsDocTag.getTextRange();
    int endOffset = tr.getEndOffset();

    PsiElement runner = jsDocTag.getNextSibling();
    while (true) {
      IElementType elementType = runner.getNode().getElementType();
      if (!elementType.equals(JSDocTokenTypes.DOC_COMMENT_END) &&
          !elementType.equals(JSDocTokenTypes.DOC_TAG_NAME) &&
          !elementType.equals(JSDocTokenTypes.DOC_TAG)) {
        endOffset = runner.getTextRange().getEndOffset();
        runner = runner.getNextSibling();
      } else {
        return new TextRange(tr.getStartOffset(), endOffset);
      }
    }
  }

  @NotNull
  private List<JSDocTag> getSortedCommentParams(List<JSDocTag> paramsInComments) {
    List<JSDocTag> sortedParams = new ArrayList<>(paramsInComments);
    Collections.sort(sortedParams, new Comparator<JSDocTag>() {
      @Override
      public int compare(JSDocTag left, JSDocTag right) {
        String rightText = right.getDocCommentData() == null ? "" : right.getDocCommentData().getText();
        String leftText = left.getDocCommentData() == null ? "" : left.getDocCommentData().getText();
        return rightText.compareTo(leftText);
      }
    });
    return sortedParams;
  }

  @NotNull
  private List<JSDocTag> findParamsInComments(PsiComment comment) {
    return Lists.newArrayList(Iterables.filter(
        PsiTreeUtil.findChildrenOfType(comment, JSDocTag.class),
        new Predicate<JSDocTag>() {
          @Override
          public boolean apply(JSDocTag docTag) {
            return "param".equals(docTag.getName());
          }
        }));
  }

  private void sortFunctionArgs(JSParameterList parameterList, Document document) {
    List<String> sortedArgs = new ArrayList<>();
    for (JSParameter parameter : parameterList.getParameters()) {
      sortedArgs.add(parameter.getName());
    }
    Collections.sort(sortedArgs);
    String args = Joiner.on(", ").join(sortedArgs);

    int startOffset = parameterList.getTextOffset() + 1;
    int endOffset = parameterList.getTextLength() + startOffset - 2;

    document.replaceString(startOffset, endOffset, args);
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

  class CommentAndParamList {
    final JSParameterList parameterList;
    final PsiComment comment;

    CommentAndParamList(JSParameterList parameterList, PsiComment comment) {
      this.parameterList = parameterList;
      this.comment = comment;
    }
  }
}
