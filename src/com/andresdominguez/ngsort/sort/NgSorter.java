package com.andresdominguez.ngsort.sort;

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

import java.util.*;

class NgSorter {

  private final Document document;
  private final CommentAndParamList commentAndParamList;

  public NgSorter(Document document, CommentAndParamList commentAndParamList) {
    this.document = document;
    this.commentAndParamList = commentAndParamList;
  }

  static List<PsiElement> sortByText(Collection<PsiElement> elements) {
    List<PsiElement> list = new ArrayList<>(elements);
    Collections.sort(list, new Comparator<PsiElement>() {
      @Override
      public int compare(PsiElement o1, PsiElement o2) {
        return o2.getText().compareTo(o1.getText());
      }
    });
    return list;
  }

  public void sort() {
    final String fileText = document.getText();
    final List<JSDocTag> paramsInComments = findParamsInComments(commentAndParamList.comment);
    final List<JSDocTag> sortedParams = getSortedCommentParams(paramsInComments);

    Collections.reverse(paramsInComments);

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

  static void changeElementsOrder(Document document, List<PsiElement> fromOrder, List<PsiElement> toOrder) {
    for (int i = 0, fromOrderSize = fromOrder.size(); i < fromOrderSize; i++) {
      PsiElement fromElement = fromOrder.get(i);
      PsiElement toElement = toOrder.get(i);

      TextRange textRange = fromElement.getTextRange();
      document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), toElement.getText());
    }
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
  private String getParamText(String fileText, JSDocTag jsDocTag) {
    TextRange sortedParam = getTextRange(jsDocTag);
    return fileText.substring(sortedParam.getStartOffset(), sortedParam.getEndOffset());
  }
}
