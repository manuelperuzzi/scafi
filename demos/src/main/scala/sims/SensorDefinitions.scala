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

import it.unibo.scafi.simulation.gui.model.implementation.SensorEnum
import it.unibo.scafi.incarnations.BasicSimulationIncarnation._

trait SensorDefinitions extends StandardSensors { self: AggregateProgram =>
  def sense1 = sense[Boolean](SensorEnum.SENS1.name)
  def sense2 = sense[Boolean](SensorEnum.SENS2.name)
  def sense3 = sense[Boolean](SensorEnum.SENS3.name)
  def sense4 = sense[Boolean](SensorEnum.SENS4.name)
  override def nbrRange() = super.nbrRange() * 100
}
