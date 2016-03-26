package com.andresdominguez.ngsort.sort;

import com.intellij.lang.javascript.psi.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SortModuleAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    // Find ng module.
    int ngModuleIndex = psiFile.getText().indexOf("angular.module");
    if (ngModuleIndex == -1) {
      return;
    }
    PsiElement element = psiFile.findElementAt(ngModuleIndex);
    JSCallExpression callExpression = PsiTreeUtil.getParentOfType(element, JSCallExpression.class);
    JSArrayLiteralExpression arrayLiteralExpression = PsiTreeUtil.findChildOfType(callExpression, JSArrayLiteralExpression.class);

    final List<JSExpression> moduleElements = findArrayElements(arrayLiteralExpression);
    final Document document = editor.getDocument();
    final List<JSExpression> sorted = sort(moduleElements);

    Collections.reverse(moduleElements);

    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        changePositions(document, moduleElements, sorted);
      }
    }, "ng sort", null);
  }

  @NotNull
  private List<JSExpression> findArrayElements(JSArrayLiteralExpression arrayLiteralExpr) {
    List<JSExpression> moduleElements = new ArrayList<>();
    Collection<JSExpression> items =
        PsiTreeUtil.findChildrenOfAnyType(arrayLiteralExpr, JSLiteralExpression.class, JSReferenceExpression.class);
    for (JSExpression jsExpression : items) {
      String text = jsExpression.getText();
      if (text.contains("'") || text.contains(".name")) {
        moduleElements.add(jsExpression);
      }
    }
    return moduleElements;
  }

  private List<JSExpression> sort(Collection<JSExpression> expressions) {
    ArrayList<JSExpression> list = new ArrayList<>(expressions);
    Collections.sort(list, new Comparator<PsiElement>() {
      @Override
      public int compare(PsiElement o1, PsiElement o2) {
        return o2.getText().compareTo(o1.getText());
      }
    });
    return list;
  }

  private void changePositions(Document document, List<JSExpression> oldOrderReversed, List<JSExpression> newOrder) {
    for (int i = 0, newOrderReversedSize = newOrder.size(); i < newOrderReversedSize; i++) {
      JSExpression oldExpr = oldOrderReversed.get(i);
      PsiElement psiElement = newOrder.get(i);

      TextRange textRange = oldExpr.getTextRange();
      document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), psiElement.getText());
    }
  }
}
