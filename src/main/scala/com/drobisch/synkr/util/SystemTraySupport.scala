package com.drobisch.synkr.util

import java.awt.event.ActionListener

import dorkbox.systemTray
import dorkbox.systemTray.SystemTray

import scala.util.Try

object SystemTraySupport {

  def createTray(items: (String, Unit => Unit)*): Option[SystemTray] = {
    val availableTray: Option[SystemTray] = Option(systemTray.SystemTray.get())
    availableTray.foreach { tray =>
      Try(tray.setImage("/usr/share/icons/gnome/32x32/actions/reload.png"))

      items.foreach {
        case (name, handler) =>
          tray.getMenu.add(new systemTray.MenuItem(name, new ActionListener() {
            @Override
            def actionPerformed(arg0: java.awt.event.ActionEvent) {
              handler()
            }
          }))
      }

    }
    availableTray
  }

}
