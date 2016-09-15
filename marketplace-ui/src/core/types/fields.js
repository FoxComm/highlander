export type FormField = {
  name: string;
  type: string;
  placeholder: string;
  values?: Array<string>;
  validation?: Array<string>;
}

export type FieldValue = string|number;

export type FormData = {
  [x: string]: FieldValue;
}

export type ValidationRule = string|Array<string>;

export type Error = string;

export type ErrorsList = Array<Error>;
