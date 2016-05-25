
import * as auth from './auth'
// import * as cart from './cart'
// import * as checkout from './checkout'
// import * as product from './product'

function extendClass(cls, methods) {
  for (const name in methods) {
    if (!methods.hasOwnProperty(name)) continue;

    cls.prototype[name] = methods[name]
  }
}

export default function setup(cls) {
  extendClass(cls, auth)
  // extendClass(cls, cart)
  // extendClass(cls, checkout)
  // extendClass(cls, product)
  return cls
}
