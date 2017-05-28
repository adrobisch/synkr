package com.drobisch.synkr.util

import java.awt._
import java.awt.event.ActionListener
import javax.swing.UIManager

class SystemTray {

  def createTray = {
    if (SystemTray.isSupported()) {
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")

      val tray = SystemTray.getSystemTray()
      val image = Toolkit.getDefaultToolkit().getImage("/usr/share/icons/gnome/32x32/actions/reload.png")
      val popup = new PopupMenu()
      val item = new MenuItem("Exit")

      popup.add(item)

      val trayIcon = new TrayIcon(image, "Synkr", popup)
      trayIcon.setImageAutoSize(true)

      val listener = new ActionListener() {
        @Override
        def actionPerformed(arg0: java.awt.event.ActionEvent) {
          System.exit(0)
        }
      }

      item.addActionListener(listener)

      try{
        tray.add(trayIcon)
      }catch {
        case e: Exception => System.err.println("Can't add to tray")
      }
    } else {
      System.err.println("Tray unavailable")
    }
  }

}
