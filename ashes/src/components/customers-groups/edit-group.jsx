/* @flow */

//libs
import React, { Component } from 'react';

//components
import Form from 'components/forms/form';
import SaveCancel from 'components/common/save-cancel';
import DynamicGroupEditor from './dynamic/group-editor';

type Props = {
  group: TCustomerGroup;
  onSave: () => Promise;
  fetchGroup: (id: number) => Promise;
  fetchRegions: () => Promise;
}

export default (props: Props) => (
  <div>
    <header>
      <h1 className="fc-title">Edit Customer Group</h1>
    </header>
    <article>
      <Form onSubmit={props.onSave}>
        <DynamicGroupEditor />

        <SaveCancel
          className="fc-customer-group-edit__form-submits"
          cancelTo="customer-group"
          cancelParams={{groupId: props.group.id}}
          saveText="Save Dynamic Group"
          saveDisabled={!props.group.isValid}
        />
      </Form>
    </article>
  </div>
);
