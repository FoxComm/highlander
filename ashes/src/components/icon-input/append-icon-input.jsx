//libs
import _ from 'lodash';
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

// components
import Icon from 'components/core/icon';

//helpers
import { prefix } from '../../lib/text-utils';


const prefixed = prefix('fc-icon-input');


export default class AppendIconInput extends Component {
  static propTypes = {
    className: PropTypes.string,
    icon: PropTypes.string,
    children: PropTypes.node.isRequired,
  };

  static defaultProps = {
    icon: 'search',
  };

  state = {
    isFocused: false,
  };

  render() {
    const {className, icon, children} = this.props;

    const input = React.cloneElement(children, {
      className: classNames(children.props.className, prefixed('input')),
      onFocus: () => this.setState({isFocused: true}, children.props.onFocus || _.noop),
      onBlur: () => this.setState({isFocused: false}, children.props.onBlur || _.noop),
    });

    return (
      <div className={classNames(prefixed(), className, {'_active': this.state.isFocused})}>
        {input}
        {icon ? <Icon className={prefixed('icon')} name={icon} /> : null}
      </div>
    );
  }
}
