package it.unibo.scafi.simulation.gui.controller

import it.unibo.scafi.simulation.gui.controller.synchronization.Scheduler.SchedulerObserver
import it.unibo.scafi.simulation.gui.model.common.world.ObservableWorld

/**
  * the root trait of all controller
  * a controller controls a world
  * @tparam W the world observed
  */
trait Controller[W <: ObservableWorld] extends SchedulerObserver