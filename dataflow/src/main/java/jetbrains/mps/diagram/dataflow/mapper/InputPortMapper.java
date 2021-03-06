/*
 * Copyright 2012-2015 JetBrains s.r.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.mps.diagram.dataflow.mapper;

import jetbrains.jetpad.geometry.Vector;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.view.ViewPropertySpec;
import jetbrains.jetpad.projectional.view.RectView;
import jetbrains.jetpad.values.Color;
import jetbrains.mps.diagram.dataflow.model.InputPort;

public class InputPortMapper extends Mapper<InputPort, RectView> {
  static final ViewPropertySpec<InputPort> PORT = new ViewPropertySpec<>("inputPort");

  InputPortMapper(InputPort source) {
    super(source, new RectView());
    getTarget().dimension().set(new Vector(10, 10));
    getTarget().background().set(Color.LIGHT_GRAY);
    getTarget().getProp(PORT).set(getSource());
  }
}