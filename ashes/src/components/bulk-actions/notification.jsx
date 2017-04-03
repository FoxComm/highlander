// libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// helpers
import { numberize } from '../../lib/text-utils';


export default class Notification extends React.Component {

  static propTypes = {
    resultType: PropTypes.oneOf([
      'success',
      'error',
    ]),
    hideAlertDetails: PropTypes.boolean,
    entity: PropTypes.string.isRequired,
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

  @autobind
  renderViewLink(expanded,toggleExpanded) {
    if (this.props.hideAlertDetails) return null;
    return (
      <a className="fc-bulk-notification__details-link" onClick={toggleExpanded}>
        {expanded ? 'View Less...' : 'View Details...'}
      </a>
    );
  }

  @autobind
  renderDetailsContainer(expanded,children){
    if (this.props.hideAlertDetails) return null;
    return (
      <div className={classNames('fc-bulk-notification__details', {'_open': expanded})}>
        {children}
      </div>
    );
  }

  render() {
    const {resultType, entity, overviewMessage, children, onHide} = this.props;
    const {expanded} = this.state;
    const count = React.Children.count(children);
    const message = `${count} ${numberize(entity, count)} ${overviewMessage}.`;

    return (
      <div className={classNames('fc-bulk-notification', `_${resultType}`)}>
        <div>
          <div className="fc-bulk-notification__preview">
            <i className={`fc-bulk-notification__icon icon-${resultType}`} />
            <span className="fc-bulk-notification__message">
              {message}
            </span>
            {this.renderViewLink(expanded,this.toggleExpanded)}
            <span className="fc-bulk-notification__flex-separator"></span>
            <i onClick={onHide} className="fc-bulk-notification__close fc-btn-close icon-close" title="Close" />
          </div>
          {this.renderDetailsContainer(expanded,children)}
        </div>
      </div>
    );
  }
}
