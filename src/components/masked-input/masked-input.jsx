import React, { Component, PropTypes } from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';

import InputMask from 'react-input-mask';

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

  componentDidUpdate(prevProps, prevState) {
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
    const { mask, prepend, value, ...rest } = this.props;

    const formattedMask = !_.isEmpty(mask) ? `${prepend}${mask}` : '';

    return (
      <InputMask
        ref="maskedInput"
        type="text"
        mask={formattedMask}
        value={value}
        {...rest} />
    );
  }
}
