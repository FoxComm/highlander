/* @flow */

import React from 'react';
import { connect } from 'react-redux';
import { formValueSelector } from 'redux-form';

import Form from '../../components/form/form';
import Field from '../../components/fields/fields';

const renderFields = field => () => <Field {...field} />;

const UploadForm = props => {
  const { fields, file } = props;

  const field = fields[0];
  field.file = file;

  return <Form {...props} renderFields={renderFields(field)} />;
};

const selector = formValueSelector('upload');

export default connect(state => ({ file: selector(state, 'file') }))(UploadForm);
