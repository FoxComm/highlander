import { dispatch } from 'redux';

type ActionDispatch = (d: dispatch, getState: () => any) => any;

type ActionResult = {
  type: string;
  payload: Object;
};
