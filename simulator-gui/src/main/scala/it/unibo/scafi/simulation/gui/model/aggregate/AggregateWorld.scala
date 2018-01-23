package it.unibo.scafi.simulation.gui.model.aggregate

import it.unibo.scafi.simulation.gui.model.aggregate.AggregateEvent.{NodesDeviceChanged, NodesMoved}
import it.unibo.scafi.simulation.gui.model.common.world.ObservableWorld
/**
  * aggregate world define a mutable world with mutable node and device
  */
trait AggregateWorld extends ObservableWorld with AggregateConcept{
  this : AggregateWorld.Dependency =>
  /**
    * move a node in other position
    * @param n the node
    * @param p the new position
    * @throws IllegalArgumentException if the node isn't in the world
    * @return true if the movement is allowed false otherwise
    */
  def moveNode(n : ID, p : P) : Boolean = {
    val node = this.getNodeOrThrows(n)
    produceResult(node,p ,filterPosition(node,p),a => WorldEvent(Set(a.id),NodesMoved))
  }

  /**
    * move a set of node in a new position
    * @param nodes the map of node and new position
    * @throws IllegalArgumentException if some node aren't in the world
    * @return the node that can't be mode
    */
  def moveNodes(nodes : Map[ID,P]): Set[NODE] = {
    produceResult(nodes,filterPosition,a => WorldEvent(a map {_.id},NodesMoved))
  }
  /**
    * switch on a device
    * @param n the node
    * @param name the name of device
    * @throws IllegalArgumentException if the node isn't in world
    * @return true if the device turn on false otherwise
    */
  def switchOnDevice(n : ID, name : NAME): Boolean = {
    val node = getNodeOrThrows(n)
    val nodeChanged = switchDevice(node,name,true)
    produceResult(node,name ,nodeChanged,a => WorldEvent(Set(a.id),NodesDeviceChanged))
  }

  /**
    * switch on a set of device
    * @param nodes the node and the the device wants to switch on
    * @throws IllegalArgumentException if the some node aren't in world
    * @return true if all device are switch on false otherwise
    */
  def switchOnDevices(nodes : Map[ID,NAME]) : Set[NODE] = {
    produceResult(nodes,(a,b:NAME) => switchDevice(a,b,true),a => WorldEvent(a map {_.id},NodesDeviceChanged))
  }
  /**
    * switch of a device
    * @param n the node
    * @param name the name of device
    * @throws IllegalArgumentException if the node isn't in world
    * @return true if the device is switched off false otherwise
    */
  def switchOffDevice(n : ID, name : NAME): Boolean = {
    val node = getNodeOrThrows(n)
    val nodeChanged = switchDevice(node,name,false)
    produceResult(node,name,nodeChanged,a => WorldEvent(Set(a.id),NodesDeviceChanged))
  }
  /**
    * switch off a set of device
    * @param nodes the node and the the device wants to switch off
    * @throws IllegalArgumentException if some node aren't in the world
    * @return the set of node that can't turn on a device
    */
  def switchOffDevices(nodes : Map[ID,NAME]) : Set[NODE] = {
    produceResult(nodes,(a,b: NAME) => switchDevice(a,b,false),a => WorldEvent(a map {_.id},NodesDeviceChanged))
  }
  /**
    * add a device to a node in the world
    * @param n the node
    * @param d the device name
    * @throws IllegalArgumentException if the node isn't in world
    * @return true if the node is in the world false otherwise
    */
  def addDevice(n: ID,d : DEVICE): Boolean = {
    val node = getNodeOrThrows(n)
    val nodeChanged  = toggleDevice(node,d,true)
    produceResult(node,d,nodeChanged,a => WorldEvent(Set(a.id),NodesDeviceChanged))
  }

  /**
    * insert device in a set of node
    * @param nodes the nodes and the device to add
    * @throws IllegalArgumentException if some node aren't in the world
    * @return the set of node that can't add a device
    */
  def addDevices(nodes : Map[ID,DEVICE]) : Set[NODE] = {
    produceResult(nodes,(a,b : DEVICE) => toggleDevice(a,b,true),a => WorldEvent(a map {_.id},NodesDeviceChanged))
  }
  /**
    * remove a device in a node in the world
    * @param n the node
    * @param d the device name
    * @throws IllegalArgumentException if the node isn't in world
    * @return true if the node is in the world false otherwise
    */
  def removeDevice(n: ID,d : DEVICE): Boolean = {
    val node = getNodeOrThrows(n)
    val nodeChanged = toggleDevice(node,d,false)
    produceResult(node,d, nodeChanged , a => WorldEvent(Set(a.id),NodesDeviceChanged))
  }

  /**
    * remove a device in a set of node
    * @param nodes the nodes with the device associated
    * @throws IllegalArgumentException if some node aren't in the world
    * @return the set of node that can't remove the device
    */
  def removeDevices(nodes : Map[ID,DEVICE]) : Set[NODE] = {
    produceResult(nodes,(a,b : DEVICE) => toggleDevice(a,b,false),a => WorldEvent(a map {_.id},NodesDeviceChanged))
  }

  // Some utility method
  private def switching(switched : Set[NODE]) : Unit = {
    this.removeBeforeEvent(switched.map(_.id))
    this.addBeforeEvent(switched)
  }
  private def produceResult[A](map : Map[ID,A],
                               filter :(NODE,A) => Option[NODE],
                               producer : (Set[NODE]) => WorldEvent): Set[NODE] = {
    require(map.keySet.forall(this.apply(_).isDefined))
    val switched = map.map(x => this.apply(x._1).get -> x._2)
      .map {x => filter(x._1,x._2)}
      .filter(_.isDefined)
      .map(_.get)
      .toSet
    switching(switched)
    this.!!!(producer(switched))
    this.apply(map.keySet) -- switched
  }

  private def produceResult[A](n : NODE, v : A,
                               filter : => Option[NODE],
                               producer : NODE => WorldEvent): Boolean = {
    val node = filter
    if(node.isEmpty) return false
    switching(Set(node.get))
    this.!!!(producer(node.get))
    true
  }

  private def getNodeOrThrows(id : ID) : NODE = {
    require(this.apply(id).isDefined)
    return this.apply(id).get
  }
  private val filterPosition : (NODE,P) => Option[NODE] = (a,b) => {
    val moved = nodeFactory.copy(a)(position = b)
    val res : Option[NODE] = if(!this.nodeAllowed(moved)) None else Some(moved)
    res
  }

  private val switchDevice: (NODE,NAME,Boolean) => Option[NODE] = (node,name,state) => {
    val selected = node.getDevice(name)
    var res : Option[NODE] = None
    if(selected.isDefined && selected.get.state != state) {
      val newDevices = node.devices - selected.get
      val onSelected = deviceFactory.copy(selected.get)(state)
      res = Some((nodeFactory.copy(node)(devices = newDevices + onSelected)))
    }
    res
  }

  private val toggleDevice: (NODE,DEVICE,Boolean) => Option[NODE] = (node,dev,toAdd) => {
    if(node.getDevice(dev.name).isDefined == toAdd) None
    var res : NODE = node
    if(toAdd) {
      res = nodeFactory.copy(node)(devices = node.devices + dev)
    } else {
      res = nodeFactory.copy(node)(devices = node.devices - dev)
    }
    Some(res)
  }
}
object AggregateWorld {
  type Dependency = ObservableWorld.Dependency
}