import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import InputMask from 'react-input-mask';

export default class MaskedInput extends React.Component {
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
      const cursorPos = _.get(this.props, ['prepend', 'length'], 0);
      const target = React.findDOMNode(this.refs.maskedInput);
      target.selectionStart = cursorPos;
      target.selectionEnd = cursorPos;
    }
  }

  render() {
    const { mask, prepend, value, ...rest } = this.props;

    return (
      <InputMask
        ref="maskedInput"
        mask={`${prepend}${mask}`}
        value={value}
        {...rest} />
    );
  }
}
