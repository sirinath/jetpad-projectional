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
package jetbrains.jetpad.projectional.demo.indentDemo.mapper;

import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.demo.indentDemo.model.Container;

import static jetbrains.jetpad.cell.util.CellFactory.*;

public class ContainerMapper extends Mapper<Container, ContainerMapper.ContainerCell> {
  public ContainerMapper(Container source) {
    super(source, new ContainerCell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(LambdaSynchronizers.exprSynchronizer(this, getSource().expr, getTarget().expr));
  }

  static class ContainerCell extends IndentCell {
    final IndentCell expr = new IndentCell();

    ContainerCell() {
      to(this,
        label("Container"),
        indent(true, newLine(),
          expr
        ));
    }
  }
}