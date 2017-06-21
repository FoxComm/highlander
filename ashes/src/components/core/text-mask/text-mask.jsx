// @flow

import React from 'react';
import { isString, isEmpty, map } from 'lodash';
import ReactTextMask from 'react-text-mask';

type Props = {
  /** [react-text-mask#mask](https://github.com/text-mask/text-mask/blob/master/componentDocumentation.md#mask) */
  mask: Array<RegExp>|string|false;
  /** [react-text-mask#guide](https://github.com/text-mask/text-mask/blob/master/componentDocumentation.md#guide) */
  guide?: boolean;
  /** [react-text-mask#placeholderchar](https://github.com/text-mask/text-mask/blob/master/componentDocumentation.md#placeholderchar) */
  placeholderChar?: string;
  /** [react-text-mask#keepcharpositions](https://github.com/text-mask/text-mask/blob/master/componentDocumentation.md#keepcharpositions) */
  keepCharPositions?: boolean;
  /** [react-text-mask#showmask](https://github.com/text-mask/text-mask/blob/master/componentDocumentation.md#showmask) */
  showMask?: boolean;
  /** Injected prop from FormField */
  error?: boolean;
};

// Create masks related to react-text-mask component
// https://github.com/text-mask/text-mask/blob/master/componentDocumentation.md#mask
export function strMaskToRegExp(stringPattern: string) {
  const fieldMask = map(stringPattern, c => {
    if (c.match(/\d/)) {
      return /\d/;
    } else if (c.match(/[a-z]/i)) {
      return /[a-z]/i;
    }

    return c;
  });

  return fieldMask;
}

export const TextMask = (props: Props) => {
  const { error, ...innerProps } = props; // omit error from FormField

  if (isEmpty(props.mask)) {
    innerProps.mask = false;
  } else if (isString(props.mask)) {
    innerProps.mask = strMaskToRegExp(String(props.mask));
  }

  if (!props.placeholderChar) {
    innerProps.placeholderChar = '\u2000';
  }

  return <ReactTextMask {...innerProps} />;
};
