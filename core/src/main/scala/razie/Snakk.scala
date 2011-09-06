/*
 * Razvan's public code. Copyright 2008 based on Apache license (share alike) see LICENSE.txt for
 * details. No warranty implied nor any liability assumed for this code.
 */
package razie

import org.json.JSONObject

import razie.xp.JsonWrapper
import razie.xp.XpJsonSolver

/** wraps an URL with some arguments to be passed in the call */
class SnakkUrl (val url:java.net.URL, val attr:AA) {
  /** transform this URL in one with basic authentication */
  def  basic (user:String, password:String) = 
    new SnakkUrl (url, attr ++ AA("Authorization", "Basic " + new sun.misc.BASE64Encoder().encode((user+":"+password).getBytes)))
}

/** rapid decomposition of data in different formats, from different sources */
object Snakk {
  /** build a URL */
  def url(s: String, attr:AA=AA.EMPTY) = new SnakkUrl(new java.net.URL(s), attr)

  /** retrieve the content from URL, as String */
  def body(url: SnakkUrl) = com.razie.pub.comms.Comms.readUrl(url.url.toString, url.attr)

  def apply(node: scala.xml.Elem) = xml(node)
  def xml(node: scala.xml.Elem) = new Wrapper(node, ScalaDomXpSolver)
  def xml(body: String) = new Wrapper(scala.xml.XML.load(body), ScalaDomXpSolver)
  def xml(url: SnakkUrl) = new Wrapper(scala.xml.XML.load(url.url), ScalaDomXpSolver) // TODO use AA for auth

  def str(node: String) = new Wrapper(node, StringXpSolver)
  def str(url: SnakkUrl) = new Wrapper(body(url), StringXpSolver)

  def bean(node: Any) = new Wrapper(node, BeanXpSolver)

  def apply(node: JSONObject) = json(node)
  def json(node: JSONObject) = new Wrapper[JsonWrapper](XpJsonSolver.WrapO(node), XpJsonSolver)
  def json(node: String) = new Wrapper[JsonWrapper](XpJsonSolver.WrapO(new JSONObject(node)), XpJsonSolver)
  def json(url: SnakkUrl) = new Wrapper[JsonWrapper](XpJsonSolver.WrapO(new JSONObject(body(url))), XpJsonSolver)

  /** this will go to the URL and try to figure out what the url is */
  def apply(node: String) = new Wrapper(node, StringXpSolver)

}
/** OO wrapper for self-solving XP elements HEY this is like an open monad :) */
class ListWrapper[T](val nodes: List[T], val ctx: XpSolver[T, Any]) {
  /** the list of children with the respective tag */
  def \(name: String): ListWrapper[T] = new ListWrapper(nodes.flatMap(n => XP[T]("*/" + name).xpl(ctx, n)), ctx)
  /** the list of children two levels down with the respective tag */
  def \*(name: String): ListWrapper[T] = new ListWrapper(nodes.flatMap(n => XP[T]("*/*/" + name).xpl(ctx, n)), ctx)
  /** the list of attributes with the respective name */
  def \@(name: String): List[String] = nodes map (n => XP[T](if (name.startsWith("@")) name else "@" + name).xpa(ctx, n))
  /** the single attributes with the respective name */
  def \@@(name: String): String = nodes.headOption.map(n => XP[T](if (name.startsWith("@")) name else "@" + name).xpa(ctx, n)) getOrElse null

  def apply (i:Int) = new Wrapper(nodes.apply(i), ctx)
  def \ (i:Int) = new Wrapper(nodes.apply(i), ctx)
  
  def foreach[B](f: T => B): Unit = nodes.foreach(f)
  def map[B](f: T => B): List[B] = nodes.map(f)
  def flatMap[B](f: T => List[B]): List[B] = nodes.flatMap(f)

  def first(n: Any = null) = headOption getOrElse n
  def headOption = nodes.headOption
  //  def firstOption = nodes.firstOption map (new Wrapper(_, ctx))

  override def toString = nodes.toString
}

/** OO wrapper for self-solving XP elements */
class Wrapper[T](val node: T, val ctx: XpSolver[T, Any]) {
  /** the list of children with the respective tag */
  def \(name: String): ListWrapper[T] = new ListWrapper(XP[T]("*/" + name).xpl(ctx, node), ctx)
  /** the attribute with the respective name */
  def \@(name: String): String = XP[T](if (name.startsWith("@")) name else "@" + name).xpa(ctx, node)
  /** the single attributes with the respective name */
  def \@@(name: String): String = this \@ name

  override def toString = Option(node).map(_.toString).toString
}