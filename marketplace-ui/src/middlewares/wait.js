/* eslint-disable */
import isEqual from 'lodash/isEqual';
import isPromise from 'is-promise';

export default (...middlewares) => next => (reducer, initialState) => {
  const store = next(reducer, initialState);
  let dispatch = store.dispatch;

  let pending = 0;
  let onSuccess, onError;

  function handleWaitingOnMiddleware(middleware) {
    return action => {
      let result = middleware(action);
      if (isPromise(result)) {
        pending++;

        result
          .then(() => {
            pending -= 1;
            if (!pending && onSuccess) {
              onSuccess();
            }
          })
          .catch(err => {
            if (onError) {
              onError(err);
            } else {
              throw err;
            }
          });
      }

      return result;
    }
  }

  const middlewareAPI = {
    getState: store.getState,
    dispatch: action => dispatch(action)
  };

  const chain = middlewares
    .map(middleware => middleware(middlewareAPI))
    .map(middleware => next => handleWaitingOnMiddleware(middleware(next)));

  dispatch = compose(...chain, store.dispatch);

  function renderToString(React, element) {
    return new Promise(function(resolve, reject) {
      let html = '';
      let dirty = false;
      let resolved = false;
      let inProgress = false;

      onError = err => {
        resolved = true;
        reject(err);
      };
      onSuccess = () => {
        resolved = true;
        resolve(html)
      };

      let attempts = 10;

      let currentState = store.getState();

      function render() {
        if (inProgress) {
          return;
        }

        let previousState = { ...currentState };
        currentState = { ...store.getState() };

        if (dirty && isEqual(previousState, currentState)) {
          return;
        }
        if (resolved) {
          return;
        }
        dirty = true;
        inProgress = true;
        while (dirty && !resolved && attempts) {
          dirty = false;
          attempts--;
          html = React.renderToString(element);
        }
        inProgress = false;
      }

      store.subscribe(render);
      render();
      if (pending === 0) onSuccess();
    });
  }

  return {
    ...store,
    dispatch,
    renderToString
  };
};

const compose = (...funcs) => funcs.reduceRight((composed, f) => f(composed));
