/* @flow */

// libs
import { noop, get } from 'lodash/noop';
import React from 'react';

// components
import { FormFieldError } from 'components/forms';
import { Dropdown } from 'components/dropdown';

export default function renderOptions(state: Object, onChange: Function = noop) {
  return function(name: string, value: any, options: AttrOptions) {
    const fieldOptions = this.props.fieldsOptions && this.props.fieldsOptions[name];
    if (!fieldOptions) throw new Error('You must define fieldOptions for options fields');

    const handler = v => onChange(name, 'options', v);
    const error = get(this.state, ['errors', name]);

    return (
      <div className="fc-object-form_field">
        <div className="fc-object-form__field-label">{options.label}</div>
        <Dropdown
          value={value}
          items={fieldOptions}
          onChange={handler}
        />
        {error && <FormFieldError error={error} />}
      </div>
    );
  };
}
