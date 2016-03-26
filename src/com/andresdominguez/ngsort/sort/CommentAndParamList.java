package com.andresdominguez.ngsort.sort;

import com.intellij.lang.javascript.psi.JSParameterList;
import com.intellij.psi.PsiComment;

class CommentAndParamList {
  final JSParameterList parameterList;
  final PsiComment comment;

  CommentAndParamList(JSParameterList parameterList, PsiComment comment) {
    this.parameterList = parameterList;
    this.comment = comment;
  }
}
