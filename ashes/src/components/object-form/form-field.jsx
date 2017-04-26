/* @flow */

import React from 'react';
import { FormField } from 'components/forms';

// TODO: fix content type
export default function renderFormField(name: string, content: any, options: AttrOptions) {
  return (
    <FormField
      {...options}
      className="fc-object-form__field"
      labelClassName="fc-object-form__field-label"
      key={`object-form-attribute-${name}`}
    >
      {content}
    </FormField>
  );
}
