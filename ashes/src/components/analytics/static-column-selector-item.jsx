/**
 * @flow weak
 */

// libs
import React from 'react';

// components
import { Checkbox } from '../checkbox/checkbox';

// styles
import styles from './static-column-selector.css';

// types
type Props = {
  onChange?: ?Function,
  index: number,
  checked: boolean,
  id: any,
  text: string,
}

export default class StaticColumnSelectorItem extends React.Component {
  props: Props;

  static defaultProps = {
    checked: true,
  };

  render() {
    const { id, text, onChange, checked } = this.props;

    return (
      <li>
        <Checkbox
          id={`choose-column-${id}`}
          onChange={onChange}
          checked={checked}>
          {text}
        </Checkbox>
      </li>
    );
  }
}
