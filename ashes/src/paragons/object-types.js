// @flow

export function string(v: string): Attribute {
  return { t: 'string', v };
}

export function richText(v: string): Attribute {
  return { t: 'richText', v };
}

export function price(v: Object): Attribute {
  return { t: 'price', v };
}

export function bool(v: boolean): Attribute {
  return { t: 'bool', v };
}

export function unitInput(v: boolean): Attribute {
  return { t: 'bool', v };
}
