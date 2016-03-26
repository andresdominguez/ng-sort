package com.andresdominguez.ngsort;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.intellij.lang.javascript.psi.JSVarStatement;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortVarsAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    List<JSVariable> jsVariables = findVariables(editor, psiFile);
    if (jsVariables.size() == 0) {
      return;
    }
    JSVariable last = Iterables.getLast(jsVariables);

    final int startOffset = jsVariables.get(0).getTextOffset();
    final int endOffset = last.getTextOffset() + last.getTextLength();
    final String sortedString = getSortedString(jsVariables);

    final Document document = editor.getDocument();
    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        document.replaceString(startOffset, endOffset, sortedString);
      }
    }, "ng sort", null);
  }

  @NotNull
  private String getSortedString(List<JSVariable> list) {
    List<String> names = new ArrayList<>();
    for (JSVariable jsVariable : list) {
      names.add(jsVariable.getText());
    }
    Collections.sort(names);
    return Joiner.on(", ").join(names);
  }

  @NotNull
  private List<JSVariable> findVariables(Editor editor, PsiFile psiFile) {
    PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
    JSVarStatement varStatement = PsiTreeUtil.getParentOfType(element, JSVarStatement.class);
    return Lists.newArrayList(PsiTreeUtil.findChildrenOfType(varStatement, JSVariable.class));
  }
}
