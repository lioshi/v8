// Copyright (C) 2015 André Bargull. All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
esid: sec-arraybuffer.prototype.slice
es6id: 24.1.4.3
description: >
  The `end` index defaults to [[ArrayBufferByteLength]] if undefined.
info: >
  ArrayBuffer.prototype.slice ( start, end )

  ...
  9. If end is undefined, let relativeEnd be len; else let relativeEnd be ToInteger(end).
  10. ReturnIfAbrupt(relativeEnd).
  ...
---*/

var arrayBuffer = new ArrayBuffer(8);

var start = 6, end = undefined;
var result = arrayBuffer.slice(start, end);
assert.sameValue(result.byteLength, 2);