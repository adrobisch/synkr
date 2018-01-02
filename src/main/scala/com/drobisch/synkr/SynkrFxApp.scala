package com.drobisch.synkr

import javafx.scene.layout

import com.drobisch.synkr.util.SystemTraySupport

import scala.util.Try
import scalafx.application.JFXApp
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.{TableCell, TableColumn, TableView}
import scalafx.scene.layout.{Priority, VBox}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Circle

class Person(firstName_ : String,
             lastName_ : String,
             favoriteColor_ : Color) {
  val firstName = new StringProperty(this, "firstName", firstName_)
  val lastName = new StringProperty(this, "lastName", lastName_)
  val favoriteColor = new ObjectProperty(this, "favoriteColor", favoriteColor_)
}

object SynkrFxApp extends JFXApp {
  val tray = Try(SystemTraySupport.createTray(("Exit", _ => System.exit(0))))

  val characters = ObservableBuffer[Person](
    new Person("Peggy", "Sue", Color.Violet),
    new Person("Rocky", "Raccoon", Color.GreenYellow),
    new Person("Bungalow ", "Bill", Color.DarkSalmon)
  )

  val table = new TableView[Person](characters) {
    columns ++= List(
      new TableColumn[Person, String] {
        text = "First Name"
        cellValueFactory = { _.value.firstName }
        prefWidth = 100
      }.delegate,
      new TableColumn[Person, String]() {
        text = "Last Name"
        cellValueFactory = { _.value.lastName }
        prefWidth = 100
      }.delegate,
      new TableColumn[Person, Color] {
        text = "Favorite Color"
        cellValueFactory = { _.value.favoriteColor }
        // Render the property value when it changes,
        // including initial assignment
        cellFactory = { _ =>
          new TableCell[Person, Color] {
            item.onChange { (_, _, newColor) =>
              graphic =
                if (newColor != null)
                  new Circle {
                    fill = newColor
                    radius = 8
                  }
                else
                  null
            }
          }
        }
        prefWidth = 100
      }.delegate
    )
  }

  stage = new JFXApp.PrimaryStage {
    title.value = "Synkr"
    width = 800
    height = 600
    scene = new Scene {
      root = new VBox {
        children = table
        hgrow = Priority(layout.Priority.ALWAYS)
        vgrow = Priority(layout.Priority.ALWAYS)
      }
    }
  }
}
