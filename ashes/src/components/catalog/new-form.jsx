/* @flow */

import _ from 'lodash';
import React from 'react';

import Content from 'components/core/content/content';
import { Dropdown } from 'components/dropdown';
import Form from 'components/forms/form';
import SaveCancel from 'components/core/save-cancel';
import TextInput from 'components/forms/text-input';
import VerticalFormField from 'components/forms/vertical-form-field';

import styles from './new-form.css';

type Props = {
  name: string,
  defaultLanguage: string,
  site: string,
  countryId: ?number,
  countries: Array<Country>,
  onChange: Function,
  onCancel: Function,
  onSubmit: Function,
};

const NewCatalogForm = (props: Props) => {
  const { defaultLanguage, name, site, countryId, countries } = props;
  const { onCancel, onChange, onSubmit } = props;
  
  const country = _.find(countries, { 'id': countryId });

  let languages = _.get(country, 'languages', []);
  if (_.indexOf('en') == -1) {
    languages = ['en', ...languages];
  }

  const countryItems = countries.map((country) => [country.id, country.name]);
  const languageItems = languages.map((lang) => [lang, lang]);
  
  return (
    <Content>
      <div styleName="form-content">
      <h1>New Catalog</h1>
        <Form onSubmit={onSubmit}>
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
            controlId="site"
            label="Site URL"
          >
            <TextInput
              onChange={(v) => onChange('site', v)}
              value={site}
            />
          </VerticalFormField>
          <VerticalFormField
            controlId="countryId"
            label="Country"
            required
          >
            <Dropdown
              name="countryId"
              value={countryId}
              items={countryItems}
              onChange={(c) => onChange('countryId', c)}
            />
          </VerticalFormField>
          <VerticalFormField
            controlId="defaultLanguage"
            label="Default Language"
            required
          >
            <Dropdown
              name="defaultLanguage"
              value={defaultLanguage}
              items={languageItems}
              onChange={(l) => onChange('defaultLanguage', l)}
            />
          </VerticalFormField>
          <div styleName="submit-block">
            <SaveCancel
              onCancel={onCancel}
              saveText="Save Catalog"
            />
          </div>
        </Form>
      </div>
    </Content>
  );
};

export default NewCatalogForm;
