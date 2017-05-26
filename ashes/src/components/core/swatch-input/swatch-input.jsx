/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import TextInput from 'components/forms/text-input';

// styles
import s from './swatch-input.css';

type Props = {
  /** Callback called on input change */
  onChange: (value: string) => void,
  /** Input value */
  value: string,
};

/**
 * SwatchInput is a simple component responsible for showing a square,
 * colored according to hex code, which can be defined in component's input.
 *
 * @class SwatchInput
 */

class SwatchInput extends Component {
  props: Props;

  static defaultProps = {
    value: '',
  };

  @autobind
  handleChange(value: string) {
    this.props.onChange(value);
  }

  render() {
    const hexCode = this.props.value;
    const colorStyle = {
      background: `#${hexCode}`,
    };

    return (
      <div className={s.swatchInput}>
        <TextInput
          id="swatch-fld"
          type="text"
          maxLength="6"
          className={s.input}
          onChange={this.handleChange}
          value={hexCode}
        />
        <span className={s.swatch} style={colorStyle} key={hexCode} />
      </div>
    );
  }
}

export default SwatchInput;
