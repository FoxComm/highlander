/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// components
import { TextInput } from 'ui/text-input';
import { FormField } from 'ui/forms';

// styles
import styles from './edit-promos.css';

type Props = {
  className?: string,
  onChange: (code: string) => void,
  placeholder?: string,
  saveCode: () => void,
};

class EditPromos extends Component {
  props: Props;

  @autobind
  changeCode({ target }: SyntheticInputEvent) {
    this.props.onChange(target.value);
  }

  @autobind
  onKeyPress(e: Object) {
    if (e.key === 'Enter') {
      e.preventDefault();

      this.props.saveCode();
    }
  }

  render() {
    const { className } = this.props;
    const classes = classNames(styles['edit-promos'], className);

    return (
      <div className={classes}>
        <FormField styleName="code-field">
          <TextInput
            required
            styleName="code-input"
            placeholder={this.props.placeholder}
            onChange={this.changeCode}
            onKeyPress={this.onKeyPress}
          />
        </FormField>
      </div>
    );
  }
}

export default EditPromos;
