package it.unibo.scafi.simulation.gui.incarnation.scafi


import it.unibo.scafi.incarnations.{BasicAbstractSpatialSimulationIncarnation => ExternSimulation}
import it.unibo.scafi.simulation.gui.controller.logical.ExternalSimulation
import it.unibo.scafi.simulation.gui.incarnation.scafi.ScafiWorldIncarnation._
import it.unibo.scafi.simulation.gui.launcher.scalaFX.Simple
import it.unibo.scafi.simulation.gui.model.aggregate.AggregateEvent.{NodesDeviceChanged, NodesMoved}
import it.unibo.scafi.simulation.gui.model.common.world.CommonWorldEvent.{NodesAdded, NodesRemoved}
import it.unibo.scafi.space.Point3D
//TODO RIGUARDARE COMPLETAMENTE q
class ScafiSimulationObserver[W <: ScafiLikeWorld](override protected val world : W,
                                                   val contract : ScafiSimulationContract[W,ScafiPrototype],
                                                   override protected val minDelta : Int,
                                                   override protected val maxDelta : Int,
                                                   override val simulationPrototype : ScafiPrototype,
                                                   val program : Any) extends ExternalSimulation[W]{

  //TODO BRIDGE
  val ap = (new Simple).asInstanceOf[CONTEXT=>EXPORT]
  private val checkMoved = world.createObserver(Set(NodesMoved))
  private val checkChanged = world.createObserver(Set(NodesDeviceChanged))
  private val checkAdded = world.createObserver(Set(NodesAdded))
  private var exportProduced : Set[(world.ID,EXPORT)] = Set()
  world <-- checkAdded <-- checkChanged <-- checkAdded <-- checkMoved


  override type EXTERNAL_SIMULATION = SpaceAwareSimulator
  override type SIMULATION_PROTOTYPE = ScafiPrototype
  override type SIMULATION_CONTRACT = ScafiSimulationContract[W,SIMULATION_PROTOTYPE]

  override protected var delta: Int = (minDelta + maxDelta) / 2
  override protected var currentExecutor: ActorExecutor = _

  override protected def AsyncLogicExecution(): Unit = {
    if(contract.getSimulation.isDefined) {
      val net = contract.getSimulation.get
      val result = net.exec(ap)
      exportProduced += result._1 -> result._2
    }
  }
  //TODO AGGIUNGI CLEAR IN OBSERVER WORLD
  override def onTick(float: Float): Unit = {
    val moved = checkMoved.nodeChanged()
    val devs = checkChanged.nodeChanged()
    val added = checkAdded.nodeChanged()
    if(contract.getSimulation.isDefined) {
      val extern = contract.getSimulation.get
      //TODO AGGIUNGI ANCHE IL COMPORTAMENTO DEL MOVIMENTO, PENSA SE USARE UNA STRATEGY ESTERNA
      devs map {world(_).get} foreach {x => x.devices.foreach(y => extern.chgSensorValue(y.name,Set(x.id),y.value))}
      moved foreach { x =>
        val node = world(x).get
        contract.getSimulation.get.setPosition((x), Point3D(node.position.x, node.position.y, node.position.z))
        val neigh = contract.getSimulation.get.neighbourhood(x)
        world.network.setNeighbours(x,neigh)
        neigh foreach {x => {world.network.setNeighbours(x,contract.getSimulation.get.neighbourhood(x))}}
      }
    }
    //TODO REMEMBER TO CREATE SOMETHING DIFFERENT! ONLY TEST NOW!
    exportProduced foreach { x => {
      val devs = world(x._1).get.devices
      val dev = devs.find {y => y.name == "generic"}.get
      if(dev.value != x._2.root()) {
        world.changeSensorValue(x._1,"generic",x._2.root())
      }
    }}
    exportProduced = Set.empty
  }
}


