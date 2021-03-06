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
import jetbrains.jetpad.projectional.svg.SvgSvgElement;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.batik.dom.svg.SVGOMSVGElement;
import org.w3c.dom.DOMImplementation;

public class SvgRootDocumentMapper extends Mapper<SvgSvgElement, SVGOMDocument> {
  private static SVGOMDocument createDocument() {
    DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
    String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
    return (SVGOMDocument) impl.createDocument(svgNS, "svg", null);
  }

  private SvgElementMapper<SvgSvgElement, SVGOMSVGElement> myRootMapper;
  private SvgAwtPeer myPeer;

  public SvgRootDocumentMapper(SvgSvgElement source) {
    super(source, createDocument());
  }

  @Override
  protected void onAttach(MappingContext ctx) {
    super.onAttach(ctx);

    if (!getSource().isAttached()) {
      throw new IllegalStateException("Element must be attached");
    }
    myPeer = new SvgAwtPeer();
    getSource().container().setPeer(myPeer);

    myRootMapper = new SvgElementMapper<>(getSource(), (SVGOMSVGElement) getTarget().getDocumentElement(), getTarget(), myPeer);
    getTarget().getDocumentElement().setAttribute("shape-rendering", "geometricPrecision");
    myRootMapper.attachRoot();
  }

  @Override
  protected void onDetach() {
    myRootMapper.detachRoot();
    myRootMapper = null;

    if (getSource().isAttached()) {
      getSource().container().setPeer(null);
    }
    myPeer = null;

    super.onDetach();
  }
}