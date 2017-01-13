/* @flow */

import React from 'react';

import { reset, fetchGroup, saveGroup } from 'modules/customer-groups/group';
import { fetchRegions } from 'modules/regions';

import { Link } from 'components/link';
import Form from 'components/forms/form';
import SaveCancel from 'components/common/save-cancel';
import DynamicGroupEditor from './dynamic/group-editor';

type Props = {
  group: TCustomerGroup;
  title: string;
  cancelTo: string;
  cancelParams: Object;
  onSave: () => Promise;
};

export default ({ group, title, onSave, cancelTo, cancelParams }: Props) => (
  <div>
    <header>
      <h1 className="fc-title">{title}</h1>
    </header>
    <article>
      <Form onSubmit={onSave}>
        <DynamicGroupEditor />

        <SaveCancel
          className="fc-customer-group-edit__form-submits"
          cancelTo={cancelTo}
          cancelParams={cancelParams}
          saveText="Save Group"
          saveDisabled={!group.isValid}
        />
      </Form>
    </article>
  </div>
);
