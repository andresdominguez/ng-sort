package com.andresdominguez.ngsort;

import com.intellij.lang.javascript.psi.JSParameterList;
import com.intellij.psi.PsiComment;

public class CommentAndParamList {
  final JSParameterList parameterList;
  final PsiComment comment;

  CommentAndParamList(JSParameterList parameterList, PsiComment comment) {
    this.parameterList = parameterList;
    this.comment = comment;
  }
}
