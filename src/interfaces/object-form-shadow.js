type FormAttributes = { [key:string]: FormAttribute };
type FormAttribute = {
  type: string,
  [key:string]: any,
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
