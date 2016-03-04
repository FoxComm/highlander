
import fetch from 'isomorphic-fetch';

function hookPromise(promise, hook, hookCb) {
  const _then = promise.then;
  const _catch = promise.catch;

  promise.then = (success, fail) => {
    return hookPromise(
      _then.call(promise, hookCb(success, true), hookCb(fail)),
      hook,
      hookCb
    );
  };

  promise.catch = (fail) => {
    return hookPromise(
      _catch.call(promise, hookCb(fail)),
      hook,
      hookCb
    );
  };

  return hook(promise);
}

// ugly implementation for cancellation request
// see https://github.com/whatwg/fetch/issues/27
// @TODO: research for
// 1) Move to some existing library that uses XMLHTTPRequest underhood and has fetch interface
// 2) Implement fetch-like interface via XMLHTTPRequest and add request aborting feature
export default function(...args) {
  const promise = fetch(...args);

  let canceled = false;

  const addCancellation = promise => {
    promise.cancel = () => {
      canceled = true;
    };

    return promise;
  };

  const hookCb = func => {
    return (...args) => {
      if (!canceled && func) {
        return func(...args);
      }
    };

  };

  return hookPromise(promise, addCancellation, hookCb);
}
