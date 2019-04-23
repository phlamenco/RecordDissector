/**
  * Copyright 2018, Tencent Inc.
  * All rights reserved.
  *
  * @Author: atlasliao <atlasliao@tencent.com>
  */

package com.gdt.log_process.experimental.dragon4sc

import com.google.protobuf.Message

object Dissector {

  def dissectRecord(message: Message, tree: Tree, observer: DissectorObserver): Unit = tree match {
    case node: Node =>
      node.dissectRecord(message, 0, 0, observer)
      observer.finish()
    case leaf: Leaf =>
      throw new RuntimeException("invalid argument")
  }
//
//
//
//  def dissectNode(message: Message,
//                  node: Node,
//                  parentPath: String,
//                  rLevel: Int,
//                  dLevel: Int,
//                  observer: DissectorObserver): Unit = {
//    node.children.foreach { childNode =>
//      val fieldDescriptor = childNode.elem.fieldDescriptor
//      val fullPath = linkPath(parentPath, fieldDescriptor.getName)
//      childNode match {
//        case n: Node =>
//          if (message == null) {
//            dissectNode(message, n, fullPath, rLevel, dLevel, observer)
//          } else {
//            if (fieldDescriptor.isRepeated) {
//              dissectRepeatedTree(message, n, fullPath, rLevel, dLevel, observer)
//            } else {
//              if (message.hasField(fieldDescriptor)) {
//                val msg = message.getField(fieldDescriptor)
//                dissectNode(msg.asInstanceOf[Message], n, fullPath,
  // rLevel, n.elem.dLevel, observer)
//              } else {
//                dissectNode(null, n, fullPath, rLevel, dLevel, observer)
//              }
//            }
//          }
//        case l: Leaf =>
//          if (message == null) {
//            observer.onUpdated(fullPath, "null", rLevel, dLevel)
//          } else {
//            if (fieldDescriptor.isRepeated) {
//              dissectRepeatedTree(message, l, fullPath, rLevel, dLevel, observer)
//            } else {
//              if (message.hasField(fieldDescriptor)) {
//                val value = getLeafPremitiveStrVal(
  // message.getField(fieldDescriptor), fieldDescriptor)
//                observer.onUpdated(fullPath, value, rLevel, l.elem.dLevel)
//              } else {
//                observer.onUpdated(fullPath, "null", rLevel, dLevel)
//              }
//            }
//          }
//      }
//    }
//  }
//
//  def dissectRepeatedTree(message: Message,
//                          tree: Tree,
//                          path: String,
//                          rLevel: Int,
//                          dLevel: Int,
//                          observer: DissectorObserver): Unit = tree match {
//    case node: Node =>
//      val fieldDescriptor = node.elem.fieldDescriptor
//      val c = message.getRepeatedFieldCount(fieldDescriptor)
//      if (c == 0) {
//        dissectNode(null, node, path, rLevel, dLevel, observer)
//      } else {
//        for (i <- 0 until c) {
//          val msg = message.getRepeatedField(fieldDescriptor, i)
//          if (i == 0) {
//            dissectNode(msg.asInstanceOf[Message], node, path, rLevel, node.elem.dLevel, observer)
//          } else {
//            dissectNode(msg.asInstanceOf[Message], node, path,
  // node.elem.rLevel, node.elem.dLevel, observer)
//          }
//        }
//      }
//
//    case leaf: Leaf =>
//      val fieldDescriptor = leaf.elem.fieldDescriptor
//      val count = message.getRepeatedFieldCount(fieldDescriptor)
//      if (count == 0) {
//        observer.onUpdated(path, "null", rLevel, dLevel)
//      } else {
//        for (i <- 0 until count) {
//          val field = message.getRepeatedField(fieldDescriptor, i)
//          val value = getLeafPremitiveStrVal(field, fieldDescriptor)
//          if (i == 0) {
//            observer.onUpdated(path, value, rLevel, leaf.elem.dLevel)
//          } else {
//            observer.onUpdated(path, value, leaf.elem.rLevel, leaf.elem.dLevel)
//          }
//        }
//      }
//  }

}
