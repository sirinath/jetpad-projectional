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

import jetbrains.jetpad.base.Registration;
import jetbrains.jetpad.event.MouseEvent;
import jetbrains.jetpad.mapper.Synchronizer;
import jetbrains.jetpad.mapper.SynchronizerContext;
import jetbrains.jetpad.mapper.Synchronizers;
import jetbrains.jetpad.model.property.WritableProperty;
import jetbrains.jetpad.projectional.svg.SvgAttributeSpec;
import jetbrains.jetpad.projectional.svg.SvgElement;
import jetbrains.jetpad.projectional.svg.SvgElementListener;
import jetbrains.jetpad.projectional.svg.event.SvgAttributeEvent;
import jetbrains.jetpad.projectional.svg.event.SvgEventSpec;
import org.apache.batik.dom.AbstractDocument;
import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.dom.svg.SVGOMElement;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

class SvgElementMapper<SourceT extends SvgElement, TargetT extends SVGOMElement> extends SvgNodeMapper<SourceT, TargetT> {
  private Map<SvgEventSpec, Registration> myHandlerRegs;

  public SvgElementMapper(SourceT source, TargetT target, AbstractDocument doc, SvgAwtPeer peer) {
    super(source, target, doc, peer);
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(new Synchronizer() {
      private Registration myReg;

      @Override
      public void attach(SynchronizerContext ctx) {
        myReg = getSource().addListener(new SvgElementListener<Object>() {
          @Override
          public void onAttrSet(SvgAttributeEvent<Object> event) {
            if (event.getNewValue() == null) {
              getTarget().removeAttribute(event.getAttrSpec().toString());
            }
            getTarget().setAttribute(event.getAttrSpec().toString(), event.getNewValue().toString());
          }
        });

        for (SvgAttributeSpec<?> key : getSource().getAttributeKeys()) {
          getTarget().setAttribute(key.toString(), getSource().getAttribute(key.getName()).get().toString());
        }
      }

      @Override
      public void detach() {
        myReg.remove();
      }
    });

    conf.add(Synchronizers.forPropsOneWay(getSource().handlersSet(), new WritableProperty<Set<SvgEventSpec>>() {
      @Override
      public void set(Set<SvgEventSpec> value) {
        if (myHandlerRegs == null) {
          myHandlerRegs = new EnumMap<>(SvgEventSpec.class);
        }

        for (SvgEventSpec spec : SvgEventSpec.values()) {
          if (!value.contains(spec) && myHandlerRegs.containsKey(spec)) {
            myHandlerRegs.remove(spec).remove();
          }
          if (!value.contains(spec) || myHandlerRegs.containsKey(spec)) continue;

          switch (spec) {
            case MOUSE_CLICKED:
              addMouseHandler(spec, SVGConstants.SVG_CLICK_EVENT_TYPE);
              break;
            case MOUSE_PRESSED:
              addMouseHandler(spec, SVGConstants.SVG_MOUSEDOWN_EVENT_TYPE);
              break;
            case MOUSE_RELEASED:
              addMouseHandler(spec, SVGConstants.SVG_MOUSEUP_EVENT_TYPE);
              break;
            case MOUSE_OVER:
              addMouseHandler(spec, SVGConstants.SVG_MOUSEOVER_EVENT_TYPE);
              break;
            case MOUSE_MOVE:
              addMouseHandler(spec, SVGConstants.SVG_MOUSEMOVE_EVENT_TYPE);
              break;
            case MOUSE_OUT:
              addMouseHandler(spec, SVGConstants.SVG_MOUSEOUT_EVENT_TYPE);
              break;
            default:
              break;
          }
        }

        if (myHandlerRegs.isEmpty()) {
          myHandlerRegs = null;
        }
      }
    }));
  }

  private void addMouseHandler(final SvgEventSpec spec, final String eventType) {
    final EventListener listener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        evt.stopPropagation();
        getSource().dispatch(spec, new MouseEvent(((DOMMouseEvent) evt).getClientX(), ((DOMMouseEvent) evt).getClientY()));
      }
    };
    getTarget().addEventListener(eventType, listener, false);
    myHandlerRegs.put(spec, new Registration() {
      @Override
      public void remove() {
        getTarget().removeEventListener(eventType, listener, false);
      }
    });
  }
}