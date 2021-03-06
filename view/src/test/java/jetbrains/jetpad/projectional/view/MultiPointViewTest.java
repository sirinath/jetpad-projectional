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
package jetbrains.jetpad.projectional.view;

import jetbrains.jetpad.geometry.Vector;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class MultiPointViewTest {
  @Test
  public void addRemoveWithRelativeCoordinates() {
    GroupView container = new GroupView();
    PolygonView polygonView = new PolygonView();
    container.children().add(polygonView);

    polygonView.move(new Vector(10, 10));

    polygonView.points.add(new Vector(10, 10));

    assertEquals(1, polygonView.points.size());
    assertEquals(new Vector(10, 10), polygonView.points.get(0));
  }
}