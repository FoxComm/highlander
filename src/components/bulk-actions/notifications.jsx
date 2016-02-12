// libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// helpers
import { inflect, capitalize } from '../../lib/text-utils';


class Notification extends React.Component {

  static propTypes = {
    resultType: PropTypes.oneOf([
      'success',
      'error',
    ]),
    entityForms: PropTypes.array.isRequired,
    overviewMessage: PropTypes.string.isRequired,
    children: PropTypes.node.isRequired,
    onHide: PropTypes.func.isRequired,
  };

  state = {
    expanded: false,
  };

  @autobind
  toggleExpanded() {
    this.setState({expanded: !this.state.expanded});
  }

  render() {
    const {resultType, entityForms, overviewMessage, children, onHide} = this.props;
    const {expanded} = this.state;
    const count = React.Children.count(children);
    const message = `${count} ${inflect(count, entityForms[0], entityForms[1])} ${overviewMessage}. `;

    return (
      <div className={classNames('fc-bulk-notification', `_${resultType}`)}>
        <div>
          <div className="fc-bulk-notification__preview">
            <i className={`fc-bulk-notification__icon icon-${resultType}`} />
            {message}
            <a className="fc-bulk-notification__details-link" onClick={this.toggleExpanded}>
              {expanded ? 'View Less...' : 'View Details...'}
            </a>
            <span className="fc-bulk-notification__flex-separator"></span>
            <i onClick={onHide} className="fc-bulk-notification__close fc-btn-close icon-close" title="Close" />
          </div>
          <div className={classNames('fc-bulk-notification__details', {'_open': expanded})}>
            {children}
          </div>
        </div>
      </div>
    );
  }
}

export function SuccessNotification(props) {
  return <Notification resultType="success" {...props} />;
}

export function ErrorNotification(props) {
  return <Notification resultType="error" {...props} />;
}
