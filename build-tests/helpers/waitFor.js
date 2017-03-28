import sleep from './sleep';

function waitFor(pollDelayMs, timeoutAfterMs, promiseFn, conditionFn = () => true) {
  const error = new Error('Awaitable condition timeout');
  Error.captureStackTrace(error, waitFor);
  return (async () => {
    let timePassed = 0;
    for (; ;) {
      try {
        const result = await promiseFn();
        if (conditionFn(result)) {
          return result;
        }
        throw error;
      } catch (err) {
        if (timePassed >= timeoutAfterMs) {
          throw err;
        } else {
          await sleep(pollDelayMs);
          timePassed += pollDelayMs;
        }
      }
    }
  })();
}

export default waitFor;
