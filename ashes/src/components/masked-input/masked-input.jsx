import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import _ from 'lodash';

import InputMask from 'react-input-mask';

function formatMask(mask, prepend) {
  if (_.isEmpty(mask)) {
    return '';
  }

  return `${prepend}${mask}`.replace(/a/, '\\a'); // escape "a" symbol as it's a rule for react-input-mask
}

export default class MaskedInput extends Component {
  static propTypes = {
    mask: PropTypes.string,
    prepend: PropTypes.string,
    value: PropTypes.string
  };

  static defaultProps = {
    mask: '',
    prepend: '',
    value: ''
  };

  componentDidUpdate(prevProps) {
    if (!_.isEqual(this.props.mask, prevProps.mask)) {
      const prependLength = _.get(this.props, ['prepend', 'length'], 0);
      const valueLength = _.get(this.props, ['value', 'length'], 0);
      const cursorPos = prependLength == 0 ? valueLength : prependLength;
      const target = ReactDOM.findDOMNode(this.refs.maskedInput);
      target.selectionStart = cursorPos;
      target.selectionEnd = cursorPos;
    }
  }

  focus() {
    this.refs['maskedInput'].getInputDOMNode().focus();
  }

  render() {
    const { mask, prepend, ...rest } = this.props;

    const formattedMask = formatMask(mask, prepend);

    return (
      <InputMask
        ref="maskedInput"
        type="text"
        mask={formattedMask}
        {...rest} />
    );
  }
}
