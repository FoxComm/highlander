
import * as auth from './auth';

function extendClass(cls, methods) {
  for (const name in methods) {
    if (!methods.hasOwnProperty(name)) continue;

    cls.prototype[name] = methods[name];
  }
}

export default function setup(cls) {
  extendClass(cls, auth);
  return cls;
}
