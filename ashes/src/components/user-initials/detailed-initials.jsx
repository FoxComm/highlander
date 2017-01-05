// libs
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { PropTypes, Component } from 'react';

// components
import UserInitials from './initials';

export default class DetailedUserInitials extends Component {

  state = {
    tooltipShown: false,
  };

  static propTypes = {
    firstName: PropTypes.string,
    lastName: PropTypes.string,
    name: PropTypes.string,
    email: PropTypes.string,
    actionBlock: PropTypes.node,
    showTooltipOnClick: PropTypes.bool
  };

  static defaultProps = {
    showTooltipOnClick: false
  };

  get fullName() {
    return this.props.name || `${this.props.firstName} ${this.props.lastName}`;
  }

  get initials() {
    const initials = <UserInitials {...this.props}/>;

    if (this.props.showTooltipOnClick) {
      return (
        <div onClick={this.toggleState}>{initials}</div>
      );
    } else {
      return initials;
    }
  }

  @autobind
  toggleState() {
    this.setState({tooltipShown: !this.state.tooltipShown});
  }

  render() {
    const rootClass = classNames('initials', 'fc-with-tooltip', {
      '_clickable': this.props.showTooltipOnClick
    });
    const tooltipClass = classNames('fc-tooltip', 'fc-tooltip-left', {
      '_shown': this.props.showTooltipOnClick && this.state.tooltipShown
    });
    const overlayClassName = classNames('fc-tooltip__overlay', {
      '_shown': this.state.tooltipShown
    });
    return (
      <div className={rootClass}>
        {this.initials}
        <div className={tooltipClass}>
          <div className="fc-tooltip__body">
            <div className="fc-strong">{this.fullName}</div>
            {this.props.email && (<div>{this.props.email}</div>)}
          </div>
          {this.props.actionBlock && (<div className="fc-tooltip__actions">{this.props.actionBlock}</div>)}
        </div>
        {this.props.showTooltipOnClick && (<div className={overlayClassName} onClick={this.toggleState}></div>)}
      </div>
    );
  }
}
