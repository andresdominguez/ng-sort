package com.andresdominguez.ngsort;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSVarStatement;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortGoogRequire extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    if (editor == null || psiFile == null) {
      return;
    }

    final Document document = editor.getDocument();

    final List<RequireAndVarName> list = findGoogVarsWithGoogRequire(psiFile);
    // Sort form bottom to top.
    Collections.reverse(list);
    final List<RequireAndVarName> sortedCopy = sortByVarName(list);

    CommandProcessor.getInstance().executeCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
          JSVarStatement originalVar = list.get(i).varStatement;
          JSVarStatement replacementVar = sortedCopy.get(i).varStatement;

          TextRange textRange = originalVar.getTextRange();

          document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), replacementVar.getText());
        }
      }
    }, "ng sort", null);

  }

  @NotNull
  private List<RequireAndVarName> sortByVarName(List<RequireAndVarName> cholo) {
    List<RequireAndVarName> sortedCopy = Lists.newArrayList(cholo);
    Collections.sort(sortedCopy, new Comparator<RequireAndVarName>() {
      @Override
      public int compare(RequireAndVarName left, RequireAndVarName right) {
        return right.varName.compareTo(left.varName);
      }
    });
    return sortedCopy;
  }

  @NotNull
  private List<RequireAndVarName> findGoogVarsWithGoogRequire(PsiFile psiFile) {
    List<RequireAndVarName> result = new ArrayList<>();
    for (JSCallExpression jsCallExpression : findGoogRequires(psiFile)) {
      JSVarStatement varStatement = PsiTreeUtil.getParentOfType(jsCallExpression, JSVarStatement.class);
      JSVariable jsVariable = PsiTreeUtil.getParentOfType(jsCallExpression, JSVariable.class);

      if (jsVariable != null) {
        result.add(new RequireAndVarName(jsVariable.getText(), varStatement));
      }
    }

    return result;
  }

  @NotNull
  private List<JSCallExpression> findGoogRequires(PsiFile psiFile) {
    return Lists.newArrayList(Iterables.filter(
        PsiTreeUtil.findChildrenOfType(psiFile, JSCallExpression.class),
        new Predicate<JSCallExpression>() {
          @Override
          public boolean apply(JSCallExpression jsCallExpression) {
            return jsCallExpression.getText().startsWith("goog.require");
          }
        }));
  }

  class RequireAndVarName {
    final String varName;
    final JSVarStatement varStatement;

    RequireAndVarName(String varName, JSVarStatement varStatement) {
      this.varName = varName;
      this.varStatement = varStatement;
    }
  }
}
