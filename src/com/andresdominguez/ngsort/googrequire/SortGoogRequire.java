package com.andresdominguez.ngsort.googrequire;

import com.andresdominguez.ngsort.CommandRunner;
import com.andresdominguez.ngsort.Sorter;
import com.google.common.base.Function;
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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortGoogRequire extends AnAction {

  private final Function<RequireAndVarName, PsiElement> TO_PSI_ELEMENT = new Function<RequireAndVarName, PsiElement>() {
    @Override
    public PsiElement apply(RequireAndVarName requireAndVarName) {
      return requireAndVarName.varStatement;
    }
  };

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

    final List<PsiElement> sortedElements = sortByVarName(list);
    final List<PsiElement> elementList = Lists.transform(list, TO_PSI_ELEMENT);

    CommandRunner.runCommand(getEventProject(e), new Runnable() {
      @Override
      public void run() {
        Sorter.changeElementsOrder(document, elementList, sortedElements);
      }
    });
  }

  @NotNull
  private List<PsiElement> sortByVarName(List<RequireAndVarName> list) {
    List<RequireAndVarName> sortedCopy = Lists.newArrayList(list);
    Collections.sort(sortedCopy, new Comparator<RequireAndVarName>() {
      @Override
      public int compare(RequireAndVarName left, RequireAndVarName right) {
        return right.varName.compareTo(left.varName);
      }
    });
    return Lists.transform(sortedCopy, TO_PSI_ELEMENT);
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

  private class RequireAndVarName {
    final String varName;
    final JSVarStatement varStatement;

    RequireAndVarName(String varName, JSVarStatement varStatement) {
      this.varName = varName;
      this.varStatement = varStatement;
    }
  }
}
