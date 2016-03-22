package com.andresdominguez.ngsort;

import com.intellij.lang.javascript.psi.jsdoc.JSDocTag;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTagValue;
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

public class NgRequiredAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
    final JSDocTag jsDocTag = PsiTreeUtil.getParentOfType(element, JSDocTag.class);

    if (jsDocTag == null || jsDocTag.getValue() == null) {
      return;
    }
    final JSDocTagValue value = jsDocTag.getValue();
    final TextRange textRange = value.getTextRange();

    final Document document = editor.getDocument();
    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        String replacement = value.getText().replace("{", "{!");
        document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), replacement);
      }
    }, "ng sort", null);
  }
}
