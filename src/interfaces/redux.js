import { dispatch } from 'redux';

type ActionDispatch = (d: dispatch) => any;

type ActionResult = {
  type: string;
  payload: Object;
};
