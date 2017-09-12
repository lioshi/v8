// This file was procedurally generated from the following sources:
// - src/async-generators/yield-as-label-identifier-escaped.case
// - src/async-generators/syntax/async-class-expr-method.template
/*---
description: yield is a reserved keyword within generator function bodies and may not be used as a label identifier. (Async generator method as a ClassExpression element)
esid: prod-AsyncGeneratorMethod
features: [async-iteration]
flags: [generated]
negative:
  phase: early
  type: SyntaxError
info: |
    ClassElement :
      MethodDefinition

    MethodDefinition :
      AsyncGeneratorMethod

    Async Generator Function Definitions

    AsyncGeneratorMethod :
      async [no LineTerminator here] * PropertyName ( UniqueFormalParameters ) { AsyncGeneratorBody }


    LabelIdentifier : Identifier

    It is a Syntax Error if this production has a [Yield] parameter and
    StringValue of Identifier is "yield".

---*/
throw "Test262: This statement should not be evaluated.";


var C = class { async *gen() {
    yi\u0065ld: ;
}};
