// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.fileEditor.impl

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockContainerFactory
import com.intellij.ui.docking.DockableContent
import com.intellij.util.childScope
import org.jdom.Element
import org.jetbrains.annotations.NonNls

internal class DockableEditorContainerFactory(private val fileEditorManager: FileEditorManagerImpl) : DockContainerFactory.Persistent {
  companion object {
    const val TYPE: @NonNls String = "file-editors"
  }

  override fun createContainer(content: DockableContent<*>?): DockContainer {
    return createContainer(loadingState = false)
  }

  private fun createContainer(loadingState: Boolean): DockableEditorTabbedContainer {
    var container: DockableEditorTabbedContainer? = null
    @Suppress("DEPRECATION")
    val coroutineScope = fileEditorManager.project.coroutineScope.childScope()
    val splitters = object : EditorsSplitters(fileEditorManager, coroutineScope = coroutineScope) {
      override fun afterFileClosed(file: VirtualFile) {
        container!!.fireContentClosed(file)
      }

      override fun afterFileOpen(file: VirtualFile) {
        container!!.fireContentOpen(file)
      }

      override val isFloating: Boolean
        get() = true
    }
    if (!loadingState) {
      splitters.createCurrentWindow()
    }
    container = DockableEditorTabbedContainer(splitters, true, coroutineScope)
    return container
  }

  override fun loadContainerFrom(element: Element): DockContainer {
    val container = createContainer(true)
    container.splitters.readExternal(element.getChild("state"))
    return container
  }
}