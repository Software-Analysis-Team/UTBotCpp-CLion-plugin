package com.github.vol0n.utbotcppclion.coverage

import com.github.vol0n.utbotcppclion.RunConfig.UTBotRunWithCoverageConfig
import com.intellij.coverage.CoverageFileProvider

class UTBotCoverageFileProvider(val config: UTBotRunWithCoverageConfig) : CoverageFileProvider {
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

