/* @flow */

import map from 'lodash/map';
import { connect } from 'react-redux';
import { formValueSelector } from 'redux-form';

import Form from '../../components/form/form';

const selector = formValueSelector('shipping');

export default connect((state, props) => ({ formValues: selector(state, ...map(props.fields, 'name')) }))(Form);
