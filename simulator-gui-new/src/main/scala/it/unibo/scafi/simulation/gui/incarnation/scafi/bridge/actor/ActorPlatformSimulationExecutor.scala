/*
 * Copyright (C) 2016-2017, Roberto Casadei, Mirko Viroli, and contributors.
 * See the LICENCE.txt file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package it.unibo.scafi.simulation.gui.incarnation.scafi.bridge.actor

import it.unibo.scafi.simulation.gui.controller.logger.LogManager
import it.unibo.scafi.simulation.gui.controller.logger.LogManager.{Channel, TreeLog}
import it.unibo.scafi.simulation.gui.incarnation.scafi.bridge.ScafiBridge

import scala.language.postfixOps
import it.unibo.scafi.simulation.gui.incarnation.scafi.bridge.ScafiWorldIncarnation._

object ActorPlatformSimulationExecutor extends ScafiBridge {
  import ScafiBridge._

  private var exportProduced : Map[ID,EXPORT] = Map.empty
  private val indexToName = (i : Int) => "output" + (i + 1)
  override protected val maxDelta: Option[Int] = None

  override protected def asyncLogicExecution(): Unit = {
    if(contract.simulation.isDefined) {
      val net = contract.simulation.get
      net.exports().filter(_._2.isDefined).map(n => n._1 -> n._2.get).foreach { node =>
        exportProduced += node._1 -> node._2
        if (idsObserved.contains(node._1)) {
          val mapped = node._2.paths.toSeq.map { x => {
            if (x._1.isRoot) {
              (None, x._1, x._2)
            } else {
              (Some(x._1.pull()), x._1, x._2)
            }
          }}.sortWith((x, y) => x._2.level < y._2.level)
          LogManager.notify(TreeLog[Path](Channel.Export, node._1.toString, mapped))
        }
        val metaActions = this.simulationInfo.get.metaActions
        metaActions
          .filter(x => x.valueParser(node._2.root()).isDefined)
          .foreach(x => net.add(x(node._1, node._2)))
        net.process()
      }
    }
  }
  override def onTick(float: Float): Unit = {()
    val simulationMoved = simulationObserver.idMoved
    if(contract.simulation.isDefined) {
      val bridge = contract.simulation.get
      val exportValutations = simulationInfo.get.exportValutations
      if(exportValutations.nonEmpty) {
        var exportToUpdate = Map.empty[ID,EXPORT]
        exportToUpdate = exportProduced
        exportProduced = Map.empty
        for(export <- exportToUpdate) {
          for(i <- exportValutations.indices) {
            world.changeSensorValue(export._1,indexToName(i),exportValutations(i)(export._2))
          }
        }
      }
      var idsNetworkUpdate = Set.empty[Int]
      simulationMoved foreach {id =>
        val p = contract.simulation.get.space.getLocation(id)
        world.moveNode(id,p)
        idsNetworkUpdate ++= world.network.neighbours(id)
        idsNetworkUpdate ++= contract.simulation.get.neighbourhood(id)
        idsNetworkUpdate += id
      }
      idsNetworkUpdate foreach {x => {world.network.setNeighbours(x,contract.simulation.get.neighbourhood(x))}}

      val simulationSensor = simulationObserver.idSensorChanged
      simulationSensor.foreach(nodeChanged =>
        nodeChanged._2.foreach(name =>
          world.changeSensorValue(nodeChanged._1, name, bridge.localSensor(name)(nodeChanged._1))
        )
      )
    }
  }
}

