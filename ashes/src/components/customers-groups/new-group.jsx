/* @flow */

//libs
import { autobind } from 'core-decorators';
import React, { Component } from 'react';

//components
import Form from 'components/forms/form';
import SaveCancel from 'components/common/save-cancel';
import DynamicGroupEditor from './dynamic/group-editor';

type Props = {
  group: TCustomerGroup;
  reset: () => void;
  onSave: () => Promise;
  fetchRegions: () => Promise;
}

export default class NewGroup extends Component {
  props: Props;

  componentWillMount() {
    this.props.reset();
  }

  render() {
    return (
      <div>
        <header>
          <h1 className="fc-title">New Customer Group</h1>
        </header>
        <article>
          <Form onSubmit={this.props.onSave}>
            <DynamicGroupEditor />
            <SaveCancel
              className="fc-customer-group-edit__form-submits"
              cancelTo="customer-groups"
              saveText="Save Dynamic Group"
              saveDisabled={!this.props.group.isValid}
            />
          </Form>
        </article>
      </div>
    );
  }
}
