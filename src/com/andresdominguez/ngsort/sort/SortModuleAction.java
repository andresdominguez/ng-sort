package com.andresdominguez.ngsort.sort;

import com.andresdominguez.ngsort.CommandRunner;
import com.andresdominguez.ngsort.Sorter;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.javascript.psi.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
      HintManager.getInstance().showErrorHint(editor, "Can't find Angular module");
      return;
    }
    PsiElement element = psiFile.findElementAt(ngModuleIndex);
    JSCallExpression callExpression = PsiTreeUtil.getParentOfType(element, JSCallExpression.class);
    JSArrayLiteralExpression arrayLiteralExpression = PsiTreeUtil.findChildOfType(callExpression, JSArrayLiteralExpression.class);

    final List<PsiElement> moduleElements = findArrayElements(arrayLiteralExpression);
    final Document document = editor.getDocument();
    final List<PsiElement> sorted = Sorter.sortByText(moduleElements);

    Collections.reverse(moduleElements);

    CommandRunner.runCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        Sorter.changeElementsOrder(document, moduleElements, sorted);
      }
    });
  }

  @NotNull
  private List<PsiElement> findArrayElements(JSArrayLiteralExpression arrayLiteralExpr) {
    List<PsiElement> moduleElements = new ArrayList<>();
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
}
