// This file was procedurally generated from the following sources:
// - src/arguments/args-trailing-comma-multiple.case
// - src/arguments/default/gen-func-expr.template
/*---
description: A trailing comma should not increase the arguments.length, using multiple args (generator function expression)
esid: sec-arguments-exotic-objects
flags: [generated]
info: |
    9.4.4 Arguments Exotic Objects

    Most ECMAScript functions make an arguments object available to their code. Depending upon the
    characteristics of the function definition, its arguments object is either an ordinary object
    or an arguments exotic object.

    Trailing comma in the arguments list

    12.3 Left-Hand-Side Expressions

    Arguments[Yield, Await] : ( ArgumentList[?Yield, ?Await] , )
---*/


var callCount = 0;
// Stores a reference `ref` for case evaluation
var ref;
ref = function*() {
  assert.sameValue(arguments.length, 2);
  assert.sameValue(arguments[0], 42);
  assert.sameValue(arguments[1], 'TC39');
  callCount = callCount + 1;
};

ref(42, 'TC39',).next();

assert.sameValue(callCount, 1, 'generator function invoked exactly once');
