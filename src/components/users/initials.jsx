import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

export default class UserInitials extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      tooltipShown: false
    };
  }

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
    let {firstName, lastName, name, email} = this.props;
    if (!firstName && !lastName) {
      if (name) {
        [firstName, lastName] = this.props.name.split(/\s+/);
      } else {
        throw new Error('UserInitials: at least firstName,lastName or name are required');
      }
    }
    if (this.props.showTooltipOnClick) {
      return (
        <div onClick={this.toggleState}>
          { `${firstName.charAt(0)}${lastName.charAt(0)}` }
        </div>
      );
    } else {
      return `${firstName.charAt(0)}${lastName.charAt(0)}`;
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
