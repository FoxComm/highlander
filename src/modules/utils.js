
import { createAction } from 'redux-act';

export function createNsAction(namespace, description, ...args) {
  const cleanNamespace = namespace
    .replace('.list', '')
    .replace(/\./g, '_')
    .replace(/^[A-Z_]/, '');

  const name = `${cleanNamespace}_${description}`.toUpperCase();
  return createAction(name, ...args);
}
