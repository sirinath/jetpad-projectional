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
package jetbrains.jetpad.cell.toView;

import com.google.common.base.Strings;
import jetbrains.jetpad.base.Handler;
import jetbrains.jetpad.base.Registration;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.CellPropertySpec;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.indent.IndentContainerCellListener;
import jetbrains.jetpad.cell.indent.updater.CellWrapper;
import jetbrains.jetpad.cell.indent.updater.IndentUpdater;
import jetbrains.jetpad.cell.indent.updater.IndentUpdaterTarget;
import jetbrains.jetpad.cell.toUtil.CounterUtil;
import jetbrains.jetpad.mapper.MappingContext;
import jetbrains.jetpad.model.collections.CollectionItemEvent;
import jetbrains.jetpad.model.collections.set.ObservableSet;
import jetbrains.jetpad.model.composite.Composites;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.projectional.view.HorizontalView;
import jetbrains.jetpad.projectional.view.TextView;
import jetbrains.jetpad.projectional.view.VerticalView;
import jetbrains.jetpad.projectional.view.View;

import java.util.List;

class IndentRootCellMapper extends BaseCellMapper<IndentCell, VerticalView> {
  private IndentUpdater<View> myIndentUpdater;
  private ObservableSet<BaseCellMapper<?, ?>> myCellMappers;
  private Registration myRegistration;

  IndentRootCellMapper(IndentCell source, CellToViewContext ctx) {
    super(source, new VerticalView(), ctx);

    myCellMappers = createChildSet();

    myIndentUpdater = new IndentUpdater<View>(getSource(), getTarget(),
      new IndentUpdaterTarget<View>() {
        @Override
        public View newLine() {
          return new HorizontalView();
        }

        @Override
        public View newIndent(int size) {
          TextView result = new TextView();
          result.text().set(Strings.repeat("  ", size));
          return result;
        }

        @Override
        public CellWrapper<View> wrap(final Cell cell) {
          final BaseCellMapper<?, ?> mapper = createMapper(cell);

          CounterUtil.updateOnAdd(getSource(), cell, mapper);
          myCellMappers.add(mapper);

          return new CellWrapper<View>() {
            @Override
            public View item() {
              return mapper.getTarget();
            }

            @Override
            public void remove() {
              CounterUtil.updateOnRemove(getSource(), cell, mapper);
              myCellMappers.remove(mapper);
            }
          };
        }

        @Override
        public List<View> children(View item) {
          return item.children();
        }

        @Override
        public View parent(View item) {
          return item.getParent();
        }
      }
    ) {
      @Override
      protected void onVisibilityChanged(Cell cell, PropertyChangeEvent<Boolean> event) {
        myIndentUpdater.visibilityChanged(cell, event);
      }
    };
  }

  @Override
  boolean isAutoChildManagement() {
    return false;
  }

  @Override
  boolean isAutoPopupManagement() {
    return false;
  }

  @Override
  protected void onAttach(MappingContext ctx) {
    super.onAttach(ctx);

    for (Cell child : getSource().children()) {
      myIndentUpdater.childAdded(child);
    }
    myIndentUpdater.initialized();

    myRegistration = getSource().addListener(new IndentContainerCellListener() {
      @Override
      public void childAdded(CollectionItemEvent<Cell> event) {
        Cell cell = event.getItem();
        myIndentUpdater.childAdded(cell);
      }

      @Override
      public void childRemoved(CollectionItemEvent<Cell> event) {
        Cell cell = event.getItem();
        myIndentUpdater.childRemoved(cell);
      }

      @Override
      public void propertyChanged(Cell cell, final CellPropertySpec<?> prop, final PropertyChangeEvent<?> event) {
        if (CounterUtil.isCounterProp(prop)) {
          iterateLeaves(cell, new Handler<Cell>() {
            @Override
            public void handle(Cell item) {
              BaseCellMapper<?, ?> mapper = (BaseCellMapper<?, ?>) getDescendantMapper(item);
              if (mapper == null) {
                throw new IllegalStateException();
              }
              if (CounterUtil.update(mapper, prop, event)) {
                mapper.refreshProperties();
              }
            }
          });
        } else if (prop == Cell.VISIBLE) {
          myIndentUpdater.visibilityChanged(cell, (PropertyChangeEvent<Boolean>) event);
        } else if (Cell.isPopupProp(prop)) {
          updateIndentCellPopup(cell, (PropertyChangeEvent<Cell>) event);
        }
      }

      private void iterateLeaves(Cell cell, Handler<Cell> handler) {
        for (Cell child : cell.children()) {
          if (!Composites.isVisible(child)) continue;
          if (child instanceof IndentCell) {
            iterateLeaves(child, handler);
          } else {
            handler.handle(child);
          }
        }
      }
    });
  }

  @Override
  protected void onDetach() {
    List<Cell> children = getSource().children();
    for (int i = children.size() - 1; i >= 0; i--) {
      Cell c = children.get(i);
      myIndentUpdater.childRemoved(c);
    }

    myRegistration.remove();

    super.onDetach();
  }

  private void updateIndentCellPopup(Cell targetCell ,PropertyChangeEvent<Cell> event) {
    if (event.getOldValue() != null) {
      BaseCellMapper<?, ?> popupMapper = (BaseCellMapper<?, ?>) getDescendantMapper(event.getOldValue());
      Composites.<View>removeFromParent(popupMapper.getTarget());
      myCellMappers.remove(popupMapper);
    }

    if (event.getNewValue() != null) {
      BaseCellMapper<?, ?> popupMapper = createMapper(event.getNewValue());
      myCellMappers.add(popupMapper);
      cellToViewContext().popupView().children().add(popupMapper.getTarget());
      updatePopupPositions(targetCell);
    }
  }
}