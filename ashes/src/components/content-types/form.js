/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ObjectFormInner from 'components/object-form/object-form-inner';
import SaveCancel from 'components/core/save-cancel';

type Props = {
  title: string,
  isVisible: boolean,
  schema: object,
  object: object,
  fieldsToRender: Array<string>,
  onCancel: Function,
  onUpdateObject: Function,
};

type State = {
  object: object,
};

export default class MyForm extends Component {
  props: Props;

  state: State = {
    object: {},
  };

  @autobind
  handleChange(object: object) {
    this.setState({ object });
  }

  @autobind
  handleSave() {
    this.props.onSave(this.state.object);
    this.props.onCancel();
  }

  get footer() {
    const saveDisabled = false;

    return <SaveCancel onCancel={this.props.onCancel} onSave={this.handleSave} saveDisabled={saveDisabled} />;
  }

  render() {
    const props = this.props;

    const attributes = {
      ...props.object,
      ...this.state.object,
    };

    return (
      <div>
        <ObjectFormInner
          schema={props.schema}
          attributes={attributes}
          fieldsToRender={props.fieldsToRender}
          onChange={this.handleChange}
        />
        {this.footer}
      </div>
    );
  }
}
