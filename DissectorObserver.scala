/**
  * Copyright 2018, Tencent Inc.
  * All rights reserved.
  *
  * @Author: atlasliao <atlasliao@tencent.com>
  */

package com.gdt.log_process.experimental.dragon4sc

import scala.collection.mutable

trait DissectorObserver {
  def onUpdated(path: String, value: String, repetitionLevel: Int, definitionLevel: Int): Unit
  def finish(): Unit
}

class PrinterObserver extends DissectorObserver {
  val table = mutable.Map.empty[String, List[(String, Int, Int)]]
    .withDefaultValue(List.empty[(String, Int, Int)])

  override def onUpdated(path: String,
                         value: String,
                         repetitionLevel: Int,
                         definitionLevel: Int): Unit = {
    table(path) = table(path) :+ (value, repetitionLevel, definitionLevel)
  }

  private def printTable(): Unit = {
    // scalastyle:off println
    table.foreach{ case(k, v) =>
      v.foreach{ i =>
        println(s"$k: val=${i._1} r=${i._2} d=${i._3}")
      }
      println()
    }
    // scalastyle:on println
  }

  override def finish(): Unit = {
    printTable()
  }
}
