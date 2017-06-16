/* @flow */

import React from 'react';
import { transitionToLazy } from 'browserHistory';

import Form from 'components/forms/form';
import ErrorAlerts from 'components/alerts/error-alerts';
import SaveCancel from 'components/core/save-cancel';
import DynamicGroupEditor from './editor/group-editor';

type Props = {
  group: TCustomerGroup,
  title: string,
  cancelTo: string,
  cancelParams?: Object,
  saveInProgress: boolean,
  saveError: boolean,
  onSave: () => Promise<*>,
  params: {
    type: string,
  },
};

export default ({ group, title, onSave, cancelTo, cancelParams, saveInProgress, saveError, params }: Props) => (
  <div>
    <header>
      <h1 className="fc-title">{title}</h1>
    </header>
    <ErrorAlerts error={saveError} />
    <article>
      <Form onSubmit={onSave}>
        <DynamicGroupEditor type={params.type} />

        <SaveCancel
          className="fc-customer-group-edit__form-submits"
          onCancel={transitionToLazy(cancelTo, cancelParams)}
          saveText="Save Group"
          saveDisabled={!group.isValid}
          isLoading={saveInProgress}
        />
      </Form>
    </article>
  </div>
);
