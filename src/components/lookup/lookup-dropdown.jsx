// libs
import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// helpers
import { prefix } from '../../lib/text-utils';

// components
import Lookup from './lookup';
import { DecrementButton, IncrementButton } from '../common/buttons';


const prefixed = prefix('fc-lookup__');

/**
 * Simplistic lookup component, that is to be extended if needed
 *
 * Used for looking up in given
 */
export default class LookupDropdown extends Component {

  constructor(props, context) {
    super(props, context);

    this.state = {
      showMenu: false,
    };
  }

  get button() {
    const button = this.state.showMenu
      ? <IncrementButton />
      : <DecrementButton />;

    return React.cloneElement(button, {
      className: 'fc-dock fc-dock-right',
      onClick: this.toggleMenu,
      onBlur: this.toggleMenu,
    });
  }

  @autobind
  toggleMenu() {
    this.setState({
      showMenu: !this.state.showMenu,
    });
  }

  render() {
    const {className, ...rest} = this.props;

    return (
      <div className={classNames(prefixed('dropdown'), className)}>
        <Lookup inputClassName="fc-dock fc-dock-left"
                showMenu={this.state.showMenu}
                onToggleMenu={showMenu => this.setState({showMenu})}
                {...rest} />
        {this.button}
      </div>
    );
  }
};
