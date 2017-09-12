// Copyright 2009 the Sputnik authors.  All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
info: >
    The Date.prototype.toString property "length" has { ReadOnly, DontDelete,
    DontEnum } attributes
esid: sec-date.prototype.tostring
es5id: 15.9.5.2_A3_T3
description: Checking DontEnum attribute
---*/

if (Date.prototype.toString.propertyIsEnumerable('length')) {
  $ERROR('#1: The Date.prototype.toString.length property has the attribute DontEnum');
}

for(var x in Date.prototype.toString) {
  if(x === "length") {
    $ERROR('#2: The Date.prototype.toString.length has the attribute DontEnum');
  }
}
