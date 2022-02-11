package com.github.vol0n.utbotcppclion.coverage

import com.intellij.coverage.CoverageFileProvider

class UTBotCoverageFileProvider : CoverageFileProvider {
    override fun getCoverageDataFilePath(): String {
        return ""
    }

    override fun ensureFileExists(): Boolean {
        return true
    }

    override fun isValid(): Boolean {
        return true
    }
}
