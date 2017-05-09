import React, { Component } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import Form from './form';

/**
 * FoxyForm is a simple form that emphasizes using the contents of the onSubmit
 * event that propogates from <Form> to determine form state on submit, rather
 * than Redux or React state. The names and values of all input fields in the
 * form are serialized into a flat map and returned to the caller through the
 * onSubmit method.
 */
export default class FoxyForm extends Component {
  static propTypes = {
    children: PropTypes.node,
    onSubmit: PropTypes.func,
  };

  static defaultProps = {
    onSubmit: _.noop,
  };

  handleSubmit(onSubmit, event) {
    event.preventDefault();
    const { target } = event;
    let data = {};
    for (let i = 0; i < target.length; i++) {
      const elt = target[i];
      if (elt.type !== 'submit') {
        data[target[i].name] = target[i].value;
      }
    }

    onSubmit(data);
  }

  render() {
    const { children, onSubmit, ...rest } = this.props;
    return (
      <Form onSubmit={(event) => this.handleSubmit(onSubmit, event)} {...rest}>
        {children}
      </Form>
    );
  }
}
