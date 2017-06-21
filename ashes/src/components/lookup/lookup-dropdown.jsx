// libs
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import Lookup from './lookup';
import Icon from 'components/core/icon';

// helpers
import { prefix } from '../../lib/text-utils';

const prefixed = prefix('fc-lookup');

/**
 * Simplistic lookup component, that is to be extended if needed
 *
 * Used for looking up in given
 */
export default class LookupDropdown extends Component {

  static propTypes = {
    className: PropTypes.string,
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      showMenu: false,
    };
  }

  get button() {
    const icon = this.state.showMenu ? 'chevron-up' : 'chevron-down';

    return (
      <div className="fc-btn fc-dock fc-dock-right"
           onClick={this.toggleMenu}
           onBlur={this.hideMenu}>
        <Icon name={icon} />
      </div>
    );
  }

  @autobind
  hideMenu() {
    if (this.state.showMenu) {
      this.setState({
        showMenu: false,
      });
    }
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
}
