/*
 * Copyright 2012-2014 JetBrains s.r.o
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
package jetbrains.jetpad.projectional.svg.toDom;

import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;
import jetbrains.jetpad.projectional.svg.SvgElement;
import jetbrains.jetpad.projectional.svg.SvgEllipse;
import jetbrains.jetpad.projectional.svg.SvgRect;
import jetbrains.jetpad.projectional.svg.SvgRoot;
import org.vectomatic.dom.svg.OMSVGElement;
import org.vectomatic.dom.svg.OMSVGEllipseElement;
import org.vectomatic.dom.svg.OMSVGRectElement;

public class SvgElementMapperFactory implements MapperFactory<SvgElement, OMSVGElement> {
  @Override
  public Mapper<? extends SvgElement, ? extends OMSVGElement> createMapper(SvgElement source) {
    Mapper<? extends SvgElement, ? extends OMSVGElement> result;
    if (source instanceof SvgEllipse) {
      result = new SvgEllipseMapper( (SvgEllipse) source, new OMSVGEllipseElement());
    } else if (source instanceof SvgRect) {
      result = new SvgRectMapper( (SvgRect) source, new OMSVGRectElement());
    } else if (source instanceof SvgRoot) {
      throw new IllegalStateException("Svg root element can't be embedded");
    } else {
      throw new IllegalStateException("Unsupported SvgElement");
    }
    return result;
  }
}
