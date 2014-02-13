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
package jetbrains.jetpad.grammar.parser;

import jetbrains.jetpad.grammar.Grammar;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class LRTable {
  private Grammar myGrammar;
  private LRState myInitialState;
  private Set<LRState> myStates = new HashSet<>();

  public LRTable(Grammar grammar) {
    myGrammar = grammar;
    myInitialState = newState("S0");
  }

  public Grammar getGrammar() {
    return myGrammar;
  }

  public LRState getInitialState() {
    return myInitialState;
  }

  public Set<LRState> getStates() {
    return Collections.unmodifiableSet(myStates);
  }

  public LRState newState(String name) {
    LRState result = new LRState(name);
    myStates.add(result);
    return result;
  }
}