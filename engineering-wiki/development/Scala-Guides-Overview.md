# Scala Guides Overview

My short reviews about various Scala guides after a month of development.

## Tutorials

1. [Scala Exercises](http://scala-exercises.47deg.com/koans) - very basic (no `Futures`, only one section related to `Options` etc.) tutorial in form of koans, with some sections overbloated by repetetive exercises (e.g. `Traversables`), while more important stuff (e.g. pattern-matching `Options`) lacks good practice. Contains outdated stuff like `Manifests` (deprecated since 2.10).

2. Twitter's [Scala School](http://twitter.github.io/scala_school) and [Effective Scala](http://twitter.github.io/effectivescala) - good tutorials, by very short and superficial, obviously written by highly-skilled engineer, who doesn't want to waste time describing details, since they're obvious to him. Both can be used as cheat sheets at start.

3. [A Little Guide on Using Futures for Web Developers](http://codemonkeyism.com/a-little-guide-on-using-futures-for-web-developers) - probably the best tutorial about `Futures`.

4. [FP for the average Joe - II - ScalaZ Monad Transformers](http://www.47deg.com/blog/fp-for-the-average-joe-part-2-scalaz-monad-transformers) - to understand concepts behind the "starfish" pattern, used widely in Phoenix.

## Books

1. [Scala for the Impatient](http://www.horstmann.com/scala/index.html) - the only Scala book translated to Russian, but if you've completed koans or have read Twitter's guides, don't waste time on this. It describes entry-level concepts, goes deeper into unnecessary stuff like XML Processing (come on, it's 2015) and Actors (can be delayed until you reach Akka).

2. [Programming in Scala](http://www.artima.com/shop/programming_in_scala_2ed) - a book written by language creator, a bit deeper than previous, but has the same cons (who needs GUI programming in Scala at start?). The main problem of this two books is the way the present Scala to you - just another OO language on top of JVM with some syntax sugar and lack of some annoying Java limitations. This leads us too to the next book...

3. [Functional Programming in Scala](https://www.manning.com/books/functional-programming-in-scala) - a luminous gem in the kingdom of shades. This book teaches you to think in pure functional way, describes the most useful patterns of functional programming. I've even understood the monads finally.

  1. If you don't have enough time to read, there is a nice [Companion Booklet](http://blog.higher-order.com/blog/2015/03/06/a-companion-booklet-to-functional-programming-in-scala) with short summary of basic functional concepts. 

4. [Scala in Depth](http://www.manning.com/suereth) - a book, obviously written for the people who already have industrial development experience with Scala, but want to sharpen their skills. Imagine [Effective Scala](http://twitter.github.io/effectivescala), but more detailed, in form of a book. Has only one paragraph related to category theory (previous book describes monads and applicative functors slightly deeper).

## TBD

This ones will be read and reviewed when I'll reach higher level:

1. [Akka in Action](https://www.manning.com/books/akka-in-action)
2. [Spark in Action](https://www.manning.com/books/spark-in-action)
