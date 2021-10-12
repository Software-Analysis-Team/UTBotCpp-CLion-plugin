package com.github.vol0n.utbotcppclion.utils

import java.nio.file.Paths

fun relativize(from: String, to: String): String {
        val toPath = Paths.get(to)
        val fromPath = Paths.get(from)
        return fromPath.relativize(toPath).toString()
}