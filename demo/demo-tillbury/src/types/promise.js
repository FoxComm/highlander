
export type AbortablePromise<T> = Promise<T> & {
  abort: () => void,
}
