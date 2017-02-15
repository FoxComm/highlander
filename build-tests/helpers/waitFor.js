import sleep from './sleep';

export default async (pollDelayMs, timeoutAfterMs, promiseFn) => {
  let timePassed = 0;
  for (; ;) {
    try {
      return await promiseFn();
    } catch (error) {
      if (timePassed >= timeoutAfterMs) {
        throw error;
      } else {
        await sleep(pollDelayMs);
        timePassed += pollDelayMs;
      }
    }
  }
};
