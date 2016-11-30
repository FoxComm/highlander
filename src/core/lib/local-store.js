
import _makeLocalStore from 'wings/lib/redux/make-local-store';
import { thunkMiddleware } from '../../store';

export default function makeLocalStore(reducer, initialState = {}, middlewares = [thunkMiddleware]) {
  return _makeLocalStore(reducer, initialState, middlewares);
}
