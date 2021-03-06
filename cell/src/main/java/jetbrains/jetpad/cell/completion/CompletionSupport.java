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
package jetbrains.jetpad.cell.completion;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import jetbrains.jetpad.base.*;
import jetbrains.jetpad.cell.*;
import jetbrains.jetpad.cell.event.CompletionEvent;
import jetbrains.jetpad.cell.event.FocusEvent;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.text.TextEditingTrait;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.cell.trait.CellTraitPropertySpec;
import jetbrains.jetpad.completion.*;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.event.ModifierKey;
import jetbrains.jetpad.model.event.CompositeRegistration;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.PropertyBinding;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.model.property.ReadableProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompletionSupport {
  public static final CellTraitPropertySpec<Runnable> HIDE_COMPLETION = new CellTraitPropertySpec<>("hideCompletion");
  public static final CellTraitPropertySpec<Supplier<String>> INITIAL_TEXT_PROVIDER = new CellTraitPropertySpec<Supplier<String>>("initialTextProvider");

  public static CellTrait trait() {
    return new CellTrait() {
      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == Completion.COMPLETION_CONTROLLER) {
          return getCompletionHandler(cell);
        }

        return super.get(cell, spec);
      }

      private CompletionController getCompletionHandler(final Cell cell) {
        return new CompletionController() {
          @Override
          public boolean isActive() {
            return cell.frontPopup().get() != null;
          }

          @Override
          public boolean canActivate() {
            return !Completion.isCompletionEmpty(cell, CompletionParameters.EMPTY);
          }

          @Override
          public void activate(final Runnable restoreState) {
            if (isActive()) {
              throw new IllegalStateException();
            }

            showPopup(cell, cell.frontPopup(), Completion.allCompletion(cell, new BaseCompletionParameters() {
              @Override
              public boolean isMenu() {
                return true;
              }
            }), restoreState);
          }

          @Override
          public void activate() {
            activate(Runnables.EMPTY);
          }

          @Override
          public void deactivate() {
            if (!isActive()) {
              throw new IllegalStateException();
            }
            cell.focus();
          }

          @Override
          public boolean hasAmbiguousMatches() {
            //todo implement it. we use this method only in hybrid synchronizers so we have mostly text view completion
            return true;
          }
        };
      }

      @Override
      public void onComplete(Cell cell, CompletionEvent event) {
        if (canComplete(cell)) {
          CompletionController handler = getCompletionHandler(cell);
          if (handler.canActivate()) {
            handler.activate();
          }
          event.consume();
        }
        super.onComplete(cell, event);
      }

      private boolean canComplete(Cell cell) {
        Cell current = cell.cellContainer().get().focusedCell.get();
        while (current != cell) {
          Cell parent = current.getParent();
          if (parent == null) {
            throw new IllegalStateException();
          }
          int index = parent.children().indexOf(current);
          if (index != 0) return false;
          current = parent;
        }
        return true;
      }
    };
  }

  public static void showCompletion(final TextCell textCell, Async<List<CompletionItem>> items, final Registration removeOnClose, final Runnable restoreState) {
    if (!textCell.focused().get()) {
      throw new IllegalArgumentException();
    }

    final CompletionMenuModel menuModel = new CompletionMenuModel();
    menuModel.loading.set(true);

    final CompositeRegistration reg = new CompositeRegistration();
    final ReadableProperty<String> prefixText = textCell.prefixText();
    reg.add(PropertyBinding.bindOneWay(prefixText, menuModel.text));

    final Handler<CompletionItem> completer = new Handler<CompletionItem>() {
      @Override
      public void handle(CompletionItem item) {
        reg.remove();
        restoreState.run();
        item.complete(prefixText.get()).run();
      }
    };

    final Cell completionCell = CompletionMenu.createCell(menuModel, completer, reg);
    reg.add(textCell.addTrait(new CellTrait() {
      @Override
      public void onPropertyChanged(Cell cell, CellPropertySpec<?> propery, PropertyChangeEvent<?> event) {
        if (propery == Cell.FOCUSED) {
          PropertyChangeEvent<Boolean> e = (PropertyChangeEvent<Boolean>) event;
          if (!e.getNewValue()) {
            reg.remove();
          }
        }

        super.onPropertyChanged(cell, propery, event);
      }

      @Override
      public void onKeyPressed(Cell cell, KeyEvent event) {
        CompletionItem selectedItem = menuModel.selectedItem.get();
        int pageHeight = completionCell.getBounds().dimension.y / textCell.dimension().y;
        if (selectedItem == null) return;

        if (event.is(Key.ENTER)) {
          completer.handle(selectedItem);
          event.consume();
          return;
        }

        if (event.is(Key.UP)) {
          menuModel.up();
          event.consume();
          return;
        }

        if (event.is(Key.DOWN)) {
          menuModel.down();
          event.consume();
          return;
        }

        if (event.is(Key.PAGE_UP) || event.is(Key.PAGE_DOWN)) {
          for (int i = 0; i < pageHeight; i++) {
            if (event.is(Key.PAGE_DOWN)) {
              menuModel.down();
            } else {
              menuModel.up();
            }
          }
          event.consume();
        }

        if (event.is(Key.ESCAPE)) {
          reg.remove();
          restoreState.run();
          event.consume();
          return;
        }

        super.onKeyPressed(cell, event);
      }

      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == HIDE_COMPLETION) {
          return new Runnable() {
            @Override
            public void run() {
              reg.remove();
              restoreState.run();
            }
          };
        }

        return super.get(cell, spec);
      }
    }));
    reg.add(new Registration() {
      @Override
      public void remove() {
        completionCell.hideSlide(300).whenDone(new Runnable() {
          @Override
          public void run() {
            completionCell.removeFromParent();
            removeOnClose.remove();
          }
        });
      }
    });

    textCell.bottomPopup().set(completionCell);
    completionCell.showSlide(150);

    items.onSuccess(new Handler<List<CompletionItem>>() {
      @Override
      public void handle(List<CompletionItem> items) {
        menuModel.loading.set(false);
        menuModel.items.addAll(items);
      }
    });
    items.onFailure(new Handler<Throwable>() {
      @Override
      public void handle(Throwable item) {
        menuModel.loading.set(true);
      }
    });
  }

  private static TextCell showPopup(
      Cell cell,
      Property<Cell> targetPopup,
      Async<List<CompletionItem>> items,
      Runnable onDeactivate) {
    CellContainer container = cell.cellContainer().get();
    final HorizontalCell popup = new HorizontalCell();
    final TextCell textCell = new TextCell();

    textCell.focusable().set(true);
    final Registration textEditingReg = textCell.addTrait(new TextEditingTrait());

    Supplier<String> initialProvider = cell.get(INITIAL_TEXT_PROVIDER);
    if (initialProvider != null) {
      String initialText = initialProvider.get();
      textCell.text().set(initialText);
      textCell.caretPosition().set(initialText.length());
    }

    popup.children().add(textCell);
    targetPopup.set(popup);
    final Runnable state = container.saveState();
    textCell.focus();
    showCompletion(textCell, items, new Registration() {
      @Override
      public void remove() {
        popup.removeFromParent();
        textEditingReg.remove();
      }
    }, Runnables.seq(state, onDeactivate));
    return textCell;
  }

  public static TextCell showSideTransformPopup(
      final Cell cell,
      final Property<Cell> targetPopup,
      List<CompletionItem> items) {
    final CellContainer container = cell.cellContainer().get();
    final Value<Boolean> completed = new Value<>(false);
    final Value<Boolean> dismissed = new Value<>(false);
    final Runnable restoreState = container.saveState();

    final List<CompletionItem> wrappedItems = new ArrayList<>();
    for (CompletionItem i : items) {
      wrappedItems.add(new CompletionItemWrapper(i) {
        @Override
        public Runnable complete(String text) {
          completed.set(true);
          return super.complete(text);
        }
      });
    }

    final HorizontalCell popup = new HorizontalCell();
    final TextCell textCell = new TextCell() {
      @Override
      public Registration addTrait(CellTrait trait) {
        return super.addTrait(trait);
      }
    };
    final Value<Handler<Boolean>> dismiss = new Value<>();
    final CompletionHelper completion = new CompletionHelper(new CompletionSupplier() {
      @Override
      public List<CompletionItem> get(CompletionParameters cp) {
        return wrappedItems;
      }
    }, CompletionParameters.EMPTY);
    textCell.focusable().set(true);
    final Registration traitReg = textCell.addTrait(new TextEditingTrait() {
      @Override
      public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
        if (spec == Completion.COMPLETION) {
          return new CompletionSupplier() {
            @Override
            public List<CompletionItem> get(CompletionParameters cp) {
              return wrappedItems;
            }
          };
        }

        return super.get(cell, spec);
      }

      @Override
      public void onPropertyChanged(Cell cell, CellPropertySpec<?> prop, PropertyChangeEvent<?> e) {
        if (prop == TextCell.TEXT) {
          PropertyChangeEvent<String> event = (PropertyChangeEvent<String>) e;
          if (Strings.isNullOrEmpty(event.getNewValue())) {
            dismiss.get().handle(false);
          }
        }

        super.onPropertyChanged(cell, prop, e);
      }

      @Override
      public void onKeyPressed(Cell cell, KeyEvent event) {
        if (event.is(Key.ESCAPE)) {
          dismiss.get().handle(false);
          event.consume();
          return;
        }

        super.onKeyPressed(cell, event);
      }

      @Override
      protected boolean onAfterType(TextCell tv) {
        if (super.onAfterType(tv)) return true;

        if (!textCell.isEnd()) return false;

        String text = textCell.text().get();
        if (completion.hasSingleMatch(text, cell.get(TextEditing.EAGER_COMPLETION))) {
          completion.matches(text).get(0).complete(text).run();
          return true;
        }

        String prefix = text.substring(0, text.length() - 1);
        String suffix = text.substring(text.length() - 1);
        if (completion.matches(prefix).size() == 1 && completion.prefixedBy(text).isEmpty()) {
          completion.matches(prefix).get(0).complete(prefix).run();
          for (int i = 0; i < suffix.length(); i++) {
            container.keyTyped(new KeyEvent(Key.UNKNOWN, suffix.charAt(i), Collections.<ModifierKey>emptySet()));
          }
        }
        return true;
      }

      @Override
      public void onFocusLost(Cell cell, FocusEvent event) {
        super.onFocusLost(cell, event);
        dismiss.get().handle(true);
      }
    });

    popup.children().add(textCell);

    if (targetPopup.get() != null) {
      throw new IllegalStateException();
    }

    targetPopup.set(popup);
    textCell.focus();

    dismiss.set(new Handler<Boolean>() {
      @Override
      public void handle(Boolean focusLoss) {
        if (dismissed.get()) return;
        dismissed.set(true);
        popup.removeFromParent();
        traitReg.remove();
        if (!completed.get() && !focusLoss) {
          restoreState.run();
        }
      }
    });

    return textCell;
  }
}