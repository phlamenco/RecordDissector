import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.Descriptors.{Descriptor, EnumValueDescriptor, FieldDescriptor}
import com.google.protobuf.{ByteString, Message}
// scalastyle:off
import scala.collection.JavaConverters._
// scalastyle: on

case class FieldInfo(rLevel: Int,
                     dLevel: Int,
                     fieldDescriptor: FieldDescriptor,
                     path: String)

sealed trait Tree {
  def elem: FieldInfo

  def debugString(tab: Int): String

  def dissectRecord(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit

  def onRepeated(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit

  def onNonRepeated(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit

}

case class Node(elem: FieldInfo, children: List[Tree]) extends Tree {
  override def debugString(tab: Int): String = {
    val first = elem.fieldDescriptor match {
      case null => s"${" " * tab}${elem.path} r:${elem.rLevel} d:${elem.dLevel}\n"
      case _ => s"${" " * tab}${elem.path} " +
        s"r:${elem.rLevel} d:${elem.dLevel} i:${elem.fieldDescriptor.getNumber}\n"
    }
    first + children.map(_.debugString(2 + tab)).mkString("\n")
  }

  override def dissectRecord(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit = {
    children.foreach { childNode =>
      if (message == null) {
        childNode.dissectRecord(message, r, d, observer)
      } else {
        val fieldDescriptor = childNode.elem.fieldDescriptor
        if (fieldDescriptor.isRepeated) {
          childNode.onRepeated(message, r, d, observer)
        } else {
          childNode.onNonRepeated(message, r, d, observer)
        }
      }
    }
  }

  override def onRepeated(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit = {
    val fieldDescriptor = elem.fieldDescriptor
    val c = message.getRepeatedFieldCount(fieldDescriptor)
    if (c == 0) {
      dissectRecord(null, r, d, observer)
    } else {
      for (i <- 0 until c) {
        val msg = message.getRepeatedField(fieldDescriptor, i).asInstanceOf[Message]
        if (i == 0) {
          dissectRecord(msg, r, elem.dLevel, observer)
        } else {
          dissectRecord(msg, elem.rLevel, elem.dLevel, observer)
        }
      }
    }
  }

  override def onNonRepeated(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit = {
    if (message.hasField(elem.fieldDescriptor)) {
      val subMessage = message.getField(elem.fieldDescriptor).asInstanceOf[Message]
      dissectRecord(subMessage, r, elem.dLevel, observer)
    } else {
      dissectRecord(null, r, d, observer)
    }
  }
}

case class Leaf(elem: FieldInfo) extends Tree {
  override def debugString(tab: Int): String =
    s"${" " * tab}${elem.path} r:${elem.rLevel}" +
      s" d:${elem.dLevel} i:${elem.fieldDescriptor.getNumber}"

  override def dissectRecord(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit = {
    if (message == null) {
      observer.onUpdated(elem.path, "null", r, d)
    } else {
      if (elem.fieldDescriptor.isRepeated) {
        onRepeated(message, r, d, observer)
      } else {
        onNonRepeated(message, r, d, observer)
      }
    }
  }

  override def onRepeated(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit = {
    val fieldDescriptor = elem.fieldDescriptor
    val c = message.getRepeatedFieldCount(fieldDescriptor)
    if (c == 0) {
      observer.onUpdated(elem.path, "null", r, d)
    } else {
      for (i <- 0 until c) {
        val field = message.getRepeatedField(fieldDescriptor, i)
        val value = FieldInfo.getLeafPremitiveStrVal(field, fieldDescriptor)
        if (i == 0) {
          observer.onUpdated(elem.path, value, r, elem.dLevel)
        } else {
          observer.onUpdated(elem.path, value, elem.rLevel, elem.dLevel)
        }
      }
    }
  }

  override def onNonRepeated(message: Message, r: Int, d: Int, observer: DissectorObserver): Unit = {
    if (message.hasField(elem.fieldDescriptor)) {
      val value = message.getField(elem.fieldDescriptor)
      val strVal = FieldInfo.getLeafPremitiveStrVal(value, elem.fieldDescriptor)
      observer.onUpdated(elem.path, strVal, r, elem.dLevel)
    } else {
      observer.onUpdated(elem.path, "null", r, d)
    }
  }
}

object FieldInfo {

  def linkPath(parentPath: String, selfPath: String): String = {
    if (parentPath.isEmpty) {
      selfPath
    } else {
      val builder = new StringBuilder
      builder.append(parentPath)
      builder.append('.')
      builder.append(selfPath)
      builder.toString()
    }
  }

  def getLeafPremitiveStrVal(value: Object, fieldDescriptor: FieldDescriptor): String = {
    fieldDescriptor.getJavaType match {
      case JavaType.INT => value.asInstanceOf[Int].toString
      case JavaType.STRING => value.asInstanceOf[String]
      case JavaType.BOOLEAN => if (value.asInstanceOf[Boolean]) "true" else "false"
      case JavaType.BYTE_STRING => value.asInstanceOf[ByteString].toString
      case JavaType.DOUBLE => value.asInstanceOf[Double].toString
      // maybe incorrect
      case JavaType.ENUM => value.asInstanceOf[EnumValueDescriptor].getNumber.toString
      case JavaType.FLOAT => value.asInstanceOf[Float].toString
      case JavaType.LONG => value.asInstanceOf[Long].toString
      case _ => throw new RuntimeException("unhandled javaType for message")
    }
  }

  def constructMessage(descriptor: Descriptor,
                       repetitionLevel: Int,
                       definitionLevel: Int,
                       fieldDescriptor: FieldDescriptor = null, path: String = ""): Tree = {
    val children = descriptor.getFields.asScala.toList
    val fullPath = if (fieldDescriptor != null) linkPath(path, fieldDescriptor.getName) else ""
    val childrenNodes = children.map { fieldDescriptor =>
      constructField(fieldDescriptor, repetitionLevel, definitionLevel, fullPath)
    }
    Node(
      FieldInfo(repetitionLevel, definitionLevel, fieldDescriptor, fullPath), childrenNodes)
  }

  def constructField(fieldDescriptor: FieldDescriptor, rLevel: Int, dLevel: Int, path: String): Tree = {
    val (r, d) = if (fieldDescriptor.isRepeated) {
      (rLevel + 1, dLevel + 1)
    } else {
      if (fieldDescriptor.isOptional) {
        (rLevel, dLevel + 1)
      } else {
        (rLevel, dLevel)
      }
    }
    fieldDescriptor.getJavaType match {
      case JavaType.MESSAGE => constructMessage(fieldDescriptor.getMessageType, r, d, fieldDescriptor, path)
      case _ => Leaf(FieldInfo(r, d, fieldDescriptor, linkPath(path, fieldDescriptor.getName)))
    }
  }

  def initTreeBySchema(descriptor: Descriptor): Tree = constructMessage(descriptor, 0, 0)

  def printTree(tree: Tree): Unit = {
    println(tree.debugString(0))
  }

}
