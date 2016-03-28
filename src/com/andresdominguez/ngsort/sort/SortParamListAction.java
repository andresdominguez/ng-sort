package com.andresdominguez.ngsort.sort;

import com.andresdominguez.ngsort.CommandRunner;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.javascript.psi.JSParameterList;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

public class SortParamListAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
    final JSParameterList parameterList = PsiTreeUtil.getParentOfType(element, JSParameterList.class);
    if (parameterList == null) {
      HintManager.getInstance().showErrorHint(editor, "Can't find parameter list");
      return;
    }

    final Document document = editor.getDocument();
    CommandRunner.runCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        NgSorter.sortFunctionArgs(parameterList, document);
      }
    });
  }
}
