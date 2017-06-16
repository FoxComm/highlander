/* @flow */

// libs
import _ from 'lodash';

// helpers
import createStore from './store-creator';

export type AsyncState = {
  isRunning: boolean;
  isCompleted: boolean;
  isCanceled: boolean;
  isSucceeded: boolean;
  data: any;
  error: any;
  state: any;
};

export const initialState: AsyncState = {
  //flag, that is set when operation is running
  isRunning: false,
  //flag, that is set when operation is completed (canceled/succeeded/failed - doesn't matter)
  //this one can be used to work with results of previous operation call
  //when !isCompleted && !isRunning - operation was never called before
  isCompleted: false,
  //flag, notifying about cancellation of operation.
  //if isRunning - about current operation (cancellation during execution). Else - about previous operation
  isCanceled: false,
  //this flag is an indicator of whether operation was successful or not
  //if isRunning - about previous operation. Else - about current operation
  isSucceeded: false,
  //data and error are operation artifacts and are not to be used instead of this flag
  data: null,
  error: null,
  //operation state value for operations, that are reporting their state changes
  state: null,
};

//state reducers for async action
const reducers = {
  reset: () => initialState,
  canceled: state => ({...state, isCanceled: true}),
  started: state => ({
    ...state,
    isRunning: true,
    isCompleted: false,
  }),
  updated: (state, update) => ({...state, state: update}),
  succeeded: (state, data) => ({
    ...state,
    isRunning: false,
    isCompleted: true,
    isSucceeded: true,
    data,
    error: null,
  }),
  failed: (state, error) => ({
    ...state,
    isRunning: false,
    isCompleted: true,
    isSucceeded: false,
    data: null,
    error,
  }),
};

//action handler creator
const getHandler = (path, action) => (actions, ...args) => (dispatch, getState) => {
  dispatch(actions.started());
  return action(actions, _.get(getState(), path), ...args)(dispatch, getState)
    .then(
      data => dispatch(actions.succeeded(data)),
      error => dispatch(actions.failed(error))
    );
};

//async wrapper
export default function createAsyncAction(path:string, action:Function):Function {
  const { actions, reducer } = createStore({path, reducers});

  const asyncAction = getHandler(path, action);
  asyncAction.actions = actions;
  asyncAction.reducer = reducer;

  return asyncAction;
}
