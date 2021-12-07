package com.github.vol0n.utbotcppclion.ui

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.psi.OCStruct

/*
class GeneratedTestsLineMarkerProvider: LineMarkerProviderDescriptor() {
    override fun getName() = "Run tests"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*> {
        if (element is PsiIdentifier &&
                       (parent = element.getParent()) instanceof PsiMethod &&
                       ((PsiMethod)parent).getMethodIdentifier() == element))  // aha, we are looking at method name
                 return new LineMarkerInfo(element, element.getTextRange(), icon, null,null, alignment);
               else
                 return null;
    }
}
 */