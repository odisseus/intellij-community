// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.issue.BuildIssue
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.pom.Navigatable
import com.intellij.util.PlatformUtils
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.issue.quickfix.GradleSettingsQuickFix
import org.jetbrains.plugins.gradle.jvmcompat.GradleJvmSupportMatrix
import org.jetbrains.plugins.gradle.util.GradleBundle

class UnsupportedGradleJvmIssueChecker : GradleIssueChecker {

  override fun check(issueData: GradleIssueData): BuildIssue? {
    val buildEnvironment = issueData.buildEnvironment ?: return null
    val gradleVersion = GradleVersion.version(buildEnvironment.gradle.gradleVersion)
    val javaHome = buildEnvironment.java.javaHome
    if (!GradleJvmSupportMatrix.isJavaHomeSupportedByIdea(javaHome.path)) {
      val title = GradleBundle.message("gradle.build.issue.gradle.jvm.unsupported.title")
      val description = DescriptionBuilder()
      val oldestSupportedJavaVersion = GradleJvmSupportMatrix.getOldestSupportedJavaVersionByIdea()
      description.addDescription(
        GradleBundle.message("gradle.build.issue.gradle.jvm.unsupported.description", oldestSupportedJavaVersion.feature)
      )
      val gradleSettingsFix = GradleSettingsQuickFix(
        issueData.projectPath, true,
        GradleSettingsQuickFix.GradleJvmChangeDetector,
        GradleBundle.message("gradle.settings.text.jvm.path")
      )
      val isAndroidStudio = "AndroidStudio" == PlatformUtils.getPlatformPrefix()
      val oldestCompatibleJavaVersion = GradleJvmSupportMatrix.suggestOldestSupportedJavaVersion(gradleVersion)
      if (!isAndroidStudio && oldestCompatibleJavaVersion != null) {
        description.addQuickFixPrompt(
          GradleBundle.message("gradle.build.quick.fix.gradle.jvm", oldestCompatibleJavaVersion, gradleSettingsFix.id)
        )
      }
      return object : BuildIssue {
        override val title = title
        override val description = description.toString()
        override val quickFixes = listOf(gradleSettingsFix)
        override fun getNavigatable(project: Project): Navigatable? = null
      }
    }
    return null
  }

  private class DescriptionBuilder {

    private val descriptions = ArrayList<String>()
    private val quickFixPrompts = ArrayList<String>()

    fun addDescription(description: @NlsContexts.DetailedDescription String) {
      descriptions.add(description)
    }

    fun addQuickFixPrompt(prompt: @NlsContexts.DetailedDescription String) {
      quickFixPrompts.add(prompt)
    }

    override fun toString(): String {
      return buildString {
        append(descriptions.joinToString("\n"))
          .append("\n")
        if (quickFixPrompts.isNotEmpty()) {
          append(GradleBundle.message("gradle.build.quick.fix.title"))
            .append("\n")
          append(quickFixPrompts.joinToString("\n") { "- $it" })
            .append("\n")
        }
      }
    }
  }
}