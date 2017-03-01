import sleep from './sleep';

export default async (pollDelayMs, timeoutAfterMs, promiseFn, conditionFn = () => true) => {
  let timePassed = 0;
  for (; ;) {
    try {
      const result = await promiseFn();
      if (conditionFn(result)) {
        return result;
      }
      throw new Error('Awaitable condition timeout\n' +
        `result=${JSON.stringify(result, null, 2)},\n` +
        `condition=${conditionFn}`);
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
