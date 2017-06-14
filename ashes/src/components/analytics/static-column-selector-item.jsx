/* @flow weak */

// libs
import React from 'react';

// components
import { Checkbox } from 'components/core/checkbox';

// styles
import s from './static-column-selector.css';

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
          className={s.checkbox}
          label={text}
          onChange={onChange}
          checked={checked}
        />
      </li>
    );
  }
}
