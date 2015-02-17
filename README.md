# marbles: Heterogeneous Maps and Memoization for Scala

## What's inside?

### A thread safe memo (```PersistentMemo```).

Based on a twitter implementation, but the memo is decoupled from the function it memoizes.
See the docs for behavioural specification.

### A type safe heterogeneous map (```HMap```).

The map allows several type mappings to co-exist in one map.
This gives capabilities usually associated with dynamic languages.
A key difference is that the type mappings are known at compile time, so they are type safe, and
the compiler infers value types from key types.

This implementation is similar to the one found in shapeless, but differs in the following ways:

- The marbles ```HMap``` uses the type evidence as part of the key,
while the shapeless one does not store it. Storing the evidence combats type erasure and allows:
    - "type polymorphic" resolution of keys at runtime.
    - slicing the ```HMap``` by type.
    - running over all type mappings invoking specific code for each one (using ```HMapSection```)
    - one key to be mapped into many values of different types - useful for some caching situations.
- The marbles ```HMap``` defines a meaningful ```equals``` and ```hashCode```.
- The marbles ```HMap``` has more operations defined for it, making it easier to work with.
- The marbles ```HMap``` has a cool apply method allowing for very easy construction.

We have found ```HMap``` to be useful in the following ways:

- As an extensible record

    ```HMap```s can be much more flexible than using case classes for holding data,
    because extending them does not change their interface.
    They are also more flexible than traits,
    because trait composition cannot occur dynamically at runtime.
    Singleton objects can be used as keys.

- As a map between generic types

    For example, if we want to map from ```A[T]``` to ```B[T]``` in a type safe way
    (can't do that with a regular map).

- As a type "switch"

    Generics allow the same code to work across many types.
    Shapeless ```Poly``` can be used to have different code for each type,
    if it's known at compile time.
    ```HMap``` can be used to store different functions for different types, have dispatch
    dynamically according to the runtime type.

Currently, there is still an annoying issue which makes working with type hierarchies in ```HMap```
and ```HMemo``` slightly harder than it should be.

The first section in ```HMapTest``` gives code examples for these use cases.

### A heterogeneous pair (```HPair```)

This class is used to implement the awesome```HMap.apply```, but also be used to implement
extensible "named" arguments for methods, which can be manipulated at runtime (Python \**kwargs!!!).
Currently this is somewhat hindered by the subtyping issue which makes working with co-products
quite annoying.

### A type safe heterogeneous tread safe memo (```PersistentMemo```).

Brings the H into memo! Conversions provided to and from ```HMap```


## Current status

The library is still experimental and APIs could change.

## Feedback and contribution

Ideas, issues and contributions all welcome. To contribute, make a pull request.

## Scala version

Currently, the code is built against 2.10.4 although there should be no problem compiling against
2.11.x.

We may add cross building once the code is more stable.

## Publishing for SBT

Currently, the code is not yet published to a public repository.
We will probably publish it in the future.
