declare type AttrOptions = {
  required: boolean,
  label: string,
  isDefined: (value: any) => boolean,
  disabled?: boolean,
  error?: string,
};
