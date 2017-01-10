type HttpError = {
  status: number,
  statusText?: string,
  messages: Array<string>,
};

type AbortablePromise = Promise & {
  abort?: () => void,
}
