type FormAttributes = { [key:string]: FormAttribute };
type FormAttribute = {
  type: string;
  [key:string]: any;
};

type ShadowAttributes = { [key:string]: ShadowAttribute };
type ShadowAttribute = {
  type: string,
  ref: string,
};

type IlluminatedAttributes = { [key:string]: IlluminatedAttribute };
type IlluminatedAttribute = {
  label: string,
  type: string,
  value: string,
};

type FormShadowObject = {
  form: {attributes: FormAttributes};
  shadow: {attributes: ShadowAttributes};
};

type FormShadowAttrs = {
  form: FormAttributes,
  shadow: ShadowAttributes,
};

type FormShadowAttrsPair = [FormAttributes, ShadowAttributes];
