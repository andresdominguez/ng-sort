package com.andresdominguez.ngsort;

import com.intellij.lang.javascript.psi.JSBlockStatement;
import com.intellij.lang.javascript.psi.JSFunctionExpression;
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

public class ToArrowFunctionAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
    final JSFunctionExpression functionExpr = PsiTreeUtil.getParentOfType(element, JSFunctionExpression.class);
    if (functionExpr == null) {
      return;
    }

    final JSBlockStatement fnBlock = PsiTreeUtil.findChildOfType(functionExpr, JSBlockStatement.class);
    if (fnBlock == null) {
      return;
    }

    final Document document = editor.getDocument();
    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        // Add =>
        TextRange fnBlockRange = fnBlock.getTextRange();
        document.replaceString(fnBlockRange.getStartOffset(), fnBlockRange.getEndOffset(),
            String.format("=> %s", fnBlock.getText()));

        // Migrate function() to ()
        int startOffset = functionExpr.getTextRange().getStartOffset();
        int endOffset = startOffset + functionExpr.getText().indexOf("(");
        document.replaceString(startOffset, endOffset, "");
      }
    }, "ng sort", null);

  }
}
