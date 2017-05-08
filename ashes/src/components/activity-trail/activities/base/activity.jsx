
// libs
import { autobind } from 'core-decorators';
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

// components
import { Button } from 'components/core/button';
import { Time } from '../../../common/datetime';
import AuthorTitle from './author-title';
import AuthorIcon from './author-icon';

export default class Activity extends React.Component {

  static propTypes = {
    activity: PropTypes.shape({
      kind: PropTypes.string.isRequired,
    }).isRequired,
    undoAction: PropTypes.func,
    title: PropTypes.node,
    details: PropTypes.shape({
      newOne: PropTypes.node,
      previous: PropTypes.node,
    }),
    isFirst: PropTypes.bool,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      expanded: false
    };
  }

  get controls() {
    if (this.props.undoAction) {
      return (
        <div className="fc-activity__controls">
          <Button onClick={this.props.undoAction}>Undo</Button>
        </div>
      );
    }
  }

  get title() {
    if (this.props.title) {
      return this.props.title;
    }
  }

  get detailsView() {
    const details = this.props.details;

    if (details && this.state.expanded) {
      if (details.newOne || details.previous) {
        const previousTitle = <div className="fc-activity__details-head">Previous</div>;
        const previous = details.previous ? <div>{previousTitle}{details.previous}</div> : null;
        const newTitle = <div className="fc-activity__details-head">New</div>;
        const newOne = details.newOne ? <div>{newTitle}{details.newOne}</div> : null;
        return (
          <div className="fc-activity__details">
            {newOne}
            {previous}
          </div>
        );
      } else {
        return (
          <div className="fc-activity__details">
            {details}
          </div>
        );
      }
    }
  }

  @autobind
  toggleExpanded() {
    this.setState({
      expanded: !this.state.expanded
    });
  }

  get viewMoreLink() {
    if (this.props.details) {
      return (
        <div className="fc-activity__view-more">
          <span onClick={this.toggleExpanded}>
            {this.state.expanded ? '...View Less' : 'View More...'}
          </span>
        </div>
      );
    }
  }

  render() {
    const props = this.props;
    const { activity } = props;

    const className = classNames('fc-activity', {
      '_first': props.isFirst
    });

    return (
      <li className={className}>
        <div className="fc-activity__head-content">
          <div className="fc-activity__body">
            <div className="fc-activity__timestamp">
              <Time value={activity.createdAt} />
            </div>
            <div className="fc-activity__info">
              <AuthorIcon activity={activity} />
              <div className="fc-activity__description">
                <AuthorTitle activity={activity} />&nbsp;{this.title}
                {this.viewMoreLink}
              </div>
            </div>
          </div>
          {this.controls}
        </div>
        {this.detailsView}
      </li>
    );
  }
}
