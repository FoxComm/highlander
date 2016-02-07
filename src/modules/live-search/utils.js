
import { createAction } from 'redux-act';

export function createNsAction(namespace, description, ...args) {
  const name = `${namespace.replace('.list', '')}_${description}`.toUpperCase();
  return createAction(name, ...args);
}
