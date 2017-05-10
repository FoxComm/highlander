/* @flow */

// libs
import classNames from 'classnames';
import { get, kebabCase, noop } from 'lodash';
import React from 'react';

// components
import { FormFieldError } from 'components/forms';
import RichTextEditor from 'components/rich-text-editor/rich-text-editor';

import type { FieldErrors, ChangeHandler } from './index';

export default function renderRichText(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function (name: string, value: string = '', options: AttrOptions) {
    const handler = v => onChange(name, 'richText', v);

    const error = get(errors, name);
    const classForContainer = classNames('fc-object-form__field', {
      '_has-error': error != null,
    });
    const nameVal = kebabCase(name);

    return (
      <div className={classForContainer}>
        <RichTextEditor
          className={`fc-rich-text__name-${nameVal}`}
          label={options.label}
          value={value}
          onChange={handler}
        />
        {error && <FormFieldError error={error} />}
      </div>
    );
  };
}
