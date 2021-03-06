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

import jetbrains.jetpad.geometry.Rectangle;
import jetbrains.jetpad.geometry.Vector;
import jetbrains.jetpad.model.collections.CollectionItemEvent;
import jetbrains.jetpad.model.property.PropertyChangeEvent;

/**
 * This interface created for the sole purpose of memory optimization.
 * It's internal to view package. Do not make it public.
 */
interface ViewListener {
  void onPropertySet(ViewPropertySpec<?> prop, PropertyChangeEvent<?> event);
  void onCustomViewFeatureChange(CustomViewFeatureSpec spec);

  void onViewValidated();
  void onViewInvalidated();

  void onViewAttached();
  void onViewDetached();

  void onBoundsChanged(PropertyChangeEvent<Rectangle> change);

  /**
   * If you just add a listener via View.addListener, this method might not be called.
   * See View.myDeltaListenersCount
   */
  void onToRootDeltaChanged(PropertyChangeEvent<Vector> change);

  void onChildAdded(CollectionItemEvent<View> event);
  void onChildRemoved(CollectionItemEvent<View> event);

  void onParentChanged(PropertyChangeEvent<View> event);
}