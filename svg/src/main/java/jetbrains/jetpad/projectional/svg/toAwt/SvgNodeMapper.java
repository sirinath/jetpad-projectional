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
package jetbrains.jetpad.projectional.svg.toAwt;

import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MappingContext;
import jetbrains.jetpad.mapper.Synchronizers;
import jetbrains.jetpad.projectional.svg.SvgNode;
import org.apache.batik.dom.AbstractDocument;
import org.w3c.dom.Node;

class SvgNodeMapper<SourceT extends SvgNode, TargetT extends Node> extends Mapper<SourceT, TargetT> {
  private AbstractDocument myDoc;
  private SvgAwtPeer myPeer;

  SvgNodeMapper(SourceT source, TargetT target, AbstractDocument doc, SvgAwtPeer peer) {
    super(source, target);
    myDoc = doc;
    myPeer = peer;
  }

  @Override
  protected void registerSynchronizers(final Mapper.SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(Synchronizers.forObservableRole(this, getSource().children(), Utils.elementChildren(getTarget()),
        new SvgNodeMapperFactory(myDoc, myPeer)));
  }

  @Override
  protected void onAttach(MappingContext ctx) {
    super.onAttach(ctx);

    myPeer.registerMapper(getSource(), this);
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    myPeer.unregisterMapper(getSource());
  }
}