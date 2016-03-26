package com.andresdominguez.ngsort;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

import java.util.*;

public class Sorter {
  public static List<PsiElement> sortByText(Collection<PsiElement> elements) {
    List<PsiElement> list = new ArrayList<>(elements);
    Collections.sort(list, new Comparator<PsiElement>() {
      @Override
      public int compare(PsiElement o1, PsiElement o2) {
        return o2.getText().compareTo(o1.getText());
      }
    });
    return list;
  }

  public static void changeElementsOrder(Document document, List<PsiElement> fromOrder, List<PsiElement> toOrder) {
    for (int i = 0, fromOrderSize = fromOrder.size(); i < fromOrderSize; i++) {
      PsiElement fromElement = fromOrder.get(i);
      PsiElement toElement = toOrder.get(i);

      TextRange textRange = fromElement.getTextRange();
      document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), toElement.getText());
    }
  }
}
