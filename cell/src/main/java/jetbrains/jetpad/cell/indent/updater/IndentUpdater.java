/*
 * Copyright 2012-2013 JetBrains s.r.o
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
package jetbrains.jetpad.cell.indent.updater;

import jetbrains.jetpad.model.children.Composite;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.cell.Cell;

import java.util.*;

public class IndentUpdater<SourceCT extends Composite<SourceCT>, TargetT> {
  private SourceCT myRoot;
  private Map<SourceCT, TargetT> myNewLineToLine = new HashMap<SourceCT, TargetT>();
  private Map<Cell, CellWrapper<TargetT>> myWrappers = new HashMap<Cell, CellWrapper<TargetT>>();
  private TargetT myTarget;
  private IndentUpdaterSource<SourceCT> myIndentUpdaterSource;
  private IndentUpdaterTarget<TargetT> myIndentUpdaterTarget;
  private SourceCT myJustBecameInvisible;

  public IndentUpdater(
      SourceCT root,
      TargetT target,
      IndentUpdaterSource<SourceCT> indentUpdaterSource,
      IndentUpdaterTarget<TargetT> indentUpdaterTarget) {
    myIndentUpdaterSource = indentUpdaterSource;
    myIndentUpdaterTarget = indentUpdaterTarget;
    myRoot = root;
    myTarget = target;

    children(myTarget).add(myIndentUpdaterTarget.newLine());
  }

  public void childAdded(SourceCT child) {
    childAdded(child, true);
  }

  private void childAdded(SourceCT child, boolean setAttached) {
    if (setAttached) {
      myIndentUpdaterSource.setAttached(child, true);
    }
    onChildAdd(child);
    if (!myIndentUpdaterSource.isCell(child)) {
      List<SourceCT> children = child.children();
      for (SourceCT c : children) {
        childAdded(c, setAttached);
      }
    }
  }


  public void childRemoved(SourceCT child) {
    childRemoved(child, true);
  }

  private void childRemoved(SourceCT child, boolean setAttached) {
    List<SourceCT> children = child.children();
    if (!myIndentUpdaterSource.isCell(child)) {
      for (int i = children.size() - 1; i >= 0; i--) {
        SourceCT c = children.get(i);
        childRemoved(c, setAttached);
      }
    }
    onChildRemove(child);
    if (setAttached) {
      myIndentUpdaterSource.setAttached(child, false);
    }
  }

  public void visibilityChanged(SourceCT item, PropertyChangeEvent<Boolean> change) {
    if (change.getNewValue()) {
      childAdded(item, false);
    } else {
      myJustBecameInvisible = item;
      try {
        childRemoved(item, false);
      } finally {
        myJustBecameInvisible = null;
      }
    }
  }

  private void onChildAdd(SourceCT child) {
    if (!isVisible(child)) return;

    Position<SourceCT> insertAt = new Position<SourceCT>(this, child);
    Position<SourceCT> prevNewLinePos = prevNewLine(insertAt.prev());
    Position<SourceCT> nextNewLinePos = nextNewLine(insertAt.next());

    if (myIndentUpdaterSource.isCell(child)) {
      Cell cell = myIndentUpdaterSource.getCell(child);
      CellWrapper<TargetT> wrapper = myIndentUpdaterTarget.wrap(cell);
      myWrappers.put(cell, wrapper);

      if (prevNewLinePos == null) {
        Position<SourceCT> first = new Position<SourceCT>(this, firstLeaf(myRoot));
        children(children(myTarget).get(0)).add(first.deltaTo(insertAt), wrapper.item());
      } else {
        TargetT targetLine = myNewLineToLine.get(prevNewLinePos.get());
        int indentDelta = indent(prevNewLinePos.get()) > 0 ? 1 : 0;
        children(targetLine).add(prevNewLinePos.deltaTo(insertAt) - 1 + indentDelta, wrapper.item());
      }
    } else if (myIndentUpdaterSource.isNewLine(child)) {
      TargetT newLine = myIndentUpdaterTarget.newLine();
      int indent = indent(child);

      if (indent > 0) {
        TargetT indentItem = myIndentUpdaterTarget.newIndent(indent);
        children(newLine).add(indentItem);
      }

      myNewLineToLine.put(child, newLine);

      if (prevNewLinePos == null) {
        children(myTarget).add(1, newLine);
      } else {
        TargetT prevLine = myNewLineToLine.get(prevNewLinePos.get());
        int prevLineIndex = children(myTarget).indexOf(prevLine);
        children(myTarget).add(prevLineIndex + 1, newLine);
      }

      Iterator<Position<SourceCT>> positions = nextNewLinePos == null ? toEnd(insertAt.next()) : range(insertAt.next(), nextNewLinePos);
      while (positions.hasNext()) {
        SourceCT part = positions.next().get();
        Cell cell = myIndentUpdaterSource.getCell(part);
        CellWrapper<TargetT> wrapper = myWrappers.get(cell);
        TargetT item = wrapper.item();
        removeFromParent(item);
        children(newLine).add(item);
      }
    }
  }

  private void onChildRemove(SourceCT child) {
    if (!isVisible(child)) return;

    Position<SourceCT> removeAt = new Position<SourceCT>(this, child);
    Position<SourceCT> prevNewLinePos = prevNewLine(removeAt.prev());

    if (myIndentUpdaterSource.isCell(child)) {
      if (prevNewLinePos == null) {
        Position<SourceCT> first = new Position<SourceCT>(this, firstLeaf(myRoot));
        children(children(myTarget).get(0)).remove(first.deltaTo(removeAt));
      } else {
        TargetT line = myNewLineToLine.get(prevNewLinePos.get());
        children(line).remove(prevNewLinePos.deltaTo(removeAt) - 1 + (indent(child) > 0 ? 1 : 0));
      }
      myWrappers.remove(myIndentUpdaterSource.getCell(child)).remove();
    } else if (myIndentUpdaterSource.isNewLine(child)) {
      TargetT lineCell = myNewLineToLine.remove(child);

      if (lineCell == null) throw new IllegalStateException();

      if (indent(child) > 0) {
        children(lineCell).remove(0);
      }

      removeFromParent(lineCell);

      TargetT mergeWith;
      if (prevNewLinePos == null) {
        mergeWith = children(myTarget).get(0);
      } else {
        mergeWith = myNewLineToLine.get(prevNewLinePos.get());
      }

      for (TargetT c : new ArrayList<TargetT>(children(lineCell))) {
        removeFromParent(c);
        children(mergeWith).add(c);
      }
    }
  }

  private void removeFromParent(TargetT c) {
    children(myIndentUpdaterTarget.parent(c)).remove(c);
  }

  private Position<SourceCT> prevNewLine(Position<SourceCT> from) {
    Position<SourceCT> current = from;
    while (current != null) {
      if (myIndentUpdaterSource.isNewLine(current.get())) {
        return  current;
      }
      current = current.prev();
    }
    return null;
  }

  private Position<SourceCT> nextNewLine(Position<SourceCT> from) {
    Position<SourceCT> current = from;
    while (current != null) {
      if (myIndentUpdaterSource.isNewLine(current.get())) {
        return current;
      }
      current = current.next();
    }
    return null;
  }

  private Iterator<Position<SourceCT>> toEnd(final Position<SourceCT> pos) {
    return new Iterator<Position<SourceCT>>() {
      private Position<SourceCT> myCurrent = pos;

      @Override
      public boolean hasNext() {
        return myCurrent != null;
      }

      @Override
      public Position<SourceCT> next() {
        Position<SourceCT> result = myCurrent;
        myCurrent = myCurrent.next();
        return result;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  private Iterator<Position<SourceCT>> range(final Position<SourceCT> from, final Position<SourceCT> to) {
    return new Iterator<Position<SourceCT>>() {
      private Position<SourceCT> myCurrent = from;

      @Override
      public boolean hasNext() {
        return !myCurrent.equals(to);
      }

      @Override
      public Position<SourceCT> next() {
        Position<SourceCT> result = myCurrent;
        myCurrent = myCurrent.next();
        return result;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  private int indent(SourceCT part) {
    int result = 0;
    SourceCT current = part;
    while (current != null) {
      if (myIndentUpdaterSource.isIndented(current)) {
        result++;
      }
      current = current.parent().get();
    }
    return result;
  }

  private SourceCT firstLeaf(SourceCT item) {
    if (myIndentUpdaterSource.isCell(item) || item.children().isEmpty()) return item;
    for (SourceCT child : item.children()) {
      if (isVisible(child)) return firstLeaf(child);
    }
    throw new IllegalStateException();
  }

  SourceCT root() {
    return myRoot;
  }

  boolean isAttached(SourceCT source) {
    return myIndentUpdaterSource.isAttached(source);
  }

  boolean isCell(SourceCT source) {
    return myIndentUpdaterSource.isCell(source);
  }

  boolean isVisible(SourceCT source) {
    SourceCT current = source;
    while (current != myRoot) {
      if (current == null) throw new IllegalStateException();
      if (!myIndentUpdaterSource.isVisible(current) && current != myJustBecameInvisible) return false;
      current = current.parent().get();
    }
    return true;
  }

  private List<TargetT> children(TargetT target) {
    return myIndentUpdaterTarget.children(target);
  }

}