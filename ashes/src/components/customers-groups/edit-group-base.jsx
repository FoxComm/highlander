/* @flow */

import React from 'react';

import { reset, fetchGroup, saveGroup } from 'modules/customer-groups/details/group';
import { fetchRegions } from 'modules/regions';

import { Link } from 'components/link';
import Form from 'components/forms/form';
import ErrorAlerts from 'components/alerts/error-alerts';
import SaveCancel from 'components/common/save-cancel';
import DynamicGroupEditor from './editor/group-editor';

type Props = {
  group: TCustomerGroup;
  title: string;
  cancelTo: string;
  cancelParams: Object;
  saveInProgress: boolean;
  saveError: boolean;
  onSave: () => Promise;
};

export default ({ group, title, onSave, cancelTo, cancelParams, saveInProgress, saveError }: Props) => (
  <div>
    <header>
      <h1 className="fc-title">{title}</h1>
    </header>
    <ErrorAlerts error={saveError} />
    <article>
      <Form onSubmit={onSave}>
        <DynamicGroupEditor />

        <SaveCancel
          className="fc-customer-group-edit__form-submits"
          cancelTo={cancelTo}
          cancelParams={cancelParams}
          saveText="Save Group"
          saveDisabled={!group.isValid}
          isLoading={saveInProgress}
        />
      </Form>
    </article>
  </div>
);
