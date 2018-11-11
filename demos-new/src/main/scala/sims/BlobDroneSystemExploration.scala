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

package sims

import it.unibo.scafi.incarnations.BasicSimulationIncarnation.{AggregateProgram, BlockG}
import it.unibo.scafi.simulation.gui.configuration.environment.ProgramEnvironment.NearRealTimePolicy
import it.unibo.scafi.simulation.gui.incarnation.scafi.bridge.ScafiSimulationInitializer.RadiusSimulation
import it.unibo.scafi.simulation.gui.incarnation.scafi.bridge.reflection.{Demo, SimulationType}
import it.unibo.scafi.simulation.gui.incarnation.scafi.bridge.{MetaActionProducer, SimulationInfo}
import it.unibo.scafi.simulation.gui.incarnation.scafi.configuration.{ScafiInformation, ScafiProgramBuilder, ScafiWorldInformation}
import it.unibo.scafi.simulation.gui.incarnation.scafi.world.ScafiWorldInitializer.Random
import it.unibo.scafi.simulation.gui.view.scalaFX.drawer.StandardFXOutput
import it.unibo.scafi.space.SpatialAbstraction
import it.unibo.scafi.space.graphics2D.BasicShape2D.Rectangle
import lib.{FlockingLib, Movement2DSupport}

object BlobDroneSystemExploration extends App {
  /*
  ViewSetting.backgroundImage = Some("file:///background-image")
  SenseImageFXOutput.addRepresentation(SensorName.sensor1, "drone-path")
  SenseImageFXOutput.addRepresentation(SensorName.sensor3, "base-path")
  */
  ScafiProgramBuilder (
    Random(100,1120,780),
    SimulationInfo(program = classOf[BlobDroneSystemExplorationDemo],
      metaActions = List(MetaActionProducer.movementDtActionProducer),
      exportValutations = List.empty),
    RadiusSimulation(100),
    scafiWorldInfo = ScafiWorldInformation(
      boundary = Some(SpatialAbstraction.Bound(Rectangle(1120,780))),
      shape = Some(Rectangle(20,20))),
    neighbourRender = true,
    performance = NearRealTimePolicy,
    outputPolicy = StandardFXOutput /*SenseImageFXOutput*/
  ).launch()
}

/**
  * Scenario: the drones explore a region while keeping constant connection with the base.
  *  If the network gets disconnected, the drones are able to go back to the
  *  last known position of the base.
  *   - Sense1: drones
  *   - Sense2: obstacles
  *   - Sense3: base
  */

@Demo(simulationType = SimulationType.MOVEMENT)
class BlobDroneSystemExplorationDemo extends AggregateProgram with SensorDefinitions with FlockingLib with Movement2DSupport with BlockG {
  private val attractionForce: Double = 10.0
  private val alignmentForce: Double = 40.0
  private val repulsionForce: Double = 80.0
  private lazy val repulsionRange: Double = ScafiInformation.configuration.simulationInitializer match {
    case RadiusSimulation(radius) => radius * 60.0 / 100
    case _ => 60.0 / 200
  }
  lazy val width = ScafiInformation.configuration.worldInitializer.size._1
  lazy val height = ScafiInformation.configuration.worldInitializer.size._2
  private val obstacleForce: Double = 400.0

  private lazy val separationThr = ScafiInformation.configuration.simulationInitializer match {
    case RadiusSimulation(radius) => radius * 80
    case _ => 80
  }
  private val neighboursThr = 4


  override def main(): (Double, Double) = SizeConversion.normalSizeToWorldSize(rep(randomMovement(), (0.5,0.5))(behaviour)._1)

  private def flockWithBase(myTuple: ((Double, Double),(Double,Double))): ((Double, Double),(Double,Double)) = {
    val myPosition = currentPosition()
    val gradient = distanceTo(sense3)
    val minGradHood = minHood(nbr(gradient))
    val nbrCount: Int = foldhoodPlus(0)(_ + _){1}
    val basePosition: (Double, Double) = broadcast(sense3, (myPosition.x, myPosition.y))
    mux(((gradient - minGradHood) > separationThr | gradient > 100) & nbrCount < neighboursThr) {
      val baseVector = goToPointWithSeparation(myTuple._2, repulsionRange)
      (baseVector, myTuple._2)
    } {
      val flockVector = flock(myTuple._1, Seq(sense1), Seq(sense2), repulsionRange, attractionForce, alignmentForce, repulsionForce, obstacleForce)

      mux(basePosition._1 > width | basePosition._2 > height | (basePosition._1 == 0.0 & basePosition._2 == 0.0)) {
        ((flockVector._1, flockVector._2), myTuple._2)
      } {
        ((flockVector._1, flockVector._2), basePosition)
      }
    }
  }

  private def behaviour(tuple:((Double, Double),(Double,Double))): ((Double, Double),(Double,Double)) = {
    val myPosition = currentPosition()
    mux(sense1){
      flockWithBase(tuple)
    }
    {
      mux(sense3){
        val bp: (Double, Double) = broadcast(sense3, (myPosition.x, myPosition.y))
        ((0.0,0.0), bp)
      }
      {
        val fv = flock(tuple._1, Seq(sense1), Seq(sense2), repulsionRange, 0.0, 0.0, repulsionForce, 0.0)
        (fv, (250,250))
      }
    }
  }

}