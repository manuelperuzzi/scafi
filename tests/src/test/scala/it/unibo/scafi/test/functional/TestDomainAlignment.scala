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

package it.unibo.scafi.test.functional

import it.unibo.scafi.config.GridSettings
import it.unibo.scafi.test.FunctionalTestIncarnation._
import org.scalatest._

import scala.collection.{Map => M}

class TestDomainAlignment extends FlatSpec with Matchers {
  import ScafiAssertions._
  import ScafiTestUtils._

  val Domains = new ItWord
  val stepx: Double = 1.0
  val stepy: Double = 1.0
  val SRC = "source"
  val FLAG = "flag"
  val (fewRounds, someRounds, manyRounds, manyManyRounds) = (100, 500, 1000, 2000)

  private[this] trait SimulationContextFixture {
    val net: Network with SimulatorOps =
      SetupNetwork(simulatorFactory.gridLike(GridSettings(3, 3, stepx, stepy), rng = 1.5))
  }

  private[this] trait TestProgram extends AggregateProgram with StandardSensors with FieldUtils {
    def s = if(mid<=2 || mid==4) -1 else if(mid%3==0) 2 else 5
  }

  def SetupNetwork(n: Network with SimulatorOps) = {
    n.addSensor(FLAG, false)
    n.chgSensorValue(FLAG, ids = Set(0,1,2,3,4), value = true)
    n
  }

  Domains should "align properly" in new SimulationContextFixture {
    // ACT
    exec(new TestProgram {
      override def main(): Any = (
        excludingSelf.sumHood(branch(mid<4){nbr(1)}{nbr(0)}+branch(mid%3!=0)(nbr(0))(nbr(1)) +0),
        includingSelf.sumHood(branch(mid<4){nbr(1)}{nbr(0)}+branch(mid%3!=0)(nbr(0))(nbr(1)) +0),
        excludingSelf.unionHood {
          branch(s<4)(nbr("a"+mid))(nbr("b"+mid)) +
            branch(s>0)(nbr("c"+mid))(nbr("d"+mid))
        }
      )
    }, ntimes = fewRounds)(net)

    // ASSERT
    assertNetworkValues((0 to 8).zip(List(
      (3,5,Set("a1d1","a4d4")), (5,6,Set("a0d0","a2d2","a4d4")), (1,2,Set("a1d1","a4d4")),
      (4,6,Set("a6c6")),        (7,7,Set("a0d0","a1d1","a2d2")), (2,2,Set("b8c8","b7c7")),
      (2,3,Set("a3c3")),        (3,3,Set("b5c5","b8c8")),        (0,0,Set("b5c5","b7c7"))
    )).toMap)(net)
  }
}
