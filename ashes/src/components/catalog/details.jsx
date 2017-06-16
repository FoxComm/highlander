/* @flow */

import _ from 'lodash';
import React from 'react';

import ContentBox from 'components/content-box/content-box';
import { Dropdown } from 'components/dropdown';
import ErrorAlerts from 'components/alerts/error-alerts';
import Form from 'components/forms/form';
import TextInput from 'components/forms/text-input';
import VerticalFormField from 'components/forms/vertical-form-field';

import styles from './details.css';

type Props = {
  name: string,
  defaultLanguage: string,
  site: string,
  countryId: ?number,
  countries: Array<Country>,
  err: ?any,
  isLoading: boolean,
  onChange: Function,
  onCancel: Function,
  onSubmit: Function,
};

const CatalogDetails = (props: Props) => {
  const { defaultLanguage, name, site, countryId, countries } = props;
  const { onChange, onSubmit } = props;
  const { err } = props;

  const country = _.find(countries, { 'id': countryId });

  let languages = _.get(country, 'languages', []);
  if (_.indexOf('en') == -1) {
    languages = ['en', ...languages];
  }

  const countryItems = countries.map((country) => [country.id, country.name]);
  const languageItems = languages.map((lang) => [lang, lang]);

  return (
    <div styleName="form-content">
      {err && (
        <div>
          <ErrorAlerts error={err} />
        </div>
      )}
      <ContentBox title="General">
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
        </Form>
      </ContentBox>
    </div>
  );
};

export default CatalogDetails;
