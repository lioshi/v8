// Copyright 2017 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef V8_OBJECTS_BIGINT_INL_H_
#define V8_OBJECTS_BIGINT_INL_H_

#include "src/objects/bigint.h"

#include "src/objects.h"

// Has to be the last include (doesn't have include guards):
#include "src/objects/object-macros.h"

namespace v8 {
namespace internal {

SMI_ACCESSORS(BigInt, value, kValueOffset)
TYPE_CHECKER(BigInt, BIGINT_TYPE)

}  // namespace internal
}  // namespace v8

#include "src/objects/object-macros-undef.h"

#endif  // V8_OBJECTS_BIGINT_INL_H_
