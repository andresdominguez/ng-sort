package com.andresdominguez.ngsort;

import com.intellij.lang.javascript.psi.JSBlockStatement;
import com.intellij.lang.javascript.psi.JSFunctionExpression;
import com.intellij.lang.javascript.psi.JSParameterList;
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

    final JSParameterList paramList = PsiTreeUtil.findChildOfType(functionExpr, JSParameterList.class);
    if (paramList == null) {
      return;
    }

    final JSBlockStatement fnBlock = PsiTreeUtil.findChildOfType(functionExpr, JSBlockStatement.class);
    if (fnBlock == null) {
      return;
    }

    final TextRange fnBlockRange = fnBlock.getTextRange();
    final TextRange paramListRange = paramList.getTextRange();

    final Document document = editor.getDocument();
    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        // Add =>
        document.replaceString(fnBlockRange.getStartOffset(), fnBlockRange.getEndOffset(),
            String.format("=> %s", fnBlock.getText()));

        // Migrate function() to ()
        boolean shouldDropParens = paramList.getParameters().length == 1;
        if (shouldDropParens) {
          int startOffset = paramListRange.getStartOffset();
          int endOffset = paramListRange.getEndOffset();
          String replacement = paramList.getText().replace("(", "").replace(")", "");
          document.replaceString(startOffset, endOffset, replacement);
        }

        int startOffset = functionExpr.getTextRange().getStartOffset();
        int endOffset = paramListRange.getStartOffset();
        document.replaceString(startOffset, endOffset, "");
      }
    }, "ng sort", null);

  }
}
