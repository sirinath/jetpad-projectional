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
package jetbrains.jetpad.completion;

public class CompletionItemWrapper implements CompletionItem {
  private CompletionItem myWrappedItem;

  public CompletionItemWrapper(CompletionItem wrappedItem) {
    myWrappedItem = wrappedItem;
  }

  @Override
  public String visibleText(String text) {
    return myWrappedItem.visibleText(text);
  }

  @Override
  public boolean isStrictMatchPrefix(String text) {
    return myWrappedItem.isStrictMatchPrefix(text);
  }

  @Override
  public boolean isMatchPrefix(String text) {
    return myWrappedItem.isMatchPrefix(text);
  }

  @Override
  public boolean isMatch(String text) {
    return myWrappedItem.isMatch(text);
  }

  @Override
  public Runnable complete(String text) {
    return myWrappedItem.complete(text);
  }

  @Override
  public boolean isLowPriority() {
    return myWrappedItem.isLowPriority();
  }
}