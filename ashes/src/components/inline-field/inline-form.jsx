/**
 * @flow
 */

import React, { Component } from 'react';

import { FormField } from '../forms';
import FoxyForm from '../forms/foxy-form';
import SaveCancel from 'components/core/save-cancel';

type FormContents = { [key:string]: any };

type Props = {
  label: string,
  type: string,
  value: any,

  onSave: (contents: FormContents) => void,
  onCancel: () => void,
};

export default class InlineForm extends Component<void, Props, void> {
  props: Props;

  render() {
    return (
      <FoxyForm className="fc-inline-field__form" onSubmit={this.props.onSave}>
        <FormField>
          <input
            className="fc-inline-field__form-input"
            type="text"
            name={this.props.label}
            defaultValue={this.props.value} />
        </FormField>
        <SaveCancel onCancel={this.props.onCancel} />
      </FoxyForm>
    );
  }
}
