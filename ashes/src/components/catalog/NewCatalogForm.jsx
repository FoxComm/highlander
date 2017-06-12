/* @flow */

import React from 'react';

import Form from 'components/forms/form';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/forms/text-input';
import VerticalFormField from 'components/forms/vertical-form-field';

type Props = {
  name: string,
  defaultLanguage: string,
  site: string,
  onChange: Function,
};

const NewCatalogForm = (props: Props) => {
  const { defaultLanguage, name, site, onChange } = props;
  
  return (
    <div>
      <h1>New Catalog</h1>
      <Form onSubmit={(e) => console.log(e)}>
        <VerticalFormField
          controlId= "name"
          label="Name"
          required
        >
          <TextInput
            onChange={(v) => onChange('name', v)}
            value={name}
          />
        </VerticalFormField>
        <VerticalFormField
          controlId="defaultLanguage"
          label="Default Language"
          required
        >
          <TextInput
            onChange={(v) => onChange('defaultLanguage', v)}
            value={defaultLanguage}
          />
        </VerticalFormField>
        <VerticalFormField
          controlId="site"
          label="Site URL"
        >
          <TextInput
            onChange={(v) => onChange('site', v)}
            value={site}
          />
        </VerticalFormField>
        <SaveCancel saveText="Save Catalog" />
      </Form>
    </div>
  );
};

export default NewCatalogForm;
