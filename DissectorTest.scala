/**
  * Copyright 2018, Tencent Inc.
  * All rights reserved.
  *
  * @Author: atlasliao <atlasliao@tencent.com>
  */

package com.gdt.log_process.experimental.dragon4sc

import com.gdt.log_process.experimental.dragon4sc.DocumentProtos.Document
import com.google.protobuf.TextFormat
import org.scalatest.FunSuite

import scala.collection.mutable

class DissectorTest extends FunSuite {

  private val baseTable = Map[String, List[(String, Int, Int)]](
    "doc_id" -> List(
      ("10", 0, 0),
      ("20", 0, 0)),
    "name.url" -> List(
      ("http://A", 0, 2),
      ("http://B", 1, 2),
      ("null", 1, 1),
      ("http://C", 0, 2)),
    "links.forward" -> List(
      ("20", 0, 2),
      ("40", 1, 2),
      ("60", 1, 2),
      ("80", 0, 2)),
    "links.backward" -> List(
      ("null", 0, 1),
      ("10", 0, 2),
      ("30", 1, 2)),
    "name.language.code" -> List(
      ("en-us", 0, 2),
      ("en", 2, 2),
      ("null", 1, 1),
      ("en-gb", 1, 2),
      ("null", 0, 1)),
    "name.language.country" -> List(
      ("us", 0, 3),
      ("null", 2, 2),
      ("null", 1, 1),
      ("gb", 1, 3),
      ("null", 0, 1))
  )

  private def checkTable(table: mutable.Map[String, List[(String, Int, Int)]]): Unit = {
    baseTable.foreach{ case (k, v) =>
      assert(table(k).sameElements(v))
    }
  }

  test("field info construct and message dissect") {
    val pbTxt =
      """
        |doc_id: 10
        |links {
        |  forward: 20
        |  forward: 40
        |  forward: 60
        |}
        |name {
        |  language {
        |    code: "en-us"
        |    country: "us"
        |  }
        |  language {
        |    code: "en"
        |  }
        |  url: "http://A"
        |}
        |name {
        |  url: "http://B"
        |}
        |name {
        |  language {
        |    code: "en-gb"
        |    country: "gb"
        |  }
        |}
      """.stripMargin
    val builder = Document.newBuilder
    TextFormat.merge(pbTxt, builder)
    val doc = builder.build()
    val tree = FieldInfo.initTreeBySchema(Document.getDescriptor)
    FieldInfo.printTree(tree)
    val observer = new PrinterObserver

    val pbTxt2 =
      """
        |doc_id: 20
        |links {
        |  backward: 10
        |  backward: 30
        |  forward: 80
        |}
        |name {
        |  url: "http://C"
        |}
      """.stripMargin
    val builder2 = Document.newBuilder
    TextFormat.merge(pbTxt2, builder2)
    val doc2 = builder2.build()
    Dissector.dissectRecord(doc, tree, observer)
    Dissector.dissectRecord(doc2, tree, observer)
    checkTable(observer.table)
  }
}
