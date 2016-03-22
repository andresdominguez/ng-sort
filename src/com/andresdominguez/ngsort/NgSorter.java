package com.andresdominguez.ngsort;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.intellij.lang.javascript.JSDocTokenTypes;
import com.intellij.lang.javascript.psi.JSParameter;
import com.intellij.lang.javascript.psi.JSParameterList;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTag;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NgSorter {

  private final Document document;
  private final CommentAndParamList commentAndParamList;

  public NgSorter(Document document, CommentAndParamList commentAndParamList) {
    this.document = document;
    this.commentAndParamList = commentAndParamList;
  }

  static void sortFunctionArgs(JSParameterList parameterList, Document document) {
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

  @NotNull
  static List<JSDocTag> findParamsInComments(PsiComment comment) {
    return Lists.newArrayList(Iterables.filter(
        PsiTreeUtil.findChildrenOfType(comment, JSDocTag.class),
        new Predicate<JSDocTag>() {
          @Override
          public boolean apply(JSDocTag docTag) {
            return "param".equals(docTag.getName());
          }
        }));
  }

  @NotNull
  static List<JSDocTag> getSortedCommentParams(List<JSDocTag> paramsInComments) {
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

  static TextRange getTextRange(JSDocTag jsDocTag) {
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
  static String getParamText(String fileText, JSDocTag jsDocTag) {
    TextRange sortedParam = getTextRange(jsDocTag);
    return fileText.substring(sortedParam.getStartOffset(), sortedParam.getEndOffset());
  }
}
