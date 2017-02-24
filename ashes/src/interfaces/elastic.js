export type SearchFilter = {
  term: string,
  operator?: string,
  hidden?: boolean,
  value: {
    type: string,
    value: any,
  },
}
