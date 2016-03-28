package com.andresdominguez.ngsort.googrequire;

import com.andresdominguez.ngsort.CommandRunner;
import com.google.common.collect.Iterables;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.javascript.psi.JSExpressionStatement;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class GoogVarAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    PsiElement currentElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
    JSExpressionStatement exprStmt = PsiTreeUtil.getParentOfType(currentElement, JSExpressionStatement.class);
    if (exprStmt == null) {
      HintManager.getInstance().showErrorHint(editor, "Can't find goog.require");
      return;
    }

    final Document document = editor.getDocument();
    final int offset = exprStmt.getTextOffset();
    final String varName = getVarName(exprStmt);

    CommandRunner.runCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        document.replaceString(offset, offset, varName);
      }
    });
  }

  private String getVarName(JSExpressionStatement expressionStatement) {
    ArrayList<JSLiteralExpression> expressions = new ArrayList<>(
        PsiTreeUtil.findChildrenOfType(expressionStatement, JSLiteralExpression.class));
    String text = expressions.get(0).getText();
    String lastToken = Iterables.getLast(Arrays.asList(text.replaceAll("'", "").split("\\.")));
    return String.format("var %s = ", lastToken);
  }
}
