/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package app.kanela.instrumentation.mixin

import app.kanela.cases.simple.SpyAware
import kanela.agent.api.instrumentation.mixin.Initializer

import scala.collection.mutable.ListBuffer

class SpyMixin extends SpyAware {
  private var _tracks: ListBuffer[String] = _
  def tracks: ListBuffer[String] = _tracks
  def addTracks(v: String): Unit = { this._tracks += v }

  @Initializer
  def _initializer(): Unit = {
    this._tracks = ListBuffer.empty
    this.addTracks("init")
  }
}
