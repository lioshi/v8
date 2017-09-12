// Copyright (c) 2012 Ecma International.  All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
esid: sec-array.prototype.filter
es5id: 15.4.4.20-9-c-i-30
description: >
    Array.prototype.filter - unnhandled exceptions happened in getter
    terminate iteration on an Array-like object
---*/

        var accessed = false;
        function callbackfn(val, idx, obj) {
            if (idx > 1) {
                accessed = true;
            }
            return true;
        }

        var obj = { 0: 11, 5: 10, 10: 8, length: 20 };
        Object.defineProperty(obj, "1", {
            get: function () {
                throw new RangeError("unhandle exception happened in getter");
            },
            configurable: true
        });
assert.throws(RangeError, function() {
            Array.prototype.filter.call(obj, callbackfn);
});
assert.sameValue(accessed, false, 'accessed');
