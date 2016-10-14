export type FormField = {
  name?: string;
  names?: Array<string>;
  type: string;
  placeholder: string;
  value?: string;
  values?: Array<string>;
  validation?: Array<string>;
  normalize?: (value: string) => any;
  showPredicate?: (values: Array<string>) => boolean;
}

export type FieldValue = string|number;

export type FormData = {
  [x: string]: FieldValue;
}

export type ValidationRule = string|Array<string>;

export type Error = string;

export type ErrorsList = Array<Error>;
